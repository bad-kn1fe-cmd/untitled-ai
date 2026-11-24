package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

@Composable
fun Modifier.motionBlurOnScroll(
    enabled: Boolean,
    listState: LazyListState,
    intensity: Float
): Modifier {
    if (!enabled) return this

    var isActivelyMoving by remember { mutableStateOf(false) }
    var lastOffset by remember { mutableIntStateOf(0) }
    var lastIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            isActivelyMoving = false
        } else {
            val currIndex = listState.firstVisibleItemIndex
            val currOffset = listState.firstVisibleItemScrollOffset
            val delta = abs((currIndex - lastIndex) * 800 + (currOffset - lastOffset))

            lastIndex = currIndex
            lastOffset = currOffset

            // Only blur if actually moving > 4px between scroll ticks
            isActivelyMoving = delta > 4
        }
    }

    val targetBlur = if (isActivelyMoving && listState.isScrollInProgress) intensity.coerceIn(2f, 25f) else 0f
    val animatedBlur by animateFloatAsState(
        targetValue = targetBlur,
        animationSpec = tween(durationMillis = 60, easing = LinearEasing),
        label = "motion_blur_anim"
    )

    if (animatedBlur <= 0.3f) return this

    return this.graphicsLayer {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Directional vertical motion blur along Y movement axis
            renderEffect = android.graphics.RenderEffect.createBlurEffect(
                0.1f,
                animatedBlur,
                android.graphics.Shader.TileMode.CLAMP
            ).asComposeRenderEffect()
        } else {
            alpha = (1f - (animatedBlur / 90f)).coerceAtLeast(0.88f)
        }
    }
}
