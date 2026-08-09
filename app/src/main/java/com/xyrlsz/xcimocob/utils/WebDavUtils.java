package com.xyrlsz.xcimocob.utils;

import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.core.WebDavClient;
import com.xyrlsz.xcimocob.core.WebDavConf;
import com.xyrlsz.xcimocob.saf.CimocDocumentFile;
import com.xyrlsz.xcimocob.saf.WebDavCimocDocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class WebDavUtils {

    /** 并行上传的最大并发数（OkHttp 每主机默认并发 5，取 6 以内避免大量排队） */
    private static final int MAX_CONCURRENCY = 6;

    /**
     * 并发上传本地备份目录到 WebDAV。
     *
     * 每个目录内的文件并行上传（每个文件只需一次 PUT，跳过冗余的
     * HEAD/空建文件/PROPFIND），子目录先 MKCOL 再递归并行上传。
     * 阻塞调用在 io 线程执行；返回上传成功的文件总数。
     */
    public static Observable<Integer> upload2WebDav(final CimocDocumentFile src, final WebDavCimocDocumentFile dst, boolean isOverwrite) {
        return Observable.defer(() -> {
            WebDavClient client = WebDavConf.client;
            if (client == null) {
                return Observable.error(new IOException("WebDAV 未初始化"));
            }
            // 确保目标根目录存在（mkdir -p）
            client.createDirectory(dst.getCurrentPath());
            return uploadRecursive(client, src, dst.getCurrentPath(), isOverwrite);
        })
                .subscribeOn(Schedulers.io())
                .reduce(0, Integer::sum)
                .toObservable();
    }

    /**
     * 递归上传一个本地文件或目录。
     *
     * @return 发射每个上传成功文件数（0 或 1）的 Observable
     */
    private static Observable<Integer> uploadRecursive(WebDavClient client, CimocDocumentFile src, String dstUrl, boolean isOverwrite) {
        if (src.isFile()) {
            return Observable.fromCallable(() -> {
                String target = dstUrl + "/" + src.getName();
                if (!isOverwrite && client.exists(target)) {
                    return 0;
                }
                InputStream input = App.getApp().getContentResolver().openInputStream(src.getUri());
                if (input == null) {
                    throw new IOException("无法打开文件: " + src.getUri());
                }
                try {
                    client.put(target, BinStreamUtils.readAllBytesCompat(input));
                    return 1;
                } finally {
                    input.close();
                }
            }).subscribeOn(Schedulers.io());
        }

        if (src.isDirectory()) {
            CimocDocumentFile[] children = src.listFiles();
            if (children == null || children.length == 0) {
                return Observable.just(0);
            }
            List<Observable<Integer>> sources = new ArrayList<>(children.length);
            for (CimocDocumentFile child : children) {
                if (child.isFile()) {
                    sources.add(uploadRecursive(client, child, dstUrl, isOverwrite));
                } else if (child.isDirectory()) {
                    String childDir = dstUrl + "/" + child.getName();
                    // 先确保子目录存在，再递归并行上传其内容
                    sources.add(Observable.defer(() -> {
                        client.createDirectory(childDir);
                        return uploadRecursive(client, child, childDir, isOverwrite);
                    }).subscribeOn(Schedulers.io()));
                }
            }
            // 限流并发上传，避免同时建立过多连接
            return Observable.merge(sources, MAX_CONCURRENCY);
        }
        return Observable.just(0);
    }
}
