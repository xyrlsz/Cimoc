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
public final class QuickJSEngine {

    static {
        System.loadLibrary("quickjs_jni");
    }

    private static native long nativeCreateRuntime();

    private static native long nativeCreateContext(long runtimePtr);

    private static native String nativeEvaluate(long contextPtr, String script,
                                                String filename, String varName);

    private static native void nativeFreeContext(long contextPtr);

    private static native void nativeFreeRuntime(long runtimePtr);

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
     * 释放底层资源。关闭后不能再调用 {@link #evaluate(String)}。
     */
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
