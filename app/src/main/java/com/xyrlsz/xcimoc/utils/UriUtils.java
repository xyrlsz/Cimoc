package com.xyrlsz.xcimoc.utils;

import android.net.Uri;
import android.os.Environment;

import java.io.File;

public class UriUtils {
    public static boolean isHttpOrHttps(Uri uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    public static String convertContentToFileUri(Uri contentUri) {
        try {
            String path = contentUri.getPath();
            if (path != null && path.startsWith("/tree/primary:")) {
                String relativePath = path.substring("/tree/primary:".length());
                File storageDir = Environment.getExternalStorageDirectory();
                File targetFile = new File(storageDir, relativePath);

                // 返回完整的 file:// URI
                return "file://" + targetFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
