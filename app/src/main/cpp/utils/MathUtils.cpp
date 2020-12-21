//
// Created by roger on 2020/12/14.
//

#include "MathUtils.h"

glm::vec3 MathUtils::toEulerAngle(const glm::quat &q) {

    float roll,pitch,yaw;

    // roll (x-axis rotation)
    float sinr_cosp = +2.0f * (q.w * q.x + q.y * q.z);
    float cosr_cosp = +1.0f - 2.0f * (q.x * q.x + q.y * q.y);
    roll = atan2(sinr_cosp, cosr_cosp);

    // pitch (y-axis rotation)
    float sinp = + 2.0f * (q.w * q.y - q.z * q.x);
    if (fabs(sinp) >= 1)
        pitch = copysign(M_PI / 2.0f, sinp); // use 90 degrees if out of range
    else
        pitch = asin(sinp);

    // yaw (z-axis rotation)
    float siny_cosp = +2.0f * (q.w * q.z + q.x * q.y);
    float cosy_cosp = +1.0f - 2.0f * (q.y * q.y + q.z * q.z);
    yaw = atan2(siny_cosp, cosy_cosp);

    return glm::vec3(pitch, yaw, roll);
}
