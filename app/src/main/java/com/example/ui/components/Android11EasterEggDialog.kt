package com.example.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

/**
 * Физическое тело шарика-робота.
 */
class RobotPhysicsBall(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var angle: Float = 0f,
    var vAngle: Float = 0f,
    var isGrabbed: Boolean = false,
    val primaryColor: Color,
    val containerColor: Color,
    val borderColor: Color
)

@Composable
fun Android11EasterEggDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    // Цвета из текущей темы приложения
    val themeBackground = MaterialTheme.colorScheme.background
    val themeSurface = MaterialTheme.colorScheme.surface
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeOnPrimary = MaterialTheme.colorScheme.onPrimary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val themePrimaryContainer = MaterialTheme.colorScheme.primaryContainer
    val themeOnPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val themeOutline = MaterialTheme.colorScheme.outline

    // Состояние циферблата
    // 11 кружков расположены от 140° до 400° (шаг 26°)
    val totalSteps = 11
    val startAngleDeg = 140f
    val endAngleDeg = 400f
    val stepAngleDeg = (endAngleDeg - startAngleDeg) / (totalSteps - 1) // 26f

    var currentStep by remember { mutableIntStateOf(0) }
    var dialAngle by remember { mutableFloatStateOf(startAngleDeg) }
    var isUnlocked by remember { mutableStateOf(false) }

    // Акселерометр для наклона гравитации (если доступен на девайсе)
    var gravityX by remember { mutableFloatStateOf(0f) }
    var gravityY by remember { mutableFloatStateOf(1600f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.values.size >= 2) {
                    val ax = event.values[0] // наклон вправо/влево
                    val ay = event.values[1] // наклон вверх/вниз
                    // Приводим к физическим координатам Compose (y направлен вниз, x вправо)
                    gravityX = -ax * 160f
                    gravityY = (ay * 160f).coerceAtLeast(300f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    // Роботы
    val robotBalls = remember { mutableStateListOf<RobotPhysicsBall>() }
    var screenWidthPx by remember { mutableFloatStateOf(0f) }
    var screenHeightPx by remember { mutableFloatStateOf(0f) }
    var physicsFrameTick by remember { mutableLongStateOf(0L) }

    // Инициализация роботов при разблокировке
    fun spawnRobots(centerX: Float, centerY: Float) {
        robotBalls.clear()
        val ballRadius = with(density) { 30.dp.toPx() }
        for (i in 0 until 11) {
            val launchAngleRad = (i.toFloat() / 11f) * 2f * PI.toFloat() - PI.toFloat() / 2f
            val launchSpeed = Random.nextFloat() * 500f + 700f
            robotBalls.add(
                RobotPhysicsBall(
                    id = i,
                    x = centerX + cos(launchAngleRad) * 45f,
                    y = centerY + sin(launchAngleRad) * 45f,
                    vx = cos(launchAngleRad) * launchSpeed + (Random.nextFloat() - 0.5f) * 200f,
                    vy = sin(launchAngleRad) * launchSpeed - 400f, // Выброс вверх
                    radius = ballRadius,
                    angle = Random.nextFloat() * 360f,
                    vAngle = (Random.nextFloat() - 0.5f) * 720f,
                    primaryColor = themePrimary,
                    containerColor = themePrimaryContainer,
                    borderColor = themeOutline
                )
            )
        }
    }

    // Игровой физический цикл
    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) return@LaunchedEffect

        var lastFrameTimeNanos = 0L
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos == 0L) {
                    lastFrameTimeNanos = frameTimeNanos
                    return@withFrameNanos
                }

                val dt = ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.033f)
                lastFrameTimeNanos = frameTimeNanos
                physicsFrameTick = frameTimeNanos

                val w = screenWidthPx
                val h = screenHeightPx
                if (w <= 0f || h <= 0f) return@withFrameNanos

                val dialCenterX = w / 2f
                val dialCenterY = h / 2f
                val dialRadius = with(density) { 110.dp.toPx() }

                val gx = gravityX
                val gy = gravityY

                // Обновление положения каждого робота
                for (ball in robotBalls) {
                    if (ball.isGrabbed) continue

                    // Гравитация
                    ball.vx += gx * dt
                    ball.vy += gy * dt

                    // Движение
                    ball.x += ball.vx * dt
                    ball.y += ball.vy * dt
                    ball.angle += ball.vAngle * dt

                    // Сопротивление воздуха
                    ball.vx *= 0.998f
                    ball.vy *= 0.998f
                    ball.vAngle *= 0.995f

                    // Отскок от левой стенки
                    if (ball.x - ball.radius < 0f) {
                        ball.x = ball.radius
                        ball.vx = -ball.vx * 0.82f
                        ball.vAngle = -ball.vy * 0.15f
                    }
                    // Отскок от правой стенки
                    if (ball.x + ball.radius > w) {
                        ball.x = w - ball.radius
                        ball.vx = -ball.vx * 0.82f
                        ball.vAngle = ball.vy * 0.15f
                    }
                    // Отскок от потолка
                    if (ball.y - ball.radius < 0f) {
                        ball.y = ball.radius
                        ball.vy = -ball.vy * 0.80f
                    }
                    // Отскок от пола
                    if (ball.y + ball.radius > h) {
                        ball.y = h - ball.radius
                        ball.vy = -ball.vy * 0.78f
                        ball.vx *= 0.96f // Трение качения по полу
                        ball.vAngle = ball.vx * 0.35f
                        if (abs(ball.vy) < 35f) {
                            ball.vy = 0f
                        }
                    }

                    // Отскок от центрального диска Android 11
                    val dxDial = ball.x - dialCenterX
                    val dyDial = ball.y - dialCenterY
                    val distDial = hypot(dxDial, dyDial)
                    val minDialDist = ball.radius + dialRadius
                    if (distDial < minDialDist && distDial > 0.001f) {
                        val nx = dxDial / distDial
                        val ny = dyDial / distDial
                        val overlap = minDialDist - distDial
                        ball.x += nx * overlap
                        ball.y += ny * overlap

                        val dot = ball.vx * nx + ball.vy * ny
                        if (dot < 0f) {
                            val bounce = 0.85f
                            ball.vx -= (1f + bounce) * dot * nx
                            ball.vy -= (1f + bounce) * dot * ny
                            ball.vAngle += (Random.nextFloat() - 0.5f) * 180f
                        }
                    }
                }

                // Взаимные упругие соударения мячиков между собой
                val count = robotBalls.size
                for (i in 0 until count) {
                    val a = robotBalls[i]
                    for (j in i + 1 until count) {
                        val b = robotBalls[j]
                        val dx = b.x - a.x
                        val dy = b.y - a.y
                        val distSq = dx * dx + dy * dy
                        val minDist = a.radius + b.radius
                        if (distSq < minDist * minDist && distSq > 0.0001f) {
                            val dist = sqrt(distSq)
                            val nx = dx / dist
                            val ny = dy / dist
                            val overlap = (minDist - dist) * 0.5f

                            if (!a.isGrabbed) {
                                a.x -= nx * overlap
                                a.y -= ny * overlap
                            }
                            if (!b.isGrabbed) {
                                b.x += nx * overlap
                                b.y += ny * overlap
                            }

                            val kx = a.vx - b.vx
                            val ky = a.vy - b.vy
                            val vn = kx * nx + ky * ny
                            if (vn > 0f) {
                                val impulse = vn * 1.8f * 0.5f
                                if (!a.isGrabbed) {
                                    a.vx -= impulse * nx
                                    a.vy -= impulse * ny
                                    a.vAngle += (Random.nextFloat() - 0.5f) * 100f
                                }
                                if (!b.isGrabbed) {
                                    b.vx += impulse * nx
                                    b.vy += impulse * ny
                                    b.vAngle += (Random.nextFloat() - 0.5f) * 100f
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var grabbedBall by remember { mutableStateOf<RobotPhysicsBall?>(null) }
    var lastTouchPos by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            themeSurface,
                            themeBackground
                        )
                    )
                )
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            screenWidthPx = widthPx
            screenHeightPx = heightPx

            val centerX = widthPx / 2f
            val centerY = heightPx / 2f
            val dialRadius = with(density) { 110.dp.toPx() }
            val dotsRadius = dialRadius + with(density) { 44.dp.toPx() }

            // Обработка жестов по циферблату и мячикам
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isUnlocked) {
                        if (!isUnlocked) {
                            // Режим поворота регулятора Android 11
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val dx = offset.x - centerX
                                    val dy = offset.y - centerY
                                    var touchAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                    if (touchAngle < 90f) {
                                        touchAngle += 360f
                                    }
                                    if (touchAngle in (startAngleDeg - 30f)..(endAngleDeg + 30f)) {
                                        val clamped = touchAngle.coerceIn(startAngleDeg, endAngleDeg)
                                        dialAngle = clamped
                                        val step = (((clamped - startAngleDeg) / stepAngleDeg) + 0.35f).toInt().coerceIn(0, totalSteps - 1)
                                        if (step != currentStep) {
                                            currentStep = step
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (step == totalSteps - 1) {
                                                isUnlocked = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                spawnRobots(centerX, centerY)
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val dx = change.position.x - centerX
                                    val dy = change.position.y - centerY
                                    var touchAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                    if (touchAngle < 90f) {
                                        touchAngle += 360f
                                    }
                                    if (touchAngle in (startAngleDeg - 40f)..(endAngleDeg + 40f)) {
                                        val clamped = touchAngle.coerceIn(startAngleDeg, endAngleDeg)
                                        dialAngle = clamped
                                        val step = (((clamped - startAngleDeg) / stepAngleDeg) + 0.35f).toInt().coerceIn(0, totalSteps - 1)
                                        if (step != currentStep) {
                                            currentStep = step
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (step == totalSteps - 1) {
                                                isUnlocked = true
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                spawnRobots(centerX, centerY)
                                            }
                                        }
                                    }
                                }
                            )
                        } else {
                            // Режим взаимодействия с мячиками
                            detectDragGestures(
                                onDragStart = { pos ->
                                    lastTouchPos = pos
                                    // Проверяем, попал ли палец в мячик
                                    val touched = robotBalls.lastOrNull { ball ->
                                        hypot(ball.x - pos.x, ball.y - pos.y) <= ball.radius * 1.3f
                                    }
                                    if (touched != null) {
                                        touched.isGrabbed = true
                                        grabbedBall = touched
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } else {
                                        // Ударная волна от касания фона
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        for (ball in robotBalls) {
                                            val dx = ball.x - pos.x
                                            val dy = ball.y - pos.y
                                            val dist = hypot(dx, dy).coerceAtLeast(10f)
                                            if (dist < 480f) {
                                                val force = (480f - dist) * 3.8f
                                                ball.vx += (dx / dist) * force
                                                ball.vy += (dy / dist) * force - 200f
                                                ball.vAngle += (Random.nextFloat() - 0.5f) * 360f
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val cur = change.position
                                    val ball = grabbedBall
                                    if (ball != null) {
                                        ball.x = cur.x
                                        ball.y = cur.y
                                        ball.vx = dragAmount.x * 55f
                                        ball.vy = dragAmount.y * 55f
                                    }
                                    lastTouchPos = cur
                                },
                                onDragEnd = {
                                    grabbedBall?.isGrabbed = false
                                    grabbedBall = null
                                },
                                onDragCancel = {
                                    grabbedBall?.isGrabbed = false
                                    grabbedBall = null
                                }
                            )
                        }
                    }
            ) {
                // Холст отрисовки пасхалки Android 11 и физики
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val _redrawTick = physicsFrameTick // Подписка на обновление физического кадра
                    val shadowAngleRad = Math.toRadians(45.0).toFloat()
                    val cos45 = cos(shadowAngleRad)
                    val sin45 = sin(shadowAngleRad)

                    // 1. Длинная тень от центрального диска (Long Shadow 45° как на скриншоте Android 11)
                    val p1 = Offset(centerX - dialRadius * sin45, centerY + dialRadius * cos45)
                    val p2 = Offset(centerX + dialRadius * cos45, centerY - dialRadius * sin45)
                    val shadowLen = max(widthPx, heightPx) * 1.2f
                    val p3 = Offset(p2.x + shadowLen * cos45, p2.y + shadowLen * sin45)
                    val p4 = Offset(p1.x + shadowLen * cos45, p1.y + shadowLen * sin45)

                    val shadowPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                        close()
                    }
                    drawPath(
                        path = shadowPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.28f),
                                Color.Black.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            start = Offset(centerX, centerY),
                            end = Offset(centerX + shadowLen * 0.7f * cos45, centerY + shadowLen * 0.7f * sin45)
                        )
                    )

                    // 2. 11 кружков вокруг диска
                    for (i in 0 until totalSteps) {
                        val angleDeg = startAngleDeg + i * stepAngleDeg
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val dotX = centerX + dotsRadius * cos(rad).toFloat()
                        val dotY = centerY + dotsRadius * sin(rad).toFloat()
                        val isPassed = i <= currentStep

                        if (isPassed) {
                            // Активный кружок (подсвечен)
                            drawCircle(
                                color = themePrimary.copy(alpha = 0.35f),
                                radius = 10.dp.toPx(),
                                center = Offset(dotX, dotY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = Offset(dotX, dotY)
                            )
                        } else {
                            // Неактивный кружок
                            drawCircle(
                                color = themePrimary.copy(alpha = 0.35f),
                                radius = 4.5.dp.toPx(),
                                center = Offset(dotX, dotY)
                            )
                        }
                    }

                    // 3. Центральный круг регулятора Android 11
                    drawCircle(
                        color = themePrimary,
                        radius = dialRadius,
                        center = Offset(centerX, centerY)
                    )

                    // Внутренний блик/градиент на диске для объема
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(centerX - dialRadius * 0.3f, centerY - dialRadius * 0.3f),
                            radius = dialRadius * 0.9f
                        ),
                        radius = dialRadius,
                        center = Offset(centerX, centerY)
                    )

                    // 4. Белая точка-индикатор на диске (ручка регулятора)
                    val knobRad = Math.toRadians(dialAngle.toDouble())
                    val knobDist = dialRadius * 0.65f
                    val knobX = centerX + knobDist * cos(knobRad).toFloat()
                    val knobY = centerY + knobDist * sin(knobRad).toFloat()

                    // Мягкая тень под точкой
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.20f),
                        radius = 16.dp.toPx(),
                        center = Offset(knobX + 2.dp.toPx(), knobY + 3.dp.toPx())
                    )
                    // Сама точка
                    drawCircle(
                        color = themeOnPrimary,
                        radius = 14.dp.toPx(),
                        center = Offset(knobX, knobY)
                    )

                    // 5. Отрисовка числа 11 при разблокировке
                    if (isUnlocked) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f),
                            radius = dialRadius * 0.85f,
                            center = Offset(centerX, centerY)
                        )
                    }

                    // 6. Отрисовка 11 роботов-мячиков с физикой
                    if (isUnlocked) {
                        val emojiPaint = android.graphics.Paint().apply {
                            textSize = with(density) { 34.sp.toPx() }
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        for (ball in robotBalls) {
                            // Тень шарика на полу/фоне
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.25f),
                                radius = ball.radius,
                                center = Offset(ball.x + 4f, ball.y + 6f)
                            )

                            // Тело мячика с градиентом в тонах темы
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        ball.containerColor,
                                        ball.primaryColor
                                    ),
                                    center = Offset(ball.x - ball.radius * 0.3f, ball.y - ball.radius * 0.3f),
                                    radius = ball.radius
                                ),
                                radius = ball.radius,
                                center = Offset(ball.x, ball.y)
                            )

                            // Ободок мячика
                            drawCircle(
                                color = Color.White.copy(alpha = 0.7f),
                                radius = ball.radius,
                                center = Offset(ball.x, ball.y),
                                style = Stroke(width = with(density) { 2.dp.toPx() })
                            )

                            // Блик на сфере
                            drawCircle(
                                color = Color.White.copy(alpha = 0.32f),
                                radius = ball.radius * 0.35f,
                                center = Offset(ball.x - ball.radius * 0.32f, ball.y - ball.radius * 0.32f)
                            )

                            // Смайлик робота 🤖 внутри мячика с учетом угла вращения
                            rotate(degrees = ball.angle, pivot = Offset(ball.x, ball.y)) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    "🤖",
                                    ball.x,
                                    ball.y + with(density) { 12.dp.toPx() },
                                    emojiPaint
                                )
                            }
                        }
                    }
                }
            }

            // Центральный текст "11" при разблокировке
            if (isUnlocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "11",
                        fontSize = 76.sp,
                        fontWeight = FontWeight.Black,
                        color = themeOnPrimary.copy(alpha = 0.95f),
                        letterSpacing = (-2).sp
                    )
                }
            }

            // Верхняя панель управления: кнопка сброса и крестик закрытия
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка перезапуска / сброса
                if (isUnlocked) {
                    IconButton(
                        onClick = {
                            isUnlocked = false
                            currentStep = 0
                            dialAngle = startAngleDeg
                            robotBalls.clear()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Сброс",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Кнопка закрытия
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
