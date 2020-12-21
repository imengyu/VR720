#include "stdafx.h"
#include "VR720Renderer.h"
#include "./utils/Logger.h"
#include "./core/CCAssetsManager.h"
#include "./core/CCMeshLoader.h"
#include "./core/CCSmartPtr.hpp"
#include "./core/CMobileGameRenderer.h"
#include "./player/CCVideoPlayer.h"

JavaVM *globalJvm;

jboolean JNICALL NativeVR720_initNative(JNIEnv *env, jclass, jobject assetManager, jobject context) {
  Logger::InitConst();
  CCPtrPool::InitPool();
  CCMeshLoader::Init();
  CCVideoPlayer::GlobalInit();
  CCAssetsManager::Android_InitFromJni(env, assetManager);
  CMobileGameRenderer::GlobalInit(env, context);
  return JNI_TRUE;
}
void JNICALL NativeVR720_releaseNative(JNIEnv *env, jclass) {

  CCMeshLoader::Destroy();
  CCPtrPool::ReleasePool();
  Logger::DestroyConst();
}
void JNICALL NativeVR720_updateAssetManagerPtr(JNIEnv *env, jclass clazz, jobject asset_manager) {
  CCAssetsManager::Android_InitFromJni(env, asset_manager);
}
jstring JNICALL NativeVR720_getNativeVersion(JNIEnv *env, jclass) {
  return env->NewStringUTF("2.1.6");
}
void JNICALL NativeVR720_lowMemory(JNIEnv *env, jclass clazz) {
    auto* pool = CCPtrPool::GetStaticPool();
    if(pool) pool->ClearUnUsedPtr();
}

static JNINativeMethod nativeMethods[] = {
        { "initNative", "(Landroid/content/res/AssetManager;Landroid/content/Context;)Z", (void*)NativeVR720_initNative },
        { "releaseNative", "()V", (void*)NativeVR720_releaseNative },
        { "updateAssetManagerPtr", "(Landroid/content/res/AssetManager;)V", (void*)NativeVR720_updateAssetManagerPtr },
        { "getNativeVersion", "()Ljava/lang/String;", (void*)NativeVR720_getNativeVersion },
        { "lowMemory", "()V", (void*)NativeVR720_lowMemory },
};

static int registerNativeMethods(JNIEnv* env) {
    jclass clazz;

    clazz = env->FindClass("com/imengyu/vr720/core/NativeVR720");
    if (clazz == NULL)
        return JNI_FALSE;
    if (env->RegisterNatives(clazz, nativeMethods, 5) < 0)
        return JNI_FALSE;
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void *reserved) {
    globalJvm = vm;
    __android_log_print(ANDROID_LOG_INFO, "Native", "JNI_OnLoad");

    JNIEnv* env;
    if (vm->GetEnv((void**)(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);

    if (!registerNativeMethods(env)) {
        return -1;
    }
    if (!registerRendererNativeMethods(env)) {
        return -1;
    }

    return JNI_VERSION_1_6;
}
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved)
{
  globalJvm = nullptr;
}