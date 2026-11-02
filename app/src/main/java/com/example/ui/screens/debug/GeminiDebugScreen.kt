package com.example.ui.screens.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.debug.GeminiDebugLogger
import com.example.ai.debug.GeminiDebugSession
import com.example.ai.engine.AiProviderEngine
import com.example.data.local.entity.AiProviderProfileEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiDebugScreen(
    geminiProfiles: List<AiProviderProfileEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessions by GeminiDebugLogger.sessions.collectAsState()
    val activeSession by GeminiDebugLogger.activeSession.collectAsState()

    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var selectedProfileId by remember { mutableStateOf<Long?>(geminiProfiles.firstOrNull { it.providerType == "GEMINI" }?.id) }
    var customModelInput by remember { mutableStateOf("") }
    var isStreamingMode by remember { mutableStateOf(true) }
    var isRunningTest by remember { mutableStateOf(false) }
    var liveStreamedText by remember { mutableStateOf("") }

    val activeOrFirstSession: GeminiDebugSession? = remember(sessions, selectedSessionId, activeSession) {
        if (selectedSessionId != null) {
            sessions.find { it.requestId == selectedSessionId }
        } else {
            activeSession ?: sessions.firstOrNull()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gemini Debug", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Диагностика сетевого потока и chunks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (activeOrFirstSession != null) {
                        IconButton(onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Gemini Diagnostic", activeOrFirstSession.toFormattedLog()))
                            Toast.makeText(context, "Диагностический лог скопирован!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Копировать лог")
                        }
                    }
                    IconButton(onClick = {
                        GeminiDebugLogger.clearHistory()
                        selectedSessionId = null
                        liveStreamedText = ""
                        Toast.makeText(context, "История очищена", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить историю")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Панель тестового запроса", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            "Отправляет эталонный запрос без истории чата для проверки обрыва потока:\n«Напиши связный текст примерно на 500–1000 символов. Последними символами ответа обязательно должны быть TEST FINISHED 12345. После этого ничего не пиши.»",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Profile / model selection
                        val targetProfile = geminiProfiles.find { it.id == selectedProfileId }
                            ?: geminiProfiles.firstOrNull { it.providerType == "GEMINI" }

                        if (geminiProfiles.isNotEmpty()) {
                            Text("Выбранный профиль Gemini:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            geminiProfiles.filter { it.providerType == "GEMINI" }.forEach { prof ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedProfileId = prof.id }
                                        .background(if (targetProfile?.id == prof.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(prof.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Модель: ${prof.model.ifBlank { "gemini-3.5-flash-lite" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    RadioButton(
                                        selected = targetProfile?.id == prof.id,
                                        onClick = { selectedProfileId = prof.id }
                                    )
                                }
                            }
                        } else {
                            Text(
                                "⚠️ В приложении пока нет настроенного профиля Gemini с API-ключом. Перейдите в раздел Провайдеров и укажите ключ.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // Streaming toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Потоковый режим (Streaming / SSE)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("Получение chunks в реальном времени", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isStreamingMode,
                                onCheckedChange = { isStreamingMode = it }
                            )
                        }

                        // Run Test Button
                        Button(
                            onClick = {
                                val prof = targetProfile
                                if (prof == null || prof.apiKey.isBlank()) {
                                    Toast.makeText(context, "API-ключ Gemini не найден в профилях", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isRunningTest = true
                                liveStreamedText = ""
                                coroutineScope.launch {
                                    val testResult = AiProviderEngine.runGeminiDiagnosticTest(
                                        profile = prof,
                                        isStreaming = isStreamingMode,
                                        onTokenStream = { piece ->
                                            liveStreamedText += piece
                                        }
                                    )
                                    isRunningTest = false
                                    if (testResult.isSuccess) {
                                        Toast.makeText(context, "Тест завершен успешно!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Тест завершился ошибкой: ${testResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isRunningTest && (targetProfile != null && targetProfile.apiKey.isNotBlank()),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isRunningTest) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Выполняется диагностика...")
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Запустить тестовый запрос")
                            }
                        }
                    }
                }
            }

            // Session history selector tabs
            if (sessions.isNotEmpty()) {
                item {
                    Text("История запросов (${sessions.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sessions.take(6).forEach { s ->
                            val isSelected = (activeOrFirstSession?.requestId == s.requestId)
                            val isErr = s.error != null || s.isTimeout
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSessionId = s.requestId },
                                label = {
                                    Text(
                                        "${s.requestId} ${if (isErr) "⚠️" else "✓"}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isErr) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Live / Active Diagnostic Details
            if (activeOrFirstSession != null) {
                val s = activeOrFirstSession
                item {
                    DiagnosticSummaryCard(session = s)
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Полный полученный текст (${s.totalCharsReceived} симв.):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                val displayText = if (s.finalText.isNotEmpty()) s.finalText else liveStreamedText
                                if (displayText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("Gemini Text", displayText))
                                            Toast.makeText(context, "Текст скопирован!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Копировать текст", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            val displayText = if (s.finalText.isNotEmpty()) s.finalText else liveStreamedText
                            if (displayText.isEmpty()) {
                                Text(
                                    if (isRunningTest) "Ожидание первых данных от Gemini..." else "[Текст ответа не был получен]",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    displayText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                )

                                val testMarker = "TEST FINISHED 12345"
                                if (displayText.contains(testMarker)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                        Text("Маркер \"$testMarker\" успешно получен!", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                    }
                                } else if (s.connectionStatus == "COMPLETED" || s.connectionStatus == "STREAM_ENDED") {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Text("ВНИМАНИЕ: Маркер \"$testMarker\" отсутствует в конце! Текст оборвался!", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Streaming Chunks list
                if (s.chunks.isNotEmpty()) {
                    item {
                        Text("Полученные Streaming Chunks (${s.chunks.size} шт):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    items(s.chunks.takeLast(50)) { c ->
                        ChunkRow(chunk = c, baseStartTime = s.startTimeMs)
                    }
                }

                // Detailed lifecycle logs
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Журнал жизненного цикла (Events):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            s.logLines.forEach { line ->
                                Text(
                                    text = line,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = when {
                                        line.contains("[ERROR]") || line.contains("[TIMEOUT]") -> MaterialTheme.colorScheme.error
                                        line.contains("[SUCCESS]") -> Color(0xFF4CAF50)
                                        line.contains("[HTTP_STATUS]") -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Нажмите «Запустить тестовый запрос» для старта диагностики", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticSummaryCard(session: GeminiDebugSession) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val isErr = session.error != null || session.isTimeout

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isErr) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Request ID: ${session.requestId}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Badge(
                    containerColor = when (session.connectionStatus) {
                        "COMPLETED" -> Color(0xFF4CAF50)
                        "FAILED", "TIMEOUT" -> MaterialTheme.colorScheme.error
                        "STREAMING" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    }
                ) {
                    Text(session.connectionStatus, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            MetricRow("Модель", session.model)
            MetricRow("Время начала", timeFormat.format(Date(session.startTimeMs)))
            MetricRow("HTTP Status", "${session.httpStatusCode ?: "ожидание..."}")
            MetricRow("Получено Chunks", "${session.chunksCount} шт")
            MetricRow("Получено символов", "${session.totalCharsReceived} симв.")
            MetricRow("Finish Reason", session.finishReason ?: "не указан")
            if (session.usageMetadata != null) {
                MetricRow("Usage Metadata", session.usageMetadata!!)
            }
            MetricRow("Статус соединения", session.connectionStatus)

            if (session.isTimeout) {
                MetricRow("Таймаут", "ДА (произошел обрыв по timeout)", isError = true)
            }
            if (session.error != null) {
                MetricRow("Ошибка", session.error!!, isError = true)
            }
            if (session.emptyResponseReason != null) {
                MetricRow("Причина пустого ответа", session.emptyResponseReason!!, isError = true)
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}

@Composable
private fun ChunkRow(chunk: com.example.ai.debug.GeminiChunkLog, baseStartTime: Long) {
    val offsetMs = chunk.timestampMs - baseStartTime
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "#${chunk.chunkIndex} (+${offsetMs}ms) • ${chunk.byteSize}b • сумм: ${chunk.totalCharsSoFar} с.",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (chunk.finishReason != null) {
                    Text(
                        "finish: ${chunk.finishReason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Text(
                text = if (chunk.textPiece.isBlank()) "[пустой кусок / metadata]" else chunk.textPiece,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = if (chunk.textPiece.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
