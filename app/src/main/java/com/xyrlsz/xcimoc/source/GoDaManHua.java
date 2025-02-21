package com.xyrlsz.xcimoc.source;

import android.annotation.SuppressLint;

import com.google.common.collect.Lists;
import com.xyrlsz.xcimoc.core.Manga;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.NodeIterator;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.soup.Node;
import com.xyrlsz.xcimoc.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;
import okhttp3.Request;

public class GoDaManHua extends MangaParser {
    public static final int TYPE = 108;
    public static final String DEFAULT_TITLE = "GoDa漫畫";
    private static final String baseUrl = "https://manhuafree.com";
    private static final String picBaseUrl = "https://f40-1-4.g-mh.online";
    private String _mid = "";

    public GoDaManHua(Source source) {
        init(source, null);
        setParseImagesUseWebParser(true);
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws Exception {
        if (page == 1) {
            String url = baseUrl + "/s/" + keyword;
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        List<Node> list = new Node(html).list(".cardlist > div.pb-2");
        if (list.isEmpty()) {
            return null;
        }
        return new NodeIterator(list) {
            @Override
            protected Comic parse(Node node) {
                String title = node.text(".cardtitle");
                String cover = node.src(".text-center > div > img");
                String cid = node.href("a").split("/")[2];
                return new Comic(TYPE, cid, title, cover, "", "");
            }
        };
    }

    @Override
    public Request getInfoRequest(String cid) {
        return new Request.Builder().url(baseUrl + "/manga/" + cid).build();
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException, JSONException {
        String regex = "<script type=\"application/ld\\+json\">(.*?)</script>";
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            String json = Objects.requireNonNull(matcher.group(1)).trim();
            JSONObject data = new JSONObject(json);
            String title = data.getString("name");
            String intro = data.getString("description");
            String cover = data.getString("image");
            String status = data.getString("creativeWorkStatus");
            StringBuilder author = new StringBuilder();
            JSONArray authorArray = data.getJSONArray("author");
            for (int i = 0; i < authorArray.length(); i++) {
                JSONObject authorObject = authorArray.getJSONObject(i);
                author.append(authorObject.getString("name"));
                if (i < authorArray.length() - 1) {
                    author.append(",");
                }
            }
            String update = data.getJSONObject("hasPart").getString("datePublished");
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(TimeZone.getDefault());

            Date date = null;
            try {
                date = isoFormat.parse(update);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            outputFormat.setTimeZone(TimeZone.getDefault());

            update = outputFormat.format(date);
            comic.setInfo(title, cover, update, intro, author.toString(), isFinish(status));

        }
        Node body = new Node(html);
        _mid = body.id("bookmarkData").attr("data-mid");
        return comic;
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        return new Request.Builder().url(StringUtils.format("https://api-get-v2.mgsearcher.com/api/manga/get?mid=%s", _mid))
                .addHeader("referer", baseUrl.concat("/"))
                .build();
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        List<Chapter> list = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(html);
        JSONArray chapters = jsonObject.getJSONObject("data").getJSONArray("chapters");

        for (int i = 0; i < chapters.length(); i++) {
            String title = chapters.getJSONObject(i).getJSONObject("attributes").getString("title");
            String path = chapters.getJSONObject(i).getLong("id") + "";
            list.add(new Chapter(Long.parseLong(sourceComic + "0" + i), sourceComic, title, path));
        }

        return Lists.reverse(list);
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        return new Request.Builder().url(StringUtils.format("https://api-get-v2.mgsearcher.com/api/chapter/getinfo?m=%s&c=%s", _mid, path))
                .addHeader("referer", baseUrl.concat("/"))
                .addHeader("Accept", "application/json")
                .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
                .build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        List<ImageUrl> list = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{.*\\}");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            html = matcher.group(0);
        }
        JSONArray images = new JSONObject(html)
                .getJSONObject("data")
                .getJSONObject("info")
                .getJSONObject("images")
                .getJSONArray("images");
        for (int i = 1; i <= images.length(); i++) {
            Long comicChapter = chapter.getId();
            Long id = Long.parseLong(comicChapter + "0" + i);
            String imgUrl = picBaseUrl + images.getJSONObject(i - 1).getString("url");
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false));
        }
        return list;
    }

    @Override
    public String getUrl(String cid) {
        return StringUtils.format("%s/manga/%s", baseUrl, cid);
    }

    @Override
    public Headers getHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36");
        headers.put("referer", baseUrl.concat("/"));
        return Headers.of(headers);
    }
}
