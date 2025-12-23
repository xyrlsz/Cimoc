package com.xyrlsz.xcimoc.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;
import com.xyrlsz.xcimoc.Constants;

import java.io.IOException;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class WebDavConf {
    public static String url = "";
    public static Sardine sardine = new OkHttpSardine();
    public static boolean isInit = false;

    public static void init(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.WEBDAV_SHARED, Context.MODE_PRIVATE);
        url = sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, "");
        String username = sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, "");
        String password = sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, "");
        if (!(username.isEmpty() || password.isEmpty() || url.isEmpty())) {
            sardine.setCredentials(username, password);
            Observable.create((Observable.OnSubscribe<Void>) subscriber -> {
                        try {
                            String mWebDavUrl = url + "/cimoc";
                            if (!sardine.exists(mWebDavUrl)) {
                                sardine.createDirectory(mWebDavUrl);
                            }
                            subscriber.onCompleted();
                        } catch (IOException e) {
                            subscriber.onError(e);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Subscriber<>() {
                        @Override
                        public void onCompleted() {
                            Log.i("WebDavConf", "WebDav 目录检查/创建成功");
                            isInit = true;
                        }

                        @Override
                        public void onError(Throwable e) {
                            sardine = null;
                            Log.e("WebDavConf", "WebDav 初始化失败: ", e);
                        }

                        @Override
                        public void onNext(Void unused) {

                        }
                    });
        }
    }

    public static void update(Context context) {
        url = null;
        sardine = null;
        init(context);
    }

}
