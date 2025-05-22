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
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by xyrlsz on 2025/02/13.
 */

public class Manhuayu extends MangaParser {
    public static final int TYPE = 107;
    public static final String DEFAULT_TITLE = "漫画鱼";
    private static final String baseUrl = "https://www.manhuayu8.com";

    public Manhuayu(Source source) {
        init(source, null);
        setParseImagesUseWebParser(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    protected void initUrlFilterList() {
        super.initUrlFilterList();
        filter.add(new UrlFilter("manhuayu.com"));
        filter.add(new UrlFilter("manhuayu8.com"));
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws Exception {
        if (page == 1) {
            String url = baseUrl + "/search?q=" + keyword;
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        Node body = new Node(html);
        List<Node> resList = body.list("div.media");
        if (resList.isEmpty()) {
            return null;
        }
        return new NodeIterator(resList) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text(".media-content > a.title");
                String cover = node.attr(".media-left > a", "data-original");
                String cid = node.href(".media-content > a.title").replace("/", "");
                return new Comic(TYPE, cid, title, cover, "", "");
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        return new Request.Builder().url(baseUrl + "/" + cid).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException, JSONException {
        Node body = new Node(html);
        String title = body.text(".metas-title");
        String cover = body.src(".metas-image > img");
        String author = null;
        boolean status = true;
        for (Node node : body.list(".metas-body > .author")) {
            String tmp = node.text();
            if (tmp.contains("作者")) {
                author = tmp.replace("作者：", "").strip();
            } else if (tmp.contains("连载")) {
                status = false;
            }
        }
        String update = body.text(".metas-body > .has-text-danger");
        String intro = body.text(".metas-desc > p");
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        List<Node> chapterNodes = body.list("ul.comic-chapters > li > a");

        int i = 0;

        for (Node chapterNode : chapterNodes) {
            String title = chapterNode.text();
            String path = chapterNode.href().split("/")[2].replace(".html", "");
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i++), sourceComic, title, path));
        }

        return Lists.reverse(list);
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return new Request.Builder().url(StringUtils.format("%s/%s/%s.html", baseUrl, cid, path)).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        List<ImageUrl> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> imageNodes = body.list(".chapter-images > .chapter-image");
        for (int i = 1; i <= imageNodes.size(); i++) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "0" + i);
            String imgUrl = imageNodes.get(i - 1).attr("data-original");
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false));
        }
        return list;
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/" + cid;
    }

    @Override
    public Headers getHeader() {
        return new Headers.Builder()
                .add("referer", baseUrl + "/")
                .add("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0")
                .build();
    }
}
