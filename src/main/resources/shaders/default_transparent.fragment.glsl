#version 330 core
in VS_OUT {
    vec3 pos;
    vec2 uv;
    vec3 lighting;
//    uint atlas;
    vec4 atlasData;
} fs_in;

out vec4 FragColor;

uniform sampler2D atlasPage0;

void main()
{
    float u = fs_in.uv.x;
    u -= floor(u);
    if (u < 0.0) {
        u += 1.0;
    }
    float v = fs_in.uv.y;
    v -= floor(v);
    if (v < 0.0) {
        v += 1.0;
    }
    vec3 lighting = fs_in.lighting;
    vec2 coord = vec2(fs_in.atlasData.x + u * fs_in.atlasData.z, fs_in.atlasData.y + v * fs_in.atlasData.w);
    vec4 texel = texture(atlasPage0, coord);
    if (texel.a >= 1.0) {
        discard;
    }
    FragColor = vec4(lighting * texel.rgb, texel.a);
}