import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MuscleDiagramView(
    modifier: Modifier = Modifier,
    muscleColor: Color = Color(0xFFE57373),
    outlineColor: Color = Color.Black,
    backgroundColor: Color = Color.Black
) {
    Box(modifier = modifier.background(backgroundColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2
            val scale = h / 1000f

            // Layer 1: Posterior/Deep
            drawLats(cx, 280f * scale, scale, muscleColor, outlineColor)
            drawTraps(cx, 150f * scale, scale, muscleColor, outlineColor)

            // Layer 2: Main Torso
            drawNeck(cx, 130f * scale, scale, muscleColor, outlineColor)
            drawPecs(cx, 220f * scale, scale, muscleColor, outlineColor)
            drawSerratus(cx, 280f * scale, scale, muscleColor, outlineColor)
            drawObliques(cx, 360f * scale, scale, muscleColor, outlineColor)
            drawAbs(cx, 320f * scale, scale, muscleColor, outlineColor)

            // Layer 3: Arms
            drawDelts(cx, 190f * scale, scale, muscleColor, outlineColor)
            drawTriceps(cx, 240f * scale, scale, muscleColor, outlineColor)
            drawBiceps(cx, 260f * scale, scale, muscleColor, outlineColor)
            drawForearms(cx, 350f * scale, scale, muscleColor, outlineColor)
            drawHands(cx, 480f * scale, scale, muscleColor, outlineColor)

            // Layer 4: Hips & Legs
            drawHips(cx, 440f * scale, scale, muscleColor, outlineColor)
            drawAdductors(cx, 580f * scale, scale, muscleColor, outlineColor)
            drawQuads(cx, 550f * scale, scale, muscleColor, outlineColor)
            drawKnees(cx, 700f * scale, scale, muscleColor, outlineColor)
            drawTibialis(cx, 800f * scale, scale, muscleColor, outlineColor)
            drawCalves(cx, 780f * scale, scale, muscleColor, outlineColor)
            drawFeet(cx, 950f * scale, scale, muscleColor, outlineColor)

            // Layer 5: Head (Top)
            drawHead(cx, 80f * scale, scale, muscleColor, outlineColor)
        }
    }
}

fun DrawScope.drawHead(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val path = Path().apply {
        moveTo(cx, cy - 50f * s)
        cubicTo(cx + 40f * s, cy - 50f * s, cx + 45f * s, cy, cx + 35f * s, cy + 40f * s)
        quadraticBezierTo(cx, cy + 60f * s, cx - 35f * s, cy + 40f * s)
        cubicTo(cx - 45f * s, cy, cx - 40f * s, cy - 50f * s, cx, cy - 50f * s)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawNeck(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val path = Path().apply {
        moveTo(cx - 30f * s, cy)
        lineTo(cx - 35f * s, cy + 50f * s)
        quadraticBezierTo(cx, cy + 60f * s, cx + 35f * s, cy + 50f * s)
        lineTo(cx + 30f * s, cy)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawTraps(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val path = Path().apply {
        moveTo(cx - 35f * s, cy)
        lineTo(cx - 100f * s, cy + 40f * s) // To shoulder
        lineTo(cx - 40f * s, cy + 60f * s) // Back to spine
        lineTo(cx, cy + 80f * s)
        lineTo(cx + 40f * s, cy + 60f * s)
        lineTo(cx + 100f * s, cy + 40f * s)
        lineTo(cx + 35f * s, cy)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawDelts(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val leftDelt = Path().apply {
        moveTo(cx - 95f * s, cy)
        quadraticBezierTo(cx - 150f * s, cy + 10f * s, cx - 140f * s, cy + 80f * s)
        lineTo(cx - 100f * s, cy + 50f * s)
        close()
    }
    val rightDelt = Path().apply {
        moveTo(cx + 95f * s, cy)
        quadraticBezierTo(cx + 150f * s, cy + 10f * s, cx + 140f * s, cy + 80f * s)
        lineTo(cx + 100f * s, cy + 50f * s)
        close()
    }
    drawPath(leftDelt, fill)
    drawPath(leftDelt, stroke, style = Stroke(width = 2f * s))
    drawPath(rightDelt, fill)
    drawPath(rightDelt, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawPecs(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val leftPec = Path().apply {
        moveTo(cx, cy - 20f * s)
        lineTo(cx - 90f * s, cy - 10f * s)
        quadraticBezierTo(cx - 100f * s, cy + 50f * s, cx - 10f * s, cy + 70f * s)
        lineTo(cx, cy + 70f * s)
        close()
    }
    val rightPec = Path().apply {
        moveTo(cx, cy - 20f * s)
        lineTo(cx + 90f * s, cy - 10f * s)
        quadraticBezierTo(cx + 100f * s, cy + 50f * s, cx + 10f * s, cy + 70f * s)
        lineTo(cx, cy + 70f * s)
        close()
    }
    drawPath(leftPec, fill)
    drawPath(leftPec, stroke, style = Stroke(width = 2f * s))
    drawPath(rightPec, fill)
    drawPath(rightPec, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawLats(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val leftLat = Path().apply {
        moveTo(cx - 60f * s, cy)
        lineTo(cx - 110f * s, cy - 20f * s) // Under armpit
        quadraticBezierTo(cx - 120f * s, cy + 60f * s, cx - 70f * s, cy + 100f * s)
        close()
    }
    val rightLat = Path().apply {
        moveTo(cx + 60f * s, cy)
        lineTo(cx + 110f * s, cy - 20f * s)
        quadraticBezierTo(cx + 120f * s, cy + 60f * s, cx + 70f * s, cy + 100f * s)
        close()
    }
    drawPath(leftLat, fill)
    drawPath(leftLat, stroke, style = Stroke(width = 2f * s))
    drawPath(rightLat, fill)
    drawPath(rightLat, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawSerratus(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // Rib muscles under pecs
    val left = Path().apply {
        moveTo(cx - 70f * s, cy)
        lineTo(cx - 100f * s, cy - 10f * s)
        lineTo(cx - 80f * s, cy + 40f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 70f * s, cy)
        lineTo(cx + 100f * s, cy - 10f * s)
        lineTo(cx + 80f * s, cy + 40f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawAbs(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // 8-pack segmentation
    val startY = cy
    for (i in 0..3) {
        val yOffset = i * 35f * s
        // Left
        drawRect(
            color = fill,
            topLeft = Offset(cx - 40f * s, startY + yOffset),
            size = Size(38f * s, 32f * s)
        )
        drawRect(
            color = stroke,
            topLeft = Offset(cx - 40f * s, startY + yOffset),
            size = Size(38f * s, 32f * s),
            style = Stroke(width = 2f * s)
        )
        // Right
        drawRect(
            color = fill,
            topLeft = Offset(cx + 2f * s, startY + yOffset),
            size = Size(38f * s, 32f * s)
        )
        drawRect(
            color = stroke,
            topLeft = Offset(cx + 2f * s, startY + yOffset),
            size = Size(38f * s, 32f * s),
            style = Stroke(width = 2f * s)
        )
    }
}

fun DrawScope.drawObliques(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 45f * s, cy)
        lineTo(cx - 80f * s, cy - 10f * s)
        quadraticBezierTo(cx - 85f * s, cy + 60f * s, cx - 60f * s, cy + 100f * s)
        lineTo(cx - 45f * s, cy + 80f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 45f * s, cy)
        lineTo(cx + 80f * s, cy - 10f * s)
        quadraticBezierTo(cx + 85f * s, cy + 60f * s, cx + 60f * s, cy + 100f * s)
        lineTo(cx + 45f * s, cy + 80f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawTriceps(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 140f * s, cy)
        lineTo(cx - 155f * s, cy + 50f * s)
        lineTo(cx - 145f * s, cy + 100f * s) // Elbow
        lineTo(cx - 135f * s, cy + 50f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 140f * s, cy)
        lineTo(cx + 155f * s, cy + 50f * s)
        lineTo(cx + 145f * s, cy + 100f * s)
        lineTo(cx + 135f * s, cy + 50f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawBiceps(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 135f * s, cy)
        quadraticBezierTo(cx - 105f * s, cy + 50f * s, cx - 120f * s, cy + 110f * s) // To elbow pit
        lineTo(cx - 145f * s, cy + 80f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 135f * s, cy)
        quadraticBezierTo(cx + 105f * s, cy + 50f * s, cx + 120f * s, cy + 110f * s)
        lineTo(cx + 145f * s, cy + 80f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawForearms(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 120f * s, cy) // Elbow pit
        quadraticBezierTo(cx - 160f * s, cy + 40f * s, cx - 140f * s, cy + 140f * s) // Wrist
        lineTo(cx - 110f * s, cy + 140f * s)
        lineTo(cx - 115f * s, cy + 30f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 120f * s, cy)
        quadraticBezierTo(cx + 160f * s, cy + 40f * s, cx + 140f * s, cy + 140f * s)
        lineTo(cx + 110f * s, cy + 140f * s)
        lineTo(cx + 115f * s, cy + 30f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawHands(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 140f * s, cy)
        lineTo(cx - 150f * s, cy + 50f * s)
        lineTo(cx - 120f * s, cy + 50f * s)
        lineTo(cx - 110f * s, cy)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 140f * s, cy)
        lineTo(cx + 150f * s, cy + 50f * s)
        lineTo(cx + 120f * s, cy + 50f * s)
        lineTo(cx + 110f * s, cy)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawHips(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val path = Path().apply {
        moveTo(cx - 60f * s, cy)
        lineTo(cx + 60f * s, cy)
        lineTo(cx + 50f * s, cy + 60f * s)
        lineTo(cx, cy + 80f * s) // Crotch
        lineTo(cx - 50f * s, cy + 60f * s)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawAdductors(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // Inner thigh
    val left = Path().apply {
        moveTo(cx - 5f * s, cy - 60f * s) // Crotch area
        lineTo(cx - 30f * s, cy + 80f * s)
        lineTo(cx - 50f * s, cy + 20f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 5f * s, cy - 60f * s)
        lineTo(cx + 30f * s, cy + 80f * s)
        lineTo(cx + 50f * s, cy + 20f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawQuads(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // Rectus Femoris & Vastus Lateralis/Medialis complex
    val left = Path().apply {
        moveTo(cx - 50f * s, cy)
        quadraticBezierTo(cx - 100f * s, cy + 50f * s, cx - 80f * s, cy + 150f * s) // Outer sweep
        lineTo(cx - 30f * s, cy + 150f * s) // Knee top
        lineTo(cx - 20f * s, cy + 80f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 50f * s, cy)
        quadraticBezierTo(cx + 100f * s, cy + 50f * s, cx + 80f * s, cy + 150f * s)
        lineTo(cx + 30f * s, cy + 150f * s)
        lineTo(cx + 20f * s, cy + 80f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawKnees(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(
            cx - 70f * s, cy, cx - 30f * s, cy + 40f * s
        ))
    }
    val right = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(
            cx + 30f * s, cy, cx + 70f * s, cy + 40f * s
        ))
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawTibialis(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // Shin muscle
    val left = Path().apply {
        moveTo(cx - 65f * s, cy - 40f * s) // Knee
        lineTo(cx - 40f * s, cy + 120f * s) // Ankle
        lineTo(cx - 60f * s, cy + 120f * s)
        quadraticBezierTo(cx - 80f * s, cy + 40f * s, cx - 65f * s, cy - 40f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 65f * s, cy - 40f * s)
        lineTo(cx + 40f * s, cy + 120f * s)
        lineTo(cx + 60f * s, cy + 120f * s)
        quadraticBezierTo(cx + 80f * s, cy + 40f * s, cx + 65f * s, cy - 40f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawCalves(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    // Inner gastrocnemius visible from front
    val left = Path().apply {
        moveTo(cx - 40f * s, cy - 20f * s)
        quadraticBezierTo(cx - 20f * s, cy + 40f * s, cx - 35f * s, cy + 100f * s)
        lineTo(cx - 40f * s, cy + 120f * s)
        lineTo(cx - 50f * s, cy + 100f * s)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 40f * s, cy - 20f * s)
        quadraticBezierTo(cx + 20f * s, cy + 40f * s, cx + 35f * s, cy + 100f * s)
        lineTo(cx + 40f * s, cy + 120f * s)
        lineTo(cx + 50f * s, cy + 100f * s)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

fun DrawScope.drawFeet(cx: Float, cy: Float, s: Float, fill: Color, stroke: Color) {
    val left = Path().apply {
        moveTo(cx - 40f * s, cy)
        lineTo(cx - 80f * s, cy + 40f * s)
        lineTo(cx - 25f * s, cy + 45f * s)
        lineTo(cx - 30f * s, cy)
        close()
    }
    val right = Path().apply {
        moveTo(cx + 40f * s, cy)
        lineTo(cx + 80f * s, cy + 40f * s)
        lineTo(cx + 25f * s, cy + 45f * s)
        lineTo(cx + 30f * s, cy)
        close()
    }
    drawPath(left, fill)
    drawPath(left, stroke, style = Stroke(width = 2f * s))
    drawPath(right, fill)
    drawPath(right, stroke, style = Stroke(width = 2f * s))
}

@Preview(showBackground = true)
@Composable
fun PreviewMuscleDiagram() {
    MuscleDiagramView(modifier = Modifier.fillMaxSize())
}