package com.xyrlsz.xcimoc.ui.activity;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.misc.Switcher;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.presenter.BasePresenter;
import com.xyrlsz.xcimoc.presenter.SearchPresenter;
import com.xyrlsz.xcimoc.ui.adapter.AutoCompleteAdapter;
import com.xyrlsz.xcimoc.ui.fragment.dialog.MultiAdpaterDialogFragment;
import com.xyrlsz.xcimoc.ui.view.SearchView;
import com.xyrlsz.xcimoc.utils.CollectionUtils;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Hiroshi on 2016/10/11.
 */

public class SearchActivity extends BackActivity implements SearchView, TextView.OnEditorActionListener {

    private final static int DIALOG_REQUEST_SOURCE = 0;

    @BindView(R.id.search_text_layout)
    TextInputLayout mInputLayout;
    @BindView(R.id.search_keyword_input)
    AppCompatAutoCompleteTextView mEditText;
    @BindView(R.id.search_action_button)
    FloatingActionButton mActionButton;
    @BindView(R.id.search_strict_checkbox)
    AppCompatCheckBox mStrictCheckBox;
    @BindView(R.id.search_STSame_checkbox)
    AppCompatCheckBox mSTSameCheckBox;

    private ArrayAdapter<String> mArrayAdapter;

    private SearchPresenter mPresenter;
    private List<Switcher<Source>> mSourceList;
    private boolean mAutoComplete;

    @Override
    protected BasePresenter initPresenter() {
        mPresenter = new SearchPresenter();
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void initView() {
        mAutoComplete = mPreference.getBoolean(PreferenceManager.PREF_SEARCH_AUTO_COMPLETE, false);
        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mActionButton != null && !mActionButton.isShown()) {
                    mActionButton.show();
                }
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mInputLayout.setError(null);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mAutoComplete) {
                    String keyword = mEditText.getText().toString();
                    if (!StringUtils.isEmpty(keyword)) {
                        mPresenter.loadAutoComplete(keyword);
                    }
                }
            }
        });
        mEditText.setOnEditorActionListener(this);
        if (mAutoComplete) {
            mArrayAdapter = new AutoCompleteAdapter(this);
            mEditText.setAdapter(mArrayAdapter);
        }
    }

    @Override
    protected void initData() {
        mSourceList = new ArrayList<>();
        mPresenter.loadSource();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_menu_source:
                if (!mSourceList.isEmpty()) {
                    int size = mSourceList.size();
                    String[] arr1 = new String[size];
                    boolean[] arr2 = new boolean[size];
                    for (int i = 0; i < size; ++i) {
                        arr1[i] = mSourceList.get(i).getElement().getTitle();
                        arr2[i] = mSourceList.get(i).isEnable();
                    }
                    MultiAdpaterDialogFragment fragment =
                            MultiAdpaterDialogFragment.newInstance(R.string.search_source_select, arr1, arr2, DIALOG_REQUEST_SOURCE);
                    fragment.show(getSupportFragmentManager(), null);
                    break;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_SOURCE:
                boolean[] check = bundle.getBooleanArray(EXTRA_DIALOG_RESULT_VALUE);
                if (check != null) {
                    int size = mSourceList.size();
                    for (int i = 0; i < size; ++i) {
                        mSourceList.get(i).setEnable(check[i]);
                    }
                }
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            mActionButton.performClick();
            return true;
        }
        return false;
    }

    @OnClick(R.id.search_action_button)
    void onSearchButtonClick() {
        String keyword = mEditText.getText().toString();
        boolean strictSearch = mStrictCheckBox.isChecked();
        boolean stSame = mSTSameCheckBox.isChecked();
        if (StringUtils.isEmpty(keyword)) {
            mInputLayout.setError(getString(R.string.search_keyword_empty));
        } else {
            ArrayList<Integer> list = new ArrayList<>();
            for (Switcher<Source> switcher : mSourceList) {
                if (switcher.isEnable()) {
                    list.add(switcher.getElement().getType());
                }
            }
            if (list.isEmpty()) {
                HintUtils.showToast(this, R.string.search_source_none);
            } else {
                startActivity(ResultActivity.createIntent(this, keyword, strictSearch, stSame,
                        CollectionUtils.unbox(list), ResultActivity.LAUNCH_MODE_SEARCH));
            }
        }
    }

    @Override
    public void onAutoCompleteLoadSuccess(List<String> list) {
        mArrayAdapter.clear();
        mArrayAdapter.addAll(list);
    }

    @Override
    public void onSourceLoadSuccess(List<Source> list) {
        hideProgressBar();
        for (Source source : list) {
            mSourceList.add(new Switcher<>(source, true));
        }
    }

    @Override
    public void onSourceLoadFail() {
        hideProgressBar();
        HintUtils.showToast(this, R.string.search_source_load_fail);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.comic_search);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_search;
    }

    @Override
    protected boolean isNavTranslation() {
        return true;
    }

}
