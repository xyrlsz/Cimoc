package com.xyrlsz.xcimoc.manager;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.TagRef;
import com.xyrlsz.xcimoc.model.TagRef_;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2017/1/16.
 */
public class TagRefManager {

    private static volatile TagRefManager mInstance;

    // 1. 修改：使用 ObjectBox 的 Box 替代 TagRefDao
    private final Box<TagRef> mRefBox;

    private TagRefManager(AppGetter getter) {
        // 2. 修改：从 BoxStore 获取 Box
        BoxStore boxStore = getter.getAppInstance().getBoxStore();
        mRefBox = boxStore.boxFor(TagRef.class);
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

    // 3. 修改：事务处理
    public Observable<Object> runInRx(Runnable runnable) {
        return Observable.fromCallable(() -> {
            mRefBox.getStore().runInTx(runnable);
            return null;
        }).subscribeOn(Schedulers.io());
    }

    public void runInTx(Runnable runnable) {
        mRefBox.getStore().runInTx(runnable);
    }

    // 4. 修改：查询方法
    public List<TagRef> listByTag(long tid) {
        return mRefBox.query()
                .equal(TagRef_.tid, tid)
                .build()
                .find();
    }

    public List<TagRef> listByComic(long cid) {
        return mRefBox.query()
                .equal(TagRef_.cid, cid)
                .build()
                .find();
    }

    // 5. 修改：load 方法。ObjectBox 使用 findFirst 替代 unique
    public TagRef load(long tid, long cid) {
        return mRefBox.query()
                .equal(TagRef_.tid, tid)
                .equal(TagRef_.cid, cid)
                .build()
                .findFirst();
    }

    // 6. 修改：插入操作
    public long insert(TagRef ref) {
        return mRefBox.put(ref);
    }

    public void insertInTx(Iterable<TagRef> entities) {
        mRefBox.getStore().runInTx(() -> {
            for (TagRef tagRef : entities) {
                mRefBox.put(tagRef);
            }
        });
    }

    public void deleteByTag(long tid) {
        mRefBox.query()
                .equal(TagRef_.tid, tid)
                .build()
                .remove();
    }

    public void deleteByComic(long cid) {
        mRefBox.query()
                .equal(TagRef_.cid, cid)
                .build()
                .remove();
    }

    public void delete(long tid, long cid) {
        mRefBox.query()
                .equal(TagRef_.tid, tid)
                .equal(TagRef_.cid, cid)
                .build()
                .remove();
    }

}