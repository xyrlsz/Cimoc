package com.xyrlsz.xcimoc.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;

/**
 * Created by Hiroshi on 2016/10/10.
 */
@Entity
public class Tag {
    @Id(assignable = true)
    private long id;
    @Unique
    private String title;

    public Tag(long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Tag(Long id, String title) {
        this.id = id == null ? 0 : id;
        this.title = title;
    }

    public Tag() {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tag && ((Tag) o).id == id;
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
}
