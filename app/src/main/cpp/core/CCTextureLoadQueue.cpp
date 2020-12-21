#include "CCTextureLoadQueue.h"

CCTextureLoadQueue::CCTextureLoadQueue()
{
	logger = Logger::GetStaticInstance();
	loadHandle = nullptr;
	loadHandleData = nullptr;
}
CCTextureLoadQueue::~CCTextureLoadQueue()
{
	if (queue.size() > 0) {

		logger->Log("[CCTextureLoadQueue] Destroy not load Textures, count : %d", queue.size());

		std::list<TextureLoadQueueInfo*>::iterator it;
		for (it = queue.begin(); it != queue.end(); it++)
		{
			TextureLoadQueueInfo* dat = *it;
			if (dat->pendingResult)
				delete dat->pendingResult;
			delete dat;
		}
		queue.clear();
	}
}

CCTexture* CCTextureLoadQueue::Push(CCTexture* texture, int x, int y, int id)
{
	if (texture) {
		auto* info = new TextureLoadQueueInfo();
		info->texture = texture;
		info->x = x;
		info->y = y;
		info->id = id;
		queue.push_back(info);
	}
	return texture;
}
void CCTextureLoadQueue::SetLoadHandle(CCTextureLoadQueueLoadHandle handle, void* data)
{
	loadHandle = handle;
	loadHandleData = data;
}
void CCTextureLoadQueue::ResolveMain()
{
	if (pendingTexture == nullptr && !queue.empty()) {
		pendingTexture = queue.front();
		queue.pop_front();

		logger->Log("[CCTextureLoadQueue] Load Texture %d", pendingTexture->id);

		TextureLoadQueueDataResult *result = nullptr;
		if (loadHandle)
			result = loadHandle(pendingTexture, pendingTexture->texture, loadHandleData);

		if (result) {
			if (result->success) {
				pendingLoadDataTexture = pendingTexture;
				pendingTexture->pendingResult = result;
			}
			else delete result;
		}
	}
}
void CCTextureLoadQueue::ResolveRender()
{
	if (pendingLoadDataTexture != nullptr) {
		auto result = pendingLoadDataTexture->pendingResult;
		if (result) {
			logger->Log("[CCTextureLoadQueue:Render] Load Texture Data %d", pendingTexture->id);
			switch (result->compoents)
			{
			case 3:
				pendingLoadDataTexture->texture->LoadRGB(result->buffer, result->width, result->height);
				break;
			case 4:
				pendingLoadDataTexture->texture->LoadRGBA(result->buffer, result->width, result->height);
				break;
			default:
				break;
			}
			if(result->buffer)
				free(result->buffer);
			delete result;
			pendingLoadDataTexture->pendingResult = nullptr;
		}
		delete pendingLoadDataTexture;
		pendingLoadDataTexture = nullptr;
		pendingTexture = nullptr;
	}
}
