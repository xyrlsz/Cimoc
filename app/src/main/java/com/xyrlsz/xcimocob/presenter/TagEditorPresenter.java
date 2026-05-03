package com.xyrlsz.xcimocob.presenter;

import com.xyrlsz.xcimocob.manager.TagManager;
import com.xyrlsz.xcimocob.manager.TagRefManager;
import com.xyrlsz.xcimocob.misc.Switcher;
import com.xyrlsz.xcimocob.model.Tag;
import com.xyrlsz.xcimocob.model.TagRef;
import com.xyrlsz.xcimocob.rx.RxBus;
import com.xyrlsz.xcimocob.rx.RxEvent;
import com.xyrlsz.xcimocob.rx.ToAnotherList;
import com.xyrlsz.xcimocob.ui.view.TagEditorView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2016/12/2.
 */

public class TagEditorPresenter extends BasePresenter<TagEditorView> {

    private TagManager mTagManager;
    private TagRefManager mTagRefManager;
    private long mComicId;
    private Set<Long> mTagSet;

    @Override
    protected void onViewAttach() {
        mTagManager = TagManager.getInstance(mBaseView);
        mTagRefManager = TagRefManager.getInstance(mBaseView);
        mTagSet = new HashSet<>();
    }

    public void load(long id) {
        mComicId = id;
        mCompositeSubscription.add(mTagManager.listInRx()
                .doOnNext(new Consumer<List<Tag>>() {
                    @Override
                    public void accept(List<Tag> list) {
                        for (TagRef ref : mTagRefManager.listByComic(mComicId)) {
                            mTagSet.add(ref.getTid());
                        }
                    }
                })
                .compose(new ToAnotherList<>(new Function<Tag, Switcher<Tag>>() {
                    @Override
                    public Switcher<Tag> apply(Tag tag) {
                        return new Switcher<>(tag, mTagSet.contains(tag.getId()));
                    }
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Switcher<Tag>>>() {
                    @Override
                    public void accept(List<Switcher<Tag>> list) {
                        mBaseView.onTagLoadSuccess(list);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        mBaseView.onTagLoadFail();
                    }
                }));
    }

    private void updateInTx(final List<Long> list) {
        mTagRefManager.runInTx(new Runnable() {
            @Override
            public void run() {
                for (long id : list) {
                    if (!mTagSet.contains(id)) {
                        mTagRefManager.insert(new TagRef(null, id, mComicId));
                    }
                }
                mTagSet.removeAll(list);
                for (long id : mTagSet) {
                    mTagRefManager.delete(id, mComicId);
                }
            }
        });
    }

    public void updateRef(List<Long> list) {
        mCompositeSubscription.add(Observable.just(list)
                .doOnNext(new Consumer<List<Long>>() {
                    @Override
                    public void accept(List<Long> list) {
                        updateInTx(list);
                        mTagSet.clear();
                        mTagSet.addAll(list);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Long>>() {
                    @Override
                    public void accept(List<Long> list) {
                        mBaseView.onTagUpdateSuccess();
                        RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_TAG_UPDATE, mComicId, list));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        mBaseView.onTagUpdateFail();
                    }
                }));
    }

}
