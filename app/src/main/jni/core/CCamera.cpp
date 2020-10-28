#include "CCamera.h"
#include "GlUtils.h"
#include "COpenGLView.h"

CCamera::CCamera(glm::vec3 position, glm::vec3 up, glm::vec3 rotate)
{
	Position = position;
	WorldUp = up;
	Rotate = rotate;
	updateCameraVectors();
}

// 返回使用欧拉角和LookAt矩阵计算的view矩阵
glm::mat4 CCamera::GetViewMatrix()
{
	return glm::lookAt(Position, Position + Front, Up);
}

void CCamera::SetPosItion(glm::vec3 position)
{
	Position = position;
	updateCameraVectors();
}

void CCamera::SetRotation(glm::vec3 rotation)
{
	Rotate = rotation;
	updateCameraVectors();
}

void CCamera::SetFOV(float fov)
{
	if (FiledOfView != fov) {
		FiledOfView = fov;
		if (fovChangedCallback)
			fovChangedCallback(fovChangedCallbackData, FiledOfView);
	}
}

void CCamera::SetOrthoSize(float o)
{
	if (OrthographicSize != o) {
		OrthographicSize = o;
		if (orthoSizeChangedCallback)
			orthoSizeChangedCallback(orthoSizeChangedCallbackData, FiledOfView);
	}
}
void CCamera::SetFOVChangedCallback(CCPanoramaCameraFovChangedCallback callback, void* data)
{
	fovChangedCallback = callback;
	fovChangedCallbackData = data;
}

void CCamera::SetOrthoSizeChangedCallback(CCPanoramaCameraFovChangedCallback callback, void* data)
{
	orthoSizeChangedCallback = callback;
	orthoSizeChangedCallbackData = data;
}

void CCamera::ForceUpdate() {
	updateCameraVectors();
}

void CCamera::Reset()
{
	Position = glm::vec3(0.0f);
	Up = glm::vec3(0.0f, 1.0f, 0.0f);
	Front = glm::vec3(0.0f, 0.0f, -1.0f);
	Rotate = glm::vec3(DEF_YAW, DEF_PITCH, 0.0f);
	FiledOfView = DEF_FOV;
	OrthographicSize = 1.0f;
	updateCameraVectors();
}

// 从更新的CameraEuler的欧拉角计算前向量

void CCamera::updateCameraVectors()
{
	// 计算新的前向量
	glm::vec3 front;
	front.x = cos(glm::radians(Rotate.x)) * cos(glm::radians(Rotate.y));
	front.y = sin(glm::radians(Rotate.y));
	front.z = sin(glm::radians(Rotate.x)) * cos(glm::radians(Rotate.y));
	Front = glm::normalize(front);
	// 再计算右向量和上向量
	Right = glm::normalize(glm::cross(Front, WorldUp));  // 标准化
	Up = glm::normalize(glm::cross(Right, Front));
}

void CCamera::SetView(COpenGLView* view)
{
	this->glView = view;
}


glm::vec3 CCamera::Screen2World(const glm::vec2& screenPoint, glm::mat4& model, float* pPointDepth = nullptr)
{
	GLfloat pointDepth(0.0f);
	if (nullptr != pPointDepth)
	{
		pointDepth = *pPointDepth;
	}
	else
	{
		// 获取深度缓冲区中x,y的数值
		glReadPixels((GLint)screenPoint.x, (GLint)screenPoint.y, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT, &pointDepth);
	}
	return glm::unProject(glm::vec3(screenPoint, pointDepth), view * model, projection, glm::vec4(0.0f, 0.0f, glView->Width, glView->Height));
}
glm::vec3 CCamera::World2Screen(const glm::vec3& worldPoint, glm::mat4&model)
{
	return glm::project(worldPoint, view * model, projection, glm::vec4(0.0f, 0.0f, glView->Width, glView->Height));
}
