#include "stdafx.h"
#include "COpenGLView.h"
#include "COpenGLRenderer.h"

COpenGLRenderer::COpenGLRenderer()
{
}
COpenGLRenderer::~COpenGLRenderer()
{
}

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
void COpenGLRenderer::Resize(int Width, int Height)
{
	this->Width = Width;
	this->Height = Height;

	glViewport(0, 0, Width, Height);
}
void COpenGLRenderer::Destroy()
{
}
