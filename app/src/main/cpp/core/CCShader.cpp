#include "CCShader.h"
#include "../utils/CStringHlp.h"
#include <string>
#include <fstream>
#include <sstream>
#include <iostream>

CCShader::CCShader(const char* vShaderCode, const char* fShaderCode)
{
    unsigned int vertex, fragment;
    int success;
    char infoLog[512];

    //LOGIF("vShaderCode : \n%s", vShaderCode);
    //LOGIF("fShaderCode : \n%s", fShaderCode);

    // vertex Shader
    vertex = glCreateShader(GL_VERTEX_SHADER);
    glShaderSource(vertex, 1, &vShaderCode, nullptr);
    glCompileShader(vertex);
    // print compile errors if any
    glGetShaderiv(vertex, GL_COMPILE_STATUS, &success);
    if (!success)
    {
        glGetShaderInfoLog(vertex, 512, nullptr, infoLog);
        LOGDF(LOG_TAG, "D: vShaderCode : \n%s", vShaderCode);
        LOGEF(LOG_TAG, "Compile vertex shader file failed! \n%s", infoLog);
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
        LOGDF(LOG_TAG, "D: fShaderCode : \n%s", fShaderCode);
        LOGEF(LOG_TAG, "Compile fragment shader file failed! \n%s", infoLog);
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
        LOGEF(LOG_TAG, "Link shader program failed! \n%s", infoLog);
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
