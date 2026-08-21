package com.xyrlsz.xcimocob.saf;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.documentfile.provider.DocumentFile;

import com.xyrlsz.xcimocob.core.DavResourceInfo;
import com.xyrlsz.xcimocob.core.WebDavClient;
import com.xyrlsz.xcimocob.core.WebDavConf;
import com.xyrlsz.xcimocob.utils.BinStreamUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * WebDAV 版 {@link CimocDocumentFile}，基于 dav4jvm。
 * <p>
 * 注意：构造器不做网络请求（主线程安全）；所有网络操作（exists/findFile/
 * listFiles/createFile 等）都是阻塞调用，必须在后台线程执行。
 */
public class WebDavCimocDocumentFile extends CimocDocumentFile {

    private final WebDavClient mClient;
    private final String mCurrentPath;
    private DavResourceInfo mResource;

    /**
     * 根目录构造：对应 WebDAV 服务器上的 /cimoc 集合。不做网络请求。
     */
    public WebDavCimocDocumentFile(CimocDocumentFile parent) {
        super(parent, null);
        mClient = WebDavConf.client;
        mCurrentPath = WebDavConf.url + "/cimoc";
        mResource = null;
    }

    /**
     * 相对路径构造：parent 目录下的 path。不做网络请求（目录由使用时确保）。
     */
    public WebDavCimocDocumentFile(WebDavCimocDocumentFile parent, String path) {
        super(parent, null);
        mClient = WebDavConf.client;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        mCurrentPath = parent.getCurrentPath() + "/" + path;
        mResource = null;
    }

    /**
     * 内部构造：已知完整路径与资源信息。
     */
    private WebDavCimocDocumentFile(WebDavCimocDocumentFile parent, String path, DavResourceInfo resource) {
        super(parent, null);
        mClient = WebDavConf.client;
        mCurrentPath = path;
        mResource = resource;
    }

    private static String getTypeForName(String name) {
        final int lastDot = name.lastIndexOf('.');
        if (lastDot >= 0) {
            final String extension = name.substring(lastDot + 1).toLowerCase(Locale.ROOT);
            final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
        }
        return "application/octet-stream";
    }

    /**
     * 确保当前目录在服务器上存在（不存在则创建），并刷新自身资源信息。
     * 阻塞调用，请在后台线程执行。
     */
    public void ensureDirectory() {
        mClient.createDirectory(mCurrentPath);
        mResource = mClient.getResource(mCurrentPath);
    }

    public static void UploadFile(File src, String urlPath) {
        WebDavClient client = WebDavConf.client;
        if (client == null) {
            return;
        }
        try {
            // 上传文件到 WebDAV 服务器
            client.put(urlPath, src, "application/octet-stream");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void UploadStreamFile(InputStream inputStream, String urlPath) {
        WebDavClient client = WebDavConf.client;
        if (client == null) {
            return;
        }
        try {
            // 上传文件到 WebDAV 服务器
            client.put(urlPath, BinStreamUtils.readAllBytesCompat(inputStream));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static InputStream getInputStream(String path) throws FileNotFoundException {
        WebDavClient client = WebDavConf.client;
        if (client == null) {
            throw new FileNotFoundException("WebDAV 未初始化");
        }
        try {
            return new BufferedInputStream(client.get(path));
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    public String getCurrentPath() {
        return mCurrentPath;
    }

    @Override
    public CimocDocumentFile createFile(String mimeType, String displayName) {
        String newPath = mCurrentPath + "/" + displayName;
        try {
            if (!mClient.exists(newPath)) {
                // 创建一个空文件
                mClient.put(newPath, new byte[0]);
                DavResourceInfo resource = mClient.getResource(newPath);
                if (resource != null) {
                    return new WebDavCimocDocumentFile(this, newPath, resource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public CimocDocumentFile createDirectory(String displayName) {
        String newPath = mCurrentPath + "/" + displayName;
        try {
            if (!mClient.exists(newPath)) {
                mClient.createDirectory(newPath);
                DavResourceInfo resource = mClient.getResource(newPath);
                if (resource != null) {
                    return new WebDavCimocDocumentFile(this, newPath, resource);
                }
            }
        } catch (Exception e) {
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
        return mResource != null ? mResource.getName() : "";
    }

    @Override
    public String getType() {
        if (mResource != null && !mResource.isDirectory()) {
            return getTypeForName(mResource.getName());
        }
        return null;
    }

    @Override
    public boolean isDirectory() {
        return mResource != null && mResource.isDirectory();
    }

    @Override
    public boolean isFile() {
        return mResource != null && !mResource.isDirectory();
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public long lastModified() {
        // 服务器返回的修改时间（epoch 毫秒），未知时为 0
        return mResource != null ? mResource.getLastModified() : 0;
    }

    @Override
    public long length() {
        return mResource != null ? mResource.getContentLength() : 0;
    }

    @Override
    public boolean canRead() {
        return mClient.exists(mCurrentPath);
    }

    @Override
    public boolean canWrite() {
        return mClient.exists(mCurrentPath);
    }

    @Override
    public boolean delete() {
        try {
            mClient.delete(mCurrentPath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean exists() {
        return mClient.exists(mCurrentPath);
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        try {
            return new BufferedInputStream(mClient.get(mCurrentPath));
        } catch (Exception e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override
    public List<CimocDocumentFile> listFiles(DocumentFileFilter filter, Comparator<? super CimocDocumentFile> comp) {
        final ArrayList<CimocDocumentFile> results = new ArrayList<>();
        List<DavResourceInfo> resources = mClient.listChildren(mCurrentPath);
        for (DavResourceInfo resource : resources) {
            String path = mCurrentPath + "/" + resource.getName();
            CimocDocumentFile doc = new WebDavCimocDocumentFile(this, path, resource);
            if (filter == null || filter.call(doc)) {
                results.add(doc);
            }
        }

        if (comp != null) {
            Collections.sort(results, comp);
        }
        return results;
    }

    @Override
    public CimocDocumentFile[] listFiles() {
        List<CimocDocumentFile> files = listFiles(null, null);
        return files.toArray(new CimocDocumentFile[0]);
    }

    @Override
    public void refresh() {
        mResource = mClient.getResource(mCurrentPath);
    }

    @Override
    public CimocDocumentFile findFile(String displayName) {
        String targetPath = mCurrentPath + "/" + displayName;
        try {
            if (mClient.exists(targetPath)) {
                DavResourceInfo resource = mClient.getResource(targetPath);
                if (resource != null) {
                    return new WebDavCimocDocumentFile(this, targetPath, resource);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean renameTo(String displayName) {
        String newPath = mCurrentPath.substring(0, mCurrentPath.lastIndexOf('/')) + "/" + displayName;
        try {
            mClient.move(mCurrentPath, newPath);
            mResource = mClient.getResource(newPath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected CimocDocumentFile wrap(DocumentFile delegate) {
        // WebDAV 不使用官方 delegate，此方法不会被调用
        return null;
    }
}