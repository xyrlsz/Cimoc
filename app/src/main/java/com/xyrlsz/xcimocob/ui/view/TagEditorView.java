package com.xyrlsz.xcimocob.ui.view;

import com.xyrlsz.xcimocob.misc.Switcher;
import com.xyrlsz.xcimocob.model.Tag;

import java.util.List;

/**
 * Created by Hiroshi on 2016/12/2.
 */

public interface TagEditorView extends BaseView {

    void onTagLoadSuccess(List<Switcher<Tag>> list);

    void onTagLoadFail();

    void onTagUpdateSuccess();

    void onTagUpdateFail();

}
