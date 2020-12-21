package com.imengyu.vr720.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLException;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;

import com.imengyu.vr720.core.representation.Quaternion;

import java.nio.IntBuffer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class NativeVR720GLSurfaceView extends GLSurfaceView {

    private static final String TAG = "VR720GLSurfaceView";

    public NativeVR720GLSurfaceView(Context context) {
        super(context);
    }
    public NativeVR720GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private NativeVR720Renderer nativeVR720Renderer = null;
    private RendererWrapper renderer = null;
    private boolean dragVelocityEnabled = true;

    /**
     * 设置本地渲染器
     * @param nativeRenderer 本地渲染器
     */
    public void setNativeRenderer(NativeVR720Renderer nativeRenderer) {
        if(renderer == null) {
            nativeVR720Renderer = nativeRenderer;
            renderer = new RendererWrapper(this, nativeVR720Renderer);

            setEGLContextFactory(new ContextFactory());
            setRenderer(renderer);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }

    /**
     * 设置是否开启惯性拖动
     * @param dragVelocityEnabled 是否开启
     */
    public void setDragVelocityEnabled(boolean dragVelocityEnabled) {
        this.dragVelocityEnabled = dragVelocityEnabled;
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

    /**
     * 开始截图
     */
    public void startCapture() {
        isTakePic = true;
    }

    /**
     * 设置截图完成回调
     * @param callback 回调
     */
    public void setCaptureCallback(OnCaptureCallback callback) {
        captureCallback = callback;
    }
    /**
     * 设置渲染回调
     * @param rendererCallback 回调
     */
    public void setRendererCallback(OnRendererCallback rendererCallback) {
        this.rendererCallback = rendererCallback;
    }

    public interface OnCaptureCallback {
        void onCaptureCallback(Bitmap bitmap);
    }
    public interface OnRendererCallback {
        void onRender(GL10 gl10);
    }

    /**
     * 截图
     */
    private Bitmap createBitmapFromGLSurface(int w, int h, GL10 gl) {
        int[] bitmapBuffer = new int[w * h];
        int[] bitmapSource = new int[w * h];
        IntBuffer intBuffer = IntBuffer.wrap(bitmapBuffer);
        intBuffer.position(0);
        try {
            gl.glReadPixels(0, 0, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE,
                    intBuffer);
            int offset1, offset2;
            for (int i = 0; i < h; i++) {
                offset1 = i * w;
                offset2 = (h - i - 1) * w;
                for (int j = 0; j < w; j++) {
                    int texturePixel = bitmapBuffer[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    bitmapSource[offset2 + j] = pixel;
                }
            }
        } catch (GLException e) {
            return null;
        }
        return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888);
    }
    private void captureFinish(Bitmap b) {
        if(captureCallback != null) {
            new Thread(() -> captureCallback.onCaptureCallback(b)).start();
        }
    }

    //变量
    //********************************

    private int frameExecuteTime = 1000 / 20;
    private ScheduledExecutorService pool = null;
    private boolean isTakePic = false;
    private OnCaptureCallback captureCallback = null;
    private OnRendererCallback rendererCallback = null;

    //context
    //********************************

    private static class ContextFactory implements GLSurfaceView.EGLContextFactory {

        public EGLContext createContext(
                EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {

            double glVersion = 3.0;
            Log.i(TAG, "Creating OpenGL ES " + glVersion + " context");
            int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, (int) glVersion,
                    EGL10.EGL_NONE };
            // attempt to create a OpenGL ES 3.0 context
            return egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list); // returns null if 3.0 is not supported;
        }

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay eglDisplay, EGLContext eglContext) {
            egl.eglDestroyContext(eglDisplay, eglContext);
        }
    }

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
            Log.i(TAG, "startRenderer");
            pool = Executors.newScheduledThreadPool(1);
            pool.scheduleAtFixedRate(task, 0, frameExecuteTime, TimeUnit.MILLISECONDS);
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

    public void forceStop() {
        stopRenderer();
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

        private int width = 0;
        private int height = 0;
        private final Quaternion gyroQuaternion = new Quaternion();

        @Override
        public void onSurfaceCreated(GL10 gl10, javax.microedition.khronos.egl.EGLConfig eglConfig) {
            nativeVR720Renderer.onSurfaceCreated();
        }
        @Override
        public void onSurfaceChanged(GL10 gl10, int w, int h) {
            width = w;
            height = h;
            nativeVR720Renderer.onSurfaceChanged(w, h);
        }
        @Override
        public void onDrawFrame(GL10 gl10) {

            //更新陀螺仪数据
            if(nativeVR720Renderer.getGyroEnable()) {
                nativeVR720Renderer.requestGyroValue(gyroQuaternion);
                nativeVR720Renderer.updateGyroValue(gyroQuaternion.x(),
                        gyroQuaternion.y(), gyroQuaternion.z(), gyroQuaternion.w());
            }

            //绘制
            nativeVR720Renderer.onDrawFrame();

            //刷新FPS
            nativeVR720GLSurfaceView.calculateFrameRate();
            nativeVR720Renderer.onUpdateFps((float) nativeVR720GLSurfaceView.framesPerSecond);
            //System.out.println("onDrawFrame: " + gl10.glGetError());

            //渲染回调
            if(nativeVR720GLSurfaceView.rendererCallback != null)
                nativeVR720GLSurfaceView.rendererCallback.onRender(gl10);

            //截屏控制
            if (nativeVR720GLSurfaceView.isTakePic) {
                nativeVR720GLSurfaceView.isTakePic = false;
                Bitmap bmp = nativeVR720GLSurfaceView.createBitmapFromGLSurface(width, height, gl10);
                nativeVR720GLSurfaceView.captureFinish(bmp);
            }
        }
    }

    //事件处理
    //***********************

    @Override
    public void onPause() {
        nativeVR720Renderer.onPause();
        stopRenderer();

        super.onPause();
    }
    @Override
    public void onResume() {
        nativeVR720Renderer.onResume();
        startRenderer();
        super.onResume();
    }

    /*
    @Override
    protected void onDetachedFromWindow() {
        if(nativeVR720Renderer != null)
            nativeVR720Renderer.onDestroy();
        super.onDetachedFromWindow();
    }
    */

    public void onDestroyComplete() {
        nativeVR720Renderer.nativeSetNativePtr(0);
    }

    //输入事件处理
    //***********************


    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private double nLenStart = 0;
    private boolean lastIsZoom = false;
    private double lastIncrement = 0;
    private VelocityTracker mVelocityTracker;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Boolean result = null;

        //获取速度
        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        boolean eventAddedToVelocityTracker = false;
        final MotionEvent vtev = MotionEvent.obtain(event);

        int pCount = event.getPointerCount();// 触摸设备时手指的数量
        int action = event.getAction();// 获取触屏动作。比如：按下、移动和抬起等手势动作 // 手势按下且屏幕上是两个手指数量时
        if ((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN && pCount == 2) { // 获取按下时候两个坐标的x轴的水平距离，取绝对值
            int xLen = Math.abs((int) event.getX(0) - (int) event.getX(1)); // 获取按下时候两个坐标的y轴的水平距离，取绝对值
            int yLen = Math.abs((int) event.getY(0) - (int) event.getY(1)); // 根据x轴和y轴的水平距离，求平方和后再开方获取两个点之间的直线距离。此时就获取到了两个手指刚按下时的直线距离
            nLenStart = Math.sqrt((double) xLen * xLen + (double) yLen * yLen);
            lastIncrement = 0;
            lastIsZoom = true;
            nativeVR720Renderer.processMouseDragVelocity(0, 0);
        }
        else {
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    nativeVR720Renderer.processMouseDragVelocity(0, 0);
                    if(pCount == 1) {
                        nativeVR720Renderer.processMouseDown(event.getX(), event.getY());
                        lastIsZoom = false;
                    }
                    result = true;
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    nativeVR720Renderer.processMouseDragVelocity(0, 0);
                    if(!lastIsZoom && pCount == 1) {
                        eventAddedToVelocityTracker = true;

                        mVelocityTracker.addMovement(vtev);
                        mVelocityTracker.computeCurrentVelocity(30, (float)200);
                        float yVelocity = -mVelocityTracker.getYVelocity();
                        float xVelocity = -mVelocityTracker.getXVelocity();
                        if(dragVelocityEnabled)
                            nativeVR720Renderer.processMouseDragVelocity(xVelocity, yVelocity);

                        nativeVR720Renderer.processMouseUp(event.getX(), event.getY());
                        performClick();
                    }
                    result = true;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    nativeVR720Renderer.processMouseDragVelocity(0, 0);
                    if(lastIsZoom && pCount == 2) {

                        int xLen = Math.abs((int) event.getX(0) - (int) event.getX(1)); // 获取抬起时候两个坐标的y轴的水平距离，取绝对值
                        int yLen = Math.abs((int) event.getY(0) - (int) event.getY(1)); // 根据x轴和y轴的水平距离，求平方和后再开方获取两个点之间的直线距离。此时就获取到了两个手指抬起时的直线距离
                        double nLenEnd = Math.sqrt((double) xLen * xLen + (double) yLen * yLen); // 根据手势按下时两个手指触点之间的直线距离A和手势抬起时两个手指触点之间的直线距离B。比较A和B的大小，得出用户是手势放大还是手势缩小

                        double nowInc = nLenEnd - nLenStart;
                        double incNew = nowInc - lastIncrement;
                        lastIncrement = nowInc;

                        nativeVR720Renderer.processViewZoom((float) incNew);
                    } else if(!lastIsZoom)
                        nativeVR720Renderer.processMouseMove(event.getX(), event.getY());
                    result = true;
                    break;
                }
            }
        }

        if (!eventAddedToVelocityTracker)
            mVelocityTracker.addMovement(vtev);
        vtev.recycle();

        if(result == null)
            return super.onTouchEvent(event);
        return result;
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
