#pragma once
#include "stdafx.h"

//贴图类
class CCTexture
{
	const char* LOG_TAG = "CCTexture";
public:
	CCTexture();

	CCTexture(GLuint type);

	~CCTexture();

	/**
	 * 从文件加载贴图（支持bmp、png、jpg、ttf、gif）
	 * @param path 贴图文件路径
	 * @return
	 */
	bool Load(const char* path);
	/**
	 * 从内存自动加载贴图数据
	 * @param buffer 数据
	 * @param bufferSize 数据长度
	 */
	bool Load(BYTE* buffer, size_t bufferSize);
	/**
	 * 从内存加载贴图数据
	 * @param data 数据
	 * @param width 图像宽
	 * @param height 图像高
	 * @param type 自定义数据类型（GL_RGB/GL_RGBA/GL_BGR）
	 */
	void LoadBytes(BYTE* data, int width, int height, GLenum format);


	/**
	 * 销毁贴图
	 */
	void Destroy();
	/**
	 * 使用当前贴图
	 */
	void Use() const;
	/**
	 * 取消使用当前贴图
	 */
	static void UnUse(GLenum type);

	//设置是否缓存
	bool backupData = false;
	//获取是否有透明通道
	bool alpha = false;
	//获取贴图宽度
	int width = 0;
	//获取贴图高度
	int height = 0;
	//获取贴图ID
	GLuint texture = 0;
	//获取或设置贴图横轴重复类型
	GLuint wrapS = GL_REPEAT;
	//获取或设置贴图纵轴重复类型
	GLuint wrapT = GL_REPEAT;
	//贴图类型
	GLuint textureType = GL_TEXTURE_2D;
	//立方贴图大小
	GLuint cubeMapSize = 256;

	/**
	 * 重新创建贴图并填充数据
	 * @param reCreate 是否重新创建贴图数据
	 */
	void ReBufferData(bool reCreate);

	/**
	 * 创建OpenGL贴图
	 */
	void CreateGLTexture();

	/**
	 * 获取备份缓冲区
	 */
	void* GetBackupDataPtr() { return backupDataPtr; }

	/**
	 * 获取备份缓冲区大小
	 */
	size_t GetBackupDataLength() { return backupDataLength; }

	/**
	 * 生成备份缓冲区或者是生成备份缓冲区然后备份数据
	 * @param data 数据，如果为空则只创建缓冲区不填充数据
	 * @param width 宽度
	 * @param height 高度
	 * @param type GL类型
	 */
	void DoBackupBufferData(BYTE* data, int width, int height, GLenum format);

	void LoadGridTexture(int w, int h, int gridSize, bool alpha, bool backup);
protected:
	BYTE* backupDataPtr = nullptr;
	size_t backupDataLength = 0;
	GLenum backupType = 0;

	void LoadDataToGL(BYTE* data, int width, int height, GLenum format);


};

