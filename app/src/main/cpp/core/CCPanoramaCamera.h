#pragma once
#include "stdafx.h"
#include "CCamera.h"

//摄像机全景模式
enum class CCPanoramaCameraMode {
	CenterRoate,
	OutRoataround,
	Static,
	OrthoZoom,
};
//摄像机移动选项
enum class CCameraMovement {
	FORWARD,
	BACKWARD,
	LEFT,
	RIGHT,
	ROATE_UP,
	ROATE_DOWN,
	ROATE_LEFT,
	ROATE_RIGHT = 7
};

class CCPanoramaCamera;

typedef void(*CCPanoramaCameraCallback)(void* data, CCPanoramaCamera* cam);

//全景摄像机
class CCPanoramaCamera : public CCamera
{
public:
	//摄像机全景模式
	CCPanoramaCameraMode Mode = CCPanoramaCameraMode::Static;

	// 处理从任何类似键盘的输入系统接收的输入，以摄像机定义的ENUM形式接受输入参数（从窗口系统中抽象出来）
	void ProcessKeyboard(CCameraMovement direction, float deltaTime);
	// 处理从鼠标输入系统接收的输入，预测x和y方向的偏移值
	void ProcessMouseMovement(float xoffset, float yoffset, bool constrainPitch);
	// 处理从鼠标滚轮事件接收的输入
	void ProcessMouseScroll(float yoffset);
	//缩放事件
	void ProcessZoomChange(float precent);
	//设置模式
	void SetMode(CCPanoramaCameraMode mode);
	//设置旋转回调
	void SetRotateCallback(CCPanoramaCameraCallback callback, void* data);
	//获取缩放百分比
	float GetZoomPercentage();

	float RoateNearMax = 0.2f;
	float RoateFarMax = 3.5f;
	float ZoomSpeed = 0.05f;
	float FovMax = 170.0f;
	float FovMin = 2.0f;
	float RoateYForWorld = 0.0f;
	float RoateXForWorld = 0.0f;
	float MovementSpeed = DEF_SPEED;
	float RoateSpeed = DEF_ROATE_SPEED;
	float MouseSensitivity = DEF_SENSITIVITY;
	float OrthoSizeMin = 0.05f;
	float OrthoSizeMax = 1.0f;
	float OrthoSizeZoomSpeed = 0.001f;

private:
	CCPanoramaCameraCallback rotateCallback = nullptr;
	void* rotateCallbackData = nullptr;

	void CallRotateCallback();
};

