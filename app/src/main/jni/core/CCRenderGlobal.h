#pragma once
#ifndef VR720_CCRENDER_GLOBAL_H
#define VR720_CCRENDER_GLOBAL_H
#include "stdafx.h"

class CCRenderGlobal
{
public :
	static CCRenderGlobal* GetInstance();
	static void SetInstance(CCRenderGlobal*instance);
	static void Destroy();

	GLint viewLoc = -1;
	GLint projectionLoc = -1;
	GLint modelLoc = -1;
	GLint ourTextrueLoc = -1;
	GLint ourColorLoc = -1;
	GLint useColorLoc = -1;
	GLint texTilling = -1;
	GLint texOffest = -1;

	GLubyte* glVendor = nullptr;
	GLubyte* glRenderer = nullptr;
	GLubyte* glVersion = nullptr;
	GLubyte* glslVersion = nullptr;
};

#endif
