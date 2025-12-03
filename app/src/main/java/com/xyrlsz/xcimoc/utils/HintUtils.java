package com.xyrlsz.xcimoc.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.xyrlsz.xcimoc.App;

/**
 * Created by Hiroshi on 2016/9/22.
 */

public class HintUtils {

    public static void showSnackbar(View layout, String msg) {
        if (layout == null) {
            return;
        }
        runOnMainThread(() -> {
            if (layout.isShown()) {
                Snackbar snackbar = Snackbar.make(layout, msg, Snackbar.LENGTH_SHORT);
                int theme = ThemeUtils.getThemeId();
                snackbar.setBackgroundTint(App.getAppContext().getResources().getColor(ThemeUtils.getThemeColorById(theme)));
                snackbar.show();
            }
        });
    }

    public static void showToast(Context context, int resId) {
        if (context == null) return;
        runOnMainThread(() -> {
            Toast.makeText(context.getApplicationContext(), resId, Toast.LENGTH_SHORT).show();
        });
    }

    public static void showToastLong(Context context, int resId) {
        if (context == null) return;
        runOnMainThread(() -> {
            Toast.makeText(context.getApplicationContext(), resId, Toast.LENGTH_LONG).show();
        });
    }

    public static void showToast(Context context, CharSequence text) {
        if (context == null) return;
        runOnMainThread(() -> {
            Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        });
    }

    public static void showToastLong(Context context, CharSequence text) {
        if (context == null) return;
        runOnMainThread(() -> {
            Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_LONG).show();
        });
    }

    private static void runOnMainThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

}
