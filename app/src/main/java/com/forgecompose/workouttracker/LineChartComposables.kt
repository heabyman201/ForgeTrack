package com.forgecompose.workouttracker

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun LineChart(
    data: List<Workout>,
    maxWeight: Double,
    minWeight: Double
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(200, 255, 255, 255)
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }
    val tooltipTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 14.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val lineBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFFF85757), Color(0xFFD32F2F))) }
    val areaBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFF9B111E).copy(alpha = 0.4f), Color.Transparent)) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 8.dp)
            .pointerInput(data) {
                detectTapGestures { tapOffset ->
                    val pointRadius = with(density) { 15.dp.toPx() }
                    val closestIndex = data
                        .mapIndexed { index, workout ->
                            val yAxisRange = (maxWeight - minWeight).coerceAtLeast(1.0)
                            val xAxisSpacing = size.width / (data.size - 1).coerceAtLeast(1)
                            val pointX = index * xAxisSpacing
                            val pointY = size.height - (((workout.weight ?: 0.0) - minWeight) / yAxisRange * size.height).toFloat()
                            val distance = (tapOffset - Offset(pointX.toFloat(), pointY)).getDistance()
                            index to distance
                        }
                        .minByOrNull { it.second }
                        ?.takeIf { it.second < pointRadius }
                        ?.first
                    selectedIndex = closestIndex
                }
            }
    ) {
        if (data.size < 2) return@Canvas

        val yAxisRange = (maxWeight - minWeight).coerceAtLeast(1.0)
        val xAxisSpacing = size.width / (data.size - 1)
        val points = data.mapIndexed { index, workout ->
            val x = index * xAxisSpacing
            val y = size.height - (((workout.weight ?: 0.0) - minWeight) / yAxisRange * size.height).toFloat()
            Offset(x.toFloat(), y)
        }

        val areaPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(path = areaPath, brush = areaBrush)

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path = linePath, brush = lineBrush, style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.cornerPathEffect(16f)))

        points.forEachIndexed { index, point ->
            val isSelected = selectedIndex == index
            val radius = if (isSelected) 12f else 7f
            drawCircle(color = Color.White.copy(alpha = if (isSelected) 0.9f else 0.5f), radius = radius + 3f, center = point)
            drawCircle(brush = lineBrush, radius = radius, center = point)
        }

        // --- Smart Label Logic ---
        val maxLabels = (size.width / with(density) { 70.dp.toPx() }).toInt().coerceAtMost(data.size)
        val step = (data.size - 1) / (maxLabels - 1).coerceAtLeast(1)
        val indicesToLabel = (0 until maxLabels).map { (it * step).coerceAtMost(data.size - 1) }.distinct()

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        indicesToLabel.forEach { index ->
            val x = points[index].x
            drawContext.canvas.nativeCanvas.drawText(dateFormat.format(Date(data[index].date)), x, size.height + 60f, textPaint)
        }


        selectedIndex?.let { index ->
            val selectedPoint = points[index]
            val selectedWorkout = data[index]
            val tooltipText = "${selectedWorkout.weight} kg"

            val tooltipWidth = tooltipTextPaint.measureText(tooltipText) + 24.dp.toPx()
            val tooltipHeight = 40.dp.toPx()
            val tooltipRect = RoundRect(
                left = (selectedPoint.x - tooltipWidth / 2).coerceIn(0f, size.width - tooltipWidth),
                top = selectedPoint.y - tooltipHeight - 12.dp.toPx(),
                right = (selectedPoint.x + tooltipWidth / 2).coerceIn(tooltipWidth, size.width),
                bottom = selectedPoint.y - 12.dp.toPx(),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawRoundRect(color = Color(0xFF1A0808), topLeft = Offset(tooltipRect.left, tooltipRect.top), size = Size(tooltipRect.width, tooltipRect.height), cornerRadius = tooltipRect.topLeftCornerRadius, alpha = 0.9f)
            drawContext.canvas.nativeCanvas.drawText(tooltipText, tooltipRect.center.x, tooltipRect.center.y + 10.dp.toPx() / 2, tooltipTextPaint)
        }
    }
}
@Composable
fun LineChartSets(
    data: List<Workout>,
    maxValue: Double,
    minValue: Double
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(200, 255, 255, 255)
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }
    val tooltipTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 14.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val lineBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFF00E676), Color(0xFF1B8E4B))) }
    val areaBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFF00E676).copy(alpha = 0.35f), Color.Transparent)) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 8.dp)
            .pointerInput(data) {
                detectTapGestures { tapOffset ->
                    val pointRadius = with(density) { 15.dp.toPx() }
                    val closestIndex = data
                        .mapIndexed { index, workout ->
                            val yAxisRange = (maxValue - minValue).coerceAtLeast(1.0)
                            val xAxisSpacing = size.width / (data.size - 1).coerceAtLeast(1)
                            val pointX = index * xAxisSpacing
                            val pointY = size.height - (((((workout.sets ?: 0).toDouble()) - minValue) / yAxisRange * size.height).toFloat())
                            val distance = (tapOffset - Offset(pointX.toFloat(), pointY)).getDistance()
                            index to distance
                        }
                        .minByOrNull { it.second }
                        ?.takeIf { it.second < pointRadius }
                        ?.first
                    selectedIndex = closestIndex
                }
            }
    ) {
        if (data.size < 2) return@Canvas

        val yAxisRange = (maxValue - minValue).coerceAtLeast(1.0)
        val xAxisSpacing = size.width / (data.size - 1)
        val points = data.mapIndexed { index, workout ->
            val x = index * xAxisSpacing
            val v = (workout.sets ?: 0).toDouble()
            val y = size.height - (((v - minValue) / yAxisRange * size.height).toFloat())
            Offset(x.toFloat(), y)
        }

        val areaPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(path = areaPath, brush = areaBrush)

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = linePath,
            brush = lineBrush,
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.cornerPathEffect(16f))
        )

        points.forEachIndexed { index, point ->
            val isSelected = selectedIndex == index
            val radius = if (isSelected) 12f else 7f
            drawCircle(color = Color.White.copy(alpha = if (isSelected) 0.9f else 0.5f), radius = radius + 3f, center = point)
            drawCircle(brush = lineBrush, radius = radius, center = point)
        }

        val maxLabels = (size.width / with(density) { 70.dp.toPx() }).toInt().coerceAtMost(data.size)
        val step = (data.size - 1) / (maxLabels - 1).coerceAtLeast(1)
        val indicesToLabel = (0 until maxLabels).map { (it * step).coerceAtMost(data.size - 1) }.distinct()

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        indicesToLabel.forEach { index ->
            val x = points[index].x
            drawContext.canvas.nativeCanvas.drawText(dateFormat.format(Date(data[index].date)), x, size.height + 60f, textPaint)
        }

        selectedIndex?.let { index ->
            val selectedPoint = points[index]
            val selectedWorkout = data[index]
            val tooltipText = "${selectedWorkout.sets ?: 0} sets"

            val tooltipWidth = tooltipTextPaint.measureText(tooltipText) + 24.dp.toPx()
            val tooltipHeight = 40.dp.toPx()
            val tooltipRect = RoundRect(
                left = (selectedPoint.x - tooltipWidth / 2).coerceIn(0f, size.width - tooltipWidth),
                top = selectedPoint.y - tooltipHeight - 12.dp.toPx(),
                right = (selectedPoint.x + tooltipWidth / 2).coerceIn(tooltipWidth, size.width),
                bottom = selectedPoint.y - 12.dp.toPx(),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF081A12),
                topLeft = Offset(tooltipRect.left, tooltipRect.top),
                size = Size(tooltipRect.width, tooltipRect.height),
                cornerRadius = tooltipRect.topLeftCornerRadius,
                alpha = 0.9f
            )
            drawContext.canvas.nativeCanvas.drawText(tooltipText, tooltipRect.center.x, tooltipRect.center.y + 10.dp.toPx() / 2, tooltipTextPaint)
        }
    }
}

@Composable
 fun LineChartReps(
    data: List<Workout>,
    maxValue: Double,
    minValue: Double
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    val textPaint = remember {
        Paint().apply {
            color = android.graphics.Color.argb(200, 255, 255, 255)
            textSize = with(density) { 12.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }
    val tooltipTextPaint = remember {
        Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 14.sp.toPx() }
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
    }

    val lineBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFFFF3B30), Color(0xFFD32F2F))) }
    val areaBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFFFF3B30).copy(alpha = 0.35f), Color.Transparent)) }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 8.dp)
            .pointerInput(data) {
                detectTapGestures { tapOffset ->
                    val pointRadius = with(density) { 15.dp.toPx() }
                    val closestIndex = data
                        .mapIndexed { index, workout ->
                            val yAxisRange = (maxValue - minValue).coerceAtLeast(1.0)
                            val xAxisSpacing = size.width / (data.size - 1).coerceAtLeast(1)
                            val pointX = index * xAxisSpacing
                            val pointY = size.height - (((((workout.reps ?: 0).toDouble()) - minValue) / yAxisRange * size.height).toFloat())
                            val distance = (tapOffset - Offset(pointX.toFloat(), pointY)).getDistance()
                            index to distance
                        }
                        .minByOrNull { it.second }
                        ?.takeIf { it.second < pointRadius }
                        ?.first
                    selectedIndex = closestIndex
                }
            }
    ) {
        if (data.size < 2) return@Canvas

        val yAxisRange = (maxValue - minValue).coerceAtLeast(1.0)
        val xAxisSpacing = size.width / (data.size - 1)
        val points = data.mapIndexed { index, workout ->
            val x = index * xAxisSpacing
            val v = (workout.reps ?: 0).toDouble()
            val y = size.height - (((v - minValue) / yAxisRange * size.height).toFloat())
            Offset(x.toFloat(), y)
        }

        val areaPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(path = areaPath, brush = areaBrush)

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = linePath,
            brush = lineBrush,
            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.cornerPathEffect(16f))
        )

        points.forEachIndexed { index, point ->
            val isSelected = selectedIndex == index
            val radius = if (isSelected) 12f else 7f
            drawCircle(color = Color.White.copy(alpha = if (isSelected) 0.9f else 0.5f), radius = radius + 3f, center = point)
            drawCircle(brush = lineBrush, radius = radius, center = point)
        }

        val maxLabels = (size.width / with(density) { 70.dp.toPx() }).toInt().coerceAtMost(data.size)
        val step = (data.size - 1) / (maxLabels - 1).coerceAtLeast(1)
        val indicesToLabel = (0 until maxLabels).map { (it * step).coerceAtMost(data.size - 1) }.distinct()

        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        indicesToLabel.forEach { index ->
            val x = points[index].x
            drawContext.canvas.nativeCanvas.drawText(dateFormat.format(Date(data[index].date)), x, size.height + 60f, textPaint)
        }

        selectedIndex?.let { index ->
            val selectedPoint = points[index]
            val selectedWorkout = data[index]
            val tooltipText = "${selectedWorkout.reps ?: 0} reps"

            val tooltipWidth = tooltipTextPaint.measureText(tooltipText) + 24.dp.toPx()
            val tooltipHeight = 40.dp.toPx()
            val tooltipRect = RoundRect(
                left = (selectedPoint.x - tooltipWidth / 2).coerceIn(0f, size.width - tooltipWidth),
                top = selectedPoint.y - tooltipHeight - 12.dp.toPx(),
                right = (selectedPoint.x + tooltipWidth / 2).coerceIn(tooltipWidth, size.width),
                bottom = selectedPoint.y - 12.dp.toPx(),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF1A0808),
                topLeft = Offset(tooltipRect.left, tooltipRect.top),
                size = Size(tooltipRect.width, tooltipRect.height),
                cornerRadius = tooltipRect.topLeftCornerRadius,
                alpha = 0.9f
            )
            drawContext.canvas.nativeCanvas.drawText(tooltipText, tooltipRect.center.x, tooltipRect.center.y + 10.dp.toPx() / 2, tooltipTextPaint)
        }
    }
}
@Composable
fun CombinedWorkoutChart(
    data: List<Workout>,
    modifier: Modifier = Modifier,
    height: Dp = 250.dp
) {
    if (data.isEmpty()) return
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    val axisTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(200, 255, 255, 255)
            textSize = with(density) { 12.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    val tooltipTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 14.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
    }

    val weightBrush = remember { Brush.verticalGradient(listOf(Color(0xFFF85757), Color(0xFFD32F2F))) }
    val setsBrush = remember { Brush.verticalGradient(listOf(Color(0xFF00E676), Color(0xFF1B8E4B))) }
    val repsBrush = remember { Brush.verticalGradient(listOf(Color(0xFFFFC107), Color(0xFFFF8F00))) }

    val wVals = remember(data) { data.map { (it.weight ?: 0.0) } }
    val sVals = remember(data) { data.map { (it.sets ?: 0).toDouble() } }
    val rVals = remember(data) { data.map { (it.reps ?: 0).toDouble() } }

    val wMin = remember(wVals) { wVals.minOrNull() ?: 0.0 }
    val wMax = remember(wVals) { wVals.maxOrNull() ?: 1.0 }
    val sMin = remember(sVals) { sVals.minOrNull() ?: 0.0 }
    val sMax = remember(sVals) { sVals.maxOrNull() ?: 1.0 }
    val rMin = remember(rVals) { rVals.minOrNull() ?: 0.0 }
    val rMax = remember(rVals) { rVals.maxOrNull() ?: 1.0 }

    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(text = "Weight", brush = weightBrush)
            LegendDot(text = "Sets", brush = setsBrush)
            LegendDot(text = "Reps", brush = repsBrush)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .padding(top = 8.dp, bottom = 24.dp, start = 8.dp, end = 8.dp)
                .pointerInput(data) {
                    detectTapGestures { tapOffset ->
                        if (data.size < 2) return@detectTapGestures
                        val xSpacing = size.width / (data.size - 1).coerceAtLeast(1)
                        val idx = (tapOffset.x / xSpacing).roundToInt().coerceIn(0, data.lastIndex)
                        selectedIndex = idx
                    }
                }
        ) {
            if (data.size < 2) return@Canvas

            val xSpacing = size.width / (data.size - 1)
            fun norm(v: Double, mn: Double, mx: Double): Float {
                val r = (mx - mn).coerceAtLeast(1e-6)
                return ((v - mn) / r).toFloat().coerceIn(0f, 1f)
            }

            val wPoints = data.mapIndexed { i, wk ->
                val y = size.height - norm((wk.weight ?: 0.0), wMin, wMax) * size.height
                Offset(i * xSpacing, y)
            }
            val sPoints = data.mapIndexed { i, wk ->
                val y = size.height - norm((wk.sets ?: 0).toDouble(), sMin, sMax) * size.height
                Offset(i * xSpacing, y)
            }
            val rPoints = data.mapIndexed { i, wk ->
                val y = size.height - norm((wk.reps ?: 0).toDouble(), rMin, rMax) * size.height
                Offset(i * xSpacing, y)
            }

            val stroke = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.cornerPathEffect(14f))

            val wPath = Path().apply { moveTo(wPoints.first().x, wPoints.first().y); wPoints.drop(1).forEach { lineTo(it.x, it.y) } }
            val sPath = Path().apply { moveTo(sPoints.first().x, sPoints.first().y); sPoints.drop(1).forEach { lineTo(it.x, it.y) } }
            val rPath = Path().apply { moveTo(rPoints.first().x, rPoints.first().y); rPoints.drop(1).forEach { lineTo(it.x, it.y) } }

            drawPath(path = wPath, brush = weightBrush, style = stroke)
            drawPath(path = sPath, brush = setsBrush, style = stroke)
            drawPath(path = rPath, brush = repsBrush, style = stroke)

            val sel = selectedIndex
            if (sel != null) {
                val px = sel * xSpacing
                drawLine(Color.White.copy(alpha = 0.15f), start = Offset(px, 0f), end = Offset(px, size.height), strokeWidth = 2f)
                fun hitDot(p: Offset, brush: Brush) {
                    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 9f, center = p)
                    drawCircle(brush = brush, radius = 6f, center = p)
                }
                hitDot(wPoints[sel], weightBrush)
                hitDot(sPoints[sel], setsBrush)
                hitDot(rPoints[sel], repsBrush)

                val wk = data[sel]
                val dateLine = dateFormat.format(Date(wk.date))
                val t1 = "${(wk.weight ?: 0.0)} kg"
                val t2 = "${(wk.sets ?: 0)} sets"
                val t3 = "${(wk.reps ?: 0)} reps"

                val padX = 12.dp.toPx()
                val lineH = 22.dp.toPx()

                val widest = listOf(dateLine, t1, t2, t3).maxOf { tooltipTextPaint.measureText(it) }
                val contentW = widest + padX * 2
                val contentH = lineH * 4 + 12.dp.toPx()

                val baseY = min(min(wPoints[sel].y, sPoints[sel].y), rPoints[sel].y) - 12.dp.toPx() - contentH
                val top = baseY.coerceAtLeast(8.dp.toPx())
                val left = (px - contentW / 2).coerceIn(0f, size.width - contentW)
                val rect = RoundRect(left, top, left + contentW, top + contentH, CornerRadius(10.dp.toPx()))

                drawRoundRect(
                    color = Color(0xFF0F0F12),
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    cornerRadius = rect.topLeftCornerRadius,
                    alpha = 0.95f
                )

                val cx = rect.center.x
                val nc = drawContext.canvas.nativeCanvas


                val originalSize = tooltipTextPaint.textSize
                tooltipTextPaint.textSize = with(density) { 15.sp.toPx() }
                nc.drawText(dateLine, cx, rect.top + lineH, tooltipTextPaint)
                tooltipTextPaint.textSize = originalSize

                nc.drawText(t1, cx, rect.top + lineH * 2, tooltipTextPaint)
                nc.drawText(t2, cx, rect.top + lineH * 3, tooltipTextPaint)
                nc.drawText(t3, cx, rect.top + lineH * 4, tooltipTextPaint)
            }

            val maxLabels = (size.width / with(density) { 70.dp.toPx() }).toInt().coerceAtMost(data.size)
            val step = (data.size - 1) / (maxLabels - 1).coerceAtLeast(1)
            val idxs = (0 until maxLabels).map { (it * step).coerceAtMost(data.lastIndex) }.distinct()
            idxs.forEach { i ->
                val x = i * xSpacing
                drawContext.canvas.nativeCanvas.drawText(dateFormat.format(Date(data[i].date)), x, size.height + 56f, axisTextPaint)
            }
        }
    }
}

@Composable
private fun LegendDot(text: String, brush: Brush) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(brush = brush, radius = size.minDimension / 2)
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
    }
}
