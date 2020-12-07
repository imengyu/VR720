#pragma once
#ifndef VR720_CCMODEL_H
#define VR720_CCMODEL_H
#include "stdafx.h"

class CCMaterial;
class CCMesh;
//模型类
class CCModel
{
public:
	CCModel();
	~CCModel();

	//刷新模型向量
	void UpdateVectors();
	//获取模型变换矩阵
	glm::mat4 GetMatrix() const;

	/**
	 * 重置模型位置和旋转
	 */
	void Reset();
	/**
	 * 绘制模型
	 */
	void Render() const;
	/**
	 * 重新缓冲数据
	 */
	void ReBufferData() const;

	//模型是否可见
	bool Visible = true;

	//模型位置
	glm::vec3 Position = glm::vec3(0.0f);
	//模型旋转
	glm::vec3 Rotation = glm::vec3(0.0f);

	//模型材质
	CCSmartPtr<CCMaterial> Material;
	//模型网格
	CCSmartPtr<CCMesh> Mesh;

	glm::vec3 Front = glm::vec3(0.0f);
	glm::vec3 Right = glm::vec3(0.0f);
	glm::vec3 Up = glm::vec3(0.0f);
	glm::vec3 WorldUp = glm::vec3(0.0f, 1.0f, 0.0f);
};

#endif

