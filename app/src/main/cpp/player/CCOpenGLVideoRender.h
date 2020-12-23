//
// Created by roger on 2020/12/22.
//

#ifndef VR720_CCOPENGLVIDEORENDER_H
#define VR720_CCOPENGLVIDEORENDER_H
#include "CCPlayerRender.h"
#include "../core/CCPanoramaRenderer.h"

class CCOpenGLVideoRender : public CCPlayerRender {

public:
    CCOpenGLVideoRender(CCPanoramaRenderer * renderer);

protected:
    CCVideoDevice* CreateVideoDevice() override ;
    CCPanoramaRenderer * renderer;
};


#endif //VR720_CCOPENGLVIDEORENDER_H
