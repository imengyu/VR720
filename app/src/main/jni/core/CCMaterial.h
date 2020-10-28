#pragma once
#include "stdafx.h"
#include "CCTexture.h"
#include <vector>

class CCMaterial
{
public:
	CCMaterial();
	CCMaterial(CCTexture* diffuse);
	~CCMaterial();

	CCTexture* diffuse = nullptr;

	glm::vec2 tilling = glm::vec2(1.0f, 1.0f);
	glm::vec2 offest = glm::vec2(0.0f, 0.0f);

	void Use();
};

