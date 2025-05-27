package com.xyrlsz.xcimoc.test;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.core.Manga;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;

import java.util.List;

import rx.Observable;

public class SearchTest {
    public static void test(AppGetter appGetter, TestCallBack callBack) {
        Observable<List<Source>> sources = SourceManager.getInstance(appGetter).list();
        sources.subscribe(s -> {
            for (Source source : s) {
                MangaParser parser = SourceManager.getInstance(appGetter).getParser(source.getType());
                Observable<Comic> res = Manga.getSearchResult(parser, "a", 1, false, false);
                if (res != null) {
                    callBack.onSuccess(source);
                } else {
                    callBack.onFail(source);
                }
            }
        });
    }
}
