#include "stdafx.h"
#include "VR720Renderer.h"
#include "./utils/Logger.h"
#include "./core/CCAssetsManager.h"
#include "./core/CCMeshLoader.h"
#include "./core/CCSmartPtr.hpp"
#include "./core/CMobileGameRenderer.h"
#include "./player/CCVideoPlayer.h"

JavaVM *globalJvm;
bool nativeInitStaus = false;

jboolean JNICALL NativeVR720_initNative(JNIEnv *env, jclass, jobject assetManager, jobject context) {

    if(nativeInitStaus) {
        CCAssetsManager::Android_InitFromJni(env, assetManager);
        CMobileGameRenderer::GlobalInit(env, context);

        ALOGW("Native", "Can not init, already init");
        return JNI_FALSE;
    }

    Logger::InitConst();
    CCPtrPool::InitPool();
    CCMeshLoader::Init();
    CCVideoPlayer::GlobalInit();
    CCAssetsManager::Android_InitFromJni(env, assetManager);
    CMobileGameRenderer::GlobalInit(env, context);

    nativeInitStaus = true;
    return JNI_TRUE;
}
void JNICALL NativeVR720_releaseNative(JNIEnv *env, jclass clazz) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(clazz);

    if(!nativeInitStaus) {
        ALOGW("Native", "Can not release, Not init");
        return;
    }

    CCMeshLoader::Destroy();
    CCPtrPool::ReleasePool();
    Logger::DestroyConst();

    nativeInitStaus = false;
}
jstring JNICALL NativeVR720_getNativeVersion(JNIEnv *env, jclass clazz) {
    UNREFERENCED_PARAMETER(clazz);
    return env->NewStringUTF("2.1.6");
}
void JNICALL NativeVR720_lowMemory(JNIEnv *env, jclass clazz) {
    UNREFERENCED_PARAMETER(env);
    UNREFERENCED_PARAMETER(clazz);
    auto *pool = CCPtrPool::GetStaticPool();
    if (pool) pool->ClearUnUsedPtr();
}

static JNINativeMethod nativeMethods[] = {
        {"initNative",            "(Landroid/content/res/AssetManager;Landroid/content/Context;)Z", (void *) NativeVR720_initNative},
        {"releaseNative",         "()V",                                                            (void *) NativeVR720_releaseNative},
        {"getNativeVersion",      "()Ljava/lang/String;",                                           (void *) NativeVR720_getNativeVersion},
        {"lowMemory",             "()V",                                                            (void *) NativeVR720_lowMemory},
};

static int registerNativeMethods(JNIEnv *env) {
    jclass clazz;

    clazz = env->FindClass("com/imengyu/vr720/core/NativeVR720");
    if (clazz == nullptr) {
        ALOGE("Native", "NativeVR720 class not found!");
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, nativeMethods, 4) < 0) {
        ALOGE("Native", "RegisterNatives failed!");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JavaVM *GetGlobalJvm() { return globalJvm; }
JNIEnv *GetJniEnv() {
    JNIEnv *env = nullptr;
    if (globalJvm != nullptr)
        globalJvm->AttachCurrentThread(&env, nullptr);
    return env;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    UNREFERENCED_PARAMETER(reserved);

    globalJvm = vm;
    ALOGI("Native", "JNI_OnLoad");

    JNIEnv *env;
    if (vm->GetEnv((void **) (&env), JNI_VERSION_1_6) != JNI_OK) {
        ALOGE("Native", "Jni version not support !");
        return -1;
    }
    assert(env != nullptr);

    if (!registerNativeMethods(env)) {
        return -1;
    }
    if (!registerRendererNativeMethods(env)) {
        return -1;
    }

    av_jni_set_java_vm((void*)vm, nullptr);

    return JNI_VERSION_1_6;
}
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    UNREFERENCED_PARAMETER(vm);
    UNREFERENCED_PARAMETER(reserved);
    globalJvm = nullptr;
}