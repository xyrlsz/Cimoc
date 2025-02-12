package com.xyrlsz.xcimoc.utils;

import android.net.Uri;

public class UriUtils {
    public static boolean isHttpOrHttps(Uri uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }
}
