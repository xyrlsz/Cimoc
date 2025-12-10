package com.xyrlsz.xcimoc.manager;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.ImageUrlDao;

import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by HaleyDu on 2020/8/27.
 */
public class ImageUrlManager {

    private static ImageUrlManager mInstance;

    private final ImageUrlDao mImageUrlDao;

    private ImageUrlManager(AppGetter getter) {
        mImageUrlDao = getter.getAppInstance().getDaoSession().getImageUrlDao();
    }

    public static ImageUrlManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (ImageUrlManager.class) {
                if (mInstance == null) {
                    mInstance = new ImageUrlManager(getter);
                }
            }
        }
        return mInstance;
    }

    public void runInTx(Runnable runnable) {
        mImageUrlDao.getSession().runInTx(runnable);
    }

    public Observable<List<ImageUrl>> getListImageUrlRX(Long comicChapter) {
        return mImageUrlDao.queryBuilder()
                .where(ImageUrlDao.Properties.ComicChapter.eq(comicChapter))
                .rx()
                .list();
    }

    public List<ImageUrl> getListImageUrl(Long comicChapter) {
        return mImageUrlDao.queryBuilder()
                .where(ImageUrlDao.Properties.ComicChapter.eq(comicChapter))
                .list();
    }

    public ImageUrl load(long id) {
        return mImageUrlDao.load(id);
    }

    public void updateOrInsert(List<ImageUrl> imageUrlList) {
//        for (ImageUrl imageurl : imageUrlList) {
//            if (imageurl.getId() == null) {
//                insert(imageurl);
//            } else {
//                update(imageurl);
//            }
//        }
        Observable.from(imageUrlList)
                .flatMap((Func1<ImageUrl, Observable<?>>) imageUrl -> Observable.fromCallable(new Callable<Void>() {
                    @Override
                    public Void call() {
                        if (imageUrl.getId() == null) {
                            runInTx(() -> insert(imageUrl));
                        } else {
                            runInTx(() -> update(imageUrl));
                        }
                        return null;
                    }
                }).subscribeOn(Schedulers.io()), 10)
                .subscribe();
    }

    public void insertOrReplace(List<ImageUrl> imageUrlList) {
//        for (ImageUrl imageurl : imageUrlList) {
//            if (imageurl.getId() != null) {
//                mImageUrlDao.insertOrReplace(imageurl);
//            }
//        }
        Observable.from(imageUrlList)
                .filter(imageUrl -> imageUrl.getId() != null)
                .flatMap((Func1<ImageUrl, Observable<?>>) imageUrl -> Observable.fromCallable(new Callable<Void>() {
                    @Override
                    public Void call() {
                        runInTx(() -> mImageUrlDao.insertOrReplace(imageUrl));
                        return null;
                    }
                }).subscribeOn(Schedulers.io()), 10)
                .subscribe();
    }

    public void update(ImageUrl imageurl) {
        mImageUrlDao.update(imageurl);
    }

    public void deleteByKey(long key) {
        mImageUrlDao.deleteByKey(key);
    }

    public void deleteByComicChapter(Long comicChapter) {
        mImageUrlDao.queryBuilder()
                .where(ImageUrlDao.Properties.ComicChapter.eq(comicChapter))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
    }

    public void insert(ImageUrl imageurl) {
        long id = mImageUrlDao.insert(imageurl);
        imageurl.setId(id);
    }

}
