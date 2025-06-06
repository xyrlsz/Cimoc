package com.xyrlsz.xcimoc.utils.dmzj;


import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;

public class RsaDecryptor {
    private static final String PRIVATE_KEY_BASE64 =
            "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAK8nNR1lTnIfIes6oRWJNj3mB6OssDGx0uGMpgpbVCpf6+VwnuI2stmhZNoQcM417Iz7WqlPzbUmu9R4dEKmLGEEqOhOdVaeh9Xk2IPPjqIu5TbkLZRxkY3dJM1htbz57d/roesJLkZXqssfG5EJauNc+RcABTfLb4IiFjSMlTsnAgMBAAECgYEAiz/pi2hKOJKlvcTL4jpHJGjn8+lL3wZX+LeAHkXDoTjHa47g0knYYQteCbv+YwMeAGupBWiLy5RyyhXFoGNKbbnvftMYK56hH+iqxjtDLnjSDKWnhcB7089sNKaEM9Ilil6uxWMrMMBH9v2PLdYsqMBHqPutKu/SigeGPeiB7VECQQDizVlNv67go99QAIv2n/ga4e0wLizVuaNBXE88AdOnaZ0LOTeniVEqvPtgUk63zbjl0P/pzQzyjitwe6HoCAIpAkEAxbOtnCm1uKEp5HsNaXEJTwE7WQf7PrLD4+BpGtNKkgja6f6F4ld4QZ2TQ6qvsCizSGJrjOpNdjVGJ7bgYMcczwJBALvJWPLmDi7ToFfGTB0EsNHZVKE66kZ/8Stx+ezueke4S556XplqOflQBjbnj2PigwBN/0afT+QZUOBOjWzoDJkCQClzo+oDQMvGVs9GEajS/32mJ3hiWQZrWvEzgzYRqSf3XVcEe7PaXSd8z3y3lACeeACsShqQoc8wGlaHXIJOHTcCQQCZw5127ZGs8ZDTSrogrH73Kw/HvX55wGAeirKYcv28eauveCG7iyFR0PFB/P/EDZnyb+ifvyEFlucPUI0+Y87F";

    private static final int MAX_DECRYPT_BLOCK = 128;

    private static PrivateKey privateKey;

    static {
        try {
            byte[] privateKeyBytes = Base64.decode(PRIVATE_KEY_BASE64, Base64.DEFAULT);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize private key", e);
        }
    }

    public static byte[] decryptBytes(byte[] encrypted) throws Exception {
        byte[] encryptedData = Base64.decode(encrypted, Base64.DEFAULT);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        List<Byte> resultList = new ArrayList<>();

        for (int i = 0; i < encryptedData.length; i += MAX_DECRYPT_BLOCK) {
            int end = Math.min(encryptedData.length, i + MAX_DECRYPT_BLOCK);
            byte[] chunk = new byte[end - i];
            System.arraycopy(encryptedData, i, chunk, 0, chunk.length);

            byte[] decryptedChunk = cipher.doFinal(chunk);
            for (byte b : decryptedChunk) {
                resultList.add(b);
            }
        }

        byte[] result = new byte[resultList.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = resultList.get(i);
        }

        return result;
    }

    public static byte[] decryptBytes(String encrypted) throws Exception {
        return decryptBytes(encrypted.getBytes(StandardCharsets.UTF_8));
    }
}