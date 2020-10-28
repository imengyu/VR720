#pragma once
#include "CCMeshLoader.h"

//Obj ¼ÓÔØÆ÷
class CCObjLoader :  public CCMeshLoader
{
	bool Load(const wchar_t* path, CCMesh* mesh) override;
};

