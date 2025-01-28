package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.component.DialogCaller;
import com.xyrlsz.xcimoc.model.Source;

import java.util.List;

/**
 * Created by Hiroshi on 2016/10/11.
 */

public interface SearchView extends BaseView, DialogCaller {

    void onSourceLoadSuccess(List<Source> list);

    void onSourceLoadFail();

    void onAutoCompleteLoadSuccess(List<String> list);

}
