package com.xyrlsz.xcimoc.ui.fragment.recyclerview.grid;

import android.content.Intent;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.MiniComic;
import com.xyrlsz.xcimoc.ui.activity.DetailActivity;
import com.xyrlsz.xcimoc.ui.activity.TaskActivity;
import com.xyrlsz.xcimoc.ui.adapter.BaseAdapter;
import com.xyrlsz.xcimoc.ui.adapter.GridAdapter;
import com.xyrlsz.xcimoc.ui.fragment.dialog.ItemDialogFragment;
import com.xyrlsz.xcimoc.ui.fragment.dialog.MessageDialogFragment;
import com.xyrlsz.xcimoc.ui.fragment.recyclerview.RecyclerViewFragment;
import com.xyrlsz.xcimoc.ui.view.GridView;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Hiroshi on 2016/9/22.
 */

public abstract class GridFragment extends RecyclerViewFragment implements GridView {

    protected static final int DIALOG_REQUEST_OPERATION = 0;
    protected GridAdapter mGridAdapter;
    protected long mSavedId = -1;
    @BindView(R.id.grid_action_button)
    FloatingActionButton mActionButton;

    @Override
    protected BaseAdapter initAdapter() {
        mGridAdapter = new GridAdapter(getActivity(), new LinkedList<>());
        mGridAdapter.setProvider(getAppInstance().getBuilderProvider());
        mGridAdapter.setTitleGetter(SourceManager.getInstance(this).new TitleGetter());
        mRecyclerView.setRecycledViewPool(getAppInstance().getGridRecycledPool());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NotNull RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        getAppInstance().getBuilderProvider().pause();
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        getAppInstance().getBuilderProvider().resume();
                        break;
                }
            }
        });
        mActionButton.setImageResource(getActionButtonRes());
        return mGridAdapter;
    }

    @Override
    protected RecyclerView.LayoutManager initLayoutManager() {
//        double rate = (double) App.mWidthPixels / App.mHeightPixels;
//        int spanCount = 3;
//        if (rate > 9.0 / 16) {
//            spanCount = 4;
//        }
        int spanCount = 3;
        if (App.mHeightPixels * 9 < App.mWidthPixels * 16) {
            spanCount = 4;
        }
        GridLayoutManager manager = new GridLayoutManager(getActivity(), spanCount);
        manager.setRecycleChildrenOnDetach(true);
        return manager;
    }

    @OnClick(R.id.grid_action_button)
    void onActionButtonClick() {
        performActionButtonClick();
    }

    @Override
    public void onItemClick(View view, int position) {
        MiniComic comic = (MiniComic) mGridAdapter.getItem(position);
        Intent intent = comic.isLocal() ? TaskActivity.createIntent(getActivity(), comic.getId()) :
                DetailActivity.createIntent(getActivity(), comic.getId(), -1, null);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        mSavedId = ((MiniComic) mGridAdapter.getItem(position)).getId();
        ItemDialogFragment fragment = ItemDialogFragment.newInstance(R.string.common_operation_select,
                getOperationItems(), DIALOG_REQUEST_OPERATION);
        fragment.setTargetFragment(this, 0);
        fragment.show(requireActivity().getSupportFragmentManager(), null);
        return true;
    }

    @Override
    public void onComicLoadSuccess(List<Object> list) {
        mGridAdapter.addAll(list);
    }

    @Override
    public void onComicLoadFail() {
        HintUtils.showToast(getActivity(), R.string.common_data_load_fail);
    }

    @Override
    public void onExecuteFail() {
        hideProgressDialog();
        HintUtils.showToast(getActivity(), R.string.common_execute_fail);
    }

    @Override
    public void onThemeChange(@ColorRes int primary, @ColorRes int accent) {
        mActionButton.setBackgroundTintList(ContextCompat.getColorStateList(getActivity(), accent));
    }

    protected void showComicInfo(Comic comic, int request) {
        if (comic == null) {
            MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.common_execute_fail,
                    R.string.comic_info_not_found, true, request);
            fragment.setTargetFragment(this, 0);
            fragment.show(requireActivity().getSupportFragmentManager(), null);
            return;
        }
        String content =
                StringUtils.format("%s  %s\n%s  %s\n%s  %s\n%s  %s\n%s  %s",
                        getString(R.string.comic_info_title),
                        comic.getTitle(),
                        getString(R.string.comic_info_source),
                        SourceManager.getInstance(this).getParser(comic.getSource()).getTitle(),
                        getString(R.string.comic_info_status),
                        comic.getFinish() == null ? getString(R.string.comic_status_finish) :
                                getString(R.string.comic_status_continue),
                        getString(R.string.comic_info_chapter),
                        comic.getChapter() == null ? getString(R.string.common_null) : comic.getChapter(),
                        getString(R.string.comic_info_time),
                        comic.getHistory() == null ? getString(R.string.common_null) :
                                StringUtils.getFormatTime("yyyy-MM-dd HH:mm:ss", comic.getHistory()));
        MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.comic_info,
                content, true, request);
        fragment.setTargetFragment(this, 0);
        fragment.show(requireActivity().getSupportFragmentManager(), null);
    }

    protected abstract void performActionButtonClick();

    protected abstract int getActionButtonRes();

    protected abstract String[] getOperationItems();

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_grid;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!App.isNormalExited()) {
            HintUtils.showToastLong(getContext(), R.string.fragment_destroy_tip);
            App.restartApp();
        }
    }
}
