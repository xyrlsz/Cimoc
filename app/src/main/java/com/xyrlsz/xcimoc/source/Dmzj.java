package com.xyrlsz.xcimoc.source;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.util.Pair;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.JsonIterator;
import com.xyrlsz.xcimoc.parser.MangaCategory;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.UrlFilter;
import com.xyrlsz.xcimoc.soup.Node;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;
import com.xyrlsz.xcimoc.utils.UicodeBackslashU;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import okhttp3.Headers;
import okhttp3.Request;

public class Dmzj extends MangaParser {

    public static final int TYPE = 10;
    public static final String DEFAULT_TITLE = "动漫之家";
    private static final String baseUrl = "https://m.idmzj.com";
    private static final String pcBaseUrl = "https://www.idmzj.com";
    private static final String NNV3ApiBaseUtl = "https://nnv3api.idmzj.com";
    //    private List<UrlFilter> filter = new ArrayList<>();
    String COOKIES = "";
    String UID = "";

    public Dmzj(Source source) {
//        init(source, new Category());
        init(source, null);
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.DMZJ_SHARED, MODE_PRIVATE);
        UID = sharedPreferences.getString(Constants.DMZJ_SHARED_UID, "");
        COOKIES = sharedPreferences.getString(Constants.DMZJ_SHARED_COOKIES, "");
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("dmzj.com", "info/(\\w+).html"));
        filter.add(new UrlFilter("idmzj.com", "info/(\\w+).html"));
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        if (page == 1) {
            String url = StringUtils.format("%s/dynamic/%s", pcBaseUrl, keyword);
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        try {
            String JsonString = StringUtils.match("comic_list:(\\[\\{.*?\\}\\])", html, 1);
            String decodeJsonString = UicodeBackslashU.unicodeToCn(JsonString).replace("\\/", "/");
            return new JsonIterator(new JSONArray(decodeJsonString)) {
                @Override
                protected Comic parse(JSONObject object) {
                    try {
                        String cid = object.getString("comic_py");
                        String title = object.getString("name");
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
        return StringUtils.format("%s/info/%s.html", baseUrl, cid);
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = StringUtils.format("%s/info/%s.html", baseUrl, cid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        Node body = new Node(html);
        String intro = body.textWithSubstring("p.txtDesc", 3);
        String title = body.text("#comicName");
        String cover = body.src("#Cover > img");
        String author = body.text("a.pd.introName");
        String update = body.textWithSubstring("div.Introduct_Sub > div.sub_r > p:eq(3) > span.date", 0, 10);
        boolean status = isFinish(body.text("div.sub_r > p:eq(2)"));
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        try {
            String JsonArrayString = StringUtils.match("initIntroData\\((.*)\\);", html, 1);
            String decodeJsonArrayString = UicodeBackslashU.unicodeToCn(JsonArrayString);
            JSONArray allJsonArray = new JSONArray(decodeJsonArrayString);
            int k = 1;
            for (int i = 0; i < allJsonArray.length(); i++) {
                JSONArray JSONArray = allJsonArray.getJSONObject(i).getJSONArray("data");
                String tag = allJsonArray.getJSONObject(i).getString("title");
                for (int j = 0; j != JSONArray.length(); ++j) {
                    JSONObject chapter = JSONArray.getJSONObject(j);
                    String title = chapter.getString("chapter_name");
                    String comic_id = chapter.getString("comic_id");
                    String chapter_id = chapter.getString("id");
                    String path = comic_id + "/" + chapter_id;
                    Long id = IdCreator.createChapterId(sourceComic, k++);
                    list.add(new Chapter(id, sourceComic, title, path, tag));
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
//        if (COOKIES.isEmpty() || UID.isEmpty()) {
////            App.goActivity(ComicSourceLoginActivity.class);
//            App.runOnMainThread(() ->
//                    HintUtils.showToast(App.getAppContext(), App.getAppResources().getString(R.string.dmzj_should_login))
//            );
//
//            return new Request.Builder().url(StringUtils.format("%s/chapinfo/%s.html", baseUrl, path))
//                    .build();
//        }
//
        String url = StringUtils.format("%s/chapter/%s.json?channel=Android&version=2.7.038", NNV3ApiBaseUtl, path);
//        String url = StringUtils.format("%s/api/v1/comic1/chapter/detail?channel=pc&app_name=dmzj&version=1.0.0&timestamp=%s&uid=%s&comic_id=%s&chapter_id=%s", pcBaseUrl, timestamp, UID, comic_id, chapter_id);
        return new Request.Builder().url(url)
                .addHeader("Cookie", COOKIES)
                .addHeader("User-Agent", "Android,DMZJ1,7.1.2")
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
        return Headers.of("Referer", "https://images.idmzj.com/", "user-agent", "Dalvik/2.1.0 (Linux; U; Android 12; SM-N9700 Build/SP1A.210812.016);");
    }

    private static class Category extends MangaCategory {

        @Override
        public boolean isComposite() {
            return true;
        }

        @Override
        public String getFormat(String... args) {
            String path = args[CATEGORY_SUBJECT].concat(" ").concat(args[CATEGORY_READER]).concat(" ").concat(args[CATEGORY_PROGRESS])
                    .concat(" ").concat(args[CATEGORY_AREA]).trim();
            if (path.isEmpty()) {
                path = String.valueOf(0);
            } else {
                path = path.replaceAll("\\s+", "-");
            }
            return StringUtils.format("http://v2.api.dmzj.com/classify/%s/%s/%%d.json", path, args[CATEGORY_ORDER]);
        }

        @Override
        public List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("冒险", "4"));
            list.add(Pair.create("百合", "3243"));
            list.add(Pair.create("生活", "3242"));
            list.add(Pair.create("四格", "17"));
            list.add(Pair.create("伪娘", "3244"));
            list.add(Pair.create("悬疑", "3245"));
            list.add(Pair.create("后宫", "3249"));
            list.add(Pair.create("热血", "3248"));
            list.add(Pair.create("耽美", "3246"));
            list.add(Pair.create("其他", "16"));
            list.add(Pair.create("恐怖", "14"));
            list.add(Pair.create("科幻", "7"));
            list.add(Pair.create("格斗", "6"));
            list.add(Pair.create("欢乐向", "5"));
            list.add(Pair.create("爱情", "8"));
            list.add(Pair.create("侦探", "9"));
            list.add(Pair.create("校园", "13"));
            list.add(Pair.create("神鬼", "12"));
            list.add(Pair.create("魔法", "11"));
            list.add(Pair.create("竞技", "10"));
            list.add(Pair.create("历史", "3250"));
            list.add(Pair.create("战争", "3251"));
            list.add(Pair.create("魔幻", "5806"));
            list.add(Pair.create("扶她", "5345"));
            list.add(Pair.create("东方", "5077"));
            list.add(Pair.create("奇幻", "5848"));
            list.add(Pair.create("轻小说", "6316"));
            list.add(Pair.create("仙侠", "7900"));
            list.add(Pair.create("搞笑", "7568"));
            list.add(Pair.create("颜艺", "6437"));
            list.add(Pair.create("性转换", "4518"));
            list.add(Pair.create("高清单行", "4459"));
            list.add(Pair.create("治愈", "3254"));
            list.add(Pair.create("宅系", "3253"));
            list.add(Pair.create("萌系", "3252"));
            list.add(Pair.create("励志", "3255"));
            list.add(Pair.create("节操", "6219"));
            list.add(Pair.create("职场", "3328"));
            list.add(Pair.create("西方魔幻", "3365"));
            list.add(Pair.create("音乐舞蹈", "3326"));
            list.add(Pair.create("机战", "3325"));
            return list;
        }

        @Override
        public boolean hasArea() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getArea() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("日本", "2304"));
            list.add(Pair.create("韩国", "2305"));
            list.add(Pair.create("欧美", "2306"));
            list.add(Pair.create("港台", "2307"));
            list.add(Pair.create("内地", "2308"));
            list.add(Pair.create("其他", "8453"));
            return list;
        }

        @Override
        public boolean hasReader() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getReader() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("少年", "3262"));
            list.add(Pair.create("少女", "3263"));
            list.add(Pair.create("青年", "3264"));
            return list;
        }

        @Override
        public boolean hasProgress() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getProgress() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("连载", "2309"));
            list.add(Pair.create("完结", "2310"));
            return list;
        }

        @Override
        public boolean hasOrder() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新", "1"));
            list.add(Pair.create("人气", "0"));
            return list;
        }

    }

}
