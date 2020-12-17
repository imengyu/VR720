//
// Created by roger on 2020/12/17.
//

#ifndef VR720_FILEUTILS_H
#define VR720_FILEUTILS_H

#include <stdio.h>

#define READ_DATA_SIZE	1024
#define MD5_SIZE		16
#define MD5_STR_LEN		(MD5_SIZE * 2)

class FileUtils {
public:
    static int ComputeFileMd5(const char *file_path, char *md5_str);
    static int SaveImageBufferToJPEGFile(const char *saveFilePath, unsigned char *data, size_t bufferSize,
            int width, int height, int compoents);

};


#endif //VR720_FILEUTILS_H
