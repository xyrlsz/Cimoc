package com.xyrlsz.xcimoc.source;

import com.google.common.collect.Lists;
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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by xyrlsz on 2025/01/07.
 */

public class Baozi extends MangaParser {

    public static final int TYPE = 101;
    public static final String DEFAULT_TITLE = "包子漫画";
    //    private static String baseUrl = "https://www.baozimh.com";
    private static final String baseUrl = "https://cn.baozimhcn.com";

    public Baozi(Source source) {
        init(source, null);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        if (page != 1) return null;
        String url = baseUrl + "/search?q=" + keyword;
        return new Request.Builder().url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".comics-card")) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text(".comics-card__info > div > h3");
                String author = node.text(".comics-card__info > small");

                String cid = node.href(".comics-card__info").split("/")[2];
                String cover = node.src(".comics-card > a > amp-img");
                return new Comic(TYPE, cid, title, cover, null, author);
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/comic/" + cid;
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("cn.baozimhcn.com", "comic/([\\w\\-]+)"));
        filter.add(new UrlFilter("www.baozimh.com", "comic/([\\w\\-]+)"));
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = baseUrl + "/comic/" + cid;
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text(".comics-detail__title");
        String cover = body.src("div > amp-img");
        String author = body.text(".comics-detail__author");
        String intro = body.text(".comics-detail__desc");
        List<Node> tags = body.list(".tag-list");
        boolean status = false;
        for (Node tag : tags) {
            if (tag.text().equals("已完结") || tag.text().equals("已完結")) {
                status = true;
            }
        }
        String update = body.text("div > span > em");
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        
        List<Node> chapterNodes = body.list(".comics-chapters");
        if (html.contains("章节目录")) {
            chapterNodes = Lists.reverse(chapterNodes);
        } 
        int i = 0;
        Set<String> pathSet = new HashSet<>();
        for (Node chapterNode : chapterNodes) {
            String title = chapterNode.text("div > span");
            String path = chapterNode.href("a").split("chapter_slot=")[1];
            if (pathSet.contains(path)) {
                continue;
            }
            pathSet.add(path);
            Long id = IdCreator.createChapterId(sourceComic, i++);
            list.add(new Chapter(id, sourceComic, title, path));
        }
        return list;
    }


    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("%s/comic/chapter/%s/0_%s.html", baseUrl, cid, path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> imageNodes = body.list("amp-img > noscript");
        for (int i = 1; i <= imageNodes.size(); i++) {
            Long comicChapter = chapter.getId();
            Long id = IdCreator.createImageId(comicChapter, i);
            String imgUrl = imageNodes.get(i - 1).src("img");
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false));
        }

        return list;
    }


    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public Headers getHeader() {
        return Headers.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36");
    }
}
