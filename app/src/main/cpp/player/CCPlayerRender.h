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
    const char* LOG_TAG = "CCPlayerRender";
public:

    virtual bool Init(CCVideoPlayerExternalData *data);
    virtual void Destroy();
    virtual void Start(bool isStartBySeek);
    virtual void Reset();
    virtual void Pause();

    virtual CCVideoDevice* GetVideoDevice() { return videoDevice; }
    virtual CCAudioDevice *GetAudioDevice() { return audioDevice; }

    virtual int64_t GetCurVideoDts() { return curVideoDts; }
    virtual int64_t GetCurVideoPts() { return curVideoPts; }
    virtual int64_t GetCurAudioDts() { return curAudioDts; }
    virtual int64_t GetCurAudioPts() { return curAudioPts; }

    virtual void SetVolume(int i);
    virtual int GetVolume() { return 0; }

    virtual void SetSeekDest(int64_t dest);

    virtual bool IsCurrentSeekToPosFinished() {
        if(currentSeekToPosFinished) {
            currentSeekToPosFinished = false;
            return true;
        }
        return false;
    }

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


    int64_t seekDest = 0;
    bool audioSeeking = false;
    bool videoSeeking = false;

    //
    //状态控制

    CCVideoPlayerExternalData *externalData;

    CCRenderState status = CCRenderState::NotRender;
    bool currentSeekToPosFinished = false;

    CCAudioDevice* audioDevice = nullptr;
    CCVideoDevice* videoDevice = nullptr;

    AVFrame *outFrame = nullptr;
    uint8_t *outFrameBuffer = nullptr;
    size_t outFrameBufferSize = 0;
    int outFrameDestWidth = 0;
    int outFrameDestHeight = 0;
    AVPixelFormat outFrameDestFormat = AV_PIX_FMT_RGBA;

    pthread_t renderVideoThread = 0;
    pthread_t renderAudioThread = 0;

    static void* RenderVideoThreadStub(void *param);
    //static void* RenderAudioThreadStub(void *param);
    //void* RenderAudioThread();
    static void RenderAudioThreadStub(CCAudioDevice* dev, void* customData, uint8_t **buf, int *len);
    void RenderAudioThread(CCAudioDevice* dev, uint8_t **buf, int *len);
    void* RenderVideoThread();

    double videoLastPlayTime,  //上一帧的播放时间
        videoPlayTime,             //当前帧的播放时间
        videoLastDelay,    // 上一次播放视频的两帧视频间隔时间
        videoDelay,         //两帧视频间隔时间
        videoActualDelay,//真正需要延迟时间
        syncThreshold, //合理的范围
        startTime;  //从第一帧开始的绝对时间

    double VideoPlayTimeSynchronize(AVFrame *frame, double play);
};


#endif //VR720_CCPLAYERRENDER_H
