#pragma once
#include "stdafx.h"


class COpenGLView;
/**
 * 渲染器抽象类
 */
class COpenGLRenderer
{
protected:
	//宽度和高度
	int Width, Height;

public:
	COpenGLRenderer();
	~COpenGLRenderer();

	/**
	 * 重新初始化
	 * @return 返回初始化是否成功
	 */
	virtual bool ReInit();
	/**
	 * 初始化
	 * @return 返回初始化是否成功
	 */
	virtual bool Init();
	/**
	 * 渲染时调用
	 * @param FrameTime 增量时间
	 */
	virtual void Render(float FrameTime);
	/**
	 * 渲染UI时调用
	 */
	virtual void RenderUI();
	/**
	 * 每一帧更新时调用
	 */
	virtual void Update();
	/**
	 * 当视图重新调整大小时会调用此方法
	 * @param Width 新宽度
	 * @param Height 新高度
	 */
	virtual void Resize(int Width, int Height);
	/**
	 * 释放
	 */
	virtual void Destroy();
	/**
	 * 延迟释放
	 */
	virtual void MarkDestroy();

	COpenGLView * View;
};

