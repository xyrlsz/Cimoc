package com.xyrlsz.xcimoc.source;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.NodeIterator;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.UrlFilter;
import com.xyrlsz.xcimoc.soup.Node;
import com.xyrlsz.xcimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by FEILONG on 2017/12/21.
 */

public class BuKa extends MangaParser {

    public static final int TYPE = 52;
    public static final String DEFAULT_TITLE = "布卡漫画";
    private static final String baseUrl = "https://www.bukamh.org";

    public BuKa(Source source) {
        init(source, null);
        setParseImagesUseWebParser(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
//        String url = "http://m.buka.cn/search/ajax_search";
//        RequestBody data = new FormBody.Builder()
//                .add("key", keyword)
//                .add("start", String.valueOf(15 * (page - 1)))
//                .add("count", "15")
//                .build();//key=%E4%B8%8D%E5%AE%9C%E5%AB%81&start=0&count=15
//        return new Request.Builder()
//                .url(url)
//                .post(data)
//                .build();
        String url;
        if (page == 1) {
            url = StringUtils.format("%s/index.php/search?key=%s", baseUrl, keyword);
        } else {
            url = StringUtils.format("%s/search/%s/%d", baseUrl, keyword, page);
        }

        return new Request.Builder().url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        Node body = new Node(html);
        List<Node> resList = body.list(".u_list> li");
        return new NodeIterator(resList) {
            @Override
            protected Comic parse(Node node) {
                String cover = node.src(".pic > a >img");
                String cid = node.href(".neirong > .name").replace("/", "");
                String title = node.text(".neirong > .name");
                String author = "", update = "";
                int i = 0;
                for (Node n : node.list(".neirong > p")) {
                    if (i == 0) {
                        author = n.text();
                    }
                    if (i == 2) {
                        update = n.text();
                    }
                    i++;
                }
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return baseUrl + "/".concat(cid);
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("www.bukamh.org", ".*",0));
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = baseUrl + "/".concat(cid);
        return new Request.Builder()
//                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                .url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text(".infobox > .title");
        String cover = body.src(".infobox > .info > .img > img");
        String update = "";
        String author = "";
        String intro = body.text(".infocomic > .text");
        for (Node n : body.list(".infobox > .info > .tage")) {
            String tmp = n.text();
            if (tmp.contains("作者：")) {
                author = tmp.substring(3).strip();
            }
            if (tmp.contains("更新于：")) {
                update = tmp.substring(4).strip();
            }
        }

//        boolean status = isFinish("连载中");//todo: fix here
        comic.setInfo(title, cover, update, intro, author, false);
        return comic;
    }

//    @Override
//    public Request getChapterRequest(String html, String cid){
//        String url = "https://m.ac.qq.com/comic/chapterList/id/".concat(cid);
//        return new Request.Builder()
//            .url(url)
//            .build();
//    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        int i = 0;
        for (Node node : new Node(html).list(".listbox > .list > li > a")) {
            String title = node.text();
            String path = node.href();
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i++), sourceComic, title, path));
        }
        return Lists.reverse(list);
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = StringUtils.format("%s/%s", baseUrl, path);
        return new Request.Builder()
//                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 7.0;) Chrome/58.0.3029.110 Mobile")
                .url(url)
                .build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        Node body = new Node(html);
        int i = 0;
        for (Node n : body.list(".chapterbox >#manga-imgs > .pic > img")) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "0" + i);
            list.add(new ImageUrl(id, comicChapter, ++i, n.attr("data-src"), false));
        }
//        Matcher m = Pattern.compile("<img class=\"lazy\" data-original=\"(http.*?jpg)\" />").matcher(html);
//        if (m.find()) {
//            try {
//                int i = 0;
//                do {
//                    Long comicChapter = chapter.getId();
//                    Long id = Long.parseLong(comicChapter + "0" + i);
//                    list.add(new ImageUrl(id, comicChapter, ++i, StringUtils.match("http.*jpg", m.group(0), 0), false));
//                } while (m.find());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
        return list;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

//    @Override
//    public String parseCheck(String html) {
//        return new Node(html).text("div.book-detail > div.cont-list > dl:eq(2) > dd");
//    }
//
//    @Override
//    public List<Comic> parseCategory(String html, int page) {
//        List<Comic> list = new LinkedList<>();
//        Node body = new Node(html);
//        for (Node node : body.list("li > a")) {
//            String cid = node.hrefWithSplit(1);
//            String title = node.text("h3");
//            String cover = node.attr("div > img", "data-src");
//            String update = node.text("dl:eq(5) > dd");
//            String author = node.text("dl:eq(2) > dd");
//            list.add(new Comic(TYPE, cid, title, cover, update, author));
//        }
//        return list;
//    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://www.bukamh.org");
    }

}
