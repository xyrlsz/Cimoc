package com.haleydu.cimoc.source;

import static com.haleydu.cimoc.core.Manga.getResponseBody;

import com.google.common.collect.Lists;
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class CopyMH extends MangaParser {
    public static final int TYPE = 26;
    public static final String DEFAULT_TITLE = "拷贝漫画";
    public static final String website = "https://www.mangacopy.com";

    public CopyMH(Source source) {
        init(source, null);
//        setParseImagesUseWebParser(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url = "";
        if (page == 1) {
//            JChineseConvertor jChineseConvertor = JChineseConvertor.getInstance();
//            keyword = jChineseConvertor.s2t(keyword);
            url = StringUtils.format(
                    "https://www.mangacopy.com/api/kb/web/searchbc/comics?offset=0&platform=2&limit=12&q=%sq_type=", keyword);
            return new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                    .build();
        }
        return null;
    }

    @Override
    public String getUrl(String cid) {
        return StringUtils.format("%s/comic/%s", website, cid);
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("copymanga.com", "\\w+", 0));
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        try {
            JSONObject jsonObject = new JSONObject(html);
            return new JsonIterator(jsonObject.getJSONObject("results").getJSONArray("list")) {
                @Override
                protected Comic parse(JSONObject object) {
                    try {
//                        JChineseConvertor jChineseConvertor = JChineseConvertor.getInstance();
                        String cid = object.getString("path_word");
                        String title = object.getString("name");
                        String cover = object.getString("cover");
                        String author = object.getJSONArray("author").getJSONObject(0).getString("name").trim();
                        return new Comic(TYPE, cid, title, cover, null, author);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = StringUtils.format("%s/comic/%s", website, cid);
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                .build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        Node titleBody = body.getChild(".comicParticulars-title");
        String title = titleBody.text("li > [title]");
        List<Node> info = body.list(".comicParticulars-title-right > ul > li");
        String cover, update = "", intro, author = "";
        boolean finish = false;
        cover = titleBody.attr(".comicParticulars-left-img > img", "data-src");
        for (Node node : info) {
            String tmp = node.text();
            if (tmp.contains("作者：")) {
                author = tmp.substring(3).strip();

            }
            if (tmp.contains("最後更新：")) {
                update = tmp.substring(5).strip();
            }
            if (tmp.contains("狀態：")) {
                finish = !tmp.contains("連載中");
            }
        }
        intro = body.text(".intro");

        comic.setInfo(title, cover, update, intro, author, finish);


        return comic;
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        String url = String.format("%s/api/v3/comic/%s/group/default/chapters?limit=500&offset=0", website, cid);
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                .build();
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        List<Chapter> list = new LinkedList<>();
        JSONObject jsonObject = new JSONObject(html);
        JSONArray array = jsonObject.getJSONObject("results").getJSONArray("list");
        for (int i = 0; i < array.length(); ++i) {
            String title = array.getJSONObject(i).getString("name");
            String path = array.getJSONObject(i).getString("uuid");
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i), sourceComic, title, path, "默认"));
        }
        try {
            JSONObject groups = (JSONObject) comic.note;
            Iterator<String> keys = groups.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.equals("default")) continue;
                String path_word = groups.getJSONObject(key).getString("path_word");
                String PathName = groups.getJSONObject(key).getString("name");
                String url = String.format("%s/api/v3/comic/%s/group/%s/chapters?limit=500&offset=0", website, comic.getCid(), path_word);
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                        .build();
                html = getResponseBody(App.getHttpClient(), request);
                jsonObject = new JSONObject(html);
                array = jsonObject.getJSONObject("results").getJSONArray("list");
                for (int i = 0; i < array.length(); ++i) {
                    String title = array.getJSONObject(i).getString("name");
                    String path = array.getJSONObject(i).getString("uuid");
                    list.add(new Chapter(Long.parseLong(sourceComic + "0" + i), sourceComic, title, path, PathName));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        return Lists.reverse(list);
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("%s/api/v3/comic/%s/chapter2/%s", website, cid, path);
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                .build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws JSONException {
        List<ImageUrl> list = new LinkedList<>();
        JSONObject object = new JSONObject(html);
        JSONObject res = object.getJSONObject("results");
        JSONObject chapterInfo = res.getJSONObject("chapter");
        JSONArray imgUrls = chapterInfo.getJSONArray("contents");
        for (int i = 0; i < imgUrls.length(); ++i) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "0" + i);
            String url = imgUrls.getJSONObject(i).getString("url");
            list.add(new ImageUrl(id, comicChapter, i + 1, url, false));
        }

        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        try {
            JSONObject comicInfo = new JSONObject(html).getJSONObject("results");
            JSONObject body = comicInfo.getJSONObject("comic");
            return body.getString("datetime_updated");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public Headers getHeader() {

        return Headers.of("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0");
    }
}
