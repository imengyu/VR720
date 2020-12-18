#version 300 es
#ifdef GL_ES
    precision mediump float;
#endif

out vec4 FragColor;
in vec2 TexCoord;
in vec3 FragPos;

uniform sampler2D ourTexture;

void main(){
  vec4 color = texture(ourTexture, TexCoord);
  if(color.a < 0.1f)
	discard;
  FragColor = color;
}