#include "CMobileGameRenderer.h"
#include "COpenGLView.h"
#include "CImageLoader.h"
#include "CCRenderGlobal.h"
#include "CCMaterial.h"
#include "CCursor.h"
#include "CApp.h"
#include "CStringHlp.h"
#include "SettingHlp.h"
#include <Shlwapi.h>
#include <time.h>

CMobileGameRenderer::CMobileGameRenderer()
{
    logger = CApp::Instance->GetLogger();
}
CMobileGameRenderer::~CMobileGameRenderer()
{
}

void CMobileGameRenderer::SetOpenFilePath(const wchar_t* path)
{
	currentOpenFilePath = path;
}
void CMobileGameRenderer::DoOpenFile()
{
    loading_dialog_active = true;
    if (fileManager->DoOpenFile(currentOpenFilePath.c_str())) {

        if (fileManager->ImageRatioNotStandard && mode <= PanoramaMode::PanoramaOuterBall) {
            if (uiWapper->ShowConfirmBox(L"看起来这张图片的长宽比不是 2:1，不是标准的720度全景图像，如果要显示此图像，可能会导致图像变形，是否继续？", 
                L"提示", L"", L"", CAppUIMessageBoxIcon::IconWarning) == CAppUIMessageBoxResult::ResultCancel)
            {
                welecome_dialog_active = true;
                file_opened = false;
                renderer->renderOn = false;
                loading_dialog_active = false;
                return;
            }
        }

        LoadImageInfo();

        //主图
        renderer->panoramaThumbnailTex = texLoadQueue->Push(new CCTexture(), 0, 0, -1);//MainTex
        renderer->panoramaTexPool.push_back(renderer->panoramaThumbnailTex);
        renderer->UpdateMainModelTex();

        //检查是否需要分片并加载
        needTestImageAndSplit = true;
        welecome_dialog_active = false;
        file_opened = true;
        renderer->renderOn = true;
    }
    else {
        last_image_error = CStringHlp::UnicodeToUtf8(std::wstring(fileManager->GetLastError()));
        ShowErrorDialog();
    }
}
void CMobileGameRenderer::ShowErrorDialog() {
    welecome_dialog_active = false;
    image_err_dialog_active = true;
    file_opened = false;
    renderer->renderOn = false;
    loading_dialog_active = false;
    uiWapper->MessageBeep(CAppUIMessageBoxIcon::IconWarning);
}
void CMobileGameRenderer::LoadImageInfo() {
    //获取图片信息
    auto loader = fileManager->CurrentFileLoader;
    auto imgSize = loader->GetImageSize();
    auto imgFileInfo = loader->GetImageFileInfo();

    uiInfo->currentImageType = fileManager->CurrenImageType;
    uiInfo->currentImageName = CStringHlp::UnicodeToUtf8(fileManager->GetCurrentFileName());
    uiInfo->currentImageImgSize = CStringHlp::FormatString("%dx%dx%db", (int)imgSize.x, (int)imgSize.y, loader->GetImageDepth());
    uiInfo->currentImageSize = CStringHlp::GetFileSizeStringAuto(imgFileInfo->fileSize);
    uiInfo->currentImageChangeDate = imgFileInfo->Write;

    uiInfo->currentImageOpened = true;
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
            logger->LogError2(L"Too big image (%.2f, %.2f) that cant split chunks.", chunkW, chunkH);
            SplitFullImage = false;
            return;
        }

        int chunkWi = (int)ceil(chunkW), chunkHi = (int)ceil(chunkH);

        uiInfo->currentImageAllChunks = chunkWi * chunkHi;
        uiInfo->currentImageLoadChunks = 0;
        logger->Log(L"Image use split mode , size: %d, %d", chunkWi, chunkHi);
        renderer->sphereFullSegmentX = renderer->sphereSegmentX + (renderer->sphereSegmentX % chunkWi);
        renderer->sphereFullSegmentY = renderer->sphereSegmentY + (renderer->sphereSegmentY % chunkHi);
        renderer->GenerateFullModel(chunkWi, chunkHi);
    }
    else {
        uiInfo->currentImageAllChunks = 0;
    }

    SwitchMode(mode);
}

bool CMobileGameRenderer::Init()
{
    CCursor::SetViewCursur(View, CCursor::Default);

    camera = new CCPanoramaCamera();
    renderer = new CCPanoramaRenderer(this);
    fileManager = new CCFileManager(this);
    uiWapper = new CAppUIWapper(this->View);
    texLoadQueue = new CCTextureLoadQueue();
    uiInfo = new CCGUInfo();

    renderer->Init();
    texLoadQueue->SetLoadHandle(LoadTexCallback, this);
    camera->SetMode(CCPanoramaCameraMode::CenterRoate);
    camera->SetFOVChangedCallback(CameraFOVChanged, this);
    camera->SetOrthoSizeChangedCallback(CameraOrthoSizeChanged, this);
    camera->SetRotateCallback(CameraRotate, this);
    camera->Background = CColor::FromString("#FFFFFF");
    fileManager->SetOnCloseCallback(FileCloseCallback, this);

    View->SetCamera(camera);
    View->SetBeforeQuitCallback(BeforeQuitCallback);
    View->SetMouseCallback(MouseCallback);
    View->SetScrollCallback(ScrollCallback);

    LoadSettings();
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
    if (fileManager != nullptr) {
        delete fileManager;
        fileManager = nullptr;
    }
    if (uiWapper != nullptr) {
        delete uiWapper;
        uiWapper = nullptr;
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
char* CMobileGameRenderer::GetPanoramaModeStr(PanoramaMode mode)
{
    switch (mode)
    {
    case PanoramaSphere:
        return (char*)u8"球面";
    case PanoramaAsteroid:
        return (char*)u8"小行星";
    case PanoramaOuterBall:
        return (char*)u8"水晶球";
    case PanoramaCylinder:
        return (char*)u8"平面";
    case PanoramaMercator:
        return (char*)u8"全景";
    case PanoramaFull360:
        return (char*)u8"360度全景";
    case PanoramaFullOrginal:
        return (char*)u8"原始图像";
    default:
        break;
    }
    return nullptr;
}
void CMobileGameRenderer::Resize(int Width, int Height)
{
    glViewport(0, 0, Width, Height);
}

//输入处理

void CMobileGameRenderer::MouseCallback(COpenGLView* view, float xpos, float ypos, int button, int type) {
    CMobileGameRenderer* renderer = (CMobileGameRenderer*)view->GetRenderer();

    if (type == ViewMouseEventType::ViewMouseMouseDown) {
        if ((button & MK_LBUTTON) == MK_LBUTTON) {
            renderer->lastX = xpos;
            renderer->lastY = ypos;
            view->MouseCapture();
            CCursor::SetViewCursur(view, CCursor::Grab);
        }
    }
    else  if (type == ViewMouseEventType::ViewMouseMouseUp) {
        if ((button & MK_LBUTTON) == MK_LBUTTON) {
            view->ReleaseCapture();
            CCursor::SetViewCursur(view, CCursor::Default);
        }
    }
    else  if (type == ViewMouseEventType::ViewMouseMouseMove) {

        //Skip when mouse hover on window
        if (ImGui::IsAnyWindowHovered()) {
            renderer->main_menu_active = true;
            return;
        }

        if ((button & MK_LBUTTON) == MK_LBUTTON) {//left button down

            renderer->xoffset = xpos - renderer->lastX;
            renderer->yoffset = renderer->lastY - ypos; // reversed since y-coordinates go from bottom to top

            renderer->lastX = xpos;
            renderer->lastY = ypos;

            //旋转球体
            if (renderer->mode <= PanoramaMode::PanoramaOuterBall) {
                float xoffset = -renderer->xoffset * renderer->MouseSensitivity;
                float yoffset = -renderer->yoffset * renderer->MouseSensitivity;
                renderer->renderer->RotateModel(xoffset, yoffset);
            }
            //全景模式是更改U偏移和纬度偏移
            else if(renderer->mode == PanoramaMode::PanoramaMercator) {

            }
            else if (renderer->mode == PanoramaMode::PanoramaFull360 
                || renderer->mode == PanoramaMode::PanoramaFullOrginal) {
                float xoffset = -renderer->xoffset * renderer->MouseSensitivity;
                float yoffset = -renderer->yoffset * renderer->MouseSensitivity;
                renderer->renderer->MoveModel(xoffset, yoffset);
            }
        }

        renderer->main_menu_active = ypos < 100 || ypos >  renderer->View->Height - 70;
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
        }
    }
}

//Settings

void CMobileGameRenderer::LoadSettings()
{
    settings = CApp::Instance->GetSettings();

    View->ShowInfoOverlay = settings->GetSettingBool(L"ShowInfoOverlay", false);
    show_console = settings->GetSettingBool(L"ShowConsole", false);
    show_fps = settings->GetSettingBool(L"ShowFps", show_fps);
    show_status_bar = settings->GetSettingBool(L"ShowStatusBar", show_status_bar);
    debug_tool_active = settings->GetSettingBool(L"DebugTool", false);
    renderer->renderDebugWireframe = settings->GetSettingBool(L"renderDebugWireframe", false);
    renderer->renderDebugVector = settings->GetSettingBool(L"renderDebugVector", false);
    renderer->sphereSegmentX = settings->GetSettingInt(L"sphereSegmentX", renderer->sphereSegmentX);
    renderer->sphereSegmentY = settings->GetSettingInt(L"sphereSegmentY", renderer->sphereSegmentY);
    mode = (PanoramaMode)settings->GetSettingInt(L"LastMode", mode);
    View->IsFullScreen = settings->GetSettingBool(L"FullScreen", false);
    View->Width = settings->GetSettingInt(L"Width", 1024);
    View->Height = settings->GetSettingInt(L"Height", 768);
    if (View->Width <= 800) View->Width = 1024;
    if (View->Height <= 600) View->Height = 768;
    View->Resize(View->Width, View->Height, true);
    View->UpdateFullScreenState();

    UpdateConsoleState();
}
void CMobileGameRenderer::SaveSettings()
{
    settings->SetSettingBool(L"ShowInfoOverlay", View->ShowInfoOverlay);
    settings->SetSettingBool(L"DebugTool", debug_tool_active);
    settings->SetSettingBool(L"ShowConsole", show_console);
    settings->SetSettingBool(L"renderDebugWireframe", renderer->renderDebugWireframe);
    settings->SetSettingBool(L"renderDebugVector", renderer->renderDebugVector);
    settings->SetSettingBool(L"FullScreen", View->IsFullScreen);
    settings->SetSettingInt(L"Width", View->Width);
    settings->SetSettingInt(L"Height", View->Height);
    settings->SetSettingInt(L"sphereSegmentX", renderer->sphereSegmentX);
    settings->SetSettingInt(L"sphereSegmentY", renderer->sphereSegmentY);
    settings->SetSettingInt(L"LastMode", mode);
    settings->SetSettingBool(L"ShowFps", show_fps);
    settings->SetSettingBool(L"ShowStatusBar", show_status_bar);
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

    LoadAndChechRegister();
    if (should_close_file) {
        should_close_file = false;
        std::wstring path = fileManager->CurrentFileLoader->GetPath();
        fileManager->CloseFile();
        if (delete_after_close)
            DeleteFile(path.c_str());
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
    
    //Load full main tex
    TextureLoadQueueDataResult* result = new TextureLoadQueueDataResult();
    result->buffer = fileManager->CurrentFileLoader->GetImageChunkData(chunkX, chunkY, chunkW, chunkH);
    result->size = fileManager->CurrentFileLoader->GetChunkDataSize();
    result->compoents = fileManager->CurrentFileLoader->GetImageDepth();
    result->width = chunkW;
    result->height = chunkH;
    result->success = true;

    uiInfo->currentImageLoadedChunks++;
    uiInfo->currentImageLoading = false;

    return result;
}
TextureLoadQueueDataResult* CMobileGameRenderer::LoadTexCallback(TextureLoadQueueInfo* info, CCTexture* texture, void* data) {
    CMobileGameRenderer* ptr = (CMobileGameRenderer*)data;
    if (ptr->destroying)
        return nullptr;
    if (info->id == -1) {
        ptr->logger->Log(L"Load main tex: id: -1");
        ptr->uiInfo->currentImageLoading = true;

        //Load full main tex
        TextureLoadQueueDataResult* result = new TextureLoadQueueDataResult();
        result->buffer = ptr->fileManager->CurrentFileLoader->GetAllImageData();
        if (!result->buffer) {
            ptr->last_image_error = CStringHlp::UnicodeToUtf8(std::wstring(L"图像可能已经损坏，错误信息：") + std::wstring(ptr->fileManager->CurrentFileLoader->GetLastError()));
            ptr->ShowErrorDialog();
            ptr->logger->LogError2(L"Load tex main buffer failed : %s", ptr->fileManager->CurrentFileLoader->GetLastError());
            delete result;
            return nullptr;
        }

        result->size = ptr->fileManager->CurrentFileLoader->GetFullDataSize();
        result->compoents = ptr->fileManager->CurrentFileLoader->GetImageDepth();
        glm::vec2 size = ptr->fileManager->CurrentFileLoader->GetImageScaledSize();
        result->width = (int)size.x;
        result->height = (int)size.y;
        result->success = true;

        ptr->logger->Log(L"Load tex buffer: w: %d h: %d (%d)  Buffer Size: %d", (int)size.x, (int)size.y, result->compoents, result->size);
        ptr->loading_dialog_active = false;
        ptr->uiInfo->currentImageLoading = false;

        return result;
    }
    else {
        ptr->logger->Log(L"Load block tex : x: %d y: %d id: %d", info->x, info->y, info->id);
        return ptr->LoadChunkTexCallback(info, texture);
    }
    return nullptr;
}
void CMobileGameRenderer::FileCloseCallback(void* data) {
    CMobileGameRenderer* ptr = (CMobileGameRenderer*)data;
    ptr->renderer->panoramaThumbnailTex = nullptr;
    ptr->renderer->renderPanoramaFull = false;
    ptr->renderer->ReleaseTexPool();
    ptr->renderer->ReleaseFullModel();
    ptr->renderer->UpdateMainModelTex();
    ptr->uiInfo->currentImageOpened = false;
    ptr->renderer->renderOn = false;
    ptr->welecome_dialog_active = true;
    ptr->file_opened = false;
}
void CMobileGameRenderer::CameraFOVChanged(void* data, float fov) {
    CMobileGameRenderer* ptr = (CMobileGameRenderer*)data;
    ptr->zoom_slider_value = (int)((1.0f - (fov - ptr->camera->FovMin) / (ptr->camera->FovMax - ptr->camera->FovMin)) * 100);
    if (ptr->mode == PanoramaSphere || ptr->mode == PanoramaCylinder) {
        ptr->renderer->renderPanoramaFull = ptr->SplitFullImage && fov < 40;
        if(ptr->renderer->renderPanoramaFull) ptr->renderer->UpdateFullChunksVisible();
    }
}
void CMobileGameRenderer::CameraOrthoSizeChanged(void* data, float fov) {
    CMobileGameRenderer* ptr = (CMobileGameRenderer*)data;
    ptr->zoom_slider_value = (int)((1.0f - (fov - ptr->camera->OrthoSizeMin) / (ptr->camera->OrthoSizeMax - ptr->camera->OrthoSizeMin)) * 100);
    ptr->renderer->UpdateFlatModelMinMax(fov);
}

void CMobileGameRenderer::CameraRotate(void* data, CCPanoramaCamera* cam)
{
    CMobileGameRenderer* ptr = (CMobileGameRenderer*)data;
    if (ptr->SplitFullImage) {
    }
}
void CMobileGameRenderer::BeforeQuitCallback(COpenGLView* view) {
    CMobileGameRenderer* renderer = (CMobileGameRenderer*)view->GetRenderer();
    renderer->SaveSettings();
}

void CMobileGameRenderer::AddTextureToQueue(CCTexture* tex, int x, int y, int id) {
    texLoadQueue->Push(tex, x, y, id);
}
void CMobileGameRenderer::SwitchMode(PanoramaMode mode)
{
    this->mode = mode;
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
void CMobileGameRenderer::UpdateConsoleState() {
    ShowWindow(GetConsoleWindow(), show_console ? SW_SHOW : SW_HIDE);
    if (show_console) View->Active();
}
void CMobileGameRenderer::LoadAndChechRegister() {
    if (!reg_dialog_showed) {
        loopCount += View->GetDeltaTime();
        if (loopCount >= 10.0f) {
            loopCount = 0;
            reg_dialog_showed = true;
            time_t timep;
            struct tm* p;
            time(&timep);
            p = gmtime(&timep);
            int lastDayShowRegCount = settings->GetSettingInt(L"regShowLast", p->tm_mday);
            int todayShowRegCount = 0;
            if (lastDayShowRegCount == p->tm_mday) {
                todayShowRegCount = settings->GetSettingInt(L"regShowCount", 0);
            }
            else 
                settings->SetSettingInt(L"regShowCount", 0);

            if (!settings->GetSettingBool(L"registered", false) && todayShowRegCount < 2) {
                settings->SetSettingInt(L"regShowCount", todayShowRegCount++);
                settings->SetSettingInt(L"regShowLast", p->tm_mday);
                View->SendWindowsMessage(WM_CUSTOM_SHOW_REG, 0, 0);
            }
        }
    }
}