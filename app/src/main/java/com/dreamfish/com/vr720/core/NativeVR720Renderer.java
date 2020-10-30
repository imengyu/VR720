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
        assert activityManager != null;
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


    /**
     * 打开全景图片文件
     * @param path 文件路径
     * @return 返回是否成功，失败可以调用 getLastError 获取错误信息
     */
    public native boolean openFile(String path);
    /**
     * 获取打开文件的错误信息
     * @return 错误信息
     */
    public native String getLastError();
    /**
     * 关闭当前文件
     */
    public native void closeFile();

    /**
     * 进行移动视图
     * @param xOff x偏移参数
     * @param yOff y偏移参数
     */
    public native void processMouseMove(float xOff, float yOff);
    /**
     * 进行缩放视图
     * @param v +-参数
     */
    public native void processMouseScroll(float v);
    /**
     * 进行缩放视图
     * @param v +-参数
     */
    public native void processKey(int key);

    /**
     * 获取当前全景模式
     * @return 全景模式（PANO_MODE_*）
     */
    public native int getPanoramaMode();
    /**
     * 设置全景模式
     * @param mode 全景模式（PANO_MODE_*）
     */
    public native void setPanoramaMode(int mode);

}
