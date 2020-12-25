#pragma once
#ifndef VR720_CCFILEMANAGER_H
#define VR720_CCFILEMANAGER_H
#include "stdafx.h"
#include "../imageloaders/CImageLoader.h"
#include "CCErrors.h"
#include <string>

//加载贴图回调
typedef void (*CCFileManagerOnCloseCallback)(void* data);

#define CC_FILE_TYPE_JPG 3
#define CC_FILE_TYPE_PNG 4
#define CC_FILE_TYPE_BMP 5

#define CC_FILE_TYPE_IMG_MAX 10
#define CC_FILE_TYPE_MP4 11
#define CC_FILE_TYPE_AVI 12
#define CC_FILE_TYPE_WMV 13
#define CC_FILE_TYPE_RMVB 14
#define CC_FILE_TYPE_MPG 15
#define CC_FILE_TYPE_3GP 16
#define CC_FILE_TYPE_MOV 17
#define CC_FILE_TYPE_MKV 18
#define CC_FILE_TYPE_FLV 19

#define CC_FILE_TYPE_VIDEO_MAX 20

//判断文件类型是不是图像
#define CC_IS_FILE_TYPE_IMAGE(x) x < CC_FILE_TYPE_IMG_MAX
//判断文件类型是不是视频
#define CC_IS_FILE_TYPE_VIDEO(x) (x > CC_FILE_TYPE_IMG_MAX && x < CC_FILE_TYPE_VIDEO_MAX)

/**
 * 文件打开管理
 */
class COpenGLRenderer;
class CCFileManager
{
public:

    CCFileManager(COpenGLRenderer *render);

	/**
	 * 打开文件
	 * @param path 文件路径
	 * @return 返回是否成功
	 */
    bool OpenFile(const char* path);
    /**
     * 关闭文件
     */
    void CloseFile();
    /**
     * 获取当前打开文件的路径
     * @return 返回路径
     */
    std::string GetCurrentFileName() const ;

    /**
     * 获取当前打开文件的类型
     * @return 返回文件的类型
     */
    int CheckCurrentFileType() const;

    CImageLoader* CurrentFileLoader = nullptr;
    ImageType CurrenImageType = ImageType::Unknow;
    std::string CurrenImagePath;

    /**
     * 设置文件关闭回调
     * @param c 回调
     * @param data 回调自定义数据
     */
    void SetOnCloseCallback(CCFileManagerOnCloseCallback c, void* data) {
        onCloseCallback = c;
        onCloseCallbackData = data;
    }

    /**
     * 获取上一个错误
     * @return 上一个错误
     */
    const int GetLastError() { return lastErr; }

    const char* LOG_TAG = "CCFileManager";
private:
    Logger* logger = nullptr;

    int lastErr;
    COpenGLRenderer* Render = nullptr;

    CCFileManagerOnCloseCallback onCloseCallback = nullptr;
    void*onCloseCallbackData = nullptr;

};

#endif


