package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import org.json.JSONObject
import java.io.File

data class LanguagePackInfo(
    val code: String,
    val name: String,
    val description: String = "",
    val author: String = "Community",
    val version: String = "1.0",
    val isCustom: Boolean = false,
    val iconEmoji: String = "🌐"
)

val LocalAppLanguage = staticCompositionLocalOf { "ru" }

@Composable
@ReadOnlyComposable
fun tr(key: String, defaultText: String = ""): String {
    val lang = LocalAppLanguage.current
    return AppStrings.tr(lang, key, defaultText)
}

object AppStrings {
    private const val TAG = "AppStrings"
    private const val LANG_PACKS_DIR = "langpacks"

    // In-memory cache for custom loaded language packs: code -> Map<key, translation>
    private val customPacks = mutableMapOf<String, Pair<LanguagePackInfo, Map<String, String>>>()
    private var customPacksLoaded = false

    /**
     * Get translated string by key for the given language code.
     */
    fun tr(lang: String, key: String, defaultText: String = ""): String {
        // 1. Check custom packs if matches
        val customMap = customPacks[lang]?.second
        if (customMap != null && customMap.containsKey(key)) {
            val res = customMap[key]
            if (!res.isNullOrBlank()) return res
        }

        // 2. Check built-in Femboy language
        if (lang.equals("femboy", ignoreCase = true)) {
            return femboyStrings[key] ?: ruStrings[key] ?: defaultText.ifBlank { key }
        }

        // 3. Check built-in English
        if (lang.equals("en", ignoreCase = true)) {
            return enStrings[key] ?: ruStrings[key] ?: defaultText.ifBlank { key }
        }

        // 4. Check built-in Russian (default)
        return ruStrings[key] ?: enStrings[key] ?: defaultText.ifBlank { key }
    }

    /**
     * Get all available languages (built-in + any imported custom packs).
     */
    fun getAvailableLanguages(context: Context? = null): List<LanguagePackInfo> {
        if (context != null && !customPacksLoaded) {
            loadCustomPacksFromStorage(context)
        }

        val list = mutableListOf<LanguagePackInfo>()
        list.add(
            LanguagePackInfo(
                code = "ru",
                name = "Русский",
                description = "Официальная русская локализация",
                author = "Untitled AI Core",
                version = "2.0",
                isCustom = false,
                iconEmoji = "🇷🇺"
            )
        )
        list.add(
            LanguagePackInfo(
                code = "femboy",
                name = "Фембойский (Ня~)",
                description = "Фембойский фурри вайбик: ня, мармеладки, чулочки и лапки (uwu)",
                author = "Femboy Furry Community",
                version = "2.0",
                isCustom = false,
                iconEmoji = "🐾"
            )
        )
        list.add(
            LanguagePackInfo(
                code = "en",
                name = "English",
                description = "Official English localization (Complete)",
                author = "Untitled AI Core",
                version = "2.0",
                isCustom = false,
                iconEmoji = "🇺🇸"
            )
        )

        customPacks.values.forEach { (info, _) ->
            if (info.code != "ru" && info.code != "en" && info.code != "femboy") {
                list.add(info)
            }
        }

        return list
    }

    /**
     * Load custom JSON packs from internal files directory.
     */
    fun loadCustomPacksFromStorage(context: Context) {
        try {
            val dir = File(context.filesDir, LANG_PACKS_DIR)
            if (!dir.exists()) dir.mkdirs()

            val files = dir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            for (file in files) {
                try {
                    val jsonStr = file.readText(Charsets.UTF_8)
                    parseAndRegisterPack(jsonStr, isCustom = true)
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading language pack from ${file.name}", e)
                }
            }
            customPacksLoaded = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing custom language packs", e)
        }
    }

    /**
     * Export complete language pack to formatted JSON string.
     */
    fun exportLanguagePackJson(langCode: String): String {
        val root = JSONObject()
        val isEn = langCode.equals("en", ignoreCase = true)
        val custom = customPacks[langCode]

        val code = custom?.first?.code ?: if (isEn) "en" else "ru"
        val name = custom?.first?.name ?: if (isEn) "English" else "Русский"
        val desc = custom?.first?.description ?: if (isEn) "Complete UI Translation for Untitled AI" else "Полный языковой пакет приложения Untitled AI"
        val author = custom?.first?.author ?: "Untitled AI Team"
        val version = custom?.first?.version ?: "2.0"

        root.put("languageCode", code)
        root.put("languageName", name)
        root.put("description", desc)
        root.put("author", author)
        root.put("version", version)
        root.put("totalKeys", ruStrings.size)

        val stringsObj = JSONObject()
        // Collect all keys dynamically from baseline ru/en dictionaries and any custom overrides
        val allKeys = (ruStrings.keys + enStrings.keys + (custom?.second?.keys ?: emptySet())).sorted()
        for (key in allKeys) {
            val value = tr(langCode, key)
            stringsObj.put(key, value)
        }
        root.put("strings", stringsObj)

        return root.toString(2)
    }

    /**
     * Import a language pack from JSON string, save to local disk, and register in memory.
     */
    fun importLanguagePackFromJson(context: Context, jsonString: String): Result<LanguagePackInfo> {
        return try {
            val (info, map) = parseAndRegisterPack(jsonString, isCustom = true)
                ?: return Result.failure(Exception("Не удалось распарсить JSON языкового пакета. Проверьте структуру файла."))

            // Save to storage
            val dir = File(context.filesDir, LANG_PACKS_DIR)
            if (!dir.exists()) dir.mkdirs()

            val safeFileName = "pack_${info.code.replace("[^a-zA-Z0-9_]".toRegex(), "_")}.json"
            val file = File(dir, safeFileName)
            file.writeText(jsonString, Charsets.UTF_8)

            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import language pack", e)
            Result.failure(e)
        }
    }

    /**
     * Delete an imported custom language pack.
     */
    fun deleteCustomLanguagePack(context: Context, langCode: String): Boolean {
        if (langCode == "ru" || langCode == "en") return false
        customPacks.remove(langCode)
        try {
            val dir = File(context.filesDir, LANG_PACKS_DIR)
            val files = dir.listFiles { _, name -> name.contains(langCode) || name.endsWith(".json") } ?: emptyArray()
            for (f in files) {
                try {
                    val content = f.readText(Charsets.UTF_8)
                    val json = JSONObject(content)
                    if (json.optString("languageCode") == langCode) {
                        f.delete()
                    }
                } catch (_: Exception) {}
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting pack $langCode", e)
            return false
        }
    }

    private fun parseAndRegisterPack(jsonString: String, isCustom: Boolean): Pair<LanguagePackInfo, Map<String, String>>? {
        try {
            val json = JSONObject(jsonString)
            val code = json.optString("languageCode").ifBlank { "custom_${System.currentTimeMillis() % 10000}" }
            val name = json.optString("languageName").ifBlank { "Custom ($code)" }
            val desc = json.optString("description").ifBlank { "Imported Custom Language Pack" }
            val author = json.optString("author").ifBlank { "User" }
            val version = json.optString("version").ifBlank { "1.0" }
            val emoji = json.optString("iconEmoji").ifBlank { "🌐" }

            val stringsObj = json.optJSONObject("strings") ?: json
            val map = mutableMapOf<String, String>()
            val keys = stringsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                if (k !in listOf("languageCode", "languageName", "description", "author", "version", "iconEmoji", "totalKeys")) {
                    map[k] = stringsObj.optString(k)
                }
            }

            val info = LanguagePackInfo(
                code = code,
                name = name,
                description = desc,
                author = author,
                version = version,
                isCustom = isCustom,
                iconEmoji = emoji
            )
            customPacks[code] = Pair(info, map)
            return Pair(info, map)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse pack", e)
            return null
        }
    }

    // =========================================================================
    // BASELINE DICTIONARIES (Russian & Complete English)
    // =========================================================================

    private val ruStrings = mapOf(
        // App Core & Navigation
        "app_title" to "Untitled",
        "app_subtitle" to "Интерактивный ролплей и литературные новеллы",
        "tab_chats" to "Диалоги",
        "tab_characters" to "Персонажи",
        "tab_stories" to "Сюжеты",
        "tab_comics" to "Комиксы",
        "tab_personas" to "Персоны",
        "tab_providers" to "Провайдеры ИИ",
        "tab_settings" to "Настройки",
        "tab_backup" to "Резервное копирование",

        // Common Buttons & Actions
        "btn_save" to "Сохранить",
        "btn_cancel" to "Отмена",
        "btn_delete" to "Удалить",
        "btn_edit" to "Редактировать",
        "btn_close" to "Закрыть",
        "btn_apply" to "Применить",
        "btn_confirm" to "Подтвердить",
        "btn_create" to "Создать",
        "btn_import" to "Импорт",
        "btn_export" to "Экспорт",
        "btn_copy" to "Скопировать",
        "btn_share" to "Поделиться",
        "btn_retry" to "Повторить",
        "btn_reset" to "Сбросить",
        "btn_search" to "Поиск",
        "btn_generate" to "Сгенерировать",
        "btn_send" to "Отправить",
        "btn_stop" to "Остановить",

        // Character List Screen
        "search_characters_hint" to "Поиск персонажей по имени, тегам...",
        "filter_all" to "Все",
        "filter_favorites" to "Избранные",
        "filter_nsfw" to "18+ NSFW",
        "filter_recent" to "Недавние",
        "sort_by" to "Сортировка",
        "sort_newest" to "По новизне",
        "sort_name" to "По имени (А-Я)",
        "sort_activity" to "По активности",
        "create_new_character" to "Создать персонажа",
        "import_character_card" to "Импортировать карту (PNG / JSON)",
        "no_characters_title" to "Список персонажей пуст",
        "no_characters_desc" to "Создайте уникального персонажа или импортируйте готовую карточку формата Tavern/V2",
        "start_chat" to "Начать диалог",
        "continue_chat" to "Продолжить диалог",
        "character_info" to "Информация о персонаже",
        "export_tavern_card" to "Экспорт карточки Tavern",
        "delete_character_confirm" to "Удалить персонажа и всю историю диалогов?",

        // Chat Screen
        "chat_input_hint" to "Напишите реплику или действие...",
        "chat_action_btn" to "Действие *...*",
        "chat_regenerate" to "Перегенерировать ответ",
        "chat_edit_message" to "Редактировать сообщение",
        "chat_delete_message" to "Удалить сообщение",
        "chat_copy_message" to "Скопировать текст",
        "chat_branch_navigation" to "Вариант ответа",
        "chat_typing_indicator" to "печатает ответ...",
        "chat_generating_indicator" to "генерирует продолжение...",
        "chat_tokens_used" to "Использовано токенов",
        "chat_model_label" to "Модель",
        "chat_clear_history" to "Очистить историю сообщений",
        "chat_delete_dialogue" to "Удалить этот диалог",
        "chat_settings_btn" to "Параметры сессии диалога",
        "chat_photo_btn" to "Сгенерировать фото сцены",
        "chat_tts_btn" to "Озвучить реплику",
        "chat_comic_btn" to "Создать комикс сцены",
        "chat_memory_btn" to "Сводка памяти и факты",
        "chat_relationship_btn" to "Уровень отношений",
        "chat_persona_select" to "Выбрать активную персону",
        "chat_no_messages" to "Начните историю первым сообщением или используйте приветствие персонажа!",
        "chat_active_persona_none" to "Без персоны (Стандарт)",

        // Chat Settings Screen
        "chat_settings_title" to "Параметры сессии диалога",
        "chat_settings_ai_params" to "Параметры генерации ИИ",
        "chat_settings_temp" to "Температура (Креативность)",
        "chat_settings_temp_desc" to "0.1 - строгость и логика, 1.2+ - максимальная фантазия",
        "chat_settings_top_p" to "Top-P выборка",
        "chat_settings_presence" to "Штраф за присутствие (Разнообразие)",
        "chat_settings_freq" to "Штраф за повторы слов",
        "chat_settings_max_tokens" to "Максимальное количество токенов",
        "chat_settings_context_size" to "Глубина контекста (Кол-во сообщений)",
        "chat_settings_system_prompt" to "Индивидуальный системный промпт",
        "chat_settings_system_prompt_desc" to "Переопределяет глобальные инструкции только для этой беседы",
        "chat_settings_jailbreak" to "Творческий Uncensored / NSFW режим",
        "chat_settings_memory_enable" to "Автоматическая память и сбор фактов",
        "chat_settings_wallpaper" to "Фоновое изображение для этого чата",
        "chat_settings_reset_btn" to "Сбросить к значениям по умолчанию",

        // Character Editor Screen
        "editor_title_create" to "Создание персонажа",
        "editor_title_edit" to "Редактирование персонажа",
        "editor_name" to "Имя персонажа",
        "editor_name_hint" to "Например: Элеонора, Детектив",
        "editor_tagline" to "Краткий слоган / Титул",
        "editor_tagline_hint" to "Например: Загадочная волшебница древнего ордена",
        "editor_description" to "Биография и описание",
        "editor_description_hint" to "Подробная история персонажа, его прошлое и цели...",
        "editor_personality" to "Характер и поведение",
        "editor_personality_hint" to "Черты личности, привычки, тон общения, эмоции...",
        "editor_scenario" to "Сценарий и начальная обстановка",
        "editor_scenario_hint" to "Где разворачивается действие, текущая ситуация...",
        "editor_greeting" to "Первое сообщение (Приветствие)",
        "editor_greeting_hint" to "Стартовая реплика или действие, с которого начинается чат...",
        "editor_dialogue_examples" to "Примеры диалога (<START>...)",
        "editor_appearance" to "Внешность и визуальные теги",
        "editor_appearance_hint" to "Цвет глаз, волос, стиль одежды, характерные детали для генератора артов...",
        "editor_avatar" to "Основной аватар",
        "editor_avatar_pick" to "Выбрать фото аватара",
        "editor_reference_photos" to "Галерея визуальных референсов",
        "editor_add_photo" to "Добавить фото",
        "editor_lora_tags" to "LoRA теги для генерации",
        "editor_nsfw_flag" to "Метка 18+ (Взрослый контент)",
        "editor_tags" to "Категории и теги",
        "editor_tags_hint" to "Фэнтези, Романтика, Аниме, Детектив...",

        // Image Generation Dialog
        "generate_photo_title" to "Генерация изображения сцены",
        "generate_photo_desc" to "Создание визуального арта на основе текущего контекста и внешности",
        "image_engine_select" to "Движок генерации",
        "engine_flux_free" to "Flux Ultra (Бесплатно / Без ключа)",
        "engine_pollinations_turbo" to "SDXL Turbo (Быстрая генерация)",
        "engine_openai_dalle" to "OpenAI DALL-E 3",
        "engine_civitai" to "Civitai Air (SDXL / LoRA)",
        "engine_custom" to "Custom / SD WebUI API",
        "scene_prompt_label" to "Описание сцены или позы",
        "scene_prompt_hint" to "Опишите желаемое действие, окружение или ракурс...",
        "include_char_appearance" to "Учитывать внешность персонажа",
        "include_char_appearance_sub" to "Черты лица, стиль одежды и визуальные референсы",
        "art_style" to "Художественный стиль",
        "aspect_ratio" to "Соотношение сторон",
        "btn_generate_photo" to "Сгенерировать арт",

        // AI Providers Screen
        "providers_title" to "Провайдеры ИИ и Модели",
        "providers_desc" to "Управление API-ключами, моделями и эндпоинтами генерации",
        "providers_add_btn" to "Добавить профиль провайдера",
        "providers_profile_name" to "Название профиля",
        "providers_type" to "Тип провайдера",
        "providers_model" to "Имя модели",
        "providers_base_url" to "Базовый URL эндпоинта",
        "providers_api_key" to "API Ключ",
        "providers_test_btn" to "Проверить соединение",
        "providers_default_text" to "Основной для текста диалогов",
        "providers_default_image" to "Основной для генерации фото",
        "providers_active_badge" to "АКТИВЕН",
        "providers_free_badge" to "БЕСПЛАТНО",
        "providers_test_success" to "✅ Связь с провайдером успешно установлена!",
        "providers_test_error" to "❌ Ошибка соединения:",

        // Settings Screen
        "settings_title" to "Настройки приложения",
        "settings_tab_appearance" to "Оформление & Язык",
        "settings_tab_wallpaper" to "Обои & Эффекты",
        "settings_tab_ai" to "ИИ & Провайдеры",
        "settings_tab_behavior" to "Поведение & Ролплей",
        "settings_tab_system" to "Система & Бэкап",

        "settings_language_section" to "Язык интерфейса / Localization",
        "settings_language_desc" to "Выбор активного языка, экспорт и импорт языковых пакетов JSON",
        "settings_export_lang_btn" to "Экспорт языкового пакета (JSON)",
        "settings_import_lang_btn" to "Импорт языка из файла (JSON)",
        "settings_delete_lang_btn" to "Удалить языковой пакет",
        "settings_lang_keys_count" to "Количество локализованных строк",

        "settings_theme_title" to "Цветовая тема оформления",
        "settings_theme_dark" to "Тёмная тема",
        "settings_theme_light" to "Светлая тема",
        "settings_theme_system" to "Системная тема",
        "settings_accent_color" to "Акцентный цвет интерфейса",
        "settings_bubble_style_title" to "Стиль отображения реплик",
        "settings_bubble_chat" to "Сообщения (Бабблы)",
        "settings_bubble_novel" to "Визуальная новелла",
        "settings_font_size" to "Размер шрифта сообщений",
        "settings_wallpaper_parallax" to "3D Параллакс обоев при наклоне",
        "settings_wallpaper_blur" to "Размытие фона чата",
        "settings_wallpaper_vignette" to "Эффект затемнения краев (Виньетка)",

        "settings_roleplay_mode_title" to "Режим ролплея по умолчанию",
        "settings_roleplay_mode_desc" to "Базовый стиль изложения и глубина диалогов для новых персонажей",
        "settings_response_length_title" to "Объем ответов по умолчанию",
        "settings_response_length_desc" to "Базовый размер генерируемого текста",
        "settings_atmosphere_title" to "Атмосфера и вайб диалогов",
        "settings_atmosphere_desc" to "Включает передачу эмоционального вайба, погодных и сенсорных оттенков в промпт",
        "settings_nsfw_toggle_title" to "Контент 18+ (NSFW)",
        "settings_nsfw_toggle_desc" to "Разрешить взрослые темы, страсть и нецензурированные сцены",
        "settings_streaming_title" to "Потоковая генерация ответов",
        "settings_streaming_desc" to "Отображать текст в реальном времени по мере поступления токенов",
        "settings_haptics_title" to "Тактильный отклик (Вибрация)",
        "settings_haptics_desc" to "Приятные микро-вибрации при наборе текста и действиях",

        // User Personas & Other Screens
        "personas_title" to "Персоны пользователя",
        "personas_desc" to "Создавайте разные образы себя (имя, характер, внешность) для диалогов",
        "personas_create_btn" to "Создать персону",
        "stories_title" to "Сюжеты и Сценарии",
        "comics_title" to "Галерея комиксов сцен",
        "backup_title" to "Резервное копирование и Восстановление",
        "backup_create_btn" to "Создать полный бэкап (JSON)",
        "backup_restore_btn" to "Восстановить данные из бэкапа",
        "relationship_title" to "История развития отношений"
    )

    private val enStrings = mapOf(
        // App Core & Navigation
        "app_title" to "UntitledAI",
        "app_subtitle" to "Interactive Roleplay & Visual Novels",
        "tab_chats" to "Dialogues",
        "tab_characters" to "Characters",
        "tab_stories" to "Stories",
        "tab_comics" to "Comics",
        "tab_personas" to "User Personas",
        "tab_providers" to "AI Providers",
        "tab_settings" to "Settings",
        "tab_backup" to "Backup & Restore",

        // Common Buttons & Actions
        "btn_save" to "Save",
        "btn_cancel" to "Cancel",
        "btn_delete" to "Delete",
        "btn_edit" to "Edit",
        "btn_close" to "Close",
        "btn_apply" to "Apply",
        "btn_confirm" to "Confirm",
        "btn_create" to "Create",
        "btn_import" to "Import",
        "btn_export" to "Export",
        "btn_copy" to "Copy",
        "btn_share" to "Share",
        "btn_retry" to "Retry",
        "btn_reset" to "Reset",
        "btn_search" to "Search",
        "btn_generate" to "Generate",
        "btn_send" to "Send",
        "btn_stop" to "Stop",

        // Character List Screen
        "search_characters_hint" to "Search characters by name, tags...",
        "filter_all" to "All",
        "filter_favorites" to "Favorites",
        "filter_nsfw" to "18+ NSFW",
        "filter_recent" to "Recent",
        "sort_by" to "Sort by",
        "sort_newest" to "Newest First",
        "sort_name" to "By Name (A-Z)",
        "sort_activity" to "By Recent Activity",
        "create_new_character" to "Create Character",
        "import_character_card" to "Import Card (PNG / JSON)",
        "no_characters_title" to "Character list is empty",
        "no_characters_desc" to "Create a unique persona or import an existing Tavern/V2 character card",
        "start_chat" to "Start Dialogue",
        "continue_chat" to "Continue Dialogue",
        "character_info" to "Character Information",
        "export_tavern_card" to "Export Tavern Card",
        "delete_character_confirm" to "Delete character and all associated dialogues?",

        // Chat Screen
        "chat_input_hint" to "Type a dialogue line or action...",
        "chat_action_btn" to "Action *...*",
        "chat_regenerate" to "Regenerate Response",
        "chat_edit_message" to "Edit Message",
        "chat_delete_message" to "Delete Message",
        "chat_copy_message" to "Copy Text",
        "chat_branch_navigation" to "Response Variant",
        "chat_typing_indicator" to "is typing a response...",
        "chat_generating_indicator" to "is generating continuation...",
        "chat_tokens_used" to "Tokens consumed",
        "chat_model_label" to "Model",
        "chat_clear_history" to "Clear Message History",
        "chat_delete_dialogue" to "Delete Dialogue",
        "chat_settings_btn" to "Session Parameters",
        "chat_photo_btn" to "Generate Scene Image",
        "chat_tts_btn" to "Voice TTS Audio",
        "chat_comic_btn" to "Generate Scene Comic",
        "chat_memory_btn" to "Memory Notes & Facts",
        "chat_relationship_btn" to "Relationship Level",
        "chat_persona_select" to "Select Active Persona",
        "chat_no_messages" to "Begin the journey with a first message or character greeting!",
        "chat_active_persona_none" to "No Persona (Default)",

        // Chat Settings Screen
        "chat_settings_title" to "Dialogue Session Parameters",
        "chat_settings_ai_params" to "AI Model Inference Parameters",
        "chat_settings_temp" to "Temperature (Creativity)",
        "chat_settings_temp_desc" to "0.1 - precise & logical, 1.2+ - imaginative & colorful",
        "chat_settings_top_p" to "Top-P Sampling",
        "chat_settings_presence" to "Presence Penalty (Variety)",
        "chat_settings_freq" to "Frequency Penalty (Repetition)",
        "chat_settings_max_tokens" to "Max Generation Tokens",
        "chat_settings_context_size" to "Context Depth (Message History)",
        "chat_settings_system_prompt" to "Session System Prompt Override",
        "chat_settings_system_prompt_desc" to "Overrides global prompt directives specifically for this conversation",
        "chat_settings_jailbreak" to "Uncensored / Creative Mode",
        "chat_settings_memory_enable" to "Automated Memory & Fact Extraction",
        "chat_settings_wallpaper" to "Custom Background Wallpaper for Chat",
        "chat_settings_reset_btn" to "Reset to Defaults",

        // Character Editor Screen
        "editor_title_create" to "Create Character",
        "editor_title_edit" to "Edit Character",
        "editor_name" to "Character Name",
        "editor_name_hint" to "e.g. Eleanor, Cyber Detective",
        "editor_tagline" to "Tagline / Title",
        "editor_tagline_hint" to "e.g. Enigmatic sorceress of the ancient sanctuary",
        "editor_description" to "Biography & Background",
        "editor_description_hint" to "Detailed lore, backstory, motivations and origin...",
        "editor_personality" to "Personality & Behavioral Traits",
        "editor_personality_hint" to "Demeanor, habits, voice tone, quirks and temperament...",
        "editor_scenario" to "Scenario & Initial Setting",
        "editor_scenario_hint" to "World setting, current dilemma, immediate circumstances...",
        "editor_greeting" to "First Message (Greeting)",
        "editor_greeting_hint" to "Introductory greeting or evocative action that starts the story...",
        "editor_dialogue_examples" to "Dialogue Examples (<START>...)",
        "editor_appearance" to "Appearance & Visual Descriptors",
        "editor_appearance_hint" to "Eye color, hair, wardrobe aesthetic, physical traits for art generator...",
        "editor_avatar" to "Main Character Avatar",
        "editor_avatar_pick" to "Select Avatar Image",
        "editor_reference_photos" to "Reference Visual Gallery",
        "editor_add_photo" to "Add Visual Reference",
        "editor_lora_tags" to "LoRA Generation Tags",
        "editor_nsfw_flag" to "Mature Content Flag (18+)",
        "editor_tags" to "Categories & Tags",
        "editor_tags_hint" to "Fantasy, Romance, Anime, Mystery, Sci-Fi...",

        // Image Generation Dialog
        "generate_photo_title" to "Scene Image Generation",
        "generate_photo_desc" to "Generate visual artwork based on narrative context and character appearance",
        "image_engine_select" to "Generation Engine",
        "engine_flux_free" to "Flux Ultra (Free / No API Key)",
        "engine_pollinations_turbo" to "SDXL Turbo (High Speed)",
        "engine_openai_dalle" to "OpenAI DALL-E 3",
        "engine_civitai" to "Civitai Air (SDXL / LoRA)",
        "engine_custom" to "Custom / SD WebUI API",
        "scene_prompt_label" to "Scene or Pose Description",
        "scene_prompt_hint" to "Describe desired actions, environment, or perspective...",
        "include_char_appearance" to "Include Character Appearance",
        "include_char_appearance_sub" to "Facial features, clothing style, and visual reference tags",
        "art_style" to "Art Style",
        "aspect_ratio" to "Aspect Ratio",
        "btn_generate_photo" to "Generate Artwork",

        // AI Providers Screen
        "providers_title" to "AI Providers & Models",
        "providers_desc" to "Configure API keys, models, and custom generation endpoints",
        "providers_add_btn" to "Add Provider Profile",
        "providers_profile_name" to "Profile Name",
        "providers_type" to "Provider Type",
        "providers_model" to "Model Identifier",
        "providers_base_url" to "Base Endpoint URL",
        "providers_api_key" to "API Secret Key",
        "providers_test_btn" to "Test Connection",
        "providers_default_text" to "Default for Dialogue Text",
        "providers_default_image" to "Default for Image Generation",
        "providers_active_badge" to "ACTIVE",
        "providers_free_badge" to "FREE TIER",
        "providers_test_success" to "✅ Connection successfully verified!",
        "providers_test_error" to "❌ Connection test failed:",

        // Settings Screen
        "settings_title" to "Application Settings",
        "settings_tab_appearance" to "Appearance & Language",
        "settings_tab_wallpaper" to "Wallpapers & FX",
        "settings_tab_ai" to "AI & Providers",
        "settings_tab_behavior" to "Behavior & Roleplay",
        "settings_tab_system" to "System & Backup",

        "settings_language_section" to "Interface Language & Localization",
        "settings_language_desc" to "Select active language, export all UI strings or import custom JSON language packs",
        "settings_export_lang_btn" to "Export Language Pack (JSON)",
        "settings_import_lang_btn" to "Import Language from File (JSON)",
        "settings_delete_lang_btn" to "Delete Language Pack",
        "settings_lang_keys_count" to "Total localized strings",

        "settings_theme_title" to "Interface Theme",
        "settings_theme_dark" to "Dark Theme",
        "settings_theme_light" to "Light Theme",
        "settings_theme_system" to "System Theme",
        "settings_accent_color" to "UI Accent Color",
        "settings_bubble_style_title" to "Message Presentation Mode",
        "settings_bubble_chat" to "Chat Bubbles",
        "settings_bubble_novel" to "Visual Novel",
        "settings_font_size" to "Dialogue Font Size",
        "settings_wallpaper_parallax" to "3D Wallpaper Parallax Motion",
        "settings_wallpaper_blur" to "Chat Background Blur",
        "settings_wallpaper_vignette" to "Vignette Edge Darkening",

        "settings_roleplay_mode_title" to "Default Roleplay Narrative Mode",
        "settings_roleplay_mode_desc" to "Baseline vocabulary depth and narrative complexity for new characters",
        "settings_response_length_title" to "Default Response Volume",
        "settings_response_length_desc" to "Baseline token length for bot responses",
        "settings_atmosphere_title" to "Dialogue Atmosphere & Vibe",
        "settings_atmosphere_desc" to "Injects mood, sensory details, and atmospheric cues into system instructions",
        "settings_nsfw_toggle_title" to "Mature Content (18+ / NSFW)",
        "settings_nsfw_toggle_desc" to "Permit unfiltered adult narrative themes and intimacy",
        "settings_streaming_title" to "Real-Time Token Streaming",
        "settings_streaming_desc" to "Render response tokens interactively as they arrive",
        "settings_haptics_title" to "Tactile Haptics (Vibration)",
        "settings_haptics_desc" to "Subtle haptic pulses on typing and key actions",

        // User Personas & Other Screens
        "personas_title" to "User Personas",
        "personas_desc" to "Create multiple persona profiles (name, personality, appearance) for dialogues",
        "personas_create_btn" to "Create Persona",
        "stories_title" to "Stories & Scenarios",
        "comics_title" to "Scene Comics Gallery",
        "backup_title" to "Backup & Restore",
        "backup_create_btn" to "Create Full Backup (JSON)",
        "backup_restore_btn" to "Restore Data from Backup",
        "relationship_title" to "Relationship Progression"
    )

    private val femboyStrings = mapOf(
        // App Core & Navigation
        "app_title" to "Untitled (Ня~)",
        "app_subtitle" to "Миленький ролплей, чулочки и новеллки (uwu)",
        "tab_chats" to "Чатики 💬",
        "tab_characters" to "Персики / Ботики 🐾",
        "tab_stories" to "Сюжетики 📖",
        "tab_comics" to "Комиксики 🎨",
        "tab_personas" to "Масочки / Персоны 🎭",
        "tab_providers" to "ИИ-Няшки / Провайдеры ⚡",
        "tab_settings" to "Настроечки ⚙️",
        "tab_backup" to "Сохраняшки 📦",

        // Common Buttons & Actions
        "btn_save" to "Сохранить (uwu)",
        "btn_cancel" to "Отменка",
        "btn_delete" to "Стереть",
        "btn_edit" to "Подправить",
        "btn_close" to "Закрыть лапкой 🐾",
        "btn_apply" to "Применить (ня~)",
        "btn_confirm" to "Подтвердить!",
        "btn_create" to "Создать милашку ✨",
        "btn_import" to "Загрузить карточку",
        "btn_export" to "Поделиться лапкой",
        "btn_copy" to "Цап-царап (Скопировать)",
        "btn_share" to "Помурчать друзьям",
        "btn_retry" to "Ещё раз! (nya)",
        "btn_reset" to "Сбросить в ноль",
        "btn_search" to "Поискать мармеладки 🍬",
        "btn_generate" to "Сгенерировать (owo)",
        "btn_send" to "Мурк! (Отправить)",
        "btn_stop" to "Стоппп!",

        // Character List Screen
        "search_characters_hint" to "Поискать фембойчиков и лапок... 🐾",
        "filter_all" to "Все милашки",
        "filter_favorites" to "Любимки 💖",
        "filter_nsfw" to "18+ Тян/Кун 🔞",
        "filter_recent" to "Свежие",
        "sort_by" to "Сортировочка",
        "sort_newest" to "По новизне (ня)",
        "sort_name" to "По имени (owo)",
        "sort_activity" to "По активности",
        "create_new_character" to "Создать фембойчика / бота 🐾",
        "import_character_card" to "Загрузить картиночку (PNG / JSON)",
        "no_characters_title" to "Где все милашки? (OwO)",
        "no_characters_desc" to "Создай своего первого фембойчика или загрузи готовую карточку!",
        "start_chat" to "Помурчать в чатике",
        "continue_chat" to "Продолжить помурчать",
        "character_info" to "Анкета милашки",
        "export_tavern_card" to "Сохранить карточку",
        "delete_character_confirm" to "Убрать этого зайку из списка?",

        // Chat Screen
        "chat_input_hint" to "Напиши реплику или обними лапками... (uwu)",
        "chat_action_btn" to "Сделать действие *...*",
        "chat_regenerate" to "Переделать ответик (ня)",
        "chat_edit_message" to "Поправить реплику",
        "chat_delete_message" to "Стереть сообщение",
        "chat_copy_message" to "Цап-царап текст",
        "chat_branch_navigation" to "Вариантик ответа",
        "chat_typing_indicator" to "печатает ответик... 🐾",
        "chat_generating_indicator" to "думает лапками... ⚡",
        "chat_tokens_used" to "Кушать токенов",
        "chat_model_label" to "Моделька",
        "chat_clear_history" to "Очистить историю чатика",
        "chat_delete_dialogue" to "Удалить этот чатик",
        "chat_settings_btn" to "Параметры мурчания",
        "chat_photo_btn" to "Сгенерировать артик",
        "chat_tts_btn" to "Озвучить голоском",
        "chat_comic_btn" to "Сделать комиксик",
        "chat_memory_btn" to "Память и мармеладки",
        "chat_relationship_btn" to "Уровень милоты",
        "chat_persona_select" to "Выбрать масочку",
        "chat_no_messages" to "Начни историю первым сообщением или обними лапкой! (uwu)",
        "chat_active_persona_none" to "Без масочки (Стандарт)",

        // Chat Settings Screen
        "chat_settings_title" to "Параметры чатика",
        "chat_settings_ai_params" to "Параметры фантазии ИИ",
        "chat_settings_temp" to "Креативность / Фантазия",
        "chat_settings_temp_desc" to "0.1 - строгость и логика, 1.2+ - безумная милота",
        "chat_settings_top_p" to "Top-P выборка",
        "chat_settings_presence" to "Разнообразие словечек",
        "chat_settings_freq" to "Штраф за повторы",
        "chat_settings_max_tokens" to "Лимит токенов ответа",
        "chat_settings_context_size" to "Память чатика (Сообщений)",
        "chat_settings_system_prompt" to "Свой системный промпт",
        "chat_settings_system_prompt_desc" to "Переопределяет инструкции для этого чатика",
        "chat_settings_jailbreak" to "Uncensored / NSFW фембой режим",
        "chat_settings_memory_enable" to "Авто-память и факт-сборник",
        "chat_settings_wallpaper" to "Фоник для этого чатика",
        "chat_settings_reset_btn" to "Сбросить настроечки",

        // Character Editor Screen
        "editor_title_create" to "Создание милашки",
        "editor_title_edit" to "Редактирование милашки",
        "editor_name" to "Имя милашки",
        "editor_name_hint" to "Например: Феликс, Астрель",
        "editor_tagline" to "Краткий слоган / Титул",
        "editor_tagline_hint" to "Например: Милый фембойчик в полосатых чулочках",
        "editor_description" to "Биография и описание",
        "editor_description_hint" to "История персонажа, любимые мармеладки и увлечения...",
        "editor_personality" to "Характер и милота",
        "editor_personality_hint" to "Черты личности, привычка делать ня~, полосатые чулочки...",
        "editor_scenario" to "Сценарий и обстановка",
        "editor_scenario_hint" to "Где вы встретились, уютная комнатка...",
        "editor_greeting" to "Первое сообщение (Приветствие)",
        "editor_greeting_hint" to "Первая реплика: *смущенно машет лапкой* Приветик! (uwu)",
        "editor_dialogue_examples" to "Примеры реплик (<START>...)",
        "editor_appearance" to "Внешность, чулочки и ушки",
        "editor_appearance_hint" to "Мягкие волосы, розовые чулочки, милые лапки...",
        "editor_avatar" to "Главный аватарчик",
        "editor_avatar_pick" to "Выбрать фоточку",
        "editor_reference_photos" to "Галерея артов",
        "editor_add_photo" to "Добавить артик",
        "editor_lora_tags" to "LoRA теги",
        "editor_nsfw_flag" to "Метка 18+ (NSFW)",
        "editor_tags" to "Теги и категории",
        "editor_tags_hint" to "Фембой, Фурри, Ня, Аниме, Милота...",

        // AI Providers Screen
        "providers_title" to "ИИ-Провайдеры и Няшки",
        "providers_desc" to "Управление API-ключами и модельками",
        "providers_add_btn" to "Добавить провайдера",
        "providers_profile_name" to "Название профиля",
        "providers_type" to "Тип провайдера",
        "providers_model" to "Моделька ИИ",
        "providers_base_url" to "Базовый URL",
        "providers_api_key" to "API Ключик",
        "providers_test_btn" to "Проверить связь (ня)",
        "providers_default_text" to "Основной для чатиков",
        "providers_default_image" to "Основной для картинок",
        "providers_active_badge" to "АКТИВЕН 🐾",
        "providers_free_badge" to "БЕСПЛАТНО 🌸",
        "providers_test_success" to "✅ Связь с ИИ установлена! Мурк!",
        "providers_test_error" to "❌ Ошибочка связи:",

        // Settings Screen
        "settings_title" to "Настроечки приложения",
        "settings_tab_appearance" to "Оформление & Язык",
        "settings_tab_wallpaper" to "Обои & Вайбик",
        "settings_tab_ai" to "ИИ & Провайдеры",
        "settings_tab_behavior" to "Поведение & Чатики",
        "settings_tab_system" to "Система & Бэкапы",

        "settings_language_section" to "Язык интерфейса / Language",
        "settings_language_desc" to "Выбор языка: Русский, English или Фембойский (Ня~)",
        "settings_export_lang_btn" to "Экспорт языкового пакета (JSON)",
        "settings_import_lang_btn" to "Импорт языка из файла (JSON)",
        "settings_delete_lang_btn" to "Удалить языковой пакет",
        "settings_lang_keys_count" to "Количество локализованных строк",

        "settings_theme_title" to "Цветовая тема оформления",
        "settings_theme_dark" to "Тёмная тема",
        "settings_theme_light" to "Светлая тема",
        "settings_theme_system" to "Системная тема",
        "settings_accent_color" to "Акцентный цвет интерфейса",
        "settings_bubble_style_title" to "Стиль диалогов",
        "settings_bubble_chat" to "Сообщения (Бабблы)",
        "settings_bubble_novel" to "Визуальная новелла",
        "settings_font_size" to "Размер шрифта сообщений",
        "settings_wallpaper_parallax" to "3D Параллакс при наклоне",
        "settings_wallpaper_blur" to "Размытие фона чатика",
        "settings_wallpaper_vignette" to "Виньетка / Затемнение",

        "settings_roleplay_mode_title" to "Режим ролплея",
        "settings_roleplay_mode_desc" to "Базовый стиль изложения для новых фембойчиков",
        "settings_response_length_title" to "Объем ответов",
        "settings_response_length_desc" to "Базовый размер ответа ИИ",
        "settings_atmosphere_title" to "Атмосфера и вайбик",
        "settings_atmosphere_desc" to "Передача настроения и эмоций в промпт",
        "settings_nsfw_toggle_title" to "Контент 18+ (NSFW)",
        "settings_nsfw_toggle_desc" to "Разрешить взрослые темы, страсть и нецензурированные сцены",
        "settings_streaming_title" to "Потоковая генерация",
        "settings_streaming_desc" to "Печатать текст в реальном времени",
        "settings_haptics_title" to "Тактильный отклик (Вибрация)",
        "settings_haptics_desc" to "Приятные микро-вибрации при касании",

        // User Personas & Other Screens
        "personas_title" to "Персоны пользователя",
        "personas_desc" to "Создавайте разные образы себя для диалогов",
        "personas_create_btn" to "Создать персону",
        "stories_title" to "Сюжетики и Сценарии",
        "comics_title" to "Галерея комиксов",
        "backup_title" to "Сохраняшки и Восстановление",
        "backup_create_btn" to "Создать бэкап (JSON)",
        "backup_restore_btn" to "Восстановить из бэкапа",
        "relationship_title" to "История милоты и отношений"
    )
}
