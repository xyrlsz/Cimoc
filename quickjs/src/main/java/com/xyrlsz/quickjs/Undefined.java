package com.xyrlsz.quickjs;

/**
 * 对应 {@code org.mozilla.javascript.Undefined}：表示 JS 的 undefined 值（单例）。
 * <p>
 * 注意：JSON 桥接层默认把 undefined 归一为 null（与 Rhino 的
 * {@code Undefined.instance} 行为不同），需要显式区分时可使用
 * {@link #instance} 作为 Java 侧占位值。
 */
public final class Undefined {

    public static final Undefined instance = new Undefined();

    private Undefined() {
    }

    @Override
    public String toString() {
        return "undefined";
    }
}
