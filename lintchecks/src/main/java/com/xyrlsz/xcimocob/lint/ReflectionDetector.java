package com.xyrlsz.xcimocob.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UReferenceExpression;
import com.intellij.psi.PsiMethod;

import java.util.Arrays;
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

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return REFLECTION_METHOD_NAMES;
    }

    @Override
    public void visitMethodCall(@NotNull JavaContext context,
                                @NotNull UCallExpression node,
                                @NotNull PsiMethod method) {
        if (context.isEnabled(ISSUE_REFLECTION_API)) {
            String containingClass = method.getContainingClass() != null
                    ? method.getContainingClass().getQualifiedName()
                    : "";

            // 只检测 Java 反射包中的方法调用
            boolean isReflectionApi = containingClass.startsWith("java.lang.reflect.")
                    || containingClass.equals("java.lang.Class")
                    || containingClass.equals("java.lang.reflect.AccessibleObject");

            // 对于 Field.set/get 等，检测接收者类型是否为 java.lang.reflect.Field
            if (!isReflectionApi && containingClass.startsWith("java.lang.reflect.Field")) {
                isReflectionApi = true;
            }

            if (isReflectionApi) {
                String receiverType = "";
                if (node.getReceiver() instanceof UReferenceExpression) {
                    receiverType = ((UReferenceExpression) node.getReceiver())
                            .getResolvedName();
                }

                String message = String.format(REFLECTION_API_MESSAGE,
                        method.getName() + "()");
                context.report(ISSUE_REFLECTION_API, node, context.getLocation(node), message);
            }
        }
    }
}
