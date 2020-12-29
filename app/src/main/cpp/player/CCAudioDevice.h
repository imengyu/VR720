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

typedef struct {
    uint8_t *buffer;
    int bufferLen;
} AUDIO_BUFFER;

class CCAudioDevice;
typedef void (*CCAudioDeviceBufferCallback)(CCAudioDevice* dev, void* customData, uint8_t **buf, int *len);

class CCAudioDevice {
    const char* LOG_TAG = "CCAudioDevice";
public:

    CCAudioDevice(CCVideoPlayerExternalData *externalData);

    virtual void Destroy();
    virtual void Write(uint8_t *buf, int len, int64_t pts);
    virtual void Pause(int pause);
    virtual void Reset();
    virtual void SetBufferCallback(CCAudioDeviceBufferCallback callback, void* data);

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
    virtual size_t GetQueueSize() {
        return buffers.size(); //44100Hz
    }



private:

    bool isPlaying = false;
    bool isSurePlaying = false;

    CCAudioDeviceBufferCallback bufferCallback = nullptr;
    void* bufferCallbackData = nullptr;

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

    std::list<AUDIO_BUFFER*>buffers;

    AUDIO_BUFFER* popBuffer();
};


#endif //VR720_CCAUDIODEVICE_H
