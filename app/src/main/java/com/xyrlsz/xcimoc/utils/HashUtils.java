package com.xyrlsz.xcimoc.utils;

import java.security.MessageDigest;

public class HashUtils {
    public static String MD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            return convertByteToHex(messageDigest);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String MD5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input);
            return convertByteToHex(messageDigest);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convertByteToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
