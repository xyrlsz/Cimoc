package com.haleydu.cimoc.presenter;

import com.haleydu.cimoc.manager.ComicManager;
import com.haleydu.cimoc.manager.SourceManager;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.ui.view.SourceDetailView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2017/1/18.
 */

public class SourceDetailPresenter extends BasePresenter<SourceDetailView> {

    private SourceManager mSourceManager;
    private ComicManager mComicManager;

    @Override
    protected void onViewAttach() {
        mSourceManager = SourceManager.getInstance(mBaseView);
        mComicManager = ComicManager.getInstance(mBaseView);
    }

    public void load(int type) {
        Disposable disposable =  mSourceManager.load(type).subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.single())
                .subscribe(new Consumer<Source>() {
                    @Override
                    public void accept(Source source) throws Throwable {
                        long count = mComicManager.countBySource(type);
                        mBaseView.onSourceLoadSuccess(type, source.getTitle(), count);
                    }
                });
        disposable.dispose();
    }

}
