package com.example.ai.engine

import com.example.data.local.entity.AiProviderProfileEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.model.CivitaiModelItem
import com.example.data.model.CivitaiVersionItem
import com.example.ai.debug.GeminiDebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AiProviderEngine {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val reqBuilder = original.newBuilder()
            if (original.header("User-Agent") == null) {
                reqBuilder.header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                )
            }
            if (original.header("Accept") == null) {
                reqBuilder.header("Accept", "application/json, text/event-stream, */*")
            }
            chain.proceed(reqBuilder.build())
        }
        .build()

    private fun appendReactionFeedback(text: String, reaction: String?): String {
        if (reaction.isNullOrBlank()) return text
        val signal = com.example.util.MessageReactionHelper.getReactionSignal(reaction) ?: return text
        return "$text\n\n[Сигнал обратной связи от пользователя: $reaction $signal]"
    }

    suspend fun generateChatCompletion(
        profile: AiProviderProfileEntity,
        systemPrompt: String,
        recentMessages: List<MessageEntity>,
        openRouterFallbackModels: String = "",
        geminiSafetyThreshold: String = "BLOCK_NONE",
        onTokenStream: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (profile.apiKey.isBlank() && profile.providerType != "CUSTOM") {
                return@withContext Result.failure(
                    IllegalStateException("API-ключ для провайдера \"${profile.name}\" не заполнен. Перейдите в Настройки -> AI-провайдеры и введите ваш ключ.")
                )
            }

            when (profile.providerType.uppercase()) {
                "OPENROUTER" -> callOpenRouterOrOpenAi(profile, systemPrompt, recentMessages, isRouters = true, openRouterFallbackModels = openRouterFallbackModels, onTokenStream = onTokenStream)
                "ANTHROPIC" -> callAnthropic(profile, systemPrompt, recentMessages, onTokenStream = onTokenStream)
                "GEMINI" -> callGemini(profile, systemPrompt, recentMessages, geminiSafetyThreshold = geminiSafetyThreshold, onTokenStream = onTokenStream)
                "CEREBRAS", "GROK", "XAI", "GROQ", "SAMBANOVA", "DEEPSEEK", "TOGETHER", "MISTRAL", "FIREWORKS", "PERPLEXITY", "OPENAI", "CUSTOM" ->
                    callOpenRouterOrOpenAi(profile, systemPrompt, recentMessages, isRouters = false, openRouterFallbackModels = openRouterFallbackModels, onTokenStream = onTokenStream)
                else -> callOpenRouterOrOpenAi(profile, systemPrompt, recentMessages, isRouters = false, openRouterFallbackModels = openRouterFallbackModels, onTokenStream = onTokenStream)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val DEFAULT_OPENROUTER_FALLBACK_MODELS = listOf(
        "google/gemma-4-26b-a4b-it:free",
        "google/gemma-4-31b-it:free",
        "z-ai/glm-5.2:free",
        "openai/gpt-oss-2",
        "openai/gpt-oss-20b:free"
    )

    private suspend fun callOpenRouterOrOpenAi(
        profile: AiProviderProfileEntity,
        systemPrompt: String,
        messages: List<MessageEntity>,
        isRouters: Boolean,
        openRouterFallbackModels: String = "",
        onTokenStream: ((String) -> Unit)? = null
    ): Result<String> {
        val isCerebras = profile.providerType.equals("CEREBRAS", ignoreCase = true)
        val isGrokOrXai = profile.providerType.equals("GROK", ignoreCase = true) ||
                profile.providerType.equals("XAI", ignoreCase = true) ||
                profile.apiKey.trim().startsWith("xai-") ||
                profile.model.lowercase().startsWith("grok") ||
                profile.name.lowercase().contains("grok") ||
                profile.baseUrl.contains("api.x.ai")

        var rawBaseUrl = when {
            profile.baseUrl.isNotBlank() -> profile.baseUrl.trim().trimEnd('/')
            isRouters -> "https://openrouter.ai/api/v1"
            isCerebras -> "https://api.cerebras.ai/v1"
            isGrokOrXai -> "https://api.x.ai/v1"
            else -> getDefaultBaseUrl(profile.providerType)
        }

        // Smart correction: If user configured Grok key or model, but baseUrl points to Groq or OpenAI
        if (isGrokOrXai && (rawBaseUrl.contains("api.groq.com") || rawBaseUrl.contains("api.openai.com") || rawBaseUrl.isBlank())) {
            rawBaseUrl = "https://api.x.ai/v1"
        }

        // Ensure api.x.ai has /v1 if omitted
        if (rawBaseUrl.contains("api.x.ai") && !rawBaseUrl.contains("/v1")) {
            rawBaseUrl = "$rawBaseUrl/v1"
        }

        val baseUrl = rawBaseUrl.trimEnd('/')
        val url = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

        val messagesJson = JSONArray()
        val hasImageInMessages = messages.any { !it.imageUrl.isNullOrBlank() }

        // System prompt
        val sysObj = JSONObject()
        sysObj.put("role", "system")
        sysObj.put("content", systemPrompt)
        messagesJson.put(sysObj)

        // Conversation history
        for (msg in messages) {
            val role = if (msg.sender == "user") "user" else if (msg.sender == "character") "assistant" else "user"
            val msgObj = JSONObject()
            msgObj.put("role", role)

            if (!msg.imageUrl.isNullOrBlank()) {
                val mediaItems = com.example.util.ChatMediaHelper.deserialize(msg.imageUrl)
                val contentParts = JSONArray()

                val photosCount = mediaItems.count { it is com.example.util.ChatMediaItem.Photo }
                val videosCount = mediaItems.count { it is com.example.util.ChatMediaItem.Video }

                val userPrompt = if (msg.content.isNotBlank()) msg.content else if (videosCount > 0) "*показывает видео*" else "*показывает фото*"

                val descriptiveText = when {
                    mediaItems.size == 1 && mediaItems.first() is com.example.util.ChatMediaItem.Video -> {
                        val video = mediaItems.first() as com.example.util.ChatMediaItem.Video
                        val durationText = if (video.durationSec > 0) " (${video.durationSec} сек)" else ""
                        "[🎬 ВИДЕОЗАПИСЬ$durationText: Пользователь отправил видеоролик. Ниже представлены ${video.frameBase64List.size} последовательных хронологических кадров этого видео (от начала до конца сцены). ВНИМАНИЕ: Это единая непрерывная видеозапись, а НЕ отдельные разрозненные картинки. Воспринимай, анализируй и комментируй происходящее именно как ВИДЕО — оценивай сюжет, действия, движения и динамику]\n\n$userPrompt"
                    }
                    mediaItems.size == 1 && mediaItems.first() is com.example.util.ChatMediaItem.Photo -> {
                        if (msg.content.isNotBlank()) msg.content else "Посмотри на это фото:"
                    }
                    else -> {
                        "[📎 МЕДИАВЛОЖЕНИЯ (${mediaItems.size} шт): Прикреплено $photosCount фото и $videosCount видео. Видеокадры представляют собой последовательную раскадровку видеороликов во времени. Воспринимай видеозаписи как целостные динамичные сюжеты, а фото — как статичные изображения]\n\n$userPrompt"
                    }
                }

                val textPart = JSONObject()
                textPart.put("type", "text")
                textPart.put("text", descriptiveText)
                contentParts.put(textPart)

                for (item in mediaItems) {
                    when (item) {
                        is com.example.util.ChatMediaItem.Photo -> {
                            val imagePart = JSONObject()
                            imagePart.put("type", "image_url")
                            val imageObj = JSONObject()
                            imageObj.put("url", item.base64Data)
                            imagePart.put("image_url", imageObj)
                            contentParts.put(imagePart)
                        }
                        is com.example.util.ChatMediaItem.Video -> {
                            for (frame in item.frameBase64List) {
                                val imagePart = JSONObject()
                                imagePart.put("type", "image_url")
                                val imageObj = JSONObject()
                                imageObj.put("url", frame)
                                imagePart.put("image_url", imageObj)
                                contentParts.put(imagePart)
                            }
                        }
                    }
                }

                msgObj.put("content", contentParts)
            } else if (!msg.audioFilePath.isNullOrBlank()) {
                val spoken = if (msg.content.isNotBlank() && !msg.content.startsWith("*отправил голосовое")) {
                    msg.content
                } else ""
                val promptText = if (spoken.isNotBlank()) spoken else "*отправил голосовое сообщение*"
                msgObj.put("content", appendReactionFeedback(promptText, msg.reaction))
            } else {
                msgObj.put("content", appendReactionFeedback(msg.content, msg.reaction))
            }
            messagesJson.put(msgObj)
        }

        val primaryModel = if (hasImageInMessages && isRouters && profile.visionModel.isNotBlank()) {
            profile.visionModel
        } else if (hasImageInMessages && !isRouters && profile.visionModel.isNotBlank()) {
            profile.visionModel
        } else {
            profile.model.ifBlank {
                when {
                    isCerebras -> "gpt-oss-120b"
                    isRouters -> "google/gemma-4-26b-a4b-it:free"
                    isGrokOrXai -> "grok-2-latest"
                    else -> "gpt-4o-mini"
                }
            }
        }

        // List of models to try in case of 429
        val modelsToTry = if (isRouters) {
            val list = mutableListOf(primaryModel)
            val configuredFallbacks = if (openRouterFallbackModels.isNotBlank()) {
                openRouterFallbackModels.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                DEFAULT_OPENROUTER_FALLBACK_MODELS
            }
            for (m in configuredFallbacks) {
                if (!list.contains(m)) list.add(m)
            }
            list
        } else {
            listOf(primaryModel)
        }

        var lastErrorMsg = ""
        val maxCycles = if (isRouters) 4 else 1

        for (cycle in 0 until maxCycles) {
            for (modelToUse in modelsToTry) {
                try {
                    val bodyJson = JSONObject()
                    bodyJson.put("model", modelToUse)
                    bodyJson.put("messages", messagesJson)
                    bodyJson.put("temperature", profile.temperature.toDouble())
                    bodyJson.put("top_p", profile.topP.toDouble())
                    bodyJson.put("max_tokens", profile.maxTokens)
                    if (onTokenStream != null) {
                        bodyJson.put("stream", true)
                    }
                    val lastUserMsg = messages.lastOrNull { it.sender == "user" }?.content ?: ""
                    applyOpenAiModelCustomizations(
                        bodyJson = bodyJson,
                        profile = profile,
                        modelName = modelToUse,
                        systemPrompt = systemPrompt,
                        userPrompt = lastUserMsg,
                        isStream = onTokenStream != null
                    )

                    val requestBuilder = Request.Builder()
                        .url(url)
                        .post(bodyJson.toString().toRequestBody(jsonMediaType))

                    val cleanKey = profile.apiKey.trim().removePrefix("Bearer ").removePrefix("bearer ").trim()
                    if (cleanKey.isNotBlank()) {
                        requestBuilder.header("Authorization", "Bearer $cleanKey")
                    }
                    requestBuilder.header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                    requestBuilder.header("Accept", "application/json, text/event-stream, */*")
                    if (isRouters) {
                        requestBuilder.header("HTTP-Referer", "https://ai.studio")
                        requestBuilder.header("X-Title", "AI Roleplay Engine Android")
                    }
                    applyExtraHeaders(requestBuilder, profile.extraHeadersJson)

                    val response = httpClient.newCall(requestBuilder.build()).execute()

                    if (!response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        val parsedMessage = try {
                            val errorJson = JSONObject(responseBody)
                            val errorObj = errorJson.optJSONObject("error")
                            val type = errorObj?.optString("type") ?: ""
                            val code = errorObj?.optString("code") ?: ""
                            val rawMsg = errorObj?.optString("message") ?: responseBody
                            if (code == "insufficient_quota" || rawMsg.contains("quota", ignoreCase = true)) {
                                "Закончился баланс на аккаунте OpenAI API (Insufficient Quota). Внимание: подписка ChatGPT Plus не действует на API — требуется пополнить баланс на platform.openai.com/settings/organization/billing"
                            } else if (response.code == 429 || type.contains("rate_limit", ignoreCase = true)) {
                                "Превышен лимит запросов (Rate Limit 429) для $modelToUse: $rawMsg"
                            } else {
                                rawMsg
                            }
                        } catch (_: Exception) {
                            responseBody.ifBlank { "HTTP ${response.code} ${response.message}" }
                        }

                        if (response.code == 401 || response.code == 403) {
                            val isXaiEndpoint = url.contains("api.x.ai") || isGrokOrXai
                            val isGroqEndpoint = url.contains("api.groq.com") || profile.providerType.equals("GROQ", ignoreCase = true)

                            val detailedMsg = when {
                                isXaiEndpoint && (responseBody.contains("cloudflare", ignoreCase = true) || responseBody.contains("<html", ignoreCase = true)) -> {
                                    "Ошибка [403 Cloudflare]: Доступ к серверу xAI временно заблокирован защитным экраном Cloudflare. Попробуйте сменить сеть или использовать другое интернет-подключение/VPN."
                                }
                                isXaiEndpoint -> {
                                    "Ошибка Grok API [403 Forbidden]: $parsedMessage.\n\n" +
                                    "💡 Возможные причины 403 Forbidden от xAI (Grok):\n" +
                                    "1. Региональные ограничения: API xAI доступен не во всех странах. Используйте IP/VPN поддерживаемого региона.\n" +
                                    "2. Доступ к модели ($modelToUse): Убедитесь, что выбранная модель доступна для вашего ключа в console.x.ai.\n" +
                                    "3. Бесплатный лимит / Баланс: У xAI есть бесплатные лимиты, но они могут исчерпаться, либо требуется подтверждение аккаунта в console.x.ai -> Usage / Billing.\n" +
                                    "4. Разрешения API-ключа: Проверьте в console.x.ai, что ключ активен и имеет права на chat completions.\n" +
                                    "5. Подписка X (Twitter) Premium: Подписка в соцсети Twitter не включает доступ к API разработчиков xAI."
                                }
                                isGroqEndpoint && (profile.apiKey.trim().startsWith("xai-") || modelToUse.lowercase().startsWith("grok")) -> {
                                    "Ошибка Groq [403]: Вы используете ключ или модель Grok (xAI) с сервером Groq Cloud (api.groq.com). Groq и xAI Grok — это совершенно разные сервисы! Выберите тип провайдера «xAI Grok» с адресом https://api.x.ai/v1."
                                }
                                else -> {
                                    "Ошибка авторизации [${response.code}]: $parsedMessage. Проверьте API-ключ провайдера ${profile.name} в Настройках -> AI-провайдеры."
                                }
                            }
                            return Result.failure(IOException(detailedMsg))
                        }

                        if (isRouters && (response.code == 429 || responseBody.contains("rate limit", ignoreCase = true) || responseBody.contains("free-models-per-day", ignoreCase = true) || responseBody.contains("quota", ignoreCase = true))) {
                            lastErrorMsg = "Лимит модели $modelToUse (429). Пробуем следующую модель из списка..."
                            delay(600)
                            continue
                        }

                        lastErrorMsg = "Ошибка API [${response.code}]: $parsedMessage"
                        if (response.code in 500..599) {
                            delay(800)
                            continue
                        }
                        return Result.failure(IOException(lastErrorMsg))
                    }

                    if (onTokenStream != null) {
                        val streamedText = StringBuilder()
                        val responseBody = response.body ?: return Result.failure(IOException("Пустой ответ от сервера"))
                        val source = responseBody.source()

                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            val trimmed = line.trim()
                            if (trimmed.startsWith("data:")) {
                                val dataContent = trimmed.removePrefix("data:").trim()
                                if (dataContent == "[DONE]") break
                                if (dataContent.isEmpty()) continue
                                try {
                                    val chunk = JSONObject(dataContent)
                                    val choices = chunk.optJSONArray("choices")
                                    if (choices != null && choices.length() > 0) {
                                        val first = choices.getJSONObject(0)
                                        val delta = first.optJSONObject("delta")
                                        val piece = delta?.optString("content")?.takeIf { it.isNotEmpty() }
                                            ?: delta?.optString("text")?.takeIf { it.isNotEmpty() }
                                            ?: (if (profile.thinkingMode != "disabled") delta?.optString("reasoning_content")?.takeIf { it.isNotEmpty() } else null)
                                            ?: first.optString("text", "")
                                        if (piece.isNotEmpty()) {
                                            streamedText.append(piece)
                                            withContext(Dispatchers.Main) {
                                                onTokenStream(piece)
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }

                        if (streamedText.isNotEmpty()) {
                            return Result.success(stripThinkingIfDisabled(streamedText.toString(), profile))
                        }
                    }

                    val responseBody = response.body?.string() ?: ""
                    val parsed = JSONObject(responseBody)
                    val choices = parsed.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val messageObj = firstChoice.getJSONObject("message")
                        val content = messageObj.optString("content", "")
                        if (content.isNotBlank()) {
                            return Result.success(stripThinkingIfDisabled(content, profile))
                        }
                    }
                } catch (e: Exception) {
                    lastErrorMsg = e.message ?: "Ошибка сети"
                    delay(1000)
                }
            }
        }

        return Result.failure(IOException(if (lastErrorMsg.isNotBlank()) lastErrorMsg else "Все модели из списка вернули ошибку лимита 429"))
    }

    private suspend fun callAnthropic(
        profile: AiProviderProfileEntity,
        systemPrompt: String,
        messages: List<MessageEntity>,
        onTokenStream: ((String) -> Unit)? = null
    ): Result<String> {
        val baseUrl = profile.baseUrl.ifBlank { "https://api.anthropic.com/v1" }.trimEnd('/')
        val url = if (baseUrl.endsWith("/messages")) baseUrl else "$baseUrl/messages"

        val messagesJson = JSONArray()
        for (msg in messages) {
            val role = if (msg.sender == "user") "user" else "assistant"
            val obj = JSONObject()
            obj.put("role", role)
            val contentText = if (!msg.audioFilePath.isNullOrBlank()) {
                val spoken = if (msg.content.isNotBlank() && !msg.content.startsWith("*отправил голосовое")) msg.content else ""
                val p = if (spoken.isNotBlank()) spoken else "*отправил голосовое сообщение*"
                appendReactionFeedback(p, msg.reaction)
            } else {
                appendReactionFeedback(msg.content, msg.reaction)
            }
            obj.put("content", contentText)
            messagesJson.put(obj)
        }

        if (messagesJson.length() == 0) {
            val obj = JSONObject()
            obj.put("role", "user")
            obj.put("content", "Привет!")
            messagesJson.put(obj)
        }

        val bodyJson = JSONObject()
        bodyJson.put("model", profile.model.ifBlank { "claude-3-5-haiku-20241022" })
        bodyJson.put("system", systemPrompt)
        bodyJson.put("messages", messagesJson)
        bodyJson.put("temperature", profile.temperature.toDouble())
        bodyJson.put("max_tokens", profile.maxTokens)
        if (onTokenStream != null) {
            bodyJson.put("stream", true)
        }
        val lastUserMsg = messages.lastOrNull { it.sender == "user" }?.content ?: ""
        applyAnthropicModelCustomizations(bodyJson, profile, systemPrompt, lastUserMsg)

        val requestBuilder = Request.Builder()
            .url(url)
            .header("x-api-key", profile.apiKey.trim())
            .header("anthropic-version", "2023-06-01")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
        applyExtraHeaders(requestBuilder, profile.extraHeadersJson)
        val request = requestBuilder.build()

        var lastError: Exception? = null
        for (attempt in 1..3) {
            try {
                val response = httpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    if (response.code == 429 || response.code in 500..599) {
                        delay(1200L * attempt)
                        continue
                    }
                    return Result.failure(IOException("Ошибка Anthropic [${response.code}]: $responseBody"))
                }

                if (onTokenStream != null) {
                    val streamedText = StringBuilder()
                    val source = response.body?.source()
                    if (source != null) {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            val trimmed = line.trim()
                            if (trimmed.startsWith("data:")) {
                                val dataContent = trimmed.removePrefix("data:").trim()
                                try {
                                    val chunk = JSONObject(dataContent)
                                    if (chunk.optString("type") == "content_block_delta") {
                                        val delta = chunk.optJSONObject("delta")
                                        val text = delta?.optString("text") ?: ""
                                        if (text.isNotEmpty()) {
                                            streamedText.append(text)
                                            withContext(Dispatchers.Main) {
                                                onTokenStream(text)
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                    if (streamedText.isNotEmpty()) {
                        return Result.success(streamedText.toString())
                    }
                }

                val responseBody = response.body?.string() ?: ""
                val parsed = JSONObject(responseBody)
                val contentArray = parsed.optJSONArray("content")
                if (contentArray != null && contentArray.length() > 0) {
                    val first = contentArray.getJSONObject(0)
                    return Result.success(first.optString("text", ""))
                }
                return Result.failure(IOException("Пустой ответ от Claude"))
            } catch (e: Exception) {
                lastError = e
                delay(1000L * attempt)
            }
        }

        return Result.failure(lastError ?: IOException("Не удалось получить ответ от Anthropic"))
    }

    private suspend fun callGemini(
        profile: AiProviderProfileEntity,
        systemPrompt: String,
        messages: List<MessageEntity>,
        geminiSafetyThreshold: String = "BLOCK_NONE",
        onTokenStream: ((String) -> Unit)? = null
    ): Result<String> {
        val model = profile.model.ifBlank { "gemini-3.5-flash-lite" }
        val baseUrl = profile.baseUrl.ifBlank { "https://generativelanguage.googleapis.com" }.trimEnd('/')
        val endpoint = if (onTokenStream != null) "streamGenerateContent?alt=sse&" else "generateContent?"
        val url = "$baseUrl/v1beta/models/$model:$endpoint" + "key=${profile.apiKey.trim()}"

        val contentsJson = JSONArray()

        // Sanitize messages for Gemini strict role alternation
        val rawList = mutableListOf<Pair<String, MessageEntity>>()
        for (msg in messages) {
            val role = if (msg.sender == "user") "user" else "model"
            rawList.add(Pair(role, msg))
        }

        val contentsList = mutableListOf<Pair<String, MessageEntity>>()
        for ((role, msg) in rawList) {
            if (contentsList.isEmpty()) {
                if (role == "model") {
                    val dummyUser = MessageEntity(chatId = msg.chatId, sender = "user", content = "Привет", timestamp = msg.timestamp - 1)
                    contentsList.add(Pair("user", dummyUser))
                    contentsList.add(Pair("model", msg))
                } else {
                    contentsList.add(Pair("user", msg))
                }
            } else {
                val lastRole = contentsList.last().first
                if (role == lastRole) {
                    if (role == "user") {
                        val dummyModel = MessageEntity(chatId = msg.chatId, sender = "character", content = "...", timestamp = msg.timestamp - 1)
                        contentsList.add(Pair("model", dummyModel))
                        contentsList.add(Pair("user", msg))
                    } else {
                        val dummyUser = MessageEntity(chatId = msg.chatId, sender = "user", content = "...", timestamp = msg.timestamp - 1)
                        contentsList.add(Pair("user", dummyUser))
                        contentsList.add(Pair("model", msg))
                    }
                } else {
                    contentsList.add(Pair(role, msg))
                }
            }
        }

        if (contentsList.isNotEmpty() && contentsList.last().first == "model") {
            val lastMsg = contentsList.last().second
            val dummyUser = MessageEntity(chatId = lastMsg.chatId, sender = "user", content = "Продолжай", timestamp = lastMsg.timestamp + 1)
            contentsList.add(Pair("user", dummyUser))
        }

        for ((role, msg) in contentsList) {
            val parts = JSONArray()
            val mediaItems = if (!msg.imageUrl.isNullOrBlank()) {
                com.example.util.ChatMediaHelper.deserialize(msg.imageUrl)
            } else {
                emptyList()
            }

            val photosCount = mediaItems.count { it is com.example.util.ChatMediaItem.Photo }
            val videosCount = mediaItems.count { it is com.example.util.ChatMediaItem.Video }
            val spokenAudio = if (!msg.audioFilePath.isNullOrBlank() && msg.content.isNotBlank() && !msg.content.startsWith("*отправил голосовое")) {
                msg.content
            } else ""
            val userPrompt = when {
                spokenAudio.isNotBlank() -> spokenAudio
                !msg.audioFilePath.isNullOrBlank() -> "*отправил голосовое сообщение*"
                msg.content.isNotBlank() -> msg.content
                videosCount > 0 -> "*показывает видео*"
                photosCount > 0 -> "*показывает фото*"
                else -> "..."
            }

            val textContent = when {
                mediaItems.size == 1 && mediaItems.first() is com.example.util.ChatMediaItem.Video -> {
                    val video = mediaItems.first() as com.example.util.ChatMediaItem.Video
                    val durationText = if (video.durationSec > 0) " (${video.durationSec} сек)" else ""
                    "[🎬 ВИДЕОЗАПИСЬ$durationText: Пользователь отправил видеоролик. Ниже представлены ${video.frameBase64List.size} последовательных хронологических кадров этого видео (от начала до конца сцены). ВНИМАНИЕ: Это единая непрерывная видеозапись, а НЕ отдельные разрозненные картинки. Воспринимай, анализируй и комментируй происходящее именно как ВИДЕО — оценивай сюжет, действия, движения и динамику]\n\n$userPrompt"
                }
                mediaItems.size > 1 -> {
                    "[📎 МЕДИАВЛОЖЕНИЯ (${mediaItems.size} шт): Прикреплено $photosCount фото и $videosCount видео. Видеокадры представляют собой последовательную раскадровку видеороликов во времени. Воспринимай видеозаписи как целостные динамичные сюжеты, а фото — как статичные изображения]\n\n$userPrompt"
                }
                else -> userPrompt
            }

            val textPart = JSONObject().put("text", appendReactionFeedback(textContent, msg.reaction))
            parts.put(textPart)

            // Audio parts if exists
            if (!msg.audioFilePath.isNullOrBlank()) {
                try {
                    val audioFile = java.io.File(msg.audioFilePath)
                    if (audioFile.exists() && audioFile.length() > 0L) {
                        val bytes = audioFile.readBytes()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val inlineAudio = JSONObject().put("mimeType", "audio/mp4").put("data", base64)
                        parts.put(JSONObject().put("inlineData", inlineAudio))
                    }
                } catch (_: Exception) {}
            }
            
            // Image or video parts if exists
            for (item in mediaItems) {
                when (item) {
                    is com.example.util.ChatMediaItem.Photo -> {
                        if (item.base64Data.startsWith("data:image/")) {
                            val base64 = item.base64Data.substringAfter(",")
                            val mimeType = item.base64Data.substringBefore(";").substringAfter("data:", "image/jpeg")
                            val inlineData = JSONObject().put("mimeType", mimeType).put("data", base64)
                            parts.put(JSONObject().put("inlineData", inlineData))
                        } else {
                            parts.put(JSONObject().put("text", "Image: ${item.base64Data}"))
                        }
                    }
                    is com.example.util.ChatMediaItem.Video -> {
                        for (frame in item.frameBase64List) {
                            val base64 = frame.substringAfter(",")
                            val mimeType = frame.substringBefore(";").substringAfter("data:", "image/jpeg")
                            val inlineData = JSONObject().put("mimeType", mimeType).put("data", base64)
                            parts.put(JSONObject().put("inlineData", inlineData))
                        }
                    }
                }
            }
            
            val contentObj = JSONObject().put("role", role).put("parts", parts)
            contentsJson.put(contentObj)
        }

        if (contentsJson.length() == 0) {
            val part = JSONObject().put("text", "Привет!")
            val parts = JSONArray().put(part)
            contentsJson.put(JSONObject().put("role", "user").put("parts", parts))
        }

        val bodyJson = JSONObject()
        val sysInstruction = JSONObject().put(
            "parts",
            JSONArray().put(JSONObject().put("text", systemPrompt))
        )
        bodyJson.put("systemInstruction", sysInstruction)
        bodyJson.put("contents", contentsJson)

        val genConfig = JSONObject()
        genConfig.put("temperature", profile.temperature.toDouble())
        genConfig.put("topP", profile.topP.toDouble())
        genConfig.put("maxOutputTokens", profile.maxTokens)
        bodyJson.put("generationConfig", genConfig)

        // Gemini Safety Settings threshold configuration
        val effectiveThreshold = geminiSafetyThreshold.ifBlank { "BLOCK_NONE" }
        val safetyCategories = listOf(
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_HATE_SPEECH",
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT",
            "HARM_CATEGORY_CIVIC_INTEGRITY"
        )
        val safetyArray = JSONArray()
        for (category in safetyCategories) {
            safetyArray.put(
                JSONObject()
                    .put("category", category)
                    .put("threshold", effectiveThreshold)
            )
        }
        bodyJson.put("safetySettings", safetyArray)

        val lastUserMsg = messages.lastOrNull { it.sender == "user" }?.content ?: ""
        applyGeminiModelCustomizations(bodyJson, genConfig, profile, systemPrompt, lastUserMsg)

        val requestBuilder = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
        applyExtraHeaders(requestBuilder, profile.extraHeadersJson)
        val request = requestBuilder.build()

        val requestId = GeminiDebugLogger.nextRequestId()
        GeminiDebugLogger.startSession(
            requestId = requestId,
            model = model,
            isStreaming = (onTokenStream != null)
        )

        var lastError: Exception? = null
        for (attempt in 1..3) {
            var response: Response? = null
            try {
                if (attempt > 1) {
                    GeminiDebugLogger.logEvent(requestId, "RETRY", "Attempt #$attempt after previous failure")
                }
                response = httpClient.newCall(request).execute()

                GeminiDebugLogger.recordResponseStart(
                    requestId = requestId,
                    statusCode = response.code,
                    headers = response.headers
                )

                if (!response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val errorMsg = "Ошибка Gemini [${response.code}]: $responseBody"
                    GeminiDebugLogger.recordFailure(
                        requestId = requestId,
                        errorMsg = errorMsg,
                        isTimeout = false,
                        emptyReason = "HTTP status code ${response.code} is not successful",
                        exception = IOException(errorMsg)
                    )
                    if (response.code == 429 || response.code in 500..599) {
                        delay(1200L * attempt)
                        continue
                    }
                    return Result.failure(IOException(errorMsg))
                }

                if (onTokenStream != null) {
                    val streamedText = StringBuilder()
                    var chunkCount = 0
                    var lastFinishReason: String? = null
                    var latestUsageMetadata: String? = null
                    var latestPromptTokens: Int? = null
                    var latestCandidatesTokens: Int? = null
                    var latestTotalTokens: Int? = null

                    val source = response.body?.source()
                    if (source != null) {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            val byteSize = line.toByteArray(Charsets.UTF_8).size
                            val trimmed = line.trim()
                            if (trimmed.isEmpty()) continue
                            val dataContent = if (trimmed.startsWith("data:")) trimmed.removePrefix("data:").trim() else trimmed
                            if (dataContent.startsWith("{")) {
                                try {
                                    val parsed = JSONObject(dataContent)
                                    val usage = parsed.optJSONObject("usageMetadata")
                                    if (usage != null) {
                                        latestUsageMetadata = usage.toString()
                                        latestPromptTokens = usage.optInt("promptTokenCount", 0)
                                        latestCandidatesTokens = usage.optInt("candidatesTokenCount", 0)
                                        latestTotalTokens = usage.optInt("totalTokenCount", 0)
                                    }

                                    var chunkTextPiece = ""
                                    val candidates = parsed.optJSONArray("candidates")
                                    if (candidates != null && candidates.length() > 0) {
                                        val first = candidates.getJSONObject(0)
                                        val finish = first.optString("finishReason", "")
                                        if (finish.isNotBlank()) {
                                            lastFinishReason = finish
                                        }
                                        val content = first.optJSONObject("content")
                                        val parts = content?.optJSONArray("parts")
                                        if (parts != null) {
                                            val partPieceBuilder = StringBuilder()
                                            for (i in 0 until parts.length()) {
                                                val pObj = parts.getJSONObject(i)
                                                if (profile.thinkingMode == "disabled" && pObj.optBoolean("thought", false)) continue
                                                val piece = pObj.optString("text", "")
                                                if (piece.isNotEmpty()) {
                                                    partPieceBuilder.append(piece)
                                                }
                                            }
                                            chunkTextPiece = partPieceBuilder.toString()
                                            if (chunkTextPiece.isNotEmpty()) {
                                                streamedText.append(chunkTextPiece)
                                                withContext(Dispatchers.Main) {
                                                    onTokenStream(chunkTextPiece)
                                                }
                                            }
                                        }
                                    }

                                    chunkCount++
                                    GeminiDebugLogger.recordChunk(
                                        requestId = requestId,
                                        chunkIndex = chunkCount,
                                        byteSize = byteSize,
                                        textPiece = chunkTextPiece,
                                        finishReason = lastFinishReason,
                                        promptTokens = latestPromptTokens,
                                        candidatesTokens = latestCandidatesTokens,
                                        totalTokens = latestTotalTokens,
                                        usageMetadataStr = latestUsageMetadata,
                                        rawSnippet = if (dataContent.length > 150) dataContent.take(150) + "..." else dataContent
                                    )
                                } catch (parseEx: Exception) {
                                    GeminiDebugLogger.logEvent(requestId, "PARSE_WARNING", "Failed to parse chunk JSON: ${parseEx.message}")
                                }
                            }
                        }
                    }

                    GeminiDebugLogger.recordStreamEnd(requestId)

                    if (streamedText.isNotEmpty()) {
                        val finalOut = stripThinkingIfDisabled(streamedText.toString(), profile)
                        GeminiDebugLogger.recordSuccess(requestId, finalOut)
                        return Result.success(finalOut)
                    } else {
                        val isBlocked = lastFinishReason == "PROHIBITED_CONTENT" || lastFinishReason == "SAFETY"
                        val emptyReason = if (isBlocked) {
                            "Ответ заблокирован фильтром безопасности Gemini ($lastFinishReason). Перейдите в Настройки -> ИИ & Модели и установите порог безопасности модели в значение 'Отключено (BLOCK_NONE)'."
                        } else {
                            "Поток SSE завершился (получено chunks: $chunkCount), но извлеченный текст пуст. finishReason='${lastFinishReason ?: "отсутствует"}', usageMetadata='${latestUsageMetadata ?: "отсутствует"}'"
                        }
                        GeminiDebugLogger.recordFailure(
                            requestId = requestId,
                            errorMsg = if (isBlocked) "Ответ заблокирован фильтром безопасности ($lastFinishReason)" else "Ответ Gemini не пришел",
                            isTimeout = false,
                            emptyReason = emptyReason,
                            exception = IOException(emptyReason)
                        )
                    }
                } else {
                    val responseBody = response.body?.string() ?: ""
                    val parsed = JSONObject(responseBody)
                    val usage = parsed.optJSONObject("usageMetadata")
                    val candidates = parsed.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val first = candidates.getJSONObject(0)
                        val finishReason = first.optString("finishReason", "")
                        val content = first.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val textBuilder = StringBuilder()
                        if (parts != null) {
                            for (i in 0 until parts.length()) {
                                val pObj = parts.getJSONObject(i)
                                if (profile.thinkingMode == "disabled" && pObj.optBoolean("thought", false)) continue
                                textBuilder.append(pObj.optString("text", ""))
                            }
                        }
                        val resultText = stripThinkingIfDisabled(textBuilder.toString(), profile)
                        if (resultText.isNotEmpty()) {
                            GeminiDebugLogger.recordSuccess(requestId, resultText)
                            return Result.success(resultText)
                        } else {
                            val isBlocked = finishReason == "PROHIBITED_CONTENT" || finishReason == "SAFETY"
                            val emptyReason = if (isBlocked) {
                                "Ответ заблокирован фильтром безопасности Gemini ($finishReason). Перейдите в Настройки -> ИИ & Модели и установите порог безопасности модели в значение 'Отключено (BLOCK_NONE)'."
                            } else {
                                "Ответ получен (HTTP 200), но текст в частях parts пуст. finishReason='$finishReason'"
                            }
                            val errorMsg = if (isBlocked) "Ответ заблокирован фильтром безопасности ($finishReason)" else "Пустой ответ от Gemini"
                            GeminiDebugLogger.recordFailure(
                                requestId = requestId,
                                errorMsg = errorMsg,
                                isTimeout = false,
                                emptyReason = emptyReason,
                                exception = IOException(emptyReason)
                            )
                            return Result.failure(IOException(emptyReason))
                        }
                    } else {
                        val emptyReason = "Массив candidates пуст или отсутствует в ответе Gemini"
                        GeminiDebugLogger.recordFailure(
                            requestId = requestId,
                            errorMsg = "Пустой ответ от Gemini",
                            isTimeout = false,
                            emptyReason = emptyReason,
                            exception = IOException(emptyReason)
                        )
                    }
                    return Result.failure(IOException("Пустой ответ от Gemini"))
                }
            } catch (e: Exception) {
                lastError = e
                val isTimeout = (e is java.net.SocketTimeoutException || e is java.io.InterruptedIOException)
                GeminiDebugLogger.recordFailure(
                    requestId = requestId,
                    errorMsg = e.message ?: e.toString(),
                    isTimeout = isTimeout,
                    emptyReason = if (isTimeout) "Таймаут соединения при чтении данных" else "Исключение при чтении ответа: ${e.message}",
                    exception = e
                )
                delay(1000L * attempt)
            } finally {
                try {
                    response?.close()
                    GeminiDebugLogger.recordConnectionClosed(requestId)
                } catch (_: Exception) {}
            }
        }

        val finalError = lastError ?: IOException("Не удалось получить ответ от Gemini")
        GeminiDebugLogger.recordFailure(
            requestId = requestId,
            errorMsg = finalError.message ?: finalError.toString(),
            isTimeout = (finalError is java.net.SocketTimeoutException),
            emptyReason = "Исчерпаны все попытки выполнения запроса: ${finalError.message}",
            exception = finalError
        )
        return Result.failure(finalError)
    }

    suspend fun runGeminiDiagnosticTest(
        profile: AiProviderProfileEntity,
        isStreaming: Boolean = true,
        onTokenStream: ((String) -> Unit)? = null
    ): Result<String> {
        val testMsg = MessageEntity(
            chatId = 0L,
            sender = "user",
            content = "Напиши связный текст примерно на 500–1000 символов. Последними символами ответа обязательно должны быть TEST FINISHED 12345. После этого ничего не пиши."
        )
        val streamCallback: ((String) -> Unit)? = if (isStreaming) {
            onTokenStream ?: { _ -> }
        } else null
        return callGemini(
            profile = profile,
            systemPrompt = "",
            messages = listOf(testMsg),
            onTokenStream = streamCallback
        )
    }

    suspend fun generateImage(
        profile: AiProviderProfileEntity?,
        prompt: String,
        negativePrompt: String = "",
        resolution: String = "832x1216",
        engineMode: String = "auto" // "auto", "flux", "turbo", "openai", "civitai", "custom"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (width, height) = when (resolution) {
                "512x512" -> Pair(512, 512)
                "1024x1024" -> Pair(1024, 1024)
                "1216x832" -> Pair(1024, 768)
                else -> Pair(768, 1024)
            }

            // 1. Direct Free Flux Engine
            if (engineMode == "flux" || (engineMode == "auto" && (profile == null || (profile.apiKey.isBlank() && profile.providerType != "CUSTOM")))) {
                val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
                val seed = (10000..999999).random()
                val fluxUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&model=flux&nologo=true&seed=$seed"
                return@withContext Result.success(fluxUrl)
            }

            // 2. Direct Fast Turbo Engine
            if (engineMode == "turbo") {
                val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
                val seed = (10000..999999).random()
                val turboUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&model=turbo&nologo=true&seed=$seed"
                return@withContext Result.success(turboUrl)
            }

            // 3. Civitai Engine
            if (engineMode == "civitai" || (profile != null && profile.providerType.equals("CIVITAI", ignoreCase = true))) {
                if (profile != null && profile.apiKey.isNotBlank()) {
                    val civitaiRes = callCivitaiImageGeneration(profile, prompt, negativePrompt, resolution)
                    if (civitaiRes.isSuccess) return@withContext civitaiRes
                }
                // Fallback to Flux if Civitai has no API key or failed
                val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
                val seed = (10000..999999).random()
                return@withContext Result.success("https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&model=flux&nologo=true&seed=$seed")
            }

            // 4. OpenAI / Custom Profile
            if (profile != null && (profile.apiKey.isNotBlank() || profile.providerType == "CUSTOM")) {
                val baseUrl = if (profile.baseUrl.isNotBlank()) profile.baseUrl.trimEnd('/') else "https://api.openai.com/v1"
                val isCustomSd = profile.providerType == "CUSTOM" || baseUrl.contains("7860") || baseUrl.contains("sdapi")

                if (isCustomSd) {
                    // Automatic1111 / ComfyUI / SD WebUI format
                    val sdUrl = if (baseUrl.endsWith("/txt2img")) baseUrl else "$baseUrl/txt2img"
                    val body = JSONObject().apply {
                        put("prompt", prompt)
                        put("negative_prompt", negativePrompt.ifBlank { profile.negativePrompt })
                        put("steps", 25)
                        put("width", width)
                        put("height", height)
                        if (profile.seedMode == "fixed") put("seed", profile.fixedSeed)
                    }

                    val req = Request.Builder()
                        .url(sdUrl)
                        .post(body.toString().toRequestBody(jsonMediaType))
                        .build()

                    val resp = httpClient.newCall(req).execute()
                    val bodyStr = resp.body?.string() ?: ""
                    if (resp.isSuccessful) {
                        val json = JSONObject(bodyStr)
                        val images = json.optJSONArray("images")
                        if (images != null && images.length() > 0) {
                            val base64 = images.getString(0)
                            return@withContext Result.success("data:image/png;base64,$base64")
                        }
                    }
                } else {
                    // OpenAI / OpenRouter / compatible DALL-E format
                    val url = if (baseUrl.endsWith("/images/generations")) baseUrl else "$baseUrl/images/generations"
                    val body = JSONObject().apply {
                        put("prompt", prompt)
                        put("model", profile.model.ifBlank { "dall-e-3" })
                        put("n", 1)
                        val dSize = if (resolution == "1024x1024" || resolution == "512x512") "1024x1024" else if (resolution == "1216x832") "1792x1024" else "1024x1792"
                        put("size", dSize)
                        put("response_format", "url")
                    }

                    val reqBuilder = Request.Builder()
                        .url(url)
                        .post(body.toString().toRequestBody(jsonMediaType))

                    if (profile.apiKey.isNotBlank()) {
                        reqBuilder.header("Authorization", "Bearer ${profile.apiKey.trim()}")
                    }

                    val resp = httpClient.newCall(reqBuilder.build()).execute()
                    val bodyStr = resp.body?.string() ?: ""

                    if (resp.isSuccessful) {
                        val json = JSONObject(bodyStr)
                        val data = json.optJSONArray("data")
                        if (data != null && data.length() > 0) {
                            val item = data.getJSONObject(0)
                            val imgUrl = item.optString("url")
                            if (imgUrl.isNotBlank()) return@withContext Result.success(imgUrl)
                            val b64 = item.optString("b64_json")
                            if (b64.isNotBlank()) return@withContext Result.success("data:image/png;base64,$b64")
                        }
                    } else {
                        // If OpenAI failed (e.g. quota/429), fallback gracefully to Flux
                        val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
                        val seed = (10000..999999).random()
                        return@withContext Result.success("https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&model=flux&nologo=true&seed=$seed")
                    }
                }
            }

            // High-quality Pollinations.ai (Flux) fallback generator: free, instant, and high quality
            val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
            val seed = (10000..999999).random()
            val fallbackUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&model=flux&nologo=true&seed=$seed"
            Result.success(fallbackUrl)
        } catch (e: Exception) {
            val encoded = java.net.URLEncoder.encode(prompt.take(200), "UTF-8")
            Result.success("https://image.pollinations.ai/prompt/$encoded?width=768&height=1024&nologo=true")
        }
    }

    private suspend fun callCivitaiImageGeneration(
        profile: AiProviderProfileEntity,
        prompt: String,
        negativePrompt: String,
        resolution: String
    ): Result<String> {
        if (profile.apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Civitai API токен не введен. Добавьте его в настройках провайдера Civitai."))
        }

        val dimensions = when (resolution) {
            "512x512" -> Pair(512, 512)
            "768x1024" -> Pair(768, 1024)
            "832x1216" -> Pair(832, 1216)
            "1024x1024" -> Pair(1024, 1024)
            "1216x832" -> Pair(1216, 832)
            else -> Pair(832, 1216)
        }

        val modelAir = profile.model.ifBlank { "urn:air:sdxl:checkpoint:civitai:257201@290640" } // Pony Diffusion V6 XL

        // If it's Pony Diffusion, augment with quality tags
        val finalPrompt = if (modelAir.contains("257201") || modelAir.contains("pony", ignoreCase = true)) {
            if (!prompt.contains("score_9")) {
                "score_9, score_8_up, score_7_up, score_6_up, masterpiece, high quality, $prompt"
            } else prompt
        } else {
            prompt
        }

        val defaultNeg = if (modelAir.contains("257201") || modelAir.contains("pony", ignoreCase = true)) {
            "score_4, score_5, score_6, ugly, deformed, disfigured, blurry, low quality, bad anatomy, bad hands, artifacts"
        } else {
            "ugly, deformed, disfigured, blurry, low quality, bad anatomy, artifacts"
        }
        val finalNegative = negativePrompt.ifBlank { profile.negativePrompt.ifBlank { defaultNeg } }

        val baseUrl = profile.baseUrl.ifBlank { "https://orchestration.civitai.com/v2/consumer" }.trimEnd('/')
        val jobUrl = "$baseUrl/recipes/imageGen"

        val paramsJson = JSONObject().apply {
            put("prompt", finalPrompt)
            put("negativePrompt", finalNegative)
            put("model", modelAir)
            put("steps", 25)
            put("cfgScale", 7.0)
            put("width", dimensions.first)
            put("height", dimensions.second)
            put("clipSkip", 2)
            put("scheduler", "EulerA")
            if (profile.seedMode == "fixed") put("seed", profile.fixedSeed)
        }

        val rootJson = JSONObject().apply {
            put("params", paramsJson)
        }

        val req = Request.Builder()
            .url(jobUrl)
            .header("Authorization", "Bearer ${profile.apiKey.trim()}")
            .post(rootJson.toString().toRequestBody(jsonMediaType))
            .build()

        val resp = httpClient.newCall(req).execute()
        val bodyStr = resp.body?.string() ?: ""

        if (!resp.isSuccessful) {
            return Result.failure(IOException("Ошибка Civitai API [${resp.code}]: $bodyStr"))
        }

        val respJson = JSONObject(bodyStr)
        val jobsArray = respJson.optJSONArray("jobs")
        val token = respJson.optString("token", "")

        val jobId = if (jobsArray != null && jobsArray.length() > 0) {
            jobsArray.getJSONObject(0).optString("jobId", "")
        } else ""

        if (jobId.isBlank() && token.isBlank()) {
            // Check direct result in response
            val directBlob = respJson.optJSONObject("result")?.optString("blobUrl") ?: ""
            if (directBlob.isNotBlank()) return Result.success(directBlob)
            return Result.failure(IOException("Civitai не вернул идентификатор задачи"))
        }

        // Poll for completion (up to 40 seconds)
        val pollUrl = if (jobId.isNotBlank()) "$baseUrl/jobs/$jobId" else "$baseUrl/workflows/$token"

        for (attempt in 1..20) {
            delay(2000)
            val pollReq = Request.Builder()
                .url(pollUrl)
                .header("Authorization", "Bearer ${profile.apiKey.trim()}")
                .get()
                .build()

            val pollResp = httpClient.newCall(pollReq).execute()
            val pollBody = pollResp.body?.string() ?: ""

            if (pollResp.isSuccessful) {
                val pollJson = JSONObject(pollBody)
                val status = pollJson.optString("status", "")
                val resultObj = pollJson.optJSONObject("result")

                if (resultObj != null) {
                    val blobUrl = resultObj.optString("blobUrl", "")
                    if (blobUrl.isNotBlank()) return Result.success(blobUrl)

                    val images = resultObj.optJSONArray("images")
                    if (images != null && images.length() > 0) {
                        val imgUrl = images.getJSONObject(0).optString("url")
                        if (imgUrl.isNotBlank()) return Result.success(imgUrl)
                    }
                }

                if (status.equals("Failed", ignoreCase = true) || status.equals("Canceled", ignoreCase = true)) {
                    val reason = pollJson.optString("reason", "Ошибка генерации Civitai")
                    return Result.failure(IOException(reason))
                }

                if (status.equals("Succeeded", ignoreCase = true) || status.equals("Completed", ignoreCase = true)) {
                    val blobUrl = resultObj?.optString("blobUrl", "") ?: ""
                    if (blobUrl.isNotBlank()) return Result.success(blobUrl)
                }
            }
        }
        return Result.failure(IOException("Таймаут ожидания генерации Civitai (проверьте историю в аккаунте Civitai)"))
    }

    suspend fun testConnection(profile: AiProviderProfileEntity): Result<String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val cleanKey = profile.apiKey.trim().removePrefix("Bearer ").removePrefix("bearer ").trim()
            if (cleanKey.isBlank() && !profile.providerType.equals("CUSTOM", ignoreCase = true)) {
                return@withContext Result.failure(IllegalStateException("API-ключ для провайдера \"${profile.name}\" не заполнен."))
            }

            if (profile.providerType.equals("CIVITAI", ignoreCase = true)) {
                val endpoint = "https://civitai.com/api/v1/models?limit=1"
                val reqPayload = JSONObject().apply {
                    put("method", "GET")
                    put("url", endpoint)
                    put("headers", JSONObject().apply {
                        put("Authorization", "Bearer ${if (cleanKey.isNotBlank()) "key_provided_***" else "missing"}")
                        put("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    })
                }.toString(2)

                val req = Request.Builder()
                    .url(endpoint)
                    .header("Authorization", "Bearer $cleanKey")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    .get()
                    .build()

                val resp = httpClient.newCall(req).execute()
                val duration = System.currentTimeMillis() - startTime
                val rawBody = resp.body?.string() ?: ""
                val formattedResponse = prettyJson(rawBody)

                val report = buildDiagnosticReport(
                    isSuccess = resp.isSuccessful,
                    status = "HTTP ${resp.code} ${resp.message}",
                    durationMs = duration,
                    summary = if (resp.isSuccessful) "Civitai API успешно подключен! Доступ к генерации открыт." else "Ошибка Civitai API: код ${resp.code}",
                    requestJson = reqPayload,
                    responseJson = formattedResponse,
                    endpoint = endpoint
                )

                return@withContext if (resp.isSuccessful) Result.success(report) else Result.failure(IOException(report))
            }

            val testUserText = profile.testPrompt.ifBlank { "Ответь одним словом: 'Готов'" }
            val testSysText = profile.testSystemPrompt.ifBlank { "Ты тестовый помощник. Отвечай кратко." }
            val testTokens = profile.testMaxTokens.coerceAtLeast(1)

            if (profile.providerType.equals("GEMINI", ignoreCase = true)) {
                val model = profile.model.ifBlank { "gemini-3.5-flash-lite" }
                val baseUrl = profile.baseUrl.ifBlank { "https://generativelanguage.googleapis.com" }.trimEnd('/')
                val action = if (profile.testStream) "streamGenerateContent?alt=sse&" else "generateContent?"
                val endpoint = "$baseUrl/v1beta/models/$model:${action}key=$cleanKey"
                val displayEndpoint = "$baseUrl/v1beta/models/$model:${action.removeSuffix("&").removeSuffix("?")}"

                val genConfig = JSONObject().apply {
                    put("temperature", profile.temperature.toDouble())
                    put("topP", profile.topP.toDouble())
                    put("maxOutputTokens", testTokens)
                }
                val reqJsonObj = JSONObject().apply {
                    if (testSysText.isNotBlank()) {
                        put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", testSysText))))
                    }
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", testUserText))
                            })
                        })
                    })
                    put("generationConfig", genConfig)
                }
                applyGeminiModelCustomizations(reqJsonObj, genConfig, profile, testSysText, testUserText)
                if (profile.testCustomBodyJson.isNotBlank()) {
                    applyJsonOverrideOrMerge(reqJsonObj, profile.testCustomBodyJson)
                }
                val reqPayload = reqJsonObj.toString(2)

                val reqBuilder = Request.Builder()
                    .url(endpoint)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    .post(reqJsonObj.toString().toRequestBody(jsonMediaType))
                applyExtraHeaders(reqBuilder, profile.extraHeadersJson)

                val resp = httpClient.newCall(reqBuilder.build()).execute()
                val duration = System.currentTimeMillis() - startTime
                val rawBody = resp.body?.string() ?: ""
                val formattedResponse = prettyJson(rawBody)

                val answer = try {
                    val root = JSONObject(rawBody)
                    val candidates = root.optJSONArray("candidates")
                    val first = candidates?.optJSONObject(0)
                    val content = first?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val sb = StringBuilder()
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val p = parts.getJSONObject(i)
                            if (profile.thinkingMode == "disabled" && p.optBoolean("thought", false)) continue
                            sb.append(p.optString("text", ""))
                        }
                    }
                    stripThinkingIfDisabled(sb.toString().trim(), profile).ifBlank { "Готов" }
                } catch (_: Exception) { "Готов" }

                val report = buildDiagnosticReport(
                    isSuccess = resp.isSuccessful,
                    status = "HTTP ${resp.code} ${resp.message}",
                    durationMs = duration,
                    summary = if (resp.isSuccessful) "Gemini ответил: \"$answer\"" else "Ошибка Gemini API: HTTP ${resp.code}",
                    requestJson = reqPayload,
                    responseJson = formattedResponse,
                    endpoint = displayEndpoint,
                    model = model
                )

                return@withContext if (resp.isSuccessful) Result.success(report) else Result.failure(IOException(report))
            }

            if (profile.providerType.equals("ANTHROPIC", ignoreCase = true)) {
                val model = profile.model.ifBlank { "claude-3-5-haiku-20241022" }
                val baseUrl = profile.baseUrl.ifBlank { "https://api.anthropic.com/v1" }.trimEnd('/')
                val endpoint = if (baseUrl.endsWith("/messages")) baseUrl else "$baseUrl/messages"

                val reqJsonObj = JSONObject().apply {
                    put("model", model)
                    put("max_tokens", testTokens)
                    put("temperature", profile.temperature.toDouble())
                    if (testSysText.isNotBlank()) put("system", testSysText)
                    if (profile.testStream) put("stream", true)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", testUserText)
                        })
                    })
                }
                applyAnthropicModelCustomizations(reqJsonObj, profile, testSysText, testUserText)
                if (profile.testCustomBodyJson.isNotBlank()) {
                    applyJsonOverrideOrMerge(reqJsonObj, profile.testCustomBodyJson)
                }
                val reqPayload = reqJsonObj.toString(2)

                val reqBuilder = Request.Builder()
                    .url(endpoint)
                    .header("x-api-key", cleanKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
                    .post(reqJsonObj.toString().toRequestBody(jsonMediaType))
                applyExtraHeaders(reqBuilder, profile.extraHeadersJson)

                val resp = httpClient.newCall(reqBuilder.build()).execute()
                val duration = System.currentTimeMillis() - startTime
                val rawBody = resp.body?.string() ?: ""
                val formattedResponse = prettyJson(rawBody)

                val answer = try {
                    val root = JSONObject(rawBody)
                    val contentArr = root.optJSONArray("content")
                    contentArr?.optJSONObject(0)?.optString("text")?.trim() ?: "Готов"
                } catch (_: Exception) { "Готов" }

                val report = buildDiagnosticReport(
                    isSuccess = resp.isSuccessful,
                    status = "HTTP ${resp.code} ${resp.message}",
                    durationMs = duration,
                    summary = if (resp.isSuccessful) "Claude ответил: \"$answer\"" else "Ошибка Anthropic API: HTTP ${resp.code}",
                    requestJson = reqPayload,
                    responseJson = formattedResponse,
                    endpoint = endpoint,
                    model = model
                )

                return@withContext if (resp.isSuccessful) Result.success(report) else Result.failure(IOException(report))
            }

            // OpenAI and OpenAI-compatible APIs (Cerebras, Grok/xAI, Groq, SambaNova, DeepSeek, Together, Mistral, Fireworks, Perplexity, OpenRouter, OpenAI, Custom)
            val isCerebras = profile.providerType.equals("CEREBRAS", ignoreCase = true)
            val isGrokOrXai = profile.providerType.equals("GROK", ignoreCase = true) ||
                    profile.providerType.equals("XAI", ignoreCase = true) ||
                    profile.apiKey.trim().startsWith("xai-") ||
                    profile.model.lowercase().startsWith("grok") ||
                    profile.baseUrl.contains("api.x.ai")
            val isRouters = profile.providerType.equals("OPENROUTER", ignoreCase = true)

            var rawBaseUrl = when {
                profile.baseUrl.isNotBlank() -> profile.baseUrl.trim().trimEnd('/')
                isCerebras -> "https://api.cerebras.ai/v1"
                isGrokOrXai -> "https://api.x.ai/v1"
                isRouters -> "https://openrouter.ai/api/v1"
                else -> getDefaultBaseUrl(profile.providerType)
            }

            if (isGrokOrXai && (rawBaseUrl.contains("api.groq.com") || rawBaseUrl.contains("api.openai.com") || rawBaseUrl.isBlank())) {
                rawBaseUrl = "https://api.x.ai/v1"
            }
            if (rawBaseUrl.contains("api.x.ai") && !rawBaseUrl.contains("/v1")) {
                rawBaseUrl = "$rawBaseUrl/v1"
            }

            val baseUrl = rawBaseUrl.trimEnd('/')
            val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"

            val modelToUse = profile.model.ifBlank {
                when {
                    isCerebras -> "gpt-oss-120b"
                    isGrokOrXai -> "grok-2-latest"
                    isRouters -> "google/gemma-4-26b-a4b-it:free"
                    else -> "gpt-4o-mini"
                }
            }

            val reqJsonObj = JSONObject().apply {
                put("model", modelToUse)
                put("messages", JSONArray().apply {
                    if (testSysText.isNotBlank()) {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", testSysText)
                        })
                    }
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", testUserText)
                    })
                })
                put("temperature", profile.temperature.toDouble())
                put("top_p", profile.topP.toDouble())
                put("max_tokens", testTokens)
                if (profile.testStream) put("stream", true)
            }
            applyOpenAiModelCustomizations(
                bodyJson = reqJsonObj,
                profile = profile,
                modelName = modelToUse,
                systemPrompt = testSysText,
                userPrompt = testUserText,
                isStream = profile.testStream
            )
            if (profile.testCustomBodyJson.isNotBlank()) {
                applyJsonOverrideOrMerge(reqJsonObj, profile.testCustomBodyJson)
            }
            val reqPayload = reqJsonObj.toString(2)

            val reqBuilder = Request.Builder()
                .url(endpoint)
                .post(reqJsonObj.toString().toRequestBody(jsonMediaType))

            if (cleanKey.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $cleanKey")
            }
            reqBuilder.header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
            reqBuilder.header("Accept", "application/json, */*")
            reqBuilder.header("Content-Type", "application/json")
            if (isRouters) {
                reqBuilder.header("HTTP-Referer", "https://ai.studio")
                reqBuilder.header("X-Title", "AI Roleplay Engine Android")
            }
            applyExtraHeaders(reqBuilder, profile.extraHeadersJson)

            val resp = httpClient.newCall(reqBuilder.build()).execute()
            val duration = System.currentTimeMillis() - startTime
            val rawBody = resp.body?.string() ?: ""
            val formattedResponse = prettyJson(rawBody)

            if (resp.isSuccessful) {
                val answer = try {
                    val root = JSONObject(rawBody)
                    val choices = root.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val msg = firstChoice?.optJSONObject("message")
                    stripThinkingIfDisabled(msg?.optString("content", "")?.trim() ?: "Готов", profile)
                } catch (_: Exception) { "Готов" }

                val report = buildDiagnosticReport(
                    isSuccess = true,
                    status = "HTTP ${resp.code} ${resp.message}",
                    durationMs = duration,
                    summary = "Модель ответила: \"$answer\"",
                    requestJson = reqPayload,
                    responseJson = formattedResponse,
                    endpoint = endpoint,
                    model = modelToUse
                )
                Result.success(report)
            } else {
                val errorAdvice = when {
                    resp.code == 403 && (endpoint.contains("api.x.ai") || isGrokOrXai) -> {
                        "Возможные причины 403 от xAI (Grok):\n" +
                        "• Региональные ограничения xAI (требуется IP/VPN поддерживаемой страны)\n" +
                        "• Доступ к модели ($modelToUse) ограничен для вашего ключа в console.x.ai\n" +
                        "• Лимиты бесплатного тарифа (Free tier) исчерпаны или требуется верификация аккаунта\n" +
                        "• Разрешения API-ключа: проверьте права ключа в console.x.ai\n" +
                        "• Подписка X (Twitter) Premium не дает доступа к xAI Developer API"
                    }
                    resp.code == 403 && rawBody.contains("cloudflare", ignoreCase = true) -> {
                        "Блокировка Cloudflare WAF. Попробуйте сменить сеть или использовать VPN."
                    }
                    resp.code == 401 -> "Неверный API-ключ или срок его действия истек."
                    resp.code == 429 -> "Превышен лимит запросов (Rate Limit 429)."
                    else -> null
                }

                val report = buildDiagnosticReport(
                    isSuccess = false,
                    status = "HTTP ${resp.code} ${resp.message}",
                    durationMs = duration,
                    summary = "Ошибка API [${resp.code}]: ${resp.message}",
                    requestJson = reqPayload,
                    responseJson = formattedResponse,
                    endpoint = endpoint,
                    model = modelToUse,
                    advice = errorAdvice
                )
                Result.failure(IOException(report))
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val report = buildDiagnosticReport(
                isSuccess = false,
                status = "Исключение сети: ${e.javaClass.simpleName}",
                durationMs = duration,
                summary = e.message ?: "Не удалось выполнить запрос",
                requestJson = "{}",
                responseJson = "{}",
                advice = "Проверьте подключение к интернету или корректность адреса сервера."
            )
            Result.failure(IOException(report))
        }
    }

    private fun prettyJson(raw: String): String {
        if (raw.isBlank()) return "{}"
        return try {
            JSONObject(raw).toString(2)
        } catch (_: Exception) {
            try {
                JSONArray(raw).toString(2)
            } catch (_: Exception) {
                raw
            }
        }
    }

    private fun buildDiagnosticReport(
        isSuccess: Boolean,
        status: String,
        durationMs: Long,
        summary: String,
        requestJson: String,
        responseJson: String,
        endpoint: String = "",
        model: String = "",
        advice: String? = null
    ): String {
        val sb = StringBuilder()
        sb.append(if (isSuccess) "--- RESULT_STATUS: SUCCESS ---\n" else "--- RESULT_STATUS: ERROR ---\n")
        sb.append("--- STATUS: $status ---\n")
        sb.append("--- DURATION_MS: $durationMs ---\n")
        if (endpoint.isNotBlank()) sb.append("--- ENDPOINT: $endpoint ---\n")
        if (model.isNotBlank()) sb.append("--- MODEL: $model ---\n")
        sb.append("--- SUMMARY: $summary ---\n")
        if (!advice.isNullOrBlank()) {
            sb.append("--- ADVICE_START ---\n")
            sb.append(advice)
            sb.append("\n--- ADVICE_END ---\n")
        }
        sb.append("--- REQUEST_JSON_START ---\n")
        sb.append(requestJson)
        sb.append("\n--- REQUEST_JSON_END ---\n")
        sb.append("--- RESPONSE_JSON_START ---\n")
        sb.append(responseJson)
        sb.append("\n--- RESPONSE_JSON_END ---")
        return sb.toString()
    }

    fun getDefaultBaseUrl(providerType: String): String {
        return when (providerType.uppercase()) {
            "CEREBRAS" -> "https://api.cerebras.ai/v1"
            "OPENROUTER" -> "https://openrouter.ai/api/v1"
            "GROK", "XAI" -> "https://api.x.ai/v1"
            "GROQ" -> "https://api.groq.com/openai/v1"
            "SAMBANOVA" -> "https://api.sambanova.ai/v1"
            "DEEPSEEK" -> "https://api.deepseek.com/v1"
            "TOGETHER" -> "https://api.together.xyz/v1"
            "MISTRAL" -> "https://api.mistral.ai/v1"
            "FIREWORKS" -> "https://api.fireworks.ai/inference/v1"
            "PERPLEXITY" -> "https://api.perplexity.ai"
            "ANTHROPIC" -> "https://api.anthropic.com/v1"
            "GEMINI" -> "https://generativelanguage.googleapis.com"
            "CIVITAI" -> "https://orchestration.civitai.com/v2/consumer"
            "CUSTOM" -> "http://192.168.1.100:7860/sdapi/v1"
            else -> "https://api.openai.com/v1"
        }
    }

    fun getRecommendedModels(providerType: String): List<String> {
        return when (providerType.uppercase()) {
            "CEREBRAS" -> listOf(
                "gpt-oss-120b",
                "llama-3.3-70b",
                "llama3.1-8b",
                "llama3.1-70b",
                "qwen-2.5-coder-32b"
            )
            "GROK", "XAI" -> listOf(
                "grok-2-latest",
                "grok-2",
                "grok-3",
                "grok-3-mini",
                "grok-2-vision-1212",
                "grok-beta"
            )
            "GROQ" -> listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "mixtral-8x7b-32768",
                "deepseek-r1-distill-llama-70b",
                "gemma2-9b-it"
            )
            "SAMBANOVA" -> listOf(
                "Meta-Llama-3.3-70B-Instruct",
                "Meta-Llama-3.1-405B-Instruct",
                "Meta-Llama-3.1-70B-Instruct",
                "Meta-Llama-3.1-8B-Instruct",
                "Qwen2.5-72B-Instruct",
                "DeepSeek-R1-Distill-Llama-70B"
            )
            "DEEPSEEK" -> listOf(
                "deepseek-chat",
                "deepseek-reasoner"
            )
            "TOGETHER" -> listOf(
                "meta-llama/Llama-3.3-70B-Instruct-Turbo",
                "meta-llama/Meta-Llama-3.1-405B-Instruct-Turbo",
                "mistralai/Mixtral-8x22B-Instruct-v0.1",
                "deepseek-ai/DeepSeek-V3",
                "Qwen/Qwen2.5-72B-Instruct-Turbo"
            )
            "MISTRAL" -> listOf(
                "mistral-large-latest",
                "mistral-small-latest",
                "mistral-nemo",
                "codestral-latest",
                "pixtral-12b-2409"
            )
            "FIREWORKS" -> listOf(
                "accounts/fireworks/models/llama-v3p3-70b-instruct",
                "accounts/fireworks/models/llama-v3p1-405b-instruct",
                "accounts/fireworks/models/qwen2p5-72b-instruct",
                "accounts/fireworks/models/deepseek-v3"
            )
            "PERPLEXITY" -> listOf(
                "sonar-pro",
                "sonar",
                "sonar-reasoning",
                "sonar-reasoning-pro",
                "r1-1776"
            )
            "OPENROUTER" -> listOf(
                "deepseek/deepseek-chat",
                "meta-llama/llama-3.3-70b-instruct:free",
                "google/gemini-2.0-flash-001",
                "anthropic/claude-3.5-sonnet",
                "openai/gpt-4o-mini"
            )
            "OPENAI" -> listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "o3-mini",
                "gpt-4-turbo"
            )
            "ANTHROPIC" -> listOf(
                "claude-3-7-sonnet-20250219",
                "claude-3-5-sonnet-20241022",
                "claude-3-5-haiku-20241022"
            )
            "GEMINI" -> listOf(
                "gemini-3.5-flash-lite",
                "gemini-2.5-flash",
                "gemini-2.5-pro",
                "gemini-2.0-flash",
                "gemini-1.5-flash"
            )
            else -> listOf("gpt-4o-mini", "llama-3.3-70b-instruct")
        }
    }

    suspend fun fetchRemoteModels(
        providerType: String,
        baseUrl: String = "",
        apiKey: String = ""
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val endpoint = when (providerType.uppercase()) {
                "CEREBRAS" -> "https://api.cerebras.ai/v1/models"
                "OPENROUTER" -> "https://openrouter.ai/api/v1/models"
                "GROK", "XAI" -> "https://api.x.ai/v1/models"
                "GROQ" -> "https://api.groq.com/openai/v1/models"
                "SAMBANOVA" -> "https://api.sambanova.ai/v1/models"
                "DEEPSEEK" -> "https://api.deepseek.com/models"
                "TOGETHER" -> "https://api.together.xyz/v1/models"
                "MISTRAL" -> "https://api.mistral.ai/v1/models"
                "FIREWORKS" -> "https://api.fireworks.ai/inference/v1/models"
                "OPENAI" -> "https://api.openai.com/v1/models"
                else -> {
                    val base = if (baseUrl.isNotBlank()) baseUrl.trimEnd('/') else getDefaultBaseUrl(providerType).trimEnd('/')
                    if (base.endsWith("/models")) base else "$base/models"
                }
            }

            val reqBuilder = Request.Builder().url(endpoint).get()

            val cleanKey = apiKey.trim().removePrefix("Bearer ").removePrefix("bearer ").trim()
            if (cleanKey.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $cleanKey")
            }
            reqBuilder.header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
            reqBuilder.header("Accept", "application/json, */*")
            if (providerType.uppercase() == "OPENROUTER") {
                reqBuilder.header("HTTP-Referer", "https://ai.studio")
                reqBuilder.header("X-Title", "AI Roleplay Engine Android")
            }

            val resp = httpClient.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                val curated = getRecommendedModels(providerType)
                if (curated.isNotEmpty()) return@withContext Result.success(curated)
                return@withContext Result.failure(IOException("Не удалось получить список моделей: ${resp.code}"))
            }

            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: JSONArray()
            val models = mutableListOf<String>()
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val id = obj.optString("id")
                if (id.isNotBlank()) models.add(id)
            }
            if (models.isEmpty()) {
                val curated = getRecommendedModels(providerType)
                Result.success(curated)
            } else {
                Result.success(models.sorted())
            }
        } catch (e: Exception) {
            val curated = getRecommendedModels(providerType)
            if (curated.isNotEmpty()) {
                Result.success(curated)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun fetchOpenRouterModels(apiKey: String): Result<List<String>> =
        fetchRemoteModels("OPENROUTER", "", apiKey)

    suspend fun searchCivitaiModels(query: String, apiKey: String = ""): Result<List<CivitaiModelItem>> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query.ifBlank { "Pony" }, "UTF-8")
            val url = "https://civitai.com/api/v1/models?types=Checkpoint&sort=Highest+Rated&limit=15&query=$encodedQuery"

            val reqBuilder = Request.Builder().url(url).get()
            if (apiKey.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
            }

            val resp = httpClient.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string() ?: ""

            if (!resp.isSuccessful) {
                // Fallback to top curated list
                return@withContext Result.success(getCuratedCivitaiModels())
            }

            val json = JSONObject(body)
            val itemsArray = json.optJSONArray("items") ?: JSONArray()
            val list = mutableListOf<CivitaiModelItem>()

            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                val id = itemObj.optLong("id")
                val name = itemObj.optString("name")
                val type = itemObj.optString("type", "Checkpoint")
                val description = itemObj.optString("description", "")

                val versionsList = mutableListOf<CivitaiVersionItem>()
                val versionsArr = itemObj.optJSONArray("modelVersions")
                if (versionsArr != null) {
                    for (v in 0 until versionsArr.length()) {
                        val vObj = versionsArr.getJSONObject(v)
                        val vId = vObj.optLong("id")
                        val vName = vObj.optString("name")
                        val baseModel = vObj.optString("baseModel", "SDXL 1.0")
                        val air = vObj.optString("air", "urn:air:sdxl:checkpoint:civitai:$id@$vId")

                        var previewUrl: String? = null
                        val imagesArr = vObj.optJSONArray("images")
                        if (imagesArr != null && imagesArr.length() > 0) {
                            previewUrl = imagesArr.getJSONObject(0).optString("url")
                        }

                        versionsList.add(
                            CivitaiVersionItem(
                                id = vId,
                                name = vName,
                                baseModel = baseModel,
                                air = if (air.isNotBlank()) air else "urn:air:sdxl:checkpoint:civitai:$id@$vId",
                                previewImageUrl = previewUrl
                            )
                        )
                    }
                }

                list.add(
                    CivitaiModelItem(
                        id = id,
                        name = name,
                        type = type,
                        description = description,
                        versions = versionsList
                    )
                )
            }

            if (list.isEmpty()) {
                Result.success(getCuratedCivitaiModels())
            } else {
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.success(getCuratedCivitaiModels())
        }
    }

    fun getCuratedCivitaiModels(): List<CivitaiModelItem> {
        return listOf(
            CivitaiModelItem(
                id = 257201L,
                name = "Pony Diffusion V6 XL",
                type = "Checkpoint",
                description = "Лучшая универсальная модель для аниме, артов и персонажей на базе SDXL.",
                versions = listOf(
                    CivitaiVersionItem(
                        id = 290640L,
                        name = "V6 (start with this one)",
                        baseModel = "SDXL 1.0",
                        air = "urn:air:sdxl:checkpoint:civitai:257201@290640",
                        previewImageUrl = "https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/466632c0-e71f-4740-96f7-a3cfab7593fb/width=450/00021-1372583549.jpeg"
                    )
                )
            ),
            CivitaiModelItem(
                id = 497558L,
                name = "Illustrious-XL",
                type = "Checkpoint",
                description = "Мощная модель нового поколения для стилизованной графики.",
                versions = listOf(
                    CivitaiVersionItem(
                        id = 556754L,
                        name = "v0.1",
                        baseModel = "SDXL 1.0",
                        air = "urn:air:sdxl:checkpoint:civitai:497558@556754"
                    )
                )
            ),
            CivitaiModelItem(
                id = 260267L,
                name = "Animagine XL V3.1",
                type = "Checkpoint",
                description = "Популярная открытая модель для аниме стиля и детализированных сцен.",
                versions = listOf(
                    CivitaiVersionItem(
                        id = 293564L,
                        name = "v3.1",
                        baseModel = "SDXL 1.0",
                        air = "urn:air:sdxl:checkpoint:civitai:260267@293564"
                    )
                )
            ),
            CivitaiModelItem(
                id = 112552L,
                name = "DreamShaper XL",
                type = "Checkpoint",
                description = "Универсальный чекпоинт для кинематографичных и реалистичных изображений.",
                versions = listOf(
                    CivitaiVersionItem(
                        id = 126688L,
                        name = "Turbo v2.1",
                        baseModel = "SDXL Turbo",
                        air = "urn:air:sdxl:checkpoint:civitai:112552@126688"
                    )
                )
            ),
            CivitaiModelItem(
                id = 133005L,
                name = "Juggernaut XL",
                type = "Checkpoint",
                description = "Высочайшая фотореалистичность и детальные портреты.",
                versions = listOf(
                    CivitaiVersionItem(
                        id = 456627L,
                        name = "Version 9+RDPhoto2",
                        baseModel = "SDXL 1.0",
                        air = "urn:air:sdxl:checkpoint:civitai:133005@456627"
                    )
                )
            )
        )
    }

    suspend fun generateComicPanels(
        textProfile: AiProviderProfileEntity,
        imageProfile: AiProviderProfileEntity?,
        characterName: String,
        characterAppearance: String,
        messages: List<MessageEntity>,
        style: String,
        chapterTitle: String,
        civitaiModelAir: String? = null,
        customPromptAddition: String = "",
        customNegativePrompt: String = "",
        chapterSummaryText: String? = null,
        panelCount: Int = 4,
        onPanelProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<List<ComicPanelData>> = withContext(Dispatchers.IO) {
        try {
            onPanelProgress(0, 4, "Анализ сцен и диалогов главы...")

            val stylePrefix = when (style) {
                "anime_color" -> "masterpiece, anime artwork, vibrant colors, comic panel, highly detailed, clean lines"
                "manga_bw" -> "manga page, classic monochrome manga, screentone, inked lineart, expressive, sharp contrast"
                "marvel_dc" -> "western comic book style, bold ink lines, dynamic superhero comic illustration, dramatic lighting"
                "cyberpunk" -> "cyberpunk neon comic art, glowing neon lights, futuristic gritty comic panel, cinematic"
                "webtoon" -> "digital manhwa webtoon style, soft cel shading, vertical comic panel illustration, crisp colors"
                "watercolor" -> "watercolor graphic novel illustration, artistic soft brushstrokes, storybook painting"
                "pixel_art" -> "pixel art retro 16-bit graphic novel panel, detailed pixel illustration"
                "dark_fantasy" -> "dark fantasy gothic comic art, gloomy atmosphere, painterly, hyper-detailed, dramatic shadows"
                else -> "anime comic panel, high quality illustration"
            }

            // Extract context from messages or chapter summary
            val chatExcerpt = if (!chapterSummaryText.isNullOrBlank()) {
                "Краткое содержание главы:\n$chapterSummaryText\n\nНедавние сообщения:\n" +
                        messages.takeLast(10).joinToString("\n") { msg ->
                            val speaker = if (msg.sender == "user") "Собеседник" else characterName
                            "$speaker: ${msg.content}"
                        }
            } else {
                messages.takeLast(12).joinToString("\n") { msg ->
                    val speaker = if (msg.sender == "user") "Собеседник" else characterName
                    "$speaker: ${msg.content}"
                }
            }

            val targetPanels = panelCount.coerceIn(2, 8)

            val scriptPrompt = """
                Ты — профессиональный сценарист и раскадровщик комиксов и манги.
                На основе следующих событий и диалога из главы "$chapterTitle", создай $targetPanels последовательных комикс-кадров (панелей).
                
                Персонаж: $characterName
                Внешность персонажа: $characterAppearance
                ${if (customPromptAddition.isNotBlank()) "Дополнительные пожелания к сценам/деталям: $customPromptAddition" else ""}
                
                События и контекст главы:
                $chatExcerpt
                
                Ответь СТРОГО в формате JSON-массива из $targetPanels объектов без лишнего текста вокруг:
                [
                  {
                    "panelIndex": 1,
                    "speakerName": "$characterName",
                    "dialogueText": "Реплика или мысль персонажа",
                    "soundEffect": "*SFX*",
                    "sceneDescription": "Краткое описание действия и эмоции",
                    "imagePrompt": "Детальный английский промпт для генератора картинок описывающий позу, ракурс, эмоции персонажа и окружение"
                  }
                ]
            """.trimIndent()

            val scriptResult = generateChatCompletion(
                profile = textProfile,
                systemPrompt = "Ты генератор раскадровки комиксов. Отвечай только валидным JSON-массивом.",
                recentMessages = listOf(
                    MessageEntity(chatId = 0, sender = "user", content = scriptPrompt)
                )
            )

            val rawScript = scriptResult.getOrElse {
                // Fallback default panels if script LLM failed
                """
                [
                  {"panelIndex": 1, "speakerName": "$characterName", "dialogueText": "Я рада, что мы здесь...", "soundEffect": "*шелест ветра*", "sceneDescription": "Персонаж смотрит с легкой улыбкой", "imagePrompt": "1girl, $characterAppearance, gentle smile, looking at viewer, cinematic lighting, outdoor breeze, depth of field"},
                  {"panelIndex": 2, "speakerName": "Собеседник", "dialogueText": "Расскажи мне подробнее.", "soundEffect": "*шаги*", "sceneDescription": "Персонаж делает шаг навстречу", "imagePrompt": "1girl, $characterAppearance, expressive eyes, taking a step forward, detailed background, dynamic perspective"},
                  {"panelIndex": 3, "speakerName": "$characterName", "dialogueText": "Это было так неожиданно!", "soundEffect": "*DOKI DOKI*", "sceneDescription": "Эмоциональный крупный план", "imagePrompt": "1girl, $characterAppearance, close-up face, blushing, surprised happy expression, vibrant comic art"},
                  {"panelIndex": 4, "speakerName": "$characterName", "dialogueText": "Давай продолжим этот путь вместе.", "soundEffect": "*сияние*", "sceneDescription": "Теплый финальный кадр", "imagePrompt": "1girl, $characterAppearance, warm sunset glow, soft smile, holding hands perspective, masterpiece"}
                ]
                """.trimIndent()
            }

            val jsonCleaned = rawScript.trim().let {
                var s = it
                if (s.startsWith("```json")) s = s.removePrefix("```json")
                if (s.startsWith("```")) s = s.removePrefix("```")
                if (s.endsWith("```")) s = s.removeSuffix("```")
                s.trim()
            }

            val jsonArray = try {
                JSONArray(jsonCleaned)
            } catch (_: Exception) {
                val start = jsonCleaned.indexOf('[')
                val end = jsonCleaned.lastIndexOf(']')
                if (start != -1 && end != -1 && end > start) {
                    JSONArray(jsonCleaned.substring(start, end + 1))
                } else {
                    JSONArray()
                }
            }

            val panelList = mutableListOf<ComicPanelData>()
            val totalPanels = if (jsonArray.length() > 0) minOf(jsonArray.length(), targetPanels) else targetPanels

            for (i in 0 until totalPanels) {
                val obj = if (i < jsonArray.length()) jsonArray.optJSONObject(i) else null
                val pIndex = obj?.optInt("panelIndex", i + 1) ?: (i + 1)
                val speaker = obj?.optString("speakerName", if (i % 2 == 0) characterName else "Собеседник") ?: characterName
                val dialogue = obj?.optString("dialogueText", "...") ?: "..."
                val sfx = obj?.optString("soundEffect", "") ?: ""
                val scene = obj?.optString("sceneDescription", "Кадр $pIndex") ?: "Кадр $pIndex"
                val rawPrompt = obj?.optString("imagePrompt", "1girl, $characterAppearance, expressive") ?: "1girl, $characterAppearance"

                val promptAdditionStr = if (customPromptAddition.isNotBlank()) ", $customPromptAddition" else ""
                val finalImagePrompt = "$stylePrefix, $rawPrompt$promptAdditionStr, comic book framing, high resolution"

                panelList.add(
                    ComicPanelData(
                        panelIndex = pIndex,
                        speakerName = speaker,
                        dialogueText = dialogue,
                        soundEffect = sfx,
                        sceneDescription = scene,
                        imagePrompt = finalImagePrompt,
                        imageUri = ""
                    )
                )
            }

            // Generate images if imageProfile is available
            val effectiveImageProfile = if (imageProfile != null && !civitaiModelAir.isNullOrBlank() && imageProfile.providerType.equals("CIVITAI", ignoreCase = true)) {
                imageProfile.copy(model = civitaiModelAir)
            } else {
                imageProfile
            }

            val negPrompt = if (customNegativePrompt.isNotBlank()) {
                customNegativePrompt
            } else {
                "ugly, deformed, disfigured, text watermark, bad anatomy, bad hands, lowres, blurry"
            }

            for (idx in panelList.indices) {
                val panel = panelList[idx]
                onPanelProgress(idx + 1, panelList.size, "Генерация арта кадра ${panel.panelIndex} из ${panelList.size}...")
                
                var generatedUri = ""
                if (effectiveImageProfile != null && (effectiveImageProfile.apiKey.isNotBlank() || effectiveImageProfile.providerType == "CUSTOM")) {
                    try {
                        val imgRes = generateImage(
                            profile = effectiveImageProfile,
                            prompt = panel.imagePrompt,
                            negativePrompt = negPrompt,
                            resolution = "832x1216"
                        )
                        if (imgRes.isSuccess) {
                            generatedUri = imgRes.getOrDefault("")
                        }
                    } catch (_: Exception) {}
                }

                if (generatedUri.isBlank()) {
                    // Fallback to free high-quality Pollinations.ai image generator so the comic always has pictures
                    try {
                        val encodedPrompt = java.net.URLEncoder.encode(panel.imagePrompt, "UTF-8")
                        val seed = (10000..999999).random()
                        generatedUri = "https://image.pollinations.ai/prompt/$encodedPrompt?width=768&height=1024&nologo=true&seed=$seed"
                    } catch (_: Exception) {}
                }

                panel.imageUri = generatedUri
            }

            onPanelProgress(panelList.size, panelList.size, "Комикс готов!")
            Result.success(panelList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // DEEP JSON CUSTOMIZATION, THINKING MODE & PREVIEW HELPERS
    // =========================================================================

    fun applyOpenAiModelCustomizations(
        bodyJson: JSONObject,
        profile: AiProviderProfileEntity,
        modelName: String,
        systemPrompt: String,
        userPrompt: String,
        isStream: Boolean
    ) {
        val lowerModel = modelName.lowercase()
        val isOpenRouter = profile.providerType.equals("OPENROUTER", ignoreCase = true) ||
            profile.baseUrl.contains("openrouter", ignoreCase = true)

        // Extended sampling parameters
        if (profile.topK > 0) bodyJson.put("top_k", profile.topK)
        if (profile.frequencyPenalty != 0f) bodyJson.put("frequency_penalty", profile.frequencyPenalty.toDouble())
        if (profile.presencePenalty != 0f) bodyJson.put("presence_penalty", profile.presencePenalty.toDouble())
        if (profile.repetitionPenalty != 1f && profile.repetitionPenalty > 0f) {
            bodyJson.put("repetition_penalty", profile.repetitionPenalty.toDouble())
        }
        if (profile.minP > 0f) bodyJson.put("min_p", profile.minP.toDouble())
        if (profile.seed != null) bodyJson.put("seed", profile.seed)

        if (profile.stopSequences.isNotBlank()) {
            val stops = profile.stopSequences.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (stops.isNotEmpty()) {
                val arr = JSONArray()
                stops.forEach { arr.put(it) }
                bodyJson.put("stop", arr)
            }
        }

        when (profile.responseFormatMode) {
            "json_object" -> bodyJson.put("response_format", JSONObject().put("type", "json_object"))
            "text" -> bodyJson.put("response_format", JSONObject().put("type", "text"))
        }

        // Thinking / Reasoning Mode Control
        when (profile.thinkingMode) {
            "disabled" -> {
                if (isOpenRouter) {
                    bodyJson.put("include_reasoning", false)
                    bodyJson.put(
                        "reasoning",
                        JSONObject().put("exclude", true).put("enabled", false)
                    )
                } else if (lowerModel.contains("qwen") || lowerModel.contains("qwq") || lowerModel.contains("glm")) {
                    bodyJson.put("enable_thinking", false)
                    bodyJson.put("chat_template_kwargs", JSONObject().put("enable_thinking", false))
                } else if (
                    lowerModel.startsWith("o1") || lowerModel.startsWith("o3") || lowerModel.startsWith("o4") ||
                    lowerModel.contains("grok-3-mini") || lowerModel.contains("gpt-oss") || lowerModel.contains("r1")
                ) {
                    bodyJson.put("reasoning_effort", "low")
                }
            }
            "low", "medium", "high" -> {
                if (isOpenRouter) {
                    val rObj = JSONObject().put("effort", profile.thinkingMode).put("exclude", false)
                    if (profile.thinkingBudget > 0) rObj.put("max_tokens", profile.thinkingBudget)
                    bodyJson.put("reasoning", rObj)
                    bodyJson.put("include_reasoning", true)
                } else {
                    bodyJson.put("reasoning_effort", profile.thinkingMode)
                    if (lowerModel.contains("qwen") || lowerModel.contains("qwq")) {
                        bodyJson.put("enable_thinking", true)
                    }
                }
            }
            else -> {
                if (profile.thinkingBudget > 0 && isOpenRouter) {
                    bodyJson.put("reasoning", JSONObject().put("max_tokens", profile.thinkingBudget))
                }
            }
        }

        // Full raw JSON template override if user provided one
        if (profile.rawJsonTemplate.isNotBlank()) {
            applyRawTemplate(
                target = bodyJson,
                rawTemplate = profile.rawJsonTemplate,
                profile = profile,
                modelName = modelName,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                isStream = isStream
            )
        }

        // Line-by-line extraBodyJson deep merge (can add, modify, or delete any key via null)
        if (profile.extraBodyJson.isNotBlank() && profile.extraBodyJson.trim() != "{}") {
            applyJsonOverrideOrMerge(bodyJson, profile.extraBodyJson)
        }
    }

    fun applyGeminiModelCustomizations(
        bodyJson: JSONObject,
        genConfig: JSONObject,
        profile: AiProviderProfileEntity,
        systemPrompt: String,
        userPrompt: String
    ) {
        if (profile.topK > 0) genConfig.put("topK", profile.topK)
        if (profile.frequencyPenalty != 0f) genConfig.put("frequencyPenalty", profile.frequencyPenalty.toDouble())
        if (profile.presencePenalty != 0f) genConfig.put("presencePenalty", profile.presencePenalty.toDouble())
        if (profile.seed != null) genConfig.put("seed", profile.seed)

        if (profile.stopSequences.isNotBlank()) {
            val stops = profile.stopSequences.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (stops.isNotEmpty()) {
                val arr = JSONArray()
                stops.forEach { arr.put(it) }
                genConfig.put("stopSequences", arr)
            }
        }

        if (profile.responseFormatMode == "json_object") {
            genConfig.put("responseMimeType", "application/json")
        } else if (profile.responseFormatMode == "text") {
            genConfig.put("responseMimeType", "text/plain")
        }

        // Gemini 2.5 Flash / Pro Thinking Mode control
        when (profile.thinkingMode) {
            "disabled" -> {
                genConfig.put(
                    "thinkingConfig",
                    JSONObject().put("thinkingBudget", 0).put("includeThoughts", false)
                )
            }
            "low" -> {
                val budget = if (profile.thinkingBudget > 0) profile.thinkingBudget else 1024
                genConfig.put("thinkingConfig", JSONObject().put("thinkingBudget", budget))
            }
            "medium" -> {
                val budget = if (profile.thinkingBudget > 0) profile.thinkingBudget else 4096
                genConfig.put("thinkingConfig", JSONObject().put("thinkingBudget", budget))
            }
            "high" -> {
                val budget = if (profile.thinkingBudget > 0) profile.thinkingBudget else 16384
                genConfig.put("thinkingConfig", JSONObject().put("thinkingBudget", budget))
            }
            else -> {
                if (profile.thinkingBudget > 0) {
                    genConfig.put("thinkingConfig", JSONObject().put("thinkingBudget", profile.thinkingBudget))
                }
            }
        }

        if (profile.rawJsonTemplate.isNotBlank()) {
            applyRawTemplate(
                target = bodyJson,
                rawTemplate = profile.rawJsonTemplate,
                profile = profile,
                modelName = profile.model.ifBlank { "gemini-3.5-flash-lite" },
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                isStream = false
            )
        }

        if (profile.extraBodyJson.isNotBlank() && profile.extraBodyJson.trim() != "{}") {
            applyJsonOverrideOrMerge(bodyJson, profile.extraBodyJson)
        }
    }

    fun applyAnthropicModelCustomizations(
        bodyJson: JSONObject,
        profile: AiProviderProfileEntity,
        systemPrompt: String,
        userPrompt: String
    ) {
        if (profile.topP < 1f) bodyJson.put("top_p", profile.topP.toDouble())
        if (profile.topK > 0) bodyJson.put("top_k", profile.topK)

        if (profile.stopSequences.isNotBlank()) {
            val stops = profile.stopSequences.split(',', '\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (stops.isNotEmpty()) {
                val arr = JSONArray()
                stops.forEach { arr.put(it) }
                bodyJson.put("stop_sequences", arr)
            }
        }

        when (profile.thinkingMode) {
            "disabled" -> {
                bodyJson.put("thinking", JSONObject().put("type", "disabled"))
            }
            "low", "medium", "high" -> {
                val defaultBudget = when (profile.thinkingMode) {
                    "low" -> 1024
                    "medium" -> 4000
                    else -> 10000
                }
                val budget = if (profile.thinkingBudget >= 1024) profile.thinkingBudget else defaultBudget
                bodyJson.put("thinking", JSONObject().put("type", "enabled").put("budget_tokens", budget))
            }
        }

        if (profile.rawJsonTemplate.isNotBlank()) {
            applyRawTemplate(
                target = bodyJson,
                rawTemplate = profile.rawJsonTemplate,
                profile = profile,
                modelName = profile.model.ifBlank { "claude-3-5-haiku-20241022" },
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                isStream = false
            )
        }

        if (profile.extraBodyJson.isNotBlank() && profile.extraBodyJson.trim() != "{}") {
            applyJsonOverrideOrMerge(bodyJson, profile.extraBodyJson)
        }
    }

    private fun applyRawTemplate(
        target: JSONObject,
        rawTemplate: String,
        profile: AiProviderProfileEntity,
        modelName: String,
        systemPrompt: String,
        userPrompt: String,
        isStream: Boolean
    ) {
        try {
            val existingMessages = target.optJSONArray("messages")?.toString()
                ?: target.optJSONArray("contents")?.toString()
                ?: "[]"
            val escapedSys = JSONObject.quote(systemPrompt).removeSurrounding("\"")
            val escapedUser = JSONObject.quote(userPrompt).removeSurrounding("\"")

            val rendered = rawTemplate
                .replace("{{MODEL}}", modelName)
                .replace("{{MESSAGES_JSON}}", existingMessages)
                .replace("{{SYSTEM_PROMPT}}", escapedSys)
                .replace("{{USER_PROMPT}}", escapedUser)
                .replace("{{TEMPERATURE}}", profile.temperature.toString())
                .replace("{{TOP_P}}", profile.topP.toString())
                .replace("{{MAX_TOKENS}}", profile.maxTokens.toString())
                .replace("{{STREAM}}", isStream.toString())

            val parsed = JSONObject(rendered)
            val existingKeys = target.keys().asSequence().toList()
            existingKeys.forEach { target.remove(it) }
            val newKeys = parsed.keys()
            while (newKeys.hasNext()) {
                val k = newKeys.next()
                target.put(k, parsed.get(k))
            }
        } catch (_: Exception) {}
    }

    fun applyJsonOverrideOrMerge(target: JSONObject, rawJson: String) {
        try {
            val patch = JSONObject(rawJson.trim())
            deepMergeJson(target, patch)
        } catch (_: Exception) {}
    }

    private fun deepMergeJson(target: JSONObject, source: JSONObject) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = source.get(key)
            if (value == JSONObject.NULL) {
                target.remove(key)
            } else if (value is JSONObject && target.optJSONObject(key) != null) {
                deepMergeJson(target.getJSONObject(key), value)
            } else {
                target.put(key, value)
            }
        }
    }

    private fun applyExtraHeaders(requestBuilder: Request.Builder, extraHeadersJson: String) {
        if (extraHeadersJson.isBlank() || extraHeadersJson.trim() == "{}") return
        try {
            val obj = JSONObject(extraHeadersJson)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k, "")
                if (k.isNotBlank() && v.isNotBlank()) {
                    requestBuilder.header(k.trim(), v.trim())
                }
            }
        } catch (_: Exception) {}
    }

    private fun stripThinkingIfDisabled(rawText: String, profile: AiProviderProfileEntity): String {
        if (profile.thinkingMode != "disabled") return rawText
        return rawText
            .replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<thinking>[\\s\\S]*?</thinking>", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    /**
     * Builds a formatted JSON preview of the exact request payload that will be sent for a profile.
     */
    fun buildLiveRequestPreview(profile: AiProviderProfileEntity, forTest: Boolean = false): String {
        return try {
            val sysText = if (forTest) {
                profile.testSystemPrompt.ifBlank { "Ты тестовый помощник. Отвечай кратко." }
            } else {
                profile.systemPromptPrefix.ifBlank { "{{SYSTEM_PROMPT}}" }
            }
            val userText = if (forTest) {
                profile.testPrompt.ifBlank { "Ответь одним словом: 'Готов'" }
            } else {
                "{{USER_MESSAGE}}"
            }
            val tokens = if (forTest) profile.testMaxTokens.coerceAtLeast(1) else profile.maxTokens

            val json = when (profile.providerType.uppercase()) {
                "GEMINI" -> {
                    val genConfig = JSONObject().apply {
                        put("temperature", profile.temperature.toDouble())
                        put("topP", profile.topP.toDouble())
                        put("maxOutputTokens", tokens)
                    }
                    val root = JSONObject().apply {
                        put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sysText))))
                        put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", userText)))))
                        put("generationConfig", genConfig)
                    }
                    applyGeminiModelCustomizations(root, genConfig, profile, sysText, userText)
                    if (forTest && profile.testCustomBodyJson.isNotBlank()) {
                        applyJsonOverrideOrMerge(root, profile.testCustomBodyJson)
                    }
                    root
                }
                "ANTHROPIC" -> {
                    val root = JSONObject().apply {
                        put("model", profile.model.ifBlank { "claude-3-5-haiku-20241022" })
                        put("system", sysText)
                        put("max_tokens", tokens)
                        put("temperature", profile.temperature.toDouble())
                        put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", userText)))
                    }
                    applyAnthropicModelCustomizations(root, profile, sysText, userText)
                    if (forTest && profile.testCustomBodyJson.isNotBlank()) {
                        applyJsonOverrideOrMerge(root, profile.testCustomBodyJson)
                    }
                    root
                }
                else -> {
                    val root = JSONObject().apply {
                        put("model", profile.model.ifBlank { "gpt-4o-mini" })
                        put("messages", JSONArray().apply {
                            put(JSONObject().put("role", "system").put("content", sysText))
                            put(JSONObject().put("role", "user").put("content", userText))
                        })
                        put("temperature", profile.temperature.toDouble())
                        put("top_p", profile.topP.toDouble())
                        put("max_tokens", tokens)
                        if (forTest && profile.testStream) put("stream", true)
                    }
                    applyOpenAiModelCustomizations(
                        bodyJson = root,
                        profile = profile,
                        modelName = profile.model.ifBlank { "gpt-4o-mini" },
                        systemPrompt = sysText,
                        userPrompt = userText,
                        isStream = forTest && profile.testStream
                    )
                    if (forTest && profile.testCustomBodyJson.isNotBlank()) {
                        applyJsonOverrideOrMerge(root, profile.testCustomBodyJson)
                    }
                    root
                }
            }
            json.toString(2)
        } catch (e: Exception) {
            "{\n  \"error\": \"Некорректный синтаксис JSON: ${e.message}\"\n}"
        }
    }
}

data class ComicPanelData(
    val panelIndex: Int,
    val speakerName: String,
    val dialogueText: String,
    val soundEffect: String,
    val sceneDescription: String,
    val imagePrompt: String,
    var imageUri: String = "",
    var isGenerating: Boolean = false
)

