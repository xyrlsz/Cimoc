# ========== Debug info ==========
-keepattributes SourceFile, LineNumberTable

# ========== Fresco ==========
# Fresco 3.7.0 自带 consumer ProGuard 规则，此处仅为显式补充
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip
-keep @com.facebook.common.internal.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.common.internal.DoNotStrip *;
}

# ========== Native methods ==========
-keepclassmembers class * { native <methods>; }

# ========== ObjectBox ==========
# ObjectBox 编译期生成绑定代码直接引用 @Entity 类，R8 可追踪，无需 keep 实体类。
# 仅保留通过反射加载的部分：自定义 PropertyConverter 和 ReLinker。
-keep class * implements io.objectbox.converter.PropertyConverter { *; }
-keep class com.getkeepsafe.relinker.** { *; }

# ========== Gson ==========
# Gson 2.14.0 已自带 consumer 规则处理 @SerializedName 和 TypeToken。
# 数据同步模型未使用 @SerializedName，依赖字段名匹配 JSON key，需防混淆。
-keep class com.xyrlsz.xcimocob.network.sync.DataSyncModels { *; }
-keep class com.xyrlsz.xcimocob.network.sync.DataSyncModels$* { *; }

# ========== Serializable ==========
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}

# ========== JCC (libs/jcc-bate-0.7.3.jar, bare jar without consumer rules) ==========
-keep class taobe.tec.jcc.** { *; }

# ========== android-opencc (JNI library) ==========
-keep class com.xyrlsz.opencc.android.lib.** { *; }

# ========== Rhino ==========
-keep class org.mozilla.javascript.** { *; }

# ========== Suppress warnings ==========
# Many libraries reference @Nullable etc. which is not on Android
-dontwarn javax.annotation.**
# Rhino references desktop JDK APIs not available on Android
-dontwarn java.beans.**
-dontwarn jdk.dynalink.**
