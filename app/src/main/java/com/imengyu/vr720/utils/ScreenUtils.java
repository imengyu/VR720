package com.imengyu.vr720.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.util.Size;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/**
 * 全屏工具类
 */
public class ScreenUtils {

    /**
     * 设置是否全屏
     * @param activity Activity
     * @param fullScreen 是否全屏
     */
    public static void setFullScreen(Activity activity, boolean fullScreen) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                if(!fullScreen) insetsController.show(WindowInsets.Type.statusBars());
                else insetsController.hide(WindowInsets.Type.statusBars());
            }

            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
        else {
            WindowManager.LayoutParams attrs = window.getAttributes();
            if(fullScreen)
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            else
                attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);

            window.setAttributes(attrs);
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    }

    /**
     * 获取屏幕大小（宽高为竖屏时的大小）
     * @param context 上下文
     * @return 屏幕大小（宽高为竖屏时的大小）
     */
    public static Size getScreenSize(Context context) {
        Size screenSize  = null;

        Point point = new Point();
        context.getDisplay().getRealSize(point);
        Size getSize = new Size(point.x, point.y);

        Configuration newConfig = context.getResources().getConfiguration();
        if(newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
            screenSize = new Size(getSize.getWidth(), getSize.getHeight());
        else if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            screenSize = new Size(getSize.getHeight(), getSize.getWidth());

        return screenSize;
    }
}
