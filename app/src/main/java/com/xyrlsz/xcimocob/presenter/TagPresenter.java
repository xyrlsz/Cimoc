package com.xyrlsz.xcimocob.presenter;

import com.xyrlsz.xcimocob.manager.TagManager;
import com.xyrlsz.xcimocob.manager.TagRefManager;
import com.xyrlsz.xcimocob.model.Tag;
import com.xyrlsz.xcimocob.rx.RxEvent;
import com.xyrlsz.xcimocob.ui.view.TagView;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by Hiroshi on 2016/10/10.
 */

public class TagPresenter extends BasePresenter<TagView> {

    private TagManager mTagManager;
    private TagRefManager mTagRefManager;

    @Override
    protected void onViewAttach() {
        mTagManager = TagManager.getInstance(mBaseView);
        mTagRefManager = TagRefManager.getInstance(mBaseView);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initSubscription() {
        addSubscription(RxEvent.EVENT_TAG_RESTORE, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                mBaseView.onTagRestore((List<Tag>) rxEvent.getData());
            }
        });
    }

    public void load() {
        mCompositeSubscription.add(mTagManager.listInRx()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Tag>>() {
                    @Override
                    public void accept(List<Tag> list) {
                        mBaseView.onTagLoadSuccess(list);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        mBaseView.onTagLoadFail();
                    }
                }));
    }

    public void insert(Tag tag) {
        mTagManager.insert(tag);
    }

    public void delete(final Tag tag) {
        mCompositeSubscription.add(mTagRefManager.runInRx(new Runnable() {
                    @Override
                    public void run() {
                        mTagRefManager.deleteByTag(tag.getId());
                        mTagManager.delete(tag);
                    }
                }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object aVoid) {
                        mBaseView.onTagDeleteSuccess(tag);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        mBaseView.onTagDeleteFail();
                    }
                }));
    }

}
