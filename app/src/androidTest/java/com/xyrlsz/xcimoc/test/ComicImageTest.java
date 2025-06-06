package com.xyrlsz.xcimoc.test;

import android.util.Pair;

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
import java.util.concurrent.CountDownLatch;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class ComicImageTest {

    public static void test(final AppGetter appGetter, final TestCallBack callBack) throws InterruptedException {
        final Map<Integer, String> dataSet = ComicTestSet.getComicTestSet();
        final SourceManager sourceManager = SourceManager.getInstance(appGetter);
        CountDownLatch latch = new CountDownLatch(dataSet.size());

        sourceManager.list()
                .flatMap(Observable::from)
                .flatMap(source -> {
                    final MangaParser parser = sourceManager.getParser(source.getType());
                    final String comicId = dataSet.get(source.getType());

                    if (comicId == null) {
                        return Observable.just(new Pair<>(source, false));
                    }

                    Comic comic = new Comic(source.getType(), comicId);

                    // 获取章节列表
                    return Manga.getComicInfo(parser, comic)
                            .flatMap(chapters -> {
                                if (chapters.isEmpty()) {
                                    return Observable.just(new Pair<>(source, false));
                                }

                                Chapter firstChapter = chapters.get(0);
                                // 获取图片
                                return Manga.getChapterImage(firstChapter, parser, comicId, firstChapter.getPath())
                                        .map(imageUrls -> new Pair<>(source, imageUrls != null && !imageUrls.isEmpty()))
                                        .onErrorReturn(throwable -> new Pair<>(source, false));
                            })
                            .onErrorReturn(throwable -> new Pair<>(source, false));
                })
                .distinct(result -> result.first.getType()) // 去重
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
                    System.err.println("测试发生错误: " + throwable.getMessage());
                });

        latch.await(); // 等待所有任务结束
    }
}


