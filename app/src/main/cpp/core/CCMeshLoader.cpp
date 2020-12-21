#include "CCMeshLoader.h"
#include "CCMesh.h"
#include "CCObjLoader.h"
#include "../utils/PathHelper.h"

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
CCMeshLoader *CCMeshLoader::GetMeshLoaderByFilePath(const char *path) {
	std::string ext = Path::GetExtension(path);
	if(ext == "obj")
		return objLoader;
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
bool CCMeshLoader::Load(const char* path, CCMesh* mesh)
{
	return false;
}
bool CCMeshLoader::Load(BYTE *buffer, size_t bufferSize, CCMesh *mesh) {
	return false;
}
const char* CCMeshLoader::GetLastError()
{
	return lastErr.c_str();
}
void CCMeshLoader::SetLastError(const char* err)
{
	lastErr = err;
}


