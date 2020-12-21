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

    void Pause() override;
    void Resume() override;

    bool Init() override;
    void Destroy() override;

    void Resize(int w, int h) override;

    void SetCurrentFPS(float fps) { currentFps = fps; }
    void ProcessMouseEvent(ViewMouseEventType event, float x, float y);
    void ProcessKeyEvent(int key, bool down);
    void ProcessZoomEvent(float v);

    void ManualDestroy();

private:
    float currentFps = 0;
    int destroyFrame = 3;
    bool ready = false;
    bool pause = false;
    bool destroyWithForce = false;
};


#endif //VR720_CMOBILEOPENGLVIEW_H
