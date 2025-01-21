package com.haleydu.cimoc.utils;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.manager.PreferenceManager;

public class STConvertUtils {
    public static String convert(final String s) {
        if (s == null) {
            return null;
        }
        String input = s;
        PreferenceManager preferenceManager = App.getPreferenceManager();
        switch (preferenceManager.getInt(PreferenceManager.PREF_DETAIL_TEXT_ST, PreferenceManager.DETAIL_TEXT_DEFAULT)) {
            case PreferenceManager.DETAIL_TEXT_SIMPLE:
                try {
                    input = xyropencc.Xyropencc.t2S(s);

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case PreferenceManager.DETAIL_TEXT_TRADITIONAL:
                try {
                    input = xyropencc.Xyropencc.s2T(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;

        }
        return input;
    }
}