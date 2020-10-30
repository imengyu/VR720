#pragma once
#ifndef LOHGGER_H
#define LOHGGER_H
#include "stdafx.h"
#include <stdio.h>
#include <stdarg.h>
#include <list>
#include <string>

//日志级别
enum LogLevel {
	//文字
	LogLevelText,
	//信息
	LogLevelInfo,
	//警告
	LogLevelWarn,
	//错误
	LogLevelError,
	//禁用
	LogLevelDisabled,
};
//日志输出位置
enum LogOutPut {
	//输出到系统默认控制台
	LogOutPutConsolne,
	//输出到文件
	LogOutPutFile,
	//输出到自定义回调
	LogOutPutCallback,
};

typedef void(*LogCallBack)(const vchar *str, LogLevel level, void* lparam);

//快速记录日志

#define LOG Logger::GetStaticInstance()
#define LOGI(fmt) Logger::GetStaticInstance()->LogInfo(fmt)
#define LOGW(fmt) Logger::GetStaticInstance()->LogWarn(fmt)
#define LOGE(fmt) Logger::GetStaticInstance()->LogError(fmt)
#define LOGD(fmt) Logger::GetStaticInstance()->Log(fmt)
#define LOGIF(fmt, ...) Logger::GetStaticInstance()->LogInfo(fmt, __VA_ARGS__)
#define LOGWF(fmt, ...) Logger::GetStaticInstance()->LogWarn(fmt, __VA_ARGS__)
#define LOGEF(fmt, ...) Logger::GetStaticInstance()->LogError(fmt, __VA_ARGS__)
#define LOGDF(fmt, ...) Logger::GetStaticInstance()->Log(fmt, __VA_ARGS__)

//日志记录
class Logger
{
public:

	struct LOG_SLA {
		vstring str;
		LogLevel level;
	};
    Logger(const vchar * tag);
    ~Logger();

    static void InitConst();
    static void DestroyConst();
    static Logger* GetStaticInstance();

	void Log(const vchar * str, ...);
	void LogWarn(const vchar * str, ...);
	void LogError(const vchar * str, ...);
	void LogInfo(const vchar * str, ...);

	void Log2(const vchar * str, const char* file, int line, const char* functon, ...);
	void LogWarn2(const vchar * str, const  char* file, int line, const char* functon, ...);
	void LogError2(const vchar * str, const char* file, int line, const char* functon, ...);
	void LogInfo2(const vchar * str, const char* file, int line, const char* functon, ...);

	LogLevel GetLogLevel();
	void SetLogLevel(LogLevel logLevel);
	void SetLogOutPut(LogOutPut output);
	void SetLogOutPutCallback(LogCallBack callback, void* lparam);
	void SetLogOutPutFile(const vchar *filePath);

	void ResentNotCaputureLog();
	void InitLogConsoleStdHandle();
private:
    std::list<LOG_SLA> logPendingBuffer;
    vstring logFilePath;
    vstring logTag;
    FILE *logFile = nullptr;
    LogLevel level = LogLevelInfo;
    LogOutPut outPut = LogOutPutConsolne;
    LogCallBack callBack = nullptr;
    void* callBackData{};
    void* hOutput = NULL;

	void LogOutputToStdHandle(LogLevel logLevel, const vchar* str, size_t len);
	void WritePendingLog(const vchar *str, LogLevel logLevel);

	void LogInternalWithCodeAndLine(LogLevel logLevel, const vchar * str, const char*file, int line, const char*functon, va_list arg);
	void LogInternal(LogLevel logLevel, const vchar *str, va_list arg);
	void LogOutput(LogLevel logLevel, const vchar *str, const vchar *srcStr, size_t len);
	void CloseLogFile();
};

#define LogError2(str, ...) LogError2(str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)
#define LogWarn2(str, ...) LogWarn2(str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)
#define LogInfo2(str, ...) LogInfo2(str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)
#define Log2(str, ...) Log2(str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)

#endif

