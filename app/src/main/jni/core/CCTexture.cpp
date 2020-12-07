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

bool CCTexture::Load(const char* path)
{
	int w, h, nrChannels;
    BYTE*data = stbi_load(path, &w, &h, &nrChannels, 0);
	if (data) {
		LOGIF("Load texture %s : %dx%dx%db", path, w, h, nrChannels);
		if(nrChannels == 3)
			LoadRGB(data, w, h);
		else if(nrChannels == 4)
			LoadRGBA(data, w, h);
		stbi_image_free(data);
		return true;
	}
	else
        LOGWF("Load texture %s failed : %s", path, stbi_failure_reason());
	return false;
}

bool CCTexture::Load(BYTE *buffer, size_t bufferSize) {
	if(!buffer || bufferSize <= 0) {
		LOGE("CCTexture::Load() had bad param!");
		return false;
	}
	int w, h, nrChannels;
	stbi_uc* data = stbi_load_from_memory(buffer, bufferSize, &w, &h, &nrChannels, 0);
	if (data) {
		LOGIF("CCTexture::Load() in memory %X : %dx%dx%db", buffer, w, h, nrChannels);
		if(nrChannels == 3)
			LoadRGB(data, w, h);
		else if(nrChannels == 4)
			LoadRGBA(data, w, h);
		stbi_image_free(data);
		return true;
	}else
		LOGEF("Texture::Load() in memory %X failed : %s", buffer, stbi_failure_reason());
	return false;
}
void CCTexture::LoadRGB(BYTE* data, int w, int h)
{
	LoadBytes(data, w, h, GL_RGB);
}
void CCTexture::LoadRGBA(BYTE* data, int w, int h)
{
	LoadBytes(data, w, h, GL_RGBA);
}
void CCTexture::LoadBytes(BYTE* data, int w, int h, GLenum type) {

	if(!data || w <= 0 || h <= 0) {
		LOGE("CCTexture::LoadBytes() had bad param!");
		return;
	}

	//load dT
    LoadToGl(data, w, h, type);

    this->width = w;
    this->height = h;

    //backup data
    if(backupData)
        DoBackupBufferData(data, w, h, type);
}

void CCTexture::Destroy()
{
    if (backupDataPtr) {
        free(backupDataPtr);
        backupDataPtr = nullptr;
    }
	if (texture >= 0)
		glDeleteTextures(1, &texture);
}
void CCTexture::Use() const
{
	glBindTexture(GL_TEXTURE_2D, texture);
}
void CCTexture::UnUse()
{
	glBindTexture(GL_TEXTURE_2D, 0);
}
bool CCTexture::Loaded() const
{
	return texture > 0;
}

void CCTexture::DoBackupBufferData(BYTE* data, int w, int h, GLenum type)
{
    switch (type) {
        case GL_RGB:
            backupDataLength = w * h * 3;
            break;
        case GL_RGBA:
            backupDataLength = w * h * 4;
            break;
        default:
            return;
    }

    backupDataPtr = (BYTE*)malloc(backupDataLength);
    backupType = type;

    memcpy(backupDataPtr, data, backupDataLength);
}
void CCTexture::ReBufferData()
{
    if (backupDataPtr)
        LoadToGl(backupDataPtr, width, height, backupType);
}
void CCTexture::LoadToGl(BYTE *data, int w, int h, GLenum type) {
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

    alpha = type == GL_RGBA;

    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, (GLsizei)w, (GLsizei)h, 0, type, GL_UNSIGNED_BYTE, data);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapS);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    //glGenerateMipmap(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, 0);
}


