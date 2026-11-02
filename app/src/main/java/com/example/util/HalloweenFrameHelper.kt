package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import com.example.ui.components.AVATAR_FRAME_INNER_RATIO
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object HalloweenFrameHelper {

    private const val FRAME_FILE_NAME = "halloween_avatar_frame.png"
    private var cachedUri: String? = null

    /**
     * Ensures the custom Halloween frame PNG exists on the device and returns its Uri string.
     */
    fun ensureHalloweenFrame(context: Context): String {
        cachedUri?.let { if (File(it.removePrefix("file://")).exists()) return it }

        val framesDir = File(context.filesDir, "frames")
        if (!framesDir.exists()) framesDir.mkdirs()

        val file = File(framesDir, FRAME_FILE_NAME)
        if (file.exists() && file.length() > 1024) {
            val uriStr = Uri.fromFile(file).toString()
            cachedUri = uriStr
            return uriStr
        }

        val bitmap = generateHalloweenFrameBitmap()
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val uriStr = Uri.fromFile(file).toString()
        cachedUri = uriStr
        return uriStr
    }

    /**
     * Procedurally paints a high-detail Halloween Avatar Frame (512x512)
     * with transparent interior matching AVATAR_FRAME_INNER_RATIO (0.72f),
     * glowing gothic vines, carved pumpkins, flying bats, and festive spooky styling.
     */
    fun generateHalloweenFrameBitmap(): Bitmap {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val center = size / 2f
        val innerAvatarRadius = (size * AVATAR_FRAME_INNER_RATIO) / 2f // ~184.32px
        val outerFrameRadius = size * 0.47f // ~240px

        // Paints
        val orangeGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFF6F00")
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
        }

        val purpleRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6A00F4")
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }

        val goldTrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFB703")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val darkVinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF1F112D")
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }

        val pumpkinFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFF7518") // Classic pumpkin orange
            style = Paint.Style.FILL
        }

        val pumpkinShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFC84600")
            style = Paint.Style.FILL
        }

        val pumpkinGlowEyes = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFEE32") // Glowing yellow eyes
            style = Paint.Style.FILL
        }

        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF2D6A4F") // Forest green stem
            style = Paint.Style.FILL
        }

        val batPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF100B1A") // Deep midnight black
            style = Paint.Style.FILL
        }

        // 1. Draw outer magical glowing aura around the avatar boundary
        canvas.drawCircle(center, center, (innerAvatarRadius + outerFrameRadius) / 2f, orangeGlowPaint)
        canvas.drawCircle(center, center, innerAvatarRadius + 8f, purpleRingPaint)
        canvas.drawCircle(center, center, innerAvatarRadius + 2f, goldTrimPaint)

        // 2. Draw Gothic Thorns and Vine Wreaths circling the frame
        val numSegments = 24
        for (i in 0 until numSegments) {
            val angle = (i.toFloat() / numSegments) * (2 * PI.toFloat())
            val r = (innerAvatarRadius + outerFrameRadius) / 2f + sin(angle * 6) * 12f
            val x = center + r * cos(angle)
            val y = center + r * sin(angle)

            // Small thorn leaf
            val thornAngle = angle + PI.toFloat() / 2f
            val thornLen = if (i % 2 == 0) 18f else 10f
            val tx = x + thornLen * cos(thornAngle)
            val ty = y + thornLen * sin(thornAngle)
            canvas.drawLine(x, y, tx, ty, darkVinePaint)

            // Spooky orange/purple magic orb beads
            if (i % 3 == 0) {
                val beadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (i % 2 == 0) Color.parseColor("#FFFF9E00") else Color.parseColor("#FFB5179E")
                }
                canvas.drawCircle(x, y, 5f, beadPaint)
            }
        }

        // 3. Draw Spider Web in top-right and top-left corners
        fun drawCornerWeb(startX: Float, startY: Float, sweepStart: Float) {
            val webPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(140, 230, 230, 255)
                style = Paint.Style.STROKE
                strokeWidth = 2.2f
            }
            val rays = 5
            for (r in 0 until rays) {
                val angle = sweepStart + (r.toFloat() / (rays - 1)) * (PI.toFloat() / 2.2f)
                val len = 85f
                canvas.drawLine(startX, startY, startX + len * cos(angle), startY + len * sin(angle), webPaint)
            }
            // Connecting arches
            for (dist in listOf(28f, 52f, 76f)) {
                val webPath = Path()
                for (r in 0 until rays) {
                    val angle = sweepStart + (r.toFloat() / (rays - 1)) * (PI.toFloat() / 2.2f)
                    val px = startX + dist * cos(angle)
                    val py = startY + dist * sin(angle)
                    if (r == 0) webPath.moveTo(px, py) else webPath.lineTo(px, py)
                }
                canvas.drawPath(webPath, webPaint)
            }
        }
        drawCornerWeb(45f, 45f, 0.1f)
        drawCornerWeb(size - 45f, 45f, PI.toFloat() * 0.55f)

        // 4. Draw Jack-o'-lantern pumpkins
        // Bottom large centerpiece pumpkin
        fun drawPumpkin(cx: Float, cy: Float, scale: Float) {
            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(scale, scale)

            // Stem
            val stemPath = Path().apply {
                moveTo(-4f, -32f)
                cubicTo(-12f, -48f, 2f, -52f, -2f, -56f)
                lineTo(8f, -54f)
                cubicTo(8f, -46f, 4f, -32f, 4f, -32f)
                close()
            }
            canvas.drawPath(stemPath, stemPaint)

            // Outer Lobes (Side bulges)
            canvas.drawOval(-42f, -32f, -10f, 32f, pumpkinShadowPaint)
            canvas.drawOval(10f, -32f, 42f, 32f, pumpkinShadowPaint)
            canvas.drawOval(-38f, -30f, -8f, 30f, pumpkinFillPaint)
            canvas.drawOval(8f, -30f, 38f, 30f, pumpkinFillPaint)

            // Center Lobes
            canvas.drawOval(-24f, -34f, 24f, 34f, pumpkinShadowPaint)
            canvas.drawOval(-20f, -32f, 20f, 32f, pumpkinFillPaint)

            // Glowing Eyes (triangles)
            val leftEye = Path().apply {
                moveTo(-20f, -12f)
                lineTo(-8f, -12f)
                lineTo(-14f, -24f)
                close()
            }
            val rightEye = Path().apply {
                moveTo(8f, -12f)
                lineTo(20f, -12f)
                lineTo(14f, -24f)
                close()
            }
            // Nose
            val nose = Path().apply {
                moveTo(-5f, -4f)
                lineTo(5f, -4f)
                lineTo(0f, -12f)
                close()
            }
            // Wicked jagged smile
            val mouth = Path().apply {
                moveTo(-24f, 6f)
                lineTo(-16f, 18f)
                lineTo(-8f, 10f)
                lineTo(0f, 20f)
                lineTo(8f, 10f)
                lineTo(16f, 18f)
                lineTo(24f, 6f)
                lineTo(16f, 12f)
                lineTo(8f, 4f)
                lineTo(0f, 12f)
                lineTo(-8f, 4f)
                lineTo(-16f, 12f)
                close()
            }
            canvas.drawPath(leftEye, pumpkinGlowEyes)
            canvas.drawPath(rightEye, pumpkinGlowEyes)
            canvas.drawPath(nose, pumpkinGlowEyes)
            canvas.drawPath(mouth, pumpkinGlowEyes)

            canvas.restore()
        }

        // Center bottom big pumpkin
        drawPumpkin(center, size - 42f, 0.95f)

        // Bottom left and right decorative companion pumpkins
        drawPumpkin(center - 110f, size - 64f, 0.65f)
        drawPumpkin(center + 110f, size - 64f, 0.65f)

        // 5. Draw Flying Bats near top
        fun drawBat(cx: Float, cy: Float, scale: Float, rotationDeg: Float) {
            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(scale, scale)
            canvas.rotate(rotationDeg)

            val batPath = Path().apply {
                moveTo(0f, -4f)
                // Left wing
                cubicTo(-14f, -24f, -34f, -22f, -50f, -12f)
                cubicTo(-42f, 0f, -28f, 2f, -24f, 14f)
                cubicTo(-18f, 4f, -8f, 12f, 0f, 6f)
                // Right wing
                cubicTo(8f, 12f, 18f, 4f, 24f, 14f)
                cubicTo(28f, 2f, 42f, 0f, 50f, -12f)
                cubicTo(34f, -22f, 14f, -24f, 0f, -4f)
                close()
            }
            canvas.drawPath(batPath, batPaint)
            // Bat tiny red glowing eyes
            val batEye = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED }
            canvas.drawCircle(-3f, -4f, 1.5f, batEye)
            canvas.drawCircle(3f, -4f, 1.5f, batEye)

            canvas.restore()
        }

        drawBat(center, 38f, 0.65f, 0f)
        drawBat(center - 95f, 62f, 0.48f, -18f)
        drawBat(center + 95f, 62f, 0.48f, 18f)

        return bitmap
    }
}
