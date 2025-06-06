package com.xyrlsz.xcimoc.test;

import android.util.Pair;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.core.Manga;
import com.xyrlsz.xcimoc.helper.UpdateHelper;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import rx.Observable;

public class SearchTest {

    public static void test(AppGetter appGetter, TestCallBack callBack) throws InterruptedException {
        SourceManager sourceManager = SourceManager.getInstance(appGetter);
        Observable<List<Source>> sourcesObservable = sourceManager.list();
        Map<Integer, Source> t = UpdateHelper.getComicSourceTable();
        CountDownLatch latch = new CountDownLatch(t.size());

        sourcesObservable.flatMap(sources -> {
                    List<Observable<Pair<Source, Boolean>>> observables = new ArrayList<>();

                    for (Source source : sources) {
                        MangaParser parser = sourceManager.getParser(source.getType());

                        Observable<Pair<Source, Boolean>> result = Manga.getSearchResult(parser, "a", 1, false, false)
                                .take(1)
                                .map(comic -> new Pair<>(source, true))
                                .onErrorReturn(throwable -> new Pair<>(source, false))
                                .defaultIfEmpty(new Pair<>(source, false)); // handle null results

                        observables.add(result);
                    }

                    return Observable.merge(observables); // Wait for all
                })
                .toList()
                .subscribe(results -> {
                    Set<String> processed = new HashSet<>();
                    for (Pair<Source, Boolean> result : results) {
                        String key = result.first.getTitle();
                        if (processed.contains(key)) continue;
                        processed.add(key);

                        if (result.second) {
                            callBack.onSuccess(result.first);
                        } else {
                            callBack.onFail(result.first);
                        }
                        latch.countDown();
                    }
                });
        latch.await();
    }
}

