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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GenericLineChart(
    data: List<Workout>,
    maxValue: Double,
    minValue: Double,
    valueSelector: (Workout) -> Double,
    unit: String,
    lineBrush: Brush,
    areaBrush: Brush,
    tooltipColor: Color
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
                            val pointY = size.height - (((valueSelector(workout) - minValue) / yAxisRange * size.height).toFloat())
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
            val y = size.height - (((valueSelector(workout) - minValue) / yAxisRange * size.height).toFloat())
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
            val selectedValue = valueSelector(selectedWorkout)
            val tooltipText = if (selectedValue.rem(1) == 0.0) {
                "${selectedValue.toInt()} $unit"
            } else {
                String.format("%.1f %s", selectedValue, unit)
            }


            val tooltipWidth = tooltipTextPaint.measureText(tooltipText) + 24.dp.toPx()
            val tooltipHeight = 40.dp.toPx()
            val tooltipRect = RoundRect(
                left = (selectedPoint.x - tooltipWidth / 2).coerceIn(0f, size.width - tooltipWidth),
                top = selectedPoint.y - tooltipHeight - 12.dp.toPx(),
                right = (selectedPoint.x + tooltipWidth / 2).coerceIn(tooltipWidth, size.width),
                bottom = selectedPoint.y - 12.dp.toPx(),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawRoundRect(color = tooltipColor, topLeft = Offset(tooltipRect.left, tooltipRect.top), size = Size(tooltipRect.width, tooltipRect.height), cornerRadius = tooltipRect.topLeftCornerRadius, alpha = 0.9f)
            drawContext.canvas.nativeCanvas.drawText(tooltipText, tooltipRect.center.x, tooltipRect.center.y + 10.dp.toPx() / 2, tooltipTextPaint)
        }
    }
}