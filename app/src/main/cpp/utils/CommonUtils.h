//
// Created by roger on 2020/12/21.
//

#ifndef VR720_COMMONUTILS_H
#define VR720_COMMONUTILS_H
#include "stdafx.h"

class CommonUtils {
public:
    static int CheckAppSignature(JNIEnv*env, jobject context, int *buf);
};


#endif //VR720_COMMONUTILS_H
