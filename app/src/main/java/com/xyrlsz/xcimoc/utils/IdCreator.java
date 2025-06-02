package com.xyrlsz.xcimoc.utils;

import com.xyrlsz.xcimoc.model.Comic;

public class IdCreator {

    public static long createChapterId(Long sourceComic, int num) {
        return (sourceComic << 16) | (num & 0xFFFF);
    }

    public static long createImageId(Long chapterId, int num) {
        return (chapterId << 16) | (num & 0xFFFF);
    }

    public static Long createSourceComic(Comic comic) {
        return Long.parseLong(comic.getSource() + "0" + (comic.getId() == null ? "00" : comic.getId()));
    }

}
