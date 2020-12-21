//
// Created by roger on 2020/12/19.
//

#ifndef VR720_CCVIDEOPLAYER_H
#define VR720_CCVIDEOPLAYER_H
#include "stdafx.h"
extern "C" {
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
#include "libavutil/opt.h"
#include "libavutil/imgutils.h"
#include "libavutil/mathematics.h"
#include "libavutil/time.h"
}
#include "CCDecodeQueue.h"
#include <pthread.h>

struct CCVideoPlayerExternalData {
    int64_t start_time;
    int64_t start_tick;
    int64_t start_pts ;
    int64_t apts;  // current apts
    int64_t vpts;  // current vpts
    int     apktn; // available audio packet number in pktqueue
    int     vpktn; // available video packet number in pktqueue
};
class CCVideoPlayerInitParams {
public:
    bool openSyncmode = true;
    int autoReconnect = 1000;         // w  播放流媒体时自动重连的超时时间，毫秒为单位
    int videoVWidth = 0;              // wr video actual width
    int videoVHeight = 0;             // wr video actual height
    int videoOWidth = 0;              // r  video output width  (after rotate)
    int videoOHeight = 0;             // r  video output height (after rotate)
    int videoFrameRate = 0;           // wr 视频帧率
    int videoStreamTotal = 0;
    int audioChannels = 0;
    int audioSampleRate = 0;
    int audioStreamTotal = 0;
    AVCodecID videoCodecId;
};

//解码器状态值

#define DECODER_FLAG_PS_A_PAUSE    (1 << 0)  // audio decoding pause
#define DECODER_FLAG_PS_V_PAUSE    (1 << 1)  // video decoding pause
#define DECODER_FLAG_PS_R_PAUSE    (1 << 2)  // rendering pause
#define DECODER_FLAG_PS_F_SEEK     (1 << 3)  // seek flag
#define DECODER_FLAG_PS_A_SEEK     (1 << 4)  // seek audio
#define DECODER_FLAG_PS_V_SEEK     (1 << 5)  // seek video
#define DECODER_FLAG_PS_CLOSE      (1 << 6)  // close player
#define DECODER_FLAG_PS_RECONNECT  (1 << 7)  // reconnect

//用户可见的播放器状态值
enum class CCVideoState {
    NotOpen = 0,
    Playing = 1,
    Ended = 2,
    Opened = 3,
    Paused = 3,
    Failed = 4,
};

enum {
    SEEK_STEP_FORWARD = 1,
    SEEK_STEP_BACKWARD,
};

class CCVideoPlayer {


public:
    static void GlobalInit();
    const char* GetLastError();

    int GetStreamTotal(AVMediaType type);

    //
    //初始配置和状态信息

    CCVideoPlayerInitParams InitParams;

    //
    //Player events

    bool OpenVideo(char* filePath);
    bool CloseVideo();

    void SetVideoState(CCVideoState newState);
    void SetVideoPos(int64_t pos, int type);

    CCVideoState GetVideoState();
    int64_t GetVideoLength();
    int64_t GetVideoPos();

protected:

    AVFormatContext* pFormatCtx;

    AVCodec *aCodec = nullptr;
    AVCodec *vCodec = nullptr;

    AVCodecContext *vCodecCtx = nullptr;
    AVCodecContext *aCodecCtx = nullptr;

    int videoIndex = -1;
    int audioIndex = -1;

    AVRational aStreamTimebase;
    AVRational vStreamTimebase;
    AVFrame vFrame;
    AVFrame aFrame;

    char lastError[64];
    std::string currentFile;

    int width = 0;
    int height = 0;

    CCDecodeQueue decodeQueue;

    int seekReq;
    int64_t seekPos ;
    int64_t seekDest;
    int64_t seekVpts;
    int seekDiff;
    int seekSidx;

    //Threads
    pthread_t avdemux_thread;
    pthread_t adecode_thread;
    pthread_t vdecode_thread;

    // player init timeout, and init params
    int64_t readTimeLast;
    int64_t readTimeout;

    CCVideoState playerStatus = CCVideoState::NotOpen;
    int decoderFlags = 0;

    CCVideoPlayerExternalData internalData;

    void SetLastError(const char * str);

private:

    bool InitDecoder();
    void InitThread();
    void StartDecoder();
    void StopDecoder();
    bool DestroyDecoder();

    static void* av_demux_thread_proc(void *param);
    static void* audio_decode_thread_proc(void *param);
    static void* video_decode_thread_proc(void *param);


    void HandleFSeekOrReconnect(int reconnect);
};


#endif //VR720_CCVIDEOPLAYER_H
