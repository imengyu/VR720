#include "CCShader.h"
#include "CStringHlp.h"
#include <string>
#include <fstream>
#include <sstream>
#include <iostream>

CCShader::CCShader(const vchar* vertexPath, const vchar* fragmentPath)
{
    // 1. retrieve the vertex/fragment source code from filePath
    vstring vertexCode;
    vstring fragmentCode;
#if WCHAR_API
    std::wifstream vShaderFile;
    std::wifstream fShaderFile;
    // ensure ifstream objects can throw exceptions:
    vShaderFile.exceptions(std::wifstream::failbit | std::wifstream::badbit);
    fShaderFile.exceptions(std::wifstream::failbit | std::wifstream::badbit);
    try
    {
        // open files
        vShaderFile.open(vertexPath);
        fShaderFile.open(fragmentPath);
        std::wstringstream vShaderStream, fShaderStream;
        // read file's buffer contents into streams
        vShaderStream << vShaderFile.rdbuf();
        fShaderStream << fShaderFile.rdbuf();
        // close file handlers
        vShaderFile.close();
        fShaderFile.close();
        // convert stream into string
        vertexCode = vShaderStream.str();
        fragmentCode = fShaderStream.str();
    }
    catch (std::wifstream::failure e)
    {
        LOGEF(_vstr("[CCShader] Read shader file failed! %s (%d)"), e.code().message().c_str(), e.code().value());
    }
    const char* vShaderCode = CStringHlp::UnicodeToAnsi(vertexCode).c_str();
    const char* fShaderCode = CStringHlp::UnicodeToAnsi(fragmentCode).c_str();
#else
    std::ifstream vShaderFile;
    std::ifstream fShaderFile;
    // ensure ifstream objects can throw exceptions:
    vShaderFile.exceptions(std::ifstream::failbit | std::ifstream::badbit);
    fShaderFile.exceptions(std::ifstream::failbit | std::ifstream::badbit);
    try
    {
        // open files
        vShaderFile.open(vertexPath);
        fShaderFile.open(fragmentPath);
        std::stringstream vShaderStream, fShaderStream;
        // read file's buffer contents into streams
        vShaderStream << vShaderFile.rdbuf();
        fShaderStream << fShaderFile.rdbuf();
        // close file handlers
        vShaderFile.close();
        fShaderFile.close();
        // convert stream into string
        vertexCode = vShaderStream.str();
        fragmentCode = fShaderStream.str();
    }
    catch (std::ifstream::failure e)
    {
        LOGEF(_vstr("[CCShader] Read shader file failed! %s (%d)"), e.code().message().c_str(), e.code().value());
    }
    const char* vShaderCode = vertexCode.c_str();
    const char* fShaderCode = fragmentCode.c_str();
#endif

    // 2. compile shaders
    unsigned int vertex, fragment;
    int success;
    char infoLog[512];

    // vertex Shader
    vertex = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertex, 1, &vShaderCode, nullptr);
    glCompileShader(vertex);
    // print compile errors if any
    glGetShaderiv(vertex, GL_COMPILE_STATUS, &success);
    if (!success)
    {
        glGetShaderInfoLog(vertex, 512, nullptr, infoLog);
#if WCHAR_API
        LOGEF(_vstr("[CCShader] Compile vertex shader file failed! \n%hs"), infoLog);
#else
        LOGEF(_vstr("[CCShader] Compile vertex shader file failed! \n%s"), infoLog);
#endif
    };

    // similiar for Fragment Shader
    fragment = glCreateShader(GL_FRAGMENT_SHADER);
    glShaderSource(fragment, 1, &fShaderCode, nullptr);
    glCompileShader(fragment);
    // print compile errors if any
    glGetShaderiv(fragment, GL_COMPILE_STATUS, &success);
    if (!success)
    {
        glGetShaderInfoLog(fragment, 512, nullptr, infoLog);
#if WCHAR_API
        LOGEF(_vstr("[CCShader] Compile fragment shader file failed! \n%hs"), infoLog);
#else
        LOGEF(_vstr("[CCShader] Compile fragment shader file failed! \n%s"), infoLog);
#endif

    };

    // shader Program
    ID = glCreateProgram();
    glAttachShader(ID, vertex);
    glAttachShader(ID, fragment);
    glLinkProgram(ID);
    // print linking errors if any
    glGetProgramiv(ID, GL_LINK_STATUS, &success);
    if (!success)
    {
        glGetProgramInfoLog(ID, 512, nullptr, infoLog);
#if WCHAR_API
        LOGEF(_vstr("[CCShader] Link shader program failed! \n%hs"), infoLog);
#else
        LOGEF(_vstr("[CCShader] Link shader program failed! \n%s"), infoLog);
#endif
    }

    // delete the shaders as they're linked into our program now and no longer necessary
    glDeleteShader(vertex);
    glDeleteShader(fragment);

    viewLoc = GetUniformLocation("view");
    projectionLoc = GetUniformLocation("projection");
    modelLoc = GetUniformLocation("model");
}
CCShader::~CCShader()
{
    glDeleteProgram(ID);
}

void CCShader::Use() const
{
    glUseProgram(ID);
}

GLint CCShader::GetUniformLocation(const char* name) const
{
    return glGetUniformLocation(ID, name);
}
GLint CCShader::GetUniformLocation(const std::string& name) const
{
    return glGetUniformLocation(ID, name.c_str());
}
void CCShader::SetBool(const std::string& name, bool value) const
{
    glUniform1i(glGetUniformLocation(ID, name.c_str()), (int)value);
}
void CCShader::SetInt(const std::string& name, int value) const
{
    glUniform1i(glGetUniformLocation(ID, name.c_str()), value);
}
void CCShader::SetFloat(const std::string& name, float value) const
{
    glUniform1f(glGetUniformLocation(ID, name.c_str()), value);
}
void CCShader::SetBool(GLint location, bool value) const
{
    glUniform1i(location, (int)value);
}
void CCShader::SetInt(GLint location, int value) const
{
    glUniform1i(location, value);
}
void CCShader::SetFloat(GLint location, float value) const
{
    glUniform1f(location, value);
}
