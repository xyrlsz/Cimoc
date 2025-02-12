package com.xyrlsz.xcimoc.saf;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.Constants;
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


public class WebDavDocumentFile extends DocumentFile {
    private final Sardine mSardine;
    private final String mUsername;
    private final String mPassword;
    private final String mWebDavUrl;
    private final String mCurrentPath;
    private DavResource mDavResource;

    public WebDavDocumentFile(DocumentFile parent) {
        super(parent);
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.WEBDAV_SHARED, Context.MODE_PRIVATE);
        mUsername = sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, "");
        mPassword = sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, "");
        mWebDavUrl = sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, "") + "/cimoc";
        mSardine = new OkHttpSardine();
        mSardine.setCredentials(mUsername, mPassword);
        mCurrentPath = mWebDavUrl;
        new Thread(() -> {
            try {
                if (!mSardine.exists(mWebDavUrl)) {
                    mSardine.createDirectory(mWebDavUrl);
                }
                List<DavResource> resources = mSardine.list(mWebDavUrl, 0);
                if (!resources.isEmpty()) {
                    mDavResource = resources.get(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }

    WebDavDocumentFile(DocumentFile parent, String path, DavResource resource) {
        super(parent);
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.WEBDAV_SHARED, Context.MODE_PRIVATE);
        mUsername = sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, "");
        mPassword = sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, "");
        mWebDavUrl = sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, "") + "/cimoc";
        mSardine = new OkHttpSardine();
        mSardine.setCredentials(mUsername, mPassword);
        new Thread(() -> {
            try {
                if (!mSardine.exists(mWebDavUrl)) {
                    mSardine.createDirectory(mWebDavUrl);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        mCurrentPath = path;
        mDavResource = resource;
    }

    public WebDavDocumentFile(WebDavDocumentFile parent, String path) {
        super(parent);
        SharedPreferences sharedPreferences = App.getAppContext().getSharedPreferences(Constants.WEBDAV_SHARED, Context.MODE_PRIVATE);
        mUsername = sharedPreferences.getString(Constants.WEBDAV_SHARED_USERNAME, "");
        mPassword = sharedPreferences.getString(Constants.WEBDAV_SHARED_PASSWORD, "");
        mWebDavUrl = sharedPreferences.getString(Constants.WEBDAV_SHARED_URL, "") + "/cimoc";
        mSardine = new OkHttpSardine();
        mSardine.setCredentials(mUsername, mPassword);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        mCurrentPath = parent.getCurrentPath() + "/" + path;
        new Thread(() -> {
            try {
                if (!mSardine.exists(mWebDavUrl)) {
                    mSardine.createDirectory(mWebDavUrl);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (!mSardine.exists(mCurrentPath)) {
                    mSardine.createDirectory(mCurrentPath);
                }
                List<DavResource> resources = mSardine.list(mCurrentPath, 0);
                if (!resources.isEmpty()) {
                    mDavResource = resources.get(0);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

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

    public void UploadFile(File src, String urlPath) {

        try {

            byte[] fileContent = new byte[(int) src.length()];
            try (InputStream inputStream = new java.io.FileInputStream(src)) {
                inputStream.read(fileContent);
            }

            // 上传文件到 WebDAV 服务器
            mSardine.put(urlPath, fileContent);

            // 更新当前资源信息
            List<DavResource> resources = mSardine.list(urlPath, 0);
            if (!resources.isEmpty()) {
                mDavResource = resources.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void UploadStreamFile(InputStream inputStream, String urlPath) {

        try {
            // 上传文件到 WebDAV 服务器
            mSardine.put(urlPath, BinStreamUtils.readAllBytesCompat(inputStream));

            // 更新当前资源信息
            List<DavResource> resources = mSardine.list(urlPath, 0);
            if (!resources.isEmpty()) {
                mDavResource = resources.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InputStream getInputStream(String path) throws FileNotFoundException {
        try {
            return new BufferedInputStream(mSardine.get(path));
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }
}