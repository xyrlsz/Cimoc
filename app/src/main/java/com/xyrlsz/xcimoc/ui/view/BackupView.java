package com.xyrlsz.xcimoc.ui.view;

import com.xyrlsz.xcimoc.component.DialogCaller;

/**
 * Created by Hiroshi on 2016/10/19.
 */

public interface BackupView extends BaseView, DialogCaller {

    void onBackupSaveSuccess(int size);

    void onBackupSaveFail();

    void onBackupRestoreSuccess();

    void onBackupRestoreFail();

    void onComicFileLoadSuccess(String[] file);

    void onTagFileLoadSuccess(String[] file);

    void onSettingsFileLoadSuccess(String[] file);

    void onClearFileLoadSuccess(String[] file);

    void onFileLoadFail();

    void onClearBackupSuccess();

    void onClearBackupFail();

    void onUploadSuccess();

    void onUploadFail();
}
