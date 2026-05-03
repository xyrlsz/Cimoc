package com.xyrlsz.xcimocob.ui.activity;

import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.utils.ThemeUtils;
import com.xyrlsz.xcimocob.utils.ThreadRunUtils;



/**
 * Created by Hiroshi on 2016/9/11.
 */
public abstract class BackActivity extends BaseActivity {

    @Nullable
    ProgressBar mProgressBar;

    @Override
    protected void initToolbar() {
        super.initToolbar();
        if (mToolbar != null) {
            mToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    @Override
    protected void initViewById() {
        super.initViewById();
        mProgressBar = findViewById(R.id.custom_progress_bar);
    }

    @Override
    protected void initView() {
        if (mProgressBar != null) {
            int resId = ThemeUtils.getResourceId(this, R.attr.colorAccent);
            mProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, resId), PorterDuff.Mode.SRC_ATOP);
        }
    }

    protected boolean isProgressBarShown() {
        return mProgressBar != null && mProgressBar.isShown();
    }

    protected void hideProgressBar() {
        ThreadRunUtils.runOnMainThread(() -> {
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }

}
