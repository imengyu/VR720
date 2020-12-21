#pragma once
#include "stdafx.h"

inline constexpr float  FLOAT_PRECISION_EPSILON()
{
	return 1.0e-6f;
}

inline bool FuzzyIsZero(float f)
{
	return std::abs(f) < FLOAT_PRECISION_EPSILON();
}

float LinearMap(float x, float a, float b, float a1, float b1);
