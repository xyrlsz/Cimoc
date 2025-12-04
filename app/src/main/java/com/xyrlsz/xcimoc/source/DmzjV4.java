package com.xyrlsz.xcimoc.source;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.JsonIterator;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.IdCreator;
import com.xyrlsz.xcimoc.utils.StringUtils;
import com.xyrlsz.xcimoc.utils.TimestampUtils;
import com.xyrlsz.xcimoc.utils.UicodeBackslashU;
import com.xyrlsz.xcimoc.utils.dmzj.RsaDecryptor;
import com.xyrlsz.xcimoc.utils.dmzj.protos.DmzjComic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import okhttp3.Headers;
import okhttp3.Request;

public class DmzjV4 extends MangaParser {
    public static final int TYPE = 112;
    public static final String DEFAULT_TITLE = "动漫之家v4";
    private static final String baseUrl = "https://m.idmzj.com";
    private static final String pcBaseUrl = "https://www.idmzj.com";
    String V3_API_URL = "https://nnv3api.idmzj.com";
    String V4_API_URL = "https://nnv4api.dmzj.com";
    String COOKIES = "";
    String UID = "";
    private final SharedPreferences sharedPreferences;

    public DmzjV4(Source source) {
        //        init(source, new Category());
        init(source);
        sharedPreferences =
                App.getAppContext().getSharedPreferences(Constants.DMZJ_SHARED, MODE_PRIVATE);
        UID = sharedPreferences.getString(Constants.DMZJ_SHARED_UID, "");
        COOKIES = sharedPreferences.getString(Constants.DMZJ_SHARED_COOKIES, "");
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }


    private String getUID() {
        if (UID.isEmpty()) {
            UID = sharedPreferences.getString(Constants.DMZJ_SHARED_UID, "");
        }
        return UID;
    }

    private String getCOOKIES() {
        if (COOKIES.isEmpty()) {
            COOKIES = sharedPreferences.getString(Constants.DMZJ_SHARED_COOKIES, "");
        }
        return COOKIES;
    }

    @Override
    protected void initUrlFilterList() {
//        filter.add(new UrlFilter("dmzj.com", "info/(\\d+).html"));
//        filter.add(new UrlFilter("idmzj.com", "info/(\\d+).html"));
    }

    @Override
    public Request getSearchRequest(String keyword, int page) {
        if (page == 1) {
            String url =
                    StringUtils.format("%s/search/showWithLevel/0/%s/0.json", V3_API_URL, keyword);
            return new Request.Builder().url(url).build();
        }
        return null;
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) {
        try {
            String decodeJsonString = UicodeBackslashU.unicodeToCn(html).replace("\\/", "/");
            return new JsonIterator(new JSONArray(decodeJsonString)) {
                @Override
                protected Comic parse(JSONObject object) {
                    try {
                        String cid = object.getString("id");
                        String title = object.getString("title");
                        String cover = object.getString("cover");
                        String author = object.optString("authors");
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
        String uid = getUID();
        if (uid.isEmpty()) {

            HintUtils.showToast(App.getAppContext(),
                    App.getAppResources().getString(R.string.dmzj_should_login));
        }
        String url =
                StringUtils.format("%s/comic/detail/%s?uid=%s&channel=android", V4_API_URL, cid, uid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public Comic parseInfo(String html, Comic comic) {
        try {
            byte[] data = RsaDecryptor.decryptBytes(html);
            DmzjComic.ComicDetailResponse response = DmzjComic.ComicDetailResponse.parseFrom(data);
            String intro = response.getData().getDescription();
            String title = response.getData().getTitle();
            String cover = response.getData().getCover();
            StringBuilder author = new StringBuilder();
            for (DmzjComic.ComicTag tag : response.getData().getAuthorsList()) {
                author.append(tag.getTagName()).append(" ");
            }
            String update =
                    TimestampUtils.formatTimestampSeconds(response.getData().getLastUpdatetime());
            boolean status = isFinish(response.getData().getStatus(0).getTagName());
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
            byte[] data = RsaDecryptor.decryptBytes(html);
            DmzjComic.ComicDetailResponse response = DmzjComic.ComicDetailResponse.parseFrom(data);
            int k = 1;
            for (DmzjComic.ComicChapterList chapterList : response.getData().getChaptersList()) {
                String tag = chapterList.getTitle();
                for (DmzjComic.ComicChapterInfo chapter : chapterList.getDataList()) {
                    String title = chapter.getChapterTitle();
                    String chapter_id = Long.toString(chapter.getChapterId());
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
        String uid = getUID();
        String url =
                StringUtils.format("%s/comic/chapter/%s/%s?uid=%s", V4_API_URL, cid, path, uid);
        return new Request.Builder().url(url).build();
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) {
        List<ImageUrl> list = new LinkedList<>();
        try {
            byte[] data = RsaDecryptor.decryptBytes(html);
            DmzjComic.ComicChapterResponse response =
                    DmzjComic.ComicChapterResponse.parseFrom(data);
            int i = 1;
            for (String PicUrl : response.getData().getPageUrlList()) {
                Long comicChapter = chapter.getId();
                Long id = IdCreator.createImageId(comicChapter, i);
                list.add(new ImageUrl(id, comicChapter, i++, PicUrl, false));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of("Referer", "https://images.idmzj.com/", "user-agent",
                "Dalvik/2.1.0 (Linux; U; Android 12; SM-N9700 Build/SP1A.210812.016);");
    }
}
