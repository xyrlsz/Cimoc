package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.component.DialogCaller;

/**
 * Created by Hiroshi on 2016/8/21.
 */
public interface SettingsView extends BaseView, DialogCaller {

    void onFileMoveSuccess();

    void onExecuteSuccess();

    void onExecuteFail();

}
