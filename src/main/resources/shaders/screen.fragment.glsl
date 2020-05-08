#version 330 core

/* sum(rgb * a, a) */
uniform sampler2D accumTexture;

/* prod(1 - a) */
uniform sampler2D revealageTexture;

out vec4 FragColor;

float maxComponent(vec4 v) {
    return max(max(v.x, v.y), max(v.z, 0.0));
}

void main() {
    ivec2 C = ivec2(gl_FragCoord.xy);
    float revealage = texelFetch(revealageTexture, C, 0).r;
    if (revealage == 1.0) {
        // Save the blending and color texture fetch cost
        discard;
    }

    vec4 accum = texelFetch(accumTexture, C, 0);
    // Suppress overflow
    if (isinf(maxComponent(abs(accum)))) {
        accum.rgb = vec3(accum.a);
    }
    vec3 averageColor = accum.rgb / max(accum.a, 0.00001);

    // dst' =  (accum.rgb / accum.a) * (1 - revealage) + dst * revealage
    FragColor = vec4(averageColor, 1.0 - revealage);
}