package com.xyrlsz.xcimoc.utils;

import androidx.collection.LongSparseArray;

import com.xyrlsz.xcimoc.model.Comic;

import java.util.List;

/**
 * Created by Hiroshi on 2017/3/24.
 */

public class ComicUtils {

    public static LongSparseArray<Comic> buildComicMap(List<Comic> list) {
        LongSparseArray<Comic> array = new LongSparseArray<>();
        for (Comic comic : list) {
            array.put(comic.getId(), comic);
        }
        return array;
    }

}
