package com.xyrlsz.xcimoc.presenter;

import com.xyrlsz.xcimoc.manager.TagManager;
import com.xyrlsz.xcimoc.model.Tag;
import com.xyrlsz.xcimoc.ui.view.ComicView;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

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
                .subscribe(new Action1<List<Tag>>() {
                    @Override
                    public void call(List<Tag> list) {
                        mBaseView.onTagLoadSuccess(list);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mBaseView.onTagLoadFail();
                    }
                }));
    }

}
