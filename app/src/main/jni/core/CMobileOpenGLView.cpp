//
// Created by roger on 2020/10/31.
//

#include "CMobileOpenGLView.h"
#include "CCamera.h"
#include "COpenGLRenderer.h"

bool CMobileOpenGLView::Init() {
    if(!ready) {
        LOGI("[OpenGLView] Init!");
        if (OpenGLRenderer && !OpenGLRenderer->Init()) {
            LOGE("[OpenGLView] OpenGLRenderer init failed!");
            return false;
        }
        ready = true;
    } else {
        LOGI("[OpenGLView] ReInit!");

        if (OpenGLRenderer && !OpenGLRenderer->ReInit()) {
            LOGE("[OpenGLView] OpenGLRenderer reinit failed!");
            return false;
        }
    }
    return true;
}
void CMobileOpenGLView::Destroy() {
    if(ready) {
        ready = false;
        LOGI("[OpenGLView] destroy!");
        if (OpenGLRenderer) {
            if(destroyWithForce)
                OpenGLRenderer->Destroy();
            else
                OpenGLRenderer->MarkDestroy();
            OpenGLRenderer = nullptr;
        }
        if (Camera) {
            delete Camera;
            Camera = nullptr;
        }
    }
}
void CMobileOpenGLView::Pause() {
    LOGI("[OpenGLView] Pause!");
}
void CMobileOpenGLView::Resume() {
    LOGI("[OpenGLView] Resume!");
}

void CMobileOpenGLView::Update() {
    if(ready && OpenGLRenderer)
        OpenGLRenderer->Update();
}

void CMobileOpenGLView::RenderUI() {
    if (ready && OpenGLRenderer) OpenGLRenderer->RenderUI();
}
void CMobileOpenGLView::Render() {
    if(!ready)
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
    if(!ready) return;
    if(down) HandleDownKey(key);
    else HandleUpKey(key);
}
void CMobileOpenGLView::ProcessMouseEvent(ViewMouseEventType event, float x, float y) {
    if(ready && mouseCallback) mouseCallback(this, x, y, 0, event);
}
void CMobileOpenGLView::ProcessZoomEvent(float v) {
    if(ready && scrollCallback) scrollCallback(this, v, v, 0, ViewMouseEventType::ViewMouseMouseWhell);
}

CMobileOpenGLView::CMobileOpenGLView(COpenGLRenderer *renderer) : COpenGLView(renderer) {

}

void CMobileOpenGLView::ManualDestroy() {
    destroyWithForce = true;
    Destroy();
}


