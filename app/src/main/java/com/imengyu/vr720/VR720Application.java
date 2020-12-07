package com.imengyu.vr720;

import android.app.Application;
import com.imengyu.vr720.core.NativeVR720;
import com.hjq.toast.ToastUtils;

/**
 * 应用全局初始化操作
 */
public class VR720Application extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ToastUtils.init(this);
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
