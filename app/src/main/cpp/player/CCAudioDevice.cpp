//
// Created by roger on 2020/12/22.
//

#include "CCAudioDevice.h"

void CCAudioDevice::Reset() {
    buffer = nullptr;
    bufferLen = 0;
}
void CCAudioDevice::Pause(int pause) {
    if(bqPlayerInterface)
        (*bqPlayerInterface)->SetPlayState(bqPlayerInterface, pause ? SL_PLAYSTATE_PAUSED : SL_PLAYSTATE_PLAYING);
}
void CCAudioDevice::Write(uint8_t *buf, int len, int64_t pts) {
    buffer = buf;
    bufferLen = len;
}
void CCAudioDevice::Destroy() {
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
            0, 0, 0);
    if (SL_RESULT_SUCCESS != result) {
        return;
    }
    // 初始化引擎(init)
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        return;
    }

    // 获取引擎接口SLEngineItf engineInterface
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineInterface);
    if (SL_RESULT_SUCCESS != result) {
        return;
    }

    result = (*engineInterface)->CreateOutputMix(engineInterface,&outputMixObject, 0, nullptr, nullptr);
    if (SL_RESULT_SUCCESS != result) {
        return;
    }
    // 初始化混音器outputMixObject
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
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
    SLDataFormat_PCM pcm = {SL_DATAFORMAT_PCM, 2, SL_SAMPLINGRATE_44_1, SL_PCMSAMPLEFORMAT_FIXED_16,SL_PCMSAMPLEFORMAT_FIXED_16,
                            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
                            SL_BYTEORDER_LITTLEENDIAN};

    //数据源 将上述配置信息放到这个数据源中
    SLDataSource slDataSource = {&android_queue, &pcm};
    //创建播放器
    (*engineInterface)->CreateAudioPlayer(engineInterface, &bqPlayerObject, &slDataSource,&audioSnk, 1,ids, req);
    //初始化播放器
    (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);

    (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
                                    &bqPlayerBufferQueueInterface);
    //设置回调
    (*bqPlayerBufferQueueInterface)->RegisterCallback(bqPlayerBufferQueueInterface, bqPlayerCallback, this);

    //设置播放状态
    (*bqPlayerInterface)->SetPlayState(bqPlayerInterface, SL_PLAYSTATE_PLAYING);

    //6. 手动激活启动回调
    bqPlayerCallback(bqPlayerBufferQueueInterface, this);
}

void CCAudioDevice::bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
    auto *dev = static_cast<CCAudioDevice*>(context);
    if(dev->bufferLen > 0){
        (*bq)-> Enqueue(bq, dev->buffer, dev->bufferLen);//这里取 16位数据
    }
}