package com.xyrlsz.quickjs;

/**
 * QuickJS 引擎的 JNI 封装（独立 Android 库）。
 * <p>
 * 底层引擎为 {@code https://github.com/bellard/quickjs}（MIT License），
 * 通过 git 子模块固定在 2026-06-04 发布版提交。
 * 每个实例拥有独立的 JSRuntime + JSContext（堆、全局对象互相隔离）。
 * QuickJS 的 Runtime 非线程安全：同一个实例不要跨线程使用；
 * 需要并发时请为每个线程创建独立实例。
 * <p>
 * 安全特性（由 C 层保证）：
 * <ul>
 *     <li>未注册 std/os 模块，脚本无法访问文件系统、进程等宿主资源</li>
 *     <li>内存上限 32MB、JS 调用栈上限 512KB、单次执行超时 10s</li>
 * </ul>
 */
public final class QuickJSEngine implements AutoCloseable {

    static {
        System.loadLibrary("quickjs_jni");
    }

    private static native long nativeCreateRuntime();

    private static native long nativeCreateContext(long runtimePtr);

    private static native String nativeEvaluate(long contextPtr, String script,
                                                String filename, String varName);

    private static native String nativeCallFunction(long contextPtr, String name,
                                                    String argsJson, long timeoutMs);

    private static native boolean nativeHasGlobalFunction(long contextPtr, String name);

    private static native String nativeGetGlobalJson(long contextPtr, String name);

    private static native void nativeFreeContext(long contextPtr);

    private static native void nativeFreeRuntime(long runtimePtr);

    /**
     * JS 脚本通过全局函数 {@code hostCall(name, argsJson)} 回调到 Java 的宿主桥。
     * <p>
     * 由于 JNI 回调是静态分发，桥接实现需为无状态（每次调用所需的全部上下文都
     * 通过参数传递），多个引擎实例并发调用是安全的。
     */
    public interface HostBridge {
        /**
         * 处理一次 JS 宿主调用。
         *
         * @param name     宿主方法名
         * @param argsJson 参数数组的 JSON 字符串
         * @return 结果 JSON 字符串；出错时可返回 null 或
         * {@code {"__error__":"..."}}，两者都会被转成 JS 异常
         */
        String onHostCall(String name, String argsJson);
    }

    private static volatile HostBridge sHostBridge;

    /**
     * 注册全局宿主桥（通常注册一次即可，实现需无状态）。
     */
    public static void setHostBridge(HostBridge bridge) {
        sHostBridge = bridge;
    }

    /**
     * 供 JNI 调用的静态宿主分发入口。
     */
    public static String onHostCall(String name, String argsJson) {
        HostBridge bridge = sHostBridge;
        if (bridge == null) {
            return "{\"__error__\":\"host bridge not registered\"}";
        }
        try {
            return bridge.onHostCall(name, argsJson);
        } catch (Throwable t) {
            String msg = t.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = t.getClass().getSimpleName();
            }
            String escaped = msg.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r");
            return "{\"__error__\":\"" + escaped + "\"}";
        }
    }

    private long runtimePtr;
    private long contextPtr;

    public QuickJSEngine() {
        runtimePtr = nativeCreateRuntime();
        if (runtimePtr == 0) {
            throw new RuntimeException("Failed to create QuickJS runtime");
        }
        contextPtr = nativeCreateContext(runtimePtr);
        if (contextPtr == 0) {
            nativeFreeRuntime(runtimePtr);
            runtimePtr = 0;
            throw new RuntimeException("Failed to create QuickJS context");
        }
    }

    /**
     * 执行一段 JS 脚本，返回表达式结果转成的字符串。
     * 执行出错（含超时）时返回空字符串。
     *
     * @param script JS 代码
     * @return 结果字符串，出错时为 ""
     */
    public String evaluate(String script) {
        return evaluate(script, null);
    }

    /**
     * 执行一段 JS 脚本，返回指定全局变量的字符串值。
     *
     * @param script  JS 代码
     * @param varName 需要读取的全局变量名，为 null 时返回表达式结果
     * @return 结果字符串，出错时为 ""
     */
    public String evaluate(String script, String varName) {
        if (contextPtr == 0) {
            throw new IllegalStateException("QuickJSEngine already closed");
        }
        return nativeEvaluate(contextPtr, script, "<eval>", varName);
    }

    /**
     * 调用已定义在全局作用域的 JS 函数。
     *
     * @param name     函数名
     * @param jsonArgs 参数数组的 JSON 字符串，如 {@code "[\"a\",\"b\"]"}
     * @return 返回值经 {@code JSON.stringify} 后的字符串；函数不存在时返回
     * {@code "null"}；执行出错时抛出 {@link RuntimeException}（携带 JS 错误信息）
     */
    public String callFunction(String name, String jsonArgs) {
        return callFunction(name, jsonArgs, 0);
    }

    /**
     * 调用已定义在全局作用域的 JS 函数，可自定义超时。
     *
     * @param name     函数名
     * @param jsonArgs 参数数组的 JSON 字符串
     * @param timeoutMs 本次调用的超时毫秒数，&lt;=0 时使用引擎默认 10s
     * @return 返回值经 {@code JSON.stringify} 后的字符串；函数不存在时返回
     * {@code "null"}；执行出错时抛出 {@link RuntimeException}
     */
    public String callFunction(String name, String jsonArgs, long timeoutMs) {
        if (contextPtr == 0) {
            throw new IllegalStateException("QuickJSEngine already closed");
        }
        if (jsonArgs == null) {
            jsonArgs = "[]";
        }
        return nativeCallFunction(contextPtr, name, jsonArgs, timeoutMs);
    }

    /**
     * 检测全局作用域中是否存在指定函数。
     */
    public boolean hasFunction(String name) {
        if (contextPtr == 0) {
            throw new IllegalStateException("QuickJSEngine already closed");
        }
        return nativeHasGlobalFunction(contextPtr, name);
    }

    /**
     * 读取全局变量的 JSON 序列化值。变量缺失或出错时返回 {@code "null"}。
     * 用于读取源脚本的元数据对象（如 {@code SOURCE}）。
     */
    public String getGlobalJson(String name) {
        if (contextPtr == 0) {
            throw new IllegalStateException("QuickJSEngine already closed");
        }
        return nativeGetGlobalJson(contextPtr, name);
    }

    /**
     * 释放底层资源。关闭后不能再调用 {@link #evaluate(String)}。
     */
    @Override
    public void close() {
        if (contextPtr != 0) {
            nativeFreeContext(contextPtr);
            contextPtr = 0;
        }
        if (runtimePtr != 0) {
            nativeFreeRuntime(runtimePtr);
            runtimePtr = 0;
        }
    }
}
