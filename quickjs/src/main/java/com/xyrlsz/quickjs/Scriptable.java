package com.xyrlsz.quickjs;

/**
 * 对应 {@code org.mozilla.javascript.Scriptable}：
 * 所有可被 JS 脚本访问的对象都要实现的接口。
 * <p>
 * 本兼容层面向"脚本求值 + 全局变量读写 + 全局函数调用"的常用场景，
 * 当前实现类为 {@link ScriptableObject}（全局对象）。
 */
public interface Scriptable {

    /** 返回对象的类名（如 "Global"、"Function"）。 */
    String getClassName();

    /** 读取属性值，start 为查找起点对象（通常传 this）。 */
    Object get(String name, Scriptable start);

    /** 便捷：读取属性值。 */
    Object get(String name);

    /** 判断是否存在属性。 */
    boolean has(String name, Scriptable start);

    /** 写入属性。 */
    void put(String name, Scriptable start, Object value);

    /** 便捷：写入属性。 */
    void put(String name, Object value);

    /** 返回所有属性名（数组元素为 String）。 */
    Object[] getIds();

    /** 返回对象的默认转换值。 */
    Object getDefaultValue(Class<?> typeHint);

    /** 返回父作用域（本实现恒为 null）。 */
    Scriptable getParentScope();

    /** 设置父作用域（本实现仅记录，不参与属性查找）。 */
    void setParentScope(Scriptable parent);

    /** 返回原型对象（本实现恒为 null）。 */
    Scriptable getPrototype();

    /** 设置原型对象（本实现仅记录，不参与属性查找）。 */
    void setPrototype(Scriptable prototype);
}
