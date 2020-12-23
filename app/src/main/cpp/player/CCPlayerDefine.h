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
};

//解码器状态值
//***************************************

//用户可见的播放器状态值
enum class CCVideoState {
    Failed = 0,
    NotOpen = 1,
    Playing = 2,
    Ended = 3,
    Opened = 4,
    Paused = 4,
    Loading = 5,
};

enum class CCDecodeState {
    NotInit,
    Preparing,
    Ready,
    Decoding,
    Paused,
    Finish,
    Finished,
    FinishedWithError
};

enum class CCRenderState {
    NotRender,
    Rendering,
};

#define AUDIO_DEST_SAMPLE_RATE 44100
#define AUDIO_DEST_CHANNEL_COUNTS 2
#define AUDIO_DEST_CHANNEL_LAYOUT AV_CH_LAYOUT_STEREO

#define ACC_NB_SAMPLES 1024


#endif //VR720_CCPLAYERDEFINE_H
