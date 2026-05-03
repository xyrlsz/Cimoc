package com.xyrlsz.xcimocob.presenter;

//import com.google.firebase.analytics.FirebaseAnalytics;

import com.xyrlsz.xcimocob.core.Manga;
import com.xyrlsz.xcimocob.manager.ComicManager;
import com.xyrlsz.xcimocob.manager.SourceManager;
import com.xyrlsz.xcimocob.manager.TagRefManager;
import com.xyrlsz.xcimocob.model.Comic;
import com.xyrlsz.xcimocob.model.MiniComic;
import com.xyrlsz.xcimocob.rx.RxEvent;
import com.xyrlsz.xcimocob.rx.ToAnotherList;
import com.xyrlsz.xcimocob.ui.view.FavoriteView;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2016/7/6.
 */
public class FavoritePresenter extends BasePresenter<FavoriteView> {

    private ComicManager mComicManager;
    private SourceManager mSourceManager;
    private TagRefManager mTagRefManager;

    @Override
    protected void onViewAttach() {
        mComicManager = ComicManager.getInstance(mBaseView);
        mSourceManager = SourceManager.getInstance(mBaseView);
        mTagRefManager = TagRefManager.getInstance(mBaseView);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initSubscription() {
        super.initSubscription();
        addSubscription(RxEvent.EVENT_COMIC_FAVORITE, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                MiniComic comic = (MiniComic) rxEvent.getData();
                mBaseView.OnComicFavorite(comic);
            }
        });
        addSubscription(RxEvent.EVENT_COMIC_UNFAVORITE, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                mBaseView.OnComicUnFavorite((long) rxEvent.getData());
            }
        });
        addSubscription(RxEvent.EVENT_COMIC_FAVORITE_RESTORE, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                mBaseView.OnComicRestore((List<Object>) rxEvent.getData());
            }
        });
        addSubscription(RxEvent.EVENT_COMIC_READ, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                mBaseView.onComicRead((MiniComic) rxEvent.getData());
            }
        });
        addSubscription(RxEvent.EVENT_COMIC_CANCEL_HIGHLIGHT, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                mBaseView.onHighlightCancel((MiniComic) rxEvent.getData());
            }
        });
    }

    public Comic load(long id) {
        return mComicManager.load(id);
    }

    public void load() {
        mCompositeSubscription.add(mComicManager.listFavoriteInRx().compose(new ToAnotherList<>(new Function<Comic, Object>() {
            @Override
            public MiniComic apply(Comic comic) {
                return new MiniComic(comic);
            }
        })).observeOn(AndroidSchedulers.mainThread()).subscribe(new Consumer<List<Object>>() {
            @Override
            public void accept(List<Object> list) {
                mBaseView.onComicLoadSuccess(list);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                mBaseView.onComicLoadFail();
            }
        }));
    }

    public void cancelAllHighlight() {
        mComicManager.cancelHighlight();
    }

    public void unfavoriteComic(long id) {
        Comic comic = mComicManager.load(id);
        comic.setFavorite(null);
        mTagRefManager.deleteByComic(id);
        mComicManager.updateOrDelete(comic);
        mBaseView.OnComicUnFavorite(id);
    }

    public void checkUpdate() {
        final List<Comic> list = mComicManager.listFavorite();
        mCompositeSubscription.add(Manga.checkUpdate(mSourceManager, list).doOnNext(new Consumer<Comic>() {
            @Override
            public void accept(Comic comic) {
                if (comic != null) {
                    mComicManager.update(comic);
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
            new io.reactivex.rxjava3.functions.Consumer<Comic>() {
                private int count = 0;
                @Override
                public void accept(Comic comic) {
                    ++count;
                    MiniComic miniComic = new MiniComic(comic);
                    mBaseView.onComicCheckSuccess(miniComic, count, list.size());
                }
            },
            new io.reactivex.rxjava3.functions.Consumer<Throwable>() {
                @Override
                public void accept(Throwable e) {
                    mBaseView.onComicCheckFail();
                }
            },
            new io.reactivex.rxjava3.functions.Action() {
                @Override
                public void run() {
                    mBaseView.onComicCheckComplete();
                }
            }
        ));
    }

}
