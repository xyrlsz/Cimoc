package com.haleydu.cimoc.ui.activity;

import android.os.Bundle;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;

import com.haleydu.cimoc.R;

public class WebviewActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        // 获取 WebView 实例
        webView = findViewById(R.id.web);

        // 从 Intent 中获取 URL
        String url = getIntent().getStringExtra("URL");
        if (url != null) {
            webView.loadUrl(url);
        }
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
}