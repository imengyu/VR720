//
// Created by roger on 2020/10/31.
//

#include "CMobileOpenGLView.h"
#include "CCamera.h"
#include "COpenGLRenderer.h"

#define LOG_TAG "OpenGLView"

bool CMobileOpenGLView::Init() {
    if(!ready) {
        LOGI(LOG_TAG, "Init!");
        if (OpenGLRenderer && !OpenGLRenderer->Init()) {
            LOGE(LOG_TAG, "OpenGLRenderer init failed!");
            return false;
        }
        ready = true;
    } else {
        LOGI(LOG_TAG, "ReInit!");

        if (OpenGLRenderer && !OpenGLRenderer->ReInit()) {
            LOGE(LOG_TAG, "OpenGLRenderer reinit failed!");
            return false;
        }
    }
    return true;
}
void CMobileOpenGLView::Destroy() {
    if(ready) {
        ready = false;
        LOGI(LOG_TAG, "Destroy!");

        //Destroy Renderer
        if (OpenGLRenderer) {
            if (destroyWithForce) {
                LOGI(LOG_TAG, "Force destroy OpenGLRenderer");
                OpenGLRenderer->Destroy();
                OpenGLRenderer = nullptr;
            } else {
                LOGI(LOG_TAG, "Mark destroy OpenGLRenderer");
                OpenGLRenderer->MarkDestroy();
            }
        }
        //Destroy camera
        if (Camera && !IsManualDestroyCamera) {
            delete Camera;
            Camera = nullptr;
        }
        //Make a clear
        CCPtrPool::GetStaticPool()->ClearUnUsedPtr();
    }
}
void CMobileOpenGLView::ManualDestroy() {
    destroyWithForce = true;
    Destroy();
}
void CMobileOpenGLView::Pause() {
    LOGI(LOG_TAG, "Pause!");
}
void CMobileOpenGLView::Resume() {
    LOGI(LOG_TAG, "Resume!");
}

void CMobileOpenGLView::Update() {
    if(ready && OpenGLRenderer)
        OpenGLRenderer->Update();
}

void CMobileOpenGLView::RenderUI() {
    if (ready && OpenGLRenderer) OpenGLRenderer->RenderUI();
}
void CMobileOpenGLView::Render() {

    //绘制
    glClear(GL_COLOR_BUFFER_BIT);

    //清空
    if (Camera)
        glClearColor(Camera->Background.r, Camera->Background.g, Camera->Background.b,
                     Camera->Background.a);
    else
        glClearColor(0,0,0,0);

    if(!ready) {

        //Frames for destroy
        if(destroyFrame > 0) {
            destroyFrame--;
            if (OpenGLRenderer)
                OpenGLRenderer->Render(currentFps);
        } else OpenGLRenderer = nullptr;
        return;
    }

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

CMobileOpenGLView::CMobileOpenGLView(COpenGLRenderer *renderer) : COpenGLView(renderer) {}




