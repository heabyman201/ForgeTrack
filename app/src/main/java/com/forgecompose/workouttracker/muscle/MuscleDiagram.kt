package com.forgecompose.workouttracker.muscle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

enum class MuscleGroup {
    TRAPEZIUS,
    DELTOID_ANTERIOR,
    PECTORALIS_MAJOR,
    RECTUS_ABDOMINIS,
    OBLIQUES,
    LATISSIMUS_DORSI,
    BICEPS,
    TRICEPS,
    FOREARMS,
    QUADRICEPS,
    ADDUCTORS,
    CALVES,
    TIBIALIS
}

@Composable
fun MuscleHeatmapView(
    modifier: Modifier = Modifier,
    activations: Map<MuscleGroup, Float> = emptyMap(),
    baseColor: Color = Color(0xFF2A2A2A),
    maxHeatColor: Color = Color(0xFFFF3D00),
    outlineColor: Color = Color(0xFF121212),
    backgroundColor: Color = Color.Black
) {
    Box(modifier = modifier.background(backgroundColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2
            // Standardize scaling based on a 1000x2000 reference grid
            // This ensures the anatomy maintains aspect ratio regardless of container
            val scale = (h / 2000f).coerceAtMost(w / 1000f)

            // Center the diagram vertically
            val drawingHeight = 2000f * scale
            val topOffset = (h - drawingHeight) / 2

            fun getColor(muscle: MuscleGroup): Color {
                val intensity = activations[muscle] ?: 0f
                return androidx.compose.ui.graphics.lerp(baseColor, maxHeatColor, intensity)
            }

            // --- Layer 1: Deep / Posterior ---
            drawLatissimus(cx, topOffset + 500f * scale, scale, getColor(MuscleGroup.LATISSIMUS_DORSI), outlineColor)
            drawTrapezius(cx, topOffset + 320f * scale, scale, getColor(MuscleGroup.TRAPEZIUS), outlineColor)

            // --- Layer 2: Torso & Core ---
            drawNeck(cx, topOffset + 280f * scale, scale, baseColor, outlineColor)
            drawPectorals(cx, topOffset + 430f * scale, scale, getColor(MuscleGroup.PECTORALIS_MAJOR), outlineColor)
            drawObliques(cx, topOffset + 750f * scale, scale, getColor(MuscleGroup.OBLIQUES), outlineColor)
            drawAbs(cx, topOffset + 680f * scale, scale, getColor(MuscleGroup.RECTUS_ABDOMINIS), outlineColor)

            // --- Layer 3: Arms ---
            drawDeltoids(cx, topOffset + 400f * scale, scale, getColor(MuscleGroup.DELTOID_ANTERIOR), outlineColor)
            drawTriceps(cx, topOffset + 520f * scale, scale, getColor(MuscleGroup.TRICEPS), outlineColor)
            drawBiceps(cx, topOffset + 550f * scale, scale, getColor(MuscleGroup.BICEPS), outlineColor)
            drawForearms(cx, topOffset + 720f * scale, scale, getColor(MuscleGroup.FOREARMS), outlineColor)
            drawHands(cx, topOffset + 980f * scale, scale, baseColor, outlineColor)

            // --- Layer 4: Hips & Legs ---
            drawAdductors(cx, topOffset + 1050f * scale, scale, getColor(MuscleGroup.ADDUCTORS), outlineColor)
            drawQuadriceps(cx, topOffset + 1100f * scale, scale, getColor(MuscleGroup.QUADRICEPS), outlineColor)
            drawKnees(cx, topOffset + 1400f * scale, scale, baseColor, outlineColor)
            drawTibialis(cx, topOffset + 1550f * scale, scale, getColor(MuscleGroup.TIBIALIS), outlineColor)
            drawCalves(cx, topOffset + 1520f * scale, scale, getColor(MuscleGroup.CALVES), outlineColor)
            drawFeet(cx, topOffset + 1850f * scale, scale, baseColor, outlineColor)

            // --- Layer 5: Head ---
            drawHead(cx, topOffset + 150f * scale, scale, baseColor, outlineColor)
        }
    }
}

// --- Drawing Helpers using Organic Bézier Curves ---

fun DrawScope.drawHead(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val path = Path().apply {
        moveTo(cx, cy)
        cubicTo(cx + 70f * s, cy, cx + 65f * s, cy + 120f * s, cx, cy + 140f * s) // Chin
        cubicTo(cx - 65f * s, cy + 120f * s, cx - 70f * s, cy, cx, cy)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawNeck(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val path = Path().apply {
        moveTo(cx - 35f * s, cy)
        quadraticBezierTo(cx - 30f * s, cy + 60f * s, cx - 50f * s, cy + 100f * s) // To Traps
        lineTo(cx + 50f * s, cy + 100f * s)
        quadraticBezierTo(cx + 30f * s, cy + 60f * s, cx + 35f * s, cy)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawTrapezius(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val path = Path().apply {
        moveTo(cx - 35f * s, cy)
        cubicTo(cx - 80f * s, cy + 20f * s, cx - 120f * s, cy + 60f * s, cx - 160f * s, cy + 90f * s) // Slope to shoulder
        lineTo(cx - 130f * s, cy + 110f * s)
        lineTo(cx, cy + 140f * s) // Mid back point
        lineTo(cx + 130f * s, cy + 110f * s)
        lineTo(cx + 160f * s, cy + 90f * s)
        cubicTo(cx + 120f * s, cy + 60f * s, cx + 80f * s, cy + 20f * s, cx + 35f * s, cy)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawDeltoids(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 160f * s, cy)
        cubicTo(cx - 210f * s, cy + 10f * s, cx - 225f * s, cy + 100f * s, cx - 200f * s, cy + 180f * s) // Insertion
        quadraticBezierTo(cx - 170f * s, cy + 100f * s, cx - 130f * s, cy + 80f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 160f * s, cy)
        cubicTo(cx + 210f * s, cy + 10f * s, cx + 225f * s, cy + 100f * s, cx + 200f * s, cy + 180f * s)
        quadraticBezierTo(cx + 170f * s, cy + 100f * s, cx + 130f * s, cy + 80f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawPectorals(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx, cy)
        lineTo(cx - 60f * s, cy + 10f * s) // Clavicle line
        cubicTo(cx - 140f * s, cy + 20f * s, cx - 180f * s, cy + 80f * s, cx - 190f * s, cy + 120f * s) // Insertion
        cubicTo(cx - 150f * s, cy + 180f * s, cx - 80f * s, cy + 190f * s, cx - 10f * s, cy + 170f * s) // Bottom curve
        lineTo(cx, cy + 170f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx, cy)
        lineTo(cx + 60f * s, cy + 10f * s)
        cubicTo(cx + 140f * s, cy + 20f * s, cx + 180f * s, cy + 80f * s, cx + 190f * s, cy + 120f * s)
        cubicTo(cx + 150f * s, cy + 180f * s, cx + 80f * s, cy + 190f * s, cx + 10f * s, cy + 170f * s)
        lineTo(cx, cy + 170f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawLatissimus(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 180f * s, cy) // Under armpit
        cubicTo(cx - 190f * s, cy + 100f * s, cx - 160f * s, cy + 300f * s, cx - 90f * s, cy + 400f * s) // Sweep to waist
        lineTo(cx - 90f * s, cy + 100f * s) // Tuck behind obliques/ribs
        close()
    }
    val right = Path().apply {
        moveTo(cx + 180f * s, cy)
        cubicTo(cx + 190f * s, cy + 100f * s, cx + 160f * s, cy + 300f * s, cx + 90f * s, cy + 400f * s)
        lineTo(cx + 90f * s, cy + 100f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawAbs(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // Drawn as individual packs for accuracy
    val startY = cy
    val widthTop = 50f * s
    val widthBot = 40f * s

    // Top Pair
    for(side in listOf(-1f, 1f)) {
        val p = Path().apply {
            moveTo(cx + (5f * s * side), startY)
            lineTo(cx + (widthTop * side), startY)
            lineTo(cx + (widthTop * side), startY + 50f * s)
            lineTo(cx + (5f * s * side), startY + 50f * s)
            close()
        }
        drawPath(p, fill)
        drawPath(p, stroke, style = Stroke(width = 2f * s))
    }

    // Mid Pair
    for(side in listOf(-1f, 1f)) {
        val p = Path().apply {
            moveTo(cx + (5f * s * side), startY + 55f * s)
            lineTo(cx + (widthTop * side), startY + 55f * s)
            lineTo(cx + (widthBot * side), startY + 110f * s)
            lineTo(cx + (5f * s * side), startY + 110f * s)
            close()
        }
        drawPath(p, fill)
        drawPath(p, stroke, style = Stroke(width = 2f * s))
    }

    // Lower Pair
    for(side in listOf(-1f, 1f)) {
        val p = Path().apply {
            moveTo(cx + (5f * s * side), startY + 115f * s)
            lineTo(cx + (widthBot * side), startY + 115f * s)
            cubicTo(cx + (widthBot * side), startY + 150f * s, cx + (20f * s * side), startY + 180f * s, cx + (5f * s * side), startY + 180f * s)
            close()
        }
        drawPath(p, fill)
        drawPath(p, stroke, style = Stroke(width = 2f * s))
    }
}

fun DrawScope.drawObliques(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 60f * s, cy) // Ribs start
        cubicTo(cx - 100f * s, cy + 20f * s, cx - 120f * s, cy + 100f * s, cx - 100f * s, cy + 250f * s) // Iliac crest
        lineTo(cx - 60f * s, cy + 300f * s) // Pubic bone direction
        lineTo(cx - 50f * s, cy + 150f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 60f * s, cy)
        cubicTo(cx + 100f * s, cy + 20f * s, cx + 120f * s, cy + 100f * s, cx + 100f * s, cy + 250f * s)
        lineTo(cx + 60f * s, cy + 300f * s)
        lineTo(cx + 50f * s, cy + 150f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawBiceps(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 195f * s, cy) // Shoulder insertion
        cubicTo(cx - 230f * s, cy + 50f * s, cx - 230f * s, cy + 120f * s, cx - 190f * s, cy + 180f * s) // Elbow pit
        cubicTo(cx - 160f * s, cy + 120f * s, cx - 160f * s, cy + 50f * s, cx - 195f * s, cy)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 195f * s, cy)
        cubicTo(cx + 230f * s, cy + 50f * s, cx + 230f * s, cy + 120f * s, cx + 190f * s, cy + 180f * s)
        cubicTo(cx + 160f * s, cy + 120f * s, cx + 160f * s, cy + 50f * s, cx + 195f * s, cy)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawTriceps(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // Visible mainly on outer arm
    val left = Path().apply {
        moveTo(cx - 200f * s, cy)
        cubicTo(cx - 240f * s, cy + 40f * s, cx - 250f * s, cy + 100f * s, cx - 220f * s, cy + 160f * s) // Elbow outer
        lineTo(cx - 200f * s, cy + 80f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 200f * s, cy)
        cubicTo(cx + 240f * s, cy + 40f * s, cx + 250f * s, cy + 100f * s, cx + 220f * s, cy + 160f * s)
        lineTo(cx + 200f * s, cy + 80f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawForearms(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 190f * s, cy) // Elbow
        cubicTo(cx - 240f * s, cy + 50f * s, cx - 230f * s, cy + 150f * s, cx - 200f * s, cy + 250f * s) // Wrist
        lineTo(cx - 170f * s, cy + 250f * s)
        cubicTo(cx - 160f * s, cy + 150f * s, cx - 170f * s, cy + 50f * s, cx - 190f * s, cy)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 190f * s, cy)
        cubicTo(cx + 240f * s, cy + 50f * s, cx + 230f * s, cy + 150f * s, cx + 200f * s, cy + 250f * s)
        lineTo(cx + 170f * s, cy + 250f * s)
        cubicTo(cx + 160f * s, cy + 150f * s, cx + 170f * s, cy + 50f * s, cx + 190f * s, cy)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawQuadriceps(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 70f * s, cy) // Hip attachment
        // Outer sweep (Vastus Lateralis)
        cubicTo(cx - 140f * s, cy + 100f * s, cx - 150f * s, cy + 250f * s, cx - 100f * s, cy + 350f * s) // Knee outer
        // Knee connection
        lineTo(cx - 50f * s, cy + 350f * s)
        // Teardrop (Vastus Medialis)
        cubicTo(cx - 20f * s, cy + 300f * s, cx - 30f * s, cy + 150f * s, cx - 40f * s, cy + 100f * s) // Inner groin
        close()
    }
    // Rectus Femoris detail (Line down center of quad)
    val leftDetail = Path().apply {
        moveTo(cx - 70f * s, cy)
        quadraticBezierTo(cx - 80f * s, cy + 150f * s, cx - 75f * s, cy + 320f * s)
    }

    val right = Path().apply {
        moveTo(cx + 70f * s, cy)
        cubicTo(cx + 140f * s, cy + 100f * s, cx + 150f * s, cy + 250f * s, cx + 100f * s, cy + 350f * s)
        lineTo(cx + 50f * s, cy + 350f * s)
        cubicTo(cx + 20f * s, cy + 300f * s, cx + 30f * s, cy + 150f * s, cx + 40f * s, cy + 100f * s)
        close()
    }
    val rightDetail = Path().apply {
        moveTo(cx + 70f * s, cy)
        quadraticBezierTo(cx + 80f * s, cy + 150f * s, cx + 75f * s, cy + 320f * s)
    }

    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(leftDetail, stroke, style = Stroke(width = 1.5f * s)) // Detail line

    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
    drawPath(rightDetail, stroke, style = Stroke(width = 1.5f * s))
}

fun DrawScope.drawAdductors(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx, cy - 20f * s) // Crotch
        lineTo(cx - 40f * s, cy + 100f * s)
        lineTo(cx - 30f * s, cy + 250f * s) // Inner knee
        lineTo(cx, cy + 100f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx, cy - 20f * s)
        lineTo(cx + 40f * s, cy + 100f * s)
        lineTo(cx + 30f * s, cy + 250f * s)
        lineTo(cx, cy + 100f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawCalves(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // Gastrocnemius
    val left = Path().apply {
        moveTo(cx - 100f * s, cy) // Knee outer
        cubicTo(cx - 120f * s, cy + 50f * s, cx - 110f * s, cy + 150f * s, cx - 80f * s, cy + 250f * s) // Ankle outer
        lineTo(cx - 60f * s, cy + 200f * s) // Achilles
        lineTo(cx - 50f * s, cy + 150f * s) // Inner calf
        cubicTo(cx - 40f * s, cy + 50f * s, cx - 60f * s, cy + 20f * s, cx - 50f * s, cy) // Knee inner
        close()
    }
    val right = Path().apply {
        moveTo(cx + 100f * s, cy)
        cubicTo(cx + 120f * s, cy + 50f * s, cx + 110f * s, cy + 150f * s, cx + 80f * s, cy + 250f * s)
        lineTo(cx + 60f * s, cy + 200f * s)
        lineTo(cx + 50f * s, cy + 150f * s)
        cubicTo(cx + 40f * s, cy + 50f * s, cx + 60f * s, cy + 20f * s, cx + 50f * s, cy)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawTibialis(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // Shin muscle (Tibialis Anterior)
    val left = Path().apply {
        moveTo(cx - 90f * s, cy + 20f * s)
        quadraticBezierTo(cx - 100f * s, cy + 100f * s, cx - 80f * s, cy + 280f * s)
        lineTo(cx - 60f * s, cy + 280f * s)
        lineTo(cx - 60f * s, cy + 50f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 90f * s, cy + 20f * s)
        quadraticBezierTo(cx + 100f * s, cy + 100f * s, cx + 80f * s, cy + 280f * s)
        lineTo(cx + 60f * s, cy + 280f * s)
        lineTo(cx + 60f * s, cy + 50f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawKnees(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(cx - 90f * s, cy, cx - 40f * s, cy + 50f * s))
    }
    val right = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(cx + 40f * s, cy, cx + 90f * s, cy + 50f * s))
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawHands(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 200f * s, cy)
        lineTo(cx - 210f * s, cy + 60f * s)
        lineTo(cx - 170f * s, cy + 70f * s)
        lineTo(cx - 160f * s, cy + 10f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 200f * s, cy)
        lineTo(cx + 210f * s, cy + 60f * s)
        lineTo(cx + 170f * s, cy + 70f * s)
        lineTo(cx + 160f * s, cy + 10f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

fun DrawScope.drawFeet(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 80f * s, cy)
        lineTo(cx - 110f * s, cy + 50f * s)
        lineTo(cx - 50f * s, cy + 60f * s)
        lineTo(cx - 60f * s, cy)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 80f * s, cy)
        lineTo(cx + 110f * s, cy + 50f * s)
        lineTo(cx + 50f * s, cy + 60f * s)
        lineTo(cx + 60f * s, cy)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2.5f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2.5f * s))
}

@Preview(showBackground = true)
@Composable
fun PreviewMuscleHeatmap() {
    val sampleActivations = mapOf(
        MuscleGroup.PECTORALIS_MAJOR to 1.0f,
        MuscleGroup.DELTOID_ANTERIOR to 0.7f,
        MuscleGroup.RECTUS_ABDOMINIS to 0.4f,
        MuscleGroup.QUADRICEPS to 0.2f
    )
    MuscleHeatmapView(
        modifier = Modifier.aspectRatio(0.5f).fillMaxSize(),
        activations = sampleActivations
    )
}