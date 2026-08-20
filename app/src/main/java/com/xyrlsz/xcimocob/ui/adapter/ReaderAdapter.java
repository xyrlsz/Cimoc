package com.xyrlsz.xcimocob.ui.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilderSupplier;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.view.DraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.fresco.ControllerBuilderSupplierFactory;
import com.xyrlsz.xcimocob.fresco.ImagePipelineFactoryBuilder;
import com.xyrlsz.xcimocob.fresco.processor.MangaPostprocessor;
import com.xyrlsz.xcimocob.manager.PreferenceManager;
import com.xyrlsz.xcimocob.model.ImageUrl;
import com.xyrlsz.xcimocob.ui.widget.OnTapGestureListener;
import com.xyrlsz.xcimocob.ui.widget.PhotoDraweeView;
import com.xyrlsz.xcimocob.ui.widget.RetryDraweeView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;

/**
 * Created by Hiroshi on 2016/8/5.
 */
public class ReaderAdapter extends BaseAdapter<ImageUrl> {

    public static final int READER_PAGE = 0;
    public static final int READER_STREAM = 1;

    private static final int TYPE_LOADING = 2016101214;
    private static final int TYPE_IMAGE = 2016101215;
    private static final int TYPE_IMAGE_PAGE = 0;
    private static final int TYPE_IMAGE_STREAM = 1;
    private static @ReaderMode int reader;
    private PipelineDraweeControllerBuilderSupplier mControllerSupplier;
    private PipelineDraweeControllerBuilderSupplier mLargeControllerSupplier;
    private OnTapGestureListener mTapGestureListener;
    private OnLazyLoadListener mLazyLoadListener;
    private boolean isVertical; // 开页方向
    private boolean isPaging;
    private boolean isPagingReverse;
    private boolean isWhiteEdge;
    private boolean isBanTurn;
    private boolean isDoubleTap;
    private boolean isCloseAutoResizeImage;
    private float mScaleFactor;

    // 缓存 ImagePipelineFactory/Supplier 对（按 headers 缓存），减少重复创建开销
    private static final int MAX_SUPPLIER_CACHE = 32;
    private final LinkedHashMap<String, SupplierPair> mSupplierCache =
            new LinkedHashMap<String, SupplierPair>(MAX_SUPPLIER_CACHE + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Entry<String, SupplierPair> eldest) {
                    return size() > MAX_SUPPLIER_CACHE;
                }
            };

    /** 预计算的 ResizeOptions，减少重复创建 */
    private ResizeOptions mCachedVerticalResize;
    private ResizeOptions mCachedHorizontalResize;

    /** 页面模式图片加载成功监听器（复用实例） */
    private final BaseControllerListener<ImageInfo> mPageSuccessListener =
            new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    // onBindViewHolder 会通过 tag 传递 ImageUrl
                }

                @Override
                public void onFailure(String id, Throwable throwable) {
                    // onBindViewHolder 会通过 tag 传递 ImageUrl
                }
            };

    /** 流模式图片加载成功监听器（复用实例） */
    private final BaseControllerListener<ImageInfo> mStreamSuccessListener =
            new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    // onBindViewHolder 会通过 tag 传递 ImageUrl
                }

                @Override
                public void onFailure(String id, Throwable throwable) {
                    // onBindViewHolder 会通过 tag 传递 ImageUrl
                }
            };

    /** 请求成功监听器（复用实例） */
    private final BaseRequestListener mSharedRequestListener = new BaseRequestListener() {
        @Override
        public void onRequestSuccess(@NonNull ImageRequest request,
                                     @NonNull String requestId,
                                     boolean isPrefetch) {
            // URL 会在构建时设置到 ImageRequest 的 tag，或者通过 ImageUrl 获取
        }
    };

    private static class SupplierPair {
        final PipelineDraweeControllerBuilderSupplier normal;
        final PipelineDraweeControllerBuilderSupplier large;
        SupplierPair(PipelineDraweeControllerBuilderSupplier normal,
                     PipelineDraweeControllerBuilderSupplier large) {
            this.normal = normal;
            this.large = large;
        }
    }


    public ReaderAdapter(Context context, List<ImageUrl> list) {
        super(context, list);
    }

    @Override
    public int getItemViewType(int position) {
//        return mDataSet.get(position).isLazy() ? TYPE_LOADING : TYPE_IMAGE;
        if (mDataSet.get(position).isLazy()) return TYPE_LOADING;

        return reader == READER_PAGE ? TYPE_IMAGE_PAGE : TYPE_IMAGE_STREAM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        boolean isWhiteBackground = App.getPreferenceManager()
                .getBoolean(PreferenceManager.PREF_READER_WHITE_BACKGROUND, true);

        int resId;

        if (viewType == TYPE_LOADING) {
            resId = isWhiteBackground ? R.layout.item_loading_black : R.layout.item_loading;
            View view = mInflater.inflate(resId, parent, false);
            return new LoadingHolder(view);
        }

        if (viewType == TYPE_IMAGE_PAGE) {
            resId = isWhiteBackground ? R.layout.item_picture_black : R.layout.item_picture;
            View view = mInflater.inflate(resId, parent, false);
            return new PageHolder(view); // 👈 用 PhotoDraweeView
        }

        // TYPE_IMAGE_STREAM
        resId = isWhiteBackground ? R.layout.item_picture_stream_black : R.layout.item_picture_stream;
        View view = mInflater.inflate(resId, parent, false);
        return new StreamHolder(view); // 👈 用 RetryDraweeView
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final ImageUrl imageUrl = mDataSet.get(position);

        // 处理懒加载占位
        if (imageUrl.isLazy()) {
            if (!imageUrl.isLoading() && mLazyLoadListener != null) {
                imageUrl.setLoading(true);
                mLazyLoadListener.onLoad(imageUrl);
            }
            return;
        }

        // 根据 holder 类型获取 draweeView
        DraweeView draweeView;
        final boolean isPageMode;

        if (holder instanceof PageHolder) {
            PageHolder pageHolder = (PageHolder) holder;
            PhotoDraweeView photoView = pageHolder.draweeView;
            draweeView = photoView;
            isPageMode = true;

            // Page 模式特有配置（仅在首次绑定时设置）
            if (!pageHolder.mConfigured) {
                pageHolder.mConfigured = true;
                photoView.setTapListenerListener(mTapGestureListener);
                photoView.setAlwaysBlockParent(isBanTurn);
                photoView.setDoubleTap(isDoubleTap);
                photoView.setScaleFactor(mScaleFactor);
                photoView.setScrollMode(isVertical ?
                        PhotoDraweeView.MODE_VERTICAL :
                        PhotoDraweeView.MODE_HORIZONTAL);
            }

        } else if (holder instanceof StreamHolder) {
            draweeView = ((StreamHolder) holder).draweeView;
            isPageMode = false;

        } else {
            return;
        }

        // 设置 Headers
        Headers currHeaders = imageUrl.getHeaders();

        if (currHeaders != null) {
            Context context = App.getAppContext();
            // 缓存 ImagePipelineFactory/Supplier 对（LRU 淘汰）
            String cacheKey = imageUrl.isDownload() ? "" : currHeaders.toString();
            SupplierPair pair = mSupplierCache.get(cacheKey);
            if (pair == null) {
                ImagePipelineFactory normalFactory = ImagePipelineFactoryBuilder
                        .build(context, imageUrl.isDownload() ? null : currHeaders, false);
                ImagePipelineFactory largeFactory = ImagePipelineFactoryBuilder
                        .build(context, imageUrl.isDownload() ? null : currHeaders, true);
                pair = new SupplierPair(
                        ControllerBuilderSupplierFactory.get(context, normalFactory),
                        ControllerBuilderSupplierFactory.get(context, largeFactory)
                );
                mSupplierCache.put(cacheKey, pair);
            }
            mControllerSupplier = pair.normal;
            mLargeControllerSupplier = pair.large;
        }

        // 选择 ControllerBuilder
        final boolean needResize = isNeedResize(imageUrl);
        PipelineDraweeControllerBuilder builder = needResize
                ? mLargeControllerSupplier.get()
                : mControllerSupplier.get();

        // 构建 ImageRequest 数组（使用预计算的 ResizeOptions）
        String[] urls = imageUrl.getUrls().toArray(new String[0]);
        ImageRequest[] requests = new ImageRequest[urls.length];

        // 预计算 ResizeOptions（仅首次或方向改变时）
        if (mCachedVerticalResize == null) {
            mCachedVerticalResize = new ResizeOptions(App.mWidthPixels, App.mHeightPixels);
            mCachedHorizontalResize = new ResizeOptions(App.mHeightPixels, App.mWidthPixels);
        }
        final ResizeOptions resizeOptions = isVertical
                ? mCachedVerticalResize : mCachedHorizontalResize;

        // 预创建 MangaPostprocessor（所有 URL 共享同一个处理器）
        final MangaPostprocessor sharedProcessor = new MangaPostprocessor(
                imageUrl, isPaging, isPagingReverse, isWhiteEdge);

        for (int i = 0; i < urls.length; i++) {
            String url = urls[i];
            if (url == null) continue;

            ImageRequestBuilder reqBuilder = ImageRequestBuilder
                    .newBuilderWithSource(Uri.parse(url))
                    .setProgressiveRenderingEnabled(true);

            // 使用共享的 postprocessor（状态由 ImageUrl 管理）
            reqBuilder.setPostprocessor(sharedProcessor);

            if (!isCloseAutoResizeImage) {
                reqBuilder.setResizeOptions(resizeOptions);
            }

            // 使用共享的 RequestListener（URL 更新通过 ImageUrl 完成）
            reqBuilder.setRequestListener(mSharedRequestListener);

            requests[i] = reqBuilder.build();
        }

        // 绑定 Controller（使用 ControllerListener 统一处理成功/失败）
        final int[] consumed = {0};
        final long imageId = imageUrl.getId();

        if (isPageMode) {
            final PhotoDraweeView photoView = (PhotoDraweeView) draweeView;
            builder.setControllerListener(new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    if (imageInfo != null) {
                        imageUrl.setSuccess(true);
                        photoView.update(imageId);
                    }
                }

                @Override
                public void onFailure(String id, Throwable throwable) {
                    imageUrl.setSuccess(false);
                    if (consumed[0] == 0) {
                        consumed[0] = 1;
                        android.util.Log.e("ReaderAdapter",
                                "图片加载失败: " + imageUrl.getUrl(), throwable);
                    }
                }
            });
        } else {
            final DraweeView finalDraweeView = draweeView;
            builder.setControllerListener(new BaseControllerListener<ImageInfo>() {
                @Override
                public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    if (imageInfo != null) {
                        imageUrl.setSuccess(true);

                        if (isVertical) {
                            finalDraweeView.getLayoutParams().height =
                                    ViewGroup.LayoutParams.WRAP_CONTENT;
                        } else {
                            finalDraweeView.getLayoutParams().width =
                                    ViewGroup.LayoutParams.WRAP_CONTENT;
                        }

                        finalDraweeView.setAspectRatio(
                                (float) imageInfo.getWidth() / imageInfo.getHeight()
                        );
                    }
                }

                @Override
                public void onFailure(String id, Throwable throwable) {
                    imageUrl.setSuccess(false);
                    if (consumed[0] == 0) {
                        consumed[0] = 1;
                        android.util.Log.e("ReaderAdapter",
                                "图片加载失败: " + imageUrl.getUrl(), throwable);
                    }
                }
            });
        }

        builder.setOldController(draweeView.getController())
                .setTapToRetryEnabled(true)
                .setRetainImageOnFailure(true);

        if (currHeaders != null) {
            builder.setCallerContext(currHeaders);
        }

        draweeView.setController(
                builder.setFirstAvailableImageRequests(requests).build()
        );
    }

    public void setControllerSupplier(PipelineDraweeControllerBuilderSupplier normal,
                                      PipelineDraweeControllerBuilderSupplier large) {
        mControllerSupplier = normal;
        mLargeControllerSupplier = large;
    }

    public void setTapGestureListener(OnTapGestureListener listener) {
        mTapGestureListener = listener;
    }

    public void setLazyLoadListener(OnLazyLoadListener listener) {
        mLazyLoadListener = listener;
    }

    public void setScaleFactor(float factor) {
        mScaleFactor = factor;
    }

    public void setDoubleTap(boolean enable) {
        isDoubleTap = enable;
    }

    public void setBanTurn(boolean block) {
        isBanTurn = block;
    }

    public void setVertical(boolean vertical) {
        isVertical = vertical;
    }

    public void setPaging(boolean paging) {
        isPaging = paging;
    }

    public void setPagingReverse(boolean pagingReverse) {
        isPagingReverse = pagingReverse;
    }

    public void setCloseAutoResizeImage(boolean closeAutoResizeImage) {
        isCloseAutoResizeImage = closeAutoResizeImage;
    }

    public void setWhiteEdge(boolean whiteEdge) {
        isWhiteEdge = whiteEdge;
    }

    public void setReaderMode(@ReaderMode int reader) {
        ReaderAdapter.reader = reader;
    }

    private boolean isNeedResize(ImageUrl imageUrl) {
        // 长图例如条漫不 resize
        return (imageUrl.getWidth() * 2) > imageUrl.getHeight() && imageUrl.getSize() > App.mLargePixels;
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        switch (reader) {
            default:
            case READER_PAGE:
                return new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                        outRect.set(0, 0, 0, 0);
                    }
                };
            case READER_STREAM:
                return new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                        if (isVertical) {
                            outRect.set(0, 10, 0, 10);
                        } else {
                            outRect.set(10, 0, 10, 0);
                        }
                    }
                };
        }
    }

    /**
     * 假设一定找得到
     */
    public int getPositionByNum(int current, int num, boolean reverse) {
        try {
            while (mDataSet.get(current).getNum() < num) {
                current = reverse ? current - 1 : current + 2;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return current;
        }
    }

    public int getPositionById(Long id) {
        int size = mDataSet.size();
        for (int i = 0; i < size; ++i) {
            if (mDataSet.get(i).getId() == (id)) {
                return i;
            }
        }
        return -1;
    }

    public void update(Long id, String url) {
        for (int i = 0; i < mDataSet.size(); ++i) {
            ImageUrl imageUrl = mDataSet.get(i);
            if (imageUrl.getId() == (id) && imageUrl.isLoading()) {
                if (url == null) {
                    imageUrl.setLoading(false);
                    return;
                }
                imageUrl.setUrl(url);
                imageUrl.setLoading(false);
                imageUrl.setLazy(false);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @IntDef({READER_PAGE, READER_STREAM})
    @Retention(RetentionPolicy.SOURCE)
    @interface ReaderMode {
    }

    public interface OnLazyLoadListener {
        void onLoad(ImageUrl imageUrl);
    }


    // 👇 Page模式（支持缩放）
    public static class PageHolder extends RecyclerView.ViewHolder {
        public PhotoDraweeView draweeView;
        /** 是否已完成首次配置（避免重复设置） */
        boolean mConfigured = false;

        public PageHolder(View itemView) {
            super(itemView);
            draweeView = itemView.findViewById(R.id.reader_image_view);
        }
    }

    // 👇 Stream模式（普通图）
    public static class StreamHolder extends RecyclerView.ViewHolder {
        public RetryDraweeView draweeView;

        public StreamHolder(View itemView) {
            super(itemView);
            draweeView = itemView.findViewById(R.id.reader_image_view);
        }
    }

    // 👇 Loading
    public static class LoadingHolder extends RecyclerView.ViewHolder {
        public LoadingHolder(View itemView) {
            super(itemView);
        }
    }
}
