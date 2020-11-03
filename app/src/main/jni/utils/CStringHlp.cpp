#include "CStringHlp.h"

std::string & CStringHlp::FormatString(std::string & _str, const char * format, ...) {
	std::string tmp;

	va_list marker;
	va_start(marker, format);

#ifdef _MSC_VER
	size_t num_of_chars = _vscprintf(format, marker);
#else
    size_t num_of_chars = vsprintf(nullptr, format, marker);
#endif
	if (num_of_chars > tmp.capacity()) {
		tmp.resize(num_of_chars + 1);
	}
#if defined(_MSC_VER) && _MSC_VER > 1600
	vsprintf_s((char *)tmp.data(), tmp.capacity(), format, marker);
#else
	vsprintf(nullptr, format, marker);
#endif

	va_end(marker);

	_str = tmp;
	return _str;
}
std::wstring & CStringHlp::FormatString(std::wstring & _str, const wchar_t * format, ...) {

	std::wstring tmp;

	va_list marker;
	va_start(marker, format);
#ifdef _MSC_VER
	size_t num_of_chars = _vscwprintf(format, marker);
#else
	size_t num_of_chars = vswprintf(nullptr, 0, format, marker);
#endif
	if (num_of_chars > tmp.capacity()) {
		tmp.resize(num_of_chars + 1);
	}
#if defined(_MSC_VER) && _MSC_VER > 1600
	vswprintf_s((wchar_t *)tmp.data(), tmp.capacity(), format, marker);
#else
	vswprintf((wchar_t *)tmp.data(), tmp.capacity(), format, marker);
#endif

	va_end(marker);

	_str = tmp;
	return _str;
}
std::wstring CStringHlp::FormatString(const wchar_t * format, va_list marker)
{
	std::wstring tmp;
#ifdef _MSC_VER
	size_t num_of_chars = _vscwprintf(format, marker);
#else
	size_t num_of_chars = vswprintf(nullptr, 0, format, marker);
#endif
	if (num_of_chars > tmp.capacity()) {
		tmp.resize(num_of_chars + 1);
	}
#if defined(_MSC_VER) && _MSC_VER > 1600
	vswprintf_s((wchar_t *)tmp.data(), tmp.capacity(), format, marker);
#else
	vswprintf((wchar_t *)tmp.data(), tmp.capacity(), format, marker);
#endif

	return tmp;
}
std::string CStringHlp::FormatString(const char * format, va_list marker)
{
	std::string tmp;
#ifdef _MSC_VER
	size_t num_of_chars = _vscprintf(format, marker);
#else
	size_t num_of_chars = vsnprintf(nullptr, 0, format, marker);;
#endif
	if (num_of_chars > tmp.capacity()) {
		tmp.resize(num_of_chars + 1);
	}

#if defined(_MSC_VER) && _MSC_VER > 1600
	vsprintf_s((char *)tmp.data(), tmp.capacity(), format, marker);
#else
	vsprintf((char *)tmp.data(), format, marker);
#endif
	return tmp;
}
std::wstring CStringHlp::FormatString(const wchar_t * format, ...)
{
	std::wstring tmp;
	va_list marker;
	va_start(marker, format);
#ifdef _MSC_VER
	size_t num_of_chars = _vscwprintf(format, marker);
#else
	size_t num_of_chars = vswprintf(nullptr, 0, format, marker);
#endif

	if (num_of_chars > tmp.capacity()) {
		tmp.resize(num_of_chars + 1);
	}
#if defined(_MSC_VER) && _MSC_VER > 1600
	vswprintf_s((wchar_t *)tmp.data(), tmp.capacity(), format, marker);
#else
	vswprintf((wchar_t *)tmp.data(), tmp.capacity(), format, marker);
#endif
	va_end(marker);

	return tmp;
}
std::string CStringHlp::FormatString(const char * format, ...)
{
	std::string tmp;

	va_list marker;
	va_start(marker, format);

#ifdef _MSC_VER
	size_t num_of_chars = _vscprintf(format, marker);
#else
	size_t num_of_chars = vsnprintf(nullptr, 0, format, marker);
#endif
	if (num_of_chars > tmp.capacity()) {
		tmp.resize(num_of_chars + 1);
	}

#if defined(_MSC_VER) && _MSC_VER > 1600
	vsprintf_s((char *)tmp.data(), tmp.capacity(), format, marker);
#else
	vsprintf((char *)tmp.data(), format, marker);
#endif

	va_end(marker);

	return tmp;
}

std::string CStringHlp::GetFileSizeStringAuto(long long byteSize) {
	std::string sizeStr;
	double size;
	if (byteSize >= 1073741824) {
		size = round(((float)byteSize / (float)1073741824) * 100.0f) / 100.0f;
		sizeStr = FormatString("%.2fG", size);
	}
	else if (byteSize >= 1048576) {
		size = round(((float)byteSize / 1048576) * 100.0f) / 100.0f;
		sizeStr = FormatString("%.2fM", size);
	}
	else {
		size = round(((float)byteSize / 1024) * 100.0f) / 100.0f;
		sizeStr = FormatString("%.2fK", size);
	}
	return sizeStr;
}

std::string CStringHlp::UnicodeToAnsi(const std::wstring& szStr)
{
#ifdef VR720_WINDOWS
	int nLen = WideCharToMultiByte(CP_ACP, 0, szStr.c_str(), -1, NULL, 0, NULL, NULL);
	if (nLen == 0)
		return NULL;
	std::string pResult;
	pResult.resize(nLen);
	WideCharToMultiByte(CP_ACP, 0, szStr.c_str(), -1, (LPSTR)pResult.data(), nLen, NULL, NULL);
	return pResult;
#else
	std::string pResult;
	int len = (int)szStr.size() + 1;

	if (len <= 1) return pResult;

	pResult.resize(len);

	/*这里的第三个长度参数，应为字节长度，即宽字符长度 * 4 */
	wcstombs((char*)pResult.data(), szStr.c_str(), len * sizeof(wchar_t));
	return pResult;
#endif
}
std::string CStringHlp::UnicodeToUtf8(const std::wstring& unicode)
{
#ifdef VR720_WINDOWS
	int len;
	len = WideCharToMultiByte(CP_UTF8, 0, unicode.c_str(), -1, NULL, 0, NULL, NULL);
	std::string  szUtf8;
	szUtf8.resize(len);
	WideCharToMultiByte(CP_UTF8, 0, unicode.c_str(), -1, (LPSTR)szUtf8.data(), len, NULL, NULL);
	return szUtf8;
#else
	//not implemented
    return std::string();
#endif
}
std::wstring CStringHlp::AnsiToUnicode(const std::string& szStr)
{
#ifdef VR720_WINDOWS
	int nLen = MultiByteToWideChar(CP_ACP, MB_PRECOMPOSED, szStr.c_str(), -1, NULL, 0);
	if (nLen == 0)
		return NULL;
	std::wstring  pResult;
	pResult.resize(nLen + 1);
	MultiByteToWideChar(CP_ACP, MB_PRECOMPOSED, szStr.c_str(), -1, (LPWSTR)pResult.data(), nLen);
	return pResult;
#else
	std::wstring pResult;
	size_t nLen = mbstowcs(nullptr, szStr.c_str(), 0) + 1;
	if (nLen <= 1) return pResult;

	pResult.resize(nLen + 1);

	mbstowcs((wchar_t*)pResult.data(), szStr.c_str(), nLen);
	return pResult;
#endif
}
std::wstring CStringHlp::Utf8ToUnicode(const std::string& szU8)
{
#ifdef VR720_WINDOWS
	int wcsLen = ::MultiByteToWideChar(CP_UTF8, NULL, szU8.c_str(), szU8.size(), NULL, 0);
	std::wstring wszString;
	wszString.resize(wcsLen + 1) ;
	::MultiByteToWideChar(CP_UTF8, NULL, szU8.c_str(), szU8.size(), (LPWSTR)wszString.data(), wcsLen);
	return wszString;
#else
    //not implemented
    return std::wstring();
#endif
}

#ifdef VR720_ANDROID

jstring CStringHlp::charTojstring(JNIEnv* env, const char* pStr) {
	int        strLen    = strlen(pStr);
	jclass     jstrObj   = env->FindClass("java/lang/String");
	jmethodID  methodId  = env->GetMethodID(jstrObj, "<init>", "([BLjava/lang/String;)V");
	jbyteArray byteArray = env->NewByteArray(strLen);
	jstring    encode    = env->NewStringUTF("utf-8");

	env->SetByteArrayRegion(byteArray, 0, strLen, (jbyte*)pStr);

	return (jstring)env->NewObject(jstrObj, methodId, byteArray, encode);
}
char* CStringHlp::jstringToChar(JNIEnv* env, jstring jstr) {
	char* rtn = nullptr;
	jclass clsstring = env->FindClass("java/lang/String");
	jstring strencode = env->NewStringUTF("utf-8");
	jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
	auto barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
	jsize alen = env->GetArrayLength(barr);
	jbyte* ba = env->GetByteArrayElements(barr, JNI_FALSE);
	if (alen > 0) {
		rtn = (char*) malloc(alen + 1);
		memcpy(rtn, ba, alen);
		rtn[alen] = 0;
	}
	env->ReleaseByteArrayElements(barr, ba, 0);
	return rtn;
}

#endif