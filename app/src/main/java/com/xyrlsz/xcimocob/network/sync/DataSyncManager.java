package com.xyrlsz.xcimocob.network.sync;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.manager.ComicManager;
import com.xyrlsz.xcimocob.manager.PreferenceManager;
import com.xyrlsz.xcimocob.model.Comic;
import com.xyrlsz.xcimocob.model.MiniComic;
import com.xyrlsz.xcimocob.rx.RxBus;
import com.xyrlsz.xcimocob.rx.RxEvent;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 自动数据同步管理器
 * <p>
 * 在后台静默同步数据到 data_server，无需用户手动操作。
 * - 监听 RxBus 数据变更事件（收藏、阅读、标签），防抖后自动同步
 * - 监听应用前后台切换，回到前台时自动同步全部数据
 * - 仅在开启自动同步、已登录、已配置服务器地址时执行
 * - 同步失败静默处理，不弹 Toast
 */
public class DataSyncManager {

    private static final String TAG = "DataSyncManager";
    private static final long DEBOUNCE_MS = 3000; // 3 秒防抖

    private static volatile DataSyncManager sInstance;

    private final CompositeDisposable mDisposable = new CompositeDisposable();
    private final ComicManager mComicManager;

    /**
     * 应用是否在前台
     */
    private final AtomicBoolean mIsForeground = new AtomicBoolean(false);

    /**
     * 防止多个同步触发源（冷启动、前台切换、防抖）并发执行同步
     */
    private final AtomicBoolean mSyncRunning = new AtomicBoolean(false);

    private DataSyncManager() {
        // 使用 App 实例作为 AppGetter（App 实现了 AppGetter 接口）
        mComicManager = ComicManager.getInstance(App.getApp());
    }

    public static DataSyncManager getInstance() {
        if (sInstance == null) {
            synchronized (DataSyncManager.class) {
                if (sInstance == null) {
                    sInstance = new DataSyncManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * 初始化：开始监听数据变更事件。
     * 在 App.onCreate() 中调用。
     */
    public void init() {
        if (mDisposable.size() > 0) {
            return; // 已经初始化
        }
        Log.d(TAG, "DataSyncManager initialized");
        listenDataChanges();
    }

    /**
     * 释放资源，停止监听
     */
    public void destroy() {
        mDisposable.clear();
    }

    // ==================== 生命周期 ====================

    /**
     * 应用首次启动时调用（一次性的，仅用于冷启动）。
     * 在 App.onCreate() 中 init() 完毕后调用。
     */
    public void onAppStart() {
        mIsForeground.set(true);
        Log.d(TAG, "App cold start -> try full bidirectional sync");
        trySyncNow();
    }

    /**
     * 应用进入前台时调用 → 全量双向同步
     */
    public void onForeground() {
        mIsForeground.set(true);
        Log.d(TAG, "App foreground -> try full bidirectional sync");
        trySyncNow();
    }

    /**
     * 立即触发一次全量双向同步（跳过防抖间隔检查）。
     * 用于登录/注册成功后主动同步。
     */
    public void triggerNow() {
        Log.d(TAG, "Trigger immediate full bidirectional sync");
        trySyncNow();
    }

    /**
     * 内部：无条件执行一次全量双向同步，更新计时器
     */
    private void trySyncNow() {
        if (!shouldSync()) return;
        mLastFullSync = System.currentTimeMillis();
        doSyncAllBidirectional();
    }

    /**
     * 应用进入后台时调用
     */
    public void onBackground() {
        mIsForeground.set(false);
    }

    // ==================== 事件监听（按数据类型拆分） ====================

    private void listenDataChanges() {
        // 收藏事件 → 从 MiniComic 加载完整 Comic 并构建事件后同步
        mDisposable.add(RxBus.getInstance().toObservable(RxEvent.EVENT_COMIC_FAVORITE)
                .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> {
                    Object data = e.getData();
                    if (data instanceof MiniComic) {
                        MiniComic mc = (MiniComic) data;
                        Comic comic = mComicManager.load(mc.getSource(), mc.getCid());
                        if (comic != null) {
                            enqueueComicEvent(comic, true, false, false, false);
                        }
                    }
                    triggerDebounced(SyncType.COMIC);
                }, t -> Log.w(TAG, "EVENT_COMIC_FAVORITE error", t)));

        // 取消收藏 → Presenter 已直接调用 enqueueClearFavoriteEvent，这里只需触发同步
        mDisposable.add(RxBus.getInstance().toObservable(RxEvent.EVENT_COMIC_UNFAVORITE)
                .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> triggerDebounced(SyncType.COMIC),
                        t -> Log.w(TAG, "EVENT_COMIC_UNFAVORITE error", t)));

        // 阅读进度事件
        mDisposable.add(RxBus.getInstance().toObservable(RxEvent.EVENT_COMIC_READ)
                .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> {
                    Object data = e.getData();
                    if (data instanceof MiniComic) {
                        MiniComic mc = (MiniComic) data;
                        Comic comic = mComicManager.load(mc.getSource(), mc.getCid());
                        if (comic != null) {
                            enqueueComicEvent(comic, false, false, true, false);
                        }
                    }
                    triggerDebounced(SyncType.COMIC);
                }, t -> Log.w(TAG, "EVENT_COMIC_READ error", t)));

        // 漫画信息更新事件（data 可能是 MiniComic 或 Long id）
        mDisposable.add(RxBus.getInstance().toObservable(RxEvent.EVENT_COMIC_UPDATE)
                .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> {
                    Comic comic = extractComicFromEvent(e);
                    if (comic != null) {
                        enqueueComicEvent(comic, false, false, false, true);
                    }
                    triggerDebounced(SyncType.COMIC);
                }, t -> Log.w(TAG, "EVENT_COMIC_UPDATE error", t)));

        // 漫画元信息更新事件（详情页解析完标题/封面/话数等）。
        // Manga.java 解析后只 post EVENT_COMIC_UPDATE_INFO（data 为完整 Comic），
        // 不会触发 EVENT_COMIC_UPDATE，必须单独监听，否则元信息变更无法同步到服务器。
        mDisposable.add(RxBus.getInstance().toObservable(RxEvent.EVENT_COMIC_UPDATE_INFO)
                .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(e -> {
                    Object data = e.getData();
                    if (data instanceof Comic) {
                        Comic eventComic = (Comic) data;
                        Comic local = mComicManager.load(eventComic.getSource(), eventComic.getCid());
                        // 仅当元信息确有变化时才入队，避免每次打开详情页都产生 update_info 事件
                        if (local == null || !sameMetadata(local, eventComic)) {
                            enqueueComicEvent(eventComic, false, false, false, true);
                        }
                    }
                    triggerDebounced(SyncType.COMIC);
                }, t -> Log.w(TAG, "EVENT_COMIC_UPDATE_INFO error", t)));

    }

    /**
     * 比较两本漫画的元信息（标题/封面/更新时间/是否完结/话数）是否相同。
     * 用于判断 EVENT_COMIC_UPDATE_INFO 是否真正产生了变化，避免重复入队。
     */
    private static boolean sameMetadata(Comic a, Comic b) {
        return Objects.equals(a.getTitle(), b.getTitle())
                && Objects.equals(a.getCover(), b.getCover())
                && Objects.equals(a.getUpdate(), b.getUpdate())
                && Objects.equals(a.getFinish(), b.getFinish())
                && Objects.equals(a.getChapterCount(), b.getChapterCount());
    }

    /**
     * 从 RxEvent 中提取 Comic 对象。
     * 兼容 MiniComic 和 Long id 两种数据类型。
     */
    @Nullable
    private Comic extractComicFromEvent(RxEvent e) {
        Object data = e.getData();
        if (data instanceof MiniComic) {
            MiniComic mc = (MiniComic) data;
            return mComicManager.load(mc.getSource(), mc.getCid());
        } else if (data instanceof Long) {
            return mComicManager.load((Long) data);
        }
        return null;
    }

    // ==================== 防抖触发 ====================

    private enum SyncType {COMIC, ALL}

    /**
     * 各类同步的上次执行时间
     */
    private long mLastComicSync = 0, mLastFullSync = 0;
    /**
     * 各类同步的最小间隔
     */
    private static final long INTERVAL_COMIC = 8_000;  // 漫画 8s
    private static final long INTERVAL_FULL = 60_000; // 全量 60s（前台触发）

    private synchronized void triggerDebounced(SyncType type) {
        if (!shouldSync()) return;

        long now = System.currentTimeMillis();
        switch (type) {
            case COMIC:
                if (now - mLastComicSync < INTERVAL_COMIC) return;
                mLastComicSync = now;
                doSyncComicsBidirectional();
                break;
            case ALL:
                if (now - mLastFullSync < INTERVAL_FULL) return;
                mLastFullSync = now;
                doSyncAllBidirectional();
                break;
        }
    }

    // ==================== 权限检查 ====================

    private boolean shouldSync() {
        if (!mIsForeground.get()) return false;
        PreferenceManager pm = App.getPreferenceManager();
        if (!pm.getBoolean(PreferenceManager.PREF_DATA_SERVER_AUTO_SYNC, true)) return false;
        String token = pm.getString(PreferenceManager.PREFERENCES_USER_TOCKEN, "");
        String url = pm.getString(PreferenceManager.PREF_DATA_SERVER_URL, "");
        return !TextUtils.isEmpty(token) && !TextUtils.isEmpty(url);
    }

    // ==================== 事件驱动同步 ====================

    /** 事件同步的持久化 key */
    public static final String PREF_LAST_EVENT_ID = "data_sync_last_event_id";
    public static final String PREF_CLIENT_ID = "data_sync_client_id";

    /** Gson 实例（用于序列化事件 payload） */
    private static final Gson GSON = new Gson();

    /**
     * 本设备发送的待推送事件缓冲（线程安全）。
     * 积累到一定数量或时间后批量推送。
     */
    private final List<DataSyncModels.SyncEvent> mPendingEvents = new CopyOnWriteArrayList<>();

    /** 获取或生成设备唯一标识 */
    private static String getClientId() {
        String clientId = App.getPreferenceManager().getString(PREF_CLIENT_ID, "");
        if (TextUtils.isEmpty(clientId)) {
            clientId = UUID.randomUUID().toString();
            App.getPreferenceManager().putString(PREF_CLIENT_ID, clientId);
        }
        return clientId;
    }

    /** 获取上次拉取到的事件 ID，0 表示从头开始 */
    private static long getLastEventId() {
        Number n = App.getPreferenceManager().getNumber(PREF_LAST_EVENT_ID, 0L);
        return n != null ? n.longValue() : 0L;
    }

    private static void setLastEventId(long eventId) {
        App.getPreferenceManager().putNumber(PREF_LAST_EVENT_ID, eventId);
    }

    // ==================== 事件构建 ====================

    /** 将本地漫画数据变更转为事件 */
    public void enqueueComicEvent(Comic comic, boolean isFavorite, boolean isUnfavorite,
                                   boolean isRead, boolean isUpdate) {
        DataSyncModels.SyncEvent event = null;

        if (isUnfavorite) {
            event = buildUnfavoriteEvent(comic);
        } else if (isFavorite && comic.getFavorite() != null) {
            event = buildFavoriteEvent(comic);
            // 重新收藏 → 清除"收藏已取消"标记，避免全量上传误发 clear_favorite 把数据清掉
            unmarkFavoriteDeleted(comic.getSource(), comic.getCid());
        } else if (isRead && comic.getHistory() != null) {
            event = buildReadEvent(comic);
            // 重新阅读/恢复历史 → 清除"历史已删除"标记
            unmarkHistoryDeleted(comic.getSource(), comic.getCid());
        } else if (isUpdate) {
            event = buildUpdateInfoEvent(comic);
        }

        if (event != null) {
            event.client_id = getClientId();
            mPendingEvents.add(event);
            Log.d(TAG, "[Event] Enqueued: " + event.type + " for " + comic.getSource() + ":" + comic.getCid());
        }
    }

    private DataSyncModels.SyncEvent buildFavoriteEvent(Comic c) {
        FavoritePayload p = new FavoritePayload();
        p.source = c.getSource();
        p.cid = c.getCid();
        p.title = c.getTitle();
        p.cover = c.getCover();
        p.update = c.getUpdate();
        p.finish = c.getFinish() != null && c.getFinish();
        p.chapter_count = c.getChapterCount();
        p.timestamp = c.getFavorite() != null ? c.getFavorite() : System.currentTimeMillis();
        return new DataSyncModels.SyncEvent(DataSyncModels.EVENT_FAVORITE, GSON.toJson(p));
    }

    private DataSyncModels.SyncEvent buildUnfavoriteEvent(Comic c) {
        UnfavoritePayload p = new UnfavoritePayload();
        p.source = c.getSource();
        p.cid = c.getCid();
        return new DataSyncModels.SyncEvent(DataSyncModels.EVENT_UNFAVORITE, GSON.toJson(p));
    }

    private DataSyncModels.SyncEvent buildReadEvent(Comic c) {
        ReadPayload p = new ReadPayload();
        p.source = c.getSource();
        p.cid = c.getCid();
        p.chapter = c.getChapter() != null ? c.getChapter() : "";
        p.page = c.getPage() != null ? c.getPage() : 0;
        p.last = c.getLast() != null ? c.getLast() : "";
        p.timestamp = c.getHistory() != null ? c.getHistory() : System.currentTimeMillis();
        return new DataSyncModels.SyncEvent(DataSyncModels.EVENT_READ, GSON.toJson(p));
    }

    private DataSyncModels.SyncEvent buildUpdateInfoEvent(Comic c) {
        UpdateInfoPayload p = new UpdateInfoPayload();
        p.source = c.getSource();
        p.cid = c.getCid();
        p.title = c.getTitle() != null ? c.getTitle() : "";
        p.cover = c.getCover() != null ? c.getCover() : "";
        p.update = c.getUpdate() != null ? c.getUpdate() : "";
        p.finish = c.getFinish() != null && c.getFinish();
        p.chapter_count = c.getChapterCount();
        return new DataSyncModels.SyncEvent(DataSyncModels.EVENT_UPDATE_INFO, GSON.toJson(p));
    }

    /**
     * 添加清除历史事件到待推送队列。
     * 由外部（如 HistoryPresenter）在清除历史时调用。
     * 同时记录"历史已删除"标记：事件若因进程被杀而丢失，
     * 下次全量上传仍会携带 clear_history 把删除传播到服务器。
     */
    public void enqueueClearHistoryEvent(int source, String cid) {
        ClearHistoryPayload p = new ClearHistoryPayload();
        p.source = source;
        p.cid = cid;
        DataSyncModels.SyncEvent event = new DataSyncModels.SyncEvent(
                DataSyncModels.EVENT_CLEAR_HISTORY, GSON.toJson(p));
        event.client_id = getClientId();
        mPendingEvents.add(event);
        markHistoryDeleted(source, cid);
        Log.d(TAG, "[Event] Enqueued clear_history for " + source + ":" + cid);
    }

    /**
     * 添加取消收藏事件到待推送队列。
     * 由外部（如 DetailPresenter、FavoritePresenter）在取消收藏时调用。
     * 同时记录"收藏已取消"标记：事件若因进程被杀而丢失，
     * 下次全量上传仍会携带 clear_favorite 把删除传播到服务器。
     */
    public void enqueueClearFavoriteEvent(int source, String cid) {
        UnfavoritePayload p = new UnfavoritePayload();
        p.source = source;
        p.cid = cid;
        DataSyncModels.SyncEvent event = new DataSyncModels.SyncEvent(
                DataSyncModels.EVENT_UNFAVORITE, GSON.toJson(p));
        event.client_id = getClientId();
        mPendingEvents.add(event);
        markFavoriteDeleted(source, cid);
        Log.d(TAG, "[Event] Enqueued unfavorite for " + source + ":" + cid);
    }

    // ==================== 漫画双向同步（事件驱动） ====================

    private void doSyncComicsBidirectional() {
        Observable.fromCallable(() -> {
            if (!mSyncRunning.compareAndSet(false, true)) {
                Log.d(TAG, "[EventSync] Sync already running, skip");
                return false;
            }
            try {
                Log.d(TAG, "[EventSync] Starting bidirectional event sync...");
                String token = DataSyncClient.ensureValidToken();
                if (token == null) {
                    Log.w(TAG, "[EventSync] Token is null, aborting sync");
                    return false;
                }
                if (!createClient()) {
                    Log.w(TAG, "[EventSync] Failed to create client, aborting sync");
                    return false;
                }

                // 1. 拉取远程事件并本地重放（其他设备的变化）
                pullAndReplayEvents(mClient, token);

                // 2. 推送本地积压的事件
                flushPendingEvents(mClient, token);

                Log.d(TAG, "[EventSync] Bidirectional event sync completed");
                return true;
            } finally {
                mSyncRunning.set(false);
            }
        }).subscribeOn(Schedulers.io()).subscribe(
                r -> Log.d(TAG, "[EventSync] Sync finished"),
                t -> Log.e(TAG, "[EventSync] Bidirectional event sync failed", t));
    }

    // ==================== 全量双向同步（事件驱动 + 设置，前台触发） ====================

    private void doSyncAllBidirectional() {
        Observable.fromCallable(() -> {
            if (!mSyncRunning.compareAndSet(false, true)) {
                Log.d(TAG, "[EventSync] Sync already running, skip");
                return false;
            }
            try {
                Log.d(TAG, "[EventSync] Starting full bidirectional event sync...");
                String token = DataSyncClient.ensureValidToken();
                if (token == null) {
                    Log.w(TAG, "[EventSync] Token is null, aborting full sync");
                    return false;
                }
                if (!createClient()) {
                    Log.w(TAG, "[EventSync] Failed to create client, aborting full sync");
                    return false;
                }

                // 1. 拉取远程事件并本地重放
                pullAndReplayEvents(mClient, token);

                // 2. 推送本地积压的事件
                flushPendingEvents(mClient, token);

                // 2.5 全量对账：从服务器拉取完整漫画数据合并到本地。
                // 事件流只覆盖通过 /api/events/push 上传的增量；手动全量同步
                // （/api/comics/sync）写入的数据不产生事件。只做事件重放会导致
                // 设备数据不完整，这里保证本地始终与服务器全量收敛。
                reconcileServerComics(mClient, token);

                // 2.6 全量上传本地漫画状态（时间戳合并）——保证服务器数据完整。
                // 事件队列仅在内存中，App 被杀或推送失败时会丢失；这里作为兜底，
                // 每次前台全量同步都把本地完整收藏/历史状态推送到服务器。
                pushLocalComicsState(mClient, token);

                // 3. 设置同步（保持原有机制）
                uploadSettings(mClient, token);
                downloadSettings(mClient, token);

                Log.d(TAG, "[EventSync] Full bidirectional event sync completed");
                return true;
            } finally {
                mSyncRunning.set(false);
            }
        }).subscribeOn(Schedulers.io()).subscribe(
                r -> Log.d(TAG, "[EventSync] Full sync finished"),
                t -> Log.e(TAG, "[EventSync] Full bidirectional event sync failed", t));
    }

    // ==================== 事件推送/拉取/重放 ====================

    @Nullable
    private DataSyncClient mClient;

    /** 创建或获取 DataSyncClient 实例（复用 OkHttpClient） */
    private boolean createClient() {
        String url = App.getPreferenceManager().getString(PreferenceManager.PREF_DATA_SERVER_URL, "");
        if (TextUtils.isEmpty(url)) return false;
        if (mClient == null || !mClient.isSameBaseUrl(url)) {
            mClient = new DataSyncClient(url);
        }
        return true;
    }

    /**
     * 从服务端拉取新事件并在本地重放。
     * 自动翻页处理 has_more 直到全部拉完。
     * 重放完成后发送 RxBus 事件通知 UI 刷新。
     */
    private void pullAndReplayEvents(DataSyncClient client, String token) throws Exception {
        long sinceID = getLastEventId();
        int totalReplayed = 0;
        int pageCount = 0;

        // 收集受影响的需要通知 UI 的漫画
        final List<MiniComic> favoriteChanged = new LinkedList<>();
        final List<MiniComic> historyChanged = new LinkedList<>();

        while (true) {
            DataSyncModels.PullEventsResponse resp = client.pullEvents(token, sinceID);
            if (resp == null || resp.events == null || resp.events.isEmpty()) {
                break;
            }

            pageCount++;
            for (DataSyncModels.SyncEvent event : resp.events) {
                ReplayResult result = replayEvent(event);
                if (result.replayed) {
                    totalReplayed++;
                }
                if (result.comic != null) {
                    if (result.favChanged) {
                        favoriteChanged.add(result.comic);
                    }
                    if (result.histChanged) {
                        historyChanged.add(result.comic);
                    }
                }
                sinceID = event.id; // 逐条推进，确保中断后能续传
            }

            if (!resp.has_more) {
                break;
            }
        }

        if (sinceID > getLastEventId()) {
            setLastEventId(sinceID);
        }

        Log.d(TAG, "[EventSync] Pulled " + totalReplayed + " events in " + pageCount
                + " pages, favChanged=" + favoriteChanged.size()
                + " histChanged=" + historyChanged.size()
                + ", latest_id=" + sinceID);

        // 通知 UI 刷新
        if (!favoriteChanged.isEmpty()) {
            RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_FAVORITE_RESTORE, favoriteChanged));
        }
        if (!historyChanged.isEmpty()) {
            RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_HISTORY_RESTORE, historyChanged));
        }
    }

    /**
     * 将本地积累的事件批量推送到服务端。
     */
    private void flushPendingEvents(DataSyncClient client, String token) throws Exception {
        if (mPendingEvents.isEmpty()) {
            return;
        }

        // 取出所有待推送事件（不提前清空：推送成功后才移除，失败则保留重试，
        // 避免网络异常导致本地事件永久丢失、服务器数据不完整）
        List<DataSyncModels.SyncEvent> batch = new ArrayList<>(mPendingEvents);

        Log.d(TAG, "[EventSync] Pushing " + batch.size() + " pending events");
        try {
            client.pushEvents(token, batch, getClientId());
        } catch (Exception e) {
            Log.w(TAG, "[EventSync] Push failed, keeping " + batch.size()
                    + " pending events for retry", e);
            throw e;
        }
        mPendingEvents.removeAll(batch);
        Log.d(TAG, "[EventSync] Pushed " + batch.size() + " events successfully");
    }

    /**
     * 全量对账：从服务器拉取完整漫画列表 + 删除记录，合并到本地数据库。
     * <p>
     * 事件流（/api/events/pull）只包含通过事件推送上传的增量；手动全量同步
     * （/api/comics/sync）直接写入服务器 Comic 表但不产生事件。因此只做事件
     * 重放会导致设备数据不完整。此方法在每次前台全量同步时执行，确保本地
     * 与服务器状态收敛：
     * - 服务器有、本地没有的漫画 → 创建
     * - 两边都有 → 按时间戳合并（取新）
     * - 服务器删除记录（ComicDelete 墓碑）→ 应用到本地（清除对应收藏/历史）
     */
    private void reconcileServerComics(DataSyncClient client, String token) throws Exception {
        DataSyncModels.ComicListResponse resp = client.listComicsFull(token, null);
        if (resp == null) {
            Log.d(TAG, "[Reconcile] Server returned null, skip");
            return;
        }
        boolean hasComics = resp.comics != null && !resp.comics.isEmpty();
        boolean hasDeletes = resp.deletes != null && !resp.deletes.isEmpty();
        if (!hasComics && !hasDeletes) {
            Log.d(TAG, "[Reconcile] Server has no comics and no deletes, nothing to merge");
            return;
        }

        final List<MiniComic> favoriteChanged = new LinkedList<>();
        final List<MiniComic> historyChanged = new LinkedList<>();
        final int[] created = {0};
        final int[] merged = {0};

        mComicManager.runInTx(() -> {
            if (resp.comics != null) {
                for (DataSyncModels.ComicServerItem item : resp.comics) {
                    Comic local = mComicManager.load(item.source, item.cid);
                    if (local == null) {
                        // 服务器有、本地没有 → 创建
                        local = createComicFromServer(item);
                        mComicManager.insert(local);
                        created[0]++;
                    } else {
                        // 两边都有 → 时间戳合并
                        if (mergeServerComic(local, item)) {
                            merged[0]++;
                        }
                    }
                    if (local.getFavorite() != null) {
                        favoriteChanged.add(new MiniComic(local));
                    }
                    if (local.getHistory() != null) {
                        historyChanged.add(new MiniComic(local));
                    }
                }
            }

            // 应用服务器删除记录（其他设备取消收藏/清除历史）。
            // 不设置本地删除标记：删除会通过事件路径 + 服务端墓碑传播，
            // 且本方法在全量上传之前执行（本地已清除 → 不会重新上传）。
            // 若在此设置标记，用户重新收藏后标记不会自动移除，
            // 会导致下次全量上传误发 clear_favorite 把重新收藏的数据清掉。
            if (resp.deletes != null) {
                for (DataSyncModels.ComicDeleteItem d : resp.deletes) {
                    if (d.delete_fav) {
                        Comic local = mComicManager.load(d.source, d.cid);
                        if (local != null && local.getFavorite() != null) {
                            local.setFavorite(null);
                            mComicManager.update(local);
                            favoriteChanged.add(new MiniComic(local));
                        }
                    }
                    if (d.delete_his) {
                        Comic local = mComicManager.load(d.source, d.cid);
                        if (local != null && local.getHistory() != null) {
                            local.setHistory(null);
                            local.setLast(null);
                            local.setPage(null);
                            local.setChapter(null);
                            mComicManager.update(local);
                            historyChanged.add(new MiniComic(local));
                        }
                    }
                }
            }
        });

        Log.d(TAG, "[Reconcile] Server comics=" + (hasComics ? resp.comics.size() : 0)
                + " deletes=" + (hasDeletes ? resp.deletes.size() : 0)
                + " created=" + created[0] + " merged=" + merged[0]
                + " favChanged=" + favoriteChanged.size()
                + " histChanged=" + historyChanged.size());

        // 通知 UI 刷新
        if (!favoriteChanged.isEmpty()) {
            RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_FAVORITE_RESTORE, favoriteChanged));
        }
        if (!historyChanged.isEmpty()) {
            RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_HISTORY_RESTORE, historyChanged));
        }
    }

    /**
     * 全量上传：将本地所有收藏/历史的漫画状态推送到服务器（时间戳合并）。
     * <p>
     * 事件流依赖内存队列，App 被杀或推送失败时可能丢失；此方法作为兜底，
     * 保证前台全量同步时服务器始终收敛到本地完整状态。删除标记（取消收藏/
     * 清除历史）会作为 clear_favorite / clear_history 一并上传。
     */
    private void pushLocalComicsState(DataSyncClient client, String token) throws Exception {
        List<Comic> comics = mComicManager.listFavoriteOrHistory();
        Set<String> historyDeleted = getHistoryDeletedKeysForUpload();
        Set<String> favoriteDeleted = getFavoriteDeletedKeysForUpload();

        // 本地既没有收藏/历史、也没有删除标记时才跳过。
        // 否则"取消了最后一个收藏"只剩删除标记的场景无法通过全量上传传播删除。
        if (comics.isEmpty() && historyDeleted.isEmpty() && favoriteDeleted.isEmpty()) {
            Log.d(TAG, "[PushAll] No local comics or delete marks to upload");
            return;
        }

        List<DataSyncModels.ComicSyncItem> items = new ArrayList<>(comics.size() + 8);
        Set<String> uploadedKeys = new HashSet<>();
        for (Comic comic : comics) {
            DataSyncModels.ComicSyncItem item = new DataSyncModels.ComicSyncItem();
            item.source = comic.getSource();
            item.cid = comic.getCid();
            item.title = comic.getTitle();
            item.cover = comic.getCover();
            item.update = comic.getUpdate();
            item.finish = comic.getFinish() != null && comic.getFinish();
            item.favorite = comic.getFavorite();
            item.history = comic.getHistory();
            item.last = comic.getLast();
            item.page = comic.getPage();
            item.chapter = comic.getChapter();
            item.chapter_count = comic.getChapterCount();
            items.add(item);
            uploadedKeys.add(comic.getSource() + ":" + comic.getCid());
        }

        // 附加"历史已删除"标记
        for (String key : historyDeleted) {
            String[] parts = key.split(":", 2);
            if (parts.length != 2) continue;
            int source;
            try {
                source = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                continue;
            }
            if (uploadedKeys.contains(key)) {
                for (DataSyncModels.ComicSyncItem item : items) {
                    if (item.source == source && item.cid != null && item.cid.equals(parts[1])) {
                        item.clear_history = true;
                        break;
                    }
                }
            } else {
                DataSyncModels.ComicSyncItem del = new DataSyncModels.ComicSyncItem();
                del.source = source;
                del.cid = parts[1];
                del.clear_history = true;
                items.add(del);
            }
        }

        // 附加"收藏已取消"标记
        for (String key : favoriteDeleted) {
            String[] parts = key.split(":", 2);
            if (parts.length != 2) continue;
            int source;
            try {
                source = Integer.parseInt(parts[0]);
            } catch (NumberFormatException e) {
                continue;
            }
            if (uploadedKeys.contains(key)) {
                for (DataSyncModels.ComicSyncItem item : items) {
                    if (item.source == source && item.cid != null && item.cid.equals(parts[1])) {
                        item.clear_favorite = true;
                        break;
                    }
                }
            } else {
                DataSyncModels.ComicSyncItem del = new DataSyncModels.ComicSyncItem();
                del.source = source;
                del.cid = parts[1];
                del.clear_favorite = true;
                items.add(del);
            }
        }

        Log.d(TAG, "[PushAll] Uploading " + items.size() + " comic items (full state)");
        client.syncComics(token, items, null, true);

        // 上传成功后清除删除标记（服务器已记录墓碑或已解决冲突）
        if (!historyDeleted.isEmpty()) clearHistoryDeletedKeysAfterUpload();
        if (!favoriteDeleted.isEmpty()) clearFavoriteDeletedKeysAfterUpload();
    }

    /**
     * 在本地重放一个远程事件（将事件应用到本地数据库）。
     * 所有操作都是幂等的 —— 重复重放同一事件不会产生副作用。
     *
     * @return ReplayResult 包含重放状态和受影响的漫画信息
     */
    private ReplayResult replayEvent(DataSyncModels.SyncEvent event) {
        // 跳过自己产生的事件
        if (getClientId().equals(event.client_id)) {
            return ReplayResult.SKIPPED;
        }

        try {
            switch (event.type) {
                case DataSyncModels.EVENT_FAVORITE:
                    return replayFavorite(GSON.fromJson(event.payload, FavoritePayload.class));
                case DataSyncModels.EVENT_UNFAVORITE:
                    return replayUnfavorite(GSON.fromJson(event.payload, UnfavoritePayload.class));
                case DataSyncModels.EVENT_READ:
                    return replayRead(GSON.fromJson(event.payload, ReadPayload.class));
                case DataSyncModels.EVENT_CLEAR_HISTORY:
                    return replayClearHistory(GSON.fromJson(event.payload, ClearHistoryPayload.class));
                case DataSyncModels.EVENT_UPDATE_INFO:
                    return replayUpdateInfo(GSON.fromJson(event.payload, UpdateInfoPayload.class));
                default:
                    Log.d(TAG, "[EventSync] Unknown event type: " + event.type);
                    return ReplayResult.SKIPPED;
            }
        } catch (Exception e) {
            Log.w(TAG, "[EventSync] Failed to replay event " + event.type, e);
            return ReplayResult.SKIPPED;
        }
    }

    /** 事件重放结果 */
    private static class ReplayResult {
        static final ReplayResult SKIPPED = new ReplayResult(false, null, false, false);

        final boolean replayed;
        @Nullable final MiniComic comic;
        final boolean favChanged;
        final boolean histChanged;

        ReplayResult(boolean replayed, @Nullable MiniComic comic, boolean favChanged, boolean histChanged) {
            this.replayed = replayed;
            this.comic = comic;
            this.favChanged = favChanged;
            this.histChanged = histChanged;
        }
    }

    // ==================== 事件重放实现（幂等） ====================

    private ReplayResult replayFavorite(FavoritePayload p) {
        if (p == null || p.cid == null) return ReplayResult.SKIPPED;
        Comic local = mComicManager.load(p.source, p.cid);
        if (local == null) {
            local = new Comic();
            local.setId(0);
            local.setSource(p.source);
            local.setCid(p.cid);
        }
        // 只接受更新的时间戳
        if (local.getFavorite() != null && local.getFavorite() >= p.timestamp) {
            return ReplayResult.SKIPPED; // 本地已有更新的数据
        }
        local.setFavorite(p.timestamp);
        if (p.title != null && !p.title.isEmpty()) {
            local.setTitle(p.title);
            local.setCover(p.cover);
            local.setUpdate(p.update);
            local.setFinish(p.finish);
            if (p.chapter_count != null) local.setChapterCount(p.chapter_count);
        }
        if (local.getId() == 0) {
            mComicManager.insert(local);
        } else {
            mComicManager.update(local);
        }
        Log.d(TAG, "[EventSync] Replay: favorite " + p.source + ":" + p.cid);
        return new ReplayResult(true, new MiniComic(local), true, false);
    }

    private ReplayResult replayUnfavorite(UnfavoritePayload p) {
        if (p == null || p.cid == null) return ReplayResult.SKIPPED;
        Comic local = mComicManager.load(p.source, p.cid);
        if (local == null || local.getFavorite() == null) return ReplayResult.SKIPPED;
        local.setFavorite(null);
        mComicManager.update(local);
        Log.d(TAG, "[EventSync] Replay: unfavorite " + p.source + ":" + p.cid);
        return new ReplayResult(true, new MiniComic(local), true, false);
    }

    private ReplayResult replayRead(ReadPayload p) {
        if (p == null || p.cid == null) return ReplayResult.SKIPPED;
        Comic local = mComicManager.load(p.source, p.cid);
        if (local == null) {
            local = new Comic();
            local.setId(0);
            local.setSource(p.source);
            local.setCid(p.cid);
        }
        // 只接受更新的阅读时间
        if (local.getHistory() != null && local.getHistory() >= p.timestamp) {
            return ReplayResult.SKIPPED;
        }
        local.setHistory(p.timestamp);
        local.setChapter(p.chapter);
        local.setPage(p.page);
        local.setLast(p.last);
        if (local.getId() == 0) {
            mComicManager.insert(local);
        } else {
            mComicManager.update(local);
        }
        Log.d(TAG, "[EventSync] Replay: read " + p.source + ":" + p.cid);
        return new ReplayResult(true, new MiniComic(local), false, true);
    }

    private ReplayResult replayClearHistory(ClearHistoryPayload p) {
        if (p == null || p.cid == null) return ReplayResult.SKIPPED;
        Comic local = mComicManager.load(p.source, p.cid);
        if (local == null || local.getHistory() == null) return ReplayResult.SKIPPED;
        local.setHistory(null);
        local.setLast(null);
        local.setPage(null);
        local.setChapter(null);
        mComicManager.update(local);
        Log.d(TAG, "[EventSync] Replay: clear_history " + p.source + ":" + p.cid);
        return new ReplayResult(true, new MiniComic(local), false, true);
    }

    private ReplayResult replayUpdateInfo(UpdateInfoPayload p) {
        if (p == null || p.cid == null) return ReplayResult.SKIPPED;
        Comic local = mComicManager.load(p.source, p.cid);
        if (local == null) return ReplayResult.SKIPPED;
        boolean changed = false;
        if (p.title != null && !p.title.equals(local.getTitle())) {
            local.setTitle(p.title); changed = true;
        }
        if (p.cover != null && !p.cover.equals(local.getCover())) {
            local.setCover(p.cover); changed = true;
        }
        if (p.update != null && !p.update.equals(local.getUpdate())) {
            local.setUpdate(p.update); changed = true;
        }
        if (local.getFinish() == null || local.getFinish() != p.finish) {
            local.setFinish(p.finish); changed = true;
        }
        if (p.chapter_count != null) {
            local.setChapterCount(p.chapter_count); changed = true;
        }
        if (changed) {
            mComicManager.update(local);
            Log.d(TAG, "[EventSync] Replay: update_info " + p.source + ":" + p.cid);
        }
        return new ReplayResult(changed, new MiniComic(local), false, false);
    }

    // ==================== 事件 Payload 内部类 ====================

    private static class FavoritePayload {
        int source;
        String cid;
        String title;
        String cover;
        String update;
        boolean finish;
        Integer chapter_count;
        long timestamp;
    }

    private static class UnfavoritePayload {
        int source;
        String cid;
    }

    private static class ReadPayload {
        int source;
        String cid;
        String chapter;
        int page;
        String last;
        long timestamp;
    }

    private static class ClearHistoryPayload {
        int source;
        String cid;
    }

    private static class UpdateInfoPayload {
        int source;
        String cid;
        String title;
        String cover;
        String update;
        boolean finish;
        Integer chapter_count;
    }

    /** 上传本地设置到服务端（只同步 SYNCABLE_SETTINGS 白名单内的用户设置） */
    private void uploadSettings(DataSyncClient client, String token) throws Exception {
        Map<String, ?> allPrefs = App.getPreferenceManager().getAll();
        List<DataSyncModels.SettingItem> settingItems = new ArrayList<>();
        for (Map.Entry<String, ?> e : allPrefs.entrySet()) {
            if (e.getValue() != null && PreferenceManager.SYNCABLE_SETTINGS.contains(e.getKey())) {
                settingItems.add(new DataSyncModels.SettingItem(e.getKey(), e.getValue().toString()));
            }
        }
        client.syncSettings(token, settingItems);
    }

    /** 从服务端下载设置并合并到本地（只应用白名单内的用户设置，避免覆盖设备本地状态） */
    private void downloadSettings(DataSyncClient client, String token) throws Exception {
        List<DataSyncModels.SettingServerItem> serverSettings = client.listSettings(token);
        if (serverSettings == null) return;

        PreferenceManager pm = App.getPreferenceManager();
        for (DataSyncModels.SettingServerItem s : serverSettings) {
            if (s.key != null && s.value != null && PreferenceManager.SYNCABLE_SETTINGS.contains(s.key)) {
                pm.putObject(s.key, s.value);
            }
        }
    }

    // ==================== 标签同步 ====================

    /** 上传本地标签到服务端 */
    private void uploadTags(DataSyncClient client, String token) throws Exception {
        // TODO: 需要 TagManager 提供 listAllTags 方法获取所有本地标签
        // 当前先记录日志，后续接入 TagManager
        Log.d(TAG, "[Sync] Tag upload: TagManager integration pending");
        // 示例调用:
        // TagManager tm = TagManager.getInstance(App.getApp());
        // List<Tag> tags = tm.listAll();
        // ... 构建 TagSyncItem 列表 ...
        // client.syncTags(token, items, false);
    }

    /** 从服务端下载标签并合并到本地 */
    private void downloadAndMergeTags(DataSyncClient client, String token) throws Exception {
        List<DataSyncModels.TagServerItem> serverTags = client.listTags(token);
        if (serverTags == null || serverTags.isEmpty()) {
            Log.d(TAG, "[Sync] Tag download: server returned empty");
            return;
        }
        Log.d(TAG, "[Sync] Tag download: " + serverTags.size() + " tags from server");

        // TODO: 需要 TagManager 和 TagRefManager 的写入方法
        // 当前先记录日志
        Log.d(TAG, "[Sync] Tag merge: TagManager integration pending");
        // 示例逻辑:
        // TagManager tm = TagManager.getInstance(App.getApp());
        // for (TagServerItem s : serverTags) {
        //     Tag local = tm.findByTitle(s.title);
        //     if (local == null) { tm.insert(...); }
        //     // 合并 refs...
        // }
    }

    // ==================== 删除标记解析 ====================

    /**
     * 服务端已记录删除，从本地删除标记集合中移除
     */
    private static void markFavoriteDeletedResolved(int source, String cid) {
        Set<String> keys = getFavoriteDeletedKeys();
        if (keys.remove(source + ":" + cid)) {
            saveFavoriteDeletedKeys(keys);
        }
    }

    private static void markHistoryDeletedResolved(int source, String cid) {
        Set<String> keys = getHistoryDeletedKeys();
        if (keys.remove(source + ":" + cid)) {
            saveHistoryDeletedKeys(keys);
        }
    }

    /** 构建漫画同步项 */
    private static DataSyncModels.ComicSyncItem buildComicSyncItem(Comic c) {
        DataSyncModels.ComicSyncItem item = new DataSyncModels.ComicSyncItem();
        item.source = c.getSource();
        item.cid = c.getCid();
        item.title = c.getTitle();
        item.cover = c.getCover();
        item.update = c.getUpdate();
        item.finish = c.getFinish() != null && c.getFinish();
        item.favorite = c.getFavorite();
        item.history = c.getHistory();
        item.last = c.getLast();
        item.page = c.getPage();
        item.chapter = c.getChapter();
        item.chapter_count = c.getChapterCount();
        return item;
    }

    /** 从服务端数据创建新 Comic */
    private static Comic createComicFromServer(DataSyncModels.ComicServerItem s) {
        Comic local = new Comic();
        local.setId(0);
        local.setSource(s.source);
        local.setCid(s.cid);
        local.setTitle(s.title);
        local.setCover(s.cover);
        local.setUpdate(s.update);
        local.setFinish(s.finish);
        local.setFavorite(s.favorite);
        local.setHistory(s.history);
        local.setLast(s.last);
        local.setPage(s.page);
        local.setChapter(s.chapter);
        if (s.chapter_count != null) local.setChapterCount(s.chapter_count);
        return local;
    }

    /** 将服务端漫画数据合并到本地（服务端数据优先） */
    private boolean mergeServerComic(Comic local, DataSyncModels.ComicServerItem s) {
        boolean changed = false;
        String key = s.source + ":" + s.cid;
        // 如果本地明确标记了"收藏已取消"，则不从服务端恢复收藏
        boolean favoriteDeleted = isFavoriteDeleted(s.source, s.cid);
        if (favoriteDeleted) {
            // 本地已取消收藏 → 保持本地 null，不恢复
            Log.d(TAG, "[Sync] Merge " + key + ": skip restore favorite (local deleted)");
        } else if (s.favorite != null) {
            // 服务端有收藏 → 取较新的
            if (local.getFavorite() == null || s.favorite > local.getFavorite()) {
                local.setFavorite(s.favorite);
                changed = true;
                Log.d(TAG, "[Sync] Merge " + key + ": update favorite to " + s.favorite);
            }
        } else if (local.getFavorite() != null) {
            // 服务端收藏为 null（另一台设备取消了收藏）→ 同步清除本地
            local.setFavorite(null);
            changed = true;
            Log.d(TAG, "[Sync] Merge " + key + ": clear favorite (server has null)");
        }
        // 如果本地明确标记了"历史已删除"，则不从服务端恢复历史
        boolean historyDeleted = isHistoryDeleted(s.source, s.cid);
        if (historyDeleted) {
            // 本地已删除历史 → 保持本地 null，不恢复
            Log.d(TAG, "[Sync] Merge " + key + ": skip restore history (local deleted)");
        } else if (s.history != null) {
            // 服务端有历史 → 取较新的
            if (local.getHistory() == null || s.history > local.getHistory()) {
                local.setHistory(s.history);
                local.setLast(s.last);
                local.setPage(s.page);
                local.setChapter(s.chapter);
                changed = true;
                Log.d(TAG, "[Sync] Merge " + key + ": update history to " + s.history);
            }
        } else if (local.getHistory() != null) {
            // 服务端历史为 null（另一台设备清除了历史）→ 同步清除本地
            local.setHistory(null);
            local.setLast(null);
            local.setPage(null);
            local.setChapter(null);
            changed = true;
            Log.d(TAG, "[Sync] Merge " + key + ": clear history (server has null)");
        }
        if ((local.getTitle() == null || local.getTitle().isEmpty()) && s.title != null) {
            local.setTitle(s.title);
            local.setCover(s.cover);
            local.setUpdate(s.update);
            local.setFinish(s.finish);
            if (s.chapter_count != null) local.setChapterCount(s.chapter_count);
            changed = true;
        }
        if (changed) {
            mComicManager.update(local);
            Log.d(TAG, "[Sync] Merge " + key + ": saved changes to local DB");
        }
        return changed;
    }

    // ==================== 历史删除标记追踪 ====================

    public static final String PREF_HISTORY_DELETED_KEYS = "history_deleted_keys";
    public static final String PREF_FAVORITE_DELETED_KEYS = "favorite_deleted_keys";

    /**
     * 添加一条"历史已删除"的漫画标记（source:cid）
     */
    public static void markHistoryDeleted(int source, String cid) {
        Set<String> keys = getHistoryDeletedKeys();
        keys.add(source + ":" + cid);
        saveHistoryDeletedKeys(keys);
    }

    /**
     * 移除某漫画的"历史已删除"标记（重新阅读/恢复历史时调用）
     */
    public static void unmarkHistoryDeleted(int source, String cid) {
        Set<String> keys = getHistoryDeletedKeys();
        if (keys.remove(source + ":" + cid)) {
            saveHistoryDeletedKeys(keys);
        }
    }

    /**
     * 批量添加"历史已删除"标记
     */
    public static void markHistoryDeleted(List<Comic> comics) {
        Set<String> keys = getHistoryDeletedKeys();
        for (Comic c : comics) {
            keys.add(c.getSource() + ":" + c.getCid());
        }
        saveHistoryDeletedKeys(keys);
    }

    /**
     * 检查某漫画是否被标记为"历史已删除"
     */
    public static boolean isHistoryDeleted(int source, String cid) {
        return getHistoryDeletedKeys().contains(source + ":" + cid);
    }

    /**
     * 获取所有"历史已删除"标记（供 BackupPresenter 上传使用）
     */
    public static Set<String> getHistoryDeletedKeysForUpload() {
        return getHistoryDeletedKeys();
    }

    /**
     * 获取所有"历史已删除"标记
     */
    private static Set<String> getHistoryDeletedKeys() {
        String json = App.getPreferenceManager().getString(PREF_HISTORY_DELETED_KEYS, "");
        if (json.isEmpty()) return new HashSet<>();
        Set<String> result = new HashSet<>();
        // 简单格式：逗号分隔，例如 "1:abc,2:def"
        for (String key : json.split(",")) {
            String trimmed = key.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * 持久化"历史已删除"标记集合
     */
    private static void saveHistoryDeletedKeys(Set<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (sb.length() > 0) sb.append(",");
            sb.append(key);
        }
        App.getPreferenceManager().putString(PREF_HISTORY_DELETED_KEYS, sb.toString());
    }

    /**
     * 清除所有"历史已删除"标记（上传成功后调用）
     */
    private static void clearHistoryDeletedKeys() {
        App.getPreferenceManager().putString(PREF_HISTORY_DELETED_KEYS, "");
    }

    /**
     * 供 BackupPresenter 上传成功后调用的公开清除方法
     */
    public static void clearHistoryDeletedKeysAfterUpload() {
        clearHistoryDeletedKeys();
    }

    /**
     * 获取所有"收藏已取消"标记（供 BackupPresenter 上传使用）
     */
    public static Set<String> getFavoriteDeletedKeysForUpload() {
        return getFavoriteDeletedKeys();
    }

    /**
     * 供 BackupPresenter 上传成功后调用的公开清除方法
     */
    public static void clearFavoriteDeletedKeysAfterUpload() {
        clearFavoriteDeletedKeys();
    }

    // ==================== 收藏删除标记追踪 ====================

    /**
     * 添加一条"收藏已取消"的漫画标记（source:cid）
     */
    public static void markFavoriteDeleted(int source, String cid) {
        Set<String> keys = getFavoriteDeletedKeys();
        keys.add(source + ":" + cid);
        saveFavoriteDeletedKeys(keys);
    }

    /**
     * 移除某漫画的"收藏已取消"标记（重新收藏时调用）
     */
    public static void unmarkFavoriteDeleted(int source, String cid) {
        Set<String> keys = getFavoriteDeletedKeys();
        if (keys.remove(source + ":" + cid)) {
            saveFavoriteDeletedKeys(keys);
        }
    }

    /**
     * 检查某漫画是否被标记为"收藏已取消"
     */
    public static boolean isFavoriteDeleted(int source, String cid) {
        return getFavoriteDeletedKeys().contains(source + ":" + cid);
    }

    /**
     * 获取所有"收藏已取消"标记
     */
    private static Set<String> getFavoriteDeletedKeys() {
        String json = App.getPreferenceManager().getString(PREF_FAVORITE_DELETED_KEYS, "");
        if (json.isEmpty()) return new HashSet<>();
        Set<String> result = new HashSet<>();
        for (String key : json.split(",")) {
            String trimmed = key.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * 持久化"收藏已取消"标记集合
     */
    private static void saveFavoriteDeletedKeys(Set<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (sb.length() > 0) sb.append(",");
            sb.append(key);
        }
        App.getPreferenceManager().putString(PREF_FAVORITE_DELETED_KEYS, sb.toString());
    }

    /**
     * 清除所有"收藏已取消"标记（上传成功后调用）
     */
    private static void clearFavoriteDeletedKeys() {
        App.getPreferenceManager().putString(PREF_FAVORITE_DELETED_KEYS, "");
    }

}
