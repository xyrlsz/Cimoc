package com.xyrlsz.xcimoc.ui.activity;

import static com.xyrlsz.xcimoc.core.Backup.BACKUP;

import android.os.Bundle;
import android.view.View;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.presenter.BackupPresenter;
import com.xyrlsz.xcimoc.presenter.BasePresenter;
import com.xyrlsz.xcimoc.saf.DocumentFile;
import com.xyrlsz.xcimoc.saf.WebDavDocumentFile;
import com.xyrlsz.xcimoc.ui.fragment.dialog.ChoiceDialogFragment;
import com.xyrlsz.xcimoc.ui.fragment.dialog.MessageDialogFragment;
import com.xyrlsz.xcimoc.ui.view.BackupView;
import com.xyrlsz.xcimoc.ui.widget.WebDavConfDialog;
import com.xyrlsz.xcimoc.ui.widget.preference.CheckBoxPreference;
import com.xyrlsz.xcimoc.utils.DocumentUtils;
import com.xyrlsz.xcimoc.utils.PermissionUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;
import com.xyrlsz.xcimoc.utils.ThemeUtils;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Hiroshi on 2016/10/19.
 */

public class BackupActivity extends BackActivity implements BackupView {

    private static final int DIALOG_REQUEST_RESTORE_COMIC = 0;
    private static final int DIALOG_REQUEST_RESTORE_TAG = 1;
    private static final int DIALOG_REQUEST_RESTORE_SETTINGS = 2;
    private static final int DIALOG_REQUEST_RESTORE_CLEAR = 3;
    public static final int DIALOG_REQUEST_RESTORE_DELETE = 4;
    DocumentFile mDocumentFile;

    @BindView(R.id.backup_layout)
    View mLayoutView;
    @BindView(R.id.backup_save_comic_auto)
    CheckBoxPreference mSaveComicAuto;
    @BindView(R.id.backup_cloud_sync)
    CheckBoxPreference mSaveComicCloudAuto;
    private BackupPresenter mPresenter;

    @Override
    protected BasePresenter initPresenter() {
        mPresenter = new BackupPresenter();
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void initView() {
        super.initView();
        mSaveComicAuto.bindPreference(PreferenceManager.PREF_BACKUP_SAVE_COMIC, true);
        mSaveComicCloudAuto.bindPreference(PreferenceManager.PREF_BACKUP_SAVE_COMIC_CLOUD, true);
    }

    @OnClick(R.id.backup_save_comic)
    void onSaveFavoriteClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            mPresenter.saveComic(getAppInstance().getDocumentFile());
        } else {
            onFileLoadFail();
        }
    }


    @OnClick(R.id.backup_save_tag)
    void onSaveTagClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            mPresenter.saveTag(getAppInstance().getDocumentFile());
        } else {
            onFileLoadFail();
        }
    }

    @OnClick(R.id.backup_save_settings)
    void onSaveSettingsClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            mPresenter.saveSettings(getAppInstance().getDocumentFile());
        } else {
            onFileLoadFail();
        }
    }

    @OnClick(R.id.backup_restore_comic)
    void onRestoreFavoriteClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            mDocumentFile = getAppInstance().getDocumentFile();
            mPresenter.loadComicFile(mDocumentFile);
        } else {
            onFileLoadFail();
        }
    }

    @OnClick(R.id.backup_restore_tag)
    void onRestoreTagClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            mDocumentFile = getAppInstance().getDocumentFile();
            mPresenter.loadTagFile(mDocumentFile);
        } else {
            onFileLoadFail();
        }
    }

    @OnClick(R.id.backup_restore_settings)
    void onRestoreSettingsClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            mDocumentFile = getAppInstance().getDocumentFile();
            mPresenter.loadSettingsFile(mDocumentFile);
        } else {
            onFileLoadFail();
        }
    }

    @OnClick(R.id.backup_clear_record)
    void onClearRecordClick() {
        showProgressDialog();
        if (PermissionUtils.hasStoragePermission(this)) {
            mDocumentFile = getAppInstance().getDocumentFile();
            mPresenter.loadClearBackupFile(mDocumentFile);
        } else {
            onFileLoadFail();
        }
    }

    @OnClick(R.id.backup_cloud_config)
    void onWebDavConfClick() {
        int theme = mPreference.getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_PINK);
        WebDavConfDialog dialog = new WebDavConfDialog(this, ThemeUtils.getDialogThemeById(theme));
        dialog.show();
    }

    @OnClick(R.id.backup_cloud_backup)
    void onWebDavCloudBackupClick() {
        showProgressDialog();
        mDocumentFile = DocumentFile.fromWebDav();
        mPresenter.saveComic(mDocumentFile);
    }


    @OnClick(R.id.backup_cloud_restore)
    void onWebDavCloudRestoreClick() {
        showProgressDialog();
        mDocumentFile = DocumentFile.fromWebDav();
        mPresenter.loadComicFile(mDocumentFile);
    }

    @OnClick(R.id.backup_cloud_clear)
    void onWebDavCloudClearClick() {
        showProgressDialog();
        mDocumentFile = DocumentFile.fromWebDav();
        mPresenter.loadClearBackupFile(mDocumentFile);
    }

    @OnClick(R.id.backup_cloud_upload)
    void onWebDavCloudUploadClick() {
        showProgressDialog();
        mDocumentFile = DocumentFile.fromWebDav();
        DocumentFile localDocumentFiles = DocumentUtils.getOrCreateSubDirectory(getAppInstance().getDocumentFile(), BACKUP);
        mPresenter.uploadBackup2Cloud(localDocumentFiles, new WebDavDocumentFile((WebDavDocumentFile) mDocumentFile,BACKUP));
    }

    @OnClick(R.id.backup_save_settings_cloud)
    void onSaveSettingsCloudClick() {
        showProgressDialog();
        mDocumentFile = DocumentFile.fromWebDav();
        mPresenter.saveSettings(mDocumentFile);
    }

    @OnClick(R.id.backup_restore_settings_cloud)
    void onRestoreSettingsCloudClick() {
        showProgressDialog();
        mDocumentFile = DocumentFile.fromWebDav();
        mPresenter.loadSettingsFile(mDocumentFile);
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_RESTORE_COMIC:
                showProgressDialog();
                mPresenter.restoreComic(bundle.getString(EXTRA_DIALOG_RESULT_VALUE), mDocumentFile);
                break;
            case DIALOG_REQUEST_RESTORE_TAG:
                showProgressDialog();
                mPresenter.restoreTag(bundle.getString(EXTRA_DIALOG_RESULT_VALUE), mDocumentFile);
                break;
            case DIALOG_REQUEST_RESTORE_SETTINGS:
                showProgressDialog();
                mPresenter.restoreSetting(bundle.getString(EXTRA_DIALOG_RESULT_VALUE), mDocumentFile);
                break;
            case DIALOG_REQUEST_RESTORE_CLEAR:
                showProgressDialog();
                mPresenter.clearBackup(mDocumentFile);
                break;

            case DIALOG_REQUEST_RESTORE_DELETE:
                showProgressDialog();
                mPresenter.deleteBackup(bundle.getString(EXTRA_DIALOG_RESULT_VALUE), mDocumentFile);
                break;
        }
    }

    @Override
    public void onComicFileLoadSuccess(String[] file) {
        showChoiceDialog(R.string.backup_restore_comic, file, DIALOG_REQUEST_RESTORE_COMIC);
    }

    @Override
    public void onTagFileLoadSuccess(String[] file) {
        showChoiceDialog(R.string.backup_restore_tag, file, DIALOG_REQUEST_RESTORE_TAG);
    }

    @Override
    public void onSettingsFileLoadSuccess(String[] file) {
        showChoiceDialog(R.string.backup_restore_settings, file, DIALOG_REQUEST_RESTORE_SETTINGS);
    }

    private void showChoiceDialog(int title, String[] item, int request) {
        hideProgressDialog();
        ChoiceDialogFragment fragment = ChoiceDialogFragment.newInstance(title, item, -1, request);
        fragment.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onClearFileLoadSuccess(String[] file) {
        hideProgressDialog();
        MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.backup_clear_record,
                R.string.backup_clear_record_notice_summary, true, DIALOG_REQUEST_RESTORE_CLEAR);
        fragment.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onFileLoadFail() {
        hideProgressDialog();
        showSnackbar(R.string.backup_restore_not_found);
    }

    @Override
    public void onBackupRestoreSuccess() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_success);
    }

    @Override
    public void onClearBackupSuccess() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_clear_success);
    }

    @Override
    public void onClearBackupFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_clear_fail);
    }

    @Override
    public void onUploadSuccess() {
        hideProgressDialog();
        showSnackbar(R.string.common_uploud_success);
    }

    @Override
    public void onUploadFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_uploud_fail);
    }

    @Override
    public void onBackupRestoreFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_fail);
    }

    @Override
    public void onBackupSaveSuccess(int size) {
        hideProgressDialog();
        showSnackbar(StringUtils.format(getString(R.string.backup_save_success), size));
    }

    @Override
    public void onBackupSaveFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_fail);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.drawer_backup);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_backup;
    }

    @Override
    protected View getLayoutView() {
        return mLayoutView;
    }

}
