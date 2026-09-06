package com.xyrlsz.xcimocob.service;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;

import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.component.AppGetter;
import com.xyrlsz.xcimocob.core.Download;
import com.xyrlsz.xcimocob.core.Manga;
import com.xyrlsz.xcimocob.global.Extra;
import com.xyrlsz.xcimocob.manager.ChapterManager;
import com.xyrlsz.xcimocob.manager.ComicManager;
import com.xyrlsz.xcimocob.manager.PreferenceManager;
import com.xyrlsz.xcimocob.manager.SourceManager;
import com.xyrlsz.xcimocob.manager.TaskManager;
import com.xyrlsz.xcimocob.misc.NotificationWrapper;
import com.xyrlsz.xcimocob.model.Chapter;
import com.xyrlsz.xcimocob.model.Comic;
import com.xyrlsz.xcimocob.model.ImageUrl;
import com.xyrlsz.xcimocob.model.Task;
import com.xyrlsz.xcimocob.parser.MangaParser;
import com.xyrlsz.xcimocob.rx.RxBus;
import com.xyrlsz.xcimocob.rx.RxEvent;
import com.xyrlsz.xcimocob.saf.CimocDocumentFile;
import com.xyrlsz.xcimocob.utils.DocumentUtils;
import com.xyrlsz.xcimocob.utils.FrescoUtils;
import com.xyrlsz.xcimocob.utils.IdCreator;
import com.xyrlsz.xcimocob.utils.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Hiroshi on 2016/9/1.
 */
public class DownloadService extends Service implements AppGetter {

    private static final String NOTIFICATION_DOWNLOAD = "NOTIFICATION_DOWNLOAD";
    private static final int MAX_PARALLEL_PAGE_DOWNLOADS = 3;
    private static final long NOTIFICATION_UPDATE_INTERVAL_MS = 1000L;

    private static boolean sRunning;

    private LongSparseArray<Pair<Worker, Future>> mWorkerArray;
    private ExecutorService mExecutorService;
    private OkHttpClient mHttpClient;
    private NotificationWrapper mNotification;
    private TaskManager mTaskManager;
    private SourceManager mSourceManager;
    private ComicManager mComicManager;
    private ChapterManager mChapterManager;
    private ContentResolver mContentResolver;
    private long mLastNotificationUpdate;

    public static Intent createIntent(Context context, Task task) {
        ArrayList<Task> list = new ArrayList<>(1);
        list.add(task);
        return createIntent(context, list);
    }

    public static Intent createIntent(Context context, ArrayList<Task> list) {
        Intent intent = new Intent(context, DownloadService.class);
        long[] ids = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            ids[i] = list.get(i).getId();
        }
        intent.putExtra(Extra.EXTRA_TASK_IDS, ids);
        return intent;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new DownloadServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sRunning = true;
//        getApplication();
        PreferenceManager manager = App.getPreferenceManager();
        int num = manager.getNumber(PreferenceManager.PREF_DOWNLOAD_THREAD, 2).intValue();
        if (num <= 0) {
            num = 1;
        }
        mWorkerArray = new LongSparseArray<>();
        mExecutorService = Executors.newFixedThreadPool(num);
        mHttpClient = App.getHttpClient();
        mTaskManager = TaskManager.getInstance(this);
        mSourceManager = SourceManager.getInstance(this);
        mContentResolver = getContentResolver();
        mChapterManager = ChapterManager.getInstance(this);
        mComicManager = ComicManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_DOWNLOAD_START));
            if (mNotification == null) {
                mNotification = new NotificationWrapper(this, NOTIFICATION_DOWNLOAD,
                        R.drawable.ic_file_download_white_24dp, true);
                mNotification.postIndeterminate(getString(R.string.download_service_doing), true);
                try {
                    startForeground(NOTIFICATION_DOWNLOAD.hashCode(), mNotification.getNotification());
                } catch (Exception e) {
                    Log.e("DownloadService", "startForeground failed", e);
                    // 如果前台服务启动失败，仍然继续执行下载任务
                }
            }
            long[] ids = intent.getLongArrayExtra(Extra.EXTRA_TASK_IDS);
            List<Task> list = ids == null ? new ArrayList<>() : mTaskManager.listByIds(ids);
            for (Task task : list) {
                Comic comic = mComicManager.load(task.getKey());
                if (comic == null) {
                    continue;
                }
                task.setSource(comic.getSource());
                task.setCid(comic.getCid());
                Worker worker = new Worker(task);
                Future future = mExecutorService.submit(worker);
                addWorker(task.getId(), worker, future);
            }
            // 更新下载通知
            updateNotification();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sRunning = false;
        if (mNotification != null) {
            mExecutorService.shutdownNow();
            notifyCompleted();
        }
    }

    public static boolean isRunning() {
        return sRunning;
    }

    @Override
    public App getAppInstance() {
        return App.getApp();
    }

    public synchronized void addWorker(long id, Worker worker, Future future) {
        if (mWorkerArray.get(id) == null) {
            mWorkerArray.put(id, Pair.create(worker, future));
        }
    }

    public synchronized void removeDownload(long id) {
        Pair<Worker, Future> pair = mWorkerArray.get(id);
        if (pair != null) {
            pair.second.cancel(true);
            mWorkerArray.remove(id);
        }
    }

    public synchronized void completeDownload(long id) {
        mWorkerArray.remove(id);
        if (mWorkerArray.isEmpty()) {
            notifyCompleted();
            stopSelf();
        } else {
            // 还有剩余任务，刷新通知
            updateNotification();
        }
    }

    private void notifyCompleted() {
        if (mNotification != null) {
            mNotification.post(getString(R.string.download_service_done), false);
            try {
                stopForeground(true);
            } catch (Exception e) {
                Log.e("DownloadService", "stopForeground failed", e);
            }
            mNotification.cancel();
            mNotification = null;
        }
        mWorkerArray.clear();
        RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_DOWNLOAD_STOP));
    }

    /**
     * 更新下载通知：仅显示"正在下载漫画"，不显示进度
     */
    public synchronized void updateNotification() {
        if (mNotification == null) return;
        long now = System.currentTimeMillis();
        if (now - mLastNotificationUpdate < NOTIFICATION_UPDATE_INTERVAL_MS) {
            return;
        }
        mLastNotificationUpdate = now;
        try {
            mNotification.post(getString(R.string.download_service_doing), true);
        } catch (Exception e) {
            Log.e("DownloadService", "updateNotification failed", e);
        }
    }

    public synchronized void initTask(List<Task> list) {
        for (Task task : list) {
            Pair<Worker, Future> pair = mWorkerArray.get(task.getId());
            if (pair != null) {
                task.setState(pair.first.mTask.getState());
            }
        }
    }

    public class Worker implements Runnable {

        private final Task mTask;
        private final MangaParser mParse;

        Worker(Task task) {
            mTask = task;
            mParse = mSourceManager.getParser(task.getSource());
        }

        /**
         * 下载单页图片的内部任务，支持并行提交到线程池
         */
        private class PageDownloadTask implements Callable<Boolean> {
            private final ImageUrl mImage;
            private final int mPageIndex; // 1-based
            private final CimocDocumentFile mDir;

            PageDownloadTask(ImageUrl image, int pageIndex, CimocDocumentFile dir) {
                this.mImage = image;
                this.mPageIndex = pageIndex;
                this.mDir = dir;
            }

            @Override
            public Boolean call() {
                int retryCount = 0;
                while (retryCount++ < 20) {
                    if (Thread.currentThread().isInterrupted()) {
                        return false;
                    }
                    List<String> urls = mImage.getUrls();
                    for (int j = 0; j < urls.size(); j++) {
                        String url;
                        try {
                            url = mImage.isLazy()
                                    ? Manga.getLazyUrl(mParse, urls.get(j))
                                    : urls.get(j);
                        } catch (InterruptedIOException e) {
                            return false;
                        }
                        try {
                            if (GetCacheAndWrite(mDir, mPageIndex, url)) {
                                return true;
                            }
                            Headers imgHeaders = mImage.getHeaders();
                            Request request = buildRequest(
                                    imgHeaders == null ? mParse.getHeader(url) : imgHeaders, url);
                            if (RequestAndWrite(mDir, request, mPageIndex, url)) {
                                return true;
                            }
                        } catch (InterruptedIOException e) {
                            return false;
                        } catch (Exception e) {
                            // 重试
                        }
                    }
                }
                return false;
            }
        }

        @Override
        public void run() {
            try {
                List<ImageUrl> list = onDownloadParse();
                int size = list.size();
                if (size != 0) {
                    CimocDocumentFile dir = Download.updateChapterIndex(
                            mContentResolver, getAppInstance().getDocumentFile(), mTask);
                    if (dir != null) {
                        mTask.setMax(size);
                        mTask.setState(Task.STATE_DOING);
                        // 更新通知
                        updateNotification();

                        // 根据 CPU 核心数和文件系统性能确定页级并发数
                        int parallelPages = MAX_PARALLEL_PAGE_DOWNLOADS;

                        // 从断点开始下载
                        int startPage = mTask.getProgress();
                        int remaining = size - startPage;

                        if (remaining <= 1) {
                            // 只有一页或已下载完，退化为串行
                            boolean success = true;
                            for (int i = startPage; i < size; ++i) {
                                onDownloadProgress(i);
                                PageDownloadTask task = new PageDownloadTask(list.get(i), i + 1, dir);
                                Boolean result = task.call();
                                if (!result) {
                                    RxBus.getInstance().post(new RxEvent(
                                            RxEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_ERROR, mTask.getId()));
                                    success = false;
                                    break;
                                }
                            }
                            if (success) {
                                onDownloadProgress(size);
                            }
                        } else {
                            // 并行下载多页
                            ExecutorService pageExecutor = Executors.newFixedThreadPool(parallelPages,
                                    r -> {
                                        Thread t = new Thread(r, "xcimoc-page-dl-"
                                                + mTask.getId());
                                        t.setPriority(Thread.NORM_PRIORITY);
                                        return t;
                                    });
                            try {
                                ExecutorCompletionService<Boolean> ecs =
                                        new ExecutorCompletionService<>(pageExecutor);
                                ConcurrentHashMap<Integer, Boolean> results =
                                        new ConcurrentHashMap<>();

                                // 提交所有剩余页面任务
                                int submitted = 0;
                                for (int i = startPage; i < size; ++i) {
                                    results.put(i, false);
                                    ecs.submit(new PageDownloadTask(list.get(i), i + 1, dir));
                                    submitted++;
                                }

                                // 收集结果，每次完成一个页面就更新进度
                                boolean allSuccess = true;
                                int completed = 0;
                                for (int i = 0; i < submitted; i++) {
                                    try {
                                        Future<Boolean> future = ecs.take();
                                        Boolean pageResult = future.get();
                                        if (!pageResult) {
                                            allSuccess = false;
                                            // 取消剩余未完成的任务
                                            pageExecutor.shutdownNow();
                                            break;
                                        }
                                        completed++;
                                        // 更新进度：已完成页数 = startPage + completed
                                        onDownloadProgress(startPage + completed);
                                    } catch (InterruptedException | ExecutionException e) {
                                        allSuccess = false;
                                        pageExecutor.shutdownNow();
                                        break;
                                    }
                                }

                                if (!allSuccess) {
                                    RxBus.getInstance().post(new RxEvent(
                                            RxEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_ERROR, mTask.getId()));
                                } else {
                                    onDownloadProgress(size);
                                }
                            } finally {
                                if (!pageExecutor.isShutdown()) {
                                    pageExecutor.shutdownNow();
                                }
                            }
                        }
                    } else {
                        RxBus.getInstance().post(new RxEvent(
                                RxEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_ERROR, mTask.getId()));
                    }
                } else {
                    RxBus.getInstance().post(new RxEvent(
                            RxEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_ERROR, mTask.getId()));
                }
            } catch (InterruptedIOException e) {
                RxBus.getInstance().post(new RxEvent(
                        RxEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_PAUSE, mTask.getId()));
            } catch (Exception e) {
                RxBus.getInstance().post(new RxEvent(
                        RxEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_ERROR, mTask.getId()));
            }

            completeDownload(mTask.getId());
            Comic comic = mComicManager.load(mTask.getSource(), mTask.getCid());
            if (comic != null) {
                Long sourceComic = IdCreator.createSourceComic(comic);
                List<Chapter> chapterList = mChapterManager.getChapterList(sourceComic);
                updateChapterList(chapterList);
            }
        }

        private void updateChapterList(List<Chapter> list) {
            Map<String, Task> map = new HashMap<>();
            Comic comic = mComicManager.load(mTask.getSource(), mTask.getCid());
            for (Task task : mTaskManager.list(comic.getId())) {
                map.put(task.getPath(), task);
            }
            if (!map.isEmpty()) {
                List<Chapter> res = new LinkedList<>();
                for (Chapter chapter : list) {
                    Task task = map.get(chapter.getPath());
                    if (task != null) {
                        chapter.setDownload(true);
                        chapter.setCount(task.getProgress());
                        chapter.setComplete(task.isFinish());
//                        mChapterManager.update(chapter);
                        res.add(chapter);
                    }
                }
                mChapterManager.runInTx(() -> mChapterManager.updateOrInsert(res));
            }

        }

        private boolean GetCacheAndWrite(CimocDocumentFile parent, int num, String url) throws IOException {
            InputStream cacheIs = FrescoUtils.getCacheFileInputStream(url);
            if (cacheIs != null) {
                String displayName = buildFileName(num, url);
                displayName = displayName.replaceAll("[:/(\\\\)(\\?)<>\"(\\|)(\\.)]", "_") + ".jpg";
                CimocDocumentFile file = DocumentUtils.getOrCreateFile(parent, displayName);
                DocumentUtils.writeBinaryToFile(mContentResolver, Objects.requireNonNull(file), cacheIs);
                return true;
            }
            return false;
        }

        private boolean RequestAndWrite(CimocDocumentFile parent, Request request, int num, String url) throws InterruptedIOException {
            if (request != null) {
                try (Response response = mHttpClient.newCall(request).execute()) {
//                    if (mTask.getSource() == 72) {
//                        OkHttpClient mJMTTHttpClient = new OkHttpClient().newBuilder()
//                                .followRedirects(true)
//                                .followSslRedirects(true)
//                                .retryOnConnectionFailure(true)
//                                .addInterceptor(chain -> {
//                                    String url1 = chain.request().url().toString();
//                                    Response response1 = chain.proceed(chain.request());
//                                    if (!url1.toLowerCase().contains("media/photos"))
//                                        return response1;
//                                    int cha = Integer.parseInt(url1.substring(url1.indexOf("photos/") + 7, url1.lastIndexOf("/")));
//                                    if (cha < 220980) return response1;
//                                    byte[] res = new JMTTUtil().decodeImage(response1.body().byteStream());
//                                    MediaType mediaType = MediaType.parse("image/avif,image/webp,image/apng,image/*,*/*");
//                                    ResponseBody outputBytes = ResponseBody.create(mediaType, res);
//                                    return response1.newBuilder().body(outputBytes).build();
//                                })
//                                .build();
//                        response = mJMTTHttpClient.newCall(request).execute();
//                    } else {
                    //                    }
                    if (response.isSuccessful()) {
                        String displayName = buildFileName(num, url);
                        displayName = displayName.replaceAll("[:/(\\\\)(\\?)<>\"(\\|)(\\.)]", "_") + ".jpg";
                        CimocDocumentFile file = DocumentUtils.getOrCreateFile(parent, displayName);
                        DocumentUtils.writeBinaryToFile(mContentResolver, Objects.requireNonNull(file), response.body().byteStream());
                        return true;
                    }
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                } catch (InterruptedIOException e) {
                    // 由暂停下载引发，需要抛出以便退出外层循环，结束任务
                    throw e;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        private Request buildRequest(Headers headers, String url) {
            if (StringUtils.isEmpty(url)) {
                return null;
            }

            return new Request.Builder()
                    .cacheControl(new CacheControl.Builder().noStore().build())
                    .headers(headers)
                    .url(url)
                    .get()
                    .build();
        }

        private String buildFileName(int num, String url) {
            String suffix = StringUtils.split(url, "\\.", -1);
            if (suffix == null) {
                suffix = "jpg";
            } else {
                suffix = suffix.split("\\?")[0];
            }
            return StringUtils.format("%03d.%s", num, suffix);
        }

        private List<ImageUrl> onDownloadParse() throws InterruptedIOException {
            mTask.setState(Task.STATE_PARSE);
            RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_TASK_STATE_CHANGE, Task.STATE_PARSE, mTask.getId()));
            return Manga.getImageUrls(mParse, mTask.getSource(), mTask.getCid(), mTask.getPath(), mTask.getTitle(), mChapterManager);
        }

        private void onDownloadProgress(int progress) {
            mTask.setProgress(progress);
            mTaskManager.update(mTask);
            RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_TASK_PROCESS, mTask.getId(), progress, mTask.getMax()));
            // 刷新下载通知
            updateNotification();
        }

    }

    public class DownloadServiceBinder extends Binder {

        public DownloadService getService() {
            return DownloadService.this;
        }

    }

}
