package com.xyrlsz.xcimoc.ui.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
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
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.fresco.ControllerBuilderSupplierFactory;
import com.xyrlsz.xcimoc.fresco.ImagePipelineFactoryBuilder;
import com.xyrlsz.xcimoc.fresco.processor.MangaPostprocessor;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.ui.widget.OnTapGestureListener;
import com.xyrlsz.xcimoc.ui.widget.PhotoDraweeView;
import com.xyrlsz.xcimoc.ui.widget.RetryDraweeView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import butterknife.BindView;
import okhttp3.Headers;

/**
 * Created by Hiroshi on 2016/8/5.
 */
public class ReaderAdapter extends BaseAdapter<ImageUrl> {

    public static final int READER_PAGE = 0;
    public static final int READER_STREAM = 1;

    private static final int TYPE_LOADING = 2016101214;
    private static final int TYPE_IMAGE = 2016101215;
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


    public ReaderAdapter(Context context, List<ImageUrl> list) {
        super(context, list);
    }

    @Override
    public int getItemViewType(int position) {
        return mDataSet.get(position).isLazy() ? TYPE_LOADING : TYPE_IMAGE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        boolean isWhiteBackground = App.getPreferenceManager().getBoolean(PreferenceManager.PREF_READER_WHITE_BACKGROUND, false);
        int resId;
        if (isWhiteBackground) {
            resId = viewType == TYPE_IMAGE ? (reader == READER_PAGE ?
                    R.layout.item_picture_black : R.layout.item_picture_stream_black) : R.layout.item_loading_black;
        } else {
            resId = viewType == TYPE_IMAGE ? (reader == READER_PAGE ?
                    R.layout.item_picture : R.layout.item_picture_stream) : R.layout.item_loading;
        }
        View view = mInflater.inflate(resId, parent, false);
        return new ImageHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final ImageUrl imageUrl = mDataSet.get(position);
        if (imageUrl.isLazy()) {
            if (!imageUrl.isLoading() && mLazyLoadListener != null) {
                imageUrl.setLoading(true);
                mLazyLoadListener.onLoad(imageUrl);
            }
            return;
        }

        final DraweeView draweeView = ((ImageHolder) holder).draweeView;

        Headers currHeaders = imageUrl.getHeaders();
        if (currHeaders != null) {
            Context context = App.getAppContext();
            ImagePipelineFactory mImagePipelineFactory = ImagePipelineFactoryBuilder
                    .build(context, imageUrl.isDownload() ? null : currHeaders, false);
            ImagePipelineFactory mLargeImagePipelineFactory = ImagePipelineFactoryBuilder
                    .build(context, imageUrl.isDownload() ? null : currHeaders, true);
            setControllerSupplier(ControllerBuilderSupplierFactory.get(context, mImagePipelineFactory),
                    ControllerBuilderSupplierFactory.get(context, mLargeImagePipelineFactory));

        }

        PipelineDraweeControllerBuilder builder = isNeedResize(imageUrl) ?
                mLargeControllerSupplier.get() : mControllerSupplier.get();

        switch (reader) {
            case READER_PAGE:
                ((PhotoDraweeView) draweeView).setTapListenerListener(mTapGestureListener);
                ((PhotoDraweeView) draweeView).setAlwaysBlockParent(isBanTurn);
                ((PhotoDraweeView) draweeView).setDoubleTap(isDoubleTap);
                ((PhotoDraweeView) draweeView).setScaleFactor(mScaleFactor);
                ((PhotoDraweeView) draweeView).setScrollMode(isVertical ?
                        PhotoDraweeView.MODE_VERTICAL : PhotoDraweeView.MODE_HORIZONTAL);
                builder.setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                        if (imageInfo != null) {
                            imageUrl.setSuccess(true);
                            ((PhotoDraweeView) draweeView).update(imageUrl.getId());
                        }
                    }
                });
                break;
            case READER_STREAM:
                builder.setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                        if (imageInfo != null) {
                            imageUrl.setSuccess(true);
                            if (isVertical) {
                                draweeView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            } else {
                                draweeView.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
                            }
                            draweeView.setAspectRatio((float) imageInfo.getWidth() / imageInfo.getHeight());
                        }
                    }
                });
                break;
        }

        String[] urls = imageUrl.getUrls();
        ImageRequest[] request = new ImageRequest[urls.length];
        for (int i = 0; i != urls.length; ++i) {
            final String url = urls[i];
            if (url == null) {
                continue;
            }
            ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder
                    .newBuilderWithSource(Uri.parse(url))
                    .setProgressiveRenderingEnabled(true);

            // TODO 切图后可能需要修改图片高度和宽度
            MangaPostprocessor processor = new MangaPostprocessor(imageUrl, isPaging, isPagingReverse, isWhiteEdge);
            imageRequestBuilder.setPostprocessor(processor);
            if (!isCloseAutoResizeImage) {
                ResizeOptions options = isVertical ? new ResizeOptions(App.mWidthPixels, App.mHeightPixels) :
                        new ResizeOptions(App.mHeightPixels, App.mWidthPixels);
                imageRequestBuilder.setResizeOptions(options);
            }
            imageRequestBuilder.setRequestListener(new BaseRequestListener() {
                @Override
                public void onRequestSuccess(ImageRequest request, String requestId, boolean isPrefetch) {
                    imageUrl.setUrl(url);
                }
            });
            request[i] = imageRequestBuilder.build();
        }
        builder.setOldController(draweeView.getController()).setTapToRetryEnabled(true).setRetainImageOnFailure(true);
        draweeView.setController(builder.setFirstAvailableImageRequests(request).build());
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
                    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                        outRect.set(0, 0, 0, 0);
                    }
                };
            case READER_STREAM:
                return new RecyclerView.ItemDecoration() {
                    @Override
                    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
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
            if (mDataSet.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public void update(Long id, String url) {
        for (int i = 0; i < mDataSet.size(); ++i) {
            ImageUrl imageUrl = mDataSet.get(i);
            if (imageUrl.getId().equals(id) && imageUrl.isLoading()) {
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

    public static class ImageHolder extends BaseViewHolder {
        public @BindView(R.id.reader_image_view)
        RetryDraweeView draweeView;

        ImageHolder(View view) {
            super(view);
        }
    }

}
