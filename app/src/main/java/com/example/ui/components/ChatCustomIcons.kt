package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom vector icons drawn manually on Canvas to match real app interfaces 1:1.
 */
object ChatCustomIcons {

    /**
     * Two horizontal parallel lines (DeepSeek & Gemini side drawer toggle).
     */
    @Composable
    fun TwoLines(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = LocalContentColor.current
    ) {
        Canvas(modifier = modifier.size(size)) {
            val strokeWidth = 2.dp.toPx()
            val startX = 3.dp.toPx()
            val endX = size.toPx() - 3.dp.toPx()
            val line1Y = size.toPx() * 0.36f
            val line2Y = size.toPx() * 0.64f

            drawLine(
                color = color,
                start = Offset(startX, line1Y),
                end = Offset(endX, line1Y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(startX, line2Y),
                end = Offset(endX, line2Y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }

    /**
     * DeepSeek Atom/Whirl Reasoning Icon (orbital rings with central nucleus).
     */
    @Composable
    fun DeepSeekAtom(
        modifier: Modifier = Modifier,
        size: Dp = 18.dp,
        color: Color = LocalContentColor.current
    ) {
        Canvas(modifier = modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val strokeWidth = 1.35.dp.toPx()

            // Center small circle
            drawCircle(
                color = color,
                radius = 2.2.dp.toPx(),
                center = center
            )

            // 3 rotated ellipses / orbits
            val orbitW = this.size.width * 0.85f
            val orbitH = this.size.height * 0.36f

            listOf(0f, 60f, 120f).forEach { angle ->
                withTransform({ rotate(angle, center) }) {
                    drawOval(
                        color = color,
                        topLeft = Offset(center.x - orbitW / 2f, center.y - orbitH / 2f),
                        size = androidx.compose.ui.geometry.Size(orbitW, orbitH),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
    }

    /**
     * Gemini 4-pointed Star Sparkle Icon.
     */
    @Composable
    fun GeminiSpark(
        modifier: Modifier = Modifier,
        size: Dp = 16.dp,
        color: Color = LocalContentColor.current
    ) {
        Canvas(modifier = modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            val path = Path().apply {
                moveTo(cx, 0f)
                cubicTo(cx, cy * 0.55f, cx * 1.45f, cy, w, cy)
                cubicTo(cx * 1.45f, cy, cx, cy * 1.45f, cx, h)
                cubicTo(cx, cy * 1.45f, cx * 0.55f, cy, 0f, cy)
                cubicTo(cx * 0.55f, cy, cx, cy * 0.55f, cx, 0f)
                close()
            }
            drawPath(path = path, color = color)
        }
    }

    /**
     * DeepSeek Voice Audio Waves Icon (sound bars inside parentheses/arcs).
     */
    @Composable
    fun DeepSeekVoice(
        modifier: Modifier = Modifier,
        size: Dp = 24.dp,
        color: Color = LocalContentColor.current
    ) {
        Canvas(modifier = modifier.size(size)) {
            val stroke = 1.8.dp.toPx()
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f

            // Outer circle
            drawCircle(
                color = color,
                radius = this.size.width / 2f - 2.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = stroke)
            )

            // Inner vertical waveform dot/bars
            // Center bar
            drawLine(
                color = color,
                start = Offset(cx, cy - 5.dp.toPx()),
                end = Offset(cx, cy + 5.dp.toPx()),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            // Left small arc
            val arcPathLeft = Path().apply {
                moveTo(cx - 3.5.dp.toPx(), cy - 3.5.dp.toPx())
                cubicTo(cx - 5.5.dp.toPx(), cy - 1.5.dp.toPx(), cx - 5.5.dp.toPx(), cy + 1.5.dp.toPx(), cx - 3.5.dp.toPx(), cy + 3.5.dp.toPx())
            }
            drawPath(arcPathLeft, color = color, style = Stroke(stroke, cap = StrokeCap.Round))

            // Right small arc
            val arcPathRight = Path().apply {
                moveTo(cx + 3.5.dp.toPx(), cy - 3.5.dp.toPx())
                cubicTo(cx + 5.5.dp.toPx(), cy - 1.5.dp.toPx(), cx + 5.5.dp.toPx(), cy + 1.5.dp.toPx(), cx + 3.5.dp.toPx(), cy + 3.5.dp.toPx())
            }
            drawPath(arcPathRight, color = color, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }

    /**
     * Character.AI Top Book/Novel with Bookmark Icon (angled book with ribbon/pages).
     */
    @Composable
    fun CaiBook(
        modifier: Modifier = Modifier,
        size: Dp = 22.dp,
        color: Color = LocalContentColor.current
    ) {
        Canvas(modifier = modifier.size(size)) {
            val stroke = 1.8.dp.toPx()
            val w = this.size.width
            val h = this.size.height

            // Book tilted ~45 degrees or slanted rectangle
            withTransform({ rotate(-35f, Offset(w / 2f, h / 2f)) }) {
                val bookLeft = w * 0.22f
                val bookTop = h * 0.15f
                val bookWidth = w * 0.56f
                val bookHeight = h * 0.70f
                val corner = 3.dp.toPx()

                // Book outline
                drawRoundRect(
                    color = color,
                    topLeft = Offset(bookLeft, bookTop),
                    size = androidx.compose.ui.geometry.Size(bookWidth, bookHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
                    style = Stroke(width = stroke)
                )

                // Spine crease line
                drawLine(
                    color = color,
                    start = Offset(bookLeft + bookWidth * 0.25f, bookTop),
                    end = Offset(bookLeft + bookWidth * 0.25f, bookTop + bookHeight),
                    strokeWidth = stroke
                )

                // Bookmark ribbon protruding at top right
                val ribbon = Path().apply {
                    moveTo(bookLeft + bookWidth * 0.6f, bookTop)
                    lineTo(bookLeft + bookWidth * 0.6f, bookTop - 3.5.dp.toPx())
                    lineTo(bookLeft + bookWidth * 0.75f, bookTop - 1.5.dp.toPx())
                    lineTo(bookLeft + bookWidth * 0.9f, bookTop - 3.5.dp.toPx())
                    lineTo(bookLeft + bookWidth * 0.9f, bookTop)
                }
                drawPath(ribbon, color = color, style = Fill)
            }
        }
    }

    /**
     * Character.AI Double Chevron Fast-Forward Send Icon (>>).
     */
    @Composable
    fun CaiDoubleChevron(
        modifier: Modifier = Modifier,
        size: Dp = 20.dp,
        color: Color = LocalContentColor.current
    ) {
        Canvas(modifier = modifier.size(size)) {
            val stroke = 2.2.dp.toPx()
            val w = this.size.width
            val h = this.size.height

            // First chevron >
            val p1 = Path().apply {
                moveTo(w * 0.22f, h * 0.22f)
                lineTo(w * 0.50f, h * 0.50f)
                lineTo(w * 0.22f, h * 0.78f)
            }
            drawPath(p1, color = color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

            // Second chevron >
            val p2 = Path().apply {
                moveTo(w * 0.52f, h * 0.22f)
                lineTo(w * 0.80f, h * 0.50f)
                lineTo(w * 0.52f, h * 0.78f)
            }
            drawPath(p2, color = color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    /**
     * Character.AI Refresh/Cycle Arrow Icon.
     */
    @Composable
    fun CaiCycleArrow(
        modifier: Modifier = Modifier,
        size: Dp = 20.dp,
        color: Color = Color(0xFF3B82F6) // Bright blue like screenshot
    ) {
        Canvas(modifier = modifier.size(size)) {
            val stroke = 2.2.dp.toPx()
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = this.size.width * 0.38f

            // 300 degree circular arc
            drawArc(
                color = color,
                startAngle = 50f,
                sweepAngle = 275f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Arrow tip at end of arc (around angle 325)
            val rad = Math.toRadians(330.0)
            val tipX = cx + (radius * Math.cos(rad)).toFloat()
            val tipY = cy + (radius * Math.sin(rad)).toFloat()

            val arrowPath = Path().apply {
                moveTo(tipX - 1.dp.toPx(), tipY - 5.dp.toPx())
                lineTo(tipX + 2.5.dp.toPx(), tipY)
                lineTo(tipX - 3.5.dp.toPx(), tipY + 3.dp.toPx())
            }
            drawPath(arrowPath, color = color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }

    /**
     * Janitor AI Speedometer / Reasoning / Prompt Gauge Icon.
     */
    @Composable
    fun JanitorGauge(
        modifier: Modifier = Modifier,
        size: Dp = 22.dp,
        color: Color = LocalContentColor.current
    ) {
        Canvas(modifier = modifier.size(size)) {
            val stroke = 1.8.dp.toPx()
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = this.size.width * 0.38f

            // Semi-circle gauge arc from ~140 deg to ~40 deg (sweep 260 deg)
            drawArc(
                color = color,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Small center circle
            drawCircle(
                color = color,
                radius = 2.dp.toPx(),
                center = Offset(cx, cy)
            )

            // Indicator needle pointing up-right (~45 deg)
            val needleAngle = Math.toRadians(-45.0)
            val needleLen = radius * 0.72f
            drawLine(
                color = color,
                start = Offset(cx, cy),
                end = Offset(
                    cx + (needleLen * Math.cos(needleAngle)).toFloat(),
                    cy + (needleLen * Math.sin(needleAngle)).toFloat()
                ),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    /**
     * Gemini Sparkle Pen / Edit Icon in top bar.
     */
    @Composable
    fun GeminiPenSparkle(
        modifier: Modifier = Modifier,
        size: Dp = 22.dp,
        color: Color = LocalContentColor.current
    ) {
        Canvas(modifier = modifier.size(size)) {
            val stroke = 1.8.dp.toPx()
            val w = this.size.width
            val h = this.size.height

            // Diagonal stylus / pen tilted 40 deg
            withTransform({ rotate(-40f, Offset(w / 2f, h / 2f)) }) {
                val cx = w / 2f
                val cy = h / 2f

                // Pen body outline
                val penPath = Path().apply {
                    moveTo(cx - 3.dp.toPx(), cy - 7.dp.toPx())
                    lineTo(cx + 3.dp.toPx(), cy - 7.dp.toPx())
                    lineTo(cx + 3.dp.toPx(), cy + 4.dp.toPx())
                    lineTo(cx, cy + 8.dp.toPx())
                    lineTo(cx - 3.dp.toPx(), cy + 4.dp.toPx())
                    close()
                }
                drawPath(penPath, color = color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Curved line near pen tip
                drawLine(
                    color = color,
                    start = Offset(cx - 3.dp.toPx(), cy + 4.dp.toPx()),
                    end = Offset(cx + 3.dp.toPx(), cy + 4.dp.toPx()),
                    strokeWidth = stroke * 0.8f
                )
            }

            // Small 4-pointed sparkle star at top left of pen
            val starCx = w * 0.28f
            val starCy = h * 0.28f
            val starR = 3.dp.toPx()
            val starPath = Path().apply {
                moveTo(starCx, starCy - starR)
                cubicTo(starCx, starCy, starCx, starCy, starCx + starR, starCy)
                cubicTo(starCx, starCy, starCx, starCy, starCx, starCy + starR)
                cubicTo(starCx, starCy, starCx, starCy, starCx - starR, starCy)
                cubicTo(starCx, starCy, starCx, starCy, starCx, starCy - starR)
            }
            drawPath(starPath, color = color)
        }
    }
}
