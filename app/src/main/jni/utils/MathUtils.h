//
// Created by roger on 2020/12/14.
//

#include "stdafx.h"

#ifndef VR720_MATHUTILS_H
#define VR720_MATHUTILS_H

class MathUtils {
public:
    static glm::vec3 toEulerAngle(const glm::quat & q);
};


#endif //VR720_MATHUTILS_H
