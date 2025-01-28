package com.xyrlsz.xcimoc.utils;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.manager.PreferenceManager;

/**
 * Created by Hiroshi on 2016/9/22.
 */

public class HintUtils {

    public static void showSnackbar(View layout, String msg) {
        if (layout != null && layout.isShown()) {
            Snackbar snackbar = Snackbar.make(layout, msg, Snackbar.LENGTH_SHORT);
            int theme = App.getPreferenceManager().getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_PINK);
            snackbar.setBackgroundTint(App.getAppContext().getResources().getColor(ThemeUtils.getThemeColorById(theme)));
            snackbar.show();
        }
    }

    public static void showToast(Context context, int resId) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

}
