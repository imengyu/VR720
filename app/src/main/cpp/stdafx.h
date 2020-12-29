#pragma once
#ifndef STDAFX_H
#define STDAFX_H
#include "platform.h"

//android ndk support
#include <jni.h>
#include <sys/types.h>
#include <sys/prctl.h>
#include <unistd.h>

//android log
#include <android/log.h>


// C 运行时头文件
#include <stdlib.h>
#include <stdio.h>
#include <malloc.h>
#include <memory.h>
//Cpp
#include <string>
#include <memory>
#include "../utils/Logger.h"
#include "../core/CCSmartPtr.hpp"

#include <glm.hpp>
#include <ext.hpp>
#include <gtc/matrix_transform.hpp>
#include <gtc/constants.hpp>

//OpenGL includes


#include <GLES3/gl31.h>
#include <GLES3/gl3ext.h>

#define LPBYTE BYTE*
#define USHORT unsigned short
#define UINT unsigned int
#define ULONG unsigned long
#define UCHAR unsigned char

typedef unsigned long DWORD;
typedef unsigned char BYTE;
typedef unsigned short WORD;

JavaVM* GetGlobalJvm();
JNIEnv* GetJniEnv();

#endif //STDAFX_H
