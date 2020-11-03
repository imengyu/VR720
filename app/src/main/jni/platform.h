#pragma once
#ifndef PLATFORM_H
#define PLATFORM_H

#if defined(_WIN16) || defined(_WIN32) || defined(_WIN64)
#define VR720_WINDOWS 1
#define VR720_USE_GLES 0
#elif defined(ANDROID)
#define VR720_ANDROID 1
#define VR720_USE_GLES 1
#elif defined(__linux__)
#define VR720_LINUX 1
#define VR720_USE_GLES 0
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

//key map

#if defined(VR720_WINDOWS)
#define VR720_KEY_LEFT VK_LEFT
#define VR720_KEY_UP VK_UP
#define VR720_KEY_RIGHT VK_RIGHT
#define VR720_KEY_DOWN VK_DOWN
#define VR720_KEY_ESCAPE VK_ESCAPE
#define VR720_KEY_F11 VK_F11
#define VR720_KEY_F10 VK_F10
#elif defined(VR720_ANDROID)
#define VR720_KEY_LEFT 22
#define VR720_KEY_UP 21
#define VR720_KEY_RIGHT 19
#define VR720_KEY_DOWN 20
#define VR720_KEY_ESCAPE 111
#endif

#endif // !PLATFORM_H

