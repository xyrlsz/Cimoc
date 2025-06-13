package com.xyrlsz.xcimoc.core;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import com.xyrlsz.xcimoc.model.Chapter;
import com.xyrlsz.xcimoc.model.ImageUrl;
import com.xyrlsz.xcimoc.saf.CimocDocumentFile;
import com.xyrlsz.xcimoc.utils.DecryptionUtils;
import com.xyrlsz.xcimoc.utils.DocumentUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2016/10/16.
 */

public class Storage {

    private static final String DOWNLOAD = "download";
    private static final String PICTURE = "picture";
    private static final String BACKUP = "backup";

    public static CimocDocumentFile initRoot(Context context, String uri) {
        if (uri == null || uri.isEmpty()) {
//            File file = new File(Environment.getExternalStorageDirectory(), "Cimoc");
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Cimoc");
//            File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Cimoc");
            if (file.exists() || file.mkdirs()) {
                return CimocDocumentFile.fromFile(file);
            } else {
                return null;
            }
        } else if (uri.startsWith("content")) {
            return CimocDocumentFile.fromTreeUri(context, Uri.parse(uri));
        } else if (uri.startsWith("file")) {
            return CimocDocumentFile.fromFile(new File(Objects.requireNonNull(Uri.parse(uri).getPath())));
        } else {
            return CimocDocumentFile.fromFile(new File(uri, "Cimoc"));
        }
    }

    private static boolean copyFile(ContentResolver resolver, CimocDocumentFile src,
                                    CimocDocumentFile parent, Subscriber<? super String> subscriber) {
        CimocDocumentFile file = DocumentUtils.getOrCreateFile(parent, src.getName());
        if (file != null) {
            subscriber.onNext(StringUtils.format("正在移动 %s...", src.getUri().getLastPathSegment()));
            try {
                DocumentUtils.writeBinaryToFile(resolver, src, file);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean copyDir(ContentResolver resolver, CimocDocumentFile src,
                                   CimocDocumentFile parent, Subscriber<? super String> subscriber) {
        if (src.isDirectory()) {
            CimocDocumentFile dir = DocumentUtils.getOrCreateSubDirectory(parent, src.getName());
            for (CimocDocumentFile file : src.listFiles()) {
                if (file.isDirectory()) {
                    if (!copyDir(resolver, file, dir, subscriber)) {
                        return false;
                    }
                } else if (!copyFile(resolver, file, dir, subscriber)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean copyDir(ContentResolver resolver, CimocDocumentFile src,
                                   CimocDocumentFile dst, String name, Subscriber<? super String> subscriber) {
        CimocDocumentFile file = src.findFile(name);
        if (file != null && file.isDirectory()) {
            return copyDir(resolver, file, dst, subscriber);
        }
        return true;
    }

    private static void deleteDir(CimocDocumentFile parent, String name, Subscriber<? super String> subscriber) {
        CimocDocumentFile file = parent.findFile(name);
        if (file != null && file.isDirectory()) {
            subscriber.onNext(StringUtils.format("正在删除 %s", file.getUri().getLastPathSegment()));
            file.delete();
        }
    }

    private static boolean isDirSame(CimocDocumentFile root, CimocDocumentFile dst) {
        return root.getUri().getScheme().equals("file") && dst.getUri().getPath().endsWith("primary:Cimoc") ||
                root.getUri().getPath().equals(dst.getUri().getPath());
    }

    public static Observable<String> moveRootDir(final ContentResolver resolver, final CimocDocumentFile root, final CimocDocumentFile dst) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (dst.canRead() && !isDirSame(root, dst)) {
                    root.refresh();
                    if (copyDir(resolver, root, dst, BACKUP, subscriber) &&
                            copyDir(resolver, root, dst, DOWNLOAD, subscriber) &&
                            copyDir(resolver, root, dst, PICTURE, subscriber)) {
                        deleteDir(root, BACKUP, subscriber);
                        deleteDir(root, DOWNLOAD, subscriber);
                        deleteDir(root, PICTURE, subscriber);
                        subscriber.onCompleted();
                    }
                }
                subscriber.onError(new Exception());
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<Uri> savePicture(final ContentResolver resolver, final CimocDocumentFile root,
                                              final InputStream stream, final String filename) {
        return Observable.create(new Observable.OnSubscribe<Uri>() {
            @Override
            public void call(Subscriber<? super Uri> subscriber) {
                try {
                    CimocDocumentFile dir = DocumentUtils.getOrCreateSubDirectory(root, PICTURE);
                    if (dir != null) {
                        CimocDocumentFile file = DocumentUtils.getOrCreateFile(dir, filename);
                        DocumentUtils.writeBinaryToFile(resolver, file, stream);
                        subscriber.onNext(file.getUri());
                        subscriber.onCompleted();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                subscriber.onError(new Exception());
            }
        }).subscribeOn(Schedulers.io());
    }

    public static List<ImageUrl> buildImageUrlFromDocumentFile(List<CimocDocumentFile> list, String chapterStr, int max, Chapter chapter) {
        int count = 0;
        List<ImageUrl> result = new ArrayList<>(list.size());
        for (CimocDocumentFile file : list) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try {
                BitmapFactory.decodeStream(file.openInputStream(), null, opts);
                String uri = file.getUri().toString();
                if (uri.startsWith("file")) {   // content:// 解码会出错 file:// 中文路径如果不解码 Fresco 读取不了
                    uri = DecryptionUtils.urlDecrypt(uri);
                }
                Long comicChapter = chapter.getId();
                Long id = Long.parseLong(comicChapter + "300" + count);
                ImageUrl image = new ImageUrl(id, chapter.getSourceComic(), ++count, uri, false);
                image.setHeight(opts.outHeight);
                image.setWidth(opts.outWidth);
                image.setChapter(chapterStr);
                result.add(image);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (count >= max) {
                break;
            }
        }
        return result;
    }

}
