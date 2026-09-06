package com.xyrlsz.xcimocob.manager;

import com.xyrlsz.xcimocob.component.AppGetter;
import com.xyrlsz.xcimocob.model.Chapter;
import com.xyrlsz.xcimocob.model.Chapter_;
import com.xyrlsz.xcimocob.utils.IdCreator;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.Query;
import io.reactivex.rxjava3.core.Observable;

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

            Long sourceComic0 = IdCreator.recreateSourceComic(sourceComic, 0L);
            try (Query<Chapter> query = mChapterBox.query()
                    .equal(Chapter_.sourceComic, sourceComic)
                    .build()) {
                List<Chapter> list = query.find();

                return list.stream()
                        .filter(chapter -> !IdCreator.getSourceComicFromChapter(chapter.getId()).equals(sourceComic0))
                        .collect(Collectors.toList());
            }
        });
    }

    public List<Chapter> getChapterList(long sourceComic) {
        try (Query<Chapter> query = mChapterBox.query().equal(Chapter_.sourceComic, sourceComic).build()) {
            return query.find();
        }
    }

    public List<Chapter> getChapter(String path, String title) {
        try (Query<Chapter> query = mChapterBox.query(Chapter_.path.equal(path).and(Chapter_.title.equal(title)))
                .build()) {
            return query.find();
        }
    }

    public List<Chapter> getChapter(String path) {
        try (Query<Chapter> query = mChapterBox.query(Chapter_.path.equal(path)).build()) {
            return query.find();
        }
    }

    public List<Chapter> getChapter(long sourceComic, String path) {
        try (Query<Chapter> query = mChapterBox.query(Chapter_.path.equal(path))
                .equal(Chapter_.sourceComic, sourceComic)
                .build()) {
            return query.find();
        }
    }

    public Chapter load(long id) {
        if (id == 0 || id == 1) {
            return null;
        }
        return mChapterBox.get(id);
    }

    public void updateOrInsert(List<Chapter> chapterList) {
        mChapterBox.put(chapterList);
    }

    public void update(Chapter chapter) {
        if (chapter.getId() != 0) {
            mChapterBox.put(chapter);
        }
    }

    public void deleteByKey(long key) {
        mChapterBox.remove(key);
    }

    public void deleteBySourceComic(long sourceComic) {
        try (Query<Chapter> query = mChapterBox.query().equal(Chapter_.sourceComic, sourceComic).build()) {
            List<Chapter> chapters = query.find();
            if (!chapters.isEmpty()) {
                mChapterBox.remove(chapters);
            }
        }
    }
}
