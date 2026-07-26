package com.xyrlsz.xcimocob.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import java.util.Arrays;
import java.util.List;

/**
 * 检测 Gson 序列化 API 的调用，提醒开发者添加 R8 Keep 规则。
 * <p>
 * 类级别的检测（Parcelable、Serializable 等）由 Gradle 任务
 * {@code checkR8KeepRules} 在 R8 打包后通过解析 seeds.txt 完成。
 */
public class SerializationDetector extends Detector implements Detector.UastScanner {

    private static final String GSON_DETAIL =
            "Gson 通过反射读写对象的字段。如果 Gson 反/序列化的数据类字段被 R8 混淆，" +
            "会导致 JSON 字段始终为 null 或序列化/反序列化失败。\n\n" +
            "建议添加 Keep 规则：\n" +
            "```\n" +
            "-keepattributes Signature\n" +
            "-keep class 你的数据类 { *; }\n" +
            "-keep class com.google.gson.reflect.TypeToken { *; }\n" +
            "-keep class * extends com.google.gson.reflect.TypeToken\n" +
            "```";

    public static final Issue ISSUE_GSON = Issue.create(
            "R8GsonUsage",
            "使用了 Gson 序列化，相关数据类可能需要添加 R8 Keep 规则",
            GSON_DETAIL,
            Category.CORRECTNESS, 7, Severity.WARNING,
            new Implementation(SerializationDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    private static final List<String> GSON_API_METHODS = Arrays.asList(
            "fromJson", "toJson"
    );

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return GSON_API_METHODS;
    }

    @Override
    public void visitMethodCall(@NotNull JavaContext context,
                                @NotNull UCallExpression node,
                                @NotNull PsiMethod method) {
        if (!context.isEnabled(ISSUE_GSON)) return;

        String qualifiedName = method.getContainingClass() != null
                ? method.getContainingClass().getQualifiedName()
                : "";

        // 检测 Gson 的 fromJson() / toJson() 调用
        if ("com.google.gson.Gson".equals(qualifiedName)) {
            String message = "使用了 Gson 的 `" + method.getName() + "()` —— "
                    + "被序列化/反序列化的数据类需要添加 R8 Keep 规则";
            context.report(ISSUE_GSON, (org.jetbrains.uast.UElement) node,
                    context.getLocation((org.jetbrains.uast.UElement) node), message);
        }

        // 检测 TypeToken 构造调用
        if ("com.google.gson.reflect.TypeToken".equals(qualifiedName)) {
            String message = "使用了 `TypeToken` —— "
                    + "TypeToken 子类需要添加 R8 Keep 规则以防止泛型信息丢失";
            context.report(ISSUE_GSON, (org.jetbrains.uast.UElement) node,
                    context.getLocation((org.jetbrains.uast.UElement) node), message);
        }
    }
}
