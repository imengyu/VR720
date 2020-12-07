#pragma once
#ifndef VR720_COBJ_LOADER_H
#define VR720_COBJ_LOADER_H
#include "CCMeshLoader.h"

//Obj 模型加载器
class CCObjLoader : public CCMeshLoader
{
    /**
	 * 从文件加载Mesh
	 * @param path 文件路径
	 * @param mesh 要被加载的Mesh
	 * @return 返回是否成功
	 */
	bool Load(const char* path, CCMesh* mesh) override;
	/**
	 * 从内存数据加载Mesh
	 * @param buffer mesh数据内存
	 * @param bufferSize mesh数据大小
	 * @param mesh 要被加载的Mesh
	 * @return 返回是否成功
	 */
	bool Load(BYTE * buffer, size_t bufferSize, CCMesh *mesh) override;
};

#endif
