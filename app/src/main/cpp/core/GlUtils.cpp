#include "stdafx.h"
#include "GlUtils.h"

/**
 * @brief 一维线性映射函数，将[a,b]中的点x，映射到[a1,b1]中的点x1
 *
 **/
float LinearMap(float x, float a, float b, float a1, float b1)
{
	auto srcDelt(b - a);
	if (FuzzyIsZero(srcDelt))
	{
		// 原始范围时0,
		//LOG_WARN("原始范围不能为0(%f,%f)", a, b);
		return a1;
	}

	return (b1 - a1) / srcDelt * (x - a) + a1;
}