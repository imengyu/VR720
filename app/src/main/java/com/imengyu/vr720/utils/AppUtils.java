package com.imengyu.vr720.utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * app工具类
 */
public class AppUtils {

    /**
     * 跳转到APP应用商店指定包名的应用详情页
     * @param context 上下文
     * @param packageName 包名
     */
    public static void goToAppStore(Context context, String packageName) {
        Uri uri = Uri.parse("market://details?id=" + packageName);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 设置语言
     * @param context 上下文
     * @param val 语言标识
     */
    public static void setLanguage(Context context, String val) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(StringUtils.isNullOrEmpty(val) ? Locale.getDefault() : Locale.forLanguageTag(val));
        context.getResources().updateConfiguration(configuration, metrics);
    }
}
