//
// Created by roger on 2020/10/29.
//

#ifndef VR720_CCASSETSMANAGER_H
#define VR720_CCASSETSMANAGER_H

#include "stdafx.h"

/**
 * 资源管理
 */
class CCAssetsManager {

public:
#ifdef VR720_WINDOWS
    static const std::wstring GetResourcePath(const wchar_t* typeName, const wchar_t* name);
    static const std::wstring GetDirResourcePath(const wchar_t* dirName, const wchar_t* name);
#endif
    /**
     * 获取资源路径
     * @param typeName 类型文件夹
     * @param name 资源名称
     * @return
     */
    static std::string GetResourcePath(const char* typeName, const char* name);
    /**
     * 获取自定义文件夹路径
     * @param dirName 文件夹
     * @param name 资源名称
     * @return
     */
    static std::string GetDirResourcePath(const char* dirName, const char* name);

    /**
     * 加载资源到内存
     * @param path 资源完整路径
     * @param bufferLength 用来存放缓冲区大小
     * @return 返回数据缓冲区
     */
    static BYTE* LoadResource(const vchar* path, size_t *bufferLength);

#ifdef VR720_ANDROID
    /**
     * 资源管理从JNI初始化
     * @param env JNIEnv
     * @param assetManager getAssets
     */
    static void Android_InitFromJni(JNIEnv* env, jobject assetManager);
    /**
     * 加载 Android assets 文件夹资源。返回缓冲区使用完成后需要 free
     * @param path 资源路径
     * @param buffer 用来存放缓冲区地址
     * @param bufferLength 用来存放缓冲区大小
     * @return 返回是否成功
     */
    static bool Android_LoadAsset(const char* path, BYTE **buffer, size_t *bufferLength);

#endif
};


#endif //VR720_CCASSETSMANAGER_H