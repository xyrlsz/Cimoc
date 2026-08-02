package com.xyrlsz.xcimocob.parser;

/**
 * 单个解析环节（搜索 / 详情 / 章节 / 图片 / 惰性图片）使用 WebView 渲染抓取时的配置。
 *
 * <p>搜索 / 详情 / 章节 / 图片 / 惰性图片 五个环节（对应
 * {@link MangaParser#getSearchConfig()} / {@link MangaParser#getInfoConfig()} /
 * {@link MangaParser#getChapterConfig()} / {@link MangaParser#getImagesConfig()} /
 * {@link MangaParser#getImagesLazyConfig()}）均可独立配置：
 * <ul>
 *     <li>{@link #useWebParser}：是否使用 WebView 渲染页面后再解析，用于需要执行 JS 才能拿到完整内容的网站；</li>
 *     <li>{@link #autoScroll}：渲染完成后是否自动向下滑动页面以触发懒加载，获取完整 HTML；</li>
 *     <li>{@link #injectJs}：DOM 就绪后注入并执行的自定义 JS 脚本（例如点击按钮展开内容）。</li>
 *     <li>{@link #handleCloudflare}：是否自动处理 Cloudflare 认证挑战（纯 JS 挑战自动等待通过，交互式验证需手动完成）；</li>
 *     <li>{@link #cloudflareTimeoutMs}：Cloudflare JS 挑战等待通过的超时时间。</li>
 *     <li>{@link #interactiveChallenge}：交互式验证时是否把 WebView 显示到前台由用户手动完成。</li>
 * </ul>
 *
 * <p>{@link #autoScroll} 仅在 {@link #useWebParser} 为 {@code true} 时生效，默认开启以保持原有行为。
 * 链式调用示例：
 * <pre>{@code
 * getSearchConfig().setUseWebParser(true).setAutoScroll(false);
 * getInfoConfig().setUseWebParser(true).setInjectJs("javascript:...").setAutoScroll(true);
 * getImagesConfig().setUseWebParser(true).setInjectJs("javascript:...");
 * // 站点被 Cloudflare 拦截时可调整挑战等待超时；交互式验证需手动完成时开启
 * getInfoConfig().setHandleCloudflare(true).setCloudflareTimeoutMs(60_000).setInteractiveChallenge(true);
 * }</pre>
 */
public class WebParserConfig {

    private boolean useWebParser = false;

    /**
     * 是否自动滑动。默认 {@code true}，保持与旧版一致的行为。
     */
    private boolean autoScroll = true;

    /**
     * DOM 就绪后注入执行的自定义 JS，可为 {@code null}。
     */
    private String injectJs = null;

    /**
     * 是否处理 Cloudflare 认证，默认 {@code true}。仅在页面命中 Cloudflare 挑战页时生效：
     * 纯 JS 挑战会自动等待其通过（最长 {@link #cloudflareTimeoutMs}），交互式验证则直接报错。
     */
    private boolean handleCloudflare = true;

    /**
     * Cloudflare JS 挑战等待通过的超时（毫秒），默认 30 秒。
     */
    private long cloudflareTimeoutMs = 30_000;

    /**
     * 是否允许交互式 Cloudflare 验证：将 WebView 显示到前台由用户手动点击"验证您是真人"后继续。
     * 默认 {@code false}（交互式验证直接报错）。仅当 {@link #handleCloudflare} 为 {@code true} 时生效。
     */
    private boolean interactiveChallenge = false;

    public WebParserConfig() {
    }

    public WebParserConfig(boolean useWebParser, boolean autoScroll) {
        this.useWebParser = useWebParser;
        this.autoScroll = autoScroll;
    }

    public WebParserConfig(boolean useWebParser, boolean autoScroll, String injectJs) {
        this.useWebParser = useWebParser;
        this.autoScroll = autoScroll;
        this.injectJs = injectJs;
    }

    public boolean isUseWebParser() {
        return useWebParser;
    }

    public WebParserConfig setUseWebParser(boolean useWebParser) {
        this.useWebParser = useWebParser;
        return this;
    }

    public boolean isAutoScroll() {
        return autoScroll;
    }

    public WebParserConfig setAutoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
        return this;
    }

    public String getInjectJs() {
        return injectJs;
    }

    public WebParserConfig setInjectJs(String injectJs) {
        this.injectJs = injectJs;
        return this;
    }

    public boolean isHandleCloudflare() {
        return handleCloudflare;
    }

    public WebParserConfig setHandleCloudflare(boolean handleCloudflare) {
        this.handleCloudflare = handleCloudflare;
        return this;
    }

    public long getCloudflareTimeoutMs() {
        return cloudflareTimeoutMs;
    }

    public WebParserConfig setCloudflareTimeoutMs(long cloudflareTimeoutMs) {
        this.cloudflareTimeoutMs = cloudflareTimeoutMs;
        return this;
    }

    public boolean isInteractiveChallenge() {
        return interactiveChallenge;
    }

    public WebParserConfig setInteractiveChallenge(boolean interactiveChallenge) {
        this.interactiveChallenge = interactiveChallenge;
        return this;
    }
}
