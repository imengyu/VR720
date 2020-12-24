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

    virtual int64_t GetCurVideoDts() { return curVideoDts; }
    virtual int64_t GetCurVideoPts() { return curVideoPts; }
    virtual int64_t GetCurAudioDts() { return curAudioDts; }
    virtual int64_t GetCurAudioPts() { return curAudioPts; }

    virtual void SetVolume(int i);
    virtual int GetVolume() { return volCur - volZeroDb; }

    virtual void SetSeekDest(int64_t dest);

protected:

    virtual CCVideoDevice* CreateVideoDevice();
    virtual CCAudioDevice *CreateAudioDevice(CCVideoPlayerExternalData *data);

private:

    SwrContext *swrContext = NULL;
    SwsContext *swsContext = nullptr;

    int64_t curVideoDts = 0;	//记录当前播放的视频流Packet的DTS
    int64_t curVideoPts = 0;	//记录当前播放的视频流Packet的DTS
    int64_t curAudioDts = 0;	//记录当前播放的音频流Packet的DTS
    int64_t curAudioPts = 0;	//记录当前播放的音频流Packet的DTS

    // 输出缓冲
    uint8_t *audioOutBuffer[1] = { nullptr };
    // 重采样后，每个通道包含的采样数
    // acc默认为1024，重采样后可能会变化
    int destNbSample = 1024;
    // 重采样以后，一帧数据的大小
    size_t destDataSize = 0;

    //时钟
    double currentAudioClock = 0;
    double currentVideoClock = 0;

    //软件音量控制
    int volZeroDb = 0;
    int volScaler[256];
    int volCur = 0;

    int64_t seekDest = 0;
    bool audioSeeking = false;
    bool videoSeeking = false;

    //
    //状态控制

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

    int SwVolScalerInit(int *scaler, int mindb, int maxdb);

    void SwVolScalerRun(int16_t *buf, int n, int multiplier);
};


#endif //VR720_CCPLAYERRENDER_H
