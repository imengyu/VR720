//
// Created by roger on 2020/10/30.
//

#ifndef VR720_CCSMARTPTR_HPP
#define VR720_CCSMARTPTR_HPP

#include <unordered_map>

//模板类作为友元时要先有声明
template <typename T>
class CCSmartPtr;

//辅助类
class CCUPtr
{
public:

    //构造函数的参数为基础对象的指针
    CCUPtr(void* ptr);

    //析构函数
    ~CCUPtr();

    //引用计数
    int count;

    //基础对象指针
    void *p;
};

//指针池
class CCPtrPool {

public:

    std::unordered_map<void*, CCUPtr*> pool;

    static bool IsStaticPoolCanUse();
    static CCPtrPool* GetStaticPool();
    static void InitPool();
    static void ReleasePool();

    CCUPtr* GetPtr(void* ptr);
    CCUPtr* AddPtr(void* ptr);
    CCUPtr* AddRefPtr(void* ptr);
    CCUPtr* RemoveRefPtr(void* ptr);
    void ReleasePtr(void* ptr);
    void ClearUnUsedPtr();

    void ReleaseAllPtr();
};
#define CCPtrPoolStatic CCPtrPool::GetStaticPool()
#define CCPtrPoolStaticCanUse CCPtrPool::IsStaticPoolCanUse()
//智能指针类
template <typename T>
class CCSmartPtr
{
private:
    T* ptr = nullptr;
    CCUPtr* rp = nullptr;  //辅助类对象指针
public:
    CCSmartPtr() {  
        ptr = nullptr;
        if(CCPtrPoolStaticCanUse)
            rp = CCPtrPoolStatic->AddPtr(nullptr);
    }
    //构造函数
    CCSmartPtr(T *srcPtr)  { 
        ptr = srcPtr;
        if(CCPtrPoolStaticCanUse)
            rp = CCPtrPoolStatic->AddPtr(srcPtr);
    }      
    //复制构造函数
    CCSmartPtr(const CCSmartPtr<T> &sp)  {      
        ptr = sp.ptr;
        if(CCPtrPoolStaticCanUse)
            rp = CCPtrPoolStatic->AddRefPtr(sp.ptr);
    }     

    //重载赋值操作符
    CCSmartPtr& operator = (const CCSmartPtr<T>& rhs) {
        if(CCPtrPoolStaticCanUse)
            CCPtrPoolStatic->RemoveRefPtr(ptr);
        ptr = rhs.ptr;
        if(CCPtrPoolStaticCanUse)
            rp = CCPtrPoolStatic->AddRefPtr(ptr);
        return *this;
    }

    T & operator *() const { //重载*操作符
        return *ptr;
    }
    T* operator ->() const { //重载->操作符
        return ptr;
    }

    bool IsNullptr() const {
        return ptr == nullptr;
    }
    T* GetPtr() const { return ptr;  }
    int CheckRef() {
        if (rp) return rp->count;
        return 0;
    }
    int AddRef() {
        if (rp) 
            return ++rp->count;
        return 0;
    }
    void ForceRelease() {
        if(CCPtrPoolStaticCanUse)
            CCPtrPool::GetStaticPool()->ReleasePtr(ptr);
        ptr = nullptr;
        *rp = nullptr;
    }

    ~CCSmartPtr() {        //析构函数
        if(CCPtrPoolStaticCanUse)
            CCPtrPoolStatic->RemoveRefPtr(ptr);
        ptr = nullptr;
        rp = nullptr;
    }

};


#endif //VR720_CCSMARTPTR_HPP
