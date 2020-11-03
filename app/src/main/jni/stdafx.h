#pragma once
#ifndef STDAFX_H
#define STDAFX_H
#include "config.h"
#include "platform.h"

#if defined(VR720_WINDOWS)

	#define WIN32_LEAN_AND_MEAN             // 从 Windows 头文件中排除极少使用的内容
	// Windows 头文件
	#include <windows.h>
	#include <intrin.h>
	#include <tchar.h>

#elif defined(VR720_ANDROID)

	//android ndk support
	#include <jni.h>
	#include <sys/types.h>
	#include <unistd.h>
	#include "type-defines.h"

	//android log
	#include <android/log.h>

#endif

// C 运行时头文件
#include <stdlib.h>
#include <stdio.h>
#include <malloc.h>
#include <memory.h>
//Cpp
#include <string>
#include <memory>
#include "Logger.h"
#include "CCSmartPtr.hpp"

#include <glm.hpp>
#include <ext.hpp>
#include <gtc/matrix_transform.hpp>
#include <gtc/constants.hpp>

//OpenGL includes

#if defined(VR720_WINDOWS)

	#define GLEW_STATIC
	#include <gl/glew.h>
	#include <gl/wglew.h>

	#include "messages.h"

#elif defined(VR720_ANDROID) //VR720_WINDOWS

	#include <GLES3/gl31.h>
    #include <GLES3/gl3ext.h>

#endif //VR720_ANDROID

#include "api-defines.h"

#endif //STDAFX_H
