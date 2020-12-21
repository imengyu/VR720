#pragma once
#ifndef VR720_CCMESHLOADER_H
#define VR720_CCMESHLOADER_H
#include "stdafx.h"

//Mesh 类型
enum class CCMeshType {
	MeshTypeObj,
	MeshTypeFbx,
};

class CCMesh;
//Mesh 加载器
class CCMeshLoader
{
public:
	/**
	 * 获取指定类型的Mesh加载器
	 * @param type 类型，CCMeshType
	 * @return 返回加载器
	 */
	static CCMeshLoader* GetMeshLoaderByType(CCMeshType type);
	/**
	 * 通过文件路径获取指定类型的Mesh加载器
	 * @param path 网格文件路径
	 * @return 返回加载器
	 */
	static CCMeshLoader* GetMeshLoaderByFilePath(const char* path);
	/**
	 * 全局初始化
	 */
	static void Init();
	/**
	 * 全局释放资源
	 */
	static void Destroy();

	/**
	 * 从文件加载Mesh
	 * @param path 文件路径
	 * @param mesh 要被加载的Mesh
	 * @return 返回是否成功
	 */
	virtual bool Load(const char * path, CCMesh *mesh);
	/**
	 * 从内存数据加载Mesh
	 * @param buffer mesh数据内存
	 * @param bufferSize mesh数据大小
	 * @param mesh 要被加载的Mesh
	 * @return 返回是否成功
	 */
	virtual bool Load(BYTE * buffer, size_t bufferSize, CCMesh *mesh);
	/**
	 * 获取上一次加载发生的错误
	 * @return 加载错误
	 */
	virtual const char* GetLastError();

protected:
	/**
	 * 设置加载错误
	 * @param err 加载错误
	 */
	void SetLastError(const char* err);
private:
	std::string lastErr;
};

#endif
