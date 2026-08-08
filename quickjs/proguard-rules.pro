# ========== QuickJS (JNI) ==========
# JNI_OnLoad 通过 RegisterNatives 按类名/方法名注册，禁止混淆、裁剪。
# 该文件作为 consumer rules 随 AAR 发布，自动应用到宿主 app 的 R8 流程。
-keep class com.xyrlsz.quickjs.QuickJSEngine {
    native <methods>;
    public static <methods>;
}
-keepclassmembers class com.xyrlsz.quickjs.QuickJSEngine {
    private static native <methods>;
    # JS -> Java 宿主回调由 C 层 GetStaticMethodID 按名查找，禁止重命名/裁剪
    public static java.lang.String onHostCall(java.lang.String, java.lang.String);
}
-keep interface com.xyrlsz.quickjs.QuickJSEngine$HostBridge {
    <methods>;
}
