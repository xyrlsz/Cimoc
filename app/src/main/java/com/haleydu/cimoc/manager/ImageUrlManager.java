package com.haleydu.cimoc.manager;

import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.database.AppDatabase;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.utils.ObservableUtils;

import java.util.List;

import rx.Observable;


/**
 * Created by HaleyDu on 2020/8/27.
 * Modified for Room database.
 */
public class ImageUrlManager {

    private static ImageUrlManager mInstance;

    private AppDatabase mDatabase;

    private ImageUrlManager(AppGetter getter) {
        mDatabase = getter.getAppInstance().getAppDatabase();
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
        mDatabase.runInTransaction(runnable);
    }

    public Observable<List<ImageUrl>> getListImageUrlRX(Long comicChapter) {
        return ObservableUtils.V3toV1(mDatabase.imageUrlDao().findByComicChapterRx(comicChapter).toObservable());
    }

    public List<ImageUrl> getListImageUrl(Long comicChapter) {
        return mDatabase.imageUrlDao().findByComicChapter(comicChapter);
    }

    public ImageUrl load(long id) {
        return mDatabase.imageUrlDao().findById(id);
    }

    public void updateOrInsert(List<ImageUrl> imageUrlList) {
        mDatabase.runInTransaction(() -> {
            for (ImageUrl imageUrl : imageUrlList) {
                if (imageUrl.getId() == null) {
                    insert(imageUrl);
                } else {
                    update(imageUrl);
                }
            }
        });
    }

    public void insertOrReplace(List<ImageUrl> imageUrlList) {
        mDatabase.runInTransaction(() -> {
            for (ImageUrl imageUrl : imageUrlList) {
                if (imageUrl.getId() != null) {
                    mDatabase.imageUrlDao().insertOrReplace(imageUrl);
                }
            }
        });
    }

    public void update(ImageUrl imageUrl) {
        mDatabase.imageUrlDao().update(imageUrl);
    }

    public void deleteByKey(long key) {
        mDatabase.imageUrlDao().deleteById(key);
    }

    public void insert(ImageUrl imageUrl) {
        long id = mDatabase.imageUrlDao().insert(imageUrl);
        imageUrl.setId(id);
    }
}