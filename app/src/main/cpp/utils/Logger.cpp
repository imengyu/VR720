#include "Logger.h"
#include "CStringHlp.h"
#include <ctime>

using namespace std;

#undef LogError2
#undef LogWarn2
#undef LogInfo2
#undef Log2

Logger::Logger(const char* tag)
{
	logTag = tag;
}
Logger::~Logger()
{
	CloseLogFile();
}

void Logger::Log(const char * tag, const char* str, ...)
{
	if (level <= LogLevelText) {
		va_list arg;
		va_start(arg, str);
		LogInternal(LogLevelText, tag, str, arg);
		va_end(arg);
	}
}
void Logger::LogWarn(const char * tag, const char* str, ...)
{
	if (level <= LogLevelWarn) {
		va_list arg;
		va_start(arg, str);
		LogInternal(LogLevelWarn, tag, str, arg);
		va_end(arg);
	}
}
void Logger::LogError(const char * tag, const char* str, ...)
{
	if (level <= LogLevelError) {
		va_list arg;
		va_start(arg, str);
		LogInternal(LogLevelError, tag, str, arg);
		va_end(arg);
	}
}
void Logger::LogInfo(const char * tag, const char* str, ...)
{
	if (level <= LogLevelInfo) {
		va_list arg;
		va_start(arg, str);
		LogInternal(LogLevelInfo, tag, str, arg);
		va_end(arg);
	}
}

void Logger::Log2(const char * tag, const char* str, const char* file, int line, const char* functon, ...)
{
	if (level <= LogLevelText) {
		va_list arg;
		va_start(arg, functon);
		LogInternalWithCodeAndLine(LogLevelText, tag, str, file, line, functon, arg);
		va_end(arg);
	}
}
void Logger::LogWarn2(const char * tag, const char* str, const char* file, int line, const char* functon, ...)
{
	if (level <= LogLevelWarn) {
		va_list arg;
		va_start(arg, functon);
		LogInternalWithCodeAndLine(LogLevelWarn, tag, str, file, line, functon, arg);
		va_end(arg);
	}
}
void Logger::LogError2(const char * tag, const char* str, const char* file, int line, const char* functon, ...)
{
	if (level <= LogLevelError) {
		va_list arg;
		va_start(arg, functon);
		LogInternalWithCodeAndLine(LogLevelError, tag, str, file, line, functon, arg);
		va_end(arg);
	}
}
void Logger::LogInfo2(const char * tag, const char* str, const char* file, int line, const  char* functon, ...)
{
	if (level <= LogLevelInfo) {
		va_list arg;
		va_start(arg, functon);
		LogInternalWithCodeAndLine(LogLevelInfo, tag, str, file, line, functon, arg);
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
void Logger::SetLogOutPutFile(const char* filePath)
{
	if (logFilePath != filePath)
	{
		CloseLogFile();
		logFilePath = filePath;
		logFile =  fopen(logFilePath.data(), "w");
	}
}
void Logger::SetLogOutPutCallback(LogCallBack callback, void* lparam)
{
	callBack = callback;
	callBackData = lparam;
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
void Logger::WritePendingLog(const char* str, LogLevel logLevel)
{
	LOG_SLA sla = { std::string(str), logLevel };
	logPendingBuffer.push_back(sla);
}

void Logger::LogInternalWithCodeAndLine(LogLevel logLevel, const char * tag, const char* str, const char* file, int line, const char* functon, va_list arg)
{
	std::string format1 = CStringHlp::FormatString("%s\n[In] %s:%d : %s", str, file, line, functon);
	LogInternal(logLevel, tag, format1.c_str(), arg);
}
void Logger::LogInternal(LogLevel logLevel, const char * tag, const char* str, va_list arg)
{
	std::string out = CStringHlp::FormatString(str, arg);
	LogOutput(logLevel, tag, out.c_str(), str, out.size());
}
void Logger::LogOutput(LogLevel logLevel, const char * tag, const char* str, const char* srcStr, size_t len)
{
	if (outPut == LogOutPutFile && logFile)
		fprintf(logFile, "[%s] %s", tag, str);
	else if (outPut == LogOutPutConsolne) {
		switch (logLevel)
		{
			case LogLevelInfo: __android_log_print(ANDROID_LOG_INFO, logTag.c_str(), "[%s] %s", tag, str); break;
			case LogLevelWarn: __android_log_print(ANDROID_LOG_WARN, logTag.c_str(), "[%s] %s", tag, str);  break;
			case LogLevelError: __android_log_print(ANDROID_LOG_ERROR, logTag.c_str(), "[%s] %s", tag, str);  break;
			case LogLevelText: __android_log_print(ANDROID_LOG_DEBUG, logTag.c_str(), "[%s] %s", tag, str);  break;
			default: __android_log_print(ANDROID_LOG_DEFAULT, logTag.c_str(), "[%s] %s", tag, str);  break;
		}
	}
	else if (outPut == LogOutPutCallback && callBack)
		callBack(str, logLevel, callBackData);
	else
		WritePendingLog(str, logLevel);
}
void Logger::CloseLogFile()
{
	if (logFile != nullptr) {
		fclose(logFile);
		logFile = nullptr;
	}
}

Logger globalStaticLogger = Logger("VR720Native");
Logger* Logger::GetStaticInstance() {
	return &globalStaticLogger;
}
void Logger::InitConst() {
	globalStaticLogger.SetLogOutPut(LogOutPutConsolne);
}
void Logger::DestroyConst() {
}

void Logger::SetWithWarp(bool e) {
	withWarp = e;
}

