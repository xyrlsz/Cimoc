package com.xyrlsz.xcimoc.utils;

import android.content.ContentResolver;
import android.net.Uri;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hiroshi on 2016/12/4.
 */

public class DocumentUtils {

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

    public static CimocDocumentFile findFile(CimocDocumentFile parent, String... filenames) {
        if (parent != null) {
            for (String filename : filenames) {
                parent = parent.findFile(filename);
                if (parent == null) {
                    return null;
                }
            }
        }
        return parent;
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
