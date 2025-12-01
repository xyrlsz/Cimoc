package com.xyrlsz.xcimoc.ui.fragment;

import android.annotation.SuppressLint;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;

import androidx.appcompat.widget.AppCompatSpinner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.core.Manga;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.MiniComic;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.Category;
import com.xyrlsz.xcimoc.parser.Parser;
import com.xyrlsz.xcimoc.presenter.BasePresenter;
import com.xyrlsz.xcimoc.ui.adapter.CategoryAdapter;
import com.xyrlsz.xcimoc.ui.adapter.CategoryGridAdapter;
import com.xyrlsz.xcimoc.ui.view.CategoryView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;


public class CategoryFragment extends BaseFragment implements CategoryView, AdapterView.OnItemSelectedListener {
    private static final int STATE_NULL = 0;
    private static final int STATE_DOING = 1;
    private static final int STATE_DONE = 3;
    private final List<MiniComic> mComicList = new ArrayList<>();
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
    @BindView(R.id.recycler_view_content)
    RecyclerView mRecyclerView;
    CategoryGridAdapter categoryGridAdapter;
    int mSource;
    private Category mCategory;
    private SourceManager mSourceManager;
    private LinkedList<Pair<String, String>> mSourceList;
    private CompositeSubscription mCompositeSubscription;

    @Override
    protected BasePresenter initPresenter() {
        return super.initPresenter();
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
        mSource = Integer.parseInt(mSourceList.get(0).second);
        mCategorySourceSpinner.setAdapter(new CategoryAdapter(getContext(), mSourceList));
        mCategorySourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSource = Integer.parseInt(mSourceList.get(position).second);
                initSpinner(mSourceList.get(position).second);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSource = Integer.parseInt(mSourceList.get(0).second);
                initSpinner(mSourceList.get(0).second);
            }
        });
        mCompositeSubscription = new CompositeSubscription();
    }

    @Override
    protected void initView() {
        setHasOptionsMenu(true);

        categoryGridAdapter = new CategoryGridAdapter(getContext(), mComicList);

        mRecyclerView.setRecycledViewPool(getAppInstance().getGridRecycledPool());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setLayoutManager(initLayoutManager());


        initSpinner(mSourceList.get(0).second);

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
        categoryGridAdapter.setOnComicClickListener(comic -> {
            // TODO: 打开详情页
        });
        mRecyclerView.setAdapter(categoryGridAdapter);
    }

    protected RecyclerView.LayoutManager initLayoutManager() {
        int spanCount = 3;
        if (App.mHeightPixels * 9 < App.mWidthPixels * 16) {
            spanCount = 4;
        }
        GridLayoutManager manager = new GridLayoutManager(getActivity(), spanCount);
        manager.setRecycleChildrenOnDetach(true);
        return manager;
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
            } else {
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

        String format = mCategory.getFormat(args);
        State state = new State();
        state.source = mSource;
        state.page = 0;
        state.state = STATE_NULL;
        loadCategory(state, format);
    }

    public void loadCategory(State state, String format) {
        if (state.state == STATE_NULL) {
            Parser parser = mSourceManager.getParser(state.source);
            state.state = STATE_DOING;
            mCompositeSubscription.add(Manga.getCategoryComic(parser, format, ++state.page)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<List<Comic>>() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void call(List<Comic> list) {
                            if (state.page == 1) {
                                mComicList.clear();
                            }
                            for (Comic comic : list) {
                                mComicList.add(new MiniComic(comic));
                            }
                            App.runOnMainThread(() -> categoryGridAdapter.notifyDataSetChanged());
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            throwable.printStackTrace();
                            if (state.page == 1) {

                            }
                        }
                    }));
        }
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

    private static class State {
        int source;
        int page;
        int state;
    }


}
