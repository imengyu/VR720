#include "CCamera.h"
#include "GlUtils.h"
#include "COpenGLView.h"

CCamera::CCamera(glm::vec3 position, glm::vec3 up, glm::vec3 rotate)
{
	SetPosition(position);
	SetEulerAngles(rotate);
}

// 返回使用欧拉角和LookAt矩阵计算的view矩阵
glm::mat4 CCamera::GetViewMatrix()
{
	if(VectorDirty) UpdateVectors();
	return glm::lookAt(Position, Position + GetFront(), GetUp());
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

void CCamera::SwitchToFace(int faceIndex)
{
	switch (faceIndex) {
		case 0:
			SetEulerAngles(glm::vec3(0, 90, 0));
			break;
		case 1:
			SetEulerAngles(glm::vec3(0, -90, 0));
			break;
		case 2:
			SetEulerAngles(glm::vec3(-90, 180, 0));
			break;
		case 3:
			SetEulerAngles(glm::vec3(90, 180, 0));
			break;
		case 4:
			SetEulerAngles(glm::vec3(0, 180, 0));
			break;
		case 5:
			SetEulerAngles(glm::vec3(0, 0, 0));
			break;
		default:
			break;
	}
}

void CCamera::ForceUpdate() {
}
void CCamera::Reset()
{
	CCModel::Reset();
	Position = glm::vec3(0.0f);
	SetEulerAngles(glm::vec3(DEF_YAW, DEF_PITCH, 0.0f));
	FiledOfView = DEF_FOV;
	ClippingNear = 0.1f;
	ClippingFar = 1000.0f;
	OrthographicSize = 1.0f;
}
void CCamera::SetView(COpenGLView* view)
{
	this->glView = view;
}

glm::vec3 CCamera::Screen2World(const glm::vec2& screenPoint, glm::mat4& model, const float* pPointDepth = nullptr)
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
