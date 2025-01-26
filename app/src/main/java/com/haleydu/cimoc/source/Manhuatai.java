package com.haleydu.cimoc.source;

import android.os.Build;
import android.util.Pair;

import com.google.common.collect.Lists;
import com.haleydu.cimoc.App;
import com.haleydu.cimoc.core.Manga;
import com.haleydu.cimoc.model.Chapter;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.ImageUrl;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.parser.JsonIterator;
import com.haleydu.cimoc.parser.MangaCategory;
import com.haleydu.cimoc.parser.MangaParser;
import com.haleydu.cimoc.parser.SearchIterator;
import com.haleydu.cimoc.soup.Node;
import com.haleydu.cimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;


/**
 * Created by reborn on 18-1-18.
 */

public class Manhuatai extends MangaParser {

    public static final int TYPE = 49;
    public static final String DEFAULT_TITLE = "漫画台";
    public static final String baseUrl = "https://www.kanman.com";
    private String _path = null;

    public Manhuatai(Source source) {
//        init(source, new Category());
        init(source, null);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            url = StringUtils.format(baseUrl + "/api/getsortlist/?product_id=2&productname=mht&platformname=wap&orderby=click&search_key=%s&page=%d&size=48",
                    URLEncoder.encode(keyword, StandardCharsets.UTF_8), page);
        }else{
            url = StringUtils.format(baseUrl + "/api/getsortlist/?product_id=2&productname=mht&platformname=wap&orderby=click&search_key=%s&page=%d&size=48",
                    URLEncoder.encode(keyword, "UTF-8"), page);
        }

        return new Request.Builder().url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        JSONObject object = new JSONObject(html);

        return new JsonIterator(object.getJSONObject("data").getJSONArray("data")) {
            @Override
            protected Comic parse(JSONObject object) throws JSONException {
                String title = object.getString("comic_name");
                String cid = object.getString("comic_id");
                String cover = "https://image.yqmh.com/mh/" + object.getString("comic_id") + ".jpg-300x400.webp";
                String author = object.getString("comic_author");
                Long timestamp = object.getLong("update_time");
                Date date = new Date(timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String update = sdf.format(date);
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

//    private String getResponseBody(OkHttpClient client, Request request) throws Manga.NetworkErrorException {
//        Response response = null;
//        try {
//            response = client.newCall(request).execute();
//            if (response.isSuccessful()) {
////                return response.body().string();
//
//                // 1.修正gb2312编码网页读取错误
//                byte[] bodybytes = response.body().bytes();
//                String body = new String(bodybytes);
//                if (body.indexOf("charset=gb2312") != -1) {
//                    body = new String(bodybytes, "GB2312");
//                }
//                return body;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (response != null) {
//                response.close();
//            }
//        }
//        throw new Manga.NetworkErrorException();
//    }

    private Node getComicNode(String cid) throws Manga.NetworkErrorException {
        Request request = getInfoRequest(cid);
        String html = Manga.getResponseBody(App.getHttpClient(), request);
        return new Node(html);
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "https://www.kanman.com/".concat(cid) + "/";
        return new Request.Builder().url(url).build();
    }

    //获取封面等信息（非搜索页）
    @Override
    public Comic parseInfo(String html, Comic comic) {
        Node body = new Node(html);

        // 获取标题
        String title = body.attr("h1.title", "title"); // 从 h1.title 的 title 属性中获取标题

        // 获取封面
        String cover = "https://image.yqmh.com/mh/" + comic.getCid() + ".jpg-300x400.webp";

        // 获取更新日期
        String update = body.text(".hd > span").replace("更新至", "").trim(); // 从 .hd > span 中提取更新日期
        update = update.substring(0, 10); // 只取前 10 个字符（即日期部分）

        // 获取简介（假设简介在某个描述区域，需要根据实际 HTML 调整）
        String intro = body.text(".introduce .content"); // 从 .introduce .content 中提取简介

        // 获取作者（假设作者信息在某个标签中，需要根据实际 HTML 调整）
        String author = body.text("div.introduce-box[data-index='0'] .username a");
        int index = author.indexOf("|");
        if (index > 0) {
            author = author.substring(0, index - 1);
        }

        // 设置漫画信息
        comic.setInfo(title, cover, update, intro, author, false);

        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        int i = 0;
        for (Node node : new Node(html).list("ol#j_chapter_list > li > a")) {
            String title = node.attr("title");
            String path = node.hrefWithSplit(1);
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i++), sourceComic, title, path));
        }
        return Lists.reverse(list);
    }

    //获取漫画图片Request
    @Override
    public Request getImagesRequest(String cid, String path) {
        _path = path;
//        String url = StringUtils.format("https://www.kanman.com/api/getcomicinfo_body?product_id=2&productname=mht&platformname=wap&comic_id=%s", cid);
        String url = StringUtils.format("https://www.kanman.com/%s/%s.html", cid, path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        try {
            String regex = "window\\.comicInfo\\s*=\\s*\\{.*?current_chapter\\s*:\\s*(\\{.*?\\})(?:,|\\})";
            Pattern pattern = Pattern.compile(regex, Pattern.DOTALL); // DOTALL 模式匹配多行
            Matcher matcher = pattern.matcher(html);

            // 如果找到匹配的内容
            if (matcher.find()) {
                String json = matcher.group(1); // 返回 current_chapter 的 JSON 数据
                JSONObject currChapter = new JSONObject(json);
                JSONArray imgUrl = currChapter.getJSONArray("chapter_img_list");
                for (int index = currChapter.getInt("start_num"); index <= currChapter.getInt("end_num"); index++) {
                    Long comicChapter = chapter.getId();
                    Long id = Long.parseLong(comicChapter + "0" + index);
                    String image = imgUrl.getString(index - 1);

                    list.add(new ImageUrl(id, comicChapter, index, image, false));
                }
            } else {
                return null; // 未找到匹配内容
            }
//            JSONObject object = new JSONObject(html);
//            if (object.getInt("status") != 0) {
//                return list;
//            }
//
//            JSONArray chapters = object.getJSONObject("data").getJSONArray("comic_chapter");
//            JSONObject chapterNew = null;
//            for (int i = 0; i < chapters.length(); i++) {
//                chapterNew = chapters.getJSONObject(i);
//                String a = chapterNew.getString("chapter_id");
//                if(a.equals(_path)) {
//                    break;
//                }
//            }
//
//            String ImagePattern = "http://mhpic." + chapterNew.getString("chapter_domain") + chapterNew.getString("rule") + "-mht.low.webp";
//
//            for (int index = chapterNew.getInt("start_num"); index <= chapterNew.getInt("end_num"); index++) {
//                Long comicChapter = chapter.getId();
//                Long id = Long.parseLong(comicChapter + "0" + index);
//
//                String image = ImagePattern.replaceFirst("\\$\\$", Integer.toString(index));
//                list.add(new ImageUrl(id, comicChapter, index, image, false));
//            }
        } catch (JSONException ex) {
            // ignore
        }

        return list;
    }

    //
//
//    class MhInfo {
//        @SerializedName("startimg")
//        int startimg;
//        @SerializedName("totalimg")
//        int totalimg;
//        @SerializedName("pageid")
//        int pageid;
//        @SerializedName("comic_size")
//        String comic_size;
//        @SerializedName("domain")
//        String domain;
//        @SerializedName("imgpath")
//        String imgpath;
//    }
    @Override
    public String getUrl(String cid) {
        return StringUtils.format("https://www.kanman.com/%s", cid);
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return new Node(html).text("span.update").substring(0, 10);
    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new LinkedList<>();
        Node body = new Node(html);
        for (Node node : body.list("a.sdiv")) {
            String cid = node.hrefWithSplit(0);
            String title = node.attr("title");
            String cover = node.getChild("img").attr("data-url");
            Node node1 = null;
            try {
                node1 = getComicNode(cid);
            } catch (Manga.NetworkErrorException e) {
                e.printStackTrace();
            }
            if (StringUtils.isEmpty(cover) && node1 != null) {
                cover = node1.src("#offlinebtn-container > img");
            }
            String author = null;
            String update = null;
            if (node1 != null) {
                author = node1.text("div.jshtml > ul > li:nth-child(3)").substring(3);
                update = node1.text("div.jshtml > ul > li:nth-child(5)").substring(3);
            }
            list.add(new Comic(TYPE, cid, title, cover, update, author));
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://www.kanman.com");
    }

    private static class Category extends MangaCategory {

        @Override
        public boolean isComposite() {
            return true;
        }

        @Override
        public String getFormat(String... args) {
            return StringUtils.format("https://www.kanman.com/%s_p%%d.html",
                    args[CATEGORY_SUBJECT]);
        }

        @Override
        public List<Pair<String, String>> getSubject() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部漫画", "all"));
            list.add(Pair.create("知音漫客", "zhiyinmanke"));
            list.add(Pair.create("神漫", "shenman"));
            list.add(Pair.create("风炫漫画", "fengxuanmanhua"));
            list.add(Pair.create("漫画周刊", "manhuazhoukan"));
            list.add(Pair.create("飒漫乐画", "samanlehua"));
            list.add(Pair.create("飒漫画", "samanhua"));
            list.add(Pair.create("漫画世界", "manhuashijie"));
//            list.add(Pair.create("排行榜", "top"));

//            list.add(Pair.create("热血", "rexue"));
//            list.add(Pair.create("神魔", "shenmo"));
//            list.add(Pair.create("竞技", "jingji"));
//            list.add(Pair.create("恋爱", "lianai"));
//            list.add(Pair.create("霸总", "bazong"));
//            list.add(Pair.create("玄幻", "xuanhuan"));
//            list.add(Pair.create("穿越", "chuanyue"));
//            list.add(Pair.create("搞笑", "gaoxiao"));
//            list.add(Pair.create("冒险", "maoxian"));
//            list.add(Pair.create("萝莉", "luoli"));
//            list.add(Pair.create("武侠", "wuxia"));
//            list.add(Pair.create("社会", "shehui"));
//            list.add(Pair.create("都市", "dushi"));
//            list.add(Pair.create("漫改", "mangai"));
//            list.add(Pair.create("杂志", "zazhi"));
//            list.add(Pair.create("悬疑", "xuanyi"));
//            list.add(Pair.create("恐怖", "kongbu"));
//            list.add(Pair.create("生活", "shenghuo"));
            return list;
        }

        @Override
        protected boolean hasOrder() {
            return false;
        }

        @Override
        protected List<Pair<String, String>> getOrder() {
//            List<Pair<String, String>> list = new ArrayList<>();
//            list.add(Pair.create("更新", "update"));
//            list.add(Pair.create("发布", "index"));
//            list.add(Pair.create("人气", "view"));
            return null;
        }

    }

}
