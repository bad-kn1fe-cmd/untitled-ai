package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

@Composable
fun BoykisserOverlay(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    variant: String = "classic", // "classic" vs "3d"
    speedFactor: Float = 1.0f,
    isInChat: Boolean = false,
    customTopYPx: Float? = null,
    behavior: String = "walk" // "walk" vs "sit"
) {
    if (!enabled) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val isWalkMode = behavior.equals("walk", ignoreCase = true)

    var posXPercent by remember { mutableFloatStateOf(if (isWalkMode) 0.12f else 0.76f) }
    var direction by remember { mutableFloatStateOf(1f) } // 1f = right, -1f = left

    val jumpAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Smooth walking loop if behavior is "walk"
    LaunchedEffect(isWalkMode, speedFactor) {
        if (isWalkMode) {
            var time = 0f
            val step = 0.0025f * speedFactor.coerceIn(0.5f, 2.5f)
            while (true) {
                delay(16) // ~60 FPS
                time += step
                posXPercent += direction * step
                if (posXPercent >= 0.82f) {
                    posXPercent = 0.82f
                    direction = -1f
                } else if (posXPercent <= 0.05f) {
                    posXPercent = 0.05f
                    direction = 1f
                }
            }
        } else {
            posXPercent = 0.76f
        }
    }

    val resId = if (variant.equals("3d", ignoreCase = true)) R.drawable.boykisser_3d else R.drawable.boykisser_classic

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val screenHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }

        val petHeightPx = with(density) { 48.dp.toPx() }
        val xPx = screenWidthPx * posXPercent
        val baseYPx = if (isInChat && customTopYPx != null && customTopYPx > 100f) {
            (customTopYPx - petHeightPx + with(density) { 2.dp.toPx() }).coerceAtLeast(60f)
        } else if (isInChat) {
            (screenHeightPx - with(density) { 180.dp.toPx() }).coerceAtLeast(80f)
        } else {
            (screenHeightPx - with(density) { 108.dp.toPx() }).coerceAtLeast(80f)
        }

        val jumpOffsetPx = with(density) { jumpAnim.value.dp.toPx() }
        val finalYPx = baseYPx + jumpOffsetPx

        Box(
            modifier = Modifier
                .offset { IntOffset(xPx.roundToInt(), finalYPx.roundToInt()) }
                .size(52.dp)
                .graphicsLayer {
                    scaleX = if (isWalkMode) direction else 1f
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.performLightHaptic(context, true)
                    coroutineScope.launch {
                        jumpAnim.animateTo(
                            targetValue = -32f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        jumpAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }
                }
        ) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = "Boykisser Pet",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
