//
// Created by roger on 2020/10/29.
//

#ifndef VR720_CCASSETSMANAGER_H
#define VR720_CCASSETSMANAGER_H

#include "stdafx.h"

class CCMesh;
class CCTexture;
/**
 * 资源管理
 */
class CCAssetsManager {

public:
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
    static BYTE* LoadResource(const char* path, size_t *bufferLength);

    /**
     * 加载字符串资源到内存
     * @param path 资源完整路径
     * @param bufferLength 用来存放缓冲区大小
     * @return 返回数据缓冲区
     */
    static std::string LoadStringResource(const char* path);

    /**
     * 从文件加载贴图
     * @param path 贴图路径
     * @return 返回贴图，如果加载失败返回nullptr
     */
    static CCTexture* LoadTexture(const char* path);

    /**
     * 从文件加载网格
     * @param path 网格路径
     * @return 返回网格，如果加载失败返回nullptr
     */
    static CCMesh* LoadMesh(const char* path);

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

};


#endif //VR720_CCASSETSMANAGER_H
