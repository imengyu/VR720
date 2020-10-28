#include "CCPanoramaRenderer.h"
#include "CApp.h"
#include "COpenGLRenderer.h"
#include "CGameRenderer.h"
#include "CGameRenderer.h"
#include "CCMesh.h"
#include "CCMeshLoader.h"
#include "CCMaterial.h"
#include "CCRenderGlobal.h"

CCPanoramaRenderer::CCPanoramaRenderer(CGameRenderer* renderer)
{
    Renderer = renderer;
    logger = CApp::Instance->GetLogger();
}

void CCPanoramaRenderer::Init()
{
    glEnable(GL_TEXTURE_2D);
    glEnable(GL_BLEND);
    glEnable(GL_ALPHA_TEST);
    glDisable(GL_DITHER);
    glDisable(GL_LIGHTING);
    glDisable(GL_DEPTH_TEST);
    glEnableClientState(GL_VERTEX_ARRAY);

    LoadBuiltInResources();

    staticTexPool.push_back(uiLogoTex);
    staticTexPool.push_back(uiOpenButtonTex);
    staticTexPool.push_back(uiTitleTex);
    staticTexPool.push_back(uiFailedTex);

    shader = new CCShader(
        CCFileManager::GetResourcePath("shader", "Standard_vertex.glsl").c_str(),
        CCFileManager::GetResourcePath("shader", "Standard_fragment.glsl").c_str());

    CreateMainModel();

    globalRenderInfo = new CCRenderGlobal();

    globalRenderInfo->glVendor = (GLubyte*)glGetString(GL_VENDOR);            //返回负责当前OpenGL实现厂商的名字
    globalRenderInfo->glRenderer = (GLubyte*)glGetString(GL_RENDERER);    //返回一个渲染器标识符，通常是个硬件平台
    globalRenderInfo->glVersion = (GLubyte*)glGetString(GL_VERSION);    //返回当前OpenGL实现的版本号
    globalRenderInfo->glslVersion = (GLubyte*)glGetString(GL_SHADING_LANGUAGE_VERSION);//返回着色预压编译器版本号

    globalRenderInfo->viewLoc = shader->GetUniformLocation("view");
    globalRenderInfo->projectionLoc = shader->GetUniformLocation("projection");
    globalRenderInfo->modelLoc = shader->GetUniformLocation("model");
    globalRenderInfo->ourTextrueLoc = shader->GetUniformLocation("ourTexture");
    globalRenderInfo->useColorLoc = shader->GetUniformLocation("useColor");
    globalRenderInfo->ourColorLoc = shader->GetUniformLocation("ourColor");
    globalRenderInfo->texOffest = shader->GetUniformLocation("texOffest");
    globalRenderInfo->texTilling = shader->GetUniformLocation("texTilling");

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
    if (staticTexPool.size() > 0) {
        std::vector<CCTexture*>::iterator it;
        for (it = staticTexPool.begin(); it != staticTexPool.end(); it++) {
            CCTexture* tex = *it;
            if (tex)
                delete tex;
        }
        staticTexPool.clear();
    }
    ReleaseTexPool();
}

void CCPanoramaRenderer::Render(float deltaTime)
{
    CCRenderGlobal::SetInstance(globalRenderInfo);
    CCTexture::UnUse();
    shader->Use();

    //摄像机矩阵
    Renderer->View->CalcMainCameraProjection(shader);

    //模型位置和矩阵映射
    model = mainModel->GetMatrix();
    glUniformMatrix4fv(globalRenderInfo->modelLoc, 1, GL_FALSE, glm::value_ptr(model));

    glPolygonMode(GL_FRONT, GL_FILL);
    glPolygonMode(GL_BACK, GL_LINE);

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

    //绘制测试
    if (renderPanoramaFullTest) {
        glPolygonMode(GL_FRONT, GL_LINE);
        glPolygonMode(GL_BACK, GL_LINE);
        glUniform1i(globalRenderInfo->useColorLoc, 1);
        glUniform3f(globalRenderInfo->ourColorLoc, wireframeColor2.r, wireframeColor2.g, wireframeColor2.b);
        glColor3f(wireframeColor2.r, wireframeColor2.g, wireframeColor2.b);
        RenderFullChunks(deltaTime);
    }
    if (renderPanoramaATest && testModel) 
        testModel->Render();

    //绘制调试线框
    if (renderDebugWireframe) {

        glPolygonMode(GL_FRONT, GL_LINE);
        glPolygonMode(GL_BACK, GL_LINE);
        glUniform1i(globalRenderInfo->useColorLoc, 1);
        glUniform3f(globalRenderInfo->ourColorLoc, wireframeColor.r, wireframeColor.g, wireframeColor.b);
        glColor3f(wireframeColor.r, wireframeColor.g, wireframeColor.b);

        if (renderPanoramaFlat)
            RenderFlat();
        else 
            RenderThumbnail();
    }
    //绘制向量标线
    if (renderDebugVector) {

        glBindTexture(GL_TEXTURE_2D, 0);
        glUniform1i(globalRenderInfo->useColorLoc, 1);

      
        glUniform3f(globalRenderInfo->ourColorLoc, 0.0f, 1.0f, 0.0f);
        glBegin(GL_LINES);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.05f, 0.0f);
        glEnd();

        glUniform3f(globalRenderInfo->ourColorLoc, 0.0f, 0.0f, 1.0f);

        glBegin(GL_LINES);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.05f);
        glEnd();

        glUniform3f(globalRenderInfo->ourColorLoc, 1.0f, 0.0f, 0.0f);

        glBegin(GL_LINES);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.05f, 0.0f, 0.0f);
        glEnd();
    }
}

void CCPanoramaRenderer::CreateMainModel() {

    mainModel = new CCModel();
    mainModel->Mesh = new CCMesh();
    mainModel->Material = new CCMaterial(panoramaCheckTex);
    mainModel->Material->tilling = glm::vec2(50.0f, 25.0f);

    CreateMainModelSphereMesh(mainModel->Mesh);

    mainFlatModel = new CCModel();
    mainFlatModel->Mesh = new CCMesh();
    mainFlatModel->Mesh->DrawType = GL_DYNAMIC_DRAW;
    mainFlatModel->Material = new CCMaterial(panoramaCheckTex);
    mainFlatModel->Material->tilling = glm::vec2(50.0f, 25.0f);

    CreateMainModelFlatMesh(mainFlatModel->Mesh);
}
void CCPanoramaRenderer::CreateMainModelFlatMesh(CCMesh* mesh) {
    mesh->normals.clear();
    mesh->positions.clear();
    mesh->texCoords.clear();
    mesh->indices.clear();

    float ustep = 1.0f / sphereSegmentX, vstep = 1.0f / sphereSegmentY;
    float u = 0, v = 0;

    for (int j = 0; j <= sphereSegmentY; j++, v += vstep) {
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++, u += ustep) {
            mesh->positions.push_back(glm::vec3(0.5f - u, (0.5f - v) / 2.0f, 0.0f));
            mesh->texCoords.push_back(glm::vec2(1.0f - u, v));
        }
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
void CCPanoramaRenderer::CreateMainModelSphereMesh(CCMesh* mesh) {

    float r = 1.0f;
    float ustep = 1.0f / sphereSegmentX, vstep = 1.0f / sphereSegmentY;
    float u = 0, v = 0;

    //顶点
    //=======================================================

    for (int j = 0; j <= sphereSegmentY; j++, v += vstep) {
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++, u += ustep) {
            mesh->positions.push_back(GetSpherePoint(u, v, r));
            mesh->texCoords.push_back(glm::vec2(1.0f - u, v));
        }
    }

    //顶点索引
    //=======================================================

    int all_vertices_count = mesh->positions.size();
    int vertices_line_count = sphereSegmentX + 1;
    int line_start_pos = vertices_line_count;

    for (int i = 0; i < sphereSegmentX; i++) {
        mesh->indices.push_back(CCFace(line_start_pos + i + 1, 0, -1));
        mesh->indices.push_back(CCFace(line_start_pos + i, 0, -1));
        mesh->indices.push_back(CCFace(i, 0, -1));
    }
    for (int j = 0; j < sphereSegmentY - 1; j++) {
        line_start_pos = (j + 1) * vertices_line_count;
        for (int i = 0; i < sphereSegmentX; i++) {

            mesh->indices.push_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count, 0, -1));

            mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count + 1, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i, 0, -1));
            mesh->indices.push_back(CCFace(line_start_pos + i + 1, 0, -1));
        }
    }

    line_start_pos = vertices_line_count * sphereSegmentY;
    for (int i = 0; i < sphereSegmentX; i++) {
        mesh->indices.push_back(CCFace(line_start_pos + i, 0, -1));
        mesh->indices.push_back(CCFace(line_start_pos + i + 1, 0, -1));
        mesh->indices.push_back(CCFace(line_start_pos + i + vertices_line_count, 0, -1));
    }

    //创建缓冲区
    mesh->GenerateBuffer();
}
glm::vec3 CCPanoramaRenderer::CreateFullModelSphereMesh(ChunkModel*info, int segXStart, int segYStart, int segX, int segY) {

    CCMesh* mesh = info->model->Mesh;

    float r = 0.99f;
    float ustep = 1.0f / sphereFullSegmentX, vstep = 1.0f / sphereFullSegmentY;
    float u = 0, v = 0, cu = 1.0f, cv = 0;

    int segXEnd = segXStart + segX;
    int segYEnd = segYStart + segY;

    float u_start =  segXStart * ustep, v_start = segYStart * vstep;
    float custep = 1.0f / (segXEnd - segXStart), cvstep = 1.0f / (segYEnd - segYStart);
    u = u_start;
    v = v_start;

    int skip = 0;

    for (int j = segYStart; j <= segYEnd; j++, v += vstep, cv += cvstep) {
        if (j <= 2 || j >= sphereFullSegmentY - 4) {
            skip++;
            continue;
        }
        u = u_start;
        cu = 1.0f;
        for (int i = segXStart; i <= segXEnd; i++, u += ustep, cu -= custep) {
            mesh->positions.push_back(GetSpherePoint(u, v, r));
            mesh->texCoords.push_back(glm::vec2(cu, cv));
        }
    }

    int vertices_line_count = segXEnd - segXStart + 1;
    for (int j = 0, c = segYEnd - segYStart - skip; j < c; j++) {
        int line_start_pos = (j) * vertices_line_count;
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

    float center_u = (u_start + ustep * ((segXEnd - segXStart) / 2.0f)),
        center_v = (v_start + vstep * ((segYEnd - segYStart) / 2.0f)),
        center_u_2 = (center_u - u_start) / 10.0f * 9.0f,
        center_v_2 = (center_v - v_start) / 10.0f * 9.0f;

    info->pointA = GetSpherePoint(center_u - center_u_2, center_v - center_v_2, r);
    info->pointB = GetSpherePoint(center_u + center_u_2, center_v - center_v_2, r);
    info->pointC = GetSpherePoint(center_u - center_u_2, center_v + center_v_2, r);
    info->pointD = GetSpherePoint(center_u + center_u_2, center_v + center_v_2, r);

    return GetSpherePoint(center_u, center_v, r);
}
void CCPanoramaRenderer::LoadBuiltInResources() {
    panoramaCheckTex = new CCTexture();
    if (!panoramaCheckTex->Load(CCFileManager::GetResourcePath(L"textures", L"checker.jpg").c_str())) {
        delete panoramaCheckTex;
        panoramaCheckTex = nullptr;
    }
    panoramaRedCheckTex = new CCTexture();
    panoramaRedCheckTex->Load(CCFileManager::GetResourcePath(L"textures", L"red_checker.jpg").c_str());

    uiLogoTex = new CCTexture();
    uiLogoTex->Load(CCFileManager::GetResourcePath(L"textures", L"logo.png").c_str());

    uiFailedTex = new CCTexture();
    uiFailedTex->Load(CCFileManager::GetResourcePath(L"textures", L"icon_image_error.jpg").c_str());

    uiOpenButtonTex = new CCTexture();
    uiOpenButtonTex->Load(CCFileManager::GetResourcePath(L"textures", L"open_file.jpg").c_str());

    uiTitleTex = new CCTexture();
    uiTitleTex->Load(CCFileManager::GetResourcePath(L"textures", L"title.png").c_str());

    testModel = new CCModel();
    testModel->Mesh = new CCMesh();

    CCMeshLoader::GetMeshLoaderByType(MeshTypeObj)->Load(
        CCFileManager::GetResourcePath(L"prefabs", L"cube.obj").c_str(),
        testModel->Mesh);

    testModel->Material = new CCMaterial(panoramaRedCheckTex);
    testModel->Material->tilling = glm::vec2(50.0f);
}
void CCPanoramaRenderer::ReleaseTexPool() {
    renderPanoramaFull = false;
    if (panoramaTexPool.size() > 0) {
        std::vector<CCTexture*>::iterator it;
        for (it = panoramaTexPool.begin(); it != panoramaTexPool.end(); it++) {
            CCTexture* tex = *it;
            delete tex;
        }
        panoramaTexPool.clear();
    }
    panoramaThumbnailTex = nullptr;
}

//完整全景模型

void CCPanoramaRenderer::ReleaseFullModel()
{
    if (fullModels.size() > 0) {
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

    float chunkWf = 1.0f / chunkW, chunkHf = 1.0f / chunkH;
    for (int i = 0; i < chunkW; i++) {
        for (int j = 0; j < chunkH; j++) {
            ChunkModel* model = new ChunkModel();
            model->model = new CCModel();
            model->model->Visible = false;
            model->model->Mesh = new CCMesh();
            model->model->Material = new CCMaterial(panoramaRedCheckTex);
            model->model->Material->tilling = glm::vec2(50.0f);
            model->chunkX = chunkW - i - 1;
            model->chunkY =  j;
            model->chunkXv = (float)i / (float)chunkW;
            model->chunkYv = (float)j / (float)chunkH;
            model->chunkXv = model->chunkXv + chunkWf;
            model->chunkYv = model->chunkYv + chunkHf;
            model->pointCenter = CreateFullModelSphereMesh(model,
                i * segX, j * segY, 
                segX + ((i == chunkW - 1 && chunkW % 2 != 0) ? 1 : 0), segY);
            fullModels.push_back(model);
        }
    }
}

//模型控制

void CCPanoramaRenderer::ResetModel()
{
    mainModel->Reset();
    mainFlatModel->Material->offest.x = 0.0f;
}
void CCPanoramaRenderer::RotateModel(float xoffset, float yoffset)
{
    mainModel->Rotation.y += xoffset;
    mainModel->Rotation.z -= yoffset;
    mainModel->UpdateVectors();

    UpdateFullChunksVisible();
}
void CCPanoramaRenderer::RotateModelForce(float y, float z)
{
    mainModel->Rotation.y += y;
    mainModel->Rotation.z += z;
    mainModel->UpdateVectors();

    UpdateFullChunksVisible();
}
void CCPanoramaRenderer::MoveModel(float xoffset, float yoffset)
{
    if (renderPanoramaFlatXLoop) {
        float v = mainFlatModel->Material->offest.x + xoffset;
        if (v < 0) v += 1.0f;
        else if (v > 1.0f) v -= 1.0f;
        mainFlatModel->Material->offest.x = v;
    }
    else {
        mainModel->Positon.x -= xoffset * FlatModelMoveRato;
        if (mainModel->Positon.x < FlatModelMin.x) mainModel->Positon.x = FlatModelMin.x;
        if (mainModel->Positon.x > FlatModelMax.x) mainModel->Positon.x = FlatModelMax.x;
    }
    
    mainModel->Positon.y -= yoffset * FlatModelMoveRato;
    if (mainModel->Positon.y < FlatModelMin.y) mainModel->Positon.y = FlatModelMin.y;
    if (mainModel->Positon.y > FlatModelMax.y) mainModel->Positon.y = FlatModelMax.y;
}
void CCPanoramaRenderer::MoveModelForce(float x, float y)
{
    if (renderPanoramaFlatXLoop) {
        float v = mainFlatModel->Material->offest.x + x;
        if (v < 0) v += 1.0f;
        else if (v > 1.0f) v -= 1.0f;
        mainFlatModel->Material->offest.x = v;
    }
    else {
        mainModel->Positon.x -= x * FlatModelMoveRato;
        if (mainModel->Positon.x < FlatModelMin.x) mainModel->Positon.x = FlatModelMin.x;
        if (mainModel->Positon.x > FlatModelMax.x) mainModel->Positon.x = FlatModelMax.x;
    }

    mainModel->Positon.y += y * FlatModelMoveRato;
    if (mainModel->Positon.y < FlatModelMin.y) mainModel->Positon.y = FlatModelMin.y;
    if (mainModel->Positon.y > FlatModelMax.y) mainModel->Positon.y = FlatModelMax.y;
}
void CCPanoramaRenderer::UpdateMercatorControl() {
    //PrecalcMercator();

    CCMesh* mesh = mainFlatModel->Mesh;
    mesh->texCoords.clear();

    float ustep = 1.0f / sphereSegmentX, vstep = 1.0f / sphereSegmentY;
    float u = 0, v = 0;

    for (int j = 0; j <= sphereSegmentY; j++, v += vstep) {
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++, u += ustep) 
            mesh->texCoords.push_back(GetMercatorUVPoint(1.0f - u, v));
    }

    mesh->ReBufferData();
}
void CCPanoramaRenderer::ResetMercatorControl() {
    CCMesh* mesh = mainFlatModel->Mesh;
    mesh->texCoords.clear();

    float ustep = 1.0f / sphereSegmentX, vstep = 1.0f / sphereSegmentY;
    float u = 0, v = 0;

    for (int j = 0; j <= sphereSegmentY; j++, v += vstep) {
        u = 0;
        for (int i = 0; i <= sphereSegmentX; i++, u += ustep)
            mesh->texCoords.push_back(glm::vec2(1.0 - u, v));
    }
    mesh->ReBufferData();
}

//渲染

void CCPanoramaRenderer::RenderThumbnail()
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
        for (int i = 0, c = fullModels.size(); i < c; i++) {
            ChunkModel* m = fullModels[i];
            if (m->model->Visible)  //渲染区块
                m->model->Render();
        }
    }
}
void CCPanoramaRenderer::RenderFlat() {
    mainFlatModel->Render();
}

//更新

void CCPanoramaRenderer::UpdateMainModelTex()
{
    if (panoramaThumbnailTex) {
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
        for (auto it = fullModels.begin(); it != fullModels.end(); it++) {
            ChunkModel* m = *it;
            if (fov > 50)
                m->model->Visible = IsInView(m->pointCenter);
            else
                m->model->Visible = IsInView(m->pointA) || IsInView(m->pointB) || IsInView(m->pointC) || IsInView(m->pointD);
            if (m->model->Visible) {
                if (!m->loadMarked && !renderPanoramaFullTest) {//加载贴图
                    m->loadMarked = true;

                    logger->Log(L"Star load chunk %d, %d", m->chunkX, m->chunkY);

                    CCTexture* tex = new CCTexture();
                    tex->wrapS = GL_MIRRORED_REPEAT;
                    tex->wrapT = GL_MIRRORED_REPEAT;
                    m->model->Material->diffuse = tex;
                    m->model->Material->tilling = glm::vec2(1.0f, 1.0f);
                    Renderer->AddTextureToQueue(tex, m->chunkX, m->chunkY, m->chunkY * m->chunkX + m->chunkX);//MainTex
                    panoramaTexPool.push_back(tex);
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

bool CCPanoramaRenderer::IsInView(glm::vec3 worldPos)
{
    CCamera* cam = Renderer->View->Camera;
    glm::vec3 viewPos = cam->World2Screen(worldPos, model);

    //glm::vec3 dir = glm::normalize(worldPos - cam->Position);
    //float dot = glm::dot(cam->Front, dir);     //判断物体是否在相机前面  

    if (/*dot > 0 && */viewPos.x >= 0 && viewPos.x <= Renderer->View->Width && viewPos.y >= 0 && viewPos.y <= Renderer->View->Height)
        return true;
    else
        return false;
}
glm::vec2 CCPanoramaRenderer::GetSphereUVPoint(float u, float v, short i) {
    return glm::vec2(u, v);
}
glm::vec3 CCPanoramaRenderer::GetSpherePoint(float u, float v, float r)
{
    constexpr float PI = glm::pi<float>();
    float x = r * glm::sin(PI * v) * glm::sin(PI * u * 2);
    float y = r * glm::cos(PI * v);
    float z = r * glm::sin(PI * v) * glm::cos(PI * u * 2);
    return glm::vec3(x, y, z);
}
glm::vec2 CCPanoramaRenderer::GetMercatorUVPoint(float u, float v)
{
    constexpr float PI = glm::pi<float>();

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
    constexpr float PI = glm::pi<float>();

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
