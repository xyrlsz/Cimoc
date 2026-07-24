package com.xyrlsz.xcimocob.network.sync;

import java.util.List;

/**
 * 与 data_server (Go) 通信的请求/响应数据模型
 * 对应 data_server/models/ 中的 Go 结构体
 */
public class DataSyncModels {

    // ========== Auth ==========

    public static class LoginRequest {
        public String username;
        public String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static class LoginResponse {
        public String token;
        public User user;
    }

    public static class User {
        public long id;
        public String username;
        public String created_at;
        public String updated_at;
    }

    // ========== Comic ==========

    public static class ComicSyncItem {
        public int source;
        public String cid;
        public String title;
        public String cover;
        public String update;
        public boolean finish;
        public Long favorite;  // timestamp millis, null if not favorited
        public Long history;   // timestamp millis, null if no history
        public String last;
        public Integer page;
        public String chapter;
        public Integer chapter_count;
        public boolean clear_history; // true 表示客户端要求清除历史记录
        public boolean clear_favorite; // true 表示客户端要求清除收藏

        public ComicSyncItem() {
        }
    }

    public static class ComicSyncRequest {
        public List<ComicSyncItem> comics;
        public Long since;      // 客户端上次同步时间(毫秒)，增量同步用
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
        public int synced;
        public int skipped;
        public String message;
        public long server_time;  // 服务器时间(毫秒)，客户端存储为 last_synced_at
        public List<ComicServerItem> comics;    // 全量模式时返回
        public List<ComicDeleteItem> deletes;   // 其他设备的删除记录
        public boolean has_more;
    }

    public static class ComicListResponse {
        public List<ComicServerItem> comics;
        public List<ComicDeleteItem> deletes;
        public long server_time;
    }

    public static class ComicServerItem {
        public long id;
        public long user_id;
        public int source;
        public String cid;
        public String title;
        public String cover;
        public String update;
        public boolean finish;

        public Long favorite;
        public Long history;
        public String last;
        public Integer page;
        public String chapter;
        public Integer chapter_count;
        public String created_at;
        public String updated_at;
    }

    /** 服务端记录的删除操作，用于多端传播 */
    public static class ComicDeleteItem {
        public int source;
        public String cid;
        public boolean delete_fav;
        public boolean delete_his;
    }

    // ========== Sync Status ==========

    public static class SyncStatusResponse {
        public long server_time;
        public int comic_count;
    }

    // ========== Setting ==========

    public static class SettingItem {
        public String key;
        public String value;

        public SettingItem() {
        }

        public SettingItem(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class SettingSyncRequest {
        public List<SettingItem> settings;

        public SettingSyncRequest(List<SettingItem> settings) {
            this.settings = settings;
        }
    }

    public static class SettingSyncResponse {
        public int synced;
        public String message;
    }

    public static class SettingListResponse {
        public List<SettingServerItem> settings;
    }

    public static class SettingServerItem {
        public long id;
        public long user_id;
        public String key;
        public String value;
        public String created_at;
        public String updated_at;
    }

    // ========== Tag ==========

    public static class TagSyncRequest {
        public List<TagSyncItem> tags;
        public boolean partial_update;  // true 时只更新传了的标签，不删除服务端已有的

        public TagSyncRequest(List<TagSyncItem> tags, boolean partialUpdate) {
            this.tags = tags;
            this.partial_update = partialUpdate;
        }
    }

    public static class TagSyncItem {
        public String title;
        public List<TagComicRef> comics;

        public TagSyncItem() {
        }

        public TagSyncItem(String title, List<TagComicRef> comics) {
            this.title = title;
            this.comics = comics;
        }
    }

    public static class TagComicRef {
        public int source;
        public String cid;

        public TagComicRef() {
        }

        public TagComicRef(int source, String cid) {
            this.source = source;
            this.cid = cid;
        }
    }

    public static class TagListResponse {
        public List<TagServerItem> tags;
    }

    public static class TagServerItem {
        public long id;
        public long user_id;
        public String title;
        public String created_at;
        public String updated_at;
        public List<TagRefItem> comics;
    }

    public static class TagRefItem {
        public long id;
        public long user_id;
        public long tag_id;
        public int source;
        public String cid;
    }

    public static class TagSyncResponse {
        public int synced;
        public int synced_refs;
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
        public long id;
        public long user_id;
        public String type;      // 事件类型
        public String payload;   // JSON payload
        public String client_id; // 产生事件的设备ID
        public String created_at;

        public SyncEvent() {}
        public SyncEvent(String type, String payload) {
            this.type = type;
            this.payload = payload;
        }
    }

    public static class PushEventsRequest {
        public List<SyncEvent> events;
        public String client_id;

        public PushEventsRequest(List<SyncEvent> events, String clientId) {
            this.events = events;
            this.client_id = clientId;
        }
    }

    public static class PullEventsResponse {
        public List<SyncEvent> events;
        public long latest_id;
        public boolean has_more;
    }

    public static class EventStatusResponse {
        public long latest_id;
        public long total_events;
        public long comic_count;
    }

    // ========== Error ==========

    public static class ErrorResponse {
        public String error;
    }

    // ========== Health ==========

    public static class HealthResponse {
        public String status;
        public String service;
    }
}
