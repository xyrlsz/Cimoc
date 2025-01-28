package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hiroshi on 2016/8/21.
 */
public interface DetailView extends BaseView {

    void onComicLoadSuccess(Comic comic);

    void onChapterLoadSuccess(List<Chapter> list);

    void onLastChange(String chapter);

    void onParseError();

    void onTaskAddSuccess(ArrayList<Task> list);

    void onTaskAddFail();

    void onPreLoadSuccess(List<Chapter> list,Comic comic);
}
