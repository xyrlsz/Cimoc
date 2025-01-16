package com.haleydu.cimoc.source;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.parser.JsonIterator;
import com.haleydu.cimoc.parser.MangaParser;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.parser.UrlFilter;
import com.haleydu.cimoc.soup.Node;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by xyrlsz on 2025/01/15.
 */

public class DuManWu extends MangaParser {

    public static final int TYPE = 104;
    public static final String DEFAULT_TITLE = "读漫屋";
    //    private static String baseUrl = "https://www.baozimh.com";
    private static final String baseUrl = "https://dumanwu.com";

    public DuManWu(Source source) {
        init(source, null);
        setIsUseWebView(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = baseUrl + "/s";
        RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"), "k=" + URLEncoder.encode(keyword.substring(0, 12), "utf-8"));
        return new Request.Builder().url(url).post(body).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        try {
            JSONObject object = new JSONObject(html);
            JSONArray data = object.getJSONArray("data");
            return new JsonIterator(data) {
                @Override
                protected Comic parse(JSONObject object) throws JSONException {
                    String cid = object.getString("id");
                    String cover = object.getString("imgurl");
                    String title = object.getString("name");
                    String update = object.getString("remarks");

                    return new Comic(TYPE, cid, title, cover, update, "");
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/" + cid;
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("dumanwu.com"));
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = baseUrl + "/" + cid;
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text(".banner-title");
        String cover = body.attr(".banner-pic", "data-src");
        String author = body.text(".author").split(" ")[0].replace("作者：", "");
        String update = body.text(".author").split(" ")[1];
        String intro = body.text(".introduction");
        comic.setInfo(title, cover, update, intro, author, false);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);
        List<Node> chapterNodes = body.list(".chaplist-box > ul > li > a");

        int i = 0;

        for (Node chapterNode : chapterNodes) {
            String title = chapterNode.text();
            String path = chapterNode.href().split("/")[2].replace(".html", "");
            list.add(new Chapter(Long.parseLong(sourceComic + "000" + i++), sourceComic, title, path));
        }

        if (html.contains("chaplist-more")) {
            RequestBody reqBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"), "id=" + comic.getCid());
            Request request = new Request.Builder().url(baseUrl + "/morechapter").post(reqBody).build();
            try {
                Response response = App.getHttpClient().newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject object = new JSONObject(response.body().string());
                    JSONArray data = object.getJSONArray("data");
                    for (int j = 0; j < data.length(); j++) {
                        JSONObject item = data.getJSONObject(j);
                        String title = item.getString("chaptername");
                        String path = item.getString("chapterid");
                        list.add(new Chapter(Long.parseLong(sourceComic + "000" + j + i), sourceComic, title, path));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        List<Node> imageNodes = body.list(".main_img > .chapter-img-box");
        for (int i = 1; i <= imageNodes.size(); i++) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "000" + i);
            String imgUrl = imageNodes.get(i - 1).attr("img", "data-src");
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
