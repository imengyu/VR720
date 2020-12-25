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
    destNbSample = (int)av_rescale_rnd(ACC_NB_SAMPLES,
            audioDevice->GetSampleRate(externalData->AudioCodecContext->sample_rate),
            externalData->AudioCodecContext->sample_rate, AV_ROUND_UP);
    // 重采样后一帧数据的大小
    destDataSize = (size_t)av_samples_get_buffer_size(
            nullptr, AUDIO_DEST_CHANNEL_COUNTS,
            destNbSample, audioDevice->GetSampleFmt(), 1);

    audioOutBuffer[0] = (uint8_t *) malloc(destDataSize);

    volZeroDb = SwVolScalerInit(volScaler, SW_VOLUME_MINDB, SW_VOLUME_MAXDB);
    volCur = volZeroDb;

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
    if(renderAudioThread)
        pthread_join(renderAudioThread, &retval);

    renderVideoThread = 0;
    renderAudioThread = 0;

    audioDevice->Pause(true);
    videoDevice->Pause(true);
}
void CCPlayerRender::Start() {

    audioDevice->Pause(false);
    videoDevice->Pause(false);

    status = CCRenderState::Rendering;
    pthread_create(&renderVideoThread, nullptr, RenderVideoThreadStub, this);
    pthread_create(&renderAudioThread, nullptr, RenderAudioThreadStub, this);
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

void *CCPlayerRender::RenderVideoThread() {
    LOGD(LOG_TAG, "RenderVideoThread Start");

    while (status == CCRenderState::Rendering) {

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
            av_usleep((int64_t)(1000000 * frame_delays));
            continue;
        }

        //时钟
        AVRational time_base = externalData->VideoCodecContext->framerate;
        currentVideoClock = frame->best_effort_timestamp * av_q2d(time_base);

        curVideoDts = frame->pkt_dts;
        curVideoPts = frame->pts;

        if(videoSeeking) {
            if(curVideoPts >= seekDest) videoSeeking = false;
        }
        //与音频同步
        else if(externalData->AudioCodecContext != nullptr) {
            double extra_delay = frame->repeat_pict / (2 * externalData->CurrentFps);
            double delays = extra_delay + frame_delays;
            double diff = currentVideoClock - currentAudioClock;
            if (diff > 0) {
                //大于0 表示视频比较快
                LOGDF(LOG_TAG, "Video fast: %lf", diff);
                if(diff > 0.15) diff = 0.15;
                av_usleep((int64_t) ((delays + diff) * 1000000));
            } else if (diff < 0) {
                //不睡了，快点赶上音频
                LOGDF(LOG_TAG, "Audio fast: %lf",diff);
                // 视频包积压的太多了 （丢包）
                if (fabs(diff) >= 0.05 && frame->flags != AV_PKT_FLAG_KEY) {
                    LOGDF(LOG_TAG, "Skip frame %ld", frame->pts);

                    externalData->DecodeQueue->ReleaseFrame(frame);
                    externalData->DecodeQueue->VideoDrop(); //丢包
                    continue;//每次丢一帧，循环丢，重新计算延迟时间。
                } else {
                    //不睡了 快点赶上 音频
                }
            }
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
            uint8_t *target = videoDevice->Lock(src, srcStride, &destStride, 0);
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
void *CCPlayerRender::RenderAudioThread() {
    LOGD(LOG_TAG, "RenderAudioThread Start");

    while (status == CCRenderState::Rendering) {
        double frame_delays = 1.0 / externalData->CurrentFps;

        AVFrame *frame = externalData->DecodeQueue->AudioFrameDequeue();
        if(frame == nullptr) {
            av_usleep((int64_t)(1000000 * frame_delays));
            continue;
        }

        double extra_delay = frame->repeat_pict / (2 * externalData->CurrentFps);
        double delays = extra_delay + frame_delays;

        //时钟
        AVRational time_base = externalData->AudioCodecContext->framerate;
        currentAudioClock = frame->pts * av_q2d(time_base);

        curAudioDts = frame->pkt_dts;
        curAudioPts = frame->pts;

        if(audioSeeking) {
            if(curAudioPts >= seekDest) audioSeeking = false;
        } else {
            // 转换，返回每个通道的样本数
            int ret = swr_convert(swrContext, audioOutBuffer, (int) destDataSize / 2,
                                  (const uint8_t **) frame->data, frame->nb_samples);
            if (ret > 0) {
                SwVolScalerRun((int16_t *) audioOutBuffer[0], destDataSize / sizeof(int16_t),
                               volScaler[volCur]);
                curAudioPts += 5 * destDataSize / (2 * audioDevice->GetSampleRate(
                        externalData->AudioCodecContext->sample_rate));
                audioDevice->Write(audioOutBuffer[0], (size_t) destDataSize, 0);
            }
        }

        externalData->DecodeQueue->ReleaseFrame(frame);

        //睡眠
        av_usleep((int64_t)(delays * 1000000));
    }

    LOGD(LOG_TAG, "RenderAudioThread End");
    return nullptr;
}
void *CCPlayerRender::RenderVideoThreadStub(void *param) {
    return ((CCPlayerRender*)param)->RenderVideoThread();
}
void *CCPlayerRender::RenderAudioThreadStub(void *param) {
    return ((CCPlayerRender*)param)->RenderAudioThread();
}

void CCPlayerRender::SetVolume(int vol) {
    vol += volZeroDb;
    vol = MAX(vol, 0  );
    vol = MIN(vol, 255);
    volCur = vol;
}

int CCPlayerRender::SwVolScalerInit(int *scaler, int mindb, int maxdb)
{
    double tabdb[256];
    double tabf [256];
    int    z, i;

    for (i=0; i<256; i++) {
        tabdb[i]  = mindb + (maxdb - mindb) * i / 256.0;
        tabf [i]  = pow(10.0, tabdb[i] / 20.0);
        scaler[i] = (int)((1 << 14) * tabf[i]); // Q14 fix point
    }

    z = -mindb * 256 / (maxdb - mindb);
    z = MAX(z, 0  );
    z = MIN(z, 255);
    scaler[0] = 0;        // mute
    scaler[z] = (1 << 14);// 0db
    return z;
}
void CCPlayerRender::SwVolScalerRun(int16_t *buf, int n, int multiplier)
{
    if (multiplier > (1 << 14)) {
        int64_t v;
        while (n--) {
            v = ((int32_t)*buf * multiplier) >> 14;
            v = MAX(v,-0x7fff);
            v = MIN(v, 0x7fff);
            *buf++ = (int16_t)v;
        }
    } else if (multiplier < (1 << 14)) {
        while (n--) { *buf = ((int32_t)*buf * multiplier) >> 14; buf++; }
    }
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

