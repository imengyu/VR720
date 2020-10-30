#include "stdafx.h"
#include "com_dreamfish_com_vr720_core_NativeVR720.h"

#include "Logger.h"
#include "CCAssetsManager.h"
#include "CCMeshLoader.h"

extern "C" jboolean JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_initNative(JNIEnv *env, jclass, jobject assetManager) {
  Logger::InitConst();
  CCMeshLoader::Init();
  CCAssetsManager::Android_InitFromJni(env, assetManager);

  return JNI_FALSE;
}
extern "C" void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_releaseNative(JNIEnv *env, jclass) {


  CCMeshLoader::Destroy();
  Logger::DestroyConst();
}
extern "C" jstring JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_getNativeVersion(JNIEnv *env, jclass) {
  return env->NewStringUTF("1.0.2.DEV-3");
}
