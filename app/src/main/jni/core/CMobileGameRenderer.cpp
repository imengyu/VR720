#include "stdafx.h"
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
        //主图
        renderer->panoramaThumbnailTex = texLoadQueue->Push(new CCTexture(), 0, 0, -1);//MainTex
        renderer->panoramaThumbnailTex->backupData = true;
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
        last_image_error = std::string(fileManager->GetLastError());
        ShowErrorDialog();
    }
}
void CMobileGameRenderer::MarkCloseFile() {
    if(file_opened)
        should_close_file = true;
    else
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::FileClosed);
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
            logger->LogError2("Too big image (%.2f, %.2f) that cant split chunks.", chunkW, chunkH);
            SplitFullImage = false;
            return;
        }

        int chunkWi = (int)ceil(chunkW), chunkHi = (int)ceil(chunkH);

        uiInfo->currentImageAllChunks = chunkWi * chunkHi;
        uiInfo->currentImageLoadChunks = 0;
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);

        if(fullChunkLoadEnabled) {
            logger->Log("Image use split mode , size: %d, %d", chunkWi, chunkHi);
            renderer->sphereFullSegmentX =
                    renderer->sphereSegmentX + (renderer->sphereSegmentX % chunkWi);
            renderer->sphereFullSegmentY =
                    renderer->sphereSegmentY + (renderer->sphereSegmentY % chunkHi);
            renderer->GenerateFullModel(chunkWi, chunkHi);
        }
    }
    else {
        uiInfo->currentImageAllChunks = 0;
        uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
    }

    SwitchMode(mode);
}

//测试
//*************************

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

//*************************

bool CMobileGameRenderer::ReInit() {
    if (render_init_finish) {
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

    View->SetCamera(camera);
    View->SetMouseCallback(MouseCallback);
    View->SetZoomViewCallback(ScrollCallback);

    SwitchMode(mode);

    //CreateTestGlProgram();

    //renderer->renderPanoramaFullTest = true;
    //renderer->renderPanoramaFullRollTest = true;
    //renderer->renderPanoramaATest = true;
    //TestSplitImageAndLoadTexture();

    render_init_finish = true;
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
}
void CMobileGameRenderer::Resize(int Width, int Height)
{
    glViewport(0, 0, Width, Height);
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
            if (renderer->mode <= PanoramaMode::PanoramaOuterBall) {
                float xoffset = -renderer->xoffset * renderer->GetMouseSensitivity();
                float yoffset = -renderer->yoffset * renderer->GetMouseSensitivity();

                renderer->renderer->RotateModel(xoffset, renderer->gyroEnabled ? 0 : yoffset);
            }
                //全景模式是更改U偏移和纬度偏移
            else if (renderer->mode == PanoramaMode::PanoramaMercator) {

            }
            else if (renderer->mode == PanoramaMode::PanoramaFull360 || renderer->mode == PanoramaMode::PanoramaFullOrginal) {
                float xoffset = -renderer->xoffset * renderer->GetMouseSensitivity();
                float yoffset = -renderer->yoffset * renderer->GetMouseSensitivity();
                renderer->renderer->MoveModel(xoffset, yoffset);
            }
            break;
        }
        case ViewMouseEventType::ViewMouseMouseUp: {
            if(renderer->DragCurrentVelocity.x > 0 || renderer->DragCurrentVelocity.y > 0) {
                renderer->VelocityDragLastOffest.x = renderer->xoffset;
                renderer->VelocityDragLastOffest.y = renderer->gyroEnabled ? 0 : renderer->yoffset;
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
    if (mode <= PanoramaMode::PanoramaOuterBall) {
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
//*************************

void CMobileGameRenderer::Render(float FrameTime)
{
    //loop count
    //===========================

    if (should_close_file) {
        should_close_file = false;
        fileManager->CloseFile();
    }

    if (destroying)
        return;

    if (should_destroy) {
        LOGI("[CMobileGameRenderer] Start destroy in renderer thread");
        Destroy();
        return;
    }

    //渲染
    //===========================

    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);

    //RenderTest();
    renderer->Render(View->GetDeltaTime());

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

    if(ShouldResetMercatorControl) {
        ShouldResetMercatorControl = false;
        renderer->ResetMercatorControl();
    }

    if(ShouldUpdateMercatorControl) {
        ShouldUpdateMercatorControl = false;
        renderer->UpdateMercatorControl();
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

//逻辑控制
//*************************

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
        ptr->logger->Log("Load main tex: id: -1");
        ptr->uiInfo->currentImageLoading = true;
        ptr->uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);

        //Load full main tex
        auto* result = new TextureLoadQueueDataResult();
        result->buffer = ptr->fileManager->CurrentFileLoader->GetAllImageData();
        if (!result->buffer) {
            ptr->last_image_error = std::string("图像可能已经损坏，错误信息：") + std::string(ptr->fileManager->CurrentFileLoader->GetLastError());
            ptr->ShowErrorDialog();
            ptr->logger->LogError2("Load tex main buffer failed : %s", ptr->fileManager->CurrentFileLoader->GetLastError());
            delete result;
            return nullptr;
        }

        result->size = ptr->fileManager->CurrentFileLoader->GetFullDataSize();
        result->compoents = ptr->fileManager->CurrentFileLoader->GetImageDepth();
        glm::vec2 size = ptr->fileManager->CurrentFileLoader->GetImageScaledSize();
        result->width = (int)size.x;
        result->height = (int)size.y;
        result->success = true;

        ptr->logger->Log("Load tex buffer: w: %d h: %d (%d)  Buffer Size: %d", (int)size.x, (int)size.y, result->compoents, result->size);
        ptr->uiInfo->currentImageLoading = false;
        ptr->uiEventDistributor->SendEvent(CCMobileGameUIEvent::UiInfoChanged);
        ptr->uiEventDistributor->SendEvent(CCMobileGameUIEvent::MarkLoadingEnd);

        return result;
    }
    else {
        ptr->logger->Log("Load block tex : x: %d y: %d id: %d", info->x, info->y, info->id);
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

        ptr->renderer->renderPanoramaFull =
                ptr->fullChunkLoadEnabled && ptr->SplitFullImage && fov < 40;

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
char* CMobileGameRenderer::GetDebugText() {
    return debugText;
}
void CMobileGameRenderer::WriteDebugText(char* str) {
    strcpy(debugText, str);
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
        renderer->ResetModel();
        renderer->renderPanoramaFlat = true;
        renderer->renderPanoramaFull = false;
        renderer->renderNoPanoramaSmall = true;
        renderer->renderPanoramaFlatXLoop = false;
        ShouldUpdateMercatorControl = true;
        MouseSensitivityMax = 0.0005f;
        MouseSensitivityMin = 0.0015f;
        break;
    case PanoramaFullOrginal:
        camera->Projection = CCameraProjection::Orthographic;
        camera->SetMode(CCPanoramaCameraMode::OrthoZoom);
        renderer->ResetModel();
        renderer->renderPanoramaFull = false;
        renderer->renderPanoramaFlat = true;
        renderer->renderNoPanoramaSmall = true;
        renderer->renderPanoramaFlatXLoop = false;
        ShouldResetMercatorControl = true;
        MouseSensitivityMax = 0.0005f;
        MouseSensitivityMin = 0.0015f;
        break;
    case PanoramaFull360:
        camera->Projection = CCameraProjection::Orthographic;
        camera->SetMode(CCPanoramaCameraMode::OrthoZoom);
        renderer->ResetModel();
        renderer->renderPanoramaFull = false;
        renderer->renderPanoramaFlat = true;
        renderer->renderNoPanoramaSmall = true;
        renderer->renderPanoramaFlatXLoop = true;
        ShouldResetMercatorControl = true;
        MouseSensitivityMax = 0.0005f;
        MouseSensitivityMin = 0.0015f;
        break;
    case PanoramaAsteroid:
        camera->Projection = CCameraProjection::Perspective;
        camera->SetMode(CCPanoramaCameraMode::CenterRoate);
        camera->Position.z = 1.0f;
        camera->FiledOfView = 135.0f;
        camera->FovMin = 35.0f;
        camera->FovMax = 135.0f;
        MouseSensitivityMin = 0.01f;
        MouseSensitivityMax = 0.1f;
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
        renderer->renderPanoramaFull = fullChunkLoadEnabled && SplitFullImage && camera->FiledOfView < 30;
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
        renderer->renderPanoramaFull = fullChunkLoadEnabled && SplitFullImage && camera->FiledOfView < 30;
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
void CMobileGameRenderer::UpdateGyroValue(float x, float y, float z, float w) const {
    if(renderer) {
        renderer->gyroEnabled = gyroEnabled;
        if (gyroEnabled && mode <= PanoramaMode::PanoramaOuterBall)
            renderer->GyroscopeRotateModel(x, y, z, w);
    }
}
void CMobileGameRenderer::SetGyroEnabled(bool enable) {
    gyroEnabled = enable;
    if(renderer && !gyroEnabled)
        renderer->ResetModel();
}
void CMobileGameRenderer::SetEnableFullChunkLoad(bool enable) {
    fullChunkLoadEnabled = enable;
}
void CMobileGameRenderer::SetVREnabled(bool enable) {
    vREnabled = enable;
}
void CMobileGameRenderer::SetMouseDragVelocity(float x, float y) {
    DragCurrentVelocity.x = glm::abs(x);
    DragCurrentVelocity.y = glm::abs(y);

    if(DragCurrentVelocity.x < 2 && DragCurrentVelocity.y <= 2)
        VelocityDragCurrentIsInSim = false;
}


