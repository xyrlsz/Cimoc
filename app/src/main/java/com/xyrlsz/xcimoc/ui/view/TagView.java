package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.component.DialogCaller;
import com.xyrlsz.xcimoc.component.ThemeResponsive;
import com.xyrlsz.xcimoc.model.Tag;

import java.util.List;

/**
 * Created by Hiroshi on 2016/10/10.
 */

public interface TagView extends BaseView, ThemeResponsive, DialogCaller {

    void onTagLoadSuccess(List<Tag> list);

    void onTagLoadFail();

    void onTagDeleteSuccess(Tag tag);

    void onTagDeleteFail();

    void onTagRestore(List<Tag> list);

}
