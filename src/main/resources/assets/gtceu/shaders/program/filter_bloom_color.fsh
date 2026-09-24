#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D MainDepthSampler;

layout(std140) uniform BloomDepth {
    float DepthNear;
    float DepthFar;
};

in vec2 texCoord;

out vec4 fragColor;

float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0; // back to NDC
    return (2.0 * DepthNear * DepthFar) / (DepthFar + DepthNear - z * (DepthFar - DepthNear));
}

void main() {

    // calculate linear depth
    float mainDepth = linearizeDepth(texture(MainDepthSampler, texCoord).r);
    float diffuseDepth = linearizeDepth(texture(DiffuseDepthSampler, texCoord).r);
    // clear bloom color fragment if the main sampler's depth isn't the same as the bloom sampler's depth
    if (abs(mainDepth - diffuseDepth) > 1.0e-5) {
        fragColor = vec4(0.0);
    } else {
        fragColor = texture(DiffuseSampler, texCoord);
    }
}
