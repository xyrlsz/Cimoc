package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.model.MiniComic;
import com.xyrlsz.xcimoc.model.Task;

import java.util.ArrayList;

/**
 * Created by Hiroshi on 2016/9/1.
 */
public interface DownloadView extends GridView {

    void onDownloadAdd(MiniComic comic);

    void onDownloadDelete(long id);

    void onDownloadDeleteSuccess(long id);

    void onDownloadStart();

    void onDownloadStop();

    void onTaskLoadSuccess(ArrayList<Task> list);

}
