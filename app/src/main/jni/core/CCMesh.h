#pragma once
#ifndef VR720_CCMAESH_H
#define VR720_CCMAESH_H
#include "stdafx.h"
#include <vector>

//面信息
class CCFace
{
public:
	CCFace(unsigned int vertex_index, unsigned int normal_index = 0, unsigned int texcoord_index = -1);

	unsigned int vertex_index;
	unsigned int normal_index;
	unsigned int texcoord_index;
};

//简单网格类
class CCMesh
{
public:
	CCMesh();
	~CCMesh();

	/**
	 * 生成网格缓冲区
	 */
	void GenerateBuffer();
	/**
	 * 重新将数据传入缓冲区
	 */
	void ReBufferData();
	/**
	 * 释放网格缓冲区
	 */
	void ReleaseBuffer();
	/**
	 * 渲染Mesh
	 */
	void RenderMesh() const;
	/**
	 * 从obj文件加载
	 * @param path obj文件路径
	 */
	void LoadFromObj(const char* path);
	/**
	 * 清空已加载的数据和缓冲区
	 */
	void UnLoad();

	GLuint MeshVBO = 0;
	GLenum DrawType = GL_STATIC_DRAW;

	//顶点数据
	std::vector<glm::vec3> positions;
	//法线向量数据
	std::vector<glm::vec3> normals;
	//索引数据
	std::vector<CCFace> indices;
	//uv数据
	std::vector<glm::vec2> texCoords;
};

#endif

