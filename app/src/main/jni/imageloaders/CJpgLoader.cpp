#include "CJpgLoader.h"
#include "CApp.h"

char jpeg_last_err[JMSG_LENGTH_MAX];

void jpeg_error_exit(j_common_ptr cinfo) {
    (*cinfo->err->output_message) (cinfo);
    jpeg_destroy(cinfo);
}
void jpeg_output_message(j_common_ptr cinfo) {
    char buffer[JMSG_LENGTH_MAX];
    (*cinfo->err->format_message) (cinfo, buffer);
    CApp::Instance->GetLogger()->Log2(L"%hs", buffer);
    strcpy_s(jpeg_last_err, buffer);
}

glm::vec2 CJpgLoader::GetImageSize()
{
    if (decodeSuccess)
        return glm::vec2(width, height);
    return glm::vec2();
}

glm::vec2 CJpgLoader::GetImageScaledSize()
{
    return scaledSize;
}

BYTE* CJpgLoader::GetAllImageData()
{
    if (decodeSuccess) {
        struct jpeg_decompress_struct cinfo;
        struct jpeg_error_mgr jerr;
        FILE* file;
        JDIMENSION width, height;
        _wfopen_s(&file, path.c_str(), L"rb");
        if (file) {
            fseek(file, 0, SEEK_SET);
            jpeg_create_decompress(&cinfo);
            cinfo.err = jpeg_std_error(&jerr);
            jerr.error_exit = jpeg_error_exit;
            jerr.output_message = jpeg_output_message;
            jpeg_stdio_src(&cinfo, file);
            if (jpeg_read_header(&cinfo, TRUE) != JPEG_HEADER_OK) {
                SetLastError("Bad jpeg header");
                return nullptr;
            }

            width = cinfo.image_width;//图像宽度
            height = cinfo.image_height;//图像高度
            JDIMENSION rat = (int)ceil(width / 4096.0);
            if (rat > 2 && rat <= 16) {
                cinfo.scale_num = 1;
                cinfo.scale_denom = rat;
            }
            else if (rat > 16) {
                SetLastError( "Too big image");
                return nullptr;
            }

            if (width <= 0 || height <= 0) {
                SetLastError("Bad image size");
                return nullptr;
            }

            jpeg_start_decompress(&cinfo);

            JDIMENSION t_depth = cinfo.output_components;
            JDIMENSION t_width = cinfo.output_width;
            JDIMENSION t_height = cinfo.output_height;
            UINT t_bufferSize = t_width * t_height * t_depth;
            UCHAR* t_buffer = (UCHAR*)malloc(t_bufferSize);
            memset(t_buffer, 0, t_bufferSize);

            scaledSize = glm::vec2(t_width, t_height);
            SetFullDataSize(t_bufferSize);
            SetLoadingPrecent(0);

            UINT bfferSize = cinfo.output_width * cinfo.output_components;
            JSAMPARRAY buffer = (*cinfo.mem->alloc_sarray)((j_common_ptr)&cinfo, JPOOL_IMAGE, bfferSize, 1);
            UCHAR* point = t_buffer;

            while (cinfo.output_scanline < t_height)//逐行读取位图数据
            {
                SetLoadingPrecent(cinfo.output_scanline / (float)t_height);

                jpeg_read_scanlines(&cinfo, buffer, 1); //读取一行jpg图像数据到buffer
                memcpy(point, *buffer, bfferSize); //将buffer中的数据逐行给src_buff
                point += bfferSize; //指针偏移一行
            }

            if (height > t_height)
                jpeg_skip_scanlines(&cinfo, height - t_height);
            jpeg_finish_decompress(&cinfo);
            jpeg_destroy_decompress(&cinfo);
            fclose(file);

            SetLoadingPrecent(1.0f);

            return t_buffer;
        }

        SetLastError("File not exists");
        return nullptr;
    }
    return nullptr;
}

BYTE* CJpgLoader::GetImageChunkData(int x, int y, int chunkW, int chunkH)
{
    SetChunkDataSize(chunkW * chunkH * depth);

    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;
    FILE* file;
    int width, height, depth;
    _wfopen_s(&file, path.c_str(), L"rb");
    if (file) {
        fseek(file, 0, SEEK_SET);
        jpeg_create_decompress(&cinfo);
        cinfo.err = jpeg_std_error(&jerr);
        jerr.error_exit = jpeg_error_exit;
        jerr.output_message = jpeg_output_message;
        jpeg_stdio_src(&cinfo, file);
        if (jpeg_read_header(&cinfo, TRUE) != JPEG_HEADER_OK) {
            SetLastError("Bad jpeg header");
            return nullptr;
        }

        SetLoadingPrecent(0);

        width = cinfo.image_width;//图像宽度
        height = cinfo.image_height;//图像高度
        depth = cinfo.num_components;//图像深度

        if (width <= 0 || height <= 0) {
            strcpy_s(jpeg_last_err, "Bad image size");
            return nullptr;
        }

        int t_depth = depth;
        int t_width = width;
        int t_height = height;

        JDIMENSION ux = x, uw = chunkW, end = y + chunkH;

        jpeg_start_decompress(&cinfo);

        jpeg_crop_scanline(&cinfo, &ux, &uw);
        jpeg_skip_scanlines(&cinfo, y);


        UINT cropFixX = x - ux;
        UINT t_bufferSize = uw * chunkH * depth;
        UCHAR* t_buffer = (UCHAR*)malloc(t_bufferSize);
        memset(t_buffer, 0, t_bufferSize);

        UINT bfferSize = cinfo.output_width * cinfo.output_components;
        JSAMPARRAY buffer = (*cinfo.mem->alloc_sarray)((j_common_ptr)&cinfo, JPOOL_IMAGE, bfferSize, 1);
        UCHAR* point = t_buffer;

        while (cinfo.output_scanline < end)//逐行读取位图数据
        {
            SetLoadingPrecent(cinfo.output_scanline / (float)end);
            jpeg_read_scanlines(&cinfo, buffer, 1); //读取一行jpg图像数据到buffer
            memcpy(point, *buffer, bfferSize); //将buffer中的数据逐行给src_buff
            point += bfferSize; //指针偏移一行
        }

        jpeg_skip_scanlines(&cinfo, height - chunkH - y);
        jpeg_finish_decompress(&cinfo);

        jpeg_destroy_decompress(&cinfo);
        fclose(file);

        UINT t_fix_bufferSize = uw * chunkH * depth;
        UCHAR* t_fix_buffer = (UCHAR*)malloc(t_bufferSize);
        memset(t_fix_buffer, 0, t_fix_bufferSize);

        SetChunkDataSize(t_fix_bufferSize);

        UINT ox = 0, oy = 0, cx = 0, cy = 0;
        UINT cw = uw, ci = 0;;

        for (UINT i = 0; i < t_bufferSize; )
        {
            if (cx >= cropFixX && cx < cropFixX + chunkW)
            {

                t_fix_buffer[ci++] = t_buffer[i];
                t_fix_buffer[ci++] = t_buffer[i + 1];
                t_fix_buffer[ci++] = t_buffer[i + 2];

                ox++;
                if (ox >= (UINT)chunkW)
                {
                    ox = 0;
                    oy++;
                }
            }

            cx++;
            if (cx >= cw)
            {
                cx = 0;
                cy++;
            }

            i += depth;
        }

        free(t_buffer);
        
        SetLoadingPrecent(1);
        return t_fix_buffer;
    }

    SetLastError("File not exists");
    return nullptr;
}

bool CJpgLoader::Load(const wchar_t* path)
{
    decodeSuccess = false;
    this->path = path;

    FILE *file = nullptr;
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;
    if (file == nullptr) {
        _wfopen_s(&file, path, L"rb");
        if (file) {
            fseek(file, 0, SEEK_SET);
            jpeg_create_decompress(&cinfo);
            cinfo.err = jpeg_std_error(&jerr);
            jerr.error_exit = jpeg_error_exit;
            jerr.output_message = jpeg_output_message;
            jpeg_stdio_src(&cinfo, file);
            if (jpeg_read_header(&cinfo, TRUE) != JPEG_HEADER_OK) {
                SetLastError(jpeg_last_err);
                fclose(file);
                return false;
            }

            width = cinfo.image_width;//图像宽度
            height = cinfo.image_height;//图像高度
            depth = cinfo.num_components;//图像深度

            if (width <= 0 || height <= 0)
            {
                SetLastError(L"Bad image size");
                fclose(file);
                return false;
            }

            jpeg_destroy_decompress(&cinfo);

            decodeSuccess = true;
            fclose(file);
            return true;
        }
    }
    return false;
}

const wchar_t* CJpgLoader::GetPath()
{
    return path.c_str();
}
void CJpgLoader::Destroy()
{   
    path = L"";
    CImageLoader::Destroy();
}
bool CJpgLoader::IsOpened()
{
    return decodeSuccess;
}

USHORT CJpgLoader::GetImageDepth()
{
    return depth;
}
