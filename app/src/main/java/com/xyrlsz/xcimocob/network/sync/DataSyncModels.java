package com.xyrlsz.xcimocob.network.sync;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 与 data_server (Go) 通信的请求/响应数据模型
 * 对应 data_server/models/ 中的 Go 结构体
 */
public class DataSyncModels {

    // ========== Auth ==========

    public static class LoginRequest {
        @SerializedName("username")
        public String username;
        @SerializedName("password")
        public String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class LoginResponse {
        @SerializedName("token")
        public String token;
        @SerializedName("user")
        public User user;
    }

    public static class User {
        @SerializedName("id")
        public long id;
        @SerializedName("username")
        public String username;
        @SerializedName("created_at")
        public String created_at;
        @SerializedName("updated_at")
        public String updated_at;
    }

    // ========== Comic ==========

    public static class ComicSyncItem {
        @SerializedName("source")
        public int source;
        @SerializedName("cid")
        public String cid;
        @SerializedName("title")
        public String title;
        @SerializedName("cover")
        public String cover;
        @SerializedName("update")
        public String update;
        @SerializedName("finish")
        public boolean finish;
        @SerializedName("favorite")
        public Long favorite;  // timestamp millis, null if not favorited
        @SerializedName("history")
        public Long history;   // timestamp millis, null if no history
        @SerializedName("last")
        public String last;
        @SerializedName("page")
        public Integer page;
        @SerializedName("chapter")
        public String chapter;
        @SerializedName("chapter_count")
        public Integer chapter_count;
        @SerializedName("clear_history")
        public boolean clear_history; // true 表示客户端要求清除历史记录
        @SerializedName("clear_favorite")
        public boolean clear_favorite; // true 表示客户端要求清除收藏

        public ComicSyncItem() {
        }
    }

    public static class ComicSyncRequest {
        @SerializedName("comics")
        public List<ComicSyncItem> comics;
        @SerializedName("since")
        public Long since;      // 客户端上次同步时间(毫秒)，增量同步用
        @SerializedName("push_only")
        public boolean push_only; // 仅推送变更，不返回全量

        public ComicSyncRequest(List<ComicSyncItem> comics) {
            this.comics = comics;
        }

        public ComicSyncRequest(List<ComicSyncItem> comics, Long since, boolean pushOnly) {
            this.comics = comics;
            this.since = since;
            this.push_only = pushOnly;
        }
    }

    public static class ComicSyncResponse {
        @SerializedName("synced")
        public int synced;
        @SerializedName("skipped")
        public int skipped;
        @SerializedName("message")
        public String message;
        @SerializedName("server_time")
        public long server_time;  // 服务器时间(毫秒)，客户端存储为 last_synced_at
        @SerializedName("comics")
        public List<ComicServerItem> comics;    // 全量模式时返回
        @SerializedName("deletes")
        public List<ComicDeleteItem> deletes;   // 其他设备的删除记录
        @SerializedName("has_more")
        public boolean has_more;
    }

    public static class ComicListResponse {
        @SerializedName("comics")
        public List<ComicServerItem> comics;
        @SerializedName("deletes")
        public List<ComicDeleteItem> deletes;
        @SerializedName("server_time")
        public long server_time;
    }

    public static class ComicServerItem {
        @SerializedName("id")
        public long id;
        @SerializedName("user_id")
        public long user_id;
        @SerializedName("source")
        public int source;
        @SerializedName("cid")
        public String cid;
        @SerializedName("title")
        public String title;
        @SerializedName("cover")
        public String cover;
        @SerializedName("update")
        public String update;
        @SerializedName("finish")
        public boolean finish;
        @SerializedName("favorite")
        public Long favorite;
        @SerializedName("history")
        public Long history;
        @SerializedName("last")
        public String last;
        @SerializedName("page")
        public Integer page;
        @SerializedName("chapter")
        public String chapter;
        @SerializedName("chapter_count")
        public Integer chapter_count;
        @SerializedName("created_at")
        public String created_at;
        @SerializedName("updated_at")
        public String updated_at;
    }

    /** 服务端记录的删除操作，用于多端传播 */
    public static class ComicDeleteItem {
        @SerializedName("source")
        public int source;
        @SerializedName("cid")
        public String cid;
        @SerializedName("delete_fav")
        public boolean delete_fav;
        @SerializedName("delete_his")
        public boolean delete_his;
    }

    // ========== Sync Status ==========

    public static class SyncStatusResponse {
        @SerializedName("server_time")
        public long server_time;
        @SerializedName("comic_count")
        public int comic_count;
    }

    // ========== Setting ==========

    public static class SettingItem {
        @SerializedName("key")
        public String key;
        @SerializedName("value")
        public String value;

        public SettingItem() {
        }

        public SettingItem(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class SettingSyncRequest {
        @SerializedName("settings")
        public List<SettingItem> settings;

        public SettingSyncRequest(List<SettingItem> settings) {
            this.settings = settings;
        }
    }

    public static class SettingSyncResponse {
        @SerializedName("synced")
        public int synced;
        @SerializedName("message")
        public String message;
    }

    public static class SettingListResponse {
        @SerializedName("settings")
        public List<SettingServerItem> settings;
    }

    public static class SettingServerItem {
        @SerializedName("id")
        public long id;
        @SerializedName("user_id")
        public long user_id;
        @SerializedName("key")
        public String key;
        @SerializedName("value")
        public String value;
        @SerializedName("created_at")
        public String created_at;
        @SerializedName("updated_at")
        public String updated_at;
    }

    // ========== Tag ==========

    public static class TagSyncRequest {
        @SerializedName("tags")
        public List<TagSyncItem> tags;
        @SerializedName("partial_update")
        public boolean partial_update;  // true 时只更新传了的标签，不删除服务端已有的

        public TagSyncRequest(List<TagSyncItem> tags, boolean partialUpdate) {
            this.tags = tags;
            this.partial_update = partialUpdate;
        }
    }

    public static class TagSyncItem {
        @SerializedName("title")
        public String title;
        @SerializedName("comics")
        public List<TagComicRef> comics;

        public TagSyncItem() {
        }

        public TagSyncItem(String title, List<TagComicRef> comics) {
            this.title = title;
            this.comics = comics;
        }
    }

    public static class TagComicRef {
        @SerializedName("source")
        public int source;
        @SerializedName("cid")
        public String cid;

        public TagComicRef() {
        }

        public TagComicRef(int source, String cid) {
            this.source = source;
            this.cid = cid;
        }
    }

    public static class TagListResponse {
        @SerializedName("tags")
        public List<TagServerItem> tags;
    }

    public static class TagServerItem {
        @SerializedName("id")
        public long id;
        @SerializedName("user_id")
        public long user_id;
        @SerializedName("title")
        public String title;
        @SerializedName("created_at")
        public String created_at;
        @SerializedName("updated_at")
        public String updated_at;
        @SerializedName("comics")
        public List<TagRefItem> comics;
    }

    public static class TagRefItem {
        @SerializedName("id")
        public long id;
        @SerializedName("user_id")
        public long user_id;
        @SerializedName("tag_id")
        public long tag_id;
        @SerializedName("source")
        public int source;
        @SerializedName("cid")
        public String cid;
    }

    public static class TagSyncResponse {
        @SerializedName("synced")
        public int synced;
        @SerializedName("synced_refs")
        public int synced_refs;
        @SerializedName("message")
        public String message;
    }

    // ========== Event-based Sync (核心同步) ==========

    /** 事件类型常量 */
    public static final String EVENT_FAVORITE       = "favorite";
    public static final String EVENT_UNFAVORITE     = "unfavorite";
    public static final String EVENT_READ           = "read";
    public static final String EVENT_CLEAR_HISTORY  = "clear_history";
    public static final String EVENT_UPDATE_INFO    = "update_info";
    public static final String EVENT_TAG_CREATE     = "tag_create";
    public static final String EVENT_TAG_DELETE     = "tag_delete";
    public static final String EVENT_TAG_ADD_COMIC  = "tag_add_comic";
    public static final String EVENT_TAG_REMOVE_COMIC = "tag_remove_comic";
    public static final String EVENT_SETTING_UPDATE = "setting_update";

    /** 同步事件（与 Go 服务端 SyncEvent 完全对齐） */
    public static class SyncEvent {
        @SerializedName("id")
        public long id;
        @SerializedName("user_id")
        public long user_id;
        @SerializedName("type")
        public String type;      // 事件类型
        @SerializedName("payload")
        public String payload;   // JSON payload
        @SerializedName("client_id")
        public String client_id; // 产生事件的设备ID
        @SerializedName("created_at")
        public String created_at;

        public SyncEvent() {}
        public SyncEvent(String type, String payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    public static class PushEventsRequest {
        @SerializedName("events")
        public List<SyncEvent> events;
        @SerializedName("client_id")
        public String client_id;

        public PushEventsRequest(List<SyncEvent> events, String clientId) {
            this.events = events;
            this.client_id = clientId;
        }
    }

    public static class PullEventsResponse {
        @SerializedName("events")
        public List<SyncEvent> events;
        @SerializedName("latest_id")
        public long latest_id;
        @SerializedName("has_more")
        public boolean has_more;
    }

    public static class EventStatusResponse {
        @SerializedName("latest_id")
        public long latest_id;
        @SerializedName("total_events")
        public long total_events;
        @SerializedName("comic_count")
        public long comic_count;
    }

    // ========== Error ==========

    public static class ErrorResponse {
        @SerializedName("error")
        public String error;
    }

    // ========== Health ==========

    public static class HealthResponse {
        @SerializedName("status")
        public String status;
        @SerializedName("service")
        public String service;
    }
}
