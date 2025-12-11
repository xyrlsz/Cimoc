package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.component.DialogCaller;

public interface ComicSourceLoginView extends BaseView, DialogCaller {
    void onLoginSuccess();

    void onLoginFail();

    void onStartLogin();
}
