package com.example.ai.debug

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Headers
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

data class GeminiChunkLog(
    val chunkIndex: Int,
    val timestampMs: Long,
    val byteSize: Int,
    val textPiece: String,
    val totalCharsSoFar: Int,
    val finishReason: String? = null,
    val tokensSoFar: Int? = null,
    val rawSnippet: String = ""
)

data class GeminiDebugSession(
    val requestId: String,
    val model: String,
    val isStreaming: Boolean,
    val startTimeMs: Long,
    var endTimeMs: Long? = null,
    var httpStatusCode: Int? = null,
    var httpHeaders: Map<String, String> = emptyMap(),
    var responseStartTimeMs: Long? = null,
    var streamEndTimeMs: Long? = null,
    var connectionClosedTimeMs: Long? = null,
    var chunksCount: Int = 0,
    var totalCharsReceived: Int = 0,
    var promptTokens: Int? = null,
    var candidatesTokens: Int? = null,
    var totalTokens: Int? = null,
    var finishReason: String? = null,
    var usageMetadata: String? = null,
    var connectionStatus: String = "CONNECTING", // CONNECTING, RESPONSE_RECEIVED, STREAMING, STREAM_ENDED, COMPLETED, FAILED, TIMEOUT, CLOSED
    var error: String? = null,
    var isTimeout: Boolean = false,
    var emptyResponseReason: String? = null,
    var finalText: String = "",
    var finalTextLength: Int = 0,
    val chunks: MutableList<GeminiChunkLog> = mutableListOf(),
    val logLines: MutableList<String> = mutableListOf()
) {
    fun toFormattedLog(): String {
        val sb = StringBuilder()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        sb.appendLine("==================================================")
        sb.appendLine("       GEMINI API DIAGNOSTIC REPORT: $requestId")
        sb.appendLine("==================================================")
        sb.appendLine("Request ID:           $requestId")
        sb.appendLine("Model:                $model")
        sb.appendLine("Streaming (SSE):      ${if (isStreaming) "YES" else "NO"}")
        sb.appendLine("Start Time:           ${sdf.format(Date(startTimeMs))}")
        if (endTimeMs != null) {
            sb.appendLine("End Time:             ${sdf.format(Date(endTimeMs!!))} (Total: ${endTimeMs!! - startTimeMs} ms)")
        }
        if (responseStartTimeMs != null) {
            sb.appendLine("Time to First Byte:   ${responseStartTimeMs!! - startTimeMs} ms")
        }
        sb.appendLine("Connection Status:    $connectionStatus")
        sb.appendLine("HTTP Status Code:     ${httpStatusCode ?: "N/A"}")
        sb.appendLine("Chunks Received:      $chunksCount")
        sb.appendLine("Total Characters:     $totalCharsReceived")
        sb.appendLine("Finish Reason:        ${finishReason ?: "Not specified"}")
        if (usageMetadata != null) {
            sb.appendLine("Usage Metadata:       $usageMetadata")
        }
        if (promptTokens != null || candidatesTokens != null || totalTokens != null) {
            sb.appendLine("Tokens:               Prompt=${promptTokens ?: 0}, Candidates=${candidatesTokens ?: 0}, Total=${totalTokens ?: 0}")
        }
        if (isTimeout) {
            sb.appendLine("Timeout Encountered:  YES (Check socket or read timeout)")
        }
        if (emptyResponseReason != null) {
            sb.appendLine("Empty/No-Answer Reason: $emptyResponseReason")
        }
        if (error != null) {
            sb.appendLine("Error Details:        $error")
        }

        if (httpHeaders.isNotEmpty()) {
            sb.appendLine("\n--- HTTP Response Headers (Sanitized, No Secrets) ---")
            httpHeaders.forEach { (name, value) ->
                sb.appendLine("  $name: $value")
            }
        }

        sb.appendLine("\n--- Final Assembled Text (Length: $finalTextLength chars) ---")
        if (finalText.isEmpty()) {
            sb.appendLine("[NO TEXT RECEIVED / ПУСТОЙ ТЕКСТ]")
        } else {
            sb.appendLine(finalText)
        }

        sb.appendLine("\n--- Lifecycle Events Log ---")
        logLines.forEach { line ->
            sb.appendLine(line)
        }

        if (chunks.isNotEmpty()) {
            sb.appendLine("\n--- Streaming Chunks Breakdown (${chunks.size} chunks) ---")
            chunks.forEach { c ->
                val timeOffset = c.timestampMs - startTimeMs
                val finishStr = if (c.finishReason != null) " [FinishReason=${c.finishReason}]" else ""
                val tokenStr = if (c.tokensSoFar != null) " [Tokens=${c.tokensSoFar}]" else ""
                val preview = c.textPiece.replace("\n", "\\n")
                sb.appendLine("#${c.chunkIndex} (+${timeOffset}ms) Size=${c.byteSize}b, CumulChars=${c.totalCharsSoFar}$finishStr$tokenStr -> \"$preview\"")
            }
        }

        sb.appendLine("==================================================")
        sb.appendLine("           END OF DIAGNOSTIC REPORT")
        sb.appendLine("==================================================")
        return sb.toString()
    }
}

object GeminiDebugLogger {
    private const val TAG = "GeminiDebug"
    private val counter = AtomicInteger(1)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    var isDebugModeGloballyEnabled: Boolean = false

    private val _sessions = MutableStateFlow<List<GeminiDebugSession>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private val _activeSession = MutableStateFlow<GeminiDebugSession?>(null)
    val activeSession = _activeSession.asStateFlow()

    fun nextRequestId(): String {
        return String.format(Locale.US, "GEMINI-DEBUG-%03d", counter.getAndIncrement())
    }

    private fun now(): String = timeFormat.format(Date())

    @Synchronized
    fun startSession(requestId: String, model: String, isStreaming: Boolean): GeminiDebugSession {
        val session = GeminiDebugSession(
            requestId = requestId,
            model = model,
            isStreaming = isStreaming,
            startTimeMs = System.currentTimeMillis()
        )
        val line = "[${now()}] [START] 1. Request started: ID=$requestId, Model=$model, Streaming=$isStreaming"
        session.logLines.add(line)
        Log.d(TAG, "[$requestId] $line")

        _activeSession.value = session
        val currentList = _sessions.value.toMutableList()
        currentList.add(0, session)
        if (currentList.size > 25) {
            currentList.removeAt(currentList.lastIndex)
        }
        _sessions.value = currentList
        return session
    }

    @Synchronized
    fun recordResponseStart(
        requestId: String,
        statusCode: Int,
        headers: Headers
    ) {
        val session = findSession(requestId) ?: return
        val sanitized = sanitizeHeaders(headers)
        session.httpStatusCode = statusCode
        session.httpHeaders = sanitized
        session.responseStartTimeMs = System.currentTimeMillis()
        session.connectionStatus = "RESPONSE_RECEIVED"

        val ttfb = session.responseStartTimeMs!! - session.startTimeMs
        val line1 = "[${now()}] [HTTP_STATUS] 3. HTTP status code: $statusCode (TTFB: ${ttfb}ms)"
        val line2 = "[${now()}] [HTTP_HEADERS] 4. Headers: ${sanitized.entries.joinToString("; ") { "${it.key}=${it.value}" }}"
        val line3 = "[${now()}] [STREAM_START] 5. Beginning of receiving response stream"
        session.logLines.add(line1)
        session.logLines.add(line2)
        session.logLines.add(line3)
        Log.d(TAG, "[$requestId] $line1")
        Log.d(TAG, "[$requestId] $line2")
        Log.d(TAG, "[$requestId] $line3")
        notifySessionUpdated(session)
    }

    @Synchronized
    fun recordChunk(
        requestId: String,
        chunkIndex: Int,
        byteSize: Int,
        textPiece: String,
        finishReason: String?,
        promptTokens: Int?,
        candidatesTokens: Int?,
        totalTokens: Int?,
        usageMetadataStr: String?,
        rawSnippet: String
    ) {
        val session = findSession(requestId) ?: return
        session.chunksCount++
        session.totalCharsReceived += textPiece.length
        session.connectionStatus = "STREAMING"

        if (finishReason != null && finishReason.isNotBlank()) {
            session.finishReason = finishReason
        }
        if (usageMetadataStr != null) {
            session.usageMetadata = usageMetadataStr
        }
        if (promptTokens != null) session.promptTokens = promptTokens
        if (candidatesTokens != null) session.candidatesTokens = candidatesTokens
        if (totalTokens != null) session.totalTokens = totalTokens

        val chunkLog = GeminiChunkLog(
            chunkIndex = chunkIndex,
            timestampMs = System.currentTimeMillis(),
            byteSize = byteSize,
            textPiece = textPiece,
            totalCharsSoFar = session.totalCharsReceived,
            finishReason = finishReason,
            tokensSoFar = candidatesTokens ?: totalTokens,
            rawSnippet = rawSnippet
        )
        session.chunks.add(chunkLog)

        val cleanPreview = textPiece.replace("\n", "\\n")
        val finishInfo = if (!finishReason.isNullOrBlank()) ", finishReason=$finishReason" else ""
        val tokenInfo = if (totalTokens != null) ", totalTokens=$totalTokens" else ""
        val line = "[${now()}] [CHUNK #$chunkIndex] 6,7,8,9. Chunk size: $byteSize bytes, piece: \"$cleanPreview\", cumulChars: ${session.totalCharsReceived}$tokenInfo$finishInfo"
        session.logLines.add(line)
        Log.d(TAG, "[$requestId] $line")
        notifySessionUpdated(session)
    }

    @Synchronized
    fun recordStreamEnd(requestId: String) {
        val session = findSession(requestId) ?: return
        session.streamEndTimeMs = System.currentTimeMillis()
        if (session.connectionStatus == "STREAMING" || session.connectionStatus == "RESPONSE_RECEIVED") {
            session.connectionStatus = "STREAM_ENDED"
        }
        val line = "[${now()}] [STREAM_END] 12. Streaming finished normally. Total chunks: ${session.chunksCount}, Total chars: ${session.totalCharsReceived}"
        session.logLines.add(line)
        Log.d(TAG, "[$requestId] $line")
        notifySessionUpdated(session)
    }

    @Synchronized
    fun recordConnectionClosed(requestId: String) {
        val session = findSession(requestId) ?: return
        session.connectionClosedTimeMs = System.currentTimeMillis()
        val line = "[${now()}] [HTTP_CLOSE] 13. HTTP connection closed"
        session.logLines.add(line)
        Log.d(TAG, "[$requestId] $line")
        notifySessionUpdated(session)
    }

    @Synchronized
    fun recordSuccess(requestId: String, finalText: String) {
        val session = findSession(requestId) ?: return
        session.endTimeMs = System.currentTimeMillis()
        session.finalText = finalText
        session.finalTextLength = finalText.length
        session.connectionStatus = "COMPLETED"

        val duration = session.endTimeMs!! - session.startTimeMs
        val line = "[${now()}] [SUCCESS] 16. Final assembled text received (length: ${finalText.length} chars, total time: ${duration}ms)"
        session.logLines.add(line)
        Log.d(TAG, "[$requestId] $line")
        notifySessionUpdated(session)
    }

    @Synchronized
    fun recordFailure(
        requestId: String,
        errorMsg: String,
        isTimeout: Boolean,
        emptyReason: String?,
        exception: Throwable?
    ) {
        val session = findSession(requestId) ?: return
        session.endTimeMs = System.currentTimeMillis()
        session.error = errorMsg
        session.isTimeout = isTimeout
        session.emptyResponseReason = emptyReason
        session.connectionStatus = if (isTimeout) "TIMEOUT" else "FAILED"

        val excClass = exception?.javaClass?.simpleName ?: "None"
        val line1 = "[${now()}] [ERROR] 14. Exception: $excClass - $errorMsg"
        session.logLines.add(line1)
        Log.e(TAG, "[$requestId] $line1", exception)

        if (isTimeout) {
            val lineTimeout = "[${now()}] [TIMEOUT] 15. Request timed out while waiting for Gemini response / chunks"
            session.logLines.add(lineTimeout)
            Log.e(TAG, "[$requestId] $lineTimeout")
        }

        if (emptyReason != null) {
            val lineReason = "[${now()}] [EMPTY_DECISION] 17. Reason app decided \"Ответ Gemini не пришел\": $emptyReason"
            session.logLines.add(lineReason)
            Log.e(TAG, "[$requestId] $lineReason")
        }

        notifySessionUpdated(session)
    }

    @Synchronized
    fun logEvent(requestId: String, eventName: String, details: String) {
        val session = findSession(requestId) ?: return
        val line = "[${now()}] [$eventName] $details"
        session.logLines.add(line)
        Log.d(TAG, "[$requestId] $line")
        notifySessionUpdated(session)
    }

    private fun findSession(requestId: String): GeminiDebugSession? {
        return _sessions.value.find { it.requestId == requestId }
    }

    private fun notifySessionUpdated(session: GeminiDebugSession) {
        if (_activeSession.value?.requestId == session.requestId) {
            _activeSession.value = session.copy(
                chunks = ArrayList(session.chunks),
                logLines = ArrayList(session.logLines)
            )
        }
        val current = _sessions.value.toMutableList()
        val index = current.indexOfFirst { it.requestId == session.requestId }
        if (index >= 0) {
            current[index] = session.copy(
                chunks = ArrayList(session.chunks),
                logLines = ArrayList(session.logLines)
            )
            _sessions.value = current
        }
    }

    fun sanitizeHeaders(headers: Headers): Map<String, String> {
        val sanitized = mutableMapOf<String, String>()
        for (i in 0 until headers.size) {
            val name = headers.name(i)
            val lower = name.lowercase(Locale.ROOT)
            if (lower.contains("key") ||
                lower.contains("auth") ||
                lower.contains("token") ||
                lower.contains("secret") ||
                lower.contains("cookie") ||
                lower.contains("credential")
            ) {
                sanitized[name] = "[REDACTED]"
            } else {
                sanitized[name] = headers.value(i)
            }
        }
        return sanitized
    }

    fun sanitizeUrl(url: String): String {
        return url.replace(Regex("key=[^&]+", RegexOption.IGNORE_CASE), "key=[REDACTED]")
    }

    fun clearHistory() {
        _sessions.value = emptyList()
        _activeSession.value = null
    }
}
