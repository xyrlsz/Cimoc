package com.xyrlsz.xcimocob.source;

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
import com.xyrlsz.xcimocob.utils.StringUtils;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class TTKMH extends MangaParser {
    public static final int TYPE = 109;
    public static final String DEFAULT_TITLE = "天天看";
    private static final String baseUrl = "https://www.ttkmh.com";
    private static final String imgBaseUrl = "https://img1.baipiaoguai.org";

    public TTKMH(Source source) {
        init(source);
        getImagesConfig().setUseWebParser(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("www.ttkmh.com"));
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/manhua/".concat(cid);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException, Exception {
        if (page == 1) {
            String url = baseUrl + "/search?key=" + keyword;
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        Node body = new Node(html);
        List<Node> resList = body.list("ul.row > li");
        if (resList.isEmpty()) {
            return null;
        }
        return new NodeIterator(resList) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text("div.name > h3");
                String cover = node.attr("div.pic > a > .img-wrapper", "data-original");
                String cid = node.href("div.pic > a").replace("/manhua/", "");
                return new Comic(TYPE, cid, title, cover, "", "");
            }
        };

    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = baseUrl + "/manhua/".concat(cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException, JSONException {
        Node body = new Node(html);
        String title = body.text("div.info > h3");
        String cover = body.attr("div.pic > img", "src");
        String author = "";
        String update = "";
        List<Node> list = body.list("div.info > p.row > span");
        for (Node node : list) {
            String text = node.text();
            if (text.contains("作者：")) {
                author = text.replace("作者：", "");
            } else if (text.contains("时间：")) {
                update = text.replace("时间：", "");
            }
        }
        String intro = body.text("div.vod-list > div.more-box > p");
        boolean status = isFinish(html);
        comic.setInfo(title, cover, update, intro, author, status);

        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        Node body = new Node(html);
        List<Node> resList = body.list("#ewave-playlist-1 > li");
        if (resList.isEmpty()) {
            return null;
        }
        List<Chapter> list = new LinkedList<>();
        int i = 0;
        for (Node node : resList) {
            String title = node.text("a");
            String path = node.href("a").replace("/chapter/".concat(comic.getCid()), "");
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
        String url = StringUtils.format("%s/chapter/%s/%s", baseUrl, cid, path);
        return new Request.Builder()
                .url(url)
                .build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        Node body = new Node(html);
        List<Node> imgNode = body.list("#newImgs > .lazy-read");
        List<ImageUrl> list = new ArrayList<>();
        for (int i = 1; i <= imgNode.size(); i++) {
            long comicChapter = chapter.getId();
            long id = IdCreator.createImageId(comicChapter, i);
            String tmp = imgNode.get(i - 1).attr("data-src");
            String imgUrl;
            if (tmp.startsWith("http")) {
                imgUrl = tmp;
            } else {
                imgUrl = imgBaseUrl + tmp;
            }

            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false, getHeader()));
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of("referer", baseUrl.concat("/"));
    }
}
