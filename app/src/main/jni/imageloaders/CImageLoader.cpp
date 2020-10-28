#include "CImageLoader.h"
#include "CBMPLoader.h"
#include "CJpgLoader.h"
#include "CPngLoader.h"
#include "StringHlp.h"

const BYTE pngHead[8] = { 0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a };
const BYTE bmpHead[2] = { 0x42, 0x4d };
const BYTE jpgHead[3] = { 0xff,0xd8,0xff };

ImageType CImageLoader::CheckImageType(const wchar_t* path)
{
	FILE* file;
	_wfopen_s(&file, path, L"rb");
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
	}
	return nullptr;
}
CImageLoader* CImageLoader::CreateImageLoaderAuto(const wchar_t* path)
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

const wchar_t* CImageLoader::GetLastError()
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
 const wchar_t* CImageLoader::GetPath()
{
	return nullptr;
}
 ImageFileInfo* CImageLoader::GetImageFileInfo() {
	 if (currentImageInfo) 
		 return currentImageInfo;
	 const wchar_t* path = GetPath();
	 currentImageInfo = new ImageFileInfo();
	 HANDLE hFile = CreateFile(path, 0, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
	 if (hFile) {
		 LARGE_INTEGER liFileSize;
		 GetFileSizeEx(hFile, &liFileSize);
		 currentImageInfo->fileSize = liFileSize.QuadPart;

		 FILETIME ftCreate, ftAccess, ftWrite;
		 SYSTEMTIME stUTC1, stLocal1, stUTC2, stLocal2, stUTC3, stLocal3;

		 if (GetFileTime(hFile, &ftCreate, &ftAccess, &ftWrite)) {
			 FileTimeToSystemTime(&ftCreate, &stUTC1);
			 FileTimeToSystemTime(&ftAccess, &stUTC2);
			 FileTimeToSystemTime(&ftWrite, &stUTC3);

			 SystemTimeToTzSpecificLocalTime(NULL, &stUTC1, &stLocal1);
			 SystemTimeToTzSpecificLocalTime(NULL, &stUTC2, &stLocal2);
			 SystemTimeToTzSpecificLocalTime(NULL, &stUTC3, &stLocal3);

			 currentImageInfo->Create = StringHlp::FormatString("%d/%02d/%02d",
				 stLocal1.wYear, stLocal1.wMonth, stLocal1.wDay);
			 currentImageInfo->Access = StringHlp::FormatString("%d/%02d/%02d",
				 stLocal2.wYear, stLocal2.wMonth, stLocal2.wDay);
			 currentImageInfo->Write = StringHlp::FormatString("%d/%02d/%02d",
				 stLocal3.wYear, stLocal3.wMonth, stLocal3.wDay);
		 }
	 }
	 return currentImageInfo;
 }

bool CImageLoader::Load(const wchar_t* path)
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

void CImageLoader::SetLastError(const wchar_t* err)
{
	lastError = err;
}
void CImageLoader::SetLastError(const char* err)
{
	wchar_t* e = StringHlp::AnsiToUnicode(err);
	SetLastError(e);
	StringHlp::FreeStringPtr(e);
}
void CImageLoader::SetFullDataSize(unsigned long size)
{
	fullDataSize = size;
}
void CImageLoader::SetChunkDataSize(unsigned long size)
{
	chunkDataSize = size;
}
