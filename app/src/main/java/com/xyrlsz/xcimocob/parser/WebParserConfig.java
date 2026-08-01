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
 * </ul>
 *
 * <p>{@link #autoScroll} 仅在 {@link #useWebParser} 为 {@code true} 时生效，默认开启以保持原有行为。
 * 链式调用示例：
 * <pre>{@code
 * getSearchConfig().setUseWebParser(true).setAutoScroll(false);
 * getInfoConfig().setUseWebParser(true).setInjectJs("javascript:...").setAutoScroll(true);
 * getImagesConfig().setUseWebParser(true).setInjectJs("javascript:...");
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
}
