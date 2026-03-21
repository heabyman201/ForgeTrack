package com.forgecompose.workouttracker

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.launch

@Composable
fun Tutorial() {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val backdrop = rememberLayerBackdrop()


        val animationScope = rememberCoroutineScope()
        val progressAnimation = remember { Animatable(0f) }

        Box(
            Modifier
                .graphicsLayer {
                    val progress = progressAnimation.value
                    val scale = lerps(1f, 1.1f, progress)
                    scaleX = scale
                    scaleY = scale
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(32f.dp) },
                    effects = {
                        vibrancy()
                        blur(4f.dp.toPx())
                        refraction(
                            height = 24f.dp.toPx(),
                            amount = 48f.dp.toPx(),
                            hasDepthEffect = true
                        )
                    }
                )
            .clickable {}
            .pointerInput(animationScope) {
                val animationSpec = spring(0.5f, 300f, 0.001f)
                awaitEachGesture {
                    // press
                    awaitFirstDown()
                    animationScope.launch {
                        progressAnimation.animateTo(1f, animationSpec)
                    }

                    // release
                    waitForUpOrCancellation()
                    animationScope.launch {
                        progressAnimation.animateTo(0f, animationSpec)
                    }
                }
            }
            .size(320f.dp, 180f.dp),
        contentAlignment = Alignment.Center
        ) {
            BasicText(
                "Finish Text",
                style = TextStyle(color = Color.White, fontSize = 32f.sp, fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Preview
@Composable
fun TutorialPreview() {
    Tutorial()
}