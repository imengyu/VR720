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
void CCPlayerRender::Stop() {
    if(status != CCRenderState::NotRender) {
        status = CCRenderState::NotRender;

        void *retval;
        if (renderVideoThread)
            pthread_join(renderVideoThread, &retval);
        //if(renderAudioThread)
        //    pthread_join(renderAudioThread, &retval);

        audioDevice->Pause(true);
        videoDevice->Pause(true);
    }
}
void CCPlayerRender::Start(bool isStartBySeek) {

    status = isStartBySeek ? CCRenderState::RenderingToSeekPos : CCRenderState::Rendering;

    if(!isStartBySeek)
        audioDevice->Pause(false);
    videoDevice->Pause(false);

    if(!renderVideoThread)
        pthread_create(&renderVideoThread, nullptr, RenderVideoThreadStub, this);
    //if(!renderAudioThread)
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
    void* result = ((CCPlayerRender*)param)->RenderVideoThread();
    ((CCPlayerRender*)param)->renderVideoThread = 0;
    return result;
}
void *CCPlayerRender::RenderVideoThread() {
    LOGDF(LOG_TAG, "RenderVideoThread Start [%s]", CCRenderStateToString(status));

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
            av_usleep((int64_t)(100000));
            continue;
        }

        //时钟
        AVRational time_base = externalData->VideoTimeBase;
        if (frame->best_effort_timestamp == AV_NOPTS_VALUE)
            currentVideoClock = frame->pts * av_q2d(time_base);
        else
            currentVideoClock = frame->best_effort_timestamp * av_q2d(time_base);

        curVideoDts = frame->pkt_dts;
        curVideoPts = frame->pts;

        //延迟计算
        double extra_delay = frame->repeat_pict / (2 * externalData->CurrentFps);
        double delays = extra_delay + frame_delays;

        //与音频同步
        if(status != CCRenderState::RenderingToSeekPos) {
            if (externalData->AudioCodecContext != nullptr) {
                //音频与视频的时间差
                double diff = fabs(currentVideoClock - currentAudioClock);
                //LOGDF(LOG_TAG, "Sync: diff: %f, v/a %f/%f", diff, currentVideoClock, currentAudioClock);
                if (currentVideoClock > currentAudioClock) {
                    if (diff > 1) {
                        av_usleep((int64_t) ((delays * 2) * 1000000));
                    } else {
                        av_usleep((int64_t) ((delays + diff) * 1000000));
                    }
                } else {
                    if (diff >= 0.55) {
                        externalData->DecodeQueue->ReleaseFrame(frame);
                        /*int count = */externalData->DecodeQueue->VideoDrop(currentAudioClock);
                        //LOGDF(LOG_TAG, "Sync: drop video pack: %d", count);
                        continue;
                    } else {
                        av_usleep((int64_t) 1000);
                    }
                }
            } else {
                //正常播放
                av_usleep((int64_t) (delays * 1000000));
            }
        }

        {
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

        if(status == CCRenderState::RenderingToSeekPos) {
            curAudioPts = curVideoPts;
            currentSeekToPosFinished = true;
            LOGD(LOG_TAG, "RenderVideoThread SeekToPosFinished");
            break;
        }
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
    if (status == CCRenderState::Rendering) {

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

        {
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

void CCPlayerRender::SetVolume(int vol) {

}


