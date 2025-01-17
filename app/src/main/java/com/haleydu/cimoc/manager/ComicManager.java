package com.haleydu.cimoc.manager;

import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.database.AppDatabase;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.utils.ObservableUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

//import io.reactivex.Single;
//import hu.akarnokd.rxjava.interop.RxJavaInterop;
import hu.akarnokd.rxjava3.interop.RxJavaInterop;
import rx.Observable;
import rx.Single;


/**
 * Created by Hiroshi on 2016/7/9.
 * Modified for Room database.
 */
public class ComicManager {

    private static ComicManager mInstance;

    private AppDatabase mDatabase;

    private ComicManager(AppGetter getter) {
        mDatabase = getter.getAppInstance().getAppDatabase();
    }

    public static ComicManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (ComicManager.class) {
                if (mInstance == null) {
                    mInstance = new ComicManager(getter);
                }
            }
        }
        return mInstance;
    }

    public void runInTx(Runnable runnable) {
        mDatabase.runInTransaction(runnable);
    }

    public <T> T callInTx(Callable<T> callable) {
        return mDatabase.runInTransaction(callable);
    }

    public List<Comic> listDownload() {
        return mDatabase.comicDao().findByDownloadNotNull();
    }

    public List<Comic> listLocal() {
        return mDatabase.comicDao().findByLocal(true);
    }

    public Observable<List<Comic>> listLocalInRx() {
        return ObservableUtils.V3toV1(mDatabase.comicDao().findByLocalRx(true).toObservable());
    }

    public Observable<List<Comic>> listFavoriteOrHistoryInRx() {
        return ObservableUtils.V3toV1(mDatabase.comicDao().findByFavoriteOrHistoryRx().toObservable());
    }

    public List<Comic> listFavorite() {
        return mDatabase.comicDao().findByFavoriteNotNull();
    }

    public Observable<List<Comic>> listFavoriteInRx() {
        return ObservableUtils.V3toV1(mDatabase.comicDao().findByFavoriteNotNullRx().toObservable());
    }

    public Observable<List<Comic>> listFinishInRx() {
        return ObservableUtils.V3toV1( mDatabase.comicDao().findByFavoriteAndFinishRx(true).toObservable());
    }

    public Observable<List<Comic>> listContinueInRx() {
        return ObservableUtils.V3toV1(mDatabase.comicDao().findByFavoriteAndFinishRx(false).toObservable());
    }

    public Observable<List<Comic>> listHistoryInRx() {
        return ObservableUtils.V3toV1(mDatabase.comicDao().findByHistoryNotNullRx().toObservable());
    }

    public Observable<List<Comic>> listDownloadInRx() {
        return ObservableUtils.V3toV1(mDatabase.comicDao().findByDownloadNotNullRx().toObservable());
    }

    public Observable<List<Comic>> listFavoriteByTag(long tid) {
        return ObservableUtils.V3toV1( mDatabase.comicDao().findByTagIdRx(tid).toObservable());
    }

    public Observable<List<Comic>> listFavoriteNotIn(Collection<Long> collections) {
        return ObservableUtils.V3toV1(mDatabase.comicDao().findByFavoriteNotInRx(collections).toObservable());
    }

    public long countBySource(int type) {
        return mDatabase.comicDao().countBySource(type);
    }

    public Comic load(long id) {
        return mDatabase.comicDao().findById(id);
    }

    public Comic load(int source, String cid) {
        return mDatabase.comicDao().findBySourceAndCid(source, cid);
    }

    public Comic loadOrCreate(int source, String cid) {
        Comic comic = load(source, cid);
        return comic == null ? new Comic(source, cid) : comic;
    }

    public Single<Comic> loadLast() {

        return RxJavaInterop.toV1Single(mDatabase.comicDao().findLastRx());
    }

    public void cancelHighlight() {
        mDatabase.comicDao().cancelHighlight();
    }

    public void updateOrInsert(Comic comic) {
        if (comic.getId() == null) {
            insert(comic);
        } else {
            update(comic);
        }
    }

    public void update(Comic comic) {
        mDatabase.comicDao().update(comic);
    }

    public void insertOrReplace(Comic comic) {
        mDatabase.comicDao().insertOrReplace(comic);
    }

    public void updateOrDelete(Comic comic) {
        if (comic.getFavorite() == null && comic.getHistory() == null && comic.getDownload() == null) {
            mDatabase.comicDao().delete(comic);
            comic.setId(null);
        } else {
            update(comic);
        }
    }

    public void deleteByKey(long key) {
        mDatabase.comicDao().deleteById(key);
    }

    public void insert(Comic comic) {
        long id = mDatabase.comicDao().insert(comic);
        comic.setId(id);
    }
}