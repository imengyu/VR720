#version 300 es

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUv;

out vec2 TexCoord;
out vec3 Normal;
out vec3 FragPos;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform vec2 texTilling;
uniform vec2 texOffest;

void main() {
  gl_Position = projection * view * model * vec4(aPos, 1.0f);
  FragPos = vec3(model * vec4(aPos, 1.0f));
  TexCoord = aUv * texTilling + texOffest;
  Normal = normalize(mat3(transpose(inverse(model))) * aNormal);
}