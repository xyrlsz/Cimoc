package com.xyrlsz.xcimoc.source;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimoc.core.Manga;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.NodeIterator;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.UrlFilter;
import com.xyrlsz.xcimoc.soup.Node;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class MH5 extends MangaParser {
    public static final int TYPE = 116;
    public static final String DEFAULT_TITLE = "漫画屋";
    private static final String baseUrl = "https://mh5.app";
    private static final String UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36";

    public MH5(Source source) {
        init(source);
        setParseImagesUseWebParser(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws Exception {
        if (page != 1) {
            return null;
        }
        String url = baseUrl + "/index.php/search?key=" + keyword;
        return new Request.Builder()
                .url(url)
                .addHeader("user-agent", UA)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        Node body = new Node(html);
        List<Node> resList = body.list("ul.list-comic-book > li");
        if (resList.isEmpty()) {
            return null;
        }
        return new NodeIterator(resList) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text(".comic-info > h2");
                String cover = node.attr(".comic-book > img", "data-src");
                String cid = node.href("a")
                        .replace("/", "");
                String update = node.text(".comic-book > p.heat");
                return new Comic(TYPE, cid, title, cover, update, "");
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/" + cid;
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("mh5.app", "/([a-zA-Z0-9]+)"));
    }

    @Override
    public Request getInfoRequest(String cid) {
        return new Request.Builder()
                .url(getUrl(cid))
                .addHeader("user-agent", UA)
                .build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException, JSONException {
        Node body = new Node(html);
        String title = body.text(".detail-title");
        String cover = body.attr(".banner-img > img", "data-src");
        String author = body.text("p.author");
        String intro = body.text(".detail-desc");
        String update = body.text(".detail-info-btips .tips:nth-child(1) b");
        comic.setInfo(title, cover, update, intro, author, isFinish(html));
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        int i = 0;
        List<Node> chapterNodes = Lists.reverse(body.list(".chapter-list > .item > a"));
        for (Node node : chapterNodes) {
            String title = node.text();
            String path = node.href();
            Long id = IdCreator.createChapterId(sourceComic, i++);
            list.add(new Chapter(id, sourceComic, title, path));
        }

        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("%s/%s", baseUrl, path);
        return new Request.Builder()
                .addHeader("user-agent", UA)
                .url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        List<ImageUrl> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> imageNodes = body.list("img.lazy-read");
        for (int i = 1; i <= imageNodes.size(); i++) {
            Long comicChapter = chapter.getId();
            Long id = IdCreator.createImageId(comicChapter, i);
            String imgUrl = imageNodes.get(i - 1).attr("data-src");
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false, getHeader()));
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", baseUrl.concat("/"), "user-agent", UA);
    }
}
