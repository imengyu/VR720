#include "CPngLoader.h"

glm::vec2 CPngLoader::GetImageSize()
{
    return glm::vec2(width, height);
}

BYTE* CPngLoader::GetAllImageData()
{
    if (file && decodeSuccess) {
        const int stride = bytesPerPixel * width;

        SetFullDataSize(stride * height);
        SetLoadingPrecent(0);

        unsigned char* data = new unsigned char[stride * height];
        png_bytep* row_pointers = png_get_rows(png_ptr, info_ptr);
        for (unsigned int i = 0; i < height; ++i)
        {
            SetLoadingPrecent(i / (float)height);

            const unsigned int row = i;//height - i - 1;
            memcpy(data + (row * stride), row_pointers[i], stride);
        }

        SetLoadingPrecent(1);
        return data;
    }
    return nullptr;
}

BYTE* CPngLoader::GetImageChunkData(int x, int y, int chunkW, int chunkH)
{
    if (file && decodeSuccess) {
        SetChunkDataSize(0);
    }
    return nullptr;
}


void CPngLoader::user_read_data(png_structp png_ptr, png_bytep data, png_size_t length)
{
    CPngLoader* loader = (CPngLoader*)png_get_io_ptr(png_ptr);
    fread_s(data, length, 1, length, loader->file);
}

bool CPngLoader::Load(const wchar_t* path)
{
    decodeSuccess = false;
    this->path = path;
    if (file == nullptr) {
        _wfopen_s(&file, path, L"rb");
        if (file) {
            
            png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING, 0, 0, 0);
            info_ptr = png_create_info_struct(png_ptr);
            if (info_ptr == 0)
            {
                png_destroy_read_struct(&png_ptr, png_infopp(0), png_infopp(0));
                return false;
            }

            if (setjmp(png_jmpbuf(png_ptr)))
            {
                png_destroy_read_struct(&png_ptr, png_infopp(0), png_infopp(0));
                return false;
            }

            png_set_read_fn(png_ptr, this, (png_rw_ptr)user_read_data);
            png_set_sig_bytes(png_ptr, 0);
            png_read_png(png_ptr, info_ptr, PNG_TRANSFORM_IDENTITY, 0);

            const unsigned int bit_depth = png_get_bit_depth(png_ptr, info_ptr);
            width = png_get_image_width(png_ptr, info_ptr);
            height = png_get_image_height(png_ptr, info_ptr);

            if (bit_depth != 8)
            {
                png_destroy_info_struct(png_ptr, png_infopp(&info_ptr));
                png_destroy_read_struct(&png_ptr, png_infopp(0), png_infopp(0));

                SetLastError(L"不支持此图像位深度");
                return false;
            }

            colorType = png_get_color_type(png_ptr, info_ptr);
            if ((colorType != PNG_COLOR_TYPE_RGB) && (colorType != PNG_COLOR_TYPE_RGB_ALPHA))
            {
                png_destroy_info_struct(png_ptr, png_infopp(&info_ptr));
                png_destroy_read_struct(&png_ptr, png_infopp(0), png_infopp(0));

                SetLastError(L"不支持此图像颜色类型");
                return false;
            }

            bytesPerPixel = (colorType == PNG_COLOR_TYPE_RGB) ? 3 : 4;

            decodeSuccess = true;
            return true;
        }
    }
    return false;
}

const wchar_t* CPngLoader::GetPath()
{
    return path.c_str();
}

void CPngLoader::Destroy()
{
    if (png_ptr) {
        png_destroy_read_struct(&png_ptr, &info_ptr, NULL);
        png_ptr = nullptr;
        info_ptr = nullptr;
    }
    if (file) {
        fclose(file);
        file = nullptr;
    }
    CImageLoader::Destroy();
}

bool CPngLoader::IsOpened()
{
    return file != nullptr;
}

USHORT CPngLoader::GetImageDepth()
{
    return bytesPerPixel;
}
