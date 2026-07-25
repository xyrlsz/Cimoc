# ========== Debug info ==========
-keepattributes SourceFile, LineNumberTable

# ========== Fresco ==========
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip
-keep @com.facebook.common.internal.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.common.internal.DoNotStrip *;
}

# ========== Native methods ==========
-keepclassmembers class * { native <methods>; }

# ========== ObjectBox ==========
-keep class io.objectbox.** { *; }
-keep class **_ { *; }
-keep class * implements io.objectbox.converter.PropertyConverter { *; }
-keepclassmembers class * { @io.objectbox.annotation.* *; }

# ========== Gson (R8 full mode) ==========
-keepattributes Signature, RuntimeVisibleAnnotations
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.xyrlsz.xcimocob.network.sync.DataSyncModels { *; }
-keep class com.xyrlsz.xcimocob.network.sync.DataSyncModels$* { *; }

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
