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
	LogLevelText = 0,
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
#define LOGI(tag,fmt) Logger::GetStaticInstance()->LogInfo(tag,fmt)
#define LOGW(tag,fmt) Logger::GetStaticInstance()->LogWarn2(tag,fmt, 0)
#define LOGE(tag,fmt) Logger::GetStaticInstance()->LogError2(tag,fmt, 0)
#define LOGD(tag,fmt) Logger::GetStaticInstance()->Log(tag,fmt)
#define LOGIF(tag,fmt, ...) Logger::GetStaticInstance()->LogInfo(tag,fmt, __VA_ARGS__)
#define LOGWF(tag,fmt, ...) Logger::GetStaticInstance()->LogWarn2(tag,fmt, __VA_ARGS__)
#define LOGEF(tag,fmt, ...) Logger::GetStaticInstance()->LogError2(tag,fmt, __VA_ARGS__)
#define LOGDF(tag,fmt, ...) Logger::GetStaticInstance()->Log(tag,fmt, __VA_ARGS__)

#define ALOGD(tag,...) __android_log_print(ANDROID_LOG_DEBUG, tag, __VA_ARGS__)
#define ALOGI(tag,...) __android_log_print(ANDROID_LOG_INFO, tag, __VA_ARGS__)
#define ALOGE(tag,...) __android_log_print(ANDROID_LOG_ERROR, tag, __VA_ARGS__)
#define ALOGW(tag,...) __android_log_print(ANDROID_LOG_WARN, tag, __VA_ARGS__)

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

	void Log(const char * tag, const char * str, ...);
	void LogWarn(const char * tag, const char * str, ...);
	void LogError(const char * tag, const char * str, ...);
	void LogInfo(const char * tag, const char * str, ...);

	void Log2(const char * tag, const char * str, const char* file, int line, const char* functon, ...);
	void LogWarn2(const char * tag, const char * str, const  char* file, int line, const char* functon, ...);
	void LogError2(const char * tag, const char * str, const char* file, int line, const char* functon, ...);
	void LogInfo2(const char * tag, const char * str, const char* file, int line, const char* functon, ...);

	LogLevel GetLogLevel();
	void SetLogLevel(LogLevel logLevel);
	void SetLogOutPut(LogOutPut output);
	void SetLogOutPutCallback(LogCallBack callback, void* lparam);
	void SetLogOutPutFile(const char *filePath);

	bool GetEnabled() const;
	void SetEnabled(bool enable);

	void SetWithWarp(bool e);
	void ResentNotCaputureLog();
private:
    std::list<LOG_SLA> logPendingBuffer;
    std::string logFilePath;
    std::string logTag;
    FILE *logFile = nullptr;
    LogLevel level = LogLevelText;
    bool withWarp = true;
	bool enabled = true;
    LogOutPut outPut = LogOutPutConsolne;
    LogCallBack callBack = nullptr;
    void* callBackData{};


	void WritePendingLog(const char *str, LogLevel logLevel);

	void LogInternalWithCodeAndLine(LogLevel logLevel, const char * tag, const char * str, const char*file, int line, const char*functon, va_list arg);
	void LogInternal(LogLevel logLevel, const char * tag, const char *str, va_list arg);
	void LogOutput(LogLevel logLevel, const char * tag, const char *str, const char *srcStr, size_t len);
	void CloseLogFile();


};

#define LogError2(tag, str, ...) LogError2(tag, str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)
#define LogWarn2(tag, str, ...) LogWarn2(tag, str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)
#define LogInfo2(tag, str, ...) LogInfo2(tag, str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)
#define Log2(tag, str, ...) Log2(tag, str, __FILE__, __LINE__, __FUNCTION__,__VA_ARGS__)

#endif

