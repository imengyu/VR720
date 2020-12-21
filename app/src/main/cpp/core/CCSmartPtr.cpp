#include "CCSmartPtr.hpp"
#include "../utils/Logger.h"

CCUPtr::CCUPtr(void* ptr) : p(ptr), count(1) { }
CCUPtr::~CCUPtr() { 
	if(p != nullptr)
		delete (int*)p;
	p = nullptr;
}

CCPtrPool* globalPool = nullptr;

CCPtrPool* CCPtrPool::GetStaticPool()
{
	return globalPool;
}
void CCPtrPool::InitPool()
{
	globalPool = new CCPtrPool();
	globalPool->pool[nullptr] = new CCUPtr(nullptr);
}
void CCPtrPool::ReleasePool()
{
	if (globalPool) {
		globalPool->ReleaseAllPtr();
		delete globalPool;
		globalPool = nullptr;
	}
}

CCUPtr* CCPtrPool::GetPtr(void* ptr)
{
	if (pool.find(ptr) != pool.end())
		return pool[ptr];
	return nullptr;
}
CCUPtr* CCPtrPool::AddPtr(void* ptr)
{
	CCUPtr* uptr = GetPtr(ptr);
	if (uptr) uptr->count++;
	else {
		uptr = new CCUPtr(ptr);
		pool[ptr] = uptr;
	}
	return uptr;
}
CCUPtr* CCPtrPool::AddRefPtr(void* ptr)
{
	CCUPtr* uptr = GetPtr(ptr);
	if (uptr) uptr->count++;
	return uptr;
}
CCUPtr* CCPtrPool::RemoveRefPtr(void* ptr)
{
	CCUPtr* uptr = GetPtr(ptr);
	if (uptr) {
		uptr->count--;
		if (uptr->count == 0) {
			delete uptr;
			pool.erase(ptr);
		}
	}
	return uptr;
}
void CCPtrPool::ReleasePtr(void* ptr)
{
	CCUPtr* uptr = GetPtr(ptr);
	if (uptr) {
		delete uptr;
		pool.erase(ptr);
	}
}
void CCPtrPool::ClearUnUsedPtr() {

	int deletedCount = 0;
	for (auto &it : pool)
		if (it.second->count <= 0) {
			delete it.second;
			deletedCount++;
		}

	LOGIF("[CCPtrPool] Clear un used Ptr : %d ptr released ", deletedCount);
	pool.clear();
}
void CCPtrPool::ReleaseAllPtr()
{
	for (auto & it : pool)
		delete it.second;

	LOGIF("[CCPtrPool] Release Ptr : %d ptr released ", pool.size());
	pool.clear();
}