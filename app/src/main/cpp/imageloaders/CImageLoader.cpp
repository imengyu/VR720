#include "CImageLoader.h"
#include "CBMPLoader.h"
#include "CJpgLoader.h"
#include "CPngLoader.h"

const BYTE pngHead[8] = { 0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a };
const BYTE bmpHead[2] = { 0x42, 0x4d };
const BYTE jpgHead[3] = { 0xff,0xd8,0xff };

ImageType CImageLoader::CheckImageType(const char* path)
{
	FILE* file = fopen(path, "rb");
	if (file) {
		BYTE buffer[8];
		fread(buffer, 1, 8, file);
		fclose(file);

		if (memcmp(buffer, pngHead, 8) == 0)
			return ImageType::PNG;
		if (memcmp(buffer, jpgHead, 3) == 0)
			return ImageType::JPG;
		if (memcmp(buffer, bmpHead, 2) == 0)
			return ImageType::BMP;
	}
	return ImageType::Unknow;
}
CImageLoader* CImageLoader::CreateImageLoaderType(ImageType type)
{
	switch (type)
	{
	case BMP:
		return new CBMPLoader();
	case JPG:
		return new CJpgLoader();
	case PNG:
		return new CPngLoader();
	default: return nullptr;
	}

}
CImageLoader* CImageLoader::CreateImageLoaderAuto(const char* path)
{
	ImageType type = CheckImageType(path);
	CImageLoader* loader = nullptr;
	switch (type)
	{
	case BMP:
		loader = new CBMPLoader();
		loader->Load(path);
		break;
	case JPG:
		loader = new CJpgLoader();
		loader->Load(path);
		break;
	case PNG:
		loader = new CPngLoader();
		loader->Load(path);
		break;
	default:
		break;
	}
	return loader;
}

glm::vec2 CImageLoader::GetImageSize()
{
	return glm::vec2();
}
glm::vec2 CImageLoader::GetImageScaledSize()
{
	return GetImageSize();
}
BYTE* CImageLoader::GetAllImageData()
{
	return nullptr;
}
USHORT CImageLoader::GetImageDepth()
{
	return 0;
}
BYTE* CImageLoader::GetImageChunkData(int x, int y, int chunkW, int chunkH)
{
	return nullptr;
}

const char* CImageLoader::GetLastError()
{
	return lastError.c_str();;
}
 unsigned long CImageLoader::GetFullDataSize()
{
	return fullDataSize;
}
 unsigned long CImageLoader::GetChunkDataSize()
 {
	 return chunkDataSize;
 }
 const char* CImageLoader::GetPath()
{
	return nullptr;
}
bool CImageLoader::Load(const char* path)
{
	return false;
}
void CImageLoader::Destroy()
{
	if (currentImageInfo) {
		delete currentImageInfo;
		currentImageInfo = nullptr;
	}
}
bool CImageLoader::IsOpened()
{
	return false;
}
float CImageLoader::GetLoadingPrecent()
{
	return loadingPrecent;
}
void CImageLoader::SetLoadingPrecent(float v)
{
	loadingPrecent = v;
}

void CImageLoader::SetLastError(const char* err)
{
	lastError = err;
}
void CImageLoader::SetFullDataSize(unsigned long size)
{
	fullDataSize = size;
}
void CImageLoader::SetChunkDataSize(unsigned long size)
{
	chunkDataSize = size;
}
