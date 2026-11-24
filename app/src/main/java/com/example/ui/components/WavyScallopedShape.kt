package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.sin

class WavyScallopedShape(
    val wavesCount: Int = 12,
    val depthPercent: Float = 0.06f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val width = size.width
        val height = size.height

        if (depthPercent <= 0.001f || wavesCount <= 0) {
            val radius = height / 2f
            return Outline.Rounded(RoundRect(Rect(0f, 0f, width, height), CornerRadius(radius, radius)))
        }

        // For wide horizontal buttons (> 1.6 ratio), render clean capsule shape
        if (width / height > 1.6f) {
            val radius = height / 2f
            return Outline.Rounded(RoundRect(Rect(0f, 0f, width, height), CornerRadius(radius, radius)))
        }

        // For square / icon buttons (~1:1), generate polar scalloped waves
        val path = Path()
        val centerX = width / 2f
        val centerY = height / 2f
        val rx = width / 2f
        val ry = height / 2f
        val amplitudeX = rx * depthPercent.coerceIn(0f, 0.22f)
        val amplitudeY = ry * depthPercent.coerceIn(0f, 0.22f)

        val steps = (wavesCount * 16).coerceAtLeast(64)
        val angleStep = (2.0 * Math.PI / steps)

        for (i in 0..steps) {
            val theta = i * angleStep
            val wave = cos(wavesCount * theta)
            val rFactX = rx - amplitudeX + (amplitudeX * (1f + wave.toFloat()) / 2f)
            val rFactY = ry - amplitudeY + (amplitudeY * (1f + wave.toFloat()) / 2f)

            val x = centerX + rFactX * cos(theta).toFloat()
            val y = centerY + rFactY * sin(theta).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
