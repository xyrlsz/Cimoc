package com.xyrlsz.xcimoc.utils;

import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class UriUtils {
    public static boolean isHttpOrHttps(Uri uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    public static String convertContentToFilePath(Uri contentUri) {
        try {
            if (contentUri == null) {
                return null;
            }
            String scheme = contentUri.getScheme();
            if (scheme != null && scheme.equals("file")) {
                return contentUri.getPath();
            }
            String path = contentUri.getPath();
            if (path != null && path.startsWith("/tree/primary:")) {
                List<String >paths = Arrays.asList(path.split(":"));
                String relativePath;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    relativePath = paths.getLast();
                }else{
                    relativePath = paths.get(paths.size() - 1);
                }
                File storageDir = Environment.getExternalStorageDirectory();
                File targetFile = new File(storageDir, relativePath);

                // 返回完整的文件路径
                return targetFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
