package com.xyrlsz.xcimoc.core;

import static com.xyrlsz.xcimoc.ui.activity.SearchActivity.SEARCH_AUTHOR;
import static com.xyrlsz.xcimoc.ui.activity.SearchActivity.SEARCH_TITLE;

import android.util.Pair;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.manager.ChapterManager;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.parser.Parser;
import com.xyrlsz.xcimoc.parser.SearchIterator;
import com.xyrlsz.xcimoc.parser.WebParser;
import com.xyrlsz.xcimoc.rx.RxBus;
import com.xyrlsz.xcimoc.rx.RxEvent;
import com.xyrlsz.xcimoc.utils.IdCreator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2016/8/20.
 */
public class Manga {

    private static boolean indexOfIgnoreCase(String str, String search) {
        return str.toLowerCase().contains(search.toLowerCase());
    }

    public static boolean indexOfIgnoreCase(String str, String search, boolean stSame) {
        if (stSame) {
            try {
                String s1 = ZhConverterUtil.toSimple(str);
                String s2 = ZhConverterUtil.toSimple(search);
                return s1.toLowerCase().contains(s2.toLowerCase());
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return str.toLowerCase().contains(search.toLowerCase());
        }
    }

    public static Observable<Comic> getSearchResult(final MangaParser parser, final String keyword, final int page, final boolean strictSearch) {
        return Observable.create(new Observable.OnSubscribe<Comic>() {
            @Override
            public void call(Subscriber<? super Comic> subscriber) {
                try {
                    Request request = parser.getSearchRequest(keyword, page);
                    Random random = new Random();
                    String html;
                    if (parser.isGetSearchUseWebParser()) {
                        WebParser webParser = new WebParser(App.getAppContext(), request.url().toString(), request.headers());
                        html = webParser.getHtmlStrSync();
                    } else {
                        html = getResponseBody(App.getHttpClient(), request);
                    }
                    SearchIterator iterator = parser.getSearchIterator(html, page);
                    if (iterator == null || iterator.empty()) {
                        throw new Exception();
                    }
                    while (iterator.hasNext()) {
                        Comic comic = iterator.next();
//                        if (comic != null && (comic.getTitle().indexOf(keyword) != -1 || comic.getAuthor().indexOf(keyword) != -1)) {
                        if (comic != null
                                && (indexOfIgnoreCase(comic.getTitle(), keyword)
                                || indexOfIgnoreCase(comic.getAuthor(), keyword)
                                || (!strictSearch))) {
                            subscriber.onNext(comic);
                            Thread.sleep(random.nextInt(200));
                        }
                    }
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Comic> getSearchResult(final MangaParser parser, final String keyword, final int page, final boolean strictSearch, final boolean stSame) {
        return Observable.create(new Observable.OnSubscribe<Comic>() {
            @Override
            public void call(Subscriber<? super Comic> subscriber) {
                try {
                    Request request = parser.getSearchRequest(keyword, page);
                    Random random = new Random();
                    String html;
                    if (parser.isGetSearchUseWebParser()) {
                        WebParser webParser = new WebParser(App.getAppContext(), request.url().toString(), request.headers());
                        html = webParser.getHtmlStrSync();
                    } else {
                        html = getResponseBody(App.getHttpClient(), request);
                    }
                    SearchIterator iterator = parser.getSearchIterator(html, page);
                    if (iterator == null || iterator.empty()) {
                        throw new Exception();
                    }
                    while (iterator.hasNext()) {
                        Comic comic = iterator.next();
//                        if (comic != null && (comic.getTitle().indexOf(keyword) != -1 || comic.getAuthor().indexOf(keyword) != -1)) {
                        if (comic != null
                                && (indexOfIgnoreCase(comic.getTitle(), keyword, stSame)
                                || indexOfIgnoreCase(comic.getAuthor(), keyword, stSame)
                                || (!strictSearch))) {
                            subscriber.onNext(comic);
                            Thread.sleep(random.nextInt(200));
                        }
                    }
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Comic> getSearchResult(final MangaParser parser, final String keyword, final int page, final boolean strictSearch, final boolean stSame, final int searchType) {
        return Observable.create(new Observable.OnSubscribe<Comic>() {
            @Override
            public void call(Subscriber<? super Comic> subscriber) {
                try {
                    Request request = parser.getSearchRequest(keyword, page);
                    Random random = new Random();
                    String html;
                    if (parser.isGetSearchUseWebParser()) {
                        WebParser webParser = new WebParser(App.getAppContext(), request.url().toString(), request.headers());
                        html = webParser.getHtmlStrSync();
                    } else {
                        html = getResponseBody(App.getHttpClient(), request);
                    }
                    SearchIterator iterator = parser.getSearchIterator(html, page);
                    if (iterator == null || iterator.empty()) {
                        throw new Exception();
                    }
                    while (iterator.hasNext()) {
                        Comic comic = iterator.next();
//                        if (comic != null && (comic.getTitle().indexOf(keyword) != -1 || comic.getAuthor().indexOf(keyword) != -1)) {

                        if (searchType == SEARCH_TITLE) {
                            if (comic != null
                                    && (indexOfIgnoreCase(comic.getTitle(), keyword, stSame)
                                    || (!strictSearch))) {
                                subscriber.onNext(comic);
                                Thread.sleep(random.nextInt(200));
                            }
                        } else if (searchType == SEARCH_AUTHOR) {
                            if (comic != null) {
                                String[] separators = {",", ";", "、", "，", "；", " ", "/", "\\"};
                                boolean findAuthor = false;
                                for (String separator : separators) {
                                    String[] keywords = keyword.strip().split(separator);
                                    for (String key : keywords) {
                                        if (indexOfIgnoreCase(comic.getAuthor().strip(), key.strip(), stSame)) {
                                            findAuthor = true;
                                            break;
                                        }
                                    }
                                }

                                if (findAuthor || (!strictSearch)) {
                                    subscriber.onNext(comic);
                                    Thread.sleep(random.nextInt(200));
                                }
                            }
                        }
                    }
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<List<Chapter>> getComicInfo(final MangaParser parser, final Comic comic) {
        return Observable.create(new Observable.OnSubscribe<List<Chapter>>() {
            @Override
            public void call(Subscriber<? super List<Chapter>> subscriber) {
                try {
//                    Mongo mongo = new Mongo();
                    List<Chapter> list = new ArrayList<>();

//                    list.addAll(mongo.QueryComicBase(comic));
                    if (list.isEmpty()) {
                        comic.setUrl(parser.getUrl(comic.getCid()));
                        Request request = parser.getInfoRequest(comic.getCid());
                        String html;
                        if (parser.isParseInfoUseWebParser()) {
                            WebParser webParser = new WebParser(App.getAppContext(), request.url().toString(), request.headers());
                            html = webParser.getHtmlStrSync();
                        } else {
                            html = getResponseBody(App.getHttpClient(), request);
                        }
                        Comic newComic = parser.parseInfo(html, comic);
                        RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_UPDATE_INFO, newComic));
                        request = parser.getChapterRequest(html, comic.getCid());
                        if (request != null) {
                            if (parser.isParseChapterUseWebParser()) {
                                WebParser webParser = new WebParser(App.getAppContext(), request.url().toString(), request.headers());
                                html = webParser.getHtmlStrSync();
                            } else {
                                html = getResponseBody(App.getHttpClient(), request);
                            }
                        }
                        Long sourceComic = IdCreator.createSourceComic(comic);
                        list = parser.parseChapter(html, comic, sourceComic);
                        if (list == null) {
                            list = parser.parseChapter(html);
                        }
//                        mongo.UpdateComicBase(comic, list);
                    }
                    if (!list.isEmpty()) {
                        subscriber.onNext(list);
                        subscriber.onCompleted();
                    } else {
                        throw new ParseErrorException();
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<List<Comic>> getCategoryComic(final Parser parser, final String format,
                                                           final int page) {
        return Observable.create(new Observable.OnSubscribe<List<Comic>>() {
            @Override
            public void call(Subscriber<? super List<Comic>> subscriber) {
                try {
                    Request request = parser.getCategoryRequest(format, page);
                    String html = getResponseBody(App.getHttpClient(), request);
                    List<Comic> list = parser.parseCategory(html, page);
                    if (!list.isEmpty()) {
                        subscriber.onNext(list);
                        subscriber.onCompleted();
                    } else {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<List<ImageUrl>> getChapterImage(final Chapter chapter,
                                                             final MangaParser parser,
                                                             final String cid,
                                                             final String path) {
        return Observable.create(new Observable.OnSubscribe<List<ImageUrl>>() {
            @Override
            public void call(Subscriber<? super List<ImageUrl>> subscriber) {
                String html;
//                Mongo mongo = new Mongo();
                List<ImageUrl> list = new ArrayList<>();
                try {
//                    List<ImageUrl> listdoc = new ArrayList<>();
//                    list.addAll(mongo.QueryComicChapter(mComic, path));
                    if (list.isEmpty()) {
                        Request request = parser.getImagesRequest(cid, path);
                        if (parser.isParseImagesUseWebParser()) {
                            String url = request.url().toString();
                            WebParser webParser = new WebParser(App.getAppContext(), url, request.headers());

                            html = webParser.getHtmlStrSync(); // 同步获取 HTML
                            list = parser.parseImages(html, chapter);

                        } else {
                            html = getResponseBody(App.getHttpClient(), request);
                            list = parser.parseImages(html, chapter);
                            if (list == null || list.isEmpty()) {
                                list = parser.parseImages(html);
                            }
//                        if (!list.isEmpty()) {
//                            mongo.InsertComicChapter(mComic, path, list);
//                        }
                        }
                    }

                    if (list.isEmpty()) {
                        throw new Exception();
                    } else {
                        for (ImageUrl imageUrl : list) {
                            imageUrl.setChapter(path);
                        }
                        subscriber.onNext(list);
                        subscriber.onCompleted();
                    }
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static List<ImageUrl> getImageUrls(MangaParser parser, int source, String cid, String path, String title, ChapterManager mChapterManager) throws InterruptedIOException {
        List<ImageUrl> list = new ArrayList<>();
//        Mongo mongo = new Mongo();
        Response response = null;
        try {
//            list.addAll(mongo.QueryComicChapter(source, cid, path));
            if (!list.isEmpty()) {
                return list;
            }
            Request request = parser.getImagesRequest(cid, path);
            if (!parser.isParseImagesUseWebParser()) {
                response = Objects.requireNonNull(App.getHttpClient()).newCall(request).execute();
                if (response.isSuccessful()) {
                    List<Chapter> chapter = mChapterManager.getChapter(path, title);
                    if (chapter != null && !chapter.isEmpty()) {
                        list.addAll(parser.parseImages(response.body().string(), chapter.get(0)));
                    }
                    if (list.isEmpty()) {
                        list.addAll(parser.parseImages(response.body().string()));
                    }
//                mongo.InsertComicChapter(source, cid, path, list);
                } else {
                    throw new NetworkErrorException();
                }
            } else {
                WebParser webParser = new WebParser(App.getAppContext(), request.url().toString(), request.headers());

                String html = webParser.getHtmlStrSync();
                List<Chapter> chapter = mChapterManager.getChapter(path, title);
                if (chapter != null && !chapter.isEmpty()) {
                    list.addAll(parser.parseImages(html, chapter.get(0)));
                }
                if (list.isEmpty()) {
                    list.addAll(parser.parseImages(html));
                }
            }

        } catch (InterruptedIOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return list;
    }

    public static String getLazyUrl(MangaParser parser, String url) throws InterruptedIOException {
        Response response = null;
        try {
            Request request = parser.getLazyRequest(url);
            response = Objects.requireNonNull(App.getHttpClient()).newCall(request).execute();
            if (response.isSuccessful()) {
                if (parser.isParseImagesLazyUseWebParser()) {
                    WebParser webParser = new WebParser(App.getAppContext(), request.url().toString(), request.headers());
                    return parser.parseLazy(webParser.getHtmlStrSync(), url);
                }
                return parser.parseLazy(response.body().string(), url);
            } else {
                throw new NetworkErrorException();
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }

    public static Observable<String> loadLazyUrl(final MangaParser parser, final String url) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                Request request = parser.getLazyRequest(url);
                String newUrl = null;
                try {
                    if (parser.isParseImagesLazyUseWebParser()) {
                        WebParser webParser = new WebParser(App.getAppContext(), request.url().toString(), request.headers());
                        newUrl = parser.parseLazy(webParser.getHtmlStrSync(), url);
                    } else {
                        newUrl = parser.parseLazy(getResponseBody(App.getHttpClient(), request), url);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                subscriber.onNext(newUrl);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<List<String>> loadAutoComplete(final String keyword) {
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
//                RequestBody body = new FormBody.Builder()
//                        .add("key", keyword)
//                        .add("s", "1")
//                        .build();
//                Request request = new Request.Builder()
//                        .url("http://m.ikanman.com/support/word.ashx")
//                        .post(body)
//                        .build();
                Request request = new Request.Builder()
                        .url("http://m.ac.qq.com/search/smart?word=" + keyword)
                        .build();
                try {
                    String jsonString = getResponseBody(App.getHttpClient(), request);
//                    JSONArray array = new JSONArray(jsonString);
                    JSONObject jsonObject = new JSONObject(jsonString);
                    JSONArray array = jsonObject.getJSONArray("data");
                    List<String> list = new ArrayList<>();
                    for (int i = 0; i != array.length(); ++i) {
//                        list.add(array.getJSONObject(i).getString("t"));
                        list.add(array.getJSONObject(i).getString("title"));
                    }
                    subscriber.onNext(list);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Comic> checkUpdate(
            final SourceManager manager, final List<Comic> list) {
        return Observable.from(list)
                .flatMap(comic -> Observable.just(comic)
                        .subscribeOn(Schedulers.io())  // 每个 Comic 分配到不同的 IO 线程
                        .map(c -> {
                            try {
                                OkHttpClient client = new OkHttpClient.Builder()
                                        .connectTimeout(1500, TimeUnit.MILLISECONDS)
                                        .readTimeout(1500, TimeUnit.MILLISECONDS)
                                        .build();

                                Parser parser = manager.getParser(c.getSource());
                                Request request = parser.getCheckRequest(c.getCid());
                                if (request == null) {
                                    request = parser.getInfoRequest(c.getCid());
                                }

                                String update = parser.parseCheck(getResponseBody(client, request));
                                Pair<Boolean, Integer> checkRes = new Pair<>(false, 0);
                                if (update == null || update.isEmpty()) {
                                    checkRes = parser.checkUpdateByChapterCount(getResponseBody(client, request), c);
                                }
                                if ((c.getUpdate() != null && update != null && !update.isEmpty() && !c.getUpdate().equals(update))
                                        || checkRes.first) {
                                    c.setFavorite(System.currentTimeMillis());
                                    c.setUpdate(update);
                                    if (checkRes.first) {
                                        c.setChapterCount(checkRes.second);
                                    }
                                    c.setHighlight(true);
                                    return c;  // 返回更新后的 Comic
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;  // 无更新或出错时返回 null
                        }), 10
                )
                .onBackpressureBuffer();  // 防止背压问题
    }

    //    public static Observable<Comic> checkUpdate(
//            final SourceManager manager, final List<Comic> list) {
//        return Observable.create(new Observable.OnSubscribe<Comic>() {
//            @Override
//            public void call(Subscriber<? super Comic> subscriber) {
//                OkHttpClient client = new OkHttpClient.Builder()
//                        .connectTimeout(1500, TimeUnit.MILLISECONDS)
//                        .readTimeout(1500, TimeUnit.MILLISECONDS)
//                        .build();
//                for (Comic comic : list) {
//                    try {
//                        Parser parser = manager.getParser(comic.getSource());
//                        Request request = parser.getCheckRequest(comic.getCid());
//                        if (request == null) {
//                            request = parser.getInfoRequest(comic.getCid());
//                        }
//                        String update = parser.parseCheck(getResponseBody(client, request));
//                        if ((comic.getUpdate() != null && update != null && !comic.getUpdate().equals(update))
//                                || (update == null && parser.checkUpdateByChapterCount(getResponseBody(client, request), comic))) {
//                            comic.setFavorite(System.currentTimeMillis());
//                            comic.setUpdate(update);
//                            comic.setHighlight(true);
//                            subscriber.onNext(comic);
//                            continue;
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    subscriber.onNext(null);
//                }
//                subscriber.onCompleted();
//            }
//        }).subscribeOn(Schedulers.io());
//    }

    public static String getResponseBody(OkHttpClient client, Request request) throws NetworkErrorException {
        return getResponseBody(client, request, true);
    }

    private static String getResponseBody(OkHttpClient client, Request request, boolean retry) throws NetworkErrorException {
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                byte[] bodybytes = response.body().bytes();
                String body = new String(bodybytes);
                Matcher m = Pattern.compile("charset=([\\w\\-]+)").matcher(body);
                if (m.find()) {
                    body = new String(bodybytes, Objects.requireNonNull(m.group(1)));
                }
                return body;
            } else if (retry)
                return getResponseBody(client, request, false);
        } catch (Exception e) {
            e.printStackTrace();
            if (retry)
                return getResponseBody(client, request, false);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        throw new NetworkErrorException();
    }

    public static class ParseErrorException extends Exception {
    }

    public static class NetworkErrorException extends Exception {
    }

}
