package com.xyrlsz.xcimocob.network.sync;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.manager.PreferenceManager;
import com.xyrlsz.xcimocob.utils.Base64Utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 与 XCimoc Data Sync Server (Go) 通信的 API 客户端
 * <p>
 * 使用 OkHttp + Gson，无需 Retrofit 依赖。
 * 所有方法均同步执行，调用方应在后台线程执行。
 */
public class DataSyncClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson GSON = new GsonBuilder().create();

    /** 静态共享 OkHttpClient，启用连接池复用（keep-alive） */
    private static final OkHttpClient SHARED_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new okhttp3.ConnectionPool(5, 60, TimeUnit.SECONDS))
            .build();

    private final OkHttpClient mHttpClient;
    private final String mBaseUrl;

    public DataSyncClient() {
        this(App.getPreferenceManager().getString(PreferenceManager.PREF_DATA_SERVER_URL, ""));
    }

    public DataSyncClient(String baseUrl) {
        // 去除末尾斜杠
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.mBaseUrl = baseUrl;
        this.mHttpClient = SHARED_HTTP_CLIENT;
    }

    public boolean isConfigured() {
        return !TextUtils.isEmpty(mBaseUrl);
    }

    /**
     * 判断 baseUrl 是否相同，用于复用 DataSyncClient 实例
     */
    public boolean isSameBaseUrl(String url) {
        if (url == null) return false;
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return mBaseUrl.equals(url);
    }

    // ==================== Auth ====================

    /**
     * 登录，返回 token
     */
    public DataSyncModels.LoginResponse login(String username, String password)
            throws IOException, DataSyncException {
        DataSyncModels.LoginRequest req = new DataSyncModels.LoginRequest(username, password);
        String json = GSON.toJson(req);
        String body = post("/api/auth/login", json);
        return GSON.fromJson(body, DataSyncModels.LoginResponse.class);
    }

    /**
     * 刷新 token（延长有效期）
     */
    public String refreshToken(String oldToken) throws IOException, DataSyncException {
        String body = post("/api/auth/refresh", "{}", oldToken);
        // 解析响应 {"token": "..."}
        RefreshTokenResponse resp = GSON.fromJson(body, RefreshTokenResponse.class);
        return resp != null ? resp.token : null;
    }

    private static class RefreshTokenResponse {
        @SerializedName("token")
        public String token;
    }

    // ==================== Token 工具 ====================

    /**
     * 检查 token 是否需要刷新（即将在 7 天内过期时返回 true）
     */
    public static boolean isTokenExpiringSoon(String token) {
        if (TextUtils.isEmpty(token)) return false;
        try {
            // JWT 格式: header.payload.signature
            // 服务端签发的 payload 是 base64url（JVM URL 解码器自动容忍无 padding）
            String[] parts = token.split("\\.");
            if (parts.length < 2) return false;

            byte[] payload = Base64Utils.decodeUrlSafe(parts[1]);
            String json = new String(payload, "UTF-8");

            // 快速解析 exp 字段
            int expIdx = json.indexOf("\"exp\"");
            if (expIdx < 0) return false;
            int colonIdx = json.indexOf(":", expIdx + 4);
            if (colonIdx < 0) return false;

            // 提取数字
            StringBuilder numStr = new StringBuilder();
            for (int i = colonIdx + 1; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c >= '0' && c <= '9') {
                    numStr.append(c);
                } else if (numStr.length() > 0) {
                    break;
                }
            }
            if (numStr.length() == 0) return false;

            long exp = Long.parseLong(numStr.toString()) * 1000; // 转毫秒
            long now = System.currentTimeMillis();
            // 服务端签发的 token 有效期为 7 天
            // 剩余有效期少于 1 天时触发刷新，避免无意义的频繁刷新
            long oneDayMs = 24L * 60 * 60 * 1000;

            // 如果剩余有效期少于 1 天，需要刷新
            return (exp - now) < oneDayMs;
        } catch (Exception e) {
            Log.w("DataSyncClient", "Failed to parse token expiry", e);
            return false;
        }
    }

    /**
     * 确保 token 有效：如果需要刷新则自动刷新并保存。
     * 若刷新失败（401 或返回 null）且本地保存了账号密码，则自动重新登录。
     *
     * @return 有效的 token，如果全部失败则返回 null
     */
    public static String ensureValidToken() {
        PreferenceManager pm = App.getPreferenceManager();
        String token = pm.getString(PreferenceManager.PREFERENCES_USER_TOCKEN, "");
        if (TextUtils.isEmpty(token)) return null;

        if (!isTokenExpiringSoon(token)) {
            return token; // token 仍然有效
        }

        // 即将过期 → 预刷新（网络失败时回退旧 token，因为 token 此刻仍有效）
        return refreshOrRelogin(true);
    }

    /**
     * 强制刷新 token；刷新失败（401/null）时用本地保存的账号密码自动重新登录。
     * 通常在收到服务端 401 后调用（token 已被服务端拒绝，不能回退旧 token）。
     *
     * @return 新的有效 token；无法自动恢复时返回 null
     */
    public static String refreshOrRelogin() {
        return refreshOrRelogin(false);
    }

    /**
     * 刷新 token 并（必要时）自动重新登录。
     *
     * @param fallbackToOldToken true：刷新遇到网络/服务器错误（非 401）时回退返回旧 token
     *        （用于"预刷新"场景——token 仍有效只是快过期）；
     *        false：已确认 token 被服务端拒绝（401），不回退旧 token，全部失败返回 null
     * @return 可用的 token；无法获得时返回 null
     */
    public static String refreshOrRelogin(boolean fallbackToOldToken) {
        PreferenceManager pm = App.getPreferenceManager();
        String token = pm.getString(PreferenceManager.PREFERENCES_USER_TOCKEN, "");
        if (TextUtils.isEmpty(token)) return null;

        String serverUrl = pm.getString(PreferenceManager.PREF_DATA_SERVER_URL, "");
        if (TextUtils.isEmpty(serverUrl)) return fallbackToOldToken ? token : null;

        DataSyncClient client = new DataSyncClient(serverUrl);

        // 1) 尝试刷新 token（延长有效期）
        try {
            String newToken = client.refreshToken(token);
            if (newToken != null) {
                pm.putString(PreferenceManager.PREFERENCES_USER_TOCKEN, newToken);
                Log.d("DataSyncClient", "Token refreshed successfully");
                return newToken;
            }
            // refresh 返回 null → 继续尝试自动登录
            Log.w("DataSyncClient", "Token refresh returned null, trying auto re-login");
        } catch (DataSyncException e) {
            if (e.httpCode != 401 && fallbackToOldToken) {
                // 预刷新场景：非 401 错误（500 等），旧 token 仍可能有效，回退
                Log.w("DataSyncClient", "Token refresh failed (HTTP " + e.httpCode + "), using old token", e);
                return token;
            }
            Log.w("DataSyncClient", "Token refresh unauthorized/failed (HTTP " + e.httpCode + "), trying auto re-login", e);
        } catch (Exception e) {
            if (fallbackToOldToken) {
                // 预刷新场景：网络问题，旧 token 仍可能有效，回退
                Log.w("DataSyncClient", "Token refresh failed (network), using old token", e);
                return token;
            }
            Log.w("DataSyncClient", "Token refresh failed (network), trying auto re-login", e);
        }

        // 2) 自动重新登录（用本地保存的账号密码换取新 token）
        String username = pm.getString(PreferenceManager.PREFERENCES_USER_NAME, "");
        String password = pm.getString(PreferenceManager.PREFERENCES_USER_PASSWORD, "");
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            try {
                DataSyncModels.LoginResponse resp = client.login(username, password);
                if (resp != null && resp.token != null) {
                    pm.putString(PreferenceManager.PREFERENCES_USER_TOCKEN, resp.token);
                    Log.d("DataSyncClient", "Auto re-login successful");
                    return resp.token;
                }
            } catch (DataSyncException e) {
                if (e.httpCode == 401) {
                    // 密码错误或用户不存在，清除凭据
                    Log.w("DataSyncClient", "Auto re-login failed (401), clearing credentials", e);
                    pm.putString(PreferenceManager.PREFERENCES_USER_PASSWORD, "");
                } else {
                    // 其他 HTTP 错误（500 等），不清除凭据
                    Log.w("DataSyncClient", "Auto re-login failed (HTTP " + e.httpCode + "), keeping credentials", e);
                }
                pm.putString(PreferenceManager.PREFERENCES_USER_TOCKEN, "");
                return null;
            } catch (Exception e) {
                // IOException（网络问题等），不清除凭据，保留 token + 密码等待下次尝试
                Log.w("DataSyncClient", "Auto re-login failed (network), keeping credentials for next retry", e);
                return null;
            }
        } else {
            // 没有保存密码，清除 token
            pm.putString(PreferenceManager.PREFERENCES_USER_TOCKEN, "");
            Log.w("DataSyncClient", "No saved credentials for auto re-login, clearing token");
        }
        return null;
    }
    

    // ==================== Comics ====================

    /**
     * 获取服务端该用户的所有漫画（支持增量拉取）
     * @param since 增量拉取起点(毫秒)，null 则全量拉取
     */
    public List<DataSyncModels.ComicServerItem> listComics(String token, Long since)
            throws IOException, DataSyncException {
        String path = "/api/comics";
        if (since != null && since > 0) {
            path += "?since=" + since;
        }
        String body = get(path, token);
        DataSyncModels.ComicListResponse resp = GSON.fromJson(body, DataSyncModels.ComicListResponse.class);
        return resp != null ? resp.comics : null;
    }

    /**
     * 获取服务端漫画列表 + 删除记录（增量同步用）
     */
    public DataSyncModels.ComicListResponse listComicsFull(String token, Long since)
            throws IOException, DataSyncException {
        String path = "/api/comics";
        if (since != null && since > 0) {
            path += "?since=" + since;
        }
        String body = get(path, token);
        return GSON.fromJson(body, DataSyncModels.ComicListResponse.class);
    }

    /**
     * 获取服务端该用户的所有漫画（全量，兼容旧调用）
     */
    public List<DataSyncModels.ComicServerItem> listComics(String token)
            throws IOException, DataSyncException {
        return listComics(token, null);
    }

    /**
     * 同步漫画到服务端（增量模式：仅推送变更）
     * @param since 上次同步时间(毫秒)，用于服务端过滤
     */
    public DataSyncModels.ComicSyncResponse syncComics(String token,
            List<DataSyncModels.ComicSyncItem> comics, Long since, boolean pushOnly)
            throws IOException, DataSyncException {
        DataSyncModels.ComicSyncRequest req = new DataSyncModels.ComicSyncRequest(comics, since, pushOnly);
        String json = GSON.toJson(req);
        String body = post("/api/comics/sync", json, token);
        return GSON.fromJson(body, DataSyncModels.ComicSyncResponse.class);
    }

    /**
     * 同步漫画到服务端（全量模式，兼容旧调用）
     */
    public DataSyncModels.ComicSyncResponse syncComics(String token, List<DataSyncModels.ComicSyncItem> comics)
            throws IOException, DataSyncException {
        return syncComics(token, comics, null, false);
    }

    /**
     * 删除服务端某条漫画记录
     */
    public void deleteComic(String token, long comicId)
            throws IOException, DataSyncException {
        delete("/api/comics/" + comicId, token);
    }

    // ==================== Event-based Sync ====================

    /**
     * 拉取事件：获取 since_id 之后的所有事件。
     * 注意：参数名使用 since_id，刻意与 GET /api/comics?since=（毫秒时间戳）区分开，
     * 避免未来维护者把两种 since 参数搞混。服务端同时兼容旧名 ?since= 一段时间。
     * @param sinceID 上次拉取到的事件ID，0 表示从头开始
     */
    public DataSyncModels.PullEventsResponse pullEvents(String token, long sinceID)
            throws IOException, DataSyncException {
        String path = "/api/events/pull?since_id=" + sinceID;
        String body = get(path, token);
        return GSON.fromJson(body, DataSyncModels.PullEventsResponse.class);
    }

    /**
     * 推送事件：将本地产生的事件发送到服务端
     */
    public void pushEvents(String token, List<DataSyncModels.SyncEvent> events, String clientId)
            throws IOException, DataSyncException {
        DataSyncModels.PushEventsRequest req = new DataSyncModels.PushEventsRequest(events, clientId);
        String json = GSON.toJson(req);
        post("/api/events/push", json, token);
    }

    /**
     * 获取事件流状态（最新事件ID等）
     */
    public DataSyncModels.EventStatusResponse getEventStatus(String token)
            throws IOException, DataSyncException {
        String body = get("/api/events/status", token);
        return GSON.fromJson(body, DataSyncModels.EventStatusResponse.class);
    }

    // ==================== Sync Status ====================

    /**
     * 获取同步状态（服务端时间、漫画数量等）
     */
    public DataSyncModels.SyncStatusResponse getSyncStatus(String token)
            throws IOException, DataSyncException {
        String body = get("/api/sync/status", token);
        return GSON.fromJson(body, DataSyncModels.SyncStatusResponse.class);
    }

    // ==================== Tags ====================

    /**
     * 获取服务端所有标签及关联
     */
    public List<DataSyncModels.TagServerItem> listTags(String token)
            throws IOException, DataSyncException {
        String body = get("/api/tags", token);
        DataSyncModels.TagListResponse resp = GSON.fromJson(body, DataSyncModels.TagListResponse.class);
        return resp != null ? resp.tags : null;
    }

    /**
     * 同步标签到服务端
     * @param partialUpdate true: 仅更新传了的标签，不删除未提及的；false: 全量替换
     */
    public DataSyncModels.TagSyncResponse syncTags(String token,
            List<DataSyncModels.TagSyncItem> tags, boolean partialUpdate)
            throws IOException, DataSyncException {
        DataSyncModels.TagSyncRequest req = new DataSyncModels.TagSyncRequest(tags, partialUpdate);
        String json = GSON.toJson(req);
        String body = post("/api/tags/sync", json, token);
        return GSON.fromJson(body, DataSyncModels.TagSyncResponse.class);
    }

    // ==================== Settings ====================

    /**
     * 获取服务端该用户的所有设置
     */
    public List<DataSyncModels.SettingServerItem> listSettings(String token)
            throws IOException, DataSyncException {
        String body = get("/api/settings", token);
        DataSyncModels.SettingListResponse resp = GSON.fromJson(body, DataSyncModels.SettingListResponse.class);
        return resp != null ? resp.settings : null;
    }

    /**
     * 同步设置到服务端
     */
    public DataSyncModels.SettingSyncResponse syncSettings(String token, List<DataSyncModels.SettingItem> settings)
            throws IOException, DataSyncException {
        DataSyncModels.SettingSyncRequest req = new DataSyncModels.SettingSyncRequest(settings);
        String json = GSON.toJson(req);
        String body = post("/api/settings/sync", json, token);
        return GSON.fromJson(body, DataSyncModels.SettingSyncResponse.class);
    }

    // ==================== HTTP helpers ====================

    private String get(String path, String token) throws IOException, DataSyncException {
        Request.Builder builder = new Request.Builder()
                .url(mBaseUrl + path)
                .get();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return execute(builder.build());
    }

    private String post(String path, String jsonBody) throws IOException, DataSyncException {
        return post(path, jsonBody, null);
    }

    private String post(String path, String jsonBody, String token) throws IOException, DataSyncException {
        Request.Builder builder = new Request.Builder()
                .url(mBaseUrl + path)
                .post(RequestBody.create(jsonBody, JSON));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return execute(builder.build());
    }

    private String delete(String path, String token) throws IOException, DataSyncException {
        Request.Builder builder = new Request.Builder()
                .url(mBaseUrl + path)
                .delete();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return execute(builder.build());
    }

    /**
     * 执行 HTTP 请求。若受保护接口返回 401（token 被服务端拒绝——过期、
     * 服务端重启换了密钥、密码被改导致 token_version 失效等），自动刷新
     * token 或自动重新登录，并用新 token 重试一次，避免用户手动登录。
     */
    private String execute(Request request) throws IOException, DataSyncException {
        // 认证接口本身（login/refresh）不做 401 重试，避免递归
        String path = request.url().encodedPath();
        if (path.startsWith("/api/auth/")) {
            return executeInternal(request);
        }

        try {
            return executeInternal(request);
        } catch (DataSyncException e) {
            if (e.httpCode != 401) {
                throw e;
            }
            // token 已被服务端拒绝 → 强制刷新或自动重登（不回退旧 token）
            String newToken = refreshOrRelogin();
            if (newToken == null) {
                throw e; // 无法自动恢复，维持原 401
            }
            Request retry = rebuildWithToken(request, newToken);
            if (retry == null) {
                throw e;
            }
            Log.d("DataSyncClient", "Got 401, refreshed token and retrying request: " + path);
            return executeInternal(retry);
        }
    }

    private String executeInternal(Request request) throws IOException, DataSyncException {
        Response response = mHttpClient.newCall(request).execute();
        String body = response.body() != null ? response.body().string() : "";

        if (!response.isSuccessful()) {
            String errorMsg = "请求失败: HTTP " + response.code();
            // 尝试解析服务端返回的错误信息
            if (!TextUtils.isEmpty(body)) {
                try {
                    DataSyncModels.ErrorResponse err = GSON.fromJson(body, DataSyncModels.ErrorResponse.class);
                    if (err != null && err.error != null) {
                        errorMsg = err.error;
                    }
                } catch (Exception ignored) {
                }
            }
            throw new DataSyncException(response.code(), errorMsg);
        }

        return body;
    }

    /**
     * 用新 token 重建请求（替换 Authorization 头）。
     * 若原请求没有 Authorization 头则返回 null（无需重试）。
     */
    private Request rebuildWithToken(Request request, String newToken) {
        if (request.header("Authorization") == null) {
            return null;
        }
        return request.newBuilder()
                .removeHeader("Authorization")
                .header("Authorization", "Bearer " + newToken)
                .build();
    }

    /**
     * 自定义异常，包含 HTTP 状态码。
     * 显式加 serialVersionUID：异常通过 RxJava/RxBus 跨层传递，若对象被写入持久化
     * 队列（崩溃日志/同步重放）后反序列化，缺该字段会抛 InvalidClassException。
     */
    public static class DataSyncException extends Exception implements Serializable {
        private static final long serialVersionUID = 1L;

        public final int httpCode;

        public DataSyncException(int httpCode, String message) {
            super(message);
            this.httpCode = httpCode;
        }
    }
}
