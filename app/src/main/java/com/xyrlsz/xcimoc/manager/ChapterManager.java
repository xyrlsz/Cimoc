package com.xyrlsz.xcimoc.manager;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Chapter_;

import java.util.List;
import java.util.concurrent.Callable;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2016/7/9.
 */
public class ChapterManager {
    private static volatile ChapterManager mInstance;

    private final Box<Chapter> mChapterBox;

    private ChapterManager(AppGetter getter) {
        BoxStore boxStore = getter.getAppInstance().getBoxStore();
        mChapterBox = boxStore.boxFor(Chapter.class);
    }

    public static ChapterManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (ChapterManager.class) {
                if (mInstance == null) {
                    mInstance = new ChapterManager(getter);
                }
            }
        }
        return mInstance;
    }

    public void runInTx(Runnable runnable) {
        mChapterBox.getStore().runInTx(runnable);
    }

    public <T> T callInTx(Callable<T> callable) throws Exception {
        return mChapterBox.getStore().callInTx(callable);
    }

    public Observable<List<Chapter>> getListChapter(long sourceComic) {
        return Observable.fromCallable(() -> {
            return mChapterBox.query().equal(Chapter_.sourceComic, sourceComic).build().find();
        });
    }

    public List<Chapter> getChapterList(long sourceComic) {
        return mChapterBox.query().equal(Chapter_.sourceComic, sourceComic).build().find();
    }

    public List<Chapter> getChapter(String path, String title) {
        return mChapterBox.query(Chapter_.path.equal(path).and(Chapter_.title.equal(title)))
                .build()
                .find();
    }

    public List<Chapter> getChapter(String path) {
        return mChapterBox.query(Chapter_.path.equal(path)).build().find();
    }

    public List<Chapter> getChapter(long sourceComic, String path) {
        return mChapterBox.query(Chapter_.path.equal(path))
                .equal(Chapter_.sourceComic, sourceComic)
                .build()
                .find();
    }

    public Chapter load(long id) {
        return mChapterBox.get(id);
    }

    public void updateOrInsert(List<Chapter> chapterList) {
        Observable
                .fromCallable(() -> {
                    mChapterBox.put(chapterList);
                    return null;
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }


    public void update(Chapter chapter) {
        if (chapter.getId() != 0) {
            long res = mChapterBox.put(chapter);
            System.out.println("update:" + res);
        }
    }

    public void deleteByKey(long key) {
        mChapterBox.remove(key);
    }

    public void deleteBySourceComic(long sourceComic) {
        List<Chapter> chapters =
                mChapterBox.query().equal(Chapter_.sourceComic, sourceComic).build().find();
        if (!chapters.isEmpty()) {
            mChapterBox.remove(chapters);
        }
    }

    public void insert(Chapter chapter) {
        long id = mChapterBox.put(chapter);
        chapter.setId(id);
    }
}
