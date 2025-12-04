package com.xyrlsz.xcimoc.utils;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ThreadRunUtils {
    public static void runOnMainThread(Runnable runnable) {
        Observable.just(true)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> runnable.run());
    }

    public static void runTaskObserveOnUI(Runnable io, Runnable ui) {

        Observable.create((Observable.OnSubscribe<String>) subscriber -> {
                    // 在 IO 线程执行耗时操作
                    io.run();
                    subscriber.onNext("");
                    subscriber.onCompleted();
                })
                .subscribeOn(Schedulers.io())          // 指定上游执行线程
                .observeOn(AndroidSchedulers.mainThread()) // 指定下游回调线程
                .subscribe(result -> {
                    // 在主线程更新 UI
                    ui.run();
                });
    }

}
