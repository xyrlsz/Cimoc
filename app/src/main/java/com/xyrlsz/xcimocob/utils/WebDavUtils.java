package com.xyrlsz.xcimocob.utils;

import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.saf.CimocDocumentFile;
import com.xyrlsz.xcimocob.saf.WebDavCimocDocumentFile;

import java.io.IOException;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
        return Observable.create((io.reactivex.rxjava3.core.ObservableOnSubscribe<Integer>) emitter -> {
            try {
                // 执行文件复制
                copyFilesByOver(src, dst, isOverwrite);
                // 文件复制完成后，通知观察者
                emitter.onNext(1); // 传递一个表示成功的结果（这里可以根据需要传递不同的值）
                emitter.onComplete(); // 完成
            } catch (IOException e) {
                e.printStackTrace();
                emitter.onError(e); // 如果发生异常，通知错误
            }
        }).subscribeOn(Schedulers.io());
    }

}
