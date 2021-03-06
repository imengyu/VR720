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
    DestroyComplete,
    VideoStateChanged,
};

class CMobileGameUIEventDistributor {

public:
    CMobileGameUIEventDistributor(JNIEnv * env, jobject objNativeVR720Renderer);
    ~CMobileGameUIEventDistributor();

    void SendEvent(CCMobileGameUIEvent ev);

private:
    jclass objNativeVR720RendererClass;
    jobject objNativeVR720RendererObject;
    bool initSuccess = false;
};


#endif //VR720_CMOBILEGAMEUIEVENTDISTRIBUTOR_H
