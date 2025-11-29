package com.forgecompose.workouttracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun BadgeSection(
    badges: List<BadgeUiState>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme
        .flow(context)
        .collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                badges.filter { !it.isUnlocked }.take(5),
                key = { it.id }
            ) { badge ->
                BadgeCard(badge = badge, theme = theme)
            }
        }
    }
}

@Composable
fun BadgeCard(
    badge: BadgeUiState,
    theme: ColorSchemeAppTheme,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)

    val borderBrush = Brush.linearGradient(
        colors = if (badge.isUnlocked) {
            listOf(
                theme.primary.copy(alpha = 0.95f),
                theme.secondary.copy(alpha = 0.85f)
            )
        } else {
            listOf(
                theme.primary.copy(alpha = 0.2f),
                theme.secondary.copy(alpha = 0.1f)
            )
        }
    )

    val backgroundBrush = Brush.linearGradient(
        colors = listOf(
            theme.secondary.copy(alpha = 0.22f),
            theme.tertiary.copy(alpha = 0.12f)
        )
    )

    Card(
        modifier = modifier
            .width(210.dp)
            .heightIn(min = 120.dp)
            .border(
                width = 1.4.dp,
                brush = borderBrush,
                shape = cardShape
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (badge.isUnlocked)
                            theme.primary
                        else
                            theme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )

                    Text(
                        text = badge.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = theme.primary.copy(alpha = 0.9f)
                    )
                }

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.primary.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { badge.progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    trackColor = theme.secondary.copy(alpha = 0.4f),
                    color = if (badge.isUnlocked)
                        theme.primary
                    else
                        theme.primary.copy(alpha = 0.6f)
                )
                Text(
                    text = "${(badge.progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = theme.primary
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = if (badge.isUnlocked) "Unlocked" else "In progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (badge.isUnlocked)
                        theme.primary
                    else
                        theme.primary.copy(alpha = 0.6f)
                )
            }
        }
    }
}