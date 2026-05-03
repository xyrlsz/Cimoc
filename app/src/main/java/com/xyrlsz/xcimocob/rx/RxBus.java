package com.xyrlsz.xcimocob.rx;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * Created by Hiroshi on 2016/8/21.
 */
public class RxBus {

    private static RxBus instance;

    private Subject<Object> bus;

    private RxBus() {
        bus = PublishSubject.<Object>create().toSerialized();
    }

    public static RxBus getInstance() {
        if (instance == null) {
            synchronized (RxBus.class) {
                if (instance == null) {
                    instance = new RxBus();
                }
            }
        }
        return instance;
    }

    public void post(RxEvent event) {
        bus.onNext(event);
    }

    public Observable<RxEvent> toObservable(@RxEvent.EventType final int type) {
        return bus.ofType(RxEvent.class)
                .filter(new io.reactivex.rxjava3.functions.Predicate<RxEvent>() {
                    @Override
                    public boolean test(RxEvent rxEvent) {
                        return rxEvent.getType() == type;
                    }
                }).observeOn(AndroidSchedulers.mainThread());
    }

}
