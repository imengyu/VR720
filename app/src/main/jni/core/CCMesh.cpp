#include "CCMesh.h"
#include "CCRenderGlobal.h"
#include "CCMaterial.h"
#include "CCMeshLoader.h"

CCMesh::CCMesh()
{

}
CCMesh::~CCMesh()
{
	ReleaseBuffer();
}

void CCMesh::GenerateBuffer()
{
	//VBO
	glGenBuffers(1, &MeshVBO);
	ReBufferData();
}
void CCMesh::ReBufferData()
{
	size_t indices_size = indices.size();
	size_t positions_size = positions.size();
	size_t normals_size = normals.size();
	size_t texCoords_size = texCoords.size();

	std::vector<GLfloat> vertices_temp;
	std::vector<GLint> indices_temp;
	for (size_t i = 0; i < indices_size; i++) {
		glm::vec3 vertex = indices[i].vertex_index < positions_size ? positions[indices[i].vertex_index] : glm::vec3();
		glm::vec3 normal = indices[i].normal_index < normals_size ? normals[indices[i].normal_index] : glm::vec3();
		glm::vec2 texCoord = indices[i].texcoord_index < texCoords_size ? texCoords[indices[i].texcoord_index] : glm::vec3();

		vertices_temp.push_back(vertex.x);
		vertices_temp.push_back(vertex.y);
		vertices_temp.push_back(vertex.z);
		vertices_temp.push_back(normal.x);
		vertices_temp.push_back(normal.y);
		vertices_temp.push_back(normal.z);
		vertices_temp.push_back(texCoord.x);
		vertices_temp.push_back(texCoord.y);
	}

	glBindBuffer(GL_ARRAY_BUFFER, MeshVBO);
	glBufferData(GL_ARRAY_BUFFER, vertices_temp.size() * sizeof(GLfloat), &vertices_temp[0], DrawType);
	glBindBuffer(GL_ARRAY_BUFFER, 0);
}
void CCMesh::ReleaseBuffer()
{
	if (MeshVBO > 0) glDeleteBuffers(1, &MeshVBO);
}

void CCMesh::RenderMesh()
{
	if (MeshVBO > 0) {
		glBindBuffer(GL_ARRAY_BUFFER, MeshVBO);

		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);
		glEnableVertexAttribArray(2);

		glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 8 * sizeof(GLfloat), (GLvoid*)0);
		glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 8 * sizeof(GLfloat), (GLvoid*)(3 * sizeof(GLfloat)));
		glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 8 * sizeof(GLfloat), (GLvoid*)(6 * sizeof(GLfloat)));

		glDrawArrays(GL_TRIANGLES, 0, indices.size());
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
	}
}

void CCMesh::LoadFromObj(const wchar_t* path)
{
	CCMeshLoader::GetMeshLoaderByType(MeshTypeObj)->Load(path, this);
}
void CCMesh::UnLoad()
{
	positions.clear();
	normals.clear();
	indices.clear();
	texCoords.clear();

	ReleaseBuffer();
}

CCFace::CCFace(unsigned int vertex_index, unsigned int normal_index, unsigned int texcoord_index)
{
	this->vertex_index = vertex_index;
	this->normal_index = normal_index;
	this->texcoord_index = texcoord_index == -1 ? vertex_index : texcoord_index;
}
