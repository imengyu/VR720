package com.dreamfish.com.vr720.core;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import com.dreamfish.com.vr720.config.MainMessages;

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
    public NativeVR720Renderer(Handler uiHandler) {
        mainHandler = uiHandler;
        onCreate();
    }

    private final Handler mainHandler;
    private long mainNativePtr = 0;

    public void nativeFeedBackMessage(int msg) {
        Message msgObj = new Message();
        msgObj.what = MainMessages.MSG_NATIVE_MESSAGE;
        msgObj.obj = msg;
        mainHandler.sendMessage(msgObj);
    }
    public void nativeSetNativePtr(long ptr) { mainNativePtr = ptr; }
    public long nativeGetNativePtr() { return mainNativePtr; }

    //消息回传
    public static final int MobileGameUIEvent_MarkLoadingStart = 0;
    public static final int MobileGameUIEvent_MarkLoadingEnd = 1;
    public static final int MobileGameUIEvent_MarkLoadFailed = 2;
    public static final int MobileGameUIEvent_UiInfoChanged = 3;
    public static final int MobileGameUIEvent_FileClosed = 4;

    //全景模式
    public static final int PanoramaMode_PanoramaSphere = 0;
    public static final int PanoramaMode_PanoramaCylinder = 1;
    public static final int PanoramaMode_PanoramaAsteroid = 2;
    public static final int PanoramaMode_PanoramaOuterBall = 3;
    public static final int PanoramaMode_PanoramaMercator = 4;
    public static final int PanoramaMode_PanoramaFull360 = 5;
    public static final int PanoramaMode_PanoramaFullOrginal = 6;
    public static final int PanoramaMode_PanoramaModeMax = 7;

    //C++代码声明
    //***********************************

    private native void onCreate();

    public void onSurfaceCreated() { onSurfaceCreated(mainNativePtr); }
    public void onSurfaceChanged(int width, int height) { onSurfaceChanged(mainNativePtr, width, height); }
    public void onDrawFrame() { onDrawFrame(mainNativePtr); }
    public void onMainThread() { onMainThread(mainNativePtr); }
    public void onDestroy() { onDestroy(mainNativePtr); }

    private native void onSurfaceCreated(long nativePtr);
    private native void onSurfaceChanged(long nativePtr, int width, int height);
    private native void onDrawFrame(long nativePtr);
    private native void onMainThread(long nativePtr);
    private native void onDestroy(long nativePtr);
    private native void openFile(long nativePtr, String path);
    private native String getLastError(long nativePtr);
    private native void closeFile(long nativePtr);
    private native void processMouseMove(long nativePtr, float x, float y);
    private native void processMouseDown(long nativePtr, float x, float y);
    private native void processMouseUp(long nativePtr, float x, float y);
    private native void processViewZoom(long nativePtr, float v);
    private native void processKey(long nativePtr, int key, boolean down);
    private native int getPanoramaMode(long nativePtr);
    private native void setPanoramaMode(long nativePtr, int mode);
    private native void setGryoEnable(long nativePtr, boolean enable);
    private native void setVREnable(long nativePtr, boolean enable);
    private native void updateGryoValue(long nativePtr, float x, float y, float z);

    /**
     * 打开全景图片文件
     * @param path 文件路径
     */
    public void openFile(String path) { openFile(mainNativePtr, path); }
    /**
     * 获取打开文件的错误信息
     * @return 错误信息
     */
    public String getLastError() { return getLastError(mainNativePtr); }
    /**
     * 关闭当前文件
     */
    public void closeFile() { closeFile(mainNativePtr); }

    /**
     * 进行移动视图
     * @param x x坐标
     * @param y y坐标
     */
    public void processMouseMove(float x, float y) { processMouseMove(mainNativePtr, x, y); }
    /**
     * 进行移动视图
     * @param y x坐标
     * @param x y坐标
     */
    public void processMouseDown(float x, float y) { processMouseDown(mainNativePtr, x, y); }
    /**
     * 进行移动视图
     * @param x x偏移参数
     * @param y y偏移参数
     */
    public void processMouseUp(float x, float y) { processMouseUp(mainNativePtr, x, y); }

    /**
     * 进行缩放视图
     * @param v +-参数
     */
    public void processViewZoom(float v) { processViewZoom(mainNativePtr, v); }

    /**
     * 处理按键事件
     * @param key 按键
     * @param down 是否是按下
     */
    public void processKey(int key, boolean down) { processKey(mainNativePtr, key, down); }

    /**
     * 获取当前全景模式
     * @return 全景模式（PANO_MODE_*）
     */
    public int getPanoramaMode() { return getPanoramaMode(mainNativePtr); }
    /**
     * 设置全景模式
     * @param mode 全景模式（PANO_MODE_*）
     */
    public void setPanoramaMode(int mode) { setPanoramaMode(mainNativePtr, mode); }

    /**
     * 设置是否启用陀螺仪
     * @param enable 是否启用
     */
    public void setGryoEnable(boolean enable) { setGryoEnable(mainNativePtr, enable); }

    /**
     * 强制更新陀螺仪参数
     */
    public void updateGryoValue(float x, float y, float z) { updateGryoValue(mainNativePtr, x,y,z); }

    /**
     * 设置是否启用VR双屏模式
     * @param enable 是否启用
     */
    public void setVREnable(boolean enable) { setVREnable(mainNativePtr, enable); }
}
