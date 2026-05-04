package com.xyrlsz.xcimocob.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;
import com.xyrlsz.xcimocob.Constants;

import java.io.IOException;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class WebDavConf {
    public static String url = "";
    public static Sardine sardine = null;
    public static boolean isInit = false;

    public static void init(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.WEBDAV_SHARED, Context.MODE_PRIVATE);
        url = sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, "");
        String username = sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, "");
        String password = sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, "");
        sardine = new OkHttpSardine();
        if (!(username.isEmpty() || password.isEmpty() || url.isEmpty())) {
            sardine.setCredentials(username, password);
            Observable.create((io.reactivex.rxjava3.core.ObservableOnSubscribe<Void>) emitter -> {
                        try {
                            String mWebDavUrl = url + "/cimoc";
                            if (!sardine.exists(mWebDavUrl)) {
                                sardine.createDirectory(mWebDavUrl);
                            }
                            emitter.onComplete();
                            return;
                        } catch (IOException e) {
                            emitter.onError(e);
                            return;
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
        sardine = null;
        isInit = false;
        init(context);
    }

}
