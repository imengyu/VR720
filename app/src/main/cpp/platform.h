#pragma once
#ifndef PLATFORM_H
#define PLATFORM_H

//key map

#define VR720_KEY_LEFT 22
#define VR720_KEY_UP 21
#define VR720_KEY_RIGHT 19
#define VR720_KEY_DOWN 20
#define VR720_KEY_ESCAPE 111

#define MAX(a,b) a > b ? a : b
#define MIN(a,b) a < b ? a : b

#define UNREFERENCED_PARAMETER(P)          (P)

typedef struct tagRECT
{
    long left;
    long top;
    long right;
    long bottom;
} RECT, *PRECT, *NPRECT, *LPRECT;

#endif // !PLATFORM_H

