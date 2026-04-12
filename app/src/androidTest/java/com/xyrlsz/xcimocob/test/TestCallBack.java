package com.xyrlsz.xcimocob.test;

import com.xyrlsz.xcimocob.model.Source;

public interface TestCallBack {
    void onSuccess(Source source);

    void onFail(Source source);
}
