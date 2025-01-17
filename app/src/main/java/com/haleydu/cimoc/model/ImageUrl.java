package com.haleydu.cimoc.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;

import com.haleydu.cimoc.database.StringArrayConverter;

@Entity(tableName = "image_url")
@TypeConverters(StringArrayConverter.class)
public class ImageUrl {

    public static final int STATE_NULL = 0;
    public static final int STATE_PAGE_1 = 1;
    public static final int STATE_PAGE_2 = 2;

    @PrimaryKey(autoGenerate = true)
    private Long id; // 唯一标识

    @ColumnInfo(name = "comic_chapter")
    private Long comicChapter;

    @ColumnInfo(name = "num")
    private int num;    // 章节的第几页

    @ColumnInfo(name = "urls")
    private String[] urls;

    @ColumnInfo(name = "chapter")
    private String chapter; // 所属章节

    @ColumnInfo(name = "state")
    private int state;  // 切图时表示状态 这里可以改为编号 比如长图可以切为多张方便加载

    @ColumnInfo(name = "height")
    private int height; // 图片高度

    @ColumnInfo(name = "width")
    private int width;  // 图片宽度

    @ColumnInfo(name = "lazy")
    private boolean lazy;   // 懒加载

    @ColumnInfo(name = "loading")
    private boolean loading;    // 正在懒加载

    @ColumnInfo(name = "success")
    private boolean success;    // 图片显示成功

    @ColumnInfo(name = "download")
    private boolean download;   // 下载的图片
    @Ignore
    public ImageUrl(Long id, Long comicChapter, int num, String[] urls, String chapter, int state, boolean lazy) {
        this(id, comicChapter, num, urls, chapter, state, 0, 0, lazy,
                false, false, false);
    }
    @Ignore
    public ImageUrl(Long id, Long comicChapter, int num, String url, boolean lazy) {
        this(id, comicChapter, num, new String[]{url}, null, STATE_NULL,
                0, 0, lazy, false, false, false);
    }

    public ImageUrl(Long id,  Long comicChapter, int num, String[] urls,
                    String chapter, int state, int height, int width, boolean lazy, boolean loading,
                    boolean success, boolean download) {
        this.id = id;
        this.comicChapter = comicChapter;
        this.num = num;
        this.urls = urls;
        this.chapter = chapter;
        this.state = state;
        this.height = height;
        this.width = width;
        this.lazy = lazy;
        this.loading = loading;
        this.success = success;
        this.download = download;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getComicChapter() {
        return comicChapter;
    }

    public void setComicChapter(Long comicChapter) {
        this.comicChapter = comicChapter;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String[] getUrls() {
        return urls;
    }

    public void setUrls(String[] urls) {
        this.urls = urls;
    }

    public String getUrl() {
        return urls[0];
    }

    public void setUrl(String url) {
        this.urls = new String[]{url};
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public long getSize() {
        return height * width;
    }

    public boolean isLazy() {
        return lazy;
    }

    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ImageUrl && ((ImageUrl) o).id.equals(id);
    }
}