#pragma once
#include "stdafx.h"
#include "COpenGLRenderer.h"
#include "CCamera.h"
#include "CCPanoramaCamera.h"
#include "CCPanoramaRenderer.h"
#include "CCTextureLoadQueue.h"
#include "CCModel.h"
#include "CCFileManager.h"
#include "CCErrors.h"
#include "../player/CCVideoPlayer.h"
#include "../player/CCOpenGLTexVideoDevice.h"
#include "CMobileGameUIEventDistributor.h"
#include <vector>

//全景模式
enum PanoramaMode : int16_t {
	PanoramaSphere = 0,
	PanoramaCylinder,
	PanoramaAsteroid,
	PanoramaOuterBall,
	PanoramaMercator,
	PanoramaFull360,
	PanoramaFullOriginal,
};

#define ORIENTATION_ROTATE_180 3
#define ORIENTATION_ROTATE_90 6
#define ORIENTATION_ROTATE_270 8
#define ORIENTATION_NORMAL 1

/**
 * 全景游戏基础渲染器
 */
class CMobileGameUIEventDistributor;
class CImageLoader;
class CMobileGameRenderer : public COpenGLRenderer
{
public:
	CMobileGameRenderer();
	~CMobileGameRenderer();

	static void GlobalInit(JNIEnv *env, jobject context);

	void DoOpenFile();
	void MarkShouldOpenFile();
	void MarkCloseFile();
	void MarkDestroy() override { shouldDestroy = true; }

	void UpdateGyroValue(float x, float y, float z, float w) const;
	void UpdateDebugValue(float x, float y, float z, float w, float u, float v);

	//属性方法

	void SetIntProp(int id, int value);
	int GetIntProp(int id);
	void SetBoolProp(int id, bool value);
	bool GetBoolProp(int id);
	void SetProp(int id, const char* string);
	const char* GetProp(int id);

	//视频播放器方法

	void SetVideoState(CCVideoState newState);
	void SetVideoPos(int64_t pos);
	CCVideoState GetVideoState();
	int64_t GetVideoLength();
	int64_t GetVideoPos();

	void SetUiEventDistributor( CMobileGameUIEventDistributor*uv) { uiEventDistributor = uv; }
    void SendUiEvent(CCMobileGameUIEvent event);

	//鼠标移动速度与粘性计算

	float MouseSensitivityMin = 0.01f;
	float MouseSensitivityMax = 0.06f;
	float RotateSpeed = 20.0f;
	float MoveSpeed = 0.3f;
	const float MoveInGyroSensitivity = 1.0f;
	float MouseInFlatSensitivityMin = 0.01f;
	float MouseInFlatSensitivityMax = 0.06f;

	float GetMouseSensitivity();
	float GetMouseSensitivityInFlat();
	void SetMouseDragVelocity(float x, float y);

	CCPanoramaCamera * GetCamera() const { return camera; }
	float GetGyroRotateCorrectionValue() const;

	void AddTextureToQueue(CCTexture* tex, int x, int y, int id) { texLoadQueue->Push(tex, x, y, id); }

	CCamera* GetMercatorCylinderCaptureCamera() { return cameraMercatorCylinderCapture; }

private:

	Logger* logger;

	bool ReInit() override;
	bool Init() override;
	void Render(float FrameTime) override;
	void Update() override;
	void RenderUI() override;
	void Resize(int Width, int Height) override;
	void Destroy() override;

	//全景模式
	PanoramaMode mode = PanoramaMode::PanoramaSphere;
	CCPanoramaCamera* camera = nullptr;
	CCamera* cameraMercatorCylinderCapture = nullptr;
	CCPanoramaRenderer* renderer = nullptr;
	CCFileManager* fileManager = nullptr;
	CCTextureLoadQueue* texLoadQueue = nullptr;

	void SwitchMode(PanoramaMode mode);

	bool gyroEnabled = false;
    bool vREnabled = false;
	bool fullChunkLoadEnabled = false;
	int gyroRotateCorrection = ORIENTATION_NORMAL;

	void FinishLoadAndNotifyError();

    CMobileGameUIEventDistributor*uiEventDistributor = nullptr;

	bool fileOpened = false;
	bool fileIsLoading = false;
    bool enableViewCache = true;

	std::string currentOpenFilePath;
	std::string currentFileCachePath;
	std::string currentFileSmallThumbnailCachePath;

    std::string viewCachePath;

	int lastError = VR_ERR_SUCCESS;

	bool renderInitFinish = false;
	bool shouldOpenFile = false;
	bool shouldCloseFile = false;
	bool shouldDestroy = false;

	bool destroying = false;
	bool needTestImageAndSplit = false;
	float lastX = 0, lastY = 0, xoffset = 0, yoffset = 0;

	bool GetNeedInterruptLoading() const;
	bool forceInterruptLoading = false;
	int forceInterruptLoadingTick = 0;

	void TestSplitImageAndLoadTexture();
	void TestToLoadTextureImageCache();

    TextureLoadQueueDataResult* LoadMainTexCallback(TextureLoadQueueInfo* info, CCTexture* texture);
	TextureLoadQueueDataResult* LoadChunkTexCallback(TextureLoadQueueInfo* info, CCTexture* texture);
	static TextureLoadQueueDataResult* LoadTexCallback(TextureLoadQueueInfo* info, CCTexture* texture, void* data);
	static void FileCloseCallback(void* data);
	static void CameraFOVChanged(void* data, float fov);
	static void CameraOrthoSizeChanged(void* data, float fov);
	static void VideoPlayerEventCallBack(CCVideoPlayer* player, int message, void* customData);

	bool shouldSplitFullImage = true;
	bool currentFileIsVideo = false;
	bool thisFileShouldSaveCache = false;
    bool thisFileShouldLoadInCache = false;

	void ReBufferAllData();

	static void MouseCallback(COpenGLView* view, float x, float y, int button, int type);
	static void ScrollCallback(COpenGLView* view, float x, float y, int button, int type);
	void KeyMoveCallback(CCameraMovement move);

	glm::vec2 DragCurrentVelocity = glm::vec2(0);
	glm::vec2 VelocityDragLastOffset = glm::vec2(0);
	bool VelocityDragCurrentIsInSim = false;
	float VelocityDragCutSensitivity = 1.0f;

    void DoOpenAsImage();
	void DoOpenAsVideo();

	CCVideoPlayerInitParams playerInitParams;
	CCPlayerRender* playerRender = nullptr;
	CCVideoPlayer* player = nullptr;

private:
	void SetVRViewPort(int index);
	void SetGyroEnabled(bool enable);
	void SetVREnabled(bool enable);

    void TryLoadSmallThumbnail();
};
