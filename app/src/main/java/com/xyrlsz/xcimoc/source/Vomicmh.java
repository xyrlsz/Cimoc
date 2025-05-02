package com.xyrlsz.xcimoc.source;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;
import com.xyrlsz.xcimoc.R;
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
import com.xyrlsz.xcimoc.ui.activity.ComicSourceLoginActivity;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class Vomicmh extends MangaParser {
    public static final int TYPE = 110;
    public static final String DEFAULT_TITLE = "vomic漫";
    private static final String baseUrl = "https://www.vomicmh.com";

    public Vomicmh(Source source) {
        init(source, null);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/detail/" + cid;
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("www.vomicmh.com"));
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException, Exception {
        if (page == 1) {
            String url = StringUtils.format(baseUrl + "/so/key/%s/1", keyword);
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        Node body = new Node(html);
        return new NodeIterator(body.list("div.justify-between > a")) {

            @Override
            protected Comic parse(Node node) {
                String cid = node.href().split("/")[2];
                String title = node.text(".title");
                if (title.contains("&amp;")) {
                    title = title.replace("&amp;", "&");
                }
                String cover = node.attr("img", "src");
                return new Comic(TYPE, cid, title, cover, null, null);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        return new Request.Builder().url(baseUrl + "/detail/" + cid).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException, JSONException {
        Node body = new Node(html);
        List<Node> list = body.list("div.detail > div > div");
        String title = body.text("div.detail > div > div.text-lg").replace("&amp;", "&");
        String cover = "";
        String author = "";
        String update = "";
        String intro = "";
        for (Node node : list) {
            String text = node.text();
            if (text.contains("作者：")) {
                author = text.replace("作者：", "").replace("&amp;", "&");
            }

            if (text.contains("简介：")) {
                intro = text.replace("简介：", "").replace("&amp;", "&");
            }

        }
        cover = body.src(".cover-img > div > img");
        comic.setInfo(title, cover, update, intro, author, false);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        Node body = new Node(html);
        List<Node> resList = body.list("div > a.chapter");
        if (resList.isEmpty()) {
            return null;
        }
        List<Chapter> list = new LinkedList<>();
        int i = 0;
        for (Node node : resList) {
            String title = node.text();
            String path = node.href();
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i++), sourceComic, title, path));
        }
        return Lists.reverse(list);
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.VOMIC_SHARED, MODE_PRIVATE);
        String cookie = sharedPreferences.getString(Constants.VOMIC_SHARED_COOKIES, "");
        if (cookie.isEmpty()) {
            App.goActivity(ComicSourceLoginActivity.class);
            App.runOnMainThread(() ->
                    HintUtils.showToast(App.getAppContext(), App.getAppResources().getString(R.string.user_login_tips))
            );
            return null;
        }
        return new Request.Builder().url(baseUrl + path).addHeader("cookie", cookie).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        List<ImageUrl> list = new LinkedList<>();

        Node body = new Node(html);
        List<Node> resList = body.list("#myscroll > img.myimage");
        if (resList.isEmpty()) {
            return null;
        }

        for (int i = 1; i <= resList.size(); i++) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "0" + (i - 1));
            String imgUrl = resList.get(i - 1).src();
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false));
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("referer", baseUrl.concat("/"));
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0");
        return Headers.of(headers);
    }
}
