package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class RemoteSystemPrompt(
    val version: String,
    val prompt: String
)

object SystemPromptManager {

    const val GITHUB_REPO_URL = "https://github.com/bad-kn1fe-cmd/box-of-untitledAI.git"

    val PROMPT_URLS = listOf(
        "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/systemprompt.json",
        "https://github.com/bad-kn1fe-cmd/box-of-untitledAI/raw/main/systemprompt.json",
        "https://cdn.jsdelivr.net/gh/bad-kn1fe-cmd/box-of-untitledAI@main/systemprompt.json"
    )

    /**
     * Downloads systemprompt.json from GitHub repo.
     * Parses { "ver": "...", "prompt": "..." } with fallback for relaxed JSON formats.
     */
    suspend fun fetchRemoteSystemPrompt(customUrl: String? = null): Result<RemoteSystemPrompt> = withContext(Dispatchers.IO) {
        val targets = if (!customUrl.isNullOrBlank()) listOf(customUrl.trim()) else PROMPT_URLS
        var lastException: Exception? = null

        for (targetUrl in targets) {
            var conn: HttpURLConnection? = null
            try {
                val targetWithTimestamp = if (targetUrl.contains("?")) "$targetUrl&_t=${System.currentTimeMillis()}" else "$targetUrl?_t=${System.currentTimeMillis()}"
                val url = URL(targetWithTimestamp)
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "UntitledAI-Android")
                conn.setRequestProperty("Accept", "application/json, text/plain, */*")
                conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                conn.setRequestProperty("Pragma", "no-cache")

                val code = conn.responseCode
                if (code in 200..299) {
                    val rawText = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use {
                        it.readText()
                    }.trim()

                    val parsed = parseSystemPrompt(rawText)
                    if (parsed != null && parsed.prompt.isNotBlank()) {
                        return@withContext Result.success(parsed)
                    } else {
                        lastException = Exception("Не удалось извлечь системный промпт из ответа")
                    }
                } else {
                    lastException = Exception("HTTP $code: ошибка при скачивании systemprompt.json")
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                conn?.disconnect()
            }
        }
        Result.failure(lastException ?: Exception("Не удалось загрузить системный промпт с GitHub"))
    }

    /**
     * Parses JSON string into RemoteSystemPrompt.
     * Supports standard JSON as well as relaxed/raw formats with missing commas or internal unescaped quotes.
     */
    fun parseSystemPrompt(raw: String): RemoteSystemPrompt? {
        if (raw.isBlank()) return null

        // 1. Try standard JSON parser
        try {
            val json = JSONObject(raw)
            val ver = json.optString("ver", "").trim()
            val prompt = json.optString("prompt", "").trim()
            if (prompt.isNotBlank()) {
                return RemoteSystemPrompt(version = ver.ifBlank { "1.0.0" }, prompt = prompt)
            }
        } catch (_: Exception) {
            // Fallback to relaxed parser below
        }

        // 2. Relaxed regex & substring extraction
        try {
            val verRegex = Regex("\"ver\"\\s*:\\s*\"([^\"]+)\"")
            val verMatch = verRegex.find(raw)
            val ver = verMatch?.groupValues?.get(1)?.trim() ?: "1.0.0"

            val promptRegex = Regex("\"prompt\"\\s*:\\s*\"")
            val promptMatch = promptRegex.find(raw)
            if (promptMatch != null) {
                val startIdx = promptMatch.range.last + 1
                val lastBrace = raw.lastIndexOf('}')
                val lastQuote = if (lastBrace > 0) raw.lastIndexOf('"', lastBrace) else raw.lastIndexOf('"')
                if (lastQuote > startIdx) {
                    val rawPrompt = raw.substring(startIdx, lastQuote)
                    val cleanPrompt = rawPrompt
                        .replace("\\r\\n", "\n")
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\\"", "\"")
                    return RemoteSystemPrompt(version = ver, prompt = cleanPrompt)
                }
            }
        } catch (_: Exception) {}

        return null
    }
}
