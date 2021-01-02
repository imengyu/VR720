package com.imengyu.vr720.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.DisplayMetrics;

import androidx.preference.PreferenceManager;

import com.imengyu.vr720.dialog.AppDialogs;
import com.imengyu.vr720.model.TestAgreementAllowedCallback;

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

    /**
     * 检查应用许可有没有同意
     * @param activity 活动
     * @param callback 回调
     */
    public static void testAgreementAllowed(Activity activity, TestAgreementAllowedCallback callback) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if(!prefs.getBoolean("app_agreement_allowed", false)) {
            AppDialogs.showPrivacyPolicyAndAgreement(activity, (allowed) -> {
                if(allowed) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("app_agreement_allowed", true);
                    editor.apply();
                    callback.testAgreementAllowedCallback(false);
                }else activity.finish();
            });
        }else callback.testAgreementAllowedCallback(true);
    }
}
