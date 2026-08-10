package com.xyrlsz.quickjs;

/**
 * 对应 {@code org.mozilla.javascript.BaseFunction}：
 * 包装全局对象上一个 JS 函数的可调用对象。
 */
public class BaseFunction extends ScriptableObject implements Function {

    private final String name;

    public BaseFunction(ScriptableObject scope, String name) {
        super(scope.getEngine());
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    @Override
    public Object call(Context context, Scriptable scope, Scriptable thisObj, Object[] args) {
        String argsJson = JsonCodec.encodeArgs(args);
        String json = engine.callFunction(name, argsJson);
        return JsonCodec.decode(json);
    }

    @Override
    public String getFunctionName() {
        return name;
    }

    @Override
    public String getClassName() {
        return "Function";
    }

    @Override
    public String toString() {
        return "function " + name + "() { [native code] }";
    }
}
