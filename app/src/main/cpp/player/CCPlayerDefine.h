//
// Created by roger on 2020/12/22.
//

#ifndef VR720_CCPLAYERDEFINE_H
#define VR720_CCPLAYERDEFINE_H

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

//结构体声明
//***************************************

/**
 * 播放器初始化数据
 */
class CCPlayerRender;
class CCVideoPlayerInitParams {
public:

    /**
     * 播放器渲染器
     */
    CCPlayerRender* Render = nullptr;

    /**
     * 同步队列最大大小
     */
    size_t MaxRenderQueueSize = 100;
    /**
     * 同步队列增长步长
     */
    size_t PacketPoolSize = 100;
    /**
     * 同步队列增长步长
     */
    size_t PacketPoolGrowStep = 10;

    /**
     * 目标格式
     */
    AVPixelFormat DestFormat = AV_PIX_FMT_RGBA;
    /**
     * 目标宽度
     */
    int DestWidth = 0;
    /**
     * 目标高度
     */
    int DestHeight = 0;

    size_t FramePoolSize = 100;
    size_t FramePoolGrowStep = 10;

    /**
     * 限制FPS
     */
    double LimitFps = 30;
};

/**
 * 播放器额外数据
 */
class CCDecodeQueue;
class CCVideoPlayerExternalData {
public:
    CCVideoPlayerInitParams *InitParams = nullptr;
    CCDecodeQueue*DecodeQueue = nullptr;
    AVCodecContext *VideoCodecContext = nullptr;
    AVCodecContext *AudioCodecContext = nullptr;
    AVFormatContext *FormatContext = nullptr;
    double CurrentFps = 0;
    int64_t StartTime = 0;
};

//解码器状态值
//***************************************

//用户可见的播放器状态值
enum class CCVideoState {
    Loading = 1,
    Failed = 2,
    NotOpen = 3,
    Playing = 4,
    Ended = 5,
    Opened = 6,
    Paused = 6,
};

enum class CCDecodeState {
    NotInit,
    Preparing,
    Ready,
    Paused,
    Finished,
    FinishedWithError,
    Decoding,
    Finish,
};

enum class CCRenderState {
    NotRender,
    Rendering,
};

#define AUDIO_DEST_SAMPLE_RATE 44100
#define AUDIO_DEST_CHANNEL_COUNTS 2
#define AUDIO_DEST_CHANNEL_LAYOUT AV_CH_LAYOUT_STEREO

//AAC 1024
#define ACC_NB_SAMPLES 1024

//播放器事件
#define PLAYER_EVENT_OPEN_DONE 1
#define PLAYER_EVENT_CLOSED 2
#define PLAYER_EVENT_PLAY_DONE 3
#define PLAYER_EVENT_OPEN_FAIED 4
#define PLAYER_EVENT_INIT_DECODER_DONE 5

//软件音量
#define SW_VOLUME_MINDB  -30
#define SW_VOLUME_MAXDB  +12

#endif //VR720_CCPLAYERDEFINE_H
