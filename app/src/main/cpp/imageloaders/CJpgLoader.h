#pragma once
#include "CImageLoader.h"
#include "../libjpeg/jconfig.h"
#include "../libjpeg/jpeglib.h"

class CJpgLoader : public CImageLoader
{
	glm::vec2 GetImageSize() override;
	glm::vec2 GetImageScaledSize() override;
	BYTE* GetAllImageData() override;
	BYTE* GetImageChunkData(int x, int y, int chunkW, int chunkH) override;

	bool Load(const char* path) override;
	const char* GetPath() override;
	void Destroy() override;
	bool IsOpened() override;
	USHORT GetImageDepth() override;

	std::string path;
	UINT width = 0;
	UINT height = 0;
	USHORT depth = 0;
	glm::vec2 scaledSize;
	bool decodeSuccess = false;
};

