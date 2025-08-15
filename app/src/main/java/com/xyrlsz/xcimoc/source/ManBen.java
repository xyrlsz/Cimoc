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

public class ManBen extends MangaParser {
    public static final int TYPE = 113;
    public static final String DEFAULT_TITLE = "漫本";
    private static final String baseUrl = "https://www.manben.com";
    private static final String UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36";

    public ManBen(Source source) {
        init(source);
        setParseImagesUseWebParser(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException, Exception {
        if (page != 1) {
            return null;
        }
        String url = baseUrl + "/search?title=" + keyword;
        return new Request.Builder()
                .url(url)
                .addHeader("user-agent", UA)
                .build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        Node body = new Node(html);
        List<Node> resList = body.list(".searchResultList > li");
        if (resList.isEmpty()) {
            return null;
        }
        return new NodeIterator(resList) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text(".title");
                String cover = node.src("img");
                String author = node.text(".author");
                String cid = node.href("a");
                return new Comic(TYPE, cid, title, cover, "", author);
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/" + cid;
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("manben.com", "/([\\w-]+)"));
        filter.add(new UrlFilter("www.manben.com", "/([\\w-]+)"));
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
        String title = body.text(".info > .title");
        String cover = body.src(".content > .cover");
        List<Node> infoList = body.list(".info > .subtitle");
        String author = "";
        for (Node info : infoList) {
            if (info.text().contains("作者")) {
                author = info.text().split("：")[1].strip();
            }
        }
        String update = body.text(".chapter > .top > span");
        String intro = body.text(".detailContent  > p");
        comic.setInfo(title, cover, update, intro, author, isFinish(html));
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        List<Node> chapterType = body.list(".detailSelectBar > li");
        List<Node> chapterTypeNodes = body.list(".chapterList");

        int i = 0;

        for (Node chapterNode : chapterTypeNodes) {
            String type = chapterType.get(i).text();
            List<Node> chapterNodes = chapterNode.list("li > a");
            for (Node node : chapterNodes) {
                String title = node.text();
                String path = node.href().split("/")[1];
                list.add(new Chapter(null, sourceComic, title, path, type));
            }
            i++;
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
        String url = StringUtils.format("%s/%s/", baseUrl, path);
        return new Request.Builder()
                .addHeader("user-agent", UA)
                .url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        List<ImageUrl> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> imageNodes = body.list("#cp_img > img");
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
        return Headers.of("Referer", baseUrl, "user-agent", UA);
    }
}
