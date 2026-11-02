package com.example.ui.components

import android.graphics.Paint
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.datastore.UserSettings
import kotlinx.coroutines.delay
import kotlin.math.*

// Map Cells: 1 = Pine Wall, 0 = Path, 2 = Tape, 3 = Exit Gate, 4 = TV Monitor, 5 = Campfire, 6 = Pumpkin, 7 = Boombox
private val FOREST_MAP_LAYOUT_1 = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 0, 1, 0, 0, 4, 0, 1, 0, 0, 2, 1),
    intArrayOf(1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1),
    intArrayOf(1, 0, 1, 0, 0, 5, 0, 1, 0, 0, 0, 1, 0, 1),
    intArrayOf(1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 2, 1, 0, 0, 0, 1, 0, 6, 0, 1),
    intArrayOf(1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 7, 0, 1, 0, 0, 0, 1, 0, 1),
    intArrayOf(1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 0, 1),
    intArrayOf(1, 0, 1, 2, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1),
    intArrayOf(1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 5, 0, 0, 3, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
)

private val FOREST_MAP_LAYOUT_2 = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 6, 0, 0, 2, 1),
    intArrayOf(1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 1),
    intArrayOf(1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1),
    intArrayOf(1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 1, 0, 5, 0, 1, 0, 0, 4, 0, 1),
    intArrayOf(1, 1, 1, 0, 1, 0, 2, 0, 1, 0, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1),
    intArrayOf(1, 0, 0, 2, 1, 0, 1, 0, 7, 0, 0, 1, 0, 1),
    intArrayOf(1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 0, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 3, 1),
    intArrayOf(1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
)

private val FOREST_MAP_LAYOUT_3 = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 0, 1, 0, 4, 0, 0, 0, 0, 0, 2, 1),
    intArrayOf(1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1),
    intArrayOf(1, 0, 1, 0, 0, 0, 1, 2, 0, 0, 1, 0, 0, 1),
    intArrayOf(1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1),
    intArrayOf(1, 0, 0, 6, 0, 0, 0, 5, 1, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 7, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 1),
    intArrayOf(1, 0, 2, 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1),
    intArrayOf(1, 0, 0, 1, 1, 1, 1, 0, 1, 0, 1, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 3, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
)

private val ALL_FOREST_MAPS = listOf(FOREST_MAP_LAYOUT_1, FOREST_MAP_LAYOUT_2, FOREST_MAP_LAYOUT_3)

private fun getCell(map: Array<IntArray>, x: Float, y: Float): Int {
    val ix = x.toInt()
    val iy = y.toInt()
    if (iy !in 0 until map.size || ix !in 0 until map[0].size) return 1
    return map[iy][ix]
}

private fun isWall(map: Array<IntArray>, x: Float, y: Float): Boolean {
    return getCell(map, x, y) == 1
}

// Raycasting Line-of-Sight check (prevents AI from seeing through trees/walls)
private fun hasLineOfSight(map: Array<IntArray>, x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
    val dist = hypot(x2 - x1, y2 - y1)
    if (dist > 8.5f) return false
    val steps = (dist * 12).toInt().coerceAtLeast(1)
    val dx = (x2 - x1) / steps
    val dy = (y2 - y1) / steps
    var cx = x1
    var cy = y1
    for (i in 0 until steps) {
        cx += dx
        cy += dy
        if (isWall(map, cx, cy)) return false
    }
    return true
}

private data class SkibidiToilet(
    var id: Int,
    var x: Float,
    var y: Float,
    var speed: Float,
    var dirX: Float = 1f,
    var dirY: Float = 0f,
    var moveTimer: Int = 0,
    var isChasing: Boolean = false,
    var stunTimer: Int = 0
)

private data class CameramanTape(
    val x: Float,
    val y: Float,
    var isCollected: Boolean = false
)

@Composable
fun Skibidi3DForestDialog(
    settings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    // Audio Tone Generator for Skibidi Sounds
    val toneGen = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGen?.release()
            } catch (_: Exception) {}
        }
    }

    // Dynamic Map Variant selection
    var currentMap by remember { mutableStateOf(ALL_FOREST_MAPS.random()) }

    // Player 3D Position State
    var posX by remember { mutableFloatStateOf(1.5f) }
    var posY by remember { mutableFloatStateOf(1.5f) }
    var angle by remember { mutableFloatStateOf(0f) } // in radians

    var collectedTapesCount by remember { mutableIntStateOf(0) }
    var flashCharges by remember { mutableIntStateOf(3) }
    var flashEffectTimer by remember { mutableIntStateOf(0) }
    var isGameOverJumpscare by remember { mutableStateOf(false) }
    var isGameWon by remember { mutableStateOf(false) }
    var closestSkibidiDist by remember { mutableFloatStateOf(99f) }

    // Skibidi Toilets initial positions
    var toilets by remember {
        mutableStateOf(
            listOf(
                SkibidiToilet(1, 6.5f, 3.5f, 0.030f, 1f, 0f),
                SkibidiToilet(2, 9.5f, 7.5f, 0.032f, 0f, 1f),
                SkibidiToilet(3, 3.5f, 9.5f, 0.031f, -1f, 0f)
            )
        )
    }

    // Tapes position
    var tapes by remember {
        mutableStateOf(
            listOf(
                CameramanTape(12.5f, 1.5f),
                CameramanTape(4.5f, 5.5f),
                CameramanTape(3.5f, 9.5f)
            )
        )
    }

    // Initial Spawns
    fun initGameSpawns(map: Array<IntArray>) {
        val newTapes = mutableListOf<CameramanTape>()
        val newToilets = mutableListOf<SkibidiToilet>()

        for (r in 0 until map.size) {
            for (c in 0 until map[0].size) {
                if (map[r][c] == 2) {
                    newTapes.add(CameramanTape(c + 0.5f, r + 0.5f))
                }
            }
        }
        // Ensure exactly 3 tapes
        if (newTapes.size < 3) {
            newTapes.clear()
            newTapes.add(CameramanTape(12.5f, 1.5f))
            newTapes.add(CameramanTape(4.5f, 5.5f))
            newTapes.add(CameramanTape(3.5f, 9.5f))
        }

        newToilets.add(SkibidiToilet(1, 6.5f, 3.5f, 0.030f, 1f, 0f))
        newToilets.add(SkibidiToilet(2, 9.5f, 7.5f, 0.032f, 0f, 1f))
        newToilets.add(SkibidiToilet(3, 3.5f, 9.5f, 0.031f, -1f, 0f))

        toilets = newToilets
        tapes = newTapes
    }

    // Movement Controls
    var moveForward by remember { mutableStateOf(false) }
    var moveBackward by remember { mutableStateOf(false) }
    var turnLeft by remember { mutableStateOf(false) }
    var turnRight by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }

    fun restartGame() {
        val selected = ALL_FOREST_MAPS.random()
        currentMap = selected
        posX = 1.5f
        posY = 1.5f
        angle = 0f
        collectedTapesCount = 0
        flashCharges = 3
        flashEffectTimer = 0
        isGameOverJumpscare = false
        isGameWon = false
        initGameSpawns(selected)
    }

    fun triggerCameraFlash() {
        if (flashCharges <= 0 || isGameOverJumpscare || isGameWon) return
        flashCharges--
        flashEffectTimer = 16 // ~250ms bright flash
        HapticHelper.performSuccessHaptic(context, true)
        try {
            toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        } catch (_: Exception) {}

        // Stun all toilets within 7 units for ~3.5 seconds
        toilets = toilets.map { toilet ->
            val d = hypot(posX - toilet.x, posY - toilet.y)
            if (d < 7.0f) {
                toilet.copy(stunTimer = 220, isChasing = false)
            } else {
                toilet
            }
        }
    }

    // Main Game Engine Loop
    LaunchedEffect(isGameOverJumpscare, isGameWon) {
        if (isGameOverJumpscare || isGameWon) return@LaunchedEffect

        var audioCounter = 0
        while (true) {
            delay(16) // ~60 FPS
            audioCounter++

            if (flashEffectTimer > 0) flashEffectTimer--

            val moveSpeed = if (isRunning) 0.082f else 0.048f
            val turnSpeed = 0.065f

            // Player turning
            if (turnLeft) angle -= turnSpeed
            if (turnRight) angle += turnSpeed

            // Player moving
            var dx = 0f
            var dy = 0f
            if (moveForward) {
                dx += cos(angle) * moveSpeed
                dy += sin(angle) * moveSpeed
            }
            if (moveBackward) {
                dx -= cos(angle) * moveSpeed
                dy -= sin(angle) * moveSpeed
            }

            // Wall Collision detection
            val nextX = posX + dx
            val nextY = posY + dy

            if (!isWall(currentMap, nextX, posY)) {
                posX = nextX
            }
            if (!isWall(currentMap, posX, nextY)) {
                posY = nextY
            }

            // Tape Collection Check
            tapes.forEach { tape ->
                if (!tape.isCollected) {
                    val tDist = hypot(posX - tape.x, posY - tape.y)
                    if (tDist < 0.68f) {
                        tape.isCollected = true
                        collectedTapesCount++
                        HapticHelper.performSuccessHaptic(context, true)
                        try {
                            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                        } catch (_: Exception) {}
                    }
                }
            }

            // Exit Gate Check
            if (getCell(currentMap, posX, posY) == 3) {
                if (collectedTapesCount >= 3) {
                    isGameWon = true
                    HapticHelper.performSuccessHaptic(context, true)
                    try {
                        toneGen?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 350)
                    } catch (_: Exception) {}
                }
            }

            // Skibidi Toilet AI Movement & Pursuit with Line of Sight
            var minDist = 99f
            val updatedToilets = toilets.map { toilet ->
                val distToPlayer = hypot(posX - toilet.x, posY - toilet.y)
                if (distToPlayer < minDist) minDist = distToPlayer

                toilet.moveTimer++

                // Handle Stun state
                if (toilet.stunTimer > 0) {
                    toilet.stunTimer--
                    return@map toilet
                }

                // Check Line of Sight (cannot see through walls/trees)
                val canSeePlayer = hasLineOfSight(currentMap, toilet.x, toilet.y, posX, posY)

                if (canSeePlayer) {
                    // PURSUIT MODE: Chasing player directly when in LOS
                    toilet.isChasing = true
                    val chaseSpeed = toilet.speed * 1.35f
                    val chaseDx = (posX - toilet.x) / (distToPlayer + 0.001f)
                    val chaseDy = (posY - toilet.y) / (distToPlayer + 0.001f)

                    val nextTx = toilet.x + chaseDx * chaseSpeed
                    val nextTy = toilet.y + chaseDy * chaseSpeed

                    if (!isWall(currentMap, nextTx, toilet.y)) {
                        toilet.x = nextTx
                    } else if (!isWall(currentMap, toilet.x + sign(chaseDx) * chaseSpeed, toilet.y)) {
                        toilet.x += sign(chaseDx) * chaseSpeed
                    }

                    if (!isWall(currentMap, toilet.x, nextTy)) {
                        toilet.y = nextTy
                    } else if (!isWall(currentMap, toilet.x, toilet.y + sign(chaseDy) * chaseSpeed)) {
                        toilet.y += sign(chaseDy) * chaseSpeed
                    }
                } else {
                    // PATROL MODE: Active roaming through forest paths
                    toilet.isChasing = false
                    val patrolSpeed = toilet.speed * 0.95f

                    val willHitWall = isWall(currentMap, toilet.x + toilet.dirX * patrolSpeed * 2.2f, toilet.y + toilet.dirY * patrolSpeed * 2.2f)
                    if (toilet.moveTimer % 45 == 0 || willHitWall) {
                        val dirs = listOf(1f to 0f, -1f to 0f, 0f to 1f, 0f to -1f, 0.7f to 0.7f, -0.7f to 0.7f, 0.7f to -0.7f, -0.7f to -0.7f).shuffled()
                        val validDir = dirs.firstOrNull { (vx, vy) -> !isWall(currentMap, toilet.x + vx * 0.5f, toilet.y + vy * 0.5f) } ?: (1f to 0f)
                        toilet.dirX = validDir.first
                        toilet.dirY = validDir.second
                    }

                    val nextTx = toilet.x + toilet.dirX * patrolSpeed
                    val nextTy = toilet.y + toilet.dirY * patrolSpeed

                    if (!isWall(currentMap, nextTx, toilet.y)) {
                        toilet.x = nextTx
                    }
                    if (!isWall(currentMap, toilet.x, nextTy)) {
                        toilet.y = nextTy
                    }
                }

                // Check Jumpscare Collision (only when not stunned!)
                if (toilet.stunTimer <= 0 && distToPlayer < 0.48f) {
                    isGameOverJumpscare = true
                    HapticHelper.performSuccessHaptic(context, true)
                    try {
                        toneGen?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 450)
                    } catch (_: Exception) {}
                }
                toilet
            }
            toilets = updatedToilets
            closestSkibidiDist = minDist

            // Audio tone pulses when Skibidi is close
            if (minDist < 2.8f && audioCounter % 18 == 0) {
                try {
                    toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                } catch (_: Exception) {}
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 16.dp,
            border = BorderStroke(2.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF7F1D1D),
                                    Color(0xFF1E1B4B)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚽", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Escape from Skibidi in 3D Forest" else "Побег от Скибиди в 3D Лесу",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "📹 $collectedTapesCount/3",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFACC15),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть", tint = Color.White)
                            }
                        }
                    }
                }

                // 3D RAYCASTING VIEWPORT CANVAS WITH TOUCH ROTATION
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { _, dragAmount ->
                                    angle += dragAmount.x * 0.007f
                                }
                            }
                    ) {
                        val screenW = size.width
                        val screenH = size.height
                        val numRays = 84
                        val fov = 1.05f // Radians (~60 deg)
                        val rayWidth = screenW / numRays
                        val zBuffer = FloatArray(numRays)

                        // Draw Night Sky Background
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF020617), Color(0xFF0F172A))
                            ),
                            size = Size(screenW, screenH / 2f)
                        )

                        // Draw Forest Ground
                        drawRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF052E16), Color(0xFF14532D))
                            ),
                            topLeft = Offset(0f, screenH / 2f),
                            size = Size(screenW, screenH / 2f)
                        )

                        // Raycasting Wall Slices Loop
                        for (i in 0 until numRays) {
                            val rayAngle = (angle - fov / 2f) + (i.toFloat() / numRays) * fov
                            val cosA = cos(rayAngle)
                            val sinA = sin(rayAngle)

                            var distance = 0f
                            var hitWall = false
                            var isExitTile = false

                            while (!hitWall && distance < 12f) {
                                distance += 0.06f
                                val checkX = posX + cosA * distance
                                val checkY = posY + sinA * distance

                                val cell = getCell(currentMap, checkX, checkY)
                                if (cell == 1) {
                                    hitWall = true
                                } else if (cell == 3) {
                                    hitWall = true
                                    isExitTile = true
                                }
                            }

                            // Correct fisheye distortion
                            val correctedDist = distance * cos(rayAngle - angle)
                            zBuffer[i] = correctedDist

                            val wallHeight = (screenH * 1.1f / (correctedDist + 0.001f)).coerceAtMost(screenH * 1.5f)
                            val wallTop = (screenH - wallHeight) / 2f

                            // Dark Forest Fog shading
                            val fogFactor = (1f - (correctedDist / 11f)).coerceIn(0.05f, 1.0f)
                            val wallColor = if (isExitTile) {
                                Color(0xFFFACC15).copy(alpha = fogFactor)
                            } else {
                                Color(
                                    red = 0.08f * fogFactor,
                                    green = 0.35f * fogFactor,
                                    blue = 0.18f * fogFactor,
                                    alpha = 1.0f
                                )
                            }

                            drawRect(
                                color = wallColor,
                                topLeft = Offset(i * rayWidth, wallTop),
                                size = Size(rayWidth + 1f, wallHeight)
                            )
                        }

                        // RENDER 3D BILLBOARD SPRITES (SKIBIDI TOILETS, CAMERAMAN TAPES & MAP DECOR)
                        val spritesList = mutableListOf<Triple<Float, Float, String>>()

                        // Collect tapes
                        tapes.forEach { tape ->
                            if (!tape.isCollected) spritesList.add(Triple(tape.x, tape.y, "📹"))
                        }

                        // Collect map decor items (TV, Campfire, Pumpkin, Boombox)
                        for (r in 0 until currentMap.size) {
                            for (c in 0 until currentMap[0].size) {
                                when (currentMap[r][c]) {
                                    4 -> spritesList.add(Triple(c + 0.5f, r + 0.5f, "📺"))
                                    5 -> spritesList.add(Triple(c + 0.5f, r + 0.5f, "🔥"))
                                    6 -> spritesList.add(Triple(c + 0.5f, r + 0.5f, "🎃"))
                                    7 -> spritesList.add(Triple(c + 0.5f, r + 0.5f, "📻"))
                                }
                            }
                        }

                        // Collect toilets
                        toilets.forEach { toilet ->
                            val emoji = if (toilet.stunTimer > 0) "💫🚽" else "🚽"
                            spritesList.add(Triple(toilet.x, toilet.y, emoji))
                        }

                        // Sort sprites by distance (furthest first)
                        spritesList.sortByDescending { hypot(it.first - posX, it.second - posY) }

                        val paint = Paint().apply {
                            textAlign = Paint.Align.CENTER
                        }

                        spritesList.forEach { (spX, spY, emoji) ->
                            val spriteDist = hypot(spX - posX, spY - posY)
                            if (spriteDist > 0.35f && spriteDist < 11f) {
                                var spAngle = atan2(spY - posY, spX - posX) - angle
                                while (spAngle > PI) spAngle -= (2 * PI).toFloat()
                                while (spAngle < -PI) spAngle += (2 * PI).toFloat()

                                if (abs(spAngle) < fov) {
                                    val screenX = (screenW / 2f) + (tan(spAngle) * (screenW / 2f))
                                    val spriteSize = (screenH * 0.82f / spriteDist).coerceIn(28f, screenH * 1.1f)
                                    val rayIdx = ((screenX / screenW) * numRays).toInt().coerceIn(0, numRays - 1)

                                    if (spriteDist <= zBuffer[rayIdx] + 0.35f) {
                                        paint.textSize = spriteSize
                                        drawContext.canvas.nativeCanvas.drawText(
                                            emoji,
                                            screenX,
                                            (screenH / 2f) + (spriteSize * 0.3f),
                                            paint
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bright Flashlight Stun Effect Flash
                    if (flashEffectTimer > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = (flashEffectTimer / 16f).coerceIn(0f, 0.92f)))
                        )
                    }

                    // Red Warning Pulse when Skibidi is extremely close
                    if (closestSkibidiDist < 2.0f && flashEffectTimer <= 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red.copy(alpha = (2.0f - closestSkibidiDist) * 0.22f))
                        )
                    }

                    // Exit Open Overlay Banner
                    if (collectedTapesCount >= 3 && !isGameWon) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEAB308),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 10.dp)
                        ) {
                            Text(
                                text = if (isEn) "🚪 EXIT OPENED! ESCAPE TO THE EXIT GATE!" else "🚪 ВЫХОД ОТКРЫТ! БЕГИ К ЖЁЛТЫМ ВРАТАМ В УГЛУ!",
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Minimap Overlay (Top Right)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(92.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val mapHeight = currentMap.size
                            val mapWidth = currentMap[0].size
                            val cellW = size.width / mapWidth
                            val cellH = size.height / mapHeight

                            for (r in 0 until mapHeight) {
                                for (c in 0 until mapWidth) {
                                    val tile = currentMap[r][c]
                                    if (tile == 1) {
                                        drawRect(
                                            color = Color(0xFF15803D),
                                            topLeft = Offset(c * cellW, r * cellH),
                                            size = Size(cellW, cellH)
                                        )
                                    } else if (tile == 3) {
                                        drawRect(
                                            color = Color(0xFFFACC15),
                                            topLeft = Offset(c * cellW, r * cellH),
                                            size = Size(cellW, cellH)
                                        )
                                    }
                                }
                            }

                            // Tapes on minimap
                            tapes.forEach { tape ->
                                if (!tape.isCollected) {
                                    drawCircle(
                                        color = Color(0xFFEAB308),
                                        radius = cellW * 0.4f,
                                        center = Offset(tape.x * cellW, tape.y * cellH)
                                    )
                                }
                            }

                            // Toilets on minimap
                            toilets.forEach { t ->
                                drawCircle(
                                    color = if (t.stunTimer > 0) Color(0xFF38BDF8) else Color(0xFFEF4444),
                                    radius = cellW * 0.45f,
                                    center = Offset(t.x * cellW, t.y * cellH)
                                )
                            }

                            // Player on minimap
                            drawCircle(
                                color = Color(0xFF38BDF8),
                                radius = cellW * 0.5f,
                                center = Offset(posX * cellW, posY * cellH)
                            )
                        }
                    }

                    // Jumpscare Game Over Overlay
                    if (isGameOverJumpscare) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF7F1D1D).copy(alpha = 0.95f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("🚽💥", fontSize = 72.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isEn) "SKIBIDI TOILET FLUSHED YOU!" else "СКИБИДИ ТУАЛЕТ ТЕБЯ СМЫЛ!",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "«Skibidi dop dop dop yes yes!»",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { restartGame() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEn) "Try Again" else "Попробовать снова", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Game Win Overlay
                    if (isGameWon) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF065F46).copy(alpha = 0.95f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("📹🎉", fontSize = 72.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isEn) "YOU ESCAPED SKIBIDI FOREST!" else "ВЫ СБЕЖАЛИ ИЗ СКИБИДИ-ЛЕСА!",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isEn) "All 3 Agent tapes saved from flushing!" else "Все 3 кассеты Агентов спасены от смыва!",
                                    fontSize = 13.sp,
                                    color = Color(0xFFA7F3D0)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { restartGame() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEn) "Play Again" else "Сыграть ещё раз", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // TOUCH CONTROLS PANEL
                Surface(
                    color = Color(0xFF020617),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Movement D-Pad (Forward & Backward)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TouchControlButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                label = if (isEn) "Forward" else "Вперёд",
                                isPressed = moveForward,
                                onStateChange = { moveForward = it }
                            )
                            TouchControlButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                label = if (isEn) "Backward" else "Назад",
                                isPressed = moveBackward,
                                onStateChange = { moveBackward = it }
                            )
                        }

                        // Flashlight Camera Stun Button
                        IconButton(
                            onClick = { triggerCameraFlash() },
                            enabled = flashCharges > 0,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (flashCharges > 0) Color(0xFFEAB308) else Color(0xFF334155))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📸", fontSize = 16.sp)
                                Text("$flashCharges", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                            }
                        }

                        // Run / Sprint Toggle
                        IconButton(
                            onClick = { isRunning = !isRunning },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) Color(0xFFEF4444) else Color(0xFF334155))
                        ) {
                            Text("🏃", fontSize = 20.sp)
                        }

                        // Rotation Controls (Turn Left & Turn Right)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TouchControlButton(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                label = if (isEn) "Turn Left" else "Влево",
                                isPressed = turnLeft,
                                onStateChange = { turnLeft = it }
                            )
                            TouchControlButton(
                                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                label = if (isEn) "Turn Right" else "Вправо",
                                isPressed = turnRight,
                                onStateChange = { turnRight = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TouchControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isPressed: Boolean,
    onStateChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isPressed) Color(0xFF38BDF8) else Color(0xFF1E293B),
        border = BorderStroke(1.dp, Color(0xFF475569)),
        modifier = Modifier
            .size(50.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onStateChange(true)
                        tryAwaitRelease()
                        onStateChange(false)
                    }
                )
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPressed) Color.Black else Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
