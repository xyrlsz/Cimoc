package com.xyrlsz.xcimoc.ui.activity.settings;

import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.component.DialogCaller;
import com.xyrlsz.xcimoc.global.ClickEvents;
import com.xyrlsz.xcimoc.ui.activity.BackActivity;
import com.xyrlsz.xcimoc.ui.adapter.TabPagerAdapter;
import com.xyrlsz.xcimoc.ui.fragment.BaseFragment;
import com.xyrlsz.xcimoc.ui.fragment.config.PageConfigFragment;
import com.xyrlsz.xcimoc.ui.fragment.config.StreamConfigFragment;

import butterknife.BindView;

/**
 * Created by Hiroshi on 2016/10/14.
 */

public class ReaderConfigActivity extends BackActivity implements DialogCaller {

    @BindView(R.id.reader_config_tab_layout)
    TabLayout mTabLayout;
    @BindView(R.id.reader_config_view_pager)
    ViewPager mViewPager;

    private String[] mKeyArray;
    private int[] mChoiceArray;

    @Override
    protected void initView() {
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.reader_config_page));
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.reader_config_stream));
        TabPagerAdapter tabAdapter = new TabPagerAdapter(getSupportFragmentManager(),
                new BaseFragment[]{new PageConfigFragment(), new StreamConfigFragment()},
                new String[]{getString(R.string.reader_config_page), getString(R.string.reader_config_stream)});
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.setAdapter(tabAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
        boolean isStream = mViewPager.getCurrentItem() == 1;
        if (isStream) {
            mKeyArray =  ClickEvents.getStreamClickEvents();
            mChoiceArray = ClickEvents.getStreamClickEventChoice(mPreference);
        } else {
            mKeyArray = ClickEvents.getPageClickEvents();
            mChoiceArray = ClickEvents.getPageClickEventChoice(mPreference);
        }
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                boolean isStream = position == 1;
                if (isStream) {
                    mKeyArray =  ClickEvents.getStreamClickEvents();
                    mChoiceArray = ClickEvents.getStreamClickEventChoice(mPreference);
                } else {
                    mKeyArray = ClickEvents.getPageClickEvents();
                    mChoiceArray = ClickEvents.getPageClickEventChoice(mPreference);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.reader_config_title);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_reader_config;
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        int index = bundle.getInt(EXTRA_DIALOG_RESULT_INDEX);
        mChoiceArray[requestCode] = index;
        mPreference.putInt(mKeyArray[requestCode], index);
    }
}
