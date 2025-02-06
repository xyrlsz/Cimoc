package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.component.DialogCaller;

import java.util.List;

/**
 * Created by Hiroshi on 2017/5/14.
 */

public interface LocalView extends GridView, DialogCaller {

    void onLocalDeleteSuccess(long id);

    void onLocalScanSuccess(List<Object> list);

}
