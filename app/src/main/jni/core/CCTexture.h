#pragma once
#include "stdafx.h"

//贴图类
class CCTexture
{
public:
	CCTexture();
	~CCTexture();

	/**
	 * 从文件加载贴图（支持bpm、png、jpg、ttf、gif）
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
	 * 从内存加载贴图RGB数据
	 * @param data RGB数据
	 * @param width 图像宽
	 * @param height 图像高
	 */
	void LoadRGB(BYTE* data, int width, int height);
	/**
	 * 从内存加载贴图RGBA数据
	 * @param data RGBA数据
	 * @param width 图像宽
	 * @param height 图像高
	 */
	void LoadRGBA(BYTE* data, int width, int height);
	/**
	 * 从内存加载贴图数据
	 * @param data 数据
	 * @param width 图像宽
	 * @param height 图像高
	 * @param type 自定义数据类型（GL_RGB/GL_RGBA/GL_BGR）
	 */
	void LoadBytes(BYTE* data, int width, int height, GLenum type);


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
	static void UnUse();
	/**
	 * 获取贴图是否加载
	 * @return 是否加载
	 */
	bool Loaded() const;

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

	void ReBufferData();

protected:
	BYTE* backupDataPtr = nullptr;
	size_t backupDataLength = 0;
	GLenum backupType = 0;

	void LoadToGl(BYTE* data, int width, int height, GLenum type);
	void DoBackupBufferData(BYTE* data, int width, int height, GLenum type);
};

