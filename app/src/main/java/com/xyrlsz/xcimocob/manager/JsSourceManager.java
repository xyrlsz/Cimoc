package com.xyrlsz.xcimocob.manager;

import android.util.Log;
import android.util.SparseArray;

import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.component.AppGetter;
import com.xyrlsz.xcimocob.model.JsSource;
import com.xyrlsz.xcimocob.model.JsSource_;
import com.xyrlsz.xcimocob.model.Source;
import com.xyrlsz.xcimocob.source.js.JsMangaParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 动态 JS 源的存储与在线更新管理器。
 *
 * <p>脚本保存在 ObjectBox（{@link JsSource} 表）；在线更新从配置的源仓库
 * 拉取 {@code index.json} 清单 + 各脚本文件，校验通过后入库，并同步到
 * {@link SourceManager} 的 {@link Source} 表（保证 UI 可见、可开关）。
 */
public class JsSourceManager {

    private static final String TAG = "JsSourceManager";

    /**
     * 源仓库清单文件名（相对仓库根目录）。
     */
    public static final String MANIFEST_NAME = "index.json";

    private static volatile JsSourceManager mInstance;

    private final Box<JsSource> mSourceBox;
    private final SourceManager mSourceManager;

    private final SparseArray<JsSource> mCache = new SparseArray<>();
    private volatile boolean mCacheDirty = true;

    private JsSourceManager(AppGetter getter) {
        BoxStore boxStore = getter.getAppInstance().getBoxStore();
        mSourceBox = boxStore.boxFor(JsSource.class);
        mSourceManager = SourceManager.getInstance(getter);
    }

    public static JsSourceManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (JsSourceManager.class) {
                if (mInstance == null) {
                    mInstance = new JsSourceManager(getter);
                }
            }
        }
        return mInstance;
    }

    /**
     * 获取指定类型的 JS 源（带内存缓存）。
     */
    public JsSource load(int type) {
        refreshCacheIfNeeded();
        return mCache.get(type);
    }

    public List<JsSource> list() {
        return mSourceBox.getAll();
    }

    public JsSource loadByType(int type) {
        return mSourceBox.query()
                .equal(JsSource_.type, type)
                .build()
                .findFirst();
    }

    public void put(JsSource source) {
        mSourceBox.put(source);
        invalidate();
    }

    public void remove(JsSource source) {
        mSourceBox.remove(source);
        invalidate();
    }

    public void removeByType(int type) {
        mSourceBox.query()
                .equal(JsSource_.type, type)
                .build()
                .remove();
        invalidate();
    }

    /**
     * 使内存缓存失效（脚本增删改后调用）。
     */
    public void invalidate() {
        mCacheDirty = true;
        mSourceManager.invalidateParserCache();
    }

    private void refreshCacheIfNeeded() {
        if (mCacheDirty) {
            synchronized (this) {
                if (mCacheDirty) {
                    mCache.clear();
                    for (JsSource source : mSourceBox.getAll()) {
                        mCache.put(source.getType(), source);
                    }
                    mCacheDirty = false;
                }
            }
        }
    }

    /* ==================== 在线更新 ==================== */

    /**
     * 从源仓库拉取并更新全部 JS 源。
     *
     * @param repoUrl 仓库根地址（如 {@code https://example.com/js-sources}），
     *                最终请求 {@code repoUrl/index.json}
     * @return 本次新增/更新的源数量
     * @throws Exception 网络或校验失败时抛出
     */
    public int updateFromServer(String repoUrl) throws Exception {
        String base = repoUrl == null ? "" : repoUrl.trim();
        if (base.isEmpty()) {
            throw new IllegalArgumentException("源仓库地址为空");
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String manifestJson = httpGet(base + "/" + MANIFEST_NAME);
        JSONObject manifest = new JSONObject(manifestJson);
        JSONArray sources = manifest.optJSONArray("sources");
        if (sources == null) {
            throw new IllegalArgumentException("源仓库清单缺少 sources 字段");
        }

        int updated = 0;
        List<Integer> manifestTypes = new ArrayList<>();
        for (int i = 0; i < sources.length(); i++) {
            JSONObject entry = sources.getJSONObject(i);
            int type = entry.optInt("type", -1);
            if (type < 0) {
                continue;
            }
            manifestTypes.add(type);
            String title = entry.optString("title");
            String version = entry.optString("version", "");
            String baseUrl = entry.optString("baseUrl");
            String scriptUrl = entry.optString("url");

            JsSource existing = loadByType(type);
            if (existing != null && version.equals(existing.getVersion())) {
                continue; // 已是最新
            }

            String resolvedUrl = resolveUrl(base, scriptUrl);
            String script = httpGet(resolvedUrl);
            String error = JsMangaParser.validateScript(script);
            if (error != null) {
                throw new IllegalArgumentException("源 [" + title + "] 脚本校验失败: " + error);
            }

            JsSource source = new JsSource(
                    existing != null ? existing.getId() : 0,
                    type,
                    title,
                    version,
                    script,
                    baseUrl,
                    existing == null || existing.getEnable(),
                    System.currentTimeMillis());
            mSourceBox.put(source);
            syncSourceTable(type, title, baseUrl);
            updated++;
            Log.i(TAG, "已更新 JS 源 type=" + type + " title=" + title + " version=" + version);
        }

        // 失效内存缓存与 SourceManager 的解析器缓存
        invalidate();
        return updated;
    }

    /**
     * 把 JS 源同步到 Source 表（保证源列表 UI 可见、可开关）。
     * 内置源同 type 时更新标题/baseUrl，否则插入新记录。
     */
    private void syncSourceTable(int type, String title, String baseUrl) {
        Source source = mSourceManager.load(type);
        if (source == null) {
            mSourceManager.insert(new Source(null, title, type, true, baseUrl));
        } else {
            boolean changed = false;
            if (title != null && !title.equals(source.getTitle())) {
                source.setTitle(title);
                changed = true;
            }
            if (baseUrl != null && !baseUrl.equals(source.getBaseUrl())) {
                source.setBaseUrl(baseUrl);
                changed = true;
            }
            if (changed) {
                mSourceManager.update(source);
            }
        }
    }

    private static String resolveUrl(String base, String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("脚本 url 为空");
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return base + (url.startsWith("/") ? url : "/" + url);
    }

    private static String httpGet(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        try (Response response = App.getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("HTTP " + response.code() + " " + url);
            }
            return response.body().string();
        }
    }
}
