package com.xyrlsz.xcimoc.ui.widget.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.ui.fragment.BaseFragment;
import com.xyrlsz.xcimoc.ui.fragment.dialog.ChoiceDialogFragment;
import com.xyrlsz.xcimoc.ui.widget.Option;

/**
 * Created by Hiroshi on 2017/1/10.
 */

public class ChoicePreference extends Option implements View.OnClickListener {

    private final PreferenceManager mPreferenceManager;
    private SharedPreferences mSharedPreferences = null;
    private FragmentManager mFragmentManager;
    private Fragment mTargetFragment;
    private String mPreferenceKey;
    private String[] mItems;
    private int mChoice;
    private int mRequestCode;

    public ChoicePreference(Context context) {
        this(context, null);
    }

    public ChoicePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChoicePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.custom_option, this);

        mPreferenceManager = App.getPreferenceManager();

        setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mFragmentManager != null) {
            ChoiceDialogFragment fragment = ChoiceDialogFragment.newInstance(R.string.dialog_choice,
                    mItems, mChoice, mRequestCode);
            if (mTargetFragment != null) {
                fragment.setTargetFragment(mTargetFragment, 0);
            }
            fragment.show(mFragmentManager, null);
        }
    }

    public void bindPreference(FragmentManager manager, String key, int def, int item, int request) {
        bindPreference(manager, null, null, key, def, item, request);
    }

    public void bindPreference(FragmentManager manager, SharedPreferences sharedPreferences, String key, int def, int item, int request) {
        bindPreference(manager, null, sharedPreferences, key, def, item, request);
    }

    public void bindPreference(FragmentManager manager, BaseFragment fragment, String key, int def, int item, int request) {
        bindPreference(manager, fragment, null, key, def, item, request);
    }

    public void bindPreference(FragmentManager manager, BaseFragment fragment, SharedPreferences sharedPreferences, String key, int def, int item, int request) {
        mFragmentManager = manager;
        mTargetFragment = fragment;
        mPreferenceKey = key;
        mItems = getResources().getStringArray(item);
        mRequestCode = request;
        mSharedPreferences = sharedPreferences;
        if (mSharedPreferences != null) {
            mChoice = mSharedPreferences.getInt(key, def);
            int s = mChoice;
        } else {
            mChoice = mPreferenceManager.getInt(key, def);
        }
        mSummaryView.setText(mItems[mChoice < mItems.length ? mChoice : 0]);
    }

    public int getValue() {
        return mChoice;
    }

    public void setValue(int choice) {
        if (mSharedPreferences != null) {
            mSharedPreferences.edit().putInt(mPreferenceKey, choice).apply();
        } else {
            mPreferenceManager.putInt(mPreferenceKey, choice);
        }
        mChoice = choice;
        mSummaryView.setText(mItems[mChoice < mItems.length ? mChoice : 0]);
    }

}
