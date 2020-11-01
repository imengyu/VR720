package com.dreamfish.com.vr720.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NativeVR720GLSurfaceView extends GLSurfaceView {

    public NativeVR720GLSurfaceView(Context context) {
        super(context);
        init(context);
    }
    public NativeVR720GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    private void init(Context context) {
        this.context = context;
    }

    private NativeVR720Renderer nativeVR720Renderer = null;
    private RendererWrapper renderer = null;
    private Context context;

    /**
     * 设置本地渲染器
     * @param nativeRenderer 本地渲染器
     */
    public void setNativeRenderer(NativeVR720Renderer nativeRenderer) {
        if(renderer == null) {
            nativeVR720Renderer = nativeRenderer;
            renderer = new RendererWrapper(this, nativeVR720Renderer);

            setRenderer(renderer);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }

    /**
     * 获取当前帧率
     * @return 当前帧率
     */
    public double getFps() { return framesPerSecond; }

    /**
     * 设置目标帧率
     * @param fps 目标帧率
     */
    public void setFps(float fps) {
        this.frameExecuteTime = (int)(1000.0f / fps);
    }

    private int frameExecuteTime = 1000 / 30;
    private ScheduledExecutorService pool = null;

    //帧率计算
    private double framesPerSecond;
    private double lastTime;

    private void calculateFrameRate() {
        double currentTime = System.currentTimeMillis();
        ++framesPerSecond;
        if (currentTime - lastTime > 1000) {
            lastTime = currentTime;
            framesPerSecond = 0;
        }
    }

    /**
     * 启动定时渲染
     */
    public void startRenderer() {
        if(pool == null) {
            pool = Executors.newScheduledThreadPool(1);
            pool.scheduleAtFixedRate(task, 0, frameExecuteTime, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 停止定时渲染
     */
    public void stopRenderer() {
        if(pool != null) {
            ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
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

    /**
     * 更新任务
     */
    private final TimerTask task = new TimerTask() {
        @Override
        public void run() {
            requestRender();
        }
    };

    /**
     * Base Game renderer
     */
    private static class RendererWrapper implements android.opengl.GLSurfaceView.Renderer {

        public RendererWrapper(NativeVR720GLSurfaceView surfaceView, NativeVR720Renderer nativeRenderer) {
            nativeVR720Renderer = nativeRenderer;
            nativeVR720GLSurfaceView = surfaceView;
        }

        private final NativeVR720GLSurfaceView nativeVR720GLSurfaceView;
        private final NativeVR720Renderer nativeVR720Renderer;

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            nativeVR720Renderer.onSurfaceCreated();
        }
        @Override
        public void onSurfaceChanged(GL10 gl10, int w, int h) {
            nativeVR720Renderer.onSurfaceChanged(w, h);
        }
        @Override
        public void onDrawFrame(GL10 gl10) {
            nativeVR720Renderer.onDrawFrame();
            nativeVR720GLSurfaceView.calculateFrameRate();
        }
    }

    //事件处理
    //***********************

    @Override
    public void onPause() {
        super.onPause();
        stopRenderer();
    }
    @Override
    public void onResume() {
        super.onResume();
        startRenderer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(nativeVR720Renderer != null)
            nativeVR720Renderer.onDestroy();
    }

    //输入事件处理
    //***********************


    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private double nLenStart = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int pCount = event.getPointerCount();// 触摸设备时手指的数量
        int action = event.getAction();// 获取触屏动作。比如：按下、移动和抬起等手势动作 // 手势按下且屏幕上是两个手指数量时
        if ((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN
                && pCount == 2) { // 获取按下时候两个坐标的x轴的水平距离，取绝对值

            int xLen = Math.abs((int) event.getX(0) - (int) event.getX(1)); // 获取按下时候两个坐标的y轴的水平距离，取绝对值
            int yLen = Math.abs((int) event.getY(0) - (int) event.getY(1)); // 根据x轴和y轴的水平距离，求平方和后再开方获取两个点之间的直线距离。此时就获取到了两个手指刚按下时的直线距离
            nLenStart = Math.sqrt((double) xLen * xLen + (double) yLen * yLen);

        } else if ((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP
                && pCount == 2) {// 手势抬起且屏幕上是两个手指数量时 // 获取抬起时候两个坐标的x轴的水平距离，取绝对值

            int xLen = Math.abs((int) event.getX(0) - (int) event.getX(1)); // 获取抬起时候两个坐标的y轴的水平距离，取绝对值
            int yLen = Math.abs((int) event.getY(0) - (int) event.getY(1)); // 根据x轴和y轴的水平距离，求平方和后再开方获取两个点之间的直线距离。此时就获取到了两个手指抬起时的直线距离
            double nLenEnd = Math.sqrt((double) xLen * xLen + (double) yLen * yLen); // 根据手势按下时两个手指触点之间的直线距离A和手势抬起时两个手指触点之间的直线距离B。比较A和B的大小，得出用户是手势放大还是手势缩小

            float var = (float)(nLenEnd / nLenStart);
            nativeVR720Renderer.processViewZoom(var);
        } else {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    nativeVR720Renderer.processMouseUp(event.getX(), event.getY());
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    nativeVR720Renderer.processMouseUp(event.getX(), event.getY());
                    performClick();
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    nativeVR720Renderer.processMouseMove(event.getX(), event.getY());
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        nativeVR720Renderer.processKey(keyCode, true);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        nativeVR720Renderer.processKey(keyCode, false);
        return super.onKeyUp(keyCode, event);
    }
}
