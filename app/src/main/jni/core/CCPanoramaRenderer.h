#pragma once
#include "stdafx.h"
#include "CCTexture.h"
#include "CCModel.h"
#include "CCShader.h"
#include "CColor.h"
#include <vector>

struct ChunkModel {
    CCModel* model;
    int chunkX;
    int chunkY;
    float chunkXv;
    float chunkYv;
    float chunkXvE;
    float chunkYvE;
    glm::vec3 pointCenter;
    glm::vec3 pointA;
    glm::vec3 pointB;
    glm::vec3 pointC;
    glm::vec3 pointD;
    bool loadMarked = false;
};

class CCRenderGlobal;
#if defined(VR720_ANDROID)
class CMobileGameRenderer;
#elif defined(VR720_WINDOWS)
class CWindowsGameRenderer;
#endif
class CCPanoramaRenderer
{
public:
#if defined(VR720_ANDROID)
    CCPanoramaRenderer(CMobileGameRenderer* renderer);

private:

    CMobileGameRenderer* Renderer = nullptr;
#elif defined(VR720_WINDOWS)
    CCPanoramaRenderer(CWindowsGameRenderer* renderer);

private:

	CWindowsGameRenderer* Renderer = nullptr;
#endif

public:
    void Init();
    void Destroy();
    void Render(float deltaTime);

    CColor wireframeColor = CColor::FromString("#DC143C");
    CColor wireframeColor2 = CColor::FromString("#0000FF");

    Logger* logger = nullptr;

    CCRenderGlobal* globalRenderInfo = nullptr;
    CCShader* shader = nullptr;
    CCModel* mainModel = nullptr;
    CCModel* mainFlatModel = nullptr;
    std::vector<ChunkModel*> fullModels;

    glm::mat4 model = glm::mat4(1.0f);

    int sphereSegmentY = 64;
    int sphereSegmentX = 128;
    //int sphereSegmentY = 30;
    //int sphereSegmentX = 15;
    int sphereFullSegmentY = 0;
    int sphereFullSegmentX = 0;

    float panoramaFullViewWidth = 0.2f;
    float panoramaFullViewHeight = 0.1f;
    int panoramaFullSplitW = 0;
    int panoramaFullSplitH = 0;

    int renderPanoramaFullTestIndex = 0;
    float renderPanoramaFullTestTime = 0;
    bool renderPanoramaFullTestAutoLoop = true;

    bool renderDebugWireframe = false;
    bool renderDebugVector = false;

    bool renderPanoramaFlatXLoop = false;

    bool renderPanoramaFullTest = false;
    bool renderPanoramaFullRollTest = false;
    bool renderPanoramaATest = false;

    bool renderOn = false;

    bool renderNoPanoramaSmall = true;
    bool renderPanoramaFull = false;
    bool renderPanoramaFlat = false;


    CCSmartPtr<CCTexture> uiFailedTex = nullptr;
    CCSmartPtr<CCTexture> uiLogoTex = nullptr;
    CCSmartPtr<CCTexture> uiOpenButtonTex = nullptr;
    CCSmartPtr<CCTexture> uiTitleTex = nullptr;
    CCSmartPtr<CCTexture> panoramaRedCheckTex = nullptr;
    CCSmartPtr<CCTexture> panoramaCheckTex = nullptr;
    CCSmartPtr<CCTexture> panoramaThumbnailTex = nullptr;

    std::vector<CCTexture*> panoramaTexPool;

    void ReleaseTexPool();
    void ReleaseFullModel();
    void GenerateFullModel(int chunkW, int chunkH);

    void ResetModel();
    void RotateModel(float xoffset, float yoffset);
    void RotateModelForce(float y, float z);
    void MoveModel(float xoffset, float yoffset);
    void MoveModelForce(float x, float y);
    void UpdateMercatorControl();
    void ResetMercatorControl();


    glm::vec2 FlatModelMax = glm::vec2(0.0f);
    glm::vec2 FlatModelMin = glm::vec2(0.0f);
    float FlatModelMoveRato = 1.0f;

    glm::vec2 MercatorControlPoint0 = glm::vec2(0.0f);
    glm::vec2 MercatorControlPoint1 = glm::vec2(0.0f);
    glm::vec2 MercatorControlPoint2 = glm::vec2(0.0f);

    void UpdateMainModelTex();
    void UpdateFullChunksVisible();
    void UpdateFlatModelMinMax(float orthoSize);

private:


    void CreateMainModel();
    void CreateMainModelFlatMesh(CCMesh* mesh);
    glm::vec3  CreateFullModelSphereMesh(ChunkModel* info, int segXStart, int segYStart, int segXEnd, int segYEnd);
    void CreateMainModelSphereMesh(CCMesh* mesh);

    void LoadBuiltInResources();

    void RenderThumbnail();
    void RenderFullChunks(float deltaTime);
    void RenderFlat();

    bool IsInView(glm::vec3 worldPos);

    //获取球面上的UV坐标
    glm::vec2 GetSphereUVPoint(float u, float v, short i);
    //获取球面上的点
    glm::vec3 GetSpherePoint(float u, float v, float r);
    glm::vec2 GetMercatorUVPoint(float u, float v);
    void PrecalcMercator();

    float Mercator_фp;
    float Mercator_λp;
};


