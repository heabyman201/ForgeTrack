import org.intellij.lang.annotations.Language

@Language("AGSL")
val EDGE_REFRACTION_GLASS = """
uniform shader composable_child;
uniform vec2  resolution;
uniform float cornerRpx;
uniform float borderPx;
uniform float intensity;

float sdRoundRect(vec2 p, vec2 b, float r){
    vec2 q = abs(p) - (b - vec2(r));
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

vec4 main(vec2 fragCoord){
    vec2  res = resolution;
    vec2  halfSize = res * 0.5;
    vec2  p = fragCoord - halfSize;

    float d = sdRoundRect(p, halfSize, cornerRpx);

    float edge = smoothstep(borderPx*1.2, borderPx*0.25, abs(d));

    float e = 1.0;
    float dX1 = sdRoundRect(p + vec2(e, 0.0), halfSize, cornerRpx);
    float dY1 = sdRoundRect(p + vec2(0.0, e), halfSize, cornerRpx);
    vec2  n = normalize(vec2(dX1 - d, dY1 - d) + 1e-6);

    float refr = 1.8 * intensity;
    vec2  shift = n * refr * edge;

    vec2  rp  = clamp(fragCoord + shift, vec2(0.5), res - vec2(0.5));
    vec2  rpR = clamp(rp + n*0.6, vec2(0.5), res - vec2(0.5));
    vec2  rpB = clamp(rp - n*0.6, vec2(0.5), res - vec2(0.5));

    vec3 col0 = composable_child.eval(fragCoord).rgb;
    vec3 colR = composable_child.eval(rpR).rgb;
    vec3 colG = composable_child.eval(rp ).rgb;
    vec3 colB = composable_child.eval(rpB).rgb;

    vec3 refrCol = vec3(colR.r, colG.g, colB.b);

    float mixAmt = 0.65 * edge;
    vec3  col = mix(col0, refrCol, mixAmt);

    float ring = smoothstep(borderPx*1.1, borderPx*0.6, abs(d)) * 0.06;
    col += ring;

    return vec4(col, 0.88);
}
"""
