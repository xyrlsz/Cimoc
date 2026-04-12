package com.xyrlsz.xcimocob.ui.view;

import com.xyrlsz.xcimocob.model.MiniComic;

import java.util.List;

/**
 * Created by Hiroshi on 2016/8/21.
 */
public interface HistoryView extends GridView {

    void onHistoryDelete(long id);

    void onItemUpdate(MiniComic comic);

    void OnComicRestore(List<Object> list);

    void onHistoryClearSuccess();

}
