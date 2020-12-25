#include "CCFileManager.h"
#include "COpenGLRenderer.h"
#include "COpenGLView.h"
#include "CCFileReader.h"
#include "../utils/PathHelper.h"
#include "../imageloaders/CImageLoader.h"



void CCFileManager::CloseFile() {
    LOGI(LOG_TAG, "Closing file");
    if (onCloseCallback)
        onCloseCallback(onCloseCallbackData);
    if (CurrentFileLoader != nullptr) {
        CurrentFileLoader->Destroy();
        delete CurrentFileLoader;
        CurrentFileLoader = nullptr;
    }
}
std::string CCFileManager::GetCurrentFileName() const {
    return Path::GetFileName(CurrenImagePath);
}
bool CCFileManager::OpenFile(const char* path) {
    CloseFile();
    if (!Path::Exists(path)) {
        LOGEF(LOG_TAG, "File %s not exists ", path);
        lastErr = VR_ERR_FILE_NOT_EXISTS;
        return false;
    }

    CurrenImagePath = path;

    int fileType = CheckCurrentFileType();
    if(CC_IS_FILE_TYPE_VIDEO(fileType)) {
        LOGI(LOG_TAG, "Skip image check for video file");
        return true;
    }
    LOGDF(LOG_TAG, "fileType : %d", fileType);

    CurrenImageType = CImageLoader::CheckImageType(path);
    CurrentFileLoader = CImageLoader::CreateImageLoaderAuto(path);
    if (CurrentFileLoader == nullptr) {
        lastErr = VR_ERR_FILE_NOT_SUPPORT;
        return false;
    }

    LOGIF(LOG_TAG, "Open file \"%s\" type: %d", path, CurrenImageType);

    glm::vec2 size = CurrentFileLoader->GetImageSize();
    if (size.x > 65536 || size.y > 32768) {
        LOGEF(LOG_TAG, "Image size too big : %dx%d > 65536x32768", (int)size.x, (int)size.y);
        lastErr = VR_ERR_IMAGE_TOO_BIG;
        CloseFile();
        return false;
    }

    if (CurrenImageType != ImageType::JPG && (size.x > 4096 || size.y > 2048)) {
        LOGEF(LOG_TAG, "Image size too big (not jpeg) : %dx%d > 4096x2048", (int)size.x, (int)size.y);
        lastErr = VR_ERR_BIG_IMAGE_AND_NOT_JPG;
        CloseFile();
        return false;
    }
    return true;
}
int CCFileManager::CheckCurrentFileType() const {

    std::string ext = Path::GetExtension(CurrenImagePath);

    if(ext == ".jpg" || ext == ".jpeg") return CC_FILE_TYPE_JPG;
    else if(ext == ".png") return CC_FILE_TYPE_PNG;
    else if(ext == ".bmp") return CC_FILE_TYPE_BMP;
    else if(ext == ".wmv") return CC_FILE_TYPE_WMV;
    else if(ext == ".rm" || ext == ".rmvb") return CC_FILE_TYPE_RMVB;
    else if(ext == ".mpg" || ext == ".mpeg" || ext == ".mpe") return CC_FILE_TYPE_MPG;
    else if(ext == ".3gp") return CC_FILE_TYPE_3GP;
    else if(ext == ".mov") return CC_FILE_TYPE_MOV;
    else if(ext == ".mp4" || ext == ".m4v") return CC_FILE_TYPE_MP4;
    else if(ext == ".avi") return CC_FILE_TYPE_AVI;
    else if(ext == ".mkv") return CC_FILE_TYPE_MKV;
    else if(ext == ".flv") return CC_FILE_TYPE_FLV;

    if(!ext.empty())
        LOGWF(LOG_TAG, "Un support file ext : %s", ext.c_str());
    return 0;
}

CCFileManager::CCFileManager(COpenGLRenderer* render)
{
    logger = Logger::GetStaticInstance();
    lastErr = VR_ERR_SUCCESS;
    Render = render;
}







