package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.util.ImageStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    colorFilter: androidx.compose.ui.graphics.ColorFilter? = null
) {
    val context = LocalContext.current
    val modelStr = ((model as? String) ?: (model as? Uri)?.toString())?.trim()

    // Fast O(1) memory cache lookup on UI thread; never blocks tab or chat transitions
    var decodedBitmap by remember(modelStr, model) {
        mutableStateOf(
            when {
                model is Bitmap -> model
                modelStr != null -> ImageStorageHelper.peekCachedBitmap(modelStr, 1024)
                else -> null
            }
        )
    }

    LaunchedEffect(modelStr, model) {
        if (decodedBitmap == null && modelStr != null && modelStr.isNotBlank()) {
            val loaded = withContext(Dispatchers.IO) {
                when {
                    modelStr.startsWith("media_bundle:") -> {
                        val items = com.example.util.ChatMediaHelper.deserialize(modelStr)
                        when (val first = items.firstOrNull()) {
                            is com.example.util.ChatMediaItem.Photo ->
                                ImageStorageHelper.loadCachedBitmap(context, first.base64Data, 1024)
                            is com.example.util.ChatMediaItem.Video ->
                                ImageStorageHelper.loadCachedBitmap(context, first.thumbnailBase64, 1024)
                            null -> null
                        }
                    }
                    modelStr.startsWith("video_frames:") -> {
                        val firstFrame = modelStr.removePrefix("video_frames:").split("||").firstOrNull { it.isNotBlank() }
                        if (firstFrame != null) ImageStorageHelper.loadCachedBitmap(context, firstFrame, 1024) else null
                    }
                    modelStr.startsWith("data:image/") ||
                        modelStr.startsWith("file://") ||
                        modelStr.startsWith("/") ||
                        modelStr.startsWith("content://") ||
                        (modelStr.length > 200 && !modelStr.startsWith("http")) -> {
                        ImageStorageHelper.loadCachedBitmap(context, modelStr, 1024)
                    }
                    else -> null
                }
            }
            if (loaded != null) {
                decodedBitmap = loaded
            }
        }
    }

    val drawableResId: Int? = remember(modelStr) {
        if (modelStr != null && modelStr.startsWith("drawable://")) {
            val resName = modelStr.removePrefix("drawable://")
            val id = context.resources.getIdentifier(resName, "drawable", context.packageName)
            if (id != 0) id else null
        } else if (model is Int) {
            model
        } else null
    }

    val rememberedImageBitmap = remember(decodedBitmap) {
        decodedBitmap?.asImageBitmap()
    }

    when {
        rememberedImageBitmap != null -> {
            Image(
                bitmap = rememberedImageBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
        drawableResId != null -> {
            Image(
                painter = painterResource(id = drawableResId),
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
        modelStr != null && (modelStr.startsWith("http://") || modelStr.startsWith("https://")) -> {
            val request = remember(context, modelStr) {
                ImageRequest.Builder(context)
                    .data(modelStr)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                placeholder = placeholder,
                error = error,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
        model != null && modelStr == null -> {
            val request = remember(context, model) {
                ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                alignment = alignment,
                placeholder = placeholder,
                error = error,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
        else -> {
            Box(
                modifier = modifier.background(fallbackColor),
                contentAlignment = Alignment.Center
            ) {
                if (error != null) {
                    Image(painter = error, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else if (modelStr.isNullOrBlank()) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
