package com.haleydu.cimoc.utils;


import org.reactivestreams.Publisher;


import hu.akarnokd.rxjava3.interop.RxJavaInterop;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Observable;


public class ObservableUtils {

    public static <T>  rx.Observable<T> V3toV1(Observable<T> src) {
        Publisher<T> publisher = src.toFlowable(BackpressureStrategy.BUFFER);
        return RxJavaInterop.toV1Observable(publisher);
    }
}
