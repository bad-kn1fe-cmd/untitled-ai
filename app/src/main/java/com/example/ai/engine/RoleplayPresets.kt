package com.example.ai.engine

import androidx.compose.ui.graphics.Color

data class RoleplayModePreset(
    val id: String,
    val title: String,
    val shortLabel: String,
    val icon: String,
    val description: String,
    val promptInstruction: String
) {
    fun displayTitle(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        return when (id) {
            "light" -> if (isFemboy) "Лайт (uwu)" else if (isEn) "Light" else "Лайт"
            "medium" -> if (isFemboy) "Средний (ня)" else if (isEn) "Medium" else "Средний"
            "hard" -> if (isFemboy) "Сложный / Литературный" else if (isEn) "Hard / Literary" else "Сложный"
            "cinematic" -> if (isFemboy) "Кино / Экшен 🎬" else if (isEn) "Cinematic / Action" else "Кинематографичный"
            "psychological" -> if (isFemboy) "Психология 🧠" else if (isEn) "Psychological" else "Психологический"
            else -> title
        }
    }

    fun displayDescription(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        return when (id) {
            "light" -> if (isFemboy) "Простой, свободный и душевный стиль с чулочками и чаем (uwu)." else if (isEn) "Simple, relaxed, and cozy style. Feels like chatting with a comfortable friend over tea." else "Простой, свободный и душевный стиль со своим вайбиком. Словно человек уютно пишет из-под пледа с кружкой чая."
            "medium" -> if (isFemboy) "Стандартный сбалансированный ролплей с капелькой милоты." else if (isEn) "Standard balanced roleplay. Harmonious blend of dialogue, action, and character emotions." else "Стандартный сбалансированный ролплей. Гармоничное сочетание диалогов, действий и эмоций персонажа."
            "hard" -> if (isFemboy) "Красивый литературный слог, выразительные метафоры и глубокий сюжет." else if (isEn) "Refined literary vocabulary, rich metaphors, and deep book-like immersion." else "Красивый художественный слог, литературные термины, выразительные метафоры и глубокий вайб книги."
            "cinematic" -> if (isFemboy) "Динамичные сцены, акцент на действия, кадры и эликсиры." else if (isEn) "Dynamic, action-oriented narrative with cinematic pacing and dramatic climaxes." else "Динамичные сцены, акцент на действия, визуальные ракурсы, звуки и драматические кульминации."
            "psychological" -> if (isFemboy) "Глубокое погружение во внутренний мир и тонкие эмоции." else if (isEn) "Deep exploration of internal thoughts, hidden motives, and psychological subtext." else "Глубокое погружение во внутренний мир, скрытые мотивы, микро-жесты, невысказанные мысли и психологию."
            else -> description
        }
    }
}

data class ResponseLengthPreset(
    val id: String,
    val title: String,
    val tokenHint: String,
    val icon: String,
    val description: String,
    val promptInstruction: String
) {
    fun displayTitle(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        return when (id) {
            "short" -> if (isFemboy) "Коротенький" else if (isEn) "Short" else "Короткий"
            "medium" -> if (isFemboy) "Средний (ня)" else if (isEn) "Medium" else "Средний"
            "long" -> if (isFemboy) "Длинный" else if (isEn) "Long" else "Длинный"
            "epic" -> if (isFemboy) "Эпопея 📜" else if (isEn) "Epic" else "Эпопея"
            else -> title
        }
    }

    fun displayDescription(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        return when (id) {
            "short" -> if (isFemboy) "Ботики пишут коротко и лаконично (1-2 абзаца)." else if (isEn) "Concise and brief responses (1-2 paragraphs). Pairs best with Light mode." else "Боты пишут коротко, лаконично и емко (1-2 абзаца). Идеально сочетается с режимом «Лайт»."
            "medium" -> if (isFemboy) "Более длинные и содержательные ответики (3-5 абзацев)." else if (isEn) "Substantial, detailed responses (3-5 paragraphs). Pairs well with Light or Medium." else "Более длинные, развернутые и содержательные тексты (3-5 абзацев). Отлично сочетается с «Лайт» и «Средний»."
            "long" -> if (isFemboy) "Длинные и глубокие ответы с подробными описаниями." else if (isEn) "Extensive, deeply detailed responses (5+ paragraphs) describing actions, world, and thoughts." else "Боты пишут длинно, глубоко и масштабно, детально расписывая каждый шаг, диалоги и атмосферу."
            "epic" -> if (isFemboy) "Полномасштабные книжные главы с глубоким развитием сюжета." else if (isEn) "Full novel-like chapters with maximal scene, character, and world development." else "Полномасштабные книжные главы с максимальным развитием сюжета, мира и взаимодействий."
            else -> description
        }
    }
}

data class AtmospherePreset(
    val id: String,
    val title: String,
    val icon: String,
    val subtitle: String,
    val description: String,
    val tintColorHex: String,
    val promptInstruction: String
) {
    fun displayTitle(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        return when (id) {
            "cozy" -> if (isFemboy) "Уютный (uwu)" else if (isEn) "Cozy" else "Уютный"
            "noir" -> if (isFemboy) "Нуар / Дождик" else if (isEn) "Noir / Rain" else "Нуар / Дождь"
            "fantasy" -> if (isFemboy) "Фэнтези / Таверна" else if (isEn) "Fantasy / Tavern" else "Фэнтези / Таверна"
            "cyberpunk" -> if (isFemboy) "Футуризм / Неон" else if (isEn) "Futurism / Neon" else "Футуризм / Неон"
            else -> title
        }
    }

    fun displaySubtitle(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        return when (id) {
            "cozy" -> if (isFemboy) "Пледик, горячий чай и мармеладки" else if (isEn) "Blanket, hot tea, and cookies" else "Плед, горячий чай и печеньки"
            "noir" -> if (isFemboy) "Шум дождя и меланхолия" else if (isEn) "Rain, quiet jazz, and nostalgia" else "Шум дождя, джаз и меланхолия"
            "fantasy" -> if (isFemboy) "Очаг, старинные карты и магия" else if (isEn) "Hearth, parchment maps, and magic" else "Очаг, старинные карты и магия"
            "cyberpunk" -> if (isFemboy) "Мерцающий неон и хромированный город" else if (isEn) "Shimmering neon and futuristic skyline" else "Мерцающий неон и хромированный город"
            else -> subtitle
        }
    }

    fun displayDescription(lang: String = "ru"): String {
        val isEn = lang.equals("en", ignoreCase = true)
        val isFemboy = lang.equals("femboy", ignoreCase = true)
        return when (id) {
            "cozy" -> if (isFemboy) "Будто сидишь в пледике с чаем и печеньками, ролишь с милым человеком. Тепло и уютно." else if (isEn) "Feels like sitting wrapped in a warm blanket with hot tea, roleplaying comfortably with a trusted friend." else "Будто сидишь в пледе с чаем и печеньками, ролишь с другим комфортным человеком. Тепло, душевно и безопасно."
            "noir" -> if (isFemboy) "Дождь за стеклом, неоновые огни ночного города и загадочность." else if (isEn) "Rain pattering on glass, dim city lights, saxophone melodies, and thoughtful mystery." else "Дождь за стеклом, приглушенные огни ночного города, саксофон, сигаретный дым, задумчивость и загадочность."
            "fantasy" -> if (isFemboy) "Потрескивание дров в камине старой таверны, дух приключений и магии." else if (isEn) "Crackling hearth inside an ancient tavern at a crossroads, parchment maps, and magic in the air." else "Потрескивание дров в камине старой таверны на перекрестке миров, запах эля и трав, дух приключений и магии."
            "cyberpunk" -> if (isFemboy) "Высокие технологии, неон мегаполиса и цифровой шум." else if (isEn) "High technology, cold metropolis neon, synthetic noise, and futuristic adrenaline." else "Высокие технологии, холодный неон мегаполиса, цифровой шум, гул летающих машин и адреналин."
            else -> description
        }
    }
}

object RoleplayPresets {

    val ROLEPLAY_MODES = listOf(
        RoleplayModePreset(
            id = "light",
            title = "Лайт",
            shortLabel = "Лайт",
            icon = "🍵",
            description = "Простой, свободный и душевный стиль со своим вайбиком. Словно человек уютно пишет из-под пледа с кружкой чая.",
            promptInstruction = """
                ### РЕЖИМ РОЛПЛЕЯ: «ЛАЙТ» (УЮТНЫЙ, ЛЕГКИЙ И ЖИВОЙ СТИЛЬ)
                - Пиши просто, искренне, непринужденно и свободно, со своим уникальным живым характером и теплым вайбиком.
                - Создавай ощущение, будто реальный человек сидит под мягким пледом с горячим чаем и пишет от души, комфортно, естественно и без напряжения.
                - Не перегружай текст заумными или тяжелыми книжными терминами; пиши легко, живо, с теплом и душой.
            """.trimIndent()
        ),
        RoleplayModePreset(
            id = "medium",
            title = "Средний",
            shortLabel = "Средний",
            icon = "⚖️",
            description = "Стандартный сбалансированный ролплей. Гармоничное сочетание диалогов, действий и эмоций персонажа.",
            promptInstruction = """
                ### РЕЖИМ РОЛПЛЕЯ: «СРЕДНИЙ» (СТАНДАРТНЫЙ И СБАЛАНСИРОВАННЫЙ)
                - Классический качественный ролплей с балансом ярких диалогов, действий, мыслей и переживаний персонажа.
                - Передавай живые эмоции, сохраняя индивидуальный тон голоса, характерные манеры речи и атмосферу взаимодействия.
            """.trimIndent()
        ),
        RoleplayModePreset(
            id = "hard",
            title = "Сложный",
            shortLabel = "Сложный / Литературный",
            icon = "📖",
            description = "Красивый художественный слог, литературные термины, выразительные метафоры и глубокий вайб книги.",
            promptInstruction = """
                ### РЕЖИМ РОЛПЛЕЯ: «СЛОЖНЫЙ / ЛИТЕРАТУРНЫЙ» (ХУДОЖЕСТВЕННЫЙ РОМАН)
                - Пиши красиво, изящно и глубоко, используя богатый словарный запас, литературные термины, выразительные метафоры и эпитеты.
                - Создавай эффект погружения в качественную художественную книгу: описывай тонкие нюансы окружения, звуки, свет, запахи и многогранную психологию персонажа.
            """.trimIndent()
        ),
        RoleplayModePreset(
            id = "cinematic",
            title = "Кинематографичный",
            shortLabel = "Кино / Экшен",
            icon = "🎬",
            description = "Динамичные сцены, акцент на действия, визуальные ракурсы, звуки и драматические кульминации.",
            promptInstruction = """
                ### РЕЖИМ РОЛПЛЕЯ: «КИНЕМАТОГРАФИЧНЫЙ / ЭКШЕН»
                - Динамичное, экспрессивное повествование с акцентом на быстрый темп, действия, эффектные визуальные ракурсы и звуковые эффекты.
                - Держи темп сцены бодрым, передавай напряжение и накал событий, словно в ярком кинофильме.
            """.trimIndent()
        ),
        RoleplayModePreset(
            id = "psychological",
            title = "Психологический",
            shortLabel = "Психология",
            icon = "🧠",
            description = "Глубокое погружение во внутренний мир, скрытые мотивы, микро-жесты, невысказанные мысли и психологию.",
            promptInstruction = """
                ### РЕЖИМ РОЛПЛЕЯ: «ПСИХОЛОГИЧЕСКИЙ / ГЛУБОКИЙ»
                - Сосредоточься на исследовании внутреннего мира персонажа: его скрытых мотивах, невысказанных мыслях, языке тела, взглядах и подтекстах.
                - Отражай тончайшие душевные колебания и эмоциональную динамику между собеседниками.
            """.trimIndent()
        )
    )

    val RESPONSE_LENGTHS = listOf(
        ResponseLengthPreset(
            id = "short",
            title = "Короткий",
            tokenHint = "~100–300 токенов",
            icon = "⚡",
            description = "Боты пишут коротко, лаконично и емко (1-2 абзаца). Идеально сочетается с режимом «Лайт».",
            promptInstruction = """
                ### ОБЪЕМ И ДЛИНА ОТВЕТА: «КОРОТКИЙ»
                - Пиши компактно, лаконично и емко (1-2 небольших абзаца).
                - Без лишней воды и затянутых описаний. Фокусируйся на быстром и живом ответе, поддерживая динамику общения.
            """.trimIndent()
        ),
        ResponseLengthPreset(
            id = "medium",
            title = "Средний",
            tokenHint = "~1000–2000 токенов",
            icon = "📄",
            description = "Более длинные, развернутые и содержательные тексты (3-5 абзацев). Отлично сочетается с «Лайт» и «Средний».",
            promptInstruction = """
                ### ОБЪЕМ И ДЛИНА ОТВЕТА: «СРЕДНИЙ» (~1000–2000 ТОКЕНОВ)
                - Пиши развернуто, подробно и содержательно (3-5 полноценных абзацев).
                - Раскрывай действия, обстановку вокруг, эмоциональные реакции персонажа и диалог, не обрывая сцену на полуслове.
            """.trimIndent()
        ),
        ResponseLengthPreset(
            id = "long",
            title = "Длинный",
            tokenHint = "~2500+ токенов",
            icon = "📚",
            description = "Боты пишут длинно, глубоко и масштабно, детально расписывая каждый шаг, диалоги и атмосферу.",
            promptInstruction = """
                ### ОБЪЕМ И ДЛИНА ОТВЕТА: «ДЛИННЫЙ / ОБЪЕМНЫЙ»
                - Пиши объемные, детализированные и масштабные ответы (5+ абзацев).
                - Глубоко погружай в происходящее, уделяй внимание фоновым событиям, многослойным мыслям, жестам и богатому диалогу.
            """.trimIndent()
        ),
        ResponseLengthPreset(
            id = "epic",
            title = "Эпопея",
            tokenHint = "Максимум",
            icon = "📜",
            description = "Полномасштабные книжные главы с максимальным развитием сюжета, мира и взаимодействий.",
            promptInstruction = """
                ### ОБЪЕМ И ДЛИНА ОТВЕТА: «ЭПОПЕЯ / МАКСИМУМ»
                - Пиши максимально подробные, глубокие и развернутые ответы, создавая полноценную главу истории с панорамным раскрытием сцены.
            """.trimIndent()
        )
    )

    val ATMOSPHERES = listOf(
        AtmospherePreset(
            id = "cozy",
            title = "Уютный",
            icon = "☕",
            subtitle = "Плед, горячий чай и печеньки",
            description = "Будто сидишь в пледе с чаем и печеньками, ролишь с другим комфортным человеком. Тепло, душевно и безопасно.",
            tintColorHex = "#FFA726",
            promptInstruction = """
                ### ☕ АТМОСФЕРА И ВАЙБ: «УЮТНЫЙ»
                - Создавай общее ощущение тепла, мягкого пледа, горячего чая с печеньем и душевного уюта.
                - Вайб абсолютной безопасности, доверия, спокойствия, комфортного и непринужденного общения с близким человеком.
            """.trimIndent()
        ),
        AtmospherePreset(
            id = "noir",
            title = "Нуар / Дождь",
            icon = "🌧️",
            subtitle = "Шум дождя, джаз и меланхолия",
            description = "Дождь за стеклом, приглушенные огни ночного города, саксофон, сигаретный дым, задумчивость и загадочность.",
            tintColorHex = "#5C6BC0",
            promptInstruction = """
                ### 🌧️ АТМОСФЕРА И ВАЙБ: «НУАР / ДОЖДЬ И МЕЛАНХОЛИЯ»
                - Фоновая атмосфера: шелест дождя за окном, неоновые отблески в мокрых лужах, тихий джаз, сигаретный дым.
                - Придай сцене легкую кинематографичную меланхолию, загадочность, глубину размышлений и приглушенные тона.
            """.trimIndent()
        ),
        AtmospherePreset(
            id = "fantasy",
            title = "Фэнтези / Таверна",
            icon = "🔮",
            subtitle = "Очаг, старинные карты и магия",
            description = "Потрескивание дров в камине старой таверны на перекрестке миров, запах эля и трав, дух приключений и магии.",
            tintColorHex = "#AB47BC",
            promptInstruction = """
                ### 🔮 АТМОСФЕРА И ВАЙБ: «ФЭНТЕЗИ / ТАВЕРНА ПРИКЛЮЧЕНИЙ»
                - Атмосфера потрескивающего очага в старинной таверне, запахи трав, эля и жареного мяса, пергаментные карты.
                - Наполни повествование духом странствий, магии в воздухе, древних тайн и жажды открытий.
            """.trimIndent()
        ),
        AtmospherePreset(
            id = "cyberpunk",
            title = "Футуризм / Неон",
            icon = "🌆",
            subtitle = "Мерцающий неон и хромированный город",
            description = "Высокие технологии, холодный неон мегаполиса, цифровой шум, гул летающих машин и адреналин.",
            tintColorHex = "#26C6DA",
            promptInstruction = """
                ### 🌆 АТМОСФЕРА И ВАЙБ: «ФУТУРИЗМ / НЕОН»
                - Атмосфера мерцающих неоновых голограмм, ночного футуристического мегаполиса, синтетического шума и технологий.
                - Подчеркивай контраст между холодным цифровым миром и живыми человеческими эмоциями.
            """.trimIndent()
        ),
        AtmospherePreset(
            id = "romance",
            title = "Романтика / Закат",
            icon = "🌅",
            subtitle = "Теплые лучи, трепет и нежность",
            description = "Нежный закатный свет, трепетное смущение, искренность, легкое биение сердца и теплое прикосновение.",
            tintColorHex = "#EC407A",
            promptInstruction = """
                ### 🌅 АТМОСФЕРА И ВАЙБ: «РОМАНТИКА / ТЕПЛЫЙ ЗАКАТ»
                - Создавай нежную, трепетную и чувственную атмосферу, согретую лучами вечернего солнца.
                - Обращай внимание на смущенные улыбки, искренние взгляды, биение сердца, теплоту прикосновений и романтическую химию.
            """.trimIndent()
        ),
        AtmospherePreset(
            id = "thriller",
            title = "Триллер / Напряжение",
            icon = "⚡",
            subtitle = "Учащенный пульс и скрытая интрига",
            description = "Мурашки по коже, учащенное дыхание, скрытая опасность, игра на нервах и захватывающая тайна.",
            tintColorHex = "#EF5350",
            promptInstruction = """
                ### ⚡ АТМОСФЕРА И ВАЙБ: «ТРИЛЛЕР / НАПРЯЖЕНИЕ»
                - Нагнетай интригу, скрытую угрозу и обостренное ощущение опасности.
                - Описывай ускоренное сердцебиение, звуки шагов в тишине, внезапные тени и постоянную готовность к неожиданному повороту.
            """.trimIndent()
        ),
        AtmospherePreset(
            id = "mystic_forest",
            title = "Таинственный лес",
            icon = "🌲",
            subtitle = "Шорох листвы, туман и лунный свет",
            description = "Шепот вековых сосен, серебристый туман над лесной тропой, ночные светлячки, покой и древние тайны природы.",
            tintColorHex = "#66BB6A",
            promptInstruction = """
                ### 🌲 АТМОСФЕРА И ВАЙБ: «ТАИНСТВЕННЫЙ ЛЕС»
                - Атмосфера векового туманного леса, шелеста крон под ночным ветром, серебряного лунного света и мерцания светлячков.
                - Наполни сцену умиротворением, дыханием природы и легким налетом сказочной тайны.
            """.trimIndent()
        ),
        AtmospherePreset(
            id = "space",
            title = "Космос / Тишина",
            icon = "🌌",
            subtitle = "Звездная панорама и безмятежность",
            description = "Панорамный иллюминатор с видом на Млечный Путь, тихий гул станции, холодные звезды и космическое спокойствие.",
            tintColorHex = "#7E57C2",
            promptInstruction = """
                ### 🌌 АТМОСФЕРА И ВАЙБ: «КОСМИЧЕСКАЯ ТИШИНА»
                - Ощущение бескрайнего космического пространства за стеклом, созерцания далеких созвездий и галактик.
                - Навевай глубокую безмятежность, философское спокойствие и уединение вдали от земной суеты.
            """.trimIndent()
        ),
        AtmospherePreset(
            id = "none",
            title = "Без атмосферы",
            icon = "🎭",
            subtitle = "Нейтральный естественный фон",
            description = "Без принудительного наложения атмосферы, чистое следование сюжету и действиям.",
            tintColorHex = "#9E9E9E",
            promptInstruction = ""
        )
    )

    fun getRoleplayMode(id: String?): RoleplayModePreset {
        return ROLEPLAY_MODES.find { it.id.equals(id, ignoreCase = true) } ?: ROLEPLAY_MODES[1] // Default Medium
    }

    fun getResponseLength(id: String?): ResponseLengthPreset {
        return RESPONSE_LENGTHS.find { it.id.equals(id, ignoreCase = true) } ?: RESPONSE_LENGTHS[1] // Default Medium
    }

    fun getAtmosphere(id: String?): AtmospherePreset {
        return ATMOSPHERES.find { it.id.equals(id, ignoreCase = true) } ?: ATMOSPHERES[0] // Default Cozy
    }
}
