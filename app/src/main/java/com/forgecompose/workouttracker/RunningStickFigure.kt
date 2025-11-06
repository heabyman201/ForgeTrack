package com.forgecompose.workouttracker

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RunningStickFigure(isStepping: Boolean, progress: Float, modifier: Modifier = Modifier) {
    val stickFigureColor = Color(0xFFE53935)
    val sweatColor = Color(0xFF65B2FF)
    val animationDuration = (400 + 250 * progress).toInt()

    val transition = rememberInfiniteTransition(label = "running_transition")
    val legAngle by transition.animateFloat(
        initialValue = -35f, targetValue = 35f,
        animationSpec = infiniteRepeatable(tween(animationDuration, easing = LinearEasing), RepeatMode.Reverse),
        label = "legAngle"
    )
    val armAngle by transition.animateFloat(
        initialValue = 30f, targetValue = -30f,
        animationSpec = infiniteRepeatable(tween(animationDuration, easing = LinearEasing), RepeatMode.Reverse),
        label = "armAngle"
    )
    val bodyBob by transition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(animationDuration / 2, easing = LinearEasing), RepeatMode.Reverse),
        label = "bodyBob"
    )

    val sweatTransition = rememberInfiniteTransition(label = "sweat_transition")
    val sweatProgress1 by sweatTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = LinearEasing)))
    val sweatProgress2 by sweatTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, delayMillis = 300, easing = LinearEasing)))
    val sweatProgress3 by sweatTransition.animateFloat(0f, 1f, infiniteRepeatable(tween(900, delayMillis = 500, easing = LinearEasing)))
    val sweatAlpha = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)

    val animationProgress by animateFloatAsState(
        targetValue = if (isStepping) 1f else 0f,
        animationSpec = tween(500),
        label = "stand_to_run_transition"
    )

    val slouchAngle = 20f * progress

    Canvas(modifier = modifier
        .height(200.dp)
        .fillMaxWidth()) {
        val scale = 3.5f
        val strokeWidth = 8f * scale
        val headRadius = 18f * scale
        val bodyLength = 50f * scale
        val limbLength = 45f * scale

        val startX = size.width / 2
        val startY = size.height - 550f

        val currentBodyBob = bodyBob * animationProgress
        val currentLegAngle = legAngle * animationProgress
        val currentArmAngle = armAngle * animationProgress

        val hip = Offset(startX, startY + headRadius + bodyLength + currentBodyBob)

        withTransform({ rotate(degrees = slouchAngle, pivot = hip) }) {
            val headCenter = Offset(startX, startY + currentBodyBob)
            val neck = Offset(startX, startY + headRadius + currentBodyBob)
            val shoulder = Offset(startX, startY + headRadius + 10f * scale + currentBodyBob)

            drawLine(stickFigureColor, neck, hip, strokeWidth, StrokeCap.Round)
            drawCircle(stickFigureColor, headRadius, headCenter, style = Stroke(strokeWidth))

            if (sweatAlpha > 0 && isStepping) {
                val sweatRotation = Math.toRadians(slouchAngle.toDouble()).toFloat()
                val cosR = cos(sweatRotation)
                val sinR = sin(sweatRotation)
                fun rotated(offset: Offset): Offset {
                    val x = offset.x * cosR - offset.y * sinR
                    val y = offset.x * sinR + offset.y * cosR
                    return Offset(x, y)
                }

                drawSweatDroplet(sweatProgress1, headCenter, sweatColor.copy(alpha = sweatAlpha), ::rotated)
                drawSweatDroplet(sweatProgress2, headCenter, sweatColor.copy(alpha = sweatAlpha), ::rotated)
                drawSweatDroplet(sweatProgress3, headCenter, sweatColor.copy(alpha = sweatAlpha), ::rotated)
            }

            withTransform({ rotate(degrees = currentArmAngle, pivot = shoulder) }) {
                drawLine(stickFigureColor, shoulder, Offset(shoulder.x, shoulder.y + limbLength), strokeWidth, StrokeCap.Round)
            }
            withTransform({ rotate(degrees = -currentArmAngle, pivot = shoulder) }) {
                drawLine(stickFigureColor, shoulder, Offset(shoulder.x, shoulder.y + limbLength), strokeWidth, StrokeCap.Round)
            }
        }

        withTransform({ rotate(degrees = currentLegAngle, pivot = hip) }) {
            drawLine(stickFigureColor, hip, Offset(hip.x, hip.y + limbLength), strokeWidth, StrokeCap.Round)
        }
        withTransform({ rotate(degrees = -currentLegAngle, pivot = hip) }) {
            drawLine(stickFigureColor, hip, Offset(hip.x, hip.y + limbLength), strokeWidth, StrokeCap.Round)
        }
    }
}



private fun DrawScope.drawSweatDroplet(
    t: Float,
    headCenter: Offset,
    color: Color,
    applyRotation: (Offset) -> Offset
) {
    val initialVelX = -120f
    val initialVelY = -150f
    val gravity = 300f
    val rawDx = initialVelX * t
    val rawDy = initialVelY * t + 0.5f * gravity * t * t
    val rotatedOffset = applyRotation(Offset(rawDx, rawDy))

    val dropletCenter = headCenter + rotatedOffset + Offset(-20f, -20f)
    drawCircle(color, radius = 8f - 4*t, center = dropletCenter)
}