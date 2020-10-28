#pragma once
#include "stdafx.h"
#include "CCTexture.h"
#include <list>

struct TextureLoadQueueDataResult {
	bool success;
	int width;
	int height;
	int compoents;
	size_t size;
	BYTE* buffer;
};
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

	CCTexture* Push(CCTexture * texture, int x, int y, int id);

	void SetLoadHandle(CCTextureLoadQueueLoadHandle handle, void *data);

	void ResolveMain();
	void ResolveRender();

private:

	Logger*logger = nullptr;
	std::list<TextureLoadQueueInfo*> queue;
	TextureLoadQueueInfo* pendingTexture = nullptr;
	TextureLoadQueueInfo* pendingLoadDataTexture = nullptr;

	CCTextureLoadQueueLoadHandle loadHandle;
	void* loadHandleData;
};

