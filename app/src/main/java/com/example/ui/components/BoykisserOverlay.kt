package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

enum class PetPhysState {
    FLOOR,
    CLIMB_LEFT_WALL,
    CLIMB_RIGHT_WALL,
    CEILING,
    WALL_JUMP_CROSS,
    FALLING
}

@Composable
fun BoykisserOverlay(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    variant: String = "classic", // "classic", "pochita", "akira", "mephone4", "claude"
    speedFactor: Float = 1.0f,
    isInChat: Boolean = false,
    customTopYPx: Float? = null,
    behavior: String = "walk", // "walk" vs "sit"
    petIndex: Int = 0
) {
    if (!enabled) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val isWalkMode = behavior.equals("walk", ignoreCase = true)

    val petImageUrl = remember(variant) {
        if (variant.startsWith("http://") || variant.startsWith("https://")) {
            variant
        } else {
            val normalized = if (variant.equals("classic", ignoreCase = true)) "boykisser" else variant
            val found = com.example.util.PetManager.DEFAULT_PETS.find { it.id.equals(normalized, ignoreCase = true) }
            found?.imageUrl ?: "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/pets/boykisser_classic.png"
        }
    }

    // Starting offset for multi-pet separation
    val startXFrac = remember(petIndex) { (0.12f + (petIndex * 0.28f)).coerceIn(0.08f, 0.82f) }

    var posXPercent by remember { mutableFloatStateOf(startXFrac) }
    var posYPercent by remember { mutableFloatStateOf(0f) } // 0f = floor, 1f = ceiling
    var direction by remember { mutableFloatStateOf(if (petIndex % 2 == 0) 1f else -1f) } // 1f = right, -1f = left

    var physState by remember { mutableStateOf(PetPhysState.FLOOR) }
    var rotationDeg by remember { mutableFloatStateOf(0f) }

    // Ballistic Jump Physics (Smooth arc with gravity, no freezing at apex)
    var jumpOffsetDp by remember { mutableFloatStateOf(0f) }
    var jumpVelocityDp by remember { mutableFloatStateOf(0f) }
    var isJumping by remember { mutableStateOf(false) }

    val squashScaleX = remember { Animatable(1f) }
    val squashScaleY = remember { Animatable(1f) }

    val coroutineScope = rememberCoroutineScope()

    var isIdlePausing by remember { mutableStateOf(false) }
    var socialEmoji by remember { mutableStateOf<String?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var fallVelocity by remember { mutableFloatStateOf(0f) }

    // Advanced Physics & AI Loop
    LaunchedEffect(isWalkMode, speedFactor, petIndex) {
        if (!isWalkMode) {
            physState = PetPhysState.FLOOR
            posXPercent = startXFrac
            posYPercent = 0f
            rotationDeg = 0f
            jumpOffsetDp = 0f
            jumpVelocityDp = 0f
            isJumping = false
            com.example.util.PetSocialMediator.updatePetState(petIndex, variant, startXFrac, 0f)
            return@LaunchedEffect
        }

        var ticks = 0
        val baseStep = 0.0028f * speedFactor.coerceIn(0.5f, 2.5f)

        while (true) {
            delay(16) // ~60 FPS
            ticks++

            // Integrated jump physics (gravity arc)
            if (isJumping) {
                jumpOffsetDp += jumpVelocityDp
                jumpVelocityDp += 0.82f // Gravity downward acceleration
                // Carry forward momentum in air
                posXPercent = (posXPercent + direction * baseStep * 1.4f).coerceIn(0.04f, 0.84f)

                if (jumpOffsetDp >= 0f) {
                    // Smooth landing
                    jumpOffsetDp = 0f
                    jumpVelocityDp = 0f
                    isJumping = false
                    // Landing squash & stretch effect
                    coroutineScope.launch {
                        squashScaleY.snapTo(0.78f)
                        squashScaleX.snapTo(1.22f)
                        squashScaleY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh))
                    }
                    coroutineScope.launch {
                        squashScaleX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh))
                    }
                }
            }

            if (isIdlePausing) {
                delay(100)
                continue
            }

            // Occasional idle rest pause
            if (!isJumping && ticks % 250 == 0 && kotlin.random.Random.nextFloat() < 0.20f) {
                isIdlePausing = true
                delay(kotlin.random.Random.nextLong(1500, 3200))
                isIdlePausing = false
                if (kotlin.random.Random.nextBoolean()) direction *= -1f
                continue
            }

            // Occasional direction reversal
            if (ticks % 160 == 0 && kotlin.random.Random.nextFloat() < 0.18f) {
                direction *= -1f
            }

            // Check nearby pets for social interactions
            if (ticks % 60 == 0) {
                val nearby = com.example.util.PetSocialMediator.getNearbyPet(petIndex, posXPercent, 0f)
                if (nearby != null) {
                    val eventChoice = kotlin.random.Random.nextFloat()
                    when {
                        eventChoice < 0.35f -> {
                            // High leapfrog over-jump!
                            socialEmoji = "✨"
                            jumpVelocityDp = -13.5f
                            jumpOffsetDp = -2f
                            isJumping = true
                            coroutineScope.launch {
                                delay(900)
                                socialEmoji = null
                            }
                        }
                        eventChoice < 0.70f -> {
                            // Snuggle / heart reaction
                            socialEmoji = "💕"
                            isIdlePausing = true
                            delay(1200)
                            isIdlePausing = false
                            socialEmoji = null
                        }
                        else -> {
                            // Turn away or chase
                            direction = if (nearby.xPercent > posXPercent) -1f else 1f
                        }
                    }
                }
            }

            when (physState) {
                PetPhysState.FLOOR -> {
                    posYPercent = 0f
                    rotationDeg = 0f
                    if (!isJumping) {
                        posXPercent += direction * baseStep
                    }

                    // Wall collision check
                    if (posXPercent >= 0.84f) {
                        posXPercent = 0.84f
                        val choice = kotlin.random.Random.nextFloat()
                        if (choice < 0.35f && !isJumping) {
                            // Start climbing right wall!
                            physState = PetPhysState.CLIMB_RIGHT_WALL
                        } else {
                            direction = -1f
                        }
                    } else if (posXPercent <= 0.04f) {
                        posXPercent = 0.04f
                        val choice = kotlin.random.Random.nextFloat()
                        if (choice < 0.35f && !isJumping) {
                            // Start climbing left wall!
                            physState = PetPhysState.CLIMB_LEFT_WALL
                        } else {
                            direction = 1f
                        }
                    }

                    // Random spontaneous floor hop
                    if (!isJumping && ticks % 180 == 0 && kotlin.random.Random.nextFloat() < 0.25f) {
                        jumpVelocityDp = -7.5f
                        jumpOffsetDp = -1f
                        isJumping = true
                    }
                }

                PetPhysState.CLIMB_LEFT_WALL -> {
                    posXPercent = 0.02f
                    rotationDeg = 90f // Vertical orientation
                    posYPercent += baseStep * 1.2f

                    // Reach top / ceiling
                    if (posYPercent >= 0.85f) {
                        posYPercent = 0.85f
                        physState = PetPhysState.CEILING
                        direction = 1f
                    } else if (kotlin.random.Random.nextFloat() < 0.004f) {
                        // Leap across to right wall!
                        physState = PetPhysState.WALL_JUMP_CROSS
                        direction = 1f
                    }
                }

                PetPhysState.CLIMB_RIGHT_WALL -> {
                    posXPercent = 0.86f
                    rotationDeg = -90f // Vertical orientation
                    posYPercent += baseStep * 1.2f

                    // Reach top / ceiling
                    if (posYPercent >= 0.85f) {
                        posYPercent = 0.85f
                        physState = PetPhysState.CEILING
                        direction = -1f
                    } else if (kotlin.random.Random.nextFloat() < 0.004f) {
                        // Leap across to left wall!
                        physState = PetPhysState.WALL_JUMP_CROSS
                        direction = -1f
                    }
                }

                PetPhysState.CEILING -> {
                    posYPercent = 0.88f
                    rotationDeg = 180f // Upside down
                    posXPercent += direction * baseStep

                    // Reached edges or lose grip
                    if (posXPercent >= 0.84f || posXPercent <= 0.04f || (ticks % 120 == 0 && kotlin.random.Random.nextFloat() < 0.20f)) {
                        physState = PetPhysState.FALLING
                    }
                }

                PetPhysState.WALL_JUMP_CROSS -> {
                    rotationDeg += 18f
                    posXPercent += direction * baseStep * 2.2f
                    posYPercent -= baseStep * 0.4f // Slight arc

                    if (posXPercent >= 0.84f) {
                        posXPercent = 0.84f
                        physState = PetPhysState.CLIMB_RIGHT_WALL
                    } else if (posXPercent <= 0.04f) {
                        posXPercent = 0.04f
                        physState = PetPhysState.CLIMB_LEFT_WALL
                    } else if (posYPercent <= 0f) {
                        // Landed on floor safely instead of getting stuck
                        posYPercent = 0f
                        physState = PetPhysState.FLOOR
                        rotationDeg = 0f
                    }
                }

                PetPhysState.FALLING -> {
                    if (!isDragging) {
                        rotationDeg += 12f
                        fallVelocity += 0.00045f * speedFactor.coerceIn(0.5f, 2.5f)
                        posYPercent -= (baseStep * 1.8f + fallVelocity)

                        if (posYPercent <= 0f) {
                            posYPercent = 0f
                            physState = PetPhysState.FLOOR
                            rotationDeg = 0f
                            fallVelocity = 0f
                            // Impact bounce squash
                            coroutineScope.launch {
                                squashScaleY.snapTo(0.75f)
                                squashScaleX.snapTo(1.25f)
                                squashScaleY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh))
                            }
                            coroutineScope.launch {
                                squashScaleX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessHigh))
                            }
                        }
                    }
                }
            }

            com.example.util.PetSocialMediator.updatePetState(petIndex, variant, posXPercent, posYPercent)
        }
    }

    DisposableEffect(petIndex) {
        onDispose {
            com.example.util.PetSocialMediator.removePetState(petIndex)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val screenHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }

        val petHeightPx = with(density) { 48.dp.toPx() }

        // Floor baseline calculation with smooth interpolation
        val targetFloorYPx = if (isInChat && customTopYPx != null && customTopYPx > 280f) {
            (customTopYPx - petHeightPx + with(density) { 2.dp.toPx() }).coerceAtLeast(60f)
        } else if (isInChat) {
            (screenHeightPx - with(density) { 180.dp.toPx() }).coerceAtLeast(80f)
        } else {
            (screenHeightPx - with(density) { 108.dp.toPx() }).coerceAtLeast(80f)
        }

        val animatedFloorYPx by animateFloatAsState(
            targetValue = targetFloorYPx,
            animationSpec = spring(stiffness = Spring.StiffnessHigh),
            label = "pet_floor_y"
        )

        val ceilingYPx = with(density) { 48.dp.toPx() }
        val verticalTravelPx = (animatedFloorYPx - ceilingYPx).coerceAtLeast(100f)

        val xPx = screenWidthPx * posXPercent
        val baseYPx = animatedFloorYPx - (posYPercent * verticalTravelPx)

        // Running bounce/hop offset while walking on floor (disabled during active jump)
        val runningHopDp = if (isWalkMode && physState == PetPhysState.FLOOR && !isIdlePausing && !isJumping) {
            (abs(sin(posXPercent * 90f)) * 6f).dp
        } else 0.dp

        val jumpOffsetPx = with(density) { (jumpOffsetDp.dp - runningHopDp).toPx() }
        val finalYPx = baseYPx + jumpOffsetPx

        Box(
            modifier = Modifier
                .offset { IntOffset(xPx.roundToInt(), finalYPx.roundToInt()) }
                .size(52.dp)
                .graphicsLayer {
                    scaleX = (if (isWalkMode && physState == PetPhysState.FLOOR) direction else 1f) * squashScaleX.value
                    scaleY = squashScaleY.value
                    rotationZ = rotationDeg
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            physState = PetPhysState.FALLING
                            isJumping = false
                            jumpOffsetDp = 0f
                        },
                        onDragEnd = {
                            isDragging = false
                            fallVelocity = 0.002f
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val deltaXFrac = dragAmount.x / screenWidthPx
                            val deltaYFrac = -dragAmount.y / verticalTravelPx
                            posXPercent = (posXPercent + deltaXFrac).coerceIn(0.04f, 0.84f)
                            posYPercent = (posYPercent + deltaYFrac).coerceIn(0f, 0.88f)
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    HapticHelper.performLightHaptic(context, true)
                    // High responsive petting jump with stretch & immediate gravity arc
                    jumpVelocityDp = -15f
                    jumpOffsetDp = -2f
                    isJumping = true
                    coroutineScope.launch {
                        squashScaleY.snapTo(1.26f)
                        squashScaleX.snapTo(0.82f)
                        squashScaleY.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
                    }
                    coroutineScope.launch {
                        squashScaleX.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
                    }
                }
        ) {
            AppAsyncImage(
                model = petImageUrl,
                contentDescription = "Companion Pet",
                modifier = Modifier.fillMaxSize()
            )

            if (socialEmoji != null) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-22).dp)
                ) {
                    Text(
                        text = socialEmoji!!,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
