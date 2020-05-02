#version 330 core
layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aUV;

uniform vec2 frameSize;
uniform float crosshairSize;

out VS_OUT {
    vec2 uv;
} vs_out;

void main()
{
    vs_out.uv = aUV;
    gl_Position = vec4(crosshairSize * aPos.x / frameSize.x, crosshairSize * aPos.y / frameSize.y, -1.0, 1.0);
}