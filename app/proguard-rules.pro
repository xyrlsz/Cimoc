# ========== JCC (libs/jcc-bate-0.7.3.jar, bare jar without consumer rules) ==========
-keep class taobe.tec.jcc.** { *; }

# ========== Suppress warnings ==========
# Many libraries reference @Nullable etc. which is not on Android
-dontwarn javax.annotation.**
# QuickJS JNI 的 keep 规则已随 :quickjs 模块的 consumer rules 自动应用
