package com.haleydu.cimoc.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "chapter")
public class Chapter implements Parcelable {

    public static final Parcelable.Creator<Chapter> CREATOR = new Parcelable.Creator<Chapter>() {
        @Override
        public Chapter createFromParcel(Parcel source) {
            return new Chapter(source);
        }

        @Override
        public Chapter[] newArray(int size) {
            return new Chapter[size];
        }
    };

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "source_comic")
    private Long sourceComic;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "path")
    private String path;

    @ColumnInfo(name = "count")
    private int count;

    @ColumnInfo(name = "complete")
    private boolean complete;

    @ColumnInfo(name = "download")
    private boolean download;

    @ColumnInfo(name = "tid")
    private long tid;

    @ColumnInfo(name = "source_group")
    private String sourceGroup;

    public Chapter(Long id, Long sourceComic, String title, String path, int count, boolean complete, boolean download, long tid, String sourceGroup) {
        this.id = id;
        this.sourceComic = sourceComic;
        this.title = title;
        this.path = path;
        this.count = count;
        this.complete = complete;
        this.download = download;
        this.tid = tid;
        this.sourceGroup = sourceGroup;
    }

    @Ignore
    public Chapter(Parcel source) {
        this.id = source.readLong();
        this.sourceComic = source.readLong();
        this.title = source.readString();
        this.path = source.readString();
        this.count = source.readInt();
        this.complete = source.readByte() == 1;
        this.download = source.readByte() == 1;
        this.tid = source.readLong();
        this.sourceGroup = "";
    }

    @Ignore
    public Chapter(String title, String path) {
        this.title = title;
        this.path = path;
        this.count = 0;
        this.complete = false;
        this.download = false;
        this.tid = -1;
    }


    @Ignore
    public Chapter(Long id, Long sourceComic, String title, String path) {
        this(id, sourceComic, title, path, 0, false, false, -1, "");
    }

    @Ignore
    public Chapter(Long id, Long sourceComic, String title, String path, long tid) {
        this(id, sourceComic, title, path, 0, false, false, tid, "");
    }

    @Ignore
    public Chapter(Long id, Long sourceComic, String title, String path, String sourceGroup) {
        this(id, sourceComic, title, path, 0, false, false, -1, sourceGroup);

    }

    @Ignore
    public Chapter(Long id, Long sourceComic, String title, String path, int progress, boolean b, boolean b1, Long id1) {
        this(id, sourceComic, title, path, progress, b, b1, id1, "");
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceComic() {
        return sourceComic;
    }

    public void setSourceComic(Long sourceComic) {
        this.sourceComic = sourceComic;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }

    public long getTid() {
        return tid;
    }

    public void setTid(long tid) {
        this.tid = tid;
    }

    public String getSourceGroup() {
        return sourceGroup == null ? "" : sourceGroup;
    }

    public void setSourceGroup(String sourceGroup) {
        this.sourceGroup = sourceGroup;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Chapter && ((Chapter) o).path.equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id != null ? id : 0L);
        dest.writeLong(sourceComic != null ? sourceComic : 0L);
        dest.writeString(title);
        dest.writeString(path);
        dest.writeInt(count);
        dest.writeByte((byte) (complete ? 1 : 0));
        dest.writeByte((byte) (download ? 1 : 0));
        dest.writeLong(tid);
    }
}