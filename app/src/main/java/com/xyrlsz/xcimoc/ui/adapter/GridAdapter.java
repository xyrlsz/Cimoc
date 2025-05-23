package com.xyrlsz.xcimoc.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.fresco.ControllerBuilderProvider;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.MiniComic;
import com.xyrlsz.xcimoc.utils.FrescoUtils;
import com.xyrlsz.xcimoc.utils.STConvertUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * Created by Hiroshi on 2016/7/1.
 */
public class GridAdapter extends BaseAdapter<Object> {

    public static final int TYPE_GRID = 2016101213;

    private ControllerBuilderProvider mProvider;
    private SourceManager.TitleGetter mTitleGetter;
    private boolean symbol = false;


    public GridAdapter(Context context, List<Object> list) {
        super(context, list);
    }

    @Override
    public int getItemViewType(int position) {
        return TYPE_GRID;
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_grid, parent, false);
        return new GridHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        int viewType = getItemViewType(position);
        switch (viewType) {
            case TYPE_GRID:
            default:
                MiniComic comic = (MiniComic) mDataSet.get(position);
                GridHolder gridHolder = (GridHolder) holder;
                gridHolder.rlItemGrid.getViewTreeObserver().addOnDrawListener(() -> {
                    int width = gridHolder.rlItemGrid.getWidth();
                    ViewGroup.LayoutParams params = gridHolder.rlItemGrid.getLayoutParams();
                    params.height = (int) (width * (4 / 3.0));
                    gridHolder.rlItemGrid.setLayoutParams(params);
                });
                gridHolder.comicTitle.setText(STConvertUtils.convert(comic.getTitle()));
                gridHolder.comicSource.setText(mTitleGetter.getTitle(comic.getSource()));
                if (mProvider != null) {
                    //            ImageRequest request = ImageRequestBuilder
                    //                    .newBuilderWithSource(Uri.parse(comic.getCover()))
                    //                    .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                    //                    .build();
                    ImageRequest request = null;
                    try {
                        if (!App.getManager_wifi().isWifiEnabled() && App.getPreferenceManager().getBoolean(PreferenceManager.PREF_OTHER_CONNECT_ONLY_WIFI, false)) {
                            //                    request = null;
                            if (FrescoUtils.isCached(comic.getCover())) {
                                request = ImageRequestBuilder
                                        .newBuilderWithSource(Uri.fromFile(FrescoUtils.getFileFromDiskCache(comic.getCover())))
                                        .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                                        .build();
                            }
                        } else if (!App.getManager_wifi().isWifiEnabled() && App.getPreferenceManager().getBoolean(PreferenceManager.PREF_OTHER_LOADCOVER_ONLY_WIFI, false)) {
                            //                    request = null;
                            if (FrescoUtils.isCached(comic.getCover())) {
                                request = ImageRequestBuilder
                                        .newBuilderWithSource(Uri.fromFile(FrescoUtils.getFileFromDiskCache(comic.getCover())))
                                        .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                                        .build();
                            }
                        } else {
                            if (FrescoUtils.isCached(comic.getCover())) {
                                request = ImageRequestBuilder
                                        .newBuilderWithSource(Uri.fromFile(FrescoUtils.getFileFromDiskCache(comic.getCover())))
                                        .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                                        .build();
                            } else {
                                request = ImageRequestBuilder
                                        .newBuilderWithSource(Uri.parse(comic.getCover()))
                                        .setResizeOptions(new ResizeOptions(App.mCoverWidthPixels / 3, App.mCoverHeightPixels / 3))
                                        .build();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    DraweeController controller = mProvider.get(comic.getSource())
                            .setOldController(gridHolder.comicImage.getController())
                            .setImageRequest(request)
                            .build();
                    gridHolder.comicImage.setController(controller);
                }
                gridHolder.comicHighlight.setVisibility(symbol && comic.isHighlight() ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void setProvider(ControllerBuilderProvider provider) {
        mProvider = provider;
    }

    public void setTitleGetter(SourceManager.TitleGetter getter) {
        mTitleGetter = getter;
    }

    public void setSymbol(boolean symbol) {
        this.symbol = symbol;
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NotNull Rect outRect, @NotNull View view,
                                       @NotNull RecyclerView parent, @NotNull RecyclerView.State state) {
                int offset = parent.getWidth() / 90;
                outRect.set(offset, 0, offset, (int) (2.8 * offset));
            }
        };
    }

    public void removeItemById(long id) {
        for (Object O_comic : mDataSet) {
            MiniComic comic = (MiniComic) O_comic;
            if (id == comic.getId()) {
                remove(comic);
                break;
            }
        }
    }

    public int findFirstNotHighlight() {
        int count = 0;
        if (symbol) {
            for (Object O_comic : mDataSet) {
                MiniComic comic = (MiniComic) O_comic;
                if (!comic.isHighlight()) {
                    break;
                }
                ++count;
            }
        }
        return count;
    }

    public void cancelAllHighlight() {
        int count = 0;
        for (Object O_comic : mDataSet) {
            MiniComic comic = (MiniComic) O_comic;
            if (!comic.isHighlight()) {
                break;
            }
            ++count;
            comic.setHighlight(false);
        }
        notifyItemRangeChanged(0, count);
    }

    public void moveItemTop(MiniComic comic) {
        if (remove(comic)) {
            add(findFirstNotHighlight(), comic);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void filterByKeyword(String keyword,List<Object>original) {
        List<Object> temp = new ArrayList<>();
        for (Object O_comic : mDataSet) {
            MiniComic comic = (MiniComic) O_comic;
            String title = STConvertUtils.T2S(comic.getTitle()).toUpperCase();
            if (title.contains(STConvertUtils.T2S(keyword.toUpperCase()))) {
                temp.add(comic);
            }
            original.add(comic);
        }
        mDataSet.clear();
        mDataSet.addAll(temp);
        notifyDataSetChanged();
    }

    public void cancelFilter(List<Object> original) {
        if (original == null || original.isEmpty()) {
            return; // 如果 original 为 null 或空，直接返回，不修改 mDataSet
        }

        if (mDataSet == null) {
            mDataSet = new ArrayList<>(); // 防止 mDataSet 为 null（可选）
        }

        mDataSet.clear();          // 清空旧数据
        mDataSet.addAll(original); // 添加新数据
        notifyDataSetChanged();    // 通知 Adapter 刷新
    }

    static class GridHolder extends BaseViewHolder {
        @BindView(R.id.item_grid_image)
        SimpleDraweeView comicImage;
        @BindView(R.id.item_grid_title)
        TextView comicTitle;
        @BindView(R.id.item_grid_subtitle)
        TextView comicSource;
        @BindView(R.id.item_grid_symbol)
        View comicHighlight;
        @BindView(R.id.rl_item_grid)
        RelativeLayout rlItemGrid;

        GridHolder(View view) {
            super(view);
        }
    }
}
