package com.xyrlsz.xcimoc.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Created by Hiroshi on 2016/10/20.
 */

public class PermissionUtils {

    public static boolean hasStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) and above - use granular media permissions
            int imagesResult = checkPermission(activity, Manifest.permission.READ_MEDIA_IMAGES);
            int videoResult = checkPermission(activity, Manifest.permission.READ_MEDIA_VIDEO);
            int audioResult = checkPermission(activity, Manifest.permission.READ_MEDIA_AUDIO);
            return imagesResult == PackageManager.PERMISSION_GRANTED &&
                    videoResult == PackageManager.PERMISSION_GRANTED &&
                    audioResult == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and Android 12 (API 31-32)
            // Check for MANAGE_EXTERNAL_STORAGE permission
            boolean check = Environment.isExternalStorageManager();
            return check;
        } else {
            // Android 10 (API 29) and below
            int readResult = checkPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writeResult = checkPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readResult == PackageManager.PERMISSION_GRANTED &&
                    writeResult == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static boolean hasAllPermissions(Activity activity) {
        boolean hasStoragePermission = hasStoragePermission(activity);
        int readPhoneState = checkPermission(activity, Manifest.permission.READ_PHONE_STATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int postNotificationsResult = checkPermission(activity, Manifest.permission.POST_NOTIFICATIONS);
            return hasStoragePermission &&
                    readPhoneState == PackageManager.PERMISSION_GRANTED &&
                    postNotificationsResult == PackageManager.PERMISSION_GRANTED;
        }
        return hasStoragePermission &&
                readPhoneState == PackageManager.PERMISSION_GRANTED;
    }

    public static int checkPermission(@NonNull Activity activity, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(activity, permission);
    }
}