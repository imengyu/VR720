//
// Created by roger on 2020/11/1.
//
#include "stdafx.h"
#include "CMobileGameRenderer.h"
#include "CMobileGameUIEventDistributor.h"
#include "CMobileOpenGLView.h"
#include "CStringHlp.h"

#define GET_VIEW(ptr) auto* view = (CMobileOpenGLView*)native_ptr;\
                      if(!view) return;
#define GET_VIEW_OR_RET(ptr,ret) auto* view = (CMobileOpenGLView*)native_ptr;\
                      if(!view) return ret;

CMobileOpenGLView* getJavaObjectOpenGLViewPtr(JNIEnv *env, jobject thiz) {
    //nativeGetNativePtr
    jclass clazz = env->FindClass("com/imengyu/vr720/core/NativeVR720Renderer");
    jmethodID nativeGetNativePtr = env->GetMethodID(clazz, "nativeGetNativePtr", "()J");
    jlong viewPtr = env->CallLongMethod(thiz, nativeGetNativePtr);
    env->DeleteLocalRef(clazz);
    return (CMobileOpenGLView*)viewPtr;
}

extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_onCreate(JNIEnv *env, jobject thiz) {
    auto* gameRenderer = new CMobileGameRenderer();
    auto* gameUIEventDistributor = new CMobileGameUIEventDistributor(env, thiz);
    auto* newView = new CMobileOpenGLView(gameRenderer);
    auto newViewPtr = (jlong)newView;

    //nativeSetNativePtr
    jclass clazz = env->FindClass("com/imengyu/vr720/core/NativeVR720Renderer");
    jmethodID nativeSetNativePtr = env->GetMethodID(clazz, "nativeSetNativePtr", "(J)V");
    env->CallVoidMethod(thiz, nativeSetNativePtr, newViewPtr);
    env->DeleteLocalRef(clazz);

    gameRenderer->SetUiEventDistributor(gameUIEventDistributor);

    LOGIF("NativeVR720Renderer.onCreate: View ptr : 0x%xl", newViewPtr);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_onDestroy(JNIEnv *env, jobject thiz, jlong native_ptr) {

    GET_VIEW(native_ptr);
    LOGI("NativeVR720Renderer.onDestroy");
    view->ManualDestroy();
    //Destroy
    delete view;
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_onSurfaceCreated(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW(native_ptr);
    view->Init();
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_onSurfaceChanged(JNIEnv *env, jobject thiz, jlong native_ptr, int width, jint height) {
    GET_VIEW(native_ptr);
    view->Resize(width, height);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_onDrawFrame(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW(native_ptr);
    view->Render();
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_onMainThread(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW(native_ptr);
    view->Update();
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_processKey(JNIEnv *env, jobject thiz, jlong native_ptr, jint key, jboolean down) {
    GET_VIEW(native_ptr);
    view->ProcessKeyEvent(key, down);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_processViewZoom(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat v) {
    GET_VIEW(native_ptr);
    view->ProcessZoomEvent(v);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_processMouseUp(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    GET_VIEW(native_ptr);
    view->ProcessMouseEvent(ViewMouseMouseUp, x, y);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_processMouseDown(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    GET_VIEW(native_ptr);
    view->ProcessMouseEvent(ViewMouseMouseDown, x, y);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_processMouseMove(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    GET_VIEW(native_ptr);
    view->ProcessMouseEvent(ViewMouseMouseMove, x, y);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_openFile(JNIEnv *env, jobject thiz, jlong native_ptr, jstring path) {
    GET_VIEW(native_ptr);
    char* pathptr = CStringHlp::jstringToChar(env, path);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetOpenFilePath(pathptr);
    gameRenderer->MarkShouldOpenFile();
    free(pathptr);
}
extern "C" JNIEXPORT jstring JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_getLastError(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW_OR_RET(native_ptr, nullptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return CStringHlp::charTojstring(env, gameRenderer->GetImageOpenError());
}
extern "C" JNIEXPORT jstring JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_getDebugText(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW_OR_RET(native_ptr, nullptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return nullptr;
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_closeFile(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->MarkCloseFile();
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_setPanoramaMode(JNIEnv *env, jobject thiz, jlong native_ptr, jint mode) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SwitchMode((PanoramaMode)mode);
}
extern "C" JNIEXPORT jint JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_getPanoramaMode(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW_OR_RET(native_ptr, 0);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return gameRenderer->GetMode();
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_updateGyroValue(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y, jfloat z, jfloat w) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->UpdateGyroValue(x,y,z,w);
}
extern "C" JNIEXPORT jboolean JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_isFileOpen(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW_OR_RET(native_ptr, false);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return  gameRenderer->IsFileOpen();
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_updateDebugValue(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, float y, jfloat z, jfloat w, jfloat v, jfloat u) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->UpdateDebugValue(x,y,z,w,v,u);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_setVREnable(JNIEnv *env, jobject thiz, jlong native_ptr, jboolean enable) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetVREnabled(enable);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_setGyroEnable(JNIEnv *env, jobject thiz, jlong native_ptr, jboolean enable) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetGyroEnabled(enable);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_onUpdateFps(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat fps) {
    GET_VIEW(native_ptr);
    view->SetCurrentFPS(fps);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_destroy(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW(native_ptr);
    view->Destroy();
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_onResume(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW(native_ptr);
    view->Resume();
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_onPause(JNIEnv *env, jobject thiz, jlong native_ptr) {
    GET_VIEW(native_ptr);
    view->Pause();
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_setEnableFullChunks(JNIEnv *env, jobject thiz, jlong native_ptr, jboolean enable) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetEnableFullChunkLoad(enable);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_processMouseDragVelocity(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetMouseDragVelocity(x, y);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_setCachePath(JNIEnv *env, jobject thiz, jlong native_ptr, jstring path) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    char* pathStr = CStringHlp::jstringToChar(env, path);
    gameRenderer->SetCachePath(pathStr);
    free(pathStr);
}
extern "C" JNIEXPORT void JNICALL Java_com_imengyu_vr720_core_NativeVR720Renderer_setEnableCache(JNIEnv *env, jobject thiz, jlong native_ptr, jboolean enable) {
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetViewCacheEnabled(enable);
}
