package com.xyrlsz.xcimocob.ui.view;

import com.xyrlsz.xcimocob.component.ThemeResponsive;
import com.xyrlsz.xcimocob.model.Source;

import java.util.List;

/**
 * Created by Hiroshi on 2016/8/21.
 */
public interface SourceView extends BaseView, ThemeResponsive {

    void onSourceLoadSuccess(List<Source> list);

    void onSourceLoadFail();

}
