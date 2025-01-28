package com.xyrlsz.xcimoc.utils;

import android.content.Context;
import android.util.TypedValue;

import com.xyrlsz.xcimoc.R;

/**
 * Created by Hiroshi on 2016/10/2.
 */

public class ThemeUtils {

    public static final int THEME_PINK = 0;
    public static final int THEME_GREY = 1;
    public static final int THEME_TEAL = 2;
    public static final int THEME_PURPLE = 3;
    public static final int THEME_BLUE = 4;
    public static final int THEME_BROWN = 5;

    public static int getResourceId(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.resourceId;
    }

    public static int getThemeById(int id) {
        switch (id) {
            default:
            case THEME_PINK:
                return R.style.AppThemePink;
            case THEME_GREY:
                return R.style.AppThemeGrey;
            case THEME_TEAL:
                return R.style.AppThemeTeal;
            case THEME_PURPLE:
                return R.style.AppThemePurple;
            case THEME_BLUE:
                return R.style.AppThemeBlue;
            case THEME_BROWN:
                return R.style.AppThemeBrown;
        }
    }

    public static int getDialogThemeById(int id) {
        switch (id) {
            default:
            case THEME_PINK:
                return R.style.DialogThemePink;
            case THEME_GREY:
                return R.style.DialogThemeGrey;
            case THEME_TEAL:
                return R.style.DialogThemeTeal;
            case THEME_PURPLE:
                return R.style.DialogThemePurple;
            case THEME_BLUE:
                return R.style.DialogThemeBlue;
            case THEME_BROWN:
                return R.style.DialogThemeBrown;
        }
    }

    public static int getThemeColorById(int id) {
        switch (id) {
            default:
            case THEME_PINK:
                return R.color.colorPrimaryPink;
            case THEME_GREY:
                return R.color.colorPrimaryGrey;
            case THEME_TEAL:
                return R.color.colorPrimaryTeal;
            case THEME_PURPLE:
                return R.color.colorPrimaryPurple;
            case THEME_BLUE:
                return R.color.colorPrimaryBlue;
            case THEME_BROWN:
                return R.color.colorPrimaryBrown;
        }
    }

}
