//
// Created by roger on 2020/10/29.
//

#include "CCAssetsManager.h"
#include "CCFileReader.h"
#include "CCTexture.h"
#include "CCMesh.h"
#include "CCMeshLoader.h"
#include "CCAndroidAssetReader.h"
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#define LOG_TAG "CCAssetsManager"

static AAssetManager * gAssetMgr = nullptr;

std::string CCAssetsManager::GetResourcePath(const char* typeName, const char* name)
{
#ifdef VR720_WINDOWS
    std::string str(CApp::Instance->GetCurrentDirA());
    str += "//resources//";
#else
    std::string str = "resources/";
#endif
    str += typeName;
    str += "/";
    str += name;
    return str;
}
std::string CCAssetsManager::GetDirResourcePath(const char* dirName, const char* name)
{
    std::string str = dirName;
    str += "/";
    str += name;
    return str;
}
BYTE *CCAssetsManager::LoadResource(const char *path, size_t *bufferLength) {
    std::string strpath(path);
    CCAndroidAssetReader reader(strpath, gAssetMgr);
    if(reader.Opened())
        return reader.ReadAllByte(bufferLength);
    return nullptr;
}
CCTexture *CCAssetsManager::LoadTexture(const char *path) {
    auto * tex = new CCTexture();
    tex->backupData = true;
    BYTE *buffer = nullptr;
    size_t bufferLength = 0;
    if (Android_LoadAsset(path, &buffer, &bufferLength)) {
        tex->Load(buffer, bufferLength);
        free(buffer);
    }
    return tex;
}
CCMesh *CCAssetsManager::LoadMesh(const char *path) {
    auto * mesh = new CCMesh();
    CCMeshLoader * meshLoader = CCMeshLoader::GetMeshLoaderByFilePath(path);
    if(meshLoader) {
        BYTE *buffer = nullptr;
        size_t bufferLength = 0;
        if (Android_LoadAsset(path, &buffer, &bufferLength)) {
            meshLoader->Load(buffer, bufferLength, mesh);
            free(buffer);
        }
    }
    return mesh;
}
std::string CCAssetsManager::LoadStringResource(const char *path) {
    std::string str;
    size_t bufferLength = 0;
    BYTE *buffer = LoadResource(path, &bufferLength);
    if(buffer) {
        bufferLength = bufferLength / sizeof(char) + 1;
        str.resize(bufferLength);
        memset((void*)str.data(),0, bufferLength);
        strncpy((char *) str.data(), (char *) buffer, bufferLength - 1);
        free(buffer);
    } else
        LOGEF(LOG_TAG, "LoadStringResource %s failed !", path);
    return str;
}

void CCAssetsManager::Android_InitFromJni(JNIEnv *env, jobject assetManager) {
    gAssetMgr = AAssetManager_fromJava(env, assetManager);
}
bool CCAssetsManager::Android_LoadAsset(const char *path, BYTE **buffer, size_t *bufferLength) {
    AAsset* asset = AAssetManager_open(gAssetMgr, path,AASSET_MODE_UNKNOWN);
    if(asset == nullptr) {
        LOGWF(LOG_TAG, "Assets: %s not found!", path);
        return false;
    }
    /*获取文件大小*/
    off_t bufferSize = AAsset_getLength(asset);
    *bufferLength = bufferSize;
    *buffer = (BYTE*)malloc(bufferSize + 1);
    AAsset_read(asset, *buffer, bufferSize);
    /*关闭文件*/
    AAsset_close(asset);

    LOGIF(LOG_TAG, "Assets: %s loaded.", path);
    return true;
}




