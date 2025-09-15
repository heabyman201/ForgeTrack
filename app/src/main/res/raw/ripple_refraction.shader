uniform float2 resolution;
uniform float2 rippleOrigin;
uniform float rippleRadius;
uniform float rippleAlpha;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;
    float2 origin = rippleOrigin / resolution;

    float dist = distance(uv, origin);
    float ripple = smoothstep(rippleRadius * 0.8, rippleRadius, dist);

    half3 rippleColor = half3(1.0); // White shimmer
    return half4(rippleColor, (1.0 - ripple) * rippleAlpha);
}
