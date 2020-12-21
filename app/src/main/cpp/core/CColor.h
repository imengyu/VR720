#pragma once
#ifndef VR720_CCCOLOR_H
#define VR720_CCCOLOR_H
#include "stdafx.h"

//颜色类
class CColor
{
public:
	CColor();
	CColor(float r, float g, float b);
	CColor(float r, float g, float b, float a);
	~CColor();

	float r = 1.0f;
	float g = 1.0f;
	float b = 1.0f;
	float a = 1.0f;

	void Set(float r, float g, float b, float a = 1.0f);

	static CColor FromRGBA(float r, float g, float b);
	static CColor FromRGBA(float r, float g, float b, float a);
	static CColor FromString(const char*str);

	static CColor Black; 
	static CColor White;
};

#endif

