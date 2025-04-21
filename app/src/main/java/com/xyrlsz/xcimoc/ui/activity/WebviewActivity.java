package com.xyrlsz.xcimoc.ui.activity;

import static com.xyrlsz.xcimoc.ui.activity.BrowserFilter.URL_KEY;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xyrlsz.xcimoc.R;

import java.util.HashMap;
import java.util.Map;

public class WebviewActivity extends AppCompatActivity {

    private WebView webView;
    public static final String EXTRA_WEB_URL = "extra_web_url";
    public static final String EXTRA_WEB_HEADERS = "extra_web_headers";
    public static final String EXTRA_WEB_HTML = "extra_web_html";
    public static final String EXTRA_IS_USE_TO_WEB_PARSER = "extra_is_use_to_web_parser";
    private String htmlStr = "";

    FloatingActionButton loadButton;
    FloatingActionButton exitButton;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        webView = findViewById(R.id.web);
        loadButton = findViewById(R.id.load_button);
        exitButton = findViewById(R.id.exit_button);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (getIntent().getBooleanExtra(EXTRA_IS_USE_TO_WEB_PARSER, true)) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            smoothScrollToBottom();
                        }
                    }, 1000); // 延迟一秒
                }

            }


        });


        // 设置 WebChromeClient 以便获取页面中的 JavaScript 输出
        webView.setWebChromeClient(new WebChromeClient());

        // 启用 JavaScript 和 DOM 存储
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        // 从 Intent 中获取 URL
        String url = getIntent().getStringExtra(EXTRA_WEB_URL);
        Bundle bundle = getIntent().getBundleExtra(EXTRA_WEB_HEADERS);
        Map<String, String> headers = new HashMap<>();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                headers.put(key, bundle.getString(key));
            }
        }
        if (url != null) {
            webView.loadUrl(url, headers);
        }

        loadButton.setOnClickListener(v -> {
            Intent intent = new Intent(WebviewActivity.this, BrowserFilter.class);
            intent.putExtra(URL_KEY, webView.getOriginalUrl());
            startActivity(intent);
        });
        exitButton.setOnClickListener(v -> finish());

    }

    @Override
    public void onBackPressed() {
        // 如果 WebView 可以返回上一页，则返回上一页
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // 否则执行默认的返回操作
            super.onBackPressed();
        }
    }

    // 缓慢滚动到底部
    private void smoothScrollToBottom() {
        String js = "var scrollHeight = document.body.scrollHeight; " +
                "var currentScroll = document.documentElement.scrollTop || document.body.scrollTop; " +
                "var step = 100; " + // 每次滚动的距离
                "var interval = setInterval(function() { " +
                "  if (currentScroll < scrollHeight) { " +
                "    currentScroll += step; " +
                "    window.scrollTo(0, currentScroll); " +
                "  } else { " +
                "    clearInterval(interval); " + // 滚动完毕后停止
                "  } " +
                "}, 50);"; // 每次滚动的间隔时间（单位：毫秒）

        webView.evaluateJavascript(js, value -> {
            // 获取页面的 HTML 内容
            getPageHtml();
        });
    }

    private void getPageHtml() {
        // 使用 JavaScript 获取页面的 HTML
        String js = "document.documentElement.outerHTML";
        webView.evaluateJavascript(js, value -> {
            // 在这里你可以处理获取到的 HTML 内容
            // 例如输出到日志
            if (value != null) {
                // 解码 Unicode 编码为正常字符
                String decodedHtml = value.replace("\\u003C", "<")
                        .replace("\\u003E", ">")
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\'", "'");
                System.out.println("HTML Content: " + decodedHtml);
                htmlStr = decodedHtml;

                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_WEB_HTML, htmlStr);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }


}
