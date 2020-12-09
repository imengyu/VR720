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
    glDisable(GL_DITHER);
    glDisable(GL_DEPTH_TEST);

    //Re create shader
    if(shader != nullptr) delete shader;
    InitShader();

    //reload resources
    ReleaseBuiltInResources();
    LoadBuiltInResources();

    //Re buffer all data
    ReBufferAllData();
}
void CCPanoramaRenderer::Init()
{
    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glDisable(GL_DITHER);
    glDisable(GL_DEPTH_TEST);

    LoadBuiltInResources();

    std::string vshaderPath = CCAssetsManager::GetResourcePath("shader", "Standard_vertex.glsl");
    std::string fshaderPath = CCAssetsManager::GetResourcePath("shader", "Standard_fragment.glsl");

    globalRenderInfo = new CCRenderGlobal();

    vshaderCode = CCAssetsManager::LoadStringResource(vshaderPath.c_str());
    fshaderCode = CCAssetsManager::LoadStringResource(fshaderPath.c_str());

    InitShader();
    CreateMainModel();

    globalRenderInfo->glVendor = (GLubyte*)glGetString(GL_VENDOR);            //返回负责当前OpenGL实现厂商的名字
    globalRenderInfo->glRenderer = (GLubyte*)glGetString(GL_RENDERER);    //返回一个渲染器标识符，通常是个硬件平台
    globalRenderInfo->glVersion = (GLubyte*)glGetString(GL_VERSION);    //返回当前OpenGL实现的版本号
    globalRenderInfo->glslVersion = (GLubyte*)glGetString(GL_SHADING_LANGUAGE_VERSION);//返回着色预压编译器版本号
}
void CCPanoramaRenderer::Destroy()
{
    CCRenderGlobal::Destroy();

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

    shader = new CCShader(vshaderCode.c_str(), fshaderCode.c_str());

    globalRenderInfo->viewLoc = shader->GetUniformLocation("view");
    globalRenderInfo->projectionLoc = shader->GetUniformLocation("projection");
    globalRenderInfo->modelLoc = shader->GetUniformLocation("model");
    globalRenderInfo->ourTextrueLoc = shader->GetUniformLocation("ourTexture");
    globalRenderInfo->useColorLoc = shader->GetUniformLocation("useColor");
    globalRenderInfo->ourColorLoc = shader->GetUniformLocation("ourColor");
    globalRenderInfo->texOffest = shader->GetUniformLocation("texOffest");
    globalRenderInfo->texTilling = shader->GetUniformLocation("texTilling");
}

void CCPanoramaRenderer::Render(float deltaTime)
{
    CCRenderGlobal::SetInstance(globalRenderInfo);
    CCTexture::UnUse();
    shader->Use();

    //摄像机矩阵
    Renderer->View->CalcMainCameraProjection(shader);

    //模型位置和矩阵映射
    model = mainModel->GetModelMatrix();
    glUniformMatrix4fv(globalRenderInfo->modelLoc, 1, GL_FALSE, glm::value_ptr(model));

    //完整绘制
    glUniform1i(globalRenderInfo->useColorLoc, 0);
    if (renderOn) {
        //绘制外层缩略图
        if (!renderNoPanoramaSmall) RenderThumbnail();

        //绘制区块式完整全景球
        if (renderPanoramaFull)
            RenderFullChunks(deltaTime);

        if (renderPanoramaFlat)
            RenderFlat();
    }

}
void CCPanoramaRenderer::LoadBuiltInResources() {
    panoramaCheckTex =
            CCAssetsManager::LoadTexture(
                    CCAssetsManager::GetResourcePath("textures", "checker.jpg").c_str());
    if(!panoramaCheckTex->Loaded())
        panoramaCheckTex = nullptr;

    panoramaRedCheckTex = CCAssetsManager::LoadTexture(
            CCAssetsManager::GetResourcePath("textures", "red_checker.jpg").c_str());

}
void CCPanoramaRenderer::ReleaseBuiltInResources() {
    if(!panoramaCheckTex.IsNullptr())
        panoramaCheckTex = nullptr;
    if(!panoramaCheckTex.IsNullptr())
        panoramaCheckTex = nullptr;
}
void CCPanoramaRenderer::ReleaseTexPool() {
    renderPanoramaFull = false;
    if (!panoramaTexPool.empty()) {
        for (auto & it : panoramaTexPool)
            it.ForceRelease();
        panoramaTexPool.clear();
    }
    panoramaThumbnailTex = nullptr;
}
void CCPanoramaRenderer::ReBufferAllData() {
    mainModel->ReBufferData();
    if(!fullModels.empty())
        for (auto m : fullModels) {
            m->model->ReBufferData();
        }
    if(!panoramaTexPool.empty())
        for (const auto& m : panoramaTexPool) {
            if(!m.IsNullptr())
                m->ReBufferData();
        }
}

//全景模型创建与销毁
//*************************

void CCPanoramaRenderer::CreateMainModel() {

    mainModel = new CCModel();
    mainModel->Mesh = new CCMesh();
    mainModel->Material = new CCMaterial(panoramaCheckTex);
    mainModel->Material->tilling = glm::vec2(50.0f, 25.0f);

    CreateMainModelSphereMesh(mainModel->Mesh.GetPtr());

    mainFlatModel = new CCModel();
    mainFlatModel->Mesh = new CCMesh();
    mainFlatModel->Mesh->DrawType = GL_DYNAMIC_DRAW;
    mainFlatModel->Material = new CCMaterial(panoramaCheckTex);
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

    for (int j = 0; j <= sphereSegmentY; j++) {
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++) {
            mesh->positions.push_back(glm::vec3(0.5f - u, (0.5f - v) / 2.0f, 0.0f));
            mesh->texCoords.push_back(glm::vec2(1.0f - u, v));
            u += ustep;
        }
        v += vstep;
    }


    int vertices_line_count = sphereSegmentX + 1;
    for (int j = 0, c = sphereSegmentY; j < c; j++) {
        int line_start_pos = (j)*vertices_line_count;
        for (int i = 0; i < sphereSegmentX; i++) {

            mesh->indices.push_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count, 0, -1));

            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i + 1, 0, -1));
        }
    }

    //创建缓冲区
    mesh->GenerateBuffer();
}
void CCPanoramaRenderer::CreateMainModelSphereMesh(CCMesh* mesh) const {

    float r = 1.0f;
    float ustep = 1.0f / (float)sphereSegmentX, vstep = 1.0f / (float)sphereSegmentY;
    float u = 0, v = 0;

    //顶点
    //=======================================================

    for (int j = 0; j <= sphereSegmentY; j++) {
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++) {
            mesh->positions.push_back(GetSpherePoint(u, v, r));
            mesh->texCoords.push_back(glm::vec2(1.0f - u, v));
            u += ustep;
        }
        v += vstep;
    }

    //顶点索引
    //=======================================================

    int vertices_line_count = sphereSegmentX + 1;
    int line_start_pos = 0;

    for (int i = 0; i < sphereSegmentX; i++) {
        mesh->indices.push_back(CCFace(line_start_pos + i + 1 + vertices_line_count));
        mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count));
        mesh->indices.push_back(CCFace(line_start_pos + i));
    }
    for (int j = 1; j < sphereSegmentY - 1; j++) {
        line_start_pos = j * vertices_line_count;
        for (int i = 0; i < sphereSegmentX; i++) {

            mesh->indices.push_back(CCFace(line_start_pos + i));
            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count + 1));
            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count));

            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count + 1));
            mesh->indices.push_back(CCFace(line_start_pos + i));
            mesh->indices.push_back(CCFace(line_start_pos + i + 1));
        }
    }

    line_start_pos = (sphereSegmentY - 1) * vertices_line_count;
    for (int i = 0; i < sphereSegmentX; i++) {
        mesh->indices.push_back(CCFace(line_start_pos + i));
        mesh->indices.push_back(CCFace(line_start_pos + i + 1));
        mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count));
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
            mesh->texCoords.push_back(glm::vec2(cu, cv));
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

            mesh->indices.push_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count, 0, -1));

            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i + 1, 0, -1));
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
            pChunkModel->model->Material = new CCMaterial(panoramaRedCheckTex);
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

void CCPanoramaRenderer::ResetModel()
{
    mainModel->Reset();
    mainFlatModel->Material->offest.x = 0.0f;
    mainModelYRotationBase = 0.0f;
}
void CCPanoramaRenderer::RotateModel(float xoffset, float yoffset)
{
    glm::vec3 localEulerAngles = mainModel->GetLocalEulerAngles();
    mainModel->SetLocalEulerAngles(glm::vec3(
            localEulerAngles.x - yoffset,
            localEulerAngles.y + xoffset,
            localEulerAngles.z
    ));
    mainModelYRotationBase += xoffset;

    UpdateFullChunksVisible();
}
void CCPanoramaRenderer::RotateModelForce(float x, float y)
{
    glm::vec3 localEulerAngles = mainModel->GetLocalEulerAngles();
    mainModel->SetLocalEulerAngles(glm::vec3(
            localEulerAngles.x + x,
            localEulerAngles.y + y,
            localEulerAngles.z
    ));
    mainModelYRotationBase += x;
    UpdateFullChunksVisible();
}
void CCPanoramaRenderer::GyroscopeRotateModel(float x, float y, float z, float w) {

    glm::quat gyroscopeQua = glm::quat(w, x, y, z);
    glm::vec3 euler = glm::degrees(glm::eulerAngles(gyroscopeQua));

    mainModel->SetLocalEulerAngles(glm::vec3(
            euler.x,
            euler.y + mainModelYRotationBase,
            euler.z + 90.0f
    ));
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

void CCPanoramaRenderer::UpdateMercatorControl() {
    //PrecalcMercator();

    CCMesh* mesh = mainFlatModel->Mesh.GetPtr();
    mesh->texCoords.clear();

    float ustep = 1.0f / (float)sphereSegmentX, vstep = 1.0f / (float)sphereSegmentY;
    float u, v = 0;

    for (int j = 0; j <= sphereSegmentY; j++) {
        v += vstep;
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++) {
            u += ustep;
            mesh->texCoords.push_back(GetMercatorUVPoint(1.0f - u, v));
        }
    }

    mesh->ReBufferData();
}
void CCPanoramaRenderer::ResetMercatorControl() const {
    CCMesh* mesh = mainFlatModel->Mesh.GetPtr();
    mesh->texCoords.clear();

    float ustep = 1.0f / (float)sphereSegmentX, vstep = 1.0f / (float)sphereSegmentY;
    float u, v = 0;

    for (int j = 0; j <= sphereSegmentY; j++) {
        v += vstep;
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++) {
            u += ustep;
            mesh->texCoords.emplace_back(1.0 - u, v);
        }
    }
    mesh->ReBufferData();
}

//渲染
//*************************

void CCPanoramaRenderer::RenderThumbnail() const
{
    mainModel->Render();
}
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
void CCPanoramaRenderer::RenderFlat() const {
    mainFlatModel->Render();
}

//更新
//*************************

void CCPanoramaRenderer::UpdateMainModelTex() const
{
    if (!panoramaThumbnailTex.IsNullptr()) {
        mainModel->Material->diffuse = panoramaThumbnailTex;
        mainModel->Material->tilling = glm::vec2(1.0f);
        mainFlatModel->Material->diffuse = panoramaThumbnailTex;
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

                    logger->Log("Star load chunk %d, %d", m->chunkX, m->chunkY);

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
    FlatModelMoveRato = orthoSize / 1.0f;
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
glm::vec2 CCPanoramaRenderer::GetSphereUVPoint(float u, float v, short i) {
    return glm::vec2(u, v);
}
glm::vec3 CCPanoramaRenderer::GetSpherePoint(float u, float v, float r)
{
    constexpr auto PI = glm::pi<float>();
    float x = r * glm::sin(PI * v) * glm::sin(PI * u * 2);
    float y = r * glm::cos(PI * v);
    float z = r * glm::sin(PI * v) * glm::cos(PI * u * 2);
    return glm::vec3(x, y, z);
}
glm::vec2 CCPanoramaRenderer::GetMercatorUVPoint(float u, float v) const
{
    constexpr auto PI = glm::pi<float>();

    float λ0 = MercatorControlPoint0.x * PI;
    float  λ = u * PI;
    float  ф = v * PI;

    float y = glm::atanh(glm::sin(ф));
    return glm::vec2(λ - λ0, y);

    /*
    float λ0 = MercatorControlPoint0.x * PI;
    float  λ = u * PI;
    float  ф = v * PI;
    float  λp = Mercator_λp;
    float  фp = Mercator_фp;
    float A = glm::sin(фp) * glm::sin(ф) - glm::cos(фp) * glm::cos(ф) * glm::sin(λ - λ0);

    float x = glm::atan(
        (glm::tan(ф) * glm::cos(фp) + glm::sin(фp) - glm::sin(λ - λ0)) /
        (glm::cos(λ - λ0))
    );
    float y = glm::atan(A);
    return glm::vec2(x, y);
    */
}
void CCPanoramaRenderer::PrecalcMercator() {
    constexpr auto PI = glm::pi<float>();

    float λ0 = MercatorControlPoint0.x * PI;
    float λ1 = MercatorControlPoint1.x * PI;
    float λ2 = MercatorControlPoint2.x * PI;
    float ф1 = MercatorControlPoint1.y * PI;
    float ф2 = MercatorControlPoint2.y * PI;

    Mercator_λp = glm::atan(
        (glm::cos(ф1) * glm::sin(ф2) * glm::cos(λ1) - glm::sin(ф1) * glm::cos(ф2) * glm::cos(λ2)) /
        (glm::sin(ф1) * glm::cos(ф2) * glm::sin(λ2) - glm::cos(ф1) * glm::sin(ф2) * glm::sin(λ1))
    );
    Mercator_фp = glm::atan(
        -((glm::cos(Mercator_λp - λ1)) / glm::tan(ф1))
    );
}


