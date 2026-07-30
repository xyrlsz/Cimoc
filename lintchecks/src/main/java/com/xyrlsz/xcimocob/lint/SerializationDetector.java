package com.xyrlsz.xcimocob.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClassLiteralExpression;
import org.jetbrains.uast.UExpression;

import java.util.Arrays;
import java.util.List;

/**
 * 检测 Gson 序列化相关数据类是否缺少 {@code @SerializedName} 注解。
 * <p>
 * 当代码调用 {@code gson.fromJson(json, SomeClass.class)} 时，检查 {@code SomeClass}
 * 的实例字段是否都有 {@code @SerializedName}，缺少则报告。
 * <p>
 * Gson 2.14.0 的 consumer 规则已自包含（Signature、TypeToken、{@code @SerializedName}
 * 字段保留等），无需额外保持规则。真正有风险的是字段没有 {@code @SerializedName} 时
 * R8 混淆导致字段名与 JSON key 不匹配。
 */
public class SerializationDetector extends Detector implements Detector.UastScanner {

    private static final String MISSING_SERIALIZED_NAME_DETAIL =
            "Gson 反序列化时通过反射直接设置字段值。如果数据类字段缺少 @SerializedName 注解，" +
            "当 R8 开启混淆后，Gson 将使用被混淆后的字段名作为 JSON key，" +
            "导致与服务器约定的 JSON 字段名不匹配，反序列化结果中该字段始终为 null。\n\n" +
            "建议：在 Gson 反/序列化的数据类中，为每个字段添加 @SerializedName 注解，" +
            "明确指定 JSON key 名称，这样即使字段被 R8 混淆，Gson 仍然能正确读写。";

    public static final Issue ISSUE_MISSING_SERIALIZED_NAME = Issue.create(
            "R8MissingSerializedName",
            "Gson 数据类字段缺少 @SerializedName 注解，R8 混淆后可能导致反序列化失败",
            MISSING_SERIALIZED_NAME_DETAIL,
            Category.CORRECTNESS, 8, Severity.WARNING,
            new Implementation(SerializationDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("fromJson");
    }

    @Override
    public void visitMethodCall(@NotNull JavaContext context,
                                @NotNull UCallExpression node,
                                @NotNull PsiMethod method) {
        String qualifiedName = method.getContainingClass() != null
                ? method.getContainingClass().getQualifiedName()
                : "";

        // 只检测 Gson.fromJson()，检查目标类字段是否缺少 @SerializedName
        if ("com.google.gson.Gson".equals(qualifiedName)
                && "fromJson".equals(method.getName())) {
            checkMissingSerializedName(context, node, method);
        }
    }

    /**
     * 检查 Gson {@code fromJson()} 的目标类，若其中存在未添加 @SerializedName 的实例字段则报告。
     * <p>
     * 仅当第二个参数是 {@code SomeClass.class} 类字面量时可解析。
     */
    private void checkMissingSerializedName(@NotNull JavaContext context,
                                            @NotNull UCallExpression node,
                                            @NotNull PsiMethod method) {
        if (!context.isEnabled(ISSUE_MISSING_SERIALIZED_NAME)) return;
        if (!"fromJson".equals(method.getName())) return;

        List<UExpression> args = node.getValueArguments();
        if (args.size() < 2) return;

        // 第二个参数应为类字面量，如 SomeClass.class
        UExpression classArg = args.get(1);
        if (!(classArg instanceof UClassLiteralExpression)) return;

        PsiType psiType = ((UClassLiteralExpression) classArg).getType();
        if (!(psiType instanceof PsiClassType)) return;

        PsiClass targetClass = ((PsiClassType) psiType).resolve();
        if (targetClass == null) return;

        boolean hasInstanceField = false;
        boolean hasMissingAnnotation = false;
        PsiField firstMissingField = null;

        for (PsiField field : targetClass.getFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue;
            hasInstanceField = true;
            if (field.getAnnotation("com.google.gson.annotations.SerializedName") == null) {
                hasMissingAnnotation = true;
                if (firstMissingField == null) {
                    firstMissingField = field;
                }
            }
        }

        if (hasInstanceField && hasMissingAnnotation) {
            String message = "类 `" + targetClass.getName() + "` 的字段缺少 @SerializedName 注解"
                    + "（例如 `" + firstMissingField.getName() + "`），"
                    + "R8 混淆后可能导致 Gson 反序列化失败";
            context.report(ISSUE_MISSING_SERIALIZED_NAME, node,
                    context.getLocation(node), message);
        }
    }
}
