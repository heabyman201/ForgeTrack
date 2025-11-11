package com.forgecompose.workouttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
fun QuickStartWorkout(navController: NavController){
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current


    val textBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFCBFC3),
                Color.White.copy(alpha = 0.9f)
            )
        )
    }

    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            navController.navigate("WorkoutSelector")
        },

        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Start a new workout",
                modifier = Modifier.weight(1f),

                style = TextStyle(
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    brush = textBrush
                )
            )
            Icon(
                Icons.Default.ArrowForward, contentDescription = "Start a new workout",
                tint = Color.White,

                modifier = Modifier.padding(end = 8.dp).size(32.dp)
            )
        }
    }
}