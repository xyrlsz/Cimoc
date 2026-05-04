package com.xyrlsz.xcimocob.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.ui.fragment.BaseFragment;

/**
 * Created by Hiroshi on 2016/10/11.
 * <p>
 * 修复：配置变化后（如旋转屏幕），FragmentManager 会恢复已有 Fragment，
 * 而 getItem() 可能返回未使用的新实例。
 * 改用 FragmentManager.findFragmentByTag() 优先查找已存在的 Fragment。
 */
public class TabPagerAdapter extends FragmentPagerAdapter {

    private BaseFragment[] fragment;
    private String[] title;
    private FragmentManager mFragmentManager;

    public TabPagerAdapter(FragmentManager manager, BaseFragment[] fragment, String[] title) {
        super(manager);
        this.mFragmentManager = manager;
        this.fragment = fragment;
        this.title = title;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        // 优先查找 FragmentManager 中已恢复的实例
        String tag = getTag(position);
        Fragment existing = mFragmentManager.findFragmentByTag(tag);
        if (existing != null) {
            return existing;
        }
        return fragment[position];
    }

    @Override
    public int getCount() {
        return fragment.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return title[position];
    }

    /**
     * 获取 Fragment 在 FragmentManager 中的 tag
     */
    private String getTag(int position) {
        return "android:switcher:" + R.id.comic_view_pager + ":" + position;
    }

}
