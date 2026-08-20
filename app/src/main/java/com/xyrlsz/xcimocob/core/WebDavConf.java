package com.xyrlsz.xcimocob.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.xyrlsz.xcimocob.Constants;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class WebDavConf {
    public static String url = "";
    public static WebDavClient client = null;
    public static boolean isInit = false;

    public static void init(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.WEBDAV_SHARED, Context.MODE_PRIVATE);
        url = sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, "");
        String username = sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, "");
        String password = sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, "");
        if (client != null) {
            client.close();
        }
        client = WebDavClient.create(username, password);
        isInit = false;
        if (!(username.isEmpty() || password.isEmpty() || url.isEmpty())) {
            // 显式接住 Disposable，消除 CheckResult 警告。
            // init() 是启动期一次性异步初始化：成功后 onComplete 自动 dispose，
            // 失败时 onError 终止流程；无需额外生命周期绑定。
            @SuppressWarnings("unused")
            Disposable webdavInitDisposable = Observable.create((io.reactivex.rxjava3.core.ObservableOnSubscribe<Void>) emitter -> {
                        try {
                            // 在后台线程检查/创建 /cimoc 目录
                            client.createDirectory(url + "/cimoc");
                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        new io.reactivex.rxjava3.functions.Consumer<Object>() {
                            @Override
                            public void accept(Object v) {
                                Log.i("WebDavConf", "WebDav 目录检查/创建成功");
                                isInit = true;
                            }
                        },
                        new io.reactivex.rxjava3.functions.Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable e) {
                                isInit = false;
                                Log.e("WebDavConf", "WebDav 初始化失败: ", e);
                            }
                        }
                    );
        }
    }

    public static void update(Context context) {
        url = null;
        client = null;
        isInit = false;
        init(context);
    }

}
