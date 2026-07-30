package com.xyrlsz.xcimocob.parser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.xyrlsz.xcimocob.source.CopyMHWeb;
import com.xyrlsz.xcimocob.utils.StringUtils;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import okhttp3.Headers;


public class WebParser {
    // 参数（可调）
    private static final int MAX_SCROLL = 512; // 最多滚动次数
    private static final int SAME_LIMIT = 3; // 高度连续不变次数
    private static final int SCROLL_DELAY = 50;
    /**
     * 总超时时间：120 秒后强制完成，防止永久阻塞
     */
    private static final long TOTAL_TIMEOUT_MS = 120_000;

    private final String url;
    private final Headers headers;
    private final PublishSubject<String> htmlSubject = PublishSubject.create();
    private final Context mContext;
    private String UA = "";
    /**
     * 每个实例独立的 WebView，不再共享静态实例
     */
    private WebView mWebView;
    // 滚动控制
    private int lastHeight = 0;
    private int sameCount = 0;
    private int scrollCount = 0;

    private int errTimes = 0;
    private volatile boolean emitted = false;
    private volatile boolean destroyed = false;

    public WebParser(Context context, String url, Headers headers) {
        this(context, url, headers, "");
    }

    public WebParser(Context context, String url, Headers headers, String UA) {
        this.url = url;
        this.headers = headers;
        this.UA = UA;
        this.mContext = context.getApplicationContext();

        // 在 UI 线程创建 WebView 并开始加载
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                mWebView = new WebView(mContext);
                initWebViewSettings(mWebView);
                initWebViewForRequest();
            } catch (Exception e) {
                Log.e("WebParser", "WebView init error", e);
                emitError(e);
            }
        });
    }

    /**
     * 安全发射结果，防止重复发射
     */
    private void emitResult(String html) {
        if (!emitted) {
            emitted = true;
            htmlSubject.onNext(html);
            htmlSubject.onComplete();
            destroyWebView();
        }
    }

    private void emitError(Throwable e) {
        if (!emitted) {
            emitted = true;
            htmlSubject.onError(e);
            destroyWebView();
        }
    }

    /**
     * 销毁 WebView（必须在 UI 线程执行）
     */
    private void destroyWebView() {
        if (destroyed) return;
        destroyed = true;
        if (mWebView != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    mWebView.stopLoading();
                    mWebView.removeAllViews();
                    mWebView.destroy();
                } catch (Exception e) {
                    Log.w("WebParser", "WebView destroy error", e);
                }
                mWebView = null;
            });
        }
    }

    /**
     * WebView 初始化设置
     */
    @SuppressLint("SetJavaScriptEnabled")
    private static void initWebViewSettings(WebView wv) {
        wv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wv.setWebChromeClient(new WebChromeClient());
        wv.getSettings().setJavaScriptEnabled(true);
        wv.getSettings().setDomStorageEnabled(true);
        wv.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
    }

    /**
     * 加载 URL 前的配置
     */
    private void initWebViewForRequest() {
        if (destroyed) return;
        if (!StringUtils.isEmpty(UA)) {
            mWebView.getSettings().setUserAgentString(UA);
        }

        // 使用 TreeMap 并指定忽略大小写的比较器
        Map<String, String> headersMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (headers != null) {
            for (String key : headers.names()) {
                headersMap.put(key, headers.get(key));
            }
            if (StringUtils.isEmpty(UA) && headersMap.containsKey("User-Agent")) {
                mWebView.getSettings().setUserAgentString(headersMap.get("User-Agent"));
            }
        }

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                waitForDomReady();
            }
        });

        mWebView.loadUrl(url, headersMap);
    }

    /**
     * 只等 DOM ready 一次
     */
    private void waitForDomReady() {
        if (destroyed || mWebView == null) return;

        mWebView.evaluateJavascript("(function(){return document.readyState})()", value -> {
            if (destroyed || mWebView == null) return;

            if (value != null && value.contains("complete")) {
                if (url.contains(CopyMHWeb.website) && url.contains("/comic/") && !url.contains("/chapter/")) {
                    String jsCode = "javascript:(function() { " +
                            "var btns = document.getElementsByClassName('next-all'); " +
                            "for(var i = 0; i < btns.length; i++) { " +
                            "   btns[i].click(); " +
                            "} " +
                            "})()";

                    mWebView.evaluateJavascript(jsCode, s -> {
                        if (destroyed || mWebView == null) return;
                        new Handler(Looper.getMainLooper()).postDelayed(this::autoScroll, 500);
                    });
                } else {
                    new Handler(Looper.getMainLooper()).postDelayed(this::autoScroll, 300);
                }
            } else {
                new Handler(Looper.getMainLooper()).postDelayed(this::waitForDomReady, 100);
            }
        });
    }

    /**
     * 核心：智能滚动
     */
    private void autoScroll() {
        if (destroyed || mWebView == null) return;

        String js = "(function(){"
                + "var h = document.body.scrollHeight;"
                + "var ch = document.body.clientHeight;"
                + "var st = document.body.scrollTop || document.documentElement.scrollTop;"
                + "window.scrollBy(0, 500);"
                + "return h + ',' + ch + ',' + st;"
                + "})()";

        mWebView.evaluateJavascript(js, value -> {
            if (destroyed || mWebView == null) return;

            try {
                if (value == null)
                    return;

                String[] parts = value.replace("\"", "").split(",");
                double currentScrollHeight = Double.parseDouble(parts[0]);
                double clientHeight = Double.parseDouble(parts[1]);
                double currentScrollTop = Double.parseDouble(parts[2]);

                double distanceToBottom = currentScrollHeight - (currentScrollTop + clientHeight);

                if (distanceToBottom <= 100) {
                    getPageHtml();
                    return;
                }

                if (currentScrollHeight == lastHeight) {
                    sameCount++;
                } else {
                    sameCount = 0;
                    lastHeight = (int) currentScrollHeight;
                }

                scrollCount++;

                if (sameCount >= SAME_LIMIT || scrollCount >= MAX_SCROLL) {
                    getPageHtml();
                    return;
                }

                int nextDelay = (distanceToBottom < 1000) ? 200 : 50;

                new Handler(Looper.getMainLooper()).postDelayed(this::autoScroll, nextDelay);

            } catch (Exception ignored) {
                if (errTimes <= 5) {
                    new Handler(Looper.getMainLooper()).postDelayed(this::autoScroll, SCROLL_DELAY);
                    errTimes++;
                } else {
                    emitError(new Exception("WebParser: autoScroll failed after retries"));
                }
            }
        });
    }

    /**
     * 获取 HTML
     */
    private void getPageHtml() {
        if (destroyed || mWebView == null) return;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (destroyed || mWebView == null) return;

            mWebView.evaluateJavascript(
                    "(function(){return document.documentElement.outerHTML})()", value -> {
                        if (destroyed || mWebView == null) return;

                        if (value != null) {
                            String result = value.replace("\\u003C", "<")
                                    .replace("\\u003E", ">")
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\'", "'")
                                    .replace("\\t", "    ")
                                    .replace("\\\\/", "\\/");

                            emitResult(result);
                        } else {
                            emitError(new Exception("WebParser: failed to get HTML"));
                        }
                    });
        }, 300);
    }

    /**
     * 获取 HTML 的 Observable
     */
    public Observable<String> getHtmlObservable() {
        return htmlSubject
                .timeout(TOTAL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .firstElement()
                .toObservable();
    }
}