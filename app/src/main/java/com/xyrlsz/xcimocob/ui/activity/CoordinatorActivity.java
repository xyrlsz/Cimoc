package com.xyrlsz.xcimocob.ui.activity;

import android.view.View;
import android.widget.FrameLayout;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.ui.adapter.BaseAdapter;



/**
 * Created by Hiroshi on 2016/12/1.
 */

public abstract class CoordinatorActivity extends BackActivity implements
        BaseAdapter.OnItemClickListener, BaseAdapter.OnItemLongClickListener {

    FloatingActionButton mActionButton;
    FloatingActionButton mActionButton2;
    RecyclerView mRecyclerView;
    CoordinatorLayout mCoordinatorLayout;

    FrameLayout mLayoutView;

    @Override
    protected void initViewById() {
        super.initViewById();
        mActionButton = findViewById(R.id.coordinator_action_button);
        mActionButton2 = findViewById(R.id.coordinator_action_button2);
        mRecyclerView = findViewById(R.id.coordinator_recycler_view);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        mLayoutView = findViewById(R.id.coordinator_frame_layout);
    }

    @Override
    protected void initView() {
        super.initView();
        mRecyclerView.setLayoutManager(initLayoutManager());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
        BaseAdapter adapter = initAdapter();
        adapter.setOnItemClickListener(this);
        adapter.setOnItemLongClickListener(this);
        RecyclerView.ItemDecoration decoration = adapter.getItemDecoration();
        if (decoration != null) {
            mRecyclerView.addItemDecoration(adapter.getItemDecoration());
        }
        mRecyclerView.setAdapter(adapter);
        ViewCompat.setOnApplyWindowInsetsListener(mLayoutView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    systemBars.bottom
            );
            return insets;
        });
        initActionButton();
    }

    protected abstract BaseAdapter initAdapter();

    protected void initActionButton() {
    }

    protected RecyclerView.LayoutManager initLayoutManager() {
        return new LinearLayoutManager(this);
    }

    @Override
    public void onItemClick(View view, int position) {
    }

    @Override
    public boolean onItemLongClick(View view, int position) {
        return false;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_coordinator;
    }

    @Override
    protected View getLayoutView() {
        return mCoordinatorLayout;
    }

}
