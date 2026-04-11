package com.xyrlsz.xcimoc.manager;

import android.util.Log;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.ImageUrl_;

import java.util.List;
import java.util.concurrent.Callable;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by HaleyDu on 2020/8/27.
 * Modified to use ObjectBox (参照 ComicManager)
 */
public class ImageUrlManager {
    private static ImageUrlManager mInstance;

    // 1. 修改：使用 ObjectBox 的 Box 替代 Dao
    private final Box<ImageUrl> mImageUrlBox;

    private ImageUrlManager(AppGetter getter) {
        // 2. 修改：从 BoxStore 获取 Box
        BoxStore boxStore = getter.getAppInstance().getBoxStore();
        mImageUrlBox = boxStore.boxFor(ImageUrl.class);
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

    // 3. 修改：封装 runInTx 和 callInTx 方法
    public void runInTx(Runnable runnable) {
        mImageUrlBox.getStore().runInTx(runnable);
    }

    public <T> T callInTx(Callable<T> callable) throws Exception {
        return mImageUrlBox.getStore().callInTx(callable);
    }

    // 4. 修改：使用 ObjectBox Query 查询
    public Observable<List<ImageUrl>> getListImageUrlRX(Long comicChapter) {
        return Observable.fromCallable(() ->
                mImageUrlBox.query()
                        .equal(ImageUrl_.comicChapter, comicChapter) // 注意：ObjectBox 使用字段名直接比较
                        .build()
                        .find()
        ).subscribeOn(Schedulers.io());
    }

    public List<ImageUrl> getListImageUrl(Long comicChapter) {
        return mImageUrlBox.query()
                .equal(ImageUrl_.comicChapter, comicChapter)
                .build()
                .find();
    }

    // 5. 修改：load 方法
    public ImageUrl load(long id) {
        return mImageUrlBox.get(id);
    }

    // 6. 修改：updateOrInsert 逻辑
    public void updateOrInsert(List<ImageUrl> imageUrlList) {
        Observable.from(imageUrlList)
                .flatMap((Func1<ImageUrl, Observable<?>>) imageUrl ->
                        Observable.fromCallable(() -> {
                            // ObjectBox 的 put 会自动判断插入或更新
                            // 如果 ID 为 0 或 null (取决于定义)，则插入；否则更新
                            mImageUrlBox.put(imageUrl);
                            return null;
                        }).subscribeOn(Schedulers.io()), 10)
                .subscribe(
                        result -> {},
                        throwable -> Log.e("RX", "Chapter error", throwable)
                );
    }

    // 7. 修改：insertOrReplace (ObjectBox 的 put 类似于 insertOrReplace)
    public void insertOrReplace(List<ImageUrl> imageUrlList) {
        Observable.from(imageUrlList)
                .filter(imageUrl -> imageUrl.getId() != 0) // 假设 ID 为 long 基本类型，0 代表无效
                .flatMap((Func1<ImageUrl, Observable<?>>) imageUrl ->
                        Observable.fromCallable(() -> {
                            runInTx(() -> mImageUrlBox.put(imageUrl));
                            return null;
                        }).subscribeOn(Schedulers.io()), 10)
                .subscribe(
                        result -> {},
                        throwable -> Log.e("RX", "Chapter error", throwable)
                );
    }

    // 8. 修改：单独的 update
    public void update(ImageUrl imageurl) {
        mImageUrlBox.put(imageurl);
    }

    // 9. 修改：删除单个
    public void deleteByKey(long key) {
        mImageUrlBox.remove(key);
    }

    // 10. 修改：根据 comicChapter 删除
    public void deleteByComicChapter(Long comicChapter) {
        // ObjectBox 不支持直接的 QueryBuilder 删除，需要先查 ID 再删，或者使用 removeAll
        // 这里采用先查后删的逻辑
        long[] ids = mImageUrlBox.query()
                .equal(ImageUrl_.comicChapter, comicChapter)
                .build()
                .findIds();
        if (ids.length > 0) {
            mImageUrlBox.remove(ids);
        }
    }

    // 11. 修改：插入
    public void insert(ImageUrl imageurl) {
        long id = mImageUrlBox.put(imageurl);
        imageurl.setId(id);
    }
}