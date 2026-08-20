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

# 【反射实例化保护】ImagePipelineFactory 通过
# Class.forName("com.facebook.animated.gif.GifImageDecoder") 反射创建 GIF 解码器。
-keep class com.facebook.animated.gif.GifImageDecoder { *; }
-keep class com.facebook.animated.gif.GifImage { *; }
-keep class com.facebook.animated.gif.GifFrame { *; }

# 【反射实例化保护】PoolFactory 通过 Class.forName 加载多种 MemoryChunkPool
# （Ashmem/Buffer/Native），R8 默认认为未使用会裁掉构造器 -> NoSuchMethodException
-keep class com.facebook.imagepipeline.memory.AshmemMemoryChunkPool { *; }
-keep class com.facebook.imagepipeline.memory.BufferMemoryChunkPool { *; }
-keep class com.facebook.imagepipeline.memory.NativeMemoryChunkPool { *; }

# 【反射实例化保护】NativeImageTranscoderFactory 反射加载 NativeJpegTranscoderFactory
-keep class com.facebook.imagepipeline.nativecode.NativeJpegTranscoderFactory { *; }

# 【反射实例化保护】BasePostprocessor 通过 Class.forName 加载 native Bitmaps 工具
-keep class com.facebook.imagepipeline.nativecode.Bitmaps { *; }

# ========== AgentWeb (com.just.agentweb) ==========

# 【反射实例化保护】AbsAgentWebUIController 反射加载 Material 组件
# 用于 WebView 内错误提示/底部弹窗；若项目集成了 Material 则必须保留，
# 没集成时 AbsAgentWebUIController 内部会 try/catch 忽略 ClassNotFoundException。
-keep class com.google.android.material.bottomsheet.BottomSheetDialog { *; }
-keep class com.google.android.material.snackbar.Snackbar { *; }

# 【反射实例化保护】AgentWebUtils 反射加载 WebView 文件选择器模块
-keep class com.just.agentweb.filechooser.FileChooser { *; }

# 【反射实例化保护】DefaultWebClient 检测并打开支付宝 SDK 发起支付（可选依赖，未集
# 成时 try/catch 兜底 ClassNotFoundException）。
-keep class com.alipay.sdk.app.PayTask { *; }

# ========== ObjectBox (io.objectbox) ==========

# 【反射实例化保护】NativeLibraryLoader 若检测到 ReLinker 则用其加载 native 库，
# 避免 Android 5.x/6.x 部分机型出现 libobjectbox.so 找不到 UnsatisfiedLinkError。
-keep class com.getkeepsafe.relinker.ReLinker { *; }

# 【反射实例化保护】objectbox-sync 的 Platform 在 Android 下通过反射加载
# AndroidPlatform，使用 Context 注册网络变化监听等
-keep class io.objectbox.android.internal.AndroidPlatform { *; }

# 【通用规则】保留所有 ObjectBox 实体默认构造器与 @Id 属性（@Entity 类自带生成的
# MyObjectBox_ 等通常由 objectbox-gradle-plugin 的 consumer rules 处理，这里作为兜底）
-keep class ** implements io.objectbox.annotation.Entity { *; }
-keep class **_ { *; }

# ========== Serializable 通用 ==========

# 实现 java.io.Serializable 的类（如 ClickEvents.JoyLocks、DataSyncException、SyncType、
# Logger.Level 等）：serialVersionUID / writeObject / readObject 未被直接引用时 R8
# 会裁剪或混淆字段名，导致反序列化 InvalidClassException。
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========== androidx (反射目标保护) ==========

# 【反射实例化保护】androidx.fragment 通过 Class.forName 加载 FragmentTransitionSupport
# 执行 Fragment 转场动画；R8 会裁掉其转场方法 -> 转场时 NoSuchMethodError
-keep class androidx.transition.FragmentTransitionSupport { *; }

# ========== 应用内反射 (release R8 崩溃保护) ==========

# 【反射字段保护】RecyclerViewPager.onRestoreInstanceState 通过反射读写
# SavedState 内的 mLayoutState 与其 mAnchorOffset / mAnchorPosition 字段，
# 修复横竖屏切换/后台回收时恢复第一页偏移偏差；R8 重命名字段会
# 导致 NoSuchFieldException 走 catch 分支 -> 滚动锚点错乱。
-keep class androidx.recyclerview.widget.RecyclerView$SavedState {
    *;
}
# SavedState.mLayoutState 值类型（package-private LayoutState）也不能被重命名
-keep class androidx.recyclerview.widget.LinearLayoutManager$LayoutState {
    int mAnchorOffset;
    int mAnchorPosition;
}
# 兜底：其它 LayoutManager 的 LayoutState 内部类如果有同名字段也保持
-keep class androidx.recyclerview.widget.*$LayoutState {
    ** mAnchor*;
}

# 【反射字段保护】MultiAdpaterDialogFragment.isCloseDialog() 通过反射修改
# Dialog 父类 (AlertDialog.Builder 实际返回的 AlertDialog / AppCompatDialog 链)
# 里的 mShowing 字段，阻止按钮点击时无条件关闭对话框；字段被 R8 重命名
# 会让反射 catch 静默失败 -> 对话框仍然被关闭。
-keepnames class * extends android.app.Dialog {
    boolean mShowing;
}
-keep class android.app.Dialog {
    boolean mShowing;
}
# AppCompatDialog 也有 mShowing（从 Dialog 继承），但 X AppCompat 版本若额外包装也需要保留
-keep class androidx.appcompat.app.AppCompatDialog {
    boolean mShowing;
}

-keep class com.xyrlsz.xcimocob.utils.Logger { *; }
# ========== Suppress warnings ==========
# Many libraries reference @Nullable etc. which is not on Android
-dontwarn javax.annotation.**
# QuickJS JNI 的 keep 规则已随 :quickjs 模块的 consumer rules 自动应用
