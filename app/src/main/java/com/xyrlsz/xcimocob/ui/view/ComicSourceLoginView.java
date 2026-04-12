package com.xyrlsz.xcimocob.ui.view;

import com.xyrlsz.xcimocob.component.DialogCaller;

public interface ComicSourceLoginView extends BaseView, DialogCaller {
    void onLoginSuccess();

    void onLoginFail();

    void onStartLogin();
}
