#pragma once
#include "stdafx.h"
#include "CCTexture.h"
#include "CCModel.h"
#include "CCShader.h"
#include "CColor.h"
#include <vector>
#include "CCSmartPtr.hpp"

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
class CMobileGameRenderer;
class CCPanoramaRenderer
{
public:
    CCPanoramaRenderer(CMobileGameRenderer* renderer);
private:
    CMobileGameRenderer* Renderer = nullptr;

public:
    void Init();
    void ReInit();
    void Destroy();
    void Render(float deltaTime);

    Logger* logger = nullptr;

    std::string vshaderCode;
    std::string fshaderCode;

    CCRenderGlobal* globalRenderInfo = nullptr;
    CCShader* shader = nullptr;
    CCModel* mainModel = nullptr;
    CCModel* mainFlatModel = nullptr;
    float mainModelYRotationBase = 0;
    bool gyroEnabled = false;

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

    bool renderNoPanoramaSmall = false;
    bool renderPanoramaFull = false;
    bool renderPanoramaFlat = false;


    CCSmartPtr<CCTexture> uiFailedTex = nullptr;
    CCSmartPtr<CCTexture> uiLogoTex = nullptr;
    CCSmartPtr<CCTexture> uiOpenButtonTex = nullptr;
    CCSmartPtr<CCTexture> uiTitleTex = nullptr;
    CCSmartPtr<CCTexture> panoramaRedCheckTex = nullptr;
    CCSmartPtr<CCTexture> panoramaCheckTex = nullptr;
    CCSmartPtr<CCTexture> panoramaThumbnailTex = nullptr;

    std::vector< CCSmartPtr<CCTexture>> panoramaTexPool;

    void SetCurrentFrameVRValue(int w, int h);

    void ReleaseTexPool();
    void ReleaseFullModel();
    void GenerateFullModel(int chunkW, int chunkH);
    void ReBufferAllData();

    void ResetModel();
    void RotateModel(float xoffset, float yoffset);
    void RotateModelForce(float y, float z);
    void GyroscopeRotateModel(float x, float y, float z, float w);
    void MoveModel(float xoffset, float yoffset) const;
    void MoveModelForce(float x, float y) const;
    void UpdateMercatorControl();
    void ResetMercatorControl() const;

    glm::vec2 FlatModelMax = glm::vec2(0.0f);
    glm::vec2 FlatModelMin = glm::vec2(0.0f);
    float FlatModelMoveRato = 1.0f;

    glm::vec2 MercatorControlPoint0 = glm::vec2(0.0f);
    glm::vec2 MercatorControlPoint1 = glm::vec2(0.0f);
    glm::vec2 MercatorControlPoint2 = glm::vec2(0.0f);

    void UpdateMainModelTex() const;
    void UpdateFullChunksVisible();
    void UpdateFlatModelMinMax(float orthoSize);

private:

    void CreateMainModel();
    void CreateMainModelFlatMesh(CCMesh* mesh) const;
    glm::vec3  CreateFullModelSphereMesh(ChunkModel* info, int segXStart, int segYStart, int segXEnd, int segYEnd) const;
    void CreateMainModelSphereMesh(CCMesh* mesh) const;

    void LoadBuiltInResources();
    void ReleaseBuiltInResources();
    void InitShader();

    void RenderThumbnail() const;
    void RenderFullChunks(float deltaTime);
    void RenderFlat() const;

    bool IsInView(glm::vec3 worldPos);

    //获取球面上的UV坐标
    static glm::vec2 GetSphereUVPoint(float u, float v, short i);
    //获取球面上的点
    static glm::vec3 GetSpherePoint(float u, float v, float r);
    glm::vec2 GetMercatorUVPoint(float u, float v) const;
    void PrecalcMercator();

    float Mercator_фp = 0;
    float Mercator_λp = 0;

    bool currentFrameVr = false;
    int currentFrameVrW = 0;
    int currentFrameVrH = 0;
};


