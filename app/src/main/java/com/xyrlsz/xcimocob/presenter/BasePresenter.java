package com.xyrlsz.xcimocob.presenter;

import com.xyrlsz.xcimocob.rx.RxBus;
import com.xyrlsz.xcimocob.rx.RxEvent;
import com.xyrlsz.xcimocob.ui.view.BaseView;

import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * Created by Hiroshi on 2016/7/4.
 */
public abstract class BasePresenter<T extends BaseView> {

    protected T mBaseView;
    protected CompositeDisposable mCompositeSubscription;

    public void attachView(T view) {
        this.mBaseView = view;
        onViewAttach();
        mCompositeSubscription = new CompositeDisposable();
        addSubscription(RxEvent.EVENT_SWITCH_NIGHT, new Consumer<RxEvent>() {
            @Override
            public void accept(RxEvent rxEvent) {
                mBaseView.onNightSwitch();
            }
        });
        initSubscription();
    }

    protected void onViewAttach() {
    }

    protected void initSubscription() {
    }

    protected void addSubscription(@RxEvent.EventType int type, Consumer<RxEvent> action) {
        Disposable disposable = RxBus.getInstance().toObservable(type).subscribe(action, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        mCompositeSubscription.add(disposable);
    }

    public void detachView() {
        if (mCompositeSubscription != null) {
            mCompositeSubscription.dispose();
        }
        mBaseView = null;
    }

}
