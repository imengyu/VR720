package com.imengyu.vr720.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {

    public static String md5(String plainText) {
        return md5(plainText.getBytes());
    }
    public static String md5(byte[] bytes) {
        byte[] secretBytes;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有这个md5算法！");
        }
        StringBuilder md5code = new StringBuilder(new BigInteger(1, secretBytes).toString(16));
        for (int i = 0; i < 32 - md5code.length(); i++)
            md5code.insert(0, "0");
        return md5code.toString();
    }

}
