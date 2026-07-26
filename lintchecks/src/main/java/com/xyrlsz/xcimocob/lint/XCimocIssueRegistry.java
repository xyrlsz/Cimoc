package com.xyrlsz.xcimocob.lint;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.Vendor;
import com.android.tools.lint.detector.api.ApiKt;
import com.android.tools.lint.detector.api.Issue;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * 自定义 Lint 检查注册表 - XCimoc R8 Keep 规则检查
 * <p>
 * 本模块在编译期检测代码中需要 R8 Keep 规则的场景。
 * 类级别的检测（ObjectBox @Entity、Parcelable、Serializable、JNI native 方法、
 * 运行时注解等）由 Gradle 任务 {@code checkR8KeepRules} 在 R8 打包后，
 * 通过解析 {@code seeds.txt} 完成验证。
 * <p>
 * 本 Lint 模块聚焦于开发阶段能静态检测的编解码模式：
 * <ul>
 *   <li>R8Reflection - 检测反射 API 的使用（Class.forName, Method.invoke 等）</li>
 *   <li>R8DynamicProxy - 检测动态代理调用</li>
 *   <li>R8GsonUsage - 检测 Gson toJson/fromJson 调用</li>
 * </ul>
 * 详见项目根目录的 {@code check_r8_keep.gradle} 脚本。
 */
public final class XCimocIssueRegistry extends IssueRegistry {

    @NotNull
    @Override
    public Vendor getVendor() {
        return new Vendor(
                "XCimoc",
                null,
                "https://github.com/xyrlsz/XCimoc",
                null
        );
    }

    @NotNull
    @Override
    public List<Issue> getIssues() {
        return Arrays.asList(
                ReflectionDetector.ISSUE_REFLECTION_API,
                ReflectionDetector.ISSUE_DYNAMIC_PROXY,
                SerializationDetector.ISSUE_GSON,
                ClassLevelDetector.ISSUE_PARCELABLE,
                ClassLevelDetector.ISSUE_SERIALIZABLE,
                ClassLevelDetector.ISSUE_OBJECTBOX
        );
    }

    @Override
    public int getApi() {
        return ApiKt.CURRENT_API;
    }
}
