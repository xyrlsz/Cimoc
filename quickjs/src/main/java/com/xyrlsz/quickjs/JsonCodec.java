package com.xyrlsz.quickjs;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * JSON 桥接编解码（内部使用）。
 * <p>
 * JS 值在 QuickJS（C 层）与 Java 之间以 JSON 文本传输。
 * 支持类型：String / Boolean / Integer / Long / Double / Float /
 * JSONObject / JSONArray / null。
 */
final class JsonCodec {

    private JsonCodec() {
    }

    /** 把参数数组编码为 JSON 数组文本。 */
    static String encodeArgs(Object[] args) {
        JSONArray arr = new JSONArray();
        if (args != null) {
            for (Object arg : args) {
                arr.put(encodeValue(arg));
            }
        }
        return arr.toString();
    }

    /** 把单个 Java 值编码为可放入 JSON 的结构（null 原样返回）。 */
    static Object encodeValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean
                || value instanceof Integer || value instanceof Long
                || value instanceof Double || value instanceof Float
                || value instanceof JSONObject || value instanceof JSONArray) {
            return value;
        }
        throw new RhinoException("Unsupported value type: "
                + value.getClass().getName());
    }

    /** 把单个 Java 值编码为 JSON 文本（供 setGlobal 等使用）。 */
    static String encode(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return JSONObject.quote((String) value);
        }
        if (value instanceof Boolean || value instanceof Integer
                || value instanceof Long || value instanceof Double
                || value instanceof Float) {
            return String.valueOf(value);
        }
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return value.toString();
        }
        throw new RhinoException("Unsupported value type: "
                + value.getClass().getName());
    }

    /** 解析 JSON 文本为 Java 对象；null / 非法输入 → null。 */
    static Object decode(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return normalize(new JSONTokener(json).nextValue());
        } catch (Exception e) {
            return null;
        }
    }

    private static Object normalize(Object value) throws org.json.JSONException {
        if (value == null || value == JSONObject.NULL) {
            return null;
        }
        if (value instanceof JSONArray) {
            JSONArray arr = (JSONArray) value;
            for (int i = 0; i < arr.length(); i++) {
                arr.put(i, normalize(arr.opt(i)));
            }
            return arr;
        }
        if (value instanceof JSONObject) {
            JSONObject obj = (JSONObject) value;
            for (java.util.Iterator<String> it = obj.keys(); it.hasNext(); ) {
                String key = it.next();
                obj.put(key, normalize(obj.opt(key)));
            }
            return obj;
        }
        return value;
    }
}
