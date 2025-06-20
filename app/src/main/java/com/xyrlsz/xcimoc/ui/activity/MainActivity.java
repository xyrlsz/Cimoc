package com.xyrlsz.xcimoc.ui.activity;

import static com.xyrlsz.xcimoc.Constants.GITEE_RELEASE_URL;
import static com.xyrlsz.xcimoc.Constants.GITHUB_RELEASE_URL;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.android.material.navigation.NavigationView;
import com.king.app.updater.constant.Constants;
import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.component.ThemeResponsive;
import com.xyrlsz.xcimoc.core.Update;
import com.xyrlsz.xcimoc.fresco.ControllerBuilderProvider;
import com.xyrlsz.xcimoc.global.Extra;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.presenter.BasePresenter;
import com.xyrlsz.xcimoc.presenter.MainPresenter;
import com.xyrlsz.xcimoc.ui.fragment.BaseFragment;
import com.xyrlsz.xcimoc.ui.fragment.ComicFragment;
import com.xyrlsz.xcimoc.ui.fragment.dialog.MessageDialogFragment;
import com.xyrlsz.xcimoc.ui.fragment.recyclerview.SourceFragment;
import com.xyrlsz.xcimoc.ui.view.MainView;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.PermissionUtils;
import com.xyrlsz.xcimoc.utils.STConvertUtils;

import java.io.IOException;

import butterknife.BindView;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by Hiroshi on 2016/7/1.
 * fixed by Haleydu on 2020/8/8.
 */
public class MainActivity extends BaseActivity implements MainView, NavigationView.OnNavigationItemSelectedListener {

    private static final int DIALOG_REQUEST_NOTICE = 0;
    private static final int DIALOG_REQUEST_PERMISSION = 1;
    //private static final int DIALOG_REQUEST_LOGOUT = 2;

    private static final int REQUEST_ACTIVITY_SETTINGS = 0;

    private static final int FRAGMENT_NUM = 3;
    private final Update update = new Update();
    private final long mExitTime = 0;
    @BindView(R.id.main_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.main_navigation_view)
    NavigationView mNavigationView;
    @BindView(R.id.main_fragment_container)
    FrameLayout mFrameLayout;
    private TextView mLastText;
    private SimpleDraweeView mDraweeView;
    private ControllerBuilderProvider mControllerBuilderProvider;
    private MainPresenter mPresenter;
    private ActionBarDrawerToggle mDrawerToggle;
    private long mLastId = -1;
    private int mLastSource = -1;
    private String mLastCid;
    private int mCheckItem;
    private SparseArray<BaseFragment> mFragmentArray;
    private BaseFragment mCurrentFragment;
    private ComicFragment mComicFragment;
    private boolean night;
    private String versionName, content, mUrl, md5;
    private int versionCode;

    //auth0
//    private Auth0 auth0;
    @Override
    protected BasePresenter initPresenter() {
        mPresenter = new MainPresenter();
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            private long mExitTime = 0;

            @Override
            public void handleOnBackPressed() {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else if (System.currentTimeMillis() - mExitTime > 2000) {
                    HintUtils.showToast(MainActivity.this, R.string.main_double_click);
                    mExitTime = System.currentTimeMillis();
                } else {
                    App.setIsNormalExited(true);
                    finishAffinity();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.exit(0);
                }
            }
        };

        // 注册回调
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    protected void initView() {
        initDrawerToggle();
        initNavigation();
        initFragment();
    }

//    private void login() {
//        HintUtils.showToast(MainActivity.this, R.string.user_login_tips);
//        WebAuthProvider.init(auth0)
//            .withScheme("demo")
//            .withScope("openid profile email")
//            .withAudience(String.format("https://%s/userinfo", getString(R.string.com_auth0_domain)))
//            .start(MainActivity.this, new AuthCallback() {
//                @Override
//                public void onFailure(@NonNull final Dialog dialog) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            dialog.show();
//                        }
//                    });
//                }
//
//                @Override
//                public void onFailure(final AuthenticationException exception) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            Toast.makeText(MainActivity.this, "Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
//                            HintUtils.showToast(MainActivity.this, R.string.user_login_failed);
//                        }
//                    });
//                }
//
//                @Override
//                public void onSuccess(@NonNull final Credentials credentials) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            Toast.makeText(MainActivity.this, "Logged in: " + credentials.getAccessToken(), Toast.LENGTH_LONG).show();
//                            HintUtils.showToast(MainActivity.this, R.string.user_login_sucess);
//                            mPreference.putString(PreferenceManager.PREFERENCES_USER_TOCKEN, credentials.getAccessToken());
//                            getUesrInfo();
//                        }
//                    });
//                }
//            });
//    }
//
//    private void logoutShowDialog(){
//        MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.user_login_logout,
//            R.string.user_login_logout_tips, true, DIALOG_REQUEST_LOGOUT);
//        fragment.show(getSupportFragmentManager(), null);
//    }
//
//    private void logout() {
//        HintUtils.showToast(MainActivity.this, R.string.user_login_logout_sucess);
//        mPreference.putString(PreferenceManager.PREFERENCES_USER_EMAIL, "");
//        mPreference.putString(PreferenceManager.PREFERENCES_USER_TOCKEN, "");
//        mPreference.putString(PreferenceManager.PREFERENCES_USER_NAME, "");
//        mPreference.putString(PreferenceManager.PREFERENCES_USER_ID, "");
//    }
//
//    private void loginout() {
//        if (mPreference.getString(PreferenceManager.PREFERENCES_USER_ID, "") == "") {
//            login();
//        } else {
//            logoutShowDialog();
//        }
//    }

    @Override
    protected void initData() {
        mPresenter.loadLast();

        //检查App更新
        String updateUrl;
        if (mPreference.getBoolean(PreferenceManager.PREF_UPDATE_APP_AUTO, true)) {
            if ((updateUrl = App.getPreferenceManager().getString(PreferenceManager.PREF_UPDATE_CURRENT_URL)) != null) {
                App.setUpdateCurrentUrl(updateUrl);
            }
            checkUpdate();
        }
        mPresenter.getSourceBaseUrl();

//        showAuthorNotice();
        showPermission();
//        getMh50KeyIv();

    }


//    public void getUesrInfo() {
//        String accessTocken = mPreference.getString(PreferenceManager.PREFERENCES_USER_TOCKEN, null);
//        if (accessTocken != null) {
//            AuthenticationAPIClient authentication = new AuthenticationAPIClient(auth0);
//            authentication
//                .userInfo(accessTocken)
//                .start(new BaseCallback<UserProfile, AuthenticationException>() {
//                    @Override
//                    public void onSuccess(UserProfile information) {
//                        //user information received
//                        mPreference.putString(PreferenceManager.PREFERENCES_USER_EMAIL, information.getEmail());
//                        mPreference.putString(PreferenceManager.PREFERENCES_USER_NAME, information.getName());
//                        mPreference.putString(PreferenceManager.PREFERENCES_USER_ID, (String) information.getExtraInfo().get("sub"));
//                    }
//
//                    @Override
//                    public void onFailure(AuthenticationException error) {
//                        //user information request failed
//                        HintUtils.showToast(MainActivity.this, R.string.user_login_failed);
//                    }
//                });
//        } else {
//            HintUtils.showToast(MainActivity.this, R.string.user_login_failed);
//        }
//    }

//    @Override
//    protected void initUser() {
//        //auth0
//        auth0 = new Auth0(this);
//        auth0.setOIDCConformant(true);
//    }

    private void initDrawerToggle() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, 0, 0) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (refreshCurrentFragment()) {
                    getSupportFragmentManager().beginTransaction().show(mCurrentFragment).commit();
                } else {
                    getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, mCurrentFragment).commit();
                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private void goToLastComic() {
        if (mPresenter.checkLocal(mLastId)) {
            Intent intent = TaskActivity.createIntent(MainActivity.this, mLastId);
            startActivity(intent);
        } else if (mLastSource != -1 && mLastCid != null) {
            Intent intent = DetailActivity.createIntent(MainActivity.this, null, mLastSource, mLastCid);
            startActivity(intent);
        } else {
            HintUtils.showToast(MainActivity.this, R.string.common_execute_fail);
        }
    }

    private void initNavigation() {
        night = mPreference.getBoolean(PreferenceManager.PREF_NIGHT, false);
        mNavigationView.getMenu().findItem(R.id.drawer_night).setTitle(night ? R.string.drawer_light : R.string.drawer_night);
        mNavigationView.setNavigationItemSelectedListener(this);
        View header = mNavigationView.getHeaderView(0);
        mLastText = header.findViewById(R.id.drawer_last_title);
        mDraweeView = header.findViewById(R.id.drawer_last_cover);
        mDraweeView.setOnClickListener(v -> {
            goToLastComic();
        });
        mLastText.setOnClickListener(v -> {
            goToLastComic();
        });
        mControllerBuilderProvider = new ControllerBuilderProvider(this,
                SourceManager.getInstance(this).new HeaderGetter(), false);
    }

    private void initFragment() {
        int home = mPreference.getInt(PreferenceManager.PREF_OTHER_LAUNCH, PreferenceManager.HOME_FAVORITE);
        switch (home) {
            default:
            case PreferenceManager.HOME_FAVORITE:
            case PreferenceManager.HOME_HISTORY:
            case PreferenceManager.HOME_DOWNLOAD:
                mCheckItem = R.id.drawer_comic;
                break;
            case PreferenceManager.HOME_SOURCE:
                mCheckItem = R.id.drawer_source;
                break;
//            case PreferenceManager.HOME_TAG:
//                mCheckItem = R.id.drawer_tag;
//                break;
        }
        mNavigationView.setCheckedItem(mCheckItem);
        mFragmentArray = new SparseArray<>(FRAGMENT_NUM);
        refreshCurrentFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, mCurrentFragment).commit();
    }

    private boolean refreshCurrentFragment() {
        mCurrentFragment = mFragmentArray.get(mCheckItem);
        if (mCurrentFragment == null) {
            switch (mCheckItem) {
                case R.id.drawer_comic:
                    mComicFragment = new ComicFragment();
                    mCurrentFragment = mComicFragment;
                    break;
                case R.id.drawer_source:
                    mCurrentFragment = new SourceFragment();
                    break;
//                case R.id.drawer_tag:
//                    mCurrentFragment = new TagFragment();
//                    break;
            }
            mFragmentArray.put(mCheckItem, mCurrentFragment);
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mControllerBuilderProvider.clear();
        ((App) getApplication()).getBuilderProvider().clear();
        ((App) getApplication()).getGridRecycledPool().clear();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
//            mDrawerLayout.closeDrawer(GravityCompat.START);
//        } else if (System.currentTimeMillis() - mExitTime > 2000) {
//            HintUtils.showToast(this, R.string.main_double_click);
//            mExitTime = System.currentTimeMillis();
//        } else {
//            finish();
//        }
//    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId != mCheckItem) {
            switch (itemId) {
                case R.id.drawer_comic:
                    mToolbarTitle.setText(mComicFragment.getCurrTitle());
                case R.id.drawer_source:
//                case R.id.drawer_tag:
                    mCheckItem = itemId;
                    getSupportFragmentManager().beginTransaction().hide(mCurrentFragment).commit();
                    if (itemId == R.id.drawer_source) {
                        mToolbarTitle.setText(item.getTitle().toString());
                    }
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    break;
                case R.id.drawer_comiclist:
                    Intent intentBaidu = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.home_page_comiclist_url)));
                    try {
                        startActivity(intentBaidu);
                    } catch (Exception e) {
                        showSnackbar(R.string.about_resource_fail);
                    }
                    break;
                case R.id.drawer_comicUpdate:
//                    update.startUpdate(versionName, content, mUrl, versionCode, md5);
                    new Thread(() -> {
                        boolean checkGithubOk = false;
                        try {
                            Request request = new Request.Builder().url(GITHUB_RELEASE_URL).build();
                            Response response = App.getHttpClient().newCall(request).execute();
                            if (response.isSuccessful()) {
                                checkGithubOk = true;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String releaseUrl;
                        if (checkGithubOk) {
                            releaseUrl = GITHUB_RELEASE_URL;
                        } else {
                            releaseUrl = GITEE_RELEASE_URL;
                        }
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(releaseUrl));
                        startActivity(intent);
                    }).start();
                    break;
                case R.id.drawer_night:
                    onNightSwitch();
                    mPreference.putBoolean(PreferenceManager.PREF_NIGHT, night);
                    break;
                case R.id.drawer_settings:
                    startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class), REQUEST_ACTIVITY_SETTINGS);
                    break;
                case R.id.drawer_about:
                    startActivity(new Intent(MainActivity.this, AboutActivity.class));
                    break;
                case R.id.drawer_backup:
                    startActivity(new Intent(MainActivity.this, BackupActivity.class));
                    break;
//                case R.id.user_info:
//                    loginout();
//                    break;
            }
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCurrentFragment.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_ACTIVITY_SETTINGS:
                    int[] result = data.getIntArrayExtra(Extra.EXTRA_RESULT);
                    if (result[0] == 1) {
                        changeTheme(result[1], result[2], result[3]);
                    }
                    if (result[4] == 1 && mNightMask != null) {
                        mNightMask.setBackgroundColor(result[5] << 24);
                    }
                    break;
            }
        }
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_NOTICE:
                mPreference.putBoolean(PreferenceManager.PREF_MAIN_NOTICE, true);
                //showPermission();
                break;
//            case DIALOG_REQUEST_PERMISSION:
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
//                com.king.app.updater.util.PermissionUtils.verifyReadAndWritePermissions(this, Constants.RE_CODE_STORAGE_PERMISSION);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
//                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                    startActivity(intent);
//                }
            case DIALOG_REQUEST_PERMISSION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13 (API 33) and above
                    if (!Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(intent);

                    }
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.POST_NOTIFICATIONS
                    }, Constants.RE_CODE_STORAGE_PERMISSION);


                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11 (API 30) and Android 12 (API 31-32)
                    if (!Environment.isExternalStorageManager()) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivity(intent);

                    }
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.READ_PHONE_STATE
                    }, Constants.RE_CODE_STORAGE_PERMISSION);

                } else {
                    // Below Android 11
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_PHONE_STATE
                    }, Constants.RE_CODE_STORAGE_PERMISSION);
                }
                break;

//            case DIALOG_REQUEST_LOGOUT:
//                logout();
//                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ((App) getApplication()).initRootDocumentFile();
                    HintUtils.showToast(this, R.string.main_permission_success);
                } else {
                    HintUtils.showToast(this, R.string.main_permission_fail);
                }
                break;
        }
    }

    @Override
    public void onNightSwitch() {
        night = !night;
        mNavigationView.getMenu().findItem(R.id.drawer_night).setTitle(night ? R.string.drawer_light : R.string.drawer_night);
        if (mNightMask != null) {
            mNightMask.setVisibility(night ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    public void onUpdateReady() {
        HintUtils.showToast(this, R.string.main_ready_update);
        if (mPreference.getBoolean(PreferenceManager.PREF_OTHER_CHECK_SOFTWARE_UPDATE, true)) {
            mNavigationView.getMenu().findItem(R.id.drawer_comicUpdate).setVisible(true);
        }
//        Update.update(this);
    }

    @Override
    public void onUpdateReady(String versionName, String content, String mUrl, int versionCode, String md5) {
        this.versionName = versionName;
        this.content = content;
        this.mUrl = mUrl;
        this.md5 = md5;
        this.versionCode = versionCode;
        if (mPreference.getBoolean(PreferenceManager.PREF_OTHER_CHECK_SOFTWARE_UPDATE, true)) {
            mNavigationView.getMenu().findItem(R.id.drawer_comicUpdate).setVisible(true);
            update.startUpdate(versionName, content, mUrl, versionCode, md5);
        } else {
            HintUtils.showToast(this, R.string.main_ready_update);
        }
    }

    @Override
    public void onLastLoadSuccess(long id, int source, String cid, String title, String cover) {
        onLastChange(id, source, cid, title, cover);
    }

    @Override
    public void onLastLoadFail() {
        HintUtils.showToast(this, R.string.main_last_read_fail);
    }

    @Override
    public void onLastChange(long id, int source, String cid, String title, String cover) {
        mLastId = id;
        mLastSource = source;
        mLastCid = cid;
        mLastText.setText(STConvertUtils.convert(title));
        ImageRequest request = ImageRequestBuilder
                .newBuilderWithSource(Uri.parse(cover))
                .setResizeOptions(new ResizeOptions(App.mWidthPixels, App.mHeightPixels))
                .build();
        DraweeController controller = mControllerBuilderProvider.get(source)
                .setOldController(mDraweeView.getController())
                .setImageRequest(request)
                .build();
        mDraweeView.setController(controller);
    }

    private void changeTheme(@StyleRes int theme, @ColorRes int primary, @ColorRes int accent) {
        setTheme(theme);
        ColorStateList itemList = new ColorStateList(new int[][]{{-android.R.attr.state_checked},
                {android.R.attr.state_checked}},
                new int[]{Color.BLACK, ContextCompat.getColor(this, accent)});
        mNavigationView.setItemTextColor(itemList);
        ColorStateList iconList = new ColorStateList(new int[][]{{-android.R.attr.state_checked},
                {android.R.attr.state_checked}},
                new int[]{0x8A000000, ContextCompat.getColor(this, accent)});
        mNavigationView.setItemIconTintList(iconList);
        mNavigationView.getHeaderView(0).setBackgroundColor(ContextCompat.getColor(this, primary));
        if (mToolbar != null) {
            mToolbar.setBackgroundColor(ContextCompat.getColor(this, primary));
        }

        for (int i = 0; i < mFragmentArray.size(); ++i) {
            ((ThemeResponsive) mFragmentArray.valueAt(i)).onThemeChange(primary, accent);
        }
    }

//    private void showAuthorNotice() {
//        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
//        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
//                .setMinimumFetchIntervalInSeconds(3600)
//                .build();
//        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
//        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config);
//        mFirebaseRemoteConfig.fetchAndActivate()
//                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Boolean> task) {
//                        if (task.isSuccessful()) {
//                            boolean updated = task.getResult();
//                            Log.d("FireBase_FirstOpenMsg", "Config params updated: " + updated);
//                        } else {
//                            Log.d("FireBase_FirstOpenMsg", "Config params updated Failed. ");
//                        }
//
//                        String showMsg = mFirebaseRemoteConfig.getString("first_open_msg");
//                        if (!mPreference.getBoolean(PreferenceManager.PREF_MAIN_NOTICE, false)
//                                || showMsg.compareTo(mPreference.getString(PreferenceManager.PREF_MAIN_NOTICE_LAST, "")) != 0) {
//                            mPreference.putString(PreferenceManager.PREF_MAIN_NOTICE_LAST, showMsg);
//                            MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.main_notice,
//                                    showMsg, false, DIALOG_REQUEST_NOTICE);
//                            fragment.show(getSupportFragmentManager(), null);
//                        }
//                    }
//                });
//    }

//    private void getMh50KeyIv() {
//        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
//        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
//                .setMinimumFetchIntervalInSeconds(60*60)
//                .build();
//        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
//        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config);
//        mFirebaseRemoteConfig.fetchAndActivate()
//                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Boolean> task) {
//                        if (task.isSuccessful()) {
//                            boolean updated = task.getResult();
//                            Log.d("FireBase_FirstOpenMsg", "Config params updated: " + updated);
//                        } else {
//                            Log.d("FireBase_FirstOpenMsg", "Config params updated Failed. ");
//                        }
//
//                        String mh50_key = mFirebaseRemoteConfig.getString("mh50_key_msg");
//                        String mh50_iv = mFirebaseRemoteConfig.getString("mh50_iv_msg");
//
//                        if (!mh50_key.equals(mPreference.getString(PreferenceManager.PREFERENCES_MH50_KEY_MSG, "KA58ZAQ321oobbG8"))){
//                            mPreference.putString(PreferenceManager.PREFERENCES_MH50_KEY_MSG, mh50_key);
//                            Toast.makeText(MainActivity.this,"漫画堆key已更新",Toast.LENGTH_LONG).show();
//                        }
//                        if (!mh50_iv.equals(mPreference.getString(PreferenceManager.PREFERENCES_MH50_IV_MSG, "A1B2C3DEF1G321o8"))){
//                            mPreference.putString(PreferenceManager.PREFERENCES_MH50_IV_MSG, mh50_iv);
//                            Toast.makeText(MainActivity.this,"漫画堆iv已更新",Toast.LENGTH_LONG).show();
//                        }
//                    }
//                });
//    }

    private void showPermission() {
        SharedPreferences sharedPreferences = getSharedPreferences("showPermission", MODE_PRIVATE);
        boolean isFirstRun = sharedPreferences.getBoolean("isFirstRun", true);
        if (isFirstRun) {
            if (!PermissionUtils.hasAllPermissions(this)) {
                MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.main_permission,
                        R.string.main_permission_content, false, DIALOG_REQUEST_PERMISSION);
                fragment.show(getSupportFragmentManager(), null);
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isFirstRun", false);
            editor.apply();
        }
    }

    private void checkUpdate() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
//            mPresenter.checkGiteeUpdate(info.versionCode);
            mPresenter.checkUpdate(info.versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getDefaultTitle() {
        int home = mPreference.getInt(PreferenceManager.PREF_OTHER_LAUNCH, PreferenceManager.HOME_FAVORITE);
        switch (home) {
            default:
            case PreferenceManager.HOME_FAVORITE:
                return getString(R.string.comic_tab_favorite);
            case PreferenceManager.HOME_HISTORY:
                return getString(R.string.comic_tab_history);
            case PreferenceManager.HOME_DOWNLOAD:
                return getString(R.string.comic_tab_download);
            case PreferenceManager.HOME_LOCAL:
//                return getString(R.string.drawer_comic);
                return getString(R.string.comic_tab_local);
            case PreferenceManager.HOME_SOURCE:
                return getString(R.string.drawer_source);
//            case PreferenceManager.HOME_TAG:
//                return getString(R.string.drawer_tag);
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_main;
    }

    @Override
    protected View getLayoutView() {
        return mDrawerLayout;
    }
}
