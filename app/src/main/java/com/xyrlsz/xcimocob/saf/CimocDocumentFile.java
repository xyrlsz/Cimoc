package com.xyrlsz.xcimocob.saf;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Hiroshi on 2017/3/24.
 *
 * 统一文件抽象：SAF（Tree）与文件系统（Raw）内部委托安卓官方
 * {@link androidx.documentfile.provider.DocumentFile}（官方已处理 mimeType、
 * API 版本等兼容问题），WebDAV 使用自实现。保留项目自定义增强：
 * findFile（带缓存）、refresh、openInputStream、带 Comparator 的 listFiles。
 *
 * 注：官方 {@code DocumentFile} 构造器为包私有，无法跨包继承，故采用组合委托。
 */
public abstract class CimocDocumentFile {

    private final CimocDocumentFile mParent;
    private DocumentFile mDelegate;

    CimocDocumentFile(CimocDocumentFile parent, DocumentFile delegate) {
        mParent = parent;
        mDelegate = delegate;
    }

    /** 官方 DocumentFile 委托（WebDAV 实现为 null，全部自行实现）。 */
    protected final DocumentFile getDelegate() {
        return mDelegate;
    }

    /** 重设委托（Raw 重命名后需要同步底层 File）。 */
    protected final void setDelegate(DocumentFile delegate) {
        mDelegate = delegate;
    }

    public static CimocDocumentFile fromFile(File file) {
        return new RawCimocDocumentFile(null, file);
    }

    public static CimocDocumentFile fromWebDav() {
        return new WebDavCimocDocumentFile(null);
    }

    public static CimocDocumentFile fromTreeUri(Context context, Uri treeUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile delegate = DocumentFile.fromTreeUri(context, treeUri);
            if (delegate != null) {
                return new TreeCimocDocumentFile(null, context, delegate);
            }
        }
        return null;
    }

    public static CimocDocumentFile fromSubTreeUri(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= 21) {
            /*
             * https://stackoverflow.com/questions/27759915/bug-when-listing-files-with-android-storage-access-framework-on-lollipop
             * 如果使用 buildDocumentUriUsingTree 会获取到授权的那个 DocumentFile
             */
            DocumentFile delegate = DocumentFile.fromTreeUri(context, uri);
            if (delegate != null) {
                return new TreeCimocDocumentFile(null, context, delegate);
            }
        }
        return null;
    }

    // ===== 官方 DocumentFile API（默认委托官方实现，WebDAV 需自行 override）=====

    /**
     * 创建文件。mimeType 不能为 null：Android 5.x 系统 DocumentsProvider 对
     * null mimeType 会抛 NPE（官方单参 createFile 默认用 application/octet-stream）。
     */
    public CimocDocumentFile createFile(String mimeType, String displayName) {
        DocumentFile result = mDelegate.createFile(mimeType, displayName);
        return result != null ? wrap(result) : null;
    }

    public CimocDocumentFile createFile(String displayName) {
        return createFile("application/octet-stream", displayName);
    }

    public CimocDocumentFile createDirectory(String displayName) {
        DocumentFile result = mDelegate.createDirectory(displayName);
        return result != null ? wrap(result) : null;
    }

    public Uri getUri() {
        return mDelegate.getUri();
    }

    public String getName() {
        return mDelegate.getName();
    }

    public String getType() {
        return mDelegate.getType();
    }

    public CimocDocumentFile getParentFile() {
        return mParent;
    }

    public boolean isDirectory() {
        return mDelegate.isDirectory();
    }

    public boolean isFile() {
        return mDelegate.isFile();
    }

    public boolean isVirtual() {
        return mDelegate.isVirtual();
    }

    public long lastModified() {
        return mDelegate.lastModified();
    }

    public long length() {
        return mDelegate.length();
    }

    public boolean canRead() {
        return mDelegate.canRead();
    }

    public boolean canWrite() {
        return mDelegate.canWrite();
    }

    public boolean delete() {
        return mDelegate.delete();
    }

    public boolean exists() {
        return mDelegate.exists();
    }

    public CimocDocumentFile[] listFiles() {
        DocumentFile[] files = mDelegate.listFiles();
        CimocDocumentFile[] result = new CimocDocumentFile[files.length];
        for (int i = 0; i < files.length; i++) {
            result[i] = wrap(files[i]);
        }
        return result;
    }

    public boolean renameTo(String displayName) {
        return mDelegate.renameTo(displayName);
    }

    // ===== 项目自定义增强（子类实现）=====

    public abstract InputStream openInputStream() throws FileNotFoundException;

    public List<CimocDocumentFile> listFiles(DocumentFileFilter filter) {
        return listFiles(filter, null);
    }

    public CimocDocumentFile[] listFiles(Comparator<? super CimocDocumentFile> comp) {
        CimocDocumentFile[] files = listFiles();
        Arrays.sort(files, comp);
        return files;
    }

    public abstract List<CimocDocumentFile> listFiles(DocumentFileFilter filter, Comparator<? super CimocDocumentFile> comp);

    public abstract void refresh();

    public abstract CimocDocumentFile findFile(String displayName);

    /** 将官方子级 DocumentFile 包装回 CimocDocumentFile。 */
    protected abstract CimocDocumentFile wrap(DocumentFile delegate);

    public interface DocumentFileFilter {
        boolean call(CimocDocumentFile file);
    }

}
