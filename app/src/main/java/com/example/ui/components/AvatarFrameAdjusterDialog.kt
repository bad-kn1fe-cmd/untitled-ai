package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Standard ratio between the inner circular avatar and the outer frame box.
 * 0.72f leaves 28% margin (14% on each side) for decorations, wings, ears, crowns, and borders.
 * This exact ratio MUST be identical between AvatarFrameAdjusterDialog and AvatarImage.
 */
const val AVATAR_FRAME_INNER_RATIO = 0.72f

@Composable
fun AvatarFrameAdjusterDialog(
    avatarUri: String,
    initialFrameSource: String,
    initialScale: Float = 1.0f,
    initialOffsetX: Float = 0f,
    initialOffsetY: Float = 0f,
    onDismiss: () -> Unit,
    onSaveFrame: (savedFileUri: String, base64: String, scale: Float, offsetX: Float, offsetY: Float) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    var currentFrameSource by remember { mutableStateOf(initialFrameSource) }
    var loadedFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadedAvatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Transformation for the frame in stage coordinates
    var frameScale by remember { mutableFloatStateOf(initialScale.coerceIn(0.35f, 3.0f)) }
    var frameOffsetX by remember { mutableFloatStateOf(initialOffsetX) }
    var frameOffsetY by remember { mutableFloatStateOf(initialOffsetY) }

    // Actual measured size of the interactive stage in pixels
    var stageSizePx by remember { mutableFloatStateOf(with(density) { 280.dp.toPx() }) }

    // Picker for new frame PNG
    val pickFrameLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            currentFrameSource = uri.toString()
            frameScale = 1.0f
            frameOffsetX = 0f
            frameOffsetY = 0f
        }
    }

    // Load frame and avatar bitmaps
    LaunchedEffect(currentFrameSource, avatarUri) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val frameBmp = ImageStorageHelper.loadBitmapFromSource(context, currentFrameSource, maxDimension = 1024)
            val avBmp = ImageStorageHelper.loadBitmapFromSource(context, avatarUri, maxDimension = 512)
            loadedFrameBitmap = frameBmp
            loadedAvatarBitmap = avBmp
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF101014)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isEn) "Fit Bot Frame" else "Подгонка рамки бота",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isEn) "Drag & scale frame over avatar" else "Двигайте и масштабируйте рамку",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    Button(
                        onClick = {
                            val frameBmp = loadedFrameBitmap ?: return@Button
                            isSaving = true
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    try {
                                        // Output canvas is 512x512 transparent PNG
                                        val outputSize = 512f
                                        val transparentCanvasBitmap = Bitmap.createBitmap(
                                            outputSize.toInt(),
                                            outputSize.toInt(),
                                            Bitmap.Config.ARGB_8888
                                        )
                                        val canvas = AndroidCanvas(transparentCanvasBitmap)

                                        // Exact mathematical conversion between stage pixels and output pixels:
                                        val factor = outputSize / stageSizePx.coerceAtLeast(1f)

                                        val matrix = Matrix()
                                        // 1. Center source frame
                                        matrix.postTranslate(-frameBmp.width / 2f, -frameBmp.height / 2f)

                                        // 2. Base scale: fits outputSize, then multiplied by frameScale
                                        val baseScale = outputSize / maxOf(frameBmp.width, frameBmp.height)
                                        matrix.postScale(baseScale * frameScale, baseScale * frameScale)

                                        // 3. Translate to center of output + converted offset
                                        matrix.postTranslate(
                                            outputSize / 2f + frameOffsetX * factor,
                                            outputSize / 2f + frameOffsetY * factor
                                        )

                                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                                        canvas.drawBitmap(frameBmp, matrix, paint)

                                        val savedFileUri = ImageStorageHelper.savePngFile(context, transparentCanvasBitmap, "frames")
                                        val base64 = ImageStorageHelper.bitmapToPngBase64(transparentCanvasBitmap)

                                        withContext(Dispatchers.Main) {
                                             isSaving = false
                                             val finalUri = savedFileUri ?: base64
                                             onSaveFrame(finalUri, base64, frameScale, frameOffsetX, frameOffsetY)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            onDismiss()
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading && !isSaving && loadedFrameBitmap != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEn) "Save Frame" else "Сохранить", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Scrollable container for stage and adjustment tools
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Main Interactive Canvas Stage
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF141418))
                            .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    frameScale = (frameScale * zoom).coerceIn(0.35f, 3.0f)
                                    val maxBound = stageSizePx * 0.75f
                                    frameOffsetX = (frameOffsetX + pan.x).coerceIn(-maxBound, maxBound)
                                    frameOffsetY = (frameOffsetY + pan.y).coerceIn(-maxBound, maxBound)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        } else {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cWidth = size.width
                                val cHeight = size.height
                                stageSizePx = cWidth

                                val centerX = cWidth / 2f
                                val centerY = cHeight / 2f

                                // Avatar radius is exactly AVATAR_FRAME_INNER_RATIO / 2 of canvas width
                                val avatarRadius = (cWidth * AVATAR_FRAME_INNER_RATIO) / 2f

                                // 1. Draw circular avatar clipped to center
                                val saveLayer = drawContext.canvas.nativeCanvas.saveLayer(0f, 0f, cWidth, cHeight, null)

                                val bgPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.DKGRAY
                                    isAntiAlias = true
                                }
                                drawContext.canvas.nativeCanvas.drawCircle(centerX, centerY, avatarRadius, bgPaint)

                                val avBmp = loadedAvatarBitmap
                                if (avBmp != null) {
                                    val avPaint = android.graphics.Paint().apply {
                                        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                                        isAntiAlias = true
                                        isFilterBitmap = true
                                    }
                                    val avScale = (avatarRadius * 2f) / minOf(avBmp.width, avBmp.height)
                                    val avMatrix = Matrix().apply {
                                        postTranslate(-avBmp.width / 2f, -avBmp.height / 2f)
                                        postScale(avScale, avScale)
                                        postTranslate(centerX, centerY)
                                    }
                                    drawContext.canvas.nativeCanvas.drawBitmap(avBmp, avMatrix, avPaint)
                                }
                                drawContext.canvas.nativeCanvas.restoreToCount(saveLayer)

                                // Faint guide circle showing exact avatar perimeter
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.45f),
                                    radius = avatarRadius,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )

                                // Faint center crosshairs
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(centerX, centerY - avatarRadius),
                                    end = Offset(centerX, centerY + avatarRadius),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(centerX - avatarRadius, centerY),
                                    end = Offset(centerX + avatarRadius, centerY),
                                    strokeWidth = 1.dp.toPx()
                                )

                                // 2. Draw user's frame overlay with scale and offset
                                val frameBmp = loadedFrameBitmap
                                if (frameBmp != null) {
                                    drawContext.canvas.nativeCanvas.save()

                                    // Center + user offset
                                    drawContext.canvas.nativeCanvas.translate(
                                        centerX + frameOffsetX,
                                        centerY + frameOffsetY
                                    )

                                    // Base scale: fits stage dimensions
                                    val baseScale = cWidth / maxOf(frameBmp.width, frameBmp.height)
                                    val finalScale = baseScale * frameScale
                                    drawContext.canvas.nativeCanvas.scale(finalScale, finalScale)
                                    drawContext.canvas.nativeCanvas.translate(-frameBmp.width / 2f, -frameBmp.height / 2f)

                                    val framePaint = android.graphics.Paint().apply {
                                        isAntiAlias = true
                                        isFilterBitmap = true
                                    }
                                    drawContext.canvas.nativeCanvas.drawBitmap(frameBmp, 0f, 0f, framePaint)
                                    drawContext.canvas.nativeCanvas.restore()
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Mini-Preview Strip to verify appearance in chat (48dp) and profile (72dp)
                    Surface(
                        color = Color(0xFF16161D),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEn) "Live Preview:" else "Живое превью:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            // 48dp Preview
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val pWidth = size.width
                                        val pCenterX = pWidth / 2f
                                        val pCenterY = pWidth / 2f
                                        val pAvatarRadius = (pWidth * AVATAR_FRAME_INNER_RATIO) / 2f

                                        // Draw avatar
                                        val saveLayer = drawContext.canvas.nativeCanvas.saveLayer(0f, 0f, pWidth, pWidth, null)
                                        val bgP = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; isAntiAlias = true }
                                        drawContext.canvas.nativeCanvas.drawCircle(pCenterX, pCenterY, pAvatarRadius, bgP)

                                        val avBmp = loadedAvatarBitmap
                                        if (avBmp != null) {
                                            val avP = android.graphics.Paint().apply {
                                                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                                                isAntiAlias = true
                                                isFilterBitmap = true
                                            }
                                            val avScale = (pAvatarRadius * 2f) / minOf(avBmp.width, avBmp.height)
                                            val avMat = Matrix().apply {
                                                postTranslate(-avBmp.width / 2f, -avBmp.height / 2f)
                                                postScale(avScale, avScale)
                                                postTranslate(pCenterX, pCenterY)
                                            }
                                            drawContext.canvas.nativeCanvas.drawBitmap(avBmp, avMat, avP)
                                        }
                                        drawContext.canvas.nativeCanvas.restoreToCount(saveLayer)

                                        // Draw frame
                                        val frameBmp = loadedFrameBitmap
                                        if (frameBmp != null) {
                                            drawContext.canvas.nativeCanvas.save()
                                            val ratio = pWidth / stageSizePx.coerceAtLeast(1f)
                                            drawContext.canvas.nativeCanvas.translate(
                                                pCenterX + frameOffsetX * ratio,
                                                pCenterY + frameOffsetY * ratio
                                            )
                                            val baseScale = pWidth / maxOf(frameBmp.width, frameBmp.height)
                                            val finalScale = baseScale * frameScale
                                            drawContext.canvas.nativeCanvas.scale(finalScale, finalScale)
                                            drawContext.canvas.nativeCanvas.translate(-frameBmp.width / 2f, -frameBmp.height / 2f)
                                            val frameP = android.graphics.Paint().apply { isAntiAlias = true; isFilterBitmap = true }
                                            drawContext.canvas.nativeCanvas.drawBitmap(frameBmp, 0f, 0f, frameP)
                                            drawContext.canvas.nativeCanvas.restore()
                                        }
                                    }
                                }
                                Text("48dp (Чат)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                            }

                            // 72dp Preview
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val pWidth = size.width
                                        val pCenterX = pWidth / 2f
                                        val pCenterY = pWidth / 2f
                                        val pAvatarRadius = (pWidth * AVATAR_FRAME_INNER_RATIO) / 2f

                                        val saveLayer = drawContext.canvas.nativeCanvas.saveLayer(0f, 0f, pWidth, pWidth, null)
                                        val bgP = android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY; isAntiAlias = true }
                                        drawContext.canvas.nativeCanvas.drawCircle(pCenterX, pCenterY, pAvatarRadius, bgP)

                                        val avBmp = loadedAvatarBitmap
                                        if (avBmp != null) {
                                            val avP = android.graphics.Paint().apply {
                                                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
                                                isAntiAlias = true
                                                isFilterBitmap = true
                                            }
                                            val avScale = (pAvatarRadius * 2f) / minOf(avBmp.width, avBmp.height)
                                            val avMat = Matrix().apply {
                                                postTranslate(-avBmp.width / 2f, -avBmp.height / 2f)
                                                postScale(avScale, avScale)
                                                postTranslate(pCenterX, pCenterY)
                                            }
                                            drawContext.canvas.nativeCanvas.drawBitmap(avBmp, avMat, avP)
                                        }
                                        drawContext.canvas.nativeCanvas.restoreToCount(saveLayer)

                                        val frameBmp = loadedFrameBitmap
                                        if (frameBmp != null) {
                                            drawContext.canvas.nativeCanvas.save()
                                            val ratio = pWidth / stageSizePx.coerceAtLeast(1f)
                                            drawContext.canvas.nativeCanvas.translate(
                                                pCenterX + frameOffsetX * ratio,
                                                pCenterY + frameOffsetY * ratio
                                            )
                                            val baseScale = pWidth / maxOf(frameBmp.width, frameBmp.height)
                                            val finalScale = baseScale * frameScale
                                            drawContext.canvas.nativeCanvas.scale(finalScale, finalScale)
                                            drawContext.canvas.nativeCanvas.translate(-frameBmp.width / 2f, -frameBmp.height / 2f)
                                            val frameP = android.graphics.Paint().apply { isAntiAlias = true; isFilterBitmap = true }
                                            drawContext.canvas.nativeCanvas.drawBitmap(frameBmp, 0f, 0f, frameP)
                                            drawContext.canvas.nativeCanvas.restore()
                                        }
                                    }
                                }
                                Text("56dp (Профиль)", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Controls Section
                    Surface(
                        color = Color(0xFF16161C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val maxOffsetBound = (stageSizePx * 0.75f).coerceAtLeast(100f)

                            // Size / Scale Slider + Fine controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isEn) "Scale" else "Размер",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.width(54.dp)
                                )
                                IconButton(
                                    onClick = { frameScale = (frameScale - 0.05f).coerceIn(0.35f, 3.0f) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Меньше", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Slider(
                                    value = frameScale.coerceIn(0.35f, 3.0f),
                                    onValueChange = { frameScale = it },
                                    valueRange = 0.35f..3.0f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                                IconButton(
                                    onClick = { frameScale = (frameScale + 0.05f).coerceIn(0.35f, 3.0f) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Больше", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${(frameScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.width(44.dp)
                                )
                            }

                            // Offset X Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Сдвиг X",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.width(54.dp)
                                )
                                IconButton(
                                    onClick = { frameOffsetX = (frameOffsetX - 2f).coerceIn(-maxOffsetBound, maxOffsetBound) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Slider(
                                    value = frameOffsetX.coerceIn(-maxOffsetBound, maxOffsetBound),
                                    onValueChange = { frameOffsetX = it },
                                    valueRange = -maxOffsetBound..maxOffsetBound,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.secondary,
                                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                                IconButton(
                                    onClick = { frameOffsetX = (frameOffsetX + 2f).coerceIn(-maxOffsetBound, maxOffsetBound) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${frameOffsetX.toInt()}px",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.width(44.dp)
                                )
                            }

                            // Offset Y Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Сдвиг Y",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.width(54.dp)
                                )
                                IconButton(
                                    onClick = { frameOffsetY = (frameOffsetY - 2f).coerceIn(-maxOffsetBound, maxOffsetBound) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Slider(
                                    value = frameOffsetY.coerceIn(-maxOffsetBound, maxOffsetBound),
                                    onValueChange = { frameOffsetY = it },
                                    valueRange = -maxOffsetBound..maxOffsetBound,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.secondary,
                                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                                IconButton(
                                    onClick = { frameOffsetY = (frameOffsetY + 2f).coerceIn(-maxOffsetBound, maxOffsetBound) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${frameOffsetY.toInt()}px",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.width(44.dp)
                                )
                            }

                            // Preset Buttons: Center, Fit to Avatar circle, 100%, Replace PNG
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        frameOffsetX = 0f
                                        frameOffsetY = 0f
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isEn) "Center" else "По центру", fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        frameScale = AVATAR_FRAME_INNER_RATIO
                                        frameOffsetX = 0f
                                        frameOffsetY = 0f
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text(if (isEn) "Circle 72%" else "Круг (72%)", fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        frameScale = 1.0f
                                        frameOffsetX = 0f
                                        frameOffsetY = 0f
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Text("100%", fontSize = 11.sp, maxLines = 1)
                                }

                                Button(
                                    onClick = {
                                        pickFrameLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (isEn) "PNG" else "Сменить", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
