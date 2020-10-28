#pragma once
#include "stdafx.h"

enum CCMeshType {
	MeshTypeObj,
	MeshTypeFbx,
};

class CCMesh;
//Mesh ¼ÓÔØÆ÷
class CCMeshLoader
{
public:
	static CCMeshLoader* GetMeshLoaderByType(CCMeshType type);
	static void Init();
	static void Destroy();

	virtual bool Load(const wchar_t*path, CCMesh *mesh);
	virtual const wchar_t* GetLastError();

protected:
	void SetLastError(const wchar_t* err);
private:
	std::wstring lastErr;
};

