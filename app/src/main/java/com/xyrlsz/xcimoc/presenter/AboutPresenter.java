package com.xyrlsz.xcimoc.presenter;

import com.xyrlsz.xcimoc.core.Update;
import com.xyrlsz.xcimoc.ui.view.AboutView;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by Hiroshi on 2016/8/24.
 */
public class AboutPresenter extends BasePresenter<AboutView> {

    public void checkUpdate(final String version) {
        mCompositeSubscription.add(Update.check()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        if (version.equals(s)) {
                            mBaseView.onUpdateNone();
                        } else if(s.compareTo(version)>0){
                            mBaseView.onUpdateReady();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mBaseView.onCheckError();
                    }
                }));
    }

}
