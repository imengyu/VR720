package com.dreamfish.com.vr720.core;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Build;

/**
 * 本地渲染器
 */
public class NativeVR720Renderer {

    /**
     * 检查是否是模拟器中
     */
    public static boolean isProbablyEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86");
    }

    /**
     * 检查本机是否支持OPENGL ES2
     * @param context 上下文
     * @return 是否支持
     */
    public static boolean checkSupportsEs2(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000 || isProbablyEmulator();
    }

    /**
     * 创建本地渲染器
     */
    public NativeVR720Renderer() {
        onCreate();
    }

    //C++代码声明
    //***********************************

    private native void onCreate();

    public native void onSurfaceCreated();
    public native void onSurfaceChanged(int width, int height);
    public native void onDrawFrame();
}
