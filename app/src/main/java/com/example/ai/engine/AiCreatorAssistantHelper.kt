package com.example.ai.engine

import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.UserPersonaEntity
import com.example.util.RelationshipBoardHelper
import org.json.JSONArray
import org.json.JSONObject

data class GeneratedBotDraft(
    val assistantMessage: String = "Готово! Я заполнил поля бота.",
    val name: String? = null,
    val age: Int? = null,
    val shortDescription: String? = null,
    val personality: String? = null,
    val appearance: String? = null,
    val habits: String? = null,
    val likes: String? = null,
    val dislikes: String? = null,
    val greetingMessage: String? = null,
    val tags: List<String>? = null,
    val voiceStyle: String? = null,
    val communicationFormality: String? = null,
    val startingRelationship: String? = null,
    val customRelationship: String? = null,
    val initialRelationshipTitle: String? = null,
    val knowledgeCards: List<Pair<String, String>>? = null
) {
    fun toCharacterEntity(
        base: CharacterEntity? = null,
        playerName: String = "Игрок"
    ): CharacterEntity {
        val current = base ?: CharacterEntity(
            name = "Новый персонаж",
            avatarUri = "drawable://app_logo_black"
        )
        val finalName = name?.trim()?.takeIf { it.isNotBlank() } ?: current.name
        val finalAge = age?.coerceIn(1, 9999) ?: current.age
        val finalShortDesc = shortDescription?.trim()?.takeIf { it.isNotBlank() } ?: current.shortDescription
        val finalPersonality = personality?.trim()?.takeIf { it.isNotBlank() } ?: current.personality
        val finalAppearance = appearance?.trim()?.takeIf { it.isNotBlank() } ?: current.appearance
        val finalHabits = habits?.trim()?.takeIf { it.isNotBlank() } ?: current.habits
        val finalLikes = likes?.trim()?.takeIf { it.isNotBlank() } ?: current.likes
        val finalDislikes = dislikes?.trim()?.takeIf { it.isNotBlank() } ?: current.dislikes
        val finalGreeting = greetingMessage?.trim()?.takeIf { it.isNotBlank() } ?: current.greetingMessage

        val finalTagsJson = if (!tags.isNullOrEmpty()) {
            val arr = JSONArray()
            tags.map { it.trim() }.filter { it.isNotBlank() }.forEach { arr.put(it) }
            arr.toString()
        } else {
            current.tags
        }

        val finalCardsJson = if (!knowledgeCards.isNullOrEmpty()) {
            val arr = JSONArray()
            knowledgeCards.forEach { (title, content) ->
                if (title.isNotBlank() || content.isNotBlank()) {
                    val obj = JSONObject()
                    obj.put("title", title.trim().ifBlank { "Заметка" })
                    obj.put("content", content.trim())
                    arr.put(obj)
                }
            }
            arr.toString()
        } else {
            current.knowledgeCards
        }

        val validVoiceStyles = setOf("natural", "cheeky", "formal", "shy", "gentle", "cold", "playful")
        val finalVoiceStyle = voiceStyle?.trim()?.lowercase()?.takeIf { it in validVoiceStyles } ?: current.voiceStyle

        val validFormalities = setOf("adaptive", "informal", "formal")
        val finalFormality = communicationFormality?.trim()?.lowercase()?.takeIf { it in validFormalities } ?: current.communicationFormality

        val validRels = setOf("strangers", "friends", "lovers", "enemies", "rivals", "colleagues", "custom")
        val finalStartRel = startingRelationship?.trim()?.lowercase()?.takeIf { it in validRels } ?: current.startingRelationship
        val finalCustomRel = customRelationship?.trim() ?: current.customRelationship
        val finalRelTitle = initialRelationshipTitle?.trim()?.takeIf { it.isNotBlank() } ?: current.initialRelationshipTitle

        val boardState = RelationshipBoardHelper.parseBoard(
            json = current.relationshipBoardJson,
            mainCharName = finalName.ifBlank { "Персонаж 1" },
            playerName = playerName.ifBlank { "Игрок" },
            startingRelationship = finalStartRel,
            customRelationship = finalCustomRel,
            initialTitle = finalRelTitle,
            initialPercent = current.initialRelationshipPercent
        )

        return current.copy(
            name = finalName,
            age = finalAge,
            shortDescription = finalShortDesc,
            personality = finalPersonality,
            appearance = finalAppearance,
            habits = finalHabits,
            likes = finalLikes,
            dislikes = finalDislikes,
            greetingMessage = finalGreeting,
            tags = finalTagsJson,
            knowledgeCards = finalCardsJson,
            voiceStyle = finalVoiceStyle,
            communicationFormality = finalFormality,
            startingRelationship = finalStartRel,
            customRelationship = finalCustomRel,
            initialRelationshipTitle = finalRelTitle,
            relationshipBoardJson = boardState.toJson()
        )
    }
}

data class GeneratedPersonaDraft(
    val assistantMessage: String = "Готово! Я создал и заполнил персону.",
    val name: String = "",
    val age: Int = 20,
    val gender: String = "",
    val shortDescription: String = "",
    val personality: String = "",
    val appearance: String = "",
    val habits: String = "",
    val likes: String = "",
    val dislikes: String = ""
) {
    fun toPersonaEntity(base: UserPersonaEntity? = null): UserPersonaEntity {
        val current = base ?: UserPersonaEntity(
            name = "Игрок",
            avatarUri = "drawable://app_logo_white",
            isSelected = true
        )
        return current.copy(
            name = name.trim().ifBlank { current.name },
            age = age.coerceIn(1, 9999),
            gender = gender.trim().ifBlank { current.gender },
            shortDescription = shortDescription.trim().ifBlank { current.shortDescription },
            personality = personality.trim().ifBlank { current.personality },
            appearance = appearance.trim().ifBlank { current.appearance },
            habits = habits.trim().ifBlank { current.habits },
            likes = likes.trim().ifBlank { current.likes },
            dislikes = dislikes.trim().ifBlank { current.dislikes },
            isSelected = true
        )
    }
}

object AiCreatorAssistantHelper {

    fun buildCreateBotSystemPrompt(): String {
        return """
Ты — профессиональный сценарист и дизайнер ролевых персонажей для движка UntitledAI.
Пользователь описывает свои пожелания к боту (персонажу или сюжетному сценарию).
Твоя задача — создать детального, живого, харизматичного персонажа и вернуть результат СТРОГО в формате валидного JSON-объекта (без лишнего текста вокруг JSON или внутри блока ```json).

Структура JSON:
{
  "assistantMessage": "Краткий дружелюбный комментарий от тебя пользователю (1-2 предложения), что персонаж создан и поля заполнены",
  "name": "Имя персонажа (или название сценария)",
  "age": 22,
  "shortDescription": "Краткое описание персонажа, его роли и завязки (1-3 предложения)",
  "personality": "Подробное описание характера, психологии, темперамента, манеры поведения, сильных и слабых сторон, секретов и мотивации",
  "appearance": "Детальное описание внешности, роста, телосложения, прически, глаз, одежды и особых примет",
  "habits": "Привычки, жесты, манера речи, характерные фразы или тики",
  "likes": "Что любит персонаж (через запятую или списком)",
  "dislikes": "Что не любит или ненавидит персонаж",
  "greetingMessage": "Атмосферное первое сообщение (приветствие) для старта ролевой игры. Опиши сцену и действия в *звездочках*, а прямую речь персонажа выдели с новой строки через — или кавычки",
  "tags": ["Тег1", "Тег2", "Тег3"],
  "voiceStyle": "Один из вариантов: natural, cheeky, formal, shy, gentle, cold, playful",
  "communicationFormality": "Один из вариантов: adaptive, informal, formal",
  "startingRelationship": "Один из вариантов: strangers, friends, lovers, enemies, rivals, colleagues, custom",
  "customRelationship": "Если выбран custom — опиши тип отношений, иначе пустая строка",
  "initialRelationshipTitle": "Статус отношений на старте (например: Незнакомцы, Соперники, Давние друзья)",
  "knowledgeCards": [
    {
      "title": "Название карточки лора / мира / предыстории",
      "content": "Подробное содержание лора, важные факты о мире или прошлом персонажа"
    }
  ]
}

Правила:
1. Пиши на русском языке, литературно, выразительно и без шаблонных клише.
2. Поле "greetingMessage" должно сразу погружать игрока в интересную сцену с участием персонажа.
3. Возвращай ТОЛЬКО валидный JSON.
        """.trimIndent()
    }

    fun buildAssistBotInEditorSystemPrompt(): String {
        return """
Ты — встроенный ИИ-помощник редактора персонажей UntitledAI («Помоги!»).
У пользователя уже открыт редактор бота с текущими заполненными (или частично пустыми) полями.
Пользователь пишет просьбу: например, «допиши характер и внешность», «придумай крутое приветствие», «добавь карточки лора», «сделай его более дерзким» или «заполни пустые поля».

Твоя задача — выполнить просьбу пользователя и вернуть JSON-объект, в котором указаны ТОЛЬКО те поля, которые нужно добавить или обновить (поля, которые менять не нужно, оставь null или не включай в JSON, либо верни обновлёнными).

Структура JSON:
{
  "assistantMessage": "Твой ответ пользователю в окне помощника: кратко расскажи, какие поля ты дописал или обновил",
  "name": "Новое имя (или null, если менять не нужно)",
  "age": null,
  "shortDescription": "Обновленное краткое описание (или null)",
  "personality": "Дополненный/новый характер (или null)",
  "appearance": "Дополненная/новая внешность (или null)",
  "habits": "Привычки и манера речи (или null)",
  "likes": "Что любит (или null)",
  "dislikes": "Что не любит (или null)",
  "greetingMessage": "Новое или улучшенное приветствие (или null)",
  "tags": ["Тег1", "Тег2"],
  "voiceStyle": "natural | cheeky | formal | shy | gentle | cold | playful (или null)",
  "startingRelationship": "strangers | friends | lovers | enemies | rivals | colleagues | custom (или null)",
  "customRelationship": "Текст кастомных отношений (или null)",
  "initialRelationshipTitle": "Название отношений (или null)",
  "knowledgeCards": [
    {
      "title": "Заголовок карточки лора",
      "content": "Текст карточки лора"
    }
  ]
}

Правила:
1. Обязательно заполняй "assistantMessage" дружелюбным и полезным ответом на русском языке.
2. Все изменённые или дописанные поля из JSON будут АВТОМАТИЧЕСКИ вставлены в поля редактора бота.
3. Если пользователь просит «допиши бота» или «заполни пустые поля», обязательно сгенерируй качественный текст для всех пустых или коротких полей (shortDescription, personality, appearance, habits, likes, dislikes, greetingMessage, tags, knowledgeCards).
4. Возвращай ТОЛЬКО валидный JSON.
        """.trimIndent()
    }

    fun buildAssistBotInEditorUserPrompt(
        name: String,
        ageText: String,
        shortDescription: String,
        personality: String,
        appearance: String,
        habits: String,
        likes: String,
        dislikes: String,
        greetingMessage: String,
        tagsText: String,
        knowledgeCards: List<Pair<String, String>>,
        userRequest: String
    ): String {
        val cardsSummary = if (knowledgeCards.isEmpty()) "Пусто" else {
            knowledgeCards.joinToString("\n") { "- ${it.first}: ${it.second}" }
        }
        return """
ТЕКУЩИЕ ПОЛЯ БОТА В РЕДАКТОРЕ:
- Имя: ${name.ifBlank { "(пусто)" }}
- Возраст: ${ageText.ifBlank { "(пусто)" }}
- Краткое описание: ${shortDescription.ifBlank { "(пусто)" }}
- Характер и личность: ${personality.ifBlank { "(пусто)" }}
- Внешность: ${appearance.ifBlank { "(пусто)" }}
- Привычки: ${habits.ifBlank { "(пусто)" }}
- Любит: ${likes.ifBlank { "(пусто)" }}
- Не любит: ${dislikes.ifBlank { "(пусто)" }}
- Приветствие: ${greetingMessage.ifBlank { "(пусто)" }}
- Теги: ${tagsText.ifBlank { "(пусто)" }}
- Карточки лора:
$cardsSummary

ЗАПРОС ПОЛЬЗОВАТЕЛЯ:
$userRequest
        """.trimIndent()
    }

    fun buildCreatePersonaSystemPrompt(): String {
        return """
Ты — профессиональный сценарист ролевых игр для движка UntitledAI.
Пользователь описывает свои пожелания к своей ролевой Персоне (персонажу игрока).
Твоя задача — создать яркую, детальную и удобную для отыгрыша анкету Персоны игрока и вернуть результат СТРОГО в формате валидного JSON-объекта.

Структура JSON:
{
  "assistantMessage": "Краткий дружелюбный комментарий (1-2 предложения), что персона создана и автоматически сохранена в профиле",
  "name": "Имя персоны игрока",
  "age": 21,
  "gender": "Пол (например: Мужской, Женский или Другой)",
  "shortDescription": "Краткая суть роли и биографии игрока (1-3 предложения)",
  "personality": "Характер, темперамент, манера поведения и особенности личности игрока",
  "appearance": "Подробное описание внешности, роста, прически, глаз, стиля одежды",
  "habits": "Привычки, жесты, манера речи, особые черты",
  "likes": "Что любит персона (через запятую)",
  "dislikes": "Что не любит персона (через запятую)"
}

Правила:
1. Пиши на русском языке, выразительно и структурированно.
2. Возвращай ТОЛЬКО валидный JSON.
        """.trimIndent()
    }

    private fun extractJsonObjectString(raw: String): String {
        val trimmed = raw.trim()
        val fenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val fenced = fenceRegex.find(trimmed)?.groupValues?.getOrNull(1)?.trim()
        val candidate = if (!fenced.isNullOrBlank()) fenced else trimmed

        val firstBrace = candidate.indexOf('{')
        val lastBrace = candidate.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return candidate.substring(firstBrace, lastBrace + 1)
        }
        return candidate
    }

    private fun optNonBlankString(obj: JSONObject, key: String): String? {
        if (!obj.has(key) || obj.isNull(key)) return null
        val v = obj.optString(key, "").trim()
        return if (v.isNotBlank() && !v.equals("null", ignoreCase = true)) v else null
    }

    fun parseBotDraft(rawResponse: String): Result<GeneratedBotDraft> {
        return try {
            val jsonStr = extractJsonObjectString(rawResponse)
            val obj = JSONObject(jsonStr)

            val tagsList = mutableListOf<String>()
            val tagsNode = obj.opt("tags")
            when (tagsNode) {
                is JSONArray -> {
                    for (i in 0 until tagsNode.length()) {
                        val t = tagsNode.optString(i).trim()
                        if (t.isNotBlank()) tagsList.add(t)
                    }
                }
                is String -> {
                    tagsNode.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tagsList.add(it) }
                }
            }

            val cardsList = mutableListOf<Pair<String, String>>()
            val cardsArr = obj.optJSONArray("knowledgeCards")
            if (cardsArr != null) {
                for (i in 0 until cardsArr.length()) {
                    val cardObj = cardsArr.optJSONObject(i) ?: continue
                    val title = cardObj.optString("title", "").trim()
                    val content = cardObj.optString("content", "").trim()
                    if (title.isNotBlank() || content.isNotBlank()) {
                        cardsList.add(title.ifBlank { "Лор ${i + 1}" } to content)
                    }
                }
            }

            val parsedAge = if (obj.has("age") && !obj.isNull("age")) {
                val a = obj.optInt("age", -1)
                if (a > 0) a else null
            } else null

            Result.success(
                GeneratedBotDraft(
                    assistantMessage = optNonBlankString(obj, "assistantMessage")
                        ?: "Готово! Я заполнил поля бота в соответствии с твоими пожеланиями.",
                    name = optNonBlankString(obj, "name"),
                    age = parsedAge,
                    shortDescription = optNonBlankString(obj, "shortDescription"),
                    personality = optNonBlankString(obj, "personality"),
                    appearance = optNonBlankString(obj, "appearance"),
                    habits = optNonBlankString(obj, "habits"),
                    likes = optNonBlankString(obj, "likes"),
                    dislikes = optNonBlankString(obj, "dislikes"),
                    greetingMessage = optNonBlankString(obj, "greetingMessage"),
                    tags = if (tagsList.isNotEmpty()) tagsList else null,
                    voiceStyle = optNonBlankString(obj, "voiceStyle"),
                    communicationFormality = optNonBlankString(obj, "communicationFormality"),
                    startingRelationship = optNonBlankString(obj, "startingRelationship"),
                    customRelationship = optNonBlankString(obj, "customRelationship"),
                    initialRelationshipTitle = optNonBlankString(obj, "initialRelationshipTitle"),
                    knowledgeCards = if (cardsList.isNotEmpty()) cardsList else null
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Не удалось разобрать ответ ИИ: ${e.localizedMessage ?: "неверный формат"}"))
        }
    }

    fun parsePersonaDraft(rawResponse: String): Result<GeneratedPersonaDraft> {
        return try {
            val jsonStr = extractJsonObjectString(rawResponse)
            val obj = JSONObject(jsonStr)
            Result.success(
                GeneratedPersonaDraft(
                    assistantMessage = optNonBlankString(obj, "assistantMessage")
                        ?: "Готово! Персона создана и заполнена.",
                    name = optNonBlankString(obj, "name") ?: "Игрок",
                    age = obj.optInt("age", 20).coerceIn(1, 9999),
                    gender = optNonBlankString(obj, "gender") ?: "",
                    shortDescription = optNonBlankString(obj, "shortDescription") ?: "",
                    personality = optNonBlankString(obj, "personality") ?: "",
                    appearance = optNonBlankString(obj, "appearance") ?: "",
                    habits = optNonBlankString(obj, "habits") ?: "",
                    likes = optNonBlankString(obj, "likes") ?: "",
                    dislikes = optNonBlankString(obj, "dislikes") ?: ""
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Не удалось разобрать ответ ИИ: ${e.localizedMessage ?: "неверный формат"}"))
        }
    }
}
