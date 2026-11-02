package com.example.util

data class MessageReaction(
    val emoji: String,
    val titleRu: String,
    val titleEn: String,
    val titleFemboy: String,
    val descriptionRu: String,
    val descriptionEn: String,
    val signalRu: String,
    val signalEn: String,
    val isPositive: Boolean
) {
    fun displayTitle(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        return when {
            isFemboy -> titleFemboy
            isEn -> titleEn
            else -> titleRu
        }
    }
}

object MessageReactionHelper {

    val POSITIVE_REACTIONS = listOf(
        MessageReaction(
            emoji = "❤️",
            titleRu = "В восторге",
            titleEn = "Loved it",
            titleFemboy = "В восторге (ня~)",
            descriptionRu = "Отличный ход сюжета",
            descriptionEn = "Great plot development",
            signalRu = "Пользователю невероятно понравился этот поворот сюжета, эмоции и стиль отыгрыша. Продолжай развивать эту тему и атмосферу в том же ключе!",
            signalEn = "User loved this plot twist, emotional depth, and roleplay style! Continue developing this narrative theme and mood.",
            isPositive = true
        ),
        MessageReaction(
            emoji = "🔥",
            titleRu = "Страстно",
            titleEn = "Passionate",
            titleFemboy = "Страстно (uwu)",
            descriptionRu = "Яркие эмоции и накал",
            descriptionEn = "Vivid passion and intensity",
            signalRu = "Пользователь оценил высокий накал страстей, чувственность и динамику момента. Поддерживай эту близость, чувственность и живой драйв.",
            signalEn = "User appreciated the high intensity, intimacy, and passion. Maintain this closeness, sensory details, and vivid pace.",
            isPositive = true
        ),
        MessageReaction(
            emoji = "👍",
            titleRu = "Одобряю",
            titleEn = "Approve",
            titleFemboy = "Одобряю (owo)",
            descriptionRu = "Хорошее направление",
            descriptionEn = "Good narrative direction",
            signalRu = "Пользователь одобряет выбранное действие и реакцию персонажа. Двигайся дальше по этой ветке событий.",
            signalEn = "User approves character's response and current direction. Proceed along this storyline.",
            isPositive = true
        ),
        MessageReaction(
            emoji = "🎯",
            titleRu = "В характере",
            titleEn = "In Character",
            titleFemboy = "В характере 🐾",
            descriptionRu = "Идеальное попадание в образ",
            descriptionEn = "Perfect match for persona",
            signalRu = "Пользователь отмечает точное попадание в каноничный характер, привычки и психологию персонажа. Сохраняй именно эту манеру речи и мышления!",
            signalEn = "User highlights accurate canonical personality, tone, and character psychology. Preserve this manner of speech and thought!",
            isPositive = true
        ),
        MessageReaction(
            emoji = "✨",
            titleRu = "Интригует",
            titleEn = "Intriguing",
            titleFemboy = "Интригует ✨",
            descriptionRu = "Интересный сюжетный крючок",
            descriptionEn = "Engaging story hook",
            signalRu = "Пользователя заинтриговала эта деталь или сюжетный ход. Разверни эту интригу глубже и добавь неожиданных подробностей.",
            signalEn = "User was intrigued by this story hook. Unfold this mystery deeper and add surprising narrative details.",
            isPositive = true
        )
    )

    val NEGATIVE_REACTIONS = listOf(
        MessageReaction(
            emoji = "👎",
            titleRu = "Не то",
            titleEn = "Not right",
            titleFemboy = "Не то (nya)",
            descriptionRu = "Неудачное направление",
            descriptionEn = "Unfortunate direction",
            signalRu = "Пользователю не понравилось данное действие или развитие событий. Плавно скорректируй вектор сюжета и предложи более удачный ход.",
            signalEn = "User disliked this development. Smoothly adjust narrative trajectory toward a better plot direction.",
            isPositive = false
        ),
        MessageReaction(
            emoji = "🛑",
            titleRu = "Не в характере (OOC)",
            titleEn = "Out of Character (OOC)",
            titleFemboy = "Не в характере (OOC)",
            descriptionRu = "Нарушение канона / фальшиво",
            descriptionEn = "Canonical mismatch",
            signalRu = "Персонаж повел себя не в своем характере (Out of Character) или неестественно. Срочно вернись к изначальному психотипу, тону и поведению из описания карточки!",
            signalEn = "Character acted Out of Character (OOC). Immediately realign tone and behavior with original persona description!",
            isPositive = false
        ),
        MessageReaction(
            emoji = "⚠️",
            titleRu = "Слишком быстро",
            titleEn = "Too Fast",
            titleFemboy = "Слишком быстро",
            descriptionRu = "Спешка / нелогичный темп",
            descriptionEn = "Rushed pacing",
            signalRu = "События или сближение развиваются слишком торопливо или скомкано. Замедли темп (pacing), удели внимание окружению, мелким жестам, паузам и внутренним мыслям.",
            signalEn = "Events or intimacy progressed too quickly. Slow down pacing, focus on sensory surroundings, subtle gestures, and internal thoughts.",
            isPositive = false
        ),
        MessageReaction(
            emoji = "💔",
            titleRu = "Сухо / Пресно",
            titleEn = "Boring / Dry",
            titleFemboy = "Пресно (owo)",
            descriptionRu = "Не хватает эмоций",
            descriptionEn = "Lacks emotional depth",
            signalRu = "Ответ показался пользователю пресным или пассивным. Добавь больше выразительных эмоций, телесных реакций, инициативы и активных действий персонажа.",
            signalEn = "Response felt dry or passive. Inject more expressive emotion, physical reactions, and active character initiative.",
            isPositive = false
        )
    )

    val ALL_REACTIONS: List<MessageReaction> = POSITIVE_REACTIONS + NEGATIVE_REACTIONS

    fun getReactionInfo(emoji: String?): MessageReaction? {
        if (emoji.isNullOrBlank()) return null
        return ALL_REACTIONS.find { it.emoji == emoji }
    }

    fun getReactionSignal(emoji: String?, isEn: Boolean = false): String? {
        if (emoji.isNullOrBlank()) return null
        val info = ALL_REACTIONS.find { it.emoji == emoji } ?: return null
        return if (isEn) info.signalEn else info.signalRu
    }
}
