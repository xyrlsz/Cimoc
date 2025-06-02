package com.xyrlsz.xcimoc.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    public static void writeBytesToFile(File file, byte[] data) {
        FileOutputStream fos = null;
        try {
            // 创建文件的父目录（如果不存在）
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // 创建输出流并写入数据
            fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace(); // 或者使用日志记录错误
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace(); // 关闭异常也应处理
                }
            }
        }
    }
    public static boolean copyFile(File src, File dst) {
        if (src == null || dst == null || !src.exists() || !src.isFile()) {
            return false;
        }

        InputStream in = null;
        OutputStream out = null;

        try {
            // 创建目标目录
            File parent = dst.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            in = new FileInputStream(src);
            out = new FileOutputStream(dst);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static boolean deleteFile(File file){
        return file.delete();
    }
}
