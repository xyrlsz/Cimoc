package com.xyrlsz.xcimoc.utils;

public class IdCreator {
    public static long chapterIdCreate(Long sourceComic, int num) {
        return (sourceComic << 16) | (num & 0xFFFF);
    }

    public static long imageIdCreate(Long chapterId, int num) {
        return (chapterId << 16) | (num & 0xFFFF);
    }
}
