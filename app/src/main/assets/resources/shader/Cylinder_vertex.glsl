#version 300 es

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUv;

out vec2 Spherical;
out vec2 TexCoord;

const float pi = 3.14159265358;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
  gl_Position = projection * view * vec4(aPos, 1.0f);
  TexCoord = aUv;
  Spherical.x = (aUv.x * 2.0) * pi;
  Spherical.y = (aUv.y - 0.5) * pi;
}