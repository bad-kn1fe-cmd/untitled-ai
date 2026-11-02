package com.example.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_settings")

data class CustomChatFolder(
    val id: String,
    val name: String,
    val icon: String = "folder",
    val characterIds: List<Long> = emptyList()
)

data class UserSettings(
    val themeMode: String = "dark", // "dark", "light", "system"
    val accentColorLong: Long = 0xFFD0BCFFL, // Sophisticated Lavender
    val chatWallpaperPreset: String = "pattern_chat_icons", // "none", "pattern_chat_icons", "pattern_stars_constellation", "pattern_geometric_mesh", "pattern_aurora_gradient", "custom"
    val customWallpaperUri: String = "",
    val customWallpapersJson: String = "[]",
    val parallaxEnabled: Boolean = true, // Parallax accelerometer sensor movement
    val chatWallpaperAlpha: Float = 0.28f,
    val fontSizeSp: Int = 15,
    val bubbleStyle: String = "novel", // "bubbles", "novel"
    val showAvatarInChat: Boolean = true,
    val characterLimitMode: String = "standard", // "standard" (50000), "extended" (50000)
    val contextWindowSize: Int = 12,
    val showInternalReasoningDebug: Boolean = false,
    val globalDefaultNsfw: Boolean = false,
    val textingModeDelayPreset: String = "normal", // "fast", "normal", "slow", "custom", "instant"
    val textingModeCustomSeconds: Int = 4,
    val showTypingIndicator: Boolean = true,
    val showTextingModeSystemMessages: Boolean = true,
    val isAgeConfirmed: Boolean = false,
    val eternalAssistantMode: Boolean = false,
    val naturalHapticsEnabled: Boolean = true,
    val backgroundNotificationsEnabled: Boolean = true,
    val civitaiBlueBuzz: Int = 1000,
    val civitaiOrangeBuzz: Int = 5000,
    val civitaiGreenBuzz: Int = 2500,
    val customSystemPrompt: String = "",
    val systemPromptVersion: String = "",
    val downloadedSystemPrompt: String = "",
    val isSystemPromptUserModified: Boolean = false,
    val isSystemPromptFirstLaunchDone: Boolean = false,
    val openRouterFallbackModels: String = "google/gemma-4-31b-it:free,google/gemma-4-26b-a4b-it:free,z-ai/glm-5.2:free,openai/gpt-oss-20b:free",
    val defaultTextProviderId: Long? = null,
    val chatScrollAnimation: String = "spring", // "spring", "bounce", "cinematic", "instant"
    val streamingEnabled: Boolean = true, // Beta streaming tokens real-time
    val animationPerformanceMode: String = "smooth", // "smooth", "fast", "off"
    val novelDividerEnabled: Boolean = true,
    val novelDividerWidthPercent: Int = 35, // 10% - 100%
    val novelDividerThicknessDp: Int = 1, // 1 - 5 dp
    val novelDividerAlpha: Float = 0.35f, // 0.1f - 1.0f
    val novelDividerColorHex: String = "#FFFFFF",
    val novelTextAlignment: String = "left", // "left", "center", "right"
    val atmosphereSelectionEnabled: Boolean = false, // Включение выбора атмосферы
    val defaultAtmosphere: String = "cozy", // cozy, noir, fantasy, cyberpunk, romance, thriller, mystic_forest, space, none
    val defaultRoleplayMode: String = "medium", // light, medium, hard, cinematic, psychological
    val defaultResponseLength: String = "medium", // short, medium, long, epic
    val defaultMaxTokens: Int = 1000, // Среднее количество токенов на ответ
    val tabSwitchAnimation: String = "smooth_slide", // "smooth_slide", "spring_slide", "depth_stack", "zoom_fade", "cube_3d", "vertical_lift", "shared_axis", "fade", "instant"
    val tabSwitchDurationMs: Int = 260, // 80 - 750 ms
    val hotbarAnimation: String = "spring", // "spring", "instant"
    val isHotbarVisible: Boolean = false,
    val voiceMessagesEnabled: Boolean = false,
    val botTextAppearanceMode: String = "typing", // "typing", "instant"
    val botTypingSpeedMs: Int = 15, // 1 - 100 ms per char
    val appLanguage: String = "ru", // "ru", "en"
    val geminiDebugEnabled: Boolean = false,
    val uiVariant: String = "vault_hub", // "vault_hub" (Хранилище с витриной), "classic" (Классический)
    val fontFamilyChoice: String = "system", // "system", "sans_serif", "serif", "monospace", "cursive", "custom"
    val customFontPath: String = "",
    val customFontName: String = "",
    val glassmorphismEnabled: Boolean = true, // "Жидкое стекло" в чате
    val wallpaperTone: String = "dark", // "dark", "light"
    val autoMemoryInterval: Int = 5, // Автоматическое обновление памяти каждые N сообщений
    val autoChronicleInterval: Int = 10, // Автоматическая хронизация/самари каждые N сообщений
    val appIconVariant: String = "purple", // "purple", "cyan", "pink", "emerald", "crimson", "gold", "dark", "arctic"
    val liveVideoWallpaperUri: String = "", // BETA: Живые видео-обои (.mp4 / .webm)
    val liveVideoWallpaperEnabled: Boolean = false,
    val glassBlurRadius: Float = 16f, // Радиус размытия жидкого стекла
    val glassAlpha: Float = 0.75f, // Прозрачность стекла
    val glassBorderGlow: Boolean = true, // Блик и свечение граней стекла в стиле iOS 26
    val glassSpecularIntensity: Float = 0.8f, // Интенсивность глянцевых бликов
    val appThemeBackgroundPreset: String = "dark_space", // "dark_space", "cyberpunk_neon", "sakura_gradient", "deep_ocean", "oled_black", "warm_sunset", "custom"
    val appCustomBackgroundUri: String = "",
    val appCustomBackgroundAlpha: Float = 0.15f,
    val vaultHubStartTab: String = "general", // "general" (Общее), "chats" (Чаты)
    val geminiSafetyThreshold: String = "BLOCK_NONE", // "BLOCK_NONE", "BLOCK_ONLY_HIGH", "BLOCK_MEDIUM_AND_ABOVE", "BLOCK_LOW_AND_ABOVE"
    val frameParallaxEnabled: Boolean = false, // По умолчанию выключена
    val frameParallaxSensitivity: Float = 1.0f, // Чувствительность 0.5x - 2.5x
    val frameParallaxMaxOffset: Float = 12f, // Максимальное смещение в dp (4 - 24)
    val userActiveFrameUri: String = "", // Текущая выбранная рамка пользователя
    val userFramesJson: String = "[]", // JSON список рамок пользователя
    val pumpkinRainDropLimit: Float = 0.7f, // 0.1f - 1.0f
    val pumpkinRainAlpha: Float = 0.45f, // 0.1f - 1.0f
    val customChatFoldersJson: String = "[]", // JSON список пользовательских вкладок/папок в диалогах
    val easterEggCode: String = "", // Поле ввода пасхалки
    val relationshipViewMode: String = "board", // "board" (Доска отношений БЕТА) или "table" (Таблица отношений)
    val customRelationshipTypesJson: String = "[]", // Пользовательские типы/цвета отношений для Доски отношений
    val isGeminiTutorialShown: Boolean = true, // true before DataStore emission, false on fresh install
    val verityUnlocked: Boolean = false, // Открыта ли пасхалка #VERITY
    val showVerityInHotbar: Boolean = true, // Показывать ли кнопку со смайликом в хотбаре
    val verityFormScore: Int = 0, // Баланс формы от -100 (Обессити) до +100 (Моггити), 0 = Верити
    val veritySatiety: Int = 75, // Сытость (0..100)
    val verityEnergy: Int = 85, // Бодрость (0..100)
    val verityMood: Int = 90, // Настроение (0..100)
    val verityMuscleMass: Int = 20, // Мышцы / Сигма-уровень (0..100)
    val verityFatMass: Int = 20, // Жирок / Чилл-уровень (0..100)
    val verityTotalActions: Int = 0, // Счётчик действий заботы
    val settingsCategoryIsolationMode: Boolean = true, // Изоляция вкладок в настройках
    val skibidiUnlocked: Boolean = false, // Открыта ли пасхалка SKIBIDILEAVE
    val accentTintIntensity: Float = 0.0f, // 0.0f - 0.40f Тонирование аватарок/баннеров под цвет темы
    val boykisserPetEnabled: Boolean = false, // Питомец Бойкиссер на экране
    val boykisserPetVariant: String = "classic", // "classic" или "3d"
    val boykisserPetSpeed: Float = 1.0f, // 0.5f - 2.5f Скорость бега
    val boykisserMainBehavior: String = "walk", // "walk" (Ходить) или "sit" (Сидеть)
    val boykisserChatBehavior: String = "walk", // "walk" (Ходить) или "sit" (Сидеть)
    val lastSeenWhatsNewVersion: String = "" // Последняя увиденная версия для "Что нового"
) {
    fun getCustomChatFolders(): List<CustomChatFolder> {
        val list = mutableListOf<CustomChatFolder>()
        try {
            val jsonArray = org.json.JSONArray(customChatFoldersJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                val name = obj.optString("name", "")
                val icon = obj.optString("icon", "folder")
                val charIdsJson = obj.optJSONArray("characterIds")
                val charIds = mutableListOf<Long>()
                if (charIdsJson != null) {
                    for (j in 0 until charIdsJson.length()) {
                        charIds.add(charIdsJson.optLong(j))
                    }
                }
                if (id.isNotBlank() && name.isNotBlank()) {
                    list.add(CustomChatFolder(id = id, name = name, icon = icon, characterIds = charIds))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    fun withAddedCustomChatFolder(folder: CustomChatFolder): UserSettings {
        val current = getCustomChatFolders().filter { it.id != folder.id }.toMutableList()
        current.add(folder)
        return copy(customChatFoldersJson = customFoldersToJson(current))
    }

    fun withUpdatedCustomChatFolder(folder: CustomChatFolder): UserSettings {
        val current = getCustomChatFolders().map { if (it.id == folder.id) folder else it }
        return copy(customChatFoldersJson = customFoldersToJson(current))
    }

    fun withRemovedCustomChatFolder(folderId: String): UserSettings {
        val current = getCustomChatFolders().filter { it.id != folderId }
        return copy(customChatFoldersJson = customFoldersToJson(current))
    }

    fun withBotInFolder(folderId: String, charId: Long, add: Boolean): UserSettings {
        val current = getCustomChatFolders().map { folder ->
            if (folder.id == folderId) {
                val newCharIds = if (add) {
                    if (folder.characterIds.contains(charId)) folder.characterIds else folder.characterIds + charId
                } else {
                    folder.characterIds.filter { it != charId }
                }
                folder.copy(characterIds = newCharIds)
            } else {
                folder
            }
        }
        return copy(customChatFoldersJson = customFoldersToJson(current))
    }

    private fun customFoldersToJson(list: List<CustomChatFolder>): String {
        val arr = org.json.JSONArray()
        for (item in list) {
            val obj = org.json.JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("icon", item.icon)
            val charArr = org.json.JSONArray()
            item.characterIds.forEach { charArr.put(it) }
            obj.put("characterIds", charArr)
            arr.put(obj)
        }
        return arr.toString()
    }
    val isSystemPromptUnmodified: Boolean
        get() {
            if (customSystemPrompt.isBlank()) return false
            if (isSystemPromptUserModified) return false
            if (downloadedSystemPrompt.isBlank()) return false
            return customSystemPrompt.trim() == downloadedSystemPrompt.trim()
        }

    fun getCustomWallpapersList(): List<String> {
        val list = mutableListOf<String>()
        try {
            val jsonArray = org.json.JSONArray(customWallpapersJson)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i)
                if (!item.isNullOrBlank() && !list.contains(item)) {
                    list.add(item)
                }
            }
        } catch (_: Exception) {}
        if (customWallpaperUri.isNotBlank() && !list.contains(customWallpaperUri)) {
            list.add(0, customWallpaperUri)
        }
        return list
    }

    fun withAddedCustomWallpaper(uri: String): UserSettings {
        val current = getCustomWallpapersList().toMutableList()
        if (!current.contains(uri)) {
            current.add(0, uri)
        }
        val jsonArray = org.json.JSONArray(current)
        return this.copy(
            customWallpaperUri = uri,
            chatWallpaperPreset = "custom",
            customWallpapersJson = jsonArray.toString()
        )
    }

    fun withRemovedCustomWallpaper(uri: String): UserSettings {
        val current = getCustomWallpapersList().toMutableList()
        current.remove(uri)
        val jsonArray = org.json.JSONArray(current)
        val newSelected = if (customWallpaperUri == uri) {
            current.firstOrNull() ?: ""
        } else {
            customWallpaperUri
        }
        val newPreset = if (newSelected.isBlank() && chatWallpaperPreset == "custom") "pattern_chat_icons" else chatWallpaperPreset
        return this.copy(
            customWallpaperUri = newSelected,
            chatWallpaperPreset = newPreset,
            customWallpapersJson = jsonArray.toString()
        )
    }
}

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_ACCENT_COLOR = longPreferencesKey("accent_color")
        val KEY_WALLPAPER_PRESET = stringPreferencesKey("wallpaper_preset")
        val KEY_CUSTOM_WALLPAPER_URI = stringPreferencesKey("custom_wallpaper_uri")
        val KEY_CUSTOM_WALLPAPERS_JSON = stringPreferencesKey("custom_wallpapers_json")
        val KEY_PARALLAX_ENABLED = booleanPreferencesKey("parallax_enabled")
        val KEY_WALLPAPER_ALPHA = floatPreferencesKey("wallpaper_alpha")
        val KEY_FONT_SIZE = intPreferencesKey("font_size")
        val KEY_BUBBLE_STYLE = stringPreferencesKey("bubble_style")
        val KEY_SHOW_AVATAR = booleanPreferencesKey("show_avatar")
        val KEY_CHARACTER_LIMIT = stringPreferencesKey("character_limit_mode")
        val KEY_CONTEXT_WINDOW = intPreferencesKey("context_window")
        val KEY_SHOW_REASONING = booleanPreferencesKey("show_reasoning")
        val KEY_DEFAULT_NSFW = booleanPreferencesKey("default_nsfw")
        val KEY_TEXTING_DELAY_PRESET = stringPreferencesKey("texting_delay_preset")
        val KEY_TEXTING_CUSTOM_SECONDS = intPreferencesKey("texting_custom_seconds")
        val KEY_SHOW_TYPING = booleanPreferencesKey("show_typing")
        val KEY_SHOW_TEXTING_SYSTEM_MSGS = booleanPreferencesKey("show_texting_system_msgs")
        val KEY_AGE_CONFIRMED = booleanPreferencesKey("age_confirmed")
        val KEY_ETERNAL_ASSISTANT = booleanPreferencesKey("eternal_assistant")
        val KEY_NATURAL_HAPTICS = booleanPreferencesKey("natural_haptics")
        val KEY_BG_NOTIFICATIONS = booleanPreferencesKey("background_notifications")
        val KEY_CIVITAI_BLUE_BUZZ = intPreferencesKey("civitai_blue_buzz")
        val KEY_CIVITAI_ORANGE_BUZZ = intPreferencesKey("civitai_orange_buzz")
        val KEY_CIVITAI_GREEN_BUZZ = intPreferencesKey("civitai_green_buzz")
        val KEY_CUSTOM_SYSTEM_PROMPT = stringPreferencesKey("custom_system_prompt")
        val KEY_SYSTEM_PROMPT_VERSION = stringPreferencesKey("system_prompt_version")
        val KEY_DOWNLOADED_SYSTEM_PROMPT = stringPreferencesKey("downloaded_system_prompt")
        val KEY_SYSTEM_PROMPT_USER_MODIFIED = booleanPreferencesKey("system_prompt_user_modified")
        val KEY_SYSTEM_PROMPT_FIRST_LAUNCH = booleanPreferencesKey("system_prompt_first_launch")
        val KEY_OPENROUTER_FALLBACKS = stringPreferencesKey("openrouter_fallbacks")
        val KEY_DEFAULT_TEXT_PROVIDER_ID = longPreferencesKey("default_text_provider_id")
        val KEY_CHAT_SCROLL_ANIMATION = stringPreferencesKey("chat_scroll_animation")
        val KEY_STREAMING_ENABLED = booleanPreferencesKey("streaming_enabled")
        val KEY_ANIMATION_PERFORMANCE_MODE = stringPreferencesKey("animation_perf_mode")
        val KEY_NOVEL_DIVIDER_ENABLED = booleanPreferencesKey("novel_divider_enabled")
        val KEY_NOVEL_DIVIDER_WIDTH = intPreferencesKey("novel_divider_width")
        val KEY_NOVEL_DIVIDER_THICKNESS = intPreferencesKey("novel_divider_thickness")
        val KEY_NOVEL_DIVIDER_ALPHA = floatPreferencesKey("novel_divider_alpha")
        val KEY_NOVEL_DIVIDER_COLOR = stringPreferencesKey("novel_divider_color")
        val KEY_NOVEL_TEXT_ALIGNMENT = stringPreferencesKey("novel_text_alignment")
        val KEY_ATMOSPHERE_SELECTION_ENABLED = booleanPreferencesKey("atmosphere_selection_enabled")
        val KEY_DEFAULT_ATMOSPHERE = stringPreferencesKey("default_atmosphere")
        val KEY_DEFAULT_ROLEPLAY_MODE = stringPreferencesKey("default_roleplay_mode")
        val KEY_DEFAULT_RESPONSE_LENGTH = stringPreferencesKey("default_response_length")
        val KEY_DEFAULT_MAX_TOKENS = intPreferencesKey("default_max_tokens")
        val KEY_TAB_SWITCH_ANIMATION = stringPreferencesKey("tab_switch_animation")
        val KEY_HOTBAR_ANIMATION = stringPreferencesKey("hotbar_animation")
        val KEY_IS_HOTBAR_VISIBLE = booleanPreferencesKey("is_hotbar_visible")
        val KEY_VOICE_MESSAGES_ENABLED = booleanPreferencesKey("voice_messages_enabled")
        val KEY_BOT_TEXT_APPEARANCE_MODE = stringPreferencesKey("bot_text_appearance_mode")
        val KEY_BOT_TYPING_SPEED_MS = intPreferencesKey("bot_typing_speed_ms")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_GEMINI_DEBUG_ENABLED = booleanPreferencesKey("gemini_debug_enabled")
        val KEY_UI_VARIANT = stringPreferencesKey("ui_variant")
        val KEY_FONT_FAMILY_CHOICE = stringPreferencesKey("font_family_choice")
        val KEY_CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val KEY_CUSTOM_FONT_NAME = stringPreferencesKey("custom_font_name")
        val KEY_GLASSMORPHISM_ENABLED = booleanPreferencesKey("glassmorphism_enabled")
        val KEY_WALLPAPER_TONE = stringPreferencesKey("wallpaper_tone")
        val KEY_AUTO_MEMORY_INTERVAL = intPreferencesKey("auto_memory_interval")
        val KEY_AUTO_CHRONICLE_INTERVAL = intPreferencesKey("auto_chronicle_interval")
        val KEY_APP_ICON_VARIANT = stringPreferencesKey("app_icon_variant")
        val KEY_LIVE_VIDEO_WALLPAPER_URI = stringPreferencesKey("live_video_wallpaper_uri")
        val KEY_LIVE_VIDEO_WALLPAPER_ENABLED = booleanPreferencesKey("live_video_wallpaper_enabled")
        val KEY_GLASS_BLUR_RADIUS = floatPreferencesKey("glass_blur_radius")
        val KEY_GLASS_ALPHA = floatPreferencesKey("glass_alpha")
        val KEY_GLASS_BORDER_GLOW = booleanPreferencesKey("glass_border_glow")
        val KEY_GLASS_SPECULAR_INTENSITY = floatPreferencesKey("glass_specular_intensity")
        val KEY_APP_THEME_BG_PRESET = stringPreferencesKey("app_theme_bg_preset")
        val KEY_APP_CUSTOM_BG_URI = stringPreferencesKey("app_custom_bg_uri")
        val KEY_APP_CUSTOM_BG_ALPHA = floatPreferencesKey("app_custom_bg_alpha")
        val KEY_VAULT_HUB_START_TAB = stringPreferencesKey("vault_hub_start_tab")
        val KEY_GEMINI_SAFETY_THRESHOLD = stringPreferencesKey("gemini_safety_threshold")
        val KEY_FRAME_PARALLAX_ENABLED = booleanPreferencesKey("frame_parallax_enabled")
        val KEY_FRAME_PARALLAX_SENSITIVITY = floatPreferencesKey("frame_parallax_sensitivity")
        val KEY_FRAME_PARALLAX_MAX_OFFSET = floatPreferencesKey("frame_parallax_max_offset")
        val KEY_USER_ACTIVE_FRAME_URI = stringPreferencesKey("user_active_frame_uri")
        val KEY_USER_FRAMES_JSON = stringPreferencesKey("user_frames_json")
        val KEY_PUMPKIN_RAIN_DROP_LIMIT = floatPreferencesKey("pumpkin_rain_drop_limit")
        val KEY_PUMPKIN_RAIN_ALPHA = floatPreferencesKey("pumpkin_rain_alpha")
        val KEY_TAB_SWITCH_DURATION_MS = intPreferencesKey("tab_switch_duration_ms")
        val KEY_CUSTOM_CHAT_FOLDERS_JSON = stringPreferencesKey("custom_chat_folders_json")
        val KEY_EASTER_EGG_CODE = stringPreferencesKey("easter_egg_code")
        val KEY_RELATIONSHIP_VIEW_MODE = stringPreferencesKey("relationship_view_mode")
        val KEY_CUSTOM_REL_TYPES_JSON = stringPreferencesKey("custom_rel_types_json")
        val KEY_GEMINI_TUTORIAL_SHOWN = booleanPreferencesKey("gemini_tutorial_shown")
        val KEY_VERITY_UNLOCKED = booleanPreferencesKey("verity_unlocked")
        val KEY_SHOW_VERITY_IN_HOTBAR = booleanPreferencesKey("show_verity_in_hotbar")
        val KEY_VERITY_FORM_SCORE = intPreferencesKey("verity_form_score")
        val KEY_VERITY_SATIETY = intPreferencesKey("verity_satiety")
        val KEY_VERITY_ENERGY = intPreferencesKey("verity_energy")
        val KEY_VERITY_MOOD = intPreferencesKey("verity_mood")
        val KEY_VERITY_MUSCLE_MASS = intPreferencesKey("verity_muscle_mass")
        val KEY_VERITY_FAT_MASS = intPreferencesKey("verity_fat_mass")
        val KEY_VERITY_TOTAL_ACTIONS = intPreferencesKey("verity_total_actions")
        val KEY_SETTINGS_CATEGORY_ISOLATION = booleanPreferencesKey("settings_category_isolation")
        val KEY_SKIBIDI_UNLOCKED = booleanPreferencesKey("skibidi_unlocked")
        val KEY_ACCENT_TINT_INTENSITY = floatPreferencesKey("accent_tint_intensity")
        val KEY_BOYKISSER_PET_ENABLED = booleanPreferencesKey("boykisser_pet_enabled")
        val KEY_BOYKISSER_PET_VARIANT = stringPreferencesKey("boykisser_pet_variant")
        val KEY_BOYKISSER_PET_SPEED = floatPreferencesKey("boykisser_pet_speed")
        val KEY_BOYKISSER_MAIN_BEHAVIOR = stringPreferencesKey("boykisser_main_behavior")
        val KEY_BOYKISSER_CHAT_BEHAVIOR = stringPreferencesKey("boykisser_chat_behavior")
        val KEY_LAST_SEEN_WHATS_NEW_VERSION = stringPreferencesKey("last_seen_whats_new_version")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { pref ->
        UserSettings(
            themeMode = pref[KEY_THEME] ?: "dark",
            accentColorLong = pref[KEY_ACCENT_COLOR] ?: 0xFFD0BCFFL,
            chatWallpaperPreset = pref[KEY_WALLPAPER_PRESET] ?: "pattern_chat_icons",
            customWallpaperUri = pref[KEY_CUSTOM_WALLPAPER_URI] ?: "",
            customWallpapersJson = pref[KEY_CUSTOM_WALLPAPERS_JSON] ?: "[]",
            parallaxEnabled = pref[KEY_PARALLAX_ENABLED] ?: true,
            chatWallpaperAlpha = pref[KEY_WALLPAPER_ALPHA] ?: 0.28f,
            fontSizeSp = pref[KEY_FONT_SIZE] ?: 15,
            bubbleStyle = pref[KEY_BUBBLE_STYLE] ?: "bubbles",
            showAvatarInChat = pref[KEY_SHOW_AVATAR] ?: true,
            characterLimitMode = pref[KEY_CHARACTER_LIMIT] ?: "standard",
            contextWindowSize = pref[KEY_CONTEXT_WINDOW] ?: 12,
            showInternalReasoningDebug = pref[KEY_SHOW_REASONING] ?: false,
            globalDefaultNsfw = pref[KEY_DEFAULT_NSFW] ?: false,
            textingModeDelayPreset = pref[KEY_TEXTING_DELAY_PRESET] ?: "normal",
            textingModeCustomSeconds = pref[KEY_TEXTING_CUSTOM_SECONDS] ?: 4,
            showTypingIndicator = pref[KEY_SHOW_TYPING] ?: true,
            showTextingModeSystemMessages = pref[KEY_SHOW_TEXTING_SYSTEM_MSGS] ?: true,
            isAgeConfirmed = pref[KEY_AGE_CONFIRMED] ?: false,
            eternalAssistantMode = pref[KEY_ETERNAL_ASSISTANT] ?: true,
            naturalHapticsEnabled = false,
            backgroundNotificationsEnabled = pref[KEY_BG_NOTIFICATIONS] ?: true,
            civitaiBlueBuzz = pref[KEY_CIVITAI_BLUE_BUZZ] ?: 1000,
            civitaiOrangeBuzz = pref[KEY_CIVITAI_ORANGE_BUZZ] ?: 5000,
            civitaiGreenBuzz = pref[KEY_CIVITAI_GREEN_BUZZ] ?: 2500,
            customSystemPrompt = pref[KEY_CUSTOM_SYSTEM_PROMPT] ?: "",
            systemPromptVersion = pref[KEY_SYSTEM_PROMPT_VERSION] ?: "",
            downloadedSystemPrompt = pref[KEY_DOWNLOADED_SYSTEM_PROMPT] ?: "",
            isSystemPromptUserModified = pref[KEY_SYSTEM_PROMPT_USER_MODIFIED] ?: false,
            isSystemPromptFirstLaunchDone = pref[KEY_SYSTEM_PROMPT_FIRST_LAUNCH] ?: false,
            openRouterFallbackModels = pref[KEY_OPENROUTER_FALLBACKS] ?: "google/gemma-4-31b-it:free,google/gemma-4-26b-a4b-it:free,z-ai/glm-5.2:free,openai/gpt-oss-20b:free",
            defaultTextProviderId = pref[KEY_DEFAULT_TEXT_PROVIDER_ID],
            chatScrollAnimation = pref[KEY_CHAT_SCROLL_ANIMATION] ?: "spring",
            streamingEnabled = false,
            animationPerformanceMode = pref[KEY_ANIMATION_PERFORMANCE_MODE] ?: "smooth",
            novelDividerEnabled = pref[KEY_NOVEL_DIVIDER_ENABLED] ?: true,
            novelDividerWidthPercent = pref[KEY_NOVEL_DIVIDER_WIDTH] ?: 35,
            novelDividerThicknessDp = pref[KEY_NOVEL_DIVIDER_THICKNESS] ?: 1,
            novelDividerAlpha = pref[KEY_NOVEL_DIVIDER_ALPHA] ?: 0.35f,
            novelDividerColorHex = pref[KEY_NOVEL_DIVIDER_COLOR] ?: "#FFFFFF",
            novelTextAlignment = pref[KEY_NOVEL_TEXT_ALIGNMENT] ?: "left",
            atmosphereSelectionEnabled = false,
            defaultAtmosphere = pref[KEY_DEFAULT_ATMOSPHERE] ?: "cozy",
            defaultRoleplayMode = pref[KEY_DEFAULT_ROLEPLAY_MODE] ?: "medium",
            defaultResponseLength = pref[KEY_DEFAULT_RESPONSE_LENGTH] ?: "medium",
            defaultMaxTokens = pref[KEY_DEFAULT_MAX_TOKENS] ?: 1000,
            tabSwitchAnimation = pref[KEY_TAB_SWITCH_ANIMATION] ?: "smooth_slide",
            tabSwitchDurationMs = (pref[KEY_TAB_SWITCH_DURATION_MS] ?: 260).coerceIn(60, 1200),
            hotbarAnimation = pref[KEY_HOTBAR_ANIMATION] ?: "spring",
            isHotbarVisible = pref[KEY_IS_HOTBAR_VISIBLE] ?: false,
            voiceMessagesEnabled = pref[KEY_VOICE_MESSAGES_ENABLED] ?: false,
            botTextAppearanceMode = pref[KEY_BOT_TEXT_APPEARANCE_MODE] ?: "typing",
            botTypingSpeedMs = pref[KEY_BOT_TYPING_SPEED_MS] ?: 15,
            appLanguage = pref[KEY_APP_LANGUAGE] ?: "ru",
            geminiDebugEnabled = pref[KEY_GEMINI_DEBUG_ENABLED] ?: false,
            uiVariant = pref[KEY_UI_VARIANT] ?: "vault_hub",
            fontFamilyChoice = pref[KEY_FONT_FAMILY_CHOICE] ?: "system",
            customFontPath = pref[KEY_CUSTOM_FONT_PATH] ?: "",
            customFontName = pref[KEY_CUSTOM_FONT_NAME] ?: "",
            glassmorphismEnabled = pref[KEY_GLASSMORPHISM_ENABLED] ?: true,
            wallpaperTone = pref[KEY_WALLPAPER_TONE] ?: "dark",
            autoMemoryInterval = pref[KEY_AUTO_MEMORY_INTERVAL] ?: 5,
            autoChronicleInterval = pref[KEY_AUTO_CHRONICLE_INTERVAL] ?: 10,
            appIconVariant = pref[KEY_APP_ICON_VARIANT] ?: "purple",
            liveVideoWallpaperUri = pref[KEY_LIVE_VIDEO_WALLPAPER_URI] ?: "",
            liveVideoWallpaperEnabled = pref[KEY_LIVE_VIDEO_WALLPAPER_ENABLED] ?: false,
            glassBlurRadius = pref[KEY_GLASS_BLUR_RADIUS] ?: 16f,
            glassAlpha = pref[KEY_GLASS_ALPHA] ?: 0.75f,
            glassBorderGlow = pref[KEY_GLASS_BORDER_GLOW] ?: true,
            glassSpecularIntensity = pref[KEY_GLASS_SPECULAR_INTENSITY] ?: 0.8f,
            appThemeBackgroundPreset = pref[KEY_APP_THEME_BG_PRESET] ?: "dark_space",
            appCustomBackgroundUri = pref[KEY_APP_CUSTOM_BG_URI] ?: "",
            appCustomBackgroundAlpha = pref[KEY_APP_CUSTOM_BG_ALPHA] ?: 0.15f,
            vaultHubStartTab = pref[KEY_VAULT_HUB_START_TAB] ?: "general",
            geminiSafetyThreshold = pref[KEY_GEMINI_SAFETY_THRESHOLD] ?: "BLOCK_NONE",
            frameParallaxEnabled = pref[KEY_FRAME_PARALLAX_ENABLED] ?: false,
            frameParallaxSensitivity = pref[KEY_FRAME_PARALLAX_SENSITIVITY] ?: 1.0f,
            frameParallaxMaxOffset = pref[KEY_FRAME_PARALLAX_MAX_OFFSET] ?: 12f,
            userActiveFrameUri = pref[KEY_USER_ACTIVE_FRAME_URI] ?: "",
            userFramesJson = pref[KEY_USER_FRAMES_JSON] ?: "[]",
            pumpkinRainDropLimit = pref[KEY_PUMPKIN_RAIN_DROP_LIMIT] ?: 0.7f,
            pumpkinRainAlpha = (pref[KEY_PUMPKIN_RAIN_ALPHA] ?: 0.45f).coerceIn(0.05f, 1.0f),
            customChatFoldersJson = pref[KEY_CUSTOM_CHAT_FOLDERS_JSON] ?: "[]",
            easterEggCode = pref[KEY_EASTER_EGG_CODE] ?: "",
            relationshipViewMode = pref[KEY_RELATIONSHIP_VIEW_MODE] ?: "board",
            customRelationshipTypesJson = pref[KEY_CUSTOM_REL_TYPES_JSON] ?: "[]",
            isGeminiTutorialShown = pref[KEY_GEMINI_TUTORIAL_SHOWN] ?: false,
            verityUnlocked = (pref[KEY_VERITY_UNLOCKED] ?: false) || (pref[KEY_EASTER_EGG_CODE]?.trim()?.removePrefix("#")?.equals("VERITY", ignoreCase = true) == true),
            showVerityInHotbar = pref[KEY_SHOW_VERITY_IN_HOTBAR] ?: true,
            verityFormScore = (pref[KEY_VERITY_FORM_SCORE] ?: 0).coerceIn(-100, 100),
            veritySatiety = (pref[KEY_VERITY_SATIETY] ?: 75).coerceIn(0, 100),
            verityEnergy = (pref[KEY_VERITY_ENERGY] ?: 85).coerceIn(0, 100),
            verityMood = (pref[KEY_VERITY_MOOD] ?: 90).coerceIn(0, 100),
            verityMuscleMass = (pref[KEY_VERITY_MUSCLE_MASS] ?: 20).coerceIn(0, 100),
            verityFatMass = (pref[KEY_VERITY_FAT_MASS] ?: 20).coerceIn(0, 100),
            verityTotalActions = (pref[KEY_VERITY_TOTAL_ACTIONS] ?: 0).coerceAtLeast(0),
            settingsCategoryIsolationMode = pref[KEY_SETTINGS_CATEGORY_ISOLATION] ?: true,
            skibidiUnlocked = (pref[KEY_SKIBIDI_UNLOCKED] ?: false) || (pref[KEY_EASTER_EGG_CODE]?.trim()?.removePrefix("#")?.equals("SKIBIDILEAVE", ignoreCase = true) == true),
            accentTintIntensity = (pref[KEY_ACCENT_TINT_INTENSITY] ?: 0.0f).coerceIn(0.0f, 0.40f),
            boykisserPetEnabled = pref[KEY_BOYKISSER_PET_ENABLED] ?: false,
            boykisserPetVariant = pref[KEY_BOYKISSER_PET_VARIANT] ?: "classic",
            boykisserPetSpeed = (pref[KEY_BOYKISSER_PET_SPEED] ?: 1.0f).coerceIn(0.5f, 2.5f),
            boykisserMainBehavior = pref[KEY_BOYKISSER_MAIN_BEHAVIOR] ?: "walk",
            boykisserChatBehavior = pref[KEY_BOYKISSER_CHAT_BEHAVIOR] ?: "walk",
            lastSeenWhatsNewVersion = pref[KEY_LAST_SEEN_WHATS_NEW_VERSION] ?: ""
        )
    }

    val userSettings: Flow<UserSettings> = userSettingsFlow

    suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { pref ->
            pref[KEY_THEME] = settings.themeMode
            pref[KEY_ACCENT_COLOR] = settings.accentColorLong
            pref[KEY_WALLPAPER_PRESET] = settings.chatWallpaperPreset
            pref[KEY_CUSTOM_WALLPAPER_URI] = settings.customWallpaperUri
            pref[KEY_CUSTOM_WALLPAPERS_JSON] = settings.customWallpapersJson
            pref[KEY_PARALLAX_ENABLED] = settings.parallaxEnabled
            pref[KEY_WALLPAPER_ALPHA] = settings.chatWallpaperAlpha
            pref[KEY_FONT_SIZE] = settings.fontSizeSp
            pref[KEY_BUBBLE_STYLE] = settings.bubbleStyle
            pref[KEY_SHOW_AVATAR] = settings.showAvatarInChat
            pref[KEY_CHARACTER_LIMIT] = settings.characterLimitMode
            pref[KEY_CONTEXT_WINDOW] = settings.contextWindowSize
            pref[KEY_SHOW_REASONING] = settings.showInternalReasoningDebug
            pref[KEY_DEFAULT_NSFW] = settings.globalDefaultNsfw
            pref[KEY_TEXTING_DELAY_PRESET] = settings.textingModeDelayPreset
            pref[KEY_TEXTING_CUSTOM_SECONDS] = settings.textingModeCustomSeconds
            pref[KEY_SHOW_TYPING] = settings.showTypingIndicator
            pref[KEY_SHOW_TEXTING_SYSTEM_MSGS] = settings.showTextingModeSystemMessages
            pref[KEY_AGE_CONFIRMED] = settings.isAgeConfirmed
            pref[KEY_ETERNAL_ASSISTANT] = settings.eternalAssistantMode
            pref[KEY_NATURAL_HAPTICS] = false
            pref[KEY_BG_NOTIFICATIONS] = settings.backgroundNotificationsEnabled
            pref[KEY_CIVITAI_BLUE_BUZZ] = settings.civitaiBlueBuzz
            pref[KEY_CIVITAI_ORANGE_BUZZ] = settings.civitaiOrangeBuzz
            pref[KEY_CIVITAI_GREEN_BUZZ] = settings.civitaiGreenBuzz
            pref[KEY_CUSTOM_SYSTEM_PROMPT] = settings.customSystemPrompt
            pref[KEY_SYSTEM_PROMPT_VERSION] = settings.systemPromptVersion
            pref[KEY_DOWNLOADED_SYSTEM_PROMPT] = settings.downloadedSystemPrompt
            pref[KEY_SYSTEM_PROMPT_USER_MODIFIED] = settings.isSystemPromptUserModified
            pref[KEY_SYSTEM_PROMPT_FIRST_LAUNCH] = settings.isSystemPromptFirstLaunchDone
            pref[KEY_OPENROUTER_FALLBACKS] = settings.openRouterFallbackModels
            pref[KEY_CHAT_SCROLL_ANIMATION] = settings.chatScrollAnimation
            pref[KEY_STREAMING_ENABLED] = false
            pref[KEY_ANIMATION_PERFORMANCE_MODE] = settings.animationPerformanceMode
            pref[KEY_NOVEL_DIVIDER_ENABLED] = settings.novelDividerEnabled
            pref[KEY_NOVEL_DIVIDER_WIDTH] = settings.novelDividerWidthPercent
            pref[KEY_NOVEL_DIVIDER_THICKNESS] = settings.novelDividerThicknessDp
            pref[KEY_NOVEL_DIVIDER_ALPHA] = settings.novelDividerAlpha
            pref[KEY_NOVEL_DIVIDER_COLOR] = settings.novelDividerColorHex
            pref[KEY_NOVEL_TEXT_ALIGNMENT] = settings.novelTextAlignment
            pref[KEY_ATMOSPHERE_SELECTION_ENABLED] = false
            pref[KEY_DEFAULT_ATMOSPHERE] = settings.defaultAtmosphere
            pref[KEY_DEFAULT_ROLEPLAY_MODE] = settings.defaultRoleplayMode
            pref[KEY_DEFAULT_RESPONSE_LENGTH] = settings.defaultResponseLength
            pref[KEY_DEFAULT_MAX_TOKENS] = settings.defaultMaxTokens
            pref[KEY_TAB_SWITCH_ANIMATION] = settings.tabSwitchAnimation
            pref[KEY_TAB_SWITCH_DURATION_MS] = settings.tabSwitchDurationMs.coerceIn(60, 1200)
            pref[KEY_HOTBAR_ANIMATION] = settings.hotbarAnimation
            pref[KEY_IS_HOTBAR_VISIBLE] = settings.isHotbarVisible
            pref[KEY_VOICE_MESSAGES_ENABLED] = settings.voiceMessagesEnabled
            pref[KEY_BOT_TEXT_APPEARANCE_MODE] = settings.botTextAppearanceMode
            pref[KEY_BOT_TYPING_SPEED_MS] = settings.botTypingSpeedMs
            pref[KEY_APP_LANGUAGE] = settings.appLanguage
            pref[KEY_GEMINI_DEBUG_ENABLED] = settings.geminiDebugEnabled
            pref[KEY_UI_VARIANT] = settings.uiVariant
            pref[KEY_FONT_FAMILY_CHOICE] = settings.fontFamilyChoice
            pref[KEY_CUSTOM_FONT_PATH] = settings.customFontPath
            pref[KEY_CUSTOM_FONT_NAME] = settings.customFontName
            pref[KEY_GLASSMORPHISM_ENABLED] = settings.glassmorphismEnabled
            pref[KEY_WALLPAPER_TONE] = settings.wallpaperTone
            pref[KEY_AUTO_MEMORY_INTERVAL] = settings.autoMemoryInterval
            pref[KEY_AUTO_CHRONICLE_INTERVAL] = settings.autoChronicleInterval
            pref[KEY_APP_ICON_VARIANT] = settings.appIconVariant
            pref[KEY_LIVE_VIDEO_WALLPAPER_URI] = settings.liveVideoWallpaperUri
            pref[KEY_LIVE_VIDEO_WALLPAPER_ENABLED] = settings.liveVideoWallpaperEnabled
            pref[KEY_GLASS_BLUR_RADIUS] = settings.glassBlurRadius
            pref[KEY_GLASS_ALPHA] = settings.glassAlpha
            pref[KEY_GLASS_BORDER_GLOW] = settings.glassBorderGlow
            pref[KEY_GLASS_SPECULAR_INTENSITY] = settings.glassSpecularIntensity
            pref[KEY_APP_THEME_BG_PRESET] = settings.appThemeBackgroundPreset
            pref[KEY_APP_CUSTOM_BG_URI] = settings.appCustomBackgroundUri
            pref[KEY_APP_CUSTOM_BG_ALPHA] = settings.appCustomBackgroundAlpha
            pref[KEY_VAULT_HUB_START_TAB] = settings.vaultHubStartTab
            pref[KEY_GEMINI_SAFETY_THRESHOLD] = settings.geminiSafetyThreshold
            pref[KEY_FRAME_PARALLAX_ENABLED] = settings.frameParallaxEnabled
            pref[KEY_FRAME_PARALLAX_SENSITIVITY] = settings.frameParallaxSensitivity
            pref[KEY_FRAME_PARALLAX_MAX_OFFSET] = settings.frameParallaxMaxOffset
            pref[KEY_USER_ACTIVE_FRAME_URI] = settings.userActiveFrameUri
            pref[KEY_USER_FRAMES_JSON] = settings.userFramesJson
            pref[KEY_PUMPKIN_RAIN_DROP_LIMIT] = settings.pumpkinRainDropLimit
            pref[KEY_PUMPKIN_RAIN_ALPHA] = settings.pumpkinRainAlpha.coerceIn(0.05f, 1.0f)
            pref[KEY_CUSTOM_CHAT_FOLDERS_JSON] = settings.customChatFoldersJson
            pref[KEY_EASTER_EGG_CODE] = settings.easterEggCode
            pref[KEY_RELATIONSHIP_VIEW_MODE] = settings.relationshipViewMode
            pref[KEY_CUSTOM_REL_TYPES_JSON] = settings.customRelationshipTypesJson
            pref[KEY_GEMINI_TUTORIAL_SHOWN] = settings.isGeminiTutorialShown
            pref[KEY_VERITY_UNLOCKED] = settings.verityUnlocked || settings.easterEggCode.trim().removePrefix("#").equals("VERITY", ignoreCase = true)
            pref[KEY_SHOW_VERITY_IN_HOTBAR] = settings.showVerityInHotbar
            pref[KEY_VERITY_FORM_SCORE] = settings.verityFormScore.coerceIn(-100, 100)
            pref[KEY_VERITY_SATIETY] = settings.veritySatiety.coerceIn(0, 100)
            pref[KEY_VERITY_ENERGY] = settings.verityEnergy.coerceIn(0, 100)
            pref[KEY_VERITY_MOOD] = settings.verityMood.coerceIn(0, 100)
            pref[KEY_VERITY_MUSCLE_MASS] = settings.verityMuscleMass.coerceIn(0, 100)
            pref[KEY_VERITY_FAT_MASS] = settings.verityFatMass.coerceIn(0, 100)
            pref[KEY_VERITY_TOTAL_ACTIONS] = settings.verityTotalActions.coerceAtLeast(0)
            pref[KEY_SETTINGS_CATEGORY_ISOLATION] = settings.settingsCategoryIsolationMode
            pref[KEY_SKIBIDI_UNLOCKED] = settings.skibidiUnlocked || settings.easterEggCode.trim().removePrefix("#").equals("SKIBIDILEAVE", ignoreCase = true)
            pref[KEY_ACCENT_TINT_INTENSITY] = settings.accentTintIntensity
            pref[KEY_BOYKISSER_PET_ENABLED] = settings.boykisserPetEnabled
            pref[KEY_BOYKISSER_PET_VARIANT] = settings.boykisserPetVariant
            pref[KEY_BOYKISSER_PET_SPEED] = settings.boykisserPetSpeed
            pref[KEY_BOYKISSER_MAIN_BEHAVIOR] = settings.boykisserMainBehavior
            pref[KEY_BOYKISSER_CHAT_BEHAVIOR] = settings.boykisserChatBehavior
            pref[KEY_LAST_SEEN_WHATS_NEW_VERSION] = settings.lastSeenWhatsNewVersion
            if (settings.defaultTextProviderId != null) {
                pref[KEY_DEFAULT_TEXT_PROVIDER_ID] = settings.defaultTextProviderId
            } else {
                pref.remove(KEY_DEFAULT_TEXT_PROVIDER_ID)
            }
        }
    }

    suspend fun markGeminiTutorialShown() {
        context.dataStore.edit { it[KEY_GEMINI_TUTORIAL_SHOWN] = true }
    }

    suspend fun updateUserActiveFrame(uri: String) {
        context.dataStore.edit { it[KEY_USER_ACTIVE_FRAME_URI] = uri }
    }

    suspend fun updateUserFramesJson(json: String) {
        context.dataStore.edit { it[KEY_USER_FRAMES_JSON] = json }
    }

    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun updateAccentColor(color: Long) {
        context.dataStore.edit { it[KEY_ACCENT_COLOR] = color }
    }
}
