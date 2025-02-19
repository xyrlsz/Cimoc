package com.xyrlsz.xcimoc.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.xyrlsz.xcimoc.Constants;

public class WebDavConf {
    public static String url = "";
    public static String username = "";
    public static String password = "";

    public static void init(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.WEBDAV_SHARED, Context.MODE_PRIVATE);
        url = sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, "");
        username = sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, "");
        password = sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, "");
    }

    public static void update(Context context) {
        init(context);
    }

}
