package com.xyrlsz.xcimoc.source;


import android.content.Context;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.JsonIterator;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.UrlFilter;
import com.xyrlsz.xcimoc.utils.KomiicUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by xyrlsz on 2025/01/21.
 */

public class Komiic extends MangaParser {

    public static final int TYPE = 106;
    public static final String DEFAULT_TITLE = "komiic";

    private static final String baseUrl = "https://komiic.com";

    private String _cid = "", _path = "";

    public Komiic(Source source) {
        init(source, null);
        if (KomiicUtils.checkExpired()) {
            KomiicUtils.refresh();
        }
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url = StringUtils.format("%s/api/query", baseUrl);
        String jsonBody = "{"
                + "\"operationName\":\"searchComicAndAuthorQuery\","
                + "\"variables\":{\"keyword\":\""
                + keyword
                + "\"},"
                + "\"query\":\"query searchComicAndAuthorQuery($keyword: String!) {\\n  searchComicsAndAuthors(keyword: $keyword) {\\n    comics {\\n      id\\n      title\\n      status\\n      year\\n      imageUrl\\n      authors {\\n        id\\n        name\\n        __typename\\n      }\\n      categories {\\n        id\\n        name\\n        __typename\\n      }\\n      dateUpdated\\n      monthViews\\n      views\\n      favoriteCount\\n      lastBookUpdate\\n      lastChapterUpdate\\n      __typename\\n    }\\n    authors {\\n      id\\n      name\\n      chName\\n      enName\\n      wikiLink\\n      comicCount\\n      views\\n      __typename\\n    }\\n    __typename\\n  }\\n}\""
                + "}";
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody);

        return new Request.Builder().url(url).post(requestBody).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        try {
            JSONObject data = new JSONObject(html).getJSONObject("data");
            JSONArray comics = data.getJSONObject("searchComicsAndAuthors")
                    .getJSONArray("comics");
            return new JsonIterator(comics) {
                @Override
                protected Comic parse(JSONObject object) throws JSONException {
                    String cid = object.getString("id");
                    String title = object.getString("title");
                    String cover = object.getString("imageUrl");
//                    String status = object.getString("status");

                    String update = KomiicUtils.FormatTime(object.getString("dateUpdated"));
                    String author = "";
                    JSONArray authors = object.getJSONArray("authors");
                    for (int i = 0; i < authors.length(); i++) {
                        if (i != authors.length() - 1) {
                            author += authors.getJSONObject(i).getString("name") + ",";
                        } else {
                            author += authors.getJSONObject(i).getString("name");
                        }
                    }
                    return new Comic(TYPE, cid, title, cover, update, author);
                }
            };

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/comic/" + cid;
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("komiic.com"));
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = StringUtils.format("%s/api/query", baseUrl);
        String jsonBody = "{"
                + "\"operationName\":\"comicById\","
                + "\"variables\":{\"comicId\":\""
                + cid + "\"},"
                + "\"query\":\"query comicById($comicId: ID!) {\\n  comicById(comicId: $comicId) {\\n    id\\n    title\\n    status\\n    year\\n    imageUrl\\n    authors {\\n      id\\n      name\\n      __typename\\n    }\\n    categories {\\n      id\\n      name\\n      __typename\\n    }\\n    dateCreated\\n    dateUpdated\\n    views\\n    favoriteCount\\n    lastBookUpdate\\n    lastChapterUpdate\\n    __typename\\n  }\\n}\""
                + "}";
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody);
        return new Request.Builder().url(url).post(requestBody).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws JSONException {

        JSONObject data = new JSONObject(html).getJSONObject("data");
        JSONObject comicObject = data.getJSONObject("comicById");
        String title = comicObject.getString("title");
        String cover = comicObject.getString("imageUrl");
        StringBuilder author = new StringBuilder();
        JSONArray authors = comicObject.getJSONArray("authors");
        for (int i = 0; i < authors.length(); i++) {
            if (i != authors.length() - 1) {
                author.append(authors.getJSONObject(i).getString("name")).append(",");
            } else {
                author.append(authors.getJSONObject(i).getString("name"));
            }
        }

        String update = KomiicUtils.FormatTime(comicObject.getString("dateUpdated"));
        String intro = "";

        comic.setInfo(title, cover, update, intro, author.toString(), !comicObject.getString("status").equals("ONGOING"));
        return comic;
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        String jsonBody = "{"
                + "\"operationName\":\"chapterByComicId\","
                + "\"variables\":{\"comicId\":\"" + cid + "\"},"
                + "\"query\":\"query chapterByComicId($comicId: ID!) {\\n  chaptersByComicId(comicId: $comicId) {\\n    id\\n    serial\\n    type\\n    dateCreated\\n    dateUpdated\\n    size\\n    __typename\\n  }\\n}\""
                + "}";
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody);
        return new Request.Builder().url(baseUrl + "/api/query").post(requestBody).build();
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        List<Chapter> list = new LinkedList<>();

        JSONObject data = new JSONObject(html).getJSONObject("data");
        JSONArray chapters = data.getJSONArray("chaptersByComicId");
        Map<String, Integer> hash = new HashMap<>();
        List<JSONObject> jsonList = new ArrayList<>();
        for (int i = 0; i < chapters.length(); i++) {
            jsonList.add(chapters.getJSONObject(i));
        }
        Collections.sort(jsonList, (o1, o2) -> {
            try {
                String type1 = o1.getString("type");
                String type2 = o2.getString("type");
                return CharSequence.compare(type1, type2);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return 0;
        });
        for (int i = 0; i < jsonList.size(); i++) {

            String title = jsonList.get(i).getString("serial");
            String path = jsonList.get(i).getString("id");
            String type = jsonList.get(i).getString("type");
            if (hash.containsKey(title)) {
                hash.put(title, hash.get(title) + 1);
            } else {
                hash.put(title, 1);
            }
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i + 1), sourceComic, title, path, type));
        }

        return Lists.reverse(list);
    }


    @Override
    public Request getImagesRequest(String cid, String path) {
        String jsonBody = "{"
                + "\"operationName\":\"imagesByChapterId\","
                + "\"variables\":{\"chapterId\":\"" + path + "\"},"
                + "\"query\":\"query imagesByChapterId($chapterId: ID!) {\\n  imagesByChapterId(chapterId: $chapterId) {\\n    id\\n    kid\\n    height\\n    width\\n    __typename\\n  }\\n}\""
                + "}";
        _cid = cid;
        _path = path;
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody);
        return new Request.Builder().url(baseUrl + "/api/query").post(requestBody).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws JSONException {

        List<ImageUrl> list = new ArrayList<>();
        String imgBaseUrl = baseUrl + "/api/image/";
        JSONObject data = new JSONObject(html).getJSONObject("data");
        JSONArray images = data.getJSONArray("imagesByChapterId");
        String _cookies = App.getAppContext()
                .getSharedPreferences(Constants.KOMIIC_SHARED, Context.MODE_PRIVATE)
                .getString(Constants.KOMIIC_SHARED_COOKIES, "");
        if (KomiicUtils.checkExpired()) {
            KomiicUtils.refresh();
            _cookies = "";
        }
        if (KomiicUtils.checkIsOverImgLimit()) {
            _cookies = "";
        }
        if (KomiicUtils.checkEmptyAccountIsOverImgLimit() && _cookies.isEmpty()) {
           App.runOnMainThread(()-> Toast.makeText(App.getAppContext(), R.string.limit_over_tip, Toast.LENGTH_SHORT).show());
        }
        for (int i = 1; i <= images.length(); i++) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "0" + (i - 1));
            String imgUrl = imgBaseUrl + images.getJSONObject(i - 1).getString("kid");
            Headers headers = Headers.of("referer", StringUtils.format("https://komiic.com/comic/%s/chapter/%s", _cid, chapter.getPath()), "cookie", _cookies);
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false, headers));
        }
        return list;
    }


    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public Headers getHeader() {
        return Headers.of("referer", StringUtils.format("https://komiic.com/comic/%s/chapter/%s", _cid, _path));

    }
}
