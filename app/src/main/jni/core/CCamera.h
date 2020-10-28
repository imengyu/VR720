#pragma once
#include "stdafx.h"
#include <gtc/matrix_transform.hpp>
#include <vector>
#include "CColor.h"

// 初始化摄像机变量
const float DEF_YAW = -90.0f;
const float DEF_PITCH = 0.0f;
const float DEF_SPEED = 2.5f;
const float DEF_ROATE_SPEED = 20.0f;
const float DEF_SENSITIVITY = 0.1f;
const float DEF_FOV = 45.0f;

enum class CCameraProjection {
	Perspective,
	Orthographic
};

typedef void(*CCPanoramaCameraFovChangedCallback)(void* data, float fov);

class COpenGLView;
// 摄像机类，处理输入并计算相应的欧拉角，矢量和矩阵
class CCamera
{
public:
	// 摄像机位置
	glm::vec3 Position = glm::vec3(0.0f);
	// 旋转欧拉角
	glm::vec3 Rotate = glm::vec3(0.0f);
	//摄像机投影
	CCameraProjection Projection = CCameraProjection::Perspective;
	// 摄像机FOV
	float FiledOfView = DEF_FOV;
	//正交投影摄像机视图垂直方向的大小
	float OrthographicSize = 1.0f;
	//剪裁平面近端
	float ClippingNear = 0.1f;
	//剪裁平面远端
	float ClippingFar = 1000.0f;
	//摄像机背景颜色
	CColor Background = CColor::Black;

	glm::mat4 view = glm::mat4(1.0f);
	glm::mat4 projection = glm::mat4(1.0f);

	CCamera(glm::vec3 position = glm::vec3(0.0f, 0.0f, 0.0f), glm::vec3 up = glm::vec3(0.0f, 1.0f, 0.0f), glm::vec3 rotate = glm::vec3(0.0f, 0.0f, 0.0f));
	// 返回使用欧拉角和LookAt矩阵计算的view矩阵
	glm::mat4 GetViewMatrix();

	void SetOrthoSize(float o);

	void SetFOVChangedCallback(CCPanoramaCameraFovChangedCallback callback, void* data);
	void SetOrthoSizeChangedCallback(CCPanoramaCameraFovChangedCallback callback, void* data);

	void SetPosItion(glm::vec3 position);
	void SetRotation(glm::vec3 rotation);
	void SetFOV(float fov);

	void ForceUpdate();
	void Reset();

	glm::vec3 Front = glm::vec3(0.0f, 0.0f, -1.0f);
	glm::vec3 Up;
	glm::vec3 Right;
	glm::vec3 WorldUp;

	void SetView(COpenGLView* view);

	/**
	 * @brief  					窗口坐标转化为世界坐标
	 * @brief screenPoint		窗口坐标点
	 * @brief viewportRange 	视口范围。 各个值依次为：左上-右下
	 * @brief modelViewMatrix 	模型视图矩阵
	 * @brief projectMatrix 	投影矩阵
	 * @brief pPointDepth   	屏幕点的深度，如果不指定(为nullptr),从深度缓冲区中读取深度值
	 * @return 					世界坐标系
	 * @note 注意：得到的世界坐标系在使用前要除以齐次坐标值w，
	 *		 如果w是0，则不应使用此点。
	 * @code
	 *  // sample
	 *  ...
	 *  auto&& worldPoint = Screen2World(...);
	 *  if( !FuzzyIsZero( worldPoint.w ) )
	 *  {
	 *	 	glm::vec3 world3D(worldPoint);
	 *      world3D /= worldPoint;
	 *      /// using world3D
	 *	}
	 *	else
	 *	{
	 *		// error handler
	 *	}
	 */
	glm::vec3 Screen2World(const glm::vec2& screenPoint, glm::mat4& model, float* pPointDepth);
	/**
	 * @brief 世界坐标系转换为屏幕坐标系
	 * @brief worldPoint		世界坐标的点坐标点
	 * @brief viewportRange 	视口范围。 各个值依次为：左上-右下
	 * @brief modelViewMatrix 	模型视图矩阵
	 * @brief projectMatrix 	投影矩阵
	 * @brief pPointDepth   	屏幕点的深度，如果不指定(为nullptr),从深度缓冲区中读取深度值
	 * @return 					窗口坐标点
	 * @note 返回的窗口坐标带深度值，如果仅适用2D窗口像素坐标点，仅适用它的x,y维即可
	 */
	glm::vec3 World2Screen(const glm::vec3& worldPoint, glm::mat4& model);

protected:
	COpenGLView* glView = nullptr;

	// 从更新的CameraEuler的欧拉角计算前向量
	void updateCameraVectors();

	CCPanoramaCameraFovChangedCallback fovChangedCallback = nullptr;
	void* fovChangedCallbackData = nullptr;
	CCPanoramaCameraFovChangedCallback orthoSizeChangedCallback = nullptr;
	void* orthoSizeChangedCallbackData = nullptr;
};
