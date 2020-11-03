#version 300 es

out vec4 FragColor;
in vec2 TexCoord;
in vec3 FragPos;

uniform sampler2D ourTexture;
uniform bool useColor;
uniform vec3 ourColor;

void main(){
  vec4 color = useColor ? vec4(ourColor, 1.0f) : texture(ourTexture, TexCoord);
  if(color.a < 0.1)
	discard;
  FragColor = color;
}