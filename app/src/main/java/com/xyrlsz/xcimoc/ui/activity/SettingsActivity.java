package com.xyrlsz.xcimoc.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.global.Extra;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.presenter.BasePresenter;
import com.xyrlsz.xcimoc.presenter.SettingsPresenter;
import com.xyrlsz.xcimoc.saf.CimocDocumentFile;
import com.xyrlsz.xcimoc.service.DownloadService;
import com.xyrlsz.xcimoc.ui.activity.settings.ReaderConfigActivity;
import com.xyrlsz.xcimoc.ui.fragment.dialog.MessageDialogFragment;
import com.xyrlsz.xcimoc.ui.fragment.dialog.StorageEditorDialogFragment;
import com.xyrlsz.xcimoc.ui.view.SettingsView;
import com.xyrlsz.xcimoc.ui.widget.preference.CheckBoxPreference;
import com.xyrlsz.xcimoc.ui.widget.preference.ChoicePreference;
import com.xyrlsz.xcimoc.ui.widget.preference.SliderPreference;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.ServiceUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;
import com.xyrlsz.xcimoc.utils.ThemeUtils;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.OnClick;

/**
 * Created by Hiroshi on 2016/9/21.
 */

public class SettingsActivity extends BackActivity implements SettingsView {

    private static final int DIALOG_REQUEST_OTHER_LAUNCH = 0;
    private static final int DIALOG_REQUEST_READER_MODE = 1;
    private static final int DIALOG_REQUEST_OTHER_THEME = 2;
    private static final int DIALOG_REQUEST_OTHER_STORAGE = 3;
    private static final int DIALOG_REQUEST_DOWNLOAD_THREAD = 4;
    private static final int DIALOG_REQUEST_DOWNLOAD_SCAN = 6;
    private static final int DIALOG_REQUEST_OTHER_NIGHT_ALPHA = 7;
    private static final int DIALOG_REQUEST_READER_SCALE_FACTOR = 8;
    private static final int DIALOG_REQUEST_READER_CONTROLLER_TRIG_THRESHOLD = 9;
    private static final int DIALOG_REQUEST_DETAIL_TEXT_ST = 10;
    private static final int DIALOG_REQUEST_ST_ENGINE = 11;
    private static final int DIALOG_REQUEST_OTHER_DARK_MOD = 12;
    private final int[] mResultArray = new int[6];
    private final Intent mResultIntent = new Intent();

    @BindViews({R.id.settings_reader_title, R.id.settings_download_title, R.id.settings_other_title, R.id.settings_search_title, R.id.settings_comic_login_title})
    List<TextView> mTitleList;
    @BindView(R.id.settings_layout)
    View mSettingsLayout;
    @BindView(R.id.settings_reader_keep_bright)
    CheckBoxPreference mReaderKeepBright;
    @BindView(R.id.settings_reader_hide_info)
    CheckBoxPreference mReaderHideInfo;
    @BindView(R.id.settings_reader_hide_nav)
    CheckBoxPreference mReaderHideNav;
    @BindView(R.id.settings_reader_ban_double_click)
    CheckBoxPreference mReaderBanDoubleClick;
    @BindView(R.id.settings_reader_paging)
    CheckBoxPreference mReaderPaging;
    @BindView(R.id.settings_reader_closeautoresizeimage)
    CheckBoxPreference mReaderCloseAutoResizeImage;
    @BindView(R.id.settings_reader_paging_reverse)
    CheckBoxPreference mReaderPagingReverse;
    @BindView(R.id.settings_reader_white_edge)
    CheckBoxPreference mReaderWhiteEdge;
    @BindView(R.id.settings_reader_white_background)
    CheckBoxPreference mReaderWhiteBackground;
    @BindView(R.id.settings_reader_volume_key)
    CheckBoxPreference mReaderVolumeKeyControls;
    @BindView(R.id.settings_search_auto_complete)
    CheckBoxPreference mSearchAutoComplete;
    @BindView(R.id.settings_other_check_update)
    CheckBoxPreference mCheckCimocUpdate;
    @BindView(R.id.settings_check_update)
    CheckBoxPreference mCheckSoftwareUpdate;
    @BindView(R.id.settings_reader_mode)
    ChoicePreference mReaderMode;
    @BindView(R.id.settings_other_launch)
    ChoicePreference mOtherLaunch;
    @BindView(R.id.settings_other_theme)
    ChoicePreference mOtherTheme;
    @BindView(R.id.settings_reader_scale_factor)
    SliderPreference mReaderScaleFactor;
    @BindView(R.id.settings_reader_controller_trig_threshold)
    SliderPreference mReaderControllerTrigThreshold;
    @BindView(R.id.settings_reader_show_topbar)
    CheckBoxPreference mOtherShowTopbar;
    @BindView(R.id.settings_other_night_alpha)
    SliderPreference mOtherNightAlpha;
    @BindView(R.id.settings_download_thread)
    SliderPreference mDownloadThread;
    @BindView(R.id.settings_other_connect_only_wifi)
    CheckBoxPreference mConnectOnlyWifi;
    @BindView(R.id.settings_other_loadcover_only_wifi)
    CheckBoxPreference mLoadCoverOnlyWifi;
    //    @BindView(R.id.settings_firebase_event)
//    CheckBoxPreference mFireBaseEvent;
//    @BindView(R.id.settings_other_reduce_ad)
//    CheckBoxPreference mReduceAd;
    @BindView(R.id.settings_detail_text_st)
    ChoicePreference mDetailTextSt;

    @BindView(R.id.settings_other_dark_mod)
    ChoicePreference mOtherDarkMod;
    @BindView(R.id.settings_st_engine)
    ChoicePreference mStEngine;

    @BindView(R.id.settings_reader_paging_stream_off)
    CheckBoxPreference mReaderPagingStreamOff;

    private SettingsPresenter mPresenter;
    private String mStoragePath;
    private String mTempStorage;

    @Override
    protected BasePresenter initPresenter() {
        mPresenter = new SettingsPresenter();
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void initView() {
        super.initView();

        String path = App.getPreferenceManager().getString(PreferenceManager.PREF_OTHER_STORAGE, "");
        if (path.isEmpty()) {
            mStoragePath = getAppInstance().getDocumentFile().getUri().toString();
        } else {
            mStoragePath = path;
        }
        mReaderKeepBright.bindPreference(PreferenceManager.PREF_READER_KEEP_BRIGHT, false);
        mReaderHideInfo.bindPreference(PreferenceManager.PREF_READER_HIDE_INFO, false);
        mReaderHideNav.bindPreference(PreferenceManager.PREF_READER_HIDE_NAV, false);
        mReaderBanDoubleClick.bindPreference(PreferenceManager.PREF_READER_BAN_DOUBLE_CLICK, false);
        mReaderPaging.bindPreference(PreferenceManager.PREF_READER_PAGING, false);
        mReaderCloseAutoResizeImage.bindPreference(PreferenceManager.PREF_READER_CLOSEAUTORESIZEIMAGE, false);
        mReaderPagingReverse.bindPreference(PreferenceManager.PREF_READER_PAGING_REVERSE, false);
        mReaderWhiteEdge.bindPreference(PreferenceManager.PREF_READER_WHITE_EDGE, false);
        mReaderWhiteBackground.bindPreference(PreferenceManager.PREF_READER_WHITE_BACKGROUND, false);
        mReaderVolumeKeyControls.bindPreference(PreferenceManager.PREF_READER_VOLUME_KEY_CONTROLS_PAGE_TURNING, false);
        mSearchAutoComplete.bindPreference(PreferenceManager.PREF_SEARCH_AUTO_COMPLETE, false);
        mCheckCimocUpdate.bindPreference(PreferenceManager.PREF_OTHER_CHECK_UPDATE, false);
        mCheckSoftwareUpdate.bindPreference(PreferenceManager.PREF_OTHER_CHECK_SOFTWARE_UPDATE, true);
        mConnectOnlyWifi.bindPreference(PreferenceManager.PREF_OTHER_CONNECT_ONLY_WIFI, false);
        mLoadCoverOnlyWifi.bindPreference(PreferenceManager.PREF_OTHER_LOADCOVER_ONLY_WIFI, false);
//        mFireBaseEvent.bindPreference(PreferenceManager.PREF_OTHER_FIREBASE_EVENT, true);
//        mReduceAd.bindPreference(PreferenceManager.PREF_OTHER_REDUCE_AD, false);
        mOtherShowTopbar.bindPreference(PreferenceManager.PREF_OTHER_SHOW_TOPBAR, false);
        mReaderMode.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_READER_MODE,
                PreferenceManager.READER_MODE_PAGE, R.array.reader_mode_items, DIALOG_REQUEST_READER_MODE);
        mOtherLaunch.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_OTHER_LAUNCH,
                PreferenceManager.HOME_FAVORITE, R.array.launch_items, DIALOG_REQUEST_OTHER_LAUNCH);
        mOtherTheme.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_OTHER_THEME,
                ThemeUtils.THEME_ORANGE, R.array.theme_items, DIALOG_REQUEST_OTHER_THEME);
        mOtherDarkMod.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_OTHER_DARK_MOD, PreferenceManager.DARK_MODE_FALLOW_SYSTEM, R.array.dark_mod_items, DIALOG_REQUEST_OTHER_DARK_MOD);
        mReaderScaleFactor.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_READER_SCALE_FACTOR, 200,
                R.string.settings_reader_scale_factor, DIALOG_REQUEST_READER_SCALE_FACTOR);
        mReaderControllerTrigThreshold.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_READER_CONTROLLER_TRIG_THRESHOLD, 30,
                R.string.settings_reader_controller_trig_threshold, DIALOG_REQUEST_READER_CONTROLLER_TRIG_THRESHOLD);
        mOtherNightAlpha.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_OTHER_NIGHT_ALPHA, 0xB0,
                R.string.settings_other_night_alpha, DIALOG_REQUEST_OTHER_NIGHT_ALPHA);
        mDownloadThread.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_DOWNLOAD_THREAD, 2,
                R.string.settings_download_thread, DIALOG_REQUEST_DOWNLOAD_THREAD);
        if (mDownloadThread.getValue() <= 0) {
            mDownloadThread.setValue(1);
        }

        mDetailTextSt.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_DETAIL_TEXT_ST,
                PreferenceManager.DETAIL_TEXT_DEFAULT, R.array.detail_text_st, DIALOG_REQUEST_DETAIL_TEXT_ST);
        mStEngine.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_ST_ENGINE,
                PreferenceManager.ST_JCC, R.array.st_engine_items, DIALOG_REQUEST_ST_ENGINE);

        mReaderPagingStreamOff.bindPreference(PreferenceManager.PREF_READER_PAGING_STREAM_OFF, false);
    }

    @OnClick(R.id.settings_reader_config)
    void onReaderConfigBtnClick() {
        Intent intent = new Intent(this, ReaderConfigActivity.class);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case DIALOG_REQUEST_OTHER_STORAGE:
                    showProgressDialog();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            // Explicitly check and apply each flag based on the URI's needs
                            int flags = data.getFlags();
                            if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            }
                            if ((flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0) {
                                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            }
                            mTempStorage = uri.toString();
                            mPresenter.moveFiles(CimocDocumentFile.fromTreeUri(this, uri));
                        }
                    } else {
                        String path = data.getStringExtra(Extra.EXTRA_PICKER_PATH);
                        if (!StringUtils.isEmpty(path)) {
                            CimocDocumentFile file = CimocDocumentFile.fromFile(new File(path));
                            mTempStorage = file.getUri().toString();
                            mPresenter.moveFiles(file);
                        } else {
                            onExecuteFail();
                        }
                    }
                    break;
            }
        }
    }


    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_READER_MODE:
                mReaderMode.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_INDEX));
                break;
            case DIALOG_REQUEST_READER_SCALE_FACTOR:
                mReaderScaleFactor.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_VALUE));
                break;
            case DIALOG_REQUEST_READER_CONTROLLER_TRIG_THRESHOLD:
                mReaderControllerTrigThreshold.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_VALUE));
                break;
            case DIALOG_REQUEST_OTHER_LAUNCH:
                mOtherLaunch.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_INDEX));
                break;
            case DIALOG_REQUEST_OTHER_THEME:
                int index = bundle.getInt(EXTRA_DIALOG_RESULT_INDEX);
                if (mOtherTheme.getValue() != index) {
                    mOtherTheme.setValue(index);
                    int theme = ThemeUtils.getThemeById(index);
                    setTheme(theme);
                    int primary = ThemeUtils.getResourceId(this, R.attr.colorPrimary);
                    int accent = ThemeUtils.getResourceId(this, R.attr.colorAccent);
                    changeTheme(primary, accent);
                    mResultArray[0] = 1;
                    mResultArray[1] = theme;
                    mResultArray[2] = primary;
                    mResultArray[3] = accent;
                    mResultIntent.putExtra(Extra.EXTRA_RESULT, mResultArray);
                    setResult(Activity.RESULT_OK, mResultIntent);
                }
                break;
            case DIALOG_REQUEST_OTHER_STORAGE:
                showSnackbar(R.string.settings_other_storage_not_found);
                break;
            case DIALOG_REQUEST_DOWNLOAD_THREAD:
                int num = bundle.getInt(EXTRA_DIALOG_RESULT_VALUE);
                if (num <= 0) {
                    num = 1;
                }
                mDownloadThread.setValue(num);
                break;
            case DIALOG_REQUEST_DOWNLOAD_SCAN:
                showProgressDialog();
                mPresenter.scanTask();
                break;
            case DIALOG_REQUEST_OTHER_NIGHT_ALPHA:
                int alpha = bundle.getInt(EXTRA_DIALOG_RESULT_VALUE);
                mOtherNightAlpha.setValue(alpha);
                if (mNightMask != null) {
                    mNightMask.setBackgroundColor(alpha << 24);
                }
                mResultArray[4] = 1;
                mResultArray[5] = alpha;
                mResultIntent.putExtra(Extra.EXTRA_RESULT, mResultArray);
                setResult(Activity.RESULT_OK, mResultIntent);
                break;
            case DIALOG_REQUEST_DETAIL_TEXT_ST:
                mDetailTextSt.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_INDEX));
                break;
            case DIALOG_REQUEST_ST_ENGINE:
                mStEngine.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_INDEX));
                break;
            case DIALOG_REQUEST_OTHER_DARK_MOD:
                mOtherDarkMod.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_INDEX));
                HintUtils.showToast(getApplicationContext(), "重启生效");
                break;
        }
    }

    private void changeTheme(int primary, int accent) {
        if (mToolbar != null) {
            mToolbar.setBackgroundColor(ContextCompat.getColor(this, primary));
        }
        for (TextView textView : mTitleList) {
            textView.setTextColor(ContextCompat.getColor(this, primary));
        }
        ColorStateList stateList = new ColorStateList(new int[][]{{-android.R.attr.state_checked}, {android.R.attr.state_checked}},
                new int[]{0x8A000000, ContextCompat.getColor(this, accent)});
        mReaderKeepBright.setColorStateList(stateList);
        mReaderHideInfo.setColorStateList(stateList);
        mReaderHideNav.setColorStateList(stateList);
        mReaderBanDoubleClick.setColorStateList(stateList);
        mReaderPaging.setColorStateList(stateList);
        mReaderPagingReverse.setColorStateList(stateList);
        mReaderWhiteEdge.setColorStateList(stateList);
        mReaderWhiteBackground.setColorStateList(stateList);
        mSearchAutoComplete.setColorStateList(stateList);
        mCheckCimocUpdate.setColorStateList(stateList);
        mCheckSoftwareUpdate.setColorStateList(stateList);
        mConnectOnlyWifi.setColorStateList(stateList);
        mLoadCoverOnlyWifi.setColorStateList(stateList);
//        mFireBaseEvent.setColorStateList(stateList);
//        mReduceAd.setColorStateList(stateList);
        mOtherShowTopbar.setColorStateList(stateList);
        mReaderCloseAutoResizeImage.setColorStateList(stateList);
        mReaderVolumeKeyControls.setColorStateList(stateList);
        mReaderPagingStreamOff.setColorStateList(stateList);
    }

    @OnClick(R.id.settings_other_storage)
    void onOtherStorageClick() {
        if (ServiceUtils.isServiceRunning(this, DownloadService.class)) {
            showSnackbar(R.string.download_ask_stop);
        } else {
            StorageEditorDialogFragment fragment = StorageEditorDialogFragment.newInstance(R.string.settings_other_storage,
                    mStoragePath, DIALOG_REQUEST_OTHER_STORAGE);
            fragment.show(getSupportFragmentManager(), null);
        }
    }

    @OnClick(R.id.settings_download_scan)
    void onDownloadScanClick() {
        if (ServiceUtils.isServiceRunning(this, DownloadService.class)) {
            showSnackbar(R.string.download_ask_stop);
        } else {
            MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.dialog_confirm,
                    R.string.settings_download_scan_confirm, true, DIALOG_REQUEST_DOWNLOAD_SCAN);
            fragment.show(getSupportFragmentManager(), null);
        }
    }

    @OnClick(R.id.settings_other_clear_cache)
    void onOtherCacheClick() {
        showProgressDialog();
        mPresenter.clearCache();
        showSnackbar(R.string.common_execute_success);
        hideProgressDialog();
    }

    @OnClick(R.id.settings_comic_source_login)
    void onComicSourceLoginClick() {
        Intent intent = new Intent(this, ComicSourceLoginActivity.class);
        startActivity(intent);
    }

    @Override
    public void onFileMoveSuccess() {
        hideProgressDialog();
        mPreference.putString(PreferenceManager.PREF_OTHER_STORAGE, mTempStorage);
        mStoragePath = mTempStorage;
        ((App) getApplication()).initRootDocumentFile();
        showSnackbar(R.string.common_execute_success);
    }

    @Override
    public void onExecuteSuccess() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_success);
    }

    @Override
    public void onExecuteFail() {
        hideProgressDialog();
        showSnackbar(R.string.common_execute_fail);
    }

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.drawer_settings);
    }

    @Override
    protected View getLayoutView() {
        return mSettingsLayout;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_settings;
    }

}
