package com.imengyu.vr720.core.natives;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.Keep;

import com.imengyu.vr720.annotation.UnUsed;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.core.representation.Quaternion;

/**
 * 本地渲染器
 */
public class NativeVR720Renderer {

    /**
     * 创建本地渲染器
     */
    public NativeVR720Renderer(Handler uiHandler) {
        mainHandler = uiHandler;
        onCreate();
    }

    private final Handler mainHandler;
    private long mainNativePtr = 0;

    @Keep
    public void nativeFeedBackMessage(int msg) {
        Message msgObj = new Message();
        msgObj.what = MainMessages.MSG_NATIVE_MESSAGE;
        msgObj.obj = msg;
        mainHandler.sendMessage(msgObj);
    }
    @Keep
    public void nativeSetNativePtr(long ptr) { mainNativePtr = ptr; }
    @Keep
    @UnUsed
    public long nativeGetNativePtr() { return mainNativePtr; }

    public interface OnRequestGyroValueCallback {
        void requestGyroValue(Quaternion quaternion);
    }

    private OnRequestGyroValueCallback onRequestGyroValueCallback;

    /**
     * 设置陀螺仪数据请求回调
     * @param onRequestGyroValueCallback 回调
     */
    public void setOnRequestGyroValueCallback(OnRequestGyroValueCallback onRequestGyroValueCallback) {
        this.onRequestGyroValueCallback = onRequestGyroValueCallback;
    }
    public void requestGyroValue(Quaternion quaternion) {
        if(onRequestGyroValueCallback != null)
            onRequestGyroValueCallback.requestGyroValue(quaternion);
    }

    //消息回传
    public static final int MobileGameUIEvent_MarkLoadingStart = 0;
    public static final int MobileGameUIEvent_MarkLoadingEnd = 1;
    public static final int MobileGameUIEvent_MarkLoadFailed = 2;
    public static final int MobileGameUIEvent_UiInfoChanged = 3;
    public static final int MobileGameUIEvent_FileClosed = 4;
    public static final int MobileGameUIEvent_DestroyComplete = 5;
    public static final int MobileGameUIEvent_VideoStateChanged = 6;

    //全景模式
    public static final int PanoramaMode_PanoramaSphere = 0;
    public static final int PanoramaMode_PanoramaCylinder = 1;
    public static final int PanoramaMode_PanoramaAsteroid = 2;
    public static final int PanoramaMode_PanoramaOuterBall = 3;
    public static final int PanoramaMode_PanoramaMercator = 4;
    public static final int PanoramaMode_PanoramaFull360 = 5;
    public static final int PanoramaMode_PanoramaFullOrginal = 6;
    public static final int PanoramaMode_PanoramaModeMax = 7;

    //视频状态
    @UnUsed
    public static final int VideoState_Loading = 1;
    @UnUsed
    public static final int VideoState_Failed = 2;
    public static final int VideoState_NotOpen = 3;
    public static final int VideoState_Playing = 4;
    public static final int VideoState_Ended = 5;
    @UnUsed
    public static final int VideoState_Opened = 6;
    public static final int VideoState_Paused = 6;

    //属性
    public static final int PROP_IS_FILE_OPEN = 2;
    @UnUsed
    public static final int PROP_IS_CURRENT_FILE_OPEN = 3;
    public static final int PROP_CURRENT_FILE_IS_VIDEO = 4;

    public static final int PROP_VR_ENABLED = 12;
    public static final int PROP_GYRO_ENABLED = 13;
    public static final int PROP_FULL_CHUNK_LOAD_ENABLED = 14;
    public static final int PROP_VIEW_CACHE_ENABLED = 15;
    public static final int PROP_CACHE_PATH = 16;
    public static final int PROP_LAST_ERROR = 17;
    public static final int PROP_VIDEO_VOLUME = 20;
    public static final int PROP_PANORAMA_MODE = 21;
    public static final int PROP_SMALL_PANORAMA_PATH = 22;
    public static final int PROP_LOG_LEVEL = 23;
    public static final int PROP_ENABLE_LOG = 24;
    public static final int PROP_ENABLE_NATIVE_DECODER = 25;
    public static final int PROP_GYRO_ROTATE_CORRECTION = 26;

    //C++代码声明
    //***********************************

    @Keep
    private native void onCreate();

    public void onSurfaceCreated() { onSurfaceCreated(mainNativePtr); }
    public void onSurfaceChanged(int width, int height) { onSurfaceChanged(mainNativePtr, width, height); }
    public void onDrawFrame() { onDrawFrame(mainNativePtr); }
    public void onMainThread() { onMainThread(mainNativePtr); }
    public void onDestroy() { onDestroy(mainNativePtr); mainNativePtr = 0; }
    public void onUpdateFps(float fps) { onUpdateFps(mainNativePtr, fps); }
    public void destroy() { destroy(mainNativePtr); }

    @Keep
    private native void destroy(long nativePtr);
    @Keep
    private native void onUpdateFps(long nativePtr, float fps);
    @Keep
    private native void onSurfaceCreated(long nativePtr);
    @Keep
    private native void onSurfaceChanged(long nativePtr, int width, int height);
    @Keep
    private native void onDrawFrame(long nativePtr);
    @Keep
    private native void onMainThread(long nativePtr);
    @Keep
    private native void onDestroy(long nativePtr);
    @Keep
    private native void openFile(long nativePtr, String path);
    @Keep
    private native void closeFile(long nativePtr);
    @Keep
    private native void processMouseMove(long nativePtr, float x, float y);
    @Keep
    private native void processMouseDown(long nativePtr, float x, float y);
    @Keep
    private native void processMouseUp(long nativePtr, float x, float y);
    @Keep
    private native void processMouseDragVelocity(long nativePtr, float x, float y);
    @Keep
    private native void processViewZoom(long nativePtr, float v);
    @Keep
    private native void processKey(long nativePtr, int key, boolean down);
    @Keep
    private native void updateGyroValue(long nativePtr, float x, float y, float z, float w);
    @Keep
    private native void updateDebugValue(long nativePtr, float x, float y, float z, float w, float v, float u);
    @Keep
    private native void onResume(long nativePtr);
    @Keep
    private native void onPause(long nativePtr);
    @Keep
    private native int getVideoState(long nativePtr);
    @Keep
    private native void updateVideoState(long nativePtr, int newState);
    @Keep
    private native int getVideoLength(long nativePtr);
    @Keep
    private native int getVideoPos(long nativePtr);
    @Keep
    private native void setVideoPos(long nativePtr, int pos);
    @Keep
    private native String getProp(long nativePtr, int id);
    @Keep
    private native void setProp(long nativePtr, int id, String value);
    @Keep
    private native int getIntProp(long nativePtr, int id);
    @Keep
    private native void setIntProp(long nativePtr, int id, int value);
    @Keep
    private native boolean getBoolProp(long nativePtr, int id);
    @Keep
    private native void setBoolProp(long nativePtr, int id, boolean value);

    public void setProp(int id, String value) { setProp(mainNativePtr, id, value); }
    @UnUsed
    public String getProp(int id) { return getProp(mainNativePtr, id); }
    public void setProp(int id, int value) { setIntProp(mainNativePtr, id, value); }
    public int getIntProp(int id) { return getIntProp(mainNativePtr, id); }
    public void setProp(int id, boolean value) { setBoolProp(mainNativePtr, id, value); }
    public boolean getBoolProp(int id) { return getBoolProp(mainNativePtr, id); }

    /**
     * 获取播放器音量
     * @return 播放器音量（0-100）
     */
    @UnUsed
    public int getVolume() { return getIntProp(PROP_VIDEO_VOLUME); }

    /**
     * 设置播放器音量
     * @param vol 播放器音量（0-100）
     */
    @UnUsed
    public void setVolume(int vol) { setIntProp(mainNativePtr, PROP_VIDEO_VOLUME, vol); }

    /**
     * 获取内核当前是否打开文件
     * @return 前是否打开文件
     */
    public boolean isFileOpen() { return getBoolProp(PROP_IS_FILE_OPEN); }
    /**
     * 打开全景图片文件
     * @param path 文件路径
     */
    public void openFile(String path) { openFile(mainNativePtr, path); }
    /**
     * 获取打开文件的错误信息
     * @return 错误信息
     */
    public String getLastError(Context context) {
        return NativeVR720ErrorConverter.getStringError(context, getIntProp(PROP_LAST_ERROR));
    }

    /**
     * 关闭当前文件
     */
    public void closeFile() { closeFile(mainNativePtr); }

    /**
     * 设置是否启用缓存
     * @param enableCache 是否启用缓存
     */
    public void setEnableCache(boolean enableCache) { setProp(PROP_VIEW_CACHE_ENABLED, enableCache); }

    /**
     * 设置缓存目录
     * @param path 缓存目录路径
     */
    public void setCachePath(String path) { setProp(PROP_CACHE_PATH, path); }

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
     * 进行更新拖动速度
     * @param x x轴速度
     * @param y y轴速度
     */
    public void processMouseDragVelocity(float x, float y) { processMouseDragVelocity(mainNativePtr, x, y); }

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
    @UnUsed
    public int getPanoramaMode() { return getIntProp(PROP_PANORAMA_MODE); }
    /**
     * 设置全景模式
     * @param mode 全景模式（PANO_MODE_*）
     */
    public void setPanoramaMode(int mode) { setIntProp(mainNativePtr, PROP_PANORAMA_MODE, mode); }

    private boolean gyroEnable = false;

    /**
     * 设置是否启用陀螺仪
     * @param enable 是否启用
     */
    public void setGyroEnable(boolean enable) { gyroEnable = enable; setProp(PROP_GYRO_ENABLED, enable); }
    /**
     * 获取是否启用陀螺仪
     * @return 是否启用
     */
    public boolean getGyroEnable() { return gyroEnable; }

    /**
     * 强制更新陀螺仪参数
     */
    public void updateGyroValue(float x, float y, float z, float w) { updateGyroValue(mainNativePtr, x,y,z,w); }

    public void updateDebugValue(float x, float y, float z, float w, float v, float u) {
        updateDebugValue(mainNativePtr, x,y,z,w,v,u);
    }

    /**
     * 设置是否启用VR双屏模式
     * @param enable 是否启用
     */
    public void setVREnable(boolean enable) { setProp(PROP_VR_ENABLED, enable); }

    /**
     * 设置是否启用完整细节加载模式
     * @param enable 是否启用
     */
    public void setEnableFullChunks(boolean enable) { setProp(PROP_FULL_CHUNK_LOAD_ENABLED, enable); }

    /**
     * 获取视频播放器状态
     * @return 视频播放器状态
     */
    public int getVideoState() { return getVideoState(mainNativePtr); }

    /**
     * 设置视频播放器状态
     * @param newState 新的视频播放器状态
     */
    public void updateVideoState(int newState) { updateVideoState(mainNativePtr, newState); }

    /**
     * 获取当前打开的文件是不是视频
     * @return 是不是视频
     */
    public boolean getCurrentFileIsVideo() { return getBoolProp(PROP_CURRENT_FILE_IS_VIDEO); }

    /**
     * 获取当前视频时长
     * @return 视频时长（毫秒）
     */
    public int getVideoLength() {
        return getVideoLength(mainNativePtr);
    }

    /**
     * 获取当前视频播放位置
     * @return 当前视频播放位置（毫秒）
     */
    public int getVideoPos() {
        return getVideoPos(mainNativePtr);
    }

    /**
     * 设置当前视频播放位置
     * @param pos 视频播放位置（毫秒）
     */
    public void setVideoPos(int pos) { setVideoPos(mainNativePtr, pos); }

    /**
     * 恢复事件
     */
    public void onResume() { onResume(mainNativePtr); }

    /**
     * 暂停事件
     */
    public void onPause() { onPause(mainNativePtr); }
}
