#include "Logger.h"
#include "CStringHlp.h"
#include <ctime>

using namespace std;

#undef LogError2
#undef LogWarn2
#undef LogInfo2
#undef Log2

Logger::Logger(const vchar* tag)
{
	logTag = tag;
}
Logger::~Logger()
{
	CloseLogFile();
}

void Logger::Log(const vchar* str, ...)
{
	if (level <= LogLevelText) {
		va_list arg;
		va_start(arg, str);
		LogInternal(LogLevelText, str, arg);
		va_end(arg);
	}
}
void Logger::LogWarn(const vchar* str, ...)
{
	if (level <= LogLevelWarn) {
		va_list arg;
		va_start(arg, str);
		LogInternal(LogLevelWarn, str, arg);
		va_end(arg);
	}
}
void Logger::LogError(const vchar* str, ...)
{
	if (level <= LogLevelError) {
		va_list arg;
		va_start(arg, str);
		LogInternal(LogLevelError, str, arg);
		va_end(arg);
	}
}
void Logger::LogInfo(const vchar* str, ...)
{
	if (level <= LogLevelInfo) {
		va_list arg;
		va_start(arg, str);
		LogInternal(LogLevelInfo, str, arg);
		va_end(arg);
	}
}

void Logger::Log2(const vchar* str, const char* file, int line, const char* functon, ...)
{
	if (level <= LogLevelText) {
		va_list arg;
		va_start(arg, functon);
		LogInternalWithCodeAndLine(LogLevelText, str, file, line, functon, arg);
		va_end(arg);
	}
}
void Logger::LogWarn2(const vchar* str, const char* file, int line, const char* functon, ...)
{
	if (level <= LogLevelWarn) {
		va_list arg;
		va_start(arg, functon);
		LogInternalWithCodeAndLine(LogLevelWarn, str, file, line, functon, arg);
		va_end(arg);
	}
}
void Logger::LogError2(const vchar* str, const char* file, int line, const char* functon, ...)
{
	if (level <= LogLevelError) {
		va_list arg;
		va_start(arg, functon);
		LogInternalWithCodeAndLine(LogLevelError, str, file, line, functon, arg);
		va_end(arg);
	}
}
void Logger::LogInfo2(const vchar* str, const char* file, int line, const  char* functon, ...)
{
	if (level <= LogLevelInfo) {
		va_list arg;
		va_start(arg, functon);
		LogInternalWithCodeAndLine(LogLevelInfo, str, file, line, functon, arg);
		va_end(arg);
	}
}

void Logger::SetLogLevel(LogLevel logLevel)
{
	this->level = logLevel;
}
LogLevel Logger::GetLogLevel() {
	return this->level;
}
void Logger::SetLogOutPut(LogOutPut output)
{
	this->outPut = output;
}
void Logger::SetLogOutPutFile(const vchar* filePath)
{
	if (logFilePath != filePath)
	{
		CloseLogFile();
		logFilePath = filePath;
#if defined(_MSC_VER) && _MSC_VER > 1600
		_v_fopen_s(&logFile, (vchar*)logFilePath.data(), _vstr("w"));
#else
		logFile = _v_fopen(logFilePath.data(), _vstr("w"));
#endif
	}
}
void Logger::SetLogOutPutCallback(LogCallBack callback, void* lparam)
{
	callBack = callback;
	callBackData = lparam;
}

void Logger::InitLogConsoleStdHandle() {
#if defined(VR720_WINDOWS)
	hOutput = GetStdHandle(STD_OUTPUT_HANDLE);
#endif
}
void Logger::LogOutputToStdHandle(LogLevel logLevel, const vchar* str, size_t len) {
#if defined(VR720_WINDOWS)
	switch (logLevel)
	{
	case LogLevelInfo:
		SetConsoleTextAttribute(hOutput, FOREGROUND_INTENSITY | FOREGROUND_BLUE);
		break;
	case LogLevelWarn:
		SetConsoleTextAttribute(hOutput, FOREGROUND_INTENSITY | FOREGROUND_RED | FOREGROUND_GREEN);
		break;
	case LogLevelError:
		SetConsoleTextAttribute(hOutput, FOREGROUND_INTENSITY | FOREGROUND_RED);
		break;
	case LogLevelText:
		SetConsoleTextAttribute(hOutput, FOREGROUND_INTENSITY | FOREGROUND_RED |
			FOREGROUND_GREEN |
			FOREGROUND_BLUE);
		break;
	}
	WriteConsoleW(hOutput, str, len, NULL, NULL);
#endif
}

void Logger::ResentNotCaputureLog()
{
	if (outPut == LogOutPutCallback && callBack) {
		std::list< LOG_SLA>::iterator i;
		for (i = logPendingBuffer.begin(); i != logPendingBuffer.end(); i++)
			callBack((*i).str.c_str(), (*i).level, callBackData);
		logPendingBuffer.clear();
	}
}
void Logger::WritePendingLog(const vchar* str, LogLevel logLevel)
{
	LOG_SLA sla = { vstring(str), logLevel };
	logPendingBuffer.push_back(sla);
}

void Logger::LogInternalWithCodeAndLine(LogLevel logLevel, const vchar* str, const char* file, int line, const char* functon, va_list arg)
{
#if WCHAR_API
	vstring format1 = CStringHlp::FormatString(_vstr("%s\n[In] %hs (%d) : %hs"), str, file, line, functon);
#else
	vstring format1 = CStringHlp::FormatString(_vstr("%s\n[In] %s (%d) : %s"), str, file, line, functon);
#endif
	LogInternal(logLevel, format1.c_str(), arg);
}
void Logger::LogInternal(LogLevel logLevel, const vchar* str, va_list arg)
{
	const vchar* levelStr;
	switch (logLevel)
	{
		case LogLevelInfo: levelStr = _vstr("I"); break;
		case LogLevelWarn: levelStr = _vstr("W"); break;
		case LogLevelError: levelStr = _vstr("E"); break;
		case LogLevelText: levelStr = _vstr("T"); break;
		default: levelStr = _vstr(""); break;
	}
	time_t time_log = time(NULL);
#if defined(_MSC_VER) && _MSC_VER > 1600
	struct tm tm_log;
	localtime_s(&tm_log, &time_log);
	vstring format1 = CStringHlp::FormatString(_vstr("[%02d:%02d:%02d] [%s] %s\n"), tm_log.tm_hour, tm_log.tm_min, tm_log.tm_sec, levelStr, str);
#else
	tm* tm_log = localtime(&time_log);
	vstring format1 = CStringHlp::FormatString(_vstr("%s/%s:%02d:%02d:%02d %s\n"), logTag.c_str(), levelStr, tm_log->tm_hour, tm_log->tm_min, tm_log->tm_sec, str);
#endif
	vstring out = CStringHlp::FormatString(format1.c_str(), arg);
	LogOutput(logLevel, out.c_str(), str, out.size());
}
void Logger::LogOutput(LogLevel logLevel, const vchar* str, const vchar* srcStr, size_t len)
{
#if defined(VR720_WINDOWS)
	#if _DEBUG
	OutputDebugString(str);
#else
	if (outPut == LogOutPutConsolne)
		OutputDebugString(str);
#endif
#endif
	if (outPut == LogOutPutFile && logFile)
		_v_fprintf(logFile, _vstr("%s"), str);
	else if (outPut == LogOutPutConsolne) {
#if defined(VR720_WINDOWS)
		if (hOutput != NULL) LogOutputToStdHandle(logLevel, str, len);
		else _v_printf(_vstr("%s"), str);
#elif defined(VR720_ANDROID)
		switch (logLevel)
		{
			case LogLevelInfo: __android_log_print(ANDROID_LOG_INFO, logTag.c_str(), "%s", srcStr); break;
			case LogLevelWarn: __android_log_print(ANDROID_LOG_WARN, logTag.c_str(), "%s", srcStr);  break;
			case LogLevelError: __android_log_print(ANDROID_LOG_ERROR, logTag.c_str(), "%s", srcStr);  break;
			case LogLevelText: __android_log_print(ANDROID_LOG_VERBOSE, logTag.c_str(), "%s", srcStr);  break;
			default: __android_log_print(ANDROID_LOG_DEBUG, logTag.c_str(), "%s", srcStr);  break;
		}
#else
		printf(_vstr("%s"), str);
#endif
	}
	else if (outPut == LogOutPutCallback && callBack)
		callBack(str, logLevel, callBackData);
	else
		WritePendingLog(str, logLevel);
}
void Logger::CloseLogFile()
{
	if (logFile) {
		fclose(logFile);
		logFile = nullptr;
	}
}

Logger* globalStaticLogger = nullptr;

Logger* Logger::GetStaticInstance() { return globalStaticLogger; }
void Logger::InitConst() { globalStaticLogger = new Logger(_vstr("App")); }
void Logger::DestroyConst() { delete globalStaticLogger; }


