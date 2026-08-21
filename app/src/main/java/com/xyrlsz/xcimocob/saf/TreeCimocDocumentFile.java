package com.xyrlsz.xcimocob.saf;

import android.annotation.TargetApi;
import android.content.Context;

import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Hiroshi on 2017/3/24.
 *
 * SAF 树目录实现：内部委托安卓官方 {@link TreeDocumentFile}
 * （经 {@link DocumentFile#fromTreeUri} 创建）。官方实现负责 SAF 查询与创建，
 * 本项目保留 mSubFiles 缓存、findFile 快速查找、refresh 与线程安全等增强。
 */
@RequiresApi(21)
@TargetApi(21)
class TreeCimocDocumentFile extends CimocDocumentFile {

    private final Context mContext;
    private Map<String, CimocDocumentFile> mSubFiles;

    TreeCimocDocumentFile(CimocDocumentFile parent, Context context, DocumentFile delegate) {
        super(parent, delegate);
        mContext = context;
    }

    private void list() {
        mSubFiles = new ConcurrentHashMap<>();
        DocumentFile[] children = getDelegate().listFiles();
        for (DocumentFile child : children) {
            String displayName = child.getName();
            if (displayName != null) {
                mSubFiles.put(displayName, new TreeCimocDocumentFile(this, mContext, child));
            }
        }
    }

    @Override
    public CimocDocumentFile createFile(String mimeType, String displayName) {
        if (!checkSubFiles()) {
            return null;
        }

        CimocDocumentFile doc = findFile(displayName);
        if (doc != null) {
            return null;
        }

        try {
            // 委托官方 TreeDocumentFile.createFile：DocumentsContract.createDocument 的
            // mimeType 由官方保证非 null（默认 application/octet-stream），规避 Android 5.x
            // 传 null mimeType 导致的系统端 NPE 崩溃。
            DocumentFile result = getDelegate().createFile(mimeType, displayName);
            if (result != null) {
                doc = new TreeCimocDocumentFile(this, mContext, result);
                mSubFiles.put(displayName, doc);
            }
        } catch (RuntimeException e) {
            // 兜底：Android 5.x 的 createDocument 可能抛出未声明运行时异常（如 NPE），
            // 捕获后返回 null，避免下载线程崩溃。
            e.printStackTrace();
        }

        return doc;
    }

    @Override
    public CimocDocumentFile createDirectory(String displayName) {
        if (!checkSubFiles()) {
            return null;
        }

        CimocDocumentFile doc = findFile(displayName);
        if (doc != null) {
            return null;
        }

        try {
            DocumentFile result = getDelegate().createDirectory(displayName);
            if (result != null) {
                doc = new TreeCimocDocumentFile(this, mContext, result);
                mSubFiles.put(displayName, doc);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

        return doc;
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return mContext.getContentResolver().openInputStream(getUri());
    }

    @Override
    public List<CimocDocumentFile> listFiles(DocumentFileFilter filter, Comparator<? super CimocDocumentFile> comp) {
        if (!checkSubFiles()) {
            return new ArrayList<>();
        }

        Iterator<Map.Entry<String, CimocDocumentFile>> iterator = mSubFiles.entrySet().iterator();
        List<CimocDocumentFile> list = new ArrayList<>(mSubFiles.size());
        while (iterator.hasNext()) {
            CimocDocumentFile file = iterator.next().getValue();
            if (filter == null || filter.call(file)) {
                list.add(file);
            }
        }

        if (comp != null) {
            Collections.sort(list, comp);
        }
        return list;
    }

    @Override
    public CimocDocumentFile[] listFiles() {
        if (!checkSubFiles()) {
            return new CimocDocumentFile[0];
        }

        int size = mSubFiles.size();
        Iterator<Map.Entry<String, CimocDocumentFile>> iterator = mSubFiles.entrySet().iterator();
        CimocDocumentFile[] result = new CimocDocumentFile[size];
        for (int i = 0; i != size; ++i) {
            result[i] = iterator.next().getValue();
        }

        return result;
    }

    @Override
    public void refresh() {
        if (mSubFiles != null) {
            mSubFiles.clear();
            list();
        }
    }

    @Override
    public CimocDocumentFile findFile(String displayName) {
        if (!checkSubFiles()) {
            return null;
        }
        return mSubFiles.get(displayName);
    }

    @Override
    public boolean delete() {
        try {
            if (getDelegate().delete()) {
                CimocDocumentFile parent = getParentFile();
                if (parent instanceof TreeCimocDocumentFile) {
                    ((TreeCimocDocumentFile) parent).mSubFiles.remove(getName());
                }
                return true;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected CimocDocumentFile wrap(DocumentFile delegate) {
        return new TreeCimocDocumentFile(this, mContext, delegate);
    }

    private boolean checkSubFiles() {
        if (!isDirectory()) {
            return false;
        }
        if (mSubFiles == null) {
            list();
        }
        return true;
    }

}
