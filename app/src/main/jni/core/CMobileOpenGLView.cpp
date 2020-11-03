//
// Created by roger on 2020/10/31.
//

#include "CMobileOpenGLView.h"
#include "CCamera.h"
#include "COpenGLRenderer.h"

bool CMobileOpenGLView::Init() {
    if (OpenGLRenderer && !OpenGLRenderer->Init()) {
        LOGE(_vstr("OpenGLRenderer init failed!"));
        return false;
    }
    reday = true;
    return true;
}
void CMobileOpenGLView::Destroy() {
    if(reday) {
        reday = false;
        if (OpenGLRenderer) {
            OpenGLRenderer->Destroy();
            OpenGLRenderer = nullptr;
        }
        if (Camera) {
            delete Camera;
            Camera = nullptr;
        }
        COpenGLView::Destroy();
    }
}

void CMobileOpenGLView::Update() {
    if(reday && OpenGLRenderer)
        OpenGLRenderer->Update();
}

void CMobileOpenGLView::RenderUI() {
    if (reday && OpenGLRenderer) OpenGLRenderer->RenderUI();
}
void CMobileOpenGLView::Render() {
    if(!reday)
        return;

    //绘制
    glClear(GL_COLOR_BUFFER_BIT);

    //清空
    if (Camera)
        glClearColor(Camera->Background.r, Camera->Background.g, Camera->Background.b,
                     Camera->Background.a);

    //绘制
    if (OpenGLRenderer) OpenGLRenderer->Render(currentFps);

    //绘制界面
    RenderUI();
}

void CMobileOpenGLView::Resize(int w, int h) {
    COpenGLView::Resize(w, h);
    if (OpenGLRenderer) OpenGLRenderer->Resize(Width, Height);
}

void CMobileOpenGLView::ProcessKeyEvent(int key, bool down) {
    if(!reday) return;
    if(down) HandleDownKey(key);
    else HandleUpKey(key);
}
void CMobileOpenGLView::ProcessMouseEvent(ViewMouseEventType event, float x, float y) {
    if(reday && mouseCallback) mouseCallback(this, x, y, 0, event);
}
void CMobileOpenGLView::ProcessZoomEvent(float v) {
    if(reday && scrollCallback) scrollCallback(this, v, v, 0, ViewMouseEventType::ViewMouseMouseWhell);
}

CMobileOpenGLView::CMobileOpenGLView(COpenGLRenderer *renderer) : COpenGLView(renderer) {

}

void CMobileOpenGLView::ManualDestroy() {
    Destroy();
}
