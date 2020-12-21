#include "CCMaterial.h"
#include "CCRenderGlobal.h"

CCMaterial::CCMaterial() = default;
CCMaterial::CCMaterial(CCSmartPtr<CCTexture> &diffuse) {
	this->diffuse = diffuse;
}
CCMaterial::~CCMaterial() = default;

void CCMaterial::Use() const
{
	if (!diffuse.IsNullptr()) diffuse->Use();

	CCRenderGlobal*current = CCRenderGlobal::GetInstance();
	glUniform2fv(current->texOffest, 1, glm::value_ptr(offest));
	glUniform2fv(current->texTilling, 1, glm::value_ptr(tilling));
}


