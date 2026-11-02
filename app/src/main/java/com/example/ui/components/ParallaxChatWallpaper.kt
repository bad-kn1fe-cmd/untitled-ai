package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class WallpaperInfo(
    val id: String,
    val name: String,
    val subtitle: String,
    val isCustom: Boolean = false
) {
    fun displayName(isEn: Boolean): String = if (!isEn) name else when (id) {
        "none" -> "No Background"
        "pattern_chat_icons" -> "Dialogues & Sparks"
        "pattern_stars_constellation" -> "Starry Sky"
        "pattern_geometric_mesh" -> "Neon Geometry"
        "pattern_aurora_gradient" -> "Aurora Gradient"
        "custom" -> "Custom Background"
        else -> name
    }

    fun displaySubtitle(isEn: Boolean): String = if (!isEn) subtitle else when (id) {
        "none" -> "Clean theme color"
        "pattern_chat_icons" -> "Grid of chat icons and stars"
        "pattern_stars_constellation" -> "Night indigo with constellations"
        "pattern_geometric_mesh" -> "Dark graphite with hexagons"
        "pattern_aurora_gradient" -> "Silk emerald and indigo"
        "custom" -> "Import from gallery"
        else -> subtitle
    }
}

val WallpaperPresetsList = listOf(
    WallpaperInfo("none", "Без фона", "Чистый цвет темы"),
    WallpaperInfo("pattern_chat_icons", "Диалоги и искры", "Сетка чат-значков и звезд"),
    WallpaperInfo("pattern_stars_constellation", "Звездное небо", "Ночной индиго с созвездиями"),
    WallpaperInfo("pattern_geometric_mesh", "Неон-геометрия", "Темный графит с гексагонами"),
    WallpaperInfo("pattern_aurora_gradient", "Градиент Аврора", "Шелковый изумруд и индиго"),
    WallpaperInfo("custom", "Свой фон", "Импорт из галереи", isCustom = true)
)

@Composable
fun ProceduralWallpaper(
    preset: String,
    modifier: Modifier = Modifier
) {
    when (preset) {
        "pattern_chat_icons" -> ChatIconsPattern(modifier)
        "pattern_stars_constellation" -> StarsConstellationPattern(modifier)
        "pattern_geometric_mesh" -> GeometricMeshPattern(modifier)
        "pattern_aurora_gradient" -> AuroraGradientPattern(modifier)
        else -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0C101A),
                                Color(0xFF141926),
                                Color(0xFF0D1018)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun ChatIconsPattern(modifier: Modifier = Modifier) {
    val bgBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF0B0E17),
                Color(0xFF131828),
                Color(0xFF0E121F)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(bgBrush)
            .drawWithCache {
                val cellW = 56.dp.toPx()
                val cellH = 56.dp.toPx()
                val cols = (size.width / cellW).toInt() + 2
                val rows = (size.height / cellH).toInt() + 2

                val iconColor1 = Color.White.copy(alpha = 0.07f)
                val iconColor2 = Color(0xFF8BB4F8).copy(alpha = 0.08f)
                val iconColor3 = Color(0xFFD0BCFF).copy(alpha = 0.08f)
                val stroke = Stroke(width = 1.4.dp.toPx())

                val path1 = Path()
                val path2 = Path()
                val path3 = Path()
                val dots1 = mutableListOf<Offset>()
                val dots2 = mutableListOf<Offset>()
                val dots3 = mutableListOf<Offset>()

                val bw = 16.dp.toPx()
                val bh = 11.dp.toPx()
                val cr = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx())
                val rSpark = 7.dp.toPx()
                val hw = 12.dp.toPx()
                val hh = 11.dp.toPx()
                val dotR = 1.6.dp.toPx()
                val spacing = 4.5.dp.toPx()

                for (r in 0 until rows) {
                    val rowOffset = if (r % 2 == 1) cellW * 0.5f else 0f
                    val y = r * cellH + cellH * 0.5f

                    for (c in 0 until cols) {
                        val x = c * cellW + rowOffset
                        val shapeIdx = (r * 3 + c) % 4
                        val bucket = shapeIdx % 3
                        val targetPath = when (bucket) {
                            0 -> path1
                            1 -> path2
                            else -> path3
                        }
                        val targetDots = when (bucket) {
                            0 -> dots1
                            1 -> dots2
                            else -> dots3
                        }

                        when (shapeIdx) {
                            0 -> {
                                targetPath.addRoundRect(
                                    RoundRect(
                                        left = x - bw / 2,
                                        top = y - bh / 2,
                                        right = x + bw / 2,
                                        bottom = y + bh / 2,
                                        cornerRadius = cr
                                    )
                                )
                                targetPath.moveTo(x - bw / 2 + 3.dp.toPx(), y + bh / 2)
                                targetPath.lineTo(x - bw / 2 - 2.dp.toPx(), y + bh / 2 + 3.5.dp.toPx())
                                targetPath.lineTo(x - bw / 2 + 7.dp.toPx(), y + bh / 2)
                                targetPath.close()
                            }
                            1 -> {
                                targetPath.moveTo(x, y - rSpark)
                                targetPath.quadraticTo(x, y, x + rSpark, y)
                                targetPath.quadraticTo(x, y, x, y + rSpark)
                                targetPath.quadraticTo(x, y, x - rSpark, y)
                                targetPath.quadraticTo(x, y, x, y - rSpark)
                                targetPath.close()
                            }
                            2 -> {
                                targetPath.moveTo(x, y + hh * 0.45f)
                                targetPath.cubicTo(x - hw, y - hh * 0.2f, x - hw * 0.5f, y - hh * 0.6f, x, y - hh * 0.2f)
                                targetPath.cubicTo(x + hw * 0.5f, y - hh * 0.6f, x + hw, y - hh * 0.2f, x, y + hh * 0.45f)
                                targetPath.close()
                            }
                            3 -> {
                                targetDots.add(Offset(x - spacing, y))
                                targetDots.add(Offset(x, y))
                                targetDots.add(Offset(x + spacing, y))
                            }
                        }
                    }
                }

                onDrawBehind {
                    drawPath(path1, color = iconColor1, style = stroke)
                    drawPath(path2, color = iconColor2, style = stroke)
                    drawPath(path3, color = iconColor3, style = stroke)
                    dots1.forEach { drawCircle(iconColor1, radius = dotR, center = it) }
                    dots2.forEach { drawCircle(iconColor2, radius = dotR, center = it) }
                    dots3.forEach { drawCircle(iconColor3, radius = dotR, center = it) }
                }
            }
    )
}

@Composable
fun StarsConstellationPattern(modifier: Modifier = Modifier) {
    val bgBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF080B14),
                Color(0xFF10152B),
                Color(0xFF18112C),
                Color(0xFF0A0D18)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(bgBrush)
            .drawWithCache {
                val starColor = Color.White.copy(alpha = 0.18f)
                val dimStar = Color(0xFFC5CAE9).copy(alpha = 0.09f)
                val lineColor = Color(0xFF7986CB).copy(alpha = 0.06f)

                val cols = 6
                val rows = 12
                val cellW = size.width / cols
                val cellH = size.height / rows

                val starPoints = ArrayList<Offset>(cols * rows)
                val sparkLinesPath = Path()
                val constellationPath = Path()
                val s = 6.dp.toPx()

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val pseudoRandomX = ((c * 37 + r * 59) % 100) / 100f
                        val pseudoRandomY = ((c * 43 + r * 71) % 100) / 100f
                        val px = c * cellW + pseudoRandomX * cellW
                        val py = r * cellH + pseudoRandomY * cellH
                        starPoints.add(Offset(px, py))

                        if ((c + r) % 5 == 0) {
                            sparkLinesPath.moveTo(px - s, py)
                            sparkLinesPath.lineTo(px + s, py)
                            sparkLinesPath.moveTo(px, py - s)
                            sparkLinesPath.lineTo(px, py + s)
                        }
                    }
                }

                for (i in 0 until starPoints.size - 1) {
                    if (i % 3 == 0 && i + 1 < starPoints.size) {
                        val p1 = starPoints[i]
                        val p2 = starPoints[i + 1]
                        if ((p1 - p2).getDistance() < cellW * 1.6f) {
                            constellationPath.moveTo(p1.x, p1.y)
                            constellationPath.lineTo(p2.x, p2.y)
                        }
                    }
                }

                val sparkStroke = Stroke(width = 1.2.dp.toPx())
                val lineStroke = Stroke(width = 1.dp.toPx())
                val rSparkCenter = 2.dp.toPx()
                val rDim1 = 2.2.dp.toPx()
                val rDim2 = 1.4.dp.toPx()

                onDrawBehind {
                    drawPath(constellationPath, color = lineColor, style = lineStroke)
                    drawPath(sparkLinesPath, color = starColor, style = sparkStroke)
                    var idx = 0
                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val pt = starPoints[idx++]
                            if ((c + r) % 5 == 0) {
                                drawCircle(Color.White.copy(alpha = 0.25f), radius = rSparkCenter, center = pt)
                            } else {
                                val rad = if ((c * 7 + r) % 3 == 0) rDim1 else rDim2
                                drawCircle(dimStar, radius = rad, center = pt)
                            }
                        }
                    }
                }
            }
    )
}

@Composable
fun GeometricMeshPattern(modifier: Modifier = Modifier) {
    val bgBrush = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF090C11),
                Color(0xFF101724),
                Color(0xFF0A0F17)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(bgBrush)
            .drawWithCache {
                val step = 28.dp.toPx()
                val cols = (size.width / step).toInt() + 1
                val rows = (size.height / step).toInt() + 1

                val dotColor = Color.White.copy(alpha = 0.05f)
                val accentColor = Color(0xFF64FFDA).copy(alpha = 0.10f)
                val stroke = Stroke(width = 1.2.dp.toPx())
                val accentPath = Path()
                val dots = ArrayList<Offset>(cols * rows)
                val s = 3.5.dp.toPx()
                val rad = 5.dp.toPx()
                val dotR = 1.2.dp.toPx()

                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val x = c * step
                        val y = r * step

                        if ((c + r) % 6 == 0) {
                            accentPath.moveTo(x - s, y)
                            accentPath.lineTo(x + s, y)
                            accentPath.moveTo(x, y - s)
                            accentPath.lineTo(x, y + s)
                        } else if ((c * 2 + r * 3) % 11 == 0) {
                            for (i in 0 until 6) {
                                val angle = (i * 60f * PI / 180f).toFloat()
                                val hx = x + rad * cos(angle)
                                val hy = y + rad * sin(angle)
                                if (i == 0) accentPath.moveTo(hx, hy) else accentPath.lineTo(hx, hy)
                            }
                            accentPath.close()
                        } else {
                            dots.add(Offset(x, y))
                        }
                    }
                }

                onDrawBehind {
                    drawPath(accentPath, color = accentColor, style = stroke)
                    for (i in 0 until dots.size) {
                        drawCircle(dotColor, radius = dotR, center = dots[i])
                    }
                }
            }
    )
}

@Composable
fun AuroraGradientPattern(modifier: Modifier = Modifier) {
    val baseBrush = remember {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF06141B),
                Color(0xFF0F1E36),
                Color(0xFF1E1032),
                Color(0xFF0B1A24)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(baseBrush)
            .drawWithCache {
                val c1 = Offset(size.width * 0.8f, size.height * 0.25f)
                val r1 = size.width * 0.7f
                val b1 = Brush.radialGradient(
                    colors = listOf(Color(0xFF00E676).copy(alpha = 0.08f), Color.Transparent),
                    center = c1,
                    radius = r1
                )

                val c2 = Offset(size.width * 0.2f, size.height * 0.7f)
                val r2 = size.width * 0.65f
                val b2 = Brush.radialGradient(
                    colors = listOf(Color(0xFFB388FF).copy(alpha = 0.08f), Color.Transparent),
                    center = c2,
                    radius = r2
                )

                val c3 = Offset(size.width * 0.5f, size.height * 0.45f)
                val r3 = size.width * 0.5f
                val b3 = Brush.radialGradient(
                    colors = listOf(Color(0xFF18FFFF).copy(alpha = 0.06f), Color.Transparent),
                    center = c3,
                    radius = r3
                )

                onDrawBehind {
                    drawCircle(brush = b1, radius = r1, center = c1)
                    drawCircle(brush = b2, radius = r2, center = c2)
                    drawCircle(brush = b3, radius = r3, center = c3)
                }
            }
    )
}

@Composable
fun ParallaxChatWallpaper(
    preset: String,
    customUri: String,
    alpha: Float,
    parallaxEnabled: Boolean,
    liveVideoWallpaperEnabled: Boolean = false,
    liveVideoWallpaperUri: String = "",
    modifier: Modifier = Modifier
) {
    if (preset == "none" && (!liveVideoWallpaperEnabled || liveVideoWallpaperUri.isBlank())) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val context = LocalContext.current
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    DisposableEffect(parallaxEnabled) {
        if (!parallaxEnabled) {
            tiltX = 0f
            tiltY = 0f
            return@DisposableEffect onDispose {}
        }

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || sensor == null) {
            return@DisposableEffect onDispose {}
        }

        var lastUpdate = 0L
        var smoothX = 0f
        var smoothY = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val now = System.currentTimeMillis()
                if (now - lastUpdate < 45L) return
                lastUpdate = now

                val x = event.values[0]
                val y = event.values[1] - 4.5f
                val targetX = (-x * 3.2f).coerceIn(-24f, 24f)
                val targetY = (y * 3.2f).coerceIn(-24f, 24f)

                smoothX += (targetX - smoothX) * 0.25f
                smoothY += (targetY - smoothY) * 0.25f

                if (abs(smoothX - tiltX) > 0.35f) tiltX = smoothX
                if (abs(smoothY - tiltY) > 0.35f) tiltY = smoothY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val scaleFactor = if (parallaxEnabled) 1.12f else 1.0f

    val effectiveVideoUri = when {
        !liveVideoWallpaperEnabled -> ""
        liveVideoWallpaperUri.isNotBlank() -> liveVideoWallpaperUri
        customUri.isNotBlank() && (customUri.contains("video", ignoreCase = true) || customUri.endsWith(".mp4", ignoreCase = true) || customUri.endsWith(".webm", ignoreCase = true) || customUri.endsWith(".mkv", ignoreCase = true)) -> customUri
        preset == "custom" && customUri.isNotBlank() -> customUri
        else -> ""
    }

    val isVideoPlaying = liveVideoWallpaperEnabled && effectiveVideoUri.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isVideoPlaying) {
            LiveVideoWallpaperPlayer(
                videoUri = effectiveVideoUri,
                alpha = alpha,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = tiltX
                        translationY = tiltY
                        scaleX = scaleFactor
                        scaleY = scaleFactor
                    }
                    .alpha(alpha)
            ) {
                when {
                    preset == "custom" && customUri.isNotBlank() -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(customUri)
                                .crossfade(false)
                                .build(),
                            contentDescription = "Фон чата",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        ProceduralWallpaper(
                            preset = preset,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
