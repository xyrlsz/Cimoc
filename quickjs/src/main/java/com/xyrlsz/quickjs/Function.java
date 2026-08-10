package com.xyrlsz.quickjs;

/**
 * 对应 {@code org.mozilla.javascript.Function}：
 * JS 函数对象，可通过 {@link #call} 调用。
 */
public interface Function extends Scriptable {

    /**
     * 调用函数。
     *
     * @param context 当前上下文
     * @param scope   作用域
     * @param thisObj this 指向（本实现忽略，始终使用全局对象）
     * @param args    参数数组（可 JSON 桥接的 Java 值）
     * @return 返回值（JSON 解码后的 Java 对象）
     */
    Object call(Context context, Scriptable scope, Scriptable thisObj, Object[] args);

    /** 返回函数名。 */
    String getFunctionName();
}
