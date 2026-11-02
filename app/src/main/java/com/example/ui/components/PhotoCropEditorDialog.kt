package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class CropMode {
    AVATAR_CIRCLE,
    BANNER_RECT
}

@Composable
fun PhotoCropEditorDialog(
    initialImageSource: String,
    cropMode: CropMode,
    onDismiss: () -> Unit,
    onConfirmCrop: (savedUri: String, base64: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    var currentSource by remember { mutableStateOf(initialImageSource) }
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Transformation state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var viewportHoleRadiusPx by remember { mutableFloatStateOf(0f) }
    var viewportBannerWidthPx by remember { mutableFloatStateOf(0f) }
    var viewportBannerHeightPx by remember { mutableFloatStateOf(0f) }

    // File picker to change photo right from editor
    val pickNewPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            currentSource = uri.toString()
            scale = 1f
            offsetX = 0f
            offsetY = 0f
            rotation = 0f
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF101012)
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Отмена",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (cropMode == CropMode.AVATAR_CIRCLE) {
                                if (isEn) "Adjust Avatar" else "Подгонка аватарки"
                            } else {
                                if (isEn) "Adjust Banner" else "Подгонка баннера"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (cropMode == CropMode.AVATAR_CIRCLE) {
                                if (isEn) "Move & zoom inside the circle" else "Двигайте и масштабируйте в круге"
                            } else {
                                if (isEn) "Move & zoom inside the banner" else "Двигайте и масштабируйте баннер"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }

                    Button(
                        onClick = {
                            val bmp = loadedBitmap ?: return@Button
                            isSaving = true
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    try {
                                        // Target dimensions for export
                                        val targetWidth = if (cropMode == CropMode.AVATAR_CIRCLE) 512 else 960
                                        val targetHeight = if (cropMode == CropMode.AVATAR_CIRCLE) 512 else 384

                                        val cropped = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                                        val canvas = AndroidCanvas(cropped)

                                        // Precise pixel-perfect scale ratio matching the viewport hole seen on screen
                                        val scaleRatio = if (cropMode == CropMode.AVATAR_CIRCLE) {
                                            val hRadius = if (viewportHoleRadiusPx > 0f) viewportHoleRadiusPx else 200f
                                            targetWidth.toFloat() / (hRadius * 2f)
                                        } else {
                                            val bWidth = if (viewportBannerWidthPx > 0f) viewportBannerWidthPx else targetWidth.toFloat()
                                            targetWidth.toFloat() / bWidth
                                        }

                                        val finalScale = scale * scaleRatio
                                        val finalOffsetX = offsetX * scaleRatio
                                        val finalOffsetY = offsetY * scaleRatio

                                        val matrix = Matrix()
                                        // 1. Center bitmap
                                        matrix.postTranslate(-bmp.width / 2f, -bmp.height / 2f)
                                        // 2. Rotate
                                        matrix.postRotate(rotation)
                                        // 3. Scale by final scale matching screen viewport
                                        matrix.postScale(finalScale, finalScale)
                                        // 4. Translate by center and user drag offset
                                        matrix.postTranslate(targetWidth / 2f + finalOffsetX, targetHeight / 2f + finalOffsetY)

                                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                                        if (cropMode == CropMode.AVATAR_CIRCLE) {
                                            val clipPath = android.graphics.Path()
                                            clipPath.addCircle(targetWidth / 2f, targetHeight / 2f, targetWidth / 2f, android.graphics.Path.Direction.CW)
                                            canvas.clipPath(clipPath)
                                        }

                                        canvas.drawBitmap(bmp, matrix, paint)

                                        val (savedFileUri, base64) = if (cropMode == CropMode.AVATAR_CIRCLE) {
                                            val fileUri = ImageStorageHelper.savePngFile(context, cropped, "avatars")
                                            val b64 = ImageStorageHelper.bitmapToPngBase64(cropped)
                                            Pair(fileUri, b64)
                                        } else {
                                            val fileUri = ImageStorageHelper.saveJpegFile(context, cropped, "banners", quality = 92)
                                            val b64 = ImageStorageHelper.bitmapToJpegBase64(cropped, quality = 90)
                                            Pair(fileUri, b64)
                                        }

                                        if (savedFileUri != null) {
                                            ImageStorageHelper.putCachedBitmap(savedFileUri, cropped)
                                        }
                                        ImageStorageHelper.putCachedBitmap(base64, cropped)

                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            if (savedFileUri != null) {
                                                onConfirmCrop(savedFileUri, base64)
                                            } else {
                                                onConfirmCrop(base64, base64)
                                            }
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
                        enabled = !isLoading && !isSaving && loadedBitmap != null,
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
                            Text(if (isEn) "Apply" else "Готово", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // Interactive Crop Viewport Area with deterministic measurement
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0C))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.12f, 6.0f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val cWidth = with(density) { maxWidth.toPx() }
                    val cHeight = with(density) { maxHeight.toPx() }
                    val centerX = cWidth / 2f
                    val centerY = cHeight / 2f

                    val holeRadius = if (cropMode == CropMode.AVATAR_CIRCLE) {
                        (minOf(cWidth, cHeight) * 0.38f).coerceIn(120f * density.density, 200f * density.density)
                    } else 0f
                    val bannerWidth = if (cropMode == CropMode.BANNER_RECT) {
                        (cWidth * 0.90f).coerceAtMost(360f * density.density)
                    } else 0f
                    val bannerHeight = if (cropMode == CropMode.BANNER_RECT) {
                        bannerWidth * 0.40f
                    } else 0f

                    // Keep viewport state in sync for export calculation
                    LaunchedEffect(holeRadius, bannerWidth, bannerHeight) {
                        viewportHoleRadiusPx = holeRadius
                        viewportBannerWidthPx = bannerWidth
                        viewportBannerHeightPx = bannerHeight
                    }

                    // Load bitmap and calculate initial fitting scale
                    LaunchedEffect(currentSource, holeRadius, bannerWidth) {
                        isLoading = true
                        withContext(Dispatchers.IO) {
                            val bmp = ImageStorageHelper.loadBitmapFromSource(context, currentSource, maxDimension = 2048)
                            loadedBitmap = bmp
                            isLoading = false
                            if (bmp != null) {
                                val fitScale = if (cropMode == CropMode.AVATAR_CIRCLE && holeRadius > 0f) {
                                    val minBmpDim = minOf(bmp.width, bmp.height).coerceAtLeast(1)
                                    (holeRadius * 2f) / minBmpDim
                                } else if (cropMode == CropMode.BANNER_RECT && bannerWidth > 0f) {
                                    maxOf(bannerWidth / bmp.width.coerceAtLeast(1), bannerHeight / bmp.height.coerceAtLeast(1))
                                } else 1f

                                withContext(Dispatchers.Main) {
                                    scale = fitScale.coerceIn(0.15f, 5.0f)
                                    offsetX = 0f
                                    offsetY = 0f
                                    rotation = 0f
                                }
                            }
                        }
                    }

                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (loadedBitmap != null) {
                        val bmp = loadedBitmap!!

                        // Canvas drawing the transformed image and the mask overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw image with translation and zoom
                            drawContext.canvas.nativeCanvas.save()
                            drawContext.canvas.nativeCanvas.translate(centerX + offsetX, centerY + offsetY)
                            drawContext.canvas.nativeCanvas.rotate(rotation)
                            drawContext.canvas.nativeCanvas.scale(scale, scale)
                            drawContext.canvas.nativeCanvas.translate(-bmp.width / 2f, -bmp.height / 2f)

                            drawContext.canvas.nativeCanvas.drawBitmap(
                                bmp,
                                0f,
                                0f,
                                null
                            )
                            drawContext.canvas.nativeCanvas.restore()

                            // Draw semi-transparent dark mask covering outside viewport
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.parseColor("#B8050507")
                            }
                            val saveCount = drawContext.canvas.nativeCanvas.saveLayer(
                                0f, 0f, cWidth, cHeight, null
                            )

                            // 1. Fill entire screen with dark semi-transparent veil
                            drawContext.canvas.nativeCanvas.drawRect(0f, 0f, cWidth, cHeight, paint)

                            // 2. Punch out the crop viewport hole
                            val clearPaint = android.graphics.Paint().apply {
                                xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                            }

                            if (cropMode == CropMode.AVATAR_CIRCLE) {
                                drawContext.canvas.nativeCanvas.drawCircle(centerX, centerY, holeRadius, clearPaint)
                            } else {
                                val left = centerX - bannerWidth / 2f
                                val top = centerY - bannerHeight / 2f
                                val right = centerX + bannerWidth / 2f
                                val bottom = centerY + bannerHeight / 2f
                                drawContext.canvas.nativeCanvas.drawRoundRect(
                                    left, top, right, bottom, 24f, 24f, clearPaint
                                )
                            }
                            drawContext.canvas.nativeCanvas.restoreToCount(saveCount)

                            // 3. Draw high-polish guidelines and borders around viewport
                            if (cropMode == CropMode.AVATAR_CIRCLE) {
                                // Crisp white circle border
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.9f),
                                    radius = holeRadius,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                // Subtle inner guide ring
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.25f),
                                    radius = holeRadius * 0.66f,
                                    center = Offset(centerX, centerY),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            } else {
                                val left = centerX - bannerWidth / 2f
                                val top = centerY - bannerHeight / 2f
                                val bSize = Size(bannerWidth, bannerHeight)

                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.9f),
                                    topLeft = Offset(left, top),
                                    size = bSize,
                                    cornerRadius = CornerRadius(24f, 24f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                // Rule of thirds grid lines inside banner
                                val oneThirdX = left + bannerWidth / 3f
                                val twoThirdsX = left + bannerWidth * 2f / 3f
                                val halfY = top + bannerHeight / 2f

                                drawLine(
                                    color = Color.White.copy(alpha = 0.25f),
                                    start = Offset(oneThirdX, top),
                                    end = Offset(oneThirdX, top + bannerHeight),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.25f),
                                    start = Offset(twoThirdsX, top),
                                    end = Offset(twoThirdsX, top + bannerHeight),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.25f),
                                    start = Offset(left, halfY),
                                    end = Offset(left + bannerWidth, halfY),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (isEn) "Failed to load image" else "Не удалось загрузить изображение",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Bottom Control Toolbar
                Surface(
                    color = Color(0xFF16161A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Zoom Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = scale,
                                onValueChange = { scale = it },
                                valueRange = 0.5f..3.5f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${(scale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.width(44.dp)
                            )
                        }

                        // Action Buttons: Reset, Rotate 90, Pick another image
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    rotation = 0f
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.FilterCenterFocus, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEn) "Center" else "По центру", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    rotation = (rotation + 90f) % 360f
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEn) "Rotate 90°" else "Повернуть", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    pickNewPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEn) "New photo" else "Другое фото", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
