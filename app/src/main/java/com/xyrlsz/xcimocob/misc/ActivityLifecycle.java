package com.xyrlsz.xcimocob.misc;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Hiroshi on 2018/2/13.
 */

public class ActivityLifecycle implements Application.ActivityLifecycleCallbacks {

    private List<Activity> mActivityList;
    private Activity mCurrentActivity;

    public ActivityLifecycle() {
        mActivityList = new LinkedList<>();
    }

    public void clear() {
        List<Activity> copy = new LinkedList<>(mActivityList);
        for (Activity activity : copy) {
            activity.finish();
        }
        mActivityList.clear();
        mCurrentActivity = null;
    }

    /**
     * 返回当前可见（resumed）的 Activity，用于交互式 Cloudflare 验证挂载 WebView。
     */
    public Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        mActivityList.add(activity);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mCurrentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (mCurrentActivity == activity) {
            mCurrentActivity = null;
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        mActivityList.remove(activity);
        if (mCurrentActivity == activity) {
            mCurrentActivity = null;
        }
    }

}
