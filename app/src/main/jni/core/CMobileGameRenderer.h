#pragma once
#include "stdafx.h"
#include "COpenGLRenderer.h"
#include "CCamera.h"
#include "CCPanoramaCamera.h"
#include "CCPanoramaRenderer.h"
#include "CCTextureLoadQueue.h"
#include "CCModel.h"
#include "CCFileManager.h"

#include <vector>

//全景模式
enum PanoramaMode : int16_t {
	PanoramaSphere,
	PanoramaCylinder,
	PanoramaAsteroid,
	PanoramaOuterBall,
	PanoramaMercator,
	PanoramaFull360,
	PanoramaFullOrginal,
	PanoramaModeMax,
};

class CImageLoader;
class CMobileGameRenderer : public COpenGLRenderer
{
public:
	CMobileGameRenderer();
	~CMobileGameRenderer();

	void SetOpenFilePath(const wchar_t* path);
	void DoOpenFile();
	void MarkShouldOpenFile() { should_open_file = true; }
	void MarkCloseFile(bool delete_after_close) {
		should_close_file = true; 
		this->delete_after_close = delete_after_close;
	}
	void AddTextureToQueue(CCTexture* tex, int x, int y, int id);

private:

	Logger* logger;

	std::wstring currentOpenFilePath;
	bool fileOpened = false;

	bool Init() override;
	void Render(float FrameTime) override;
	void Update() override;
	void RenderUI() override;
	void Resize(int Width, int Height) override;
	void Destroy() override;

	char* GetPanoramaModeStr(PanoramaMode mode);

	//全景模式
	PanoramaMode mode = PanoramaMode::PanoramaSphere;
	CCPanoramaCamera*camera = nullptr;
	CCPanoramaRenderer* renderer = nullptr;
	CCFileManager*fileManager = nullptr;
	CCTextureLoadQueue*texLoadQueue = nullptr;

	void ShowErrorDialog();
	
	bool reg_dialog_showed = false;
	bool file_opened = false;

	std::string last_image_error;

	float current_fps = 0;
	DWORD current_draw_time = 0;

	bool render_init_finish = false;
	bool should_open_file = false;
	bool should_close_file = false;
	bool delete_after_close = false;
	bool destroying = false;
	bool needTestImageAndSplit = false;
	bool firstMouse = true;
	float lastX = 0, lastY = 0, xoffset = 0, yoffset = 0;
	float loopCount = 0;

	void LoadImageInfo();
	void TestSplitImageAndLoadTexture();

	TextureLoadQueueDataResult* LoadChunkTexCallback(TextureLoadQueueInfo* info, CCTexture* texture);
	static TextureLoadQueueDataResult* LoadTexCallback(TextureLoadQueueInfo* info, CCTexture* texture, void* data);
	static void FileCloseCallback(void* data);
	static void CameraFOVChanged(void* data, float fov);
	static void CameraOrthoSizeChanged(void* data, float fov);
	static void CameraRotate(void* data, CCPanoramaCamera* cam);
	static void BeforeQuitCallback(COpenGLView* view);

	void SwitchMode(PanoramaMode mode);
	void UpdateConsoleState();

	void LoadAndChechRegister();

	float MouseSensitivity = 0.1f;
	float RotateSpeed = 20.0f;
	float MoveSpeed = 0.3f;

	bool SplitFullImage = true;

	static void MouseCallback(COpenGLView* view, float x, float y, int button, int type);
	static void ScrollCallback(COpenGLView* view, float x, float y, int button, int type);

	void LoadSettings();
	void SaveSettings();

};

