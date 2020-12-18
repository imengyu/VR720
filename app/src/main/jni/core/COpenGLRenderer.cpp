#include "stdafx.h"
#include "COpenGLView.h"
#include "COpenGLRenderer.h"

COpenGLRenderer::COpenGLRenderer()
{
}
COpenGLRenderer::~COpenGLRenderer()
= default;

bool COpenGLRenderer::Init()
{
	return true;
}
void COpenGLRenderer::Render(float FrameTime)
{
}
void COpenGLRenderer::RenderUI()
{
}
void COpenGLRenderer::Update()
{

}
void COpenGLRenderer::Resize(int w, int h)
{
	this->Width = w;
	this->Height = h;
}
void COpenGLRenderer::Destroy()
{
}
void COpenGLRenderer::MarkDestroy()
{
}
bool COpenGLRenderer::ReInit() {
	return false;
}

int COpenGLRenderer::GetHeight() const {
    return Height;
}

int COpenGLRenderer::GetWidth() const {
	return Width;
}
