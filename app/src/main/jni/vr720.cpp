#include "stdafx.h"
#include "com_dreamfish_com_vr720_core_NativeVR720.h";

NIEXPORT jboolean JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_initNative
  (JNIEnv *, jclass) {
  return false;
}
JNIEXPORT void JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_releaseNative
  (JNIEnv *, jclass) {

}
JNIEXPORT jstring JNICALL Java_com_dreamfish_com_vr720_core_NativeVR720_getNativeVersion
  (JNIEnv *, jclass) {
  return env->NewStringUTF("1.0.2.DEV-3");
}
