package com.forgecompose.workouttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun QuickStartWorkout(navController: NavController) {
    val context = LocalContext.current
    val appearanceOptions by AppearanceOptionsManagerAppTheme.flow(context).collectAsState(initial = AppearanceOptionsAppTheme.Defaults)
    val theme = appearanceOptions.selectedTheme.colors

    val haptics = LocalHapticFeedback.current

    val textBrush = remember(theme) {
        Brush.horizontalGradient(
            colors = listOf(
                theme.primary,
                Color.White.copy(alpha = 0.9f)
            )
        )
    }
    val dynamicTexts = listOf(
        "Select a new workout",
        "Pick a new workout",
        "Start a new workout"
    )

    val chosenText = remember { dynamicTexts.random() }

    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            navController.navigate("WorkoutSelector")
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = chosenText,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    brush = textBrush
                )
            )
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Start workout",
                tint = theme.primary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
            )
        }
    }
}