package com.xyrlsz.xcimoc.saf;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Hiroshi on 2017/3/24.
 */

public abstract class CimocDocumentFile {

    private final CimocDocumentFile mParent;

    CimocDocumentFile(CimocDocumentFile parent) {
        mParent = parent;
    }

    public static CimocDocumentFile fromFile(File file) {
        return new RawCimocDocumentFile(null, file);
    }

    public static CimocDocumentFile fromWebDav() {
        return new WebDavCimocDocumentFile(null);
    }

    public static CimocDocumentFile fromTreeUri(Context context, Uri treeUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
            );
            return new TreeCimocDocumentFile(null, context, documentUri);
        }
        return null;
    }


    public static CimocDocumentFile fromSubTreeUri(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= 21) {
            /*
             * https://stackoverflow.com/questions/27759915/bug-when-listing-files-with-android-storage-access-framework-on-lollipop
             * 如果使用 buildDocumentUriUsingTree 会获取到授权的那个 DocumentFile
             */
            return new TreeCimocDocumentFile(null, context, uri);
        }
        return null;
    }

    public abstract CimocDocumentFile createFile(String displayName);

    public abstract CimocDocumentFile createDirectory(String displayName);

    public abstract Uri getUri();

    public abstract String getName();

    public abstract String getType();

    public CimocDocumentFile getParentFile() {
        return mParent;
    }

    public abstract boolean isDirectory();

    public abstract boolean isFile();

    public abstract long length();

    public abstract boolean canRead();

    public abstract boolean canWrite();

    public abstract boolean delete();

    public abstract boolean exists();

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

    public abstract CimocDocumentFile[] listFiles();

    public abstract void refresh();

    public abstract CimocDocumentFile findFile(String displayName);

    public abstract boolean renameTo(String displayName);

    public interface DocumentFileFilter {
        boolean call(CimocDocumentFile file);
    }

}
