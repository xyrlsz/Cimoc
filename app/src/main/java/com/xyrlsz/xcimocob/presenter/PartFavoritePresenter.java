package com.xyrlsz.xcimocob.presenter;

import androidx.collection.LongSparseArray;

import com.xyrlsz.xcimocob.manager.ComicManager;
import com.xyrlsz.xcimocob.manager.TagManager;
import com.xyrlsz.xcimocob.manager.TagRefManager;
import com.xyrlsz.xcimocob.model.Comic;
import com.xyrlsz.xcimocob.model.MiniComic;
import com.xyrlsz.xcimocob.model.TagRef;
import com.xyrlsz.xcimocob.rx.RxEvent;
import com.xyrlsz.xcimocob.rx.ToAnotherList;
import com.xyrlsz.xcimocob.ui.view.PartFavoriteView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;

/**
 * Created by Hiroshi on 2016/10/11.
 */

public class PartFavoritePresenter extends BasePresenter<PartFavoriteView> {

    private ComicManager mComicManager;
    private TagRefManager mTagRefManager;
    private long mTagId;
    private LongSparseArray<Comic> mSavedComic;

    @Override
    protected void onViewAttach() {
        mComicManager = ComicManager.getInstance(mBaseView);
        mTagRefManager = TagRefManager.getInstance(mBaseView);
        mSavedComic = new LongSparseArray<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initSubscription() {
        addSubscription(RxEvent.EVENT_COMIC_UNFAVORITE, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                mBaseView.onComicRemove((long) rxEvent.getData());
            }
        });
        addSubscription(RxEvent.EVENT_TAG_UPDATE, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                long id = (long) rxEvent.getData();
                List<Long> list = (List<Long>) rxEvent.getData(1);
                if (list.contains(mTagId)) {
                    MiniComic comic = new MiniComic(mComicManager.load(id));
                    mBaseView.onComicAdd(comic);
                } else {
                    mBaseView.onComicRemove(id);
                }
            }
        });
        addSubscription(RxEvent.EVENT_COMIC_CANCEL_HIGHLIGHT, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                mBaseView.onHighlightCancel((MiniComic) rxEvent.getData());
            }
        });
        addSubscription(RxEvent.EVENT_COMIC_READ, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                mBaseView.onComicRead((MiniComic) rxEvent.getData());
            }
        });
    }

    private Observable<List<Comic>> getObservable(long id) {
        if (id == TagManager.TAG_CONTINUE) {
            return mComicManager.listContinueInRx();
        } else if (id == TagManager.TAG_FINISH) {
            return mComicManager.listFinishInRx();
        } else {
            return mComicManager.listFavoriteByTag(id);
        }
    }

    public void load(long id) {
        mTagId = id;
        mCompositeSubscription.add(getObservable(id)
                .compose(new ToAnotherList<>(new Function<Comic, Object>() {
                    @Override
                    public MiniComic apply(Comic comic) {
                        return new MiniComic(comic);
                    }
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Object>>() {
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

    private List<Long> buildIdList(List<Object> list) {
        List<Long> result = new ArrayList<>(list.size());
        for (Object O_comic : list) {
            MiniComic comic = (MiniComic) O_comic;
            result.add(comic.getId());
        }
        return result;
    }

    public void loadComicTitle(List<Object> list) {
        // TODO 不使用 in
        mCompositeSubscription.add(mComicManager.listFavoriteNotIn(buildIdList(list))
                .compose(new ToAnotherList<>(new Function<Comic, String>() {
                    @Override
                    public String apply(Comic comic) throws Exception {
                        mSavedComic.put(comic.getId(), comic);
                        return comic.getTitle();
                    }
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> list) {
                        mBaseView.onComicTitleLoadSuccess(list);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        mBaseView.onComicTitleLoadFail();
                    }
                }));
    }

    public void insert(boolean[] check) {
        // Todo 异步
        if (check != null && mSavedComic != null && check.length == mSavedComic.size()) {
            List<TagRef> rList = new ArrayList<>();
            List<Object> cList = new ArrayList<>();
            for (int i = 0; i != check.length; ++i) {
                if (check[i]) {
                    MiniComic comic = new MiniComic(mSavedComic.valueAt(i));
                    rList.add(new TagRef(null, mTagId, comic.getId()));
                    cList.add(comic);
                }
            }
            mTagRefManager.insertInTx(rList);
            mBaseView.onComicInsertSuccess(cList);
        } else {
            mBaseView.onComicInsertFail();
        }
        mSavedComic.clear();
    }

    public void delete(long id) {
        mTagRefManager.delete(mTagId, id);
    }

}
