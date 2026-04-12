package com.xyrlsz.xcimocob.presenter;

import com.xyrlsz.xcimocob.manager.ComicManager;
import com.xyrlsz.xcimocob.manager.SourceManager;
import com.xyrlsz.xcimocob.model.Source;
import com.xyrlsz.xcimocob.ui.view.SourceDetailView;

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
