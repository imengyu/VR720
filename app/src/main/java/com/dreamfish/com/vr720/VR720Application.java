package com.dreamfish.com.vr720;

import android.app.Application;
import com.dreamfish.com.vr720.core.NativeVR720;

/**
 * 应用全局初始化操作
 */
public class VR720Application extends Application {

    static {
        System.loadLibrary("vr720");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化内核
        NativeVR720.initNative(getAssets());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        NativeVR720.releaseNative();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        NativeVR720.lowMemory();
    }
}
