//
// Created by roger on 2020/12/19.
//

#include "CCVideoPlayer.h"

static const AVRational TIMEBASE_MS = { 1, 1000 };

bool CCVideoPlayer::InitDecoder() {
    int ret = 0;
    AVRational vrate = { 20, 1 };
    AVDictionary *opts = nullptr;

    //初始配置
    if (InitParams.videoVWidth != 0 && InitParams.videoVHeight != 0) {
        char vsize[64];
        sprintf(vsize, "%dx%d", InitParams.videoVWidth, InitParams.videoVHeight);
        av_dict_set(&opts, "video_size", vsize, 0);
    }
    if (InitParams.videoFrameRate != 0) {
        char frate[64];
        sprintf(frate, "%d", InitParams.videoFrameRate);
        av_dict_set(&opts, "framerate" , frate, 0);
    }

    //分配一个AVFormatContext结构
    pFormatCtx = avformat_alloc_context();
    //打开文件
    ret = avformat_open_input(&pFormatCtx, currentFile.c_str(), NULL, &opts);
    if(ret != 0){
        LOGEF("[CCVideoPlayer] Couldn't open input stream : %d", ret);
        SetLastError("Couldn't open input stream.");
        return false;
    }
    //查找文件的流信息
    ret = avformat_find_stream_info(pFormatCtx,NULL);
    if(ret < 0){
        LOGEF("[CCVideoPlayer] Couldn't find stream information : %d", ret);
        SetLastError("Couldn't find stream information.");
        return false;
    }

    //在流信息中找到视频流
    videoIndex = -1;
    audioIndex = -1;

    for(int i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoIndex = i;
            break;
        }
    }
    for(int i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioIndex = i;
            break;
        }
    }

    if(videoIndex == -1) {
        LOGE("[CCVideoPlayer] Couldn't find a video stream.");
        SetLastError("Couldn't find a video stream");
        return false;
    }

    //获取相应视频流的解码器
    vCodec = avcodec_find_decoder(pFormatCtx->streams[videoIndex]->codecpar->codec_id);
    vStreamTimebase = pFormatCtx->streams[videoIndex]->time_base;
    vCodecCtx = avcodec_alloc_context3(vCodec);

    //vrate 设置
    vrate = pFormatCtx->streams[videoIndex]->r_frame_rate;
    if (vrate.num / vrate.den > 100) {
        vrate.num = 20;
        vrate.den = 1;
    }

    //获取解码器
    if(audioIndex != -1) {
        aCodec = avcodec_find_decoder(pFormatCtx->streams[audioIndex]->codecpar->codec_id);
        aStreamTimebase = pFormatCtx->streams[audioIndex]->time_base;
        aCodecCtx = avcodec_alloc_context3(aCodec);
    }
    if(vCodec == NULL) {
        LOGE("[CCVideoPlayer] Couldn't find decoder.");
        SetLastError("Couldn't find decoder");
        return false;
    }

    //拷贝参数
    if(audioIndex != -1) {
        ret = avcodec_parameters_to_context(aCodecCtx, pFormatCtx->streams[audioIndex]->codecpar);
        if (ret < 0) {
            LOGEF("avcodec_parameters_to_context audio failed : %d", ret);
            return false;
        }
    }
    ret = avcodec_parameters_to_context(vCodecCtx, pFormatCtx->streams[videoIndex]->codecpar);
    if (ret < 0) {
        LOGEF("avcodec_parameters_to_context vedio failed : %d", ret);
        return false;
    }

    //打开解码器
    if(audioIndex != -1) {
        //打开音频解码器
        if(avcodec_open2(aCodecCtx, aCodec,NULL)<0){
            LOGE("[CCVideoPlayer] Couldn't open audio codec.");
            SetLastError("Couldn't open audio codec");
            return false;
        }
    }
    //打开视频解码器
    if(avcodec_open2(vCodecCtx, vCodec,NULL)<0){
        LOGE("[CCVideoPlayer] Couldn't open video codec.");
        SetLastError("Couldn't open video codec");
        return false;
    }

    //参数设置
    if (aCodecCtx->channel_layout == 0)
        aCodecCtx->channel_layout = av_get_default_channel_layout(aCodecCtx->channels);

    width = vCodecCtx->width;
    height = vCodecCtx->height;

    // calculate start_time, apts & vpts
    internalData.start_time = pFormatCtx->start_time * 1000 / AV_TIME_BASE;
    internalData.apts = audioIndex != -1 ? internalData.start_time : -1;
    internalData.vpts = videoIndex != -1 ? internalData.start_time : -1;

    //player info
    InitParams.videoFrameRate = vrate.num / vrate.den;
    InitParams.videoStreamTotal = GetStreamTotal(AVMEDIA_TYPE_VIDEO);
    InitParams.audioChannels = aCodecCtx ? av_get_channel_layout_nb_channels(aCodecCtx->channel_layout) : 0;
    InitParams.audioSampleRate = aCodecCtx ? aCodecCtx->sample_rate : 0;
    InitParams.audioStreamTotal = GetStreamTotal(AVMEDIA_TYPE_AUDIO);
    InitParams.videoCodecId = pFormatCtx->video_codec_id;

    //初始化队列
    decodeQueue.Init(0, &internalData);

    // send player init message
    //TODO:  player_send_message(player->cmnvars.winmsg, ret ? MSG_OPEN_FAILED : MSG_OPEN_DONE, (int64_t)player);
    return true;
}
void CCVideoPlayer::InitThread() {
    pthread_create(&avdemux_thread, NULL, av_demux_thread_proc, this);
    pthread_create(&adecode_thread, NULL, audio_decode_thread_proc, this);
    pthread_create(&vdecode_thread, NULL, video_decode_thread_proc, this);
}
bool CCVideoPlayer::DestroyDecoder() {

    avcodec_close(vCodecCtx);
    avcodec_close(aCodecCtx);
    avformat_close_input(&pFormatCtx);
    vCodecCtx = nullptr;
    aCodecCtx = nullptr;
    pFormatCtx = nullptr;

    //结束线程 wait audio/video demuxing thread exit
    if (avdemux_thread) pthread_join(avdemux_thread, NULL);
    if (adecode_thread) pthread_join(adecode_thread, NULL);
    if (vdecode_thread) pthread_join(vdecode_thread, NULL);

    //释放队列
    decodeQueue.Destroy();

    return true;
}
void CCVideoPlayer::StartDecoder() {
    decoderFlags = (DECODER_FLAG_PS_A_PAUSE|DECODER_FLAG_PS_V_PAUSE|DECODER_FLAG_PS_R_PAUSE);
}
void CCVideoPlayer::StopDecoder() {
    decoderFlags |= DECODER_FLAG_PS_CLOSE;
}
void CCVideoPlayer::GlobalInit() {
}

void CCVideoPlayer::SetLastError(const char *str) { strcpy(lastError, str); }
const char *CCVideoPlayer::GetLastError() { return lastError; }
int CCVideoPlayer::GetStreamTotal(enum AVMediaType type) {
    int total, i;
    for (i=0,total=0; i<(int)pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codecpar->codec_type == type) {
            total++;
        }
    }
    return total;
}

void* CCVideoPlayer::av_demux_thread_proc(void *param) {

    auto player = (CCVideoPlayer*)param;
    AVPacket *packet;
    int retv = 0;

    // async prepare player
    if (!player->InitParams.openSyncmode) {
        retv = player->InitDecoder();
        if (retv != 0) 
            goto done;
    }

    while (!(player->decoderFlags & DECODER_FLAG_PS_CLOSE)) {
        //++ when player seek ++//
        if (player->decoderFlags & (DECODER_FLAG_PS_F_SEEK|DECODER_FLAG_PS_RECONNECT)) {
            player->HandleFSeekOrReconnect((player->decoderFlags & DECODER_FLAG_PS_RECONNECT) ? 1 : 0);
            if (!player->pFormatCtx) { av_usleep(20*1000); continue; }
            player->decoderFlags &= ~(DECODER_FLAG_PS_F_SEEK|DECODER_FLAG_PS_RECONNECT);
        }

        //-- when player seek --//
        packet = player->decodeQueue.RequestPacket();
        if (packet == nullptr)
            continue;

        retv = av_read_frame(player->pFormatCtx, packet);
        if (retv < 0) {
            player->decodeQueue.ReleasePacket(packet);
            if ( player->InitParams.autoReconnect > 0
                  && av_gettime_relative() - player->readTimeLast > player->InitParams.autoReconnect * 1000) {
                player->decoderFlags |= DECODER_FLAG_PS_RECONNECT;
            } else av_usleep(20*1000);
            continue;
        } else {
            player->readTimeLast = av_gettime_relative();
        }

        // audio
        if (packet->stream_index == player->audioIndex) {
            player->decodeQueue.AudioEnqueue(packet);
        }

        // video
        if (packet->stream_index == player->videoIndex) {
            player->decodeQueue.VideoEnqueue(packet);
        }

        if (packet->stream_index != player->audioIndex
              && packet->stream_index != player->videoIndex) {
            player->decodeQueue.ReleasePacket(packet);
        }
    }
    
done:
    return nullptr;
}
void* CCVideoPlayer::audio_decode_thread_proc(void *param) {
    auto player = (CCVideoPlayer*)param;

    AVPacket *packet;
    int64_t apts;

    player->aFrame.pts = -1;
    while (!(player->decoderFlags & DECODER_FLAG_PS_CLOSE)) {
        //++ when audio decode pause ++//
        if (player->decoderFlags & DECODER_FLAG_PS_A_PAUSE) {
            player->decoderFlags |= (DECODER_FLAG_PS_A_PAUSE << 16);
            av_usleep(20*1000); continue;
        }
        //-- when audio decode pause --//

        // dequeue audio packet
        packet = player->decodeQueue.AudioDequeue();
        if (packet == nullptr) {
            continue;
        }

        //++ decode audio packet ++//
        apts = AV_NOPTS_VALUE;
        while (packet->size > 0 && !(player->decoderFlags & (DECODER_FLAG_PS_A_PAUSE|DECODER_FLAG_PS_CLOSE))) {
            int consumed = 0;
            int gotaudio = 0;

            consumed = avcodec_decode_audio4(player->aCodecCtx, &player->aFrame, &gotaudio, packet);
            if (consumed < 0) {
                LOGW("an error occurred during decoding audio.");
                break;
            }

            if (gotaudio) {
                AVRational tb_sample_rate = {1, player->aCodecCtx->sample_rate};
                if (apts == AV_NOPTS_VALUE) {
                    apts = av_rescale_q(player->aFrame.pts, player->aStreamTimebase,
                                        tb_sample_rate);
                } else {
                    apts += player->aFrame.nb_samples;
                }
                player->aFrame.pts = av_rescale_q(apts, tb_sample_rate, TIMEBASE_MS);
                //++ for seek operation
                if (player->decoderFlags & DECODER_FLAG_PS_A_SEEK) {
                    if (player->seekDest - player->aFrame.pts <= player->seekDiff) {
                        player->internalData.start_tick = av_gettime_relative() / 1000;
                        player->internalData.start_pts = player->aFrame.pts;
                        player->internalData.apts = player->aFrame.pts;
                        player->internalData.vpts =
                                player->videoIndex == -1 ? -1 : player->seekDest;
                        player->decoderFlags &= ~DECODER_FLAG_PS_A_SEEK;
                        if (player->decoderFlags & DECODER_FLAG_PS_R_PAUSE) {
                            //TODO: render_pause(player->render, 1);
                        }
                    }
                }
                //-- for seek operation
                if (!(player->decoderFlags & DECODER_FLAG_PS_A_SEEK)) {
                    //TODO: render_audio(player->render, &player->aFrame);
                }
            }

            packet->data += consumed;
            packet->size -= consumed;
        }
        //-- decode audio packet --//

        // release packet
        player->decodeQueue.ReleasePacket(packet);
    }

    av_frame_unref(&player->aFrame);
    
    return nullptr;
}
void* CCVideoPlayer::video_decode_thread_proc(void *param) {
    auto player = (CCVideoPlayer*)param;

    AVPacket *packet;

    player->vFrame.pts = -1;
    while (!(player->decoderFlags & DECODER_FLAG_PS_CLOSE)) {
        //++ when video decode pause ++//
        if (player->decoderFlags & DECODER_FLAG_PS_V_PAUSE) {
            player->decoderFlags |= (DECODER_FLAG_PS_V_PAUSE << 16);
            av_usleep(20*1000);
            continue;
        }
        //-- when video decode pause --//

        // dequeue video packet
        packet = player->decodeQueue.VideoDequeue();
        if (packet == NULL) {
            //TODO: render_video(player->render, &player->vFrame);
            continue;
        }

        //++ decode video packet ++//
        while (packet->size > 0 && !(player->decoderFlags & (DECODER_FLAG_PS_V_PAUSE|DECODER_FLAG_PS_CLOSE))) {
            int consumed = 0;
            int gotvideo = 0;

            consumed = avcodec_decode_video2(player->vCodecCtx, &player->vFrame, &gotvideo, packet);
            if (consumed < 0) {
                LOGW("an error occurred during decoding video.");
                break;
            }
            if (player->vCodecCtx->width != player->InitParams.videoVWidth
                || player->vCodecCtx->height != player->InitParams.videoVHeight) {
                player->InitParams.videoVWidth  = player->InitParams.videoOWidth  = player->vCodecCtx->width;
                player->InitParams.videoVHeight = player->InitParams.videoOHeight = player->vCodecCtx->height;
                //TODO: player_send_message(player->cmnvars.winmsg, MSG_VIDEO_RESIZED, 0);
            }

            if (gotvideo) {
                player->seekVpts = player->vFrame.best_effort_timestamp;
                player->vFrame.pts = av_rescale_q(player->seekVpts, player->vStreamTimebase,
                                                  TIMEBASE_MS);
                if (player->decoderFlags & DECODER_FLAG_PS_V_SEEK) {
                    if (player->seekDest - player->vFrame.pts <= player->seekDiff) {
                        player->internalData.start_tick = av_gettime_relative() / 1000;
                        player->internalData.start_pts = player->vFrame.pts;
                        player->internalData.vpts = player->vFrame.pts;
                        player->internalData.apts =
                                player->audioIndex == -1 ? -1 : player->seekDest;
                        player->decoderFlags &= ~DECODER_FLAG_PS_V_SEEK;
                        if (player->decoderFlags & DECODER_FLAG_PS_R_PAUSE) {
                            //TODO: render_pause(player->render, 1);
                        }
                    }
                }
                if (!(player->decoderFlags & DECODER_FLAG_PS_V_SEEK)) {
                    //TODO: render_video(player->render, &player->vFrame);
                }
            }

            packet->data += packet->size;
            packet->size -= packet->size;
        }
        //-- decode video packet --//

        // release packet
        player->decodeQueue.ReleasePacket(packet);
    }

    av_frame_unref(&player->vFrame);

    return nullptr;
}

void CCVideoPlayer::HandleFSeekOrReconnect(int reconnect)
{
    int PAUSE_REQ = 0;
    int PAUSE_ACK = 0;

    if (audioIndex != -1) { PAUSE_REQ |= DECODER_FLAG_PS_A_PAUSE; PAUSE_ACK |= DECODER_FLAG_PS_A_PAUSE << 16; }
    if (videoIndex != -1) { PAUSE_REQ |= DECODER_FLAG_PS_V_PAUSE; PAUSE_ACK |= DECODER_FLAG_PS_V_PAUSE << 16; }

    // set audio & video decoding pause flags
    decoderFlags = (decoderFlags & ~PAUSE_ACK) | PAUSE_REQ | seekReq;

    // make render run
    //TODO: render_pause(player->render, 0);

    // wait for pause done
    while ((decoderFlags & PAUSE_ACK) != PAUSE_ACK) {
        if (decoderFlags & DECODER_FLAG_PS_CLOSE) return;
        av_usleep(20*1000);
    }

    if (reconnect) {

        if (aCodecCtx) { avcodec_close(aCodecCtx); aCodecCtx = NULL; }
        if (vCodecCtx) { avcodec_close(vCodecCtx); vCodecCtx = NULL; }
        if (pFormatCtx) { avformat_close_input(&pFormatCtx); }
        av_frame_unref(&aFrame); aFrame.pts = -1;
        av_frame_unref(&vFrame); vFrame.pts = -1;

        //TODO: player_send_message(player->cmnvars.winmsg, MSG_STREAM_DISCONNECT, (int64_t)player);
        InitDecoder();
        //TODO: player_send_message(player->cmnvars.winmsg, MSG_STREAM_CONNECTED , (int64_t)player);

    } else {

        av_seek_frame(pFormatCtx, seekSidx, seekPos, AVSEEK_FLAG_BACKWARD);

        if (audioIndex != -1) avcodec_flush_buffers(aCodecCtx);
        if (videoIndex != -1) avcodec_flush_buffers(vCodecCtx);
    }

    decodeQueue.Reset(); // reset pktqueue
    //TODO: render_reset (); // reset render

    // make audio & video decoding thread resume
    decoderFlags &= ~(PAUSE_REQ|PAUSE_ACK);
}

CCVideoState CCVideoPlayer::GetVideoState() { return playerStatus; }
int64_t CCVideoPlayer::GetVideoLength() {
    return pFormatCtx ? (pFormatCtx->duration * 1000 / AV_TIME_BASE) : 1;
}
int64_t CCVideoPlayer::GetVideoPos() {
    if ((decoderFlags & DECODER_FLAG_PS_F_SEEK) || (decoderFlags & seekReq))
        return seekDest - internalData.start_time;
    else
        return MAX(internalData.apts, internalData.vpts);
}

extern int64_t av_rescale_q(int64_t a, AVRational bq, AVRational cq) av_const;

void CCVideoPlayer::SetVideoPos(int64_t ms, int type) {
    if (decoderFlags & (DECODER_FLAG_PS_F_SEEK | seekReq)) {
        LOGW("[CCVideoPlayer] seek busy !");
        return;
    }

    AVRational fRate;

    switch (type) {
        case SEEK_STEP_FORWARD:
            // TODO: render_pause(player->render, 1);
            return;
        case SEEK_STEP_BACKWARD:
            fRate = pFormatCtx->streams[videoIndex]->r_frame_rate;
            seekDest = av_rescale_q(seekVpts, vStreamTimebase, TIMEBASE_MS) - 1000 * fRate.den / fRate.num - 1;
            seekPos  = seekVpts + av_rescale_q(ms, TIMEBASE_MS, vStreamTimebase);
            seekDiff = 0;
            seekSidx = videoIndex;
            decoderFlags |= DECODER_FLAG_PS_R_PAUSE;
            break;
        default:
            seekDest = internalData.start_time + ms;
            seekPos = (internalData.start_time + ms) * AV_TIME_BASE / 1000;
            seekDiff = 100;
            seekSidx = -1;
            break;
    }

    // set PS_F_SEEK flag
    decoderFlags |= DECODER_FLAG_PS_F_SEEK;
}
void CCVideoPlayer::SetVideoState(CCVideoState newState) {
    switch (newState) {
        case CCVideoState::Ended:
        case CCVideoState::Failed:
            LOGEF("[CCVideoPlayer] SetVideoState with bad state : %d, state can only get.", newState);
            return;
        case CCVideoState::NotOpen: CloseVideo(); break;
        case CCVideoState::Playing:
            decoderFlags &= DECODER_FLAG_PS_CLOSE;
            playerStatus = newState;
            break;
        case CCVideoState::Paused:
            decoderFlags &= DECODER_FLAG_PS_R_PAUSE;
            playerStatus = newState;
            break;
    }

    LOGIF("[CCVideoPlayer] New State %d", newState);
}
bool CCVideoPlayer::CloseVideo() {

    if(playerStatus == CCVideoState::NotOpen || playerStatus == CCVideoState::Failed) {
        LOGE("[CCVideoPlayer] CloseVideo failed : video not open.");
        return false;
    }

    StopDecoder();
    DestroyDecoder();

    playerStatus = CCVideoState::NotOpen;
    LOGI("[CCVideoPlayer] Video closed.");
    return true;
}
bool CCVideoPlayer::OpenVideo(char *filePath) {
    if(playerStatus != CCVideoState::NotOpen && playerStatus != CCVideoState::Failed) {
        LOGE("[CCVideoPlayer] OpenVideo failed : video already open, close it first.");
        return false;
    }

    currentFile = filePath;

    if(!InitDecoder()) {
        playerStatus = CCVideoState::Failed;
        return false;
    }

    //初始化线程
    InitThread();
    StartDecoder();

    playerStatus = CCVideoState::Opened;
    LOGI("[CCVideoPlayer] Video opened.");
    return true;
}




