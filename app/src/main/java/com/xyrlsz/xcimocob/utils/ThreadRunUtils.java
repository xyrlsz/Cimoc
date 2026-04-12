package com.xyrlsz.xcimocob.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ThreadRunUtils {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void runOnMainThread(Runnable r) {
        MAIN.post(r);
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


    public interface TaskCallback {
        void onSuccess();

        void onError(String msg);
    }
}
