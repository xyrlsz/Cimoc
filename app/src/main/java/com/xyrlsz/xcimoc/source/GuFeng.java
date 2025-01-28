package com.xyrlsz.xcimoc.source;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.NodeIterator;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.soup.Node;
import com.xyrlsz.xcimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by ZhiWen on 2019/02/25.
 */

public class GuFeng extends MangaParser {

    public static final int TYPE = 25;
    public static final String DEFAULT_TITLE = "古风漫画";

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    public GuFeng(Source source) {
        init(source, null);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "";
        if (page == 1) {
            url = StringUtils.format("https://m.gufengmh.com/search/?keywords=%s",
                    URLEncoder.encode(keyword, "UTF-8"));
        }
        return new Request.Builder()
//                .addHeader("Referer", "https://www.gufengmh8.com/")
//                .addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/12.0 Mobile/15A372 Safari/604.1")
//                .addHeader("Host", "m.gufengmh8.com")
                .url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        return new NodeIterator(body.list("div.UpdateList > div.itemBox")) {
            @Override
            protected Comic parse(Node node) {

                String cover = node.attr("div.itemImg > a > img", "src");

                String title = node.text("div.itemTxt > a");
                String cid = node.attr("div.itemTxt > a", "href").replace("https://m.gufengmh.com/manhua/", "");
                cid = cid.substring(0, cid.length() - 1);

                String update = node.text("div.itemTxt > p:eq(3) > span.date");
                String author = node.text("div.itemTxt > p:eq(1)");

                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "https://m.gufengmh.com/manhua/".concat(cid) + "/";
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        // 获取封面图片URL
        String cover = body.src("#Cover > img");

        // 获取简介
        String intro = body.text("#simple-des").replace("介绍:", "").trim();

        // 获取标题
        String title = body.attr("#Cover > img", "title");

        // 获取更新日期
        String update = body.text(".txtItme > .date");

        // 获取作者
        String author = body.text(".txtItme:eq(0)").trim();

        // 连载状态
        boolean status = !body.text(".txtItme:eq(2)").contains("连载中");

        // 设置漫画信息
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        int i=0;
        for (Node node : new Node(html).list("ul[id^=chapter-list] > li > a")) {
            String title = node.text();
            String path = node.hrefWithSplit(2);
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i++), sourceComic, title, path));
        }
        return Lists.reverse(list);
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("https://m.gufengmh.com/manhua/%s/%s.html", cid, path);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        String str = StringUtils.match("chapterImages = \\[(.*?)\\]", html, 1);
        if (str != null) {
            try {
                String[] array = str.split(",");
                String urlPrev = StringUtils.match("chapterPath = \"(.*?)\"", html, 1);
                for (int i = 0; i != array.length; ++i) {
                    // 去掉首末两端的双引号
                    String s = array[i].substring(1, array[i].length() - 1);
                    Long comicChapter = chapter.getId();
                    Long id = Long.parseLong(comicChapter + "0" + i);
                    list.add(new ImageUrl(id, comicChapter, i + 1, "https://res.xiaoqinre.com/" + urlPrev + s, false));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        // 这里表示的是更新时间
        return new Node(html).text("div.pic > dl:eq(4) > dd");
    }

    @Override
    public Headers getHeader() {
        return Headers.of("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36");
    }

}
