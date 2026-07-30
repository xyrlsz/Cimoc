package com.xyrlsz.xcimocob.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.ULiteralExpression;
import org.jetbrains.uast.UReferenceExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 检测反射 API 的使用场景，提醒开发者添加 R8 Keep 规则。
 * <p>
 * R8 无法追踪通过反射（如 Class.forName、Method.invoke、Field.set 等）动态调用的代码，
 * 这些代码可能在 R8 优化/混淆时被移除，导致运行时崩溃。
 */
public class ReflectionDetector extends Detector implements Detector.UastScanner {

    // ======================== 反射 API 调用 ========================

    private static final String REFLECTION_API_DESCRIPTION =
            "R8 的静态分析无法追踪反射调用的类、方法或字段，运行时可能抛出 " +
            "ClassNotFoundException 或 NoSuchMethodException。";

    private static final String REFLECTION_API_MESSAGE =
            "使用了反射 API「`%s`」—— 被反射调用的目标类/方法/字段" +
            "需要添加 R8 Keep 规则";

    private static final String REFLECTION_API_DETAIL =
            "通过反射（`Class.forName()`、`Method.invoke()`、`Field.set/get()`、" +
            "`Constructor.newInstance()` 等）动态访问的代码，R8 无法在编译时预知，" +
            "可能被错误地视为无用代码并移除。\n\n" +
            "建议添加 Keep 规则：\n" +
            "```\n" +
            "-keep class 被反射的类 { *; }\n" +
            "```\n" +
            "或者更精确地只保留被反射调用的特定成员：\n" +
            "```\n" +
            "-keepclassmembers class 被反射的类 {\n" +
            "    被反射的方法或字段;\n" +
            "}\n" +
            "```\n\n" +
            "如果反射涉及内部类（如 `OuterClass$InnerClass`），还需要保留类结构属性：\n" +
            "```\n" +
            "-keepattributes InnerClasses, EnclosingMethod\n" +
            "```";

    public static final Issue ISSUE_REFLECTION_API = Issue.create(
            "R8Reflection",
            "使用了反射 API，可能需要添加 R8 Keep 规则",
            REFLECTION_API_DETAIL,
            Category.CORRECTNESS, 8, Severity.WARNING,
            new Implementation(ReflectionDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    // ======================== 动态代理 ========================

    private static final String DYNAMIC_PROXY_DESCRIPTION =
            "R8 无法识别动态创建的代理类，可能导致运行时 ClassNotFoundException。";

    private static final String DYNAMIC_PROXY_MESSAGE =
            "使用了动态代理（`Proxy.newProxyInstance`）—— 代理接口 `%s` 需要添加 Keep 规则";

    private static final String DYNAMIC_PROXY_DETAIL =
            "`java.lang.reflect.Proxy.newProxyInstance()` 在运行时动态创建代理类，" +
            "R8 的静态分析无法预知这些类。\n\n" +
            "建议添加 Keep 规则：\n" +
            "```\n" +
            "-keep class 被代理的接口 { *; }\n" +
            "```";

    public static final Issue ISSUE_DYNAMIC_PROXY = Issue.create(
            "R8DynamicProxy",
            "使用了动态代理，代理接口可能需要添加 R8 Keep 规则",
            DYNAMIC_PROXY_DETAIL,
            Category.CORRECTNESS, 8, Severity.WARNING,
            new Implementation(ReflectionDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    // ======================== WebView @JavascriptInterface ========================

    private static final String JS_INTERFACE_DESCRIPTION =
            "WebView 通过反射调用带有 `@JavascriptInterface` 注解的方法。" +
            "如果该方法所在的类被混淆，WebView 将无法找到该方法。";

    private static final String JS_INTERFACE_DETAIL =
            "带有 `@android.webkit.JavascriptInterface` 注解的方法会被 WebView 通过反射调用，" +
            "R8 无法追踪这些调用，可能在混淆时将该方法或其所在类移除。\n\n" +
            "建议添加 Keep 规则：\n" +
            "```\n" +
            "-keepclassmembers class 你的JS桥接类 {\n" +
            "    @android.webkit.JavascriptInterface <methods>;\n" +
            "}\n" +
            "```";

    public static final Issue ISSUE_JAVASCRIPT_INTERFACE = Issue.create(
            "R8JavascriptInterface",
            "使用了 @JavascriptInterface，该方法需要添加 R8 Keep 规则",
            JS_INTERFACE_DETAIL,
            Category.CORRECTNESS, 8, Severity.WARNING,
            new Implementation(ReflectionDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    // ======================== JNI Native 方法 ========================

    private static final String JNI_METHOD_DESCRIPTION =
            "JNI native 方法通过 JNI 层从原生代码直接调用，R8 无法追踪这些调用。" +
            "如果 native 方法被混淆或移除，将导致运行时 UnsatisfiedLinkError。";

    private static final String JNI_METHOD_DETAIL =
            "Java 中声明的 `native` 方法由原生代码（C/C++）通过 JNI 直接调用，" +
            "R8 无法识别这些调用。如果不添加 keep 规则，R8 可能移除或混淆 native 方法，" +
            "导致运行时崩溃。\n\n" +
            "建议在 proguard-rules.pro 中添加：\n" +
            "```\n" +
            "-keepclassmembers class * { native <methods>; }\n" +
            "```\n\n" +
            "另外，`System.loadLibrary()` 加载的 .so 文件不会被 R8 处理，" +
            "但加载该库的 Java 类中的 native 方法需要上述 keep 规则。";

    public static final Issue ISSUE_JNI_METHOD = Issue.create(
            "R8JNIMethod",
            "使用了 JNI native 方法，需要添加 R8 Keep 规则",
            JNI_METHOD_DETAIL,
            Category.CORRECTNESS, 7, Severity.WARNING,
            new Implementation(ReflectionDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    // ======================== 内部类反射 ========================

    private static final String INNER_CLASS_REFLECTION_DETAIL =
            "通过反射访问内部类（如 `Class.forName(\"com.example.OuterClass$InnerClass\")`）时，" +
            "R8 在完整模式下默认不保留 InnerClasses 和 EnclosingMethod 属性，" +
            "可能导致运行时 ClassNotFoundException。\n\n" +
            "建议添加：\n" +
            "```\n" +
            "-keepattributes InnerClasses, EnclosingMethod\n" +
            "```";

    public static final Issue ISSUE_INNER_CLASS_REFLECTION = Issue.create(
            "R8InnerClassReflection",
            "通过反射访问内部类，需要保留 InnerClasses/EnclosingMethod 属性",
            INNER_CLASS_REFLECTION_DETAIL,
            Category.CORRECTNESS, 6, Severity.WARNING,
            new Implementation(ReflectionDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    // ======================== 检测逻辑 ========================

    /**
     * 需要检测的反射 API 方法名列表
     */
    private static final List<String> REFLECTION_METHOD_NAMES = Arrays.asList(
            "forName",           // Class.forName()
            "invoke",            // Method.invoke(), Constructor.newInstance() 的变体
            "newInstance",       // Constructor.newInstance(), Class.newInstance()
            "getDeclaredField",  // Class.getDeclaredField()
            "getDeclaredMethod", // Class.getDeclaredMethod()
            "getDeclaredFields", // Class.getDeclaredFields()
            "getDeclaredMethods",// Class.getDeclaredMethods()
            "getField",          // Class.getField()
            "getMethod",         // Class.getMethod()
            "setAccessible",     // AccessibleObject.setAccessible()
            "set",               // Field.set()
            "get",               // Field.get()
            "getBoolean",        // Field.getBoolean()
            "getByte",           // Field.getByte()
            "getChar",           // Field.getChar()
            "getShort",          // Field.getShort()
            "getInt",            // Field.getInt()
            "getLong",           // Field.getLong()
            "getFloat",          // Field.getFloat()
            "getDouble",         // Field.getDouble()
            "setBoolean",        // Field.setBoolean()
            "setByte",           // Field.setByte()
            "setChar",           // Field.setChar()
            "setShort",          // Field.setShort()
            "setInt",            // Field.setInt()
            "setLong",           // Field.setLong()
            "setFloat",          // Field.setFloat()
            "setDouble"          // Field.setDouble()
    );

    /** 综合的方法名列表（反射 API + JNI 加载方法） */
    private static final List<String> ALL_APPLICABLE_METHODS;
    static {
        List<String> combined = new ArrayList<>(REFLECTION_METHOD_NAMES);
        combined.add("loadLibrary");
        combined.add("load");
        ALL_APPLICABLE_METHODS = Collections.unmodifiableList(combined);
    }

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return ALL_APPLICABLE_METHODS;
    }

    @Override
    public void visitMethodCall(@NotNull JavaContext context,
                                @NotNull UCallExpression node,
                                @NotNull PsiMethod method) {
        String containingClass = method.getContainingClass() != null
                ? method.getContainingClass().getQualifiedName()
                : "";
        String methodName = method.getName();

        // ========== 反射 API 检测 ==========
        if (context.isEnabled(ISSUE_REFLECTION_API)) {
            // 只检测 Java 反射包中的方法调用
            boolean isReflectionApi = containingClass.startsWith("java.lang.reflect.")
                    || containingClass.equals("java.lang.Class")
                    || containingClass.equals("java.lang.reflect.AccessibleObject");

            // 对于 Field.set/get 等，检测接收者类型是否为 java.lang.reflect.Field
            if (!isReflectionApi && containingClass.startsWith("java.lang.reflect.Field")) {
                isReflectionApi = true;
            }

            if (isReflectionApi) {
                String message = String.format(REFLECTION_API_MESSAGE,
                        methodName + "()");
                context.report(ISSUE_REFLECTION_API, node, context.getLocation(node), message);
            }
        }

        // ========== JNI loadLibrary/load 检测 ==========
        if (context.isEnabled(ISSUE_JNI_METHOD)
                && "java.lang.System".equals(containingClass)
                && ("loadLibrary".equals(methodName) || "load".equals(methodName))) {
            String message = "使用了 `System." + methodName + "()` —— "
                    + "加载的 native 库中的方法需要添加 R8 Keep 规则";
            context.report(ISSUE_JNI_METHOD, node, context.getLocation(node), message);
        }

        // ========== 内部类反射检测（Class.forName 含 $） ==========
        if (context.isEnabled(ISSUE_INNER_CLASS_REFLECTION)
                && "java.lang.Class".equals(containingClass)
                && "forName".equals(methodName)) {
            List<UExpression> args = node.getValueArguments();
            if (!args.isEmpty() && args.get(0) instanceof ULiteralExpression) {
                Object constValue = ((ULiteralExpression) args.get(0)).getValue();
                if (constValue instanceof String && ((String) constValue).contains("$")) {
                    String message = "`Class.forName()` 参数包含内部类名（`" + constValue + "`）—— "
                            + "需要保留 InnerClasses/EnclosingMethod 属性";
                    context.report(ISSUE_INNER_CLASS_REFLECTION, node,
                            context.getLocation(node), message);
                }
            }
        }
    }

    // ======================== @JavascriptInterface 注解检测 ========================

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Arrays.asList(UAnnotation.class, UClass.class);
    }

    @Nullable
    @Override
    public UElementHandler createUastHandler(@NotNull JavaContext context) {
        boolean checkJsInterface = context.isEnabled(ISSUE_JAVASCRIPT_INTERFACE);
        boolean checkJni = context.isEnabled(ISSUE_JNI_METHOD);

        if (!checkJsInterface && !checkJni) return null;

        return new UElementHandler() {
            @Override
            public void visitAnnotation(@NotNull UAnnotation node) {
                if (!checkJsInterface) return;
                if (!"android.webkit.JavascriptInterface".equals(node.getQualifiedName())) {
                    return;
                }
                String message = "方法带有 `@JavascriptInterface` 注解 —— "
                        + "该方法会被 WebView 通过反射调用，需要添加 R8 Keep 规则";
                context.report(ISSUE_JAVASCRIPT_INTERFACE, node,
                        context.getLocation(node), message);
            }

            @Override
            public void visitClass(@NotNull UClass node) {
                if (!checkJni) return;

                // 通过 PSI 获取底层 Java 类，检查是否有 native 方法
                PsiClass psiClass = (PsiClass) node.getJavaPsi();
                if (psiClass == null || psiClass.getQualifiedName() == null) return;

                boolean hasNative = false;
                String firstNativeName = null;
                for (PsiMethod method : psiClass.getMethods()) {
                    if (method.hasModifierProperty(PsiModifier.NATIVE)) {
                        hasNative = true;
                        firstNativeName = method.getName();
                        break;
                    }
                }

                if (hasNative) {
                    String message = "类 `" + psiClass.getQualifiedName()
                            + "` 包含 native 方法（`" + firstNativeName + "`）—— "
                            + "需要添加 R8 Keep 规则";
                    context.report(ISSUE_JNI_METHOD, node,
                            context.getLocation((UElement) node), message);
                }
            }
        };
    }
}
