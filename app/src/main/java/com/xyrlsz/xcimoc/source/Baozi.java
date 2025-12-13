package com.xyrlsz.xcimoc.source;

import static com.xyrlsz.xcimoc.parser.Category.CATEGORY_AREA;
import static com.xyrlsz.xcimoc.parser.Category.CATEGORY_ORDER;
import static com.xyrlsz.xcimoc.parser.Category.CATEGORY_PROGRESS;
import static com.xyrlsz.xcimoc.parser.Category.CATEGORY_SUBJECT;
import static com.xyrlsz.xcimoc.parser.MangaCategory.getParseFormatMap;

import android.util.Pair;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaCategory;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.NodeIterator;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.UrlFilter;
import com.xyrlsz.xcimoc.soup.Node;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by xyrlsz on 2025/01/07.
 */

public class Baozi extends MangaParser {

    public static final int TYPE = 101;
    public static final String DEFAULT_TITLE = "包子漫画";
    //    private static String baseUrl = "https://www.baozimh.com";
    private static final String baseUrl = "https://cn.baozimhcn.com";
    private static final String imgDomain = "as.baozimh.com";

    public Baozi(Source source) {
        init(source, new Category());
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, "https://cn.baozimhcn.com");
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        if (page != 1) return null;
        String url = baseUrl + "/search?q=" + keyword;
        return new Request.Builder().url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list(".comics-card")) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text(".comics-card__info > div > h3");
                String author = node.text(".comics-card__info > small");

                String cid = node.href(".comics-card__info").split("/")[2];
                String cover = node.src(".comics-card > a > amp-img");
                cover = replaceDomain(cover);
                return new Comic(TYPE, cid, title, cover, null, author);
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/comic/" + cid;
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("cn.baozimhcn.com", "comic/([\\w\\-]+)"));
        filter.add(new UrlFilter("www.baozimh.com", "comic/([\\w\\-]+)"));
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = baseUrl + "/comic/" + cid;
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text(".comics-detail__title");
        String cover = body.src("div > amp-img");
        cover = replaceDomain(cover);
        String author = body.text(".comics-detail__author");
        String intro = body.text(".comics-detail__desc");
        String tags = body.text(".tag-list");
        boolean status = isFinish(tags);
        String update = body.text("div > span > em");
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        Node body = new Node(html);

        List<Node> chapterNodes = body.list(".comics-chapters");
        if (html.contains("章节目录") || html.contains("章節目錄")) {
            chapterNodes = Lists.reverse(chapterNodes);
        }
        int i = 0;
        Set<String> pathSet = new HashSet<>();
        for (Node chapterNode : chapterNodes) {
            String title = chapterNode.text("div > span");
            String path = chapterNode.href("a").split("chapter_slot=")[1];
            if (pathSet.contains(path)) {
                continue;
            }
            pathSet.add(path);
            Long id = IdCreator.createChapterId(sourceComic, i++);
            list.add(new Chapter(id, sourceComic, title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://appcn.baozimh.com/baozimhapp/comic/chapter/%s/0_%s.html", cid, path);
        return new Request.Builder()
                .addHeader("referer", "https://appcn.baozimh.com/")
                .addHeader("user-agent", "baozimh_android/1.0.29/cn/adset")
                .url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new ArrayList<>();
        Node body = new Node(html);
        List<Node> imageNodes = body.list(".comic-contain > .chapter-img");
        for (int i = 1; i <= imageNodes.size(); i++) {
            Long comicChapter = chapter.getId();
            Long id = IdCreator.createImageId(comicChapter, i);
            String imgUrl = imageNodes.get(i - 1).attr(".comic-contain__item", "data-src").replace("/w640/", "/");
            imgUrl = replaceDomain(imgUrl);
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false, getHeader()));
        }

        return list;
    }

    private String replaceDomain(String url) {
        String regex = "^(https?://)?([^/\\s:]+)(:\\d+)?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        String domain = "";
        if (matcher.find()) {
            domain = matcher.group(2);
        }
        if (domain != null && !domain.isEmpty()) {
            url = url.replace(domain, imgDomain);
        }
        return url;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public Headers getHeader() {
        return Headers.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36", "Referer", baseUrl);
    }

    @Override
    public Request getCategoryRequest(String format, int page) {

        Map<Integer, String> map = getParseFormatMap(format);
        int limit = 36;

        String url = StringUtils.format("%s/api/bzmhq/amp_comic_list?type=" +
                map.get(CATEGORY_SUBJECT) +
                "&region=" +
                map.get(CATEGORY_AREA) +
                "&state=" +
                map.get(CATEGORY_PROGRESS) +
                "&filter=" +
                map.get(CATEGORY_ORDER) +
                "&page=" +
                page +
                "&limit=" +
                limit +
                "&language=cn", baseUrl);
        return new Request.Builder().url(url).build();

    }

    @Override
    public List<Comic> parseCategory(String html, int page) {
        List<Comic> list = new ArrayList<>();
        JSONObject data;
        try {
            data = new JSONObject(html);
            JSONArray comics = data.getJSONArray("items");
            for (int i = 0; i < comics.length(); i++) {
                JSONObject object = comics.getJSONObject(i);
                String cid = object.getString("comic_id");
                String title = object.getString("name");
                String cover = StringUtils.format("https://%s/cover/%s?w=285&h=375&q=100", imgDomain, object.getString("topic_img"));
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

            list.add(Pair.create("全部", "all"));
            list.add(Pair.create("恋爱", "lianai"));
            list.add(Pair.create("纯爱", "chunai"));
            list.add(Pair.create("古风", "gufeng"));
            list.add(Pair.create("异能", "yineng"));
            list.add(Pair.create("悬疑", "xuanyi"));
            list.add(Pair.create("剧情", "juqing"));
            list.add(Pair.create("科幻", "kehuan"));
            list.add(Pair.create("奇幻", "qihuan"));
            list.add(Pair.create("玄幻", "xuanhuan"));
            list.add(Pair.create("穿越", "chuanyue"));
            list.add(Pair.create("冒险", "mouxian"));
            list.add(Pair.create("推理", "tuili"));
            list.add(Pair.create("武侠", "wuxia"));
            list.add(Pair.create("格斗", "gedou"));
            list.add(Pair.create("战争", "zhanzheng"));
            list.add(Pair.create("热血", "rexie"));
            list.add(Pair.create("搞笑", "gaoxiao"));
            list.add(Pair.create("大女主", "danuzhu"));
            list.add(Pair.create("都市", "dushi"));
            list.add(Pair.create("总裁", "zongcai"));
            list.add(Pair.create("后宫", "hougong"));
            list.add(Pair.create("日常", "richang"));
            list.add(Pair.create("韩漫", "hanman"));
            list.add(Pair.create("少年", "shaonian"));
            list.add(Pair.create("其他", "qita"));
            return list;
        }

        @Override
        protected boolean hasArea() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getArea() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "all"));
            list.add(Pair.create("国漫", "cn"));
            list.add(Pair.create("日本", "jp"));
            list.add(Pair.create("韩国", "kr"));
            list.add(Pair.create("欧美", "en"));
            return list;
        }

        @Override
        public boolean hasProgress() {
            return true;
        }

        @Override
        public List<Pair<String, String>> getProgress() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "all"));
            list.add(Pair.create("连载中", "serial"));
            list.add(Pair.create("已完结", "pub"));
            return list;
        }

        @Override
        protected boolean hasOrder() {
            return true;
        }

        @Override
        protected List<Pair<String, String>> getOrder() {
            List<Pair<String, String>> list = new ArrayList<>();
            list.add(Pair.create("全部", "*"));
            list.add(Pair.create("ABCD", "ABCD"));
            list.add(Pair.create("EFGH", "EFGH"));
            list.add(Pair.create("IJKL", "IJKL"));
            list.add(Pair.create("NMOP", "NMOP"));
            list.add(Pair.create("QRST", "QRST"));
            list.add(Pair.create("UVW", "UVW"));
            list.add(Pair.create("XYZ", "XYZ"));
            list.add(Pair.create("0-9", "0-9"));

            return list;
        }

    }
}
