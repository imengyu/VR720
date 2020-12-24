//
// Created by roger on 2020/12/19.
//

#ifndef VR720_CCVIDEOPLAYER_H
#define VR720_CCVIDEOPLAYER_H
#include "stdafx.h"
#include "CCPlayerDefine.h"
#include "CCDecodeQueue.h"
#include "CCPlayerRender.h"
#include <pthread.h>

class CCVideoPlayer;
//播放器事件回调
typedef void (*CCVideoPlayerEventCallback)(CCVideoPlayer* player, int message, void* customData);

/**
 * 简单视频播放器
 */
class CCVideoPlayer {

public:
    CCVideoPlayer();
    CCVideoPlayer(CCVideoPlayerInitParams * initParams);
    ~CCVideoPlayer();

    static void GlobalInit();
    const char* GetLastError();

    //初始配置和状态信息
    //**********************

    CCVideoPlayerInitParams InitParams;

    //播放器公共方法
    //**********************

    bool OpenVideo(const char* filePath);
    bool CloseVideo();

    void SetVideoState(CCVideoState newState);
    void SetVideoPos(int64_t pos);
    void SetVideoVolume(int vol);

    CCVideoState GetVideoState();
    int64_t GetVideoLength();
    int64_t GetVideoPos();
    int GetVideoVolume();
    void GetVideoSize(int* w, int* h);

    //回调
    //**********************

    void SetPlayerEventCallback(CCVideoPlayerEventCallback callback, void* data) {
        videoPlayerEventCallback = callback;
        videoPlayerEventCallbackData = data;
    }
    CCVideoPlayerEventCallback GetPlayerEventCallback() { return videoPlayerEventCallback; }

protected:

    std::string lastError;
    std::string currentFile;
    CCVideoState playerStatus = CCVideoState::NotOpen;

    void SetLastError(const char * str);

    CCVideoPlayerEventCallback videoPlayerEventCallback = nullptr;
    void*videoPlayerEventCallbackData = nullptr;

    void CallPlayerEventCallback(int message);

    //解码器相关
    //**********************

    AVFormatContext * formatContext = nullptr;// 解码信息上下文
    AVCodec * audioCodec = nullptr;// 音频解码器
    AVCodec * videoCodec = nullptr;// 视频解码器
    AVCodecContext * audioCodecContext = nullptr;//解码器上下文
    AVCodecContext * videoCodecContext = nullptr;//解码器上下文


    int64_t currentTime = 0;
    int64_t startedTime = -1;// 开始播放的时间
    long duration = 0;// 总时长

    CCDecodeState decodeState = CCDecodeState::NotInit;// 解码状态
    CCVideoState videoState = CCVideoState::NotOpen;

    CCDecodeQueue decodeQueue;
    CCPlayerRender *render = nullptr;

    int videoIndex = -1;// 数据流索引
    int audioIndex = -1;// 数据流索引

    bool InitDecoder();
    bool DestroyDecoder();

    void Init(CCVideoPlayerInitParams *initParams);
    void Destroy();

    CCVideoPlayerExternalData externalData;

private:

    //线程控制

    void StartDecoderThread();
    void StopDecoderThread() ;

    pthread_t decoderWorkerThread = 0;
    pthread_t decoderAudioThread = 0;
    pthread_t decoderVideoThread = 0;
    pthread_t playerWorkerThread = 0;

    bool playerWorking = false;
    bool decoderAudioFinish = false;
    bool decoderVideoFinish = false;

    static void* PlayerWorkerThreadStub(void*param);
    static void* DecoderWorkerThreadStub(void*param);
    static void* DecoderVideoThreadStub(void *param);
    static void* DecoderAudioThreadStub(void *param);

    void* PlayerWorkerThread();
    void* DecoderWorkerThread();
    void* DecoderVideoThread();
    void* DecoderAudioThread();

    //流水线启停

    void StartAll();
    void StopAll();

    void FlushStopStatus();

    //播放器工作子线程控制

    UCHAR playerClose = 0;
    UCHAR playerOpen = 0;
    UCHAR playerSeeking = 0;

    int64_t seekDest = 0;
    int64_t seekPos = 0;

    void DoOpenVideo();
    void DoCloseVideo();
    void DoSeekVideo();

    static void FFmpegLogFunc(void* ptr, int level, const char* fmt,va_list vl);
};

#endif //VR720_CCVIDEOPLAYER_H
