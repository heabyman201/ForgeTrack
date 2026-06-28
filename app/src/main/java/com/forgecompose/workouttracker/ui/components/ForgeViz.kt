package com.forgecompose.workouttracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.forgecompose.workouttracker.AppearanceOptionsAppTheme
import com.forgecompose.workouttracker.AppearanceOptionsManagerAppTheme
import com.forgecompose.workouttracker.ColorSchemeAppTheme
import com.forgecompose.workouttracker.ui.theme.ForgeMotion
import com.forgecompose.workouttracker.ui.theme.ForgeShape
import com.forgecompose.workouttracker.ui.theme.ForgeSpacing
import com.forgecompose.workouttracker.ui.theme.forgeValueColor
import com.forgecompose.workouttracker.ui.theme.tabular

/**
 * ForgeViz — one data-visualisation grammar shared across the app.
 *
 *  - [ForgeGradientLine]  : trends, where the stroke colour maps to value.
 *  - [ForgeRangeGauge]    : "where you sit on a scale", with a moving thumb.
 *  - [ForgeBreakdownRow]  : category breakdowns (label + colored value + bar).
 *  - [ForgeProgressRing]  : activity/sleep-style circular metrics.
 *
 * All four pull from the same [forgeValueColor] ramp and tabular figures, so they
 * read as one family — just recoloured per context.
 */

@Composable
private fun forgeTheme(): ColorSchemeAppTheme {
    val context = LocalContext.current
    val appearance by AppearanceOptionsManagerAppTheme.flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    return appearance.colors
}

/**
 * Gradient-stroked line chart. The line colour climbs the value ramp (calm crimson →
 * orange attention) as the metric rises, instead of a flat colour.
 */
@Composable
fun ForgeGradientLine(
    values: List<Float>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 3.dp,
    fillArea: Boolean = true
) {
    val theme = forgeTheme()
    if (values.size < 2) {
        Spacer(modifier)
        return
    }
    val minV = values.min()
    val maxV = values.max()
    val span = (maxV - minV).takeIf { it > 0f } ?: 1f

    Canvas(modifier) {
        val n = values.size
        val stepX = if (n > 1) size.width / (n - 1) else size.width
        val pad = strokeWidth.toPx()
        fun frac(i: Int) = (values[i] - minV) / span
        fun point(i: Int) = Offset(
            x = stepX * i,
            y = pad + (1f - frac(i)) * (size.height - pad * 2f)
        )

        if (fillArea) {
            val area = Path().apply {
                moveTo(0f, size.height)
                for (i in 0 until n) lineTo(point(i).x, point(i).y)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(
                    listOf(theme.primary.copy(alpha = 0.18f), Color.Transparent)
                )
            )
        }

        val sw = strokeWidth.toPx()
        for (i in 0 until n - 1) {
            val a = point(i)
            val b = point(i + 1)
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        forgeValueColor(theme, frac(i)),
                        forgeValueColor(theme, frac(i + 1))
                    ),
                    start = a,
                    end = b
                ),
                start = a,
                end = b,
                strokeWidth = sw,
                cap = StrokeCap.Round
            )
        }

        // Leading endpoint marker, coloured by its own value.
        val last = point(n - 1)
        drawCircle(Color.White, sw * 0.95f, last)
        drawCircle(forgeValueColor(theme, frac(n - 1)), sw * 0.6f, last)
    }
}

/**
 * Segmented range gauge with a moving thumb — for any "where you sit on a scale"
 * metric (readiness, intensity, fatigue). The track runs the value ramp; the thumb
 * lands at [fraction].
 */
@Composable
fun ForgeRangeGauge(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp
) {
    val theme = forgeTheme()
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = ForgeMotion.snappy(),
        label = "gaugeThumb"
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val trackH = size.height * 0.45f
        val top = (size.height - trackH) / 2f
        val r = trackH / 2f
        // Segmented ramp track.
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(
                    forgeValueColor(theme, 0f),
                    forgeValueColor(theme, 0.5f),
                    forgeValueColor(theme, 1f)
                )
            ),
            topLeft = Offset(0f, top),
            size = Size(size.width, trackH),
            cornerRadius = CornerRadius(r)
        )
        // Moving thumb with a crimson-tinted halo so it emits light.
        val cx = (size.width * animated).coerceIn(r, size.width - r)
        val cy = size.height / 2f
        val thumbR = size.height / 2f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(theme.primary.copy(alpha = 0.5f), Color.Transparent),
                center = Offset(cx, cy),
                radius = thumbR * 2.2f
            ),
            radius = thumbR * 2.2f,
            center = Offset(cx, cy)
        )
        drawCircle(Color.White, thumbR, Offset(cx, cy))
        drawCircle(forgeValueColor(theme, animated), thumbR * 0.55f, Offset(cx, cy))
    }
}

/**
 * Determinate breakdown row: category label + colored value + bar. Used for any
 * "what's it made of" list (muscle load, time split, volume by group).
 */
@Composable
fun ForgeBreakdownRow(
    label: String,
    valueText: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    val theme = forgeTheme()
    val barColor = accent ?: forgeValueColor(theme, fraction)
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = ForgeMotion.smooth(),
        label = "breakdownFill"
    )
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge.tabular(),
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        Spacer(Modifier.height(ForgeSpacing.xs))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            val r = size.height / 2f
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                cornerRadius = CornerRadius(r)
            )
            if (animated > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(barColor.copy(alpha = 0.85f), barColor)
                    ),
                    size = Size(size.width * animated, size.height),
                    cornerRadius = CornerRadius(r)
                )
            }
        }
    }
}

/**
 * Circular progress ring for activity/sleep-style metrics. Shares the ramp and the
 * crimson catch on the leading cap, so it belongs to the same family as the gauge.
 */
@Composable
fun ForgeProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    ringSize: Dp = 72.dp,
    stroke: Dp = 8.dp,
    centerLabel: String? = null,
    accent: Color? = null
) {
    val theme = forgeTheme()
    val ringColor = accent ?: forgeValueColor(theme, fraction)
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = ForgeMotion.bouncy(),
        label = "ringSweep"
    )
    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(ringSize)) {
            val sw = stroke.toPx()
            val inset = sw / 2f
            val arcSize = Size(size.width - sw, size.height - sw)
            val topLeft = Offset(inset, inset)
            // Track.
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            // Progress sweep.
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        forgeValueColor(theme, 0f),
                        ringColor,
                        forgeValueColor(theme, animated)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
        if (centerLabel != null) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.titleMedium.tabular(),
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

/**
 * Score-first header: a dominant value/status word in large heavy type, with smaller
 * lighter supporting text beneath — the hierarchy every ForgeCard leads with.
 */
@Composable
fun ForgeScoreHeader(
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    numeric: Boolean = true
) {
    Column(modifier) {
        Text(
            text = value,
            style = if (numeric) {
                MaterialTheme.typography.displaySmall.tabular()
            } else {
                MaterialTheme.typography.displaySmall
            },
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
        Spacer(Modifier.height(ForgeSpacing.xs))
        Text(
            text = caption,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
