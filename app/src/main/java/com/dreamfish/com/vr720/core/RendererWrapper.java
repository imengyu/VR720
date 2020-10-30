package com.dreamfish.com.vr720.core;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Base Game renderer
 */
public class RendererWrapper implements android.opengl.GLSurfaceView.Renderer {

    public RendererWrapper(NativeVR720Renderer nativeRenderer) {
        nativeVR720Renderer = nativeRenderer;
    }

    /**
     * 获取内核渲染器
     * @return 内核渲染器
     */
    public NativeVR720Renderer getNativeRenderer() {
        return nativeVR720Renderer;
    }

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
    }
}
