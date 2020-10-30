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

#if defined(VR720_WINDOWS)
    bool DoOpenFile(const vchar* path);
	void DeleteCurrentFile();
	void OpenCurrentFileAs();
	void OpenFile();
#else
	/**
	 *
	 * @param path
	 * @return
	 */
    bool OpenFile(const vchar* path);
#endif
    /**
     *
     */
    void CloseFile();
    /**
     *
     * @return
     */
    vstring GetCurrentFileName() const ;

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
    const vchar* GetLastError();
private:
    Logger* logger = nullptr;

    vstring lastErr;
    COpenGLRenderer* Render = nullptr;

    CCFileManagerOnCloseCallback onCloseCallback = nullptr;
    void*onCloseCallbackData = nullptr;
};

#endif


