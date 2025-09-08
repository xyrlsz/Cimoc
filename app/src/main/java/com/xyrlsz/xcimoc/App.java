package com.xyrlsz.xcimoc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.core.Storage;
import com.xyrlsz.xcimoc.core.WebDavConf;
import com.xyrlsz.xcimoc.fresco.ControllerBuilderProvider;
import com.xyrlsz.xcimoc.helper.DBOpenHelper;
import com.xyrlsz.xcimoc.helper.UpdateHelper;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.misc.ActivityLifecycle;
import com.xyrlsz.xcimoc.model.DaoMaster;
import com.xyrlsz.xcimoc.model.DaoSession;
import com.xyrlsz.xcimoc.saf.CimocDocumentFile;
import com.xyrlsz.xcimoc.ui.activity.MainActivity;
import com.xyrlsz.xcimoc.ui.adapter.GridAdapter;
import com.xyrlsz.xcimoc.utils.DocumentUtils;
import com.xyrlsz.xcimoc.utils.FrescoUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;
import com.xyrlsz.xcimoc.utils.ThemeUtils;
import com.xyrlsz.xcimoc.utils.ZaiManhuaSignUtils;

import org.greenrobot.greendao.identityscope.IdentityScopeType;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

/**
 * Created by Hiroshi on 2016/7/5.
 */
public class App extends MultiDexApplication implements AppGetter, Thread.UncaughtExceptionHandler {

    private static final String CRASH_FILE_PATH = "/Cimoc/Log/crash";
    private static final TrustAllCerts trustAllCerts = new TrustAllCerts();
    public static int mWidthPixels;
    public static int mHeightPixels;
    public static int mCoverWidthPixels;
    public static int mCoverHeightPixels;
    public static int mLargePixels;
    private static OkHttpClient mHttpClient;
    private static PreferenceManager mPreferenceManager;
    private static WifiManager manager_wifi;
    private static App mApp;
    // 默认Github源
    private static String UPDATE_CURRENT_URL = Constants.UPDATE_GITHUB_URL;
    private static boolean isNormalExited = false;
    private CimocDocumentFile mCimocDocumentFile;
    private ControllerBuilderProvider mBuilderProvider;
    private RecyclerView.RecycledViewPool mRecycledPool;
    private DaoSession mDaoSession;
    private ActivityLifecycle mActivityLifecycle;

    public static Context getAppContext() {
        return mApp.getApplicationContext();
    }

    public static App getApp() {
        return mApp;
    }

    public static Resources getAppResources() {
        return mApp.getResources();
    }


    public static WifiManager getManager_wifi() {
        return manager_wifi;
    }

    public static PreferenceManager getPreferenceManager() {
        return mPreferenceManager;
    }

    public static String getUpdateCurrentUrl() {
        return UPDATE_CURRENT_URL;
    }

    public static void setUpdateCurrentUrl(String updateCurrentUrl) {
        UPDATE_CURRENT_URL = updateCurrentUrl;
    }

    public static OkHttpClient getHttpClient() {

        //OkHttpClient返回null实现"仅WiFi联网"，后面要注意空指针处理
        if (!manager_wifi.isWifiEnabled() && mPreferenceManager.getBoolean(PreferenceManager.PREF_OTHER_CONNECT_ONLY_WIFI, false)) {
            return null;
        }

        if (mHttpClient == null) {

            // 3.OkHttp访问https的Client实例
            mHttpClient = new OkHttpClient().newBuilder()
                    .sslSocketFactory(createSSLSocketFactory(), trustAllCerts)
                    .hostnameVerifier(new TrustAllHostnameVerifier())
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .build();
        }

        return mHttpClient;
    }

    private static SSLSocketFactory createSSLSocketFactory() {
        SSLSocketFactory ssfFactory = null;

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{trustAllCerts}, new SecureRandom());

            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ssfFactory;
    }

    public static void runOnMainThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public static void goActivity(Class<?> cls) {
        Intent intent = new Intent(mApp.getApplicationContext(), cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mApp.startActivity(intent);
    }

    public static void restartApp() {
        Context context = getAppContext();
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        System.exit(0);
    }

    public static void exitApp() {
        setIsNormalExited(true);
        Context context = getAppContext();
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        System.exit(0);
    }

    public static boolean isNormalExited() {
        return isNormalExited;
    }

    public static void setIsNormalExited(boolean isNormalExited) {
        App.isNormalExited = isNormalExited;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //initXCrash();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mActivityLifecycle = new ActivityLifecycle();
        registerActivityLifecycleCallbacks(mActivityLifecycle);
        mPreferenceManager = new PreferenceManager(this);
        DBOpenHelper helper = new DBOpenHelper(this, "cimoc.db");
        mDaoSession = new DaoMaster(helper.getWritableDatabase()).newSession(IdentityScopeType.None);
        UpdateHelper.update(mPreferenceManager, getDaoSession());
        FrescoUtils.init(this, 512);
        initPixels();
        
        manager_wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //获取栈顶Activity以及当前App上下文
        mApp = this;

        // 检测并且关闭TestMode
        SharedPreferences testShared = getSharedPreferences(Constants.APP_SHARED, MODE_PRIVATE);
        boolean isTestMode = testShared.getBoolean(Constants.APP_SHARED_TEST_MODE, false);
        if (isTestMode) {
            testShared.edit().putBoolean(Constants.APP_SHARED_TEST_MODE, false).apply();
        }

        // 深色模式设置
        int darkMode = mPreferenceManager.getInt(PreferenceManager.PREF_OTHER_DARK_MOD, PreferenceManager.DARK_MODE_FALLOW_SYSTEM);
        switch (darkMode) {
            case PreferenceManager.DARK_MODE_FALLOW_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case PreferenceManager.DARK_MODE_ALWAYS_DARK:
                if (!ThemeUtils.getSysIsDarkMode(getAppContext())) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
                break;
            case PreferenceManager.DARK_MODE_ALWAYS_LIGHT:
                if (ThemeUtils.getSysIsDarkMode(getAppContext())) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                break;
        }

        // 初始化WebDAV配置
        WebDavConf.init(getAppContext());

        // 再漫画检查登录与自动签到
        SharedPreferences zaiSharedPreferences = getAppContext().getSharedPreferences(Constants.ZAI_SHARED, Context.MODE_PRIVATE);
        long timestamp = System.currentTimeMillis() / 1000;
        long exp = zaiSharedPreferences.getLong(Constants.ZAI_SHARED_EXP, 0);
        boolean autoSign = zaiSharedPreferences.getBoolean(Constants.ZAI_SHARED_AUTO_SIGN, false);
        String username = zaiSharedPreferences.getString(Constants.ZAI_SHARED_USERNAME, "");
        String passwordMd5 = zaiSharedPreferences.getString(Constants.ZAI_SHARED_PASSWD_MD5, "");
        if (timestamp > exp) {
            ZaiManhuaSignUtils.Login(this, new ZaiManhuaSignUtils.LoginCallback() {
                @Override
                public void onSuccess() {
                    if (autoSign) {
                        ZaiManhuaSignUtils.CheckSigned(isSigned -> {
                            if (!isSigned) {
                                ZaiManhuaSignUtils.SignIn();
                            }
                        });
                    }
                }

                @Override
                public void onFail() {

                }
            }, username, passwordMd5);
        } else if (autoSign) {
            ZaiManhuaSignUtils.CheckSigned(isSigned -> {
                if (!isSigned) {
                    ZaiManhuaSignUtils.SignIn();
                }
            });
        }

//        this.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
//            @Override
//            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
////                Log.d("ActivityLifecycle:",activity+"onActivityCreated");
//            }
//
//            @Override
//            public void onActivityStarted(Activity activity) {
////                Log.d("ActivityLifecycle:",activity+"onActivityStarted");
////
//            }
//
//            @Override
//            public void onActivityResumed(Activity activity) {
//
//            }
//
//            @Override
//            public void onActivityPaused(Activity activity) {
//
//            }
//
//            @Override
//            public void onActivityStopped(Activity activity) {
//
//            }
//
//            @Override
//            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
//
//            }
//
//            @Override
//            public void onActivityDestroyed(Activity activity) {
//
//            }
//        });
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append("MODEL: ").append(Build.MODEL).append('\n');
        sb.append("SDK: ").append(Build.VERSION.SDK_INT).append('\n');
        sb.append("RELEASE: ").append(Build.VERSION.RELEASE).append('\n');
        sb.append('\n').append(e.getLocalizedMessage()).append('\n');
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append('\n');
            sb.append(element.toString());
        }
        try {
            CimocDocumentFile doc = getDocumentFile();
            CimocDocumentFile dir = DocumentUtils.getOrCreateSubDirectory(doc, "log");
            CimocDocumentFile file = DocumentUtils.getOrCreateFile(dir, StringUtils.getDateStringWithSuffix("log"));
            DocumentUtils.writeStringToFile(getContentResolver(), file, sb.toString());
        } catch (Exception ex) {
        }
        mActivityLifecycle.clear();
        System.exit(1);
    }

    @Override
    public App getAppInstance() {
        return this;
    }

    private void initPixels() {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        mWidthPixels = metrics.widthPixels;
        mHeightPixels = metrics.heightPixels;
        mCoverWidthPixels = mWidthPixels / 3;
        mCoverHeightPixels = mHeightPixels * mCoverWidthPixels / mWidthPixels;
        mLargePixels = 3 * metrics.widthPixels * metrics.heightPixels;
    }

    public void initRootDocumentFile() {
        String uri = mPreferenceManager.getString(PreferenceManager.PREF_OTHER_STORAGE);
        mCimocDocumentFile = Storage.initRoot(this, uri);
    }

    public CimocDocumentFile getDocumentFile() {
        if (mCimocDocumentFile == null) {
            initRootDocumentFile();
        }
        return mCimocDocumentFile;
    }

    public DaoSession getDaoSession() {
        return mDaoSession;
    }

    public RecyclerView.RecycledViewPool getGridRecycledPool() {
        if (mRecycledPool == null) {
            mRecycledPool = new RecyclerView.RecycledViewPool();
            mRecycledPool.setMaxRecycledViews(GridAdapter.TYPE_GRID, 20);
        }
        return mRecycledPool;
    }

    public ControllerBuilderProvider getBuilderProvider() {
        if (mBuilderProvider == null) {
            mBuilderProvider = new ControllerBuilderProvider(getApplicationContext(),
                    SourceManager.getInstance(this).new HeaderGetter(), true);
        }
        return mBuilderProvider;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    // 1.实现X509TrustManager接口
    private static class TrustAllCerts implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
//
//    private void initXCrash(){
//        //异常捕捉框架,xcrash的native捕捉会导致系统死机，将之去掉不使用20200817
//        XCrash.InitParameters initParameters = new XCrash.InitParameters();
//        //不处理native层的崩溃异常
//        initParameters.setLogDir(Environment.getExternalStorageDirectory().getAbsolutePath()+CRASH_FILE_PATH);
//        initParameters.disableNativeCrashHandler();
//        //java崩溃异常文件的最大数量
//        initParameters.setJavaLogCountMax(200);
//        initParameters.setJavaDumpAllThreadsCountMax(25);
//        XCrash.init(this, initParameters);
//    }

    // 2.实现HostnameVerifier接口
    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
}
