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
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by xyrlsz on 2025/01/17.
 */

public class DuManWuOrg extends MangaParser {

    public static final int TYPE = 105;
    public static final String DEFAULT_TITLE = "读漫屋org";
    private static final String baseUrl = "https://www.dumanwu.org";

    public DuManWuOrg(Source source) {
        init(source);
        setParseImagesUseWebParser(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url;
//        if (page <= 1) {
//            url = baseUrl + "/index.php/search?key=" + keyword;
//        } else {
//            url = StringUtils.format("%s/search/%s/%d", baseUrl, keyword, page);
//        }
        if (page != 1) {
            return null;
        }
        url = baseUrl + "/index.php/search?key=" + keyword;
        return new Request.Builder().url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        List<Node> resList = body.list(".bookList_2 > .item");
        if (resList.isEmpty()) {
            return null;
        }
        return new NodeIterator(resList) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text(".title > a");
                String cover = node.src(".cover");
                String cid = node.href("a").split("/")[2];
                return new Comic(TYPE, cid, title, cover, "", "");
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/comic/" + cid;
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("dumanwu.org", "comic/(\\w+)"));
        filter.add(new UrlFilter("www.dumanwu.org", "comic/(\\w+)"));
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = baseUrl + "/comic/" + cid;
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text(".comicInfo > .info > .title");
        if (title.contains("分")) {
            title = title.substring(title.indexOf("分") + 1);
        }
        String cover = body.src(".comicInfo > .cover > .img > img");
        List<Node> infoList = body.list(".comicInfo > .info > p > span");
        String author = "";
        String status = "";
        for (Node info : infoList) {
            if (info.text().contains("作 者")) {
                author = info.text().split("：")[1];
            } else if (info.text().contains("状 态")) {
                status = info.text().split("：")[1];
            }
        }
        String update = body.text("#chapterList > .topBar > .fr").split("：")[1];
        String intro = body.text(".comicInfo > .info > .content");
        comic.setInfo(title, cover, update, intro, author, status.equals("已完结"));
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        List<Node> chapterNodes = body.list("#chapterlistload > .list > a");

        int i = 0;

        for (Node chapterNode : chapterNodes) {
            String title = chapterNode.text();
            String path = chapterNode.href().split("/")[2].replace(".html", "");
            list.add(new Chapter(null, sourceComic, title, path));
        }

        list = Lists.reverse(list);
        for (int j = 0; j < list.size(); j++) {
            Long id = IdCreator.createChapterId(sourceComic, j);
            list.get(j).setId(id);
        }
        return list;
    }


    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("%s/comic/%s.html", baseUrl, path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> imageNodes = body.list("#cp_img > img");
        for (int i = 1; i <= imageNodes.size(); i++) {
            Long comicChapter = chapter.getId();
            Long id = IdCreator.createImageId(comicChapter, i);
            String imgUrl = imageNodes.get(i - 1).attr("data-src");
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false, getHeader()));
        }
        return list;
    }


    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public Headers getHeader() {
        return Headers.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36"
                , "Referer", "https://www.dumanwu.org");
    }
}
