package com.dreamfish.com.vr720.core;

import android.content.res.AssetManager;

public class NativeVR720 {

    static {
        System.loadLibrary("vr720");
    }

    public static native boolean initNative(AssetManager assetManager);
    public static native void updateAssetManagerPtr(AssetManager assetManager);
    public static native void releaseNative();
    public static native void lowMemory();
    public static native String getNativeVersion();
}
