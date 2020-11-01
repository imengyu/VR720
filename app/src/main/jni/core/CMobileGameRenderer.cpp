#include "CMobileGameRenderer.h"
#include "COpenGLView.h"
#include "CImageLoader.h"
#include "CCRenderGlobal.h"
#include "CMobileGameUIEventDistributor.h"
#include "CCMaterial.h"
#include "CStringHlp.h"
#include <ctime>

CMobileGameRenderer::CMobileGameRenderer()
{
    logger = Logger::GetStaticInstance();
}
CMobileGameRenderer::~CMobileGameRenderer() = default;

void CMobileGameRenderer::SetOpenFilePath(const char* path)
{
	currentOpenFilePath = path;
}
void CMobileGameRenderer::DoOpenFile()
{
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::MarkLoadingStart);
    if (fileManager->OpenFile(currentOpenFilePath.c_str())) {
        //主图
        renderer->panoramaThumbnailTex = texLoadQueue->Push(new CCTexture(), 0, 0, -1);//MainTex
        renderer->panoramaTexPool.push_back(renderer->panoramaThumbnailTex);
        renderer->UpdateMainModelTex();

        //检查是否需要分片并加载
        needTestImageAndSplit = true;
        file_opened = true;
        renderer->renderOn = true;
        uiInfo->currentImageOpened = true;
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
    }
    else {
        last_image_error = vstring(fileManager->GetLastError());
        ShowErrorDialog();
    }
}
void CMobileGameRenderer::ShowErrorDialog() {
    file_opened = false;
    renderer->renderOn = false;
    uiInfo->currentImageOpened = false;
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::MarkLoadingEnd);
    uiEventDistributor->SendEvent(CCMobileGameUIEvent::MarkLoadFailed);
}

void CMobileGameRenderer::TestSplitImageAndLoadTexture() {
    glm::vec2 size = fileManager->CurrentFileLoader->GetImageSize();
    SplitFullImage = size.x > 4096 || size.y > 2048;
    if (SplitFullImage) {
        float chunkW = size.x / 2048.0f;
        float chunkH = size.y / 1024.0f;
        if (chunkW < 2) chunkW = 2;
        if (chunkH < 2) chunkH = 2;
        if (chunkW > 64 || chunkH > 32) {
            logger->LogError2(_vstr("Too big image (%.2f, %.2f) that cant split chunks."), chunkW, chunkH);
            SplitFullImage = false;
            return;
        }

        int chunkWi = (int)ceil(chunkW), chunkHi = (int)ceil(chunkH);

        uiInfo->currentImageAllChunks = chunkWi * chunkHi;
        uiInfo->currentImageLoadChunks = 0;
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
        logger->Log(_vstr("Image use split mode , size: %d, %d"), chunkWi, chunkHi);
        renderer->sphereFullSegmentX = renderer->sphereSegmentX + (renderer->sphereSegmentX % chunkWi);
        renderer->sphereFullSegmentY = renderer->sphereSegmentY + (renderer->sphereSegmentY % chunkHi);
        renderer->GenerateFullModel(chunkWi, chunkHi);
    }
    else {
        uiInfo->currentImageAllChunks = 0;
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
    }

    SwitchMode(mode);
}

bool CMobileGameRenderer::Init()
{
    camera = new CCPanoramaCamera();
    renderer = new CCPanoramaRenderer(this);
    fileManager = new CCFileManager(this);
    texLoadQueue = new CCTextureLoadQueue();
    uiInfo = new CCGUInfo();

    renderer->Init();
    texLoadQueue->SetLoadHandle(LoadTexCallback, this);
    camera->SetMode(CCPanoramaCameraMode::CenterRoate);
    camera->SetFOVChangedCallback(CameraFOVChanged, this);
    camera->SetOrthoSizeChangedCallback(CameraOrthoSizeChanged, this);
    camera->Background = CColor::FromString("#FFFFFF");
    fileManager->SetOnCloseCallback(FileCloseCallback, this);

    View->SetCamera(camera);
    View->SetMouseCallback(MouseCallback);
    View->SetZoomViewCallback(ScrollCallback);

    SwitchMode(mode);

    //renderer->renderPanoramaFullTest = true;
    //renderer->renderPanoramaFullRollTest = true;
    //renderer->renderPanoramaATest = true;
    //TestSplitImageAndLoadTexture();

    render_init_finish = true;
	return true;
}
void CMobileGameRenderer::Destroy()
{
    destroying = true;
    if (uiInfo != nullptr) {
        delete uiInfo;
        uiInfo = nullptr;
    }
    if (uiEventDistributor != nullptr) {
        delete uiEventDistributor;
        uiEventDistributor = nullptr;
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
    if (renderer != nullptr) {
        renderer->Destroy();
        delete renderer;
        renderer = nullptr;
    }
}
void CMobileGameRenderer::Resize(int Width, int Height)
{
    glViewport(0, 0, Width, Height);
}

//输入处理

void CMobileGameRenderer::MouseCallback(COpenGLView* view, float xpos, float ypos, int button, int type) {
    auto* renderer = (CMobileGameRenderer*)view->GetRenderer();

    if (type == ViewMouseEventType::ViewMouseMouseDown) {
        renderer->lastX = xpos;
        renderer->lastY = ypos;
    }
    else  if (type == ViewMouseEventType::ViewMouseMouseMove) {

        renderer->xoffset = xpos - renderer->lastX;
        renderer->yoffset =
                renderer->lastY - ypos; // reversed since y-coordinates go from bottom to top

        renderer->lastX = xpos;
        renderer->lastY = ypos;

        //旋转球体
        if (renderer->mode <= PanoramaMode::PanoramaOuterBall) {
            float xoffset = -renderer->xoffset * renderer->MouseSensitivity;
            float yoffset = -renderer->yoffset * renderer->MouseSensitivity;
            renderer->renderer->RotateModel(xoffset, yoffset);
        }
            //全景模式是更改U偏移和纬度偏移
        else if (renderer->mode == PanoramaMode::PanoramaMercator) {

        } else if (renderer->mode == PanoramaMode::PanoramaFull360
                   || renderer->mode == PanoramaMode::PanoramaFullOrginal) {
            float xoffset = -renderer->xoffset * renderer->MouseSensitivity;
            float yoffset = -renderer->yoffset * renderer->MouseSensitivity;
            renderer->renderer->MoveModel(xoffset, yoffset);
        }
    }
}
void CMobileGameRenderer::ScrollCallback(COpenGLView* view, float x, float yoffset, int button, int type) {
    CMobileGameRenderer* renderer = (CMobileGameRenderer*)view->GetRenderer();
    renderer->camera->ProcessMouseScroll(yoffset);
}
void CMobileGameRenderer::KeyMoveCallback(CCameraMovement move) {
    if (mode <= PanoramaMode::PanoramaOuterBall) {
        switch (move)
        {
        case CCameraMovement::ROATE_UP:
            renderer->RotateModelForce(0, -RotateSpeed * View->GetDeltaTime());
            break;
        case CCameraMovement::ROATE_DOWN:
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
    else if (mode == PanoramaMode::PanoramaMercator) {

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

void CMobileGameRenderer::Render(float FrameTime)
{
    //渲染
    //===========================

    glLoadIdentity();
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);

    renderer->Render(View->GetDeltaTime());

    glLoadIdentity();

    //在渲染线程中加载贴图
    //===========================

    texLoadQueue->ResolveRender();

    if (should_open_file && render_init_finish) {
        should_open_file = false;
        DoOpenFile();
    }

    if (needTestImageAndSplit) {
        needTestImageAndSplit = false;
        TestSplitImageAndLoadTexture();
    }

    //loop count
    //===========================

    if (should_close_file) {
        should_close_file = false;
        fileManager->CloseFile();
    }
}
void CMobileGameRenderer::RenderUI()
{

}
void CMobileGameRenderer::Update()
{
    //加载队列处理
    //===========================

    texLoadQueue->ResolveMain();

    //加检测按键
    //===========================

    if (View->GetKeyPress(VR720_KEY_LEFT)) KeyMoveCallback(CCameraMovement::ROATE_LEFT);
    if (View->GetKeyPress(VR720_KEY_UP)) KeyMoveCallback(CCameraMovement::ROATE_UP);
    if (View->GetKeyPress(VR720_KEY_RIGHT)) KeyMoveCallback(CCameraMovement::ROATE_RIGHT);
    if (View->GetKeyPress(VR720_KEY_DOWN)) KeyMoveCallback(CCameraMovement::ROATE_DOWN);
}

//逻辑控制

TextureLoadQueueDataResult* CMobileGameRenderer::LoadChunkTexCallback(TextureLoadQueueInfo* info, CCTexture* texture) {

    if (!file_opened)
        return nullptr;

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
    if (info->id == -1) {
        ptr->logger->Log(_vstr("Load main tex: id: -1"));
        ptr->uiInfo->currentImageLoading = true;
        ptr->uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);

        //Load full main tex
        auto* result = new TextureLoadQueueDataResult();
        result->buffer = ptr->fileManager->CurrentFileLoader->GetAllImageData();
        if (!result->buffer) {
            ptr->last_image_error = vstring("图像可能已经损坏，错误信息：") + vstring(ptr->fileManager->CurrentFileLoader->GetLastError());
            ptr->ShowErrorDialog();
            ptr->logger->LogError2(_vstr("Load tex main buffer failed : %s"), ptr->fileManager->CurrentFileLoader->GetLastError());
            delete result;
            return nullptr;
        }

        result->size = ptr->fileManager->CurrentFileLoader->GetFullDataSize();
        result->compoents = ptr->fileManager->CurrentFileLoader->GetImageDepth();
        glm::vec2 size = ptr->fileManager->CurrentFileLoader->GetImageScaledSize();
        result->width = (int)size.x;
        result->height = (int)size.y;
        result->success = true;

        ptr->logger->Log(_vstr("Load tex buffer: w: %d h: %d (%d)  Buffer Size: %d"), (int)size.x, (int)size.y, result->compoents, result->size);
        ptr->uiInfo->currentImageLoading = false;
        ptr->uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);

        return result;
    }
    else {
        ptr->logger->Log(_vstr("Load block tex : x: %d y: %d id: %d"), info->x, info->y, info->id);
        return ptr->LoadChunkTexCallback(info, texture);
    }
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
    ptr->file_opened = false;
    ptr->uiEventDistributor->SendEvent(CCMobileGameUIEvent::FileClosed);
}
void CMobileGameRenderer::CameraFOVChanged(void* data, float fov) {
    auto* ptr = (CMobileGameRenderer*)data;
    if (ptr->mode == PanoramaSphere || ptr->mode == PanoramaCylinder) {
        ptr->renderer->renderPanoramaFull = ptr->SplitFullImage && fov < 40;
        if(ptr->renderer->renderPanoramaFull) ptr->renderer->UpdateFullChunksVisible();
    }
}
void CMobileGameRenderer::CameraOrthoSizeChanged(void* data, float fov) {
    auto* ptr = (CMobileGameRenderer*)data;
    ptr->renderer->UpdateFlatModelMinMax(fov);
}

void CMobileGameRenderer::AddTextureToQueue(CCTexture* tex, int x, int y, int id) {
    texLoadQueue->Push(tex, x, y, id);
}
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
        renderer->ResetModel();
        renderer->renderPanoramaFlat = true;
        renderer->renderPanoramaFull = false;
        renderer->renderNoPanoramaSmall = true;
        renderer->renderPanoramaFlatXLoop = false;
        renderer->UpdateMercatorControl();
        MouseSensitivity = 0.001f;
        break;
    case PanoramaFullOrginal:
        camera->Projection = CCameraProjection::Orthographic;
        camera->SetMode(CCPanoramaCameraMode::OrthoZoom);
        renderer->ResetModel();
        renderer->renderPanoramaFull = false;
        renderer->renderPanoramaFlat = true;
        renderer->renderNoPanoramaSmall = true;
        renderer->renderPanoramaFlatXLoop = false;
        renderer->ResetMercatorControl();
        MouseSensitivity = 0.001f;
        break;
    case PanoramaFull360:
        camera->Projection = CCameraProjection::Orthographic;
        camera->SetMode(CCPanoramaCameraMode::OrthoZoom);
        renderer->ResetModel();
        renderer->renderPanoramaFull = false;
        renderer->renderPanoramaFlat = true;
        renderer->renderNoPanoramaSmall = true;
        renderer->renderPanoramaFlatXLoop = true;
        renderer->ResetMercatorControl();
        MouseSensitivity = 0.001f;
        break;
    case PanoramaAsteroid:
        camera->Projection = CCameraProjection::Perspective;
        camera->SetMode(CCPanoramaCameraMode::CenterRoate);
        camera->Position.z = 1.0f;
        camera->FiledOfView = 135.0f;
        camera->FovMin = 35.0f;
        camera->FovMax = 135.0f;
        MouseSensitivity = 0.1f;
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
        renderer->renderPanoramaFull = SplitFullImage && camera->FiledOfView < 30;
        renderer->renderNoPanoramaSmall = false;
        renderer->renderPanoramaFlat = false;
        MouseSensitivity = 0.1f;
        break;
    case PanoramaSphere:
        camera->Projection = CCameraProjection::Perspective;
        camera->SetMode(CCPanoramaCameraMode::CenterRoate);
        camera->Position.z = 0.5f;
        camera->FiledOfView = 50.0f;
        camera->FovMin = 5.0f;
        camera->FovMax = 75.0f;
        renderer->renderPanoramaFull = SplitFullImage && camera->FiledOfView < 30;
        renderer->renderNoPanoramaSmall = false;
        renderer->renderPanoramaFlat = false;
        MouseSensitivity = 0.1f;
        break;
    case PanoramaOuterBall:
        camera->Projection = CCameraProjection::Perspective;
        camera->SetMode(CCPanoramaCameraMode::CenterRoate);
        camera->FiledOfView = 90.0f;
        camera->Position.z = 1.5f;
        camera->FovMin = 35.0f;
        camera->FovMax = 90.0f;
        renderer->renderPanoramaFull = false;
        renderer->renderNoPanoramaSmall = false;
        renderer->renderPanoramaFlat = false;
        MouseSensitivity = 0.1f;
        break;
    default:
        break;
    }
}

void CMobileGameRenderer::UpdsteGryoValue(float x, float y, float z) {
    if(gryoEnabled) {
        
    }
}
