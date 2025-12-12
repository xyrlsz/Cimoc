/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.xyrlsz.xcimoc.fresco;

import android.net.Uri;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.facebook.common.logging.FLog;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.BaseProducerContextCallbacks;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.utils.HintUtils;

import java.io.IOException;
import java.util.concurrent.Executor;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Network fetcher that uses OkHttp 3 as a backend.
 */
public class OkHttpNetworkFetcher extends
        com.facebook.imagepipeline.backends.okhttp3.OkHttpNetworkFetcher {

    private static final String TAG = "OkHttpNetworkFetchProducer";
    private static final String QUEUE_TIME = "queue_time";
    private static final String FETCH_TIME = "fetch_time";
    private static final String TOTAL_TIME = "total_time";
    private static final String IMAGE_SIZE = "image_size";
    private final OkHttpClient mOkHttpClient;
    private final Headers mHeaders;
    private Executor mCancellationExecutor;

    /**
     * @param okHttpClient client to use
     */
    public OkHttpNetworkFetcher(OkHttpClient okHttpClient, Headers headers) {
        super(okHttpClient);
        mOkHttpClient = okHttpClient;
        mHeaders = headers;
        //修复打开仅WiFi联网功能后运行闪退的问题
        try {
            mCancellationExecutor = okHttpClient.dispatcher().executorService();
        } catch (NullPointerException e) {
//            CustomToast.showToast(App.getAppContext(), "网络连接失败，请检查网络！！", 2000);
            HintUtils.showToast(App.getAppContext(), "网络连接失败，请检查网络！！");
        }


    }

    @NonNull
    @Override
    public OkHttpNetworkFetchState createFetchState(@NonNull Consumer<EncodedImage> consumer, @NonNull ProducerContext context) {
        return new OkHttpNetworkFetchState(consumer, context);
    }

    @Override
    public void fetch(@NonNull OkHttpNetworkFetchState fetchState, @NonNull Callback callback) {
        fetchState.submitTime = SystemClock.elapsedRealtime();
        Headers headers = App.getHeaders();
        final Uri uri = fetchState.getUri();
        Request request = new Request.Builder()
                .cacheControl(new CacheControl.Builder().noStore().build())
                .headers(headers)
                .url(uri.toString())
                .get()
                .build();
        final Call call = mOkHttpClient.newCall(request);

        fetchState.getContext().addCallbacks(
                new BaseProducerContextCallbacks() {
                    @Override
                    public void onCancellationRequested() {
                        if (Looper.myLooper() != Looper.getMainLooper()) {
                            call.cancel();
                        } else {
                            mCancellationExecutor.execute(new Runnable() {
                                @Override
                                public void run() {
                                    call.cancel();
                                }
                            });
                        }
                    }
                });

        call.enqueue(
                new okhttp3.Callback() {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        fetchState.responseTime = SystemClock.elapsedRealtime();
                        final ResponseBody body = response.body();
                        try {
                            if (!response.isSuccessful()) {
                                handleException(
                                        call,
                                        new IOException("Unexpected HTTP code " + response),
                                        callback);
                                return;
                            }

                            long contentLength = body.contentLength();
                            if (contentLength < 0) {
                                contentLength = 0;
                            }
                            callback.onResponse(body.byteStream(), (int) contentLength);
                        } catch (Exception e) {
                            handleException(call, e, callback);
                        } finally {
                            try {
                                body.close();
                            } catch (Exception e) {
                                FLog.w(TAG, "Exception when closing response body", e);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        handleException(call, e, callback);
                    }
                });
    }

    /**
     * Handles exceptions.
     * <p>
     * <p> OkHttp notifies callers of cancellations via an IOException. If IOException is caught
     * after request cancellation, then the exception is interpreted as successful cancellation
     * and onCancellation is called. Otherwise onFailure is called.
     */
    private void handleException(final Call call, final Exception e, final Callback callback) {
        if (call.isCanceled()) {
            callback.onCancellation();
        } else {
            callback.onFailure(e);
        }
    }


}