package com.forgecompose.workouttracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.round

@Composable
public fun GraphBar(
    workout: Workout,
    maxWeight: Double,
    prevSameNameWeight: Double?,
    baselineWeight: Double? = null,
    epsilonKg: Double = 0.1
) {
    val current = (workout.weight ?: 0.0)
    val safeMax = if (maxWeight > 0) maxWeight else 1.0
    val target = remember(workout.id, current, safeMax) {
        (current / safeMax).toFloat().coerceIn(0f, 1f)
    }

    val anim = remember { Animatable(0f) }
    var fill by remember { mutableStateOf(0f) }
    var introDone by remember { mutableStateOf(false) }

    LaunchedEffect(target) {
        introDone = false
        anim.snapTo(0f)
        anim.animateTo(targetValue = target, animationSpec = tween(900, 100, LinearOutSlowInEasing))
        introDone = true
    }
    LaunchedEffect(anim) {
        snapshotFlow { anim.value }.collect { v -> fill = v }
    }

    fun snap01(x: Double) = round(x * 10.0) / 10.0
    val basis: Double? = prevSameNameWeight ?: baselineWeight
    val deltaRaw: Double? = basis?.let { current - it }
    val deltaSnapped: Double? = deltaRaw?.let { snap01(it) }
    val trend = when {
        deltaSnapped == null -> Trend.NONE
        abs(deltaSnapped) < epsilonKg -> Trend.FLAT
        deltaSnapped > 0 -> Trend.UP
        else -> Trend.DOWN
    }

    val good = Color(0xFF2ECC71)
    val bad = Color(0xFFE74C3C)
    val neutral = MaterialTheme.colorScheme.outline
    val trendColor = when (trend) {
        Trend.UP -> good
        Trend.DOWN -> bad
        Trend.FLAT -> neutral
        Trend.NONE -> neutral
    }
    val trendIcon = when (trend) {
        Trend.UP -> Icons.Outlined.ArrowUpward
        Trend.DOWN -> Icons.Outlined.ArrowDownward
        Trend.FLAT -> Icons.Outlined.HorizontalRule
        Trend.NONE -> Icons.Outlined.HorizontalRule
    }
    val deltaText = deltaSnapped?.let {
        val sign = if (it > 0) "+" else if (it < 0) "−" else ""
        "$sign${"%.1f".format(abs(it))} kg"
    } ?: "—"

    val glow = Color(0xFFA43434)
    val barBrush = remember { Brush.horizontalGradient(listOf(Color(0xFF5A1E1E), glow)) }
    val trackBrush = remember { Brush.verticalGradient(listOf(Color(0xFF160C0C), Color(0xFF1E0E0E))) }
    val innerHighlight = remember {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.08f),
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.10f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workout.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%.1f kg".format(current),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.bodySmall,
                    color = trendColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(CircleShape)
                .background(trackBrush)
                .drawWithCache {
                    val corner = CornerRadius(size.minDimension, size.minDimension)
                    onDrawBehind {
                        drawRoundRect(brush = innerHighlight, cornerRadius = corner, alpha = 1f)
                    }
                }
                .padding(horizontal = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .graphicsLayer {
                        shape = CircleShape
                        clip = true
                        shadowElevation = 10.dp.toPx()
                        spotShadowColor = glow.copy(alpha = 0.45f)
                        ambientShadowColor = glow.copy(alpha = 0.30f)
                    }
                    .background(barBrush)
                    .drawWithCache {
                        val corner = CornerRadius(size.minDimension, size.minDimension)
                        onDrawWithContent {
                            drawContent()
                            if (fill > 0f) {
                                val tipX = size.width
                                drawRect(
                                    brush = Brush.radialGradient(
                                        listOf(glow.copy(alpha = 0.75f), Color.Transparent),
                                        center = Offset(tipX, size.height / 2f),
                                        radius = 22f
                                    )
                                )
                            }
                        }
                    }
            )
        }
    }
}