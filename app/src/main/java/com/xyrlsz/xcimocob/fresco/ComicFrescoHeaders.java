package com.xyrlsz.xcimocob.fresco;

import okhttp3.Headers;

public class ComicFrescoHeaders {
    private static Headers mHeaders;

    public static Headers getHeaders() {
        if (mHeaders == null) {
            mHeaders = new Headers.Builder().build();
        }
        return mHeaders;
    }

    public static void setHeaders(Headers headers) {
        mHeaders = headers;
    }
}
