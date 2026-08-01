# ========== JCC (libs/jcc-bate-0.7.3.jar, bare jar without consumer rules) ==========
-keep class taobe.tec.jcc.** { *; }

# ========== Fresco (com.facebook.fresco) ==========

# 【Android 5-7 崩溃修复】解码图片时 NoClassDefFoundError:
# Failed resolution of: Landroid/graphics/ColorSpace
# android.graphics.ColorSpace 是 API 26 才引入的类。R8 内联
# BitmapUtil.decodeDimensionsAndColorSpace 时删掉了 "SDK_INT >= O" 的 API 保护，
# 使返回类型为 ColorSpace 的 ImageMetaData.getColorSpace() 在低版本被无条件调用。
# 参见 https://github.com/facebook/fresco/issues/2638
-keep public class com.facebook.imageutils.** {
    public *;
}

# 【反射实例化保护】ImagePipelineFactory 通过
# Class.forName("com.facebook.animated.webp.WebPImageDecoder")
# + getConstructor(PlatformBitmapFactory, boolean, boolean, boolean) 反射创建 WebP 解码器。
# R8 会把构造器与 decode() 当未使用代码裁剪，导致
# "Failed to instantiate WebP decoder via reflection" (NoSuchMethodException)
-keep class com.facebook.animated.webp.WebPImageDecoder { *; }
-keep class com.facebook.animated.webp.WebPImage { *; }
-keep class com.facebook.animated.webp.WebPFrame { *; }

# 【反射实例化保护】AnimatedFactoryProvider 通过
# Class.forName("com.facebook.fresco.animation.factory.AnimatedFactoryV2Impl")
# 反射创建动画工厂（GIF/WebP 动图播放依赖它），构造器同样会被 R8 裁剪
-keep class com.facebook.fresco.animation.factory.AnimatedFactoryV2Impl { *; }

# 【反射实例化保护】AnimatedFactoryProvider 通过 Class.forName 加载的 vito 性能监听接口，
# R8 会把接口方法当无用代码裁剪 -> 动图性能回调时 NoSuchMethodError
-keep class com.facebook.fresco.vito.core.AnimatedImagePerfLoggingListener { *; }

# ========== androidx (反射目标保护) ==========

# 【反射实例化保护】androidx.fragment 通过 Class.forName 加载 FragmentTransitionSupport
# 执行 Fragment 转场动画；R8 会裁掉其转场方法 -> 转场时 NoSuchMethodError
-keep class androidx.transition.FragmentTransitionSupport { *; }

# ========== Suppress warnings ==========
# Many libraries reference @Nullable etc. which is not on Android
-dontwarn javax.annotation.**
# QuickJS JNI 的 keep 规则已随 :quickjs 模块的 consumer rules 自动应用
