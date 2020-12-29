//
// Created by roger on 2020/11/1.
//
#include "stdafx.h"
#include "VR720Renderer.h"
#include "./core/CMobileGameRenderer.h"
#include "./core/CMobileGameUIEventDistributor.h"
#include "./core/CMobileOpenGLView.h"
#include "./core/CCProperties.h"
#include "./utils/CStringHlp.h"

#define GET_VIEW(ptr) auto* view = (CMobileOpenGLView*)native_ptr;\
                      if(!view) return
#define GET_VIEW_OR_RET(ptr,ret) auto* view = (CMobileOpenGLView*)native_ptr;\
                      if(!view) return ret
/**
 * 应用层入口函数
 */

/*
CMobileOpenGLView* getJavaObjectOpenGLViewPtr(JNIEnv *env, jobject thiz) {
    //nativeGetNativePtr
    jclass clazz = env->FindClass("com/imengyu/vr720/core/NativeVR720Renderer");
    jmethodID nativeGetNativePtr = env->GetMethodID(clazz, "nativeGetNativePtr", "()J");
    jlong viewPtr = env->CallLongMethod(thiz, nativeGetNativePtr);
    env->DeleteLocalRef(clazz);
    return (CMobileOpenGLView*)viewPtr;
}
*/

void JNICALL NativeVR720Renderer_onCreate(JNIEnv *env, jobject thiz) {
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

    LOGIF("NativeEntry", "NativeVR720Renderer.onCreate: View ptr : 0x%xl", newViewPtr);
}
void JNICALL NativeVR720Renderer_onDestroy(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    LOGI("NativeEntry", "NativeVR720Renderer.onDestroy");
    view->ManualDestroy();
    //Destroy
    delete view;
}
void JNICALL NativeVR720Renderer_onSurfaceCreated(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->Init();
}
void JNICALL NativeVR720Renderer_onSurfaceChanged(JNIEnv *env, jobject thiz, jlong native_ptr, int width, jint height) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->Resize(width, height);
}
void JNICALL NativeVR720Renderer_onDrawFrame(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->Render();
}
void JNICALL NativeVR720Renderer_onMainThread(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->Update();
}
void JNICALL NativeVR720Renderer_processKey(JNIEnv *env, jobject thiz, jlong native_ptr, jint key, jboolean down) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->ProcessKeyEvent(key, down);
}
void JNICALL NativeVR720Renderer_processViewZoom(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat v) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->ProcessZoomEvent(v);
}
void JNICALL NativeVR720Renderer_processMouseUp(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->ProcessMouseEvent(ViewMouseMouseUp, x, y);
}
void JNICALL NativeVR720Renderer_processMouseDown(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->ProcessMouseEvent(ViewMouseMouseDown, x, y);
}
void JNICALL NativeVR720Renderer_processMouseMove(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->ProcessMouseEvent(ViewMouseMouseMove, x, y);
}
void JNICALL NativeVR720Renderer_openFile(JNIEnv *env, jobject thiz, jlong native_ptr, jstring path) {
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    char* pathptr = CStringHlp::jstringToChar(env, path);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetProp(PROP_FILE_PATH, pathptr);
    gameRenderer->MarkShouldOpenFile();
    free(pathptr);
}
void JNICALL NativeVR720Renderer_closeFile(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->MarkCloseFile();
}
void JNICALL NativeVR720Renderer_updateGyroValue(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y, jfloat z, jfloat w) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->UpdateGyroValue(x,y,z,w);
}
void JNICALL NativeVR720Renderer_updateDebugValue(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, float y, jfloat z, jfloat w, jfloat v, jfloat u) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->UpdateDebugValue(x,y,z,w,u,v);
}
void JNICALL NativeVR720Renderer_onUpdateFps(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat fps) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->SetCurrentFPS(fps);
}
void JNICALL NativeVR720Renderer_destroy(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->Destroy();
}
void JNICALL NativeVR720Renderer_onResume(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->Resume();
}
void JNICALL NativeVR720Renderer_onPause(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    view->Pause();
}
void JNICALL NativeVR720Renderer_processMouseDragVelocity(JNIEnv *env, jobject thiz, jlong native_ptr, jfloat x, jfloat y) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetMouseDragVelocity(x, y);
}
void JNICALL NativeVR720Renderer_setVideoPos(JNIEnv *env, jobject thiz, jlong native_ptr, jint pos) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetVideoPos(pos);
}
void JNICALL NativeVR720Renderer_updateVideoState(JNIEnv *env, jobject thiz, jlong native_ptr, jint new_state) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetVideoState((CCVideoState)new_state);
}
jint JNICALL NativeVR720Renderer_getVideoState(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW_OR_RET(native_ptr, 0);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return (jint)gameRenderer->GetVideoState();
}
jint JNICALL NativeVR720Renderer_getVideoLength(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW_OR_RET(native_ptr, 0);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return gameRenderer->GetVideoLength();
}
jint JNICALL NativeVR720Renderer_getVideoPos(JNIEnv *env, jobject thiz, jlong native_ptr) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thiz);
    GET_VIEW_OR_RET(native_ptr, 0);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return gameRenderer->GetVideoPos();
}
jstring JNICALL NativeVR720Renderer_getProp(JNIEnv *env, jobject thizz, jlong native_ptr, jint id) {
    UNREFERENCED_PARAMETER(thizz);
    GET_VIEW_OR_RET(native_ptr, nullptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return CStringHlp::charTojstring(env, gameRenderer->GetProp(id));
}
void JNICALL NativeVR720Renderer_setProp(JNIEnv *env, jobject thizz, jlong native_ptr, jint id, jstring value) {
    UNREFERENCED_PARAMETER(thizz);
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    char* str = CStringHlp::jstringToChar(env, value);
    gameRenderer->SetProp(id, str);
    free(str);
}
int JNICALL NativeVR720Renderer_getIntProp(JNIEnv *env, jobject thizz, jlong native_ptr, jint id) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thizz);
    GET_VIEW_OR_RET(native_ptr, 0);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return gameRenderer->GetIntProp(id);
}
void JNICALL NativeVR720Renderer_setIntProp(JNIEnv *env, jobject thizz, jlong native_ptr, jint id, int value) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thizz);
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetIntProp(id, value);
}
jboolean JNICALL NativeVR720Renderer_getBoolProp(JNIEnv *env, jobject thizz, jlong native_ptr, jint id) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thizz);
    GET_VIEW_OR_RET(native_ptr, 0);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    return gameRenderer->GetBoolProp(id);
}
void JNICALL NativeVR720Renderer_setBoolProp(JNIEnv *env, jobject thizz, jlong native_ptr, jint id, jboolean value) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(thizz);
    GET_VIEW(native_ptr);
    auto* gameRenderer = (CMobileGameRenderer*)view->GetRenderer();
    gameRenderer->SetBoolProp(id, value);
}

static JNINativeMethod rendererNativeMethods[] = {
        { "onCreate", "()V", (void*)NativeVR720Renderer_onCreate },
        { "onDestroy", "(J)V", (void*)NativeVR720Renderer_onDestroy },
        { "onSurfaceCreated", "(J)V", (void*)NativeVR720Renderer_onSurfaceCreated },
        { "onSurfaceChanged", "(JII)V", (void*)NativeVR720Renderer_onSurfaceChanged },
        { "onDrawFrame", "(J)V", (void*)NativeVR720Renderer_onDrawFrame },
        { "destroy", "(J)V", (void*)NativeVR720Renderer_destroy },
        { "getProp", "(JI)Ljava/lang/String;", (void*)NativeVR720Renderer_getProp },
        { "setProp", "(JILjava/lang/String;)V", (void*)NativeVR720Renderer_setProp },
        { "getIntProp", "(JI)I", (void*)NativeVR720Renderer_getIntProp },
        { "setIntProp", "(JII)V", (void*)NativeVR720Renderer_setIntProp },
        { "getBoolProp", "(JI)Z", (void*)NativeVR720Renderer_getBoolProp },
        { "setBoolProp", "(JIZ)V", (void*)NativeVR720Renderer_setBoolProp },
        { "onMainThread", "(J)V", (void*)NativeVR720Renderer_onMainThread },
        { "processKey", "(JIZ)V", (void*)NativeVR720Renderer_processKey },
        { "processMouseUp", "(JFF)V", (void*)NativeVR720Renderer_processMouseUp },
        { "processMouseMove", "(JFF)V", (void*)NativeVR720Renderer_processMouseMove },
        { "processMouseDown", "(JFF)V", (void*)NativeVR720Renderer_processMouseDown },
        { "processMouseDragVelocity", "(JFF)V", (void*)NativeVR720Renderer_processMouseDragVelocity },
        { "processViewZoom", "(JF)V", (void*)NativeVR720Renderer_processViewZoom },
        { "openFile", "(JLjava/lang/String;)V", (void*)NativeVR720Renderer_openFile },
        { "closeFile", "(J)V", (void*)NativeVR720Renderer_closeFile },
        { "updateGyroValue", "(JFFFF)V", (void*)NativeVR720Renderer_updateGyroValue },
        { "updateDebugValue", "(JFFFFFF)V", (void*)NativeVR720Renderer_updateDebugValue },
        { "onUpdateFps", "(JF)V", (void*)NativeVR720Renderer_onUpdateFps },
        { "onResume", "(J)V", (void*)NativeVR720Renderer_onResume },
        { "onPause", "(J)V", (void*)NativeVR720Renderer_onPause },
        { "setVideoPos", "(JI)V", (void*)NativeVR720Renderer_setVideoPos },
        { "updateVideoState", "(JI)V", (void*)NativeVR720Renderer_updateVideoState },
        { "getVideoState", "(J)I", (void*)NativeVR720Renderer_getVideoState },
        { "getVideoLength", "(J)I", (void*)NativeVR720Renderer_getVideoLength },
        { "getVideoPos", "(J)I", (void*)NativeVR720Renderer_getVideoPos },
};

int registerRendererNativeMethods(JNIEnv* env) {
    jclass clazz;

    clazz = env->FindClass("com/imengyu/vr720/core/NativeVR720Renderer");
    if (clazz == nullptr)
        return JNI_FALSE;
    if (env->RegisterNatives(clazz, rendererNativeMethods, 31) < 0)
        return JNI_FALSE;
    return JNI_TRUE;
}