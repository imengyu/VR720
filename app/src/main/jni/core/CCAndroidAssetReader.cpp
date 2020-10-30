//
// Created by roger on 2020/10/30.
//

#include "CCAndroidAssetReader.h"

#if defined(VR720_ANDROID)

BYTE *CCAndroidAssetReader::ReadAllByte(size_t *size) {
    return CCFileReader::ReadAllByte(size);
}
BYTE CCAndroidAssetReader::ReadByte() {
    return CCFileReader::ReadByte();
}
void CCAndroidAssetReader::Read(BYTE *arr, size_t offset, size_t count) {
    Seek(offset, SEEK_CUR);
    AAsset_read(asset, arr, count);
}
void CCAndroidAssetReader::Seek(size_t i, int seekType) {
    AAsset_seek(asset, i, seekType);
}
void CCAndroidAssetReader::Seek(size_t i) {
    AAsset_seek(asset, i, SEEK_SET);
}

size_t CCAndroidAssetReader::Length() {
    return assetSize;
}
void CCAndroidAssetReader::Close() {
    CCFileReader::Close();
    CloseFileHandle();
}
bool CCAndroidAssetReader::Opened() {
    return asset != nullptr;
}

CCAndroidAssetReader::~CCAndroidAssetReader() {
    CloseFileHandle();
}
CCAndroidAssetReader::CCAndroidAssetReader(std::string &path, AAssetManager *assetManager) {
    asset = AAssetManager_open(assetManager, path.c_str(), AASSET_MODE_UNKNOWN);
    if(asset != nullptr) {
        /*获取文件大小*/
        assetSize = AAsset_getLength(asset);
    }
}

void CCAndroidAssetReader::CloseFileHandle() {
    if(asset != nullptr) {
        AAsset_close(asset);
        asset = nullptr;
    }
}

#endif


