package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarUri: String,
    val shortDescription: String = "",
    val personality: String = "",
    val appearance: String = "",
    val referenceImages: String = "[]", // JSON array of URIs / paths
    val habits: String = "",
    val likes: String = "",
    val dislikes: String = "",
    val greetingMessage: String = "",
    val age: Int = 18,
    val birthDate: String = "",
    val knowledgeCards: String = "[]", // JSON array of {title, content}
    val tags: String = "[]", // JSON array of strings
    val voiceStyle: String = "natural", // formal, cheeky, shy, dramatic, gentle, cold, playful
    val isAdult: Boolean = true, // age >= 18 hardware-enforced
    val startingRelationship: String = "strangers", // strangers, acquaintances, friends, close_friends, romantic, custom
    val customRelationship: String = "",
    val initialRelationshipTitle: String = "", // Заголовок для таблицы отношений при первом заходе в бота
    val initialRelationshipPercent: Int = -1, // Процент (0-100) для таблицы отношений при первом заходе (-1 = по пресету)
    val bannerUri: String = "", // Картинка-баннер для шапки персонажа в профиле и хранилище
    val communicationFormality: String = "adaptive", // adaptive, informal_ty, formal_vy, distant, flirty, submissive, dominant
    val avatarFrameUri: String = "", // Картинка рамки с прозрачным фоном для аватарки
    val greetingBannerUri: String = "", // Картинка-баннер для приветственного поста / сообщения
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isHidden: Boolean = false,
    val lastInteractedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val author: String = "UntitledAI",
    val relationshipBoardJson: String = "" // JSON графа Доски отношений (БЕТА)
) {
    val by: String get() = author

    fun getParsedRelationshipBoard(playerName: String = "Игрок"): com.example.util.RelationshipBoardState {
        return com.example.util.RelationshipBoardHelper.parseBoard(
            json = relationshipBoardJson,
            mainCharName = name,
            playerName = playerName,
            startingRelationship = startingRelationship,
            customRelationship = customRelationship,
            initialTitle = initialRelationshipTitle,
            initialPercent = if (initialRelationshipPercent != -1) initialRelationshipPercent else -999
        )
    }

    fun getEffectiveInitialRelTitle(isEn: Boolean = false): String {
        if (relationshipBoardJson.isNotBlank()) {
            val board = getParsedRelationshipBoard()
            if (board.edges.isNotEmpty()) {
                return board.calculateOverallTitle(initialRelationshipTitle.trim())
            }
        }
        if (initialRelationshipTitle.isNotBlank()) return initialRelationshipTitle.trim()
        if (customRelationship.isNotBlank()) return customRelationship.trim()
        return when (startingRelationship) {
            "romantic" -> if (isEn) "Romantic Attraction" else "Любовь"
            "close_friends" -> if (isEn) "Close Friendship" else "Друзья"
            "friends" -> if (isEn) "Friendly Bond" else "Друзья"
            "acquaintances" -> if (isEn) "Acquaintances" else "Симпатия"
            "enemies" -> if (isEn) "Enemies" else "Враги"
            else -> if (isEn) "First Impression" else "Знакомые"
        }
    }

    fun getEffectiveInitialRelPercent(): Int {
        if (relationshipBoardJson.isNotBlank()) {
            val board = getParsedRelationshipBoard()
            if (board.edges.isNotEmpty()) {
                return board.calculateAverageScore()
            }
        }
        if (initialRelationshipPercent != -1 && initialRelationshipPercent in -100..100) return initialRelationshipPercent
        return when (startingRelationship) {
            "romantic" -> 90
            "close_friends" -> 60
            "friends" -> 60
            "acquaintances" -> 40
            "enemies" -> -80
            else -> 0
        }
    }

    val isAssistant: Boolean
        get() = tags.contains("assistant", ignoreCase = true) ||
                tags.contains("Ассистент", ignoreCase = true) ||
                tags.contains("Помощь", ignoreCase = true) ||
                name.equals("PersonaAI Ассистент", ignoreCase = true) ||
                name.equals("PersonaAI Assistant", ignoreCase = true) ||
                name.equals("Untitled Ассистент", ignoreCase = true) ||
                name.contains("Ассистент", ignoreCase = true) ||
                name.contains("Assistant", ignoreCase = true)

    fun getDisplayName(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        if (isAssistant) {
            return when {
                isFemboy -> "Untitled Ассистент (ня~)"
                isEn -> "Untitled Assistant"
                else -> "Untitled Ассистент"
            }
        }
        return name
    }

    fun getDisplayName(isEn: Boolean): String = getDisplayName(if (isEn) "en" else "ru")

    fun getDisplayDescription(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        if (isAssistant) {
            return when {
                isFemboy -> "Официальный ИИ-помощник движка Untitled (ня~). Подскажет настроечки, исправит ошибочки, поможет с промптами!"
                isEn -> "Official Untitled built-in AI assistant and technical advisor. Helps configure models, solve errors, and refine roleplay prompts."
                else -> "Официальный ИИ-помощник движка Untitled. Подскажет настройки, исправит ошибки, поможет с промптами."
            }
        }
        return shortDescription
    }

    fun getDisplayDescription(isEn: Boolean): String = getDisplayDescription(if (isEn) "en" else "ru")

    fun getDisplayGreeting(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        if (isAssistant) {
            return when {
                isFemboy -> "Приветик! Я твой ассистент по движку Untitled (ня~).\n\nЯ помогу настроить любые ИИ-провайдеры (OpenRouter, Groq, Gemini), подобрать классные параметры модельки, составить карточку фембойчика или масочку пользователя, а также подскажу решение любых ошибочек.\n\nЗадавай любые вопросики по приложению или настроечкам! (uwu)"
                isEn -> "Greetings! I am the Untitled Assistant, your built-in advisor for Untitled.\n\nI will help you configure AI providers (OpenRouter, Groq, Gemini), select optimal model parameters, create character cards or user personas, and guide you through resolving any errors.\n\nFeel free to ask any questions about the app or settings!"
                else -> greetingMessage
            }
        }
        return greetingMessage
    }

    fun getDisplayGreeting(isEn: Boolean): String = getDisplayGreeting(if (isEn) "en" else "ru")
}
