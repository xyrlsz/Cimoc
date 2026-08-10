package com.xyrlsz.quickjs;

import org.json.JSONArray;

/**
 * Rhino 兼容上下文，对应 {@code org.mozilla.javascript.Context}。
 * <p>
 * 用法与 Rhino 相同：先 {@link #enter()}，然后
 * {@link #initStandardObjects()} 创建全局作用域，再
 * {@link #evaluateString(Scriptable, String, String, int, Object)} 求值脚本，
 * 最后务必在 finally 中 {@link #exit()}：
 * <pre>
 * Context context = Context.enter();
 * try {
 *     Scriptable scope = context.initStandardObjects();
 *     Object result = context.evaluateString(scope, jsCode, "img", 1, null);
 *     return Context.toString(result);
 * } finally {
 *     Context.exit();
 * }
 * </pre>
 * <p>
 * 底层为 QuickJS：每个线程的 Context 持有独立 {@link QuickJSEngine} 实例，
 * 通过 ThreadLocal 管理（QuickJS 单线程模型，与 Rhino 的线程关联语义一致）。
 * <p>
 * 额外能力（相对 Rhino 常用 API 的补充）：
 * <ul>
 *     <li>{@link #evaluateString} 返回值是 JSON 解码后的 Java 对象
 *         （String / Boolean / Integer / Long / Double / JSONObject / JSONArray / null），
 *         而非 Rhino 的 Scriptable，可直接使用</li>
 *     <li>{@link ScriptableObject#getFunction(String)} + {@link #callFunction}
 *         可调用脚本内定义的函数并传参</li>
 *     <li>{@link Scriptable#put} 可向全局对象注入值（如密钥、配置），
 *         脚本内通过同名全局变量读取</li>
 * </ul>
 */
public class Context implements AutoCloseable {

    /** 当前线程关联的 Context（模拟 Rhino 的 Context.enter/exit）。 */
    private static final ThreadLocal<Context> sCurrent = new ThreadLocal<>();

    private final QuickJSEngine engine;
    private ScriptableObject scope;

    private Context() {
        this.engine = new QuickJSEngine();
    }

    /** 进入上下文：当前线程无 Context 时创建新的 QuickJS 引擎实例。 */
    public static Context enter() {
        Context ctx = sCurrent.get();
        if (ctx == null) {
            ctx = new Context();
            sCurrent.set(ctx);
        }
        return ctx;
    }

    /** 返回当前线程的 Context；未 {@link #enter()} 时返回 null。 */
    public static Context getCurrentContext() {
        return sCurrent.get();
    }

    /** 退出并释放当前线程的 Context（底层引擎一并释放）。 */
    public static void exit() {
        Context ctx = sCurrent.get();
        if (ctx != null) {
            sCurrent.remove();
            ctx.close();
        }
    }

    /** 返回底层引擎（高级用法）。 */
    public QuickJSEngine getEngine() {
        return engine;
    }

    /**
     * 创建全局作用域（含标准对象）。
     *
     * @return 全局对象
     */
    public ScriptableObject initStandardObjects() {
        if (scope == null) {
            scope = new ScriptableObject(engine);
        }
        return scope;
    }

    /**
     * 求值脚本，返回表达式结果（JSON 解码后的 Java 对象：
     * String / Boolean / Integer / Long / Double / JSONObject / JSONArray / null）。
     *
     * @param scope          全局作用域（由 {@link #initStandardObjects()} 创建）
     * @param source         JS 源码
     * @param sourceName     来源名（仅用于日志/错误信息，可为 null）
     * @param lineno         起始行号（仅用于日志/错误信息）
     * @param securityDomain 安全域（本实现忽略，传 null 即可）
     * @return 求值结果
     */
    public Object evaluateString(Scriptable scope, String source,
                                 String sourceName, int lineno, Object securityDomain) {
        if (source == null) {
            throw new IllegalArgumentException("source is null");
        }
        QuickJSEngine eng = engine;
        if (scope instanceof ScriptableObject) {
            eng = ((ScriptableObject) scope).getEngine();
        }
        return JsonCodec.decode(eng.evaluateJson(source));
    }

    /** 便捷重载：使用当前上下文默认作用域求值。 */
    public Object evaluateString(String source) {
        return evaluateString(initStandardObjects(), source, "<cmd>", 1, null);
    }

    /**
     * 调用 JS 函数（对应 Rhino 的 {@code callFunctionWithContinuations}）。
     *
     * @param scope    作用域
     * @param function 函数对象（由 {@link ScriptableObject#getFunction(String)} 获取）
     * @param args     参数数组
     * @return 返回值（JSON 解码后的 Java 对象）
     */
    public Object callFunctionWithContinuations(Scriptable scope, Function function,
                                                Object[] args) {
        return function.call(this, scope, scope, args);
    }

    /**
     * 调用 JS 函数（对应 Rhino 的 {@code Context.callFunction}）。
     *
     * @param function 函数对象
     * @param scope    作用域
     * @param thisObj  this 指向
     * @param args     参数数组
     * @return 返回值
     */
    public Object callFunction(Function function, Scriptable scope,
                               Scriptable thisObj, Object[] args) {
        return function.call(this, scope, thisObj, args);
    }

    /**
     * 设置优化级别。QuickJS 总是原生执行，本方法为 no-op，
     * 与 Rhino 传 -1 走解释模式等价。
     */
    public void setOptimizationLevel(int level) {
        // no-op
    }

    /** 返回优化级别（恒为 -1，解释模式）。 */
    public int getOptimizationLevel() {
        return -1;
    }

    /** 设置语言版本（QuickJS 为现代 ES，本方法为 no-op）。 */
    public void setLanguageVersion(int version) {
        // no-op
    }

    /**
     * 把 Java 值转成 JS 值（对应 Rhino 的 {@code javaToJS}）。
     * 在 JSON 桥接模型下，可序列化值原样返回（传入脚本时由
     * {@link Scriptable#put} 编码）；复杂对象抛 {@link RhinoException}。
     */
    public static Object javaToJS(Object value, Scriptable scope) {
        return JsonCodec.encodeValue(value);
    }

    /**
     * 把 JS 值转成 Java 值（对应 Rhino 的 {@code jsToJava}）。
     * 桥接模型下值本身已是 Java 类型，按目标类型做基本转换。
     */
    public static Object jsToJava(Object value, Class<?> type) {
        if (value == null) {
            return null;
        }
        if (type == null || type == Object.class) {
            return value;
        }
        if (type == String.class) {
            return toString(value);
        }
        if (type == Boolean.class || type == boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.valueOf(toString(value));
        }
        if (type == Integer.class || type == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.valueOf(toString(value));
        }
        if (type == Long.class || type == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.valueOf(toString(value));
        }
        if (type == Double.class || type == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.valueOf(toString(value));
        }
        if (type == Float.class || type == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.valueOf(toString(value));
        }
        return value;
    }

    /**
     * 对应 Rhino 的 {@code Context.toString}：把结果值转为字符串。
     * 数组按 Array.prototype.toString 语义以逗号连接；整数值的 Double 不显示小数。
     */
    public static String toString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Double || value instanceof Float) {
            double d = ((Number) value).doubleValue();
            if (d == Math.rint(d) && !Double.isInfinite(d) && !Double.isNaN(d)) {
                return String.valueOf((long) d);
            }
            return String.valueOf(value);
        }
        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(toString(arr.opt(i)));
            }
            return sb.toString();
        }
        return String.valueOf(value);
    }

    /** 对应 Rhino 的 {@code Context.setCachingEnabled}（本实现为 no-op）。 */
    public static void setCachingEnabled(boolean enabled) {
        // no-op
    }

    /** 返回 undefined 占位值。 */
    public static Undefined getUndefinedValue() {
        return Undefined.instance;
    }

    @Override
    public void close() {
        engine.close();
        if (sCurrent.get() == this) {
            sCurrent.remove();
        }
    }
}
