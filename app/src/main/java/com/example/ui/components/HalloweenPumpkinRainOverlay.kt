package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

class PumpkinParticle(
    val id: Long,
    val spawnX: Float,
    val speedY: Float,
    val sizePx: Float,
    val swayAmpPx: Float,
    val swayFreq: Float,
    val isBlack: Boolean,
    val isSmiley: Boolean = false,
    val dropLimitRatio: Float, // Staggered ratio between 0.65f and 1.0f: some vanish sooner, some later, never beyond setting limit
    val rotationSpeed: Float,  // Physics angular velocity (deg/sec)
    var rotationDeg: Float = 0f,
    var currentY: Float = -60f,
    var currentX: Float = spawnX,
    var ageSec: Float = 0f
)

private fun createEmojiBitmap(emoji: String, sizePx: Int): Bitmap {
    val safeSize = sizePx.coerceAtLeast(32)
    val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = safeSize * 0.78f
        textAlign = Paint.Align.CENTER
    }
    val fontMetrics = paint.fontMetrics
    val textY = (safeSize / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
    canvas.drawText(emoji, safeSize / 2f, textY, paint)
    return bitmap
}

@Composable
fun HalloweenPumpkinRainOverlay(
    isActive: Boolean,
    onBlackPumpkinTapped: () -> Unit,
    onSecretSmileyTapped: (() -> Unit)? = null,
    dropLimit: Float = 0.7f,
    pumpkinAlpha: Float = 0.45f,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val context = LocalContext.current
    val density = LocalDensity.current
    var screenWidthPx by remember { mutableFloatStateOf(1080f) }
    var screenHeightPx by remember { mutableFloatStateOf(2400f) }

    // Plain ArrayList instead of SnapshotStateList to avoid Compose recomposition during tab swipes
    val pumpkins = remember { ArrayList<PumpkinParticle>(8) }
    var pumpkinCounter by remember { mutableLongStateOf(0L) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var hasActiveSkull by remember { mutableStateOf(false) }
    var hasActiveSmiley by remember { mutableStateOf(false) }
    var showSecretDialog by remember { mutableStateOf(false) }

    // Pre-rendered emoji bitmaps so GPU draws textures with true alpha transparency & zero per-frame font shaping
    val baseBitmapSizePx = remember(density) { with(density) { 30.dp.toPx().toInt().coerceAtLeast(48) } }
    val pumpkinBitmap = remember(baseBitmapSizePx) { createEmojiBitmap("🎃", baseBitmapSizePx) }
    val skullBitmap = remember(baseBitmapSizePx) { createEmojiBitmap("💀", baseBitmapSizePx) }
    val smileyBitmap = remember(baseBitmapSizePx) { createEmojiBitmap("🙂", baseBitmapSizePx) }

    val drawPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    }
    val dstRect = remember { RectF() }

    val effectiveDropLimit = dropLimit.coerceIn(0.15f, 1.0f)
    val maxLimitY = screenHeightPx * effectiveDropLimit

    // Spawner loop: adds a pumpkin every 1.1-2.0s if under limit
    LaunchedEffect(isActive) {
        while (isActive) {
            if (pumpkins.size < 5 && screenWidthPx > 100f) {
                val rand = Random.nextFloat()
                val isSmiley = rand < 0.005f // 0.5% chance for secret smiling iOS emoji
                val isBlack = !isSmiley && (rand < 0.040f) // 3.5% chance for black pumpkin / skull

                val spawnX = Random.nextFloat() * (screenWidthPx - 100f) + 50f
                val sizeDp = if (isBlack || isSmiley) 28f else (Random.nextFloat() * 7f + 20f)
                val sizePx = with(density) { sizeDp.dp.toPx() }
                val speed = with(density) { (Random.nextFloat() * 55f + 105f).dp.toPx() } // px/sec

                // Staggered limit ratio: some disappear earlier (e.g. 68% of limit), some later (up to 100% of setting limit), but NEVER beyond!
                val dropRatio = Random.nextFloat() * 0.35f + 0.65f
                // Physics rotational velocity: spinning gently or swinging with air resistance
                val rotSpeed = (Random.nextFloat() * 70f - 35f)

                pumpkins.add(
                    PumpkinParticle(
                        id = ++pumpkinCounter,
                        spawnX = spawnX,
                        speedY = speed,
                        sizePx = sizePx,
                        swayAmpPx = with(density) { (Random.nextFloat() * 14f + 6f).dp.toPx() },
                        swayFreq = Random.nextFloat() * 1.8f + 1.2f,
                        isBlack = isBlack,
                        isSmiley = isSmiley,
                        dropLimitRatio = dropRatio,
                        rotationSpeed = rotSpeed,
                        rotationDeg = Random.nextFloat() * 360f,
                        currentY = -sizePx - 10f,
                        currentX = spawnX
                    )
                )
                if (isBlack) {
                    hasActiveSkull = true
                }
                if (isSmiley) {
                    hasActiveSmiley = true
                }
            }
            delay(Random.nextLong(1000, 1900))
        }
    }

    // Animation physics loop: runs at native display refresh with ZERO recompositions
    LaunchedEffect(isActive, maxLimitY) {
        var lastTimeNanos = 0L
        while (isActive) {
            withFrameNanos { nowNanos ->
                if (lastTimeNanos != 0L) {
                    val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    var skullStillPresent = false
                    var smileyStillPresent = false
                    val iterator = pumpkins.iterator()
                    while (iterator.hasNext()) {
                        val p = iterator.next()
                        p.ageSec += dt
                        p.currentY += p.speedY * dt
                        p.currentX = p.spawnX + sin(p.ageSec * p.swayFreq) * p.swayAmpPx
                        
                        // Physics rotation: combination of continuous angular spin and aerodynamic sway tilt
                        val swayTilt = kotlin.math.cos(p.ageSec * p.swayFreq) * 16f
                        p.rotationDeg = (p.rotationDeg + p.rotationSpeed * dt + swayTilt * dt * 4f) % 360f

                        val particleMaxY = screenHeightPx * effectiveDropLimit * p.dropLimitRatio
                        if (p.currentY >= particleMaxY) {
                            iterator.remove()
                        } else {
                            if (p.isBlack) skullStillPresent = true
                            if (p.isSmiley) smileyStillPresent = true
                        }
                    }
                    if (hasActiveSkull != skullStillPresent) {
                        hasActiveSkull = skullStillPresent
                    }
                    if (hasActiveSmiley != smileyStillPresent) {
                        hasActiveSmiley = smileyStillPresent
                    }
                    if (pumpkins.isNotEmpty()) {
                        frameTick++
                    }
                }
                lastTimeNanos = nowNanos
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Pure Canvas drawing: reads frameTick ONLY inside DrawScope -> zero UI tree recomposition
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenWidthPx = size.width
            screenHeightPx = size.height

            @Suppress("UNUSED_VARIABLE")
            val tick = frameTick
            val fullSettingLimitY = size.height * effectiveDropLimit

            for (i in 0 until pumpkins.size) {
                val p = pumpkins[i]
                val particleLimitY = fullSettingLimitY * p.dropLimitRatio
                if (p.currentY < particleLimitY) {
                    val remainingDistance = particleLimitY - p.currentY
                    val fadeRange = p.sizePx * 2.2f
                    val fadeFactor = (remainingDistance / fadeRange).coerceIn(0f, 1f)

                    // Customizable pumpkin opacity with smooth fade near the drop limit
                    val clampedAlpha = pumpkinAlpha.coerceIn(0.05f, 1.0f)
                    val baseAlpha = (255f * clampedAlpha * (if (p.isBlack || p.isSmiley) 1.12f else 1.0f)).coerceIn(0f, 255f)
                    drawPaint.alpha = (baseAlpha * fadeFactor).toInt().coerceIn(0, 255)

                    val half = p.sizePx / 2f
                    dstRect.set(-half, -half, half, half)

                    val bmp = when {
                        p.isSmiley -> smileyBitmap
                        p.isBlack -> skullBitmap
                        else -> pumpkinBitmap
                    }
                    val nativeCanvas = drawContext.canvas.nativeCanvas
                    nativeCanvas.save()
                    nativeCanvas.translate(p.currentX, p.currentY)
                    nativeCanvas.rotate(p.rotationDeg)
                    nativeCanvas.drawBitmap(bmp, null, dstRect, drawPaint)
                    nativeCanvas.restore()
                }
            }
        }

        // Only mount hitbox when a rare skull is actually falling on screen
        if (hasActiveSkull) {
            val halfHitboxPx = with(density) { 24.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        @Suppress("UNUSED_VARIABLE")
                        val tick = frameTick
                        val skull = pumpkins.firstOrNull { it.isBlack && it.currentY in 0f..maxLimitY }
                        if (skull != null) {
                            IntOffset(
                                (skull.currentX - halfHitboxPx).toInt(),
                                (skull.currentY - halfHitboxPx).toInt()
                            )
                        } else {
                            IntOffset(-2000, -2000)
                        }
                    }
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val skullIndex = pumpkins.indexOfFirst { it.isBlack }
                        if (skullIndex >= 0) {
                            pumpkins.removeAt(skullIndex)
                            hasActiveSkull = pumpkins.any { it.isBlack }
                        }
                        onBlackPumpkinTapped()
                    }
            )
        }

        // Hitbox for the rare 0.5% smiling iOS emoji
        if (hasActiveSmiley) {
            val halfHitboxPx = with(density) { 26.dp.toPx() }
            Box(
                modifier = Modifier
                    .offset {
                        @Suppress("UNUSED_VARIABLE")
                        val tick = frameTick
                        val smiley = pumpkins.firstOrNull { it.isSmiley && it.currentY in 0f..maxLimitY }
                        if (smiley != null) {
                            IntOffset(
                                (smiley.currentX - halfHitboxPx).toInt(),
                                (smiley.currentY - halfHitboxPx).toInt()
                            )
                        } else {
                            IntOffset(-2000, -2000)
                        }
                    }
                    .size(52.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val smileyIndex = pumpkins.indexOfFirst { it.isSmiley }
                        if (smileyIndex >= 0) {
                            pumpkins.removeAt(smileyIndex)
                            hasActiveSmiley = pumpkins.any { it.isSmiley }
                        }
                        HapticHelper.performSuccessHaptic(context, true)
                        showSecretDialog = true
                        onSecretSmileyTapped?.invoke()
                    }
            )
        }

        // Notification dialog showing #VERITY with copy button
        if (showSecretDialog) {
            val clipboardManager = LocalClipboardManager.current
            val secretWord = "#VERITY"

            AlertDialog(
                onDismissRequest = { showSecretDialog = false },
                icon = {
                    Text(
                        text = "🙂",
                        fontSize = 38.sp
                    )
                },
                title = {
                    Text(
                        text = "Секретная пасхалка",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Вы поймали улыбающийся смайлик!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = secretWord,
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(secretWord))
                            Toast.makeText(context, "Слово $secretWord скопировано!", Toast.LENGTH_SHORT).show()
                            showSecretDialog = false
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Скопировать",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Скопировать")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSecretDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}

