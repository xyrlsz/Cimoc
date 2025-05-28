package com.xyrlsz.xcimoc.presenter;

import com.xyrlsz.xcimoc.core.Backup;
import com.xyrlsz.xcimoc.core.Download;
import com.xyrlsz.xcimoc.core.Manga;
import com.xyrlsz.xcimoc.manager.ChapterManager;
import com.xyrlsz.xcimoc.manager.ComicManager;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.manager.TagRefManager;
import com.xyrlsz.xcimoc.manager.TaskManager;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.MiniComic;
import com.xyrlsz.xcimoc.model.Task;
import com.xyrlsz.xcimoc.rx.RxBus;
import com.xyrlsz.xcimoc.rx.RxEvent;
import com.xyrlsz.xcimoc.saf.CimocDocumentFile;
import com.xyrlsz.xcimoc.ui.view.DetailView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2016/7/4.
 */
public class DetailPresenter extends BasePresenter<DetailView> {

    private ComicManager mComicManager;
    private ChapterManager mChapterManager;
    private TaskManager mTaskManager;
    private TagRefManager mTagRefManager;
    private SourceManager mSourceManager;
    private Comic mComic;

    @Override
    protected void onViewAttach() {
        mComicManager = ComicManager.getInstance(mBaseView);
        mChapterManager = ChapterManager.getInstance(mBaseView);
        mTaskManager = TaskManager.getInstance(mBaseView);
        mTagRefManager = TagRefManager.getInstance(mBaseView);
        mSourceManager = SourceManager.getInstance(mBaseView);
    }

    @Override
    protected void initSubscription() {
        addSubscription(RxEvent.EVENT_COMIC_UPDATE, new Action1<RxEvent>() {
            @Override
            public void call(RxEvent rxEvent) {
                if (mComic.getId() != null && mComic.getId() == (long) rxEvent.getData()) {
                    Comic comic = mComicManager.load(mComic.getId());
                    mComic.setPage(comic.getPage());
                    mComic.setLast(comic.getLast());
                    mComic.setChapter(comic.getChapter());
                    mBaseView.onLastChange(mComic.getLast());
                }
            }
        });
        addSubscription(RxEvent.EVENT_COMIC_UPDATE_INFO, new Action1<RxEvent>() {
            @Override
            public void call(RxEvent rxEvent) {
                if (mComic.getId() != null) {
                    Comic comic = (Comic) rxEvent.getData();
                    mComicManager.insertOrReplace(comic);
                    mComic = mComicManager.load(comic.getId());
                }
            }
        });
    }

    public void load(long id, int source, String cid) {
        if (id == -1) {
            mComic = mComicManager.loadOrCreate(source, cid);
        } else {
            mComic = mComicManager.load(id);
        }
        cancelHighlight();
        preLoad();
        load();
    }

    private void updateChapterList(List<Chapter> list) {
        Map<String, Task> map = new HashMap<>();
        for (Task task : mTaskManager.list(mComic.getId())) {
            map.put(task.getPath(), task);
        }
        if (!map.isEmpty()) {
            for (Chapter chapter : list) {
                Task task = map.get(chapter.getPath());
                if (task != null) {
                    chapter.setDownload(true);
                    chapter.setCount(task.getProgress());
                    chapter.setComplete(task.isFinish());
                    mChapterManager.update(chapter);
                }
            }
        }
        mComic.setChapterCount(list.size());
        mComicManager.update(mComic);
    }

    public void preLoad() {
        if (mComic.getId() == null) {
            return;
        }
        mCompositeSubscription.add(mChapterManager.getListChapter(Long.parseLong(mComic.getSource() + "0" + mComic.getId()))
                .doOnNext(new Action1<List<Chapter>>() {
                    @Override
                    public void call(List<Chapter> list) {
                        if (mComic.getId() != null && list.size() != 0) {
                            updateChapterList(list);
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Chapter>>() {
                    @Override
                    public void call(List<Chapter> list) {
                        if (list != null && list.size() != 0) {
                            mBaseView.onPreLoadSuccess(list, mComic);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mBaseView.onComicLoadSuccess(mComic);
                        mBaseView.onParseError();
                    }
                }));
    }

    private void load() {
        mCompositeSubscription.add(Manga.getComicInfo(mSourceManager.getParser(mComic.getSource()), mComic)
                .doOnNext(new Action1<List<Chapter>>() {
                    @Override
                    public void call(List<Chapter> list) {
                        if (mComic.getId() != null) {
                            updateChapterList(list);
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Chapter>>() {
                    @Override
                    public void call(List<Chapter> list) {
                        mChapterManager.insertOrReplace(list);
                        mBaseView.onComicLoadSuccess(mComic);
                        mBaseView.onChapterLoadSuccess(list);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mBaseView.onComicLoadSuccess(mComic);
                        mBaseView.onParseError();
                    }
                }));
    }

    private void cancelHighlight() {
        if (mComic.getHighlight()) {
            mComic.setHighlight(false);
            mComic.setFavorite(System.currentTimeMillis());
            mComicManager.update(mComic);
            RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_CANCEL_HIGHLIGHT, new MiniComic(mComic)));
        }
    }

    /**
     * 更新最后阅读
     *
     * @param path 最后阅读
     * @return 漫画ID
     */
    public long updateLast(String path) {
        if (mComic.getFavorite() != null) {
            mComic.setFavorite(System.currentTimeMillis());
        }
        mComic.setHistory(System.currentTimeMillis());
        if (!path.equals(mComic.getLast())) {
            mComic.setLast(path);
            mComic.setPage(1);
        }
        mComicManager.updateOrInsert(mComic);
        RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_READ, new MiniComic(mComic)));
        return mComic.getId();
    }

    public Comic getComic() {
        return mComic;
    }

    public void backup(CimocDocumentFile file) {
        mComicManager.listFavoriteOrHistoryInRx()
                .doOnNext(new Action1<List<Comic>>() {
                    @Override
                    public void call(List<Comic> list) {
                        Backup.saveComicAuto(mBaseView.getAppInstance().getContentResolver(),
                                file, list);
                    }
                })
                .subscribe();
    }

    public void favoriteComic() {
        mComic.setFavorite(System.currentTimeMillis());
        mComicManager.updateOrInsert(mComic);
        RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_FAVORITE, new MiniComic(mComic)));
    }

    public void unfavoriteComic() {
        long id = mComic.getId();
        mComic.setFavorite(null);
        mTagRefManager.deleteByComic(id);
        mComicManager.updateOrDelete(mComic);
        RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_UNFAVORITE, id));
    }

    private ArrayList<Task> getTaskList(List<Chapter> list) {
        ArrayList<Task> result = new ArrayList<>(list.size());
        for (Chapter chapter : list) {
            Task task = new Task(null, -1, chapter.getPath(), chapter.getTitle(), 0, 0);
            task.setSource(mComic.getSource());
            task.setCid(mComic.getCid());
            task.setState(Task.STATE_WAIT);
            result.add(task);
        }
        return result;
    }

    /**
     * 添加任务到数据库
     *
     * @param cList 所有章节列表，用于写索引文件
     * @param dList 下载章节列表
     */
    public void addTask(final List<Chapter> cList, final List<Chapter> dList) {
        mCompositeSubscription.add(Observable.create(new Observable.OnSubscribe<ArrayList<Task>>() {
                    @Override
                    public void call(Subscriber<? super ArrayList<Task>> subscriber) {
                        final ArrayList<Task> result = getTaskList(dList);
                        mComic.setDownload(System.currentTimeMillis());
                        mComicManager.runInTx(new Runnable() {
                            @Override
                            public void run() {
                                mComicManager.updateOrInsert(mComic);
                                for (Task task : result) {
                                    task.setKey(mComic.getId());
                                    mTaskManager.insert(task);
                                }
                            }
                        });
                        Download.updateComicIndex(mBaseView.getAppInstance().getContentResolver(),
                                mBaseView.getAppInstance().getDocumentFile(), cList, mComic);
                        subscriber.onNext(result);
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ArrayList<Task>>() {
                    @Override
                    public void call(ArrayList<Task> list) {
                        RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_TASK_INSERT, new MiniComic(mComic), list));
                        mBaseView.onTaskAddSuccess(list);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                        mBaseView.onTaskAddFail();
                    }
                }));
    }
}
