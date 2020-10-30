//
// Created by roger on 2020/10/30.
//

#ifndef VR720_CCANDROIDASSETREADER_H
#define VR720_CCANDROIDASSETREADER_H

#include "stdafx.h"
#include "CCFileReader.h"

#if defined(VR720_ANDROID)

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

//安卓 Assets 读取器
class CCAndroidAssetReader : public CCFileReader  {

public:

    /**
     * 创建简易文件读取器
     * @param path 目标文件路径
     * @param assetManager 安卓AssetManager对象
     */
    CCAndroidAssetReader(vstring & path, AAssetManager *assetManager);
    ~CCAndroidAssetReader();

    /**
     * 获取文件是否已经打开
     * @return
     */
    bool Opened() override;
    /**
     * 关闭文件
     */
    void Close() override;

    /**
     * 获取文件长度
     * @return 文件长度
     */
    size_t Length() override;
    /**
     * 移动fp指针至文件开始的偏移位置
     * @param i 指定位置
     * @param seekType
     */
    void Seek(size_t i) override;
    /**
     * 移动fp指针
     * @param i 指定位置
     * @param seekType 位置的类型，SEEK_*
     */
    void Seek(size_t i, int seekType) override;

    /**
     *
     * @param arr 缓冲区
     * @param offset 读取偏移
     * @param count 读取个数
     */
    void Read(BYTE* arr, size_t offset, size_t count) override;
    /**
     * 读取一个字节
     * @return 返回字节
     */
    BYTE ReadByte() override;
    /**
     * 读取整个文件
     * @param size 返回缓冲区大小
     * @return 返回缓冲区
     */
    BYTE* ReadAllByte(size_t *size) override;


private:
    AAsset* asset = nullptr;
    off_t assetSize = 0;

    void CloseFileHandle();
};

#endif

#endif //VR720_CCANDROIDASSETREADER_H
