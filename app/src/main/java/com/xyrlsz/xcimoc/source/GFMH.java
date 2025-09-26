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

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class GFMH extends MangaParser {
    public static final int TYPE = 114;
    public static final String DEFAULT_TITLE = "古风漫画";
    private static final String baseUrl = "https://www.gfmh.app";

    public GFMH(Source source) {
        init(source);
        setParseImagesUseWebParser(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/" + cid.concat(".html");
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("www.gfmh.app"));
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException, Exception {
        if (page == 1) {
            String url = baseUrl + "/index.php/search?key=" + keyword;
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        Node root = new Node(html);
        return new NodeIterator(root.list("ul.flex > li.searchresult")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.href("div > a").substring(1).replace(".html", "");
                String cover = node.attr("div.img_span > img", "data-original");
                String title = node.text("div > a > h3");
                return new Comic(TYPE, cid, title, cover, "", "");
            }
        };
    }


    @Override
    public Request getInfoRequest(String cid) {
        String url = baseUrl + "/" + cid.concat(".html");
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException, JSONException {
        Node body = new Node(html);
        String title = body.text(".novel_info_title > h1");
        String cover = body.src(".novel_info_main > img");
        String author = body.text(".novel_info_title > i").replace("作者：", "");
        String update = body.text("em.s_gray");
        String intro = body.text(".intro");
        comic.setInfo(title, cover, update, intro, author, isFinish(html));
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        List<Node> chapterNodes = body.list("ul#ul_all_chapters > li > a");
        chapterNodes = Lists.reverse(chapterNodes);
        int i = 0;
        for (Node chapterNode : chapterNodes) {
            String title = chapterNode.text();
            String path = chapterNode.href().split("/")[2].replace(".html", "");
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i++), sourceComic, title, path));
        }

        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("%s/%s/%s.html", baseUrl, cid, path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> imageNodes = body.list("#contents > .lazy-read");
        for (int i = 1; i <= imageNodes.size(); i++) {
            Long comicChapter = chapter.getId();
            Long id = IdCreator.createImageId(comicChapter, i);
            String imgUrl = imageNodes.get(i - 1).attr("data-src");
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false));
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of("referer", baseUrl.concat("/"));
    }
}
