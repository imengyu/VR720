#include "CCMeshLoader.h"
#include "CCMesh.h"
#include "CCObjLoader.h"

CCObjLoader* objLoader = nullptr;

CCMeshLoader* CCMeshLoader::GetMeshLoaderByType(CCMeshType type)
{
	switch (type)
	{
	case CCMeshType::MeshTypeObj:
		return objLoader;
	case CCMeshType::MeshTypeFbx:
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
bool CCMeshLoader::Load(const vchar* path, CCMesh* mesh)
{
	return false;
}
bool CCMeshLoader::Load(BYTE *buffer, size_t bufferSize, CCMesh *mesh) {
	return false;
}
const vchar* CCMeshLoader::GetLastError()
{
	return lastErr.c_str();
}
void CCMeshLoader::SetLastError(const vchar* err)
{
	lastErr = err;
}

