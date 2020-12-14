#pragma once
#include "COpenGLRenderer.h"

typedef void(*ViewMouseCallback)(COpenGLView* view, float xpos, float ypos, int button, int type);
typedef void(*BeforeQuitCallback)(COpenGLView* view);

const int MAX_KEY_LIST = 8;

enum ViewMouseEventType {
	ViewMouseMouseDown,
	ViewMouseMouseUp,
	ViewMouseMouseMove,
	ViewMouseMouseWhell,
	ViewZoomEvent,
};

class CCamera;
class CCShader;
class CCRenderGlobal;
//OpenGL 视图抽象类
class COpenGLView
{
public:
	COpenGLView(COpenGLRenderer* renderer);
	COpenGLView() = default;

	virtual ~COpenGLView();

	//视图高度
	int Width = 800;
	//视图宽度
	int Height = 600;

	/**
	 * 初始化
	 * @return 返回是否成功
	 */
	virtual bool Init() { return false; }
	/**
	 * 暂停
	 */
	virtual void Pause() {}
	/**
	 * 继续
	 */
	virtual void Resume() {}

	/**
	 * 释放
	 */
	virtual void Destroy() {}
	/**
	 * 手动更新视图大小
	 * @param w 宽度
	 * @param h 高度
	 */
	virtual void Resize(int w, int h);
	/**
	 * 设置鼠标事件回调
	 */
	void SetMouseCallback(ViewMouseCallback mouseCallback);
	/**
	 * 设置用户缩放手势事件回调
	 */
	void SetZoomViewCallback(ViewMouseCallback mouseCallback);


	//摄像机
	//**********************

	/**
	 * 计算当前主摄像机的矩阵映射
	 * @param shader 使用的程序
	 */
	void CalcMainCameraProjection(CCShader* shader) const;
	/**
	 * 计算无摄像机时的矩阵映射
	 * @param shader 使用的程序
	 */
	void CalcNoMainCameraProjection(CCShader* shader) const;
	/**
	 * 计算当前主摄像机的矩阵映射
	 * @param shader 使用的程序
	 */
	void CalcCameraProjection(CCamera* camera, CCShader* shader, int w, int h) const;
	/**
	 * 计算当前主摄像机的矩阵映射（自定义大小）
	 * @param shader 使用的程序
	 * @param w 屏幕宽度
	 * @param h 屏幕高度
	 */
	void CalcMainCameraProjectionWithWH(CCShader *shader, int w, int h) const;

	//当前主摄像机
	CCamera* Camera = nullptr;

	/**
	 * 设置当前主摄像机
	 * @param camera 摄像机
	 */
	virtual void SetCamera(CCamera* camera);

	//时间
	//**********************

	/**
	 * 获取当前程序绘制总时间
	 * @return
	 */
	virtual float GetTime() { return 0; }
	/**
	 * 获取当前FPS
	 * @return
	 */
	virtual float GetCurrentFps() { return 0; }
	/**
	 * 获取绘制时间
	 * @return
	 */
	virtual float GetDrawTime() { return 0; }
	/**
	 * 获取增量时间
	 * @return
	 */
	virtual float GetDeltaTime() { return 0; }

	//按键
	//**********************

	/**
	 * 获取是否有键按下
	 * @param code 按键键值
	 * @return
	 */
	virtual bool GetKeyPress(int code);
	/**
	 * 获取是否有键正在按下
	 * @param code 按键键值
	 * @return
	 */
	virtual bool GetKeyDown(int code);
	/**
	 * 获取是否有键放开
	 * @param code 按键键值
	 * @return
	 */
	virtual bool GetKeyUp(int code);

	/**
	 * 获取当前渲染器
	 * @return
	 */
	COpenGLRenderer* GetRenderer();

	void SetManualDestroyCamera(bool manual);
protected:

	int DownedKeys[MAX_KEY_LIST] = { 0 };
	int UpedKeys[MAX_KEY_LIST]= { 0 };
	bool IsManualDestroyCamera = false;

	COpenGLRenderer* OpenGLRenderer = NULL;

	ViewMouseCallback scrollCallback = nullptr;
	ViewMouseCallback mouseCallback = nullptr;
	BeforeQuitCallback beforeQuitCallback = nullptr;

	int AddKeyInKeyList(int* list, int code);
	int IsKeyInKeyListExists(int* list, int code);
	void HandleDownKey(int code);
	void HandleUpKey(int code);

};

