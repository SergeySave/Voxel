#version 330 core

in VS_OUT {
    vec3 pos;
    vec2 uv;
    vec3 lighting;
//    uint atlas;
    vec4 atlasData;
} fs_in;

layout(location = 0) out vec4 _accum;
layout(location = 1) out float _revealage;

uniform sampler2D atlasPage0;

void writePixel(vec4 premultipliedReflect, vec3 transmit, float csZ) {
    /* Modulate the net coverage for composition by the transmission. This does not affect the color channels of the
       transparent surface because the caller's BSDF model should have already taken into account if transmission modulates
       reflection. This model doesn't handled colored transmission, so it averages the color channels. See

          McGuire and Enderton, Colored Stochastic Shadow Maps, ACM I3D, February 2011
          http://graphics.cs.williams.edu/papers/CSSM/

       for a full explanation and derivation.*/

    premultipliedReflect.a *= 1.0 - clamp((transmit.r + transmit.g + transmit.b) * (1.0 / 3.0), 0, 1);

    /* You may need to adjust the w function if you have a very large or very small view volume; see the paper and
       presentation slides at http://jcgt.org/published/0002/02/09/ */
    // Intermediate terms to be cubed
    float a = min(1.0, premultipliedReflect.a) * 8.0 + 0.01;
    float b = -gl_FragCoord.z * 0.95 + 1.0;

    /* If your scene has a lot of content very close to the far plane,
       then include this line (one rsqrt instruction):
       b /= sqrt(1e4 * abs(csZ)); */
    float w    = clamp(a * a * a * 1e8 * b * b * b, 1e-2, 3e2);
    _accum     = premultipliedReflect * w;
    _revealage = premultipliedReflect.a;
}

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
    if (texel.a <= 0.0 || texel.a >= 1.0) {
        discard;
    }
    writePixel(vec4(lighting * texel.rgb, texel.a), vec3(lighting * texel.rgb) * (1.0 - texel.a), 0.0);
}