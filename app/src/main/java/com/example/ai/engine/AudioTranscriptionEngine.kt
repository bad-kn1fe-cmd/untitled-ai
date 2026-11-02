package com.example.ai.engine

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AiProviderProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * High-accuracy audio speech-to-text transcription engine.
 * Transcribes recorded voice messages into clean text using AI providers
 * (Gemini multimodal audio, Groq Whisper, OpenAI Whisper, OpenRouter).
 */
object AudioTranscriptionEngine {

    private const val TAG = "AudioTranscriptionEngine"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Transcribes an audio file into spoken text.
     * Returns the cleaned transcribed text, or null if transcription could not be completed.
     */
    suspend fun transcribeAudioFile(
        context: Context,
        audioFile: File,
        preferredProfileId: Long? = null
    ): String? = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() <= 0L) {
            Log.w(TAG, "Audio file does not exist or is empty")
            return@withContext null
        }

        try {
            val db = AppDatabase.getInstance(context)
            val allProfiles = db.aiProviderDao().getAllProfilesList()

            // 1. Resolve preferred profile
            val preferredProfile = preferredProfileId?.let { db.aiProviderDao().getProfileById(it) }
            val defaultText = db.aiProviderDao().getDefaultTextProfile()

            // 2. Build candidate list prioritizing audio-capable providers
            val candidates = mutableListOf<AiProviderProfileEntity>()

            // Gemini profiles with API key (fastest & native multimodal audio support)
            val geminiProfiles = allProfiles.filter { 
                it.providerType.equals("GEMINI", ignoreCase = true) && it.apiKey.isNotBlank() 
            }
            // Groq or OpenAI profiles with API key (Whisper support)
            val whisperProfiles = allProfiles.filter { 
                (it.providerType.equals("GROQ", ignoreCase = true) || it.providerType.equals("OPENAI", ignoreCase = true)) && it.apiKey.isNotBlank() 
            }
            // OpenRouter with API key
            val openRouterProfiles = allProfiles.filter { 
                it.providerType.equals("OPENROUTER", ignoreCase = true) && it.apiKey.isNotBlank() 
            }

            if (preferredProfile != null && preferredProfile.apiKey.isNotBlank()) {
                candidates.add(preferredProfile)
            }
            candidates.addAll(geminiProfiles)
            candidates.addAll(whisperProfiles)
            if (defaultText != null && defaultText.apiKey.isNotBlank()) {
                candidates.add(defaultText)
            }
            candidates.addAll(openRouterProfiles)

            val distinctCandidates = candidates.distinctBy { it.id }

            for (profile in distinctCandidates) {
                try {
                    val result = when (profile.providerType.uppercase()) {
                        "GEMINI" -> transcribeWithGemini(audioFile, profile)
                        "GROQ", "OPENAI" -> transcribeWithWhisper(audioFile, profile)
                        "OPENROUTER" -> transcribeWithOpenRouter(audioFile, profile)
                        else -> null
                    }
                    if (!result.isNullOrBlank()) {
                        val cleaned = cleanTranscription(result)
                        if (cleaned.isNotBlank()) {
                            Log.d(TAG, "Transcribed via ${profile.name}: \"$cleaned\"")
                            return@withContext cleaned
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed transcribing with profile: ${profile.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in transcribeAudioFile", e)
        }

        null
    }

    private fun transcribeWithGemini(audioFile: File, profile: AiProviderProfileEntity): String? {
        val model = if (profile.model.contains("gemini")) profile.model else "gemini-3.5-flash-lite"
        val baseUrl = profile.baseUrl.ifBlank { "https://generativelanguage.googleapis.com" }.trimEnd('/')
        val url = "$baseUrl/v1beta/models/$model:generateContent?key=${profile.apiKey.trim()}"

        val bytes = audioFile.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val contentsJson = JSONArray()
        val parts = JSONArray()

        val inlineData = JSONObject()
            .put("mimeType", "audio/mp4")
            .put("data", base64)
        parts.put(JSONObject().put("inlineData", inlineData))

        val promptText = "Транскрибируй аудиозапись пользователя дословно на языке оригинала без добавления лишних слов, без кавычек, пояснений и метаданных. Выведи только произнесённые слова:"
        parts.put(JSONObject().put("text", promptText))

        val contentObj = JSONObject().put("role", "user").put("parts", parts)
        contentsJson.put(contentObj)

        val bodyJson = JSONObject()
            .put("contents", contentsJson)
            .put("generationConfig", JSONObject()
                .put("temperature", 0.0)
                .put("maxOutputTokens", 1024)
            )

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val err = response.body?.string() ?: ""
            Log.w(TAG, "Gemini transcription error ${response.code}: $err")
            return null
        }

        val respStr = response.body?.string() ?: return null
        val root = JSONObject(respStr)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return null
        val resParts = content.optJSONArray("parts") ?: return null
        val sb = StringBuilder()
        for (i in 0 until resParts.length()) {
            val part = resParts.getJSONObject(i)
            sb.append(part.optString("text", ""))
        }
        return sb.toString().trim().ifBlank { null }
    }

    private fun transcribeWithWhisper(audioFile: File, profile: AiProviderProfileEntity): String? {
        val endpoint = if (profile.providerType.equals("GROQ", ignoreCase = true)) {
            "https://api.groq.com/openai/v1/audio/transcriptions"
        } else {
            val base = profile.baseUrl.ifBlank { "https://api.openai.com/v1" }.trimEnd('/')
            if (base.endsWith("/audio/transcriptions")) base else "$base/audio/transcriptions"
        }

        val whisperModel = if (profile.providerType.equals("GROQ", ignoreCase = true)) "whisper-large-v3" else "whisper-1"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/m4a".toMediaType()))
            .addFormDataPart("model", whisperModel)
            .addFormDataPart("response_format", "json")
            .build()

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer ${profile.apiKey.trim()}")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val err = response.body?.string() ?: ""
            Log.w(TAG, "Whisper transcription error ${response.code}: $err")
            return null
        }

        val respStr = response.body?.string() ?: return null
        val json = JSONObject(respStr)
        return json.optString("text", "").trim().ifBlank { null }
    }

    private fun transcribeWithOpenRouter(audioFile: File, profile: AiProviderProfileEntity): String? {
        val url = "https://openrouter.ai/api/v1/chat/completions"
        val bytes = audioFile.readBytes()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        val messages = JSONArray()
        val userMsg = JSONObject().put("role", "user")
        val contentParts = JSONArray()
        contentParts.put(
            JSONObject()
                .put("type", "text")
                .put("text", "Транскрибируй аудиозапись дословно на языке оригинала без добавления лишних слов, без кавычек и метаданных. Только произнесённый текст:")
        )

        val audioObj = JSONObject().put("type", "input_audio")
        val audioData = JSONObject().put("data", base64).put("format", "m4a")
        audioObj.put("input_audio", audioData)
        contentParts.put(audioObj)

        userMsg.put("content", contentParts)
        messages.put(userMsg)

        val body = JSONObject()
            .put("model", "google/gemini-2.5-flash")
            .put("messages", messages)
            .put("temperature", 0.0)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${profile.apiKey.trim()}")
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return null
        val respStr = response.body?.string() ?: return null
        val json = JSONObject(respStr)
        val choices = json.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        val choice = choices.getJSONObject(0).optJSONObject("message") ?: return null
        return choice.optString("content", "").trim().ifBlank { null }
    }

    private fun cleanTranscription(raw: String): String {
        return raw.trim()
            .removeSurrounding("\"")
            .removeSurrounding("«", "»")
            .removePrefix("Транскрипция:")
            .removePrefix("Текст:")
            .removePrefix("Transcript:")
            .trim()
    }
}
