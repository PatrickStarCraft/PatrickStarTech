#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D HighlightSampler;
uniform sampler2D MainSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 DiffuseSize;
    vec2 HighlightSize;
    vec2 MainSize;
};

layout(std140) uniform BloomSettings {
    float BloomStrength;
    float BaseBrightness;
    float MaxBrightness;
    float MinBrightness;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 bloom = BloomStrength * texture(DiffuseSampler, texCoord);

    vec4 highlight = texture(HighlightSampler, texCoord);
    vec4 background = texture(MainSampler, texCoord);
    background.rgb = background.rgb * (1 - highlight.a) + highlight.a * highlight.rgb;

    float min = min(background.r, min(background.g, background.b));
    float max = max(background.r, max(background.g, background.b));
    float backgroundBrightness = (max + min) / 2.0;

    // Copy-back uses the target entity-outline blit pipeline, whose color blend is alpha-based.
    // The former GL path replaced the main color with a one/zero blend, so keep this output opaque.
    fragColor = vec4(background.rgb + bloom.rgb * (MinBrightness + BaseBrightness + (1.0 - backgroundBrightness) * (MaxBrightness - MinBrightness)), 1.0);
}
