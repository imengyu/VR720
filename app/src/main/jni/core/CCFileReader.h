#pragma once
#ifndef VR720_CCFILEREADER_H
#define VR720_CCFILEREADER_H
#include "stdafx.h"

//文件读取器
class CCFileReader
{
public:
	/**
	 * 创建简易文件读取器
	 * @param path 目标文件路径
	 */
	CCFileReader(vstring & path);
	CCFileReader();
	virtual ~CCFileReader();

	/**
	 * 获取文件是否已经打开
	 * @return
	 */
	virtual bool Opened();
	/**
	 * 关闭文件
	 */
	virtual void Close();

	/**
	 * 获取文件长度
	 * @return 文件长度
	 */
	virtual size_t Length();
	/**
	 * 移动fp指针至文件开始的偏移位置
	 * @param i 指定位置
	 * @param seekType
	 */
	virtual void Seek(size_t i);
	/**
	 * 移动fp指针
	 * @param i 指定位置
	 * @param seekType 位置的类型，SEEK_*
	 */
	virtual void Seek(size_t i, int seekType);
	/**
	 * 获取文件句柄 FILE*
	 * @return
	 */
	virtual FILE* Handle();

	/**
	 *
	 * @param arr 缓冲区
	 * @param offset 读取偏移
	 * @param count 读取个数
	 */
	virtual void Read(BYTE* arr, size_t offset, size_t count);
	/**
	 * 读取一个字节
	 * @return 返回字节
	 */
	virtual BYTE ReadByte();
	/**
	 * 读取整个文件
	 * @param size 返回缓冲区大小
	 * @return 返回缓冲区
	 */
	virtual BYTE* ReadAllByte(size_t *size);

private:

	FILE* file = nullptr;
	size_t len = 0;

	void CloseFileHandle();
};

#endif

