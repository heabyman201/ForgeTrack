package com.forgecompose.workouttracker.analytics

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
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
    height: Dp = 280.dp
) {
    if (data.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }


    LaunchedEffect(data) {
        selectedIndex = null
    }

    val density = LocalDensity.current

    val wVals = remember(data) { data.map { it.weight ?: 0.0 } }
    val sVals = remember(data) { data.map { (it.sets ?: 0).toDouble() } }
    val rVals = remember(data) { data.map { (it.reps ?: 0).toDouble() } }

    val wMax = remember(wVals) { wVals.maxOrNull()?.takeIf { it > 0 } ?: 100.0 }
    val sMax = remember(sVals) { sVals.maxOrNull()?.takeIf { it > 0 } ?: 5.0 }
    val rMax = remember(rVals) { rVals.maxOrNull()?.takeIf { it > 0 } ?: 15.0 }

    val wMin = remember(wVals) { wVals.minOrNull() ?: 0.0 }
    val sMin = remember(sVals) { sVals.minOrNull() ?: 0.0 }
    val rMin = remember(rVals) { rVals.minOrNull() ?: 0.0 }

    val wColor = Color(0xFFEF5350)
    val sColor = Color(0xFF66BB6A)
    val rColor = Color(0xFFFFCA28)

    val wBrush = remember { Brush.verticalGradient(listOf(wColor, wColor.copy(alpha = 0.1f))) }
    val sBrush = remember { Brush.verticalGradient(listOf(sColor, sColor.copy(alpha = 0.1f))) }
    val rBrush = remember { Brush.verticalGradient(listOf(rColor, rColor.copy(alpha = 0.1f))) }

    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.LTGRAY
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val tooltipTitlePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = with(density) { 13.sp.toPx() }
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }
    }

    val tooltipValuePaint = remember {
        android.graphics.Paint().apply {
            textSize = with(density) { 12.sp.toPx() }
            typeface = android.graphics.Typeface.DEFAULT
            isAntiAlias = true
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegendItem("Weight", wColor)
            ChartLegendItem("Sets", sColor)
            ChartLegendItem("Reps", rColor)
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        if (data.isEmpty()) return@detectTapGestures
                        val step = size.width / (data.size - 1).coerceAtLeast(1)
                        selectedIndex = (offset.x / step).roundToInt().coerceIn(0, data.lastIndex)
                    }
                }
                .pointerInput(data) {
                    detectDragGestures(
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    ) { change, _ ->
                        change.consume()
                        if (data.isEmpty()) return@detectDragGestures
                        val step = size.width / (data.size - 1).coerceAtLeast(1)
                        selectedIndex = (change.position.x / step).roundToInt().coerceIn(0, data.lastIndex)
                    }
                }
        ) {
            if (data.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val stepX = width / (data.size - 1)
            val bottomY = height - 24.dp.toPx()

            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, 0f),
                end = Offset(width, 0f),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, bottomY / 2),
                end = Offset(width, bottomY / 2),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, bottomY),
                end = Offset(width, bottomY),
                strokeWidth = 1f
            )

            fun getPoint(index: Int, value: Double, min: Double, max: Double): Offset {
                val norm = ((value - min) / (max - min).coerceAtLeast(1e-6)).toFloat().coerceIn(0f, 1f)
                val x = index * stepX
                val y = bottomY - (norm * bottomY)
                return Offset(x, y)
            }

            fun drawChartLine(
                values: List<Double>,
                min: Double,
                max: Double,
                color: Color,
                fillBrush: Brush
            ) {
                val points = values.mapIndexed { i, v -> getPoint(i, v, min, max) }
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }

                val fillPath = Path().apply {
                    addPath(strokePath)
                    lineTo(points.last().x, bottomY)
                    lineTo(points.first().x, bottomY)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = fillBrush,
                    alpha = 0.2f
                )

                drawPath(
                    path = strokePath,
                    color = color,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.cornerPathEffect(20f)
                    )
                )
            }

            drawChartLine(wVals, wMin, wMax, wColor, wBrush)
            drawChartLine(sVals, sMin, sMax, sColor, sBrush)
            drawChartLine(rVals, rMin, rMax, rColor, rBrush)

            val labelCount = (width / 60.dp.toPx()).toInt().coerceIn(2, data.size)
            val labelStep = (data.size - 1) / (labelCount - 1).coerceAtLeast(1)

            for (i in 0 until labelCount) {
                val index = (i * labelStep).coerceAtMost(data.lastIndex)
                val x = index * stepX
                val dateStr = dateFormat.format(Date(data[index].date))
                drawContext.canvas.nativeCanvas.drawText(
                    dateStr,
                    x,
                    height,
                    textPaint
                )
            }

            selectedIndex?.let { index ->
                // FIX 2: Ensure the stale index doesn't crash the app if data shrank
                if (index < 0 || index >= data.size) return@let

                val x = index * stepX
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(x, 0f),
                    end = Offset(x, bottomY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                val wPos = getPoint(index, wVals[index], wMin, wMax)
                val sPos = getPoint(index, sVals[index], sMin, sMax)
                val rPos = getPoint(index, rVals[index], rMin, rMax)

                drawCircle(Color.White, radius = 6.dp.toPx(), center = wPos)
                drawCircle(wColor, radius = 4.dp.toPx(), center = wPos)

                drawCircle(Color.White, radius = 6.dp.toPx(), center = sPos)
                drawCircle(sColor, radius = 4.dp.toPx(), center = sPos)

                drawCircle(Color.White, radius = 6.dp.toPx(), center = rPos)
                drawCircle(rColor, radius = 4.dp.toPx(), center = rPos)

                val dateText = dateFormat.format(Date(data[index].date))
                val wText = "Weight: ${wVals[index].toInt()} kg"
                val sText = "Sets: ${sVals[index].toInt()}"
                val rText = "Reps: ${rVals[index].toInt()}"

                val padding = 12.dp.toPx()
                val lineHeight = 18.dp.toPx()
                val boxWidth = 130.dp.toPx()
                val boxHeight = (lineHeight * 4) + (padding * 2) + 4.dp.toPx()

                var boxX = x + 10.dp.toPx()
                if (boxX + boxWidth > width) {
                    boxX = x - boxWidth - 10.dp.toPx()
                }
                val boxY = 10.dp.toPx()

                val rect = RoundRect(
                    left = boxX,
                    top = boxY,
                    right = boxX + boxWidth,
                    bottom = boxY + boxHeight,
                    cornerRadius = CornerRadius(8.dp.toPx())
                )

                drawPath(
                    path = Path().apply { addRoundRect(rect) },
                    color = Color(0xFF1E1E1E),
                    alpha = 0.95f
                )
                drawPath(
                    path = Path().apply { addRoundRect(rect) },
                    color = Color.Gray.copy(alpha = 0.3f),
                    style = Stroke(width = 1.dp.toPx())
                )

                val textX = boxX + padding
                var currentY = boxY + padding + lineHeight/2

                drawContext.canvas.nativeCanvas.drawText(dateText, textX, currentY, tooltipTitlePaint)

                currentY += lineHeight + 4.dp.toPx()
                tooltipValuePaint.color = android.graphics.Color.parseColor("#EF5350")
                drawContext.canvas.nativeCanvas.drawText(wText, textX, currentY, tooltipValuePaint)

                currentY += lineHeight
                tooltipValuePaint.color = android.graphics.Color.parseColor("#66BB6A")
                drawContext.canvas.nativeCanvas.drawText(sText, textX, currentY, tooltipValuePaint)

                currentY += lineHeight
                tooltipValuePaint.color = android.graphics.Color.parseColor("#FFCA28")
                drawContext.canvas.nativeCanvas.drawText(rText, textX, currentY, tooltipValuePaint)
            }
        }
    }
}

@Composable
private fun ChartLegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun LineChartAverageHr(
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

    val lineBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFF64B5F6), Color(0xFF1E88E5))) }
    val areaBrush = remember { Brush.verticalGradient(colors = listOf(Color(0xFF64B5F6).copy(alpha = 0.35f), Color.Transparent)) }

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
                            val pointY = size.height - (((((workout.heartRateAvg ?: 0).toDouble()) - minValue) / yAxisRange * size.height).toFloat())
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
            val v = (workout.heartRateAvg ?: 0).toDouble()
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
            val tooltipText = "${selectedWorkout.heartRateAvg ?: 0} bpm"

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
                color = Color(0xFF0A1624),
                topLeft = Offset(tooltipRect.left, tooltipRect.top),
                size = Size(tooltipRect.width, tooltipRect.height),
                cornerRadius = tooltipRect.topLeftCornerRadius,
                alpha = 0.9f
            )
            drawContext.canvas.nativeCanvas.drawText(tooltipText, tooltipRect.center.x, tooltipRect.center.y + 10.dp.toPx() / 2, tooltipTextPaint)
        }
    }
}
