package com.xyrlsz.xcimoc.utils;

import android.util.Log;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ThreadRunUtils {

    public static void runOnMainThread(Runnable runnable, TaskCallback callback) {
        Observable.create(subscriber -> {
                    try {
                        runnable.run();
                        subscriber.onNext(null); // Notify completion
                        subscriber.onCompleted();
                    } catch (Throwable e) {
                        subscriber.onError(e); // Pass error directly to RxJava error handler
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> callback.onSuccess(),
                        err -> callback.onError("Error on Main Thread: " + Log.getStackTraceString(err))
                );
    }

    public static void runOnIOThread(Runnable runnable, TaskCallback callback) {
        Observable.create(subscriber -> {
                    try {
                        runnable.run();
                        subscriber.onNext(null); // Notify completion
                        subscriber.onCompleted();
                    } catch (Throwable e) {
                        subscriber.onError(e); // Pass error directly to RxJava error handler
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> callback.onSuccess(),
                        err -> callback.onError("Error on IO Thread: " + Log.getStackTraceString(err))
                );
    }

    public static void runOnMainThread(Runnable runnable) {
        runOnMainThread(runnable, new TaskCallback() {
            @Override
            public void onSuccess() {
                Log.println(Log.INFO, "ThreadRunUtils", "runOnMainThread success");
            }

            @Override
            public void onError(String msg) {
                Log.e("ThreadRunUtils", "runOnMainThread error: " + msg);
            }
        });
    }

    public static void runOnIOThread(Runnable runnable) {
        runOnIOThread(runnable, new TaskCallback() {
            @Override
            public void onSuccess() {
                // No-op
            }

            @Override
            public void onError(String msg) {
                Log.e("ThreadRunUtils", "runOnIOThread error: " + msg);
            }
        });
    }

    public static void runTaskObserveOnUI(Runnable io, Runnable ui, TaskCallback callback) {
        Observable.create(subscriber -> {
                    try {
                        io.run();
                        subscriber.onNext(null); // Notify completion
                        subscriber.onCompleted();
                    } catch (Throwable e) {
                        subscriber.onError(e); // Handle any errors
                    }
                })
                .subscribeOn(Schedulers.io()) // Do IO work on IO thread
                .observeOn(AndroidSchedulers.mainThread()) // Switch back to main thread for UI updates
                .subscribe(
                        result -> {
                            try {
                                ui.run();
                                callback.onSuccess();
                            } catch (Throwable e) {
                                callback.onError("UI error: " + Log.getStackTraceString(e));
                            }
                        },
                        err -> callback.onError("RxJava error: " + err.getMessage())
                );
    }

    public interface TaskCallback {
        void onSuccess();

        void onError(String msg);
    }
}
