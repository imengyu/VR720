#include "CCFileManager.h"
#include "COpenGLRenderer.h"
#include "COpenGLView.h"
#include "CImageLoader.h"
#include "CCFileReader.h"
#include "PathHelper.h"

void CCFileManager::CloseFile() {
    logger->Log(_vstr("Closing file"));
    if (onCloseCallback)
        onCloseCallback(onCloseCallbackData);
    if (CurrentFileLoader != nullptr) {
        CurrentFileLoader->Destroy();
        delete CurrentFileLoader;
        CurrentFileLoader = nullptr;
    }
}
vstring CCFileManager::GetCurrentFileName() const {
    if (CurrentFileLoader) 
        return Path::GetFileName(CurrentFileLoader->GetPath());
    return std::string();
}
bool CCFileManager::OpenFile(const vchar* path) {
    CloseFile();
    if (!Path::Exists(path)) {
        lastErr = "文件不存在";
        return false;
    }
    CurrenImageType = CImageLoader::CheckImageType(path);
    CurrentFileLoader = CImageLoader::CreateImageLoaderAuto(path);
    if (CurrentFileLoader == nullptr) {
        lastErr = "不支持这种文件格式";
        return false;
    }

    LOGI("Open file \"%s\" type: %d", path, CurrenImageType);

    glm::vec2 size = CurrentFileLoader->GetImageSize();
    if (size.x > 65536 || size.y > 32768) {
        LOGE("Image size too big : %dx%d > 65536x32768", (int)size.x, (int)size.y);
        lastErr = "我们暂时无法打开非常大的图像（图像大小超过65536x32768）";
        CloseFile();
        return false;
    }

    if (CurrenImageType != ImageType::JPG && (size.x > 4096 || size.y > 2048)) {
        LOGE("Image size too big (not jpeg) : %dx%d > 4096x2048", (int)size.x, (int)size.y);
        lastErr = "大图像请转为JPEG格式打开（图像大小超过4096x2048）";
        CloseFile();
        return false;
    }
    return true;
}
const vchar* CCFileManager::GetLastError()
{
    return lastErr.c_str();
}

CCFileManager::CCFileManager(COpenGLRenderer* render)
{
    logger = Logger::GetStaticInstance();
    Render = render;
}







