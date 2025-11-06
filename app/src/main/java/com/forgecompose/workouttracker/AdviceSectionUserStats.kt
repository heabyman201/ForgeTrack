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
    val glowTransition = rememberInfiniteTransition(label = "adviceGlow")
    val glow by glowTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    var isLoading by remember { mutableStateOf(aiEnabled) }
    val cardShape = remember { RoundedCornerShape(16.dp) }

    LaunchedEffect(aiEnabled) {
        isLoading = aiEnabled
        if (aiEnabled) {
            delay(1000)
            isLoading = false
        }
    }

    val accent = if (aiEnabled) Color(0xFFFF3B30) else Color(0xFFAB4747)
    val borderGlow = if (aiEnabled) glow else 0.35f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = cardShape,
                ambientColor = Color(0xFF8B0000),
                spotColor = Color(0xFF8B0000)
            ),
        shape = cardShape,
        color = Color(0xFF120707).copy(alpha = 0.75f)
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
                            imageVector = if (aiEnabled) Icons.Rounded.OutlinedFlag else Icons.Default.Flag,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))

//                        if (aiEnabled) {
//                            AnimatedContent(
//                                targetState = isLoading,
//                                transitionSpec = {
//                                    fadeIn(animationSpec = tween(800, easing = FastOutSlowInEasing)) togetherWith
//                                            fadeOut(animationSpec = tween(800))
//                                },
//                                modifier = Modifier
//                                    .weight(1f)
//                                    .heightIn(min = 24.dp),
//                                label = "textMorphAnimation"
//                            ) { loading ->
//                                if (loading) {
//                                    Box(
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .height(24.dp)
//                                            .drawWithCache {
//                                                onDrawBehind {
//                                                    val lineWidth = size.width / 3
//                                                    val lineHeight = 4.dp.toPx()
//                                                    val spacing = 8.dp.toPx()
//                                                    drawIntoCanvas {
//                                                        repeat(3) { i ->
//                                                            drawRect(
//                                                                color = Color(0xFFFF3B30).copy(alpha = glow),
//                                                                topLeft = androidx.compose.ui.geometry.Offset(
//                                                                    x = i * (lineWidth + spacing),
//                                                                    y = (size.height - lineHeight) / 2
//                                                                ),
//                                                                size = androidx.compose.ui.geometry.Size(
//                                                                    width = lineWidth,
//                                                                    height = lineHeight
//                                                                ),
//                                                                style = Stroke(
//                                                                    width = 2.dp.toPx(),
//                                                                    cap = StrokeCap.Round
//                                                                )
//                                                            )
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                    )
//                                } else {
//                                    Text(
//                                        text = advice,
//                                        color = Color.White,
//                                        overflow = TextOverflow.Ellipsis,
//                                        maxLines = 10
//                                    )
//                                }
//                            }
//                        }
//                        else {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Last workout:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = lastWorkoutName.ifBlank { "None" },
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
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
