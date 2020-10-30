#pragma once
#ifndef VR720_CCMATERIAL_H
#define VR720_CCMATERIAL_H
#include "stdafx.h"
#include "CCTexture.h"
#include <vector>

//简单材质
class CCMaterial
{
public:
	CCMaterial();
	CCMaterial(CCSmartPtr<CCTexture> & diffuse);
	~CCMaterial();

	//漫反射贴图
	CCSmartPtr<CCTexture> diffuse = nullptr;

	//贴图重复
	glm::vec2 tilling = glm::vec2(1.0f, 1.0f);
	//贴图偏移
	glm::vec2 offest = glm::vec2(0.0f, 0.0f);

	/**
	 * 使用当前材质
	 */
	void Use() const;
};

#endif

