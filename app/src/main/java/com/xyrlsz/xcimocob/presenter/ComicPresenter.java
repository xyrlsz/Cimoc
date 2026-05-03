package com.xyrlsz.xcimocob.presenter;

import com.xyrlsz.xcimocob.manager.TagManager;
import com.xyrlsz.xcimocob.model.Tag;
import com.xyrlsz.xcimocob.ui.view.ComicView;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by Hiroshi on 2016/10/11.
 */

public class ComicPresenter extends BasePresenter<ComicView> {

    private TagManager mTagManager;

    @Override
    protected void onViewAttach() {
        mTagManager = TagManager.getInstance(mBaseView);
    }

    public void loadTag() {
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

}
