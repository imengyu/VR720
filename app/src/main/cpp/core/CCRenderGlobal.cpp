#include "CCRenderGlobal.h"


CCRenderGlobal* instance;
CCRenderGlobal* CCRenderGlobal::GetInstance()
{
	return instance;
}
void CCRenderGlobal::SetInstance(CCRenderGlobal* instance)
{
	::instance = instance;
}
void CCRenderGlobal::Destroy()
{
	if (instance)
	{
		delete instance;
		instance = nullptr;
	}
}
