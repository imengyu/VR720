package com.dreamfish.com.vr720.core;

public class NativeVR720 {
    public static native boolean initNative();
    public static native void releaseNative();
    public static native String getNativeVersion();
}
