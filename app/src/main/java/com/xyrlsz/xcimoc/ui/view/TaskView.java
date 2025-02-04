package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.component.DialogCaller;
import com.xyrlsz.xcimoc.model.Task;

import java.util.List;

/**
 * Created by Hiroshi on 2016/9/7.
 */
public interface TaskView extends BaseView, DialogCaller {

    void onTaskLoadSuccess(List<Task> list, boolean local);

    void onTaskLoadFail();

    void onLastChange(String path);

    void onTaskAdd(List<Task> list);

    void onTaskParse(long id);

    void onTaskProcess(long id, int progress, int max);

    void onTaskPause(long id);

    void onTaskError(long id);

    void onTaskDeleteSuccess(List<Long> list);

    void onTaskDeleteFail();

}
