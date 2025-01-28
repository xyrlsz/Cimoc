package com.xyrlsz.xcimoc.parser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import okhttp3.Headers;

// 解析动态加载的网页
public class WebParser {
    private final String url;
    private final Context context;
    private final Headers headers;
    private final CountDownLatch latch;
    private String htmlStr;
    private WebView webView;
    private int delay = 50;

    public WebParser(Context context, String url, Headers headers) {
        this.url = url;
        this.headers = headers;
        this.context = context;
        this.latch = new CountDownLatch(1);
        new Handler(Looper.getMainLooper()).post(() -> {
            webView = new WebView(context);
            initWebView();  // 后续初始化操作
        });
    }

    public WebParser(Context context, String url, Headers headers, int delay) {
        this.url = url;
        this.headers = headers;
        this.context = context;
        this.latch = new CountDownLatch(1);
        this.delay = delay;
        new Handler(Looper.getMainLooper()).post(() -> {
            webView = new WebView(context);
            initWebView();  // 后续初始化操作
        });
    }


    @SuppressLint("SetJavaScriptEnabled")
    public void initWebView() {
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // 宽度
                ViewGroup.LayoutParams.MATCH_PARENT  // 高度
        );
        webView.setLayoutParams(layoutParams);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
//                new Handler().postDelayed(() -> {
                    smoothScrollToBottom();
//                }, delay);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        Map<String, String> headersMap = new HashMap<>();
        if (headers != null) {
            for (String key : headers.names()) {
                headersMap.put(key, headers.get(key));
            }
        }
        if (url != null) {
            webView.loadUrl(url, headersMap);
        }
    }

    private void smoothScrollToBottom() {
        String js = "var scrollHeight = document.body.scrollHeight; " +
                "var currentScroll = document.documentElement.scrollTop || document.body.scrollTop; " +
                "var step = 100; " +
                "var interval = setInterval(function() { " +
                "  if (currentScroll < scrollHeight) { " +
                "    currentScroll += step; " +
                "    window.scrollTo(0, currentScroll); " +
                "  } else { " +
                "    clearInterval(interval); " +
                "  } " +
                "}, 50);";

        webView.evaluateJavascript(js, value -> {
            getPageHtml();
        });
    }

    private void getPageHtml() {
        String js = "document.documentElement.outerHTML";
        webView.evaluateJavascript(js, value -> {
            if (value != null) {
                htmlStr = value.replace("\\u003C", "<")
                        .replace("\\u003E", ">")
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\'", "'")
                        .replace("\\t", "    ")
                        .replace("\\\\/", "\\/");
                latch.countDown(); // 释放锁，表示 HTML 已加载完成
            }
        });
    }

    public String getHtmlStrSync() throws InterruptedException {
        latch.await(); // 阻塞当前线程，直到 HTML 加载完成
        return htmlStr;
    }
}