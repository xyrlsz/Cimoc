package com.xyrlsz.xcimoc.source;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by xyrlsz on 2025/01/09.
 */

public class MYCOMIC extends MangaParser {


    public static final int TYPE = 103;
    public static final String DEFAULT_TITLE = "MYCOMIC";
    private static final String baseUrl = "https://mycomic.com";

    public MYCOMIC(Source source) {
        init(source, null);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        if (page != 1) {
            return null;
        }
        String url = baseUrl + "/comics?q=" + keyword + "&page=" + page;
        return new Request.Builder().url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".grow > div > .group")) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text("[data-flux-subheading]");
//                String author = node.text(".comics-card__info > small");
                String[] tmp = node.href("div > a").split("/");
                String cid = tmp[tmp.length - 1];
                String cover = node.attr("div > a > img", "data-src");
                if (cover.isEmpty()) {
                    cover = node.src("div > a > img");
                }
                return new Comic(TYPE, cid, title, cover, "", "");
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/comics/" + cid;
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("mycomic.com"));
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = baseUrl + "/comics/" + cid;
        return new Request.Builder().url(url).header("Referer", baseUrl.concat("/")).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html).getChild("[data-flux-card]");
        String title = body.text("[data-flux-heading]");
        String cover = body.src("div > img");
        String author = body.text(".grow > div > div > span");
        String intro = body.text(".grow > div > div > [x-cloak]");

        boolean status = body.text("[data-flux-badge]").equals("已完結")
//                ||body.text("[data-flux-badge]").equals("已完结")
                ;
        String update = body.attr("time", "datetime");
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        List<Node> chapterNodes = body.list(".grow > [x-cloak] > [x-data]");

        int i = 0;

        for (Node chapterNode : chapterNodes) {
            String chaptersJson = StringUtils.match("chapters:\\s*(\\[.*?\\])", chapterNode.attr("x-data"), 1);
            try {
                JSONArray chaptersData = new JSONArray(chaptersJson);

                for (int j = 0; j < chaptersData.length(); j++) {
                    JSONObject chapter = chaptersData.getJSONObject(j);
                    String title = chapter.getString("title");
                    String path = chapter.getString("id");
                    list.add(new Chapter(Long.parseLong(sourceComic + "0" + i++), sourceComic, title, path));

                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }

        return list;
    }


    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("%s/chapters/%s", baseUrl, path);
        return new Request.Builder().url(url).header("Referer", baseUrl.concat("/")).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> imageNodes = body.list("div > div > div > img[x-ref^=page-]");
        for (int i = 1; i <= imageNodes.size(); i++) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "0" + i);
            String imgUrl = imageNodes.get(i - 1).src();
            if (imgUrl.isEmpty()) {
                imgUrl = imageNodes.get(i - 1).attr("data-src");
            }
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false));
        }

        return list;
    }

    @Override
    public Headers getHeader() {
        Map<String, String> heads = new HashMap<>();
        heads.put("referer", baseUrl.concat("/"));
        return Headers.of(heads);
    }

    @Override
    public Headers getHeader(List<ImageUrl> list) {
        Map<String, String> heads = new HashMap<>();
        heads.put("referer", baseUrl.concat("/"));
        return Headers.of(heads);
    }


    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

}
