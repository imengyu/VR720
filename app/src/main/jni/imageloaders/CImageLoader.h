#pragma once
#include "stdafx.h"

struct ImageFileInfo {
	long long fileSize;
	std::string Create, Access, Write;
};
enum ImageType {
	Unknow,
	BMP,
	JPG,
	PNG,
};
class CImageLoader
{
public:
	static ImageType CheckImageType(const char*path);
	static CImageLoader* CreateImageLoaderType(ImageType type);
	static CImageLoader* CreateImageLoaderAuto(const char* path);

	virtual glm::vec2 GetImageSize();
	virtual glm::vec2 GetImageScaledSize();
	virtual BYTE* GetAllImageData();
	virtual USHORT GetImageDepth();
	virtual BYTE* GetImageChunkData(int xChunkIndex, int yChunkIndex, int chunkW, int chunkH);

	const char* GetLastError();
	unsigned long GetFullDataSize();
	unsigned long GetChunkDataSize();

	virtual const char* GetPath();
	virtual bool Load(const char*path);
	virtual void Destroy();
	virtual bool IsOpened();

	float GetLoadingPrecent();
	void SetLoadingPrecent(float v);

protected:
	void SetLastError(const char*err);
	void SetFullDataSize(unsigned long size);
	void SetChunkDataSize(unsigned long size);

	float loadingPrecent = 0;
	unsigned long fullDataSize = 0;
	unsigned long chunkDataSize = 0;
private:
	std::string lastError = std::string("Not implemented");
	ImageFileInfo* currentImageInfo = nullptr;
};

