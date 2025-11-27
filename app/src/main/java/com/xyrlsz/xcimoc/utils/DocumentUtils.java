package com.xyrlsz.xcimoc.utils;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;

import com.xyrlsz.xcimoc.saf.CimocDocumentFile;
import com.xyrlsz.xcimoc.saf.WebDavCimocDocumentFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Hiroshi on 2016/12/4.
 */

public class DocumentUtils {
    private static final ConcurrentHashMap<String, Object> DIR_LOCKS = new ConcurrentHashMap<>();

    public static CimocDocumentFile getOrCreateFile(CimocDocumentFile parent, String displayName) {
        CimocDocumentFile file = parent.findFile(displayName);
        if (file != null) {
            if (file.isFile()) {
                return file;
            }
            return null;
        }
        return parent.createFile(displayName);
    }

    //    public static CimocDocumentFile findFile(CimocDocumentFile parent, String... filenames) {
//        if (parent != null) {
//            for (String filename : filenames) {
//                parent = parent.findFile(filename);
//                if (parent == null) {
//                    return null;
//                }
//            }
//        }
//        return parent;
//    }

    // 线程安全的 findFile
    public static CimocDocumentFile findFile(CimocDocumentFile parent, String... filenames) {
        if (parent == null) return null;

        CimocDocumentFile current = parent;
        for (String name : filenames) {
            current = safeFindChildWithNormalization(current, name);
            if (current == null) return null;
        }
        return current;
    }

    private static CimocDocumentFile safeFindChildWithNormalization(CimocDocumentFile parent, String targetName) {
        String lockKey = getStableKeyForParent(parent);
        Object lock = DIR_LOCKS.computeIfAbsent(lockKey, k -> new Object());

        synchronized (lock) {
            // 强制列出一次，确保 lazy load 已完成（在锁内）
            List<CimocDocumentFile> children = Arrays.asList(parent.listFiles());
            if (children.isEmpty()) {
                // 调试日志：父目录下无子项（或 listFiles 返回空）
                android.util.Log.d("DocumentUtils", "listFiles() empty for parent: " + lockKey);
                return null;
            }

            // 先尝试若干严格/宽松匹配规则
            for (CimocDocumentFile child : children) {
                String childName = child.getName();
                if (childName == null) continue;

                // 1) 精确匹配
                if (childName.equals(targetName)) return child;

                // 2) trim 后匹配
                if (childName.trim().equals(targetName.trim())) return child;

                // 3) 忽略大小写
                if (childName.equalsIgnoreCase(targetName)) return child;

                // 4) Unicode 规范化（NFKC） + trim
                String cNorm = normalizeNFKC(childName);
                String tNorm = normalizeNFKC(targetName);
                if (cNorm.equals(tNorm)) return child;

                // 5) 转换全角->半角后再比较
                if (toHalfWidth(cNorm).equals(toHalfWidth(tNorm))) return child;

                // 6) 去除零宽/不可见字符后比较
                if (removeInvisible(cNorm).equals(removeInvisible(tNorm))) return child;

                // 7) 再做一次宽松比较（ignore case + trimmed + normalized）
                if (toHalfWidth(removeInvisible(cNorm)).equalsIgnoreCase(toHalfWidth(removeInvisible(tNorm))))
                    return child;
            }

            // 若都没命中，打印调试信息（包含 codepoints），便于人工检查隐藏字符差异
            android.util.Log.e("DocumentUtils", "safeFindChild NOT FOUND: target=[" + printable(targetName) + "] in parent=" + lockKey);
            for (CimocDocumentFile child : children) {
                String cn = child.getName();
                android.util.Log.e("DocumentUtils", "  child=[" + printable(cn) + "] codepoints=" + codePointsString(cn));
            }

            return null;
        }
    }

    // 试图从 parent 获得稳定标识（优先 URI 或 document id），若失败则 fallback 到 toString()
    private static String getStableKeyForParent(CimocDocumentFile parent) {
        if (parent == null) return "null-parent";
        try {
            // 常见方法名尝试：getUri, getDocumentUri, getUriString, getUriStr, getDocumentId
            String[] candidates = new String[]{"getUri", "getDocumentUri", "getUriString", "getDocumentId", "getName"};
            for (String m : candidates) {
                try {
                    Method method = parent.getClass().getMethod(m);
                    Object val = method.invoke(parent);
                    if (val != null)
                        return parent.getClass().getSimpleName() + "@" + val;
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Throwable t) {
            // ignore reflection problems
        }
        // 最后退回到 toString；尽管不够稳定，但至少可用
        return parent.getClass().getSimpleName() + "@" + parent;
    }

    // Unicode NFKC 规范化并 trim
    private static String normalizeNFKC(String s) {
        if (s == null) return "";
        try {
            return Normalizer.normalize(s, Normalizer.Form.NFKC).trim();
        } catch (Throwable t) {
            return s.trim();
        }
    }

    // 去掉常见的“不可见”或控制字符（包括零宽空格等）
    private static String removeInvisible(String s) {
        if (s == null) return "";
        // \u200B zero-width space, \uFEFF BOM, \u200E LTR, \u200F RTL, \u2060 word joiner, etc.
        return s.replaceAll("[\\p{C}\\u200B\\uFEFF\\u200E\\u200F\\u2060]", "").trim();
    }

    // 将全角字符转换为半角（常用于数字/字母/符号）
    private static String toHalfWidth(String input) {
        if (input == null) return "";
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // 全角空格
            if (c == 12288) {
                out.append(' ');
                continue;
            }
            // 全角字符范围
            if (c >= 65281 && c <= 65374) {
                out.append((char) (c - 65248));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // 可打印字符串：把不可见字符也替换成显式标记，便于日志观察
    private static String printable(String s) {
        if (s == null) return "null";
        return s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String codePointsString(String s) {
        if (s == null) return "null";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Arrays.toString(s.codePoints().toArray());
        } else {
            // 手动遍历 UTF-16，兼容所有 Android 版本
            StringBuilder sb = new StringBuilder();
            sb.append('[');

            for (int i = 0; i < s.length(); ) {
                int cp = s.codePointAt(i); // 即使在旧 SDK，这个方法也存在（Java 7 就有）
                sb.append(cp);

                i += Character.charCount(cp); // 处理代理对
                if (i < s.length()) sb.append(", ");
            }

            sb.append(']');
            return sb.toString();
        }
    }

    public static int countWithoutSuffix(CimocDocumentFile dir, String suffix) {
        int count = 0;
        if (dir.isDirectory()) {
            for (CimocDocumentFile file : dir.listFiles()) {
                if (file.isFile() && !file.getName().endsWith(suffix)) {
                    ++count;
                }
            }
        }
        return count;
    }

    public static String[] listFilesWithSuffix(CimocDocumentFile dir, String... suffix) {
        List<String> list = new ArrayList<>();
        if (dir.isDirectory()) {
            for (CimocDocumentFile file : dir.listFiles()) {
                if (file.isFile()) {
                    String name = file.getName();
                    for (String str : suffix) {
                        if (name.endsWith(str)) {
                            list.add(name);
                            break;
                        }
                    }
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    public static CimocDocumentFile getOrCreateSubDirectory(CimocDocumentFile parent, String displayName) {
        CimocDocumentFile file = parent.findFile(displayName);
        if (file != null) {
            if (file.isDirectory()) {
                return file;
            }
            return null;
        }
        return parent.createDirectory(displayName);
    }

    public static String readLineFromFile(ContentResolver resolver, CimocDocumentFile file) {
        InputStream input = null;
        BufferedReader reader = null;
        try {
            Uri fileData = file.getUri();
            if (UriUtils.isHttpOrHttps(fileData)) {
                input = WebDavCimocDocumentFile.getInputStream(fileData.toString());
            } else {
                input = resolver.openInputStream(fileData);
            }

            if (input != null) {
                reader = new BufferedReader(new InputStreamReader(input));
                return reader.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeStream(input, reader);
        }
        return null;
    }

    public static char[] readCharFromFile(ContentResolver resolver, CimocDocumentFile file, int count) {
        InputStream input = null;
        BufferedReader reader = null;
        try {
            Uri fileData = file.getUri();
            if (UriUtils.isHttpOrHttps(fileData)) {
                new WebDavCimocDocumentFile(null);
                input = WebDavCimocDocumentFile.getInputStream(fileData.toString());
            } else {
                input = resolver.openInputStream(fileData);
            }

            if (input != null) {
                reader = new BufferedReader(new InputStreamReader(input));
                char[] buffer = new char[count];
                if (reader.read(buffer, 0, count) == count) {
                    return buffer;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeStream(input, reader);
        }
        return null;
    }

    public static void writeBytesToFile(ContentResolver resolver, CimocDocumentFile file, byte[] bytes) throws IOException {
        OutputStream output = null;
        BufferedWriter writer = null;
        File tmp = null;
        try {
            Uri fileData = file.getUri();
            if (UriUtils.isHttpOrHttps(fileData)) {
                tmp = File.createTempFile(System.currentTimeMillis() + "", "tmp");
                fileData = Uri.fromFile(tmp);
            }
            output = resolver.openOutputStream(fileData);
            if (output != null) {
                output.write(bytes);
                output.flush();
                if (tmp != null) {
                    WebDavCimocDocumentFile.UploadFile(tmp, file.getUri().toString());
                }
            } else {
                throw new IOException();
            }
        } finally {
            closeStream(output, writer);
        }
    }

    public static void writeStringToFile(ContentResolver resolver, CimocDocumentFile file, String data) throws IOException {
        OutputStream output = null;
        BufferedWriter writer = null;
        File tmp = null;
        try {
            Uri fileData = file.getUri();
            if (UriUtils.isHttpOrHttps(fileData)) {
                tmp = File.createTempFile(System.currentTimeMillis() + "", "tmp");
                fileData = Uri.fromFile(tmp);
            }
            output = resolver.openOutputStream(fileData);
            if (output != null) {
                writer = new BufferedWriter(new OutputStreamWriter(output));
                writer.write(data);
                writer.flush();
                if (tmp != null) {
                    WebDavCimocDocumentFile.UploadFile(tmp, file.getUri().toString());
                }
            } else {
                throw new IOException();
            }
        } finally {
            closeStream(output, writer);
        }
    }

    public static void writeBinaryToFile(ContentResolver resolver, CimocDocumentFile file, InputStream input) throws IOException {
        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        boolean isHttp = UriUtils.isHttpOrHttps(file.getUri());
        if (isHttp) {
            WebDavCimocDocumentFile.UploadStreamFile(input, file.getUri().toString());
            return;
        }
        try {
            Uri fileData = file.getUri();
            OutputStream output = resolver.openOutputStream(fileData);
            if (output != null) {
                inputStream = new BufferedInputStream(input, 8192);
                outputStream = new BufferedOutputStream(output, 8192);

                int length;
                byte[] buffer = new byte[8192];
                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                output.flush();

            } else {
                closeStream(input);
                throw new FileNotFoundException();
            }
        } finally {
            closeStream(inputStream, outputStream);
        }
    }

    public static void writeBinaryToFile(ContentResolver resolver, CimocDocumentFile src, CimocDocumentFile dst) throws IOException {
        InputStream input = resolver.openInputStream(src.getUri());
        writeBinaryToFile(resolver, dst, input);
    }

    private static void closeStream(Closeable... stream) {
        for (Closeable closeable : stream) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean copyFile(ContentResolver resolver, CimocDocumentFile src, CimocDocumentFile dst) {

        try {
            writeBinaryToFile(resolver, src, dst);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
