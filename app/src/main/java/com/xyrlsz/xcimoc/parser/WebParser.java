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

import com.xyrlsz.xcimoc.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import okhttp3.Headers;

public class WebParser {

    // 参数（可调）
    private static final int MAX_SCROLL = 512;   // 最多滚动次数
    private static final int SAME_LIMIT = 3;    // 高度连续不变次数
    private static final int SCROLL_DELAY = 50;
    private final String url;
    private final Headers headers;
    private final CountDownLatch latch;
    private WebView webView;
    private String htmlStr;
    private String UA = "";
    // 滚动控制
    private int lastHeight = 0;
    private int sameCount = 0;
    private int scrollCount = 0;

    public WebParser(Context context, String url, Headers headers) {
        this(context, url, headers, "");
    }

    public WebParser(Context context, String url, Headers headers, String UA) {
        this.url = url;
        this.headers = headers;
        this.UA = UA;
        this.latch = new CountDownLatch(1);

        new Handler(Looper.getMainLooper()).post(() -> {
            webView = new WebView(context);
            initWebView();
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {

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

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        if (!StringUtils.isEmpty(UA)) {
            webView.getSettings().setUserAgentString(UA);
        }

        Map<String, String> headersMap = new HashMap<>();
        if (headers != null) {
            for (String key : headers.names()) {
                headersMap.put(key, headers.get(key));
            }
        }

        webView.loadUrl(url, headersMap);
    }

    /**
     * 只等 DOM ready 一次
     */
    private void waitForDomReady() {
        webView.evaluateJavascript(
                "(function(){return document.readyState})()",
                value -> {
                    if (value != null && value.contains("complete")) {
                        // 给 JS 一点时间
                        new Handler(Looper.getMainLooper())
                                .postDelayed(this::autoScroll, 300);
                    } else {
                        new Handler(Looper.getMainLooper())
                                .postDelayed(this::waitForDomReady, 100);
                    }
                });
    }

    /**
     * 核心：智能滚动
     */
    private void autoScroll() {
        String js =
                "(function(){" +
                        "window.scrollBy(0, 500);" +
                        "return document.body.scrollHeight;" +
                        "})()";

        webView.evaluateJavascript(js, value -> {
            try {
                int height = Integer.parseInt(value.replace("\"", ""));

                if (height == lastHeight) {
                    sameCount++;
                } else {
                    sameCount = 0;
                    lastHeight = height;
                }

                scrollCount++;

                // ✅ 停止条件
                if (sameCount >= SAME_LIMIT || scrollCount >= MAX_SCROLL) {
                    getPageHtml();
                    return;
                }

                new Handler(Looper.getMainLooper())
                        .postDelayed(this::autoScroll, SCROLL_DELAY);

            } catch (Exception e) {
                // 出错继续滚，避免卡死
                new Handler(Looper.getMainLooper())
                        .postDelayed(this::autoScroll, SCROLL_DELAY);
            }
        });
    }

    /**
     * 获取 HTML
     */
    private void getPageHtml() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            webView.evaluateJavascript(
                    "(function(){return document.documentElement.outerHTML})()",
                    value -> {
                        if (value != null) {
                            htmlStr = value
                                    .replace("\\u003C", "<")
                                    .replace("\\u003E", ">")
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"")
                                    .replace("\\'", "'")
                                    .replace("\\t", "    ")
                                    .replace("\\\\/", "\\/");

                            latch.countDown();
                        }
                    });
        }, 300); // 最后缓冲
    }

    /**
     * 同步获取 HTML
     */
    public String getHtmlStrSync() throws InterruptedException {
        latch.await();
        return htmlStr;
    }
}