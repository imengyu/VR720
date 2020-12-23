//
// Created by roger on 2020/12/22.
//

#ifndef VR720_CCPLAYERRENDER_H
#define VR720_CCPLAYERRENDER_H
#include "stdafx.h"
extern "C" {
#include "libavutil/frame.h"
#include "libswresample/swresample.h"
#include "libswscale/swscale.h"
}
#include "CCPlayerDefine.h"

class CCAudioDevice;
class CCVideoDevice;
class CCVideoPlayerExternalData;
class CCPlayerRender {
public:

    virtual bool Init(CCVideoPlayerExternalData *data);
    virtual void Destroy();
    virtual void Start();
    virtual void Reset();
    virtual void Pause();

    virtual CCVideoDevice* GetVideoDevice() { return videoDevice; }
    virtual CCAudioDevice *GetAudioDevice() { return audioDevice; }

protected:

    virtual CCVideoDevice* CreateVideoDevice();
    virtual CCAudioDevice *CreateAudioDevice(CCVideoPlayerExternalData *data);

private:

    SwrContext *swrContext = NULL;
    SwsContext *swsContext = nullptr;

    // 输出缓冲
    uint8_t *audioOutBuffer[1] = { nullptr };
    // 重采样后，每个通道包含的采样数
    // acc默认为1024，重采样后可能会变化
    int destNbSample = 1024;
    // 重采样以后，一帧数据的大小
    size_t destDataSize = 0;

    CCVideoPlayerExternalData *externalData;

    CCRenderState status = CCRenderState::NotRender;

    CCAudioDevice* audioDevice = nullptr;
    CCVideoDevice* videoDevice = nullptr;

    pthread_t renderVideoThread = 0;
    pthread_t renderAudioThread = 0;

    static void* RenderVideoThreadStub(void *param);
    static void* RenderAudioThreadStub(void *param);
    void* RenderAudioThread();
    void* RenderVideoThread();
};


#endif //VR720_CCPLAYERRENDER_H
