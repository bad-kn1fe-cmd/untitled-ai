package com.example.ai.rag

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class RemoteEmbeddingProvider(
    private val apiKey: String,
    private val baseUrl: String,
    private val model: String? = null,
    private val providerType: String? = null
) : EmbeddingProvider {

    override suspend fun getEmbedding(text: String): FloatArray? = withContext(Dispatchers.IO) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return@withContext null

        val cleanKey = apiKey.trim()
        val cleanBase = baseUrl.trim().trimEnd('/')

        // Если ключа нет или провайдер локальный, сразу используем быстрый локальный векторизатор
        if (cleanKey.isBlank() && providerType != "CUSTOM") {
            return@withContext LocalEmbeddingGenerator.generateVector(cleanText)
        }

        try {
            val isGoogle = providerType?.equals("GEMINI", ignoreCase = true) == true ||
                    cleanBase.isEmpty() ||
                    cleanBase.contains("googleapis") ||
                    cleanBase.contains("generativelanguage")
            val isOpenRouter = providerType?.equals("OPENROUTER", ignoreCase = true) == true ||
                    cleanBase.contains("openrouter.ai")

            if (isGoogle) {
                // Попытка получить вектор от Google Gemini API (пробуем актуальную gemini-embedding-001, затем text-embedding-004)
                val preferredModel = model?.takeIf { it.isNotBlank() } ?: "gemini-embedding-001"
                val googleEmb = requestGoogleEmbedding(cleanText, cleanKey, preferredModel)
                    ?: if (preferredModel == "gemini-embedding-001") requestGoogleEmbedding(cleanText, cleanKey, "text-embedding-004") else null

                if (googleEmb != null) {
                    return@withContext googleEmb
                }
            } else {
                val targetModel = model?.takeIf { it.isNotBlank() } ?: when {
                    isOpenRouter -> "openai/text-embedding-3-small"
                    else -> "text-embedding-3-small"
                }

                val endpoint = when {
                    isOpenRouter -> "https://openrouter.ai/api/v1/embeddings"
                    cleanBase.endsWith("/v1") -> "$cleanBase/embeddings"
                    else -> "$cleanBase/v1/embeddings"
                }

                val openAiEmb = requestOpenAiEmbedding(cleanText, cleanKey, endpoint, targetModel, isOpenRouter)
                if (openAiEmb != null) {
                    return@withContext openAiEmb
                }
            }

            // Fallback: если удаленный API недоступен, возвращает 402/404 или нет embeddings endpoint
            Log.d("RAG_EMBED", "Remote embedding API unavailable or failed, falling back to local semantic vectorizer.")
            LocalEmbeddingGenerator.generateVector(cleanText)
        } catch (e: Exception) {
            Log.e("RAG_EMBED", "Remote embedding error: ${e.message}, using local fallback.", e)
            LocalEmbeddingGenerator.generateVector(cleanText)
        }
    }

    private fun requestGoogleEmbedding(text: String, apiKey: String, modelName: String): FloatArray? {
        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:embedContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) Untitled/1.0")
            conn.connectTimeout = 8000
            conn.readTimeout = 10000
            conn.doOutput = true

            val jsonInput = JSONObject().apply {
                put("model", "models/$modelName")
                put("content", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                })
            }

            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(jsonInput.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val responseStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val jsonResponse = JSONObject(responseStr)
                val embeddingObj = jsonResponse.optJSONObject("embedding")
                val valuesArray = embeddingObj?.optJSONArray("values")
                if (valuesArray != null) {
                    val floatArray = FloatArray(valuesArray.length())
                    for (i in 0 until valuesArray.length()) {
                        floatArray[i] = valuesArray.getDouble(i).toFloat()
                    }
                    return floatArray
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w("RAG_EMBED", "Google embedding ($modelName) failed with code $code: $err")
            }
            null
        } catch (e: Exception) {
            Log.w("RAG_EMBED", "Google embedding ($modelName) exception: ${e.message}")
            null
        }
    }

    private fun requestOpenAiEmbedding(
        text: String,
        apiKey: String,
        endpoint: String,
        modelName: String,
        isOpenRouter: Boolean
    ): FloatArray? {
        return try {
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) Untitled/1.0")
            if (isOpenRouter) {
                conn.setRequestProperty("HTTP-Referer", "https://github.com/badknife/untitled")
                conn.setRequestProperty("X-Title", "Untitled")
            }
            conn.connectTimeout = 8000
            conn.readTimeout = 10000
            conn.doOutput = true

            val jsonInput = JSONObject().apply {
                put("input", text)
                put("model", modelName)
            }

            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(jsonInput.toString())
                writer.flush()
            }

            val code = conn.responseCode
            if (code in 200..299) {
                val responseStr = conn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val jsonResponse = JSONObject(responseStr)
                val dataArray = jsonResponse.optJSONArray("data")
                if (dataArray != null && dataArray.length() > 0) {
                    val embeddingArray = dataArray.getJSONObject(0).optJSONArray("embedding")
                    if (embeddingArray != null) {
                        val floatArray = FloatArray(embeddingArray.length())
                        for (i in 0 until embeddingArray.length()) {
                            floatArray[i] = embeddingArray.getDouble(i).toFloat()
                        }
                        return floatArray
                    }
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.w("RAG_EMBED", "OpenAI/OpenRouter embedding failed with code $code: $err")
            }
            null
        } catch (e: Exception) {
            Log.w("RAG_EMBED", "OpenAI embedding exception: ${e.message}")
            null
        }
    }
}
