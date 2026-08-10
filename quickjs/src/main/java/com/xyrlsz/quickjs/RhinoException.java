package com.xyrlsz.quickjs;

/**
 * Rhino 兼容层的通用异常基类，对应 {@code org.mozilla.javascript.RhinoException}。
 */
public class RhinoException extends RuntimeException {

    public RhinoException() {
        super();
    }

    public RhinoException(String message) {
        super(message);
    }

    public RhinoException(String message, Throwable cause) {
        super(message, cause);
    }

    public RhinoException(Throwable cause) {
        super(cause);
    }

    /** 返回错误的详细描述（等价于 getMessage）。 */
    public String details() {
        return getMessage();
    }
}
