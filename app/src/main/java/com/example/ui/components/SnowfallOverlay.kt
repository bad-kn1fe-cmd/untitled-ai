package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

private enum class SnowType {
    DOT,
    FLAKE_SHAPE_1,
    FLAKE_SHAPE_2
}

private class SnowParticle(
    var spawnX: Float,
    var currentX: Float,
    var currentY: Float,
    var speedY: Float,
    var swayAmpPx: Float,
    var swayFreq: Float,
    var radiusPx: Float,
    var baseAlpha: Float,
    var dropLimitRatio: Float,
    var rotationDeg: Float,
    var rotationSpeed: Float,
    var ageSec: Float,
    var type: SnowType
)

private fun createEmojiBitmap(emoji: String, sizePx: Int): Bitmap {
    val safeSize = sizePx.coerceAtLeast(32)
    val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = safeSize * 0.76f
        textAlign = Paint.Align.CENTER
    }
    val fontMetrics = paint.fontMetrics
    val textY = (safeSize / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(emoji, safeSize / 2f, textY, paint)
    return bitmap
}

/**
 * Seasonal Snowfall Overlay (triggered when "event" in info.json is "newyear").
 *
 * Lightweight, zero-recomposition Canvas-driven snowfall with varied particle depth,
 * realistic air resistance sway, and configurable drop height/opacity.
 */
@Composable
fun SnowfallOverlay(
    isActive: Boolean,
    dropLimit: Float = 0.85f,
    snowAlpha: Float = 0.65f,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val density = LocalDensity.current
    var screenWidthPx by remember { mutableFloatStateOf(1080f) }
    var screenHeightPx by remember { mutableFloatStateOf(2400f) }

    val particleCount = 42
    val particles = remember { ArrayList<SnowParticle>(particleCount) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var isInitialized by remember { mutableStateOf(false) }

    val baseFlakeSizePx = remember(density) { with(density) { 20.dp.toPx().toInt().coerceAtLeast(32) } }
    val flake1Bitmap = remember(baseFlakeSizePx) { createEmojiBitmap("❄", baseFlakeSizePx) }
    val flake2Bitmap = remember(baseFlakeSizePx) { createEmojiBitmap("❅", baseFlakeSizePx) }

    val drawPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
    val dstRect = remember { RectF() }

    val effectiveDropLimit = dropLimit.coerceIn(0.15f, 1.0f)

    // Helper to spawn/respawn a particle
    fun initParticle(
        p: SnowParticle,
        width: Float,
        height: Float,
        randomizeY: Boolean
    ) {
        val rand = Random.nextFloat()
        val type = when {
            rand < 0.22f -> SnowType.FLAKE_SHAPE_1
            rand < 0.36f -> SnowType.FLAKE_SHAPE_2
            else -> SnowType.DOT
        }

        val spawnX = Random.nextFloat() * (width + 40f) - 20f
        val dropRatio = Random.nextFloat() * 0.30f + 0.70f // 70% to 100% of setting limit

        val (radiusDp, speedDp, baseAlpha) = when (type) {
            SnowType.DOT -> {
                val sizeTier = Random.nextFloat()
                when {
                    sizeTier < 0.60f -> Triple(Random.nextFloat() * 1.5f + 1.5f, Random.nextFloat() * 35f + 40f, Random.nextFloat() * 0.35f + 0.35f)
                    sizeTier < 0.90f -> Triple(Random.nextFloat() * 1.8f + 3.0f, Random.nextFloat() * 45f + 65f, Random.nextFloat() * 0.30f + 0.55f)
                    else -> Triple(Random.nextFloat() * 2.2f + 5.0f, Random.nextFloat() * 55f + 95f, Random.nextFloat() * 0.25f + 0.70f)
                }
            }
            SnowType.FLAKE_SHAPE_1, SnowType.FLAKE_SHAPE_2 -> {
                Triple(Random.nextFloat() * 3.5f + 7.5f, Random.nextFloat() * 40f + 55f, Random.nextFloat() * 0.30f + 0.60f)
            }
        }

        val sizePx = with(density) { radiusDp.dp.toPx() }
        val speedPx = with(density) { speedDp.dp.toPx() }
        val swayAmp = with(density) { (Random.nextFloat() * 16f + 8f).dp.toPx() }
        val swayFreq = Random.nextFloat() * 1.4f + 0.8f

        p.spawnX = spawnX
        p.currentX = spawnX
        p.currentY = if (randomizeY) Random.nextFloat() * (height * effectiveDropLimit) else -sizePx - Random.nextFloat() * 40f
        p.speedY = speedPx
        p.swayAmpPx = swayAmp
        p.swayFreq = swayFreq
        p.radiusPx = sizePx
        p.baseAlpha = baseAlpha
        p.dropLimitRatio = dropRatio
        p.rotationDeg = Random.nextFloat() * 360f
        p.rotationSpeed = Random.nextFloat() * 50f - 25f
        p.ageSec = Random.nextFloat() * 10f
        p.type = type
    }

    // Initialize particle pool once screen dimensions are available
    LaunchedEffect(isActive, screenWidthPx, screenHeightPx) {
        if (!isInitialized && screenWidthPx > 50f && screenHeightPx > 50f) {
            particles.clear()
            for (i in 0 until particleCount) {
                val dummy = SnowParticle(
                    spawnX = 0f,
                    currentX = 0f,
                    currentY = 0f,
                    speedY = 100f,
                    swayAmpPx = 20f,
                    swayFreq = 1f,
                    radiusPx = 6f,
                    baseAlpha = 0.7f,
                    dropLimitRatio = 1f,
                    rotationDeg = 0f,
                    rotationSpeed = 0f,
                    ageSec = 0f,
                    type = SnowType.DOT
                )
                initParticle(dummy, screenWidthPx, screenHeightPx, randomizeY = true)
                particles.add(dummy)
            }
            isInitialized = true
        }
    }

    // Physics animation loop: native display refresh with ZERO recompositions
    LaunchedEffect(isActive, isInitialized) {
        var lastTimeNanos = 0L
        while (isActive) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos != 0L && isInitialized) {
                    val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    val fullLimitY = screenHeightPx * effectiveDropLimit

                    for (i in 0 until particles.size) {
                        val p = particles[i]
                        p.ageSec += dt
                        p.currentY += p.speedY * dt
                        p.currentX = p.spawnX + sin(p.ageSec * p.swayFreq) * p.swayAmpPx
                        p.rotationDeg = (p.rotationDeg + p.rotationSpeed * dt) % 360f

                        val particleMaxY = fullLimitY * p.dropLimitRatio
                        if (p.currentY >= particleMaxY) {
                            initParticle(p, screenWidthPx, screenHeightPx, randomizeY = false)
                        }
                    }
                    if (particles.isNotEmpty()) {
                        frameTick++
                    }
                }
                lastTimeNanos = nowNanos
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenWidthPx = size.width
            screenHeightPx = size.height

            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick
            val fullSettingLimitY = size.height * effectiveDropLimit
            val clampedUserAlpha = snowAlpha.coerceIn(0.05f, 1.0f)

            for (i in 0 until particles.size) {
                val p = particles[i]
                val particleLimitY = fullSettingLimitY * p.dropLimitRatio

                if (p.currentY in -50f..particleLimitY) {
                    val remainingDistance = particleLimitY - p.currentY
                    val fadeRange = (p.radiusPx * 5f).coerceAtLeast(35f)
                    val fadeFactor = (remainingDistance / fadeRange).coerceIn(0f, 1f)

                    val computedAlpha = (p.baseAlpha * clampedUserAlpha * fadeFactor).coerceIn(0f, 1f)

                    when (p.type) {
                        SnowType.DOT -> {
                            drawCircle(
                                color = Color.White.copy(alpha = computedAlpha),
                                radius = p.radiusPx,
                                center = androidx.compose.ui.geometry.Offset(p.currentX, p.currentY)
                            )
                        }
                        SnowType.FLAKE_SHAPE_1, SnowType.FLAKE_SHAPE_2 -> {
                            val bmp = if (p.type == SnowType.FLAKE_SHAPE_1) flake1Bitmap else flake2Bitmap
                            val half = p.radiusPx
                            dstRect.set(-half, -half, half, half)
                            drawPaint.alpha = (computedAlpha * 255f).toInt().coerceIn(0, 255)

                            val nativeCanvas = drawContext.canvas.nativeCanvas
                            nativeCanvas.save()
                            nativeCanvas.translate(p.currentX, p.currentY)
                            nativeCanvas.rotate(p.rotationDeg)
                            nativeCanvas.drawBitmap(bmp, null, dstRect, drawPaint)
                            nativeCanvas.restore()
                        }
                    }
                }
            }
        }
    }
}
