#include "CCModel.h"
#include "CCMesh.h"
#include "CCMaterial.h"
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtx/euler_angles.hpp>
#include <gtx/quaternion.hpp>

CCModel::CCModel()
{
    CCModel::Reset();
}
CCModel::~CCModel() = default;

glm::vec3 CCModel::GetFront()  {
    if(VectorDirty)
        UpdateVectors();
    return mFront;
}
glm::vec3 CCModel::GetUp()  {
    if(VectorDirty)
        UpdateVectors();
    return mUp;
}
glm::vec3 CCModel::GetRight()  {
    if(VectorDirty)
        UpdateVectors();
    return mRight;
}
glm::mat4 CCModel::GetModelMatrix()
{
    if(VectorDirty) UpdateVectors();

    glm::mat4 model(1.0f);

    model = glm::rotate(model, glm::radians(mLocalEulerAngles.x), WorldRight);
    model = glm::rotate(model, glm::radians(mLocalEulerAngles.y), WorldUp);
    model = glm::rotate(model, glm::radians(mLocalEulerAngles.z), mLocalFront);

    model = glm::translate(model, Position);
    model = glm::scale(model, LocalScale);

    model = glm::rotate(model, glm::radians(mEulerAngles.x), mRight);
    model = glm::rotate(model, glm::radians(mEulerAngles.y), mUp);
    model = glm::rotate(model, glm::radians(mEulerAngles.z), mFront);

    return model;
}

void CCModel::UpdateVectors() {

    glm::mat4 mtx = glm::yawPitchRoll(
            glm::radians(mEulerAngles.x),
            glm::radians(mEulerAngles.y),
            glm::radians(mEulerAngles.z)
    );
    glm::mat4 mtxLocal = glm::yawPitchRoll(
            glm::radians(mLocalEulerAngles.x),
            glm::radians(mLocalEulerAngles.y),
            glm::radians(mLocalEulerAngles.z)
    );

    mUp = mtx * glm::vec4(0.0f, 1.0f, 0.0f, 1.0f);
    mFront = mtx * glm::vec4(0.0f, 0.0f, -1.0f, 1.0f);
    mRight = mtx * glm::vec4(1.0f, 0.0f, 0.0f, 1.0f);
    mLocalUp = mtxLocal * glm::vec4(0.0f, 1.0f, 0.0f, 1.0f);
    mLocalFront = mtxLocal * glm::vec4(0.0f, 0.0f, -1.0f, 1.0f);
    mLocalRight = mtxLocal * glm::vec4(1.0f, 0.0f, 0.0f, 1.0f);

    VectorDirty = false;
}
void CCModel::Reset() {
    Position = glm::vec3(0.0f);
    mRotation = glm::quat(0,0,0,0);
    mLocalRotation = glm::quat(0,0,0,0);
    mEulerAngles = glm::vec3(0.0f);
    mLocalEulerAngles = glm::vec3(0.0f);
}
void CCModel::Render() const
{
    if (!Visible) return;
    if (!Material.IsNullptr()) Material->Use();
    if (!Mesh.IsNullptr()) Mesh->RenderMesh();
}
void CCModel::ReBufferData() const {
    if (!Mesh.IsNullptr()) {
        Mesh->GenerateBuffer();
        Mesh->ReBufferData();
    }
}

void CCModel::SetRotation(glm::quat rotation) {
    mRotation = rotation;
    mEulerAngles = glm::degrees(glm::eulerAngles(mLocalRotation));
    VectorDirty = true;
}
void CCModel::SetLocalRotation(glm::quat rotation) {
    mLocalRotation = rotation;
    mLocalEulerAngles = glm::degrees(glm::eulerAngles(mLocalRotation));
    VectorDirty = true;
}
void CCModel::SetLocalEulerAngles(glm::vec3 eulerAngles) {

    eulerAngles.x = glm::mod(eulerAngles.x, 360.0f);
    eulerAngles.y = glm::mod(eulerAngles.y, 360.0f);
    eulerAngles.z = glm::mod(eulerAngles.z, 360.0f);

    glm::mat4x4 euler = glm::eulerAngleYXZ(
            glm::radians(eulerAngles.y),
            glm::radians(eulerAngles.x),
            glm::radians(eulerAngles.z));

    mLocalRotation = glm::toQuat(euler);
    mLocalEulerAngles = eulerAngles;

    VectorDirty = true;
}
void CCModel::SetEulerAngles(glm::vec3 eulerAngles) {

    eulerAngles.x = glm::mod(eulerAngles.x, 360.0f);
    eulerAngles.y = glm::mod(eulerAngles.y, 360.0f);
    eulerAngles.z = glm::mod(eulerAngles.z, 360.0f);

    glm::mat4x4 euler = glm::eulerAngleYXZ(
            glm::radians(eulerAngles.y),
            glm::radians(eulerAngles.x),
            glm::radians(eulerAngles.z));
    mRotation = glm::toQuat(euler);
    mEulerAngles = eulerAngles;

    VectorDirty = true;
}
void CCModel::SetPosition(glm::vec3 position) {
    Position = position;
}

glm::quat CCModel::GetLocalRotation() const { return mLocalRotation; }
glm::quat CCModel::GetRotation() const { return mRotation; }
glm::vec3 CCModel::GetLocalEulerAngles() const { return mLocalEulerAngles; }
glm::vec3 CCModel::GetEulerAngles() const { return mEulerAngles; }







