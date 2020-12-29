//
// Created by roger on 2020/12/22.
//

#include "CCPlayerRender.h"
#include "CCVideoPlayer.h"
#include "CCAudioDevice.h"
#include "CCVideoDevice.h"

bool CCPlayerRender::Init(CCVideoPlayerExternalData *data) {
    externalData = data;

    audioDevice = CreateAudioDevice(data);
    videoDevice = CreateVideoDevice();

    //初始化SwsContext
    swsContext = sws_getContext(
            data->VideoCodecContext->width,   //原图片的宽
            data->VideoCodecContext->height,  //源图高
            data->VideoCodecContext->pix_fmt, //源图片format
            data->InitParams->DestWidth,  //目标图的宽
            data->InitParams->DestHeight,  //目标图的高
            data->InitParams->DestFormat,
            SWS_BICUBIC,
            nullptr, nullptr, nullptr
    );

    if(swsContext == nullptr) {
        LOGE(LOG_TAG, "Get swsContext failed");
        return false;
    }

    //初始化 swrContext
    swrContext = swr_alloc();
    if(swrContext == nullptr){
        LOGE(LOG_TAG, "Alloc swrContext failed");
        return false;
    }

    // 配置输入/输出通道类型
    av_opt_set_int(swrContext, "in_channel_layout", externalData->AudioCodecContext->channel_layout, 0);
    // 这里 AUDIO_DEST_CHANNEL_LAYOUT = AV_CH_LAYOUT_STEREO，即 立体声
    av_opt_set_int(swrContext, "out_channel_layout", AUDIO_DEST_CHANNEL_LAYOUT, 0);
    // 配置输入/输出采样率
    av_opt_set_int(swrContext, "in_sample_rate", externalData->AudioCodecContext->sample_rate, 0);
    av_opt_set_int(swrContext, "out_sample_rate", audioDevice->GetSampleRate(externalData->AudioCodecContext->sample_rate), 0);
    // 配置输入/输出数据格式
    av_opt_set_sample_fmt(swrContext, "in_sample_fmt", externalData->AudioCodecContext->sample_fmt, 0);
    av_opt_set_sample_fmt(swrContext, "out_sample_fmt", audioDevice->GetSampleFmt(),  0);

    swr_init(swrContext);

    // 重采样后一个通道采样数
    destNbSample = (int)av_rescale_rnd(
            ACC_NB_SAMPLES,
            audioDevice->GetSampleRate(externalData->AudioCodecContext->sample_rate),
            externalData->AudioCodecContext->sample_rate, AV_ROUND_UP);
    // 重采样后一帧数据的大小
    destDataSize = (size_t)av_samples_get_buffer_size(
            nullptr, AUDIO_DEST_CHANNEL_COUNTS,
            destNbSample, audioDevice->GetSampleFmt(), 1);

    audioOutBuffer[0] = (uint8_t *) malloc(destDataSize);
    audioDevice->SetBufferCallback(RenderAudioThreadStub, this);

    return true;
}
void CCPlayerRender::Destroy() {
    if(outFrame != nullptr)
        av_frame_free(&outFrame);
    if(swsContext != nullptr) {
        sws_freeContext(swsContext);
        swsContext = nullptr;
    }
    if(swrContext != nullptr) {
        swr_free(&swrContext);
    }
    if(audioOutBuffer[0] != nullptr) {
        free(audioOutBuffer[0]);
        audioOutBuffer[0] = nullptr;
    }
    if(audioDevice != nullptr) {
        audioDevice->Destroy();
        audioDevice = nullptr;
    }
    if(videoDevice != nullptr) {
        videoDevice->Destroy();
        videoDevice = nullptr;
    }
}
void CCPlayerRender::Pause() {
    status = CCRenderState::NotRender;

    void* retval;
    if(renderVideoThread)
        pthread_join(renderVideoThread, &retval);
    renderVideoThread = 0;
    if(renderAudioThread)
        pthread_join(renderAudioThread, &retval);
    renderAudioThread = 0;

    audioDevice->Pause(true);
    videoDevice->Pause(true);
}
void CCPlayerRender::Start(bool isStartBySeek) {

    status = isStartBySeek ? CCRenderState::RenderingToSeekPos : CCRenderState::Rendering;

    audioDevice->Pause(false);
    videoDevice->Pause(false);

    pthread_create(&renderVideoThread, nullptr, RenderVideoThreadStub, this);
    //pthread_create(&renderAudioThread, nullptr, RenderAudioThreadStub, this);
}
void CCPlayerRender::Reset() {
    audioDevice->Reset();
    videoDevice->Reset();

    curVideoDts = 0;
    curVideoPts = 0;
    curAudioDts = 0;
    curAudioPts = 0;
}

CCVideoDevice *CCPlayerRender::CreateVideoDevice() {
    return new CCVideoDevice();
}
CCAudioDevice *CCPlayerRender::CreateAudioDevice(CCVideoPlayerExternalData *data) {
    return new CCAudioDevice(data);
}

void *CCPlayerRender::RenderVideoThreadStub(void *param) {
    prctl(PR_SET_NAME, "RenderVideo");
    return ((CCPlayerRender*)param)->RenderVideoThread();
}
void *CCPlayerRender::RenderVideoThread() {
    LOGD(LOG_TAG, "RenderVideo Start");

    while (status == CCRenderState::Rendering || status == CCRenderState::RenderingToSeekPos) {

        if(outFrame == nullptr || outFrameDestFormat != externalData->InitParams->DestFormat
            || outFrameDestWidth != externalData->InitParams->DestWidth
            || outFrameDestHeight != externalData->InitParams->DestHeight) {

            if(outFrame != nullptr)
                av_frame_free(&outFrame);

            outFrameDestFormat = externalData->InitParams->DestFormat;
            outFrameDestWidth = externalData->InitParams->DestWidth;
            outFrameDestHeight = externalData->InitParams->DestHeight;

            outFrame = av_frame_alloc();
            outFrameBufferSize = (size_t) av_image_get_buffer_size(outFrameDestFormat, outFrameDestWidth, outFrameDestHeight, 1);
            outFrameBuffer = (uint8_t *) av_malloc(outFrameBufferSize);
        }

        double frame_delays = 1.0 / externalData->CurrentFps;
        AVFrame *frame = externalData->DecodeQueue->VideoFrameDequeue();

        if(frame == nullptr) {
            av_usleep((int64_t)(1000));
            continue;
        }

        //时钟
        AVRational time_base = externalData->VideoTimeBase;
        if ((currentVideoClock = frame->best_effort_timestamp) == AV_NOPTS_VALUE)
            currentVideoClock = 0;
        else
            currentVideoClock = frame->best_effort_timestamp * av_q2d(time_base);

        curVideoDts = frame->pkt_dts;
        curVideoPts = frame->pts;

        if(currentVideoClock == 0)
            startTime = av_gettime() / 1000000.0;

        //延迟计算
        double extra_delay = frame->repeat_pict / (2 * externalData->CurrentFps);
        double delays = extra_delay + frame_delays;

        videoPlayTime = curVideoPts * av_q2d(time_base);
        //纠正时间
        videoPlayTime = VideoPlayTimeSynchronize(frame, videoPlayTime);
        videoDelay = videoPlayTime - videoLastPlayTime;
        if (videoDelay <= 0 || videoDelay > 1)
            videoDelay = videoLastDelay;
        videoLastDelay = videoDelay;
        videoLastPlayTime = videoPlayTime;

        if(videoSeeking) {
            if(curVideoPts >= seekDest) {
                videoSeeking = false;
                currentSeekToPosFinished = true;
                if(status == CCRenderState::RenderingToSeekPos) {
                    externalData->DecodeQueue->ReleaseFrame(frame);
                    LOGD(LOG_TAG, "RenderVideoThread End because seek to dest pos");
                    return nullptr;
                }
            }
        }
        //与音频同步
        else if(externalData->AudioCodecContext != nullptr) {


            //音频与视频的时间差
            double diff = currentVideoClock - currentAudioClock;
            //在合理范围外  才会延迟  加快
            syncThreshold = (videoDelay > 0.01 ? 0.01 : videoDelay);

            if (fabs(diff) < 10) {
                if (diff <= -syncThreshold) {
                    videoDelay = 0;
                    //如果视频太落后于音频，那么必须丢包，不然赶不上了
                    if(diff < 0.5) {
                        int count = externalData->DecodeQueue->VideoDrop(currentAudioClock);
                        LOGDF(LOG_TAG, "Sync: Video too slow, skip frame : %d, %f (v/a -> %f/%f)", count, diff, currentVideoClock, currentAudioClock);
                    } else {
                        LOGDF(LOG_TAG, "Sync: Video slow : %f (v/a -> %f/%f)", diff, currentVideoClock, currentAudioClock);
                    }
                } else if (diff >= syncThreshold) {
                    videoDelay = 2 * videoDelay;
                    LOGDF(LOG_TAG, "Sync: Audio slow : %f (v/a -> %f/%f)", diff, currentVideoClock, currentAudioClock);
                }
            }

            startTime += videoDelay;
            videoActualDelay = startTime - av_gettime() / 1000000.0;
            if (videoActualDelay < 0.01)
                videoActualDelay = 0.01;

            av_usleep((int64_t)(videoActualDelay * 1000000.0 + 6000));
        }
        else {
            //正常播放
            av_usleep((int64_t)(delays * 1000000));
        }

        //seeking时不刷新屏幕，直接跳过
        if(!videoSeeking) {

            memset(outFrameBuffer, 0, outFrameBufferSize);

            //更具指定的数据初始化/填充缓冲区
            av_image_fill_arrays(outFrame->data, outFrame->linesize, outFrameBuffer, outFrameDestFormat,
                                 outFrameDestWidth, outFrameDestHeight, 1);
            //转码
            sws_scale(swsContext, (const uint8_t *const *) frame->data, frame->linesize, 0,
                      frame->height, outFrame->data, outFrame->linesize);

            uint8_t *src = outFrame->data[0];
            int srcStride = outFrame->linesize[0];
            int destStride = 0;
            uint8_t *target = videoDevice->Lock(src, srcStride, &destStride, curVideoPts);
            if (target && src) {
                for (int i = 0, c = externalData->VideoCodecContext->height; i < c; i++)
                    memcpy(target + i * destStride, src + i * srcStride, srcStride);
                videoDevice->Dirty();
            }else if(!src) {
                LOGWF(LOG_TAG, "Frame %ld scale failed", frame->pts);
            }
            videoDevice->Unlock();

            av_frame_unref(outFrame);
        }

        externalData->DecodeQueue->ReleaseFrame(frame);
    }

    LOGD(LOG_TAG, "RenderVideoThread End");
    return nullptr;
}
/*
void *CCPlayerRender::RenderAudioThread() {
    LOGD(LOG_TAG, "RenderAudioThread Start");

    while (status == CCRenderState::Rendering) {

        AVFrame *frame = externalData->DecodeQueue->AudioFrameDequeue();
        if(frame == nullptr) {
            av_usleep((int64_t)(1000000));
            continue;
        }

        //时钟
        AVRational time_base = externalData->AudioTimeBase;
        currentAudioClock = frame->best_effort_timestamp * av_q2d(time_base);

        curAudioDts = frame->pkt_dts;
        curAudioPts = frame->pts;

        if(audioSeeking) {
            if(curAudioPts >= seekDest) audioSeeking = false;
        } else {
            // 转换，返回每个通道的样本数
            int samples = swr_convert(swrContext, audioOutBuffer, (int) destDataSize / 2,
                                  (const uint8_t **) frame->data, frame->nb_samples);
            if (samples > 0)
                audioDevice->Write(audioOutBuffer[0], (size_t) destDataSize, 0);

            double frame_delays = 1.0 / externalData->CurrentFps;
            double extra_delay = frame->repeat_pict / (2 * externalData->CurrentFps);
            double delays = (extra_delay + frame_delays) / 2;
            av_usleep((int64_t)(delays * 1000000));
        }

        externalData->DecodeQueue->ReleaseFrame(frame);
    }

    LOGD(LOG_TAG, "RenderAudioThread End");
    return nullptr;
}
void *CCPlayerRender::RenderAudioThreadStub(void *param) {
    return ((CCPlayerRender*)param)->RenderAudioThread();
}
*/
void CCPlayerRender::RenderAudioThreadStub(CCAudioDevice* dev, void* customData, uint8_t **buf, int *len) {
    return ((CCPlayerRender*)customData)->RenderAudioThread(dev, buf, len);
}
void CCPlayerRender::RenderAudioThread(CCAudioDevice* dev, uint8_t **buf, int *len) {
    if (status == CCRenderState::Rendering || status == CCRenderState::RenderingToSeekPos) {

        AVFrame *frame = nullptr;
        int noneFrameTick = 0;
GET_FRAME:
        while(frame == nullptr) {
            av_usleep((int64_t)(10000));
            frame = externalData->DecodeQueue->AudioFrameDequeue();
            if(noneFrameTick < 32) noneFrameTick ++;
            else return;
        }

        //时钟
        AVRational time_base = externalData->AudioTimeBase;
        if (frame->best_effort_timestamp == AV_NOPTS_VALUE)
            currentAudioClock = 0;
        else
            currentAudioClock = frame->best_effort_timestamp * av_q2d(time_base);

        curAudioDts = frame->pkt_dts;
        curAudioPts = frame->pts;

        //seek时直接跳过至目标帧
        if(audioSeeking) {
            if(curAudioPts >= seekDest) audioSeeking = false;
            else {
                frame = nullptr;
                noneFrameTick = 0;
                goto GET_FRAME;
            }
        }

        if(!audioSeeking) {
            // 转换，返回每个通道的样本数
            int samples = swr_convert(swrContext, audioOutBuffer, (int) destDataSize / 2,
                                      (const uint8_t **) frame->data, frame->nb_samples);
            if (samples > 0) {
                *buf = audioOutBuffer[0];
                *len = destDataSize;

                //pts时间+当前帧播放需要的时间
                currentAudioClock += samples / ((double) (audioDevice->GetSampleRate(0) * 2 * 2));
            }

            //audioDevice->Write(audioOutBuffer[0], (size_t) destDataSize, 0);

            //double frame_delays = 1.0 / externalData->CurrentFps;
            //double extra_delay = frame->repeat_pict / (2 * externalData->CurrentFps);
            //double delays = (extra_delay + frame_delays) / 2;
            //av_usleep((int64_t)(delays * 1000000));
        }

        externalData->DecodeQueue->ReleaseFrame(frame);
    }
}

double CCPlayerRender::VideoPlayTimeSynchronize(AVFrame *frame, double playTime) {
    //clock是当前播放的时间位置
    if (playTime != 0)
        currentVideoClock = playTime;
    else //pst为0 则先把pts设为上一帧时间
        playTime = currentVideoClock;
    //可能有pts为0 则主动增加clock
    //需要求出扩展延时：
    //使用AvCodecContext的而不是stream的

    double frame_delay = av_q2d(externalData->VideoCodecContext->time_base);
    double fps = 1 / frame_delay;
    //pts 加上 这个延迟 是显示时间
    double extra_delay = frame->repeat_pict / (2 * fps);
    currentVideoClock += extra_delay + frame_delay;
    return playTime;
}

void CCPlayerRender::SetVolume(int vol) {

}
void CCPlayerRender::SetSeekDest(int64_t dest) {
    if(dest == -1) {
        seekDest = 0;
        audioSeeking = false;
        videoSeeking = false;
    } else {
        seekDest = dest;
        audioSeeking = true;
        videoSeeking = true;
    }
}


