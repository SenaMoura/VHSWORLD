#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

// Função matemática para gerar ruído aleatório (estática de TV)
float rand(vec2 co) {
    return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    // 1. Distorção de Olho de Peixe
    vec2 uv = texCoord - vec2(0.5);
    float dist = length(uv);
    float distortion = 1.0 + dist * dist * 0.3;
    vec2 distortedUV = vec2(0.5) + uv * distortion;

    // Se estiver fora do raio da lente
    if (distortedUV.x < 0.0 || distortedUV.x > 1.0 || distortedUV.y < 0.0 || distortedUV.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    // 2. Cor do jogo original
    vec4 col = texture(DiffuseSampler, distortedUV);

    // 3. Linhas de varredura VHS (Scanlines)
    float scanline = sin(distortedUV.y * 800.0) * 0.04;
    col.rgb -= scanline;

    // 4. Estática/Ruído VHS animado com o tempo
    float noise = (rand(distortedUV + vec2(Time * 0.1, Time * 0.05)) - 0.5) * 0.12;
    col.rgb += vec4(noise).rgb;

    fragColor = col;
}