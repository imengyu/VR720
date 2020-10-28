#pragma once
#include "CImageLoader.h"
#include <../libpng/png.h>

class CPngLoader : public CImageLoader
{
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
	png_structp png_ptr = nullptr;
	png_infop info_ptr = nullptr;
	png_byte colorType = 0;
	unsigned int width = 0;
	unsigned int height = 0;
	int bytesPerPixel = 0;;

	static void user_read_data(png_structp png_ptr, png_bytep data, png_size_t length);

	bool decodeSuccess = false;
};

