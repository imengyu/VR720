#include "stdafx.h"
#include "COpenGLView.h"
#include "CCamera.h"
#include "CCShader.h"

COpenGLView::COpenGLView(COpenGLRenderer* renderer) {
	OpenGLRenderer = renderer;
	OpenGLRenderer->View = this;
}
COpenGLView::~COpenGLView() = default;

//摄像机
//*******************************

COpenGLRenderer* COpenGLView::GetRenderer() {
	return OpenGLRenderer;
}
void COpenGLView::SetCamera(CCamera * camera) {
	if (Camera)
		Camera->SetView(nullptr);
	Camera = camera;
	if (Camera)
		Camera->SetView(this);
}

void COpenGLView::CalcCameraProjection(CCamera * camera, CCShader * shader) const {
	if (camera) {

		//摄像机矩阵变换
		camera->view = camera->GetViewMatrix();
		glUniformMatrix4fv(shader->viewLoc, 1, GL_FALSE, glm::value_ptr(camera->view));
		//摄像机投影
		camera->projection = camera->Projection == CCameraProjection::Perspective ?
							 glm::perspective(glm::radians(camera->FiledOfView), (float)Width / (float)Height,
											  camera->ClippingNear,
											  camera->ClippingFar) :
							 glm::ortho(-camera->OrthographicSize / 2, camera->OrthographicSize / 2,
										-((float)Height / (float)Width * camera->OrthographicSize / 2),
										((float)Height / (float)Width * camera->OrthographicSize / 2),
										camera->ClippingNear, camera->ClippingFar);
		glUniformMatrix4fv(shader->projectionLoc, 1, GL_FALSE, glm::value_ptr(camera->projection));
	}
}
void COpenGLView::CalcNoMainCameraProjection(CCShader * shader) const {
	glm::mat4 view(1.0f);
	Camera->view = view;
	Camera->projection = view;
	glUniformMatrix4fv(shader->viewLoc, 1, GL_FALSE, glm::value_ptr(view));
	glUniformMatrix4fv(shader->projectionLoc, 1, GL_FALSE, glm::value_ptr(view));
}
void COpenGLView::CalcMainCameraProjection(CCShader * shader) const {
	CalcCameraProjection(Camera, shader);
}

//用户事件处理
//*******************************

void COpenGLView::Resize(int w, int h) {
	Width = w;
	Height = h;
}
void COpenGLView::SetMouseCallback(ViewMouseCallback mouseCallback) {
	this->mouseCallback = mouseCallback;
}
void COpenGLView::SetZoomViewCallback(ViewMouseCallback callback) {
	this->scrollCallback = callback;
}

//按键事件处理
//*******************************

int COpenGLView::AddKeyInKeyList(int* list, int code) {
	for (int i = 0; i < MAX_KEY_LIST; i++) {
		if (list[i] == 0)
		{
			list[i] = code;
			return i;
		}
	}
	return -1;
}
int COpenGLView::IsKeyInKeyListExists(int* list, int code) {
	for (int i = 0; i < MAX_KEY_LIST; i++) {
		if (list[i] == code)
			return i;
	}
	return -1;
}
void COpenGLView::HandleDownKey(int code) {

	int upIndex = IsKeyInKeyListExists(UpedKeys, code);
	if (upIndex > -1) UpedKeys[upIndex] = 0;

	int downIndex = IsKeyInKeyListExists(DownedKeys, code);
	if (downIndex == -1) AddKeyInKeyList(DownedKeys, code);

}
void COpenGLView::HandleUpKey(int code) {
	int upIndex = IsKeyInKeyListExists(UpedKeys, code);
	if (upIndex == -1) AddKeyInKeyList(UpedKeys, code);

	int downIndex = IsKeyInKeyListExists(DownedKeys, code);
	if (downIndex > -1) DownedKeys[downIndex] = 0;

}

bool COpenGLView::GetKeyPress(int code) {
	return IsKeyInKeyListExists(DownedKeys, code) > -1;
}
bool COpenGLView::GetKeyDown(int code) {
	int up = IsKeyInKeyListExists(DownedKeys, code);
	if (up > -1) {
		DownedKeys[up] = 0;
		return true;
	}
	return  false;
}
bool COpenGLView::GetKeyUp(int code) {
	int up = IsKeyInKeyListExists(UpedKeys, code);
	if (up > -1) {
		UpedKeys[up] = 0;
		return true;
	}
	return  false;
}
