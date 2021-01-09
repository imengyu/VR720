//
// Created by roger on 2020/10/31.
//

#include "CMobileGameUIEventDistributor.h"

void CMobileGameUIEventDistributor::SendEvent(CCMobileGameUIEvent ev) {
    if(!initSuccess)
        return;

    JNIEnv *env = nullptr;
    JavaVM *vm = GetGlobalJvm();
    if (vm != nullptr) {
        vm->AttachCurrentThread(&env, nullptr);
        if(env != nullptr) {
            jmethodID nativeFeedBackMessage = env->GetMethodID(objNativeVR720RendererClass,
                                                               "nativeFeedBackMessage", "(I)V");
            env->CallVoidMethod(objNativeVR720RendererObject, nativeFeedBackMessage, (int) ev);
        }
    }
}


CMobileGameUIEventDistributor::CMobileGameUIEventDistributor(JNIEnv *env,
                                                             jobject objNativeVR720Renderer) {
    objNativeVR720RendererObject = env->NewGlobalRef(objNativeVR720Renderer);
    jclass clazz = env->FindClass("com/imengyu/vr720/core/natives/NativeVR720Renderer");
    objNativeVR720RendererClass = (jclass) env->NewGlobalRef(clazz);
    if(objNativeVR720RendererClass)
        initSuccess = true;
}

CMobileGameUIEventDistributor::~CMobileGameUIEventDistributor() {
    JNIEnv *env = nullptr;
    JavaVM *vm = GetGlobalJvm();
    if (vm != nullptr) {
        vm->AttachCurrentThread(&env, nullptr);
        if(env != nullptr) {
            env->DeleteGlobalRef(objNativeVR720RendererObject);
            env->DeleteGlobalRef(objNativeVR720RendererClass);
        }
    }
    initSuccess = false;
}
