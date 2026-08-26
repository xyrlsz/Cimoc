package com.xyrlsz.xcimocob.source.js;

import android.content.Context;
import android.content.SharedPreferences;

import com.xyrlsz.quickjs.QuickJSEngine;
import com.xyrlsz.quickjs.SourceCodec;
import com.xyrlsz.xcimocob.App;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public final class JsHost implements QuickJSEngine.HostBridge {

    public static final JsHost INSTANCE = new JsHost();

    private static final String PREFS = "js_source";

    /** DOM 节点注册表：自增 id → jsoup Element（每次解析会话创建，短生命周期） */
    private final ConcurrentHashMap<Integer, Element> mNodeMap = new ConcurrentHashMap<>();
    private final AtomicInteger mNodeSeq = new AtomicInteger(1);

    /** 跨调用内存态（state）：key（已含 type 前缀）→ JSON 片段 */
    private final ConcurrentHashMap<String, String> mState = new ConcurrentHashMap<>();

    private JsHost() {
    }

    @Override
    public String onHostCall(String name, String argsJson) {
        try {
            return switch (name) {
                case "dom" -> handleDom(argsJson);
                case "md5" -> quoteStr(SourceCodec.md5(arg(argsJson, "data")));
                case "lz64" -> quoteStr(SourceCodec.lz64Decrypt(arg(argsJson, "data")));
                case "base64" -> handleBase64(argsJson);
                case "aes_cbc" -> handleAesCbc(argsJson);
                case "state" -> handleState(argsJson);
                case "setting" -> handleSetting(argsJson);
                case "login" -> handleLogin(argsJson);
                case "fetch" -> handleFetch(argsJson);
                case "log" -> {
                    android.util.Log.i("JsSource", "[" + arg(argsJson, "type") + "] "
                            + arg(argsJson, "data"));
                    yield "null";
                }
                case "urlencode" -> quoteStr(SourceCodec.urlEncode(arg(argsJson, "data")));
                case "urldecode" -> quoteStr(SourceCodec.urlDecode(arg(argsJson, "data")));
                default -> "null";
            };
        } catch (Throwable t) {
            return "null";
        }
    }

    /* ---------------- DOM ---------------- */

    private String handleDom(String argsJson) throws JSONException {
        JSONObject o = new JSONObject(argsJson);
        String op = o.optString("op");
        if ("create".equals(op)) {
            Element root = Jsoup.parse(o.optString("html")).body();
            int id = mNodeSeq.incrementAndGet();
            mNodeMap.put(id, root);
            trimNodeMap();
            return new JSONObject().put("id", id).toString();
        }
        int id = o.optInt("id", -1);
        Element base = mNodeMap.get(id);
        if (base == null) {
            return "null";
        }
        String sel = o.isNull("sel") ? null : o.optString("sel");
        switch (op) {
            case "select": {
                JSONArray out = new JSONArray();
                assert sel != null;
                for (Element child : base.select(sel)) {
                    int cid = mNodeSeq.incrementAndGet();
                    mNodeMap.put(cid, child);
                    out.put(cid);
                }
                trimNodeMap();
                return out.toString();
            }
            case "text": {
                Element target = resolveTarget(base, sel);
                return quoteStr(target == null ? null : target.text().trim());
            }
            case "attr": {
                Element target = resolveTarget(base, sel);
                String attr = o.optString("attr");
                return quoteStr(target == null ? null : target.attr(attr).trim());
            }
            case "href":
                return quoteStr(attrOf(base, sel, "href"));
            case "src":
                return quoteStr(attrOf(base, sel, "src"));
            default:
                return "null";
        }
    }

    private Element resolveTarget(Element el, String sel) {
        if (sel == null) {
            return el;
        }
        return el.select(sel).first();
    }

    private String attrOf(Element el, String sel, String attr) {
        Element target = resolveTarget(el, sel);
        if (target == null) return null;
        String v = target.attr(attr).trim();
        return v.isEmpty() ? null : v;
    }

    /** 防止节点注册表无限增长：超过阈值时整体清空（旧的短生命周期节点即可丢弃）。 */
    private void trimNodeMap() {
        if (mNodeMap.size() > 5000) {
            mNodeMap.clear();
        }
    }

    /* ---------------- 解密 ---------------- */

    private String handleBase64(String argsJson) throws JSONException {
        JSONObject o = new JSONObject(argsJson);
        String op = o.optString("op");
        String data = o.optString("data");
        return switch (op) {
            case "encode" -> quoteStr(SourceCodec.base64Encode(data));
            case "decode" -> quoteStr(SourceCodec.base64Decode(data));
            case "url" -> quoteStr(SourceCodec.base64UrlDecode(data));
            default -> "null";
        };
    }

    private String handleAesCbc(String argsJson) throws JSONException {
        JSONObject o = new JSONObject(argsJson);
        String value = o.optString("value");
        String key = o.optString("key");
        boolean ivPrefix = o.optBoolean("ivPrefix", false);
        String result = ivPrefix
                ? SourceCodec.aesCbcDecryptWithIvPrefix(value, key)
                : SourceCodec.aesCbcDecrypt(value, key, o.optString("iv"));
        return quoteStr(result);
    }

    /* ---------------- 状态 ---------------- */

    private String handleState(String argsJson) throws JSONException {
        JSONObject o = new JSONObject(argsJson);
        String op = o.optString("op");
        String key = o.optString("key");
        if ("set".equals(op)) {
            Object v = o.opt("value");
            mState.put(key, v == null ? "null" : v.toString());
            return "null";
        }
        String v = mState.get(key);
        return v == null ? "null" : v;
    }

    /* ---------------- 设置 / 登录（持久化，按 type 隔离） ---------------- */

    private SharedPreferences prefs() {
        return App.getAppContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private String handleSetting(String argsJson) throws JSONException {
        JSONObject o = new JSONObject(argsJson);
        String op = o.optString("op");
        int type = o.optInt("type", -1);
        String key = o.optString("key");
        String fullKey = "setting_" + type + "_" + key;
        SharedPreferences sp = prefs();
        if ("set".equals(op)) {
            Object v = o.opt("value");
            sp.edit().putString(fullKey, v == null ? null : v.toString()).apply();
            return "null";
        }
        String v = sp.getString(fullKey, null);
        return v == null ? "null" : quoteStr(v);
    }

    private String handleLogin(String argsJson) throws JSONException {
        JSONObject o = new JSONObject(argsJson);
        String op = o.optString("op");
        int type = o.optInt("type", -1);
        String fullKey = "login_" + type;
        SharedPreferences sp = prefs();
        if ("set".equals(op)) {
            Object v = o.opt("value");
            sp.edit().putString(fullKey, v == null ? null : v.toString()).apply();
            return "null";
        }
        if ("clear".equals(op)) {
            sp.edit().remove(fullKey).apply();
            return "null";
        }
        String v = sp.getString(fullKey, null);
        // 必须用 quoteStr 加引号：SDK 的 _call() 会对返回值做 JSON.parse，未加引号的
        // 原始 JSON 会被解析成 JS 对象，导致 getLogin() 拿到对象而非字符串，
        // 后续 loginToken()/getLoginState() 的 JSON.parse(对象) 会抛错 → 登录态读不到。
        return v == null ? "null" : quoteStr(v);
    }

    /* ---------------- 网络（登录等场景用） ---------------- */

    /** 同步 HTTP 请求，返回 {status, headers, setCookie, body}。 */
    private String handleFetch(String argsJson) throws Exception {
        JSONObject o = new JSONObject(argsJson);
        String url = o.optString("url");
        if (url.isEmpty()) return "null";
        String method = o.optString("method", "GET").toUpperCase(Locale.ROOT);
        Request.Builder b = new Request.Builder().url(url);
        JSONObject headers = o.optJSONObject("headers");
        if (headers != null) {
            Iterator<String> it = headers.keys();
            while (it.hasNext()) {
                String k = it.next();
                String v = headers.optString(k);
                if (k != null) {
                    try {
                        b.header(k, v);
                    } catch (Exception ignore) {
                    }
                }
            }
        }
        String body = o.isNull("body") ? null : o.optString("body");
        if ("POST".equals(method) || "PUT".equals(method)) {
            String ctype = o.optString("contentType", "application/x-www-form-urlencoded");
            RequestBody rb = RequestBody.create(body == null ? "" : body, MediaType.parse(ctype));
            b.method(method, rb);
        }
        try (Response resp = App.getHttpClient().newCall(b.build()).execute()) {
            String respBody = resp.body().string();
            JSONObject out = new JSONObject();
            out.put("status", resp.code());
            JSONObject respHeaders = new JSONObject();
            for (String k : resp.headers().names()) {
                respHeaders.put(k, resp.header(k));
            }
            out.put("headers", respHeaders);
            JSONArray setCookie = new JSONArray();
            for (String c : resp.headers("Set-Cookie")) {
                setCookie.put(c);
            }
            out.put("setCookie", setCookie);
            out.put("body", respBody);
            return out.toString();
        }
    }

    /* ---------------- 对外只读访问（供 UI / 解析器直接读取） ---------------- */

    /** 读取某源已保存的登录态 JSON（无则返回 null）。 */
    public String getLogin(int type) {
        return prefs().getString("login_" + type, null);
    }

    /** 写入某源登录态 JSON。 */
    public void setLogin(int type, String json) {
        prefs().edit().putString("login_" + type, json).apply();
    }

    /** 清除某源登录态。 */
    public void clearLogin(int type) {
        prefs().edit().remove("login_" + type).apply();
    }

    /** 读取某源某项设置（无则返回 null）。 */
    public String getSetting(int type, String key) {
        return prefs().getString("setting_" + type + "_" + key, null);
    }

    /** 写入某源某项设置。 */
    public void setSetting(int type, String key, String value) {
        prefs().edit().putString("setting_" + type + "_" + key, value).apply();
    }

    /* ---------------- 工具 ---------------- */

    private String arg(String argsJson, String name) {
        try {
            return new JSONObject(argsJson).optString(name);
        } catch (JSONException e) {
            return null;
        }
    }

    /** 把字符串以 JSON 字符串形式返回（null → "null"）。 */
    private String quoteStr(String s) {
        return s == null ? "null" : JSONObject.quote(s);
    }
}
