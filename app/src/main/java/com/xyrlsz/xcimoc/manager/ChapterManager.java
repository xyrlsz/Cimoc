package com.xyrlsz.xcimoc.manager;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.ChapterDao;
import com.xyrlsz.xcimoc.model.ChapterDao.Properties;

import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Created by Hiroshi on 2016/7/9.
 */
public class ChapterManager {

    private static ChapterManager mInstance;

    private ChapterDao mChapterDao;

    private ChapterManager(AppGetter getter) {
        mChapterDao = getter.getAppInstance().getDaoSession().getChapterDao();
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
        mChapterDao.getSession().runInTx(runnable);
    }

    public <T> T callInTx(Callable<T> callable) {
        return mChapterDao.getSession().callInTxNoException(callable);
    }

    public Observable<List<Chapter>> getListChapter(Long sourceComic) {
        return mChapterDao.queryBuilder()
                .where(Properties.SourceComic.eq(sourceComic))
                .rx()
                .list();
    }

    public List<Chapter> getChapter(String path, String title) {
        return mChapterDao.queryBuilder()
                .where(ChapterDao.Properties.Path.eq(path), ChapterDao.Properties.Title.eq(title))
                .list();
    }


    public Chapter load(long id) {
        return mChapterDao.load(id);
    }


    public void cancelHighlight() {
        mChapterDao.getDatabase().execSQL("UPDATE \"COMIC\" SET \"HIGHLIGHT\" = 0 WHERE \"HIGHLIGHT\" = 1");
    }

    public void updateOrInsert(List<Chapter> chapterList) {
        for (Chapter chapter : chapterList) {
            if (chapter.getId() == null) {
                insert(chapter);
            } else {
                update(chapter);
            }
        }
    }

    public void insertOrReplace(List<Chapter> chapterList) {
//        for (Chapter chapter:chapterList) {
//            if (chapter.getId()!=null) {
//                mChapterDao.insertOrReplace(chapter);
//            }
//        }

        Observable.from(chapterList)
                .filter(chapter -> chapter.getId() != null)
                .flatMap((Func1<Chapter, Observable<?>>) chapter -> Observable.fromCallable(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        mChapterDao.insertOrReplace(chapter);
                        return null;
                    }
                }).subscribeOn(Schedulers.io()), 10)
                .subscribe();

    }

    public void update(Chapter chapter) {
        if (chapter.getId() != null) {
            mChapterDao.update(chapter);
        }
    }

    public void deleteByKey(long key) {
        mChapterDao.deleteByKey(key);
    }

    public void insert(Chapter chapter) {
        long id = mChapterDao.insert(chapter);
        chapter.setId(id);
    }

}
