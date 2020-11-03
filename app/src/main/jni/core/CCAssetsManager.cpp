//
// Created by roger on 2020/10/29.
//

#include "CCAssetsManager.h"
#include "CCFileReader.h"
#include "CCTexture.h"
#include "CCMesh.h"
#include "CCMeshLoader.h"

#if defined(VR720_WINDOWS)
#include "CApp.h"

const std::wstring CCAssetsManager::GetResourcePath(const wchar_t* typeName, const wchar_t* name)
{
    std::wstring str(CApp::Instance->GetCurrentDir());
    str += L"\\resources\\";
    str += typeName;
    str += L"\\";
    str += name;
    return str;
}
const std::wstring CCAssetsManager::GetDirResourcePath(const wchar_t* dirName, const wchar_t* name)
{
    std::wstring str(CApp::Instance->GetCurrentDir());
    str += L"\\";
    str += dirName;
    str += L"\\";
    str += name;
    return str;
}

#elif defined(VR720_ANDROID)

#include "CCAndroidAssetReader.h"
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

static AAssetManager * gAssetMgr = nullptr;

#endif //VR720_ANDROID

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
#ifdef VR720_WINDOWS
    std::string str(CApp::Instance->GetCurrentDirA());
    str += "/";
    str += dirName;
#else
    std::string str = dirName;
#endif
    str += "/";
    str += name;
    return str;
}
BYTE *CCAssetsManager::LoadResource(const vchar *path, size_t *bufferLength) {
    vstring strpath(path);
#if defined(VR720_WINDOWS) || defined(VR720_LINUX)
    CCFileReader reader(strpath);
    if(reader.Opened())
        return reader.ReadAllByte(bufferLength);
#elif defined(VR720_ANDROID)
    CCAndroidAssetReader reader(strpath, gAssetMgr);
    if(reader.Opened())
        return reader.ReadAllByte(bufferLength);
#endif
    return nullptr;
}
CCTexture *CCAssetsManager::LoadTexture(const vchar *path) {
#if defined(VR720_ANDROID)
    auto * tex = new CCTexture();
    BYTE *buffer = nullptr;
    size_t bufferLength = 0;
    if (Android_LoadAsset(path, &buffer, &bufferLength)) {
        tex->Load(buffer, bufferLength);
        free(buffer);
    }
#else
    auto * tex = new CCTexture();
    tex->Load(path);
#endif
    return tex;
}
CCMesh *CCAssetsManager::LoadMesh(const vchar *path) {
    auto * mesh = new CCMesh();
    CCMeshLoader * meshLoader = CCMeshLoader::GetMeshLoaderByFilePath(path);
    if(meshLoader) {
#if defined(VR720_ANDROID)
        BYTE *buffer = nullptr;
        size_t bufferLength = 0;
        if (Android_LoadAsset(path, &buffer, &bufferLength)) {
            meshLoader->Load(buffer, bufferLength, mesh);
            free(buffer);
        }
#else
        meshLoader->Load(path, mesh);
#endif
    }
    return mesh;
}
vstring CCAssetsManager::LoadStringResource(const vchar *path) {
    vstring str;
    size_t bufferLength = 0;
    BYTE *buffer = LoadResource(path, &bufferLength);
    if(buffer) {
        bufferLength = bufferLength / sizeof(vchar) + 1;
        str.resize(bufferLength);
#if WCHAR_API
        wcsncpy((wchar_t*)str.data(), (wchar_t*)buffer, bufferLength);
#else
        strncpy((char *) str.data(), (char *) buffer, bufferLength);
#endif
        free(buffer);
    } else
        LOGEF(_vstr("LoadStringResource %s failed !"), path);
    return str;
}

#ifdef VR720_ANDROID

void CCAssetsManager::Android_InitFromJni(JNIEnv *env, jobject assetManager) {
    gAssetMgr = AAssetManager_fromJava(env, assetManager);
}
bool CCAssetsManager::Android_LoadAsset(const char *path, BYTE **buffer, size_t *bufferLength) {
    AAsset* asset = AAssetManager_open(gAssetMgr, path,AASSET_MODE_UNKNOWN);
    if(asset == nullptr) {
        LOGWF(_vstr("[CCAssetsManager] Assets: %s not found!"), path);
        return false;
    }
    /*获取文件大小*/
    off_t bufferSize = AAsset_getLength(asset);
    *bufferLength = bufferSize;
    *buffer = (BYTE*)malloc(bufferSize + 1);
    AAsset_read(asset, *buffer, bufferSize);
    /*关闭文件*/
    AAsset_close(asset);

    LOGIF(_vstr("[CCAssetsManager] Assets: %s loaded."), path);
    return true;
}


#endif



