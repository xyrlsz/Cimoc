package com.xyrlsz.xcimoc.test;

import android.util.Pair;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.core.Manga;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.parser.MangaParser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import rx.Observable;

public class ComicInfoParserTest {

    public static void test(AppGetter appGetter, TestCallBack callBack) throws InterruptedException {
        Map<Integer, String> dataSet = ComicTestSet.getComicTestSet();
        SourceManager sourceManager = SourceManager.getInstance(appGetter);
        CountDownLatch latch = new CountDownLatch(dataSet.size());
        sourceManager.list()
                .flatMapIterable(sources -> sources)
                .flatMap(source -> {
                    MangaParser parser = sourceManager.getParser(source.getType());
                    String comicId = dataSet.get(source.getType());
                    if (comicId == null) {
                        // 无效 ID，直接标记失败
                        return Observable.just(new Pair<>(source, false));
                    }

                    Comic comic = new Comic(source.getType(), comicId);
                    return Manga.getComicInfo(parser, comic)
                            .map(chapters -> new Pair<>(source, true)) // 成功获取章节
                            .onErrorReturn(throwable -> new Pair<>(source, false)) // 发生异常
                            .defaultIfEmpty(new Pair<>(source, false)); // 返回为空
                })
                .toList()
                .subscribe(results -> {
                    for (Pair<Source, Boolean> result : results) {
                        if (result.second) {
                            callBack.onSuccess(result.first);
                        } else {
                            callBack.onFail(result.first);
                        }
                        latch.countDown();
                    }
                }, throwable -> {
                    // 全局错误处理
                    System.err.println("Error during ComicInfoParserTest: " + throwable.getMessage());
                });
        latch.await();
    }
}

