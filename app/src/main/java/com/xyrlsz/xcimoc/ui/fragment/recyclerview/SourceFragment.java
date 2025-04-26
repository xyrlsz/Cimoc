package com.xyrlsz.xcimoc.ui.fragment.recyclerview;

import static com.xyrlsz.xcimoc.ui.activity.WebviewActivity.EXTRA_IS_USE_TO_WEB_PARSER;
import static com.xyrlsz.xcimoc.ui.activity.WebviewActivity.EXTRA_WEB_URL;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.presenter.BasePresenter;
import com.xyrlsz.xcimoc.presenter.SourcePresenter;
import com.xyrlsz.xcimoc.ui.activity.SearchActivity;
import com.xyrlsz.xcimoc.ui.activity.SourceDetailActivity;
import com.xyrlsz.xcimoc.ui.activity.WebviewActivity;
import com.xyrlsz.xcimoc.ui.adapter.BaseAdapter;
import com.xyrlsz.xcimoc.ui.adapter.SourceAdapter;
import com.xyrlsz.xcimoc.ui.view.SourceView;
import com.xyrlsz.xcimoc.utils.HintUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hiroshi on 2016/8/11.
 */
public class SourceFragment extends RecyclerViewFragment implements SourceView, SourceAdapter.OnItemCheckedListener {

    private SourcePresenter mPresenter;
    private SourceAdapter mSourceAdapter;

    @Override
    protected BasePresenter initPresenter() {
        mPresenter = new SourcePresenter();
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void initView() {
        setHasOptionsMenu(true);
        super.initView();
    }

    @Override
    protected BaseAdapter initAdapter() {
        mSourceAdapter = new SourceAdapter(getActivity(), new ArrayList<Source>());
        mSourceAdapter.setOnItemCheckedListener(this);
        return mSourceAdapter;
    }

    @Override
    protected RecyclerView.LayoutManager initLayoutManager() {
        return new GridLayoutManager(getActivity(), 2);
    }

    @Override
    protected void initData() {
        mPresenter.load();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_source, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.comic_search:
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
                break;
            case R.id.comic_inverseSelection:
                for (int i = 0; i < mSourceAdapter.getItemCount(); i++) {
                    Source source = mSourceAdapter.getItem(i);
                    source.setEnable(!source.getEnable());
                    mPresenter.update(source);
                }
                mSourceAdapter.notifyDataSetChanged();
                break;
            case R.id.comic_allSelection:
                for (int i = 0; i < mSourceAdapter.getItemCount(); i++) {
                    Source source = mSourceAdapter.getItem(i);
                    source.setEnable(true);
                    mPresenter.update(source);
                }
                mSourceAdapter.notifyDataSetChanged();
                break;
            case R.id.comic_AllDeselect:
                for (int i = 0; i < mSourceAdapter.getItemCount(); i++) {
                    Source source = mSourceAdapter.getItem(i);
                    source.setEnable(false);
                    mPresenter.update(source);
                }
                mSourceAdapter.notifyDataSetChanged();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(View view, int position) {
//        Source source = mSourceAdapter.getItem(position);
//        if (SourceManager.getInstance(this).getParser(source.getType()).getCategory() == null) {
//            HintUtils.showToast(getActivity(), R.string.common_execute_fail);
//        } else {
//            Intent intent = CategoryActivity.createIntent(getActivity(), source.getType(), source.getTitle());
//            startActivity(intent);
//        }
        Source source = mSourceAdapter.getItem(position);
        Intent intent = new Intent(getContext(), WebviewActivity.class);
        String url = source.getBaseUrl();
        if (url == null || url.isEmpty()) {
            return;
        }
        intent.putExtra(EXTRA_WEB_URL, url);
        intent.putExtra(EXTRA_IS_USE_TO_WEB_PARSER, false);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        Intent intent = SourceDetailActivity.createIntent(getActivity(), mSourceAdapter.getItem(position).getType());
        startActivity(intent);
        return true;
    }

    @Override
    public void onItemCheckedListener(boolean isChecked, int position) {
        Source source = mSourceAdapter.getItem(position);
        source.setEnable(isChecked);
        mPresenter.update(source);
    }

    @Override
    public void onSourceLoadSuccess(List<Source> list) {
        hideProgressBar();
        mSourceAdapter.addAll(list);
    }

    @Override
    public void onSourceLoadFail() {
        hideProgressBar();
        HintUtils.showToast(getActivity(), R.string.common_data_load_fail);
    }

    @Override
    public void onThemeChange(@ColorRes int primary, @ColorRes int accent) {
        mSourceAdapter.setColor(ContextCompat.getColor(getActivity(), accent));
        mSourceAdapter.notifyDataSetChanged();
    }

}
