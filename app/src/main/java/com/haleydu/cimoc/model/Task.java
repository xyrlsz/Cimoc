package com.haleydu.cimoc.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

@Entity(tableName = "task")
public class Task implements Parcelable {

    public static final int STATE_FINISH = 0;
    public static final int STATE_PAUSE = 1;
    public static final int STATE_PARSE = 2;
    public static final int STATE_DOING = 3;
    public static final int STATE_WAIT = 4;
    public static final int STATE_ERROR = 5;

    public static final Parcelable.Creator<Task> CREATOR = new Parcelable.Creator<Task>() {
        @Override
        public Task createFromParcel(Parcel source) {
            return new Task(source);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private Long id;

    @ColumnInfo(name = "key")
    private long key;      // 漫画主键

    @ColumnInfo(name = "path")
    private String path;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "progress")

    private int progress;

    @ColumnInfo(name = "max")

    private int max;

    @Ignore
    private int source;

    @Ignore
    private String cid;  // 漫画 ID

    @Ignore
    private int state;
    @Ignore
    public Task() {
    }

    public Task(Long id, long key, String path,  String title, int progress, int max) {
        this.id = id;
        this.key = key;
        this.path = path;
        this.title = title;
        this.progress = progress;
        this.max = max;
    }
    @Ignore
    public Task(Parcel source) {
        this.id = source.readLong();
        this.key = source.readLong();
        this.path = source.readString();
        this.title = source.readString();
        this.progress = source.readInt();
        this.max = source.readInt();
        this.source = source.readInt();
        this.cid = source.readString();
        this.state = source.readInt();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Task && ((Task) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id == null ? super.hashCode() : id.hashCode();
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getKey() {
        return this.key;
    }

    public void setKey(long key) {
        this.key = key;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMax() {
        return this.max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getSource() {
        return this.source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public boolean isFinish() {
        return max != 0 && progress == max;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(key);
        dest.writeString(path);
        dest.writeString(title);
        dest.writeInt(progress);
        dest.writeInt(max);
        dest.writeInt(source);
        dest.writeString(cid);
        dest.writeInt(state);
    }
}