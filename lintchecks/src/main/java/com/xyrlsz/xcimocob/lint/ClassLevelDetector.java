package com.xyrlsz.xcimocob.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;

import java.util.Arrays;
import java.util.List;

/**
 * 检测代码中需要 R8 Keep 规则的类级别模式。
 * <p>
 * 通过 applicableSuperClasses 检测 Parcelable/Serializable 实现类，
 * 通过 getApplicableMethodNames 检测 ObjectBox boxFor 调用。
 */
public class ClassLevelDetector extends Detector implements Detector.UastScanner {

    // ======================== Parcelable ========================

    private static final String PARCELABLE_DETAIL =
            "实现了 `android.os.Parcelable` 的类在跨进程通信中使用，" +
            "其 CREATOR 字段通过反射被系统调用。R8 完整模式不会隐式保留 CREATOR。\n\n" +
            "建议在 proguard-rules.pro 中添加：\n" +
            "```\n" +
            "-keepclassmembers class * implements android.os.Parcelable {\n" +
            "    public static final android.os.Parcelable$Creator CREATOR;\n" +
            "}\n" +
            "```";

    public static final Issue ISSUE_PARCELABLE = Issue.create(
            "R8Parcelable",
            "实现了 Parcelable，CREATOR 字段需要添加 R8 Keep 规则",
            PARCELABLE_DETAIL,
            Category.CORRECTNESS, 8, Severity.WARNING,
            new Implementation(ClassLevelDetector.class, Scope.JAVA_FILE_SCOPE)
    );

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

    // ======================== ObjectBox ========================

    private static final String OBJECTBOX_DETAIL =
            "使用了 ObjectBox（`BoxStore.boxFor()`），" +
            "所有 @Entity 实体类在运行时通过反射访问，需要添加 R8 Keep 规则。\n\n" +
            "建议在 proguard-rules.pro 中添加：\n" +
            "```\n" +
            "-keep class io.objectbox.** { *; }\n" +
            "-keep class * implements io.objectbox.converter.PropertyConverter { *; }\n" +
            "-keepclassmembers class * {\n" +
            "    @io.objectbox.annotation.* <fields>;\n" +
            "}\n" +
            "```";

    public static final Issue ISSUE_OBJECTBOX = Issue.create(
            "R8ObjectBoxUsage",
            "使用了 ObjectBox，实体类和内部类需要添加 R8 Keep 规则",
            OBJECTBOX_DETAIL,
            Category.CORRECTNESS, 8, Severity.WARNING,
            new Implementation(ClassLevelDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    // ============================================================
    // applicableSuperClasses —— 用于检测 Parcelable / Serializable
    // ============================================================

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Arrays.asList(
                "android.os.Parcelable",
                "java.io.Serializable"
        );
    }

    @Override
    public void visitClass(@NotNull JavaContext context, @NotNull UClass node) {
        if (node.getQualifiedName() == null) return;

        String className = node.getQualifiedName();

        // 检查 Parcelable
        if (context.isEnabled(ISSUE_PARCELABLE) && isSubtypeOf(node, "android.os.Parcelable")) {
            String message = "类 `" + className
                    + "` 实现了 Parcelable —— CREATOR 字段需要 Keep 规则";
            context.report(ISSUE_PARCELABLE, node,
                    context.getLocation((UElement) node), message);
        }

        // 检查 Serializable
        if (context.isEnabled(ISSUE_SERIALIZABLE) && isSubtypeOf(node, "java.io.Serializable")) {
            String message = "类 `" + className
                    + "` 实现了 Serializable —— serialVersionUID 等需要 Keep 规则";
            context.report(ISSUE_SERIALIZABLE, node,
                    context.getLocation((UElement) node), message);
        }
    }

    // ============================================================
    // getApplicableMethodNames —— 用于检测 ObjectBox boxFor
    // ============================================================

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("boxFor");
    }

    @Override
    public void visitMethodCall(@NotNull JavaContext context,
                                @NotNull UCallExpression node,
                                @NotNull PsiMethod method) {
        if (!context.isEnabled(ISSUE_OBJECTBOX)) return;

        String qualifiedName = method.getContainingClass() != null
                ? method.getContainingClass().getQualifiedName()
                : "";

        // 检测 ObjectBox BoxStore.boxFor() 调用
        if ("io.objectbox.BoxStore".equals(qualifiedName)
                && "boxFor".equals(method.getName())) {
            String message = "使用了 ObjectBox 的 `boxFor()` —— "
                    + "所有 @Entity 数据类需要添加 R8 Keep 规则";
            context.report(ISSUE_OBJECTBOX, (UElement) node,
                    context.getLocation((UElement) node), message);
        }
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /** 检查类或其父类/接口是否匹配指定类型 */
    private boolean isSubtypeOf(UClass node, String typeName) {
        if (typeName.equals(node.getQualifiedName())) return true;

        // 检查直接实现的接口
        for (PsiClass iface : node.getInterfaces()) {
            if (typeName.equals(iface.getQualifiedName())) return true;
        }

        // 检查父类（递归）
        PsiClass superClass = node.getSuperClass();
        if (superClass != null && typeName.equals(superClass.getQualifiedName())) {
            return true;
        }

        return false;
    }
}
