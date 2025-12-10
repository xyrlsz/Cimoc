package com.xyrlsz.xcimoc.presenter;

import com.xyrlsz.xcimoc.manager.ChapterManager;
import com.xyrlsz.xcimoc.manager.ComicManager;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.MiniComic;
import com.xyrlsz.xcimoc.rx.RxEvent;
import com.xyrlsz.xcimoc.rx.ToAnotherList;
import com.xyrlsz.xcimoc.ui.view.HistoryView;
import com.xyrlsz.xcimoc.utils.IdCreator;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Hiroshi on 2016/7/18.
 */
public class HistoryPresenter extends BasePresenter<HistoryView> {

    private ComicManager mComicManager;
    private ChapterManager mChapterManager;

    @Override
    protected void onViewAttach() {
        mComicManager = ComicManager.getInstance(mBaseView);
        mChapterManager = ChapterManager.getInstance(mBaseView);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initSubscription() {
        super.initSubscription();
        addSubscription(RxEvent.EVENT_COMIC_READ, new Action1<RxEvent>() {
            @Override
            public void call(RxEvent rxEvent) {
                mBaseView.onItemUpdate((MiniComic) rxEvent.getData());
            }
        });
        addSubscription(RxEvent.EVENT_COMIC_HISTORY_RESTORE, new Action1<RxEvent>() {
            @Override
            public void call(RxEvent rxEvent) {
                mBaseView.OnComicRestore((List<Object>) rxEvent.getData());
            }
        });
    }

    public Comic load(long id) {
        return mComicManager.load(id);
    }

    public void load() {
        mCompositeSubscription.add(mComicManager.listHistoryInRx()
                .compose(new ToAnotherList<>(new Func1<Comic, Object>() {
                    @Override
                    public MiniComic call(Comic comic) {
                        return new MiniComic(comic);
                    }
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Object>>() {
                    @Override
                    public void call(List<Object> list) {
                        mBaseView.onComicLoadSuccess(list);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mBaseView.onComicLoadFail();
                    }
                }));
    }

    public void delete(long id) {
        Comic comic = mComicManager.load(id);
        comic.setHistory(null);
        int res = mComicManager.updateOrDelete(comic);
        if (res == ComicManager.RESULT_DELETE) {
            mChapterManager.deleteBySourceComic(IdCreator.createSourceComic(comic));
        }
        mBaseView.onHistoryDelete(id);
    }

    public void clear() {
        mCompositeSubscription.add(mComicManager.listHistoryInRx()
                .doOnNext(new Action1<List<Comic>>() {
                    @Override
                    public void call(final List<Comic> list) {
                        mComicManager.runInTx(new Runnable() {
                            @Override
                            public void run() {
                                for (Comic comic : list) {
                                    comic.setHistory(null);
                                    int res = mComicManager.updateOrDelete(comic);
                                    if (res == ComicManager.RESULT_DELETE) {
                                        mChapterManager.deleteBySourceComic(IdCreator.createSourceComic(comic));
                                    }
                                }
                            }
                        });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Comic>>() {
                    @Override
                    public void call(List<Comic> list) {
                        mBaseView.onHistoryClearSuccess();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mBaseView.onExecuteFail();
                    }
                }));
    }

}
