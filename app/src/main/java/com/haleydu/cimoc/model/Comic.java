package com.haleydu.cimoc.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "comic")
public class Comic {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "source")
    private int source;

    @ColumnInfo(name = "cid")
    private String cid;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "cover")
    private String cover;

    @ColumnInfo(name = "highlight")
    private boolean highlight;

    @ColumnInfo(name = "local")
    private boolean local;

    @ColumnInfo(name = "update")
    private String update;

    @ColumnInfo(name = "finish")
    private Boolean finish;

    @ColumnInfo(name = "favorite")
    private Long favorite;

    @ColumnInfo(name = "history")
    private Long history;

    @ColumnInfo(name = "download")
    private Long download;

    @ColumnInfo(name = "last")
    private String last;

    @ColumnInfo(name = "page")
    private Integer page;

    @ColumnInfo(name = "chapter")
    private String chapter;

    @ColumnInfo(name = "url")
    private String url;

    @ColumnInfo(name = "intro")
    private String intro;

    @ColumnInfo(name = "author")
    private String author;

    // Transient fields are ignored by Room
    public transient Object note;
    @Ignore
    public Comic(int source, String cid, String title, String cover, String update, String author) {
        this(null, source, cid, title, cover == null ? "" : cover, false, false, update,
                null, null, null, null, null, null, null, null, null, null);
        this.author = author;
    }
    @Ignore
    public Comic(int source, String cid) {
        this.source = source;
        this.cid = cid;
    }
    @Ignore
    public Comic(int source, String cid, String title, String cover, long download) {
        this(null, source, cid, title, cover == null ? "" : cover, false, false, null,
                null, null, null, download, null, null, null, null, null, null);
    }

    public Comic(Long id, int source,   String cid,   String title,  String cover, boolean highlight,
                 boolean local, String update, Boolean finish, Long favorite, Long history, Long download, String last, Integer page,
                 String chapter, String url, String intro, String author) {
        this.id = id;
        this.source = source;
        this.cid = cid;
        this.title = title;
        this.cover = cover;
        this.highlight = highlight;
        this.local = local;
        this.update = update;
        this.finish = finish;
        this.favorite = favorite;
        this.history = history;
        this.download = download;
        this.last = last;
        this.page = page;
        this.chapter = chapter;
        this.url = url;
        this.intro = intro;
        this.author = author;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Comic && ((Comic) o).id.equals(id);
    }

    public void setInfo(String title, String cover, String update, String intro, String author, boolean finish) {
        if (title != null) {
            this.title = title;
        }
        if (cover != null) {
            this.cover = cover;
        }
        if (update != null) {
            this.update = update;
        }
        this.intro = intro;
        if (author != null) {
            this.author = author;
        }
        this.finish = finish;
        this.highlight = false;
    }

    public String getIntro() {
        return this.intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getPage() {
        return this.page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public String getLast() {
        return this.last;
    }

    public void setLast(String last) {
        this.last = last;
    }

    public Long getHistory() {
        return this.history;
    }

    public void setHistory(Long history) {
        this.history = history;
    }

    public Long getFavorite() {
        return this.favorite;
    }

    public void setFavorite(Long favorite) {
        this.favorite = favorite;
    }

    public String getUpdate() {
        return this.update;
    }

    public void setUpdate(String update) {
        this.update = update;
    }

    public String getCover() {
        return this.cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCid() {
        return this.cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public int getSource() {
        return this.source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean getHighlight() {
        return this.highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public Long getDownload() {
        return this.download;
    }

    public void setDownload(Long download) {
        this.download = download;
    }

    public Boolean getFinish() {
        return this.finish;
    }

    public void setFinish(Boolean finish) {
        this.finish = finish;
    }

    public boolean getLocal() {
        return this.local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public String getChapter() {
        return this.chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}