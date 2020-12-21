#pragma once
#include "CImageLoader.h"

//BMP结构
//**********************

typedef struct tagBITMAPFILEHEADER {
	WORD    bfType;
	DWORD   bfSize;
	WORD    bfReserved1;
	WORD    bfReserved2;
	DWORD   bfOffBits;
} BITMAPFILEHEADER, *LPBITMAPFILEHEADER, *PBITMAPFILEHEADER;
typedef struct tagBITMAPINFOHEADER{
	DWORD      biSize;
	long       biWidth;
	long       biHeight;
	WORD       biPlanes;
	WORD       biBitCount;
	DWORD      biCompression;
	DWORD      biSizeImage;
	long       biXPelsPerMeter;
	long       biYPelsPerMeter;
	DWORD      biClrUsed;
	DWORD      biClrImportant;
} BITMAPINFOHEADER, *LPBITMAPINFOHEADER, *PBITMAPINFOHEADER;

class CBMPLoader : public CImageLoader
{
private:
	glm::vec2 GetImageSize() override;
	BYTE* GetAllImageData() override;
	BYTE* GetImageChunkData(int x, int y, int chunkW, int chunkH) override;

	bool Load(const char* path) override;
	const char* GetPath() override;
	void Destroy() override;
	bool IsOpened() override;
	USHORT GetImageDepth() override;

	FILE* file = nullptr;
	std::string path;
	ULONG fileLength = 0;
	BITMAPFILEHEADER bitmapHeader;
	BITMAPINFOHEADER bitmapInfoHeader;
	UINT bitmapSize;
};

