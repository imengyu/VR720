#pragma once
#ifndef STRING_HLP_H
#define STRING_HLP_H
#include "stdafx.h"
#include <stdio.h>  
#include <stdlib.h>  
#include <string>  
#include <vector>

//×Ö·û´®°ïÖúÀà
class CStringHlp
{
public:

	/**
	 * »ñÈ¡´óÐ¡µÄ¿É¶Á×Ö·û´®
	 * @param byteSize ´óÐ¡
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
	 * ¿í×Ö·û Unicode ×ª Char
	 * @param szStr Ô´×Ö·û´®
	 * @return
	 */
	static std::string UnicodeToAnsi(std::wstring szStr);
	/**
	 * ¿í×Ö·û Unicode ×ª UTF8
	 * @param unicode Ô´×Ö·û´®
	 * @return
	 */
	static std::string UnicodeToUtf8(std::wstring unicode);
	/**
	 * Char ×ª¿í×Ö·û Unicode
	 * @param szStr Ô´×Ö·û´®
	 * @return
	 */
	static std::wstring AnsiToUnicode(std::string szStr);
	/**
	 * UTF8 ×Ö·û´®×ª Unicode¿í×Ö·û
	 * @param szU8 Ô´×Ö·û´®
	 * @return
	 */
	static std::wstring Utf8ToUnicode(std::string szU8);

#ifdef VR720_ANDROID

	static jstring charTojstring(JNIEnv* env, const char* pat);
	static char* jstringToChar(JNIEnv* env, jstring jstr);

#endif
};

#endif








