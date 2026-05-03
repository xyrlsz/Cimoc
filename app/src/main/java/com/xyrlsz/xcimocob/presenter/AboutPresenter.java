package com.xyrlsz.xcimocob.presenter;

import com.xyrlsz.xcimocob.core.Update;
import com.xyrlsz.xcimocob.ui.view.AboutView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by Hiroshi on 2016/8/24.
 */
public class AboutPresenter extends BasePresenter<AboutView> {

    public void checkUpdate(final String version) {
        mCompositeSubscription.add(Update.check()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        if (version.equals(s)) {
                            mBaseView.onUpdateNone();
                        } else if(s.compareTo(version)>0){
                            mBaseView.onUpdateReady();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        mBaseView.onCheckError();
                    }
                }));
    }

}
