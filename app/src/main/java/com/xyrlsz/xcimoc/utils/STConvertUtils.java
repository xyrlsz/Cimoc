package com.xyrlsz.xcimoc.utils;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.manager.PreferenceManager;

import taobe.tec.jcc.JChineseConvertor;
//import xyropencc.Xyropencc;

public class STConvertUtils {

    public static String T2S(String s) {
        PreferenceManager preferenceManager = App.getPreferenceManager();
        try {
            switch (preferenceManager.getInt(PreferenceManager.PREF_ST_ENGINE, PreferenceManager.ST_JCC)) {
                case PreferenceManager.ST_JCC:
                    return JChineseConvertor.getInstance().t2s(s);
                case PreferenceManager.ST_OPENCC4J:
                    return ZhConverterUtil.toSimple(s);
//                case PreferenceManager.ST_OPENCCGO:
//                    return Xyropencc.t2S(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;

    }

    public static String S2T(String s) {
        PreferenceManager preferenceManager = App.getPreferenceManager();
        try {
            switch (preferenceManager.getInt(PreferenceManager.PREF_ST_ENGINE, PreferenceManager.ST_JCC)) {
                case PreferenceManager.ST_JCC:
                    return JChineseConvertor.getInstance().s2t(s);
                case PreferenceManager.ST_OPENCC4J:
                    return ZhConverterUtil.toTraditional(s);
//                case PreferenceManager.ST_OPENCCGO:
//                    return Xyropencc.s2T(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }

    public static String convert(final String s) {
        PreferenceManager preferenceManager = App.getPreferenceManager();

        switch (preferenceManager.getInt(PreferenceManager.PREF_DETAIL_TEXT_ST, PreferenceManager.DETAIL_TEXT_DEFAULT)) {
            case PreferenceManager.DETAIL_TEXT_SIMPLE:
                try {

                    switch (preferenceManager.getInt(PreferenceManager.PREF_ST_ENGINE, PreferenceManager.ST_JCC)) {
                        case PreferenceManager.ST_JCC:
                            return JChineseConvertor.getInstance().t2s(s);
                        case PreferenceManager.ST_OPENCC4J:
                            return ZhConverterUtil.toSimple(s);
//                        case PreferenceManager.ST_OPENCCGO:
//                            return Xyropencc.t2S(s);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case PreferenceManager.DETAIL_TEXT_TRADITIONAL:
                try {
                    switch (preferenceManager.getInt(PreferenceManager.PREF_ST_ENGINE, PreferenceManager.ST_JCC)) {
                        case PreferenceManager.ST_JCC:
                            return JChineseConvertor.getInstance().s2t(s);
                        case PreferenceManager.ST_OPENCC4J:
                            return ZhConverterUtil.toTraditional(s);
//                        case PreferenceManager.ST_OPENCCGO:
//                            return Xyropencc.s2T(s);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
//            case PreferenceManager.DETAIL_TEXT_TRADITIONAL_TW:
//                try {
//                    switch (preferenceManager.getInt(PreferenceManager.PREF_ST_ENGINE, PreferenceManager.ST_JCC)) {
//                        case PreferenceManager.ST_JCC:
//                            return JChineseConvertor.getInstance().s2t(s);
//                        case PreferenceManager.ST_OPENCC4J:
//                            return ZhTwConverterUtil.toTraditional(s);
//                        case PreferenceManager.ST_OPENCCGO:
//                            return Xyropencc.s2TWP(s);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                break;
            default:
                break;

        }
        return s;
    }

}