package com.xyrlsz.xcimocob.source;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.github.gzuliyujiang.oaid.DeviceIdentifier;
import com.google.common.collect.Lists;
import com.xyrlsz.xcimocob.App;
import com.xyrlsz.xcimocob.core.Manga;
import com.xyrlsz.xcimocob.model.Chapter;
import com.xyrlsz.xcimocob.model.Comic;
import com.xyrlsz.xcimocob.model.ImageUrl;
import com.xyrlsz.xcimocob.model.Source;
import com.xyrlsz.xcimocob.parser.JsonIterator;
import com.xyrlsz.xcimocob.parser.MangaParser;
import com.xyrlsz.xcimocob.parser.SearchIterator;
import com.xyrlsz.xcimocob.utils.Base64Utils;
import com.xyrlsz.xcimocob.utils.IdCreator;
import com.xyrlsz.xcimocob.utils.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import kotlin.text.Charsets;
import kotlin.text.StringsKt;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class DuManWuApp extends MangaParser {
    public static final int TYPE = 117;
    public static final String DEFAULT_TITLE = "读漫屋app";
    private static final String MH_BASE_URL = "https://d9zfb53b.lstool.xyz";
    private static String cachedAndroidId = "";
    private static final Long openAddTime = System.currentTimeMillis();
    private static final String ref = "8";
    private static final String getVersionCode = ref;
    private static final String version = "4.4.01";
    private static final String getVersionName = version;
    private static final int lnum = 0;

    // ---------- 静态内部类：配置 ----------
    public static final class Config {
        public static final String APP_ID = "3890425215";
        public static final String APP_KEY = "af369e33def572052e546fe5af485819d";
        private static boolean is_debug;
        public static final Config INSTANCE = new Config();
        private static String MH_MAIN_URL = "https://d9b2fy3m5v.oktip.icu/";
        private static String MH_BASE_URL = "https://d9zfb53b.lstool.xyz/";
        private static String PATH = "";
        private static String Appacid = "";
        private static final List<String> backupUrls = CollectionsKt.listOf("http://d9b2fy3m5v.oktip.icu/", "https://d9b2fy3m5v.oktip.icu:40481/", "http://d9b2fy3m5v.oktip.icu:40480/", "https://d9zfb53b.lstool.xyz/", "http://d9zfb53b.lstool.xyz:40480/");
        private static final String RSA_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCST4AVp54OMEEGVJWBfl3bJdRSoChT2T9T5PsZgJTNwVf6on9Xnp9H8tSrW5D0DObXZziKy8uOdzbo1GEy23NgEuH0fxgdHsFxaw7IX4xaqGZ9HYJNMQtxyM/PCxXZvtdS9rwVh4ZJ4cF6YXbLZq+qJaNdShlaUr04b2X26ZV9nwIDAQAB";
        private static final String AES_KEY = "4STAjlKtgaXwS9io";
        private static String tctitle = "看视频解锁任意读";
        private static String tcnotify1 = "    您需要完整观看一个视频解锁任意读，解锁后即可阅读平台所有漫画。热门国漫，日漫，耿美，韩美...免费畅看全网资源🎵🎵";
        private static String tcnotify2 = "广告是为了永久免费，提供更优质的服务，感谢您的理解与支持（●^﹏^●）";
        private static String tcnotify3 = "若完整观看视频后任然提示解锁问题，请重启应用";
        private static String tcnotify4 = "额外全天奖励完成进度";
        private static int tcmaxnum = 3;
        private static int intersititialmaxnum = 5;
        private static int splashmaxnum = 5;
        private static long openAddTime = System.currentTimeMillis();
        private static String Oaid = "";

        private Config() {
        }

        public static String getAppacid() {
            return Appacid;
        }

        public static void setAppacid(String appacid) {
            Appacid = appacid;
        }

        public final String getMH_MAIN_URL() {
            return MH_MAIN_URL;
        }

        public final void setMH_MAIN_URL(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            MH_MAIN_URL = str;
        }

        public final String getMH_BASE_URL() {
            return MH_BASE_URL;
        }

        public final void setMH_BASE_URL(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            MH_BASE_URL = str;
        }

        public final String getPATH() {
            return PATH;
        }

        public final void setPATH(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            PATH = str;
        }

        public final boolean is_debug() {
            return is_debug;
        }

        public final void set_debug(boolean z) {
            is_debug = z;
        }

        public final List<String> getBackupUrls() {
            return backupUrls;
        }

        public final String getRSA_KEY() {
            return RSA_KEY;
        }

        public final String getAES_KEY() {
            return AES_KEY;
        }

        public final String getTctitle() {
            return tctitle;
        }

        public final void setTctitle(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            tctitle = str;
        }

        public final String getTcnotify1() {
            return tcnotify1;
        }

        public final void setTcnotify1(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            tcnotify1 = str;
        }

        public final String getTcnotify2() {
            return tcnotify2;
        }

        public final void setTcnotify2(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            tcnotify2 = str;
        }

        public final String getTcnotify3() {
            return tcnotify3;
        }

        public final void setTcnotify3(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            tcnotify3 = str;
        }

        public final String getTcnotify4() {
            return tcnotify4;
        }

        public final void setTcnotify4(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            tcnotify4 = str;
        }

        public final int getTcmaxnum() {
            return tcmaxnum;
        }

        public final void setTcmaxnum(int i) {
            tcmaxnum = i;
        }

        public final int getIntersititialmaxnum() {
            return intersititialmaxnum;
        }

        public final void setIntersititialmaxnum(int i) {
            intersititialmaxnum = i;
        }

        public final int getSplashmaxnum() {
            return splashmaxnum;
        }

        public final void setSplashmaxnum(int i) {
            splashmaxnum = i;
        }

        public final long getOpenAddTime() {
            return openAddTime;
        }

        public final void setOpenAddTime(long j) {
            openAddTime = j;
        }

        public static String getOaid() {
            Oaid = DeviceIdentifier.getOAID(App.getAppContext());
            return Oaid;
        }

        public void setOaid(String oaid) {
            Oaid = oaid;
        }
    }

    // ---------- 静态内部类：安全工具（签名 + 加密） ----------
    public static final class SecurityUtils {
        public static final SecurityUtils INSTANCE = new SecurityUtils();
        private static final String MY_SECRET_KEY = "YourUniqueSecret888";

        private SecurityUtils() {
        }

        public final String getRawSignature() {
            // 硬编码的 APK 签名 SHA-256 指纹（实际应从 PackageManager 动态获取）
            return "90EC4A37C3E00261891698C4520BBB4137042BD1149E2C3AB0954CFC9F0FA697";
        }

        public String generateToken(String signature, long j) throws NoSuchAlgorithmException {
            String input = signature + MY_SECRET_KEY + j;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
    }

    // ---------- 静态内部类：加密工具 ----------
    public static final class EncryptUtils {
        public static final EncryptUtils INSTANCE = new EncryptUtils();

        private EncryptUtils() {
        }

        private final PublicKey getPublicKey(String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
            PublicKey generatePublic = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64Utils.decode(str)));
            Intrinsics.checkNotNullExpressionValue(generatePublic, "generatePublic(...)");
            return generatePublic;
        }

        public final String RsaEncrypt(String str) {
            Intrinsics.checkNotNullParameter(str, "data");
            try {
                PublicKey publicKey = getPublicKey(Config.INSTANCE.getRSA_KEY());
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                byte[] bytes = str.getBytes(Charsets.UTF_8);
                Intrinsics.checkNotNullExpressionValue(bytes, "getBytes(...)");
                String encodeToString = Base64Utils.encodeToString(cipher.doFinal(bytes));
                Intrinsics.checkNotNull(encodeToString);
                return encodeToString;
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        public static String hmacSha256(String data, String key) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(result);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        private static String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
    }

    // ---------- 静态内部类：AES 加解密（与原始 Kotlin 逻辑一致） ----------
    public static final class AesHelper {
        public static String aes128Decrypt(String aesKey, String encryptedData) {
            Intrinsics.checkNotNullParameter(aesKey, "aesKey");
            Intrinsics.checkNotNullParameter(encryptedData, "encryptedData");
            try {
                byte[] bytes = StringsKt.padEnd(aesKey, 16, (char) 0).getBytes(Charsets.UTF_8);
                Intrinsics.checkNotNullExpressionValue(bytes, "getBytes(...)");
                SecretKeySpec secretKeySpec = new SecretKeySpec(bytes, "AES");
                byte[] decode = Base64.getDecoder().decode(encryptedData);
                Intrinsics.checkNotNull(decode);
                if (decode.length < 16) return null;
                byte[] ivBytes = ArraysKt.sliceArray(decode, RangesKt.until(0, 16));
                byte[] cipherBytes = ArraysKt.sliceArray(decode, RangesKt.until(16, decode.length));
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivBytes));
                byte[] doFinal = cipher.doFinal(cipherBytes);
                Intrinsics.checkNotNull(doFinal);
                return new String(doFinal, Charsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static String aes128Encrypt(String plainText) {
            Intrinsics.checkNotNullParameter(plainText, "plainText");
            return TextUtils.isEmpty(plainText) ? "" : aes128Encrypt(Config.INSTANCE.getAES_KEY(), Config.INSTANCE.getAES_KEY(), plainText);
        }

        public static String aes128Encrypt(String aesKey, String iv, String plainText) {
            try {
                byte[] keyBytes = StringsKt.padEnd(aesKey, 16, (char) 0).getBytes(Charsets.UTF_8);
                SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
                byte[] ivBytes = StringsKt.padEnd(iv, 16, (char) 0).getBytes(Charsets.UTF_8);
                IvParameterSpec ivParameterSpec = new IvParameterSpec(ivBytes);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
                byte[] plainBytes = plainText.getBytes(Charsets.UTF_8);
                byte[] cipherBytes = cipher.doFinal(plainBytes);
                // 拼接 IV + 密文
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(ivBytes);
                baos.write(cipherBytes);
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
    }

    // ---------- 辅助方法：生成随机字符串 ----------
    public static final class RandomStringUtils {
        private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        private static final Random RANDOM = new Random();

        public static String getRandomString(int length) {
            if (length <= 0) return "";
            try {
                StringBuilder sb = new StringBuilder(length);
                for (int i = 0; i < length; i++) {
                    int index = RANDOM.nextInt(CHARACTERS.length());
                    sb.append(CHARACTERS.charAt(index));
                }
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
                // 返回固定备用字符串
                return "abcdefghijklmnop".substring(0, length);
            }
        }
        public static String getRandomString() {
            return getRandomString(16);
        }
    }

    // ---------- 固定盐值 ----------
    static final class ColorKt {
        private static final String quanse = "ok37hy";

        public static String getQuanse() {
            return quanse;
        }
    }

    // ---------- 设备 ID 获取 ----------
    public static String getAndroidId() {
        if (!TextUtils.isEmpty(cachedAndroidId)) return cachedAndroidId;
        try {
            Context context = App.getAppContext();
            if (context == null) return "";
            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            cachedAndroidId = (androidId != null) ? androidId : "";
            return cachedAndroidId;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // ---------- AppAcid 初始化 ----------
    public static class AppAcidHelper {
        private final SharedPreferences manager;

        public AppAcidHelper(SharedPreferences manager) {
            this.manager = manager;
        }

        public void initAppAcid() {
            String appAcid = manager.getString("AppAcId", null);
            if (appAcid == null || appAcid.trim().isEmpty() || appAcid.length() != 16) {
                String rawUuid = UUID.randomUUID().toString();
                String newAcid = rawUuid.replace("-", "").substring(0, 16);
                manager.edit().putString("AppAcId", newAcid).apply();
                appAcid = newAcid;
            }
            Config.setAppacid(appAcid);
        }
    }

    // ---------- 核心加密请求工具（统一生成 Headers 和 Body） ----------
    public static class EncryptedRequestHelper {
        /**
         * 对任意明文字符串进行加密，返回 RequestBody
         *
         * @param plainText 明文字符串（如 "key=搜索词"）
         * @return 加密后的请求体（Content-Type: application/x-www-form-urlencoded）
         */
        public static RequestBody buildEncryptedBodyFromPlainText(String plainText) {
            String randomString = RandomStringUtils.getRandomString();
            String rsaEncrypt = EncryptUtils.INSTANCE.RsaEncrypt(
                    new StringBuilder(randomString).reverse().toString()
            );
            try {
                // 直接用明文加密，不转 JSON
                String innerEncrypted = AesHelper.aes128Encrypt(randomString, randomString, plainText);
                String plainToEncrypt = rsaEncrypt.length() + rsaEncrypt + innerEncrypted;
                String finalEncrypted = AesHelper.aes128Encrypt(plainToEncrypt);
                return RequestBody.create(
                        MediaType.parse("application/x-www-form-urlencoded"),
                        finalEncrypted
                );
            } catch (Exception e) {
                e.printStackTrace();
                String fallbackPlain = rsaEncrypt.length() + rsaEncrypt;
                String fallbackEncrypted = AesHelper.aes128Encrypt(fallbackPlain);
                return RequestBody.create(
                        MediaType.parse("application/x-www-form-urlencoded"),
                        fallbackEncrypted
                );
            }
        }

        public static Map<String, String> buildHeaders(String md5str) {
            Map<String, String> mutableMap = new HashMap<>();
            String randomString = RandomStringUtils.getRandomString();
            String str2 = ColorKt.getQuanse() + md5str;
            long currentTimeMillis = System.currentTimeMillis();
            String versionCode = getVersionCode;
            long j = currentTimeMillis - 59852;

            String plain = j + versionCode + ColorKt.getQuanse()
                    + SecurityUtils.INSTANCE.getRawSignature() + str2;
            String hmacSha256 = EncryptUtils.hmacSha256(plain, randomString);

            mutableMap.put("time", String.valueOf(currentTimeMillis));
            mutableMap.put("sgin", new StringBuilder(hmacSha256).reverse().toString());
            mutableMap.put("ref", versionCode);
            mutableMap.put("version", version);
            mutableMap.put("pkg", "com.onekmx.xsmddha");
            mutableMap.put("oaid", Config.getOaid());
            mutableMap.put("adid", getAndroidId());
            mutableMap.put("appacid", Config.getAppacid());

            mutableMap.put("k", EncryptUtils.INSTANCE.RsaEncrypt(
                    new StringBuilder(randomString).reverse().toString()
            ));
            mutableMap.put("d", AesHelper.aes128Encrypt(
                    randomString, randomString,
                    new StringBuilder(str2).reverse().toString()
            ));
            Log.d("Encrypt", "plain: " + plain);
            Log.d("Encrypt", "randomString: " + randomString);
            Log.d("Encrypt", "hmacSha256: " + hmacSha256);
            Log.d("Encrypt", "sgin: " + new StringBuilder(hmacSha256).reverse().toString());
            return mutableMap;
        }

        public static RequestBody buildEncryptedBody(JSONObject jsonObject) {
            String randomString = RandomStringUtils.getRandomString();
            String rsaEncrypt = EncryptUtils.INSTANCE.RsaEncrypt(
                    new StringBuilder(randomString).reverse().toString()
            );
            try {
                String jsonStr = jsonObject.toString();
                String innerEncrypted = AesHelper.aes128Encrypt(randomString, randomString, jsonStr);
                String decrypted = AesHelper.aes128Decrypt(randomString, innerEncrypted);
                Log.d("Encrypt", "解密验证: " + decrypted);
                if (!jsonStr.equals(decrypted)) {
                    Log.e("Encrypt", "加密/解密不一致！");
                }
                String plainToEncrypt = rsaEncrypt.length() + rsaEncrypt + innerEncrypted;
                String finalEncrypted = AesHelper.aes128Encrypt(plainToEncrypt);
                // 在 return 之前
                String decryptedFinal = AesHelper.aes128Decrypt(Config.INSTANCE.getAES_KEY(), finalEncrypted);
                Log.d("SelfTest", "decryptedFinal: " + decryptedFinal);
// 解析 plainToEncrypt 的格式：长度 + RSA密文 + innerEncrypted
                String lengthStr = decryptedFinal.substring(0, String.valueOf(rsaEncrypt.length()).length());
                int len = Integer.parseInt(lengthStr);
                String rsaPart = decryptedFinal.substring(lengthStr.length(), lengthStr.length() + len);
                String innerPart = decryptedFinal.substring(lengthStr.length() + len);
                Log.d("SelfTest", "innerPart: " + innerPart);
                String decryptedInner = AesHelper.aes128Decrypt(randomString, innerPart);
                Log.d("SelfTest", "decryptedInner: " + decryptedInner); // 应该是 jsonStr
                return RequestBody.create(
                        MediaType.parse("application/x-www-form-urlencoded"),
                        finalEncrypted
                );
            } catch (Exception e) {
                e.printStackTrace();
                String fallbackPlain = rsaEncrypt.length() + rsaEncrypt;
                String fallbackEncrypted = AesHelper.aes128Encrypt(fallbackPlain);
                return RequestBody.create(
                        MediaType.parse("application/x-www-form-urlencoded"),
                        fallbackEncrypted
                );
            }
        }
    }

    // ---------- 构造函数 & 初始化 ----------
    public DuManWuApp(Source source) {
        init(source);
        getImagesConfig().setUseWebParser(true);
        new AppAcidHelper(App.getAppContext().getSharedPreferences("DumanwuApp", MODE_PRIVATE)).initAppAcid();
    }

    public static Source getDefaultSource() {
        return new Source(null, DEFAULT_TITLE, TYPE, true);
    }

    @Override
    public Request getSearchRequest(String keyword, int page) throws Exception {
        if (page != 1) return null;
        // 在任意地方测试
        String key3 = "4STAjlKtgaXwS9io";
        String plain = "test";
        String enc = AesHelper.aes128Encrypt(key3, key3, plain);
        String dec = AesHelper.aes128Decrypt(key3, enc);
        Log.d("Test", "dec: " + dec);
        // 先判断关键词长度，若小于2则直接返回 null 或抛异常
        if (keyword == null || keyword.trim().length() < 2) {
            Log.w("Search", "关键词长度不足2，无法搜索");
            return null; // 或者自定义处理
        }
        int index = Math.min(keyword.length(), 12);
        String key = keyword.substring(0, index);

        // 构造与 SearchRequest 对应的 JSON
        JSONObject bodyJson = new JSONObject();
        bodyJson.put("key", key);
        bodyJson.put("id", "");  // id 字段默认空字符串，符合构造器默认值
        Log.d("Search", "明文JSON: " + bodyJson);
        // 加密生成请求体
        RequestBody body = EncryptedRequestHelper.buildEncryptedBody(bodyJson);

        // 生成加密 Headers（md5str 使用关键词）
        Map<String, String> headers = EncryptedRequestHelper.buildHeaders(key);

        Request.Builder builder = new Request.Builder()
                .url(MH_BASE_URL + "/search")   // 路径为 /s
                .post(body);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            // 原有逻辑中 oaid 被置空，若需要可保留，否则正常添加
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    @Override
    public SearchIterator getSearchIterator(String html, int page) throws JSONException {
        JSONObject data = new JSONObject(html);
        String responseData = data.getString("responseData");
        String aes128Decrypt = AesHelper.aes128Decrypt(Config.INSTANCE.getAES_KEY(), responseData);
        if (aes128Decrypt == null) return null;
        JSONObject searchData = new JSONObject(aes128Decrypt);
        return new JsonIterator(searchData.getJSONArray("updata")) {
            @Override
            protected Comic parse(JSONObject object) throws JSONException {
                String cid = object.getString("acId");
                String cover = object.getString("acPic");
                String author = object.getString("authorName");
                String title = object.getString("bookName");
                String update = object.getString("latestChapterName");
                return new Comic(TYPE, cid, title, cover, update, author);
            }
        };
    }

    //    @Override
//    public Request getInfoRequest(String cid) {
//        Map<String, String> headers = EncryptedRequestHelper.buildHeaders(cid);
//        Request.Builder builder = new Request.Builder()
//                .url(MH_BASE_URL + "/book/" + cid)   // 改为 /book/{id}
//                .get();  // 使用 GET
//        for (Map.Entry<String, String> entry : headers.entrySet()) {
//            builder.addHeader(entry.getKey(), entry.getValue());
//        }
//        return builder.build();
//    }
    @Override
    public Request getInfoRequest(String cid) {
        Map<String, String> headers = EncryptedRequestHelper.buildHeaders(cid);
        return new Request.Builder()
                .url(MH_BASE_URL + "/book/" + cid)
                .headers(Headers.of(headers))
                .get()
                .build();
    }

    @Override
    public Request getChapterRequest(String html, String cid) {
        Map<String, String> headers = EncryptedRequestHelper.buildHeaders(cid);
        Request.Builder builder = new Request.Builder()
                .url(MH_BASE_URL + "/chapterlist/" + cid);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    @Override
    public Request getImagesRequest(String cid, String path) {
        JSONObject params = new JSONObject();
        try {
            params.put("otime", String.valueOf(openAddTime));
            params.put("lnum", String.valueOf(lnum));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = EncryptedRequestHelper.buildEncryptedBody(params);
        Map<String, String> headers = EncryptedRequestHelper.buildHeaders(cid + path);

        Request.Builder builder = new Request.Builder()
                .url(StringUtils.format("%s/readcomic/%s/%s", MH_BASE_URL, cid, path))
                .method("POST", body);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    // ---------- 重写解析方法（响应解密保持不变） ----------

    @Override
    public Comic parseInfo(String html, Comic comic) throws UnsupportedEncodingException, JSONException {
        JSONObject bookDetail = new JSONObject(html).getJSONObject("data").getJSONObject("bookdetailed");
        String title = bookDetail.getString("bookName");
        String cover = bookDetail.getString("coverPic");
        String timestamp = bookDetail.getString("latestChapterTime");
        Instant instant = Instant.ofEpochMilli(Long.parseLong(timestamp) * 1000);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String update = dateTime.format(formatter);
        String intro = bookDetail.getString("intro");
        String author = bookDetail.getString("authorName");
        comic.setInfo(title, cover, update, intro, author, false);
        return comic;
    }

    @Override
    public List<Chapter> parseChapter(String html, Comic comic, Long sourceComic) throws JSONException {
        JSONObject data = new JSONObject(html);
        String responseData = data.getString("responseData");
        String aes128Decrypt = AesHelper.aes128Decrypt(Config.INSTANCE.getAES_KEY(), responseData);
        if (aes128Decrypt == null) return new LinkedList<>();
        JSONObject chapterData = new JSONObject(aes128Decrypt);
        JSONArray chapList = chapterData.getJSONArray("chaplist");
        List<Chapter> list = new LinkedList<>();
        for (int i = 0; i < chapList.length(); i++) {
            JSONObject item = chapList.getJSONObject(i);
            String title = item.getString("chaptername");
            String path = item.getString("chapterid");
            list.add(new Chapter(null, sourceComic, title, path));
        }
        list = Lists.reverse(list);
        for (int j = 0; j < list.size(); j++) {
            long id = IdCreator.createChapterId(sourceComic, j);
            list.get(j).setId(id);
        }
        return list;
    }

    @Override
    public List<ImageUrl> parseImages(String html, Chapter chapter) throws Manga.NetworkErrorException, JSONException {
        List<ImageUrl> list = new ArrayList<>();
        if (html.contains("<html>")) {
            Document document = Jsoup.parse(html);
            html = document.body().text();
            if (html.startsWith("\"")) {
                html = html.substring(1, html.length() - 1);
            }
        }
        JSONObject data = new JSONObject(html);
        String responseData = data.getString("responseData");
        String aes128Decrypt = AesHelper.aes128Decrypt(Config.INSTANCE.getAES_KEY(), responseData);
        if (aes128Decrypt == null) return list;
        JSONArray imgList = new JSONObject(aes128Decrypt).getJSONArray("piclist");
        for (int i = 1; i <= imgList.length(); i++) {
            long comicChapter = chapter.getId();
            long id = IdCreator.createImageId(comicChapter, i);
            String imgUrl = imgList.getString(i - 1);
            list.add(new ImageUrl(id, comicChapter, i, imgUrl, false));
        }
        return list;
    }

    @Override
    public Headers getHeader() {
        return Headers.of("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36");
    }

}