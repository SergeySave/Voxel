#version 330 core
layout (location = 0) in uvec3 packedVertexData;

uniform mat4 uCamera;
uniform mat4 uModel;

uniform usampler2D assetData;
uniform sampler2D atlasPage0;

out VS_OUT {
    vec3 pos;
    vec2 uv;
    vec3 lighting;
//    uint atlas;
    vec4 atlasData;
} vs_out;

void main()
{
    uint subX = (packedVertexData.x & 0xFFFF0000u) >> 16;
    uint subY = (packedVertexData.x & 0x0000FFFFu);
    uint subZ = (packedVertexData.y & 0xFFFF0000u) >> 16;
    uint imageIndex = ((packedVertexData.y & 0x0000FFFFu) << 4) | ((packedVertexData.z & 0xF0000000u) >> 28);
    uint facingDirection = ((packedVertexData.z & 0x0E000000u) >> 25);
    uint rotation = ((packedVertexData.z & 0x01800000u) >> 23);
    uint reflection = ((packedVertexData.z & 0x00600000u) >> 21);
    uint lightingR = ((packedVertexData.z & 0x001FC000u) >> 14);
    uint lightingG = ((packedVertexData.z & 0x00003F80u) >> 7);
    uint lightingB = (packedVertexData.z & 0x0000007Fu);

    vs_out.lighting = vec3(float(lightingR), float(lightingG), float(lightingB))/127.0 ;
    vec3 localPos = (vec3(float(subX), float(subY), float(subZ))*16.0/65535.0);

    float textureX = float(subX) / 4096.0;
    float textureY = float(subY) / 4096.0;
    float textureZ = float(subZ) / 4096.0;
    if (facingDirection == 0u) {
        vs_out.uv = vec2(-textureX, textureY);
    } else if (facingDirection == 1u) {
        vs_out.uv = vec2(textureZ, textureY);
    } else if (facingDirection == 2u) {
        vs_out.uv = vec2(-textureZ, -textureX);
    } else if (facingDirection == 3u) {
        vs_out.uv = vec2(textureX, textureY);
    } else if (facingDirection == 4u) {
        vs_out.uv = vec2(-textureZ, textureY);
    } else if (facingDirection == 5u) {
        vs_out.uv = vec2(-textureZ, textureX);
        if (rotation == 3u || rotation == 1u) { // This is required because I couldnt get it working on the bottom
            rotation = 4u - rotation;
        }
    } else {
        vs_out.uv = vec2(0.0, 0.0);
    }

    if (rotation == 1u) {
        vs_out.uv = mat2(0.0, -1.0, 1.0, 0.0) * vs_out.uv;
    } else if (rotation == 2u) {
        vs_out.uv = mat2(-1.0, 0.0, 0.0, -1.0) * vs_out.uv;
    } else if (rotation == 3u) {
        vs_out.uv = mat2(0.0, 1.0, -1.0, 0.0) * vs_out.uv;
    }

    if ((reflection & 0x1u) > 0u) {
        vs_out.uv = mat2(-1.0, 0.0, 0.0, 1.0) * vs_out.uv;
    }
    if ((reflection & 0x2u) > 0u) {
        vs_out.uv = mat2(1.0, 0.0, 0.0, -1.0) * vs_out.uv;
    }

    ivec2 dataSize = textureSize(assetData, 0);
    ivec2 coord = ivec2(int(imageIndex) % dataSize.x, (int(imageIndex) / dataSize.x));
    uvec4 texel = texelFetch(assetData, coord, int(0));
    uint e1 = texel.r;
    uint e2 = texel.g;
    uint atlasIndex = (e1 & 0xFF000000u) >> 24;
    uint x = (e1 & 0x00FFFC00u) >> 10;
    uint y = ((e1 & 0x000003FFu) << 4) | ((e2 & 0xF0000000u) >> 28);
    uint w = (e2 & 0x0FFFC000u) >> 14;
    uint h = e2 & 0x00003FFFu;

//    vs_out.atlas = atlasIndex;
    ivec2 atlasDataSize = textureSize(atlasPage0, 0);
    vs_out.atlasData = vec4(float(x) / float(atlasDataSize.x), float(y) / float(atlasDataSize.y), float(w) / float(atlasDataSize.x), float(h) / float(atlasDataSize.y));

    mat4 modelMatrix = uModel;
    vec4 p = modelMatrix * vec4(localPos, 1.0);
    vs_out.pos = vec3(p);

    gl_Position = uCamera * p;
}