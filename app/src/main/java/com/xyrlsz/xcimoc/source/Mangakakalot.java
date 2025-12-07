//package com.xyrlsz.xcimoc.source;
//
//import android.util.Pair;
//
//import com.xyrlsz.xcimoc.model.Chapter;
//import com.xyrlsz.xcimoc.model.Comic;
//import com.xyrlsz.xcimoc.model.ImageUrl;
//import com.xyrlsz.xcimoc.model.Source;
//import com.xyrlsz.xcimoc.parser.MangaCategory;
//import com.xyrlsz.xcimoc.parser.MangaParser;
//import com.xyrlsz.xcimoc.parser.NodeIterator;
//import com.xyrlsz.xcimoc.parser.SearchIterator;
//import com.xyrlsz.xcimoc.parser.UrlFilter;
//import com.xyrlsz.xcimoc.soup.Node;
//import com.xyrlsz.xcimoc.utils.IdCreator;
//import com.xyrlsz.xcimoc.utils.StringUtils;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.LinkedHashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//
//import okhttp3.Headers;
//import okhttp3.Request;
//
///**
// * Created by nich on 2017/8/6.
// * fix by haleydu on 2020/8/16.
// */
//
//public class Mangakakalot extends MangaParser {
//    public static final int TYPE = 44;
//    public static final String DEFAULT_TITLE = "Mangakakalot";
//
//    private static final String baseUrl = "https://mangakakalot.to";
//
//    public Mangakakalot(Source source) {
//        //        init(source, new Category());
//        init(source);
//        setParseImagesUseWebParser(true);
//    }
//
//    public static Source getDefaultSource() {
//        return new Source(null, DEFAULT_TITLE, TYPE, true, baseUrl);
//    }
//
//    @Override
//    protected void initUrlFilterList() {
//        super.initUrlFilterList();
//        filter.add(new UrlFilter("mangakakalot.to", ".*", 0));
//    }
//
//    /**
//     * 搜索页的 HTTP 请求
//     *
//     * @param keyword 关键字
//     * @param page    页码
//     */
//    @Override
//    public Request getSearchRequest(String keyword, int page) {
//        if (page != 1)
//            return null;
//        String url = baseUrl + "/search?keyword=" + keyword;
//        return new Request.Builder().url(url).addHeader("Referer", baseUrl + "/").build();
//    }
//
//    /**
//     * 获取搜索结果迭代器，这里不直接解析成列表是为了多图源搜索时，不同图源漫画穿插的效果
//     *
//     * @param html 页面源代码
//     * @param page 页码，可能对于一些判断有用
//     */
//    @Override
//    public SearchIterator getSearchIterator(String html, int page) {
//        Node body = new Node(html);
//        return new NodeIterator(body.list(".swiper-slide > .item-basic")) {
//            @Override
//            protected Comic parse(Node node) {
//                String cid = node.href("a").replace("/", "");
//                String title = node.attr("img", "alt");
//                String cover = node.src("img");
//                //                String update = node.text("div.story_item_right >
//                //                span:eq(4)").replace("Updated :", "").trim(); String author =
//                //                node.text("div.story_item_right > span:eq(3)").replace("Author(s)
//                //                :", "").trim();
//                return new Comic(TYPE, cid, title, cover, "", "");
//            }
//        };
//    }
//
//    @Override
//    public String getUrl(String cid) {
//        return baseUrl + "/" + cid;
//    }
//
//    /**
//     * 详情页的 HTTP 请求
//     *
//     * @param cid 漫画 ID
//     */
//    @Override
//    public Request getInfoRequest(String cid) {
//        return new Request.Builder()
//                .url(baseUrl + "/" + cid)
//                .addHeader("Referer", baseUrl + "/")
//                .build();
//    }
//
//    /**
//     * 解析详情
//     *
//     * @param html  页面源代码
//     * @param comic 漫画实体类，需要设置其中的字段
//     */
//    @Override
//    public Comic parseInfo(String html, Comic comic) {
//        Node body = new Node(html).getChild(".detail-box");
//
//        String title = body.text("h3.manga-name");
//        String cover = body.src(".manga-poster > img");
//        //        String update = body.text(".db-info >.line > :eq(3)").replace("Last updated :",
//        //        "").trim(); String author = body.text(".db-info >.line >
//        //        :eq(3)").replace("Author(s) :", "").trim();
//        String intro = body.text(".dbs-content");
//        boolean status = isFinish(body.text(".db-info"));
//        comic.setInfo(title, cover, "", intro, "", status);
//        return comic;
//    }
//
//    @Override
//    public Request getChapterRequest(String html, String cid) {
//        List<String> tmp = Arrays.asList(cid.split("-"));
//        return new Request.Builder()
//                .url(baseUrl + "/ajax/manga/list-chapter-volume?id=" + tmp.get(tmp.size() - 1))
//                .header("referer", baseUrl + "/" + cid)
//                .build();
//    }
//
//    /**
//     * 解析章节列表
//     *
//     * @param html 页面源代码
//     */
//    @Override
//    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
//        Set<Chapter> set = new LinkedHashSet<>();
//        Node body = new Node(html);
//
//        int i = 0;
//        for (Node node : body.list(".cl-body > div > .item")) {
//            String title = node.text(".item-name > a");
//            String path = node.href(".item-name > a");
//            Long id = IdCreator.createChapterId(sourceComic, i++);
//            set.add(new Chapter(id, sourceComic, title, path));
//        }
//        return new LinkedList<>(set);
//    }
//
//    /**
//     * 图片列表的 HTTP 请求
//     *
//     * @param cid  漫画 ID
//     * @param path 章节路径
//     */
//    @Override
//    public Request getImagesRequest(String cid, String path) {
//        return new Request.Builder()
//                .url(baseUrl + "/" + path)
//                .addHeader("Referer", baseUrl + "/" + cid)
//                .addHeader("user-agent",
//                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
//                                + "Chrome/132.0.0.0 Safari/537.36 Edg/132.0.0.0")
//                .build();
//    }
//
//    /// 解析图片列表，若为惰性加载，则 [lazy][ImageUrl] 为 true
//    /// 惰性加载的情况，一次性不能拿到所有图片链接，例如网站使用了多次异步请求
//    /// [#parseImages]，或需要跳转到不同页面 才能获取
//    /// [parseImages][]，这些情况一般可以根据页码构造出相应的请求链接，到阅读时再解析 支持多个链接
//    /// ，例如 [#parseImages]
//    ///
//    /// @param html 页面源代码
//    @Override
//    public List<ImageUrl> parseImages(String html, Chapter chapter) {
//        List<ImageUrl> list = new LinkedList<>();
//        Node body = new Node(html).getChild(".reading-inner");
//        int i = 0;
//
//        for (Node node : body.list("#list-image > div")) {
//            Long comicChapter = chapter.getId();
//            Long id = IdCreator.createImageId(comicChapter, i);
//            list.add(new ImageUrl(id, comicChapter, ++i, node.attr("data-url"), false));
//        }
//
//        return list;
//    }
//
//    /**
//     * 获取下载图片时的 HTTP 请求头，一般用来设置 Referer 和 Cookie
//     */
//    @Override
//    public Headers getHeader() {
//        return Headers.of("Referer", baseUrl + "/");
//    }
//
//    @Override
//    public Request getCategoryRequest(String format, int page) {
//        return super.getCategoryRequest(format, page);
//    }
//
//    @Override
//    public List<Comic> parseCategory(String html, int page) {
//        List<Comic> list = new LinkedList<>();
//        Node body = new Node(html);
//        int total = Integer.parseInt(
//                StringUtils.match("\\d+", body.list("div.phan-trang > a").get(4).href(), 0));
//        if (page <= total) {
//            for (Node node : body.list("div.truyen-list > div.list-truyen-item-wrap")) {
//                String cid = node.href("h3 > a").replace("https://mangakakalot.com/manga/", "");
//                String title = node.text("h3 > a");
//                String cover = node.src("a > img");
//                String update = node.list("a").get(2).text();
//                String author = node.text("div:eq(1) > div:eq(1) > dl:eq(1) > dd > a");
//                list.add(new Comic(TYPE, cid, title, cover, update, author));
//            }
//        }
//        return list;
//    }
//
//    private static class Category extends MangaCategory {
//        @Override
//        public boolean isComposite() {
//            return true;
//        }
//
//        @Override
//        public String getFormat(String... args) {
//            String path = "category=".concat(args[CATEGORY_SUBJECT])
//                    .concat("&alpha=all&state=")
//                    .concat(args[CATEGORY_PROGRESS])
//                    .concat("&group=all")
//                    .trim();
//            path = path.replaceAll("\\s+", "-");
//            return StringUtils.format("https://manganel.com/manga_list?type=new&page=%%d&%s", path);
//        }
//
//        @Override
//        protected List<Pair<String, String>> getSubject() {
//            List<Pair<String, String>> list = new ArrayList<>();
//            list.add(Pair.create("All", "all"));
//            list.add(Pair.create("Action", "2"));
//            //            list.add(Pair.create("Adult", "3"));
//            list.add(Pair.create("Adventure", "4"));
//            list.add(Pair.create("Comedy", "6"));
//            list.add(Pair.create("Cooking", "7"));
//            //            list.add(Pair.create("Donjinshi", "9"));
//            list.add(Pair.create("Drama", "10"));
//            //            list.add(Pair.create("Ecchi", "11"));
//            list.add(Pair.create("Fantasy", "12"));
//            //            list.add(Pair.create("Gender bender", "13"));
//            //            list.add(Pair.create("Harem", "14"));
//            list.add(Pair.create("Historical", "15"));
//            list.add(Pair.create("Horror", "16"));
//            list.add(Pair.create("Josei", "17"));
//            list.add(Pair.create("Manhua", "44"));
//            list.add(Pair.create("Manhwa", "43"));
//            list.add(Pair.create("Martial Arts", "19"));
//            list.add(Pair.create("Mature", "20"));
//            list.add(Pair.create("Mecha", "21"));
//            list.add(Pair.create("Medical", "22"));
//            list.add(Pair.create("Mystery", "24"));
//            list.add(Pair.create("One Shot", "25"));
//            list.add(Pair.create("Psychological", "26"));
//            list.add(Pair.create("Romance", "27"));
//            list.add(Pair.create("Sci Fi", "29"));
//            return list;
//        }
//
//        @Override
//        protected boolean hasArea() {
//            return false;
//        }
//
//        @Override
//        protected List<Pair<String, String>> getArea() {
//            List<Pair<String, String>> list = new ArrayList<>();
//            list.add(Pair.create("All", ""));
//            return list;
//        }
//
//        @Override
//        public boolean hasProgress() {
//            return true;
//        }
//
//        @Override
//        public List<Pair<String, String>> getProgress() {
//            List<Pair<String, String>> list = new ArrayList<>();
//            list.add(Pair.create("All", "all"));
//            list.add(Pair.create("Ongoing", "Ongoing"));
//            list.add(Pair.create("Completed", "Completed"));
//            return list;
//        }
//
//        @Override
//        protected boolean hasOrder() {
//            return false;
//        }
//
//        @Override
//        protected List<Pair<String, String>> getOrder() {
//            List<Pair<String, String>> list = new ArrayList<>();
//            list.add(Pair.create("All", ""));
//            return list;
//        }
//    }
//}
