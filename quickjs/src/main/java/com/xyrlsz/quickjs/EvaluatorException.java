package com.xyrlsz.quickjs;

/**
 * 对应 {@code org.mozilla.javascript.EvaluatorException}：
 * 脚本编译/求值阶段的异常。
 */
public class EvaluatorException extends RhinoException {

    public EvaluatorException(String message) {
        super(message);
    }

    public EvaluatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
