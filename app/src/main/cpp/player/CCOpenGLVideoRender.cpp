//
// Created by roger on 2020/12/22.
//

#include "CCOpenGLVideoRender.h"
#include "CCOpenGLTexVideoDevice.h"

CCVideoDevice *CCOpenGLVideoRender::CreateVideoDevice() {
    return new CCOpenGLTexVideoDevice(renderer);
}
CCOpenGLVideoRender::CCOpenGLVideoRender(CCPanoramaRenderer * renderer) {
    this->renderer = renderer;
}
