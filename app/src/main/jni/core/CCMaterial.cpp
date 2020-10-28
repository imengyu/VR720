#include "CCMaterial.h"
#include "CCRenderGlobal.h"

CCMaterial::CCMaterial()
{
}
CCMaterial::CCMaterial(CCTexture* diffuse)
{
	this->diffuse = diffuse;
}
CCMaterial::~CCMaterial()
{
}

void CCMaterial::Use()
{
	if (diffuse != nullptr)
		diffuse->Use();

	CCRenderGlobal*current = CCRenderGlobal::GetInstance();
	glUniform2fv(current->texOffest, 1, glm::value_ptr(offest));
	glUniform2fv(current->texTilling, 1, glm::value_ptr(tilling));
}
