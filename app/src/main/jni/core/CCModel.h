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

	//获取模型变换矩阵
	glm::mat4 GetModelMatrix();

	/**
	 * 刷新向量
	 */
	virtual void UpdateVectors();
	/**
	 * 重置模型位置和旋转
	 */
	virtual void Reset();
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
	//缩放比例
	glm::vec3 LocalScale = glm::vec3(1.0f);


	//模型材质
	CCSmartPtr<CCMaterial> Material;
	//模型网格
	CCSmartPtr<CCMesh> Mesh;

	const glm::vec3 WorldUp = glm::vec3(0.0f, 1.0f, 0.0f);
	const glm::vec3 WorldFront = glm::vec3(0.0f, 0.0f, 1.0f);
	const glm::vec3 WorldRight = glm::vec3(1.0f, 0.0f, 0.0f);

	glm::vec3 GetFront() ;
	glm::vec3 GetUp() ;
	glm::vec3 GetRight() ;

	/**
	 * 设置位置
	 * @param position 摄像机位置
	 */
	void SetPosition(glm::vec3 position);
	void SetLocalEulerAngles(glm::vec3 eulerAngles);
	void SetEulerAngles(glm::vec3 eulerAngles);
	void SetLocalRotation(glm::quat rotation);
	void SetRotation(glm::quat rotation);

	glm::vec3 GetLocalEulerAngles() const;
	glm::vec3 GetEulerAngles() const;
	glm::quat GetLocalRotation() const;
	glm::quat GetRotation() const;

	bool VectorDirty = true;

protected:

	glm::vec3 mEulerAngles = glm::vec3(0,0,0);
	glm::vec3 mLocalEulerAngles = glm::vec3(0,0,0);
	glm::quat mLocalRotation = glm::quat();
	glm::quat mRotation = glm::quat();

	glm::vec3 mUp = glm::vec3(0.0f, 1.0f, 0.0f);
	glm::vec3 mFront = glm::vec3(0.0f, 0.0f, 1.0f);
	glm::vec3 mRight = glm::vec3(0.0f, 0.0f, 1.0f);
	glm::vec3 mLocalUp = glm::vec3(0.0f, 1.0f, 0.0f);
	glm::vec3 mLocalFront = glm::vec3(0.0f, 0.0f, 1.0f);
	glm::vec3 mLocalRight = glm::vec3(0.0f, 0.0f, 1.0f);
};

#endif

