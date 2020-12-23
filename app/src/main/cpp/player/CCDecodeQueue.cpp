//
// Created by roger on 2020/12/21.
//

#include "CCDecodeQueue.h"
#include "CCVideoPlayer.h"
#include <pthread.h>

void CCDecodeQueue::Init(CCVideoPlayerExternalData *data) {

    pthread_mutex_init(&packetRequestLock, nullptr);
    pthread_mutex_init(&packetReleaseLock, nullptr);
    pthread_mutex_init(&frameRequestLock, nullptr);
    pthread_mutex_init(&frameReleaseLock, nullptr);

    AllocFramePool(data->InitParams->FramePoolSize);
    AllocPacketPool(data->InitParams->PacketPoolSize);
}
void CCDecodeQueue::Reset() {

}
void CCDecodeQueue::Destroy() {
    videoQueue.clear();
    audioQueue.clear();
    videoFrameQueue.clear();
    audioFrameQueue.clear();

    ReleasePacketPool();
    ReleaseFramePool();

    pthread_mutex_destroy(&frameReleaseLock);
    pthread_mutex_destroy(&frameRequestLock);
    pthread_mutex_destroy(&packetReleaseLock);
    pthread_mutex_destroy(&packetRequestLock);
}

void CCDecodeQueue::AudioEnqueue(AVPacket *pkt) {
    audioQueue.emplace_back(pkt);
}
AVPacket *CCDecodeQueue::AudioDequeue() {
    AVPacket *pkt = audioQueue.front();
    audioQueue.pop_front();
    return pkt;
}
size_t CCDecodeQueue::AudioQueueSize() {
    return audioQueue.size();
}

void CCDecodeQueue::VideoEnqueue(AVPacket *pkt) {
    videoQueue.emplace_back(pkt);
}
AVPacket *CCDecodeQueue::VideoDequeue()   {
    AVPacket *pkt = videoQueue.front();
    videoQueue.pop_front();
    return pkt;
}
size_t CCDecodeQueue::VideoQueueSize() {
    return videoQueue.size();
}

AVFrame *CCDecodeQueue::VideoFrameDequeue() {
    AVFrame * frame = videoFrameQueue.front();
    videoFrameQueue.pop_front();
    return frame;
}
void CCDecodeQueue::VideoFrameEnqueue(AVFrame *frame) {
    videoFrameQueue.push_back(frame);
}
AVFrame *CCDecodeQueue::AudioFrameDequeue() {
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

void CCDecodeQueue::ReleasePacket(AVPacket *pkt) {

    pthread_mutex_lock(&packetReleaseLock);

    if(packetPool.size() >= externalData->InitParams->PacketPoolSize) {
        av_packet_free(&pkt);
    } else {
        av_packet_unref(pkt);
        packetPool.push_back(pkt);
    }

    pthread_mutex_unlock(&packetReleaseLock);
}
AVPacket* CCDecodeQueue::RequestPacket() {

    pthread_mutex_lock(&packetRequestLock);

    if(packetPool.empty())
        AllocPacketPool(externalData->InitParams->PacketPoolGrowStep);
    if(packetPool.empty()) {
        LOGE("[CCDecodeQueue] Failed to alloc packet pool!");
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
    for(auto it = packetPool.begin(); it != packetPool.end(); it++) {
        AVPacket * packet = *it;
        av_packet_free(&packet);
    }
    packetPool.clear();
}

void CCDecodeQueue::ReleaseFrame(AVFrame *frame) {

    pthread_mutex_lock(&frameReleaseLock);

    if(framePool.size() >= externalData->InitParams->FramePoolSize) {
        av_frame_free(&frame);
    } else {
        av_frame_unref(frame);
        framePool.push_back(frame);
    }

    pthread_mutex_unlock(&frameReleaseLock);
}
AVFrame* CCDecodeQueue::RequestFrame() {

    pthread_mutex_lock(&frameRequestLock);

    if(framePool.empty())
        AllocPacketPool(externalData->InitParams->FramePoolGrowStep);
    if(framePool.empty()) {
        LOGE("[CCDecodeQueue] Failed to alloc packet pool!");
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
    for(auto it = framePool.begin(); it != framePool.end(); it++) {
        AVFrame * frame = *it;
        av_frame_free(&frame);
    }
    framePool.clear();
}