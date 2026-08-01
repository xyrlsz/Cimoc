package com.xyrlsz.xcimocob.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 检测【依赖库 / 编译产物】字节码中的反射 API 调用，提醒添加 R8 Keep 规则。
 * <p>
 * 本项目自己的源码由 {@link ReflectionDetector}（UAST 源码级）负责；
 * 但 Fresco 等第三方库内部的反射（如 {@code Class.forName("...")} + {@code getConstructor(...)}）
 * 写在其 AAR 的字节码里，源码级检测看不到。本检测器通过 ASM 分析
 * 项目 class 文件（{@link Scope#CLASS_FILE_SCOPE}）与所有依赖库（{@link Scope#JAVA_LIBRARIES}）
 * 的字节码，抓出反射目标，避免 R8 把它们当无用代码裁剪。
 * <p>
 * 结果过滤（减少噪音，聚焦真正需要 keep 的目标）：
 * <ol>
 *   <li>平台类/框架类（android.*、java.*、javax.*、sun.*、libcore.*、dalvik.*、com.android.*）
 *       及纯特性检测类（kotlin.*、kotlinx.*、GAE 等）—— keep 规则管不到框架类，直接跳过；</li>
 *   <li>已被主工程 {@code proguard-rules.pro} 中 {@code -keep} 规则覆盖的类 —— 跳过。</li>
 * </ol>
 * 需在 app/build.gradle 的 lint 块开启 {@code checkDependencies = true} 才会分析 AAR。
 */
public class LibraryReflectionDetector extends Detector implements Detector.ClassScanner {

    private static final String DETAIL =
            "在依赖库/编译产物的字节码中检测到反射 API 调用。R8 无法静态追踪反射动态加载的类，" +
            "被反射的目标类、构造器、方法可能在混淆/裁剪时被移除，运行时抛出 " +
            "ClassNotFoundException 或 NoSuchMethodException。\n\n" +
            "对于 `Class.forName(\"xxx\")`，请确认目标类 xxx 已有 Keep 规则：\n" +
            "```\n" +
            "-keep class xxx { *; }\n" +
            "```\n" +
            "第三方库通常自带 consumer ProGuard 规则，但存在覆盖不全的情况（如 Fresco 的 " +
            "animated-webp 模块没有自带规则）；若运行时仍报错，需要显式补充 Keep 规则。\n\n" +
            "本检测已自动过滤：平台/框架类（keep 管不到）以及已被本项目 proguard-rules.pro " +
            "中 `-keep` 规则覆盖的反射目标。\n\n" +
            "若反射目标为内部类（含 `$`），还需保留：\n" +
            "```\n" +
            "-keepattributes InnerClasses, EnclosingMethod\n" +
            "```";

    public static final Issue ISSUE_LIBRARY_REFLECTION = Issue.create(
            "R8LibraryReflection",
            "依赖库中使用了反射 API，可能需要 R8 Keep 规则",
            DETAIL,
            Category.CORRECTNESS, 8, Severity.WARNING,
            new Implementation(
                    LibraryReflectionDetector.class,
                    EnumSet.of(Scope.CLASS_FILE, Scope.JAVA_LIBRARIES)
            )
    );

    /**
     * 平台/框架类与纯特性检测类：R8 keep 规则管不到（框架类由系统提供，或类根本不在 APK 中），
     * 上报只会产生噪音，直接跳过。
     */
    private static final List<String> SKIP_PREFIXES = Arrays.asList(
            "android.", "java.", "javax.", "sun.", "libcore.", "dalvik.", "com.android.",
            "kotlin.", "kotlinx.",
            "com.google.appengine.", "com.google.apphosting."
    );

    @Nullable
    private KeepRuleMatcher keepRuleMatcher;

    @Nullable
    @Override
    public int[] getApplicableAsmNodeTypes() {
        // 不使用逐指令扫描，只通过 getApplicableCallNames/Owners 过滤方法调用
        return null;
    }

    @Nullable
    @Override
    public List<String> getApplicableCallNames() {
        // 只检测 Class.forName("<字面量>")：这是 R8 唯一"看不见"的类加载方式
        // （.class 字面量、getConstructor 等直接引用 R8 都能追踪到，无需 keep）
        return Collections.singletonList("forName");
    }

    @Nullable
    @Override
    public List<String> getApplicableCallOwners() {
        return Collections.singletonList("java/lang/Class");
    }

    @Override
    public void checkCall(@NotNull ClassContext context,
                          @NotNull ClassNode classNode,
                          @NotNull MethodNode methodNode,
                          @NotNull MethodInsnNode call) {
        // 仅处理单参 Class.forName(String) 形式（Fresco 等库的常见用法）
        if (!"(Ljava/lang/String;)Ljava/lang/Class;".equals(call.desc)) {
            return;
        }
        String target = extractPreviousLdc(call);
        if (target == null) {
            return; // 参数非字面量（运行时变量），无法给出精确建议
        }

        // 过滤 1：平台类/特性检测类 —— keep 管不到，跳过
        if (isSkippedPrefix(target)) {
            return;
        }
        // 过滤 2：已被本项目 proguard-rules.pro 的 -keep 规则覆盖 —— 跳过
        if (getKeepRuleMatcher(context).matches(target)) {
            return;
        }

        String libName = classNode.name.replace('/', '.');
        String message = "依赖库 " + libName + " 通过 Class.forName(\"" + target
                + "\") 反射加载类 —— R8 无法看到该加载，若裁剪该类将抛 "
                + "ClassNotFoundException；建议确认已有 Keep 规则: -keep class "
                + target + " { *; }";
        context.report(ISSUE_LIBRARY_REFLECTION, methodNode, call,
                context.getLocation(call), message);
    }

    @Override
    public void checkInstruction(@NotNull ClassContext context,
                                 @NotNull ClassNode classNode,
                                 @NotNull MethodNode methodNode,
                                 @NotNull AbstractInsnNode node) {
        // 未启用逐指令扫描，无需处理
    }

    @Override
    public void checkClass(@NotNull ClassContext context, @NotNull ClassNode classNode) {
        // 无需处理
    }

    private static boolean isSkippedPrefix(String className) {
        for (String prefix : SKIP_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private KeepRuleMatcher getKeepRuleMatcher(@NotNull ClassContext context) {
        if (keepRuleMatcher == null) {
            keepRuleMatcher = new KeepRuleMatcher(readProguardClassPatterns(context));
        }
        return keepRuleMatcher;
    }

    /**
     * 读取【主应用模块】配置的所有 proguard 文件（proguard-rules.pro、默认
     * proguard-android-optimize.txt 等）中的 {@code -keep} 类名模式。
     * <p>
     * 注意：不能使用 {@code Context.getMainProject()}（lint-api 31 / AGP 9 下会抛
     * LintError），改为遍历 {@code LintDriver.getProjects()} 找到非 library 的
     * Android 主模块，再读其 {@code Project.getProguardFiles()}。
     * 只认普通 {@code -keep}（含 -keep,allowobfuscation 等变体），
     * 不认 {@code -keepclassmembers} / {@code -keepclasseswithmembers}（条件式，会误伤）。
     */
    private static List<String> readProguardClassPatterns(@NotNull ClassContext context) {
        List<String> patterns = new ArrayList<>();
        try {
            for (Project p : context.getDriver().getProjects()) {
                if (p.isAndroidProject() && !p.isLibrary()) {
                    List<File> files = p.getProguardFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f != null && f.isFile()) {
                                readPatternsFrom(f, patterns);
                            }
                        }
                    }
                    break;
                }
            }
        } catch (Exception ignored) {
            // 过滤是尽力而为，读取失败则不过滤
        }
        return patterns;
    }

    private static void readPatternsFrom(@NotNull File file, @NotNull List<String> patterns)
            throws java.io.IOException {
        for (String raw : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!line.startsWith("-keep")
                    || line.startsWith("-keepattributes")
                    || line.startsWith("-keepnames")
                    || line.startsWith("-keepclass")) { // keepclasseswithmembers/keepclassmembers
                continue;
            }
            String pattern = extractClassPattern(line);
            if (pattern != null && !pattern.isEmpty()) {
                patterns.add(pattern);
            }
        }
    }

    /** 提取 "-keep [修饰符] class <类名模式>" 中的类名模式 */
    @Nullable
    private static String extractClassPattern(String line) {
        int idx = indexOfWord(line, "class");
        if (idx < 0) {
            return null;
        }
        String rest = line.substring(idx + "class".length()).trim();
        int end = 0;
        while (end < rest.length() && !Character.isWhitespace(rest.charAt(end))
                && rest.charAt(end) != '{' && rest.charAt(end) != ';') {
            end++;
        }
        if (end == 0) {
            return null;
        }
        String spec = rest.substring(0, end);
        if (spec.endsWith("[]")) {
            spec = spec.substring(0, spec.length() - 2);
        }
        return spec;
    }

    private static int indexOfWord(String line, String word) {
        int from = 0;
        while (true) {
            int idx = line.indexOf(word, from);
            if (idx < 0) {
                return -1;
            }
            boolean prevOk = idx == 0 || Character.isWhitespace(line.charAt(idx - 1));
            int after = idx + word.length();
            boolean nextOk = after >= line.length()
                    || Character.isWhitespace(line.charAt(after))
                    || line.charAt(after) == '{' || line.charAt(after) == ';';
            if (prevOk && nextOk) {
                return idx;
            }
            from = idx + 1;
        }
    }

    /** 提取紧邻前一条 ldc 常量字符串（对应 Class.forName("...") 的参数） */
    @Nullable
    private static String extractPreviousLdc(MethodInsnNode call) {
        AbstractInsnNode prev = call.getPrevious();
        if (prev instanceof LdcInsnNode) {
            Object cst = ((LdcInsnNode) prev).cst;
            if (cst instanceof String) {
                return (String) cst;
            }
        }
        return null;
    }

    /** ProGuard 类名模式匹配器：支持 ** / * / ? 通配符，内部类（$）回退到外层类匹配 */
    private static final class KeepRuleMatcher {
        private final List<Pattern> patterns = new ArrayList<>();

        KeepRuleMatcher(List<String> classPatterns) {
            for (String p : classPatterns) {
                try {
                    patterns.add(compile(p));
                } catch (Exception ignored) {
                    // 忽略非法模式
                }
            }
        }

        private static Pattern compile(String p) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            while (i < p.length()) {
                char c = p.charAt(i);
                if (c == '*') {
                    if (i + 1 < p.length() && p.charAt(i + 1) == '*') {
                        sb.append(".*");
                        i += 2;
                    } else {
                        sb.append("[^.]*");
                        i++;
                    }
                } else if (c == '?') {
                    sb.append('.');
                    i++;
                } else {
                    sb.append(Pattern.quote(String.valueOf(c)));
                    i++;
                }
            }
            return Pattern.compile(sb.toString());
        }

        /** 目标类或其外层类（去掉 $ 后缀）命中任一模式即视为已覆盖 */
        boolean matches(String className) {
            String name = className;
            while (name != null) {
                for (Pattern p : patterns) {
                    if (p.matcher(name).matches()) {
                        return true;
                    }
                }
                int idx = name.indexOf('$');
                if (idx < 0) {
                    break;
                }
                name = name.substring(0, idx);
            }
            return false;
        }
    }
}
