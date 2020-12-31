package com.imengyu.vr720.core.utils;

import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FPSController {

    private static final String TAG = "FPSController";

    private final GLSurfaceView surfaceView;

    public FPSController(GLSurfaceView surfaceView, int fps) {
        this.surfaceView = surfaceView;
        this.frameExecuteTime = 1000.0 / fps;
    }

    //帧率计算
    private double framesPerSecond;
    private double lastTime;
    private double lastFrameTime;
    private double frameExecuteTime;
    private ScheduledExecutorService pool = null;

    /**
     * 更新任务
     */
    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            surfaceView.requestRender();
        }
    };

    /**
     * 计算当前帧率
     */
    public void calculateFrameRate() {
        double currentTime = System.currentTimeMillis();
        lastFrameTime = currentTime - lastTime;
        lastTime = currentTime;
        framesPerSecond = 1000.0 / lastFrameTime;
    }

    /**
     * 获取当前帧率
     * @return 当前帧率
     */
    public double getFramesPerSecond() {
        return framesPerSecond;
    }

    public void setFramesPerSecond(int fps) {
        if(fps < 20) fps = 20;
        else if(fps > 60) fps = 60;
        framesPerSecond = fps;
        frameExecuteTime = 1000.0 / fps;
        stopRenderer();
        startRenderer();
    }

    /**
     * 启动定时渲染
     */
    public void startRenderer() {
        if(pool == null) {
            Log.i(TAG, "startRenderer");
            pool = Executors.newScheduledThreadPool(1);
            pool.scheduleAtFixedRate(task, 0, (long)Math.ceil(frameExecuteTime), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 停止定时渲染
     */
    public void stopRenderer() {
        if(pool != null) {
            Log.i(TAG, "stopRenderer");
            pool.shutdown();
            try {
                if(!pool.awaitTermination(1000, TimeUnit.MILLISECONDS))
                    pool.shutdownNow();
                pool = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
                pool.shutdownNow();
                pool = null;
            }
        }
    }

}
