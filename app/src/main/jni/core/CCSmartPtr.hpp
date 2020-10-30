//
// Created by roger on 2020/10/30.
//

#ifndef VR720_CCSMARTPTR_HPP
#define VR720_CCSMARTPTR_HPP

//模板类作为友元时要先有声明
template <typename T>
class CCSmartPtr;

//辅助类
template <typename T>
class CCUPtr
{
private:
    //该类成员访问权限全部为private，因为不想让用户直接使用该类
    friend class CCSmartPtr<T>;      //定义智能指针类为友元，因为智能指针类需要直接操纵辅助类

    //构造函数的参数为基础对象的指针
    CCUPtr(T *ptr) :p(ptr), count(1) { }

    //析构函数
    ~CCUPtr() { delete p; }
    //引用计数
    int count;

    //基础对象指针
    T *p;
};

//智能指针类
template <typename T>
class CCSmartPtr
{
public:
    CCSmartPtr() {}
    CCSmartPtr(T *ptr) :rp(new CCUPtr<T>(ptr)) { }      //构造函数
    CCSmartPtr(const CCSmartPtr<T> &sp) : rp(sp.rp) { ++rp->count; }  //复制构造函数
    CCSmartPtr& operator = (const CCSmartPtr<T>& rhs) {    //重载赋值操作符
        if(rp) {
            if(rhs.rp)
                ++rhs.rp->count;     //首先将右操作数引用计数加1，
            if (--rp->count == 0)     //然后将引用计数减1，可以应对自赋值
                ForceRelease();
            rp = rhs.rp;
        }
        return *this;
    }

    T & operator *() const { //重载*操作符
        return *(rp->p);
    }
    T* operator ->() const { //重载->操作符
        return rp->p;
    }

    bool IsNullptr() const {
        return rp == nullptr || rp->p == nullptr;
    }
    T* GetPtr() const {
        return rp ? rp->p : nullptr;
    }
    int CheckRef() {
        if (rp) return rp->count;
        return 0;
    }
    int AddRef() {
        if (rp) {
            return ++rp->count;
        }
        return 0;
    }
    void ForceRelease() {
        if (rp)
            delete rp;
        rp = nullptr;
    }

    ~CCSmartPtr() {        //析构函数
        if (rp && --rp->count == 0)    //当引用计数减为0时，删除辅助类对象指针，从而删除基础对象
            ForceRelease();
    }
private:
    CCUPtr<T> *rp = nullptr;  //辅助类对象指针
};


#endif //VR720_CCSMARTPTR_HPP
