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
    if(swsContext == nullptr){
        LOGE("Get swsContext failed");
        return false;
    }

    //初始化 swrContext
    swrContext = swr_alloc();

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

    return true;
}
void CCPlayerRender::Destroy() {
    if(swsContext != nullptr) {
        sws_freeContext(swsContext);
        swsContext = nullptr;
    }
    if (swrContext != nullptr) {
        swr_free(&swrContext);
    }
    if (audioOutBuffer[0] != nullptr) {
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
    pthread_join(renderVideoThread, &retval);
    pthread_join(renderAudioThread, &retval);

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
}

CCVideoDevice *CCPlayerRender::CreateVideoDevice() {
    return new CCVideoDevice();
}
CCAudioDevice *CCPlayerRender::CreateAudioDevice(CCVideoPlayerExternalData *data) {
    return new CCAudioDevice(data);
}

void *CCPlayerRender::RenderVideoThread() {

    while (status == CCRenderState::Rendering) {
        AVFrame *frame = externalData->DecodeQueue->VideoFrameDequeue();
        AVFrame *outFrame = externalData->DecodeQueue->RequestFrame();
        if(!frame) {
            av_usleep(1000 * 5);
            continue;
        }

        sws_scale(swsContext, (const uint8_t *const *) frame->data, frame->linesize, 0,
                  frame->height, outFrame->data, outFrame->linesize);

        uint8_t *src = outFrame->data[0];
        int srcStride = outFrame->linesize[0];
        int destStride = 0;
        uint8_t *target = videoDevice->Lock(src, srcStride, &destStride, 0);
        if(target) {
            for (int i = 0, c = externalData->VideoCodecContext->height; i < c; i++)
                memcpy(target + i * destStride, src + i * srcStride, srcStride);
        }

        videoDevice->Unlock();

        externalData->DecodeQueue->ReleaseFrame(frame);
        externalData->DecodeQueue->ReleaseFrame(outFrame);
    }

    return nullptr;
}
void *CCPlayerRender::RenderAudioThread() {

    while (status == CCRenderState::Rendering) {
        AVFrame *frame = externalData->DecodeQueue->VideoFrameDequeue();
        if(!frame) {
            av_usleep(1000 * 5);
            continue;
        }

        // 转换，返回每个通道的样本数
        int ret = swr_convert(swrContext, audioOutBuffer, destDataSize / 2,
                              (const uint8_t **) frame->data, frame->nb_samples);
        if (ret > 0)
            audioDevice->Write(audioOutBuffer[0], (size_t) destDataSize, 0);

        externalData->DecodeQueue->ReleaseFrame(frame);
    }

    return nullptr;
}
void *CCPlayerRender::RenderVideoThreadStub(void *param) {
    return ((CCPlayerRender*)param)->RenderVideoThread();
}
void *CCPlayerRender::RenderAudioThreadStub(void *param) {
    return ((CCPlayerRender*)param)->RenderAudioThread();
}


