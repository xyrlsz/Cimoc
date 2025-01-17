package com.haleydu.cimoc.manager;

import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.database.AppDatabase;
import com.haleydu.cimoc.model.TagRef;

import java.util.List;

import rx.Observable;

/**
 * Created by Hiroshi on 2017/1/16.
 * Modified for Room database.
 */
public class TagRefManager {

    private static TagRefManager mInstance;

    private AppDatabase mDatabase;

    private TagRefManager(AppGetter getter) {
        mDatabase = getter.getAppInstance().getAppDatabase();
    }

    public static TagRefManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (TagRefManager.class) {
                if (mInstance == null) {
                    mInstance = new TagRefManager(getter);
                }
            }
        }
        return mInstance;
    }

    public List<TagRef> listByTag(long tid) {
        return mDatabase.tagRefDao().findByTagId(tid);
    }

    public List<TagRef> listByComic(long cid) {
        return mDatabase.tagRefDao().findByComicId(cid);
    }

    public TagRef load(long tid, long cid) {
        return mDatabase.tagRefDao().findByTagAndComicId(tid, cid);
    }

    public long insert(TagRef ref) {
        return mDatabase.tagRefDao().insert(ref);
    }

    public void insert(Iterable<TagRef> entities) {
        mDatabase.tagRefDao().insertAll(entities);
    }

    public void insertInTx(Iterable<TagRef> entities) {
        mDatabase.runInTransaction(() -> mDatabase.tagRefDao().insertAll(entities));
    }

    public void deleteByTag(long tid) {
        mDatabase.tagRefDao().deleteByTagId(tid);
    }

    public void deleteByComic(long cid) {
        mDatabase.tagRefDao().deleteByComicId(cid);
    }

    public void delete(long tid, long cid) {
        mDatabase.tagRefDao().deleteByTagAndComicId(tid, cid);
    }

    public void runInTx(Runnable runnable)
    {
        mDatabase.runInTransaction(runnable);
    }

    public Observable<Object> runInRx(Runnable runnable) {
        return Observable.unsafeCreate(subscriber -> {
            mDatabase.runInTransaction(runnable);
            subscriber.onNext(null);
            subscriber.onCompleted();
        });
    }
}