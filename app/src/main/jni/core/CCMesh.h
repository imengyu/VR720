#pragma once
#include "stdafx.h"
#include <vector>

//面信息
class CCFace
{
public:
	CCFace(unsigned int vertex_index, unsigned int normal_index = 0, unsigned int texcoord_index = -1);

	unsigned int vertex_index;
	unsigned int normal_index;
	unsigned 	int texcoord_index;
};

//网格类
class CCMesh
{
public:
	CCMesh();
	~CCMesh();

	void GenerateBuffer();
	void ReBufferData();
	void ReleaseBuffer();

	//渲染Mesh
	void RenderMesh();

	//从obj文件加载
	void LoadFromObj(const wchar_t* path);
	//清空已加载的数据和缓冲区
	void UnLoad();

	GLuint MeshVBO = 0;

	GLenum DrawType = GL_STATIC_DRAW;

	std::vector<glm::vec3> positions;
	std::vector<glm::vec3> normals;
	std::vector<CCFace> indices;
	std::vector<glm::vec2> texCoords;
};

