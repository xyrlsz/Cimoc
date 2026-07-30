package com.xyrlsz.xcimocob.fresco;

import android.content.Context;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilderSupplier;
import com.facebook.imagepipeline.core.ImagePipelineFactory;

import okhttp3.Headers;

/**
 * Created by Hiroshi on 2016/9/5.
 */
public class ControllerBuilderSupplierFactory {

    public static PipelineDraweeControllerBuilderSupplier get(Context context, ImagePipelineFactory factory) {
        return new PipelineDraweeControllerBuilderSupplier(context.getApplicationContext(), factory, null);
    }

    public static PipelineDraweeControllerBuilder get(Context context, Headers header) {
        ImagePipelineFactory factory = ImagePipelineFactoryBuilder.build(context, header, false);
        return new PipelineDraweeControllerBuilderSupplier(context.getApplicationContext(), factory, null).get();
    }

    /**
     * 包装一个 Supplier，使每个 builder 自动带上 callerContext（headers）。
     * 这样 adapter 完全不需要知道 headers 的存在。
     */
    public static Supplier<PipelineDraweeControllerBuilder> wrapWithHeaders(
            Supplier<PipelineDraweeControllerBuilder> delegate, Headers headers) {
        return () -> delegate.get().setCallerContext(headers);
    }

}
