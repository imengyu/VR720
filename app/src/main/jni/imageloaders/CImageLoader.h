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
	static ImageType CheckImageType(const wchar_t*path);
	static CImageLoader* CreateImageLoaderType(ImageType type);
	static CImageLoader* CreateImageLoaderAuto(const wchar_t* path);

	virtual glm::vec2 GetImageSize();
	virtual glm::vec2 GetImageScaledSize();
	virtual BYTE* GetAllImageData();
	virtual USHORT GetImageDepth();
	virtual BYTE* GetImageChunkData(int xChunkIndex, int yChunkIndex, int chunkW, int chunkH);

	const wchar_t* GetLastError();
	unsigned long GetFullDataSize();
	unsigned long GetChunkDataSize();

	virtual const wchar_t* GetPath();
	ImageFileInfo* GetImageFileInfo();
	virtual bool Load(const wchar_t*path);
	virtual void Destroy();
	virtual bool IsOpened();

	float GetLoadingPrecent();
	void SetLoadingPrecent(float v);

protected:
	void SetLastError(const wchar_t*err);
	void SetLastError(const char* err);
	void SetFullDataSize(unsigned long size);
	void SetChunkDataSize(unsigned long size);

	float loadingPrecent = 0;
	unsigned long fullDataSize = 0;
	unsigned long chunkDataSize = 0;
private:
	std::wstring lastError = std::wstring(L"Not implemented");
	ImageFileInfo* currentImageInfo = nullptr;
};

