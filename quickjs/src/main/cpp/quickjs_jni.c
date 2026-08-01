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

#define LOG_TAG "QuickJS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define QJS_TIMEOUT_MS    (10 * 1000)         /* 单次脚本执行超时 */
#define QJS_MEMORY_LIMIT  (32 * 1024 * 1024)  /* 运行时内存上限 32MB */
#define QJS_STACK_SIZE    (512 * 1024)        /* JS 调用栈上限 512KB */

typedef struct {
    int64_t deadline_ms; /* 单调时钟截止时间 */
} QJSRuntimeData;

/* ---------------- 工具 ---------------- */

static int64_t now_ms(void)
{
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t)ts.tv_sec * 1000 + ts.tv_nsec / 1000000;
}

/* 执行超时中断回调：返回非 0 时引擎中止执行 */
static int qjs_interrupt_handler(JSRuntime *rt, void *opaque)
{
    QJSRuntimeData *data = (QJSRuntimeData *)opaque;
    return now_ms() >= data->deadline_ms;
}

/* 将 JSValue 转成 Java String（正确处理增补平面字符/代理对）。
 * 注意：NewStringUTF 期望"修改版 UTF-8"，直接传入标准 UTF-8 会在
 * 非 BMP 字符（如 emoji）处出错，故统一走 UTF-8 -> UTF-16 转换。 */
static jstring qjs_jsvalue_to_jstring(JNIEnv *env, JSContext *ctx, JSValueConst val)
{
    size_t len = 0;
    const char *utf8 = JS_ToCStringLen2(ctx, &len, val, 0);
    jstring jstr;

    if (!utf8)
        return (*env)->NewStringUTF(env, "");

    if (len == 0) {
        jstr = (*env)->NewStringUTF(env, "");
    } else {
        jchar *utf16 = (jchar *)malloc((len + 1) * sizeof(jchar));
        if (utf16) {
            size_t i = 0, n = 0;
            while (i < len) {
                uint32_t cp;
                unsigned char c = (unsigned char)utf8[i];
                if (c < 0x80) {
                    cp = c;
                    i += 1;
                } else if ((c & 0xE0) == 0xC0) {
                    cp = (uint32_t)(c & 0x1F);
                    cp = (cp << 6) | (utf8[i + 1] & 0x3F);
                    i += 2;
                } else if ((c & 0xF0) == 0xE0) {
                    cp = (uint32_t)(c & 0x0F);
                    cp = (cp << 6) | (utf8[i + 1] & 0x3F);
                    cp = (cp << 6) | (utf8[i + 2] & 0x3F);
                    i += 3;
                } else if ((c & 0xF8) == 0xF0) {
                    cp = (uint32_t)(c & 0x07);
                    cp = (cp << 6) | (utf8[i + 1] & 0x3F);
                    cp = (cp << 6) | (utf8[i + 2] & 0x3F);
                    cp = (cp << 6) | (utf8[i + 3] & 0x3F);
                    i += 4;
                } else {
                    cp = 0xFFFD; /* 非法字节按 U+FFFD 处理 */
                    i += 1;
                }
                if (cp >= 0x10000) {
                    cp -= 0x10000;
                    utf16[n++] = (jchar)(0xD800 + (cp >> 10));
                    utf16[n++] = (jchar)(0xDC00 + (cp & 0x3FF));
                } else {
                    utf16[n++] = (jchar)cp;
                }
            }
            jstr = (*env)->NewString(env, utf16, (jsize)n);
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
                              int argc, JSValueConst *argv)
{
    size_t cap = 256, len = 0;
    char *buf = (char *)js_malloc(ctx, cap);
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
                nbuf = (char *)js_realloc(ctx, buf, cap);
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

static void qjs_register_helpers(JSContext *ctx)
{
    JSValue global, log, console, ret;

    global = JS_GetGlobalObject(ctx);

    log = JS_NewCFunction(ctx, js_console_log, "log", 1);
    console = JS_NewObject(ctx);
    JS_SetPropertyStr(ctx, console, "log", JS_DupValue(ctx, log));
    JS_SetPropertyStr(ctx, global, "console", console);
    JS_SetPropertyStr(ctx, global, "print", log);

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

static jlong native_create_runtime(JNIEnv *env, jclass clazz)
{
    JSRuntime *rt = JS_NewRuntime();
    QJSRuntimeData *data;

    if (!rt)
        return 0;
    data = (QJSRuntimeData *)calloc(1, sizeof(QJSRuntimeData));
    if (!data) {
        JS_FreeRuntime(rt);
        return 0;
    }
    data->deadline_ms = now_ms() + QJS_TIMEOUT_MS;
    JS_SetRuntimeOpaque(rt, data);
    JS_SetMemoryLimit(rt, QJS_MEMORY_LIMIT);
    JS_SetMaxStackSize(rt, QJS_STACK_SIZE);
    JS_SetInterruptHandler(rt, qjs_interrupt_handler, data);
    return (jlong)(intptr_t)rt;
}

static jlong native_create_context(JNIEnv *env, jclass clazz, jlong rt_ptr)
{
    JSRuntime *rt = (JSRuntime *)(intptr_t)rt_ptr;
    JSContext *ctx;

    if (!rt)
        return 0;
    ctx = JS_NewContext(rt);
    if (!ctx)
        return 0;
    qjs_register_helpers(ctx);
    return (jlong)(intptr_t)ctx;
}

static jstring native_evaluate(JNIEnv *env, jclass clazz, jlong ctx_ptr,
                               jstring script, jstring filename, jstring var_name)
{
    JSContext *ctx = (JSContext *)(intptr_t)ctx_ptr;
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
    data = (QJSRuntimeData *)JS_GetRuntimeOpaque(rt);
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

static void native_free_context(JNIEnv *env, jclass clazz, jlong ctx_ptr)
{
    JSContext *ctx = (JSContext *)(intptr_t)ctx_ptr;
    if (ctx)
        JS_FreeContext(ctx);
}

static void native_free_runtime(JNIEnv *env, jclass clazz, jlong rt_ptr)
{
    JSRuntime *rt = (JSRuntime *)(intptr_t)rt_ptr;
    if (rt) {
        QJSRuntimeData *data = (QJSRuntimeData *)JS_GetRuntimeOpaque(rt);
        JS_FreeRuntime(rt);
        free(data);
    }
}

/* ---------------- 注册 ---------------- */

static const JNINativeMethod g_methods[] = {
    { "nativeCreateRuntime", "()J",
      (void *)native_create_runtime },
    { "nativeCreateContext", "(J)J",
      (void *)native_create_context },
    { "nativeEvaluate", "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
      (void *)native_evaluate },
    { "nativeFreeContext", "(J)V",
      (void *)native_free_context },
    { "nativeFreeRuntime", "(J)V",
      (void *)native_free_runtime },
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
    JNIEnv *env = NULL;
    jclass clazz;

    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) != JNI_OK)
        return JNI_ERR;
    clazz = (*env)->FindClass(env, "com/xyrlsz/quickjs/QuickJSEngine");
    if (!clazz)
        return JNI_ERR;
    if ((*env)->RegisterNatives(env, clazz, g_methods,
                                (int)(sizeof(g_methods) / sizeof(g_methods[0]))) != JNI_OK)
        return JNI_ERR;
    return JNI_VERSION_1_6;
}
