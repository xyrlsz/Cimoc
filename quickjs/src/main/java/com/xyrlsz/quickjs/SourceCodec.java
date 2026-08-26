package com.xyrlsz.quickjs;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 漫画源脚本常用的计算类工具（宿主实现）。
 * <p>
 * 这些函数原先以纯 JS 实现于 source_sdk.js，因 JS 解释执行较慢，现统一迁到
 * quickjs 库模块、由宿主（JsHost）经 hostCall 调用。实现完全自包含（不依赖
 * app 模块的 utils），行为与旧实现对齐：
 * <ul>
 *     <li>md5：RFC1321，UTF-8 输入，小写 hex</li>
 *     <li>base64：UTF-8、无换行、容忍无 padding / 空白</li>
 *     <li>lz64：LZ-string decompressFromBase64（对齐 DecryptionUtils.LZ64Decrypt）</li>
 *     <li>aesCbcDecrypt：AES/CBC/PKCS7，密文支持 base64 或 hex（hex 先转 base64）；
 *          ivPrefix 模式把密文前 16 字节作为 IV</li>
 *     <li>urlEncode/urlDecode：对齐 java.net.URLEncoder/URLDecoder（空格↔'+'）</li>
 * </ul>
 * 所有方法入参为 null 时返回 null；失败返回 null（宿主侧统一以 "null" 表达）。
 */
public final class SourceCodec {

    private SourceCodec() {
    }

    /* ---------------- MD5 ---------------- */

    /** RFC1321 MD5，UTF-8 输入，小写 hex。失败返回 null。 */
    public static String md5(String input) {
        if (input == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xf, 16));
                sb.append(Character.forDigit(b & 0xf, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /* ---------------- Base64 ---------------- */

    /** 去除空白（兼容多行 Base64），再按 android Base64 解码（容忍无 padding）。失败返回 null。 */
    private static byte[] decodeLenient(String data) {
        if (data == null) return null;
        try {
            String clean = data.replaceAll("[\\r\\n\\t \\f]", "");
            return Base64.decode(clean, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    /** UTF-8 字符串 → 标准 Base64（无换行）。入参 null → null。 */
    public static String base64Encode(String data) {
        if (data == null) return null;
        return Base64.encodeToString(data.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    /** 标准 Base64 → UTF-8 字符串。失败返回 null。 */
    public static String base64Decode(String data) {
        byte[] bytes = decodeLenient(data);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /** base64url → UTF-8 字符串。失败返回 null。 */
    public static String base64UrlDecode(String data) {
        if (data == null) return null;
        try {
            String clean = data.replaceAll("[\\r\\n\\t \\f]", "")
                    .replace('-', '+').replace('_', '/');
            byte[] bytes = Base64.decode(clean, Base64.NO_WRAP);
            return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /* ---------------- URL ---------------- */

    /** URL 编码（UTF-8，空格→'+'）。失败返回 null。 */
    public static String urlEncode(String data) {
        if (data == null) return null;
        try {
            return java.net.URLEncoder.encode(data, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /** URL 解码（'+'→空格，UTF-8）。失败返回 null。 */
    public static String urlDecode(String data) {
        if (data == null) return null;
        try {
            return java.net.URLDecoder.decode(data, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /* ---------------- AES-CBC / PKCS7 ---------------- */

    private static final byte[] HEX = new byte[256];

    static {
        for (int i = 0; i < 256; i++) HEX[i] = -1;
        for (int i = 0; i < 10; i++) HEX['0' + i] = (byte) i;
        for (int i = 0; i < 6; i++) {
            HEX['a' + i] = (byte) (10 + i);
            HEX['A' + i] = (byte) (10 + i);
        }
    }

    /** hex → 字节数组；非 hex 或奇数长度返回 null。 */
    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) return null;
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            byte hi = HEX[hex.charAt(i * 2) & 0xff];
            byte lo = HEX[hex.charAt(i * 2 + 1) & 0xff];
            if (hi < 0 || lo < 0) return null;
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static boolean isHex(String s) {
        if (s == null || s.length() % 2 != 0) return false;
        for (int i = 0; i < s.length(); i++) {
            if (HEX[s.charAt(i) & 0xff] < 0) return false;
        }
        return true;
    }

    /** 标准 AES-CBC 解密（AES/CBC/PKCS7）。密文支持 base64 或 hex（hex 先转 base64）。失败返回 null。 */
    public static String aesCbcDecrypt(String value, String key, String iv) {
        if (value == null || key == null || iv == null) return null;
        try {
            String cipher = value;
            if (isHex(cipher)) {
                byte[] bytes = hexToBytes(cipher);
                if (bytes == null) return null;
                cipher = Base64.encodeToString(bytes, Base64.NO_WRAP);
            }
            byte[] code = decodeLenient(cipher);
            if (code == null) return null;
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
            Cipher c = Cipher.getInstance("AES/CBC/PKCS7Padding");
            c.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            return new String(c.doFinal(code), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** ivPrefix 模式：密文为 base64，前 16 字节作 IV，其余为密文。失败返回 null。 */
    public static String aesCbcDecryptWithIvPrefix(String value, String key) {
        if (value == null || key == null) return null;
        try {
            byte[] all = decodeLenient(value);
            if (all == null || all.length <= 16) return null;
            byte[] iv = Arrays.copyOfRange(all, 0, 16);
            byte[] body = Arrays.copyOfRange(all, 16, all.length);
            String ivStr = new String(iv, StandardCharsets.UTF_8);
            String bodyB64 = Base64.encodeToString(body, Base64.NO_WRAP);
            return aesCbcDecrypt(bodyB64, key, ivStr);
        } catch (Exception e) {
            return null;
        }
    }

    /* ---------------- LZ-string Base64 解压 ---------------- */

    /** 对齐 DecryptionUtils.LZ64Decrypt。入参 null → null。 */
    public static String lz64Decrypt(String str) {
        if (str == null) return null;
        if (str.isEmpty()) return "";
        final char[] valStrBase64 = new char[]{
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                62, 0, 0, 0, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 0, 0, 0, 64, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
                0, 0, 0, 0, 0, 0, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51
        };
        return lz64Decrypt(str.length(), 32, str.toCharArray(), valStrBase64, 0);
    }

    private static String lz64Decrypt(int length, int resetValue, char[] getNextValue, char[] modify, int offset) {
        ArrayList<String> dictionary = new ArrayList<>();
        int enlargeIn = 4, dictSize = 4, numBits = 3, position = resetValue, index = 1, resb, maxpower, power;
        String entry, w, c;
        ArrayList<String> result = new ArrayList<>();
        char bits, val = (modify == null) ? (char) (getNextValue[0] + offset) : modify[getNextValue[0]];

        for (char i = 0; i < 3; i++) {
            dictionary.add(String.valueOf(i));
        }

        bits = 0;
        maxpower = 2;
        power = 0;
        while (power != maxpower) {
            resb = val & position;
            position >>= 1;
            if (position == 0) {
                position = resetValue;
                val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
            }
            bits |= (char) ((resb > 0 ? 1 : 0) << power++);
        }

        switch (bits) {
            case 0:
                maxpower = 8;
                power = 0;
                while (power != maxpower) {
                    resb = val & position;
                    position >>= 1;
                    if (position == 0) {
                        position = resetValue;
                        val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                    }
                    bits |= (char) ((resb > 0 ? 1 : 0) << power++);
                }
                c = String.valueOf(bits);
                break;
            case 1:
                bits = 0;
                maxpower = 16;
                power = 0;
                while (power != maxpower) {
                    resb = val & position;
                    position >>= 1;
                    if (position == 0) {
                        position = resetValue;
                        val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                    }
                    bits |= (char) ((resb > 0 ? 1 : 0) << power++);
                }
                c = String.valueOf(bits);
                break;
            default:
                return "";
        }
        dictionary.add(c);
        w = c;
        result.add(w);
        while (true) {
            if (index > length) {
                return "";
            }

            bits = 0;
            maxpower = numBits;
            power = 0;
            while (power != maxpower) {
                resb = val & position;
                position >>= 1;
                if (position == 0) {
                    position = resetValue;
                    val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                }
                bits |= (char) ((resb > 0 ? 1 : 0) << power++);
            }
            int cc;
            switch (cc = bits) {
                case 0:
                    maxpower = 8;
                    power = 0;
                    while (power != maxpower) {
                        resb = val & position;
                        position >>= 1;
                        if (position == 0) {
                            position = resetValue;
                            val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                        }
                        bits |= (char) ((resb > 0 ? 1 : 0) << power++);
                    }
                    dictionary.add(String.valueOf(bits));
                    cc = dictSize++;
                    enlargeIn--;
                    break;
                case 1:
                    bits = 0;
                    maxpower = 16;
                    power = 0;
                    while (power != maxpower) {
                        resb = val & position;
                        position >>= 1;
                        if (position == 0) {
                            position = resetValue;
                            val = (modify == null) ? (char) (getNextValue[index++] + offset) : modify[getNextValue[index++]];
                        }
                        bits |= (char) ((resb > 0 ? 1 : 0) << power++);
                    }
                    dictionary.add(String.valueOf(bits));
                    cc = dictSize++;
                    enlargeIn--;
                    break;
                case 2:
                    StringBuilder sb = new StringBuilder(result.size());
                    for (String s : result) {
                        sb.append(s);
                    }
                    return sb.toString();
            }

            if (enlargeIn == 0) {
                enlargeIn = 1 << numBits;
                numBits++;
            }

            if (cc < dictionary.size() && dictionary.get(cc) != null) {
                entry = dictionary.get(cc);
            } else {
                if (cc == dictSize) {
                    entry = w + w.charAt(0);
                } else {
                    return "";
                }
            }
            result.add(entry);

            dictionary.add(w + entry.charAt(0));
            dictSize++;
            enlargeIn--;

            w = entry;

            if (enlargeIn == 0) {
                enlargeIn = 1 << numBits;
                numBits++;
            }
        }
    }
}
