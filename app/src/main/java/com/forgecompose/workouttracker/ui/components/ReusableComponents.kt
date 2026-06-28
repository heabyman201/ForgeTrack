package com.forgecompose.workouttracker.ui.components

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Legacy entry point — now delegates to the single [ForgeCard] so every screen that
 * already uses GlassCard inherits the unified card language (24dp, top-left→bottom-right
 * light-source gradient, hairline border, top catch-light, spring press feedback).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ForgeCard(
        modifier = modifier.fillMaxWidth(),
        elevation = ForgeElevation.Standard
    ) {
        content()
    }
}

@Composable
fun InfoChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .drawWithCache {
                onDrawWithContent {
                    val r = size.height / 2f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.05f),
                        cornerRadius = CornerRadius(r)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.1f),
                        style = Stroke(width = 1.dp.toPx()),
                        cornerRadius = CornerRadius(r)
                    )
                    drawContent()
                }
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatusChip(status: WorkoutStatus) {
    val (bg, fg) = when (status) {
        WorkoutStatus.PLANNED -> Color(0xFF1E3A5F) to Color(0xFFB3D4FF)
        WorkoutStatus.IN_PROGRESS -> Color(0xFF5F3A1E) to Color(0xFFFFD7A3)
        WorkoutStatus.COMPLETED -> Color(0xFF285F1E) to Color(0xFFB3FFC2)
        WorkoutStatus.SKIPPED -> Color(0xFF5F1E1E) to Color(0xFFFFB3B3)
    }
    Box(
        modifier = Modifier
            .drawWithCache {
                onDrawWithContent {
                    val r = size.height / 2f
                    drawRoundRect(
                        color = bg.copy(alpha = 0.3f),
                        cornerRadius = CornerRadius(r)
                    )
                    drawRoundRect(
                        color = fg.copy(alpha = 0.4f),
                        style = Stroke(width = 1.dp.toPx()),
                        cornerRadius = CornerRadius(r)
                    )
                    drawContent()
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = status.name.replace('_', ' ').let { it[0].uppercase() + it.substring(1).lowercase() },
            color = fg,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun LabeledStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(min = 0.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun LoadingBlock(padding: PaddingValues) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = theme.primary)
    }
}

@Composable
fun ErrorBlock(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text("Error loading workouts.", color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun MissingBlock(padding: PaddingValues) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text("Workout not found.", color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
fun EmptyState() {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val accent = theme.primary
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = "No History",
                modifier = Modifier
                    .size(80.dp)
                    .drawWithCache {
                        val glow = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.3f), Color.Transparent),
                            radius = size.width * 0.7f
                        )
                        onDrawBehind { drawCircle(glow) }
                    },
                tint = accent.copy(alpha = 0.8f)
            )
            Text(
                text = "No workouts recorded yet.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Legacy entry point — now delegates to [ForgeCard] with the crimson glow on, keeping
 * its "emits light" character while joining the one card language. Callers pad their
 * own content, so contentPadding is zero here.
 */
@Composable
fun GlowingCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    ForgeCard(
        modifier = modifier,
        elevation = ForgeElevation.Standard,
        glow = true,
        contentPadding = PaddingValues(0.dp)
    ) {
        content()
    }
}

@Composable
fun ThemedConfirmationDialog(
    title: String,
    text: String,
    buttonText: String,
    additionalButton: Boolean,
    additionalButtonText: String,
    onConfirm: () -> Unit,
    onCustomAction: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.colors

    Dialog(onDismissRequest = onDismiss) {
        val cornerRadius = 28.dp
        Box(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .drawWithCache {
                    val cornerRpx = cornerRadius.toPx()
                    val bgBrush = Brush.radialGradient(
                        colors = listOf(theme.tertiary, theme.background),
                        radius = size.width
                    )
                    val borderBrush = Brush.linearGradient(
                        colors = listOf(theme.primary.copy(alpha = 0.5f), theme.secondary.copy(alpha = 0.3f))
                    )
                    onDrawBehind {
                        drawRoundRect(brush = bgBrush, cornerRadius = CornerRadius(cornerRpx))
                        drawRoundRect(brush = borderBrush, style = Stroke(width = 1.5.dp.toPx()), cornerRadius = CornerRadius(cornerRpx))
                    }
                }
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.secondary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(buttonText)
                    }

                }
                if (additionalButton){
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onCustomAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.secondary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(additionalButtonText)
                        }
                    }
                }
            }
        }
    }
}
