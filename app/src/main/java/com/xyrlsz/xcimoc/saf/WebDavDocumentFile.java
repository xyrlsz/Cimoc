package com.xyrlsz.xcimoc.saf;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.xyrlsz.xcimoc.core.WebDavConf;
import com.xyrlsz.xcimoc.utils.BinStreamUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class WebDavDocumentFile extends DocumentFile {
    private static final Sardine mSardine = WebDavConf.sardine;
    private final String mWebDavUrl = WebDavConf.url + "/cimoc";
    private final String mCurrentPath;
    private DavResource mDavResource;

    public WebDavDocumentFile(DocumentFile parent) {
        super(parent);
        if (parent == null) {
            mCurrentPath = mWebDavUrl;
        } else {
            WebDavDocumentFile tmp = (WebDavDocumentFile) parent;
            mCurrentPath = tmp.getCurrentPath();
        }

        // 使用 RxJava 异步获取 DavResource
        Observable.create((Observable.OnSubscribe<DavResource>) subscriber -> {
                    try {
                        List<DavResource> resources = mSardine.list(mWebDavUrl, 0);
                        if (!resources.isEmpty()) {
                            subscriber.onNext(resources.get(0));
                        } else {
                            subscriber.onNext(null);
                        }
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<>() {
                    @Override
                    public void onNext(DavResource resource) {
                        mDavResource = resource;
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });
    }

    // 传入currPath
    WebDavDocumentFile(DocumentFile parent, String currPath, DavResource resource) {
        super(parent);
        mCurrentPath = currPath;
        mDavResource = resource;
    }

    // 传入相对路径
    public WebDavDocumentFile(WebDavDocumentFile parent, String path) {
        super(parent);

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        mCurrentPath = parent.getCurrentPath() + "/" + path;
        Observable.create((Observable.OnSubscribe<DavResource>) subscriber -> {
                    try {
                        if (!mSardine.exists(mCurrentPath)) {
                            mSardine.createDirectory(mCurrentPath);
                        }
                        List<DavResource> resources = mSardine.list(mCurrentPath, 0);
                        if (!resources.isEmpty()) {
                            subscriber.onNext(resources.get(0));
                        } else {
                            subscriber.onNext(null);
                        }
                        subscriber.onCompleted();
                    } catch (IOException e) {
                        subscriber.onError(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<>() {
                    @Override
                    public void onNext(DavResource resource) {
                        mDavResource = resource;
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                });

    }

    private static String getTypeForName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase();
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    public static void UploadFile(File src, String urlPath) {

        try {
            // 上传文件到 WebDAV 服务器
            mSardine.put(urlPath, src, "application/octet-stream");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void UploadStreamFile(InputStream inputStream, String urlPath) {

        try {
            // 上传文件到 WebDAV 服务器
            mSardine.put(urlPath, BinStreamUtils.readAllBytesCompat(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static InputStream getInputStream(String path) throws FileNotFoundException {
        try {
            return new BufferedInputStream(mSardine.get(path));
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public String getCurrentPath() {
        return mCurrentPath;
    }

    @Override
    public DocumentFile createFile(String displayName) {
        String newPath = mCurrentPath + "/" + displayName;
        try {
            if (!mSardine.exists(newPath)) {
                // 创建一个空文件
                mSardine.put(newPath, new byte[0]);
                List<DavResource> resources = mSardine.list(newPath, 0);
                if (!resources.isEmpty()) {
                    return new WebDavDocumentFile(this, newPath, resources.get(0));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DocumentFile createDirectory(String displayName) {
        String newPath = mCurrentPath + "/" + displayName;
        try {
            if (!mSardine.exists(newPath)) {
                mSardine.createDirectory(newPath);
                List<DavResource> resources = mSardine.list(newPath, 0);
                if (!resources.isEmpty()) {
                    return new WebDavDocumentFile(this, newPath, resources.get(0));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Uri getUri() {
        return Uri.parse(mCurrentPath);
    }

    @Override
    public String getName() {
        return mDavResource != null ? mDavResource.getName() : "";
    }

    @Override
    public String getType() {
        if (mDavResource != null && !mDavResource.isDirectory()) {
            return getTypeForName(mDavResource.getName());
        }
        return null;
    }

    @Override
    public boolean isDirectory() {
        return mDavResource != null && mDavResource.isDirectory();
    }

    @Override
    public boolean isFile() {
        return mDavResource != null && !mDavResource.isDirectory();
    }

    @Override
    public long length() {
        return mDavResource != null ? mDavResource.getContentLength() : 0;
    }

    @Override
    public boolean canRead() {
        try {
            return mSardine.exists(mCurrentPath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean canWrite() {
        try {
            return mSardine.exists(mCurrentPath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean delete() {
        try {
            mSardine.delete(mCurrentPath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean exists() {
        try {
            return mSardine.exists(mCurrentPath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        try {
            return new BufferedInputStream(mSardine.get(mCurrentPath));
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public List<DocumentFile> listFiles(DocumentFileFilter filter, Comparator<? super DocumentFile> comp) {
        final ArrayList<DocumentFile> results = new ArrayList<>();
        try {
            List<DavResource> resources = mSardine.list(mCurrentPath);
            // 跳过第一个资源，因为它是当前目录
            for (int i = 1; i < resources.size(); i++) {
                DavResource resource = resources.get(i);
                String path = mCurrentPath + "/" + resource.getName();
                DocumentFile doc = new WebDavDocumentFile(this, path, resource);
                if (filter == null || filter.call(doc)) {
                    results.add(doc);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (comp != null) {
            Collections.sort(results, comp);
        }
        return results;
    }

    @Override
    public DocumentFile[] listFiles() {
        List<DocumentFile> files = listFiles(null, null);
        return files.toArray(new DocumentFile[0]);
    }

    @Override
    public void refresh() {
        try {
            List<DavResource> resources = mSardine.list(mCurrentPath, 0);
            if (!resources.isEmpty()) {
                mDavResource = resources.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public DocumentFile findFile(String displayName) {
        String targetPath = mCurrentPath + "/" + displayName;
        try {
            if (mSardine.exists(targetPath)) {
                List<DavResource> resources = mSardine.list(targetPath, 0);
                if (!resources.isEmpty()) {
                    return new WebDavDocumentFile(this, targetPath, resources.get(0));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean renameTo(String displayName) {
        String newPath = mCurrentPath.substring(0, mCurrentPath.lastIndexOf('/')) + "/" + displayName;
        try {
            mSardine.move(mCurrentPath, newPath);
            mDavResource = mSardine.list(newPath, 0).get(0);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}