#pragma once
#ifndef VR720_CCSHADER_H
#define VR720_CCSHADER_H
#include "stdafx.h"

//Shader 封装类
class CCShader
{
public:
    // the program ID
    unsigned int ID;

    // constructor reads and builds the shader
    CCShader(const char* vertexCode, const char* fragmentCode);
    ~CCShader();

    // use/activate the shader
    void Use() const;

    //GetUniformLocation
    GLint GetUniformLocation(const char* name) const;
    GLint GetUniformLocation(const std::string& name) const;

    // utility uniform functions
    void SetBool(const std::string& name, bool value) const;
    void SetInt(const std::string& name, int value) const;
    void SetFloat(const std::string& name, float value) const;
    void SetBool(GLint location, bool value) const;
    void SetInt(GLint location, int value) const;
    void SetFloat(GLint location, float value) const;

    GLint viewLoc = -1;
    GLint projectionLoc = -1;
    GLint modelLoc = -1;
};

#endif
