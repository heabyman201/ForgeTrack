package com.forgecompose.workouttracker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FlagCircle
import androidx.compose.material.icons.rounded.OutlinedFlag
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun AdviceSectionUser(
    advice: String,
    lastWorkoutName: String,
    extraLines: List<String>,
    maxExtraLines: Int = 4,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val aiEnabled = dynamicModel.personaConfig.value.enabled
    val cold = rememberColdStartStages()

    var isLoading by remember { mutableStateOf(aiEnabled) }
    LaunchedEffect(aiEnabled) {
        isLoading = aiEnabled
        if (aiEnabled) {
            delay(1000)
            isLoading = false
        }
    }

    val glow: Float = if (aiEnabled && isLoading) {
        val t = rememberInfiniteTransition(label = "adviceGlow")
        t.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(animation = tween(2000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
            label = "glow"
        ).value
    } else 0.35f

    val cardShape = remember { RoundedCornerShape(16.dp) }
    val accent = if (aiEnabled) Color(0xFFFF3B30) else Color(0xFFAB4747)
    val borderGlow = glow

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        color = Color(0xFF120707).copy(alpha = 0.52f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                .drawWithCache {
                    val borderBrush = Brush.linearGradient(
                        listOf(
                            Color(0xFFFF5555).copy(alpha = 0.4f * borderGlow),
                            Color(0xFF8B0000).copy(alpha = 0.25f * borderGlow)
                        )
                    )
                    val bgBrush = Brush.radialGradient(
                        listOf(
                            Color(0xFF3A0E0E).copy(alpha = 0.35f * borderGlow),
                            Color(0xFF120707).copy(alpha = 0.85f)
                        )
                    )
                    onDrawBehind {
                        drawRoundRect(
                            brush = bgBrush,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                        drawRoundRect(
                            brush = borderBrush,
                            style = Stroke(width = 1.dp.toPx()),
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (cold.after200ms) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (aiEnabled) Icons.Rounded.Flag else Icons.Default.Flag,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Last workout:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = lastWorkoutName.ifBlank { "None" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFABEC2),
                                overflow = TextOverflow.Visible
                            )
                            val linesToShow = remember(extraLines, maxExtraLines) {
                                extraLines.filter { it.isNotBlank() }.take(maxExtraLines)
                            }
                            AnimatedVisibility(
                                visible = linesToShow.isNotEmpty(),
                                enter = fadeIn() + expandVertically(clip = false),
                                exit = fadeOut() + shrinkVertically(clip = false)
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = accent.copy(alpha = 0.3f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        linesToShow.forEach { line ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.ChevronRight,
                                                    contentDescription = null,
                                                    tint = accent.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = line,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (aiEnabled && isLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 2.dp),
                            color = Color(0xFF8B0000),
                            trackColor = Color.Black.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

