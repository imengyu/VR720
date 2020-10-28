#include "CBMPLoader.h"

glm::vec2 CBMPLoader::GetImageSize()
{
    if (file)
        return glm::vec2(bitmapInfoHeader.biWidth, bitmapInfoHeader.biHeight);
    return glm::vec2();
}

BYTE* CBMPLoader::GetAllImageData()
{
    if (file) {

        ULONG bitSize = bitmapInfoHeader.biBitCount == 32 ? 4 : 3;

        ULONG bitStartOffest = bitmapHeader.bfOffBits;
        ULONG fullLength = bitmapInfoHeader.biWidth * bitmapInfoHeader.biHeight * bitSize;

        SetFullDataSize(fullLength);
        SetLoadingPrecent(0);

        fseek(file, bitStartOffest, SEEK_SET);

        ULONG index = fullLength - 1;
        LPBYTE data = (LPBYTE)malloc(fullLength);
        memset(data, 0, fullLength);

        while (index >= bitSize - 1) {
            BYTE buffer[4];
            fread(buffer, 1, bitSize, file);

            SetLoadingPrecent(index / (float)fullLength);

            data[index] = buffer[0];//B
            data[index - 1] = buffer[1];//G
            data[index - 2] = buffer[2];//R

            if (index > bitSize) index -= bitSize;
            else  index = 0;
        }

        SetLoadingPrecent(1);
        return data;
    }
    return nullptr;
}

BYTE* CBMPLoader::GetImageChunkData(int x, int y, int chunkW, int chunkH)
{
    if (file) {

        if (x < 0 || x >  bitmapInfoHeader.biWidth || y < 0 || y > bitmapInfoHeader.biHeight) {
            SetLastError(L"Chunk pos not currect");
            return nullptr;
        }
        if (x + chunkW > bitmapInfoHeader.biWidth || y + chunkH > bitmapInfoHeader.biHeight) {
            SetLastError(L"Chunk size not currect");
            return nullptr;
        }

        ULONG bitSize = bitmapInfoHeader.biBitCount == 32 ? 4 : 3;
        ULONG bitStartOffest = bitmapHeader.bfOffBits;
        ULONG fullLength = bitmapInfoHeader.biWidth * bitmapInfoHeader.biHeight * bitSize;
        ULONG dataLength = chunkW * chunkH * bitSize;
        LPBYTE data = (LPBYTE)malloc(dataLength);
        memset(data, 0, dataLength);

        SetChunkDataSize(dataLength);

        ULONG index = dataLength - 1 - chunkW * bitSize;
        ULONG sOff = 0;
        for (int i = 0; i < chunkH; i++) {

            sOff = (bitmapInfoHeader.biHeight - (y + i )- 1 + (x)) * bitSize;
            fseek(file, sOff, SEEK_SET);

            for (int j = 0; j < chunkW; j++) {

                BYTE buffer[4];
                fread(buffer, 1, bitSize, file);

                data[index] = buffer[2];//R
                data[index + 1] = buffer[1];//G
                data[index + 2] = buffer[0];//B

                index++;
            }

            index -= chunkW * bitSize;
        }

        return data;
    }
    return nullptr;
}

bool CBMPLoader::Load(const wchar_t* path)
{
    this->path = path;
    if (file == nullptr) {
        _wfopen_s(&file, path, L"rb");
        if (file) {
            fread(&bitmapHeader, 1, sizeof(bitmapHeader), file);
            fread(&bitmapInfoHeader, 1, sizeof(bitmapInfoHeader), file);
            fseek(file, 0, SEEK_END);
            fileLength = ftell(file);
            if (bitmapInfoHeader.biBitCount < 24) { //Only support 24 and 32 bmp
                SetLastError(L"不支持灰度色或256色位图格式");
                return false;
            }
            if (bitmapInfoHeader.biCompression != 0) {
                SetLastError(L"不支持该位图格式");
                return false;
            }

            return true;
        }
    }
    return false;
}

const wchar_t* CBMPLoader::GetPath()
{
    return path.c_str();
}

void CBMPLoader::Destroy()
{
    if (file) {
        fclose(file);
        file = nullptr;
    }
    CImageLoader::Destroy();
}

bool CBMPLoader::IsOpened()
{
    return file != nullptr;
}

USHORT CBMPLoader::GetImageDepth()
{
    return bitmapInfoHeader.biBitCount == 32 ? 4 : 3;
}
