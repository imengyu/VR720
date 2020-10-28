#include "CCMeshLoader.h"
#include "CCMesh.h"
#include "CCObjLoader.h"

CCObjLoader* objLoader = nullptr;

CCMeshLoader* CCMeshLoader::GetMeshLoaderByType(CCMeshType type)
{
	switch (type)
	{
	case MeshTypeObj:
		return objLoader;
	case MeshTypeFbx:
		break;
	}
	return nullptr;
}
void CCMeshLoader::Init()
{
	objLoader = new CCObjLoader();
}
void CCMeshLoader::Destroy()
{
	delete objLoader;
}
bool CCMeshLoader::Load(const wchar_t* path, CCMesh* mesh)
{
	return false;
}

const wchar_t* CCMeshLoader::GetLastError()
{
	return lastErr.c_str();
}
void CCMeshLoader::SetLastError(const wchar_t* err)
{
	lastErr = err;
}
