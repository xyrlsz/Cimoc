package com.xyrlsz.xcimoc.parser;

import android.net.Uri;

import com.xyrlsz.xcimoc.core.Manga;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by Hiroshi on 2016/8/22.
 */
public abstract class MangaParser implements Parser {

    protected String mTitle;
    protected List<UrlFilter> filter = new ArrayList<>();
    private Category mCategory;
    private boolean getSearchUseWebParser = false;
    private boolean parseInfoUseWebParser = false;
    private boolean parseChapterUseWebParser = false;
    private boolean parseImagesUseWebParser = false;
    private boolean parseImagesLazyUseWebParser = false;

    public static Source getDefaultSource() {
        return new Source(null, null, -100, true, null);
    }

    protected void init(Source source, Category category) {
        mTitle = source.getTitle();
        mCategory = category;

        initUrlFilterList();
    }

    protected void init(Source source) {
        mTitle = source.getTitle();
        mCategory = null;

        initUrlFilterList();
    }

    protected void initUrlFilterList() {

    }

    @Override
    public List<Chapter> parseChapter(String html) throws JSONException {
        return null;
    }

    @Override
    public List<ImageUrl> parseImages(String html) throws Manga.NetworkErrorException, JSONException {
        return null;
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        return null;
    }

    @Override
    public Request getLazyRequest(String url) {
        return null;
    }

    @Override
    public String parseLazy(String html, String url) {
        return null;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return null;
    }

    @Override
    public String parseCheck(String html) {
        return null;
    }

    @Override
    public boolean checkUpdateByChapterCount(String html, Comic comic) {
        try {
            Long sourceComic = IdCreator.createSourceComic(comic);
            List<Chapter> list = parseChapter(html, comic, sourceComic);
            if (list == null) {
                list = parseChapter(html);
            }
            return comic.getChapterCount() < list.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Category getCategory() {
        return mCategory;
    }

    @Override
    public Request getCategoryRequest(String format, int page) {
        String url = StringUtils.format(format, page);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        return null;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    protected String[] buildUrl(String path, String[] servers) {
        if (servers != null) {
            String[] url = new String[servers.length];
            for (int i = 0; i != servers.length; ++i) {
                url[i] = servers[i].concat(path);
            }
            return url;
        }
        return null;
    }

    protected boolean isFinish(String text) {
        return text != null && (text.contains("完结") || text.contains("Completed") || text.contains("完結"));
    }

    @Override
    public String getUrl(String cid) {
        return cid;
    }

    @Override
    public Headers getHeader() {
        return null;
    }

    @Override
    public Headers getHeader(String url) {
        return getHeader();
    }

    @Override
    public Headers getHeader(List<ImageUrl> list) {
        return getHeader();
    }

    @Override
    public boolean isHere(Uri uri) {
        boolean val = false;
        for (UrlFilter uf : filter) {
            val |= (Objects.requireNonNull(uri.getHost()).contains(uf.Filter));
        }
        return val;
    }

    @Override
    public String getComicId(Uri uri) {
        for (UrlFilter uf : filter) {
            if (Objects.requireNonNull(uri.getHost()).contains(uf.Filter)) {
                String path = uri.getPath();
                if (uf.getClass() == UrlFilterWithCidQueryKey.class) {
                    return uri.getQueryParameter(((UrlFilterWithCidQueryKey) uf).CidQueryParameterKey);
                }
                if (path != null && path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                String res = StringUtils.match(uf.Regex, path, uf.Group);
                if (res != null) {
                    if (res.startsWith("/")) {
                        res = res.substring(1);
                    }
                    return res;
                }
            }
        }
        return null;
    }

    public boolean isParseImagesUseWebParser() {
        return parseImagesUseWebParser;
    }

    protected void setParseImagesUseWebParser(boolean isUseWebView) {
        this.parseImagesUseWebParser = isUseWebView;
    }

    public boolean isParseChapterUseWebParser() {
        return parseChapterUseWebParser;
    }

    protected void setParseChapterUseWebParser(boolean parseChapterUseWebParser) {
        this.parseChapterUseWebParser = parseChapterUseWebParser;
    }

    public boolean isParseInfoUseWebParser() {
        return parseInfoUseWebParser;
    }

    protected void setParseInfoUseWebParser(boolean parseInfoUseWebParser) {
        this.parseInfoUseWebParser = parseInfoUseWebParser;
    }

    public boolean isGetSearchUseWebParser() {
        return getSearchUseWebParser;
    }

    protected void setGetSearchUseWebParser(boolean getSearchUseWebParser) {
        this.getSearchUseWebParser = getSearchUseWebParser;
    }

    public boolean isParseImagesLazyUseWebParser() {
        return parseImagesLazyUseWebParser;
    }

    public void setParseImagesLazyUseWebParser(boolean parseImagesLazyUseWebParser) {
        this.parseImagesLazyUseWebParser = parseImagesLazyUseWebParser;
    }
}
