package com.xyrlsz.xcimocob.fresco;

import android.content.Context;
import android.util.SparseArray;

import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilderSupplier;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.xyrlsz.xcimocob.manager.SourceManager;

import okhttp3.Headers;

/**
 * Created by Hiroshi on 2016/9/5.
 */
public class ControllerBuilderProvider {

    private Context mContext;
    private SparseArray<PipelineDraweeControllerBuilderSupplier> mSupplierArray;
    private SparseArray<ImagePipeline> mPipelineArray;
    private SparseArray<Headers> mHeaderArray;
    private SourceManager.HeaderGetter mHeaderGetter;
    private boolean mCover;

    public ControllerBuilderProvider(Context context, SourceManager.HeaderGetter getter, boolean cover) {
        mSupplierArray = new SparseArray<>();
        mPipelineArray = new SparseArray<>();
        mHeaderArray = new SparseArray<>();
        mContext = context;
        mHeaderGetter = getter;
        mCover = cover;
    }

    public PipelineDraweeControllerBuilder get(int type) {
        PipelineDraweeControllerBuilderSupplier supplier = mSupplierArray.get(type);
        if (supplier == null) {
            Headers headers = type < 0 ? null : mHeaderGetter.getHeader(type);
            mHeaderArray.put(type, headers);
            ImagePipelineFactory factory = ImagePipelineFactoryBuilder
                    .build(mContext, headers, mCover);
            supplier = ControllerBuilderSupplierFactory.get(mContext, factory);
            mSupplierArray.put(type, supplier);
            mPipelineArray.put(type, factory.getImagePipeline());
        }
        PipelineDraweeControllerBuilder builder = supplier.get();
        Headers headers = mHeaderArray.get(type);
        if (headers != null) {
            builder.setCallerContext(headers);
        }
        return builder;
    }

    public void pause() {
        for (int i = 0; i != mPipelineArray.size(); ++i) {
            mPipelineArray.valueAt(i).pause();
        }
    }

    public void resume() {
        for (int i = 0; i != mPipelineArray.size(); ++i) {
            mPipelineArray.valueAt(i).resume();
        }
    }

    public void clear() {
        for (int i = 0; i != mPipelineArray.size(); ++i) {
            mPipelineArray.valueAt(i).clearMemoryCaches();
        }
    }

}
