package com.xyrlsz.xcimoc.source;

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
import com.xyrlsz.xcimoc.utils.DecryptionUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class YYManHua extends MangaParser {
    public static final int TYPE = 111;
    public static final String DEFAULT_TITLE = "YY漫画";
    private static final String baseUrl = "https://www.yymanhua.com";

    public YYManHua(Source source) {
        init(source, null);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/" + cid + "/";
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("yymanhua.com", "/(\\w.+)"));
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException, Exception {
        if (page == 1) {
            return new Request.Builder().url(baseUrl + "/search?title=" + keyword).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        Node body = new Node(html);
        List<Node> nodes = body.list("ul.mh-list > li");

        return new NodeIterator(nodes) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text("h2.title");
                String cid = node.href("a").replace("/", "");
                String cover = node.src("img.mh-cover");
                return new Comic(TYPE, cid, title, cover, null, null);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        return new Request.Builder().url(getUrl(cid)).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException, JSONException {
        Node body = new Node(html);
        String title = body.text("p.detail-info-title");
        String cover = body.src("img.detail-info-cover");
        String author = body.text("p.detail-info-tip > span:nth-child(1)").replace("作者：", "").replace(" ", ",");
        String update = body.text("div.detail-list-form-title");
        String result = StringUtils.match("\\d+-\\d+-\\d+", update, 0);
        String intro = body.text("p.detail-info-content");
        boolean status = isFinish(html);
        comic.setInfo(title, cover, result, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        Node body = new Node(html);
        List<Node> resList = body.list("#chapterlistload > a");
        if (resList.isEmpty()) {
            return null;
        }
        List<Chapter> list = new LinkedList<>();
        int i = 0;
        for (Node node : resList) {
            String title = node.text();
            String path = node.href().replace("/", "");
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i++), sourceComic, title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = baseUrl + "/" + path;
        return new Request.Builder().url(url).build();
    }


    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        List<ImageUrl> list = new ArrayList<>();
        Node body = new Node(html);
        String cid = StringUtils.match("var YYMANHUA_CID\\s*=\\s*(\\d+);", html, 1);
        String mid = StringUtils.match("var YYMANHUA_MID\\s*=\\s*(\\d+);", html, 1);
        String dt = StringUtils.match("var YYMANHUA_VIEWSIGN_DT\\s*=\\s*\"(.*?)\";", html, 1);
        String sign = StringUtils.match("var YYMANHUA_VIEWSIGN\\s*=\\s*\"(.*?)\";", html, 1);

        List<Node> nodes = body.list("div.reader-bottom-page-list > a");
        int i = 1;
        for (Node ignored : nodes) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "0" + i);
            String url = baseUrl + "/m" + cid + "/chapterimage.ashx?cid=" + cid + "&page=" + i + "&key=&_cid=" + cid + "&_mid=" + mid + "&_dt=" + dt + "&_sign=" + sign;
            list.add(new ImageUrl(id, comicChapter, i++, url, true, Headers.of("Referer", baseUrl + "/")));
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        return new Request.Builder().url(url)
                .addHeader("Referer", baseUrl)
                .build();
    }

    @Override
    public String parseLazy(String html, String url) {
        String result = DecryptionUtils.evalDecrypt(html);
        if (result != null) {
            return result.split(",")[0];
        }
        return null;
    }

}
