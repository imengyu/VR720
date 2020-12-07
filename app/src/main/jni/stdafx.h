#pragma once
#ifndef STDAFX_H
#define STDAFX_H
#include "config.h"
#include "platform.h"

//android ndk support
#include <jni.h>
#include <sys/types.h>
#include <unistd.h>
#include "type-defines.h"

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
#include "Logger.h"
#include "CCSmartPtr.hpp"

#include <glm.hpp>
#include <ext.hpp>
#include <gtc/matrix_transform.hpp>
#include <gtc/constants.hpp>

//OpenGL includes


#include <GLES3/gl31.h>
#include <GLES3/gl3ext.h>

#include "type-defines.h"

#endif //STDAFX_H
