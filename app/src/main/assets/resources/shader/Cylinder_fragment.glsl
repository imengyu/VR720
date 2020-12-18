#version 300 es
#ifdef GL_ES
    precision mediump float;
#endif

out vec4 FragColor;
in vec2 Spherical;
in vec2 TexCoord;

uniform samplerCube cubeMap;

void main(){
    vec2 alpha = vec2(sin(Spherical.x), cos(Spherical.x));
    vec2 delta = vec2(sin(Spherical.y), cos(Spherical.y));
    vec3 cubeTexCoord = vec3(delta.y*alpha.x, delta.x, delta.y*alpha.y);
    FragColor = texture(cubeMap, cubeTexCoord);
}