//
// Created by roger on 2020/12/22.
//

#include "CCAudioDevice.h"

void CCAudioDevice::Reset() {
    if(!buffers.empty()) {
        for(auto buffer : buffers) {
            free(buffer->buffer);
            free(buffer);
        }
        buffers.clear();
    }
    if(bqPlayerInterface) {
        (*bqPlayerInterface)->SetPlayState(bqPlayerInterface, SL_PLAYSTATE_STOPPED);
    }

    isPlaying = false;
    isSurePlaying = false;
}
void CCAudioDevice::Pause(int pause) {
    isPlaying = !pause;
    isSurePlaying = false;
    if (bqPlayerInterface) {
        (*bqPlayerInterface)->SetPlayState(bqPlayerInterface,
                                           pause ? SL_PLAYSTATE_PAUSED : SL_PLAYSTATE_PLAYING);
        if(!pause)
            bqPlayerCallback(bqPlayerBufferQueueInterface, this);
    }
}
void CCAudioDevice::Write(uint8_t *buf, int len, int64_t pts) {
    auto *buffer = new AUDIO_BUFFER();
    buffer->buffer = (uint8_t*)malloc(len);
    buffer->bufferLen = len;
    memcpy(buffer->buffer, buf, len);
    buffers.push_back(buffer);

    if(isPlaying && !isSurePlaying)
        bqPlayerCallback(bqPlayerBufferQueueInterface, this);
}
void CCAudioDevice::Destroy() {

    Reset();

    if(bqPlayerObject){
        (*bqPlayerObject)->Destroy(bqPlayerObject);
        bqPlayerObject = nullptr;
        bqPlayerInterface = nullptr;
        bqPlayerBufferQueueInterface = nullptr;
    }

    //释放混音器
    if(outputMixObject){
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = nullptr;
    }

    //释放引擎
    if(engineObject){
        (*engineObject)->Destroy(engineObject);
        engineObject = nullptr;
        engineInterface = nullptr;
    }
}
CCAudioDevice::CCAudioDevice(CCVideoPlayerExternalData *externalData) {
    SLresult result;
    // 创建引擎 SLObjectItf engineObject
    result = slCreateEngine(&engineObject, 0, nullptr,
            0, nullptr, nullptr);

    if (SL_RESULT_SUCCESS != result) {
        LOGEF(LOG_TAG, "slCreateEngine failed : %d", result);
        return;
    }
    // 初始化引擎(init)
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        LOGEF(LOG_TAG, "(*engineObject)->Realize failed : %d", result);
        return;
    }

    // 获取引擎接口SLEngineItf engineInterface
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineInterface);
    if (SL_RESULT_SUCCESS != result) {
        LOGEF(LOG_TAG, "(*engineObject)->GetInterface failed : %d", result);
        return;
    }

    result = (*engineInterface)->CreateOutputMix(engineInterface,&outputMixObject, 0, nullptr, nullptr);
    if (SL_RESULT_SUCCESS != result) {
        LOGEF(LOG_TAG, "(*engineInterface)->CreateOutputMix failed : %d", result);
        return;
    }
    // 初始化混音器outputMixObject
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        LOGEF(LOG_TAG, "(*outputMixObject)->Realize failed : %d", result);
        return;
    }

    //3.2 配置音轨（输出）
    //设置混音器
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&outputMix, nullptr};
    //需要的接口， 操作队列的接口，可以添加混音接口
    const SLInterfaceID ids[1] = {SL_IID_BUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};

    //创建buffer缓冲类型的队列 2个队列
    SLDataLocator_AndroidSimpleBufferQueue android_queue ={SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,2};
    //pcm数据格式
    //pcm +2（双声道）+ 44100（采样率）+ 16（采样位）+ LEFT|RIGHT（双声道）+ 小端数据
    SLDataFormat_PCM pcm;
    pcm.formatType = SL_DATAFORMAT_PCM;
    pcm.numChannels = 2;
    pcm.samplesPerSec = SL_SAMPLINGRATE_44_1;
    pcm.bitsPerSample = SL_PCMSAMPLEFORMAT_FIXED_16;
    pcm.containerSize = SL_PCMSAMPLEFORMAT_FIXED_16;
    pcm.channelMask = SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT;
    pcm.endianness = SL_BYTEORDER_LITTLEENDIAN;

    //数据源 将上述配置信息放到这个数据源中
    SLDataSource slDataSource = {&android_queue, &pcm};
    //创建播放器
    (*engineInterface)->CreateAudioPlayer(engineInterface, &bqPlayerObject, &slDataSource,&audioSnk, 1,ids, req);
    //初始化播放器
    (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
    (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE, &bqPlayerBufferQueueInterface);
    (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerInterface);
    //设置回调
    (*bqPlayerBufferQueueInterface)->RegisterCallback(bqPlayerBufferQueueInterface, bqPlayerCallback, this);
}

void CCAudioDevice::bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
    auto *dev = static_cast<CCAudioDevice*>(context);
    if(dev->bufferCallback) {
        uint8_t *buf = nullptr;
        int len = 0;
        dev->bufferCallback(dev, dev->bufferCallbackData, &buf, &len);
        if(buf) {
            //LOGDF(dev->LOG_TAG, "bqPlayerCallback : %d", len);
            (*bq)->Enqueue(bq, buf, len);
        }
    }
    /*
    auto *buffer = dev->popBuffer();
    if(buffer != nullptr) {
        dev->isSurePlaying = true;
        LOGDF(dev->LOG_TAG, "bqPlayerCallback : %d (Buffer left: %d)", buffer->bufferLen, dev->buffers.size());
        (*bq)->Enqueue(bq, buffer->buffer, buffer->bufferLen);
        free(buffer->buffer);
        free(buffer);
    } else {
        dev->isSurePlaying = false;
        LOGD(dev->LOG_TAG, "bqPlayerCallback : null");
    }
    */
}

AUDIO_BUFFER *CCAudioDevice::popBuffer() {
    if(!buffers.empty()) {
        auto b = buffers.front();
        buffers.pop_front();
        return b;
    }
    return nullptr;
}

void CCAudioDevice::SetBufferCallback(CCAudioDeviceBufferCallback callback, void *data) {
    bufferCallback = callback;
    bufferCallbackData = data;
}
