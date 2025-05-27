package com.xyrlsz.xcimoc.test;

import com.xyrlsz.xcimoc.model.Source;

public interface TestCallBack {
    void onSuccess(Source source);

    void onFail(Source source);
}
