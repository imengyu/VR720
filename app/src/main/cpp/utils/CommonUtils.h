//
// Created by roger on 2020/12/21.
//

#ifndef VR720_COMMONUTILS_H
#define VR720_COMMONUTILS_H
#include "stdafx.h"

#define TEST_TIME_VAL timeuse

#define TEST_TIME_BEGIN() { struct timeval tpstart = { 0 }, tpend = { 0 };\
double TEST_TIME_VAL;\
gettimeofday(&tpstart,NULL)

#define TEST_TIME_STOP() gettimeofday(&tpend,NULL);\
TEST_TIME_VAL = (tpend.tv_sec - tpstart.tv_sec) * 1000. +       \
                (tpend.tv_usec - tpstart.tv_usec) / 1000.;

#define TEST_TIME_END() }

class CommonUtils {
public:
    static int CheckAppSignature(JNIEnv*env, jobject context, int *buf);
};


#endif //VR720_COMMONUTILS_H
