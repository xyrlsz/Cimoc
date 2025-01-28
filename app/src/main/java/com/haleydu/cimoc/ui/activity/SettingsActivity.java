package com.haleydu.cimoc.ui.activity;

import static com.haleydu.cimoc.Constants.DMZJ_SHARED;
import static com.haleydu.cimoc.Constants.DMZJ_SHARED_COOKIES;
import static com.haleydu.cimoc.Constants.DMZJ_SHARED_USERNAME;
import static com.haleydu.cimoc.Constants.KOMIIC_SHARED;
import static com.haleydu.cimoc.Constants.KOMIIC_SHARED_COOKIES;
import static com.haleydu.cimoc.Constants.KOMIIC_SHARED_EXPIRED;
import static com.haleydu.cimoc.Constants.KOMIIC_SHARED_USERNAME;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.haleydu.cimoc.App;
import com.haleydu.cimoc.R;
import com.haleydu.cimoc.global.Extra;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.presenter.BasePresenter;
import com.haleydu.cimoc.presenter.SettingsPresenter;
import com.haleydu.cimoc.saf.DocumentFile;
import com.haleydu.cimoc.service.DownloadService;
import com.haleydu.cimoc.ui.activity.settings.ReaderConfigActivity;
import com.haleydu.cimoc.ui.fragment.dialog.MessageDialogFragment;
import com.haleydu.cimoc.ui.fragment.dialog.StorageEditorDialogFragment;
import com.haleydu.cimoc.ui.view.SettingsView;
import com.haleydu.cimoc.ui.widget.LoginDialog;
import com.haleydu.cimoc.ui.widget.Option;
import com.haleydu.cimoc.ui.widget.preference.CheckBoxPreference;
import com.haleydu.cimoc.ui.widget.preference.ChoicePreference;
import com.haleydu.cimoc.ui.widget.preference.SliderPreference;
import com.haleydu.cimoc.utils.KomiicUtils;
import com.haleydu.cimoc.utils.ServiceUtils;
import com.haleydu.cimoc.utils.StringUtils;
import com.haleydu.cimoc.utils.ThemeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
    private final int[] mResultArray = new int[6];
    private final Intent mResultIntent = new Intent();
    @BindView(R.id.settings_dmzj_login)
    Option mDmzjLogin;
    @BindView(R.id.settings_komiic_login)
    Option mkomiicLogin;
    @BindViews({R.id.settings_reader_title, R.id.settings_download_title, R.id.settings_other_title, R.id.settings_search_title, R.id.settings_dmzj, R.id.settings_komiic})
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

    @BindView(R.id.settings_st_engine)
    ChoicePreference mStEngine;

    @BindView(R.id.settings_dmzj_logout)
    ImageButton mDmzjLogout;

    @BindView(R.id.settings_komiic_logout)
    ImageButton mKomiicLogout;

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

        String dmzjUsername = getSharedPreferences(DMZJ_SHARED, MODE_PRIVATE).getString(DMZJ_SHARED_USERNAME, "");
        if (!dmzjUsername.isEmpty()) {
            mDmzjLogin.setSummary(dmzjUsername);
            mDmzjLogin.setTitle(getString(R.string.logined));
            mDmzjLogout.setVisibility(View.VISIBLE);
        }

        String komiicUsername = getSharedPreferences(KOMIIC_SHARED, MODE_PRIVATE).getString(KOMIIC_SHARED_USERNAME, "");
        if (!komiicUsername.isEmpty()) {
            mkomiicLogin.setSummary(komiicUsername);
            KomiicUtils.getImageLimit(result -> mKomiicLogout.post(() -> {
                mkomiicLogin.setSummary(komiicUsername + "\n" + getString(R.string.settings_komiic_img_limit_summary) + result);
                CharSequence tmp = mkomiicLogin.getSummary();
                KomiicUtils.getImageLimit("", res -> mKomiicLogout.post(() -> {
                    mkomiicLogin.setSummary(tmp + "\n" + getString(R.string.empty_account_limit) + res);
                }));
            }));
            mkomiicLogin.setTitle(getString(R.string.logined));
            mKomiicLogout.setVisibility(View.VISIBLE);
        } else {
            CharSequence tmp = mkomiicLogin.getSummary();
            KomiicUtils.getImageLimit(result -> mKomiicLogout.post(() -> {
                mkomiicLogin.setSummary(tmp + "\n" + getString(R.string.settings_komiic_img_limit_summary) + result);
            }));
        }

        mStoragePath = getAppInstance().getDocumentFile().getUri().toString();
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
                ThemeUtils.THEME_BLUE, R.array.theme_items, DIALOG_REQUEST_OTHER_THEME);
        mReaderScaleFactor.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_READER_SCALE_FACTOR, 200,
                R.string.settings_reader_scale_factor, DIALOG_REQUEST_READER_SCALE_FACTOR);
        mReaderControllerTrigThreshold.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_READER_CONTROLLER_TRIG_THRESHOLD, 30,
                R.string.settings_reader_controller_trig_threshold, DIALOG_REQUEST_READER_CONTROLLER_TRIG_THRESHOLD);
        mOtherNightAlpha.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_OTHER_NIGHT_ALPHA, 0xB0,
                R.string.settings_other_night_alpha, DIALOG_REQUEST_OTHER_NIGHT_ALPHA);
        mDownloadThread.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_DOWNLOAD_THREAD, 2,
                R.string.settings_download_thread, DIALOG_REQUEST_DOWNLOAD_THREAD);
        mDetailTextSt.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_DETAIL_TEXT_ST,
                PreferenceManager.DETAIL_TEXT_DEFAULT, R.array.detail_text_st, DIALOG_REQUEST_DETAIL_TEXT_ST);
        mStEngine.bindPreference(getSupportFragmentManager(), PreferenceManager.PREF_ST_ENGINE,
                PreferenceManager.ST_JCC, R.array.st_engine_items, DIALOG_REQUEST_ST_ENGINE);
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
                            mPresenter.moveFiles(DocumentFile.fromTreeUri(this, uri));
                        }
                    } else {
                        String path = data.getStringExtra(Extra.EXTRA_PICKER_PATH);
                        if (!StringUtils.isEmpty(path)) {
                            DocumentFile file = DocumentFile.fromFile(new File(path));
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
                mDownloadThread.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_VALUE));
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

    @OnClick(R.id.settings_dmzj_login)
    void onDmzjLoginClick() {
        int theme = mPreference.getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_BLUE);
        LoginDialog loginDialog = new LoginDialog(this, ThemeUtils.getDialogThemeById(theme));
        loginDialog.setOnLoginListener((username, password) -> {
            if (username.isEmpty() || password.isEmpty()) {
                loginDialog.dismiss();
                showSnackbar(getString(R.string.user_login_empty));
                return;
            }
            RequestBody formBody = new FormBody.Builder()
                    .add("nickname", username)
                    .add("password", password)
                    .add("type", "1")
                    .add("to", "https://i.dmzj.com")
                    .build();

            Request request = new Request.Builder()
                    .url("https://i.dmzj.com/doLogin")
                    .addHeader("accept", "application/json, text/javascript, */*; q=0.01")
                    .addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .addHeader("origin", "https://i.dmzj.com")
                    .addHeader("pragma", "no-cache")
                    .addHeader("priority", "u=1, i")
                    .addHeader("referer", "https://i.dmzj.com/login")
                    .addHeader("sec-ch-ua", "\"Microsoft Edge\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .addHeader("sec-ch-ua-mobile", "?0")
                    .addHeader("sec-ch-ua-platform", "\"Windows\"")
                    .addHeader("sec-fetch-dest", "empty")
                    .addHeader("sec-fetch-mode", "cors")
                    .addHeader("sec-fetch-site", "same-origin")
                    .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
                    .addHeader("x-requested-with", "XMLHttpRequest")
                    .post(formBody)
                    .build();

            App.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    showSnackbar(getString(R.string.user_login_failed));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    List<String> cookies = response.headers("Set-Cookie");
                    if (response.isSuccessful() && response.body() != null && !cookies.isEmpty()) {
                        Set<String> set = new HashSet<>();
                        for (String s : cookies) {
                            List<String> tmp = Arrays.asList(s.split("; "));
                            set.addAll(tmp);
                        }
                        String cookieStr = String.join("; ", set);
                        SharedPreferences sharedPreferences = getSharedPreferences(DMZJ_SHARED, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(DMZJ_SHARED_COOKIES, cookieStr);
                        editor.putString(DMZJ_SHARED_USERNAME, username);
                        editor.apply();
                        runOnUiThread(() -> {
                            mDmzjLogin.setSummary(username);
                            mDmzjLogin.setTitle(getString(R.string.logined));
                            mDmzjLogout.setVisibility(View.VISIBLE);
                        });
                        loginDialog.dismiss();
                        showSnackbar(getString(R.string.user_login_sucess));
                    } else {
                        loginDialog.dismiss();
                        showSnackbar(getString(R.string.user_login_failed));
                    }
                }
            });


        });
        loginDialog.setOnRegisterListener(() -> {
            String url = "https://m.idmzj.com/register.html";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
        loginDialog.show();


    }


    @OnClick(R.id.settings_dmzj_logout)
    void onDmzjLogoutClick() {
        SharedPreferences sharedPreferences = getSharedPreferences(DMZJ_SHARED, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(DMZJ_SHARED_COOKIES);
        editor.remove(DMZJ_SHARED_USERNAME);
        mDmzjLogin.setSummary(getString(R.string.no_login));
        mDmzjLogin.setTitle(getString(R.string.login));
        mDmzjLogout.setVisibility(View.GONE);
        editor.apply();
        showSnackbar(getString(R.string.user_login_logout_sucess));
    }

    @OnClick(R.id.settings_komiic_login)
    void onKomiicLoginClick() {
        int theme = mPreference.getInt(PreferenceManager.PREF_OTHER_THEME, ThemeUtils.THEME_BLUE);
        LoginDialog loginDialog = new LoginDialog(this, ThemeUtils.getDialogThemeById(theme));
        loginDialog.setOnLoginListener((username, password) -> {
            if (username.isEmpty() || password.isEmpty()) {
                loginDialog.dismiss();
                showSnackbar(getString(R.string.user_login_empty));
                return;
            }

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            String json = "{\"email\":\"" + username + "\", \"password\":\"" + password + "\"}";
            RequestBody body = RequestBody.create(mediaType, json);
            Request request = new Request.Builder()
                    .url("https://komiic.com/api/login")
                    .addHeader("accept", "application/json, text/javascript, */*; q=0.01")
                    .addHeader("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                    .addHeader("cache-control", "no-cache")
                    .addHeader("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .addHeader("origin", "https://komiic.com")
                    .addHeader("pragma", "no-cache")
                    .addHeader("priority", "u=1, i")
                    .addHeader("referer", "https://komiic.com/login")
                    .addHeader("sec-ch-ua", "\"Microsoft Edge\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .addHeader("sec-ch-ua-mobile", "?0")
                    .addHeader("sec-ch-ua-platform", "\"Windows\"")
                    .addHeader("sec-fetch-dest", "empty")
                    .addHeader("sec-fetch-mode", "cors")
                    .addHeader("sec-fetch-site", "same-origin")
                    .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0")
                    .addHeader("x-requested-with", "XMLHttpRequest")
                    .post(body)
                    .build();

            App.getHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    showSnackbar(getString(R.string.user_login_failed));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    List<String> cookies = response.headers("Set-Cookie");
                    if (response.isSuccessful() && response.body() != null && !cookies.isEmpty()) {
                        Set<String> set = new HashSet<>();
                        for (String s : cookies) {
                            List<String> tmp = Arrays.asList(s.split("; "));
                            set.addAll(tmp);
                        }
                        Long expired = -1L;
                        try {
                            JSONObject data = new JSONObject(response.body().string());
                            String iso8601String = data.getString("expire");
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                            try {
                                Date date = dateFormat.parse(iso8601String);
                                long timestamp = date.getTime() / 1000;
                                expired = timestamp;
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        String cookieStr = String.join("; ", set);
                        SharedPreferences sharedPreferences = getSharedPreferences(KOMIIC_SHARED, MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(KOMIIC_SHARED_COOKIES, cookieStr);
                        editor.putString(KOMIIC_SHARED_USERNAME, username);
                        editor.putLong(KOMIIC_SHARED_EXPIRED, expired);
                        editor.apply();
                        runOnUiThread(() -> {
                            mkomiicLogin.setSummary(username);
                            mkomiicLogin.setTitle(getString(R.string.logined));
                            KomiicUtils.getImageLimit(result -> mKomiicLogout.post(() -> {
                                mkomiicLogin.setSummary(username + "\n" + getString(R.string.settings_komiic_img_limit_summary) + result);
                                CharSequence tmp = mkomiicLogin.getSummary();
                                KomiicUtils.getImageLimit("", res -> mKomiicLogout.post(() -> {
                                    mkomiicLogin.setSummary(tmp + "\n" + getString(R.string.empty_account_limit) + res);
                                }));
                            }));
                            mKomiicLogout.setVisibility(View.VISIBLE);
                        });
                        loginDialog.dismiss();
                        showSnackbar(getString(R.string.user_login_sucess));
                    } else {
                        loginDialog.dismiss();
                        showSnackbar(getString(R.string.user_login_failed));
                    }
                }
            });


        });
        loginDialog.setOnRegisterListener(() -> {
            String url = "https://komiic.com/register";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
        loginDialog.show();


    }

    @OnClick(R.id.settings_komiic_logout)
    void onKomiicLogoutClick() {
        SharedPreferences sharedPreferences = getSharedPreferences(KOMIIC_SHARED, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KOMIIC_SHARED_COOKIES);
        editor.remove(KOMIIC_SHARED_USERNAME);
        editor.remove(KOMIIC_SHARED_EXPIRED);
        editor.apply();
        mkomiicLogin.setSummary(getString(R.string.no_login));
        mkomiicLogin.setTitle(getString(R.string.login));
        mKomiicLogout.setVisibility(View.GONE);
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
