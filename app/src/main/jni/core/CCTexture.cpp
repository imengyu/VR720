#include "CCTexture.h"
#include "CCRenderGlobal.h"
#include "CStringHlp.h"
#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

CCTexture::CCTexture() = default;
CCTexture::~CCTexture()
{
	Destroy();
}

bool CCTexture::Load(const vchar* path)
{
	int w, h, nrChannels;
#if WCHAR_API
    BYTE*data = stbi_load(CStringHlp::UnicodeToAnsi(vstring(path)).c_str(), &w, &h, &nrChannels, 0);
#else
    BYTE*data = stbi_load(path, &w, &h, &nrChannels, 0);
#endif
	if (data) {
		LOGIF(_vstr("Load texture %s : %dx%dx%db"), path, w, h, nrChannels);
		if(nrChannels == 3)
			LoadRGB(data, w, h);
		else if(nrChannels == 4)
			LoadRGBA(data, w, h);
		stbi_image_free(data);
		return true;
	}
	else
#if WCHAR_API
        LOGWF(_vstr("Load texture %s failed : %hs"), path, stbi_failure_reason());
#else
        LOGWF(_vstr("Load texture %s failed : %s"), path, stbi_failure_reason());
#endif

	return false;
}

bool CCTexture::Load(BYTE *buffer, size_t bufferSize) {
	if(!buffer || bufferSize <= 0) {
		LOGE(_vstr("CCTexture::Load() had bad param!"));
		return false;
	}
	int w, h, nrChannels;
	stbi_uc* data = stbi_load_from_memory(buffer, bufferSize, &w, &h, &nrChannels, 0);
	if (data) {
		LOGIF(_vstr("CCTexture::Load() in memory %X : %dx%dx%db"), buffer, w, h, nrChannels);
		if(nrChannels == 3)
			LoadRGB(data, w, h);
		else if(nrChannels == 4)
			LoadRGBA(data, w, h);
		stbi_image_free(data);
		return true;
	}else
		LOGEF(_vstr("CCTexture::Load() in memory %X failed : %s"), buffer, stbi_failure_reason());
	return false;
}
void CCTexture::LoadRGB(BYTE* data, int width, int height)
{
	LoadBytes(data, width, height, GL_RGB);
}
void CCTexture::LoadRGBA(BYTE* data, int width, int height)
{
	LoadBytes(data, width, height, GL_RGBA);
}
void CCTexture::LoadBytes(BYTE* data, int width, int height, GLenum type) {

	if(!data || width <= 0 || height <= 0) {
		LOGE(_vstr("CCTexture::LoadBytes() had bad param!"));
		return;
	}

	glGenTextures(1, &texture);
	glBindTexture(GL_TEXTURE_2D, texture);
	glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

	alpha = type == GL_RGBA;

	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, (GLsizei)width, (GLsizei)height, 0, type, GL_UNSIGNED_BYTE, data);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapS);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapT);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

	//glGenerateMipmap(GL_TEXTURE_2D);
	glBindTexture(GL_TEXTURE_2D, 0);

	this->width = width;
	this->height = height;
}

void CCTexture::Destroy()
{
	if (texture >= 0)
		glDeleteTextures(1, &texture);
}
void CCTexture::Use() const
{
	glBindTexture(GL_TEXTURE_2D, texture);
	glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, (alpha ? GL_REPLACE : GL_MODULATE));
}
void CCTexture::UnUse()
{
	glBindTexture(GL_TEXTURE_2D, 0);
}

bool CCTexture::Loaded() const
{
	return texture > 0;
}

