package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

data class BubbleItemData(
    val id: Long,
    var xFrac: Float,
    var yFrac: Float,
    val speed: Float,
    val sizeDp: Int,
    val emoji: String,
    var isPopped: Boolean = false
)

@Composable
fun BubblesRainOverlay(
    isActive: Boolean,
    count: Int = 15,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val context = LocalContext.current
    val density = LocalDensity.current

    val bubbles = remember(count) {
        val emojis = listOf("🫧", "🫧", "🧼", "✨", "🫧")
        mutableStateListOf<BubbleItemData>().apply {
            for (i in 0 until count) {
                add(
                    BubbleItemData(
                        id = i.toLong(),
                        xFrac = Random.nextFloat() * 0.90f + 0.05f,
                        yFrac = Random.nextFloat(),
                        speed = Random.nextFloat() * 0.003f + 0.0015f,
                        sizeDp = Random.nextInt(28, 48),
                        emoji = emojis.random()
                    )
                )
            }
        }
    }

    LaunchedEffect(isActive, count) {
        while (isActive) {
            delay(16) // ~60 FPS
            for (b in bubbles) {
                if (!b.isPopped) {
                    b.yFrac -= b.speed // Float upwards
                    b.xFrac += (Random.nextFloat() - 0.5f) * 0.001f
                    if (b.yFrac < -0.1f) {
                        b.yFrac = 1.1f
                        b.xFrac = Random.nextFloat() * 0.90f + 0.05f
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val screenHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }

        bubbles.forEach { bubble ->
            if (!bubble.isPopped) {
                val xPx = screenWidthPx * bubble.xFrac.coerceIn(0.02f, 0.92f)
                val yPx = screenHeightPx * bubble.yFrac.coerceIn(-0.1f, 1.1f)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                        .size(bubble.sizeDp.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            HapticHelper.performLightHaptic(context, true)
                            bubble.isPopped = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bubble.emoji,
                        fontSize = (bubble.sizeDp * 0.75f).sp
                    )
                }
            }
        }
    }
}
