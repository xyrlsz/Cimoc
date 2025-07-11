package com.xyrlsz.xcimoc.source;

import static com.xyrlsz.xcimoc.core.Manga.getResponseBody;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.JsonIterator;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.UrlFilter;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;
import taobe.tec.jcc.JChineseConvertor;

public class HotManga extends MangaParser {
    public static final int TYPE = 102;
    public static final String DEFAULT_TITLE = "热辣漫画";
    public static final String website = "https://www.manga2025.com";

    public HotManga(Source source) {
        init(source);
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
            url = StringUtils.format("%s/api/v3/search/comic?platform=1&limit=30&offset=0&q=%s", website, keyword);
            return new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                    .build();
        }
        return null;
    }

    @Override
    public String getUrl(String cid) {
        return website + "/comic/".concat(cid);
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("manga2025.com", "/comic/(\\w.+)"));
        filter.add(new UrlFilter("manga2024.com", "/comic/(\\w.+)"));
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        try {
            JSONObject jsonObject = new JSONObject(html);
            return new JsonIterator(jsonObject.getJSONObject("results").getJSONArray("list")) {
                @Override
                protected Comic parse(JSONObject object) {
                    try {
                        JChineseConvertor jChineseConvertor = JChineseConvertor.getInstance();
                        String cid = object.getString("path_word");
                        String title = jChineseConvertor.t2s(object.getString("name"));
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
        String url = StringUtils.format("%s/api/v3/comic2/%s", website, cid);
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                .build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        JSONObject body = null;
        try {
            JSONObject comicInfo = new JSONObject(html).getJSONObject("results");
            body = comicInfo.getJSONObject("comic");
            String cover = body.getString("cover");
            String intro = body.getString("brief");
            String title = body.getString("name");
            String update = body.getString("datetime_updated");
            String author = ((JSONObject) body.getJSONArray("author").get(0)).getString("name");
            // 连载状态
            boolean finish = body.getJSONObject("status").getInt("value") != 0;
            JSONObject group = comicInfo.getJSONObject("groups");
            comic.note = group;


            comic.setInfo(title, cover, update, intro, author, finish);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return comic;
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        String url = String.format("%s/api/v3/comic/%s/group/default/chapters?limit=500&offset=0", website, cid);
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
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
            list.add(new Chapter(null, sourceComic, title, path, "默认"));
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
                        .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36")
                        .build();
                html = getResponseBody(App.getHttpClient(), request);
                jsonObject = new JSONObject(html);
                array = jsonObject.getJSONObject("results").getJSONArray("list");
                for (int i = 0; i < array.length(); ++i) {
                    String title = array.getJSONObject(i).getString("name");
                    String path = array.getJSONObject(i).getString("uuid");
                    list.add(new Chapter(null, sourceComic, title, path, PathName));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
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
        String url = StringUtils.format("%s/api/v3/comic/%s/chapter/%s", website, cid, path);
        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
                .build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws JSONException {
        List<ImageUrl> list = new LinkedList<>();
        JSONObject jsonObject = new JSONObject(html);
        JSONArray array = jsonObject.getJSONObject("results").getJSONObject("chapter").getJSONArray("contents");
        try {
            for (int i = 0; i < array.length(); ++i) {
                Long comicChapter = chapter.getId();
                Long id = IdCreator.createImageId(comicChapter, i);
                String url = array.getJSONObject(i).getString("url").replace("m_read", "kb_m_read_large");
                url = url.replace("c800x.jpg", "c1500x.jpg");
                list.add(new ImageUrl(id, comicChapter, i + 1, url, false));
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        return Headers.of("Referer", website);
    }
}
