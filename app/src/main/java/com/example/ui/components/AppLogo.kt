package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shapeRadius: Dp = size * 0.28f,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    // Invert colors based on theme:
    // Light theme: Light-gray background (#EBEBF0) with Black symbol (#121214)
    // Dark theme: Dark-gray/charcoal background (#25232A / #1F1E24) with White/Off-white symbol (#F5F5F7)
    val bgColor = if (isDark) Color(0xFF28262E) else Color(0xFFEBEBF0)
    val symbolColor = if (isDark) Color(0xFFF5F5F7) else Color(0xFF121214)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(shapeRadius))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val scale = this.size.width / 108f

            // 1. Draw "U" frame for Untitled
            val uPath = Path().apply {
                moveTo(37f * scale, 36f * scale)
                lineTo(37f * scale, 56f * scale)
                cubicTo(
                    37f * scale, 65.4f * scale,
                    44.6f * scale, 73f * scale,
                    54f * scale, 73f * scale
                )
                cubicTo(
                    63.4f * scale, 73f * scale,
                    71f * scale, 65.4f * scale,
                    71f * scale, 56f * scale
                )
                lineTo(71f * scale, 36f * scale)
            }
            drawPath(
                path = uPath,
                color = symbolColor,
                style = Stroke(
                    width = 5.5f * scale,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 2. Draw Center AI Spark Star
            val starPath = Path().apply {
                moveTo(54f * scale, 36f * scale)
                cubicTo(
                    54f * scale, 43.5f * scale,
                    48.5f * scale, 49f * scale,
                    41f * scale, 49f * scale
                )
                cubicTo(
                    48.5f * scale, 49f * scale,
                    54f * scale, 54.5f * scale,
                    54f * scale, 62f * scale
                )
                cubicTo(
                    54f * scale, 54.5f * scale,
                    59.5f * scale, 49f * scale,
                    67f * scale, 49f * scale
                )
                cubicTo(
                    59.5f * scale, 49f * scale,
                    54f * scale, 43.5f * scale,
                    54f * scale, 36f * scale
                )
                close()
            }
            drawPath(
                path = starPath,
                color = symbolColor,
                style = Fill
            )

            // 3. Draw Prompt Cursor Accent
            drawLine(
                color = symbolColor,
                start = Offset(45f * scale, 80f * scale),
                end = Offset(63f * scale, 80f * scale),
                strokeWidth = 4f * scale,
                cap = StrokeCap.Round
            )

            // 4. Draw Accent Dot
            drawCircle(
                color = symbolColor,
                radius = 2.5f * scale,
                center = Offset(71f * scale, 31f * scale)
            )
        }
    }
}
