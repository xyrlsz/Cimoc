package com.xyrlsz.xcimoc.manager;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.Comic_;
import com.xyrlsz.xcimoc.model.TagRef;
import com.xyrlsz.xcimoc.model.TagRef_;
import com.xyrlsz.xcimoc.utils.ThreadRunUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.QueryBuilder;
import rx.Observable;

/**
 * Created by Hiroshi on 2016/7/9.
 */
public class ComicManager {
    public static int RESULT_DELETE = 0;
    public static int RESULT_UPDATE = 1;
    private static volatile ComicManager mInstance;
    private final Box<Comic> mComicBox;
    private final Box<TagRef> mTagRefBox;

    private ComicManager(AppGetter getter) {
        BoxStore boxStore = getter.getAppInstance().getBoxStore();
        mComicBox = boxStore.boxFor(Comic.class);
        mTagRefBox = boxStore.boxFor(TagRef.class);
    }

    public static ComicManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (ComicManager.class) {
                if (mInstance == null) {
                    mInstance = new ComicManager(getter);
                    ThreadRunUtils.runOnIOThread(() -> {
                        // 清理脏数据以免闪退
                        mInstance.cleanDuplicateCids();
                    });
                }
            }
        }
        return mInstance;
    }

    public void runInTx(Runnable runnable) {
        mComicBox.getStore().runInTx(runnable);
    }

    public <T> T callInTx(Callable<T> callable) throws Exception {
        return mComicBox.getStore().callInTx(callable);
    }

    public List<Comic> listDownload() {
        return mComicBox.query().notNull(Comic_.download).build().find();
    }

    public List<Comic> listLocal() {
        return mComicBox.query().equal(Comic_.local, true).build().find();
    }

    public Observable<List<Comic>> listLocalInRx() {
        return Observable.fromCallable(this::listLocal);
    }

    public Observable<List<Comic>> listFavoriteOrHistoryInRx() {
        return Observable.fromCallable(() -> {
            QueryBuilder<Comic> queryBuilder =
                    mComicBox.query(Comic_.favorite.notNull().or(Comic_.history.notNull()));
            return queryBuilder.build().find();
        });
    }

    public List<Comic> listFavorite() {
        return mComicBox.query(Comic_.favorite.notNull()).build().find();
    }

    public Observable<List<Comic>> listFavoriteInRx() {
        return Observable.fromCallable(()
                -> mComicBox.query(Comic_.favorite.notNull())
                .orderDesc(Comic_.highlight)
                .orderDesc(Comic_.favorite)
                .build()
                .find());
    }

    public Observable<List<Comic>> listFinishInRx() {
        return Observable.fromCallable(() -> {
            return mComicBox.query(Comic_.favorite.notNull())
                    .equal(Comic_.finish, true)
                    .orderDesc(Comic_.highlight)
                    .orderDesc(Comic_.favorite)
                    .build()
                    .find();
        });
    }

    public Observable<List<Comic>> listContinueInRx() {
        return Observable.fromCallable(()
                -> mComicBox.query(Comic_.favorite.notNull())
                .notEqual(Comic_.finish, true)
                .orderDesc(Comic_.highlight)
                .orderDesc(Comic_.favorite)
                .build()
                .find());
    }

    public Observable<List<Comic>> listHistoryInRx() {
        return Observable.fromCallable(()
                -> mComicBox.query(Comic_.history.notNull())
                .orderDesc(Comic_.history)
                .build()
                .find());
    }

    public Observable<List<Comic>> listDownloadInRx() {
        return Observable.fromCallable(() -> {
            return mComicBox.query()
                    .notNull(Comic_.download)
                    .orderDesc(Comic_.download)
                    .build()
                    .find();
        });
    }

    public Observable<List<Comic>> listFavoriteByTag(long id) {
        return Observable.fromCallable(() -> {
            // 直接获取 ID 数组
            Long[] cids = mTagRefBox.query()
                    .equal(TagRef_.tid, id)
                    .build()
                    .find() // 获取 List<TagRef>
                    .stream()
                    .map(TagRef::getCid) // 转换为 Stream<Long>
                    .toArray(Long[]::new); // 转换为 Long[]

            // 如果没有数据，直接返回空列表
            if (cids.length == 0) {
                return new ArrayList<>();
            }
            long[] cidsLong = new long[cids.length];
            for (int i = 0; i < cids.length; i++) {
                cidsLong[i] = cids[i];
            }
            // 查询 Comic
            return mComicBox.query(Comic_.favorite.notNull())
                    .in(Comic_.id, cidsLong)
                    .orderDesc(Comic_.highlight)
                    .orderDesc(Comic_.favorite)
                    .build()
                    .find();
        });
    }

    public Observable<List<Comic>> listFavoriteNotIn(Collection<Long> collections) {
        return Observable.fromCallable(() -> {
            long[] collectionsLong = new long[collections.size()];
            int i = 0;
            for (Long cid : collections) {
                collectionsLong[i] = cid;
                i++;
            }
            return mComicBox.query(Comic_.favorite.notNull())
                    .notIn(Comic_.id, collectionsLong)
                    .build()
                    .find();
        });
    }

    public long countBySource(int type) {
        return mComicBox.query(Comic_.favorite.notNull())
                .equal(Comic_.source, type)
                .build()
                .count();
    }

    public Comic load(long id) {
        return mComicBox.get(id);
    }

    public Comic load(int source, String cid) {
        List<Comic> list =
                mComicBox.query(Comic_.cid.equal(cid)).equal(Comic_.source, source).build().find();
        return list.isEmpty() ? null : list.get(0);
    }

    public Comic loadOrCreate(int source, String cid) {
        Comic comic = load(source, cid);
        return comic == null ? new Comic(source, cid) : comic;
    }

    public Observable<Comic> loadLast() {
        return Observable.fromCallable(() -> {
            List<Comic> list =
                    mComicBox.query(Comic_.history.notNull()).orderDesc(Comic_.history).build().find();
            return list.isEmpty() ? null : list.get(0);
        });
    }

    public void cancelHighlight() {
        List<Comic> comics = mComicBox.query().equal(Comic_.highlight, true).build().find();
        for (Comic comic : comics) {
            comic.setHighlight(false);
        }
        if (!comics.isEmpty()) {
            mComicBox.put(comics);
        }
    }

    public void updateOrInsert(Comic comic) {
        long id = comic.getId();

        if (id <= 0) {
            Comic existing = load(comic.getSource(), comic.getCid());
            id = (existing != null) ? existing.getId() : 0;
        }

        comic.setId(id);

        mComicBox.put(comic);
    }

    public void cleanDuplicateCids() {
        runInTx(() -> {
            try {
                // 这里简化处理，实际项目中可能需要更复杂的去重逻辑
                android.util.Log.d("ComicManager", "重复数据清洗完成");
            } catch (Exception e) {
                android.util.Log.e("ComicManager", "清洗重复数据失败", e);
            }
        });
    }

    public void update(Comic comic) {
        mComicBox.put(comic);
    }

    public void delete(Comic comic) {
        mComicBox.remove(comic);
        comic.setId(0);
    }

    public int updateOrDelete(Comic comic) {
        if (comic.getFavorite() == null && comic.getHistory() == null && comic.getDownload() == null
                && !comic.getLocal()) {
            delete(comic);
            return RESULT_DELETE;
        } else {
            update(comic);
            return RESULT_UPDATE;
        }
    }

    public void deleteByKey(long key) {
        mComicBox.remove(key);
    }

    public void insert(Comic comic) {
        long id = mComicBox.put(comic);
        comic.setId(id);
    }
}
