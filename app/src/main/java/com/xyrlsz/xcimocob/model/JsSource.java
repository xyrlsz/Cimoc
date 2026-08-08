package com.xyrlsz.xcimocob.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;

/**
 * 动态 JS 漫画源（方案三：基于 QuickJS 的脚本源）。
 *
 * <p>脚本内容（{@link #getScript()}）是完整的 JS 源文件，遵循
 * {@code docs/js-source.md} 中定义的脚本规范；由 {@code JsMangaParser}
 * 在 QuickJS 引擎中解释执行。脚本可通过远程源仓库在线更新，无需重新发版。
 */
@Entity
public class JsSource {
    @Id(assignable = true)
    private long id;
    /**
     * 漫画源类型号，与内置源的 {@code TYPE} 同一命名空间。
     * 与内置源同 {@code type} 时，启用后将覆盖内置实现。
     */
    @Unique
    private int type;
    private String title;
    /**
     * 脚本版本号（由源仓库 manifest 提供），用于增量更新判断。
     */
    private String version;
    /**
     * 完整的 JS 脚本源码。
     */
    private String script;
    private String baseUrl;
    private boolean enable;
    /**
     * 最近一次更新时间（epoch millis）。
     */
    private long updatedAt;

    public JsSource() {
    }

    public JsSource(long id, int type, String title, String version, String script,
                    String baseUrl, boolean enable, long updatedAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.version = version;
        this.script = script;
        this.baseUrl = baseUrl;
        this.enable = enable;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean getEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
