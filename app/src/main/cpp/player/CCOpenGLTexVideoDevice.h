//
// Created by roger on 2020/12/22.
//

#ifndef VR720_CCOPENGLTEXVIDEODEVICE_H
#define VR720_CCOPENGLTEXVIDEODEVICE_H
#include "stdafx.h"
#include "CCVideoDevice.h"
#include "../core/CCPanoramaRenderer.h"

class CCOpenGLTexVideoDevice : public CCVideoDevice {
public:
    CCOpenGLTexVideoDevice(CCPanoramaRenderer* renderer) {
        this->renderer = renderer;
    }
    ~CCOpenGLTexVideoDevice() {}

    void Destroy() override ;
    uint8_t* Lock(uint8_t *src, int srcStride, int*destStride, int64_t pts) override;
    void Unlock() override;
    void Dirty() override;

    void Pause(int pause) override;
    void Reset() override;

    void UpdateVideoMode(int w, int h) override;

private:
    CCPanoramaRenderer* renderer;


};


#endif //VR720_CCOPENGLTEXVIDEODEVICE_H
