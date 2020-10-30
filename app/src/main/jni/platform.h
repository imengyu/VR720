#pragma once
#ifndef PLATFORM_H
#define PLATFORM_H

#if defined(_WIN16) || defined(_WIN32) || defined(_WIN64)
#define VR720_WINDOWS 1
#elif defined(ANDROID)
#define VR720_ANDROID 1
#elif defined(__linux__)
#define VR720_LINUX 1
#else
#define VR720_UNKNOW 1
#endif

//wide char def


#if defined(VR720_WINDOWS) && VR720_USE_WIDE_CHAR
#define vchar wchar_t
#define vstring std::wstring
#define _vstr(x) L ## x
#define WCHAR_API 1
#else
#define vchar char
#define vstring std::string
#define _vstr(x) x
#define WCHAR_API 0
#endif

#endif // !PLATFORM_H

