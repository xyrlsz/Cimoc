package com.xyrlsz.xcimocob.ui.activity;

import android.content.Context;
import android.content.Intent;

import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.global.Extra;
import com.xyrlsz.xcimocob.presenter.BasePresenter;
import com.xyrlsz.xcimocob.presenter.SourceDetailPresenter;
import com.xyrlsz.xcimocob.ui.view.SourceDetailView;
import com.xyrlsz.xcimocob.ui.widget.Option;



/**
 * Created by Hiroshi on 2017/1/18.
 */

public class SourceDetailActivity extends BackActivity implements SourceDetailView {

    Option mSourceType;
    Option mSourceTitle;
    Option mSourceFavorite;
    private SourceDetailPresenter mPresenter;

    @Override
    protected void initViewById() {
        super.initViewById();
        mSourceType = findViewById(R.id.source_detail_type);
        mSourceTitle = findViewById(R.id.source_detail_title);
        mSourceFavorite = findViewById(R.id.source_detail_favorite);
    }

    public static Intent createIntent(Context context, int type) {
        Intent intent = new Intent(context, SourceDetailActivity.class);
        intent.putExtra(Extra.EXTRA_SOURCE, type);
        return intent;
    }

    @Override
    protected BasePresenter initPresenter() {
        mPresenter = new SourceDetailPresenter();
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void initData() {
        findViewById(R.id.source_detail_favorite).setOnClickListener(v -> onSourceFavoriteClick());
        mPresenter.load(getIntent().getIntExtra(Extra.EXTRA_SOURCE, -1));
    }

    void onSourceFavoriteClick() {
        // TODO 显示这个图源的漫画
    }

    @Override
    public void onSourceLoadSuccess(int type, String title, long count) {
        mSourceType.setSummary(String.valueOf(type));
        mSourceTitle.setSummary(title);
        mSourceFavorite.setSummary(String.valueOf(count));
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_source_detail;
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.source_detail);
    }

}
