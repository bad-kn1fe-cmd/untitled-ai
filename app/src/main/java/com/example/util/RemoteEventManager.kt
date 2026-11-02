package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class RemoteEventInfo(
    val eventName: String?,
    val isHalloween: Boolean,
    val rawJson: String = ""
)

object RemoteEventManager {

    val PRIMARY_EVENT_URLS = listOf(
        "https://raw.githubusercontent.com/yoayammmDEV/box-of-untitledAI/main/info.json",
        "https://github.com/yoayammmDEV/box-of-untitledAI/raw/main/info.json",
        "https://github.com/yoayammmDEV/box-of-untitledAI/blob/main/info.json?raw=true",
        "https://cdn.jsdelivr.net/gh/yoayammmDEV/box-of-untitledAI@main/info.json"
    )

    /**
     * Checks remote info.json for active seasonal events.
     * Uses resilient regex extraction to handle JSONs with missing commas (e.g. { "ver": "..." "event": "halloween" }).
     */
    suspend fun checkEvent(customUrl: String? = null): Result<RemoteEventInfo> = withContext(Dispatchers.IO) {
        val targets = if (!customUrl.isNullOrBlank()) listOf(customUrl.trim()) else PRIMARY_EVENT_URLS
        var lastException: Exception? = null

        for (targetUrlStr in targets) {
            var connection: HttpURLConnection? = null
            try {
                val targetWithTimestamp = if (targetUrlStr.contains("?")) {
                    "$targetUrlStr&_t=${System.currentTimeMillis()}"
                } else {
                    "$targetUrlStr?_t=${System.currentTimeMillis()}"
                }
                val url = URL(targetWithTimestamp)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.setRequestProperty("User-Agent", "UntitledAI-Android/1.0")
                connection.setRequestProperty("Accept", "application/json, text/plain, */*")
                connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")

                val code = connection.responseCode
                if (code in 200..299) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val raw = reader.use { it.readText() }.trim()

                    // Resilient regex for: "event": "halloween"
                    val eventRegex = Regex("\"event\"\\s*:\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
                    val match = eventRegex.find(raw)
                    val eventVal = match?.groupValues?.get(1)?.trim()?.lowercase()

                    val isHalloween = eventVal == "halloween"

                    return@withContext Result.success(
                        RemoteEventInfo(
                            eventName = eventVal,
                            isHalloween = isHalloween,
                            rawJson = raw
                        )
                    )
                } else {
                    lastException = Exception("HTTP $code from $targetUrlStr")
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        Result.failure(lastException ?: Exception("Не удалось проверить событие в info.json"))
    }
}
