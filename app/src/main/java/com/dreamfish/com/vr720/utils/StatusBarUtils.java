package com.dreamfish.com.vr720.utils;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class StatusBarUtils {

  public static void setLightMode(Activity activity) {

    Window window = activity.getWindow();

    // 设置状态栏底色白色
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    window.setStatusBarColor(Color.WHITE);

    // 设置状态栏字体黑色
    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
  }
  public static void setDarkMode(Activity activity) {

    Window window = activity.getWindow();

    // 设置状态栏底色白色
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    window.setStatusBarColor(Color.BLACK);

    // 设置状态栏字体黑色
    window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
  }

  /**
   * 修改状态栏颜色，支持4.4以上版本
   * @param activity
   * @param color
   */
  public static void setStatusBarColor(Activity activity, int color) {
    Window window = activity.getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    window.setStatusBarColor(color);
  }
}
