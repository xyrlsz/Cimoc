package com.xyrlsz.xcimoc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import com.xyrlsz.xcimoc.saf.DocumentFile;
import com.xyrlsz.xcimoc.ui.activity.MainActivity;
import com.xyrlsz.xcimoc.ui.adapter.GridAdapter;
import com.xyrlsz.xcimoc.utils.DocumentUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;

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
    private static String UPDATE_CURRENT_URL = "https://api.github.com/repos/xyrlsz/Cimoc/releases/latest";
    private DocumentFile mDocumentFile;
    private ControllerBuilderProvider mBuilderProvider;
    private RecyclerView.RecycledViewPool mRecycledPool;
    private DaoSession mDaoSession;
    private ActivityLifecycle mActivityLifecycle;
    private static boolean isNormalExited = false;

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
                    .sslSocketFactory(createSSLSocketFactory())
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
            sc.init(null, new TrustManager[]{new TrustAllCerts()}, new SecureRandom());

            ssfFactory = sc.getSocketFactory();
        } catch (Exception e) {
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
        Fresco.initialize(this);
        initPixels();

        manager_wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //获取栈顶Activity以及当前App上下文
        mApp = this;

        // 深色模式设置
        int darkMode = mPreferenceManager.getInt(PreferenceManager.PREF_OTHER_DARK_MOD, PreferenceManager.DARK_MODE_FALLOW_SYSTEM);
        switch (darkMode) {
            case PreferenceManager.DARK_MODE_FALLOW_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case PreferenceManager.DARK_MODE_ALWAYS_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case PreferenceManager.DARK_MODE_ALWAYS_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }

        // 初始化WebDAV配置
        WebDavConf.init(getAppContext());

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
            DocumentFile doc = getDocumentFile();
            DocumentFile dir = DocumentUtils.getOrCreateSubDirectory(doc, "log");
            DocumentFile file = DocumentUtils.getOrCreateFile(dir, StringUtils.getDateStringWithSuffix("log"));
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
        mDocumentFile = Storage.initRoot(this, uri);
    }

    public DocumentFile getDocumentFile() {
        if (mDocumentFile == null) {
            initRootDocumentFile();
        }
        return mDocumentFile;
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

    public static boolean isNormalExited() {
        return isNormalExited;
    }

    public static void setIsNormalExited(boolean isNormalExited) {
        App.isNormalExited = isNormalExited;
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
