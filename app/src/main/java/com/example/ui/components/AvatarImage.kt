package com.example.ui.components

import androidx.collection.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val composeImageBitmapCache = LruCache<String, ImageBitmap>(180)

fun prewarmAvatarInMemory(context: android.content.Context, uri: String, targetDim: Int = 160) {
    val cleanUri = uri.trim()
    if (cleanUri.isBlank() || cleanUri.startsWith("drawable://") || cleanUri.startsWith("http")) return
    val key = "${cleanUri.hashCode()}_${cleanUri.length}_$targetDim"
    synchronized(composeImageBitmapCache) {
        if (composeImageBitmapCache.get(key) != null) return
    }
    try {
        val loaded = ImageStorageHelper.loadCachedBitmap(context, cleanUri, targetDim) ?: return
        val imgBmp = loaded.asImageBitmap()
        synchronized(composeImageBitmapCache) {
            composeImageBitmapCache.put(key, imgBmp)
        }
    } catch (_: Exception) {}
}

private fun peekOrWrapImageBitmap(uri: String, targetDim: Int): ImageBitmap? {
    if (uri.isBlank()) return null
    val key = "${uri.hashCode()}_${uri.length}_$targetDim"
    synchronized(composeImageBitmapCache) {
        composeImageBitmapCache.get(key)?.let { return it }
    }
    val bmp = ImageStorageHelper.peekCachedBitmap(uri, targetDim) ?: return null
    val imgBmp = bmp.asImageBitmap()
    synchronized(composeImageBitmapCache) {
        composeImageBitmapCache.put(key, imgBmp)
    }
    return imgBmp
}

@Composable
fun AvatarImage(
    uri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    frameUri: String = "",
    accentTintIntensity: Float = 0.0f,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val cleanUri = uri.trim()
    val cleanFrameUri = frameUri.trim()
    val hasFrame = cleanFrameUri.isNotBlank()
    // Quantize targetDim into 2 shared buckets so chat bubbles (24dp), headers (40dp), and lists (56dp) share the exact same decoded bitmap
    val targetDim = if (size.value <= 64f) 160 else 320

    val activeTintFilter = remember(accentTintIntensity, accentColor) {
        if (accentTintIntensity > 0.0f) {
            ColorFilter.tint(accentColor.copy(alpha = (accentTintIntensity * 0.65f).coerceIn(0.0f, 0.40f)), BlendMode.SrcAtop)
        } else null
    }

    var avatarImageBitmap by remember(cleanUri, targetDim) {
        mutableStateOf(peekOrWrapImageBitmap(cleanUri, targetDim))
    }

    if (avatarImageBitmap == null && cleanUri.isNotBlank() && !cleanUri.startsWith("drawable://") && !cleanUri.startsWith("http")) {
        LaunchedEffect(cleanUri, targetDim) {
            val loaded = withContext(Dispatchers.IO) {
                ImageStorageHelper.loadCachedBitmap(context, cleanUri, targetDim)
            }
            if (loaded != null) {
                val key = "${cleanUri.hashCode()}_${cleanUri.length}_$targetDim"
                val imgBmp = loaded.asImageBitmap()
                synchronized(composeImageBitmapCache) {
                    composeImageBitmapCache.put(key, imgBmp)
                }
                avatarImageBitmap = imgBmp
            }
        }
    }

    var frameImageBitmap by remember(cleanFrameUri, targetDim) {
        mutableStateOf(peekOrWrapImageBitmap(cleanFrameUri, targetDim))
    }

    if (hasFrame && frameImageBitmap == null && !cleanFrameUri.startsWith("http")) {
        LaunchedEffect(cleanFrameUri, targetDim) {
            val loaded = withContext(Dispatchers.IO) {
                ImageStorageHelper.loadCachedBitmap(context, cleanFrameUri, targetDim)
            }
            if (loaded != null) {
                val key = "${cleanFrameUri.hashCode()}_${cleanFrameUri.length}_$targetDim"
                val imgBmp = loaded.asImageBitmap()
                synchronized(composeImageBitmapCache) {
                    composeImageBitmapCache.put(key, imgBmp)
                }
                frameImageBitmap = imgBmp
            }
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Inner Circular Avatar
        Box(
            modifier = Modifier
                .fillMaxSize(if (hasFrame) AVATAR_FRAME_INNER_RATIO else 1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val currentAvatarBmp = avatarImageBitmap
            when {
                currentAvatarBmp != null -> {
                    Image(
                        bitmap = currentAvatarBmp,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        colorFilter = activeTintFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                cleanUri == "drawable://app_logo_black" || cleanUri == "drawable://ic_app_logo_black" || cleanUri == "drawable://avatar_app_dark" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(size = size * 0.9f, shapeRadius = size * 0.45f, isDark = true)
                    }
                }
                cleanUri == "drawable://app_logo_white" || cleanUri == "drawable://ic_app_logo_white" -> {
                    AppLogo(size = size, shapeRadius = size * 0.5f, isDark = false)
                }
                cleanUri == "drawable://ic_app_logo" -> {
                    AppLogo(size = size, shapeRadius = size * 0.5f)
                }
                cleanUri.startsWith("drawable://") -> {
                    val resName = cleanUri.removePrefix("drawable://")
                    val resId = remember(resName) {
                        context.resources.getIdentifier(resName, "drawable", context.packageName)
                    }
                    if (resId != 0) {
                        Image(
                            painter = painterResource(id = resId),
                            contentDescription = contentDescription,
                            contentScale = ContentScale.Crop,
                            colorFilter = activeTintFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = contentDescription,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(size * 0.6f)
                        )
                    }
                }
                cleanUri.startsWith("http://") || cleanUri.startsWith("https://") -> {
                    val avatarRequest = remember(context, cleanUri) {
                        ImageRequest.Builder(context)
                            .data(cleanUri)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = avatarRequest,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        colorFilter = activeTintFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = contentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(size * 0.6f)
                    )
                }
            }
        }

        // Frame Overlay
        if (hasFrame) {
            val frameModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (FrameParallaxController.isEnabled) {
                        translationX = FrameParallaxController.currentTiltX
                        translationY = FrameParallaxController.currentTiltY
                    }
                }

            val currentFrameBmp = frameImageBitmap
            if (currentFrameBmp != null) {
                Image(
                    bitmap = currentFrameBmp,
                    contentDescription = "Avatar Frame",
                    contentScale = ContentScale.Fit,
                    modifier = frameModifier
                )
            } else if (cleanFrameUri.startsWith("http://") || cleanFrameUri.startsWith("https://")) {
                val frameRequest = remember(context, cleanFrameUri) {
                    ImageRequest.Builder(context)
                        .data(cleanFrameUri)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = frameRequest,
                    contentDescription = "Avatar Frame",
                    contentScale = ContentScale.Fit,
                    modifier = frameModifier
                )
            }
        }
    }
}