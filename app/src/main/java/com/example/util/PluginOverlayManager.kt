package com.example.util

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.ui.components.AppAsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class PluginOverlayItem(
    val id: String,
    val pluginId: String = "",
    val imageUrl: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val widthDp: Float = 52f,
    val heightDp: Float = 52f,
    val alpha: Float = 1.0f,
    val rotation: Float = 0f,
    val scale: Float = 1.0f,
    val animationDurationMs: Int = 300
)

object PluginOverlayManager {

    private val _overlayItems = MutableStateFlow<Map<String, PluginOverlayItem>>(emptyMap())
    val overlayItems: StateFlow<Map<String, PluginOverlayItem>> = _overlayItems.asStateFlow()

    fun showImage(
        id: String,
        imageUrl: String,
        x: Float = 0.5f,
        y: Float = 0.5f,
        width: Float = 52f,
        height: Float = 52f,
        alpha: Float = 1.0f,
        pluginId: String = ""
    ) {
        val item = PluginOverlayItem(
            id = id,
            pluginId = pluginId,
            imageUrl = resolvePresetOrUrl(imageUrl),
            xFraction = x.coerceIn(0.0f, 1.0f),
            yFraction = y.coerceIn(0.0f, 1.0f),
            widthDp = width.coerceIn(16f, 320f),
            heightDp = height.coerceIn(16f, 320f),
            alpha = alpha.coerceIn(0.0f, 1.0f)
        )
        _overlayItems.value = _overlayItems.value + (id to item)
    }

    fun animateImage(
        id: String,
        targetX: Float,
        targetY: Float,
        durationMs: Int = 400,
        rotation: Float = 0f,
        scale: Float = 1.0f
    ) {
        val current = _overlayItems.value[id] ?: return
        val updated = current.copy(
            xFraction = targetX.coerceIn(0.0f, 1.0f),
            yFraction = targetY.coerceIn(0.0f, 1.0f),
            rotation = rotation,
            scale = scale.coerceIn(0.2f, 4.0f),
            animationDurationMs = durationMs.coerceIn(50, 5000)
        )
        _overlayItems.value = _overlayItems.value + (id to updated)
    }

    fun hideImage(id: String) {
        _overlayItems.value = _overlayItems.value - id
    }

    fun clearForPlugin(pluginId: String) {
        if (pluginId.isBlank()) return
        _overlayItems.value = _overlayItems.value.filterValues { it.pluginId != pluginId }
    }

    fun clear() {
        _overlayItems.value = emptyMap()
    }

    private fun resolvePresetOrUrl(preset: String): String {
        return when (preset.lowercase().trim()) {
            "sparkle", "star" -> "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/akira_classic.png"
            "heart" -> "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/boykisser_classic.png"
            "pochita" -> "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/pochitta_classic.png"
            else -> preset
        }
    }
}

@Composable
fun PluginOverlayHost(
    modifier: Modifier = Modifier
) {
    val items by PluginOverlayManager.overlayItems.collectAsState()
    if (items.isEmpty()) return

    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize().zIndex(999f)) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        items.values.forEach { item ->
            key(item.id) {
                val animSpec = tween<Float>(
                    durationMillis = item.animationDurationMs,
                    easing = FastOutSlowInEasing
                )

                val animatedX by animateFloatAsState(
                    targetValue = item.xFraction,
                    animationSpec = animSpec,
                    label = "overlay_x_${item.id}"
                )
                val animatedY by animateFloatAsState(
                    targetValue = item.yFraction,
                    animationSpec = animSpec,
                    label = "overlay_y_${item.id}"
                )
                val animatedRot by animateFloatAsState(
                    targetValue = item.rotation,
                    animationSpec = animSpec,
                    label = "overlay_rot_${item.id}"
                )
                val animatedScale by animateFloatAsState(
                    targetValue = item.scale,
                    animationSpec = animSpec,
                    label = "overlay_scale_${item.id}"
                )
                val animatedAlpha by animateFloatAsState(
                    targetValue = item.alpha,
                    animationSpec = animSpec,
                    label = "overlay_alpha_${item.id}"
                )

                val xPx = screenWidthPx * animatedX - with(density) { (item.widthDp / 2).dp.toPx() }
                val yPx = screenHeightPx * animatedY - with(density) { (item.heightDp / 2).dp.toPx() }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                        .size(item.widthDp.dp, item.heightDp.dp)
                        .alpha(animatedAlpha)
                        .rotate(animatedRot)
                        .scale(animatedScale)
                ) {
                    AppAsyncImage(
                        model = item.imageUrl,
                        contentDescription = "Plugin Overlay Item",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
