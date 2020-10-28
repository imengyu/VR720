#include "CColor.h"

CColor CColor::Black = CColor(0.0f, 0.0f, 0.0f);
CColor CColor::White = CColor(1.0f, 1.0f, 1.0f);

CColor::CColor()
{
}
CColor::CColor(float r, float g, float b)
{
    Set(r, g, b);
}
CColor::CColor(float r, float g, float b, float a)
{
    Set(r, g, b, a);
}
CColor::~CColor()
{
}

void CColor::Set(float r, float g, float b, float a)
{
    this->r = r;
    this->g = g;
    this->b = b;
    this->a = a;
}

CColor CColor::FromRGBA(float r, float g, float b)
{
    return CColor(r,g,b);
}
CColor CColor::FromRGBA(float r, float g, float b, float a)
{
    return CColor(r, g, b, a);
}
CColor CColor::FromString(const char* str)
{
    CColor c;
    int rx = 0, gx = 0, bx = 0, ax = 255;
    if (str[0] == '#') {
        if (strlen(str) > 8)
            sscanf_s(str, "#%02x%02x%02x%02x", &rx, &gx, &bx, &ax);
        else if (strlen(str) > 6)
            sscanf_s(str, "#%02x%02x%02x", &rx, &gx, &bx);  
        else if (strlen(str) > 3)
            sscanf_s(str, "#%01x%01x%01x", &rx, &gx, &bx);
    }
    c.Set(rx / 255.0f, gx / 255.0f, bx / 255.0f, ax / 255.0f);
    return c;
}
