package com.xyrlsz.xcimocob.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * 基于 {@link java.util.Base64} 的 Base64 工具类。
 * <p>
 * JVM 自带的 Basic 解码器遇到换行会抛 {@link IllegalArgumentException}，
 * 本工具在解码前会去除换行/空白（兼容 MIME 多行 Base64，等价于
 * Android {@code android.util.Base64} 的宽松行为），同时保留 JVM 解码器
 * 对"无 padding"输入的容忍。编码输出均不换行（等价于 Android NO_WRAP）。
 */
public final class Base64Utils {

    private Base64Utils() {
    }

    /**
     * 解码前需要去除的空白字符：回车、换行、制表符、空格、换页符
     */
    private static final Pattern WHITESPACE = Pattern.compile("[\\r\\n\\t \\f]");

    // ==================== 标准 Base64 ====================

    /**
     * 解码标准 Base64 字符串（自动去除换行/空白，容忍无 padding）。
     *
     * @param data base64 字符串，可为 null
     * @return 解码后的字节；入参为 null 时返回 null
     */
    public static byte[] decode(String data) {
        if (data == null) return null;
        return Base64.getDecoder().decode(WHITESPACE.matcher(data).replaceAll(""));
    }

    /**
     * 解码标准 Base64 字符串并转为 UTF-8 字符串。
     */
    public static String decodeToString(String data) {
        byte[] bytes = decode(data);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 将字节编码为标准 Base64（无换行）。
     */
    public static String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * 将 UTF-8 字符串编码为标准 Base64（无换行）。
     */
    public static String encodeToString(String data) {
        return encode(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeToString(byte[] data) {
        return encode(data);
    }

    // ==================== URL Safe（base64url，用于 JWT 等） ====================

    /**
     * 解码 base64url 字符串（自动去除换行/空白，容忍无 padding）。
     *
     * @param data base64url 字符串，可为 null
     * @return 解码后的字节；入参为 null 时返回 null
     */
    public static byte[] decodeUrlSafe(String data) {
        if (data == null) return null;
        return Base64.getUrlDecoder().decode(WHITESPACE.matcher(data).replaceAll(""));
    }

    /**
     * 解码 base64url 字符串并转为 UTF-8 字符串。
     */
    public static String decodeUrlSafeToString(String data) {
        byte[] bytes = decodeUrlSafe(data);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 将字节编码为 base64url（无 padding，JWT 风格）。
     */
    public static String encodeUrlSafe(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * 将 UTF-8 字符串编码为 base64url（无 padding，JWT 风格）。
     */
    public static String encodeUrlSafeToString(String data) {
        return encodeUrlSafe(data.getBytes(StandardCharsets.UTF_8));
    }
}
