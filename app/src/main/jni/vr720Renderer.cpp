//
// Created by roger on 2020/11/1.
//
#include "stdafx.h"
#include "CMobileGameRenderer.h"
#include "CMobileGameUIEventDistributor.h"
#include "CMobileOpenGLView.h"
#include "CStringHlp.h"

CMobileOpenGLView* getJavaObjectOpenGLViewPtr(JNIEnv *env, jobject thiz) {
    //nativeGetNativePtr
    jclass clazz = env->FindClass("com/dreamfish/com/vr720/core/NativeVR720Renderer");
    jmethodID nativeGetNativePtr = env->GetMethodID(clazz, "nativeGetNativePtr", "()J");
    jlong viewPtr = env->CallLongMethod(thiz, nativeGetNativePtr);
    env->DeleteLocalRef(clazz);
    return (CMobileOpenGLView*)viewPtr;
}

extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_onCreate(JNIEnv *env, jobject thiz) {
    auto* gameRenderer = new CMobileGameRenderer();
    auto* gameUIEventDistributor = new CMobileGameUIEventDistributor(env, thiz);
    auto* newView = new CMobileOpenGLView(gameRenderer);
    auto newViewPtr = (jlong)newView;

    //nativeSetNativePtr
    jclass clazz = env->FindClass("com/dreamfish/com/vr720/core/NativeVR720Renderer");
    jmethodID nativeSetNativePtr = env->GetMethodID(clazz, "nativeSetNativePtr", "(J)V");
    env->CallVoidMethod(thiz, nativeSetNativePtr, newViewPtr);
    env->DeleteLocalRef(clazz);

    gameRenderer->SetUiEventDistributor(gameUIEventDistributor);

    LOGIF(_vstr("NativeVR720Renderer.onCreate: View ptr : 0x%xl"), newViewPtr);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_onSurfaceCreated(JNIEnv *env, jobject thiz, jlong native_ptr) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->Init();
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_onSurfaceChanged(JNIEnv *env, jobject thiz, jlong native_ptr, int width, jint height) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->Resize(width, height);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_onDrawFrame(JNIEnv *env, jobject thiz, jlong native_ptr) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->Render();
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_onMainThread(JNIEnv *env, jobject thiz, jlong native_ptr) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->Update();
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_onDestroy(JNIEnv *env, jobject thiz, jlong native_ptr) {
    LOGI(_vstr("NativeVR720Renderer.onDestroy"));

    auto* view = (CMobileOpenGLView*)native_ptr;
    view->Destroy();
    delete view;
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_processKey(JNIEnv *env, jobject thiz, jlong native_ptr, jint key, jboolean down) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->ProcessKeyEvent(key, down);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_processViewZoom(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat v) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->ProcessZoomEvent(v);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_processMouseUp(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->ProcessMouseEvent(ViewMouseMouseUp, x, y);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_processMouseDown(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->ProcessMouseEvent(ViewMouseMouseDown, x, y);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_processMouseMove(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->ProcessMouseEvent(ViewMouseMouseMove, x, y);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_openFile(JNIEnv *env, jobject thiz, jlong native_ptr, jstring path) {
    char* pathptr = CStringHlp::jstringToChar(env, path);
    auto* view = (CMobileOpenGLView*)native_ptr;
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetOpenFilePath(pathptr);
    gameRenderer->MarkShouldOpenFile();
    free(pathptr);
}
extern "C" JNIEXPORT jstring JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_getLastError(JNIEnv *env, jobject thiz, jlong native_ptr) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return CStringHlp::charTojstring(env, gameRenderer->GetImageOpenError());
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_closeFile(JNIEnv *env, jobject thiz, jlong native_ptr) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->MarkCloseFile();
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_setPanoramaMode(JNIEnv *env, jobject thiz, jlong native_ptr, jint mode) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SwitchMode((PanoramaMode)mode);
}
extern "C" JNIEXPORT jint JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_getPanoramaMode(JNIEnv *env, jobject thiz, jlong native_ptr) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return gameRenderer->GetMode();
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_updateGryoValue(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y, jfloat z) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->UpdateGryoValue(x,y,z);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_setVREnable(JNIEnv *env, jobject thiz, jlong native_ptr, jboolean enable) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetVREnabled(enable);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_setGryoEnable(JNIEnv *env, jobject thiz, jlong native_ptr, jboolean enable) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetGryoEnabled(enable);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_onUpdateFps(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat fps) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->SetCurrentFPS(fps);
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720Renderer_destroy(JNIEnv *env, jobject thiz, jlong native_ptr) {
    auto* view = (CMobileOpenGLView*)native_ptr;
    view->ManualDestroy();
}