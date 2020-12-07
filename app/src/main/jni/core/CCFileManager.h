#pragma once
#ifndef VR720_CCFILEMANAGER_H
#define VR720_CCFILEMANAGER_H
#include "stdafx.h"
#include "CImageLoader.h"
#include <string>

//加载贴图回调
typedef void (*CCFileManagerOnCloseCallback)(void* data);

//文件打开管理
class COpenGLRenderer;
class CCFileManager
{
public:

    CCFileManager(COpenGLRenderer *render);

	/**
	 *
	 * @param path
	 * @return
	 */
    bool OpenFile(const char* path);
    /**
     *
     */
    void CloseFile();
    /**
     *
     * @return
     */
    std::string GetCurrentFileName() const ;

    CImageLoader* CurrentFileLoader = nullptr;
    ImageType CurrenImageType = ImageType::Unknow;

    /**
     *
     * @param c
     * @param data
     */
    void SetOnCloseCallback(CCFileManagerOnCloseCallback c, void* data) {
        onCloseCallback = c;
        onCloseCallbackData = data;
    }

    /**
     *
     * @return
     */
    const char* GetLastError();
private:
    Logger* logger = nullptr;

    std::string lastErr;
    COpenGLRenderer* Render = nullptr;

    CCFileManagerOnCloseCallback onCloseCallback = nullptr;
    void*onCloseCallbackData = nullptr;
};

#endif


