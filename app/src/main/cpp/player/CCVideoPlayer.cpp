//
// Created by roger on 2020/12/19.
//

#include "CCVideoPlayer.h"
#include "../utils/CStringHlp.h"

void CCVideoPlayer::CallPlayerEventCallback(int message) {
    if(videoPlayerEventCallback != nullptr)
        videoPlayerEventCallback(this, message, videoPlayerEventCallbackData);
}
void CCVideoPlayer::SetLastError(const char *str) {
    lastError = str;
    LOGEF("[CCVideoPlayer] %s", str);
}
const char *CCVideoPlayer::GetLastError() {
    return lastError.c_str();
}

bool CCVideoPlayer::OpenVideo(char *filePath) {

    if(playerStatus == CCVideoState::Loading) {
        SetLastError("Player is loading, please wait a second");
        return false;
    }
    if(playerStatus > CCVideoState::NotOpen) {
        SetLastError("A video has been opened. Please close it first");
        return false;
    }

    currentFile = filePath;
    playerStatus = CCVideoState::Loading;

    if(!InitDecoder())
        return false;

    decodeQueue.Init(&externalData);
    if(!render->Init(&externalData))
        return false;

    playerStatus = CCVideoState::Opened;
    return true;
}
bool CCVideoPlayer::CloseVideo() {

    if(playerStatus == CCVideoState::NotOpen || playerStatus == CCVideoState::Failed) {
        SetLastError("Can not close video because video not load");
        return false;
    }

    SetVideoState(CCVideoState::Paused);

    DestroyDecoder();

    render->Destroy();

    decodeQueue.Destroy();

    playerStatus = CCVideoState::NotOpen;
    return true;
}

//播放器公共方法
//**************************

void CCVideoPlayer::SetVideoState(CCVideoState newState) {
    if(videoState == newState)
        return;

    switch (videoState) {
        case CCVideoState::NotOpen: CloseVideo(); break;
        case CCVideoState::Playing:
            StartAll();
            break;
        case CCVideoState::Paused:
            StopAll();
            break;
        case CCVideoState::Ended:
        case CCVideoState::Failed:
        case CCVideoState::Loading:
            SetLastError(CStringHlp::FormatString("Bad state %d, this state can only get.", newState).c_str());
            return;
    }
}
void CCVideoPlayer::SetVideoPos(int64_t pos) {

}
int64_t CCVideoPlayer::GetVideoPos() {
    return 0;
}
CCVideoState CCVideoPlayer::GetVideoState() {
    return videoState;
}
int64_t CCVideoPlayer::GetVideoLength() {
    if(!formatContext) {
        SetLastError("Video not open");
        return 0;
    }
    return formatContext->duration / AV_TIME_BASE * 1000;
}
void CCVideoPlayer::SetVideoVolume(int vol) {

}
int CCVideoPlayer::GetVideoVolume() {
    return 0;
}

void CCVideoPlayer::StartAll() {
    videoState = CCVideoState::Playing;
    StartDecoderThread();
    render->Start();
}
void CCVideoPlayer::StopAll() {
    videoState = CCVideoState::Paused;
    StopDecoderThread();
    render->Pause();
}


//解码器初始化与反初始化
//**************************

bool CCVideoPlayer::InitDecoder() {

    int ret;
    decodeState = CCDecodeState::Preparing;

    formatContext = avformat_alloc_context();
    //打开视频数据源。由于Android 对SDK存储权限的原因，如果没有为当前项目赋予SDK存储权限，打开本地视频文件时会失败
    int openState = avformat_open_input(&formatContext, currentFile.c_str(), nullptr, nullptr);
    if (openState < 0) {
        char errBuf[128];
        if (av_strerror(openState, errBuf, sizeof(errBuf)) == 0)
            SetLastError(CStringHlp::FormatString("Failed to open input file, error : %s", errBuf).c_str());
        return false;
    }
    //为分配的AVFormatContext 结构体中填充数据
    if (avformat_find_stream_info(formatContext, nullptr) < 0) {
        SetLastError("Failed to read the input video stream information.");
        return false;
    }

    videoIndex = -1;
    audioIndex = -1;

    LOGDF("[CCVideoPlayer] formatContext->nb_streams : %d", formatContext->nb_streams);

    //找到"视频流".AVFormatContext 结构体中的nb_streams字段存储的就是当前视频文件中所包含的总数据流数量——
    //视频流，音频流
    //***********************************

    for (int i = 0; i < formatContext->nb_streams; i++) {
        if (formatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoIndex = i;
            break;
        }
    }
    for (int i = 0; i < formatContext->nb_streams; i++) {
        if (formatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioIndex = i;
            break;
        }
    }
    if (videoIndex == -1) {
        SetLastError("Not found video stream!");
        return false;
    }

    //初始化视频解码器
    //***********************************

    AVCodecParameters *codecParameters=formatContext->streams[videoIndex]->codecpar;
    videoCodec = avcodec_find_decoder(codecParameters->codec_id);
    if (videoCodec == nullptr) {
        SetLastError("Not find video decoder");
        return false;
    }

    //通过解码器分配(并用  默认值   初始化)一个解码器context
    videoCodecContext = avcodec_alloc_context3(videoCodec);
    if (videoCodecContext == nullptr) {
        SetLastError("avcodec_alloc_context3 for videoCodecContext failed");
        return false;
    }

    //更具指定的编码器值填充编码器上下文
    ret = avcodec_parameters_to_context(videoCodecContext, codecParameters);
    if(ret < 0) {
        SetLastError(CStringHlp::FormatString("avcodec_parameters_to_context videoCodecContext failed : %d", ret).c_str());
        return false;
    }
    ret = avcodec_open2(videoCodecContext, videoCodec, nullptr);
    //通过所给的编解码器初始化编解码器上下文
    if (ret < 0) {
        SetLastError(CStringHlp::FormatString("avcodec_open2 videoCodecContext failed : %d",ret).c_str());
        return false;
    }

    //初始化音频解码器
    //***********************************

    if(audioIndex > -1) {

        codecParameters = formatContext->streams[audioIndex]->codecpar;
        audioCodec = avcodec_find_decoder(codecParameters->codec_id);
        if (audioCodec == nullptr) {
            LOGW("[CCVideoPlayer::InitDecoder] Not find audio decoder");
            goto AUDIO_INIT_DONE;
        }
        //通过解码器分配(并用  默认值   初始化)一个解码器context
        audioCodecContext = avcodec_alloc_context3(audioCodec);
        if (audioCodecContext == nullptr) {
            LOGW("[CCVideoPlayer] avcodec_alloc_context3 for audioCodecContext failed");
            goto AUDIO_INIT_DONE;
        }

        //更具指定的编码器值填充编码器上下文
        ret = avcodec_parameters_to_context(audioCodecContext, codecParameters);
        if(ret < 0) {
            LOGWF("[CCVideoPlayer] avcodec_parameters_to_context audioCodecContext failed : %d", ret);
            goto AUDIO_INIT_DONE;
        }
        ret = avcodec_open2(audioCodecContext, audioCodec, nullptr);
        //通过所给的编解码器初始化编解码器上下文
        if (ret < 0) {
            LOGWF("[CCVideoPlayer] avcodec_open2 audioCodecContext failed : %d", ret);
            goto AUDIO_INIT_DONE;
        }

    }
AUDIO_INIT_DONE:

    externalData.AudioCodecContext = audioCodecContext;
    externalData.VideoCodecContext = videoCodecContext;
    externalData.FormatContext = formatContext;

    decodeState = CCDecodeState::Ready;
    return true;
}
bool CCVideoPlayer::DestroyDecoder() {

    if(decodeState < CCDecodeState::Ready) {
        SetLastError("Decoder not init");
        return false;
    }

    //停止线程
    StopDecoderThread();

    //释放资源
    avcodec_free_context(&videoCodecContext);
    if(audioIndex != -1)
        avcodec_free_context(&audioCodecContext);
    avformat_close_input(&formatContext);
    avformat_free_context(formatContext);

    return true;
}

//解码器线程控制
//**************************

void CCVideoPlayer::StartDecoderThread() {

    decodeState = CCDecodeState::Decoding;

    pthread_create(&decoderWorkerThread, nullptr, DecoderWorkerThreadStub, this);
    pthread_create(&decoderVideoThread, nullptr, DecoderVideoThreadStub, this);
    pthread_create(&decoderAudioThread, nullptr, DecoderAudioThreadStub, this);
}
void CCVideoPlayer::StopDecoderThread() {
    decodeState = CCDecodeState::Ready;

    void* retVal;
    pthread_join(decoderWorkerThread, &retVal);
    pthread_join(decoderVideoThread, &retVal);
    pthread_join(decoderAudioThread, &retVal);
}

void* CCVideoPlayer::DecoderWorkerThreadStub(void *param) {
    return ((CCVideoPlayer*)param)->DecoderWorkerThread();
}
void* CCVideoPlayer::DecoderVideoThreadStub(void *param) {
    return ((CCVideoPlayer*)param)->DecoderVideoThread();
}
void* CCVideoPlayer::DecoderAudioThreadStub(void *param) {
    return ((CCVideoPlayer*)param)->DecoderAudioThread();
}

void* CCVideoPlayer::DecoderWorkerThread() {
    int ret;

    while (decodeState == CCDecodeState::Decoding) {

        if (decodeQueue.AudioQueueSize() > InitParams.MaxRenderQueueSize
            && decodeQueue.VideoQueueSize() > InitParams.MaxRenderQueueSize) {
            av_usleep(1000 * 15);
            continue;
        }

        AVPacket *avPacket = decodeQueue.RequestPacket();
        ret = av_read_frame(formatContext, avPacket);
        //等于0成功，其它失败
        if (ret == 0) {
            if (avPacket->stream_index == audioIndex) {
                //添加音频包至队列中
                if (decodeQueue.AudioQueueSize() > InitParams.MaxRenderQueueSize) {
                    decodeQueue.ReleasePacket(avPacket);
                    av_usleep(1000 * 10);
                    continue;
                }
                decodeQueue.AudioEnqueue(avPacket);
            }
            else if (avPacket->stream_index == videoIndex) {
                //添加视频包至队列中
                if (decodeQueue.VideoQueueSize() > InitParams.MaxRenderQueueSize) {
                    decodeQueue.ReleasePacket(avPacket);
                    av_usleep(1000 * 10);
                    continue;
                }
                decodeQueue.VideoEnqueue(avPacket);
            }
        }
        else if (ret == AVERROR_EOF) {
            //读取完成，但是可能还没有播放完成
            if (decodeQueue.AudioQueueSize() == 0
                && decodeQueue.VideoQueueSize() == 0) {
                decodeQueue.ReleasePacket(avPacket);
                decodeState = CCDecodeState::Finish;
                break;
            }
        }
        else {
            LOGEF("[CCVideoPlayer::DecoderWorkerThread] av_read_frame failed : %d", ret);
            decodeQueue.ReleasePacket(avPacket);
            decodeState = CCDecodeState::FinishedWithError;
            break;
        }
    }

    return nullptr;
}
void* CCVideoPlayer::DecoderVideoThread() {
    int ret;
    AVPacket *packet;
    while (decodeState == CCDecodeState::Decoding) {

        if(decodeQueue.VideoQueueSize() > 50) {
            usleep(1000 * 10);
            continue;
        }

        packet = decodeQueue.VideoDequeue();
        if (!packet) {
            usleep(1000 * 5);
            continue;
        }

        //把包丢给解码器
        ret = avcodec_send_packet(videoCodecContext, packet);
        decodeQueue.ReleasePacket(packet);

        if (ret != 0)
            break;

        AVFrame *frame = decodeQueue.RequestFrame();
        ret = avcodec_receive_frame(videoCodecContext, frame);
        if (ret == AVERROR(EAGAIN)) {
            continue;
        } else if (ret != 0) {
            break;
        }
        //再开一个线程 播放。
        decodeQueue.VideoFrameEnqueue(frame);
    }
    decodeQueue.ReleasePacket(packet);

    return nullptr;
}
void* CCVideoPlayer::DecoderAudioThread() {
    int ret;
    AVPacket *packet;
    while (decodeState == CCDecodeState::Decoding) {

        if(decodeQueue.AudioQueueSize() > 50) {
            usleep(1000 * 10);
            continue;
        }

        packet = decodeQueue.AudioDequeue();
        if (!packet) {
            usleep(1000 * 5);
            continue;
        }

        //把包丢给解码器
        ret = avcodec_send_packet(audioCodecContext, packet);
        decodeQueue.ReleasePacket(packet);

        if (ret != 0)
            break;

        AVFrame *frame = decodeQueue.RequestFrame();
        ret = avcodec_receive_frame(audioCodecContext, frame);
        if (ret == AVERROR(EAGAIN)) {
            continue;
        } else if (ret != 0) {
            break;
        }
        //再开一个线程 播放。
        decodeQueue.AudioFrameEnqueue(frame);
    }
    decodeQueue.ReleasePacket(packet);

    return nullptr;
}

CCVideoPlayer::CCVideoPlayer() {
    Init(nullptr);
}
CCVideoPlayer::CCVideoPlayer(CCVideoPlayerInitParams *initParams) {
    Init(initParams);
}
void CCVideoPlayer::Init(CCVideoPlayerInitParams *initParams) {
    if(initParams != nullptr)
        memcpy(&InitParams, initParams, sizeof(CCVideoPlayerInitParams));
    if (InitParams.Render == nullptr)
       LOGE("[CCVideoPlayer] InitParams.Render not set! ") ;
    externalData.InitParams = &InitParams;
    externalData.DecodeQueue = &decodeQueue;
}
void CCVideoPlayer::GlobalInit() {

}






