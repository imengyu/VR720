//
// Created by roger on 2020/12/21.
//

#ifndef VR720_CCDECODEQUEUE_H
#define VR720_CCDECODEQUEUE_H
#include "stdafx.h"
extern "C" {
#include "libavcodec/avcodec.h"
}

class CCVideoPlayerExternalData;
class CCDecodeQueue {

public:

    void Init(size_t queueSize, CCVideoPlayerExternalData *data);
    void Reset();
    void Destroy();

    AVPacket* RequestPacket(); // request a packet
    void ReleasePacket(AVPacket *pkt); // release a packet

    void AudioEnqueue(AVPacket *pkt); // enqueue a packet to audio-queue
    AVPacket* AudioDequeue(); // dequeue a audio packet from audio-queue

    void VideoEnqueue(AVPacket *pkt);  // enqueue a packet to video-queue
    AVPacket* VideoDequeue(); // dequeue a audio packet from video-queue

private:
    int        fsize;
    int        asize;
    int        vsize;
    int        fncur;
    int        ancur;
    int        vncur;
    int        fhead;
    int        ftail;
    int        ahead;
    int        atail;
    int        vhead;
    int        vtail;
#define TS_STOP (1 << 0)
    int        status;
    AVPacket  *bpkts; // packet buffers
    AVPacket **fpkts; // free packets
    AVPacket **apkts; // audio packets
    AVPacket **vpkts; // video packets
    CCVideoPlayerExternalData *cmnvars;
    pthread_mutex_t lock;
    pthread_cond_t  cond;
};


#endif //VR720_CCDECODEQUEUE_H
