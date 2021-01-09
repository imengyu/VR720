package com.imengyu.vr720.core.natives;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class NativeVR720 {

    private static boolean libLoadFailed = false;

    public static boolean isLibLoadSuccess() {
        return !libLoadFailed;
    }
    public static boolean startLoadLibrary() {
        try {
            System.loadLibrary("avutil");
            System.loadLibrary("swresample");
            System.loadLibrary("swscale");
            System.loadLibrary("postproc");
            System.loadLibrary("avformat");
            System.loadLibrary("avcodec");
            System.loadLibrary("avfilter");
            System.loadLibrary("avdevice");
            System.loadLibrary("vr720");
        } catch (Throwable e) {
            Log.e("NativeVR720", "Load lib failed! ", e);
            libLoadFailed = true;
        }
        return isLibLoadSuccess();
    }
    public static native boolean initNative(AssetManager assetManager, Context context);
    public static native void releaseNative();
    public static native void lowMemory();
    public static native String getNativeVersion();
}
