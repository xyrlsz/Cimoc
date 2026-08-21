package com.xyrlsz.xcimocob.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClassLiteralExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UObjectLiteralExpression;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 检测 Gson 序列化相关数据类是否缺少 {@code @SerializedName} 注解。
 * <p>
 * 覆盖以下用户代码中的 Gson 调用形态：
 * <ul>
 *   <li>{@code Gson.fromJson(json, Foo.class)}</li>
 *   <li>{@code Gson.fromJson(json, new TypeToken<List<Foo>>(){}.getType())} （TypeToken
 *       匿名子类，泛型实参里出现的类都会被递归检查）</li>
 *   <li>{@code Gson.fromJson(json, someType)} 当 {@code someType} 的类型可静态解析为
 *       {@code Type} / {@code Class} 时也尽量解析。</li>
 *   <li>{@code Gson.toJson(obj, Foo.class)} 与带 Type 参数的 {@code toJson}（序列化方向同样
 *       会因为混淆字段名而导致 JSON key 错，R8 后双方不匹配）。</li>
 * </ul>
 *
 * <p>字段检查规则：
 * <ul>
 *   <li>排除 {@code static} / {@code transient} 字段（Gson 默认不读写 transient）。</li>
 *   <li>向上递归 {@code superclass}，直到 {@code java.lang.Object}；继承来的字段只要会被 Gson
 *       序列化，就同样要求 @SerializedName。</li>
 *   <li>对 {@code List<Foo> / Map<String, Foo> / Foo[]} 这种集合/数组类型，递归检查元素类型 Foo。
 *       只对 JDK 集合容器跳过「容器类自身字段检查」，直接深入元素类型。</li>
 *   <li>遇到已经检查过的类（通过 qualified name）直接跳过，避免循环泛型依赖。</li>
 *   <li>JDK/Android SDK/三方库（非项目源码）类不检查：我们不会给它们加注解。</li>
 *   <li>primitive / boxed / String / void / Object / java.lang.Enum 不检查，直接通过。</li>
 * </ul>
 *
 * <p>关于 @Expose：
 * 即使字段用了 @Expose（配合 {@code GsonBuilder.excludeFieldsWithoutExposeAnnotation()}），
 * 它只控制字段是否参与序列化，不能解决「R8 混淆后字段名与 JSON key 不一致」的问题，
 * 因此仍然要求字段有 @SerializedName（或者走 keep 字段名的 ProGuard 规则——但这里只报
 * 缺少注解的警告，ProGuard 规则由开发者单独维护）。
 */
public class SerializationDetector extends Detector implements Detector.UastScanner {

    private static final String MISSING_SERIALIZED_NAME_DETAIL =
            "Gson 序列化 / 反序列化时默认按字段名读写 JSON。若数据类字段缺少 @SerializedName，" +
            "R8 混淆后字段名会与服务器 / 本地 JSON 的 key 不匹配，最终字段恒为 null 或" +
            "写出错误 key（导致对端解析失败）。\n\n" +
            "建议：对所有被 Gson 直接序列化的数据类字段，显式加 @SerializedName 指定 key；" +
            "若使用 TypeToken<List<Foo>>、Gson.toJson(obj, Foo.class) 等调用形态也需覆盖 Foo。";

    public static final Issue ISSUE_MISSING_SERIALIZED_NAME = Issue.create(
            "R8MissingSerializedName",
            "Gson 数据类字段缺少 @SerializedName 注解，R8 混淆后可能导致序列化/反序列化失败",
            MISSING_SERIALIZED_NAME_DETAIL,
            Category.CORRECTNESS, 8, Severity.WARNING,
            new Implementation(SerializationDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    // 我们不会对这些类做「类自身字段是否缺少 @SerializedName」的检查，
    // 因为它们要么是 JDK/Android SDK/第三方库的类型，要么是 primitive/String/enum/Object。
    private static final Set<String> CONTAINER_FQN = new HashSet<>(Arrays.asList(
            "java.util.List", "java.util.ArrayList", "java.util.LinkedList",
            "java.util.Set", "java.util.HashSet", "java.util.LinkedHashSet",
            "java.util.SortedSet", "java.util.TreeSet", "java.util.NavigableSet",
            "java.util.Queue", "java.util.Deque", "java.util.ArrayDeque",
            "java.util.Collection", "java.util.Vector", "java.util.Stack",
            "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap",
            "java.util.SortedMap", "java.util.TreeMap", "java.util.NavigableMap",
            "java.util.concurrent.ConcurrentHashMap",
            "java.util.concurrent.ConcurrentMap",
            "java.util.concurrent.CopyOnWriteArrayList",
            "java.util.concurrent.CopyOnWriteArraySet",
            "java.util.Arrays$ArrayList",
            "java.util.Collections$SingletonList",
            "java.util.Collections$SingletonSet",
            "java.util.Collections$SingletonMap",
            "java.util.Collections$EmptyList",
            "java.util.Collections$EmptySet",
            "java.util.Collections$EmptyMap"
    ));

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        // 同时覆盖 fromJson 与 toJson；两者在 R8 混淆场景下都可能导致 JSON key 不匹配。
        return Arrays.asList("fromJson", "toJson");
    }

    @Override
    public void visitMethodCall(@NotNull JavaContext context,
                                @NotNull UCallExpression node,
                                @NotNull PsiMethod method) {
        String owner = method.getContainingClass() != null
                ? method.getContainingClass().getQualifiedName() : "";
        if (!"com.google.gson.Gson".equals(owner)) return;
        String name = method.getName();
        if (!"fromJson".equals(name) && !"toJson".equals(name)) return;
        if (!context.isEnabled(ISSUE_MISSING_SERIALIZED_NAME)) return;

        List<UExpression> args = node.getValueArguments();
        PsiParameter[] params = method.getParameterList().getParameters();
        if (args.isEmpty()) return;

        // ========== 1) 从参数中定位「目标 Type / Class」参数 ==========
        //   fromJson(json, Class<T>)        -> arg index 1
        //   fromJson(json, Type)            -> arg index 1
        //   fromJson(reader, Class<T>)      -> arg index 1
        //   fromJson(JsonElement, Class<T>) -> arg index 1
        //   toJson(Object)                  -> 无 type 参数：跳过（只能推断出 Object.class）
        //   toJson(Object, Type)            -> arg index 1
        //   toJson(JsonWriter, Object, Type)-> arg index 2
        //   toJson(Object, Appendable, Type)-> arg index 2
        //   toJsonTree(Object, Type)        -> arg index 1  (实际这里不检测 toJsonTree)
        // 简单且鲁棒的做法：找「第二个参数（若参数数>=2）或第三个参数（若参数数>=3且
        //   倒数第二个参数类型是 Appendable/JsonWriter/类似写入器）」里的参数类型为
        //   Class<?> 或 java.lang.reflect.Type 的那个参数。
        int typeArgIndex = -1;
        for (int i = Math.min(params.length, args.size()) - 1; i >= 0; i--) {
            PsiType pt = params[i].getType();
            String tq = pt.getCanonicalText(true);
            if (pt instanceof PsiClassType) {
                String qn = ((PsiClassType) pt).resolve() == null ? null :
                        ((PsiClassType) pt).resolve().getQualifiedName();
                if ("java.lang.Class".equals(qn) || "java.lang.reflect.Type".equals(qn)
                        || "com.google.common.reflect.TypeToken".equals(qn)
                        || "com.google.gson.reflect.TypeToken".equals(qn)) {
                    typeArgIndex = i;
                    break;
                }
            }
            if (tq != null && (tq.startsWith("java.lang.Class")
                    || tq.startsWith("java.lang.reflect.Type")
                    || tq.startsWith("com.google.gson.reflect.TypeToken"))) {
                typeArgIndex = i;
                break;
            }
        }
        if (typeArgIndex < 0 || typeArgIndex >= args.size()) {
            // toJson(Object) / fromJson(json, Foo.class?) 不匹配：跳过
            return;
        }

        UExpression typeArg = args.get(typeArgIndex);
        Set<PsiClass> targets = new HashSet<>();

        // ========== 2) 解析「类型参数」得到所有目标 PsiClass ==========
        // 2a) 类字面量：Foo.class
        if (typeArg instanceof UClassLiteralExpression) {
            PsiType pt = ((UClassLiteralExpression) typeArg).getType();
            if (pt instanceof PsiClassType) {
                // ((UClassLiteralExpression) typeArg).getType() = Class<Foo>
                // 交给 collectFrom 识别 Class<Foo> -> Foo
                collectFrom(((PsiClassType) pt), targets, new HashSet<>());
            }
        } else {
            // 2b) 形如：new TypeToken<List<Foo>>(){}.getType()   -> 返回值是 Type
            //       或   ((Type) xxx)
            //       或   someTypeVar   （无法静态解析则跳过）
            // 策略：递归剥离 getType() / getRawType()，拿到 receiver；
            //       若 receiver 是匿名类对象字面量，则取其继承的 TypeToken<X>。
            UExpression base = unwrapGetXxxMethodReceiver(typeArg);
            if (base instanceof UObjectLiteralExpression) {
                Object psi = ((UObjectLiteralExpression) base).getJavaPsi();
                PsiClass anonClass = null;
                if (psi instanceof PsiClass) anonClass = (PsiClass) psi;
                else if (base instanceof org.jetbrains.uast.UDeclaration) {
                    // fallback: 某些新版 UAST 暴露 findPsiClass() 或对应方法
                }
                if (anonClass != null) {
                    PsiClassType[] supers = anonClass.getSuperTypes();
                    for (PsiClassType s : supers) {
                        if (isTypeTokenFqn(fqnOf(s.resolve()))) {
                            // 把 TypeToken<FooBar> 这种 PsiClassType 传进 collectFrom
                            collectFrom(s, targets, new HashSet<>());
                        }
                    }
                }
            }
        }

        // ========== 3) 对每个目标类检查字段；若缺少就报告，一条 fromJson/toJson 只报一条 ==========
        if (targets.isEmpty()) return;

        // 逐个类检查，只要发现第一个缺少字段就 report（避免同一调用点重复噪声）。
        for (PsiClass clazz : targets) {
            MissingFieldReport report = findMissingSerializedName(clazz, new HashSet<>());
            if (report != null) {
                String message = "类 `" + safeName(clazz) + "` 的字段缺少 @SerializedName 注解" +
                        "（例如 `" + report.fieldName + "`），R8 混淆后可能导致 Gson 序列化/反序列化失败";
                context.report(ISSUE_MISSING_SERIALIZED_NAME, node, context.getLocation(node), message);
                return;
            }
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * 对形如 {@code foo.getType()} / {@code new T(){}.getType()} 的表达式，
     * 返回 receiver（即 `.getType()` 之前的子表达式）。若不是方法调用，原样返回。
     */
    @NotNull
    private static UExpression unwrapGetXxxMethodReceiver(@NotNull UExpression expr) {
        UExpression cur = expr;
        int guard = 10;
        while (guard-- > 0 && cur instanceof UCallExpression) {
            UCallExpression call = (UCallExpression) cur;
            String n = call.getMethodName();
            if (!"getType".equals(n) && !"getRawType".equals(n)) break;
            if (call.getReceiver() == null) break;
            cur = call.getReceiver();
        }
        return cur;
    }

    private static boolean isTypeTokenFqn(@Nullable String fqn) {
        return "com.google.gson.reflect.TypeToken".equals(fqn)
                || "com.google.common.reflect.TypeToken".equals(fqn)
                || "kotlin.reflect.type.ParameterizedTypeImpl".equals(fqn); // 误命中兜底
    }

    /**
     * 从一个 PsiClassType（可能是 {@code Class<Foo> / List<Foo> / Map<K,V> / Foo[] / Foo}）
     * 出发，提取出其中「需要 Gson 序列化且用户自己定义」的数据类，加入 {@code out}。
     * <p>
     * 对于 TypeToken 的特殊入口：
     * <pre>
     *   TypeToken<List<Foo>>             -> ClassType(TypeToken, [<List<Foo>>])
     *     => 取第 0 个类型参数 = List<Foo>，发现是容器就取第一个元素参数 = Foo 加入。
     * </pre>
     */
    private static void collectFrom(@NotNull PsiClassType classType,
                                    @NotNull Set<PsiClass> out,
                                    @NotNull Set<String> visitingFqn) {
        PsiClass resolved = classType.resolve();
        String fqn = resolved == null ? null : resolved.getQualifiedName();
        // Class<Foo> -> 深入它的第一个类型参数
        if ("java.lang.Class".equals(fqn)) {
            PsiType[] typeParams = classType.getParameters();
            if (typeParams.length == 1 && typeParams[0] instanceof PsiClassType) {
                collectFrom((PsiClassType) typeParams[0], out, visitingFqn);
            }
            return;
        }
        // TypeToken<Foo> -> 深入它的第一个类型参数（因为 TypeToken 本身不是数据类，我们要的是 <Foo>）
        if (isTypeTokenFqn(fqn)) {
            PsiType[] typeParams = classType.getParameters();
            if (typeParams.length >= 1 && typeParams[0] instanceof PsiClassType) {
                collectFrom((PsiClassType) typeParams[0], out, visitingFqn);
            }
            return;
        }
        // Foo[] -> 数组元素类型
        if (classType.getArrayDimensions() > 0) {
            PsiType deep = classType.getDeepComponentType();
            if (deep instanceof PsiClassType) {
                collectFrom((PsiClassType) deep, out, visitingFqn);
            }
            return;
        }

        if (resolved == null || fqn == null) return;
        if (isBuiltin(fqn, resolved)) return;

        // 容器：只深入元素/值类型；容器类本身不检查
        if (isContainer(fqn)) {
            for (PsiType tp : classType.getParameters()) {
                if (tp instanceof PsiClassType) collectFrom((PsiClassType) tp, out, visitingFqn);
            }
            // 对 Map 我们默认检查所有类型参数（<String, Foo> 会跳过 String，收集 Foo），够用。
            return;
        }

        // 普通用户数据类：加入集合；并对其自身字段类型递归（内嵌类也可能缺注解）
        if (visitingFqn.contains(fqn)) return;
        visitingFqn.add(fqn);
        out.add(resolved);

        // 内嵌字段类型也要覆盖（避免 Data { List<Item> items } 里 Item 没检查）
        for (PsiField f : getAllFieldsUpToObject(resolved)) {
            if (f.hasModifierProperty(PsiModifier.STATIC)) continue;
            if (f.hasModifierProperty(PsiModifier.TRANSIENT)) continue;
            PsiType t = f.getType();
            // 数组类型通过 getArrayDimensions()>0 分支处理
            if (t instanceof PsiClassType) {
                collectFrom((PsiClassType) t, out, visitingFqn);
            }
        }
    }

    /**
     * 检查单个类（含父类字段）是否存在「需要 @SerializedName 但缺失」的实例字段。
     * 返回第一个缺失字段的报告，或 null 表示通过。
     * <p>
     * 规则：
     * - static：跳过
     * - transient：跳过（Gson 默认不写/不读，不会错 key）
     * - 枚举类：跳过（Gson 用 Enum.name()）
     * - 接口/抽象类：跳过
     * - 字段有 @SerializedName：OK，包括 value="" 也是显式指定。
     * - volatile：不跳过；它不影响 Gson 是否使用字段名。
     */
    @Nullable
    private static MissingFieldReport findMissingSerializedName(@NotNull PsiClass clazz,
                                                                  @NotNull Set<String> seenFqn) {
        String fqn = clazz.getQualifiedName();
        if (fqn == null) return null;
        if (seenFqn.contains(fqn)) return null;
        seenFqn.add(fqn);

        if (clazz.isEnum() || clazz.isInterface() || clazz.isAnnotationType()) return null;
        if (isBuiltin(fqn, clazz)) return null;

        List<PsiField> allFields = getAllFieldsUpToObject(clazz);
        for (PsiField field : allFields) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue;
            if (field.hasModifierProperty(PsiModifier.TRANSIENT)) continue;

            PsiAnnotation ann = field.getAnnotation("com.google.gson.annotations.SerializedName");
            if (ann == null) {
                return new MissingFieldReport(safeName(clazz), field.getName());
            }
        }

        // 同时对字段类型递归（防止 List<Foo> 这种里的 Foo 本身没注解也没被外层发现）
        // 注：这一层是为防御 collectFrom 对字段类型递归有漏掉场景；
        // 因为 collectFrom 已经做过，这里做浅度检查即可避免重复，用 seenFqn 限制。
        for (PsiField field : allFields) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue;
            if (field.hasModifierProperty(PsiModifier.TRANSIENT)) continue;
            PsiType t = field.getType();
            if (t instanceof PsiClassType) {
                PsiClass c = ((PsiClassType) t).resolve();
                if (c != null) {
                    MissingFieldReport r = findMissingSerializedName(c, seenFqn);
                    if (r != null) return r;
                }
            }
        }
        return null;
    }

    /**
     * 向上遍历 superclass 直到 Object，收集【当前类继承层级中所有】字段。
     */
    @NotNull
    private static List<PsiField> getAllFieldsUpToObject(@NotNull PsiClass start) {
        List<PsiField> out = new java.util.ArrayList<>();
        PsiClass cur = start;
        Set<String> seenNames = new HashSet<>();
        while (cur != null) {
            String fqn = cur.getQualifiedName();
            if ("java.lang.Object".equals(fqn)) break;
            // 属于 JDK/Android SDK 的父类：其字段不检查（通常不需要我们加注解）
            if (fqn != null && isSdkOrLibrary(fqn)) {
                cur = cur.getSuperClass();
                continue;
            }
            for (PsiField f : cur.getFields()) {
                if (!f.hasModifierProperty(PsiModifier.STATIC) && seenNames.add(f.getName())) {
                    out.add(f);
                }
            }
            cur = cur.getSuperClass();
        }
        return out;
    }

    private static boolean isBuiltin(@NotNull String fqn, @Nullable PsiClass resolved) {
        // primitive boxed / string / number / Object / Throwable superclasses
        if (fqn.startsWith("java.lang.")) {
            String rest = fqn.substring("java.lang.".length());
            switch (rest) {
                case "String":
                case "Integer": case "Long": case "Short": case "Byte":
                case "Double": case "Float":
                case "Boolean": case "Character": case "Void":
                case "Object": case "Class": case "Enum": case "Number":
                case "Throwable": case "Exception": case "RuntimeException":
                case "Error":
                    return true;
                default:
                    // java.lang.reflect.* / java.lang.annotation.* 都不是数据类
                    if (rest.startsWith("reflect.") || rest.startsWith("annotation.")
                            || rest.startsWith("invoke.") || rest.startsWith("management.")) {
                        return true;
                    }
            }
        }
        if (fqn.startsWith("kotlin.")) return true;
        if (fqn.startsWith("android.")) return true;
        if (fqn.startsWith("androidx.")) return true;
        if (fqn.startsWith("com.google.gson.")) return true;
        if (fqn.startsWith("okhttp3.")) return true;
        if (fqn.startsWith("retrofit2.")) return true;
        if (fqn.startsWith("io.reactivex.")) return true;
        if (fqn.startsWith("org.json.")) return true;
        if (resolved != null && resolved.isEnum()) return true;
        return false;
    }

    private static boolean isSdkOrLibrary(@NotNull String fqn) {
        if (fqn.startsWith("java.") || fqn.startsWith("javax.")) return true;
        if (fqn.startsWith("kotlin.") || fqn.startsWith("kotlinx.")) return true;
        if (fqn.startsWith("android.") || fqn.startsWith("androidx.")) return true;
        if (fqn.startsWith("com.google.android.")) return true;
        if (fqn.startsWith("com.google.gson.")) return true;
        if (fqn.startsWith("okhttp3.") || fqn.startsWith("okio.")) return true;
        if (fqn.startsWith("retrofit2.")) return true;
        if (fqn.startsWith("io.objectbox.")) return true;
        if (fqn.startsWith("com.facebook.")) return true;
        if (fqn.startsWith("com.just.agentweb.")) return true;
        if (fqn.startsWith("org.intellij.")) return true;
        return false;
    }

    private static boolean isContainer(@NotNull String fqn) {
        if (CONTAINER_FQN.contains(fqn)) return true;
        // 其它未枚举的集合/Map：只要是 java.util.* 且继承 Collection/Map 就算
        // 通过 startsWith 避免库类不存在时再解析
        if (fqn.startsWith("java.util.")) {
            if (fqn.contains("Map") || fqn.contains("List") || fqn.contains("Set")
                    || fqn.contains("Queue") || fqn.contains("Deque")) return true;
        }
        return false;
    }

    @NotNull
    private static String safeName(@NotNull PsiClass c) {
        String name = c.getName();
        return name == null ? "<anonymous>" : name;
    }

    private static String fqnOf(@Nullable PsiClass c) {
        return c == null ? null : c.getQualifiedName();
    }

    // 简单的结果结构：第一个缺失字段的类名 + 字段名
    private static final class MissingFieldReport {
        final String className;
        final String fieldName;

        MissingFieldReport(String className, String fieldName) {
            this.className = className;
            this.fieldName = fieldName;
        }
    }
}
