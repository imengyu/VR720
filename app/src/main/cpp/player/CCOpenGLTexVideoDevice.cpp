//
// Created by roger on 2020/12/22.
//

#include "CCOpenGLTexVideoDevice.h"

void CCOpenGLTexVideoDevice::Destroy() {
    renderer->VideoTexReset();
    CCVideoDevice::Destroy();
}
uint8_t* CCOpenGLTexVideoDevice::Lock(uint8_t *src, int srcStride, int* destStride, int64_t pts) {
    renderer->VideoTexLock(true);
    auto* texture = renderer->VideoTexGet();
    if(texture) {
        *destStride = texture->width * (texture->textureType == GL_RGBA ? 4 : 3);
        return (UCHAR *) texture->GetBackupDataPtr();
    }
    return nullptr;
}
void CCOpenGLTexVideoDevice::UpdateVideoMode(int w, int h) {
    renderer->VideoTexGet()->DoBackupBufferData(nullptr, w, h, GL_RGBA);
    CCVideoDevice::UpdateVideoMode(w, h);
}
void CCOpenGLTexVideoDevice::Unlock() {
    renderer->VideoTexLock(false);
}
void CCOpenGLTexVideoDevice::Pause(int pause) {
    renderer->VideoTexUpdateRunStatus(!pause);
    CCVideoDevice::Pause(pause);
}
void CCOpenGLTexVideoDevice::Reset() {
    renderer->VideoTexReset();
    CCVideoDevice::Reset();
}
