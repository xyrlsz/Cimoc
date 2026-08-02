package com.xyrlsz.xcimocob.source;

import android.os.Build;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimocob.core.Manga;
import com.xyrlsz.xcimocob.model.Chapter;
import com.xyrlsz.xcimocob.model.Comic;
import com.xyrlsz.xcimocob.model.ImageUrl;
import com.xyrlsz.xcimocob.model.Source;
import com.xyrlsz.xcimocob.parser.MangaParser;
import com.xyrlsz.xcimocob.parser.NodeIterator;
import com.xyrlsz.xcimocob.parser.SearchIterator;
import com.xyrlsz.xcimocob.parser.UrlFilter;
import com.xyrlsz.xcimocob.soup.Node;
import com.xyrlsz.xcimocob.utils.IdCreator;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

public class ManHuaZhiJia extends MangaParser {
    public static final int TYPE = 119;
    public static final String DEFAULT_TITLE = "漫画之家";
    private static final String baseUrl = "https://www.manhuahome.com";

    public ManHuaZhiJia(Source source) {
        init(source);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    protected void initUrlFilterList() {
        super.initUrlFilterList();
        filter.add(new UrlFilter("manhuahome.com", "(/book/\\d+\\.html)", 1));

    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException, Exception {
        if (page == 1) {
            String url = "https://www.manhuahome.com/search.html?wd=" + keyword + "&page=" + page;
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {

        List<Node> nodes = new Node(html).list("div.mg-search-item");
        return new NodeIterator(nodes) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text(".mg-search-name");
                String cover = baseUrl + node.attr(".mg-search-thumb", "data-original");
                String cid = node.href(".mg-search-name > a");
                return new Comic(TYPE, cid, title, cover, "", "");
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + cid;
    }

    @Override
    public Request getInfoRequest(String cid) {
        if (!cid.startsWith("/")) {
            cid = "/".concat(cid);
        }
        return new Request.Builder()
                .url(getUrl(cid))
                .build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException, JSONException {
        Node body = new Node(html);
        String title = body.text(".mg-detail-title");
        String cover = baseUrl + body.src(".mg-banner-cover");
        String author = null;
        boolean status = false;
        String update = "";
        for (Node node : body.list(".mg-detail-meta> li")) {
            String tmp = node.text();
            if (tmp.contains("作者")) {
                author = tmp.replace("作者：", "").strip();
            } else if (tmp.contains("上架时间")) {
                update = tmp.replace("上架时间：", "").strip();
            }
        }

        String intro = body.text(".mg-blurb-text");
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        List<Node> chapterNodes = body.list(".mg-chapter-list > li");

        for (Node chapterNode : chapterNodes) {
            String title = chapterNode.text();
            String path = chapterNode.href("a");
            list.add(new Chapter(null, sourceComic, title, path));
        }

        list = Lists.reverse(list);
        for (int j = 0; j < list.size(); j++) {
            long id = IdCreator.createChapterId(sourceComic, j);
            list.get(j).setId(id);
        }
        return list;

    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return new Request.Builder().url(baseUrl + path).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter)
            throws Manga.NetworkErrorException, JSONException {

        List<ImageUrl> list = new LinkedList<>();
        // 提取 player_aaaa 里面的 url
        Pattern pattern = Pattern.compile(
                "\"url\"\\s*:\\s*\"(/[^\"]+\\.jpg(?:\\|\\|\\|/[^\"]+)*?)\"",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(html);
        if (!matcher.find()) {
            return list;
        }
        String urlValue = matcher.group(1);
        if (urlValue == null || urlValue.isEmpty()) {
            return list;
        }
        // 分割图片
        String[] imageList = urlValue.split("\\|\\|\\|");
        long comicChapter = chapter.getId();
        for (int i = 0; i < imageList.length; i++) {
            String img = imageList[i].trim();
            if (img.isEmpty()) {
                continue;
            }
            // 补全域名
            if (img.startsWith("/")) {
                img = baseUrl + img;
            }
            long id = IdCreator.createImageId(
                    comicChapter,
                    i
            );
            list.add(new ImageUrl(id, comicChapter, i, img, false, Headers.of("Referer", baseUrl + chapter.getPath())));
        }

        return list;
    }
}
