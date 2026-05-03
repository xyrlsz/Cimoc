package com.xyrlsz.xcimocob.utils;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ThreadRunUtils {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void runOnMainThread(Runnable r) {
        MAIN.post(r);
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void runOnIOThread(Runnable runnable, TaskCallback callback) {
        Observable.create(emitter -> {
                    try {
                        runnable.run();
                        emitter.onNext(0); // Notify completion (RxJava3 disallows null)
                        emitter.onComplete();
                    } catch (Throwable e) {
                        emitter.onError(e); // Pass error directly to RxJava error handler
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
