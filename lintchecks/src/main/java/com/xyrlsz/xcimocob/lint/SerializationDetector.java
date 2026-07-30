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
 * 检测 Gson 序列化 API 的调用，提醒开发者添加 R8 Keep 规则。
 * <p>
 * 类级别的检测（Parcelable、Serializable 等）由 Gradle 任务
 * {@code checkR8KeepRules} 在 R8 打包后通过解析 seeds.txt 完成。
 */
public class SerializationDetector extends Detector implements Detector.UastScanner {

    private static final String GSON_DETAIL =
            "Gson 通过反射读写对象的字段。如果 Gson 反/序列化的数据类字段被 R8 混淆，" +
            "会导致 JSON 字段始终为 null 或序列化/反序列化失败。\n\n" +
            "注意：\n" +
            "- Gson 2.11+ 已自带 consumer 规则，自动保留 @SerializedName 字段和 TypeToken，无需额外配置。\n" +
            "- 如果数据类未使用 @SerializedName，则需显式添加 -keep 规则防止字段被混淆。\n" +
            "- 建议统一使用 @SerializedName 注解字段，利用 Gson 自带规则即可。";

    public static final Issue ISSUE_GSON = Issue.create(
            "R8GsonUsage",
            "使用了 Gson 序列化，相关数据类可能需要添加 R8 Keep 规则",
            GSON_DETAIL,
            Category.CORRECTNESS, 7, Severity.WARNING,
            new Implementation(SerializationDetector.class, Scope.JAVA_FILE_SCOPE)
    );

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
            context.report(ISSUE_GSON, node,
                    context.getLocation(node), message);

            // 对 fromJson() 额外检查目标类是否缺少 @SerializedName
            checkMissingSerializedName(context, node, method);
        }

        // 检测 TypeToken 构造调用
        // Gson 2.14.0 的 consumer 规则已包含：
        //   - -keepattributes Signature
        //   - -keepattributes RuntimeVisibleAnnotations,AnnotationDefault
        //   - -keep 类继承 TypeToken
        // 因此通常无需额外配置，此处仅为提醒。
        if ("com.google.gson.reflect.TypeToken".equals(qualifiedName)) {
            String message = "使用了 `TypeToken` —— "
                    + "Gson 2.14.0 已自带保留规则（Signature、TypeToken 继承等），无需额外配置";
            context.report(ISSUE_GSON, node,
                    context.getLocation(node), message);
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
