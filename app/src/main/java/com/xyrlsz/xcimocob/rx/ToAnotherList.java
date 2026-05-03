package com.xyrlsz.xcimocob.rx;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.core.ObservableTransformer;

/**
 * Created by Hiroshi on 2016/10/23.
 */

public class ToAnotherList<T, R> implements ObservableTransformer<List<T>, List<R>> {

    private Function<T, R> func;

    public ToAnotherList(Function<T, R> func) {
        this.func = func;
    }

    @Override
    public Observable<List<R>> apply(Observable<List<T>> observable) {
        return observable.flatMap(new Function<List<T>, Observable<T>>() {
            @Override
            public Observable<T> apply(List<T> list) {
                return Observable.fromIterable(list);
            }
        }).map(func).toList().toObservable();
    }

}
