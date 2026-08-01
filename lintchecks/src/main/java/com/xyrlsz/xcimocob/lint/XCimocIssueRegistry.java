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
 * R8 打包后的完整性验证由 Gradle 任务 {@code checkR8KeepRules}
 * 通过解析 {@code seeds.txt} 完成。
 * <p>
 * 本 Lint 模块覆盖以下模式：
 * <ul>
 *   <li>R8Reflection - 检测反射 API（Class.forName、Method.invoke、Field.set 等）</li> *   <li>R8LibraryReflection - 检测【依赖库字节码】中的反射 API（需开启 checkDependencies）</li> *   <li>R8DynamicProxy - 检测动态代理（Proxy.newProxyInstance）</li>
 *   <li>R8JavascriptInterface - 检测 WebView @JavascriptInterface 注解</li>
 *   <li>R8InnerClassReflection - 检测 Class.forName 访问内部类（需 InnerClasses 属性）</li>
 *   <li>R8MissingSerializedName - 检测 Gson 数据类缺少 @SerializedName 注解</li>
 *   <li>R8Serializable - 检测 Serializable 实现类</li>
 * </ul>
 * 注意：Parcelable、ObjectBox @Entity 无需手动 keep 规则，因此不纳入检测。
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
                LibraryReflectionDetector.ISSUE_LIBRARY_REFLECTION,
                ReflectionDetector.ISSUE_DYNAMIC_PROXY,
                ReflectionDetector.ISSUE_JAVASCRIPT_INTERFACE,
                ReflectionDetector.ISSUE_INNER_CLASS_REFLECTION,
                SerializationDetector.ISSUE_MISSING_SERIALIZED_NAME,
                ClassLevelDetector.ISSUE_SERIALIZABLE
        );
    }

    @Override
    public int getApi() {
        return ApiKt.CURRENT_API;
    }
}
