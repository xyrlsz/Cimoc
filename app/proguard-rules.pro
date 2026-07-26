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
#-keep class io.objectbox.** { *; }
-keep class **_ { *; }
-keep class * implements io.objectbox.converter.PropertyConverter { *; }
-keepclassmembers class * { @io.objectbox.annotation.* *; }
-keep class com.getkeepsafe.relinker.** { *; }

# ========== Gson (R8 full mode) ==========
# Gson 2.14.0 已自带 gson.pro consumer 规则，自动处理 TypeToken 等
# 仅保留自定义数据类的规则
-keep class com.xyrlsz.xcimocob.network.sync.DataSyncModels { *; }
-keep class com.xyrlsz.xcimocob.network.sync.DataSyncModels$* { *; }

# ========== Parcelable (R8 full mode) ==========
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
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
