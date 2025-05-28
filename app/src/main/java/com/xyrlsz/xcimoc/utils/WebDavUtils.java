package com.xyrlsz.xcimoc.utils;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.saf.CimocDocumentFile;
import com.xyrlsz.xcimoc.saf.WebDavCimocDocumentFile;

import java.io.IOException;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class WebDavUtils {

    private static void copyFilesByOver(CimocDocumentFile src, WebDavCimocDocumentFile dst, boolean isOverwrite) throws IOException {
        if (src.isFile()) {
            DocumentUtils.writeBinaryToFile(App.getApp().getContentResolver(), src, dst);
        } else if (src.isDirectory()) {
            for (CimocDocumentFile file : src.listFiles()) {
                if (file.isFile()) {
                    WebDavCimocDocumentFile newDst = (WebDavCimocDocumentFile) dst.findFile(file.getName());
                    if (newDst == null) {
                        newDst = (WebDavCimocDocumentFile) dst.createFile(file.getName());
                    } else if (!isOverwrite) {
                        return;
                    }
                    DocumentUtils.writeBinaryToFile(App.getApp().getContentResolver(), file, newDst);
                } else if (file.isDirectory()) {
                    WebDavCimocDocumentFile newDst = (WebDavCimocDocumentFile) dst.createDirectory(file.getName());
                    copyFilesByOver(file, newDst, isOverwrite);
                }
            }
        }
    }

    public static Observable<Integer> upload2WebDav(final CimocDocumentFile src, final WebDavCimocDocumentFile dst, boolean isOverwrite) {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                try {
                    // 执行文件复制
                    copyFilesByOver(src, dst, isOverwrite);
                    // 文件复制完成后，通知观察者
                    subscriber.onNext(1); // 传递一个表示成功的结果（这里可以根据需要传递不同的值）
                    subscriber.onCompleted(); // 完成
                } catch (IOException e) {
                    e.printStackTrace();
                    subscriber.onError(e); // 如果发生异常，通知错误
                }
            }
        }).subscribeOn(Schedulers.io());
    }

}
