package com.xyrlsz.xcimoc.source;

import static com.xyrlsz.xcimoc.core.Manga.getResponseBody;
import static com.xyrlsz.xcimoc.parser.Category.CATEGORY_AREA;
import static com.xyrlsz.xcimoc.parser.Category.CATEGORY_ORDER;
import static com.xyrlsz.xcimoc.parser.Category.CATEGORY_SUBJECT;
import static com.xyrlsz.xcimoc.parser.MangaCategory.getParseFormatMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import com.google.common.collect.Lists;
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
import com.xyrlsz.xcimoc.utils.CopyMangaHeaderBuilder;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * 拷贝漫画
 * <a href="https://github.com/venera-app/venera-configs/blob/main/copy_manga.js">...</a>
 */

public class CopyMH extends MangaParser {
    public static final int TYPE = 26;
    public static final String DEFAULT_TITLE = "拷贝漫画";
    public static final String website = "https://www.2025copy.com";
    public static final String apiBaseUrl = "https://api.copy2000.online";
    private final String device = CopyMangaHeaderBuilder.generateDevice();
    private final String deviceInfo = CopyMangaHeaderBuilder.generateDeviceInfo();
    private final String pseudoId = CopyMangaHeaderBuilder.generatePseudoId();

    public CopyMH(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, website);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        String url;
        if (page == 1) {
            url = StringUtils.format(
                    "%s/api/v3/search/comic?platform=1&q=%s&limit=30&offset=0&q_type&_update=true&format=json", apiBaseUrl,
                    keyword);
            return new Request.Builder()
                    .url(url)
//                    .addHeader("User-Agent",
//                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like "
//                                    + "Gecko) Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
//                    .addHeader("version", "2025.05.09")
//                    .addHeader("platform", "1")
                    .headers(getHeader())
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
        //        filter.add(new UrlFilter("copymanga.com", "\\w+", 0));
        //        filter.add(new UrlFilter("mangacopy.com", "\\w+", 0));
        filter.add(new UrlFilter("www.mangacopy.com", "comic/(\\w+)", 1));
        filter.add(new UrlFilter("www.copy20.com", "comic/(\\w+)", 1));
        filter.add(new UrlFilter("www.2025copy.com", "comic/(\\w+)", 1));
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        try {
            JSONObject jsonObject = new JSONObject(html);
            return new JsonIterator(jsonObject.getJSONObject("results").getJSONArray("list")) {
                @Override
                protected Comic parse(JSONObject object) {
                    try {
                        String cid = object.getString("path_word");
                        String title = object.getString("name");
                        String cover = object.getString("cover");
                        String author = "";
                        for (int i = 0; i < object.getJSONArray("author").length(); ++i) {
                            author += object.getJSONArray("author").getJSONObject(i).getString("name").trim();
                            if (i < object.getJSONArray("author").length() - 1) {
                                author += ",";
                            }
                        }
//                        String author =
//                                object.getJSONArray("author").getJSONObject(0).getString("name").trim();
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
        String url = StringUtils.format("%s/api/v3/comic2/%s?in_mainland=true&request_id=&platform=3", apiBaseUrl, cid);
        return new Request.Builder()
                .url(url)
                .headers(getHeader())
                .build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        try {
            JSONObject body;
            JSONObject comicInfo = new JSONObject(html).getJSONObject("results");
            body = comicInfo.getJSONObject("comic");
            String cover = body.getString("cover");
            String intro = body.getString("brief");
            String title = body.getString("name");
            String update = body.getString("datetime_updated");
//            String author = ((JSONObject) body.getJSONArray("author").get(0)).getString("name");
            StringBuilder authorBuilder = new StringBuilder();
            for (int i = 0; i < body.getJSONArray("author").length(); ++i) {
                authorBuilder.append(((JSONObject) body.getJSONArray("author").get(i)).getString("name"));
                if (i < body.getJSONArray("author").length() - 1) {
                    authorBuilder.append(", ");
                }
            }
            String author = authorBuilder.toString();

            // 连载状态
            boolean finish = body.getJSONObject("status").getInt("value") != 0;
            comic.note = comicInfo.getJSONObject("groups");
            comic.setInfo(title, cover, update, intro, author, finish);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return comic;
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        String url = String.format(
                "%s/api/v3/comic/%s/group/default/chapters?limit=100&offset=0&in_mainland=true&request_id=", apiBaseUrl, cid);
        return new Request.Builder()
                .url(url)
                .headers(getHeader())
                .build();
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic)
            throws JSONException {
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
                if (key.equals("default"))
                    continue;
                String path_word = groups.getJSONObject(key).getString("path_word");
                String PathName = groups.getJSONObject(key).getString("name");
                String url =
                        String.format("%s/api/v3/comic/%s/group/%s/chapters?limit=100&offset=0&in_mainland=true&request_id=",
                                apiBaseUrl, comic.getCid(), path_word);
                Request request =
                        new Request.Builder()
                                .url(url)
                                .headers(getHeader())
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
        String url = StringUtils.format("%s/api/v3/comic/%s/chapter2/%s?in_mainland=true&request_id=", apiBaseUrl, cid, path);
        return new Request.Builder()
                .url(url)
                .headers(getHeader())
                .build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws JSONException {
        List<ImageUrl> list = new LinkedList<>();
        JSONObject object = new JSONObject(html);
        JSONObject res = object.getJSONObject("results");
        JSONObject chapterInfo = res.getJSONObject("chapter");
        JSONArray imgUrls = chapterInfo.getJSONArray("contents");

        JSONArray words = chapterInfo.optJSONArray("words");

        int contentSize = imgUrls.length();

        // 构造 words 索引数组（如果为 null 或长度不足）
        int[] wordIndices;
        if (words == null || words.length() < contentSize) {
            wordIndices = new int[contentSize];
            for (int i = 0; i < contentSize; i++) {
                wordIndices[i] = i;
            }
        } else {
            wordIndices = new int[words.length()];
            for (int i = 0; i < words.length(); i++) {
                wordIndices[i] = words.getInt(i);
            }
        }

        // 构建顺序 map
        Map<Integer, String> indexToUrl = new HashMap<>();
        for (int i = 0; i < contentSize; i++) {
            int index = wordIndices[i];
            String url = imgUrls.getJSONObject(i).getString("url");
            indexToUrl.put(index, url);
        }

        // 按顺序填入 list
        for (int i = 0; i < contentSize; i++) {
            Long comicChapter = chapter.getId();
            Long id = IdCreator.createImageId(comicChapter, i);
            String url = indexToUrl.get(i);
            if (url != null) {
                url = url.replace("c800x.jpg", "c1500x.jpg");
            }
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
        SharedPreferences prefs = App.getAppContext().getSharedPreferences(Constants.COPYMG_SHARED, Context.MODE_PRIVATE);
        String region = String.valueOf(prefs.getInt(Constants.COPYMG_SHARED_REGION, 0));
        CopyMangaHeaderBuilder builder =
                new CopyMangaHeaderBuilder(null, deviceInfo, device, pseudoId, region);
        try {
            return builder.genHeaders();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Headers.of();
    }

    @Override
    public Request getCategoryRequest(String format, int page) {

        Map<Integer, String> map = getParseFormatMap(format);
        int limit = 21;
        int offset = (page - 1) * limit;

        String url = StringUtils.format(
                "%s/api/v3/comics?free_type=1&limit=" +
                        limit +
                        "&offset=" +
                        offset +
                        "&top=" +
                        map.get(CATEGORY_AREA) +
                        "&theme=" +
                        map.get(CATEGORY_SUBJECT) +
                        "&ordering=" +
                        map.get(CATEGORY_ORDER) +
                        "&_update=true", apiBaseUrl);

        return new Request.Builder().headers(getHeader()).url(url).build();

    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        JSONObject data;
        try {
            data = new JSONObject(html).getJSONObject("results");
            JSONArray comics = data.getJSONArray("list");
            for (int i = 0; i < comics.length(); i++) {
                JSONObject object = comics.getJSONObject(i);
                String cid = object.getString("path_word");
                String title = object.getString("name");
                String cover = object.getString("cover");
                list.add(new Comic(TYPE, cid, title, cover, null, null));
            }

        } catch (JSONException e) {
            return list;
        }
        return list;
    }

    private static class Category extends MangaCategory {

        @Override
        public boolean isComposite() {
            return true;
        }


        @Override
        protected List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();

            list.add(Pair.create("全部", ""));
            list.add(Pair.create("愛情", "aiqing"));
            list.add(Pair.create("歡樂向", "huanlexiang"));
            list.add(Pair.create("冒險", "maoxian"));
            list.add(Pair.create("奇幻", "qihuan"));
            list.add(Pair.create("百合", "baihe"));
            list.add(Pair.create("校园", "xiaoyuan"));
            list.add(Pair.create("科幻", "kehuan"));
            list.add(Pair.create("東方", "dongfang"));
            list.add(Pair.create("耽美", "danmei"));
            list.add(Pair.create("生活", "shenghuo"));
            list.add(Pair.create("格鬥", "gedou"));
            list.add(Pair.create("轻小说", "qingxiaoshuo"));
            list.add(Pair.create("悬疑", "xuanyi"));
            list.add(Pair.create("其他", "qita"));
            list.add(Pair.create("神鬼", "shengui"));
            list.add(Pair.create("职场", "zhichang"));
            list.add(Pair.create("TL", "teenslove"));
            list.add(Pair.create("萌系", "mengxi"));
            list.add(Pair.create("治愈", "zhiyu"));
            list.add(Pair.create("長條", "changtiao"));
            list.add(Pair.create("四格", "sige"));
            list.add(Pair.create("节操", "jiecao"));
            list.add(Pair.create("舰娘", "jianniang"));
            list.add(Pair.create("竞技", "jingji"));
            list.add(Pair.create("搞笑", "gaoxiao"));
            list.add(Pair.create("伪娘", "weiniang"));
            list.add(Pair.create("热血", "rexue"));
            list.add(Pair.create("励志", "lizhi"));
            list.add(Pair.create("性转换", "xingzhuanhuan"));
            list.add(Pair.create("彩色", "COLOR"));
            list.add(Pair.create("後宮", "hougong"));
            list.add(Pair.create("美食", "meishi"));
            list.add(Pair.create("侦探", "zhentan"));
            list.add(Pair.create("AA", "aa"));
            list.add(Pair.create("音乐舞蹈", "yinyuewudao"));
            list.add(Pair.create("魔幻", "mohuan"));
            list.add(Pair.create("战争", "zhanzheng"));
            list.add(Pair.create("历史", "lishi"));
            list.add(Pair.create("异世界", "yishijie"));
            list.add(Pair.create("惊悚", "jingsong"));
            list.add(Pair.create("机战", "jizhan"));
            list.add(Pair.create("都市", "dushi"));
            list.add(Pair.create("穿越", "chuanyue"));
            list.add(Pair.create("恐怖", "kongbu"));
            list.add(Pair.create("C100", "comiket100"));
            list.add(Pair.create("重生", "chongsheng"));
            list.add(Pair.create("C99", "comiket99"));
            list.add(Pair.create("C101", "comiket101"));
            list.add(Pair.create("C97", "comiket97"));
            list.add(Pair.create("C96", "comiket96"));
            list.add(Pair.create("生存", "shengcun"));
            list.add(Pair.create("宅系", "zhaixi"));
            list.add(Pair.create("武侠", "wuxia"));
            list.add(Pair.create("C98", "C98"));
            list.add(Pair.create("C95", "comiket95"));
            list.add(Pair.create("FATE", "fate"));
            list.add(Pair.create("转生", "zhuansheng"));
            list.add(Pair.create("無修正", "Uncensored"));
            list.add(Pair.create("仙侠", "xianxia"));
            list.add(Pair.create("LoveLive", "loveLive"));

            return list;
        }

        @Override
        protected boolean hasOrder() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("更新時間（倒序）", "-datetime_updated"));
            list.add(Pair.create("熱度（倒序）", "-popular"));
            list.add(Pair.create("更新時間", "datetime_updated"));
            list.add(Pair.create("熱度", "popular"));
            return list;
        }

        @Override
        protected boolean hasArea() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getArea() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", ""));
            list.add(Pair.create("日漫", "japan"));
            list.add(Pair.create("韩漫", "korea"));
            list.add(Pair.create("美漫", "west"));
            list.add(Pair.create("已完结", "finish"));
            return list;
        }
    }
}
