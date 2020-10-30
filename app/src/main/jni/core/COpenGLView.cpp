#include "stdafx.h"
#include "COpenGLView.h"
#include "CCamera.h"
#include "CCShader.h"

COpenGLView::COpenGLView(COpenGLRenderer *renderer) {
	OpenGLRenderer = renderer;
	OpenGLRenderer->View = this;
}
COpenGLView::~COpenGLView() {

}

COpenGLRenderer *COpenGLView::GetRenderer() {
	return OpenGLRenderer;
}
void COpenGLView::SetCamera(CCamera *camera) {
	if (Camera)
		Camera->SetView(nullptr);
	Camera = camera;
	if (Camera)
		Camera->SetView(this);
}


void COpenGLView::CalcCameraProjection(CCamera *camera, CCShader *shader) {
	if (camera) {

		//摄像机矩阵变换
		camera->view = camera->GetViewMatrix();
		glUniformMatrix4fv(shader->viewLoc, 1, GL_FALSE, glm::value_ptr(camera->view));
		//摄像机投影
		camera->projection = camera->Projection == CCameraProjection::Perspective ?
							 glm::perspective(glm::radians(camera->FiledOfView), (float)Width / (float)Height,
											  camera->ClippingNear,
											  camera->ClippingFar) :
							 glm::ortho(-camera->OrthographicSize / 2,  camera->OrthographicSize / 2,
										-((float)Height / (float)Width * camera->OrthographicSize / 2),
										((float)Height / (float)Width * camera->OrthographicSize / 2),
										camera->ClippingNear, camera->ClippingFar);
		glUniformMatrix4fv(shader->projectionLoc, 1, GL_FALSE, glm::value_ptr(camera->projection));
	}
}
void COpenGLView::CalcNoMainCameraProjection(CCShader *shader) {
	glm::mat4 view(1.0f);
	Camera->view = view;
	Camera->projection = view;
	glUniformMatrix4fv(shader->viewLoc, 1, GL_FALSE, glm::value_ptr(view));
	glUniformMatrix4fv(shader->projectionLoc, 1, GL_FALSE, glm::value_ptr(view));
}
void COpenGLView::CalcMainCameraProjection(CCShader *shader) {
	CalcCameraProjection(Camera, shader);
}

#if defined(VR720_ANDROID)
void COpenGLView::Resize(int w, int h) {
	Width = w;
	Height = h;
}
#elif defined(VR720_WINDOWS) || defined(VR720_LINUX)
void COpenGLView::Resize(int w, int h, bool moveToCenter)
{
	Width = w;
	Height = h;
}

void COpenGLView::SetBeforeQuitCallback(BeforeQuitCallback beforeQuitCallback)
{
	this->beforeQuitCallback = beforeQuitCallback;
}
void COpenGLView::SetMouseCallback(ViewMouseCallback mouseCallback)
{
	this->mouseCallback = mouseCallback;
}
void COpenGLView::SetScrollCallback(ViewMouseCallback mouseCallback)
{
	this->scrollCallback = mouseCallback;
}
#endif
