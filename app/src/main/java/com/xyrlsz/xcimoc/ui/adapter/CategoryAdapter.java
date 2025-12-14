package com.xyrlsz.xcimoc.ui.adapter;

import android.content.Context;
import android.util.Pair;
import android.widget.ArrayAdapter;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.utils.CollectionUtils;
import com.xyrlsz.xcimoc.utils.STConvertUtils;

import java.util.List;

import rx.functions.Func1;

/**
 * Created by Hiroshi on 2018/2/13.
 */

public class CategoryAdapter extends ArrayAdapter<String> {

    private final List<Pair<String, String>> mCategoryList;

    public CategoryAdapter(Context context, List<Pair<String, String>> list) {
        super(context, R.layout.item_spinner, CollectionUtils.map(list, pair -> STConvertUtils.convert(pair.first)));
        mCategoryList = list;
    }

    public String getValue(int position) {
        return mCategoryList.get(position).second;
    }

}
