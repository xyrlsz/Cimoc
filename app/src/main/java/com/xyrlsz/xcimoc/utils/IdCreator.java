package com.xyrlsz.xcimoc.utils;

import com.xyrlsz.xcimoc.model.Comic;

public class IdCreator {

    public static long createChapterId(Long sourceComic, int num) {
        sourceComic = sourceComic == null? 0 : sourceComic;
        return (sourceComic << 16) | (num & 0xFFFF);
    }

    public static long createImageId(Long chapterId, int num) {
        chapterId = chapterId == null? 0 : chapterId;
        return (chapterId << 16) | (num & 0xFFFF);
    }

    public static Long createSourceComic(Comic comic) {
        return createSourceComic(comic.getSource(), comic.getId());
    }

    public static Long createSourceComic(int source, Long id) {
//        return Long.parseLong(source + "0" + (id == null? "00" : id));
        id = id == null? 0 : id;
        return (id << 16) | (source & 0xFFFF);
    }

}
