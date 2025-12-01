package com.xyrlsz.xcimoc.ui.fragment;

import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;

import androidx.appcompat.widget.AppCompatSpinner;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.global.Extra;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.Category;
import com.xyrlsz.xcimoc.presenter.BasePresenter;
import com.xyrlsz.xcimoc.ui.adapter.CategoryAdapter;
import com.xyrlsz.xcimoc.ui.adapter.GridAdapter;
import com.xyrlsz.xcimoc.ui.view.CategoryView;
import com.xyrlsz.xcimoc.utils.HintUtils;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.OnClick;


public class CategoryFragment extends BaseFragment implements CategoryView, AdapterView.OnItemSelectedListener {
    @BindViews({R.id.category_spinner_subject, R.id.category_spinner_area, R.id.category_spinner_reader,
            R.id.category_spinner_year, R.id.category_spinner_progress, R.id.category_spinner_order})
    List<AppCompatSpinner> mSpinnerList;
    @BindViews({R.id.category_subject, R.id.category_area, R.id.category_reader,
            R.id.category_year, R.id.category_progress, R.id.category_order})
    List<View> mCategoryView;
    @BindView(R.id.category_source)
    View mCategorySourceView;
    @BindView(R.id.category_spinner_source)
    AppCompatSpinner mCategorySourceSpinner;
    private GridAdapter mGridAdapter;
    private Category mCategory;
    private SourceManager mSourceManager;
    private LinkedList<Pair<String, String>> mSourceList;

    @Override
    protected BasePresenter initPresenter() {
        return super.initPresenter();
    }


    @Override
    protected void initView() {
        setHasOptionsMenu(true);
//        for (Pair<String, String> pair : mSourceList) {
//            mCategory = mSourceManager.getParser(Integer.parseInt(pair.second)).getCategory();
//            if (mCategory != null) {
//                initSpinner(Integer.parseInt(pair.second));
//            }
//        }
        initSpinner(mSourceList.get(0).second);
    }

    @Override
    protected void initData() {
        mSourceManager = SourceManager.getInstance(this);
        List<Source> sourceList = mSourceManager.listEnable();
        mSourceList = new LinkedList<>();
        for (Source source : sourceList) {
            mCategory = mSourceManager.getParser(source.getType()).getCategory();
            if (mCategory != null) {
                mSourceList.add(new Pair<>(source.getTitle(), Integer.toString(source.getType())));
            }
        }
        mCategorySourceSpinner.setAdapter(new CategoryAdapter(getContext(), mSourceList));
        mCategorySourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                initSpinner(mSourceList.get(position).second);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                initSpinner(mSourceList.get(0).second);
            }
        });
    }

    private void initSpinner(String source) {
        initSpinner(Integer.parseInt(source));
    }

    private void initSpinner(int source) {

        mCategory = mSourceManager.getParser(source).getCategory();
        int[] type = new int[]{Category.CATEGORY_SUBJECT, Category.CATEGORY_AREA, Category.CATEGORY_READER,
                Category.CATEGORY_YEAR, Category.CATEGORY_PROGRESS, Category.CATEGORY_ORDER};

        for (int i = 0; i != type.length; ++i) {
            if (mCategory.hasAttribute(type[i])) {
                mCategoryView.get(i).setVisibility(View.VISIBLE);
                if (!mCategory.isComposite()) {
                    mSpinnerList.get(i).setOnItemSelectedListener(this);
                }
                mSpinnerList.get(i).setAdapter(new CategoryAdapter(getContext(), mCategory.getAttrList(type[i])));
            }else{
                mCategoryView.get(i).setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        for (AppCompatSpinner spinner : mSpinnerList) {
            if (position == 0) {
                spinner.setEnabled(true);
            } else if (!parent.equals(spinner)) {
                spinner.setEnabled(false);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @OnClick(R.id.category_action_button)
    void onActionButtonClick() {
        String[] args = new String[mSpinnerList.size()];
        for (int i = 0; i != args.length; ++i) {
            args[i] = getSpinnerValue(mSpinnerList.get(i));
        }
        int source = getActivity().getIntent().getIntExtra(Extra.EXTRA_SOURCE, -1);
        String format = mCategory.getFormat(args);
        HintUtils.showToastLong(getContext(), "点击了按钮");

    }

    private String getSpinnerValue(AppCompatSpinner spinner) {
        if (!spinner.isShown()) {
            return null;
        }
        return ((CategoryAdapter) spinner.getAdapter()).getValue(spinner.getSelectedItemPosition());
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_category;
    }


}
