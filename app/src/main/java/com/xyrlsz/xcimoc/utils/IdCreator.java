package com.xyrlsz.xcimoc.utils;

import com.xyrlsz.xcimoc.model.Comic;

public class IdCreator {

    public static long createChapterId(Long sourceComic, int num) {
        sourceComic = sourceComic == null ? 0 : sourceComic;
        return (sourceComic << 16) | (num & 0xFFFF);
    }

    public static long createImageId(Long chapterId, int num) {
        chapterId = chapterId == null ? 0 : chapterId;
        return (chapterId << 16) | (num & 0xFFFF);
    }

    public static Long createSourceComic(Comic comic) {
        return createSourceComic(comic.getSource(), comic.getId());
    }

    public static Long createSourceComic(int source, Long comicID) {
        comicID = comicID == null ? 0 : comicID;
        return (comicID << 16) | (source & 0xFFFF);
    }

    public static Long recreateSourceComic(Long oldSourceComic, Long newComicID) {
        if (oldSourceComic == null) {
            return null;
        }
        int source = (int) (oldSourceComic & 0xFFFFL);
        newComicID = newComicID == null ? 0L : newComicID;
        return (newComicID << 16) | (source & 0xFFFFL);
    }

    public static Long recreateChapterId(Long newSourceComic, Long oldChapterId) {
        if (oldChapterId == null) {
            newSourceComic = newSourceComic == null ? 0 : newSourceComic;
            return createChapterId(newSourceComic, 0);
        }
        int num = (int) (oldChapterId & 0xFFFFL);
        newSourceComic = newSourceComic == null ? 0 : newSourceComic;
        return (newSourceComic << 16) | (num & 0xFFFFL);
    }
}
