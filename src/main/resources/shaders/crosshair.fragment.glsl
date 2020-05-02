#version 330 core

in VS_OUT {
    vec2 uv;
} fs_in;

out vec4 FragColor;

uniform sampler2D crosshairImage;

void main()
{
    vec4 texel = texture(crosshairImage, fs_in.uv);
    FragColor = vec4(texel);
}