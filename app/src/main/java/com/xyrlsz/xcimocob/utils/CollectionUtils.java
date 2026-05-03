package com.xyrlsz.xcimocob.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.reactivex.rxjava3.functions.Function;

/**
 * Created by Hiroshi on 2016/12/2.
 */

public class CollectionUtils {

    public static <E> Collection<E> minus(Collection<E> lhs, Collection<E> rhs) {
        Collection<E> collection = new ArrayList<>(lhs);
        lhs.removeAll(rhs);
        return collection;
    }

    public static <T, R> List<R> map(Collection<T> origin, Function<T, R> func) {
        List<R> result = new ArrayList<>(origin.size());
        for (T element : origin) {
            try {
                result.add(func.apply(element));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public static int[] unbox(List<Integer> list) {
        int[] result = new int[list.size()];
        for (int i = 0; i != result.length; ++i) {
            result[i] = list.get(i);
        }
        return result;
    }

}
