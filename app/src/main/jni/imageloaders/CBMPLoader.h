#pragma once
#include "CImageLoader.h"

class CBMPLoader : public CImageLoader
{
private:
	glm::vec2 GetImageSize() override;
	BYTE* GetAllImageData() override;
	BYTE* GetImageChunkData(int x, int y, int chunkW, int chunkH) override;

	bool Load(const wchar_t* path) override;
	const wchar_t* GetPath() override;
	void Destroy() override;
	bool IsOpened() override;
	USHORT GetImageDepth() override;

	FILE* file = nullptr;
	std::wstring path;
	ULONG fileLength = 0;
	BITMAPFILEHEADER bitmapHeader;
	BITMAPINFOHEADER bitmapInfoHeader;
	UINT bitmapSize;
};

