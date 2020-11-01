//
// Created by roger on 2020/10/31.
//

#ifndef VR720_CMOBILEGAMEUIEVENTDISTRIBUTOR_H
#define VR720_CMOBILEGAMEUIEVENTDISTRIBUTOR_H

#include "stdafx.h"

enum class CCMobileGameUIEvent {
    MarkLoadingStart,
    MarkLoadingEnd,
    MarkLoadFailed,
    UiInfoChanged,
    FileClosed,
};

class CMobileGameUIEventDistributor {

public:
#if defined(VR720_ANDROID)
    CMobileGameUIEventDistributor(JNIEnv * env, jobject objNativeVR720Renderer);
#endif
    ~CMobileGameUIEventDistributor();

    void SendEvent(CCMobileGameUIEvent ev);

private:
#if defined(VR720_ANDROID)
    JavaVM* vm = nullptr;
    jclass objNativeVR720RendererClass;
    jobject objNativeVR720RendererObject;
#endif
};


#endif //VR720_CMOBILEGAMEUIEVENTDISTRIBUTOR_H
