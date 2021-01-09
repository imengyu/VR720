#include "CCTexture.h"
#include "CCRenderGlobal.h"
#include "../utils/CStringHlp.h"
#define STB_IMAGE_IMPLEMENTATION
#include "../imageloaders/stb_image.h"

CCTexture::CCTexture() = default;
CCTexture::CCTexture(GLuint type) {
	textureType = type;
}
CCTexture::~CCTexture()
{
	Destroy();
}

void CCTexture::LoadGridTexture(int w, int h, int gridSize, bool alpha, bool backup) {

	int compoents = (alpha ? 4 : 3);
	BYTE* data = (BYTE*)malloc(w * h * compoents);
	int gridWidth = w / gridSize;
	int gridHeight = h / gridSize;
	bool currentIsLight;
	bool currentHIsLight = false;
	int nextChangeW = 0, nextChangeH = 0;

	for(int currentH = 0; currentH < h; currentH++) {

		if (currentH == nextChangeH) {
			nextChangeH += gridHeight;
			currentHIsLight = !currentHIsLight;
		}

		nextChangeW = 0;
		currentIsLight = currentHIsLight;
		for(int currentW = 0; currentW < w; currentW++) {

			if (currentW == nextChangeW) {
				nextChangeW += gridWidth;
				currentIsLight = !currentIsLight;
			}

			int pos = (currentW + currentH * w) * compoents;
			data[pos] = currentIsLight ? 255 : 0;
			data[pos + 1] = 0;
			data[pos + 2] = currentIsLight ? 255 : 0;
			if(alpha)
				data[pos + 3] = 255;
		}

	}

	LoadBytes(data, w, h, (alpha ? GL_RGBA : GL_RGB));

	free(data);
}
bool CCTexture::Load(const char* path)
{
	int w, h, nrChannels;
    BYTE*data = stbi_load(path, &w, &h, &nrChannels, 0);
	if (data) {
		LOGIF(LOG_TAG, "Load texture %s : %dx%dx%db", path, w, h, nrChannels);
		if(nrChannels == 3)
			LoadBytes(data, w, h, GL_RGB);
		else if(nrChannels == 4)
			LoadBytes(data, w, h, GL_RGBA);
		else
			LOGEF(LOG_TAG, "Load texture failed : not support channels count %d", nrChannels);
		stbi_image_free(data);
		return true;
	}
	else
        LOGWF(LOG_TAG, "Load texture %s failed : %s", path, stbi_failure_reason());
	return false;
}
bool CCTexture::Load(BYTE *buffer, size_t bufferSize) {
	if(!buffer || bufferSize <= 0) {
		LOGE(LOG_TAG, "Load had bad param!");
		return false;
	}
	int w, h, nrChannels;
	stbi_uc* data = stbi_load_from_memory(buffer, bufferSize, &w, &h, &nrChannels, 0);
	if (data) {
		LOGDF(LOG_TAG, "Load in memory : %dx%dx%db", w, h, nrChannels);
		if(nrChannels == 3)
			LoadBytes(data, w, h, GL_RGB);
		else if(nrChannels == 4)
			LoadBytes(data, w, h, GL_RGBA);
		else
			LOGEF(LOG_TAG, "Load texture failed : not support channels count %d", nrChannels);
		stbi_image_free(data);
		return true;
	}else
		LOGEF(LOG_TAG, "Load in memory failed : %s", stbi_failure_reason());
	return false;
}
void CCTexture::LoadBytes(BYTE* data, int w, int h, GLenum format) {

	if(!data || w <= 0 || h <= 0) {
		LOGE(LOG_TAG, "Load LoadBytes had bad param!");
		return;
	}

	//load dT
	LoadDataToGL(data, w, h, format);

    this->width = w;
    this->height = h;

    //backup data
    if(backupData)
        DoBackupBufferData(data, w, h, format);
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
	glBindTexture(textureType, texture);
}
void CCTexture::UnUse(GLenum type)
{
	glBindTexture(type, 0);
}

void CCTexture::DoBackupBufferData(BYTE* data, int w, int h, GLenum format)
{
	size_t newBackupDataLength;

	switch (format) {
		case GL_RGB:
			newBackupDataLength = w * h * 3;
			break;
		case GL_RGBA:
			newBackupDataLength = w * h * 4;
			break;
		default:
			return;
	}

	if (backupDataPtr && newBackupDataLength != backupDataLength) {
		free(backupDataPtr);
		backupDataPtr = nullptr;
	}

	backupDataLength = newBackupDataLength;
    backupDataPtr = (BYTE*)malloc(backupDataLength);
    backupType = format;

    if(data != nullptr)
    	memcpy(backupDataPtr, data, backupDataLength);
}
void CCTexture::ReBufferData(bool reCreate)
{
	//Recreate texture
	if(reCreate)
		CreateGLTexture();

	//backup data
    if (backupDataPtr)
		LoadDataToGL(backupDataPtr, width, height, backupType);
}

void CCTexture::CreateGLTexture() {
	glGenTextures(1, &texture);
	glBindTexture(textureType, texture);

	if(textureType == GL_TEXTURE_CUBE_MAP) {

		//init empty cubemap
		for ( GLuint face = 0; face < 6; face++) {
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + face, 0, GL_RGB8,
						  cubeMapSize, cubeMapSize, 0, GL_RGB, GL_UNSIGNED_BYTE, nullptr);
		}

		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
	}
	else {

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapS);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	}

	glBindTexture(0, texture);
}
void CCTexture::LoadDataToGL(BYTE *data, int w, int h, GLenum format) {

	if (texture == 0 || !glIsTexture(texture))
		CreateGLTexture();

	glBindTexture(textureType, texture);
	glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

	alpha = format == GL_RGBA;

	glTexImage2D(textureType, 0, format, (GLsizei) w, (GLsizei) h, 0, format, GL_UNSIGNED_BYTE, data);
	GLenum error = glGetError();
	if (error != GL_NO_ERROR)
		LOGWF(LOG_TAG, "glTexImage2D() error : 0x%04x", error);

	glBindTexture(textureType, 0);

	loaded = true;
}






