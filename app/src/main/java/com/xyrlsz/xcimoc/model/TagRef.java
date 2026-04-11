package com.xyrlsz.xcimoc.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

/**
 * Created by Hiroshi on 2016/10/10.
 */
@Entity
public class TagRef {
    @Id
    private long id;
    @Index
    private long tid;
    @Index
    private long cid;

    public TagRef(long id, long tid, long cid) {
        this.id = id;
        this.tid = tid;
        this.cid = cid;
    }

    public TagRef(Long id, long tid, long cid) {
        this.id = id == null ? 0 : id;
        this.tid = tid;
        this.cid = cid;
    }

    public TagRef() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTid() {
        return this.tid;
    }

    public void setTid(long tid) {
        this.tid = tid;
    }

    public long getCid() {
        return this.cid;
    }

    public void setCid(long cid) {
        this.cid = cid;
    }
}
