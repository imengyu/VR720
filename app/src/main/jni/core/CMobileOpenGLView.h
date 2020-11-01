//
// Created by roger on 2020/10/31.
//

#ifndef VR720_CMOBILEOPENGLVIEW_H
#define VR720_CMOBILEOPENGLVIEW_H

#include "stdafx.h"
#include "COpenGLView.h"

class COpenGLRenderer;
class CMobileOpenGLView : public COpenGLView {

public:
    CMobileOpenGLView(COpenGLRenderer* renderer);

    void Render();
    void RenderUI();

    void Update();

    bool Init() override;
    void Destroy() override;

    void Resize(int w, int h) override;

    void SetCurrentFPS(float fps) { currentFps = fps; }
    void ProcessMouseEvent(ViewMouseEventType event, float x, float y);
    void ProcessKeyEvent(int key, bool down);
    void ProcessZoomEvent(float v);

private:
    float currentFps = 0;
};


#endif //VR720_CMOBILEOPENGLVIEW_H
