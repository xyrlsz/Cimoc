package com.xyrlsz.xcimoc.utils;

import android.annotation.SuppressLint;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Headers;

/**
 * 拷贝漫画
 * <a href="https://github.com/venera-app/venera-configs/blob/main/copy_manga.js">...</a>
 */

public record CopyMangaHeaderBuilder(String token, String deviceInfo, String device,
                                     String pseudoId, String copyRegion) {

    // 固定参数
    private static final String SECRET_BASE64 = "M2FmMDg1OTAzMTEwMzJlZmUwNjYwNTUwYTA1NjNhNTM=";
    private static final String USER_AGENT = "COPY/3.0.6";
    private static final String SOURCE = "copyApp";
    private static final String PLATFORM = "3";
    private static final String REFERER = "com.copymanga.app-3.0.6";
    private static final String VERSION = "3.0.6";
    private static final String UMSTRING = "b4c89ca4104ea9a97750314d791520ac";
    private static final String REGION = "1";

    public CopyMangaHeaderBuilder(String token, String deviceInfo, String device, String pseudoId) {
        this(token, deviceInfo, device, pseudoId, REGION);
    }

    public CopyMangaHeaderBuilder(String token, String deviceInfo, String device, String pseudoId, String copyRegion) {
        this.token = token != null ? token : "";
        this.deviceInfo = deviceInfo;
        this.device = device;
        this.pseudoId = pseudoId;
        this.copyRegion = copyRegion != null ? copyRegion : REGION;
    }

    public Map<String, String> build() throws Exception {
        Map<String, String> headers = new HashMap<>();

        // 当前时间信息
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.US);
        String dt = sdf.format(now);
        String ts = String.valueOf(now.getTime() / 1000);

        // HMAC-SHA256 签名
        byte[] secretBytes = Base64.getDecoder().decode(SECRET_BASE64);
        String sig = hmacSHA256Hex(secretBytes, ts.getBytes(StandardCharsets.UTF_8));

        // Token 拼接
        String auth = "Token" + (token.isEmpty() ? "" : " " + token);

        headers.put("User-Agent", USER_AGENT);
        headers.put("source", SOURCE);
        headers.put("deviceinfo", deviceInfo);
        headers.put("dt", dt);
        headers.put("platform", PLATFORM);
        headers.put("referer", REFERER);
        headers.put("version", VERSION);
        headers.put("device", device);
        headers.put("pseudoid", pseudoId);
        headers.put("Accept", "application/json");
        headers.put("region", copyRegion);
        headers.put("authorization", auth);
        headers.put("umstring", UMSTRING);
        headers.put("x-auth-timestamp", ts);
        headers.put("x-auth-signature", sig);

        return headers;
    }

    public Headers genHeaders() throws Exception {
        try {
            Map<String, String> headers = build();
            return Headers.of(headers);
        } catch (Exception e) {
            throw new Exception("生成 Headers 失败", e);
        }
    }

    /**
     * HMAC-SHA256 签名并输出十六进制字符串
     */
    private static String hmacSHA256Hex(byte[] key, byte[] message) throws Exception {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] result = mac.doFinal(message);
            return bytesToHex(result);
        } catch (Exception e) {
            throw new Exception("HMAC-SHA256 计算失败", e);
        }
    }

    /**
     * 字节数组转 hex 字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 随机生成 deviceinfo
     */
    public static String generateDeviceInfo() {
        return randomInt(1000000, 9999999) + "V-" + randomInt(1000, 9999);
    }

    /**
     * 随机生成 device
     */
    @SuppressLint("DefaultLocale")
    public static String generateDevice() {
        Random r = new Random();
        char randA1 = (char) ('A' + r.nextInt(26));
        char randA2 = (char) ('A' + r.nextInt(26));
        char randD = (char) ('0' + r.nextInt(10));
        char randA3 = (char) ('A' + r.nextInt(26));

        int part1 = randomInt(0, 999999);
        int part2 = randomInt(0, 999);

        return String.format("%c%c%c%c.%06d.%03d",
                randA1, randA2, randD, randA3, part1, part2);
    }

    /**
     * 随机生成 pseudoid
     */
    public static String generatePseudoId() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static int randomInt(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }
}