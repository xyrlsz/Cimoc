package com.haleydu.cimoc.manager;

import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.database.AppDatabase;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.utils.ObservableUtils;

import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;


/**
 * Created by Hiroshi on 2016/7/9.
 * Modified for Room database.
 */
public class ChapterManager {

    private static ChapterManager mInstance;

    private AppDatabase mDatabase;

    private ChapterManager(AppGetter getter) {
        mDatabase = getter.getAppInstance().getAppDatabase();
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
        mDatabase.runInTransaction(runnable);
    }

    public <T> T callInTx(Callable<T> callable) {
        return mDatabase.runInTransaction(callable);
    }

    public Observable<List<Chapter>> getListChapter(Long sourceComic) {
        return ObservableUtils.V3toV1(mDatabase.chapterDao().findBySourceComicRx(sourceComic).toObservable());
    }

    public List<Chapter> getChapter(String path, String title) {
        return mDatabase.chapterDao().findByPathAndTitle(path, title);
    }

    public Chapter load(long id) {
        return mDatabase.chapterDao().findById(id);
    }

    public void cancelHighlight() {
        mDatabase.comicDao().cancelHighlight();
    }

    public void updateOrInsert(List<Chapter> chapterList) {
        mDatabase.runInTransaction(() -> {
            for (Chapter chapter : chapterList) {
                if (chapter.getId() == null) {
                    insert(chapter);
                } else {
                    update(chapter);
                }
            }
        });
    }

    public void insertOrReplace(List<Chapter> chapterList) {
        mDatabase.runInTransaction(() -> {
            for (Chapter chapter : chapterList) {
                if (chapter.getId() != null) {
                    mDatabase.chapterDao().insertOrReplace(chapter);
                }
            }
        });
    }

    public void update(Chapter chapter) {
        if (chapter.getId() != null) {
            mDatabase.chapterDao().update(chapter);
        }
    }

    public void deleteByKey(long key) {
        mDatabase.chapterDao().deleteById(key);
    }

    public void insert(Chapter chapter) {
        long id = mDatabase.chapterDao().insert(chapter);
        chapter.setId(id);
    }
}