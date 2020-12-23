//
// Created by roger on 2020/12/22.
//

#ifndef VR720_CCAUDIODEVICE_H
#define VR720_CCAUDIODEVICE_H
#include "stdafx.h"
#include "CCPlayerDefine.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Platform.h>
#include <SLES/OpenSLES_Android.h>

class CCAudioDevice {

public:

    CCAudioDevice(CCVideoPlayerExternalData *externalData);

    virtual void Destroy();
    virtual void Write(uint8_t *buf, int len, int64_t pts);
    virtual void Pause(int pause);
    virtual void Reset();

    /**
     * 采样格式：16位
     */
    virtual AVSampleFormat GetSampleFmt() {
        return AV_SAMPLE_FMT_S16;
    }
    /**
     * 目标采样率
     */
    virtual int GetSampleRate(int spr) {
        return AUDIO_DEST_SAMPLE_RATE; //44100Hz
    }

private:
    //引擎
    SLObjectItf engineObject = nullptr;
    //引擎接口
    SLEngineItf engineInterface = nullptr;
    //混音器
    SLObjectItf outputMixObject = nullptr;
    //播放器
    SLObjectItf bqPlayerObject = nullptr;
    //播放器接口
    SLPlayItf bqPlayerInterface = nullptr;
    //队列结构
    SLAndroidSimpleBufferQueueItf bqPlayerBufferQueueInterface = nullptr;

    static void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context);

    uint8_t *buffer = nullptr;
    int bufferLen = 0;
};


#endif //VR720_CCAUDIODEVICE_H
