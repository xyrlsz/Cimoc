package com.xyrlsz.xcimocob.saf;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Hiroshi on 2017/3/24.
 *
 * 本地文件系统实现：内部委托安卓官方 {@link RawDocumentFile}
 * （经 {@link DocumentFile#fromFile} 创建），保留 mFile 用于流操作与遍历。
 */
class RawCimocDocumentFile extends CimocDocumentFile {

    private File mFile;

    RawCimocDocumentFile(CimocDocumentFile parent, File file) {
        super(parent, DocumentFile.fromFile(file));
        mFile = file;
    }

    @Override
    public CimocDocumentFile createFile(String mimeType, String displayName) {
        // 保持旧行为：不根据 mimeType 追加扩展名（displayName 已含扩展名）
        File target = new File(mFile, displayName);
        if (!target.exists()) {
            try {
                if (!target.createNewFile()) {
                    return null;
                }
            } catch (IOException e) {
                return null;
            }
        }
        return new RawCimocDocumentFile(this, target);
    }

    @Override
    public InputStream openInputStream() throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(mFile));
    }

    @Override
    public List<CimocDocumentFile> listFiles(DocumentFileFilter filter, Comparator<? super CimocDocumentFile> comp) {
        final ArrayList<CimocDocumentFile> results = new ArrayList<>();
        final File[] files = mFile.listFiles();
        if (files != null) {
            for (File file : files) {
                CimocDocumentFile doc = new RawCimocDocumentFile(this, file);
                if (filter == null || filter.call(doc)) {
                    results.add(doc);
                }
            }
        }
        if (comp != null) {
            Collections.sort(results, comp);
        }
        return results;
    }

    @Override
    public CimocDocumentFile[] listFiles() {
        final File[] files = mFile.listFiles();
        final CimocDocumentFile[] results = new CimocDocumentFile[files != null ? files.length : 0];
        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                results[i] = new RawCimocDocumentFile(this, files[i]);
            }
        }
        return results;
    }

    @Override
    public void refresh() {
    }

    @Override
    public CimocDocumentFile findFile(String displayName) {
        for (CimocDocumentFile file : listFiles()) {
            if (displayName.equals(file.getName())) {
                return file;
            }
        }
        return null;
    }

    @Override
    public boolean renameTo(String displayName) {
        final File target = new File(mFile.getParentFile(), displayName);
        if (mFile.renameTo(target)) {
            mFile = target;
            setDelegate(DocumentFile.fromFile(mFile));
            return true;
        }
        return false;
    }

    @Override
    protected CimocDocumentFile wrap(DocumentFile delegate) {
        Uri uri = delegate.getUri();
        String path = uri != null && "file".equals(uri.getScheme()) ? uri.getPath() : null;
        return new RawCimocDocumentFile(this, path != null ? new File(path) : mFile);
    }

}
