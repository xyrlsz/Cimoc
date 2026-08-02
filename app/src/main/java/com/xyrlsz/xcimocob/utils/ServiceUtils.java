package com.xyrlsz.xcimocob.utils;

import android.content.Context;
import android.content.Intent;

import com.xyrlsz.xcimocob.service.DownloadService;

/**
 * Created by Hiroshi on 2016/12/4.
 */

public class ServiceUtils {

    /**
     * 判断服务是否在运行。
     * <p>
     * 由于 {@link android.app.ActivityManager#getRunningServices(int)} 自 API 26 起
     * 对第三方应用不再可用（只会返回调用方自己的服务，结果不可靠），
     * 这里改为由服务自身维护静态标志（{@code onCreate}/{@code onDestroy} 中更新）。
     */
    public static boolean isServiceRunning(Context context, Class<?> service) {
        if (service == DownloadService.class) {
            return DownloadService.isRunning();
        }
        return false;
    }

    public static void stopService(Context context, Class<?> service) {
        context.stopService(new Intent(context, DownloadService.class));
    }

}
