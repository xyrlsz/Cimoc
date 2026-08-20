package com.xyrlsz.xcimocob.utils;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.xyrlsz.xcimocob.App;

import org.json.JSONTokener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 WebView 的 JS 执行工具。
 * <p>
 * 与 QuickJS（{@link DecryptionUtils#evalDecrypt}）不同，这里运行在真实浏览器环境中，
 * 支持脚本使用 window / document / atob / btoa 等浏览器 API，可用于 QuickJS 无法
 * 执行的加解密脚本（如依赖 DOM / 编码 API 的 packed 脚本）。
 * <p>
 * 实现说明：
 * <ul>
 *     <li>用应用上下文创建离屏 WebView（不挂到任何视图），因此不依赖前台 Activity；</li>
 *     <li>WebView 操作必须在主线程执行：后台线程调用本工具时会阻塞等待结果（带超时），
 *         并用静态锁串行化，避免并发共用一个 WebView 互相干扰；</li>
 *     <li>主线程调用 / WebView 创建或执行异常时回退到 QuickJS，保证同步返回值语义；</li>
 *     <li>脚本统一在 about:blank 上执行，每次执行前重新加载以隔离全局变量。</li>
 * </ul>
 */
public class WebViewJsExecutor {

    private static final String TAG = "WebViewJs";

    /**
     * 单次求值总超时（毫秒）
     */
    private static final long EVAL_TIMEOUT_MS = 30_000;

    private static final Object LOCK = new Object();

    private static WebView mWebView;
    /**
     * 页面加载完成后要执行的任务（重新加载 about:blank 后执行脚本）
     */
    private static Runnable mOnPageFinished;

    private WebViewJsExecutor() {
    }

    /**
     * 通过 WebView 执行一段 JS 脚本，返回表达式结果字符串。
     * 兼容 {@link DecryptionUtils#evalDecrypt(String)} 的语义（返回最后表达式的值）。
     */
    public static String evaluate(String jsCode) {
        return evaluate(jsCode, null);
    }

    /**
     * 通过 WebView 执行一段 JS 脚本，返回指定全局变量的字符串值。
     * 兼容 {@link DecryptionUtils#evalDecrypt(String, String)} 的语义（如 DM5 的 "newImgs"）。
     * <p>
     * 主线程调用 / WebView 异常时回退到 QuickJS；超时返回 ""。
     */
    public static String evaluate(String jsCode, String varName) {
        // 主线程无法阻塞等待 WebView 异步回调，直接回退 QuickJS 保持同步语义
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return DecryptionUtils.evalDecrypt(jsCode, varName);
        }
        // 串行化：不同后台线程共用一个 WebView，避免并发求值互相干扰
        synchronized (LOCK) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<String> result = new AtomicReference<>("");
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    runEvaluate(jsCode, varName, result::set);
                } catch (Exception e) {
                    Log.w(TAG, "evaluate error, fallback to QuickJS", e);
                    result.set(DecryptionUtils.evalDecrypt(jsCode, varName));
                } finally {
                    latch.countDown();
                }
            });

            try {
                if (!latch.await(EVAL_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    Log.w(TAG, "evaluate timeout");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return result.get();
        }
    }

    /**
     * 释放离屏 WebView（不再需要时可手动调用）。
     */
    public static void destroy() {
        new Handler(Looper.getMainLooper()).post(() -> {
            mOnPageFinished = null;
            if (mWebView != null) {
                try {
                    mWebView.stopLoading();
                    mWebView.removeAllViews();
                    mWebView.destroy();
                } catch (Exception e) {
                    Log.w(TAG, "destroy error", e);
                }
                mWebView = null;
            }
        });
    }

    /**
     * 懒创建离屏 WebView（必须在主线程调用）。
     */
    @SuppressLint("SetJavaScriptEnabled")
    private static void ensureWebView() {
        if (mWebView != null) {
            return;
        }
        try {
            mWebView = new WebView(App.getAppContext());
            WebSettings settings = mWebView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Runnable task = mOnPageFinished;
                    mOnPageFinished = null;
                    if (task != null) {
                        task.run();
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "WebView create error", e);
            mWebView = null;
        }
    }

    /**
     * 每次执行前重新加载 about:blank（隔离上一次脚本留下的全局变量），
     * 页面就绪后再真正执行脚本。
     */
    private static void runEvaluate(String jsCode, String varName, ResultCallback onDone) {
        ensureWebView();
        if (mWebView == null) {
            onDone.onResult("");
            return;
        }
        mOnPageFinished = () -> {
            if (mWebView == null) {
                onDone.onResult("");
                return;
            }
            if (varName == null || varName.trim().isEmpty()) {
                // 返回脚本最后表达式的值（与 QuickJS evaluate(script, null) 语义一致）
                mWebView.evaluateJavascript(jsCode, value -> onDone.onResult(unquote(value)));
            } else {
                // 先执行脚本，再读取指定全局变量
                mWebView.evaluateJavascript(jsCode, v -> {
                    if (mWebView == null) {
                        onDone.onResult("");
                        return;
                    }
                    mWebView.evaluateJavascript(varName, value -> onDone.onResult(unquote(value)));
                });
            }
        };
        mWebView.loadUrl("about:blank");
    }

    /**
     * 解析 evaluateJavascript 回调值（JSON 编码的字符串）。
     * 字符串会带引号（如 {@code "\"a,b\""}），这里还原成原始字符串；
     * 数字 / 布尔 / 数组等非字符串类型按原样返回；null / undefined 返回 ""。
     */
    private static String unquote(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim();
        if (v.isEmpty() || "null".equalsIgnoreCase(v) || "undefined".equalsIgnoreCase(v)) {
            return "";
        }
        if (v.startsWith("\"")) {
            try {
                Object o = new JSONTokener(v).nextValue();
                return o == null ? "" : o.toString();
            } catch (Exception e) {
                // 手动去引号 + 反转义（兜底）
                return v.substring(1, v.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\n", "\n")
                        .replace("\\t", "\t");
            }
        }
        return v;
    }

    private interface ResultCallback {
        void onResult(String value);
    }
}
