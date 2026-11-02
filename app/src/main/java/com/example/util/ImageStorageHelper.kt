package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.collection.LruCache
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageStorageHelper {

    private val bitmapCache = LruCache<String, Bitmap>(250)

    fun uriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): String? {
        return try {
            // First decode bounds
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            // Calculate inSampleSize
            var sampleSize = 1
            while (srcWidth / (sampleSize * 2) >= maxDimension || srcHeight / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            // Decode actual bitmap with sample size
            inputStream = context.contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Memory-efficient
            }
            val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (sampledBitmap == null) return null

            // Scale precisely if still larger than maxDimension
            val scale = (maxDimension.toFloat() / maxOf(sampledBitmap.width, sampledBitmap.height)).coerceAtMost(1f)
            val finalBitmap = if (scale < 1f) {
                val targetW = (sampledBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
            } else {
                sampledBitmap
            }

            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            "data:image/jpeg;base64,$base64"
        } catch (_: Exception) {
            null
        }
    }

    fun stringUriToBase64(context: Context, uriString: String): String {
        if (uriString.isBlank()) return uriString
        if (uriString.startsWith("data:image/") || uriString.startsWith("drawable://")) return uriString
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) return uriString

        return try {
            val uri = Uri.parse(uriString)
            uriToBase64(context, uri) ?: uriString
        } catch (_: Exception) {
            uriString
        }
    }

    fun isBase64Image(uriString: String): Boolean {
        if (uriString.isBlank()) return false
        val s = uriString.trim()
        if (s.startsWith("data:image/") || s.startsWith("data:;base64,") || s.startsWith("data:application/octet-stream;base64,")) return true
        if (s.startsWith("/9j/") || s.startsWith("iVBORw0KGgo") || s.startsWith("R0lGOD") || s.startsWith("UklGR")) return true
        if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("file://") || s.startsWith("content://") || s.startsWith("drawable://")) return false
        return s.length > 100 && (s.contains("/9j/") || s.contains("iVBOR") || s.contains(";base64,"))
    }

    private fun cacheKeyFor(source: String, maxDimension: Int): String {
        val base = if (source.length > 80) {
            "${source.length}_${source.take(40)}_${source.takeLast(40)}"
        } else source
        return "${maxDimension}_$base"
    }

    fun peekCachedBitmap(source: String, maxDimension: Int = 512): Bitmap? {
        if (source.isBlank()) return null
        val key = cacheKeyFor(source.trim(), maxDimension)
        val cached = bitmapCache.get(key)
        if (cached != null && !cached.isRecycled) return cached
        // Also check legacy key without maxDimension prefix
        val legacyKey = if (source.length > 80) {
            "${source.length}_${source.take(40)}_${source.takeLast(40)}"
        } else source
        val legacyCached = bitmapCache.get(legacyKey)
        if (legacyCached != null && !legacyCached.isRecycled) return legacyCached
        return null
    }

    fun loadCachedBitmap(context: Context, source: String, maxDimension: Int = 1024): Bitmap? {
        val clean = source.trim()
        if (clean.isBlank()) return null
        peekCachedBitmap(clean, maxDimension)?.let { return it }

        val decoded: Bitmap? = when {
            isBase64Image(clean) -> base64ToBitmap(clean, maxDimension)
            clean.startsWith("file://") || clean.startsWith("/") -> {
                try {
                    val path = clean.removePrefix("file://")
                    val file = java.io.File(path)
                    if (!file.exists()) null
                    else {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, bounds)
                        var sampleSize = 1
                        if (bounds.outWidth > maxDimension || bounds.outHeight > maxDimension) {
                            while ((bounds.outWidth / (sampleSize * 2)) >= maxDimension ||
                                (bounds.outHeight / (sampleSize * 2)) >= maxDimension
                            ) {
                                sampleSize *= 2
                            }
                        }
                        val opts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                    }
                } catch (_: Exception) {
                    null
                }
            }
            clean.startsWith("content://") -> {
                loadBitmapFromSource(context, clean, maxDimension)
            }
            else -> null
        }

        if (decoded != null) {
            bitmapCache.put(cacheKeyFor(clean, maxDimension), decoded)
        }
        return decoded
    }

    fun base64ToBitmap(dataUrlOrBase64: String, maxDimension: Int = 384): Bitmap? {
        if (dataUrlOrBase64.isBlank()) return null
        val cacheKey = cacheKeyFor(dataUrlOrBase64.trim(), maxDimension)

        val cached = bitmapCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) return cached

        return try {
            val rawPart = if (dataUrlOrBase64.contains(",")) {
                dataUrlOrBase64.substringAfter(",")
            } else {
                dataUrlOrBase64
            }
            // Sanitize base64 string: replace json escaped slashes, strip whitespace and newlines
            val cleanBase64 = rawPart
                .replace("\\/", "/")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .trim()

            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            if (decodedBytes == null || decodedBytes.isEmpty()) return null

            // Determine sample size
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, boundsOptions)

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            var sampleSize = 1
            if (srcWidth > maxDimension || srcHeight > maxDimension) {
                while ((srcWidth / (sampleSize * 2)) >= maxDimension || (srcHeight / (sampleSize * 2)) >= maxDimension) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, decodeOptions)
            if (bitmap != null) {
                bitmapCache.put(cacheKey, bitmap)
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    fun saveBase64ImageFile(context: Context, dataUrlOrBase64: String, subfolder: String = "avatars"): String? {
        if (!isBase64Image(dataUrlOrBase64)) return null
        return try {
            val folder = java.io.File(context.filesDir, subfolder).apply {
                if (!exists()) mkdirs()
            }

            val rawPart = if (dataUrlOrBase64.contains(",")) {
                dataUrlOrBase64.substringAfter(",")
            } else {
                dataUrlOrBase64
            }
            val cleanBase64 = rawPart
                .replace("\\/", "/")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .trim()

            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            if (decodedBytes == null || decodedBytes.isEmpty()) return null

            val ext = when {
                dataUrlOrBase64.contains("image/png", ignoreCase = true) || cleanBase64.startsWith("iVBOR") -> "png"
                dataUrlOrBase64.contains("image/webp", ignoreCase = true) || cleanBase64.startsWith("UklGR") -> "webp"
                else -> "jpg"
            }

            val filename = "${subfolder.trimEnd('s')}_${System.currentTimeMillis()}_${(1000..9999).random()}.$ext"
            val destFile = java.io.File(folder, filename)

            destFile.outputStream().use { fos ->
                fos.write(decodedBytes)
                fos.flush()
            }

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveWallpaperFile(context: Context, uri: Uri): String? {
        return try {
            val folder = java.io.File(context.filesDir, "wallpapers").apply {
                if (!exists()) mkdirs()
            }
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video/") ||
                    uri.toString().endsWith(".mp4", ignoreCase = true) ||
                    uri.toString().endsWith(".webm", ignoreCase = true) ||
                    uri.toString().endsWith(".mkv", ignoreCase = true)

            if (isVideo) {
                val filename = "wallpaper_video_${System.currentTimeMillis()}_${(1000..9999).random()}.mp4"
                val destFile = java.io.File(folder, filename)
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val fos = java.io.FileOutputStream(destFile)
                inputStream.copyTo(fos)
                inputStream.close()
                fos.flush()
                fos.close()
                return Uri.fromFile(destFile).toString()
            }

            val filename = "wallpaper_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val destFile = java.io.File(folder, filename)

            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) {
                val retryStream = context.contentResolver.openInputStream(uri) ?: return null
                val fos = java.io.FileOutputStream(destFile)
                retryStream.copyTo(fos)
                retryStream.close()
                fos.flush()
                fos.close()
                return Uri.fromFile(destFile).toString()
            }

            val maxDimension = 1920
            val scale = (maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)).coerceAtMost(1f)
            val finalBitmap = if (scale < 1f) {
                val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else {
                bitmap
            }

            val fos = java.io.FileOutputStream(destFile)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            fos.flush()
            fos.close()

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun uriToPngBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024
    ): String? {
        return try {
            var inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            var sampleSize = 1
            while (srcWidth / (sampleSize * 2) >= maxDimension || srcHeight / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close() ?: return null
            if (sampledBitmap == null) return null

            val scale = (maxDimension.toFloat() / maxOf(sampledBitmap.width, sampledBitmap.height)).coerceAtMost(1f)
            val finalBitmap = if (scale < 1f) {
                val targetW = (sampledBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (sampledBitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetW, targetH, true)
            } else {
                sampledBitmap
            }

            bitmapToPngBase64(finalBitmap)
        } catch (_: Exception) {
            null
        }
    }

    fun bitmapToPngBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/png;base64,$base64"
    }

    fun bitmapToJpegBase64(bitmap: Bitmap, quality: Int = 90): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64"
    }

    fun putCachedBitmap(source: String, bitmap: Bitmap, maxDimension: Int = 1024) {
        val clean = source.trim()
        if (clean.isNotBlank() && !bitmap.isRecycled) {
            bitmapCache.put(cacheKeyFor(clean, maxDimension), bitmap)
            bitmapCache.put(clean, bitmap)
        }
    }

    fun savePngFile(context: Context, bitmap: Bitmap, subfolder: String = "frames"): String? {
        return try {
            val folder = java.io.File(context.filesDir, subfolder).apply {
                if (!exists()) mkdirs()
            }
            val filename = "${subfolder.trimEnd('s')}_${System.currentTimeMillis()}_${(1000..9999).random()}.png"
            val destFile = java.io.File(folder, filename)
            val fos = java.io.FileOutputStream(destFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            val uriStr = Uri.fromFile(destFile).toString()
            putCachedBitmap(uriStr, bitmap)
            uriStr
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveJpegFile(context: Context, bitmap: Bitmap, subfolder: String = "avatars", quality: Int = 90): String? {
        return try {
            val folder = java.io.File(context.filesDir, subfolder).apply {
                if (!exists()) mkdirs()
            }
            val filename = "${subfolder.trimEnd('s')}_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val destFile = java.io.File(folder, filename)
            val fos = java.io.FileOutputStream(destFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            fos.flush()
            fos.close()
            val uriStr = Uri.fromFile(destFile).toString()
            putCachedBitmap(uriStr, bitmap)
            uriStr
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun fileUriToPngBase64(fileUriString: String): String? {
        return try {
            val path = if (fileUriString.startsWith("file://")) fileUriString.removePrefix("file://") else fileUriString
            val file = java.io.File(path)
            if (!file.exists()) return null
            val bytes = file.readBytes()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        } catch (_: Exception) {
            null
        }
    }

    fun loadBitmapFromSource(context: Context, source: String, maxDimension: Int = 2048): Bitmap? {
        if (source.isBlank()) return null
        return try {
            if (isBase64Image(source)) {
                return base64ToBitmap(source, maxDimension)
            }
            if (source.startsWith("drawable://")) {
                val resName = source.removePrefix("drawable://")
                val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                if (resId != 0) {
                    return BitmapFactory.decodeResource(context.resources, resId)
                }
                return null
            }

            val uri = Uri.parse(source)
            var inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            var sampleSize = 1
            while (srcWidth / (sampleSize * 2) >= maxDimension || srcHeight / (sampleSize * 2) >= maxDimension) {
                sampleSize *= 2
            }

            inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteWallpaperFile(fileUriString: String): Boolean {
        return try {
            if (fileUriString.startsWith("file://")) {
                val path = fileUriString.removePrefix("file://")
                val file = java.io.File(path)
                if (file.exists()) file.delete() else true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}