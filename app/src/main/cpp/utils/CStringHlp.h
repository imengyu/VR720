#pragma once
#ifndef STRING_HLP_H
#define STRING_HLP_H
#include "stdafx.h"
#include <stdio.h>  
#include <stdlib.h>  
#include <string>  
#include <vector>

//字符串帮助类
class CStringHlp
{
public:

	/**
	 * 获取大小的可读字符串
	 * @param byteSize 大小
	 * @return
	 */
	static std::string GetFileSizeStringAuto(long long byteSize);

	static std::string & FormatString(std::string & _str, const char * format, ...);
	static std::wstring & FormatString(std::wstring & _str, const wchar_t * format, ...);
	static std::wstring FormatString(const wchar_t * format, ...);
	static std::wstring FormatString(const wchar_t *format, va_list marker);
	static std::string FormatString(const char * format, va_list marker);
	static std::string FormatString(const char * format, ...);

	/**
	 * 宽字符 Unicode 转 Char
	 * @param szStr 源字符串
	 * @return
	 */
	static std::string UnicodeToAnsi(const std::wstring& szStr);
	/**
	 * 宽字符 Unicode 转 UTF8
	 * @param unicode 源字符串
	 * @return
	 */
	static std::string UnicodeToUtf8(const std::wstring& unicode);
	/**
	 * Char 转宽字符 Unicode
	 * @param szStr 源字符串
	 * @return
	 */
	static std::wstring AnsiToUnicode(const std::string& szStr);
	/**
	 * UTF8 字符串转 Unicode宽字符
	 * @param szU8 源字符串
	 * @return
	 */
	static std::wstring Utf8ToUnicode(const std::string& szU8);

	static jstring charTojstring(JNIEnv* env, const char* pat);
	static char* jstringToChar(JNIEnv* env, jstring jstr);
};

#endif








