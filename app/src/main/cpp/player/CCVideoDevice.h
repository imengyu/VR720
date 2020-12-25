//
// Created by roger on 2020/12/22.
//

#ifndef VR720_CCVIDEODEVICE_H
#define VR720_CCVIDEODEVICE_H

#include "stdafx.h"
extern "C" {
#include "libavutil/frame.h"
}

class CCVideoDevice {

public:
    CCVideoDevice() {}
    virtual ~CCVideoDevice() {}

    virtual void Destroy() {}
    virtual uint8_t* Lock(uint8_t *src, int srcStride, int*destStride, int64_t pts) { return nullptr; }
    virtual void Unlock() {}
    virtual void Dirty() {}

    virtual void SetRect(int x, int y, int w, int h) {}
    virtual void Pause(int pause) {}
    virtual void Reset() {}

    virtual void UpdateVideoMode(int w, int h) {
        vw = w;
        vh = h;
    }
    virtual void PostSurface(AVFrame*frame, RECT*rect) {}

protected:
    int vh = 0,vw = 0;
};


#endif //VR720_CCVIDEODEVICE_H
