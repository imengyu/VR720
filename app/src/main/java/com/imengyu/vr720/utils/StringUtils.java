package com.imengyu.vr720.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /***
     * 是否包含指定字符串,不区分大小写
     * @param input 原字符串
     * @param regex 正则
     */
    public static boolean containsIgnoreCase(String input, String regex) {
        if(StringUtils.isNullOrEmpty(input))
            return false;
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(input);
        return m.find();
    }

    /**
     * 替换正则表达式特殊字符
     * @param input 源字符串
     */
    public static String replaceRegexSpecialChar(String input) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if(ch == '?' || ch == '!' || ch == '\\' || ch == '\'' || ch == '\"' || ch == '^' || ch == '$'|| ch == '[' || ch == ']')
                sb.append('\\');
            else if(ch == '(' || ch == '|' || ch == '*' || ch == '+' || ch == ')' || ch == '.'  || ch == '{' || ch == '}')
                sb.append('\\');
            sb.append(ch);
        }
        return sb.toString();
    }
}
