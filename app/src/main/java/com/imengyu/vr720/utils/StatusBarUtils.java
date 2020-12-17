package com.imengyu.vr720.utils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/**
 * 状态栏工具栏
 */
public class StatusBarUtils {

  /**
   * 设置状态栏亮色
   * @param activity Activity
   */
  public static void setLightMode(Activity activity) {
    Window window = activity.getWindow();

    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      final WindowInsetsController insetsController = window.getInsetsController();
      if (insetsController != null) {
        insetsController.show(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
      }
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    window.setStatusBarColor(Color.WHITE);
  }

  /**
   * 设置状态栏暗色
   * @param activity Activity
   */
  public static void setDarkMode(Activity activity) {
    Window window = activity.getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      final WindowInsetsController insetsController = window.getInsetsController();
      if (insetsController != null) {
        insetsController.hide(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
      }
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
    window.setStatusBarColor(Color.BLACK);

  }

  /**
   * 修改状态栏颜色，支持4.4以上版本
   * @param activity  Activity
   * @param color 状态栏背景颜色
   */
  public static void setStatusBarColor(Activity activity, int color) {
    Window window = activity.getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
    window.setStatusBarColor(color);
  }
}
