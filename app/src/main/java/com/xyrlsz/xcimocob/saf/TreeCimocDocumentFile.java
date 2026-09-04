package com.xyrlsz.xcimocob.saf;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

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

@RequiresApi(21)
@TargetApi(21)
class TreeCimocDocumentFile extends CimocDocumentFile {
    private static final String DIR_MIME = "vnd.android.document/directory";
    private final Context mContext;
    private Map<String, CimocDocumentFile> mSubFiles;
    private String mCachedName;
    private boolean mHasName;
    private String mCachedMime;
    private boolean mHasMime;

    TreeCimocDocumentFile(CimocDocumentFile parent, Context context, DocumentFile delegate) {
        super(parent, delegate);
        mContext = context;
    }

    private void list() {
        mSubFiles = new ConcurrentHashMap<>();
        Uri dirUri = getDelegate().getUri();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                dirUri, DocumentsContract.getDocumentId(dirUri));
        ContentResolver resolver = mContext.getContentResolver();

        try (Cursor c = resolver.query(childrenUri,
                new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE},
                null, null, null)) {
            if (c != null) {
                while (c.moveToNext()) {
                    String docId = c.getString(0);
                    String displayName = c.getString(1);
                    String mime = c.getString(2);
                    if (docId != null && displayName != null) {
                        Uri childUri = DocumentsContract.buildDocumentUriUsingTree(dirUri, docId);
                        DocumentFile childDoc = DocumentFile.fromSingleUri(mContext, childUri);
                        TreeCimocDocumentFile child = new TreeCimocDocumentFile(this, mContext, childDoc);
                        child.mCachedName = displayName;
                        child.mHasName = true;
                        child.mCachedMime = mime;
                        child.mHasMime = true;
                        mSubFiles.put(displayName, child);
                    }
                }
            }
        } catch (RuntimeException e) {
            DocumentFile[] children = getDelegate().listFiles();
            for (DocumentFile child : children) {
                String displayName = child.getName();
                if (displayName != null) {
                    TreeCimocDocumentFile wrapped = new TreeCimocDocumentFile(this, mContext, child);
                    wrapped.mCachedName = displayName;
                    wrapped.mHasName = true;
                    mSubFiles.put(displayName, wrapped);
                }
            }
        }
    }

    /**
     * 构造带缓存元数据的子节点，避免后续访问再触发查询。
     */
    private TreeCimocDocumentFile cacheChild(DocumentFile delegate, String name, String mime) {
        TreeCimocDocumentFile child = new TreeCimocDocumentFile(this, mContext, delegate);
        child.mCachedName = name;
        child.mHasName = true;
        child.mCachedMime = mime;
        child.mHasMime = true;
        return child;
    }

    @Override
    public String getName() {
        if (mHasName) {
            return mCachedName;
        }
        return super.getName();
    }

    @Override
    public String getType() {
        if (mHasMime) {
            return mCachedMime;
        }
        return super.getType();
    }

    @Override
    public boolean isDirectory() {
        if (mHasMime) {
            return DIR_MIME.equals(mCachedMime);
        }
        return super.isDirectory();
    }

    @Override
    public boolean isFile() {
        if (mHasMime) {
            return mCachedMime != null && !DIR_MIME.equals(mCachedMime);
        }
        return super.isFile();
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
            DocumentFile result = getDelegate().createFile(mimeType, displayName);
            if (result != null) {
                doc = cacheChild(result, displayName, mimeType);
                mSubFiles.put(displayName, doc);
            }
        } catch (RuntimeException e) {
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
                doc = cacheChild(result, displayName, DIR_MIME);
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
