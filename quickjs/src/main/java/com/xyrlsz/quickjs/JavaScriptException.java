package com.xyrlsz.quickjs;

/**
 * 对应 {@code org.mozilla.javascript.JavaScriptException}：
 * JS 脚本抛出的异常。
 */
public class JavaScriptException extends RhinoException {

    private final Object value;

    public JavaScriptException(Object value, String sourceName, int lineNumber) {
        super(buildMessage(value, sourceName, lineNumber));
        this.value = value;
    }

    public JavaScriptException(Object value) {
        this(value, null, 0);
    }

    /** 返回 JS 抛出的值（JSON 解码后的 Java 对象，可能是 Error 对象的字符串表示）。 */
    public Object getValue() {
        return value;
    }

    private static String buildMessage(Object value, String sourceName, int lineNumber) {
        StringBuilder sb = new StringBuilder("JavaScript exception");
        if (sourceName != null && !sourceName.isEmpty()) {
            sb.append(" in ").append(sourceName);
        }
        if (lineNumber > 0) {
            sb.append(" at line ").append(lineNumber);
        }
        sb.append(": ").append(String.valueOf(value));
        return sb.toString();
    }
}
