# ========== JCC (libs/jcc-bate-0.7.3.jar, bare jar without consumer rules) ==========
-keep class taobe.tec.jcc.** { *; }

# ========== Rhino ==========
-keep class org.mozilla.javascript.** { *; }

# ========== Suppress warnings ==========
# Many libraries reference @Nullable etc. which is not on Android
-dontwarn javax.annotation.**
# Rhino references desktop JDK APIs not available on Android
-dontwarn java.beans.**
-dontwarn jdk.dynalink.**
