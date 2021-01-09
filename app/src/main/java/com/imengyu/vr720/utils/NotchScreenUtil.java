package com.imengyu.vr720.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.List;

public class NotchScreenUtil {
    private static final String TAG = "NotchScreenUtil";


    public static final int VIVO_NOTCH = 0x00000020;//是否有刘海
    public static final int VIVO_FILLET = 0x00000008;//是否有圆角

    /**
     * 判断VIVO是否是刘海屏
     * @param context 上下文
     */
    public static boolean hasNotchInScreenAtVivo(Context context) {
        boolean ret = false;
        try {
            ClassLoader classLoader = context.getClassLoader();
            Class FtFeature = classLoader.loadClass("android.util.FtFeature");
            Method method = FtFeature.getMethod("isFeatureSupport", int.class);
            ret = (boolean) method.invoke(FtFeature, VIVO_NOTCH);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "hasNotchAtVivo ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "hasNotchAtVivo NoSuchMethodException");
        } catch (Exception e) {
            Log.e(TAG, "hasNotchAtVivo Exception");
        } 
        return ret;
    }
    /**
     * 判断华为是否是刘海屏
     * @param context 上下文
     */
    public static boolean hasNotchInScreenAtHuawei(Context context) {
        boolean ret = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class<?> HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method get = HwNotchSizeUtil.getMethod("hasNotchInScreen");
            ret = (Boolean) get.invoke(HwNotchSizeUtil);
            Log.d(TAG, "this Huawei device has notch in screen？"+ret);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "hasNotchInScreen ClassNotFoundException", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "hasNotchInScreen NoSuchMethodException", e);
        } catch (Exception e) {
            Log.e(TAG, "hasNotchInScreen Exception", e);
        }
        return ret;
    }

    /**
     * 判断Oppo是否是刘海屏
     * @param context 上下文
     */
    public static boolean hasNotchInScreenAtOppo(Context context) {
        boolean hasNotch = context.getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism");
        Log.d(TAG, "this OPPO device has notch in screen？"+hasNotch);
        return hasNotch;
    }
    /**
     * 判断是否是刘海屏
     *
     * @return 是否是刘海屏
     */
    public static boolean hasNotchScreen(Activity activity) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowInsets rootWindowInsets = activity.getWindow().getDecorView().getRootWindowInsets();
            if (rootWindowInsets == null)
                return false;
            DisplayCutout displayCutout = rootWindowInsets.getDisplayCutout();
            if(displayCutout == null)
                return false;
            List<Rect> rects = displayCutout.getBoundingRects();
            return (rects != null && rects.size() != 0);
        }
        else return getInt("ro.miui.notch", activity) || hasNotchInScreenAtHuawei(activity)
                || hasNotchInScreenAtOppo(activity)
                || hasNotchInScreenAtVivo(activity);
    }
    /**
     * 小米刘海屏判断.
     *
     * @return property ro.miui.notch，值为1时则是 Notch 屏手机
     * SystemProperties.getInt("ro.miui.notch", 0) == 1;
     */
    public static boolean getInt(String key, Activity activity) {
        int result = 0;
        if (DeviceUtils.isXiaomi()) {
            try {
                ClassLoader classLoader = activity.getClassLoader();
                @SuppressWarnings("rawtypes")
                Class SystemProperties = classLoader.loadClass("android.os.SystemProperties");
                //参数类型
                @SuppressWarnings("rawtypes")
                Class[] paramTypes = new Class[2];
                paramTypes[0] = String.class;
                paramTypes[1] = int.class;
                Method getInt = SystemProperties.getMethod("getInt", paramTypes);
                //参数
                Object[] params = new Object[2];
                params[0] = key;
                params[1] = 0;
                result = (Integer) getInt.invoke(SystemProperties, params);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result == 1;
    }


    public static int getNotchSizeAtOppo() {
        return 80;
    }
    /**
     * 获取华为刘海高度
     */
    public static int getNotchSizeAtHuawei(Context context) {
        int[] ret = new int[] { 0, 0 };
        try {
            ClassLoader cl = context.getClassLoader();
            Class<?> HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method get = HwNotchSizeUtil.getMethod("getNotchSize");
            ret = (int[]) get.invoke(HwNotchSizeUtil);

        } catch (ClassNotFoundException e) {
            Log.e(TAG, "getNotchSize ClassNotFoundException");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "getNotchSize NoSuchMethodException");
        } catch (Exception e) {
            Log.e(TAG, "getNotchSize Exception");
        }
        return ret[1];
    }
    /**
     * 获取VIVO刘海高度
     */
    public static int getNotchSizeAtVivo(Context context){
        return PixelTool.dp2px(context, 32);
    }
    /**
     * 获取小米刘海高度
     */
    public static int getNotchSizeAtXiaomi(Activity activity) {
        int resourceId = activity.getResources().getIdentifier("notch_height", "dimen", "android");
        if (resourceId > 0) {
            return activity.getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    /**
     * 获取刘海屏高度
     * @param activity 上下文
     * @return 刘海屏高度(px)
     */
    public static int getNotchSize(Activity activity) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            View decorView = activity.getWindow().getDecorView();
            WindowInsets rootWindowInsets = decorView.getRootWindowInsets();
            if(rootWindowInsets == null)
                return 0;
            DisplayCutout displayCutout = rootWindowInsets.getDisplayCutout();
            if(displayCutout == null)
                return 0;
            return displayCutout.getSafeInsetTop();
        } else {
            if (DeviceUtils.isXiaomi())
                return getNotchSizeAtXiaomi(activity);
            if (DeviceUtils.isHuawei())
                return getNotchSizeAtHuawei(activity);
            if (DeviceUtils.isOppo())
                return getNotchSizeAtOppo();
            if (DeviceUtils.isVivo())
                return getNotchSizeAtVivo(activity);
        }
        return 0;
    }

    /**
     * 设置强制刘海屏布局
     * @param activity Activity
     */
    public static void setCutoutForceNever(Activity activity) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            window.setAttributes(lp);
        }
    }
    /**
     * 设置强制刘海屏布局
     * @param activity Activity
     */
    public static void setCutoutForceShortEdges(Activity activity) {
        Window window = activity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(lp);
        }
    }
}
