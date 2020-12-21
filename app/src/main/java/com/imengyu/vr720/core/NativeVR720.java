package com.imengyu.vr720.core;

import android.content.Context;
import android.content.res.AssetManager;

public class NativeVR720 {

    static {
        System.loadLibrary("avutil");
        System.loadLibrary("swresample");
        System.loadLibrary("swscale");
        System.loadLibrary("postproc");
        System.loadLibrary("avformat");
        System.loadLibrary("avcodec");
        System.loadLibrary("avfilter");
        System.loadLibrary("avdevice");
        System.loadLibrary("vr720");
    }

    public static native boolean initNative(AssetManager assetManager, Context context);
    public static native void updateAssetManagerPtr(AssetManager assetManager);
    public static native void releaseNative();
    public static native void lowMemory();
    public static native String getNativeVersion();
}
