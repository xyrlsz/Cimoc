package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.component.ThemeResponsive;
import com.xyrlsz.xcimoc.model.Source;

import java.util.List;

/**
 * Created by Hiroshi on 2016/8/21.
 */
public interface SourceView extends BaseView, ThemeResponsive {

    void onSourceLoadSuccess(List<Source> list);

    void onSourceLoadFail();

}
