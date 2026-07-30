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
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;

import java.util.Arrays;
import java.util.List;

/**
 * 检测代码中需要 R8 Keep 规则的类级别模式。
 * <p>
 * 通过 applicableSuperClasses 检测 Serializable 实现类，
 * 通过 getApplicableMethodNames 检测 ObjectBox boxFor 调用。
 * <p>
 * 注意：Parcelable 的 CREATOR 保留规则已包含在 Android 默认 ProGuard 规则中
 * （proguard-android-optimize.txt），因此无需检测。
 */
public class ClassLevelDetector extends Detector implements Detector.UastScanner {

    // ======================== Serializable ========================

    private static final String SERIALIZABLE_DETAIL =
            "实现了 `java.io.Serializable` 的类在序列化时，" +
            "R8 可能认为 serialVersionUID 等字段未被直接使用而将其混淆或移除。\n\n" +
            "建议在 proguard-rules.pro 中添加：\n" +
            "```\n" +
            "-keepclassmembers class * implements java.io.Serializable {\n" +
            "    static final long serialVersionUID;\n" +
            "    private void writeObject(java.io.ObjectOutputStream);\n" +
            "    private void readObject(java.io.ObjectInputStream);\n" +
            "}\n" +
            "```";

    public static final Issue ISSUE_SERIALIZABLE = Issue.create(
            "R8Serializable",
            "实现了 Serializable，序列化相关成员需要添加 R8 Keep 规则",
            SERIALIZABLE_DETAIL,
            Category.CORRECTNESS, 7, Severity.WARNING,
            new Implementation(ClassLevelDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    // ============================================================
    // applicableSuperClasses —— 用于检测 Serializable
    // ============================================================
    // 注意：Parcelable 的 CREATOR 保留规则已包含在 Android 默认 ProGuard 规则中
    // （proguard-android-optimize.txt），因此无需检测。
    //
    // ObjectBox 的 @Entity 类通过编译期生成代码（MyObjectBox、EntityName_、Cursor 等）
    // 被 R8 直接引用，不需要手动 keep 规则，因此也无需检测。

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Arrays.asList("java.io.Serializable");
    }

    @Override
    public void visitClass(@NotNull JavaContext context, @NotNull UClass node) {
        if (node.getQualifiedName() == null) return;

        String className = node.getQualifiedName();

        // 检查 Serializable
        if (context.isEnabled(ISSUE_SERIALIZABLE)
                && context.getEvaluator().inheritsFrom(node, "java.io.Serializable", true)) {
            String message = "类 `" + className
                    + "` 实现了 Serializable —— serialVersionUID 等需要 Keep 规则";
            context.report(ISSUE_SERIALIZABLE, node,
                    context.getLocation((UElement) node), message);
        }
    }

}
