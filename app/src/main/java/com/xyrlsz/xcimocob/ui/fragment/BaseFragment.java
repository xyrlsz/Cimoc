package com.xyrlsz.xcimocob.ui.fragment;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.manager.PreferenceManager;
import com.xyrlsz.xcimocob.presenter.BasePresenter;
import com.xyrlsz.xcimocob.ui.activity.BaseActivity;
import com.xyrlsz.xcimocob.ui.view.BaseView;
import com.xyrlsz.xcimocob.utils.ThemeUtils;
import com.xyrlsz.xcimocob.utils.ThreadRunUtils;



/**
 * Created by Hiroshi on 2016/7/1.
 */
public abstract class BaseFragment extends Fragment implements BaseView {

    protected PreferenceManager mPreference;
    @Nullable
    ProgressBar mProgressBar;
    protected View mRootView;
    private BasePresenter mBasePresenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(getLayoutRes(), container, false);
        mProgressBar = mRootView.findViewById(R.id.custom_progress_bar);
        mPreference = App.getPreferenceManager();
        mBasePresenter = initPresenter();
        initProgressBar();
        initData();
        initView();
        return mRootView;
    }

    @Override
    public void onDestroyView() {
        if (mBasePresenter != null) {
            mBasePresenter.detachView();
        }
        super.onDestroyView();
    }

    @Override
    public App getAppInstance() {
        return App.getApp();
    }

    @Override
    public void onNightSwitch() {
    }

    private void initProgressBar() {
        if (mProgressBar != null) {
            int resId = ThemeUtils.getResourceId(requireActivity(), R.attr.colorAccent);
            mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(requireActivity(), resId), PorterDuff.Mode.SRC_ATOP);
        }
    }

    protected void initView() {
    }

    protected void initData() {
    }

    protected BasePresenter initPresenter() {
        return null;
    }

    protected abstract @LayoutRes
    int getLayoutRes();

    protected void showProgressDialog() {
        ((BaseActivity) requireActivity()).showProgressDialog();
    }

    protected void hideProgressDialog() {
        ((BaseActivity) requireActivity()).hideProgressDialog();
    }

    protected void hideProgressBar() {
        ThreadRunUtils.runOnMainThread(()->{
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }

}
