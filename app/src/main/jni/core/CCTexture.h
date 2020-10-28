#pragma once
#include "stdafx.h"

//Ã˘Õº¿‡
class CCTexture
{
public:
	CCTexture();
	~CCTexture();

	bool Load(const wchar_t* path);
	bool Load(char* path);
	void LoadRGB(BYTE* data, int width, int height);
	void LoadBytes(BYTE* data, int width, int height, GLenum type);
	void LoadRGBA(BYTE* data, int width, int height);
	void Destroy();
	void Use();
	static void UnUse();	
	bool Loaded();

	bool alpha = false;
	int width = 0;
	int height = 0;
	GLuint texture = 0;
	GLuint wrapS = GL_REPEAT;
	GLuint wrapT = GL_REPEAT;
};

