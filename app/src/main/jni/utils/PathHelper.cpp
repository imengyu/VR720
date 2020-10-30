#include "stdafx.h"
#include "PathHelper.h"

#define DirectorySeparatorChar '\\'
#define AltDirectorySeparatorChar  '/'
#define VolumeSeparatorChar  ':'

Path::Path()
{
}
Path::~Path()
{
}

bool Path::RemoveQuotes(char *pathBuffer, size_t bufferSize)
{
	if (pathBuffer[0] == L'\"')
	{
		size_t size = strlen(pathBuffer);
		if (pathBuffer[size - 1] == L'\"')
		{
			for (size_t i = 1; i < size -1 && i < bufferSize; i++) {
				pathBuffer[i - 1] = pathBuffer[i];
			}
			pathBuffer[size - 1] = L'\0';
			pathBuffer[size - 2] = L'\0';
			return true;
		}
	}	
	return false;
}
bool Path::IsValidateFolderFileName(std::string path)
{
	bool ret = true;
	size_t u32Length = 0, u32Index = 0;
	wchar_t u8SpecialChar[] = { '\\','<','>','(',')','[',']','&',':',',','/','|','?','*' };
	wchar_t u8CtrlCharBegin = 0x0, u8CtrlCharEnd = 0x31;

	char* pName = (char*)path.c_str();
	if (pName == NULL)
		ret = false;
	else
	{
		u32Length = strlen(pName);
		if (u32Length >= MAX_PATH)
			ret = false;
	}

	for (u32Index = 0; (u32Index < u32Length) && (ret == 0);
		u32Index++)
	{
		if (u8CtrlCharBegin <= pName[u32Index] && pName[u32Index] <= u8CtrlCharEnd)
			ret = false;
		else if (wcschr(u8SpecialChar, pName[u32Index]) != NULL)
			ret = false;
	}
	return ret;
}
bool Path::CheckInvalidPathChars(std::string path)
{
	for (size_t i = 0; i < path.size(); i++)
	{
		int num = (int)(path)[i];
		if (num == 34 || num == 60 || num == 62 || num == 124 || num < 32)
		{
			return true;
		}
	}
	return false;
}
std::string Path::GetExtension(std::string path)
{
	if (!path.empty())	return std::string();
	if(Path::CheckInvalidPathChars(path))	return std::string();
	size_t length = path.size();
	size_t num = length;
	while (--num >= 0)
	{
		wchar_t c = (path)[num];
		if (c == L'.')
		{
			if (num != length - 1)
				return std::string(path.substr(num, length - num));
			return std::string();
		}
		else if (c == DirectorySeparatorChar || c == AltDirectorySeparatorChar || c == VolumeSeparatorChar)
		{
			break;
		}
	}
	return std::string();
}
bool Path::IsPathRooted(std::string path)
{
	if (!path.empty())
	{
		if (Path::CheckInvalidPathChars(path)) return false;
		size_t length = path.size();
		if ((length >= 1 && (path[0] == DirectorySeparatorChar || path[0] == AltDirectorySeparatorChar)) || (length >= 2 && path[1] == VolumeSeparatorChar))
		{
			return true;
		}
	}
	return false;
}
bool Path::HasExtension(std::string path)
{
	if (!path.empty())
	{
		if(Path::CheckInvalidPathChars(path)) 	return false;
		size_t num = path.size();
		while (--num >= 0)
		{
			wchar_t c = (path)[num];
			if (c == L'.')
			{
				return num != path.size() - 1;
			}
			if (c == DirectorySeparatorChar || c == AltDirectorySeparatorChar || c == VolumeSeparatorChar)
			{
				break;
			}
		}
	}
	return false;
}
std::string Path::GetFileNameWithoutExtension(std::string path)
{
	path = Path::GetFileName(path);
	if (!path.empty())
		return std::string();
	size_t length;
	if ((length = path.find_last_of(L'.')) == -1)
		return path;
	return  std::string(path.substr(0, length));
}
std::string Path::GetFileName(std::string path)
{
	if (!path.empty())
	{
		size_t length = path.size();
		size_t num = length;
		while (--num >= 0)
		{
			wchar_t c = (path)[num];
			if (c == DirectorySeparatorChar || c == AltDirectorySeparatorChar || c == VolumeSeparatorChar)
				return std::string(path.substr(num + 1, length - num - 1));
		}
	}
	return path;
}
std::string Path::GetDirectoryName(std::string path)
{
	if (!path.empty()) {
		char exeFullPath[MAX_PATH];
		strcpy(exeFullPath, path.c_str());
		char *pos = strrchr(exeFullPath, AltDirectorySeparatorChar);
		if(pos) *pos = '\0';
		return std::string(exeFullPath);
	}
	return std::string();
}

std::string Path::GetFileNameWithoutExtension(char* path)
{
	return GetFileNameWithoutExtension(std::string(path));
}
std::string Path::GetExtension(char* path)
{
	return GetExtension(std::string(path));
}
bool Path::IsPathRooted(char* path)
{
	return IsPathRooted(std::string(path));
}
bool Path::Exists(char* path)
{
	return access(path, 0) == 0;
}
bool Path::Exists(std::string path)
{
	return Exists(path.c_str());
}

bool Path::HasExtension(char* path)
{
	return HasExtension(std::string(path));
}
bool Path::CheckInvalidPathChars(char* path)
{
	return CheckInvalidPathChars(std::string(path));
}
std::string Path::GetFileName(char* path)
{
	return GetFileName(std::string(path));
}
std::string Path::GetDirectoryName(char* path)
{
	return GetDirectoryName(std::string(path));
}