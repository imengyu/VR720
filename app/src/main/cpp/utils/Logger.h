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

typedef void(*LogCallBack)(const char *str, LogLevel level, void* lparam);

//快速记录日志

#define LOG Logger::GetStaticInstance()
#define LOGI(fmt) Logger::GetStaticInstance()->LogInfo(fmt)
#define LOGW(fmt) Logger::GetStaticInstance()->LogWarn2(fmt, 0)
#define LOGE(fmt) Logger::GetStaticInstance()->LogError2(fmt, 0)
#define LOGD(fmt) Logger::GetStaticInstance()->Log(fmt)
#define LOGIF(fmt, ...) Logger::GetStaticInstance()->LogInfo(fmt, __VA_ARGS__)
#define LOGWF(fmt, ...) Logger::GetStaticInstance()->LogWarn2(fmt, __VA_ARGS__)
#define LOGEF(fmt, ...) Logger::GetStaticInstance()->LogError2(fmt, __VA_ARGS__)
#define LOGDF(fmt, ...) Logger::GetStaticInstance()->Log(fmt, __VA_ARGS__)

//日志记录
class Logger
{
public:

	struct LOG_SLA {
		std::string str;
		LogLevel level;
	};
    Logger(const char * tag);
    ~Logger();

    static void InitConst();
    static void DestroyConst();
    static Logger* GetStaticInstance();

	void Log(const char * str, ...);
	void LogWarn(const char * str, ...);
	void LogError(const char * str, ...);
	void LogInfo(const char * str, ...);

	void Log2(const char * str, const char* file, int line, const char* functon, ...);
	void LogWarn2(const char * str, const  char* file, int line, const char* functon, ...);
	void LogError2(const char * str, const char* file, int line, const char* functon, ...);
	void LogInfo2(const char * str, const char* file, int line, const char* functon, ...);

	LogLevel GetLogLevel();
	void SetLogLevel(LogLevel logLevel);
	void SetLogOutPut(LogOutPut output);
	void SetLogOutPutCallback(LogCallBack callback, void* lparam);
	void SetLogOutPutFile(const char *filePath);

	void SetWithWarp(bool e);
	void ResentNotCaputureLog();
private:
    std::list<LOG_SLA> logPendingBuffer;
    std::string logFilePath;
    std::string logTag;
    FILE *logFile = nullptr;
    LogLevel level = LogLevelText;
    bool withWarp = true;
    LogOutPut outPut = LogOutPutConsolne;
    LogCallBack callBack = nullptr;
    void* callBackData{};

	void WritePendingLog(const char *str, LogLevel logLevel);

	void LogInternalWithCodeAndLine(LogLevel logLevel, const char * str, const char*file, int line, const char*functon, va_list arg);
	void LogInternal(LogLevel logLevel, const char *str, va_list arg);
	void LogOutput(LogLevel logLevel, const char *str, const char *srcStr, size_t len);
	void CloseLogFile();
};

#define LogError2(str, ...) LogError2(str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)
#define LogWarn2(str, ...) LogWarn2(str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)
#define LogInfo2(str, ...) LogInfo2(str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)
#define Log2(str, ...) Log2(str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)

#endif

