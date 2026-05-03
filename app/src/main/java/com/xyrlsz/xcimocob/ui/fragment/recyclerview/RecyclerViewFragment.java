package com.xyrlsz.xcimocob.ui.fragment.recyclerview;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.ui.adapter.BaseAdapter;
import com.xyrlsz.xcimocob.ui.fragment.BaseFragment;


/**
 * Created by Hiroshi on 2016/10/11.
 */

public abstract class RecyclerViewFragment extends BaseFragment implements BaseAdapter.OnItemClickListener,
        BaseAdapter.OnItemLongClickListener {

    protected RecyclerView mRecyclerView;

    @Override
    protected void initView() {
        mRecyclerView = mRootView.findViewById(R.id.recycler_view_content);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setLayoutManager(initLayoutManager());
        BaseAdapter adapter = initAdapter();
        if (adapter != null) {
            adapter.setOnItemClickListener(this);
            adapter.setOnItemLongClickListener(this);
            mRecyclerView.addItemDecoration(adapter.getItemDecoration());
            mRecyclerView.setAdapter(adapter);
        }
    }

    abstract protected BaseAdapter initAdapter();

    protected abstract RecyclerView.LayoutManager initLayoutManager();

    @Override
    public void onItemClick(View view, int position) {
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        return false;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_recycler_view;
    }

}
