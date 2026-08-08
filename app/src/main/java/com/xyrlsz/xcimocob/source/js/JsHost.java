package com.xyrlsz.xcimocob.source.js;

import android.util.Base64;
import android.util.Log;

import com.xyrlsz.quickjs.QuickJSEngine;
import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.utils.STConvertUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * JS 源运行时宿主：把 QuickJS 脚本里 {@code hostCall(...)} 的请求分发到
 * Android 能力（网络请求、DOM 解析、摘要/编码工具）。
 *
 * <p>实现为无状态单例（每次调用所需上下文全部经参数传入），并注册为
 * {@link QuickJSEngine} 的全局宿主桥。所有返回值均序列化为 JSON 字符串。
 */
public final class JsHost implements QuickJSEngine.HostBridge {

    private static final String TAG = "JsHost";
    private static final Pattern CHARSET_PATTERN = Pattern.compile("charset=([\\w\\-]+)");
    /**
     * 脚本跨调用状态存储（按 "type:key" 索引，进程内、线程安全）。
     * 用于 JS 引擎无状态场景下传递 _mid 等少量中间值。
     */
    private static final java.util.concurrent.ConcurrentHashMap<String, String> STATE =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static final JsHost INSTANCE = new JsHost();

    static {
        QuickJSEngine.setHostBridge(INSTANCE);
    }

    private JsHost() {
    }

    @Override
    public String onHostCall(String name, String argsJson) {
        try {
            return dispatch(name, argsJson);
        } catch (Exception e) {
            Log.e(TAG, "host call '" + name + "' failed", e);
            String msg = e.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = e.getClass().getSimpleName();
            }
            String escaped = msg.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r");
            return "{\"__error__\":\"" + escaped + "\"}";
        }
    }

    private String dispatch(String name, String argsJson) throws Exception {
        JSONArray args = new JSONArray(argsJson == null ? "[]" : argsJson);
        switch (name) {
            case "fetch": {
                String url = args.optString(0);
                String method = args.optString(1, "GET");
                JSONObject headers = args.optJSONObject(2);
                Object body = args.isNull(3) ? null : args.opt(3);
                return fetch(url, method, headers, body).toString();
            }
            case "dom_selectAll":
                return domSelectAll(args.optString(0), args.optString(1)).toString();
            case "dom_text":
                return jsonString(domText(args.optString(0), args.optString(1)));
            case "dom_attr":
                return jsonString(domAttr(args.optString(0), args.optString(1), args.optString(2)));
            case "md5":
                return jsonString(md5(args.optString(0)));
            case "urlencode":
                return jsonString(URLEncoder.encode(args.optString(0), "utf-8"));
            case "urldecode":
                return jsonString(URLDecoder.decode(args.optString(0), "utf-8"));
            case "base64encode":
                return jsonString(Base64.encodeToString(args.optString(0).getBytes(StandardCharsets.UTF_8),
                        Base64.NO_WRAP));
            case "base64decode":
                return jsonString(new String(Base64.decode(args.optString(0), Base64.DEFAULT),
                        StandardCharsets.UTF_8));
            case "base64url_decode":
                return jsonString(new String(Base64.decode(args.optString(0), Base64.URL_SAFE),
                        StandardCharsets.UTF_8));
            case "aes_cbc":
                return jsonString(aesCbcDecrypt(args.optString(0), args.optString(1), args.optString(2)));
            case "aes_cbc_raw":
                return jsonString(aesCbcDecryptWithIvPrefix(args.optString(0), args.optString(1)));
            case "t2s":
                return jsonString(STConvertUtils.T2S(args.optString(0)));
            case "set_state": {
                // 按源 type + key 持久化一个短字符串（进程内）
                String type = args.optString(0);
                String key = args.optString(1);
                String value = args.optString(2);
                STATE.put(type + ":" + key, value);
                return "null";
            }
            case "get_state": {
                String type = args.optString(0);
                String key = args.optString(1);
                String value = STATE.get(type + ":" + key);
                return jsonString(value == null ? "" : value);
            }
            case "log":
                Log.d(TAG, args.optString(0));
                return "null";
            default:
                throw new IllegalArgumentException("unknown host method: " + name);
        }
    }

    private static String jsonString(String value) {
        return JSONObject.quote(value == null ? "" : value);
    }

    /* ---------------- fetch ---------------- */

    private JSONObject fetch(String url, String method, JSONObject headers, Object body)
            throws Exception {
        Request.Builder builder = new Request.Builder().url(url);
        if (headers != null) {
            Iterator<String> keys = headers.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = headers.optString(key);
                if (value != null && !value.isEmpty()) {
                    builder.header(key, value);
                }
            }
        }
        Request request;
        String upper = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
        switch (upper) {
            case "POST": {
                RequestBody requestBody = buildBody(body);
                request = builder.post(requestBody != null
                        ? requestBody : RequestBody.create(new byte[0])).build();
                break;
            }
            case "HEAD":
                request = builder.head().build();
                break;
            default:
                request = builder.get().build();
                break;
        }

        try (Response response = App.getHttpClient().newCall(request).execute()) {
            byte[] bytes = response.body() != null ? response.body().bytes() : new byte[0];
            String text = decodeBody(bytes, response.header("Content-Type"));

            JSONObject result = new JSONObject();
            result.put("status", response.code());
            result.put("body", text);
            JSONObject respHeaders = new JSONObject();
            for (int i = 0; i < response.headers().size(); i++) {
                respHeaders.put(response.headers().name(i), response.headers().value(i));
            }
            result.put("headers", respHeaders);
            return result;
        }
    }

    /**
     * 根据 JS 传入的 body 构建请求体：
     * <ul>
     *   <li>String：原始字符串（默认按表单发送）</li>
     *   <li>普通 JSONObject：表单字段</li>
     *   <li>{@code {multipart:[[name,value],...]}}：multipart/form-data</li>
     *   <li>{@code {json:...}}：JSON body</li>
     *   <li>JSONArray：键值对数组 → 表单</li>
     * </ul>
     */
    private static RequestBody buildBody(Object body) throws Exception {
        if (body == null) {
            return null;
        }
        if (body instanceof String) {
            return RequestBody.create((String) body,
                    MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"));
        }
        if (body instanceof JSONObject) {
            JSONObject obj = (JSONObject) body;
            if (obj.has("multipart")) {
                JSONArray parts = obj.getJSONArray("multipart");
                MultipartBody.Builder mb = new MultipartBody.Builder().setType(MultipartBody.FORM);
                for (int i = 0; i < parts.length(); i++) {
                    JSONArray pair = parts.getJSONArray(i);
                    mb.addFormDataPart(pair.optString(0), pair.optString(1));
                }
                return mb.build();
            }
            if (obj.has("json")) {
                Object json = obj.get("json");
                String text = json instanceof String ? (String) json : json.toString();
                return RequestBody.create(text,
                        MediaType.parse("application/json; charset=utf-8"));
            }
            FormBody.Builder fb = new FormBody.Builder();
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                fb.add(key, obj.optString(key));
            }
            return fb.build();
        }
        if (body instanceof JSONArray) {
            JSONArray pairs = (JSONArray) body;
            FormBody.Builder fb = new FormBody.Builder();
            for (int i = 0; i < pairs.length(); i++) {
                JSONArray pair = pairs.getJSONArray(i);
                fb.add(pair.optString(0), pair.optString(1));
            }
            return fb.build();
        }
        return null;
    }

    /* ---------------- AES-CBC 解密 ---------------- */

    private static String aesCbcDecrypt(String data, String key, String iv) {
        try {
            byte[] cipherBytes = decodeHexOrBase64(data);
            if (cipherBytes == null) {
                return "";
            }
            byte[] keyBytes = padKey(key);
            byte[] ivBytes = iv.getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new IvParameterSpec(ivBytes));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "aes_cbc 解密失败", e);
            return "";
        }
    }

    /** IV 为 Base64 密文解码后的前 16 字节（Manhuayu / DuManWuApp 用）。 */
    private static String aesCbcDecryptWithIvPrefix(String dataBase64, String key) {
        try {
            byte[] raw = Base64.decode(dataBase64, Base64.DEFAULT);
            if (raw.length < 16) {
                return "";
            }
            byte[] iv = Arrays.copyOfRange(raw, 0, 16);
            byte[] cipherBytes = Arrays.copyOfRange(raw, 16, raw.length);
            byte[] keyBytes = padKey(key);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                    new IvParameterSpec(iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "aes_cbc_raw 解密失败", e);
            return "";
        }
    }

    /** 密钥不足 16 字节时补 0（DuManWuApp 的 g8bh4z 等短密钥）。 */
    private static byte[] padKey(String key) {
        byte[] raw = key.getBytes(StandardCharsets.UTF_8);
        if (raw.length == 16 || raw.length == 24 || raw.length == 32) {
            return raw;
        }
        byte[] padded = new byte[16];
        System.arraycopy(raw, 0, padded, 0, Math.min(raw.length, 16));
        return padded;
    }

    private static byte[] decodeHexOrBase64(String s) {
        if (s == null) {
            return null;
        }
        if (s.matches("^[0-9a-fA-F]+$") && s.length() % 2 == 0) {
            byte[] out = new byte[s.length() / 2];
            for (int i = 0; i < s.length(); i += 2) {
                out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
            }
            return out;
        }
        return Base64.decode(s, Base64.DEFAULT);
    }

    /** 与 {@code Manga.getResponseBody} 一致的字符集探测逻辑。 */
    private static String decodeBody(byte[] bytes, String contentType) {
        String charset = null;
        if (contentType != null) {
            Matcher m = CHARSET_PATTERN.matcher(contentType);
            if (m.find()) {
                charset = m.group(1);
            }
        }
        String body;
        if (charset != null) {
            try {
                body = new String(bytes, charset);
                return body;
            } catch (Exception ignored) {
                // 字符集非法，回退默认解码
            }
        }
        body = new String(bytes);
        Matcher m = CHARSET_PATTERN.matcher(body);
        if (m.find()) {
            try {
                body = new String(bytes, m.group(1));
            } catch (Exception ignored) {
                // 保留默认解码结果
            }
        }
        return body;
    }

    /* ---------------- DOM ---------------- */

    private JSONArray domSelectAll(String html, String css) throws org.json.JSONException {
        Document doc = Jsoup.parse(html == null ? "" : html);
        Elements elements = (css == null || css.isEmpty())
                ? doc.select("body")
                : doc.select(css);
        JSONArray array = new JSONArray();
        for (Element element : elements) {
            JSONObject node = new JSONObject();
            node.put("text", element.text().trim());
            node.put("html", element.outerHtml());
            JSONObject attrs = new JSONObject();
            for (Attribute attribute : element.attributes()) {
                attrs.put(attribute.getKey(), attribute.getValue());
            }
            node.put("attrs", attrs);
            array.put(node);
        }
        return array;
    }

    private String domText(String html, String css) {
        Document doc = Jsoup.parse(html == null ? "" : html);
        Element element = (css == null || css.isEmpty()) ? doc.body() : doc.selectFirst(css);
        return element == null ? "" : element.text().trim();
    }

    private String domAttr(String html, String css, String attr) {
        Document doc = Jsoup.parse(html == null ? "" : html);
        Element element = (css == null || css.isEmpty()) ? doc.body() : doc.selectFirst(css);
        return element == null ? "" : element.attr(attr);
    }

    /* ---------------- 工具 ---------------- */

    private static String md5(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
