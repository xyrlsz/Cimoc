package com.xyrlsz.xcimocob.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by Hiroshi on 2016/10/20.
 */

public class PermissionUtils {

    /**
     * 检查存储权限（按 API 级别区分）
     * <p>
     * Android 13+ (API 33+): 细粒度媒体权限，只需 READ_MEDIA_IMAGES
     * Android 11-12 (API 30-32): 需要 MANAGE_EXTERNAL_STORAGE
     * Android 10 及以下 (API < 30): 需要 READ/WRITE_EXTERNAL_STORAGE
     */
    public static boolean hasStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+): 使用细粒度媒体权限
            // 漫画 App 只需要读取图片文件
            return checkPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 (API 30-32): 需要 MANAGE_EXTERNAL_STORAGE 全文件访问权限
            // 因为 App 使用 Environment.getExternalStoragePublicDirectory() 访问共享存储
            return Environment.isExternalStorageManager();
        } else {
            // Android 10 及以下 (API < 30): 传统存储权限
            int readResult = checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writeResult = checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readResult == PackageManager.PERMISSION_GRANTED &&
                    writeResult == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 检查 App 所需的所有权限
     */
    public static boolean hasAllPermissions(Activity activity) {
        boolean hasStorage = hasStoragePermission(activity);
        int readPhoneState = checkPermission(activity, Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int postNotifications = checkPermission(activity, Manifest.permission.POST_NOTIFICATIONS);
            return hasStorage &&
                    readPhoneState == PackageManager.PERMISSION_GRANTED &&
                    postNotifications == PackageManager.PERMISSION_GRANTED;
        }
        return hasStorage &&
                readPhoneState == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查是否需要显示权限 rationale
     */
    public static boolean shouldShowPermissionRationale(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_MEDIA_IMAGES);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // MANAGE_EXTERNAL_STORAGE 是 Settings Intent，无法用 shouldShowRequestPermissionRationale
            // 用 isExternalStorageManager() 判断即可
            return false;
        } else {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    public static int checkPermission(@NonNull Activity activity, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(activity, permission);
    }

    public static boolean isPermissionGranted(int result) {
        return result == PackageManager.PERMISSION_GRANTED;
    }
}