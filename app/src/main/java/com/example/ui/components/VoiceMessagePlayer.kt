package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.engine.AudioTranscriptionEngine
import com.example.util.VoicePlayerManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.sin

fun cleanVoiceTranscript(raw: String): String {
    return raw
        .replace(Regex("\\[?🎤?\\s*ГОЛОСОВОЕ СООБЩЕНИЕ[^\\]]*\\]?", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\[?🎤?[^\\]]*сек\\)?\\]?", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\(Примечание:[^\\)]*\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("^\\*отправил голосовое[^\\*]*\\*", RegexOption.IGNORE_CASE), "")
        .removeSurrounding("\"")
        .removeSurrounding("«", "»")
        .trim()
}

@Composable
fun VoiceMessagePlayer(
    audioFilePath: String,
    durationSec: Int,
    recognizedText: String,
    accentColor: Color,
    onTranscriptUpdated: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentPath by VoicePlayerManager.currentPlayingPath.collectAsState()
    val isPlayingGlobal by VoicePlayerManager.isPlaying.collectAsState()
    val currentProgress by VoicePlayerManager.progress.collectAsState()
    val currentPosSec by VoicePlayerManager.currentPositionSec.collectAsState()

    val isThisPlaying = currentPath == audioFilePath && isPlayingGlobal
    val isThisActive = currentPath == audioFilePath
    val playbackProgress = if (isThisActive) currentProgress else 0f

    var isTranscriptExpanded by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }

    var localTranscript by remember(recognizedText) {
        val cleaned = cleanVoiceTranscript(recognizedText)
        mutableStateOf(cleaned.ifBlank { null })
    }

    val infiniteTransition = rememberInfiniteTransition(label = "springWave")
    val springPhaseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "springPhase"
    )
    val activePhase = if (isThisPlaying) springPhaseFraction else 0f

    val displayDuration = if (isThisPlaying || (isThisActive && currentPosSec > 0)) {
        val mins = currentPosSec / 60
        val secs = currentPosSec % 60
        val totalMins = durationSec / 60
        val totalSecs = durationSec % 60
        String.format(Locale.US, "%d:%02d / %d:%02d", mins, secs, totalMins, totalSecs)
    } else {
        val totalMins = durationSec / 60
        val totalSecs = durationSec % 60
        String.format(Locale.US, "%d:%02d", totalMins, totalSecs)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play / Pause Circle Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable {
                            VoicePlayerManager.togglePlay(context, audioFilePath)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isThisPlaying) "Пауза" else "Слушать",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Spring waveform and duration
                Column(modifier = Modifier.weight(1f)) {
                    val inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

                    // Rounded Zigzag Spring Canvas with seek tap and drag detection
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .pointerInput(audioFilePath) {
                                detectTapGestures { offset ->
                                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                    if (currentPath != audioFilePath) {
                                        VoicePlayerManager.play(context, audioFilePath)
                                    }
                                    VoicePlayerManager.seekTo(fraction)
                                }
                            }
                            .pointerInput(audioFilePath) {
                                detectHorizontalDragGestures { change, _ ->
                                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                    if (currentPath != audioFilePath) {
                                        VoicePlayerManager.play(context, audioFilePath)
                                    }
                                    VoicePlayerManager.seekTo(fraction)
                                }
                            }
                    ) {
                        val centerY = size.height / 2f
                        val wavelength = 20.dp.toPx()
                        val amplitude = 7.5.dp.toPx().coerceAtMost(size.height / 2f - 3.dp.toPx())
                        val phaseOffsetPx = activePhase * wavelength

                        val springPath = Path()
                        val stepPx = 2.5f
                        var curX = 0f
                        var isFirst = true

                        while (curX <= size.width) {
                            val angle = (2.0 * Math.PI * (curX - phaseOffsetPx) / wavelength)
                            val curY = centerY + (amplitude * sin(angle)).toFloat()
                            if (isFirst) {
                                springPath.moveTo(curX, curY)
                                isFirst = false
                            } else {
                                springPath.lineTo(curX, curY)
                            }
                            curX += stepPx
                        }
                        val endAngle = (2.0 * Math.PI * (size.width - phaseOffsetPx) / wavelength)
                        val endY = centerY + (amplitude * sin(endAngle)).toFloat()
                        springPath.lineTo(size.width, endY)

                        val strokeWidthPx = 3.5.dp.toPx()
                        val progressX = (size.width * playbackProgress).coerceIn(0f, size.width)

                        // 1. Unplayed background spring line
                        clipRect(left = progressX, top = 0f, right = size.width, bottom = size.height) {
                            drawPath(
                                path = springPath,
                                color = inactiveTrackColor,
                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        // 2. Played active spring line
                        if (progressX > 0f) {
                            clipRect(left = 0f, top = 0f, right = progressX, bottom = size.height) {
                                drawPath(
                                    path = springPath,
                                    color = accentColor,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }

                            // 3. Playhead knob riding on the spring line
                            if (isThisActive) {
                                val playheadAngle = (2.0 * Math.PI * (progressX - phaseOffsetPx) / wavelength)
                                val playheadY = centerY + (amplitude * sin(playheadAngle)).toFloat()

                                drawCircle(
                                    color = accentColor,
                                    radius = 5.dp.toPx(),
                                    center = Offset(progressX, playheadY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.2.dp.toPx(),
                                    center = Offset(progressX, playheadY)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayDuration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )

                        // Button to show/hide voice text
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTranscriptExpanded) accentColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !isTranscribing) {
                                    if (!isTranscriptExpanded) {
                                        if (localTranscript.isNullOrBlank()) {
                                            isTranscribing = true
                                            scope.launch {
                                                try {
                                                    val file = File(audioFilePath)
                                                    val text = AudioTranscriptionEngine.transcribeAudioFile(context, file)
                                                    isTranscribing = false
                                                    if (!text.isNullOrBlank()) {
                                                        val clean = cleanVoiceTranscript(text)
                                                        localTranscript = clean
                                                        isTranscriptExpanded = true
                                                        onTranscriptUpdated?.invoke(clean)
                                                    } else {
                                                        localTranscript = "Речь не распознана"
                                                        isTranscriptExpanded = true
                                                    }
                                                } catch (e: Exception) {
                                                    isTranscribing = false
                                                    localTranscript = "Не удалось распознать речь"
                                                    isTranscriptExpanded = true
                                                }
                                            }
                                        } else {
                                            isTranscriptExpanded = true
                                        }
                                    } else {
                                        isTranscriptExpanded = false
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                if (isTranscribing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(11.dp),
                                        strokeWidth = 1.5.dp,
                                        color = accentColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Распознавание...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accentColor,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 10.5.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.TextFields,
                                        contentDescription = null,
                                        tint = if (isTranscriptExpanded) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isTranscriptExpanded) "Скрыть текст" else "Текст",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isTranscriptExpanded) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = if (isTranscriptExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isTranscriptExpanded) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transcript text section
            AnimatedVisibility(
                visible = isTranscriptExpanded && !localTranscript.isNullOrBlank(),
                enter = expandVertically(tween(180)) + fadeIn(tween(150)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(150))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = localTranscript ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 17.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Текст голосового", localTranscript ?: ""))
                                Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Скопировать текст",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
