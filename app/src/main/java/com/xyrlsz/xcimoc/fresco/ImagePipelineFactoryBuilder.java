package com.xyrlsz.xcimoc.fresco;

import android.content.Context;
import android.graphics.Bitmap;

import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.xyrlsz.xcimoc.App;

import java.util.Objects;

import okhttp3.Headers;

/**
 * Created by Hiroshi on 2016/7/8.
 */
public class ImagePipelineFactoryBuilder {

    public static ImagePipelineFactory build(Context context, Headers header, boolean down) {
        ImagePipelineConfig.Builder builder =
                OkHttpImagePipelineConfigFactory.newBuilder(context.getApplicationContext(), Objects.requireNonNull(App.getHttpClient()))
                        .setDownsampleEnabled(down)
                        .setBitmapsConfig(down ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888);
        if (header != null) {
            builder.setNetworkFetcher(new OkHttpNetworkFetcher(App.getHttpClient(), header));
        }
        return new ImagePipelineFactory(builder.build());
    }

}
