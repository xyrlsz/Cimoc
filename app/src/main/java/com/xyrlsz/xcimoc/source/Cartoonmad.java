package com.xyrlsz.xcimoc.source;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.RegexIterator;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.UrlFilter;
import com.xyrlsz.xcimoc.soup.Node;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by FEILONG on 2017/12/21.
 */

public class Cartoonmad extends MangaParser {

    public static final int TYPE = 54;
    public static final String DEFAULT_TITLE = "动漫狂";
    private String _cid, _path;

    public Cartoonmad(Source source) {
        init(source, null);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, "https://www.cartoonmad.com");
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        if (page != 1) return null;
        String url = "https://www.cartoonmad.com/search.html";
        RequestBody body = new FormBody.Builder()
                .add("keyword", URLEncoder.encode(keyword, "BIG5"))
                .add("searchtype", "all")
                .build();
        return new Request.Builder().url(url).post(body).addHeader("Referer", "https://www.cartoonmad.com/").build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        // 正则表达式匹配漫画的链接、标题和封面图片
        Pattern pattern = Pattern.compile(
                "<a href=comic/(\\d+)\\.html title=\"(.*?)\"><span class=\"covers\"></span><img src=\"(.*?)\"",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(html);

        return new RegexIterator(matcher) {
            @Override
            protected Comic parse(Matcher match) {
                String cid = match.group(1);  // 漫画ID
                String title = match.group(2);  // 漫画标题
                String cover = "https://www.cartoonmad.com" + match.group(3);  // 封面图片URL
                return new Comic(TYPE, cid, title, cover, "", "");
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return "https://www.cartoonmad.com/comic/".concat(cid).concat(".html");
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("www.cartoonmad.com"));
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "https://www.cartoonmad.com/comic/".concat(cid).concat(".html");
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        Matcher mTitle = Pattern.compile("<meta name=\"Keywords\" content=\"(.*?),").matcher(html);
        String title = mTitle.find() ? mTitle.group(1) : "";
//        Matcher mCover = Pattern.compile("<div class=\"cover\"><\\/div><img src=\"(.*?)\"").matcher(html);
//        String cover = mCover.find() ? "https://www.cartoonmad.com" + mCover.group(1) : "";
        // 匹配封面图片URL
        Matcher mCover = Pattern.compile("<div class=\"cover\"></div>\\s*<img src=\"(.*?)\"").matcher(html);
        String cover = mCover.find() ? "https://www.cartoonmad.com" + mCover.group(1) : "";
        String update = "";
        String author = "";
        Matcher mInro = Pattern.compile("<META name=\"description\" content=\"(.*?)\"").matcher(html);
        String intro = mInro.find() ? mInro.group(1) : "";
        boolean status = false;
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        Matcher mChapter = Pattern.compile("<a href=(.*?) target=_blank>(.*?)<\\/a>&nbsp;").matcher(html);
        int i = 0;
        while (mChapter.find()) {
            String title = mChapter.group(2);
            String path = mChapter.group(1);
            Long id = IdCreator.createChapterId(sourceComic, i++);
            list.add(new Chapter(id, sourceComic, title, path));
        }
        return Lists.reverse(list);
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://www.cartoonmad.com%s", path);
        _cid = cid;
        _path = path;
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new ArrayList<>();
        Matcher pageMatcher = Pattern.compile("<a class=onpage>.*<a class=pages href=(.*)\\d{3}\\.html>(.*?)<\\/a>").matcher(html);
        if (!pageMatcher.find()) return null;
        int page = Integer.parseInt(pageMatcher.group(2));
        for (int i = 1; i <= page; ++i) {
            Long comicChapter = chapter.getId();
            Long id = IdCreator.createImageId(comicChapter, i);
            String url = StringUtils.format("https://cc.fun8.us/post/%s%03d.html", pageMatcher.group(1), i);
            list.add(new ImageUrl(id, comicChapter, i, url, true));
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        return new Request.Builder()
                .addHeader("Referer", url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                .url(url).build();
    }

    @Override
    public String parseLazy(String html, String url) {
        Matcher m = Pattern.compile("<img src=\"(.*?)\" border=\"0\" oncontextmenu").matcher(html);
        if (m.find()) {
            return "https:" + Objects.requireNonNull(m.group(1)).strip();
        }
        return null;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "http://www.cartoonmad.com");
    }

}
