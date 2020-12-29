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
        pthread_mutex_init(&videoDropLock,  nullptr);

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
        pthread_mutex_destroy(&videoDropLock);
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
size_t CCDecodeQueue::AudioQueueSize() {
    return audioQueue.size();
}
void CCDecodeQueue::VideoEnqueue(AVPacket *pkt) {

    pthread_mutex_lock(&videoDropLock);

    videoQueue.emplace_back(pkt);

    pthread_mutex_unlock(&videoDropLock);
}
AVPacket *CCDecodeQueue::VideoDequeue() {
    if(videoQueue.empty())
        return nullptr;

    pthread_mutex_lock(&videoDropLock);

    AVPacket *pkt = videoQueue.front();
    videoQueue.pop_front();

    pthread_mutex_unlock(&videoDropLock);
    return pkt;
}
size_t CCDecodeQueue::VideoQueueSize() {
    return videoQueue.size();
}

//已经解码的数据队列

AVFrame *CCDecodeQueue::VideoFrameDequeue() {

    pthread_mutex_lock(&videoDropLock);

    if(videoFrameQueue.empty()) {
        pthread_mutex_unlock(&videoDropLock);
        return nullptr;
    }

    AVFrame * frame = videoFrameQueue.front();
    videoFrameQueue.pop_front();

    pthread_mutex_unlock(&videoDropLock);

    return frame;
}
void CCDecodeQueue::VideoFrameEnqueue(AVFrame *frame) {

    pthread_mutex_lock(&videoDropLock);

    videoFrameQueue.push_back(frame);

    pthread_mutex_unlock(&videoDropLock);
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

    int droppedCount = 0;

    pthread_mutex_lock(&videoDropLock);

    if (!videoFrameQueue.empty()) {
        for (auto it = videoFrameQueue.begin(); it != videoFrameQueue.end();) {
            AVFrame *frame = *it;
            //还没有解码，需要判断它不是I帧，否则会影响后续的B帧，P帧的解码过程
            if ((frame->flags & AV_PKT_FLAG_KEY) == AV_PKT_FLAG_KEY &&
                it != videoFrameQueue.begin()) {
                ReleaseFrame(frame);
                it = videoFrameQueue.erase(it);
                droppedCount++;
            } else it++;
        }
    }

    pthread_mutex_unlock(&videoDropLock);

    return droppedCount;
}

//两个池
//可以随用随取，防止重复申请释放内存的额外开销

void CCDecodeQueue::ReleasePacket(AVPacket *pkt) {

    if(!initState)
        return;

    pthread_mutex_lock(&packetRequestLock);

    if(packetPool.size() >= externalData->InitParams->PacketPoolSize) {
        av_packet_free(&pkt);
    } else {
        av_packet_unref(pkt);
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