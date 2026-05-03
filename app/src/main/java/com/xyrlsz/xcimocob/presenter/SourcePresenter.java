package com.xyrlsz.xcimocob.presenter;

import com.xyrlsz.xcimocob.manager.SourceManager;
import com.xyrlsz.xcimocob.model.Source;
import com.xyrlsz.xcimocob.ui.view.SourceView;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by Hiroshi on 2016/8/11.
 */
public class SourcePresenter extends BasePresenter<SourceView> {

    private SourceManager mSourceManager;

    @Override
    protected void onViewAttach() {
        mSourceManager = SourceManager.getInstance(mBaseView);
    }

    public void load() {
        mCompositeSubscription.add(mSourceManager.list()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Source>>() {
                    @Override
                    public void accept(List<Source> list) {
                        mBaseView.onSourceLoadSuccess(list);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        mBaseView.onSourceLoadFail();
                    }
                }));
    }

    public void update(Source source) {
        mSourceManager.update(source);
    }

}
