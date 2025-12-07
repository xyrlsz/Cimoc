package com.xyrlsz.xcimoc.model;

public class CategoryMiniComic {
    private final String mCid;
    private final String mTitle;
    private final String mCover;
    private final int mSource;

    public CategoryMiniComic(Comic comic) {
        mCid = comic.getCid();
        mTitle = comic.getTitle();
        mCover = comic.getCover();
        mSource = comic.getSource();
    }

    public String getCover() {
        return mCover;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getCid() {
        return mCid;
    }

    public int getSource() {
        return mSource;
    }
}