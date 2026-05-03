package com.xyrlsz.xcimocob.presenter;

import com.xyrlsz.xcimocob.core.Manga;
import com.xyrlsz.xcimocob.manager.SourceManager;
import com.xyrlsz.xcimocob.model.Source;
import com.xyrlsz.xcimocob.ui.view.SearchView;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by Hiroshi on 2016/10/11.
 */

public class SearchPresenter extends BasePresenter<SearchView> {

    private SourceManager mSourceManager;

    @Override
    protected void onViewAttach() {
        mSourceManager = SourceManager.getInstance(mBaseView);
    }

    public void loadSource() {
        mCompositeSubscription.add(mSourceManager.listEnableInRx()
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

    public void loadAutoComplete(String keyword) {
        mCompositeSubscription.add(Manga.loadAutoComplete(keyword)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<String>>() {
                    @Override
                    public void accept(List<String> list) {
                        mBaseView.onAutoCompleteLoadSuccess(list);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }));
    }

}
