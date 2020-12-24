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

bool CCTexture::Load(const char* path)
{
	int w, h, nrChannels;
    BYTE*data = stbi_load(path, &w, &h, &nrChannels, 0);
	if (data) {
		LOGIF("[CCTexture] Load texture %s : %dx%dx%db", path, w, h, nrChannels);
		if(nrChannels == 3)
			LoadRGB(data, w, h);
		else if(nrChannels == 4)
			LoadRGBA(data, w, h);
		stbi_image_free(data);
		return true;
	}
	else
        LOGWF("[CCTexture] Load texture %s failed : %s", path, stbi_failure_reason());
	return false;
}
bool CCTexture::Load(BYTE *buffer, size_t bufferSize) {
	if(!buffer || bufferSize <= 0) {
		LOGE("[CCTexture] Load had bad param!");
		return false;
	}
	int w, h, nrChannels;
	stbi_uc* data = stbi_load_from_memory(buffer, bufferSize, &w, &h, &nrChannels, 0);
	if (data) {
		LOGIF("[CCTexture] Load in memory : %dx%dx%db", buffer, w, h, nrChannels);
		if(nrChannels == 3)
			LoadRGB(data, w, h);
		else if(nrChannels == 4)
			LoadRGBA(data, w, h);
		stbi_image_free(data);
		return true;
	}else
		LOGEF("[CCTexture] Load in memory failed : %s", buffer, stbi_failure_reason());
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
		LOGE("[CCTexture] Load LoadBytes had bad param!");
		return;
	}

	//load dT
	LoadDataToGL(data, w, h, type);

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
	glBindTexture(textureType, texture);
}
void CCTexture::UnUse(GLenum type)
{
	glBindTexture(type, 0);
}

void CCTexture::DoBackupBufferData(BYTE* data, int w, int h, GLenum type)
{
	if (backupDataPtr) {
		free(backupDataPtr);
		backupDataPtr = nullptr;
	}

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
void CCTexture::LoadDataToGL(BYTE *data, int w, int h, GLenum type) {

	if(texture == 0)
		CreateGLTexture();

    glBindTexture(textureType, texture);
	glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

	alpha = type == GL_RGBA;

	glTexImage2D(textureType, 0, GL_RGB, (GLsizei) w, (GLsizei) h, 0, type, GL_UNSIGNED_BYTE, data);
    glBindTexture(textureType, 0);
}






