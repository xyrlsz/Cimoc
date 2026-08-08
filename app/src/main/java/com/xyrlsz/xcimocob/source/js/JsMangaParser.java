package com.xyrlsz.xcimocob.source.js;

import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import com.xyrlsz.quickjs.QuickJSEngine;
import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.core.Manga;
import com.xyrlsz.xcimocob.model.Chapter;
import com.xyrlsz.xcimocob.model.Comic;
import com.xyrlsz.xcimocob.model.ImageUrl;
import com.xyrlsz.xcimocob.model.JsSource;
import com.xyrlsz.xcimocob.model.Source;
import com.xyrlsz.xcimocob.parser.Category;
import com.xyrlsz.xcimocob.parser.MangaCategory;
import com.xyrlsz.xcimocob.parser.MangaParser;
import com.xyrlsz.xcimocob.parser.SearchIterator;
import com.xyrlsz.xcimocob.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 动态 JS 漫画源解析器：把 {@link MangaParser}/{@link com.xyrlsz.xcimocob.parser.Parser}
 * 的调用桥接到 QuickJS 中执行源脚本。
 *
 * <p>线程模型：QuickJS 的 Runtime 非线程安全，因此每次调用都创建独立的引擎实例
 * （评估 SDK + 源脚本 → 调用函数 → 释放），解析器实例本身是无状态的，可被多线程安全共享。
 *
 * <p>脚本规范见 {@code docs/js-source.md}。
 */
public class JsMangaParser extends MangaParser {

    private static final String TAG = "JsMangaParser";

    private static volatile String sSdk;

    static {
        // 注册 JS -> Java 宿主桥（JsHost 静态块同样会设置，此处保证先加载）
        QuickJSEngine.setHostBridge(JsHost.INSTANCE);
    }

    private final int mType;
    private final String mTitle;
    private final String mBaseUrl;
    /**
     * 完整脚本 = SDK + 源脚本。
     */
    private final String mScript;

    private volatile JsMetadata mMetadata;
    private volatile Headers mHeaderCache;
    private volatile boolean mHeaderLoaded;
    /**
     * 分类分页大小（来自 categories.pageSize，用于 {offset} 占位符）。
     */
    private volatile int mCategoryPageSize = 20;
    /**
     * getImagesRequest 与 parseImages 在同一线程链路上调用，
     * 用 ThreadLocal 把 cid 传给 parseImages（JS 引擎无状态，不能跨调用保存）。
     */
    private final ThreadLocal<String> mThreadCid = new ThreadLocal<>();

    public JsMangaParser(JsSource source) {
        this(source.getType(), source.getTitle(), source.getBaseUrl(), source.getScript());
    }

    public JsMangaParser(int type, String title, String baseUrl, String script) {
        this.mType = type;
        this.mTitle = title;
        this.mBaseUrl = baseUrl;
        this.mScript = loadSdk() + "\n" + (script == null ? "" : script);
        // 复用 MangaParser 的初始化（填充标题、URL 过滤器列表等）
        init(new Source(0, title, type, true, baseUrl));
    }

    private static String loadSdk() {
        if (sSdk == null) {
            synchronized (JsMangaParser.class) {
                if (sSdk == null) {
                    String sdk = "";
                    try (InputStream is = App.getAppContext().getAssets().open("js/source_sdk.js")) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) != -1) {
                            bos.write(buf, 0, n);
                        }
                        sdk = bos.toString("utf-8");
                    } catch (Exception e) {
                        Log.e(TAG, "加载 JS SDK 失败", e);
                    }
                    sSdk = sdk;
                }
            }
        }
        return sSdk;
    }

    /**
     * 校验一段源脚本是否满足运行要求。
     *
     * @param script 源脚本（不含 SDK）
     * @return 出错信息；脚本合法时返回 {@code null}
     */
    public static String validateScript(String script) {
        if (script == null || script.trim().isEmpty()) {
            return "脚本内容为空";
        }
        String full = loadSdk() + "\n" + script;
        try (QuickJSEngine engine = new QuickJSEngine()) {
            engine.evaluate(full);
            String meta = engine.getGlobalJson("SOURCE");
            if (meta == null || "null".equals(meta)) {
                return "缺少 SOURCE 元数据对象";
            }
            JSONObject source = new JSONObject(meta);
            if (!source.has("type") || source.optInt("type") < 0) {
                return "SOURCE.type 缺失或非法";
            }
            String[] required = {"getSearchRequest", "parseSearch", "getInfoRequest",
                    "parseInfo", "getImagesRequest", "parseImages"};
            for (String fn : required) {
                if (!engine.hasFunction(fn)) {
                    return "缺少必需函数: " + fn;
                }
            }
            return null;
        } catch (Exception e) {
            return "脚本执行/解析失败: " + e.getMessage();
        }
    }

    /* ==================== 脚本调用辅助 ==================== */

    private String callFunction(String name, String... args) throws JsSourceException {
        JSONArray jsonArgs = new JSONArray();
        for (String arg : args) {
            jsonArgs.put(arg == null ? "" : arg);
        }
        try (QuickJSEngine engine = new QuickJSEngine()) {
            engine.evaluate(mScript);
            if (!engine.hasFunction(name)) {
                return null;
            }
            return engine.callFunction(name, jsonArgs.toString());
        } catch (RuntimeException e) {
            throw new JsSourceException("JS[" + mTitle + "] " + name + " 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 把 JS 返回值（JSON 序列化）转成普通字符串。
     * 支持裸字符串、{@code {"url": ...}}、{@code {"result": ...}}。
     */
    private String jsonResultToString(String json) {
        if (json == null || "null".equals(json) || json.isEmpty()) {
            return null;
        }
        try {
            Object value = new JSONTokener(json).nextValue();
            if (value instanceof String) {
                return (String) value;
            }
            if (value instanceof JSONObject) {
                JSONObject obj = (JSONObject) value;
                String url = obj.optString("url");
                if (!url.isEmpty()) {
                    return url;
                }
                String result = obj.optString("result");
                if (!result.isEmpty()) {
                    return result;
                }
            }
            return String.valueOf(value);
        } catch (JSONException e) {
            return json;
        }
    }

    /* ==================== 元数据 ==================== */

    private static class JsMetadata {
        final List<String> hosts = new ArrayList<>();
        String cidRegex = "(\\d+)";
        int cidGroup = 1;
        Category category;
    }

    private JsMetadata getMetadata() {
        if (mMetadata == null) {
            synchronized (this) {
                if (mMetadata == null) {
                    mMetadata = readMetadata();
                }
            }
        }
        return mMetadata;
    }

    private JsMetadata readMetadata() {
        JsMetadata meta = new JsMetadata();
        try (QuickJSEngine engine = new QuickJSEngine()) {
            engine.evaluate(mScript);
            String json = engine.getGlobalJson("SOURCE");
            if (json == null || "null".equals(json)) {
                return meta;
            }
            JSONObject source = new JSONObject(json);
            JSONArray hosts = source.optJSONArray("hosts");
            if (hosts != null) {
                for (int i = 0; i < hosts.length(); i++) {
                    String host = hosts.optString(i);
                    if (!host.isEmpty()) {
                        meta.hosts.add(host);
                    }
                }
            }
            meta.cidRegex = source.optString("cidRegex", "(\\d+)");
            meta.cidGroup = source.optInt("cidGroup", 1);

            JSONObject webConfig = source.optJSONObject("webConfig");
            if (webConfig != null) {
                applyWebConfig(webConfig);
            }
            JSONObject categories = source.optJSONObject("categories");
            if (categories == null) {
                // 允许脚本用 getCategories() 动态生成分类（如需要运行时拼接 URL）
                try {
                    String catJson = engine.callFunction("getCategories", "[]");
                    if (catJson != null && !"null".equals(catJson)) {
                        categories = new JSONObject(catJson);
                    }
                } catch (Exception ignored) {
                    // 脚本未定义 getCategories
                }
            }
            if (categories != null) {
                meta.category = buildCategory(categories);
            }
        } catch (Exception e) {
            Log.e(TAG, "读取源元数据失败", e);
        }
        return meta;
    }

    private void applyWebConfig(JSONObject webConfig) {
        applyConfig(getSearchConfig(), webConfig.optJSONObject("search"));
        applyConfig(getInfoConfig(), webConfig.optJSONObject("info"));
        applyConfig(getChapterConfig(), webConfig.optJSONObject("chapter"));
        applyConfig(getImagesConfig(), webConfig.optJSONObject("images"));
        applyConfig(getImagesLazyConfig(), webConfig.optJSONObject("imagesLazy"));
    }

    private void applyConfig(com.xyrlsz.xcimocob.parser.WebParserConfig config, JSONObject json) {
        if (config == null || json == null) {
            return;
        }
        config.setUseWebParser(json.optBoolean("useWebParser", config.isUseWebParser()));
        config.setAutoScroll(json.optBoolean("autoScroll", config.isAutoScroll()));
        if (json.has("injectJs")) {
            config.setInjectJs(json.optString("injectJs"));
        }
        config.setHandleCloudflare(json.optBoolean("handleCloudflare", config.isHandleCloudflare()));
        if (json.has("cloudflareTimeoutMs")) {
            config.setCloudflareTimeoutMs(json.optLong("cloudflareTimeoutMs", config.getCloudflareTimeoutMs()));
        }
        config.setInteractiveChallenge(json.optBoolean("interactiveChallenge", config.isInteractiveChallenge()));
    }

    private Category buildCategory(JSONObject categories) {
        final JSONObject cat = categories;
        mCategoryPageSize = cat.optInt("pageSize", 20);
        return new MangaCategory() {
            @Override
            public boolean isComposite() {
                return cat.optBoolean("composite", false);
            }

            @Override
            public String getFormat(String... args) {
                String format = cat.optString("format", "");
                format = format.replace("{subject}", safe(args, CATEGORY_SUBJECT));
                format = format.replace("{area}", safe(args, CATEGORY_AREA));
                format = format.replace("{reader}", safe(args, CATEGORY_READER));
                format = format.replace("{progress}", safe(args, CATEGORY_PROGRESS));
                format = format.replace("{year}", safe(args, CATEGORY_YEAR));
                format = format.replace("{order}", safe(args, CATEGORY_ORDER));
                // 复刻内置源 Category.getFormat 的空白归一化：
                // 空白串转为单个 "-"，连续 "-" 合并（对不含空白的模板无影响）
                format = format.replaceAll("\\s+", "-").replaceAll("-+", "-");
                // 保留 {page}，由 getCategoryRequest 直接替换为页码
                return format;
            }

            @Override
            protected boolean hasArea() {
                return cat.has("area");
            }

            @Override
            protected boolean hasReader() {
                return cat.has("reader");
            }

            @Override
            protected boolean hasProgress() {
                return cat.has("progress");
            }

            @Override
            protected boolean hasYear() {
                return cat.has("year");
            }

            @Override
            protected boolean hasOrder() {
                return cat.has("order");
            }

            @Override
            protected List<Pair<String, String>> getSubject() {
                return attrList(cat.optJSONArray("subject"));
            }

            @Override
            protected List<Pair<String, String>> getArea() {
                return attrList(cat.optJSONArray("area"));
            }

            @Override
            protected List<Pair<String, String>> getReader() {
                return attrList(cat.optJSONArray("reader"));
            }

            @Override
            protected List<Pair<String, String>> getProgress() {
                return attrList(cat.optJSONArray("progress"));
            }

            @Override
            protected List<Pair<String, String>> getYear() {
                return attrList(cat.optJSONArray("year"));
            }

            @Override
            protected List<Pair<String, String>> getOrder() {
                return attrList(cat.optJSONArray("order"));
            }
        };
    }

    private static String safe(String[] args, int index) {
        return args != null && index < args.length && args[index] != null ? args[index] : "";
    }

    private static List<Pair<String, String>> attrList(JSONArray array) {
        List<Pair<String, String>> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) {
                continue;
            }
            list.add(Pair.create(obj.optString("title"), obj.optString("value")));
        }
        return list;
    }

    /* ==================== 请求构建 ==================== */

    /**
     * 把 JS 返回的请求描述（{@code {url, method, headers, body}}）构造成 okhttp Request。
     */
    private Request buildRequest(String json) throws JsSourceException {
        if (json == null || "null".equals(json)) {
            return null;
        }
        try {
            JSONObject obj = new JSONObject(json);
            String url = obj.optString("url");
            if (url == null || url.isEmpty()) {
                return null;
            }
            Request.Builder builder = new Request.Builder().url(url);
            JSONObject headers = obj.optJSONObject("headers");
            if (headers != null) {
                Iterator<String> keys = headers.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = headers.optString(key);
                    if (value != null && !value.isEmpty()) {
                        builder.addHeader(key, value);
                    }
                }
            }
            String method = obj.optString("method", "GET");
            if ("POST".equalsIgnoreCase(method)) {
                Object body = obj.opt("body");
                if (body instanceof JSONObject) {
                    FormBody.Builder form = new FormBody.Builder();
                    JSONObject formObj = (JSONObject) body;
                    Iterator<String> keys = formObj.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        form.add(key, formObj.optString(key));
                    }
                    builder.post(form.build());
                } else if (body != null && !JSONObject.NULL.equals(body)) {
                    builder.post(RequestBody.create(body.toString(),
                            MediaType.parse("application/x-www-form-urlencoded; charset=utf-8")));
                } else {
                    builder.post(RequestBody.create(new byte[0]));
                }
            } else {
                builder.get();
            }
            return builder.build();
        } catch (JSONException e) {
            throw new JsSourceException("JS[" + mTitle + "] 请求描述解析失败: " + e.getMessage(), e);
        }
    }

    /* ==================== 数据转换 ==================== */

    private List<Comic> parseComics(String json) {
        List<Comic> list = new ArrayList<>();
        if (json == null || "null".equals(json)) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                String cid = obj.optString("cid");
                String title = obj.optString("title");
                if (cid == null || cid.isEmpty()) {
                    continue;
                }
                list.add(new Comic(mType, cid, title,
                        obj.optString("cover"), obj.optString("update"), obj.optString("author")));
            }
        } catch (Exception e) {
            Log.e(TAG, "解析漫画列表失败", e);
        }
        return list;
    }

    private List<Chapter> parseChapters(String json, long sourceComic) {
        List<Chapter> list = new ArrayList<>();
        if (json == null || "null".equals(json)) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj == null) {
                    continue;
                }
                String title = obj.optString("title");
                String path = obj.optString("path");
                if (title == null || path == null || title.isEmpty() || path.isEmpty()) {
                    continue;
                }
                long tid = obj.optLong("tid", -1);
                list.add(new Chapter(0L, sourceComic, title, path, tid));
            }
        } catch (Exception e) {
            Log.e(TAG, "解析章节列表失败", e);
        }
        return list;
    }

    private List<ImageUrl> parseImageUrls(String json) {
        List<ImageUrl> list = new ArrayList<>();
        if (json == null || "null".equals(json)) {
            return list;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                Object item = array.opt(i);
                String url = null;
                boolean lazy = false;
                Headers headers = null;
                if (item instanceof String) {
                    url = (String) item;
                } else if (item instanceof JSONObject) {
                    JSONObject obj = (JSONObject) item;
                    url = obj.optString("url");
                    lazy = obj.optBoolean("lazy", false);
                    headers = parseHeaders(obj.optJSONObject("headers"));
                }
                if (url == null || url.isEmpty()) {
                    continue;
                }
                ImageUrl imageUrl = new ImageUrl(0, 0, i, url, lazy);
                if (headers != null) {
                    imageUrl.setHeaders(headers);
                }
                list.add(imageUrl);
            }
        } catch (Exception e) {
            Log.e(TAG, "解析图片列表失败", e);
        }
        return list;
    }

    private Headers parseHeaders(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        try {
            Headers.Builder builder = new Headers.Builder();
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = obj.optString(key);
                if (value != null && !value.isEmpty()) {
                    builder.add(key, value);
                }
            }
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }

    /* ==================== Parser 接口实现 ==================== */

    @Override
    public Request getSearchRequest(String keyword, int page) throws Exception {
        return buildRequest(callFunction("getSearchRequest", keyword, String.valueOf(page)));
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        List<Comic> list;
        try {
            list = parseComics(callFunction("parseSearch", html, String.valueOf(page)));
        } catch (JsSourceException e) {
            Log.e(TAG, "parseSearch 失败", e);
            list = new ArrayList<>();
        }
        return new ListSearchIterator(list);
    }

    @Override
    public Request getInfoRequest(String cid) {
        try {
            return buildRequest(callFunction("getInfoRequest", cid));
        } catch (JsSourceException e) {
            Log.e(TAG, "getInfoRequest 失败", e);
            return null;
        }
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        try {
            String json = callFunction("parseInfo", html, comic.getCid());
            if (json != null && !"null".equals(json)) {
                JSONObject obj = new JSONObject(json);
                String title = obj.optString("title", comic.getTitle());
                String cover = obj.optString("cover", comic.getCover());
                String update = obj.optString("update", comic.getUpdate());
                String intro = obj.optString("intro", comic.getIntro());
                String author = obj.optString("author", comic.getAuthor());
                boolean finish = obj.optBoolean("finish",
                        comic.getFinish() != null && comic.getFinish());
                comic.setInfo(title, cover, update, intro, author, finish);
                // parseInfo 返回章节时缓存到 transient 字段，供 parseChapter 直接使用；
                // 否则若返回任意 note 字段，原样缓存供脚本 parseChapter 读取
                if (obj.has("chapters") && !obj.isNull("chapters")) {
                    comic.note = obj.optJSONArray("chapters");
                } else if (obj.has("note") && !obj.isNull("note")) {
                    comic.note = obj.get("note");
                } else {
                    comic.note = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "parseInfo 失败", e);
        }
        return comic;
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        try {
            return buildRequest(callFunction("getChapterRequest", html, cid));
        } catch (JsSourceException e) {
            Log.e(TAG, "getChapterRequest 失败", e);
            return null;
        }
    }

    @Override
    public List<Chapter> parseChapter(String html) throws JSONException {
        try {
            return parseChapters(callFunction("parseChapter", html), 0);
        } catch (JsSourceException e) {
            Log.e(TAG, "parseChapter 失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        // 优先使用 parseInfo 缓存的章节
        if (comic != null && comic.note instanceof JSONArray) {
            JSONArray cached = (JSONArray) comic.note;
            comic.note = null;
            return parseChapters(cached.toString(), sourceComic);
        }
        try {
            // 把 comic 上下文（cid/note）传给脚本，供需要它的源使用
            String ctx = comicContext(comic);
            return parseChapters(callFunction("parseChapter", html, ctx), sourceComic);
        } catch (JsSourceException e) {
            Log.e(TAG, "parseChapter 失败", e);
            return new ArrayList<>();
        }
    }

    private String comicContext(Comic comic) {
        if (comic == null) {
            return "{}";
        }
        try {
            JSONObject ctx = new JSONObject();
            ctx.put("cid", comic.getCid());
            ctx.put("title", comic.getTitle());
            ctx.put("note", comic.note == null ? JSONObject.NULL : comic.note);
            return ctx.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        // 与 parseImages 同线程链路，用 ThreadLocal 传递 cid（JS 引擎无状态）
        mThreadCid.set(cid);
        try {
            return buildRequest(callFunction("getImagesRequest", cid, path));
        } catch (JsSourceException e) {
            Log.e(TAG, "getImagesRequest 失败", e);
            return null;
        }
    }

    @Override
    public List<ImageUrl> parseImages(String html) throws Manga.NetworkErrorException, JSONException {
        try {
            return parseImageUrls(callFunction("parseImages", html, imageContext(null)));
        } catch (JsSourceException e) {
            Log.e(TAG, "parseImages 失败", e);
            throw new JSONException("parseImages 失败: " + e.getMessage());
        }
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        try {
            String ctx = imageContext(chapter);
            return parseImageUrls(callFunction("parseImages", html, ctx));
        } catch (JsSourceException e) {
            Log.e(TAG, "parseImages 失败", e);
            throw new JSONException("parseImages 失败: " + e.getMessage());
        }
    }

    private String imageContext(Chapter chapter) {
        try {
            JSONObject ctx = new JSONObject();
            ctx.put("cid", mThreadCid.get() == null ? "" : mThreadCid.get());
            ctx.put("path", chapter == null ? "" : chapter.getPath());
            ctx.put("title", chapter == null ? "" : chapter.getTitle());
            return ctx.toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    @Override
    public Request getLazyRequest(String url) {
        try {
            return buildRequest(callFunction("getLazyRequest", url));
        } catch (JsSourceException e) {
            Log.e(TAG, "getLazyRequest 失败", e);
            return null;
        }
    }

    @Override
    public String parseLazy(String html, String url) {
        try {
            return jsonResultToString(callFunction("parseLazy", html, url));
        } catch (JsSourceException e) {
            Log.e(TAG, "parseLazy 失败", e);
            return null;
        }
    }

    @Override
    public Request getCheckRequest(String cid) {
        try {
            return buildRequest(callFunction("getCheckRequest", cid));
        } catch (JsSourceException e) {
            Log.e(TAG, "getCheckRequest 失败", e);
            return null;
        }
    }

    @Override
    public String parseCheck(String html) {
        try {
            return jsonResultToString(callFunction("parseCheck", html));
        } catch (JsSourceException e) {
            Log.e(TAG, "parseCheck 失败", e);
            return null;
        }
    }

    @Override
    public Category getCategory() {
        return getMetadata().category;
    }

    @Override
    public Request getCategoryRequest(String format, int page) {
        // 脚本可自定义分类请求（如 POST/GraphQL），返回请求描述；未定义或返回 null 时回退模板
        try {
            Request custom = buildRequest(callFunction("getCategoryRequest",
                    format == null ? "" : format, String.valueOf(page)));
            if (custom != null) {
                return custom;
            }
        } catch (Exception e) {
            Log.e(TAG, "getCategoryRequest(js) 失败，回退模板", e);
        }
        String url = format == null ? "" : format.replace("{page}", String.valueOf(page));
        if (url.contains("{offset}")) {
            url = url.replace("{offset}", String.valueOf((page - 1) * mCategoryPageSize));
        }
        if (url.contains("{limit}")) {
            url = url.replace("{limit}", String.valueOf(mCategoryPageSize));
        }
        Request.Builder builder = new Request.Builder().url(url);
        Headers headers = getHeader();
        if (headers != null) {
            builder.headers(headers);
        }
        return builder.build();
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        try {
            return parseComics(callFunction("parseCategory", html, String.valueOf(page)));
        } catch (JsSourceException e) {
            Log.e(TAG, "parseCategory 失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    /* ==================== WebParser 配置（应用脚本 webConfig） ==================== */

    @Override
    public com.xyrlsz.xcimocob.parser.WebParserConfig getSearchConfig() {
        getMetadata();
        return super.getSearchConfig();
    }

    @Override
    public com.xyrlsz.xcimocob.parser.WebParserConfig getInfoConfig() {
        getMetadata();
        return super.getInfoConfig();
    }

    @Override
    public com.xyrlsz.xcimocob.parser.WebParserConfig getChapterConfig() {
        getMetadata();
        return super.getChapterConfig();
    }

    @Override
    public com.xyrlsz.xcimocob.parser.WebParserConfig getImagesConfig() {
        getMetadata();
        return super.getImagesConfig();
    }

    @Override
    public com.xyrlsz.xcimocob.parser.WebParserConfig getImagesLazyConfig() {
        getMetadata();
        return super.getImagesLazyConfig();
    }

    @Override
    public Headers getHeader() {
        if (!mHeaderLoaded) {
            synchronized (this) {
                if (!mHeaderLoaded) {
                    mHeaderCache = loadHeader();
                    mHeaderLoaded = true;
                }
            }
        }
        return mHeaderCache;
    }

    @Override
    public Headers getHeader(String url) {
        return getHeader();
    }

    @Override
    public Headers getHeader(List<ImageUrl> list) {
        return getHeader();
    }

    private Headers loadHeader() {
        try {
            String json = callFunction("getHeader");
            if (json == null || "null".equals(json)) {
                return null;
            }
            JSONObject obj = new JSONObject(json);
            Headers.Builder builder = new Headers.Builder();
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = obj.optString(key);
                if (value != null && !value.isEmpty()) {
                    builder.add(key, value);
                }
            }
            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "getHeader 失败", e);
            return null;
        }
    }

    @Override
    public String getUrl(String cid) {
        try {
            String url = jsonResultToString(callFunction("getUrl", cid));
            if (url != null && !url.isEmpty()) {
                return url;
            }
        } catch (Exception ignored) {
        }
        if (mBaseUrl != null && !mBaseUrl.isEmpty()) {
            return mBaseUrl.endsWith("/") ? mBaseUrl + cid : mBaseUrl + "/" + cid;
        }
        return cid;
    }

    @Override
    public boolean isHere(Uri uri) {
        JsMetadata meta = getMetadata();
        if (uri.getHost() == null) {
            return false;
        }
        for (String host : meta.hosts) {
            if (uri.getHost().contains(host)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getComicId(Uri uri) {
        JsMetadata meta = getMetadata();
        if (uri.getHost() == null) {
            return null;
        }
        for (String host : meta.hosts) {
            if (uri.getHost().contains(host)) {
                String path = uri.getPath();
                if (path != null && path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                String result = StringUtils.match(meta.cidRegex, path, meta.cidGroup);
                if (result != null) {
                    if (result.startsWith("/")) {
                        result = result.substring(1);
                    }
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    protected void initUrlFilterList() {
        // 运行时从脚本元数据读取 hosts，不在这里硬编码
    }

    @Override
    public String getUA() {
        return "";
    }

    /**
     * JS 源脚本执行异常。
     */
    public static class JsSourceException extends Exception {
        public JsSourceException(String message) {
            super(message);
        }

        public JsSourceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class ListSearchIterator implements SearchIterator {
        private final Iterator<Comic> iterator;
        private final int size;

        ListSearchIterator(List<Comic> list) {
            this.size = list.size();
            this.iterator = list.iterator();
        }

        @Override
        public boolean empty() {
            return size == 0;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Comic next() {
            return iterator.next();
        }
    }
}
