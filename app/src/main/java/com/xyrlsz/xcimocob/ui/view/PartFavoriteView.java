package com.xyrlsz.xcimocob.ui.view;

import com.xyrlsz.xcimocob.component.DialogCaller;
import com.xyrlsz.xcimocob.model.MiniComic;

import java.util.List;

/**
 * Created by Hiroshi on 2016/10/11.
 */

public interface PartFavoriteView extends BaseView, DialogCaller {

    void onComicLoadSuccess(List<Object> list);

    void onComicLoadFail();

    void onComicTitleLoadSuccess(List<String> list);

    void onComicTitleLoadFail();

    void onComicInsertSuccess(List<Object> list);

    void onComicInsertFail();

    void onComicAdd(MiniComic comic);

    void onComicRead(MiniComic comic);

    void onComicRemove(long id);

    void onHighlightCancel(MiniComic comic);

}
