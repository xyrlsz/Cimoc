package com.xyrlsz.xcimocob.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Unique;

/**
 * 动态 JS 漫画源。脚本内容（{@link #getScript()}）在 App 的 QuickJS 引擎中执行，
 * 通过 GitHub raw API 在线增量更新（见 {@code JsSourceManager#updateFromServer}）。
 * <p>
 * 与内置 {@link Source} 表用 {@code type} 关联：启用 JS 源时覆盖内置实现。
 */
@Entity
public class JsSource {

    @Id(assignable = true)
    private long id;

    /**
     * 源类型，与内置源 {@link Source#getType()} 对应；type >= 0 合法（0 为漫画柜）。
     */
    @Unique
    private int type;

    private String title;

    /**
     * 源脚本版本号（来自 index.json），用于增量更新判断。
     */
    private String version;

    private String baseUrl;

    /**
     * 该源匹配的 host 列表（逗号分隔），用于 URL 识别 / 分享。
     */
    private String hosts;

    /**
     * 从 URL 提取 cid 的正则（可空）。
     */
    private String cidRegex;

    /**
     * cid 位于 URL query 参数时的参数名（可空，如 zaimanhua 的 id）。
     */
    private String cidQuery;

    /**
     * 脚本原始下载地址（GitHub raw）。
     */
    private String sourceUrl;

    /**
     * JS 源脚本全文（含 SOURCE 元数据与各解析函数）。
     */
    private String script;

    private boolean enable;

    /**
     * 脚本是否声明了登录能力（login/getLoginState）。由 validateScript 计算并缓存，避免列表加载时逐源跑 JS。
     */
    private boolean hasLogin;

    /**
     * 脚本声明的设置项数量（getSettings 返回数组长度）。
     */
    private int settingCount;

    /**
     * 脚本声明的设置项 JSON（getSettings 返回的字段描述数组原文），供设置页直接渲染，无需再跑 JS。
     */
    private String settingsJson;

    /**
     * hasLogin/settingCount/settingsJson 是否已计算并缓存。存量源首次需回填。
     */
    private boolean metaReady;

    public JsSource() {
        // id 必须为 0：ObjectBox 对 @Id(assignable=true) 且 id<=0 的实体自动分配唯一 id；
        // 若用负数（如 -2）会被当作固定 id，多次 put 会写入同一行互相覆盖。
        this.id = 0;
        this.type = -1;
        this.title = "unknow";
        this.version = "";
        this.baseUrl = "";
        this.hosts = "";
        this.cidRegex = "";
        this.cidQuery = "";
        this.sourceUrl = "";
        this.script = "";
        this.enable = false;
        this.hasLogin = false;
        this.settingCount = 0;
        this.settingsJson = "";
        this.metaReady = false;
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public String getCidRegex() {
        return cidRegex;
    }

    public void setCidRegex(String cidRegex) {
        this.cidRegex = cidRegex;
    }

    public String getCidQuery() {
        return cidQuery;
    }

    public void setCidQuery(String cidQuery) {
        this.cidQuery = cidQuery;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isHasLogin() {
        return hasLogin;
    }

    public void setHasLogin(boolean hasLogin) {
        this.hasLogin = hasLogin;
    }

    public int getSettingCount() {
        return settingCount;
    }

    public void setSettingCount(int settingCount) {
        this.settingCount = settingCount;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }

    public boolean isMetaReady() {
        return metaReady;
    }

    public void setMetaReady(boolean metaReady) {
        this.metaReady = metaReady;
    }
}
