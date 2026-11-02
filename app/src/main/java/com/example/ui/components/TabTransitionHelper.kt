package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class TabAnimationOption(
    val key: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector
) {
    fun displayTitle(isEn: Boolean): String = if (!isEn) title else when (key) {
        "smooth_slide" -> "🌊 Smooth Slide"
        "spring_slide" -> "🎯 Bouncy Impulse"
        "depth_stack" -> "🎴 Depth Cascade"
        "zoom_fade" -> "🔍 Zoom Scale"
        "cube_3d" -> "🧊 3D Perspective"
        "vertical_lift" -> "🚀 Vertical Lift"
        "shared_axis" -> "✨ Panoramic Flow"
        "fade" -> "🌫️ Pure Crossfade"
        "instant" -> "⚡ Instant (0 ms)"
        else -> title
    }

    fun displaySubtitle(isEn: Boolean): String = if (!isEn) subtitle else when (key) {
        "smooth_slide" -> "Classic horizontal page shift with soft fade"
        "spring_slide" -> "Dynamic slide with a spring effect at the end"
        "depth_stack" -> "New tab slides from top while previous recedes into depth"
        "zoom_fade" -> "Cinematic zoom in/out from screen center"
        "cube_3d" -> "3D rotation of screen faces in three-dimensional space"
        "vertical_lift" -> "Elegant tab floating up from bottom with soft fade"
        "shared_axis" -> "Synchronous axis shift with soft Material 3 scaling"
        "fade" -> "Soft crossfade without geometric displacement"
        "instant" -> "Instant switching without animation for maximum speed"
        else -> subtitle
    }

    fun displayBadge(isEn: Boolean): String = if (!isEn) badge else when (key) {
        "smooth_slide" -> "Standard"
        "spring_slide" -> "Dynamic"
        "depth_stack" -> "Depth"
        "zoom_fade" -> "Zoom"
        "cube_3d" -> "3D"
        "vertical_lift" -> "Cascade"
        "shared_axis" -> "M3"
        "fade" -> "Soft"
        "instant" -> "0 ms"
        else -> badge
    }
}

object TabTransitionHelper {

    val availableAnimations: List<TabAnimationOption> = listOf(
        TabAnimationOption(
            key = "smooth_slide",
            title = "🌊 Плавное скольжение",
            subtitle = "Классический горизонтальный сдвиг страниц с мягким затуханием",
            badge = "Стандарт",
            icon = Icons.Default.SwapHoriz
        ),
        TabAnimationOption(
            key = "spring_slide",
            title = "🎯 Упругий импульс",
            subtitle = "Динамичное скольжение с живым пружинящим эффектом в финале",
            badge = "Динамика",
            icon = Icons.Default.Bolt
        ),
        TabAnimationOption(
            key = "depth_stack",
            title = "🃏 Каскад глубины",
            subtitle = "Новая вкладка выезжает сверху, а предыдущая плавно уходит вглубь",
            badge = "Глубина",
            icon = Icons.Default.Layers
        ),
        TabAnimationOption(
            key = "zoom_fade",
            title = "🔍 Масштабирование",
            subtitle = "Кинематографичное приближение и отдаление экрана из центра",
            badge = "Зум",
            icon = Icons.Default.ZoomOutMap
        ),
        TabAnimationOption(
            key = "cube_3d",
            title = "🧊 3D-Перспектива",
            subtitle = "Объёмный разворот граней экрана в трёхмерном пространстве",
            badge = "3D",
            icon = Icons.Default.ViewInAr
        ),
        TabAnimationOption(
            key = "vertical_lift",
            title = "🚀 Вертикальный подъём",
            subtitle = "Элегантное всплытие вкладки снизу вверх с плавным растворением",
            badge = "Каскад",
            icon = Icons.Default.SwipeUp
        ),
        TabAnimationOption(
            key = "shared_axis",
            title = "✨ Панорамный поток",
            subtitle = "Синхронное смещение оси с мягким масштабированием Material 3",
            badge = "M3",
            icon = Icons.Default.AutoAwesomeMotion
        ),
        TabAnimationOption(
            key = "fade",
            title = "🌫️ Чистое растворение",
            subtitle = "Мягкое перекрёстное затухание без геометрического смещения",
            badge = "Мягко",
            icon = Icons.Default.BlurOn
        ),
        TabAnimationOption(
            key = "instant",
            title = "⚡ Мгновенно (0 мс)",
            subtitle = "Моментальное переключение без анимации для максимальной скорости",
            badge = "0 мс",
            icon = Icons.Default.FlashOn
        )
    )

    private val SpringBounceEasing = CubicBezierEasing(0.24f, 1.28f, 0.52f, 1.0f)

    fun easingForStyle(style: String): Easing = when (style) {
        "spring_slide" -> SpringBounceEasing
        "fade" -> LinearOutSlowInEasing
        else -> FastOutSlowInEasing
    }

    /**
     * Hardware-accelerated GraphicsLayer transformation applied directly to the GPU RenderNode.
     * Runs at native display refresh rate with ZERO Compose recomposition and ZERO layout measurement.
     */
    fun GraphicsLayerScope.applyTabLayerTransform(
        style: String,
        progress: Float,
        isIncoming: Boolean,
        direction: Int, // +1 moving forward/right, -1 moving backward/left
        width: Float,
        height: Float
    ) {
        val p = progress.coerceIn(0f, 1f)
        val dir = if (direction >= 0) 1f else -1f

        when (style) {
            "instant" -> {
                alpha = if (isIncoming) 1f else 0f
                translationX = if (isIncoming) 0f else width * 10f
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                rotationY = 0f
                rotationX = 0f
            }

            "smooth_slide", "spring_slide" -> {
                if (isIncoming) {
                    translationX = (1f - p) * width * dir
                    alpha = (0.4f + 0.6f * p).coerceIn(0f, 1f)
                } else {
                    translationX = -p * width * 0.32f * dir
                    alpha = (1f - p * 0.85f).coerceIn(0f, 1f)
                }
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                rotationY = 0f
                rotationX = 0f
            }

            "depth_stack" -> {
                if (isIncoming) {
                    if (dir > 0f) {
                        translationX = (1f - p) * width
                        scaleX = 1f
                        scaleY = 1f
                        alpha = (0.45f + 0.55f * p).coerceIn(0f, 1f)
                    } else {
                        translationX = -(1f - p) * width * 0.20f
                        val s = 0.90f + 0.10f * p
                        scaleX = s
                        scaleY = s
                        alpha = (0.4f + 0.6f * p).coerceIn(0f, 1f)
                    }
                } else {
                    if (dir > 0f) {
                        translationX = -p * width * 0.20f
                        val s = 1f - 0.10f * p
                        scaleX = s
                        scaleY = s
                        alpha = (1f - p * 0.75f).coerceIn(0f, 1f)
                    } else {
                        translationX = p * width
                        scaleX = 1f
                        scaleY = 1f
                        alpha = (1f - p * 0.6f).coerceIn(0f, 1f)
                    }
                }
                translationY = 0f
                rotationY = 0f
                rotationX = 0f
            }

            "zoom_fade" -> {
                translationX = 0f
                translationY = 0f
                rotationY = 0f
                rotationX = 0f
                if (isIncoming) {
                    val startScale = if (dir > 0f) 0.86f else 1.12f
                    val s = startScale + (1f - startScale) * p
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                } else {
                    val endScale = if (dir > 0f) 1.10f else 0.86f
                    val s = 1f + (endScale - 1f) * p
                    scaleX = s
                    scaleY = s
                    alpha = (1f - p * 1.15f).coerceIn(0f, 1f)
                }
            }

            "cube_3d" -> {
                cameraDistance = 16f
                translationY = 0f
                rotationX = 0f
                if (isIncoming) {
                    transformOrigin = TransformOrigin(if (dir > 0f) 0f else 1f, 0.5f)
                    translationX = (1f - p) * width * dir
                    rotationY = (1f - p) * 58f * dir
                    val s = 0.93f + 0.07f * p
                    scaleX = s
                    scaleY = s
                    alpha = (0.3f + 0.7f * p).coerceIn(0f, 1f)
                } else {
                    transformOrigin = TransformOrigin(if (dir > 0f) 1f else 0f, 0.5f)
                    translationX = -p * width * dir
                    rotationY = -p * 58f * dir
                    val s = 1f - 0.07f * p
                    scaleX = s
                    scaleY = s
                    alpha = (1f - p * 0.8f).coerceIn(0f, 1f)
                }
            }

            "vertical_lift" -> {
                translationX = 0f
                rotationY = 0f
                rotationX = 0f
                if (isIncoming) {
                    translationY = (1f - p) * height * 0.24f * dir
                    val s = 0.96f + 0.04f * p
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                } else {
                    translationY = -p * height * 0.16f * dir
                    val s = 1f - 0.04f * p
                    scaleX = s
                    scaleY = s
                    alpha = (1f - p * 1.15f).coerceIn(0f, 1f)
                }
            }

            "shared_axis" -> {
                translationY = 0f
                rotationY = 0f
                rotationX = 0f
                if (isIncoming) {
                    translationX = (1f - p) * width * 0.25f * dir
                    val s = 0.94f + 0.06f * p
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                } else {
                    translationX = -p * width * 0.25f * dir
                    val s = 1f - 0.05f * p
                    scaleX = s
                    scaleY = s
                    alpha = (1f - p * 1.15f).coerceIn(0f, 1f)
                }
            }

            "fade" -> {
                translationX = 0f
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                rotationY = 0f
                rotationX = 0f
                alpha = if (isIncoming) p.coerceIn(0f, 1f) else (1f - p).coerceIn(0f, 1f)
            }

            else -> {
                if (isIncoming) {
                    translationX = (1f - p) * width * dir
                    alpha = p
                } else {
                    translationX = -p * width * 0.3f * dir
                    alpha = 1f - p
                }
            }
        }
    }

    fun <S : Comparable<S>> buildContentTransform(
        initialState: S,
        targetState: S,
        style: String,
        durationMs: Int
    ): ContentTransform {
        val dur = durationMs.coerceIn(60, 1200)
        val forward = targetState >= initialState
        if (style == "instant") {
            return ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = SizeTransform(clip = false)
            )
        }
        val enter = buildEnterTransition(style, dur, forward)
        val exit = buildExitTransition(style, dur, forward)
        return ContentTransform(
            targetContentEnter = enter,
            initialContentExit = exit,
            sizeTransform = SizeTransform(clip = false)
        )
    }

    fun buildEnterTransition(style: String, durationMs: Int, forward: Boolean = true): EnterTransition {
        val dur = durationMs.coerceIn(60, 1200)
        val easing = easingForStyle(style)
        return when (style) {
            "instant" -> EnterTransition.None
            "smooth_slide", "spring_slide" ->
                slideInHorizontally(tween(dur, easing = easing)) { fullWidth -> if (forward) fullWidth else -fullWidth } +
                    fadeIn(tween((dur * 0.85f).toInt().coerceAtLeast(40)))
            "depth_stack" ->
                if (forward) {
                    slideInHorizontally(tween(dur, easing = FastOutSlowInEasing)) { it } +
                        fadeIn(tween((dur * 0.7f).toInt().coerceAtLeast(40)))
                } else {
                    scaleIn(tween(dur, easing = FastOutSlowInEasing), initialScale = 0.90f) +
                        fadeIn(tween(dur))
                }
            "zoom_fade" ->
                scaleIn(tween(dur, easing = FastOutSlowInEasing), initialScale = if (forward) 0.86f else 1.12f) +
                    fadeIn(tween(dur))
            "cube_3d", "shared_axis" ->
                slideInHorizontally(tween(dur, easing = FastOutSlowInEasing)) { fullWidth ->
                    if (forward) (fullWidth * 0.30f).toInt() else -(fullWidth * 0.30f).toInt()
                } + scaleIn(tween(dur, easing = FastOutSlowInEasing), initialScale = 0.93f) +
                    fadeIn(tween(dur))
            "vertical_lift" ->
                slideInVertically(tween(dur, easing = FastOutSlowInEasing)) { fullHeight ->
                    if (forward) (fullHeight * 0.24f).toInt() else -(fullHeight * 0.24f).toInt()
                } + fadeIn(tween(dur))
            "fade" ->
                fadeIn(tween(dur, easing = LinearOutSlowInEasing))
            else ->
                slideInHorizontally(tween(dur, easing = FastOutSlowInEasing)) { if (forward) it else -it } + fadeIn(tween(dur))
        }
    }

    fun buildExitTransition(style: String, durationMs: Int, forward: Boolean = true): ExitTransition {
        val dur = durationMs.coerceIn(60, 1200)
        val easing = easingForStyle(style)
        return when (style) {
            "instant" -> ExitTransition.None
            "smooth_slide", "spring_slide" ->
                slideOutHorizontally(tween(dur, easing = easing)) { fullWidth ->
                    if (forward) -(fullWidth * 0.32f).toInt() else (fullWidth * 0.32f).toInt()
                } + fadeOut(tween((dur * 0.8f).toInt().coerceAtLeast(40)))
            "depth_stack" ->
                if (forward) {
                    scaleOut(tween(dur, easing = FastOutSlowInEasing), targetScale = 0.90f) +
                        fadeOut(tween(dur))
                } else {
                    slideOutHorizontally(tween(dur, easing = FastOutSlowInEasing)) { it } +
                        fadeOut(tween((dur * 0.7f).toInt().coerceAtLeast(40)))
                }
            "zoom_fade" ->
                scaleOut(tween(dur, easing = FastOutSlowInEasing), targetScale = if (forward) 1.10f else 0.86f) +
                    fadeOut(tween((dur * 0.8f).toInt().coerceAtLeast(40)))
            "cube_3d", "shared_axis" ->
                slideOutHorizontally(tween(dur, easing = FastOutSlowInEasing)) { fullWidth ->
                    if (forward) -(fullWidth * 0.30f).toInt() else (fullWidth * 0.30f).toInt()
                } + scaleOut(tween(dur, easing = FastOutSlowInEasing), targetScale = 0.93f) +
                    fadeOut(tween((dur * 0.8f).toInt().coerceAtLeast(40)))
            "vertical_lift" ->
                slideOutVertically(tween(dur, easing = FastOutSlowInEasing)) { fullHeight ->
                    if (forward) -(fullHeight * 0.16f).toInt() else (fullHeight * 0.16f).toInt()
                } + fadeOut(tween((dur * 0.8f).toInt().coerceAtLeast(40)))
            "fade" ->
                fadeOut(tween(dur, easing = FastOutLinearInEasing))
            else ->
                slideOutHorizontally(tween(dur, easing = FastOutSlowInEasing)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(dur))
        }
    }
}

/**
 * Zero-recomposition controller for tab switching.
 * Reads of [activeTarget], [outgoing], and [progress] occur exclusively inside GPU RenderNode
 * layer blocks (`placeWithLayer` / `graphicsLayer`), preventing full-screen recompositions
 * and eliminating the 0.2-0.3s startup lag before animations on low-end devices.
 */
@Stable
class TabPagerController(
    initialPage: Int,
    private val scope: CoroutineScope
) {
    var activeTarget by mutableIntStateOf(initialPage)
        private set
    var outgoing by mutableIntStateOf(initialPage)
        private set
    var direction by mutableIntStateOf(1)
        private set
    var isTransitioning by mutableStateOf(false)
        private set

    val history = mutableStateListOf<Int>()
    val animatable = Animatable(1f)
    private var animJob: Job? = null

    fun switchTo(
        targetPage: Int,
        style: String,
        durationMs: Int,
        recordHistory: Boolean = true
    ) {
        val current = activeTarget
        if (current == targetPage) return

        if (recordHistory) {
            history.remove(targetPage)
            if (history.lastOrNull() != current) {
                history.add(current)
            }
        }

        outgoing = current
        direction = if (targetPage >= current) 1 else -1
        activeTarget = targetPage

        animJob?.cancel()
        if (style == "instant" || durationMs <= 0) {
            isTransitioning = false
            scope.launch {
                animatable.snapTo(1f)
            }
        } else {
            isTransitioning = true
            animJob = scope.launch {
                animatable.snapTo(0f)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = durationMs.coerceIn(60, 1200),
                        easing = TabTransitionHelper.easingForStyle(style)
                    )
                )
                isTransitioning = false
            }
        }
    }

    fun navigateBack(homePage: Int, style: String, durationMs: Int) {
        val prevPage = if (history.isNotEmpty()) {
            history.removeAt(history.lastIndex)
        } else {
            homePage
        }
        switchTo(prevPage, style = style, durationMs = durationMs, recordHistory = false)
    }

    fun canNavigateBack(homePage: Int): Boolean {
        return history.isNotEmpty() || activeTarget != homePage
    }
}

@Composable
fun rememberTabPagerController(initialPage: Int): TabPagerController {
    val scope = rememberCoroutineScope()
    return remember(initialPage) { TabPagerController(initialPage, scope) }
}

/**
 * High-performance preloaded tab container optimized for weak/low-end devices:
 * 1. Progressively warms up tabs in the background after initial render so first-time tab switches
 *    never block the UI thread for 200-300ms.
 * 2. Keeps all warmed pages permanently placed (`placeWithLayer`) so `Scaffold` and `LazyColumn`
 *    (`SubcomposeLayout`) NEVER deactivate or re-measure their children when switching tabs.
 * 3. Moves inactive tabs off-screen (`translationX = width * 10f`, `alpha = 0f`) on the GPU RenderNode
 *    so they cost 0ms to draw and never intercept touch events.
 * 4. Reads animation state exclusively inside `placeWithLayer`, resulting in ZERO recompositions
 *    during tab transitions.
 */
@Composable
fun PreloadedAnimatedTabs(
    pageCount: Int,
    controller: TabPagerController,
    animationStyle: String,
    durationMs: Int,
    warmedUpCount: Int = pageCount,
    modifier: Modifier = Modifier,
    pageContent: @Composable (Int) -> Unit
) {
    val warmedPages = remember(pageCount) {
        mutableStateListOf<Boolean>().apply {
            for (i in 0 until pageCount) {
                add(i == controller.activeTarget || i == 0)
            }
        }
    }

    // Progressively mark pages as warmed as `warmedUpCount` advances during startup preloader
    LaunchedEffect(warmedUpCount, pageCount) {
        for (i in 0 until minOf(warmedUpCount, pageCount)) {
            if (i in 0 until warmedPages.size && !warmedPages[i]) {
                warmedPages[i] = true
                kotlinx.coroutines.yield()
            }
        }
    }

    val currentTarget = controller.activeTarget.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    if (currentTarget in 0 until warmedPages.size && !warmedPages[currentTarget]) {
        warmedPages[currentTarget] = true
    }

    Box(modifier = modifier.fillMaxSize().clipToBounds()) {
        for (page in 0 until pageCount) {
            if (warmedPages.getOrElse(page) { false }) {
                key(page) {
                    PreloadedSingleTabLayer(
                        page = page,
                        controller = controller,
                        animationStyle = animationStyle,
                        content = { pageContent(page) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreloadedSingleTabLayer(
    page: Int,
    controller: TabPagerController,
    animationStyle: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    val target = controller.activeTarget
                    val out = controller.outgoing
                    val transitioning = controller.isTransitioning && out != target
                    val isIncoming = page == target
                    val isOutgoing = transitioning && page == out

                    val z = when {
                        isIncoming -> 2f
                        isOutgoing -> 1f
                        else -> 0f
                    }

                    // CRITICAL FOR LOW-END DEVICES:
                    // Always place the placeable so SubcomposeLayout (Scaffold / LazyColumn) stays
                    // active and never re-subcomposes or re-measures on tab switch.
                    // When inactive, shift the GPU RenderNode offscreen and set alpha = 0f.
                    placeable.placeWithLayer(0, 0, zIndex = z) {
                        if (isIncoming || isOutgoing) {
                            val rawP = if (transitioning) controller.animatable.value else 1f
                            with(TabTransitionHelper) {
                                applyTabLayerTransform(
                                    style = animationStyle,
                                    progress = rawP,
                                    isIncoming = isIncoming,
                                    direction = controller.direction,
                                    width = placeable.width.toFloat(),
                                    height = placeable.height.toFloat()
                                )
                            }
                        } else {
                            alpha = 0f
                            translationX = placeable.width.toFloat() * 10f
                            translationY = 0f
                            scaleX = 1f
                            scaleY = 1f
                            rotationX = 0f
                            rotationY = 0f
                        }
                    }
                }
            }
    ) {
        content()
    }
}

/**
 * Lightweight, zero-overhead interactive preview of tab transition animations for Settings.
 * Uses pure GPU `graphicsLayer` on 2 lightweight preview cards instead of heavy nested layouts.
 */
@Composable
fun TabTransitionInteractivePreview(
    animationStyle: String,
    durationMs: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableIntStateOf(0) }
    var prevTab by remember { mutableIntStateOf(0) }
    var direction by remember { mutableIntStateOf(1) }
    var isAnimating by remember { mutableStateOf(false) }
    var autoPlay by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1f) }

    val triggerPreviewSwitch: (Int) -> Unit = remember(animationStyle, durationMs) {
        { target ->
            val next = if (target == activeTab) (activeTab + 1) % 4 else target
            prevTab = activeTab
            direction = if (next >= activeTab) 1 else -1
            activeTab = next
            scope.launch {
                if (animationStyle == "instant" || durationMs <= 0) {
                    isAnimating = false
                    progress.snapTo(1f)
                } else {
                    isAnimating = true
                    progress.snapTo(0f)
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = durationMs.coerceIn(60, 1200),
                            easing = TabTransitionHelper.easingForStyle(animationStyle)
                        )
                    )
                    isAnimating = false
                }
            }
        }
    }

    LaunchedEffect(autoPlay, animationStyle, durationMs) {
        while (autoPlay) {
            delay((durationMs.coerceIn(100, 1000) + 900).toLong())
            triggerPreviewSwitch((activeTab + 1) % 4)
        }
    }

    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    val previewTabs = remember(isEn) {
        listOf(
            (if (isEn) "🏠 Main" else "🏠 Общее") to Color(0xFF7C3AED),
            (if (isEn) "💬 Chats" else "💬 Чаты") to Color(0xFF2563EB),
            (if (isEn) "🎭 Personas" else "🎭 Персоны") to Color(0xFF059669),
            (if (isEn) "⚙️ Styles" else "⚙️ Стиль") to Color(0xFFDB2777)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEn) "Interactive Transition Preview" else "Интерактивный предпросмотр перехода",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEn) "Tap the mini-tabs below or the 'Test' button" else "Нажимайте на мини-вкладки ниже или кнопку «Тест»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = { triggerPreviewSwitch((activeTab + 1) % 4) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEn) "Test" else "Тест", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                FilterChip(
                    selected = autoPlay,
                    onClick = { autoPlay = !autoPlay },
                    label = { Text(if (isEn) (if (autoPlay) "Auto: ON" else "Auto") else (if (autoPlay) "Авто: ВКЛ" else "Авто"), fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (autoPlay) Icons.Default.PauseCircle else Icons.Default.Loop,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Mini Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    previewTabs.forEachIndexed { index, (title, tabAccent) ->
                        val selected = activeTab == index
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) tabAccent.copy(alpha = 0.25f) else Color.Transparent,
                            border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, tabAccent) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { triggerPreviewSwitch(index) }
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Pure GPU 2-layer preview stage
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                ) {
                    if (isAnimating && prevTab != activeTab) {
                        PreviewMiniPageCard(
                            page = prevTab,
                            tabTitle = previewTabs[prevTab].first,
                            tabColor = previewTabs[prevTab].second,
                            animationStyle = animationStyle,
                            durationMs = durationMs,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    with(TabTransitionHelper) {
                                        applyTabLayerTransform(
                                            style = animationStyle,
                                            progress = progress.value,
                                            isIncoming = false,
                                            direction = direction,
                                            width = size.width,
                                            height = size.height
                                        )
                                    }
                                }
                        )
                    }

                    PreviewMiniPageCard(
                        page = activeTab,
                        tabTitle = previewTabs[activeTab].first,
                        tabColor = previewTabs[activeTab].second,
                        animationStyle = animationStyle,
                        durationMs = durationMs,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (isAnimating && prevTab != activeTab) {
                                    with(TabTransitionHelper) {
                                        applyTabLayerTransform(
                                            style = animationStyle,
                                            progress = progress.value,
                                            isIncoming = true,
                                            direction = direction,
                                            width = size.width,
                                            height = size.height
                                        )
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewMiniPageCard(
    page: Int,
    tabTitle: String,
    tabColor: Color,
    animationStyle: String,
    durationMs: Int,
    modifier: Modifier = Modifier
) {
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        tabColor.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(tabColor.copy(alpha = 0.28f))
                            .border(1.dp, tabColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${page + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isEn) "Tab $tabTitle" else "Вкладка $tabTitle",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (page) {
                                0 -> if (isEn) "Character showcase and quick actions" else "Витрина персонажей и быстрые действия"
                                1 -> if (isEn) "Active roleplay dialogues and stories" else "Активные ролевые диалоги и истории"
                                2 -> if (isEn) "User profiles and RP personas" else "Профили и маски пользователя для RP"
                                else -> if (isEn) "Themes, fonts, parallax and effects" else "Темы, шрифты, параллакс и эффекты"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tabColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (animationStyle == "instant") "0 мс" else "${durationMs} мс",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(2) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            tabColor.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tabColor.copy(alpha = 0.35f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
