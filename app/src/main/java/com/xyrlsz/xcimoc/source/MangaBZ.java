package com.xyrlsz.xcimoc.source;

import android.os.Build;

import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.NodeIterator;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.UrlFilter;
import com.xyrlsz.xcimoc.soup.Node;
import com.xyrlsz.xcimoc.utils.DecryptionUtils;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

/**
 * Created by FEILONG on 2017/12/21.
 * need fix
 */
public class MangaBZ extends MangaParser {

    public static final int TYPE = 82;
    public static final String DEFAULT_TITLE = "MangaBZ";
    private String _cid = "";
    private String _path = "";

    public MangaBZ(Source source) {
        init(source);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true, "http://www.mangabz.com/");
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws UnsupportedEncodingException {
        String url = "http://www.mangabz.com/search?title=" + keyword + "&page=" + page;
        return new Request.Builder().url(url).build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        final Node body = new Node(html);
        return new NodeIterator(body.list(".mh-item")) {
            @Override
            protected Comic parse(Node node) {
                String cid = node.attr("a", "href").trim().replace("/", "");
                String title = node.text(".title");
                String cover = node.attr(".mh-cover", "src");
                String update = node.text(".chapter > a");
                String author = "";
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    @Override
    public String getUrl(String cid) {
        return "http://www.mangabz.com/" + cid + "/";
    }

    @Override
    protected void initUrlFilterList() {
        filter.add(new UrlFilter("www.mangabz.com", ".*", 0));
    }

    @Override
    public Request getInfoRequest(String cid) {
        String url = "http://www.mangabz.com/" + cid + "/";
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException {
        Node body = new Node(html);
        String title = body.text(".detail-info-title");
        String cover = body.src(".detail-info-cover");
        String update = StringUtils.match(
                "(..月..號 | ....-..-..)",
                body.text(".detail-list-form-title"), 1
        );
        String author = body.text(".detail-info-tip > span > a");
        String intro = body.text(".detail-info-content");
        boolean status = isFinish(".detail-list-form-title");
        comic.setInfo(title, cover, update, intro, author, status);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) {
        List<Chapter> list = new LinkedList<>();
        int i = 0;
        for (Node node : new Node(html).list("#chapterlistload > a")) {
            String title = node.attr("title");
            if (title.equals("")) {
                title = node.text();
            }
            String path = node.href().trim().replace("/", "");

            Long id = IdCreator.createChapterId(sourceComic, i++);
            list.add(new Chapter(id, sourceComic, title, path));
        }
        return list;
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        String url = "http://www.mangabz.com/" + path + "/";
        this._cid = cid;
        this._path = path;
        return new Request.Builder()
                .url(url)
                .build();
    }

    private String getValFromRegex(String html, String keyword, String searchfor) {
        Pattern pattern = Pattern.compile("var\\s+" + keyword + "\\s*=\\s*" + searchfor + "\\s*;");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        try {
            // get page num
            String mid = getValFromRegex(html, "MANGABZ_MID", "(\\w+)");
            String cid = getValFromRegex(html, "MANGABZ_CID", "(\\w+)");
            String sign = getValFromRegex(html, "MANGABZ_VIEWSIGN", "\"(\\w+)\"");
            int pageCount = Integer.parseInt(getValFromRegex(html, "MANGABZ_IMAGE_COUNT", "(\\d+)"));

            for (int i = 1; i <= pageCount; i++) {
                String url = "http://www.mangabz.com/" + _path + "/chapterimage.ashx?cid=" + cid +
                        "&page=" + i + "&key=&_cid=" + cid + "&_mid=" + mid +
                        "&_sign=" + sign + "&_dt=";

                Long comicChapter = chapter.getId();
                Long id = IdCreator.createImageId(comicChapter, i);
                list.add(new ImageUrl(id, comicChapter, i + 1, url, true, getHeader()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Request getLazyRequest(String url) {
        String dateFmt = "yyyy-MM-dd+HH:mm:ss";
        String dateStr;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime current = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFmt);
            dateStr = current.format(formatter);
        } else {
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat(dateFmt);
            dateStr = formatter.format(date);
        }

        return new Request.Builder()
                .addHeader("Referer", "http://www.mangabz.com/" + _path + "/")
                .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36"
                )
                .url(url + dateStr).build();
    }

    @Override
    public String parseLazy(String html, String url) {
        String image = DecryptionUtils.evalDecrypt(html).split(",")[0];
        return image;
    }

    @Override
    public Request getCheckRequest(String cid) {
        return getInfoRequest(cid);
    }

    @Override
    public String parseCheck(String html) {
        return StringUtils.match(
                "(..月..號 | ....-..-..)",
                new Node(html).text(".detail-list-form-title"), 1
        );
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "http://www.mangabz.com/");
    }

}