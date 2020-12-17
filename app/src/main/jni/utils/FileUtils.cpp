//
// Created by roger on 2020/12/17.
//

#include "FileUtils.h"
#include "md5.h"
#include "../libjpeg/jconfig.h"
#include "../libjpeg/jpeglib.h"

int FileUtils::ComputeFileMd5(const char *file_path, char *md5_str) {
    int i;
    FILE* file;
    int ret;
    unsigned char data[READ_DATA_SIZE];
    unsigned char md5_value[MD5_SIZE];
    MD5_CTX md5;

    file = fopen(file_path, "r");
    if (file == nullptr) {
        return -1;
    }

    // init md5
    MD5Init(&md5);

    while (true)
    {
        ret = fread(data, 1, READ_DATA_SIZE, file);
        if (-1 == ret)
            return -1;

        MD5Update(&md5, data, ret);
        if (0 == ret || ret < READ_DATA_SIZE)
        {
            break;
        }
    }

    fclose(file);

    MD5Final(&md5, md5_value);

    for(i = 0; i < MD5_SIZE; i++)
    {
        snprintf(md5_str + i*2, 2+1, "%02x", md5_value[i]);
    }
    md5_str[MD5_STR_LEN] = '\0'; // add end

    return 0;
}

extern void jpeg_error_exit(j_common_ptr cinfo);
extern void jpeg_output_message(j_common_ptr cinfo);

int FileUtils::SaveImageBufferToJPEGFile(const char *saveFilePath, unsigned char *data,
                                         size_t bufferSize, int width, int height, int compoents) {

    jpeg_compress_struct cinfo;
    jpeg_error_mgr jerr;
    cinfo.err = jpeg_std_error(&jerr);
    jerr.error_exit = jpeg_error_exit;
    jerr.output_message = jpeg_output_message;
    jpeg_create_compress(&cinfo);

    //保存压缩后的图片
    FILE* fp = fopen(saveFilePath, "wb+");
    jpeg_stdio_dest(&cinfo, fp);

    cinfo.image_width = width;
    cinfo.image_height = height;
    cinfo.input_components = compoents;
    cinfo.in_color_space = compoents == 4 ? JCS_EXT_RGBA : JCS_EXT_RGB; //JCS_GRAYSCALE表示灰度图，JCS_RGB表示彩色图像
    jpeg_set_defaults(&cinfo);
    jpeg_set_quality(&cinfo, 100, TRUE);	//设置压缩质量100表示100%
    jpeg_start_compress(&cinfo, TRUE);
    int nRowStride = width * compoents;
    JSAMPROW row_pointer[1];	// 一行位图
    while (cinfo.next_scanline < cinfo.image_height)
    {
        row_pointer[0] = (JSAMPROW)((unsigned char*)data + cinfo.next_scanline * nRowStride);
        jpeg_write_scanlines(&cinfo, row_pointer, 1);
    }
    jpeg_finish_compress(&cinfo);
    jpeg_destroy_compress(&cinfo);

    fclose(fp);

    return 0;
}
