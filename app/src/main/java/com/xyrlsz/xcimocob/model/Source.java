package com.xyrlsz.xcimocob.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;

/**
 * Created by Hiroshi on 2016/8/11.
 */
@Entity
public class Source {
    @Id(assignable = true)
    private long id;
    private String title;
    @Unique
    private int type;
    private boolean enable;
    private String baseUrl;

    public Source() {
    }

    public Source(long id, String title, int type, boolean enable, String baseUrl) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.enable = enable;
        this.baseUrl = baseUrl;
    }

    public Source(long id, String title, int type, boolean enable) {
        this(id, title, type, enable, null);
    }

    public Source(Long id, String title, int type, boolean enable) {
        this(id == null ? 0 : id, title, type, enable, null);
    }

    public Source(Long o, Object title, int type, boolean enable, Object baseUrl) {
        this(o == null ? 0 : o, title == null ? null : title.toString(), type, enable, baseUrl == null ? null : baseUrl.toString());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Source && ((Source) o).id == id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean getEnable() {
        return this.enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
