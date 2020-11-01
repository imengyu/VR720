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
};

class CCamera;
class CCShader;
class CCRenderGlobal;
//OpenGL 视图抽象类
class COpenGLView
{
public:
	COpenGLView(COpenGLRenderer* renderer);

	COpenGLView();

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
	 * 释放
	 */
	virtual void Destroy() {}

#if defined(VR720_ANDROID)

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

#elif defined(VR720_WINDOWS) || defined(VR720_LINUX)

	//获取或设置窗口是否全屏
	bool IsFullScreen = false;

	/**
	 * 更新窗口是否全屏的状态
	 */
	virtual void UpdateFullScreenState() {}
	/**
	 * 设置窗口是否全屏
	 * @param full 是否全屏
	 */
	virtual void SetFullScreen(bool full) {}
	/**
	 * 获取窗口是否全屏
	 * @return
	 */
	virtual bool GetIsFullScreen() { return IsFullScreen; }

	/**
	 * 显示窗口
	 * @param Maximized 是否最大化
	 */
	virtual void Show(bool Maximized = false) {}
	//激活窗口
	virtual void Active() {}
	//运行窗口消息循环（仅Windows）
	virtual void MessageLoop() {}
	/**
	 * 调整窗口大小
	 * @param w 宽度
	 * @param h 高度
	 * @param moveToCenter 是否移动到屏幕中央
	 */
	virtual void Resize(int w, int h, bool moveToCenter);

	//关闭窗口
	virtual void CloseView() {}
	//等待渲染线程退出
	virtual void WaitDestroyComplete() {}

	//设置为低FPS模式
	virtual void SetToLowerFpsMode() {}
	//退出为低FPS模式
	virtual void QuitLowerFpsMode() {}

	//开始窗口鼠标捕捉
	virtual void MouseCapture() {}
	//释放窗口鼠标捕捉
	virtual void ReleaseCapture() {}

	/**
	 * 设置当前窗口的文字
	 * @param text 文字
	 */
	virtual void SetViewText(const vchar* text) {}

	/**
	 * 设置当窗口关闭前回调
	 */
	void SetBeforeQuitCallback(BeforeQuitCallback beforeQuitCallback);
	/**
	 * 设置鼠标事件回调
	 */
	void SetMouseCallback(ViewMouseCallback mouseCallback);
	/**
	 * 设置鼠标滚动事件回调
	 */
	void SetScrollCallback(ViewMouseCallback mouseCallback);

#endif

#if defined(VR720_WINDOWS)
	/**
	 * 发送Windows消息至窗口
	 */
	virtual LRESULT SendWindowsMessage(UINT Msg, WPARAM wParam, LPARAM lParam) { return 0; }
#endif

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
	void CalcCameraProjection(CCamera* camera, CCShader* shader) const;

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

protected:

	int DownedKeys[MAX_KEY_LIST];
	int UpedKeys[MAX_KEY_LIST];

	COpenGLRenderer* OpenGLRenderer = NULL;

	ViewMouseCallback scrollCallback = nullptr;
	ViewMouseCallback mouseCallback = nullptr;
	BeforeQuitCallback beforeQuitCallback = nullptr;

	int AddKeyInKeyList(int* list, int code);
	int IsKeyInKeyListExists(int* list, int code);
	void HandleDownKey(int code);
	void HandleUpKey(int code);

};

