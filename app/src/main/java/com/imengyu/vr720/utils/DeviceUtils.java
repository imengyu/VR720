package com.imengyu.vr720.utils;

public class DeviceUtils {
    public static boolean isXiaomi() {
        String brand = android.os.Build.BRAND.trim().toUpperCase();
        return brand.contains("XIAOMI");
    }
    public static boolean isHuawei() {
        String brand = android.os.Build.BRAND.trim().toUpperCase();
        return brand.contains("HWAWEI");
    }
    public static boolean isOppo() {
        String brand = android.os.Build.BRAND.trim().toUpperCase();
        return brand.contains("OPPO");
    }
    public static boolean isVivo() {
        String brand = android.os.Build.BRAND.trim().toUpperCase();
        return brand.contains("VIVO");
    }
}
