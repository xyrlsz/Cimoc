package com.xyrlsz.xcimoc.test;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.core.Manga;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;

import java.util.List;
import java.util.Map;

import rx.Observable;

public class ComicImageTest {
    public static void test(AppGetter appGetter, TestCallBack callBack) {
        Map<Integer, String> dataSet = ComicTestSet.getComicTestSet();
        Observable<List<Source>> sources = SourceManager.getInstance(appGetter).list();
        sources.subscribe(s -> {
            for (Source source : s) {
                MangaParser parser = SourceManager.getInstance(appGetter).getParser(source.getType());
                Observable<List<Chapter>> res = Manga.getComicInfo(parser, new Comic(source.getType(), dataSet.get(source.getType())));
                if (res != null) {
                    res.subscribe(c -> {
                        for (Chapter chapter : c) {
                            Observable<List<ImageUrl>> image = Manga.getChapterImage(chapter, parser, dataSet.get(source.getType()), chapter.getPath());
                            if (image != null) {
                                callBack.onSuccess(source);
                            } else {
                                callBack.onFail(source);
                            }
                        }
                    });
                }
            }
        });
    }
}
