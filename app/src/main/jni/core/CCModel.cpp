#include "CCModel.h"
#include "CCMesh.h"
#include "CCMaterial.h"

CCModel::CCModel()
{
    Reset();
}
CCModel::~CCModel()
{
    if (Mesh) {
        delete Mesh;
        Mesh = nullptr;
    }
    if (Material) {
        delete Material;
        Material = nullptr;
    }
}

void CCModel::UpdateVectors()
{
    // 计算新的前向量
    glm::vec3 front;
    front.x = cos(glm::radians(Rotation.y)) * cos(glm::radians(Rotation.x));
    front.y = sin(glm::radians(Rotation.x));
    front.z = sin(glm::radians(Rotation.y)) * cos(glm::radians(Rotation.x));
    Front = glm::normalize(front);
    // 再计算右向量和上向量
    Right = glm::normalize(glm::cross(Front, WorldUp));  // 标准化
    Up = glm::normalize(glm::cross(Right, Front));
}
glm::mat4 CCModel::GetMatrix()
{    
    glm::mat4 model(1.0f);
    model = glm::translate(model, Positon);
    model = glm::rotate(model, glm::radians(Rotation.x), Right);
    model = glm::rotate(model, glm::radians(Rotation.y), Up);
    model = glm::rotate(model, glm::radians(Rotation.z), Front);
    return model;
}
void CCModel::Reset() {
    Positon = glm::vec3(0.0f);
    Rotation = glm::vec3(0.0f);
    UpdateVectors();
}

void CCModel::Render()
{
    if (!Visible) return;
    if (Material) Material->Use();
    if (Mesh) Mesh->RenderMesh();
}
