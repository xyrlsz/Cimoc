# ========== QuickJS (JNI) ==========
# JNI_OnLoad 通过 RegisterNatives 按类名/方法名注册，禁止混淆、裁剪。
# 该文件作为 consumer rules 随 AAR 发布，自动应用到宿主 app 的 R8 流程。
-keep class com.xyrlsz.quickjs.QuickJSEngine {
    native <methods>;
}
-keepclassmembers class com.xyrlsz.quickjs.QuickJSEngine {
    private static native <methods>;
}

# ========== Rhino 兼容 API ==========
# 公开 API 层保持类名与方法名，便于宿主直接引用（无需反射）。
-keep public class com.xyrlsz.quickjs.Context {
    public *;
}
-keep public class com.xyrlsz.quickjs.ScriptableObject {
    public *;
}
-keep public interface com.xyrlsz.quickjs.Scriptable {
    public *;
}
-keep public interface com.xyrlsz.quickjs.Function {
    public *;
}
-keep public class com.xyrlsz.quickjs.BaseFunction {
    public *;
}
-keep public class com.xyrlsz.quickjs.RhinoException {
    public *;
}
-keep public class com.xyrlsz.quickjs.JavaScriptException {
    public *;
}
-keep public class com.xyrlsz.quickjs.EvaluatorException {
    public *;
}
-keep public class com.xyrlsz.quickjs.Undefined {
    public *;
}
