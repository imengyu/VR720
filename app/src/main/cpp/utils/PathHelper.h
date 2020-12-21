#pragma once
#include "stdafx.h"
#include <string>

#define MAX_PATH 260

class Path
{
public:
	static std::string GetFileNameWithoutExtension(std::string path);
	static std::string GetExtension(std::string path);
	static bool IsPathRooted(std::string path1);
	static bool HasExtension(std::string path);
	static bool CheckInvalidPathChars(std::string path);
	static std::string GetFileName(std::string path);
	static std::string GetDirectoryName(const std::string& path);
	static bool IsValidateFolderFileName(std::string path);
	static bool RemoveQuotes(char* pathBuffer, size_t bufferSize);

	static std::string GetFileNameWithoutExtension(char* path);
	static std::string GetExtension(char* path);
	static bool IsPathRooted(char* path1);
	static bool HasExtension(char* path);
	static bool CheckInvalidPathChars(char* path);
	static std::string GetFileName(char* path);
	static std::string GetDirectoryName(char* path);
	static bool Exists(char* path1);
	static bool Exists(const std::string& path1);
};