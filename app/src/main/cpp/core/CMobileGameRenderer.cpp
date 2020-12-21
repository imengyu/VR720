#include "stdafx.h"
#include "CMobileGameRenderer.h"
#include "COpenGLView.h"
#include "CCRenderGlobal.h"
#include "CCProperties.h"
#include "CMobileGameUIEventDistributor.h"
#include "CCMaterial.h"
#include "../imageloaders/CImageLoader.h"
#include "../utils/CStringHlp.h"
#include "../utils/FileUtils.h"
#include "../utils/PathHelper.h"
#include "../utils/CommonUtils.h"
#include <ctime>

jobject applicationContext;
int applicationKey;
int applicationKey2;

CMobileGameRenderer::CMobileGameRenderer()
{
    logger = Logger::GetStaticInstance();
}
CMobileGameRenderer::~CMobileGameRenderer() = default;

//文件管理
//*************************

void CMobileGameRenderer::SetOpenFilePath(const char* path)
{
	currentOpenFilePath = path;
}
void CMobileGameRenderer::DoOpenFile()
{
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::MarkLoadingStart);
    if (fileManager->OpenFile(currentOpenFilePath.c_str())) {

        if(CC_IS_FILE_TYPE_IMAGE(fileManager->CheckCurrentFileType()))
            DoOpenAsImage();
        else if(CC_IS_FILE_TYPE_VIDEO(fileManager->CheckCurrentFileType()))
            DoOpenAsVideo();
        else {
            lastImageError = std::string("Not support file format");
            logger->LogError("[GameCore] Unknown file ext");
            ShowErrorDialog();
        }
    }
    else {
        lastImageError = std::string(fileManager->GetLastError());
        logger->LogError("[GameCore] File open failed : %s", lastImageError.c_str());
        ShowErrorDialog();
    }
}
void CMobileGameRenderer::DoOpenAsImage() {
    logger->Log("[GameCore] File open as image");

    //如果开启了缓存，那么同时加载缓存
    glm::vec2 size = fileManager->CurrentFileLoader->GetImageSize();
    thisFileShouldSaveCache = false;
    thisFileShouldLoadInCache = false;
    if(enableViewCache && (size.x > 4000 || size.y > 2000))
        TestToLoadTextureImageCache();

    //加载主图
    renderer->panoramaThumbnailTex = texLoadQueue->Push(new CCTexture(), 0, 0, -1);//MainTex
    renderer->panoramaThumbnailTex->backupData = true;
    renderer->panoramaTexPool.push_back(renderer->panoramaThumbnailTex);
    renderer->UpdateMainModelTex();

    //检查是否需要分片并加载
    needTestImageAndSplit = true;
    fileOpened = true;
    renderer->renderOn = true;
    uiInfo->currentImageOpened = true;
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
}
void CMobileGameRenderer::DoOpenAsVideo() {
    logger->Log("[GameCore] File open as video");

    //加载主图
    renderer->panoramaThumbnailTex = new CCTexture();
    renderer->panoramaThumbnailTex->backupData = true;
    renderer->panoramaTexPool.push_back(renderer->panoramaThumbnailTex);
    renderer->UpdateMainModelTex();



    fileOpened = true;
    renderer->renderOn = true;
    uiInfo->currentImageOpened = true;
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
}
void CMobileGameRenderer::MarkCloseFile() {
    if(fileOpened)
        shouldCloseFile = true;
    else
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::FileClosed);
}
void CMobileGameRenderer::ShowErrorDialog() {
    fileOpened = false;
    renderer->renderOn = false;
    uiInfo->currentImageOpened = false;
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::MarkLoadFailed);
}
void CMobileGameRenderer::TestSplitImageAndLoadTexture() {
    glm::vec2 size = fileManager->CurrentFileLoader->GetImageSize();
    shouldSplitFullImage = size.x > 4096 || size.y > 2048;
    if (shouldSplitFullImage) {

        //计算分块大小
        float chunkW = size.x / 2048.0f;
        float chunkH = size.y / 1024.0f;
        if (chunkW < 2) chunkW = 2;
        if (chunkH < 2) chunkH = 2;
        if (chunkW > 64 || chunkH > 32) {
            logger->LogError2("[GameCore] Too big image (%.2f, %.2f) that cant split chunks.", chunkW, chunkH);
            shouldSplitFullImage = false;
            return;
        }
        int chunkWi = (int)ceil(chunkW), chunkHi = (int)ceil(chunkH);

        uiInfo->currentImageAllChunks = chunkWi * chunkHi;
        uiInfo->currentImageLoadChunks = 0;
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);

        //设置渲染分块参数
        if(fullChunkLoadEnabled) {
            logger->Log("[GameCore] Image use split mode , size: %d, %d", chunkWi, chunkHi);
            renderer->sphereFullSegmentX =
                    renderer->sphereSegmentX + (renderer->sphereSegmentX % chunkWi);
            renderer->sphereFullSegmentY =
                    renderer->sphereSegmentY + (renderer->sphereSegmentY % chunkHi);
            renderer->GenerateFullModel(chunkWi, chunkHi);
        }


    }
    else {
        //不分块加载
        uiInfo->currentImageAllChunks = 0;
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
    }

    SwitchMode(mode);
}
void CMobileGameRenderer::TestToLoadTextureImageCache() {
    char md5_str[MD5_STR_LEN + 1];
    if(FileUtils::ComputeFileMd5(currentOpenFilePath.c_str(), md5_str) != 0) {
        logger->LogWarn("[GameCore] Compute file md5 failed");
        return;
    }

    currentFileCachePath = viewCachePath + md5_str;
    if(!Path::Exists(currentFileCachePath)) {
        thisFileShouldSaveCache = true;
        logger->Log("[GameCore] Image should save cache in this load");
        return;
    }

    thisFileShouldLoadInCache = true;
    logger->Log("[GameCore] Image should load cache this time");
}

//测试(不再使用)
//*************************

/*

GLuint d_glProgram;

void CreateTestGlProgram() {
    //init gl
    GLuint glProgram;
    GLuint vertexShader;
    GLuint fragmentShader;

    //shader code
    const char *shader_vertex = "uniform mediump mat4 MODELVIEWPROJECTIONMATRIX;\n"
                                "attribute vec4 POSITION;\n"
                                "void main(){\n"
                                "  gl_Position = POSITION;\n"
                                "}";
    const char *shader_fragment = "precision mediump float;\n"
                                  "void main(){\n"
                                  "   gl_FragColor = vec4(0,0,1,1);\n"
                                  "}";
    glProgram = glCreateProgram();


    if(glProgram == 0){
        LOGE("init glProgram error!");
        return ;
    }

    d_glProgram = glProgram;

//    glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
    glClearColor ( 1.0f, 1.0f, 1.0f, 0.0f );

    //vertexShader
    vertexShader = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertexShader,1,&shader_vertex,NULL);

    //fragmentShader
    fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragmentShader,1,&shader_fragment,NULL);
    glCompileShader(vertexShader);
    glCompileShader(fragmentShader);

    glAttachShader(glProgram,vertexShader);
    glAttachShader(glProgram,fragmentShader);

    glLinkProgram(glProgram);
}
void RenderTest() {
    glClear(GL_DEPTH_BUFFER_BIT|GL_COLOR_BUFFER_BIT);
    GLfloat vertexs[] = {
            0.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f
    };

    glUseProgram(d_glProgram);
    glVertexAttribPointer(0,3,GL_FLOAT,GL_FALSE,0,vertexs);
    glEnableVertexAttribArray(0);

    glDrawArrays(GL_TRIANGLES,0,3);
}

*/

//*************************

void CMobileGameRenderer::GlobalInit(JNIEnv *env, jobject context) {
    applicationContext = context;
    applicationKey2 = CommonUtils::CheckAppSignature(env, applicationContext, &applicationKey);
}
bool CMobileGameRenderer::ReInit() {
    if (renderInitFinish) {
        LOGI("[CMobileGameRenderer] ReInit!");
        ReBufferAllData();
        renderer->ReInit();
        return true;
    }
    return false;
}
bool CMobileGameRenderer::Init()
{
    LOGI("[CMobileGameRenderer] Init!");

    camera = new CCPanoramaCamera();
    cameraMercatorCylinderCapture = new CCamera();
    renderer = new CCPanoramaRenderer(this);
    fileManager = new CCFileManager(this);
    texLoadQueue = new CCTextureLoadQueue();
    uiInfo = new CCGUInfo();

    renderer->Init();
    texLoadQueue->SetLoadHandle(LoadTexCallback, this);
    camera->SetMode(CCPanoramaCameraMode::CenterRoate);
    camera->SetFOVChangedCallback(CameraFOVChanged, this);
    camera->SetOrthoSizeChangedCallback(CameraOrthoSizeChanged, this);
    camera->Background = CColor::FromString("#000000");
    fileManager->SetOnCloseCallback(FileCloseCallback, this);

    cameraMercatorCylinderCapture->ClippingNear = 0.1f;
    cameraMercatorCylinderCapture->ClippingFar = 200.0f;
    cameraMercatorCylinderCapture->Reset();
    cameraMercatorCylinderCapture->SetFOV(90);

    View->SetCamera(camera);
    View->SetManualDestroyCamera(true);
    View->SetMouseCallback(MouseCallback);
    View->SetZoomViewCallback(ScrollCallback);

    SwitchMode(mode);

    //CreateTestGlProgram();

    //renderer->renderPanoramaFullTest = true;
    //renderer->renderPanoramaFullRollTest = true;
    //renderer->renderPanoramaATest = true;
    //TestSplitImageAndLoadTexture();

    renderInitFinish = true;
	return true;
}
void CMobileGameRenderer::Destroy()
{
    if(destroying)
        return;

    destroying = true;

    LOGI("[CMobileGameRenderer] Destroy!");

    if (uiInfo != nullptr) {
        delete uiInfo;
        uiInfo = nullptr;
    }
    if (fileManager != nullptr) {
        delete fileManager;
        fileManager = nullptr;
    }
    if (texLoadQueue != nullptr) {
        delete texLoadQueue;
        texLoadQueue = nullptr;
    }
    if (camera != nullptr) {
        View->SetCamera(nullptr);
        delete camera;
        camera = nullptr;
    }
    if (cameraMercatorCylinderCapture != nullptr) {
        delete cameraMercatorCylinderCapture;
        cameraMercatorCylinderCapture = nullptr;
    }
    if (renderer != nullptr) {
        renderer->Destroy();
        delete renderer;
        renderer = nullptr;
    }
    if (uiEventDistributor != nullptr) {
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::DestroyComplete);
        delete uiEventDistributor;
        uiEventDistributor = nullptr;
    }
    if (applicationKey2 > 0) applicationKey = 0;

    applicationKey2 = 0;
}
void CMobileGameRenderer::Resize(int Width, int Height)
{
    COpenGLRenderer::Resize(Width, Height);

    if(!vREnabled) glViewport(0, 0, Width, Height);
}

//输入处理
//*************************

void CMobileGameRenderer::MouseCallback(COpenGLView* view, float xpos, float ypos, int button, int type) {
    auto* renderer = (CMobileGameRenderer*)view->GetRenderer();
    switch (type) {
        case ViewMouseEventType::ViewMouseMouseDown: {
            renderer->lastX = xpos;
            renderer->lastY = ypos;
            break;
        }
        case ViewMouseEventType::ViewMouseMouseMove: {
            renderer->xoffset = xpos - renderer->lastX;
            renderer->yoffset = renderer->lastY - ypos; // reversed since y-coordinates go from bottom to top

            renderer->lastX = xpos;
            renderer->lastY = ypos;

            //旋转球体
            if (renderer->mode <= PanoramaMode::PanoramaMercator) {
                float xoffset = -renderer->xoffset * renderer->GetMouseSensitivity();
                float yoffset = -renderer->yoffset * renderer->GetMouseSensitivity();

                renderer->renderer->RotateModel(xoffset, yoffset);
            }
            //移动模型
            else if (renderer->mode == PanoramaMode::PanoramaFull360 || renderer->mode == PanoramaMode::PanoramaFullOrginal) {
                float xoffset = -renderer->xoffset * renderer->GetMouseSensitivityInFlat();
                float yoffset = -renderer->yoffset * renderer->GetMouseSensitivityInFlat();
                renderer->renderer->MoveModel(xoffset, yoffset);
            }
            break;
        }
        case ViewMouseEventType::ViewMouseMouseUp: {
            if(renderer->DragCurrentVelocity.x > 0 || renderer->DragCurrentVelocity.y > 0) {
                renderer->VelocityDragLastOffest.x = renderer->xoffset;
                renderer->VelocityDragLastOffest.y = renderer->yoffset;
                renderer->VelocityDragCurrentIsInSim = true;
            }
            break;
        }
    }
}
void CMobileGameRenderer::ScrollCallback(COpenGLView* view, float x, float yoffset, int button, int type) {
    auto* renderer = (CMobileGameRenderer*)view->GetRenderer();
    if(type == ViewMouseEventType::ViewMouseMouseWhell)
        renderer->camera->ProcessMouseScroll(yoffset);
    else if(type == ViewMouseEventType::ViewZoomEvent)
        renderer->camera->ProcessZoomChange(yoffset);
}
void CMobileGameRenderer::KeyMoveCallback(CCameraMovement move) {
    if (mode <= PanoramaMode::PanoramaMercator) {
        switch (move)
        {
        case CCameraMovement::ROATE_UP:
            if(!gyroEnabled)
                renderer->RotateModelForce(0, -RotateSpeed * View->GetDeltaTime());
            break;
        case CCameraMovement::ROATE_DOWN:
            if(!gyroEnabled)
                renderer->RotateModelForce(0, RotateSpeed * View->GetDeltaTime());
            break;
        case CCameraMovement::ROATE_LEFT:
            renderer->RotateModelForce(-RotateSpeed * View->GetDeltaTime(), 0);
            break;
        case CCameraMovement::ROATE_RIGHT:
            renderer->RotateModelForce(RotateSpeed * View->GetDeltaTime(), 0);
            break;
        default:
            break;
        }
    }
    else if (mode == PanoramaMode::PanoramaFull360 || mode == PanoramaMode::PanoramaFullOrginal) {
        switch (move)
        {
        case CCameraMovement::ROATE_UP:
            renderer->MoveModelForce(0, -MoveSpeed * View->GetDeltaTime());
            break;
        case CCameraMovement::ROATE_DOWN:
            renderer->MoveModelForce(0, MoveSpeed * View->GetDeltaTime());
            break;
        case CCameraMovement::ROATE_LEFT:
            renderer->MoveModelForce(-MoveSpeed * View->GetDeltaTime(), 0);
            break;
        case CCameraMovement::ROATE_RIGHT:
            renderer->MoveModelForce(MoveSpeed * View->GetDeltaTime(), 0);
            break;
        default:
            break;
        }
    }
}

//绘制
//*************************

void CMobileGameRenderer::Render(float FrameTime)
{
    //loop count
    //===========================

    if (shouldCloseFile) {
        shouldCloseFile = false;
        fileManager->CloseFile();
    }

    if (destroying)
        return;

    if (shouldDestroy) {
        LOGI("[CMobileGameRenderer] Start destroy in renderer thread");
        Destroy();
        return;
    }

    //渲染
    //===========================

    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);

    //渲染测试
    //RenderTest();

    //墨卡托圆柱投影模式
    renderer->currentFrameMercatorCylinder = (mode == PanoramaMode::PanoramaMercator);

    //VR双屏模式的渲染
    if(vREnabled && mode <= PanoramaMode::PanoramaMercator) {

        //墨卡托圆柱投影模式
        if(renderer->currentFrameMercatorCylinder) {
            renderer->RenderPreMercatorCylinder();

            SetVRViewPort(0);
            renderer->RenderMercatorCylinder();

            SetVRViewPort(1);
            renderer->RenderMercatorCylinder();
        } else {

            SetVRViewPort(0);
            renderer->Render(View->GetDeltaTime());

            SetVRViewPort(1);
            renderer->Render(View->GetDeltaTime());
        }

    }
    else {

        //普通渲染
        SetVRViewPort(-1);
        renderer->Render(View->GetDeltaTime());
    }

    //在渲染线程中加载贴图
    //===========================

    texLoadQueue->ResolveRender();

    if (shouldOpenFile && renderInitFinish) {
        shouldOpenFile = false;
        if(applicationKey == -5036)
            DoOpenFile();
    }

    if (needTestImageAndSplit) {
        needTestImageAndSplit = false;
        TestSplitImageAndLoadTexture();
    }

    if (renderer->reqLoadBuiltInResources) {
        renderer->reqLoadBuiltInResources = false;
        renderer->LoadBuiltInResources();
    }

    //平滑移动处理
    //===========================

    if(VelocityDragCurrentIsInSim) {
        DragCurrentVelocity.x -= VelocityDragCutSensitivity;
        DragCurrentVelocity.y -= VelocityDragCutSensitivity;

        float targetPosX = lastX;
        float targetPosY = lastY;

        if(DragCurrentVelocity.x > 0)
            targetPosX += (DragCurrentVelocity.x) * (VelocityDragLastOffest.x < 0 ? -1.0f : 1.0f);
        if(DragCurrentVelocity.y > 0)
            targetPosY -= (DragCurrentVelocity.y) * (VelocityDragLastOffest.y < 0 ? -1.0f : 1.0f);

        MouseCallback(View, targetPosX, targetPosY, 0, ViewMouseEventType::ViewMouseMouseMove);

        if(DragCurrentVelocity.x <= 0 && DragCurrentVelocity.y <= 0)
            VelocityDragCurrentIsInSim = false;
    }

}
void CMobileGameRenderer::RenderUI()
{

}
void CMobileGameRenderer::Update()
{
    if (destroying)
        return;

    //加载队列处理
    //===========================

    if(texLoadQueue)
        texLoadQueue->ResolveMain();

    //加检测按键
    //===========================

    if (View->GetKeyPress(VR720_KEY_LEFT)) KeyMoveCallback(CCameraMovement::ROATE_LEFT);
    if (View->GetKeyPress(VR720_KEY_UP)) KeyMoveCallback(CCameraMovement::ROATE_UP);
    if (View->GetKeyPress(VR720_KEY_RIGHT)) KeyMoveCallback(CCameraMovement::ROATE_RIGHT);
    if (View->GetKeyPress(VR720_KEY_DOWN)) KeyMoveCallback(CCameraMovement::ROATE_DOWN);
}
void CMobileGameRenderer::ReBufferAllData() {

}
void CMobileGameRenderer::SetVRViewPort(int index) {

    int w = Width, h = Height;
    if(index == -1) {
        glViewport(0, 0, w, h);
        renderer->SetCurrentFrameVRValue(true, w, h);
        return;
    }

    if(w > h) { //横屏

        if(index == 0) {
            glViewport(0, 0, w / 2, h);
            renderer->SetCurrentFrameVRValue(true, w / 2, h);
        } else if(index == 1) {
            glViewport(w / 2, 0, w / 2, h);
            renderer->SetCurrentFrameVRValue(true, w / 2, h);
        }


    } else { //竖屏

        if(index == 0) {
            glViewport(0, 0, w, h / 2);
            renderer->SetCurrentFrameVRValue(true, w, h / 2);
        } else if(index == 1) {
            glViewport(0, h / 2, w, h / 2);
            renderer->SetCurrentFrameVRValue(true, w, h / 2);
        }
    }

}

//逻辑控制
//*************************

TextureLoadQueueDataResult* CMobileGameRenderer::LoadMainTexCallback(TextureLoadQueueInfo* info, CCTexture* texture) {
    logger->Log("[GameCore] Load main tex: id: -1");
    uiInfo->currentImageLoading = true;
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);

    //Load full main tex
    auto *result = new TextureLoadQueueDataResult();

    //加载上次保存的图像缓存
    if (enableViewCache && thisFileShouldLoadInCache) {

        if(!Path::Exists(currentFileCachePath)) {
            logger->LogWarn("[GameCore] Check currentFileCachePath not exists : %s", currentFileCachePath.c_str());
            goto ORG_LOAD;
        }

        CImageLoader *imageLoader = CImageLoader::CreateImageLoaderType(ImageType::JPG);
        imageLoader->Load(currentFileCachePath.c_str());

        result->buffer = imageLoader->GetAllImageData();

        if (!result->buffer) {
            logger->LogWarn("[GameCore] Load tex from cache file failed, now try original file load.");
            delete imageLoader;
            goto ORG_LOAD;
        }

        result->size = imageLoader->GetFullDataSize();
        result->compoents = imageLoader->GetImageDepth();
        glm::vec2 size = imageLoader->GetImageScaledSize();
        result->width = (int)size.x;
        result->height = (int)size.y;
        result->success = true;

        logger->Log("[GameCore] Load tex from cache file: %s", currentFileCachePath.c_str());
        logger->Log("[GameCore] Load tex buffer from cache file: w: %d h: %d (%d)  Buffer Size: %d",
                    (int)result->width, (int)result->height, result->compoents, result->size);

        delete imageLoader;
    }
    //原始加载
    else {
ORG_LOAD:
        result->buffer = fileManager->CurrentFileLoader->GetAllImageData();

        if (!result->buffer) {
            lastImageError = std::string("图像可能已经损坏，错误信息：") + std::string(fileManager->CurrentFileLoader->GetLastError());
            ShowErrorDialog();
            logger->LogError2("[GameCore] Load tex main buffer failed : %s", fileManager->CurrentFileLoader->GetLastError());
            delete result;
            return nullptr;
        }

        result->size = fileManager->CurrentFileLoader->GetFullDataSize();
        result->compoents = fileManager->CurrentFileLoader->GetImageDepth();
        glm::vec2 size = fileManager->CurrentFileLoader->GetImageScaledSize();
        result->width = (int)size.x;
        result->height = (int)size.y;
        result->success = true;

        logger->Log("[GameCore] Load tex buffer from file : w: %d h: %d (%d)  Buffer Size: %d",
                    (int)result->width, (int)result->height, result->compoents, result->size);
    }

    //保存缓存至文件
    if(enableViewCache && thisFileShouldSaveCache && !thisFileShouldLoadInCache) {

        logger->Log("[GameCore] Start save tex cache to file : %s", currentFileCachePath.c_str());

        FileUtils::SaveImageBufferToJPEGFile(currentFileCachePath.c_str(), result->buffer, result->size,
                                             result->width, result->height, result->compoents);

        logger->Log("[GameCore] Tex cache to file saved : %s", currentFileCachePath.c_str());

        thisFileShouldSaveCache = false;
    }

    //加载完成
    uiInfo->currentImageLoading = false;
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::MarkLoadingEnd);

    return result;
}
TextureLoadQueueDataResult* CMobileGameRenderer::LoadChunkTexCallback(TextureLoadQueueInfo* info, CCTexture* texture) {

    if (!fileOpened)
        return nullptr;

    logger->Log("[GameCore] Load block tex : x: %d y: %d id: %d", info->x, info->y, info->id);

    auto imgSize = fileManager->CurrentFileLoader->GetImageSize();
    int chunkW = (int)imgSize.x / renderer->panoramaFullSplitW;
    int chunkH = (int)imgSize.y / renderer->panoramaFullSplitH;
    int chunkX = info->x * chunkW;
    int chunkY = info->y * chunkH;

    uiInfo->currentImageLoading = true;
    uiInfo->currentImageLoadChunks = info->id;
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
    
    //Load full main tex
    auto* result = new TextureLoadQueueDataResult();
    result->buffer = fileManager->CurrentFileLoader->GetImageChunkData(chunkX, chunkY, chunkW, chunkH);
    result->size = fileManager->CurrentFileLoader->GetChunkDataSize();
    result->compoents = fileManager->CurrentFileLoader->GetImageDepth();
    result->width = chunkW;
    result->height = chunkH;
    result->success = true;

    uiInfo->currentImageLoadedChunks++;
    uiInfo->currentImageLoading = false;
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);

    return result;
}
TextureLoadQueueDataResult* CMobileGameRenderer::LoadTexCallback(TextureLoadQueueInfo* info, CCTexture* texture, void* data) {
    auto* ptr = (CMobileGameRenderer*)data;
    if (ptr->destroying)
        return nullptr;
    if (info->id == -1)
        return ptr->LoadMainTexCallback(info, texture);
    else
        return ptr->LoadChunkTexCallback(info, texture);
}
void CMobileGameRenderer::FileCloseCallback(void* data) {
    auto* ptr = (CMobileGameRenderer*)data;
    ptr->renderer->panoramaThumbnailTex = nullptr;
    ptr->renderer->renderPanoramaFull = false;
    ptr->renderer->ReleaseTexPool();
    ptr->renderer->ReleaseFullModel();
    ptr->renderer->UpdateMainModelTex();
    ptr->uiInfo->currentImageOpened = false;
    ptr->uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
    ptr->renderer->renderOn = false;
    ptr->fileOpened = false;
    ptr->uiEventDistributor->SendEvent(CCMobileGameUIEvent::FileClosed);
}
void CMobileGameRenderer::CameraFOVChanged(void* data, float fov) {
    auto* ptr = (CMobileGameRenderer*)data;
    if (ptr->mode == PanoramaSphere || ptr->mode == PanoramaCylinder) {

        ptr->renderer->renderPanoramaFull =
                ptr->fullChunkLoadEnabled && ptr->shouldSplitFullImage && fov < 40;

        if(ptr->renderer->renderPanoramaFull)
            ptr->renderer->UpdateFullChunksVisible();
    }
}
void CMobileGameRenderer::CameraOrthoSizeChanged(void* data, float fov) {
    auto* ptr = (CMobileGameRenderer*)data;
    ptr->renderer->UpdateFlatModelMinMax(fov);
}

float CMobileGameRenderer::GetMouseSensitivity() {
    return (MouseSensitivityMin + (MouseSensitivityMax - MouseSensitivityMin) * camera->GetZoomPercentage());
}
float CMobileGameRenderer::GetMouseSensitivityInFlat() {
    return (MouseInFlatSensitivityMin + (MouseInFlatSensitivityMax - MouseInFlatSensitivityMin) * camera->GetZoomPercentage());
}

//公共方法
//*************************

void CMobileGameRenderer::SwitchMode(PanoramaMode panoramaMode)
{
    mode = panoramaMode;
    renderer->renderPanoramaFull = false;
    switch (mode)
    {
    case PanoramaMercator:
        camera->Projection = CCameraProjection::Orthographic;
        camera->SetMode(CCPanoramaCameraMode::Static);
        camera->Position.z = 0.2f;
        camera->ForceUpdate();
        renderer->ResetModel();
        renderer->renderPanoramaFlat = true;
        renderer->renderPanoramaFull = false;
        renderer->renderNoPanoramaSmall = true;
        renderer->renderPanoramaFlatXLoop = false;
        renderer->SetIsMercator(true);
        MouseInFlatSensitivityMax = 0.001f;
        MouseInFlatSensitivityMin = 0.0001f;
        break;
    case PanoramaFullOrginal:
        camera->Projection = CCameraProjection::Orthographic;
        camera->SetMode(CCPanoramaCameraMode::OrthoZoom);
        camera->ForceUpdate();
        renderer->ResetModel();
        renderer->renderPanoramaFull = false;
        renderer->renderPanoramaFlat = true;
        renderer->renderNoPanoramaSmall = true;
        renderer->renderPanoramaFlatXLoop = false;
        renderer->SetIsMercator(false);
        MouseInFlatSensitivityMax = 0.001f;
        MouseInFlatSensitivityMin = 0.0001f;
        break;
    case PanoramaFull360:
        camera->Projection = CCameraProjection::Orthographic;
        camera->SetMode(CCPanoramaCameraMode::OrthoZoom);
        camera->ForceUpdate();
        renderer->ResetModel();
        renderer->renderPanoramaFull = false;
        renderer->renderPanoramaFlat = true;
        renderer->renderNoPanoramaSmall = true;
        renderer->renderPanoramaFlatXLoop = true;
        renderer->SetIsMercator(false);
        MouseInFlatSensitivityMax = 0.001f;
        MouseInFlatSensitivityMin = 0.0001f;
        break;
    case PanoramaAsteroid:
        camera->Projection = CCameraProjection::Perspective;
        camera->SetMode(CCPanoramaCameraMode::CenterRoate);
        camera->Position.z = 0.9f;
        camera->FiledOfView = 135.0f;
        camera->FovMin = 56.0f;
        camera->ClippingNear = 0.001f;
        camera->FovMax = 170.0f;
        MouseSensitivityMin = 0.01f;
        MouseSensitivityMax = 0.1f;
        renderer->ResetModel();
        renderer->renderNoPanoramaSmall = false;
        renderer->renderPanoramaFlat = false;
        break;
    case PanoramaCylinder:
        camera->Projection = CCameraProjection::Perspective;
        camera->SetMode(CCPanoramaCameraMode::CenterRoate);
        camera->Position.z = 0.0f;
        camera->FiledOfView = 70.0f;
        camera->FovMin = 5.0f;
        camera->FovMax = 120.0f;
        camera->ForceUpdate();
        renderer->renderPanoramaFull = fullChunkLoadEnabled && shouldSplitFullImage && camera->FiledOfView < 30;
        renderer->renderNoPanoramaSmall = false;
        renderer->renderPanoramaFlat = false;
        MouseSensitivityMin = 0.01f;
        MouseSensitivityMax = 0.1f;
        break;
    case PanoramaSphere:
        camera->Projection = CCameraProjection::Perspective;
        camera->SetMode(CCPanoramaCameraMode::CenterRoate);
        camera->Position.z = 0.5f;
        camera->FiledOfView = 50.0f;
        camera->FovMin = 5.0f;
        camera->FovMax = 75.0f;
        renderer->ResetModel();
        renderer->renderPanoramaFull = fullChunkLoadEnabled && shouldSplitFullImage && camera->FiledOfView < 30;
        renderer->renderNoPanoramaSmall = false;
        renderer->renderPanoramaFlat = false;
        MouseSensitivityMin = 0.01f;
        MouseSensitivityMax = 0.1f;
        break;
    case PanoramaOuterBall:
        camera->Projection = CCameraProjection::Perspective;
        camera->SetMode(CCPanoramaCameraMode::CenterRoate);
        camera->FiledOfView = 110.0f;
        camera->Position.z = 1.5f;
        camera->FovMin = 25.0f;
        camera->FovMax = 130.0f;
        renderer->ResetModel();
        renderer->renderPanoramaFull = false;
        renderer->renderNoPanoramaSmall = false;
        renderer->renderPanoramaFlat = false;
        MouseSensitivityMin = 0.01f;
        MouseSensitivityMax = 0.1f;
        break;
    default:
        break;
    }
}
void CMobileGameRenderer::UpdateDebugValue(float x, float y, float z, float w, float u, float v) {
    if(renderer) {
        renderer->mainModel->SetLocalEulerAngles(glm::vec3(x,y,z));
    }
}
void CMobileGameRenderer::UpdateGyroValue(float x, float y, float z, float w) const {
    if(renderer) {
        renderer->gyroEnabled = gyroEnabled;
        if(gyroEnabled && mode <= PanoramaMode::PanoramaMercator)
            renderer->GyroscopeRotateModel(x, y, z, w);
    }
}
void CMobileGameRenderer::SetMouseDragVelocity(float x, float y) {
    DragCurrentVelocity.x = glm::abs(x);
    DragCurrentVelocity.y = glm::abs(y);

    if(DragCurrentVelocity.x < 2 && DragCurrentVelocity.y <= 2)
        VelocityDragCurrentIsInSim = false;
}

//播放器公共方法
//*************************

void CMobileGameRenderer::UpdateVideoState(int newState) {

}
int CMobileGameRenderer::GetVideoLength() {
    return 0;
}
int CMobileGameRenderer::GetVideoState() {
    return 0;
}
int CMobileGameRenderer::GetVideoPos() {
    return 0;
}
bool CMobileGameRenderer::GetCurrentFileIsVideo() {
    return false;
}
void CMobileGameRenderer::SetVideoPos(int pos) {

}

//属性参数扩展方法
//*************************

void CMobileGameRenderer::SetIntProp(int id, int value) {

}
int CMobileGameRenderer::GetIntProp(int id) {
    return 0;
}

void CMobileGameRenderer::SetBoolProp(int id, bool value) {
    switch (id) {
        case PROP_VR_ENABLED: SetVREnabled(value); break;
        case PROP_GYRO_ENABLED: SetGyroEnabled(value); break;
        case PROP_FULL_CHUNK_LOAD_ENABLED: SetEnableFullChunkLoad(value); break;
        case PROP_VIEW_CACHE_ENABLED: SetViewCacheEnabled(value); break;
    }
}
bool CMobileGameRenderer::GetBoolProp(int id) {
    switch (id) {
        case PROP_IS_FILE_OPEN: return fileOpened;
        case PROP_IS_CURRENT_FILE_OPEN: fileOpened && CC_IS_FILE_TYPE_VIDEO(fileManager->CheckCurrentFileType());
        case PROP_CURRENT_FILE_IS_VIDEO: return GetCurrentFileIsVideo();
        case PROP_VR_ENABLED: return vREnabled;
        case PROP_GYRO_ENABLED: return gyroEnabled;
        case PROP_FULL_CHUNK_LOAD_ENABLED: return fullChunkLoadEnabled;
        case PROP_VIEW_CACHE_ENABLED: return enableViewCache;
    }
    return false;
}

void CMobileGameRenderer::SetProp(int id, char* string) {
    switch (id) {
        case PROP_CACHE_PATH: SetCachePath(string); break;

    }
}
const char* CMobileGameRenderer::GetProp(int id) {
    switch (id) {
        case PROP_CACHE_PATH: return viewCachePath.c_str();
        case PROP_LAST_ERROR: return lastImageError.c_str();
    }
    return nullptr;
}

//属性参数私有方法
//*************************

void CMobileGameRenderer::SetGyroEnabled(bool enable) {
    gyroEnabled = enable;
    if(renderer) {
        renderer->gyroEnabled = gyroEnabled;
        renderer->ResetModel();
    }
}
void CMobileGameRenderer::SetEnableFullChunkLoad(bool enable) {
    fullChunkLoadEnabled = enable;
}
void CMobileGameRenderer::SetVREnabled(bool enable) {
    vREnabled = enable;
    if (!vREnabled)
        CMobileGameRenderer::Resize(Width, Height);
}
void CMobileGameRenderer::SetViewCacheEnabled(bool enable) { enableViewCache = enable; }
void CMobileGameRenderer::SetCachePath(char* path) { viewCachePath = path; };


