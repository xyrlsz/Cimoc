package com.xyrlsz.xcimoc.source;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.JsonIterator;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.UrlFilter;
import com.xyrlsz.xcimoc.parser.UrlFilterWithCidQueryKey;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;
import com.xyrlsz.xcimoc.utils.TimestampUtils;
import com.xyrlsz.xcimoc.utils.ZaiManhuaSignUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.Request;

public class ZaiManhua extends MangaParser {

    public static final int TYPE = 12;
    public static final String DEFAULT_TITLE = "再漫画";
    private static final String baseUrl = "https://m.zaimanhua.com";
    private static final String pcBaseUrl = "https://manhua.zaimanhua.com";
    private static final String apiBaseUrl = "https://v4api.zaimanhua.com";
    //    private List<UrlFilter> filter = new ArrayList<>();
    String TOKEN = "";
    String UID = "";
    long EXP = 0;
    String username = "";
    String passwdMd5 = "";
    private SharedPreferences sharedPreferences;

    public ZaiManhua(Source source) {
        init(source);
        sharedPreferences = App.getAppContext().getSharedPreferences(Constants.ZAI_SHARED, MODE_PRIVATE);
        UID = sharedPreferences.getString(Constants.ZAI_SHARED_UID, "0");
        username = sharedPreferences.getString(Constants.ZAI_SHARED_USERNAME, "");
        passwdMd5 = sharedPreferences.getString(Constants.ZAI_SHARED_PASSWD_MD5, "");
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }


    private long getEXP() {
        EXP = sharedPreferences.getLong(Constants.ZAI_SHARED_EXP, 0);
        return EXP;
    }

    private String getTOKEN() {
        TOKEN = sharedPreferences.getString(Constants.ZAI_SHARED_TOKEN, "");
        return TOKEN;
    }


    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("zaimanhua.com"));
        filter.add(new UrlFilterWithCidQueryKey("m.zaimanhua.com", "id"));
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        if (page == 1) {
            String url = StringUtils.format("%s/app/v1/search/index?keyword=%s&source=0&page=1&size=24&platform=android&_v=2.2.4&_c=101_01_01_000", apiBaseUrl, keyword);
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        try {
            JSONObject jsonObject = new JSONObject(html);
            JSONObject data = jsonObject.getJSONObject("data");
            JSONArray list = data.getJSONArray("list");
            return new JsonIterator(list) {
                @Override
                protected Comic parse(JSONObject object) {
                    try {
//                        String cid = object.getString("comic_py");
                        String cid = object.getString("id");
                        String title = object.getString("title");
                        String cover = object.getString("cover");
                        String author = object.optString("authors");
//                        long time = Long.parseLong(object.getString("last_updatetime")) * 1000;
//                        String update = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(time));
                        return new Comic(TYPE, cid, title, cover, null, author);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getUrl(String cid) {
        return StringUtils.format("https://m.zaimanhua.com/pages/comic/detail?id=%s", cid);
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = StringUtils.format("%s/app/v1/comic/detail/%s?_v=2.2.4&platform=android&_v=2.2.4&_c=101_01_01_000", apiBaseUrl, cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        if (getTOKEN().isEmpty()) {
            App.runOnMainThread(() -> {
                HintUtils.showToast(App.getAppContext(), "再漫画未登录可能导致漫画无法阅读");
            });
        }
        long timestamp = System.currentTimeMillis() / 1000;
        if (timestamp > getEXP() && !getTOKEN().isEmpty()) {
            App.runOnMainThread(() -> {
                HintUtils.showToast(App.getAppContext(), "再漫画登录过期，可能需要重新登录");
            });
            ZaiManhuaSignUtils.LoginWithPasswdMd5(App.getAppContext(), new ZaiManhuaSignUtils.LoginCallback() {
                @Override
                public void onSuccess() {
                    App.runOnMainThread(() -> {
                        HintUtils.showToast(App.getAppContext(), "再漫画自动登录成功");
                    });
                }

                @Override
                public void onFail() {
                    App.runOnMainThread(() -> {
                        HintUtils.showToast(App.getAppContext(), "再漫画自动登录失败");
                    });
                }
            }, username, passwdMd5);

        }

        try {
            JSONObject jsonObject = new JSONObject(html);
            JSONObject data = jsonObject.getJSONObject("data").getJSONObject("data");
            String intro = data.getString("description");
            String title = data.getString("title");
            String cover = data.getString("cover");
            StringBuilder author = new StringBuilder();
            JSONArray authors = data.getJSONArray("authors");
            for (int i = 0; i < authors.length(); i++) {
                JSONObject obj = authors.getJSONObject(i);
                author.append(obj.getString("tag_name"));
                if (i < authors.length() - 1) {
                    author.append(",");
                }
            }
            String update = TimestampUtils.formatTimestampSeconds(data.getLong("last_updatetime"));
            boolean status = isFinish(html);
            comic.setInfo(title, cover, update, intro, author.toString(), status);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();

        try {
            JSONObject jsonObject = new JSONObject(html);
            JSONObject data = jsonObject.getJSONObject("data").getJSONObject("data");
            JSONArray allJsonArray = data.getJSONArray("chapters");
            int k = 1;
            for (int i = 0; i < allJsonArray.length(); i++) {
                JSONArray JSONArray = allJsonArray.getJSONObject(i).getJSONArray("data");
                String tag = allJsonArray.getJSONObject(i).getString("title");
                for (int j = 0; j != JSONArray.length(); ++j) {
                    JSONObject chapter = JSONArray.getJSONObject(j);
                    String title = chapter.getString("chapter_title");
                    String chapter_id = chapter.getString("chapter_id");
                    Long id = IdCreator.createChapterId(sourceComic, k++);
                    list.add(new Chapter(id, sourceComic, title, chapter_id, tag));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("%s/app/v1/comic/chapter/%s/%s?platform=android&_v=2.2.4&_c=101_01_01_000", pcBaseUrl, cid, path);
        return new Request.Builder().url(url)
                .addHeader("User-Agent", "Dart/3.6 (dart:io)")
                .addHeader("platform", "android")
                .addHeader("authorization", "Bearer " + getTOKEN())
                .build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        try {
            JSONObject jsonObject;
            JSONArray array;
            jsonObject = new JSONObject(html);
            array = jsonObject
                    .getJSONObject("data")
                    .getJSONObject("data")
                    .getJSONArray("page_url");
            for (int i = 0; i != array.length(); ++i) {
                Long comicChapter = chapter.getId();
                Long id = IdCreator.createChapterId(comicChapter, i);
                String url = array.getString(i);
                list.add(new ImageUrl(id, comicChapter, i + 1, url, false));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

//    @Override
//    public Request getCheckRequest(String cid) {
//        return getInfoRequest(cid);
//    }
//
//    @Override
//    public String parseCheck(String html) {
//        return new Node(html).textWithSubstring("div.Introduct_Sub > div.sub_r > p:eq(3) > span.date", 0, 10);
//    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new LinkedList<>();
        try {
            JSONArray array = new JSONArray(html);
            for (int i = 0; i != array.length(); ++i) {
                try {
                    JSONObject object = array.getJSONObject(i);
                    String cid = object.getString("id");
                    String title = object.getString("title");
                    String cover = object.getString("cover");
                    Long time = object.has("last_updatetime") ? object.getLong("last_updatetime") * 1000 : null;
                    String update = time == null ? null : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(time));
                    String author = object.optString("authors");
                    list.add(new Comic(TYPE, cid, title, cover, update, author));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://manhua.zaimanhua.com/", "user-agent", "Dalvik/2.1.0 (Linux; U; Android 12; SM-N9700 Build/SP1A.210812.016);");
    }


}
