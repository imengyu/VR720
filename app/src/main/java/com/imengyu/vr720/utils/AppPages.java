package com.imengyu.vr720.utils;

import android.app.Activity;
import android.content.Intent;

import com.imengyu.vr720.activity.AboutActivity;
import com.imengyu.vr720.activity.HtmlActivity;
import com.imengyu.vr720.activity.SettingsActivity;
import com.imengyu.vr720.config.Codes;
import com.imengyu.vr720.config.Constants;

public class AppPages {

    public static void showHelp(Activity activity) {
        Intent intent = new Intent(activity, HtmlActivity.class);
        intent.putExtra("url", Constants.HELP_PAGE_URL);
        activity.startActivity(intent);
    }

    public static void showAbout(Activity activity) {
      activity.startActivity(new Intent(activity, AboutActivity.class));
    }

    public static void showSettings(Activity activity) {
      activity.startActivityForResult(new Intent(activity, SettingsActivity.class), Codes.REQUEST_CODE_SETTING);
    }

    public static void showFeedBack(Activity activity) {
      Intent intent = new Intent(activity, HtmlActivity.class);
      intent.putExtra("url", Constants.FEED_BACK_URL);
      activity.startActivity(intent);
    }

}
