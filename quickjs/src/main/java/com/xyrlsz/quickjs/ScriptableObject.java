package com.xyrlsz.quickjs;

import org.json.JSONArray;

/**
 * 对应 {@code org.mozilla.javascript.ScriptableObject}：
 * 全局对象（Global Scope）的实现，包装 {@link QuickJSEngine} 的全局对象。
 * <p>
 * 属性读写直接映射到引擎的全局变量（通过 JSON 桥接），
 * 函数属性通过 {@link #getFunction(String)} 包装为 {@link Function} 后可调用。
 */
public class ScriptableObject implements Scriptable {

    protected final QuickJSEngine engine;
    private Scriptable parentScope;
    private Scriptable prototype;

    public ScriptableObject(QuickJSEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("engine is null");
        }
        this.engine = engine;
    }

    /** 返回底层引擎（高级用法）。 */
    public QuickJSEngine getEngine() {
        return engine;
    }

    @Override
    public String getClassName() {
        return "Global";
    }

    @Override
    public Object get(String name, Scriptable start) {
        if (name == null) {
            return null;
        }
        return JsonCodec.decode(engine.getGlobalJson(name));
    }

    @Override
    public Object get(String name) {
        return get(name, this);
    }

    @Override
    public boolean has(String name, Scriptable start) {
        return name != null && engine.hasGlobal(name);
    }

    @Override
    public void put(String name, Scriptable start, Object value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (!engine.setGlobal(name, JsonCodec.encode(value))) {
            throw new JavaScriptException("Failed to set global '" + name + "'");
        }
    }

    @Override
    public void put(String name, Object value) {
        put(name, this, value);
    }

    @Override
    public Object[] getIds() {
        String json = engine.evaluateJson("Object.keys(globalThis)");
        Object decoded = JsonCodec.decode(json);
        if (decoded instanceof JSONArray) {
            JSONArray arr = (JSONArray) decoded;
            Object[] ids = new Object[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                ids[i] = arr.opt(i);
            }
            return ids;
        }
        return new Object[0];
    }

    @Override
    public Object getDefaultValue(Class<?> typeHint) {
        return toString();
    }

    @Override
    public Scriptable getParentScope() {
        return parentScope;
    }

    @Override
    public void setParentScope(Scriptable parent) {
        this.parentScope = parent;
    }

    @Override
    public Scriptable getPrototype() {
        return prototype;
    }

    @Override
    public void setPrototype(Scriptable prototype) {
        this.prototype = prototype;
    }

    @Override
    public String toString() {
        return "[object " + getClassName() + "]";
    }

    /**
     * 把全局对象上的函数包装为 {@link Function}（可调用）。
     *
     * @param name 全局函数名
     * @return 函数包装对象
     * @throws JavaScriptException 函数不存在时
     */
    public Function getFunction(String name) {
        if (name == null || !engine.hasFunction(name)) {
            throw new JavaScriptException("Function '" + name + "' is not defined");
        }
        return new BaseFunction(this, name);
    }

    /** 对应 Rhino 的 {@code ScriptableObject.getProperty(Scriptable, String)}。 */
    public static Object getProperty(Scriptable obj, String name) {
        return obj.get(name, obj);
    }

    /** 对应 Rhino 的 {@code ScriptableObject.putProperty(Scriptable, String, Object)}。 */
    public static void putProperty(Scriptable obj, String name, Object value) {
        obj.put(name, obj, value);
    }
}
