#pragma once
#include "stdafx.h"
#include "CCTexture.h"
#include <list>

//贴图加载返回信息
struct TextureLoadQueueDataResult {
	bool success;
	int width;
	int height;
	int compoents;
	size_t size;
	BYTE* buffer;
};
//贴图队列元素信息
struct TextureLoadQueueInfo {
	CCTexture* texture;
	int x;
	int y;
	int id;
	TextureLoadQueueDataResult* pendingResult;
};

//加载贴图回调
typedef TextureLoadQueueDataResult*(*CCTextureLoadQueueLoadHandle)(TextureLoadQueueInfo* info, CCTexture* texture, void* data);

//贴图加载队列
class CCTextureLoadQueue
{
public:
	CCTextureLoadQueue();
	~CCTextureLoadQueue();

	/**
	 * 推入要加载的贴图
	 * @param texture 贴图实例
	 * @param x x坐标
	 * @param y y坐标
	 * @param id 自定义ID
	 * @return
	 */
	CCTexture* Push(CCTexture * texture, int x, int y, int id);

	/**
	 * 设置贴图加载回调
	 * @param handle 回调
	 * @param data 自定义数据
	 */
	void SetLoadHandle(CCTextureLoadQueueLoadHandle handle, void *data);

	/**
	 * 主线程处理
	 */
	void ResolveMain();
	/**
	 * 渲染线程处理
	 */
	void ResolveRender();

	void Clear();
private:

	Logger*logger = nullptr;
	std::list<TextureLoadQueueInfo*> queue;
	TextureLoadQueueInfo* pendingTexture = nullptr;
	TextureLoadQueueInfo* pendingLoadDataTexture = nullptr;

	CCTextureLoadQueueLoadHandle loadHandle;
	void* loadHandleData;
};

