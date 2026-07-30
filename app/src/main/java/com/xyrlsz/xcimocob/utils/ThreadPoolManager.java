package com.xyrlsz.xcimocob.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 统一线程池管理器
 * <p>
 * 为不同负载类型提供独立的线程池，避免 Schedulers.io() 无限制创建线程导致资源竞争。
 * - IO 线程池：可变核心线程数，适合网络请求和数据库操作
 * - Compute 线程池：固定线程数（CPU 核心数），适合解析 HTML/JSON 等 CPU 密集型任务
 * - 下载线程池：专用于批量下载图片页
 * - Scheduled 线程池：用于定时/延迟任务
 */
public class ThreadPoolManager {

    private static volatile ThreadPoolManager sInstance;

    /** IO 线程池 - 适合网络 I/O、数据库访问 */
    private final ThreadPoolExecutor mIoExecutor;

    /** 计算线程池 - 适合 HTML 解析、JSON 处理等 CPU 密集型任务 */
    private final ExecutorService mComputeExecutor;

    /** 下载线程池 - 专用于页面级图片并发下载 */
    private final ExecutorService mDownloadExecutor;

    /** 定时/延迟任务线程池 */
    private final ScheduledExecutorService mScheduledExecutor;

    private ThreadPoolManager() {
        int cpuCount = Runtime.getRuntime().availableProcessors();

        // IO 线程池：核心 8，最大 32，keepAlive 60 秒
        mIoExecutor = new ThreadPoolExecutor(
                8, 32,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256),
                new NamedThreadFactory("xcimoc-io"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        // 允许核心线程超时回收
        mIoExecutor.allowCoreThreadTimeOut(true);

        // 计算线程池：固定大小 = CPU 核心数 + 1
        mComputeExecutor = Executors.newFixedThreadPool(
                Math.max(cpuCount + 1, 4),
                new NamedThreadFactory("xcimoc-compute")
        );

        // 下载线程池：可并行下载页面的线程数
        int downloadThreads = Math.max(cpuCount * 2, 6);
        mDownloadExecutor = new ThreadPoolExecutor(
                downloadThreads, downloadThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(512),
                new NamedThreadFactory("xcimoc-dl"),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 定时线程池
        mScheduledExecutor = Executors.newScheduledThreadPool(
                2,
                new NamedThreadFactory("xcimoc-sched")
        );
    }

    public static ThreadPoolManager getInstance() {
        if (sInstance == null) {
            synchronized (ThreadPoolManager.class) {
                if (sInstance == null) {
                    sInstance = new ThreadPoolManager();
                }
            }
        }
        return sInstance;
    }

    /** 获取 IO 线程池执行器 */
    public ExecutorService getIoExecutor() {
        return mIoExecutor;
    }

    /** 获取计算线程池执行器 */
    public ExecutorService getComputeExecutor() {
        return mComputeExecutor;
    }

    /** 获取下载线程池执行器 */
    public ExecutorService getDownloadExecutor() {
        return mDownloadExecutor;
    }

    /** 获取定时线程池执行器 */
    public ScheduledExecutorService getScheduledExecutor() {
        return mScheduledExecutor;
    }

    /**
     * 将 {@link io.reactivex.rxjava3.core.Observable} 的操作切换到 IO 线程池
     */
    public static <T> io.reactivex.rxjava3.core.Observable<T> wrapIo(
            io.reactivex.rxjava3.core.Observable<T> source) {
        return source.subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.from(
                getInstance().getIoExecutor()));
    }

    /**
     * 将 {@link io.reactivex.rxjava3.core.Observable} 的操作切换到计算线程池
     */
    public static <T> io.reactivex.rxjava3.core.Observable<T> wrapCompute(
            io.reactivex.rxjava3.core.Observable<T> source) {
        return source.subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.from(
                getInstance().getComputeExecutor()));
    }

    /**
     * 命名线程工厂
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final String mNamePrefix;
        private final AtomicInteger mCounter = new AtomicInteger(0);

        NamedThreadFactory(String namePrefix) {
            this.mNamePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, mNamePrefix + "-" + mCounter.incrementAndGet());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
