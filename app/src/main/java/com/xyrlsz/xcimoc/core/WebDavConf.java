package com.xyrlsz.xcimoc.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;
import com.xyrlsz.xcimoc.Constants;

import java.io.IOException;

import rx.Observable;
import rx.schedulers.Schedulers;

public class WebDavConf {
    public static String url = "";
    public static Sardine sardine = null;

    public static void init(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.WEBDAV_SHARED, Context.MODE_PRIVATE);
        url = sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, "");
        String username = sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, "");
        String password = sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, "");
        if (!(username.isEmpty() || password.isEmpty() || url.isEmpty())) {
            sardine = new OkHttpSardine();
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
                    .subscribe();
        }
    }

    public static void update(Context context) {
        url = null;
        sardine = null;
        init(context);
    }

}
