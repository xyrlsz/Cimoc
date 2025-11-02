package com.xyrlsz.xcimoc.core;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.Timeout;

public class DisabledOkHttpClient extends OkHttpClient {
    @Override
    public Call newCall(Request request) {
        return new Call() {
            @NonNull
            @Override
            public Call clone() {
                return null;
            }

            @NonNull
            @Override
            public Timeout timeout() {
                return null;
            }

            @Override
            public Request request() { return request; }

            @Override
            public Response execute() throws IOException {
                throw new IOException("OkHttp is disable.");
            }

            @Override
            public void enqueue(Callback responseCallback) {
                responseCallback.onFailure(this, new IOException("OkHttp OkHttp is disable"));
            }

            @Override
            public void cancel() { }

            @Override
            public boolean isExecuted() { return false; }

            @Override
            public boolean isCanceled() { return false; }
        };
    }
}