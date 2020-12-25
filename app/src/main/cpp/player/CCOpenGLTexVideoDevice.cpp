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
        *destStride = texture->width * 4;//GL_RGBA
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
void CCOpenGLTexVideoDevice::Dirty() {
    renderer->VideoTexMarkDirty();
}
void CCOpenGLTexVideoDevice::Pause(int pause) {
    renderer->VideoTexUpdateRunStatus(!pause);
    CCVideoDevice::Pause(pause);
}
void CCOpenGLTexVideoDevice::Reset() {
    renderer->VideoTexReset();
    CCVideoDevice::Reset();
}
