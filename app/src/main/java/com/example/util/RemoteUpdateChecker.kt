package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateManifest(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val minSupportedVersion: String,
    val title: String,
    val message: String,
    val telegramUrl: String,
    val forceUpdate: Boolean
)

object RemoteUpdateChecker {

    const val CURRENT_APP_VERSION = "1.0.0"
    val UPDATE_URLS = listOf(
        "https://raw.githubusercontent.com/yoayammmDEV/box-of-untitledAI/main/info.json",
        "https://github.com/yoayammmDEV/box-of-untitledAI/raw/main/info.json",
        "https://cdn.jsdelivr.net/gh/yoayammmDEV/box-of-untitledAI@main/info.json",
        "https://raw.githubusercontent.com/bad-kn1fe-cmd/box-of-untitledAI/main/info.json"
    )
    const val DEFAULT_TELEGRAM_URL = "https://t.me/UntitledAI"

    /**
     * Checks remote update info (info.json) asynchronously without blocking.
     * Parses { "ver": "1.0.0" } and compares with current app version.
     */
    suspend fun checkForUpdates(
        customUrl: String? = null,
        currentVersion: String = CURRENT_APP_VERSION
    ): Result<AppUpdateManifest> = withContext(Dispatchers.IO) {
        val targets = if (!customUrl.isNullOrBlank()) listOf(customUrl.trim()) else UPDATE_URLS
        var lastException: Exception? = null

        for (targetUrlStr in targets) {
            var connection: HttpURLConnection? = null
            try {
                val targetWithTimestamp = if (targetUrlStr.contains("?")) "$targetUrlStr&_t=${System.currentTimeMillis()}" else "$targetUrlStr?_t=${System.currentTimeMillis()}"
                val url = URL(targetWithTimestamp)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.setRequestProperty("User-Agent", "UntitledAI-Android/$currentVersion")
                connection.setRequestProperty("Accept", "application/json, text/plain, */*")
                connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                connection.setRequestProperty("Pragma", "no-cache")

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }.trim()
                    
                    val verMatch = Regex("\"ver\"\\s*:\\s*\"([^\"]+)\"").find(response)
                    val latestVersion = verMatch?.groupValues?.get(1)?.trim() ?: currentVersion

                    val minMatch = Regex("\"min_supported_version\"\\s*:\\s*\"([^\"]+)\"").find(response)
                    val minSupportedVersion = minMatch?.groupValues?.get(1)?.trim() ?: currentVersion

                    val titleMatch = Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(response)
                    val title = titleMatch?.groupValues?.get(1)?.trim() ?: "Доступно обновление UntitledAI"

                    val message = "Вышла новая версия UntitledAI ($latestVersion). Перейдите в Telegram-канал для загрузки обновления."
                    val tgMatch = Regex("\"(telegram_url|tg)\"\\s*:\\s*\"([^\"]+)\"").find(response)
                    val telegramUrl = tgMatch?.groupValues?.get(2)?.trim() ?: DEFAULT_TELEGRAM_URL
                    val forceUpdate = response.contains("\"force_update\": true") || response.contains("\"force_update\":true")

                    val isNewer = isVersionNewer(latestVersion, currentVersion)
                    val isBelowMin = isVersionNewer(minSupportedVersion, currentVersion)

                    val manifest = AppUpdateManifest(
                        hasUpdate = isNewer || isBelowMin,
                        latestVersion = latestVersion,
                        minSupportedVersion = minSupportedVersion,
                        title = title,
                        message = message,
                        telegramUrl = telegramUrl,
                        forceUpdate = forceUpdate || isBelowMin
                    )

                    return@withContext Result.success(manifest)
                } else {
                    lastException = Exception("HTTP $responseCode: Не удалось получить данные об обновлении")
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        Result.failure(lastException ?: Exception("Не удалось проверить наличие обновлений"))
    }

    /**
     * Simple version comparison supporting semver and alpha/beta tags.
     */
    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        if (remote.trim().equals(current.trim(), ignoreCase = true)) return false

        val cleanRemote = remote.lowercase().replace("v", "").trim()
        val cleanCurrent = current.lowercase().replace("v", "").trim()

        if (cleanRemote == cleanCurrent) return false

        return try {
            val remoteBase = cleanRemote.split("-")[0]
            val currentBase = cleanCurrent.split("-")[0]

            val remoteParts = remoteBase.split(".").mapNotNull { it.toIntOrNull() }
            val currentParts = currentBase.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }

            // Base numbers are equal (e.g. "1.0" vs "1.0-alpha")
            val isCurrentPreRelease = cleanCurrent.contains("alpha") || cleanCurrent.contains("beta") || cleanCurrent.contains("rc")
            val isRemotePreRelease = cleanRemote.contains("alpha") || cleanRemote.contains("beta") || cleanRemote.contains("rc")

            if (isCurrentPreRelease && !isRemotePreRelease) {
                return true
            }
            if (!isCurrentPreRelease && isRemotePreRelease) {
                return false
            }

            cleanRemote != cleanCurrent
        } catch (_: Exception) {
            cleanRemote != cleanCurrent
        }
    }
}
