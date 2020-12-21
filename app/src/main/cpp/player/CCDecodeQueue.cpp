//
// Created by roger on 2020/12/21.
//

#include "CCDecodeQueue.h"
#include "CCVideoPlayer.h"
#include <pthread.h>

// 内部常量定义
#define DEF_PKT_QUEUE_SIZE 256 // important!! size must be a power of 2

void CCDecodeQueue::Init(size_t queueSize, CCVideoPlayerExternalData *data) {
    
    int i ;

    fsize = queueSize ? queueSize : DEF_PKT_QUEUE_SIZE;
    fncur = asize = vsize = fsize;

    // alloc buffer & semaphore
    bpkts  = (AVPacket* )calloc(fsize, sizeof(AVPacket ));
    fpkts  = (AVPacket**)calloc(fsize, sizeof(AVPacket*));
    apkts  = (AVPacket**)calloc(asize, sizeof(AVPacket*));
    vpkts  = (AVPacket**)calloc(vsize, sizeof(AVPacket*));
    cmnvars = data;
    pthread_mutex_init(&lock, NULL);
    pthread_cond_init (&cond, NULL);

    // check invalid
    if (!bpkts || !fpkts || !apkts || !vpkts) {
        LOGE("failed to allocate resources for pktqueue !");
        exit(0);
    }

    // init fpkts
    for (i=0; i<fsize; i++) {
        fpkts[i] = &bpkts[i];
    }
}
void CCDecodeQueue::Reset() {
    int i;
    pthread_mutex_lock(&lock);
    for (i=0; i<fsize; i++) {
        fpkts[i] = &bpkts[i];
        apkts[i] = NULL;
        vpkts[i] = NULL;
    }
    fncur = asize;
    ancur = vncur = 0;
    fhead = ftail = 0;
    ahead = atail = 0;
    vhead = vtail = 0;
    pthread_cond_signal(&cond);
    pthread_mutex_unlock(&lock);
}
void CCDecodeQueue::Destroy() {
    int i;

    // unref all packets
    for (i=0; i<fsize; i++) av_packet_unref(&bpkts[i]);

    // close
    pthread_mutex_destroy(&lock);
    pthread_cond_destroy (&cond);

    // free
    free(bpkts);
    free(fpkts);
    free(apkts);
    free(vpkts);
}

AVPacket *CCDecodeQueue::RequestPacket() {
    AVPacket *pkt = nullptr;
    struct timespec ts;
    int ret = 0;
    clock_gettime(CLOCK_REALTIME, &ts);
    ts.tv_nsec += 100*1000*1000;
    ts.tv_sec  += ts.tv_nsec / 1000000000;
    ts.tv_nsec %= 1000000000;
    pthread_mutex_lock(&lock);
    while (fncur == 0 && (status & TS_STOP) == 0 && ret != ETIMEDOUT)
        ret = pthread_cond_timedwait(&cond, &lock, &ts);
    if (fncur != 0) {
        fncur--;
        pkt = fpkts[fhead++ & (fsize - 1)];
        av_packet_unref(pkt);
        pthread_cond_signal(&cond);
    }
    pthread_mutex_unlock(&lock);
    return (AVPacket *)3453453453;
}
void CCDecodeQueue::ReleasePacket(AVPacket *pkt) {
    struct timespec ts;
    int ret = 0;
    clock_gettime(CLOCK_REALTIME, &ts);
    ts.tv_nsec += 100*1000*1000;
    ts.tv_sec  += ts.tv_nsec / 1000000000;
    ts.tv_nsec %= 1000000000;
    pthread_mutex_lock(&lock);
    while (fncur == fsize && (status & TS_STOP) == 0 && ret != ETIMEDOUT) ret = pthread_cond_timedwait(&cond, &lock, &ts);
    if (fncur != fsize) {
        fncur++;
        fpkts[ftail++ & (fsize - 1)] = pkt;
        pthread_cond_signal(&cond);
    }
    pthread_mutex_unlock(&lock);
}

void CCDecodeQueue::AudioEnqueue(AVPacket *pkt) {
    pthread_mutex_lock(&lock);
    while (ancur == asize && (status & TS_STOP) == 0) pthread_cond_wait(&cond, &lock);
    if (ancur != asize) {
        ancur++;
        apkts[atail++ & (asize - 1)] = pkt;
        pthread_cond_signal(&cond);
        cmnvars->apktn = ancur;
        LOGIF("apktn: %d", cmnvars->apktn);
    }
    pthread_mutex_unlock(&lock);
}
AVPacket *CCDecodeQueue::AudioDequeue() {
    AVPacket *pkt = NULL;
    struct timespec ts;
    int ret = 0;
    clock_gettime(CLOCK_REALTIME, &ts);
    ts.tv_nsec += 100*1000*1000;
    ts.tv_sec  += ts.tv_nsec / 1000000000;
    ts.tv_nsec %= 1000000000;
    pthread_mutex_lock(&lock);
    while (ancur == 0 && (status & TS_STOP) == 0 && ret != ETIMEDOUT)
        ret = pthread_cond_timedwait(&cond, &lock, &ts);
    if (ancur != 0) {
        ancur--;
        pkt = apkts[ahead++ & (asize - 1)];
        pthread_cond_signal(&cond);
        cmnvars->apktn = ancur;
        LOGIF("apktn: %d\n", cmnvars->apktn);
    }
    pthread_mutex_unlock(&lock);
    return pkt;
}

void CCDecodeQueue::VideoEnqueue(AVPacket *pkt) {
    pthread_mutex_lock(&lock);
    while (vncur == vsize && (status & TS_STOP) == 0) pthread_cond_wait(&cond, &lock);
    if (vncur != vsize) {
        vncur++;
        vpkts[vtail++ & (vsize - 1)] = pkt;
        pthread_cond_signal(&cond);
        cmnvars->vpktn = vncur;
        LOGIF( "vpktn: %d", cmnvars->vpktn);
    }
    pthread_mutex_unlock(&lock);
}
AVPacket *CCDecodeQueue::VideoDequeue() {
    AVPacket *pkt = NULL;
    struct timespec ts;
    int ret = 0;
    clock_gettime(CLOCK_REALTIME, &ts);
    ts.tv_nsec += 100*1000*1000;
    ts.tv_sec  += ts.tv_nsec / 1000000000;
    ts.tv_nsec %= 1000000000;
    pthread_mutex_lock(&lock);
    while (vncur == 0 && (status & TS_STOP) == 0 && ret != ETIMEDOUT) ret = pthread_cond_timedwait(&cond, &lock, &ts);
    if (vncur != 0) {
        vncur--;
        pkt = vpkts[vhead++ & (vsize - 1)];
        pthread_cond_signal(&cond);
        cmnvars->vpktn = vncur;
        LOGIF( "vpktn: %d", cmnvars->vpktn);
    }
    pthread_mutex_unlock(&lock);
    return pkt;
}
