package com.xyrlsz.xcimoc.parser;

import android.util.Pair;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hiroshi on 2016/12/10.
 */

public abstract class MangaCategory implements Category {

    public static Map<Integer, String> getParseFormatMap(String format) {
        Gson gson = new Gson();
        return gson.fromJson(format,
                new com.google.gson.reflect.TypeToken<Map<Integer, String>>() {
                }.getType());
    }

    @Override
    public boolean isComposite() {
        return false;
    }

    protected abstract List<Pair<String, String>> getSubject();

    protected boolean hasArea() {
        return false;
    }

    protected List<Pair<String, String>> getArea() {
        return null;
    }

    protected boolean hasReader() {
        return false;
    }

    protected List<Pair<String, String>> getReader() {
        return null;
    }

    protected boolean hasProgress() {
        return false;
    }

    @Override
    public String getFormat(String... args) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            map.put(i, args[i]);
        }
        Gson gson = new Gson();
        return gson.toJson(map);
    }

    protected List<Pair<String, String>> getProgress() {
        return null;
    }

    protected boolean hasYear() {
        return false;
    }

    protected List<Pair<String, String>> getYear() {
        return null;
    }

    protected boolean hasOrder() {
        return false;
    }

    protected List<Pair<String, String>> getOrder() {
        return null;
    }

    @Override
    public boolean hasAttribute(@Attribute int attr) {
        switch (attr) {
            case CATEGORY_SUBJECT:
                return true;
            case CATEGORY_AREA:
                return hasArea();
            case CATEGORY_READER:
                return hasReader();
            case CATEGORY_PROGRESS:
                return hasProgress();
            case CATEGORY_YEAR:
                return hasYear();
            case CATEGORY_ORDER:
                return hasOrder();
        }
        return false;
    }

    @Override
    public List<Pair<String, String>> getAttrList(@Attribute int attr) {
        switch (attr) {
            case CATEGORY_SUBJECT:
                return getSubject();
            case CATEGORY_AREA:
                return getArea();
            case CATEGORY_READER:
                return getReader();
            case CATEGORY_PROGRESS:
                return getProgress();
            case CATEGORY_YEAR:
                return getYear();
            case CATEGORY_ORDER:
                return getOrder();
        }
        return null;
    }

}
