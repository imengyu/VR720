//
// Created by roger on 2020/10/31.
//

#include "CMobileGameUIEventDistributor.h"

void CMobileGameUIEventDistributor::SendEvent(CCMobileGameUIEvent ev) {
#if defined(VR720_ANDROID)
    JNIEnv* env = nullptr;
    if(vm != nullptr) {
        vm->AttachCurrentThread(&env, nullptr);

        jmethodID nativeFeedBackMessage = env->GetMethodID(objNativeVR720RendererClass,
                "nativeFeedBackMessage", "(I)V");
        env->CallVoidMethod(objNativeVR720RendererObject, nativeFeedBackMessage, (int)ev);
    }
#endif
}

#if defined(VR720_ANDROID)
CMobileGameUIEventDistributor::CMobileGameUIEventDistributor(JNIEnv *env,
                                                             jobject objNativeVR720Renderer) {
    env->GetJavaVM(&vm);
    objNativeVR720RendererObject = env->NewGlobalRef(objNativeVR720Renderer);
    jclass clazz = env->FindClass("com/dreamfish/com/vr720/core/NativeVR720Renderer");
    objNativeVR720RendererClass = (jclass)env->NewGlobalRef(clazz);
}

CMobileGameUIEventDistributor::~CMobileGameUIEventDistributor() {
#if defined(VR720_ANDROID)
    JNIEnv* env = nullptr;
    if(vm != nullptr) {
        vm->AttachCurrentThread(&env, nullptr);
        env->DeleteGlobalRef(objNativeVR720RendererObject);
        env->DeleteGlobalRef(objNativeVR720RendererClass);
    }
#endif
}

#endif