#include "CCPanoramaRenderer.h"
#include "COpenGLRenderer.h"
#include "COpenGLView.h"
#include "CCFileManager.h"
#include "CCAssetsManager.h"
#include "CCMesh.h"
#include "CCMeshLoader.h"
#include "CCMaterial.h"
#include "CCRenderGlobal.h"
#include "CMobileGameRenderer.h"
#include "../utils/MathUtils.h"
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtx/euler_angles.hpp>



CCPanoramaRenderer::CCPanoramaRenderer(CMobileGameRenderer* renderer)
{
    Renderer = renderer;
    logger = Logger::GetStaticInstance();
}

void CCPanoramaRenderer::ReInit() {

    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glEnable(GL_TEXTURE_CUBE_MAP);
    glEnable(GL_RENDERBUFFER);
    glDisable(GL_DITHER);
    glDisable(GL_DEPTH_TEST);

    //Re create shader
    if(shader != nullptr) delete shader;
    if(shaderCylinder != nullptr) delete shaderCylinder;
    InitShader();

    //Re buffer all data
    ReBufferAllData();

    fbo = 0;
    depthBuffer = 0;
}
void CCPanoramaRenderer::Init()
{
    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glEnable(GL_TEXTURE_CUBE_MAP);
    glEnable(GL_RENDERBUFFER);
    glDisable(GL_DITHER);
    glDisable(GL_DEPTH_TEST);

    reqLoadBuiltInResources = true;

    std::string vShaderPath = CCAssetsManager::GetResourcePath("shader", "Standard_vertex.glsl");
    std::string fShaderPath = CCAssetsManager::GetResourcePath("shader", "Standard_fragment.glsl");

    std::string vCylinderShaderPath = CCAssetsManager::GetResourcePath("shader", "Cylinder_vertex.glsl");
    std::string fCylinderShaderPath = CCAssetsManager::GetResourcePath("shader", "Cylinder_fragment.glsl");

    globalRenderInfo = new CCRenderGlobal();

    vShaderCode = CCAssetsManager::LoadStringResource(vShaderPath.c_str());
    fShaderCode = CCAssetsManager::LoadStringResource(fShaderPath.c_str());
    vCylinderShaderCode = CCAssetsManager::LoadStringResource(vCylinderShaderPath.c_str());
    fCylinderShaderCode = CCAssetsManager::LoadStringResource(fCylinderShaderPath.c_str());

    InitShader();
    CreateMainModel();

    panoramaCubeMapTex = new CCTexture(GL_TEXTURE_CUBE_MAP);
    panoramaCubeMapTex->cubeMapSize = 512;
    panoramaCubeMapTex->CreateGLTexture();

    globalRenderInfo->glVendor = (GLubyte*)glGetString(GL_VENDOR);            //返回负责当前OpenGL实现厂商的名字
    globalRenderInfo->glRenderer = (GLubyte*)glGetString(GL_RENDERER);    //返回一个渲染器标识符，通常是个硬件平台
    globalRenderInfo->glVersion = (GLubyte*)glGetString(GL_VERSION);    //返回当前OpenGL实现的版本号
    globalRenderInfo->glslVersion = (GLubyte*)glGetString(GL_SHADING_LANGUAGE_VERSION);//返回着色预压编译器版本号
}
void CCPanoramaRenderer::Destroy()
{
    CCRenderGlobal::Destroy();
    VideoTexUpdateRunStatus(false);

    if(fbo != 0) glDeleteBuffers(1, &fbo);

    if (shader != nullptr) {
        delete shader;
        shader = nullptr;
    }
    if (mainModel != nullptr) {
        delete mainModel;
        mainModel = nullptr;
    }
    if (mainFlatModel != nullptr) {
        delete mainFlatModel;
        mainFlatModel = nullptr;
    }

    ReleaseBuiltInResources();
    ReleaseTexPool();
}
void CCPanoramaRenderer::InitShader() {

    shader = new CCShader(vShaderCode.c_str(), fShaderCode.c_str());

    globalRenderInfo->viewLoc = shader->GetUniformLocation("view");
    globalRenderInfo->projectionLoc = shader->GetUniformLocation("projection");
    globalRenderInfo->modelLoc = shader->GetUniformLocation("model");
    globalRenderInfo->ourTextrueLoc = shader->GetUniformLocation("ourTexture");
    globalRenderInfo->texOffest = shader->GetUniformLocation("texOffest");
    globalRenderInfo->texTilling = shader->GetUniformLocation("texTilling");

    shaderCylinder = new CCShader(vCylinderShaderCode.c_str(), fCylinderShaderCode.c_str());

    globalRenderInfo->cubeMap = shaderCylinder->GetUniformLocation("cubeMap");
}

//渲染
//*************************

void CCPanoramaRenderer::Render(float deltaTime) {
    CCRenderGlobal::SetInstance(globalRenderInfo);
    CCTexture::UnUse(GL_TEXTURE_2D);
    CCTexture::UnUse(GL_TEXTURE_CUBE_MAP);

    shader->Use();

    //视频贴图更新
    if(videoTextureFlushEnabled && !videoTextureLock && !panoramaThumbnailTex.IsNullptr()) {
        if(videoTexMarkDirty) {
            videoTexMarkDirty = false;
            panoramaThumbnailTex->ReBufferData(false);
        }
    }

    //墨卡托投影
    if(renderOn && currentFrameMercatorCylinder) {
        currentFrameMercatorCylinder = false;
        RenderPreMercatorCylinder();
        RenderMercatorCylinder();
        return;
    }

    //摄像机矩阵
    if(currentFrameVr) {
        currentFrameVr = false;
        //VR双屏模式则需要使用不同的屏幕宽高
        Renderer->View->CalcMainCameraProjectionWithWH(shader, currentFrameVrW, currentFrameVrH);
    }
    else Renderer->View->CalcMainCameraProjection( shader);

    //模型位置和矩阵映射
    model = mainModel->GetModelMatrix();
    glUniformMatrix4fv(globalRenderInfo->modelLoc, 1, GL_FALSE, glm::value_ptr(model));

    //完整绘制
    if (!renderOn)
        return;

    //绘制外层缩略图
    if (!renderNoPanoramaSmall) RenderThumbnail();

    //绘制区块式完整全景球
    if (renderPanoramaFull)
        RenderFullChunks(deltaTime);

    if (renderPanoramaFlat)
        RenderFlat();

    currentFrameMercatorCylinder = false;
}
void CCPanoramaRenderer::RenderPreMercatorCylinder() {
    shader->Use();
    panoramaCubeMapTex->Use();

    // framebuffer object
    if (fbo == 0)
        glGenFramebuffers(1, &fbo);
    if (depthBuffer == 0)
        glGenRenderbuffers(1, &depthBuffer);

    //------------------------------------

    glBindFramebuffer(GL_FRAMEBUFFER, fbo);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP,
                           panoramaCubeMapTex->texture, 0);

    glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer);
    glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, panoramaCubeMapTex->cubeMapSize,
                          panoramaCubeMapTex->cubeMapSize);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer);

    //------------------------------------

    //Change view port to cube map size
    glViewport(0, 0, panoramaCubeMapTex->cubeMapSize, panoramaCubeMapTex->cubeMapSize);

    //use capture camera projection
    CCamera *camera = Renderer->GetMercatorCylinderCaptureCamera();

    GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE)
        logger->LogWarn2(LOG_TAG, "glCheckFramebufferStatus ret : %04x (%d)", status, status);

    glUniformMatrix4fv(shader->modelLoc, 1, GL_FALSE, glm::value_ptr(mainModel->GetModelMatrix()));

    //Render sphere to cube map
    for (GLuint face = 0; face < 6; face++) {

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                               GL_TEXTURE_CUBE_MAP_POSITIVE_X + face,
                               panoramaCubeMapTex->texture, 0);

        //Switch camera to one face

        camera->SwitchToFace(face);

        Renderer->View->CalcCameraProjection(camera, shader,
                                             panoramaCubeMapTex->cubeMapSize,
                                             panoramaCubeMapTex->cubeMapSize);

        //Clear first
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        glClear((UINT)GL_COLOR_BUFFER_BIT | (UINT)GL_DEPTH_BUFFER_BIT);

        //Render
        RenderThumbnail();
    }

    status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE)
        logger->LogWarn2(LOG_TAG, "glCheckFramebufferStatus ret : %04x (%d)", status, status);

    const GLenum drawBuffers[] = { GL_COLOR_ATTACHMENT0 };
    glDrawBuffers(1, drawBuffers);

    //------------------------------------

    //set view port back
    glViewport(0,0, currentFrameVrW, currentFrameVrH);

    //Bind 0, which means render to back buffer, as a result, fbo is unbound
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glBindRenderbuffer(GL_RENDERBUFFER, 0);

    shaderCylinder->Use();
    CCTexture::UnUse(GL_TEXTURE_2D);
    panoramaCubeMapTex->Use();
}
void CCPanoramaRenderer::RenderMercatorCylinder() {
    //------------------------------------
    // start Normal render >

    //摄像机矩阵
    if(currentFrameVr) {
        currentFrameVr = false;
        Renderer->View->CalcMainCameraProjectionWithWH(shaderCylinder, currentFrameVrW, currentFrameVrH);
    }else
        Renderer->View->CalcMainCameraProjection(shaderCylinder);

    RenderFlat();
}
void CCPanoramaRenderer::RenderThumbnail() const { mainModel->Render(); }
void CCPanoramaRenderer::RenderFlat() const { mainFlatModel->Render(); }
void CCPanoramaRenderer::RenderFullChunks(float deltaTime)
{
    if (renderPanoramaFullTest && !renderPanoramaFullRollTest) {
        renderPanoramaFullTestTime += deltaTime;
        if (renderPanoramaFullTestAutoLoop) {
            if (renderPanoramaFullTestTime > 1) {
                renderPanoramaFullTestTime = 0;
                if (renderPanoramaFullTestIndex < (int)fullModels.size() - 1)renderPanoramaFullTestIndex++;
                else renderPanoramaFullTestIndex = 0;
            }
        }
        fullModels[renderPanoramaFullTestIndex]->model->Render();
    }
    else {
        for (auto m : fullModels) {
            if (m->model->Visible)  //渲染区块
                m->model->Render();
        }
    }
}

void CCPanoramaRenderer::LoadBuiltInResources() {
    panoramaCheckTex = new CCTexture();
    panoramaCheckTex->backupData = true;
    panoramaCheckTex->LoadGridTexture(512, 512, 16, true, true);
}
void CCPanoramaRenderer::ReleaseBuiltInResources() {
}
void CCPanoramaRenderer::ReleaseTexPool() {
    renderPanoramaFull = false;
    if (!panoramaTexPool.empty()) {
        for (auto & it : panoramaTexPool)
            it.ForceRelease();

        LOGIF(LOG_TAG, "ReleaseTexPool : %d texture removed in pool", panoramaTexPool.size());
        panoramaTexPool.clear();
    }
    panoramaThumbnailTex = nullptr;
}
void CCPanoramaRenderer::ReBufferAllData() {
    mainModel->ReBufferData();
    mainFlatModel->ReBufferData();
    if(!fullModels.empty())
        for (auto m : fullModels) {
            m->model->ReBufferData();
        }
    if(!panoramaTexPool.empty())
        for (const auto& m : panoramaTexPool) {
            if(!m.IsNullptr())
                m->ReBufferData(true);
        }
    if(!panoramaCubeMapTex.IsNullptr())
        panoramaCubeMapTex->ReBufferData(true);
    if(!panoramaCheckTex.IsNullptr())
        panoramaCheckTex->ReBufferData(true);
}

//全景模型创建与销毁
//*************************

void CCPanoramaRenderer::CreateMainModel() {

    mainModel = new CCModel();
    mainModel->Mesh = new CCMesh();
    mainModel->Material = new CCMaterial();
    mainModel->Material->tilling = glm::vec2(50.0f, 25.0f);

    CreateMainModelSphereMesh(mainModel->Mesh.GetPtr());

    mainFlatModel = new CCModel();
    mainFlatModel->Mesh = new CCMesh();
    mainFlatModel->Mesh->DrawType = GL_DYNAMIC_DRAW;
    mainFlatModel->Material = new CCMaterial();
    mainFlatModel->Material->tilling = glm::vec2(50.0f, 25.0f);

    CreateMainModelFlatMesh(mainFlatModel->Mesh.GetPtr());
}
void CCPanoramaRenderer::CreateMainModelFlatMesh(CCMesh* mesh) const {
    mesh->normals.clear();
    mesh->positions.clear();
    mesh->texCoords.clear();
    mesh->indices.clear();

    float ustep = 1.0f / (float)sphereSegmentX, vstep = 1.0f / (float)sphereSegmentY;
    float u, v = 0;

    for (int j = 0; j < sphereSegmentY; j++) {
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++) {
            mesh->positions.emplace_back(glm::vec3(0.5f - u, (0.5f - v) / 2.0f, 0.0f));
            mesh->texCoords.emplace_back(glm::vec2(1.0f - u, v));
            u += ustep;
        }
        v += vstep;
    }


    int vertices_line_count = sphereSegmentX + 1;
    for (int j = 0, c = sphereSegmentY; j < c; j++) {
        int line_start_pos = (j)*vertices_line_count;
        for (int i = 0; i < sphereSegmentX; i++) {

            mesh->indices.emplace_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count, 0, -1));

            mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i + 1, 0, -1));
        }
    }

    //创建缓冲区
    mesh->GenerateBuffer();
}
void CCPanoramaRenderer::CreateMainModelSphereMesh(CCMesh* mesh) const {

    float r = 1.0f;
    float ustep = 1.0f / (float)sphereSegmentX, vstep = 1.0f / (float)sphereSegmentY;
    float u, v = 0;

    //顶点
    //=======================================================

    for (int j = 0; j <= sphereSegmentY; j++) {
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++) {
            mesh->positions.push_back(GetSpherePoint(u, v, r));
            mesh->texCoords.emplace_back(glm::vec2(1.0f - u, v));
            u += ustep;
        }
        v += vstep;
    }

    //顶点索引
    //=======================================================

    int vertices_line_count = sphereSegmentX + 1;
    int line_start_pos = 0;

    for (int i = 0; i < sphereSegmentX; i++) {
        mesh->indices.emplace_back(CCFace(line_start_pos + i + 1 + vertices_line_count));
        mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count));
        mesh->indices.emplace_back(CCFace(line_start_pos + i));
    }
    for (int j = 1; j < sphereSegmentY - 1; j++) {
        line_start_pos = j * vertices_line_count;
        for (int i = 0; i < sphereSegmentX; i++) {

            mesh->indices.emplace_back(CCFace(line_start_pos + i));
            mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count + 1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count));

            mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count + 1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i));
            mesh->indices.emplace_back(CCFace(line_start_pos + i + 1));
        }
    }

    line_start_pos = (sphereSegmentY - 1) * vertices_line_count;
    for (int i = 0; i < sphereSegmentX; i++) {
        mesh->indices.emplace_back(CCFace(line_start_pos + i));
        mesh->indices.emplace_back(CCFace(line_start_pos + i + 1));
        mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count));
    }

    //创建缓冲区
    mesh->GenerateBuffer();
}
glm::vec3 CCPanoramaRenderer::CreateFullModelSphereMesh(ChunkModel* info, int segXStart, int segYStart, int segX, int segY) const {

    CCMesh* mesh = info->model->Mesh.GetPtr();

    float r = 0.99f;
    float ustep = 1.0f / (float)sphereFullSegmentX, vstep = 1.0f / (float)sphereFullSegmentY;
    float u, v, cu, cv = 0;

    int segXEnd = segXStart + segX;
    int segYEnd = segYStart + segY;

    float u_start = (float)segXStart * ustep, v_start = (float)segYStart * vstep;
    float custep = 1.0f / (float)(segXEnd - segXStart), cvstep = 1.0f / (float)(segYEnd - segYStart);
    v = v_start;

    int skip = 0;

    for (int j = segYStart; j <= segYEnd; j++) {
        if (j <= 2 || j >= sphereFullSegmentY - 4) {
            skip++;
            continue;
        }
        u = u_start;
        cu = 1.0f;
        for (int i = segXStart; i <= segXEnd; i++) {
            mesh->positions.push_back(GetSpherePoint(u, v, r));
            mesh->texCoords.emplace_back(glm::vec2(cu, cv));
            u += ustep;
            cu -= custep;
        }
        v += vstep;
        cv += cvstep;
    }

    int vertices_line_count = segXEnd - segXStart + 1;
    for (int j = 0, c = segYEnd - segYStart - skip; j < c; j++) {
        int line_start_pos = (j)*vertices_line_count;
        for (int i = 0; i < segXEnd - segXStart; i++) {

            mesh->indices.emplace_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count, 0, -1));

            mesh->indices.emplace_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.emplace_back(CCFace(line_start_pos + i + 1, 0, -1));
        }
    }

    //创建缓冲区
    mesh->GenerateBuffer();

    float center_u = (u_start + ustep * ((float)(segXEnd - segXStart) / 2.0f)),
            center_v = (v_start + vstep * ((float)(segYEnd - segYStart) / 2.0f)),
            center_u_2 = (center_u - u_start) / 10.0f * 9.0f,
            center_v_2 = (center_v - v_start) / 10.0f * 9.0f;

    info->pointA = GetSpherePoint(center_u - center_u_2, center_v - center_v_2, r);
    info->pointB = GetSpherePoint(center_u + center_u_2, center_v - center_v_2, r);
    info->pointC = GetSpherePoint(center_u - center_u_2, center_v + center_v_2, r);
    info->pointD = GetSpherePoint(center_u + center_u_2, center_v + center_v_2, r);

    return GetSpherePoint(center_u, center_v, r);
}

void CCPanoramaRenderer::ReleaseFullModel()
{
    if (!fullModels.empty()) {
        std::vector<ChunkModel*>::iterator it;
        for (it = fullModels.begin(); it != fullModels.end(); it++) {
            ChunkModel* m = *it;
            delete m->model;
            delete m;
        }
        fullModels.clear();
    }
}
void CCPanoramaRenderer::GenerateFullModel(int chunkW, int chunkH)
{
    panoramaFullSplitW = chunkW;
    panoramaFullSplitH = chunkH;

    int segX = (int)floor((float)sphereFullSegmentX / (float)chunkW);
    int segY = (int)floor((float)sphereFullSegmentY / (float)chunkH);

    float chunkWf = 1.0f / (float)chunkW, chunkHf = 1.0f / (float)chunkH;
    for (int i = 0; i < chunkW; i++) {
        for (int j = 0; j < chunkH; j++) {
            auto* pChunkModel = new ChunkModel();
            pChunkModel->model = new CCModel();
            pChunkModel->model->Visible = false;
            pChunkModel->model->Mesh = new CCMesh();
            pChunkModel->model->Material = new CCMaterial();
            pChunkModel->model->Material->tilling = glm::vec2(50.0f);
            pChunkModel->chunkX = chunkW - i - 1;
            pChunkModel->chunkY = j;
            pChunkModel->chunkXv = (float)i / (float)chunkW;
            pChunkModel->chunkYv = (float)j / (float)chunkH;
            pChunkModel->chunkXv = pChunkModel->chunkXv + chunkWf;
            pChunkModel->chunkYv = pChunkModel->chunkYv + chunkHf;
            pChunkModel->pointCenter = CreateFullModelSphereMesh(pChunkModel,
                                                                 i * segX, j * segY,
                                                                 segX + ((i == chunkW - 1 && chunkW % 2 != 0) ? 1 : 0), segY);
            fullModels.push_back(pChunkModel);
        }
    }
}

//模型控制
//*************************

void CCPanoramaRenderer::ResetModel() {
    mainModel->Reset();
    mainFlatModel->Material->offest.x = 0.0f;
    mainModelYRotationBase = 0.0f;
}
void CCPanoramaRenderer::RotateModel(float xoffset, float yoffset)
{
    glm::vec3 localEulerAngles = mainModel->GetLocalEulerAngles();
    if(gyroEnabled) {
        mainModelYRotationBase += (xoffset + yoffset) * Renderer->MoveInGyroSensitivity;
    } else {
        mainModel->SetLocalEulerAngles(glm::vec3(
                localEulerAngles.x - yoffset,
                localEulerAngles.y + xoffset,
                0.0f
        ));
    }

    UpdateFullChunksVisible();
}
void CCPanoramaRenderer::RotateModelForce(float x, float y)
{
    glm::vec3 localEulerAngles = mainModel->GetLocalEulerAngles();
    if(gyroEnabled) {
        mainModelYRotationBase += (x + y) * Renderer->MoveInGyroSensitivity;
    } else {
        mainModel->SetLocalEulerAngles(glm::vec3(
                localEulerAngles.x + x,
                localEulerAngles.y + y,
                0.0f
        ));
    }
    UpdateFullChunksVisible();
}
void CCPanoramaRenderer::GyroscopeRotateModel(float x, float y, float z, float w) {

    glm::quat quat(w,x,y,z); //= glm::angleAxis(2.0f * glm::acos(w), glm::vec3(x,y,z));

    glm::vec3 localEulerAngles = mainModel->GetLocalEulerAngles();
    mainModel->SetLocalEulerAngles(glm::vec3(
            90.0f,
            0.0f,
            mainModelYRotationBase
    ));

    mainModel->SetRotation(quat);
    UpdateFullChunksVisible();
}
void CCPanoramaRenderer::MoveModel(float xoffset, float yoffset) const
{
    if (renderPanoramaFlatXLoop) {
        float v = mainFlatModel->Material->offest.x + xoffset;
        if (v < 0) v += 1.0f;
        else if (v > 1.0f) v -= 1.0f;
        mainFlatModel->Material->offest.x = v;
    }
    else {
        mainModel->Position.x -= xoffset * FlatModelMoveRato;
        if (mainModel->Position.x < FlatModelMin.x) mainModel->Position.x = FlatModelMin.x;
        if (mainModel->Position.x > FlatModelMax.x) mainModel->Position.x = FlatModelMax.x;
    }

    mainModel->Position.y -= yoffset * FlatModelMoveRato;
    if (mainModel->Position.y < FlatModelMin.y) mainModel->Position.y = FlatModelMin.y;
    if (mainModel->Position.y > FlatModelMax.y) mainModel->Position.y = FlatModelMax.y;
}
void CCPanoramaRenderer::MoveModelForce(float x, float y) const
{
    if (renderPanoramaFlatXLoop) {
        float v = mainFlatModel->Material->offest.x + x;
        if (v < 0) v += 1.0f;
        else if (v > 1.0f) v -= 1.0f;
        mainFlatModel->Material->offest.x = v;
    }
    else {
        mainModel->Position.x -= x * FlatModelMoveRato;
        if (mainModel->Position.x < FlatModelMin.x) mainModel->Position.x = FlatModelMin.x;
        if (mainModel->Position.x > FlatModelMax.x) mainModel->Position.x = FlatModelMax.x;
    }

    mainModel->Position.y += y * FlatModelMoveRato;
    if (mainModel->Position.y < FlatModelMin.y) mainModel->Position.y = FlatModelMin.y;
    if (mainModel->Position.y > FlatModelMax.y) mainModel->Position.y = FlatModelMax.y;
}

//更新
//*************************

void CCPanoramaRenderer::UpdateMainModelTex() const
{
    if (!panoramaThumbnailTex.IsNullptr()) {
        mainModel->Material->diffuse = panoramaThumbnailTex;
        mainModel->Material->tilling = glm::vec2(1.0f);
        mainFlatModel->Material->diffuse = isMercator ? panoramaCubeMapTex : panoramaThumbnailTex;
        mainFlatModel->Material->tilling = glm::vec2(1.0f);
    }
    else {
        mainModel->Material->diffuse = panoramaCheckTex;
        mainModel->Material->tilling = glm::vec2(50.0f);
        mainFlatModel->Material->diffuse = panoramaCheckTex;
        mainFlatModel->Material->tilling = glm::vec2(50.0f);
    }
}
void CCPanoramaRenderer::UpdateFullChunksVisible() {
    if (renderPanoramaFull || renderPanoramaFullTest) {
        float fov = Renderer->View->Camera->FiledOfView;
        for (auto m : fullModels) {
            if (fov > 50)
                m->model->Visible = IsInView(m->pointCenter);
            else
                m->model->Visible = IsInView(m->pointA) || IsInView(m->pointB) || IsInView(m->pointC) || IsInView(m->pointD);
            if (m->model->Visible) {
                if (!m->loadMarked && !renderPanoramaFullTest) {//加载贴图
                    m->loadMarked = true;

                    logger->Log(LOG_TAG, "Star load chunk %d, %d", m->chunkX, m->chunkY);

                    auto* tex = new CCTexture();
                    tex->backupData = true;
                    tex->wrapS = GL_MIRRORED_REPEAT;
                    tex->wrapT = GL_MIRRORED_REPEAT;
                    m->model->Material->diffuse = tex;
                    m->model->Material->tilling = glm::vec2(1.0f, 1.0f);
                    panoramaTexPool.emplace_back(tex);
                }
            }
        }
    }
}
void CCPanoramaRenderer::UpdateFlatModelMinMax(float orthoSize) {
    FlatModelMin = glm::vec2(-((1.0f - orthoSize) / 2.0f), -((1.0f - orthoSize) / 4.0f));
    FlatModelMax = glm::vec2((1.0f - orthoSize) / 2.0f, (1.0f - orthoSize) / 4.0f);
    //FlatModelMoveRato = orthoSize / 1.0f;
    MoveModelForce(0, 0);
}

//数学计算
//*************************

bool CCPanoramaRenderer::IsInView(glm::vec3 worldPos)
{
    CCamera* cam = Renderer->View->Camera;
    glm::vec3 viewPos = cam->World2Screen(worldPos, model);

    //glm::vec3 dir = glm::normalize(worldPos - cam->Position);
    //float dot = glm::dot(cam->Front, dir);     //判断物体是否在相机前面  

    return viewPos.x >= 0 && viewPos.x <= (float)Renderer->View->Width && viewPos.y >= 0 &&
           viewPos.y <= (float)Renderer->View->Height;
}
glm::vec3 CCPanoramaRenderer::GetSpherePoint(float u, float v, float r)
{
    auto PI = glm::pi<float>();
    float x = r * glm::sin(PI * v) * glm::sin(PI * u * 2);
    float y = r * glm::cos(PI * v);
    float z = r * glm::sin(PI * v) * glm::cos(PI * u * 2);
    return glm::vec3(x, y, z);
}

void CCPanoramaRenderer::SetCurrentFrameVRValue(bool isVr, int w, int h) {
    currentFrameVr = isVr;
    currentFrameVrW = w;
    currentFrameVrH = h;
}
void CCPanoramaRenderer::SetIsMercator(bool is) {
    isMercator = is;
    mainFlatModel->Material->diffuse = isMercator ? panoramaCubeMapTex : panoramaThumbnailTex;
}

void CCPanoramaRenderer::VideoTexUpdateRunStatus(bool enable) {
    videoTextureFlushEnabled = enable;
}
void CCPanoramaRenderer::VideoTexReset() {
    videoTexMarkDirty = false;
}
void CCPanoramaRenderer::VideoTexMarkDirty() {
    videoTexMarkDirty = true;
}
void CCPanoramaRenderer::VideoTexLock(bool lock) {
    videoTextureLock = lock;
}
void CCPanoramaRenderer::VideoTexDetermineSize(int *w, int *h) const {
    int vw = *w, vh = *h;
    if(vw >= vh && vw > 4096) {
        vh = (int) (4096.0 / vw * vh);
        vw = 4096;
    } else if(vw < vh && vh > 2048) {
        vw = (int)(2048.0 / vh * vw);
        vh = 2048;
    }

    *w = vw; *h = vh;
    panoramaThumbnailTex->width = vw;
    panoramaThumbnailTex->height = vh;
    panoramaThumbnailTex->DoBackupBufferData(nullptr, vw, vh, GL_RGBA);
}



