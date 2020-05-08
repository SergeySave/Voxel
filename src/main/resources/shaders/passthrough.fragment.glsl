#version 330 core

uniform sampler2D inputTexture;

out vec4 FragColor;

void main() {
    ivec2 C = ivec2(gl_FragCoord.xy);

    FragColor = texelFetch(inputTexture, C, 0);
}