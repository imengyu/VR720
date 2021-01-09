package com.imengyu.vr720;

import android.app.Application;
import android.util.Log;

import com.hjq.toast.ToastUtils;
import com.imengyu.vr720.core.natives.NativeVR720;
import com.imengyu.vr720.service.CacheServices;
import com.imengyu.vr720.service.ErrorUploadService;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.service.ListImageCacheService;
import com.imengyu.vr720.service.UpdateService;
import com.imengyu.vr720.utils.CrashHandler;

/**
 * 应用全局初始化操作
 */
public class VR720Application extends Application {

    public static final String TAG = VR720Application.class.getSimpleName();

    //全局服务使用
    //===================================

    private ListDataService listDataService = null;
    private ListImageCacheService listImageCacheService = null;
    private CacheServices cacheServices = null;
    private UpdateService updateService = null;
    private ErrorUploadService errorUploadService = null;

    public ListDataService getListDataService() {
        return listDataService;
    }
    public ListImageCacheService getListImageCacheService() {
        return listImageCacheService;
    }
    public CacheServices getCacheServices() {
        return cacheServices;
    }
    public UpdateService getUpdateService() {
        if(updateService == null)
            updateService = new UpdateService();
        return updateService;
    }
    public ErrorUploadService getErrorUploadService() {
        if(errorUploadService == null)
            errorUploadService = new ErrorUploadService();
        return errorUploadService;
    }

    //全局初始化方法
    //===================================

    private boolean initFinish = false;

    public boolean isNotInit() { return !initFinish; }
    public void setInitFinish() { initFinish = true; }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        //设置错误回调
        CrashHandler.getInstance().init(getApplicationContext(), false);

        //初始化数据服务
        listDataService = new ListDataService(getApplicationContext());
        listImageCacheService = new ListImageCacheService(getApplicationContext());
        cacheServices = new CacheServices(getApplicationContext());

        //ToastUtils
        ToastUtils.init(this);

        //初始化内核
        if(NativeVR720.startLoadLibrary())
            NativeVR720.initNative(getAssets(), getApplicationContext());
    }

    @Override
    public void onTerminate() {
        Log.i(TAG, "onTerminate");
        if(listImageCacheService != null) {
            listImageCacheService.releaseImageCache();
            listImageCacheService = null;
        }
        if(NativeVR720.isLibLoadSuccess())
            NativeVR720.releaseNative();
        initFinish = false;
        super.onTerminate();
    }

    public void onQuit() {
        Log.i(TAG, "onQuit");
        if(NativeVR720.isLibLoadSuccess())
            NativeVR720.releaseNative();
        initFinish = false;
    }

    @Override
    public void onLowMemory() {
        Log.i(TAG, "onLowMemory");
        if(listImageCacheService != null)
            listImageCacheService.releaseAllMemoryCache();
        if(NativeVR720.isLibLoadSuccess())
            NativeVR720.lowMemory();
        super.onLowMemory();
    }
}
