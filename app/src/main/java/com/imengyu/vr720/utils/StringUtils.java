package com.imengyu.vr720.utils;

/**
 * 字符串工具类
 */
public class StringUtils {

    /**
     * 检查文字是否为null或空字符串
     * @param s 字符串
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * 获取可读时间字符串
     * @param millisecond 毫秒
     * @return 字符串
     */
    public static String getTimeString(int millisecond) {
        int hour = millisecond / (1000 * 3600);
        int min = millisecond / (1000 * 60) - hour * 60;
        int sec = millisecond / 1000 - (hour * 60 + min) * 60;

        StringBuilder sb = new StringBuilder();
        if(hour > 0) {
            sb.append(hour);
            sb.append(':');
            if(min < 10)
                sb.append('0');
        }

        sb.append(min);
        sb.append(':');
        if(sec < 10)
            sb.append('0');
        sb.append(sec);

        return sb.toString();
    }
}
