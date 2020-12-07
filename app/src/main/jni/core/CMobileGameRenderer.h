#pragma once
#include "stdafx.h"
#include "COpenGLRenderer.h"
#include "CCamera.h"
#include "CCPanoramaCamera.h"
#include "CCPanoramaRenderer.h"
#include "CCTextureLoadQueue.h"
#include "CCModel.h"
#include "CCFileManager.h"
#include "CCGUInfo.h"

#include <vector>

//全景模式
enum PanoramaMode : int16_t {
	PanoramaSphere = 0,
	PanoramaCylinder,
	PanoramaAsteroid,
	PanoramaOuterBall,
	PanoramaMercator,
	PanoramaFull360,
	PanoramaFullOrginal,
	PanoramaModeMax,
};

class CMobileGameUIEventDistributor;
class CImageLoader;
class CMobileGameRenderer : public COpenGLRenderer
{
public:
	CMobileGameRenderer();
	~CMobileGameRenderer();

	void SetOpenFilePath(const char* path);
	void DoOpenFile();
	void MarkShouldOpenFile() { should_open_file = true; }
	void MarkCloseFile();
	void MarkDestroy() override { should_destroy = true; }
	void SwitchMode(PanoramaMode mode);
	void SetGryoEnabled(bool enable);
	void SetEnableFullChunkLoad(bool enable);
	void SetVREnabled(bool enable);
	void UpdateGryoValue(float x, float y, float z) const;
    void AddTextureToQueue(CCTexture* tex, int x, int y, int id);

	PanoramaMode GetMode() { return mode; }
	const char* GetImageOpenError() { return last_image_error.c_str(); }
    CCGUInfo* GetGUInfo() { return uiInfo; }
	void SetUiEventDistributor( CMobileGameUIEventDistributor*uv) { uiEventDistributor = uv; }
private:

	Logger* logger;

	std::string currentOpenFilePath;

	bool ReInit() override;
	bool Init() override;
	void Render(float FrameTime) override;
	void Update() override;
	void RenderUI() override;
	void Resize(int Width, int Height) override;
	void Destroy() override;

	//全景模式
	PanoramaMode mode = PanoramaMode::PanoramaSphere;
	CCPanoramaCamera*camera = nullptr;
	CCPanoramaRenderer* renderer = nullptr;
	CCFileManager*fileManager = nullptr;
	CCTextureLoadQueue*texLoadQueue = nullptr;

	bool gryoEnabled = false;
    bool vREnabled = false;
	bool fullChunkLoadEnabled = false;

	void ShowErrorDialog();

    CMobileGameUIEventDistributor*uiEventDistributor = nullptr;
    CCGUInfo* uiInfo = nullptr;
	bool file_opened = false;

	std::string last_image_error;

	bool render_init_finish = false;
	bool should_open_file = false;
	bool should_close_file = false;
	bool should_destroy = false;
	bool destroying = false;
	bool needTestImageAndSplit = false;
	float lastX = 0, lastY = 0, xoffset = 0, yoffset = 0;

	void TestSplitImageAndLoadTexture();

	TextureLoadQueueDataResult* LoadChunkTexCallback(TextureLoadQueueInfo* info, CCTexture* texture);
	static TextureLoadQueueDataResult* LoadTexCallback(TextureLoadQueueInfo* info, CCTexture* texture, void* data);
	static void FileCloseCallback(void* data);
	static void CameraFOVChanged(void* data, float fov);
	static void CameraOrthoSizeChanged(void* data, float fov);

	float MouseSensitivity = 0.1f;
	float RotateSpeed = 20.0f;
	float MoveSpeed = 0.3f;

	bool SplitFullImage = true;

	void ReBufferAllData();

	static void MouseCallback(COpenGLView* view, float x, float y, int button, int type);
	static void ScrollCallback(COpenGLView* view, float x, float y, int button, int type);
	void KeyMoveCallback(CCameraMovement move);
};

