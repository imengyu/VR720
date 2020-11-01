#include "stdafx.h"
#include "com_dreamfish_com_vr720_core_NativeVR720.h"

#include "Logger.h"
#include "CCAssetsManager.h"
#include "CCMeshLoader.h"
#include "CCSmartPtr.hpp"

extern "C" JNIEXPORT jboolean JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_initNative(JNIEnv *env, jclass, jobject assetManager) {
  Logger::InitConst();
  CCPtrPool::InitPool();
  CCMeshLoader::Init();
  CCAssetsManager::Android_InitFromJni(env, assetManager);

  return JNI_TRUE;
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_releaseNative(JNIEnv *env, jclass) {


  CCMeshLoader::Destroy();
  CCPtrPool::ReleasePool();
  Logger::DestroyConst();
}
extern "C" JNIEXPORT jstring JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_getNativeVersion(JNIEnv *env, jclass) {
  return env->NewStringUTF("1.0.2.DEV-3");
}
extern "C" JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_lowMemory(JNIEnv *env, jclass clazz) {
    auto* pool = CCPtrPool::GetStaticPool();
    if(pool) pool->ClearUnUsedPtr();
}