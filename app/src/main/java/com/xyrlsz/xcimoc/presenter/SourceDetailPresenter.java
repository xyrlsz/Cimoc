package com.xyrlsz.xcimoc.presenter;

import com.xyrlsz.xcimoc.manager.ComicManager;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.ui.view.SourceDetailView;

/**
 * Created by Hiroshi on 2017/1/18.
 */

public class SourceDetailPresenter extends BasePresenter<SourceDetailView> {

    private SourceManager mSourceManager;
    private ComicManager mComicManager;

    @Override
    protected void onViewAttach() {
        mSourceManager = SourceManager.getInstance(mBaseView);
        mComicManager = ComicManager.getInstance(mBaseView);
    }

    public void load(int type) {
        Source source = mSourceManager.load(type);
        long count = mComicManager.countBySource(type);
        mBaseView.onSourceLoadSuccess(type, source.getTitle(), count);
    }

}
