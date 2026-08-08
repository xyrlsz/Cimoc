/*
 * QuickJS JavaScript 引擎的 Android JNI 封装
 *
 * 底层引擎：https://github.com/quickjs-ng/quickjs (MIT License, QuickJS-NG)
 *
 * 特性：
 *  - 每个 JSRuntime 相互隔离（独立堆、单线程模型）
 *  - 内存上限 / JS 调用栈上限 / 单次执行超时三重保护
 *  - 未注册 std/os 模块，脚本无法访问文件系统、进程等宿主资源
 *  - 兼容层：atob/btoa（基于内置 Uint8Array.fromBase64/toBase64）、console.log/print
 */

#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <android/log.h>

#include "quickjs.h"
#include "utf8to16.h"

#define LOG_TAG "QuickJS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define QJS_TIMEOUT_MS    (10 * 1000)         /* 单次脚本执行超时 */
#define QJS_MEMORY_LIMIT  (32 * 1024 * 1024)  /* 运行时内存上限 32MB */
#define QJS_STACK_SIZE    (512 * 1024)        /* JS 调用栈上限 512KB */

typedef struct {
    int64_t deadline_ms; /* 单调时钟截止时间 */
} QJSRuntimeData;

/* 全局 JavaVM 指针，用于 JS -> Java 宿主回调（hostCall） */
static JavaVM *g_vm = NULL;

/* ---------------- 工具 ---------------- */

static int64_t now_ms(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t) ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

/* 执行超时中断回调：返回非 0 时引擎中止执行 */
static int qjs_interrupt_handler(JSRuntime *rt, void *opaque) {
    QJSRuntimeData *data = (QJSRuntimeData *) opaque;
    return now_ms() >= data->deadline_ms;
}

/* 将 JSValue 转成 Java String（正确处理增补平面字符/代理对）。
 * 注意：NewStringUTF 期望"修改版 UTF-8"，直接传入标准 UTF-8 会在
 * 非 BMP 字符（如 emoji）处出错，故统一走 UTF-8 -> UTF-16 转换。 */
static jstring qjs_jsvalue_to_jstring(JNIEnv *env, JSContext *ctx, JSValueConst val) {
    size_t len = 0;
    const char *utf8 = JS_ToCStringLen2(ctx, &len, val, 0);
    jstring jstr;

    if (!utf8)
        return (*env)->NewStringUTF(env, "");

    if (len == 0) {
        jstr = (*env)->NewStringUTF(env, "");
    } else {
        jchar *utf16 = (jchar *) malloc((len + 1) * sizeof(jchar));
        if (utf16) {
            size_t n = utf8_to_utf16(utf8, len, utf16);
            jstr = (*env)->NewString(env, utf16, (jsize) n);
            free(utf16);
        } else {
            jstr = (*env)->NewStringUTF(env, "");
        }
    }
    JS_FreeCString(ctx, utf8);
    return jstr;
}

/* ---------------- console.log / print ---------------- */

static JSValue js_console_log(JSContext *ctx, JSValueConst this_val,
                              int argc, JSValueConst *argv) {
    size_t cap = 256, len = 0;
    char *buf = (char *) js_malloc(ctx, cap);
    JSValue ret = JS_UNDEFINED;
    int i;

    if (!buf)
        return JS_EXCEPTION;
    for (i = 0; i < argc; i++) {
        const char *str;
        JSValue s = JS_ToString(ctx, argv[i]);
        if (JS_IsException(s)) {
            ret = JS_EXCEPTION;
            goto done;
        }
        str = JS_ToCString(ctx, s);
        if (str) {
            size_t slen = strlen(str);
            if (len + slen + 2 > cap) {
                char *nbuf;
                cap = (len + slen + 2) * 2;
                nbuf = (char *) js_realloc(ctx, buf, cap);
                if (!nbuf) {
                    JS_FreeCString(ctx, str);
                    JS_FreeValue(ctx, s);
                    ret = JS_EXCEPTION;
                    goto done;
                }
                buf = nbuf;
            }
            if (i > 0)
                buf[len++] = ' ';
            memcpy(buf + len, str, slen);
            len += slen;
            JS_FreeCString(ctx, str);
        }
        JS_FreeValue(ctx, s);
    }
    buf[len] = '\0';
    LOGI("%s", buf);
    done:
    js_free(ctx, buf);
    return ret;
}

/* ---------------- atob / btoa 兼容层 ---------------- */
/* 基于 QuickJS 内置的 Uint8Array.fromBase64 / toBase64 实现，
   避免手写 Latin-1 字符串转换的坑。 */
static const char qjs_helpers_js[] =
        "if (typeof globalThis.atob !== 'function') {"
        "  globalThis.atob = function(b64) {"
        "    var bytes = Uint8Array.fromBase64(String(b64));"
        "    var out = '', i;"
        "    for (i = 0; i < bytes.length; i++) out += String.fromCharCode(bytes[i]);"
        "    return out;"
        "  };"
        "}"
        "if (typeof globalThis.btoa !== 'function') {"
        "  globalThis.btoa = function(bin) {"
        "    var s = String(bin), bytes = new Uint8Array(s.length), i;"
        "    for (i = 0; i < s.length; i++) bytes[i] = s.charCodeAt(i) & 0xFF;"
        "    return bytes.toBase64();"
        "  };"
        "}";

/* ---------------- JS -> Java 宿主回调 ---------------- */
/* 安装为全局函数 hostCall(name, argsJson)：
 *  - name     : 宿主方法名（如 "fetch" / "dom_selectAll"）
 *  - argsJson : 参数数组的 JSON 字符串
 * 回调 Java 静态方法 QuickJSEngine.onHostCall(name, argsJson)，
 * 其返回值为 JSON 字符串；C 层把该 JSON 解析回 JS 值返回给脚本。
 * 宿主侧出错时 Java 返回 null 或 {"__error__":"..."}，此处转为 JS 异常。 */
static JSValue js_host_call(JSContext *ctx, JSValueConst this_val,
                            int argc, JSValueConst *argv) {
    JNIEnv *env = NULL;
    jboolean attached = JNI_FALSE;
    jstring name_j = NULL, args_j = NULL, result_j = NULL;
    jclass clazz = NULL;
    jmethodID mid = NULL;
    const char *name_s = NULL, *args_s = NULL;
    JSValue ret;

    if (argc < 2 || !JS_IsString(argv[0]) || !JS_IsString(argv[1]))
        return JS_ThrowTypeError(ctx, "hostCall expects (name, argsJson)");
    if (g_vm == NULL)
        return JS_ThrowInternalError(ctx, "host bridge not initialized");

    name_s = JS_ToCString(ctx, argv[0]);
    args_s = JS_ToCString(ctx, argv[1]);
    if (!name_s || !args_s) {
        if (name_s) JS_FreeCString(ctx, name_s);
        if (args_s) JS_FreeCString(ctx, args_s);
        return JS_EXCEPTION;
    }

    if ((*g_vm)->GetEnv(g_vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        if ((*g_vm)->AttachCurrentThread(g_vm, &env, NULL) != JNI_OK) {
            JS_FreeCString(ctx, name_s);
            JS_FreeCString(ctx, args_s);
            return JS_ThrowInternalError(ctx, "failed to attach JNI thread");
        }
        attached = JNI_TRUE;
    }

    name_j = (*env)->NewStringUTF(env, name_s);
    args_j = (*env)->NewStringUTF(env, args_s);
    JS_FreeCString(ctx, name_s);
    JS_FreeCString(ctx, args_s);
    if (!name_j || !args_j) {
        ret = JS_ThrowInternalError(ctx, "host call arg conversion failed");
        goto cleanup;
    }

    clazz = (*env)->FindClass(env, "com/xyrlsz/quickjs/QuickJSEngine");
    if (!clazz) {
        ret = JS_ThrowInternalError(ctx, "host bridge class not found");
        goto cleanup;
    }
    mid = (*env)->GetStaticMethodID(env, clazz, "onHostCall",
                                    "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    if (!mid) {
        ret = JS_ThrowInternalError(ctx, "host bridge method not found");
        goto cleanup;
    }

    result_j = (jstring) (*env)->CallStaticObjectMethod(env, clazz, mid, name_j, args_j);
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionClear(env);
        ret = JS_ThrowInternalError(ctx, "host call threw an exception");
        goto cleanup;
    }

    if (result_j == NULL) {
        ret = JS_ThrowInternalError(ctx, "host call failed (null result)");
        goto cleanup;
    }

    {
        const char *res_s = (*env)->GetStringUTFChars(env, result_j, NULL);
        if (res_s == NULL) {
            ret = JS_ThrowInternalError(ctx, "host call result conversion failed");
            goto cleanup;
        }
        ret = JS_ParseJSON(ctx, res_s, strlen(res_s), "<host>");
        (*env)->ReleaseStringUTFChars(env, result_j, res_s);
        if (JS_IsException(ret)) {
            /* 宿主返回了非法 JSON（理论上不会发生），转成可读错误 */
            JSValue exc = JS_GetException(ctx);
            const char *msg = JS_ToCString(ctx, exc);
            JS_FreeValue(ctx, exc);
            JS_FreeValue(ctx, ret);
            ret = JS_ThrowInternalError(ctx, "host returned invalid JSON: %s",
                                        msg ? msg : "unknown");
            if (msg) JS_FreeCString(ctx, msg);
        }
    }

    /* 宿主调用结束后重置本次执行的超时截止时间，避免长网络请求吃掉脚本剩余配额 */
    {
        JSRuntime *rt = JS_GetRuntime(ctx);
        QJSRuntimeData *data = (QJSRuntimeData *) JS_GetRuntimeOpaque(rt);
        if (data)
            data->deadline_ms = now_ms() + QJS_TIMEOUT_MS;
    }

    goto cleanup;

    cleanup:
    if (name_j) (*env)->DeleteLocalRef(env, name_j);
    if (args_j) (*env)->DeleteLocalRef(env, args_j);
    if (result_j) (*env)->DeleteLocalRef(env, result_j);
    if (clazz) (*env)->DeleteLocalRef(env, clazz);
    if (attached)
        (*g_vm)->DetachCurrentThread(g_vm);
    return ret;
}

static void qjs_register_helpers(JSContext *ctx) {
    JSValue global, log, console, host, ret;

    global = JS_GetGlobalObject(ctx);

    log = JS_NewCFunction(ctx, js_console_log, "log", 1);
    console = JS_NewObject(ctx);
    JS_SetPropertyStr(ctx, console, "log", JS_DupValue(ctx, log));
    JS_SetPropertyStr(ctx, global, "console", console);
    JS_SetPropertyStr(ctx, global, "print", log);

    host = JS_NewCFunction(ctx, js_host_call, "hostCall", 2);
    JS_SetPropertyStr(ctx, global, "hostCall", host);

    JS_FreeValue(ctx, global);

    ret = JS_Eval(ctx, qjs_helpers_js, strlen(qjs_helpers_js), "<helpers>",
                  JS_EVAL_TYPE_GLOBAL);
    if (JS_IsException(ret)) {
        JSValue exc = JS_GetException(ctx);
        const char *msg = JS_ToCString(ctx, exc);
        LOGE("Failed to install atob/btoa: %s", msg ? msg : "unknown");
        if (msg)
            JS_FreeCString(ctx, msg);
        JS_FreeValue(ctx, exc);
    }
    JS_FreeValue(ctx, ret);
}

/* ---------------- JNI 方法 ---------------- */

static jlong native_create_runtime(JNIEnv *env, jclass clazz) {
    JSRuntime *rt = JS_NewRuntime();
    QJSRuntimeData *data;

    if (!rt)
        return 0;
    data = (QJSRuntimeData *) calloc(1, sizeof(QJSRuntimeData));
    if (!data) {
        JS_FreeRuntime(rt);
        return 0;
    }
    data->deadline_ms = now_ms() + QJS_TIMEOUT_MS;
    JS_SetRuntimeOpaque(rt, data);
    JS_SetMemoryLimit(rt, QJS_MEMORY_LIMIT);
    JS_SetMaxStackSize(rt, QJS_STACK_SIZE);
    JS_SetInterruptHandler(rt, qjs_interrupt_handler, data);
    return (jlong) (intptr_t) rt;
}

static jlong native_create_context(JNIEnv *env, jclass clazz, jlong rt_ptr) {
    JSRuntime *rt = (JSRuntime *) (intptr_t) rt_ptr;
    JSContext *ctx;

    if (!rt)
        return 0;
    ctx = JS_NewContext(rt);
    if (!ctx)
        return 0;
    qjs_register_helpers(ctx);
    return (jlong) (intptr_t) ctx;
}

static jstring native_evaluate(JNIEnv *env, jclass clazz, jlong ctx_ptr,
                               jstring script, jstring filename, jstring var_name) {
    JSContext *ctx = (JSContext *) (intptr_t) ctx_ptr;
    JSRuntime *rt;
    QJSRuntimeData *data;
    const char *script_str;
    const char *filename_str = "<eval>";
    jboolean filename_allocated = JNI_FALSE;
    const char *vname = NULL;
    JSValue result;
    jstring jresult = NULL;

    if (!ctx)
        return (*env)->NewStringUTF(env, "");

    /* 重置本次执行的超时截止时间 */
    rt = JS_GetRuntime(ctx);
    data = (QJSRuntimeData *) JS_GetRuntimeOpaque(rt);
    if (data)
        data->deadline_ms = now_ms() + QJS_TIMEOUT_MS;

    script_str = (*env)->GetStringUTFChars(env, script, NULL);
    if (!script_str)
        return (*env)->NewStringUTF(env, "");

    if (filename) {
        const char *fs = (*env)->GetStringUTFChars(env, filename, NULL);
        if (fs) {
            filename_str = fs;
            filename_allocated = JNI_TRUE;
        }
    }
    if (var_name)
        vname = (*env)->GetStringUTFChars(env, var_name, NULL);

    result = JS_Eval(ctx, script_str, strlen(script_str), filename_str,
                     JS_EVAL_TYPE_GLOBAL);

    if (JS_IsException(result)) {
        JSValue exc = JS_GetException(ctx);
        const char *msg = JS_ToCString(ctx, exc);
        LOGE("JS exception: %s", msg ? msg : "(unknown)");
        if (msg)
            JS_FreeCString(ctx, msg);
        JS_FreeValue(ctx, exc);
        JS_FreeValue(ctx, result);
        jresult = (*env)->NewStringUTF(env, "");
        goto done;
    }

    /* 需要返回指定全局变量的值 */
    if (vname != NULL) {
        JSValue global = JS_GetGlobalObject(ctx);
        JSValue prop = JS_GetPropertyStr(ctx, global, vname);
        JS_FreeValue(ctx, global);
        JS_FreeValue(ctx, result);
        if (JS_IsException(prop)) {
            JSValue exc = JS_GetException(ctx);
            const char *msg = JS_ToCString(ctx, exc);
            LOGE("JS exception (get var '%s'): %s", vname,
                 msg ? msg : "(unknown)");
            if (msg)
                JS_FreeCString(ctx, msg);
            JS_FreeValue(ctx, exc);
            JS_FreeValue(ctx, prop);
            jresult = (*env)->NewStringUTF(env, "");
            goto done;
        }
        result = prop;
    }

    /* 结果转字符串（数组会按 Array.prototype.toString 以逗号连接，
       与旧 Rhino 的 Context.toString 行为一致） */
    {
        JSValue str_val = JS_ToString(ctx, result);
        JS_FreeValue(ctx, result);
        if (JS_IsException(str_val)) {
            JSValue exc = JS_GetException(ctx);
            const char *msg = JS_ToCString(ctx, exc);
            LOGE("JS exception (to string): %s", msg ? msg : "(unknown)");
            if (msg)
                JS_FreeCString(ctx, msg);
            JS_FreeValue(ctx, exc);
            JS_FreeValue(ctx, str_val);
            jresult = (*env)->NewStringUTF(env, "");
            goto done;
        }
        jresult = qjs_jsvalue_to_jstring(env, ctx, str_val);
        JS_FreeValue(ctx, str_val);
    }

    done:
    if (jresult == NULL)
        jresult = (*env)->NewStringUTF(env, "");
    (*env)->ReleaseStringUTFChars(env, script, script_str);
    if (filename_allocated)
        (*env)->ReleaseStringUTFChars(env, filename, filename_str);
    if (vname)
        (*env)->ReleaseStringUTFChars(env, var_name, vname);
    return jresult;
}

/* 调用全局 JS 函数：argsJson 为参数数组的 JSON 字符串（如 "[\"a\",\"b\"]"），
 * 结果以 JSON 字符串返回（JS_JSONStringify）。函数不存在返回 "null"；
 * 执行抛异常时转成 Java RuntimeException 抛出。 */
static jstring native_call_function(JNIEnv *env, jclass clazz, jlong ctx_ptr,
                                    jstring name, jstring args_json, jlong timeout_ms) {
    JSContext *ctx = (JSContext *) (intptr_t) ctx_ptr;
    JSRuntime *rt;
    QJSRuntimeData *data;
    const char *name_s = NULL;
    const char *args_s = NULL;
    JSValue global, func, args_val, ret;
    JSValue len_val;
    int argc = 0, i;
    int func_valid = 0, args_valid = 0;
    JSValue *argv = NULL;
    jstring jresult = NULL;

    if (!ctx)
        return (*env)->NewStringUTF(env, "null");

    /* 重置本次调用的超时截止时间 */
    rt = JS_GetRuntime(ctx);
    data = (QJSRuntimeData *) JS_GetRuntimeOpaque(rt);
    if (data)
        data->deadline_ms = now_ms() + (timeout_ms > 0 ? timeout_ms : QJS_TIMEOUT_MS);

    name_s = (*env)->GetStringUTFChars(env, name, NULL);
    if (!name_s)
        return (*env)->NewStringUTF(env, "null");
    args_s = (*env)->GetStringUTFChars(env, args_json, NULL);
    if (!args_s) {
        (*env)->ReleaseStringUTFChars(env, name, name_s);
        return (*env)->NewStringUTF(env, "null");
    }

    global = JS_GetGlobalObject(ctx);
    func = JS_GetPropertyStr(ctx, global, name_s);
    JS_FreeValue(ctx, global);

    if (JS_IsException(func)) {
        JSValue exc = JS_GetException(ctx);
        JS_FreeValue(ctx, exc);
        JS_FreeValue(ctx, func);
        jresult = (*env)->NewStringUTF(env, "null");
        goto done;
    }
    if (!JS_IsFunction(ctx, func)) {
        /* 函数未定义 -> 返回 "null"，不视为错误 */
        JS_FreeValue(ctx, func);
        jresult = (*env)->NewStringUTF(env, "null");
        goto done;
    }
    func_valid = 1;

    /* 解析参数数组 */
    args_val = JS_ParseJSON(ctx, args_s, strlen(args_s), "<args>");
    if (JS_IsException(args_val)) {
        JSValue exc = JS_GetException(ctx);
        JS_FreeValue(ctx, exc);
        JS_FreeValue(ctx, args_val);
        (*env)->ThrowNew(env, "java/lang/RuntimeException", "invalid args JSON");
        jresult = NULL;
        goto done;
    }
    if (!JS_IsArray(args_val)) {
        JS_FreeValue(ctx, args_val);
        (*env)->ThrowNew(env, "java/lang/RuntimeException", "args JSON must be an array");
        jresult = NULL;
        goto done;
    }
    args_valid = 1;

    len_val = JS_GetPropertyStr(ctx, args_val, "length");
    JS_ToInt32(ctx, &argc, len_val);
    JS_FreeValue(ctx, len_val);
    if (argc < 0)
        argc = 0;

    if (argc > 0) {
        argv = (JSValue *) malloc(sizeof(JSValue) * argc);
        if (!argv) {
            (*env)->ThrowNew(env, "java/lang/RuntimeException", "out of memory");
            jresult = NULL;
            goto done;
        }
        for (i = 0; i < argc; i++) {
            argv[i] = JS_GetPropertyUint32(ctx, args_val, i);
            if (JS_IsException(argv[i])) {
                JSValue exc = JS_GetException(ctx);
                JS_FreeValue(ctx, exc);
                while (--i >= 0)
                    JS_FreeValue(ctx, argv[i]);
                free(argv);
                argv = NULL;
                (*env)->ThrowNew(env, "java/lang/RuntimeException", "invalid arg");
                jresult = NULL;
                goto done;
            }
        }
    }

    ret = JS_Call(ctx, func, JS_UNDEFINED, argc, argv);
    if (argv) {
        for (i = 0; i < argc; i++)
            JS_FreeValue(ctx, argv[i]);
        free(argv);
        argv = NULL;
    }
    JS_FreeValue(ctx, args_val);
    args_valid = 0;
    JS_FreeValue(ctx, func);
    func_valid = 0;

    if (JS_IsException(ret)) {
        JSValue exc = JS_GetException(ctx);
        const char *msg = JS_ToCString(ctx, exc);
        LOGE("JS exception in %s: %s", name_s, msg ? msg : "(unknown)");
        if (msg) {
            jclass rte = (*env)->FindClass(env, "java/lang/RuntimeException");
            if (rte) {
                (*env)->ThrowNew(env, rte, msg);
                (*env)->DeleteLocalRef(env, rte);
            }
            JS_FreeCString(ctx, msg);
        }
        JS_FreeValue(ctx, exc);
        JS_FreeValue(ctx, ret);
        jresult = NULL;
        goto done;
    }

    /* 结果 JSON 序列化返回 */
    {
        JSValue str_val = JS_JSONStringify(ctx, ret, JS_UNDEFINED, JS_UNDEFINED);
        JS_FreeValue(ctx, ret);
        if (JS_IsException(str_val)) {
            JSValue exc = JS_GetException(ctx);
            JS_FreeValue(ctx, exc);
            JS_FreeValue(ctx, str_val);
            jresult = (*env)->NewStringUTF(env, "null");
            goto done;
        }
        jresult = qjs_jsvalue_to_jstring(env, ctx, str_val);
        JS_FreeValue(ctx, str_val);
    }

    done:
    if (jresult == NULL)
        jresult = (*env)->NewStringUTF(env, "null");
    if (args_valid)
        JS_FreeValue(ctx, args_val);
    if (func_valid)
        JS_FreeValue(ctx, func);
    (*env)->ReleaseStringUTFChars(env, name, name_s);
    (*env)->ReleaseStringUTFChars(env, args_json, args_s);
    return jresult;
}

/* 检测全局函数是否已定义 */
static jboolean native_has_global_function(JNIEnv *env, jclass clazz, jlong ctx_ptr,
                                           jstring name) {
    JSContext *ctx = (JSContext *) (intptr_t) ctx_ptr;
    const char *name_s;
    JSValue global, func;
    jboolean result = JNI_FALSE;

    if (!ctx)
        return JNI_FALSE;
    name_s = (*env)->GetStringUTFChars(env, name, NULL);
    if (!name_s)
        return JNI_FALSE;
    global = JS_GetGlobalObject(ctx);
    func = JS_GetPropertyStr(ctx, global, name_s);
    if (!JS_IsException(func) && JS_IsFunction(ctx, func))
        result = JNI_TRUE;
    if (JS_IsException(func)) {
        JSValue exc = JS_GetException(ctx);
        JS_FreeValue(ctx, exc);
    }
    JS_FreeValue(ctx, func);
    JS_FreeValue(ctx, global);
    (*env)->ReleaseStringUTFChars(env, name, name_s);
    return result;
}

/* 读取全局变量的 JSON 序列化值（用于读取源脚本元数据，如 SOURCE 对象）。
 * 变量缺失/出错时返回 "null"。 */
static jstring native_get_global_json(JNIEnv *env, jclass clazz, jlong ctx_ptr,
                                      jstring name) {
    JSContext *ctx = (JSContext *) (intptr_t) ctx_ptr;
    const char *name_s;
    JSValue global, prop, str_val;
    jstring jresult;

    if (!ctx)
        return (*env)->NewStringUTF(env, "null");
    name_s = (*env)->GetStringUTFChars(env, name, NULL);
    if (!name_s)
        return (*env)->NewStringUTF(env, "null");
    global = JS_GetGlobalObject(ctx);
    prop = JS_GetPropertyStr(ctx, global, name_s);
    JS_FreeValue(ctx, global);
    if (JS_IsException(prop)) {
        JSValue exc = JS_GetException(ctx);
        JS_FreeValue(ctx, exc);
        JS_FreeValue(ctx, prop);
        (*env)->ReleaseStringUTFChars(env, name, name_s);
        return (*env)->NewStringUTF(env, "null");
    }
    str_val = JS_JSONStringify(ctx, prop, JS_UNDEFINED, JS_UNDEFINED);
    JS_FreeValue(ctx, prop);
    if (JS_IsException(str_val)) {
        JSValue exc = JS_GetException(ctx);
        JS_FreeValue(ctx, exc);
        JS_FreeValue(ctx, str_val);
        (*env)->ReleaseStringUTFChars(env, name, name_s);
        return (*env)->NewStringUTF(env, "null");
    }
    jresult = qjs_jsvalue_to_jstring(env, ctx, str_val);
    JS_FreeValue(ctx, str_val);
    (*env)->ReleaseStringUTFChars(env, name, name_s);
    return jresult;
}

static void native_free_context(JNIEnv *env, jclass clazz, jlong ctx_ptr) {
    JSContext *ctx = (JSContext *) (intptr_t) ctx_ptr;
    if (ctx)
        JS_FreeContext(ctx);
}

static void native_free_runtime(JNIEnv *env, jclass clazz, jlong rt_ptr) {
    JSRuntime *rt = (JSRuntime *) (intptr_t) rt_ptr;
    if (rt) {
        QJSRuntimeData *data = (QJSRuntimeData *) JS_GetRuntimeOpaque(rt);
        JS_FreeRuntime(rt);
        free(data);
    }
}

/* ---------------- 注册 ---------------- */

static const JNINativeMethod g_methods[] = {
        {"nativeCreateRuntime", "()J",
                (void *) native_create_runtime},
        {"nativeCreateContext", "(J)J",
                (void *) native_create_context},
        {"nativeEvaluate",      "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                (void *) native_evaluate},
        {"nativeCallFunction",  "(JLjava/lang/String;Ljava/lang/String;J)Ljava/lang/String;",
                (void *) native_call_function},
        {"nativeHasGlobalFunction", "(JLjava/lang/String;)Z",
                (void *) native_has_global_function},
        {"nativeGetGlobalJson", "(JLjava/lang/String;)Ljava/lang/String;",
                (void *) native_get_global_json},
        {"nativeFreeContext",   "(J)V",
                (void *) native_free_context},
        {"nativeFreeRuntime",   "(J)V",
                (void *) native_free_runtime},
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    jclass clazz;

    g_vm = vm;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK)
        return JNI_ERR;
    clazz = (*env)->FindClass(env, "com/xyrlsz/quickjs/QuickJSEngine");
    if (!clazz)
        return JNI_ERR;
    if ((*env)->RegisterNatives(env, clazz, g_methods,
                                (int) (sizeof(g_methods) / sizeof(g_methods[0]))) != JNI_OK)
        return JNI_ERR;
    return JNI_VERSION_1_6;
}
