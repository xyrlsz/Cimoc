package com.xyrlsz.xcimoc.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;
import com.xyrlsz.xcimoc.Constants;

import java.io.IOException;

public class WebDavConf {
    public static String url = "";
    public static String username = "";
    public static String password = "";

    public static void init(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.WEBDAV_SHARED, Context.MODE_PRIVATE);
        url = sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, "");
        username = sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, "");
        password = sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, "");
        if (!(username.isEmpty() || password.isEmpty() || url.isEmpty())) {
            Sardine mSardine = new OkHttpSardine();
            mSardine.setCredentials(username, password);
            new Thread(() -> {
                try {
                    String mWebDavUrl = url + "/cimoc";
                    if (!mSardine.exists(mWebDavUrl)) {
                        mSardine.createDirectory(mWebDavUrl);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public static void update(Context context) {
        init(context);
    }

}
