//
// Created by roger on 2020/12/19.
//

#include "CCVideoPlayer.h"
#include "../utils/CStringHlp.h"
#include "../core/CCErrors.h"

#define LOG_TAG "CCVideoPlayer"

void CCVideoPlayer::CallPlayerEventCallback(int message) {
    if(videoPlayerEventCallback != nullptr)
        videoPlayerEventCallback(this, message, videoPlayerEventCallbackData);
}
int CCVideoPlayer::GetLastError() const { return lastError; }

//播放器子线程方法
//**************************

void CCVideoPlayer::DoOpenVideo() {
    LOGD(LOG_TAG, "DoOpenVideo");

    playerStatus = CCVideoState::Loading;

    if(!InitDecoder()) {
        CallPlayerEventCallback(PLAYER_EVENT_OPEN_FAIED);
        return;
    }

    decodeQueue.Init(&externalData);

    CallPlayerEventCallback(PLAYER_EVENT_INIT_DECODER_DONE);

    if(!render->Init(&externalData)) {
        CallPlayerEventCallback(PLAYER_EVENT_OPEN_FAIED);
        return;
    }

    playerStatus = CCVideoState::Opened;
    CallPlayerEventCallback(PLAYER_EVENT_OPEN_DONE);
}
void CCVideoPlayer::DoCloseVideo() {
    LOGD(LOG_TAG, "DoCloseVideo");

    SetVideoState(CCVideoState::Paused);

    DestroyDecoder();
    render->Destroy();
    decodeQueue.Destroy();

    playerStatus = CCVideoState::NotOpen;
    CallPlayerEventCallback(PLAYER_EVENT_CLOSED);
}
void CCVideoPlayer::DoSeekVideo() {

    //清空队列
    decodeQueue.ClearAll();

    //跳转到指定帧
    int ret = av_seek_frame(formatContext, audioIndex, seekPos, (uint)AVSEEK_FLAG_BACKWARD | (uint)AVSEEK_FLAG_FRAME);
    if(ret) {
        LOGEF(LOG_TAG, "av_seek_frame failed : %d", ret);
        playerSeeking = 0;
        render->SetSeekDest(-1);
        return;
    }

    LOGDF(LOG_TAG, "Seek to pos : %ld", seekPos);

    render->SetSeekDest(seekPos);

    //如果解码线程没有启动，则启动线程，解码到指定目标然后停止
    if(decodeState != CCDecodeState::Decoding) {
        LOGD(LOG_TAG, "Start all for seek");
        StartDecoderThread(true);
        render->Start(true);
    }

}

//播放器公共方法
//**************************

bool CCVideoPlayer::OpenVideo(const char *filePath) {

    if(playerStatus == CCVideoState::Loading) {
        LOGE(LOG_TAG, "Player is loading, please wait a second");
        lastError = VR_ERR_VIDEO_PLAYER_NOW_IS_LOADING;
        return false;
    }
    if(playerStatus > CCVideoState::NotOpen) {
        LOGE(LOG_TAG, "A video has been opened. Please close it first");
        lastError = VR_ERR_VIDEO_PLAYER_ALREADY_OPEN;
        return false;
    }

    currentFile = filePath;
    playerStatus = CCVideoState::Loading;

    if(playerOpen == 0) {
        playerOpen = 1;
        return true;
    }
    return true;
}
bool CCVideoPlayer::CloseVideo() {

    if(playerStatus == CCVideoState::NotOpen || playerStatus == CCVideoState::Failed) {
        LOGE(LOG_TAG, "Can not close video because video not load");
        return false;
    }
    if(playerClose == 0) {
        playerClose = 1;
        return true;
    }
    return true;
}
void CCVideoPlayer::SetVideoState(CCVideoState newState) {
    if(videoState == newState)
        return;

    switch (newState) {
        case CCVideoState::NotOpen: CloseVideo(); break;
        case CCVideoState::Playing: StartAll(); break;
        case CCVideoState::Paused: StopAll(); break;
        case CCVideoState::Ended:
        case CCVideoState::Failed:
        case CCVideoState::Loading:
            LOGEF(LOG_TAG, "Bad state %d, this state can only get.", newState);
            lastError = VR_ERR_VIDEO_PLAYER_STATE_CAN_ONLY_GET;
            return;
    }

    LOGDF(LOG_TAG, "SetVideoState : %d", newState);
}
void CCVideoPlayer::SetVideoPos(int64_t pos) {
    if(playerSeeking == 0)
        playerSeeking = 1;

    seekDest = externalData.StartTime + pos;
    seekPos = seekDest * AV_TIME_BASE / 1000;

    LOGDF(LOG_TAG, "SetVideoPos : %ld/%ld", seekDest, GetVideoLength());
}
int64_t CCVideoPlayer::GetVideoPos() {

    if(playerStatus <= CCVideoState::NotOpen) {
        lastError = VR_ERR_VIDEO_PLAYER_NOT_OPEN;
        return -1;
    }

    if(playerSeeking == 1)
        return seekDest - externalData.StartTime;

    if(!render || !formatContext)
        return -1;

    if(audioIndex != -1)
        return render->GetCurAudioPts() * av_q2d(formatContext->streams[audioIndex]->time_base) * 1000;
    else
        return render->GetCurVideoPts() * av_q2d(formatContext->streams[videoIndex]->time_base) * 1000;
}
CCVideoState CCVideoPlayer::GetVideoState() { return videoState; }
int64_t CCVideoPlayer::GetVideoLength() {
    if(!formatContext) {
        lastError = VR_ERR_VIDEO_PLAYER_NOT_OPEN;
        LOGE(LOG_TAG, "Video not open");
        return 0;
    }
    return formatContext->duration / AV_TIME_BASE * 1000; //ms
}
void CCVideoPlayer::SetVideoVolume(int vol) { render->SetVolume(vol); }
int CCVideoPlayer::GetVideoVolume() { return render->GetVolume(); }
void CCVideoPlayer::GetVideoSize(int *w, int *h) {
    if(formatContext) {
        *w = formatContext->streams[videoIndex]->codecpar->width;
        *h = formatContext->streams[videoIndex]->codecpar->height;
    }
}

void CCVideoPlayer::StartAll() {
    if(videoState == CCVideoState::Playing)
        return;
    videoState = CCVideoState::Playing;

    pthread_mutex_lock(&startAllLock);

    decoderAudioFinish = audioIndex == -1;
    decoderVideoFinish = false;
    StartDecoderThread();
    render->Start(false);

    pthread_mutex_unlock(&startAllLock);
}
void CCVideoPlayer::StopAll() {
    if(videoState == CCVideoState::Paused || videoState == CCVideoState::Ended)
        return;
    videoState = CCVideoState::Paused;

    pthread_mutex_lock(&stopAllLock);

    StopDecoderThread();
    render->Pause();

    pthread_mutex_unlock(&stopAllLock);
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
        if (av_strerror(openState, errBuf, sizeof(errBuf)) == 0) {
            LOGEF(LOG_TAG, "Failed to open input file, error : %s", errBuf);
            lastError = VR_ERR_VIDEO_PLAYER_AV_ERROR;
        }
        return false;
    }
    //为分配的AVFormatContext 结构体中填充数据
    if (avformat_find_stream_info(formatContext, nullptr) < 0) {
        lastError = VR_ERR_VIDEO_PLAYER_AV_ERROR;
        LOGE(LOG_TAG, "Failed to read the input video stream information");
        return false;
    }

    videoIndex = -1;
    audioIndex = -1;

    //LOGD(LOG_TAG, "---------------- File Information ---------------");
    //av_dump_format(formatContext, 0, currentFile.c_str(), 0);
    //LOGD(LOG_TAG, "-------------- File Information end -------------");

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
        lastError = VR_ERR_VIDEO_PLAYER_NO_VIDEO_STREAM;
        LOGE(LOG_TAG, "Not found video stream!");
        return false;
    }

    LOGDF(LOG_TAG, "formatContext->nb_streams : %d", formatContext->nb_streams);
    LOGDF(LOG_TAG, "audioIndex : %d", audioIndex);
    LOGDF(LOG_TAG, "videoIndex : %d", videoIndex);

    externalData.VideoTimeBase = formatContext->streams[videoIndex]->time_base;
    if(audioIndex != -1)
        externalData.AudioTimeBase = formatContext->streams[audioIndex]->time_base;

    //FPS
    externalData.CurrentFps = av_q2d(formatContext->streams[videoIndex]->r_frame_rate);
    if(externalData.CurrentFps < InitParams.LimitFps) externalData.CurrentFps = InitParams.LimitFps;

    //初始化视频解码器
    //***********************************

    AVCodecParameters *codecParameters=formatContext->streams[videoIndex]->codecpar;
    videoCodec = avcodec_find_decoder(codecParameters->codec_id);
    if (videoCodec == nullptr) {
        LOGE(LOG_TAG, "Not find video decoder");
        lastError = VR_ERR_VIDEO_PLAYER_VIDEO_NOT_SUPPORT;
        return false;
    }

    //通过解码器分配(并用  默认值   初始化)一个解码器context
    videoCodecContext = avcodec_alloc_context3(videoCodec);
    if (videoCodecContext == nullptr) {
        LOGE(LOG_TAG, "avcodec_alloc_context3 for videoCodecContext failed");
        lastError = VR_ERR_VIDEO_PLAYER_AV_ERROR;
        return false;
    }

    //更具指定的编码器值填充编码器上下文
    ret = avcodec_parameters_to_context(videoCodecContext, codecParameters);
    if(ret < 0) {
        LOGEF(LOG_TAG, "avcodec_parameters_to_context videoCodecContext failed : %d", ret);
        lastError = VR_ERR_VIDEO_PLAYER_AV_ERROR;
        return false;
    }
    ret = avcodec_open2(videoCodecContext, videoCodec, nullptr);
    //通过所给的编解码器初始化编解码器上下文
    if (ret < 0) {
        LOGEF(LOG_TAG, "avcodec_open2 videoCodecContext failed : %d", ret);
        lastError = VR_ERR_VIDEO_PLAYER_AV_ERROR;
        return false;
    }

    //初始化音频解码器
    //***********************************

    if(audioIndex > -1) {

        codecParameters = formatContext->streams[audioIndex]->codecpar;
        audioCodec = avcodec_find_decoder(codecParameters->codec_id);
        if (audioCodec == nullptr) {
            LOGW(LOG_TAG, "Not find audio decoder");
            goto AUDIO_INIT_DONE;
        }
        //通过解码器分配(并用  默认值   初始化)一个解码器context
        audioCodecContext = avcodec_alloc_context3(audioCodec);
        if (audioCodecContext == nullptr) {
            LOGW(LOG_TAG, "avcodec_alloc_context3 for audioCodecContext failed");
            goto AUDIO_INIT_DONE;
        }

        //更具指定的编码器值填充编码器上下文
        ret = avcodec_parameters_to_context(audioCodecContext, codecParameters);
        if(ret < 0) {
            LOGWF(LOG_TAG, "avcodec_parameters_to_context audioCodecContext failed : %d", ret);
            goto AUDIO_INIT_DONE;
        }
        ret = avcodec_open2(audioCodecContext, audioCodec, nullptr);
        //通过所给的编解码器初始化编解码器上下文
        if (ret < 0) {
            LOGWF(LOG_TAG, "avcodec_open2 audioCodecContext failed : %d", ret);
            goto AUDIO_INIT_DONE;
        }

    }
    else audioCodecContext = nullptr;
AUDIO_INIT_DONE:

    LOGDF(LOG_TAG, "audioCodecContext->codec_id : %d", audioCodecContext ? audioCodecContext->codec_id : 0);
    LOGDF(LOG_TAG, "videoCodecContext->codec_id : %d", videoCodecContext->codec_id);

    externalData.StartTime = formatContext->start_time * 1000 / AV_TIME_BASE;

    externalData.AudioCodecContext = audioCodecContext;
    externalData.VideoCodecContext = videoCodecContext;
    externalData.FormatContext = formatContext;

    decodeState = CCDecodeState::Ready;
    return true;
}
bool CCVideoPlayer::DestroyDecoder() {

    if(decodeState < CCDecodeState::Ready) {
        lastError = VR_ERR_VIDEO_PLAYER_NOR_INIT;
        LOGE(LOG_TAG, "Decoder not init");
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

    decodeState = CCDecodeState::NotInit;
    return true;
}

//解码器线程控制
//**************************

void CCVideoPlayer::StartDecoderThread(bool isStartBySeek) {

    decodeState = isStartBySeek ? CCDecodeState::DecodingToSeekPos : CCDecodeState::Decoding;

    pthread_create(&decoderWorkerThread, nullptr, DecoderWorkerThreadStub, this);
    pthread_create(&decoderVideoThread, nullptr, DecoderVideoThreadStub, this);
    pthread_create(&decoderAudioThread, nullptr, DecoderAudioThreadStub, this);
}
void CCVideoPlayer::StopDecoderThread() {
    decodeState = CCDecodeState::Ready;

    void* retVal;
    if(decoderWorkerThread)
        pthread_join(decoderWorkerThread, &retVal);
    if(decoderVideoThread)
        pthread_join(decoderVideoThread, &retVal);
    if(decoderAudioThread)
        pthread_join(decoderAudioThread, &retVal);

    decoderWorkerThread = 0;
    decoderAudioThread = 0;
    decoderVideoThread = 0;
}

//线程入口包装函数

void* CCVideoPlayer::PlayerWorkerThreadStub(void *param) {
    prctl(PR_SET_NAME, "PlayerWorker");
    return ((CCVideoPlayer*)param)->PlayerWorkerThread();
}
void* CCVideoPlayer::DecoderWorkerThreadStub(void *param) {
    prctl(PR_SET_NAME, "DecoderWorker");
    return ((CCVideoPlayer*)param)->DecoderWorkerThread();
}
void* CCVideoPlayer::DecoderVideoThreadStub(void *param) {
    prctl(PR_SET_NAME, "DecoderVideo");
    return ((CCVideoPlayer*)param)->DecoderVideoThread();
}
void* CCVideoPlayer::DecoderAudioThreadStub(void *param) {
    prctl(PR_SET_NAME, "DecoderAudio");
    return ((CCVideoPlayer*)param)->DecoderAudioThread();
}

//线程主函数

void* CCVideoPlayer::PlayerWorkerThread() {
    //背景线程，用于防止用户主线程卡顿
    LOGI(LOG_TAG, "PlayerWorkerThread : Start");

    while (playerWorking) {

        if(playerClose == 1) {
            playerClose = 2;
            DoCloseVideo();
            playerClose = 0;
        }
        if(playerOpen == 1) {
            playerOpen = 2;
            DoOpenVideo();
            playerOpen = 0;
        }
        if(playerSeeking == 1) {
            playerSeeking = 2;
            DoSeekVideo();
        }
        if(playerSeeking != 2 && decoderVideoFinish && decoderAudioFinish && decodeState != CCDecodeState::Finished) {
            int64_t pos = GetVideoPos();
            if(pos >= GetVideoLength() - 10 || pos == -1) {

                if(decodeState == CCDecodeState::DecodingToSeekPos)
                    forceStopSeek = true;

                decodeState = CCDecodeState::Finished;
                playerStatus = CCVideoState::Ended;

                decodeQueue.ClearAll();//清空数据
                StopAll();

                CallPlayerEventCallback(PLAYER_EVENT_PLAY_DONE);
                LOGI(LOG_TAG, "decodeState -> Finished");
            }
        }
        if(render->IsCurrentSeekToPosFinished() || forceStopSeek) {
            forceStopSeek = false;
            StopDecoderThread();
            render->Pause();
            playerSeeking = 0;
        }

        av_usleep(100 * 1000);
    }

    LOGI(LOG_TAG, "PlayerWorkerThread : End");
    return nullptr;
}
void* CCVideoPlayer::DecoderWorkerThread() {
    //读取线程，解复用线程
    int ret;
    LOGI(LOG_TAG, "DecoderWorkerThread : Start");

    while (decodeState == CCDecodeState::Decoding || decodeState == CCDecodeState::DecodingToSeekPos) {

        uint maxMaxRenderQueueSize = (decodeState == CCDecodeState::DecodingToSeekPos ? 8 : InitParams.MaxRenderQueueSize);
        if (decodeQueue.AudioQueueSize() > maxMaxRenderQueueSize && decodeQueue.VideoQueueSize() > maxMaxRenderQueueSize) {
            av_usleep(1000 * 20);
            continue;
        } else
            av_usleep(1000);

        AVPacket *avPacket = decodeQueue.RequestPacket();
        ret = av_read_frame(formatContext, avPacket);
        //等于0成功，其它失败
        if (ret == 0) {
            if (avPacket->stream_index == audioIndex)
                decodeQueue.AudioEnqueue(avPacket);//添加音频包至队列中
            else if (avPacket->stream_index == videoIndex)
                decodeQueue.VideoEnqueue(avPacket);//添加视频包至队列中
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
            LOGEF("DecoderWorkerThread", "av_read_frame failed : %d", ret);
            decodeQueue.ReleasePacket(avPacket);
            decodeState = CCDecodeState::FinishedWithError;
            break;
        }
    }

    LOGI(LOG_TAG, "DecoderWorkerThread : End");
    return nullptr;
}
void* CCVideoPlayer::DecoderVideoThread() {
    //视频解码线程
    LOGI(LOG_TAG, "DecoderVideoThread : Start");

    int ret;
    AVPacket *packet;
    while (decodeState >= CCDecodeState::Decoding) {

        if(playerSeeking != 2 && decodeQueue.VideoFrameQueueSize() >
            (decodeState == CCDecodeState::DecodingToSeekPos ? 10 : 50)) {
            usleep(1000 * 10);
            continue;
        }
        else usleep(1000);

        packet = decodeQueue.VideoDequeue();
        if (!packet) {

            //如果主线程标记已经结束，那么没有收到包即意味着结束，退出线程
            if(!decoderVideoFinish && decodeState == CCDecodeState::Finish) {
                decoderVideoFinish = true;
                LOGI(LOG_TAG, "DecoderVideoThread : End by Finish");
                return nullptr;
            }
            if(decoderVideoFinish)
                return nullptr;

            usleep(1000 * 2);
            continue;
        }

        //把包丢给解码器
        ret = avcodec_send_packet(videoCodecContext, packet);
        decodeQueue.ReleasePacket(packet);

        if (ret != 0) {
            LOGEF(LOG_TAG, "DecoderVideoThread : avcodec_send_packet failed : %d", ret);
            break;
        }

        AVFrame *frame = decodeQueue.RequestFrame();
        ret = avcodec_receive_frame(videoCodecContext, frame);
        if (ret == AVERROR(EAGAIN)) {
            continue;
        }
        else if (ret != 0) {
            LOGEF(LOG_TAG, "DecoderVideoThread : avcodec_receive_frame failed : %d", ret);
            break;
        }
        //再开一个线程 播放。
        decodeQueue.VideoFrameEnqueue(frame);
    }

    LOGI(LOG_TAG, "DecoderVideoThread : End");
    return nullptr;
}
void* CCVideoPlayer::DecoderAudioThread() {
    //音频解码线程
    LOGI(LOG_TAG, "DecoderAudioThread : Start");

    int ret;
    AVPacket *packet;
    while (decodeState >= CCDecodeState::Decoding) {

        if(playerSeeking != 2 &&
            decodeQueue.AudioFrameQueueSize() > (decodeState == CCDecodeState::DecodingToSeekPos ? 10 : 50)) {
            usleep(1000 * 10);
            continue;
        }
        else usleep(1000 * 10);

        packet = decodeQueue.AudioDequeue();
        if (!packet) {

            //如果主线程标记已经结束，那么没有收到包即意味着结束，退出线程
            if(!decoderAudioFinish && decodeState == CCDecodeState::Finish) {
                decoderAudioFinish = true;
                LOGI(LOG_TAG, "DecoderAudioThread : End by Finish");
                return nullptr;
            }
            if(decoderAudioFinish)
                return nullptr;

            usleep(1000 * 5);
            continue;
        }

        //把包丢给解码器
        ret = avcodec_send_packet(audioCodecContext, packet);
        decodeQueue.ReleasePacket(packet);

        if (ret != 0) {
            LOGEF(LOG_TAG, "DecoderAudioThread : avcodec_send_packet failed : %d", ret);
            break;
        }

        AVFrame *frame = decodeQueue.RequestFrame();
        ret = avcodec_receive_frame(audioCodecContext, frame);
        if (ret == AVERROR(EAGAIN))
            continue;
        else if (ret != 0) {
            LOGEF(LOG_TAG, "DecoderAudioThread : avcodec_receive_frame failed : %d", ret);
            break;
        }

        decodeQueue.AudioFrameEnqueue(frame);
    }

    LOGI(LOG_TAG, "DecoderAudioThread : End");
    return nullptr;
}

CCVideoPlayer::CCVideoPlayer() {
    Init(nullptr);
}
CCVideoPlayer::CCVideoPlayer(CCVideoPlayerInitParams *initParams) {
    Init(initParams);
}
CCVideoPlayer::~CCVideoPlayer() {
    Destroy();
}
void CCVideoPlayer::Init(CCVideoPlayerInitParams *initParams) {
    if(initParams != nullptr)
        memcpy(&InitParams, initParams, sizeof(CCVideoPlayerInitParams));
    if (InitParams.Render == nullptr)
       LOGE("CCVideoPlayer", "InitParams.Render not set! ") ;
    render = InitParams.Render;
    externalData.InitParams = &InitParams;
    externalData.DecodeQueue = &decodeQueue;

    playerWorking = true;
    pthread_create(&playerWorkerThread, nullptr, PlayerWorkerThreadStub, this);

    //av_log

    av_log_set_level(AV_LOG_DEBUG);
    av_log_set_callback(FFmpegLogFunc);

    //mutex
    pthread_mutex_init(&startAllLock, nullptr);
    pthread_mutex_init(&stopAllLock, nullptr);
}
void CCVideoPlayer::Destroy() {

    if(playerStatus > CCVideoState::NotOpen)
        DoCloseVideo();

    playerWorking = false;
    void* retVal;
    if(playerWorkerThread)
        pthread_join(playerWorkerThread, &retVal);
    playerWorkerThread = 0;

    pthread_mutex_destroy(&startAllLock);
    pthread_mutex_destroy(&stopAllLock);
}
void CCVideoPlayer::GlobalInit() {

}

void CCVideoPlayer::FFmpegLogFunc(void* ptr, int level, const char* fmt, va_list vl) {
    UNREFERENCED_PARAMETER(ptr);

    std::string str = CStringHlp::FormatString(fmt, vl);
    switch (level) {
        case AV_LOG_DEBUG:
            __android_log_print(ANDROID_LOG_DEBUG, "FFmpeg", "%s", str.c_str());
            break;
        case AV_LOG_INFO:
            __android_log_print(ANDROID_LOG_INFO, "FFmpeg", "%s", str.c_str());
            break;
        case AV_LOG_WARNING:
            __android_log_print(ANDROID_LOG_WARN, "FFmpeg", "%s", str.c_str());
            break;
        case AV_LOG_ERROR:
            __android_log_print(ANDROID_LOG_ERROR, "FFmpeg", "%s", str.c_str());
            break;
        default:
            __android_log_print(ANDROID_LOG_DEFAULT, "FFmpeg", "%s", str.c_str());
            break;
    }

}










