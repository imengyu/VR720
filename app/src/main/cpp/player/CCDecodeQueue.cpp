//
// Created by roger on 2020/12/21.
//

#include "CCDecodeQueue.h"
#include "CCVideoPlayer.h"
#include <pthread.h>

void CCDecodeQueue::Init(CCVideoPlayerExternalData *data) {

    if(!initState) {
        initState = true;

        pthread_mutex_init(&packetRequestLock, nullptr);
        pthread_mutex_init(&frameRequestLock, nullptr);

        AllocFramePool(data->InitParams->FramePoolSize);
        AllocPacketPool(data->InitParams->PacketPoolSize);

        externalData = data;
    }
}
void CCDecodeQueue::Reset() {

}
void CCDecodeQueue::Destroy() {

    if(initState) {
        initState = false;

        ClearAll();

        ReleasePacketPool();
        ReleaseFramePool();

        pthread_mutex_destroy(&frameRequestLock);
        pthread_mutex_destroy(&packetRequestLock);
    }
}

//包数据队列

void CCDecodeQueue::AudioEnqueue(AVPacket *pkt) {
    audioQueue.emplace_back(pkt);
}
AVPacket *CCDecodeQueue::AudioDequeue() {
    if(audioQueue.empty())
        return nullptr;
    AVPacket *pkt = audioQueue.front();
    audioQueue.pop_front();
    return pkt;
}
void CCDecodeQueue::AudioQueueBack(AVPacket * packet) {
    audioQueue.push_front(packet);
}
size_t CCDecodeQueue::AudioQueueSize() {
    return audioQueue.size();
}
void CCDecodeQueue::VideoEnqueue(AVPacket *pkt) {
    videoQueue.emplace_back(pkt);
}
AVPacket *CCDecodeQueue::VideoDequeue() {
    if(videoQueue.empty())
        return nullptr;

    AVPacket *pkt = videoQueue.front();
    videoQueue.pop_front();

    return pkt;
}
void CCDecodeQueue::VideoQueueBack(AVPacket * packet) {
    videoQueue.push_front(packet);
}
size_t CCDecodeQueue::VideoQueueSize() {
    return videoQueue.size();
}

//已经解码的数据队列

AVFrame *CCDecodeQueue::VideoFrameDequeue() {

    if(videoFrameQueue.empty())
        return nullptr;

    AVFrame * frame = videoFrameQueue.front();
    videoFrameQueue.pop_front();

    return frame;
}
void CCDecodeQueue::VideoFrameEnqueue(AVFrame *frame) {
    videoFrameQueue.push_back(frame);
}
AVFrame *CCDecodeQueue::AudioFrameDequeue() {

    if(audioFrameQueue.empty())
        return nullptr;

    AVFrame * frame = audioFrameQueue.front();
    audioFrameQueue.pop_front();
    return frame;
}
void CCDecodeQueue::AudioFrameEnqueue(AVFrame *frame) {
    audioFrameQueue.push_back(frame);
}

size_t CCDecodeQueue::VideoFrameQueueSize() {
    return videoFrameQueue.size();
}
size_t CCDecodeQueue::AudioFrameQueueSize() {
    return audioFrameQueue.size();
}

//清空队列数据
void CCDecodeQueue::ClearAll() {
    for(auto frame : videoFrameQueue) ReleaseFrame(frame);
    for(auto frame : audioFrameQueue) ReleaseFrame(frame);
    for(auto packet : videoQueue) ReleasePacket(packet);
    for(auto packet : audioQueue) ReleasePacket(packet);

    videoFrameQueue.clear();
    audioFrameQueue.clear();
    videoQueue.clear();
    audioQueue.clear();
}

//视频太慢丢包
int CCDecodeQueue::VideoDrop(double targetClock) {

    double frameClock;
    int droppedCount = 0;
    if (!videoFrameQueue.empty()) {
        for (auto it = videoFrameQueue.begin(); it != videoFrameQueue.end();) {
            AVFrame *frame = *it;
            frameClock = frame->best_effort_timestamp == AV_NOPTS_VALUE ?
                         frame->pts : frame->best_effort_timestamp * av_q2d(externalData->VideoTimeBase);
            if((droppedCount >= videoFrameQueue.size() / 3
                || frameClock >= targetClock)
                && it != videoFrameQueue.begin())
                break;
            else {
                ReleaseFrame(frame);
                it = videoFrameQueue.erase(it);
                droppedCount++;
            }
        }
    }

    return droppedCount;
}

//两个池
//可以随用随取，防止重复申请释放内存的额外开销

void CCDecodeQueue::ReleasePacket(AVPacket *pkt) {

    if(pkt)
        av_packet_unref(pkt);
    if(!initState)
        return;

    pthread_mutex_lock(&packetRequestLock);

    if(packetPool.size() >= externalData->InitParams->PacketPoolSize) {
        av_packet_free(&pkt);
    } else {

        packetPool.push_back(pkt);
    }

    pthread_mutex_unlock(&packetRequestLock);
}
AVPacket* CCDecodeQueue::RequestPacket() {

    if(!initState)
        return nullptr;

    pthread_mutex_lock(&packetRequestLock);

    if(packetPool.empty())
        AllocPacketPool(externalData->InitParams->PacketPoolGrowStep);
    if(packetPool.empty()) {
        LOGE(LOG_TAG, "Failed to alloc packet pool!");
        return nullptr;
    }

    AVPacket * packet = packetPool.front();
    packetPool.pop_front();

    pthread_mutex_unlock(&packetRequestLock);

    return packet;
}
void CCDecodeQueue::AllocPacketPool(int size) {
    for(int i = 0; i < size; i++)
        packetPool.push_back(av_packet_alloc());
}
void CCDecodeQueue::ReleasePacketPool() {
    for(auto packet : packetPool)
        av_packet_free(&packet);
    packetPool.clear();
}

void CCDecodeQueue::ReleaseFrame(AVFrame *frame) {

    if(!initState)
        return;

    pthread_mutex_lock(&frameRequestLock);

    if(framePool.size() >= externalData->InitParams->FramePoolSize) {
        av_frame_free(&frame);
    } else {
        av_frame_unref(frame);
        framePool.push_back(frame);
    }

    pthread_mutex_unlock(&frameRequestLock);
}
AVFrame* CCDecodeQueue::RequestFrame() {

    if(!initState)
        return nullptr;

    pthread_mutex_lock(&frameRequestLock);

    if(framePool.empty())
        AllocFramePool(externalData->InitParams->FramePoolGrowStep);
    if(framePool.empty()) {
        LOGE(LOG_TAG, "Failed to alloc frame pool!");
        return nullptr;
    }

    AVFrame * frame = framePool.front();
    framePool.pop_front();

    pthread_mutex_unlock(&frameRequestLock);

    return frame;
}
void CCDecodeQueue::AllocFramePool(int size) {
    for(int i = 0; i < size; i++)
        framePool.push_back(av_frame_alloc());
}
void CCDecodeQueue::ReleaseFramePool() {
    for(auto frame : framePool)
        av_frame_free(&frame);
    framePool.clear();
}