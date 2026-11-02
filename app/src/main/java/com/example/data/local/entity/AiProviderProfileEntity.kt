package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_provider_profiles")
data class AiProviderProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val providerType: String = "OPENAI_COMPATIBLE", // OPENAI_COMPATIBLE, GEMINI, ANTHROPIC, OPENROUTER, GROK, MISTRAL, CIVITAI, POLLINATIONS, CUSTOM
    val baseUrl: String = "https://openrouter.ai/api/v1",
    val apiKey: String = "",
    val model: String = "google/gemini-2.0-flash-lite-preview-02-05:free",
    val visionModel: String = "google/gemini-2.0-flash-001",
    val imageModel: String = "",
    val temperature: Float = 0.85f,
    val topP: Float = 0.95f,
    val maxTokens: Int = 1500,
    val systemPromptTemplate: String = "",
    val systemPromptPrefix: String = "",
    val extraHeadersJson: String = "{}",
    val isDefaultText: Boolean = false,
    val isDefaultImage: Boolean = false,
    val isImageProvider: Boolean = false,
    val negativePrompt: String = "ugly, deformed, blurry, bad anatomy, extra limbs, low quality, watermark",
    val imageResolution: String = "1024x1024",
    val seedMode: String = "random",
    val fixedSeed: Long = 42L,
    // Deep JSON & Model Behavior overrides (v20)
    val thinkingMode: String = "auto", // "auto", "disabled", "low", "medium", "high"
    val thinkingBudget: Int = 0, // 0 = auto/disabled, >0 = explicit token budget
    val topK: Int = 0, // 0 = not sent, >0 = explicit top_k
    val frequencyPenalty: Float = 0f,
    val presencePenalty: Float = 0f,
    val repetitionPenalty: Float = 1f,
    val minP: Float = 0f,
    val seed: Int? = null,
    val stopSequences: String = "", // comma or newline separated
    val responseFormatMode: String = "default", // "default", "text", "json_object"
    val extraBodyJson: String = "{}", // Deep-merged into the final request JSON (can override or delete keys via null)
    val rawJsonTemplate: String = "", // If non-blank, full raw JSON body template with placeholders ({{MODEL}}, {{MESSAGES_JSON}}, {{SYSTEM_PROMPT}}, {{USER_PROMPT}}, {{TEMPERATURE}}, {{MAX_TOKENS}}, {{TOP_P}})
    // Customizable Provider Test behavior
    val testPrompt: String = "Привет! Ответь одним коротким предложением, что связь работает.",
    val testSystemPrompt: String = "You are a helpful assistant.",
    val testMaxTokens: Int = 128,
    val testStream: Boolean = false,
    val testCustomBodyJson: String = "" // Optional raw or extra JSON specifically for connection testing
)
