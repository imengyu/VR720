package com.imengyu.vr720;

import android.app.Application;
import android.util.Log;

import com.imengyu.vr720.core.NativeVR720;
import com.hjq.toast.ToastUtils;

/**
 * 应用全局初始化操作
 */
public class VR720Application extends Application {

    public static final String TAG = VR720Application.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        ToastUtils.init(this);
        //初始化内核
        NativeVR720.initNative(getAssets());
    }

    @Override
    public void onTerminate() {
        Log.i(TAG, "onTerminate");
        NativeVR720.releaseNative();
        super.onTerminate();
    }

    public void onQuit() {
        Log.i(TAG, "onQuit");
        NativeVR720.releaseNative();
    }

    @Override
    public void onLowMemory() {
        Log.i(TAG, "onLowMemory");
        NativeVR720.lowMemory();
        super.onLowMemory();
    }
}
