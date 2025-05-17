package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.component.DialogCaller;
import com.xyrlsz.xcimoc.component.ThemeResponsive;

import java.util.List;

/**
 * Created by Hiroshi on 2016/9/30.
 */

public interface GridView extends BaseView, DialogCaller, ThemeResponsive {

    void onComicLoadSuccess(List<Object> list);

    void onComicLoadFail();

    void onExecuteFail();

    void filterByKeyword(String keyword);
    void cancelFilter(List<Object>original);
}
