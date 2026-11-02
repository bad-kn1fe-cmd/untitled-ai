package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

data class VideoAnalysisResult(
    val videoUri: String,
    val durationSec: Double,
    val resolution: String,
    val thumbnailBase64: String,
    val frameBase64List: List<String>
) {
    fun toStructuredPrompt(userText: String): String {
        val durationFormatted = String.format(Locale.US, "%.1f", durationSec)
        val header = "[🎬 Прикреплено видео (длительность: ${durationFormatted} сек, разрешение: $resolution). Извлечено ${frameBase64List.size} последовательных ключевых кадров по таймлайну видео: начало, развитие сцены, действие, финал. Внимательно проанализируй происходящее на кадрах, пойми действия и отреагируй в характере своего персонажа.]"
        return if (userText.isNotBlank()) "$header\n\n$userText" else "$header\n\n*показывает видеозапись*"
    }

    fun toMediaPayload(): String {
        return "video_frames:" + frameBase64List.joinToString("||")
    }
}

object VideoAnalysisHelper {

    suspend fun analyzeVideo(
        context: Context,
        uri: Uri,
        frameDimension: Int = 512,
        numFrames: Int = 4
    ): Result<VideoAnalysisResult> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationMsStr?.toLongOrNull() ?: 3000L
            val durationSec = (durationMs / 1000.0).coerceAtLeast(0.5)

            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: "1280"
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "720"
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION) ?: "0"
            val resolution = if (rotation == "90" || rotation == "270") "${height}x${width}" else "${width}x${height}"

            val frameTimesUs = mutableListOf<Long>()
            val durationUs = durationMs * 1000L
            if (numFrames <= 1) {
                frameTimesUs.add(durationUs / 2)
            } else {
                for (i in 0 until numFrames) {
                    val fraction = 0.10 + (0.80 * i / (numFrames - 1))
                    frameTimesUs.add((durationUs * fraction).toLong())
                }
            }

            val framesBase64 = mutableListOf<String>()
            for (timeUs in frameTimesUs) {
                val frameBmp = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                if (frameBmp != null) {
                    val scaled = scaleBitmap(frameBmp, frameDimension)
                    val base64 = bitmapToBase64(scaled)
                    if (base64 != null) {
                        framesBase64.add(base64)
                    }
                }
            }

            if (framesBase64.isEmpty()) {
                val fallbackBmp = retriever.frameAtTime
                if (fallbackBmp != null) {
                    val scaled = scaleBitmap(fallbackBmp, frameDimension)
                    val b64 = bitmapToBase64(scaled)
                    if (b64 != null) framesBase64.add(b64)
                }
            }

            if (framesBase64.isEmpty()) {
                return@withContext Result.failure(Exception("Не удалось извлечь кадры из видеозаписи"))
            }

            val thumbnail = framesBase64.first()

            Result.success(
                VideoAnalysisResult(
                    videoUri = uri.toString(),
                    durationSec = durationSec,
                    resolution = resolution,
                    thumbnailBase64 = thumbnail,
                    frameBase64List = framesBase64
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun scaleBitmap(src: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(src.width, src.height)
        if (maxSide <= maxDim) return src
        val ratio = maxDim.toFloat() / maxSide
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String? {
        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val bytes = stream.toByteArray()
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        } catch (_: Exception) {
            null
        }
    }
}
