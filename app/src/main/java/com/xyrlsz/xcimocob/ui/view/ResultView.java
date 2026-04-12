package com.xyrlsz.xcimocob.ui.view;

import com.xyrlsz.xcimocob.model.Comic;

import java.util.List;

/**
 * Created by Hiroshi on 2016/8/21.
 */
public interface ResultView extends BaseView {

    void onSearchError();

    void onSearchSuccess(Comic comic);

    void onLoadSuccess(List<Comic> list);

    void onLoadFail();

}
