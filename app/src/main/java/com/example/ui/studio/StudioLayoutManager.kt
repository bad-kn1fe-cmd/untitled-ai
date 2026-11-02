package com.example.ui.studio

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object StudioLayoutManager {
    private const val PREFS_NAME = "studio_layout_prefs"
    private const val KEY_ACTIVE_PRESET_ID = "active_preset_id"
    private const val KEY_CUSTOM_PRESETS_JSON = "custom_presets_json"

    // Default Elements Catalog across all screens
    val defaultElements: List<ElementLayoutConfig> = listOf(
        // Screen: Chats (Список чатов)
        ElementLayoutConfig(
            id = "chats_top_header",
            nameRu = "Шапка и логотип",
            screenId = "chats",
            categoryRu = "Верхняя панель",
            isDeletable = false,
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "chats_search_bar",
            nameRu = "Строка поиска",
            screenId = "chats",
            categoryRu = "Навигация",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "chats_category_chips",
            nameRu = "Фильтры и категории",
            screenId = "chats",
            categoryRu = "Фильтры",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "chats_character_card",
            nameRu = "Карточка персонажа в списке",
            screenId = "chats",
            categoryRu = "Список чатов",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "chats_fab_create",
            nameRu = "Кнопка «Создать персонажа»",
            screenId = "chats",
            categoryRu = "Действия",
            anchor = LayoutAnchor.BOTTOM_END
        ),
        ElementLayoutConfig(
            id = "chats_quick_menu",
            nameRu = "Кнопка быстрого меню",
            screenId = "chats",
            categoryRu = "Верхняя панель",
            anchor = LayoutAnchor.TOP_END
        ),

        // Screen: Chat Room (Диалог)
        ElementLayoutConfig(
            id = "chat_top_bar",
            nameRu = "Шапка диалога (Имя, Аватар)",
            screenId = "chat",
            categoryRu = "Верхняя панель",
            isDeletable = false,
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "chat_action_menu",
            nameRu = "Меню действий чата",
            screenId = "chat",
            categoryRu = "Верхняя панель",
            anchor = LayoutAnchor.TOP_END
        ),
        ElementLayoutConfig(
            id = "chat_branch_selector",
            nameRu = "Ветки диалога",
            screenId = "chat",
            categoryRu = "Верхняя панель",
            anchor = LayoutAnchor.TOP_START
        ),
        ElementLayoutConfig(
            id = "chat_user_bubble",
            nameRu = "Сообщение пользователя (Вы)",
            screenId = "chat",
            categoryRu = "Сообщения диалога",
            anchor = LayoutAnchor.TOP_END
        ),
        ElementLayoutConfig(
            id = "chat_bot_bubble",
            nameRu = "Сообщение персонажа (Бота)",
            screenId = "chat",
            categoryRu = "Сообщения диалога",
            anchor = LayoutAnchor.TOP_START
        ),
        ElementLayoutConfig(
            id = "chat_bot_avatar",
            nameRu = "Аватар персонажа в сообщении",
            screenId = "chat",
            categoryRu = "Сообщения диалога",
            anchor = LayoutAnchor.TOP_START
        ),
        ElementLayoutConfig(
            id = "chat_message_variant_bar",
            nameRu = "Панель вариантов/перегенерации",
            screenId = "chat",
            categoryRu = "Сообщения диалога",
            anchor = LayoutAnchor.BOTTOM_START
        ),
        ElementLayoutConfig(
            id = "chat_format_chips",
            nameRu = "Панель быстрых подсказок/действий",
            screenId = "chat",
            categoryRu = "Ввод текста",
            anchor = LayoutAnchor.BOTTOM_CENTER
        ),
        ElementLayoutConfig(
            id = "chat_input_bar",
            nameRu = "Поле ввода сообщения",
            screenId = "chat",
            categoryRu = "Ввод текста",
            isDeletable = false,
            anchor = LayoutAnchor.BOTTOM_CENTER
        ),
        ElementLayoutConfig(
            id = "chat_attach_photo_btn",
            nameRu = "Кнопка «Прикрепить фото»",
            screenId = "chat",
            categoryRu = "Вложения",
            anchor = LayoutAnchor.BOTTOM_START
        ),
        ElementLayoutConfig(
            id = "chat_attach_video_btn",
            nameRu = "Кнопка «Прикрепить видео»",
            screenId = "chat",
            categoryRu = "Вложения",
            anchor = LayoutAnchor.BOTTOM_START
        ),
        ElementLayoutConfig(
            id = "chat_send_btn",
            nameRu = "Кнопка «Отправить»",
            screenId = "chat",
            categoryRu = "Действия",
            isDeletable = false,
            anchor = LayoutAnchor.BOTTOM_END
        ),
        ElementLayoutConfig(
            id = "chat_voice_btn",
            nameRu = "Кнопка «Голосовое» (Микрофон)",
            screenId = "chat",
            categoryRu = "Действия",
            anchor = LayoutAnchor.BOTTOM_END
        ),
        ElementLayoutConfig(
            id = "chat_scroll_bottom_fab",
            nameRu = "Кнопка «Вниз к свежим»",
            screenId = "chat",
            categoryRu = "Навигация",
            anchor = LayoutAnchor.BOTTOM_END
        ),

        // Screen: Providers (ИИ Провайдеры)
        ElementLayoutConfig(
            id = "providers_top_bar",
            nameRu = "Шапка провайдеров",
            screenId = "providers",
            categoryRu = "Верхняя панель",
            isDeletable = false,
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "providers_add_btn",
            nameRu = "Кнопка «Добавить провайдер»",
            screenId = "providers",
            categoryRu = "Действия",
            anchor = LayoutAnchor.TOP_END
        ),
        ElementLayoutConfig(
            id = "providers_test_all_btn",
            nameRu = "Кнопка «Тест соединений»",
            screenId = "providers",
            categoryRu = "Действия",
            anchor = LayoutAnchor.TOP_END
        ),
        ElementLayoutConfig(
            id = "providers_search_filter",
            nameRu = "Поиск и фильтр моделей",
            screenId = "providers",
            categoryRu = "Фильтры",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "providers_card_item",
            nameRu = "Карточка профиля провайдера",
            screenId = "providers",
            categoryRu = "Список провайдеров",
            anchor = LayoutAnchor.TOP_CENTER
        ),

        // Screen: Personas (Персоны)
        ElementLayoutConfig(
            id = "personas_top_bar",
            nameRu = "Шапка персон",
            screenId = "personas",
            categoryRu = "Верхняя панель",
            isDeletable = false,
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "personas_add_btn",
            nameRu = "Кнопка «Новая персона»",
            screenId = "personas",
            categoryRu = "Действия",
            anchor = LayoutAnchor.TOP_END
        ),
        ElementLayoutConfig(
            id = "personas_filter_chips",
            nameRu = "Фильтры и инфо персон",
            screenId = "personas",
            categoryRu = "Фильтры",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "personas_card_item",
            nameRu = "Карточка персоны пользователя",
            screenId = "personas",
            categoryRu = "Список персон",
            anchor = LayoutAnchor.TOP_CENTER
        ),

        // Screen: Settings (Настройки)
        ElementLayoutConfig(
            id = "settings_top_bar",
            nameRu = "Шапка настроек",
            screenId = "settings",
            categoryRu = "Верхняя панель",
            isDeletable = false,
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "settings_search_bar",
            nameRu = "Поиск по опциям",
            screenId = "settings",
            categoryRu = "Поиск",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "settings_studio_banner",
            nameRu = "Карточка «Режим Студии»",
            screenId = "settings",
            categoryRu = "Секции настроек",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "settings_category_nav",
            nameRu = "Панель навигации по разделам",
            screenId = "settings",
            categoryRu = "Навигация",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "settings_theme_card",
            nameRu = "Секция тем и фонов",
            screenId = "settings",
            categoryRu = "Секции настроек",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "settings_accent_card",
            nameRu = "Секция акцентных цветов",
            screenId = "settings",
            categoryRu = "Секции настроек",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "settings_chat_view_card",
            nameRu = "Секция стилей чата (Novel / Bubbles)",
            screenId = "settings",
            categoryRu = "Секции настроек",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "settings_tts_card",
            nameRu = "Секция голоса и озвучки",
            screenId = "settings",
            categoryRu = "Секции настроек",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "settings_generation_card",
            nameRu = "Секция параметров генерации AI",
            screenId = "settings",
            categoryRu = "Секции настроек",
            anchor = LayoutAnchor.TOP_CENTER
        ),
        ElementLayoutConfig(
            id = "settings_backup_card",
            nameRu = "Секция бэкапов и экспорта",
            screenId = "settings",
            categoryRu = "Секции настроек",
            anchor = LayoutAnchor.TOP_CENTER
        ),

        // Global: Hotbar
        ElementLayoutConfig(
            id = "global_hotbar",
            nameRu = "Нижняя панель (Hotbar)",
            screenId = "common",
            categoryRu = "Глобальная навигация",
            isDeletable = false,
            anchor = LayoutAnchor.BOTTOM_CENTER
        )
    )

    private val defaultMap: Map<String, ElementLayoutConfig> by lazy {
        defaultElements.associateBy { it.id }
    }

    // Built-in Presets
    val presetClassic = LayoutPreset(
        id = "classic",
        nameRu = "Классический (Стандарт)",
        descriptionRu = "Оригинальное эргономичное расположение элементов приложения",
        isBuiltIn = true,
        elements = defaultMap
    )

    private var sharedPrefs: SharedPreferences? = null

    private val _presets = MutableStateFlow<List<LayoutPreset>>(
        listOf(presetClassic)
    )
    val presets: StateFlow<List<LayoutPreset>> = _presets.asStateFlow()

    private val _activePreset = MutableStateFlow<LayoutPreset>(presetClassic)
    val activePreset: StateFlow<LayoutPreset> = _activePreset.asStateFlow()

    private val _activeElements = MutableStateFlow<Map<String, ElementLayoutConfig>>(defaultMap)
    val activeElements: StateFlow<Map<String, ElementLayoutConfig>> = _activeElements.asStateFlow()

    fun init(context: Context) {
        if (sharedPrefs != null) return
        sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val prefs = sharedPrefs ?: return
        val customJson = prefs.getString(KEY_CUSTOM_PRESETS_JSON, "[]") ?: "[]"
        val customList = mutableListOf<LayoutPreset>()

        try {
            val arr = JSONArray(customJson)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                if (obj != null) {
                    customList.add(LayoutPreset.fromJson(obj))
                }
            }
        } catch (_: Exception) {}

        val allList = listOf(presetClassic) + customList
        _presets.value = allList

        val activeId = prefs.getString(KEY_ACTIVE_PRESET_ID, presetClassic.id) ?: presetClassic.id
        val found = allList.find { it.id == activeId } ?: presetClassic
        _activePreset.value = found
        _activeElements.value = found.elements.ifEmpty { defaultMap }
    }

    private fun saveToPrefs() {
        val prefs = sharedPrefs ?: return
        val customPresets = _presets.value.filter { !it.isBuiltIn }
        val arr = JSONArray()
        customPresets.forEach { arr.put(it.toJson()) }

        prefs.edit()
            .putString(KEY_ACTIVE_PRESET_ID, _activePreset.value.id)
            .putString(KEY_CUSTOM_PRESETS_JSON, arr.toString())
            .apply()
    }

    fun selectPreset(presetId: String) {
        val preset = _presets.value.find { it.id == presetId } ?: presetClassic
        _activePreset.value = preset
        _activeElements.value = preset.elements.ifEmpty { defaultMap }
        saveToPrefs()
    }

    fun createCustomPreset(nameRu: String, descriptionRu: String = "Пользовательский макет Студии"): LayoutPreset {
        val newId = "custom_${System.currentTimeMillis()}"
        val newPreset = LayoutPreset(
            id = newId,
            nameRu = nameRu.ifBlank { "Мой Лайаут" },
            descriptionRu = descriptionRu,
            isBuiltIn = false,
            elements = _activeElements.value.toMap()
        )
        val updated = _presets.value + newPreset
        _presets.value = updated
        _activePreset.value = newPreset
        saveToPrefs()
        return newPreset
    }

    fun deletePreset(presetId: String) {
        val target = _presets.value.find { it.id == presetId }
        if (target == null || target.isBuiltIn) return

        val updated = _presets.value.filter { it.id != presetId }
        _presets.value = updated

        if (_activePreset.value.id == presetId) {
            selectPreset(presetClassic.id)
        } else {
            saveToPrefs()
        }
    }

    fun updateElement(element: ElementLayoutConfig) {
        val currentMap = _activeElements.value.toMutableMap()
        currentMap[element.id] = element
        _activeElements.value = currentMap

        val currentPreset = _activePreset.value
        val updatedPreset = if (currentPreset.isBuiltIn) {
            // Auto-clone built-in preset to custom if modified
            val newCustom = LayoutPreset(
                id = "custom_${System.currentTimeMillis()}",
                nameRu = "Кастом (${currentPreset.nameRu})",
                descriptionRu = "Пользовательские изменения в Студии",
                isBuiltIn = false,
                elements = currentMap
            )
            _presets.value = _presets.value + newCustom
            newCustom
        } else {
            currentPreset.copy(elements = currentMap)
        }

        _activePreset.value = updatedPreset
        _presets.value = _presets.value.map { if (it.id == updatedPreset.id) updatedPreset else it }
        saveToPrefs()
    }

    fun resetScreenElements(screenId: String) {
        val currentMap = _activeElements.value.toMutableMap()
        defaultElements.filter { it.screenId == screenId }.forEach { def ->
            currentMap[def.id] = def
        }
        _activeElements.value = currentMap

        val currentPreset = _activePreset.value
        if (!currentPreset.isBuiltIn) {
            val updated = currentPreset.copy(elements = currentMap)
            _activePreset.value = updated
            _presets.value = _presets.value.map { if (it.id == updated.id) updated else it }
            saveToPrefs()
        }
    }

    fun resetActiveLayout() {
        val currentPreset = _activePreset.value
        if (currentPreset.isBuiltIn) {
            _activeElements.value = currentPreset.elements.ifEmpty { defaultMap }
        } else {
            _activeElements.value = defaultMap
            val updated = currentPreset.copy(elements = defaultMap)
            _activePreset.value = updated
            _presets.value = _presets.value.map { if (it.id == updated.id) updated else it }
            saveToPrefs()
        }
    }

    fun resetElement(elementId: String) {
        val defaultConfig = defaultMap[elementId] ?: defaultElements.find { it.id == elementId }
        if (defaultConfig != null) {
            updateElement(defaultConfig)
        }
    }

    fun nudgeElement(elementId: String, deltaX: Float, deltaY: Float) {
        val current = getElementConfig(elementId)
        val updated = current.copy(
            offsetX = (current.offsetX + deltaX).coerceIn(-500f, 500f),
            offsetY = (current.offsetY + deltaY).coerceIn(-800f, 800f)
        )
        updateElement(updated)
    }

    fun scaleElement(elementId: String, deltaScale: Float) {
        val current = getElementConfig(elementId)
        val newScale = (current.scaleX + deltaScale).coerceIn(0.5f, 2.0f)
        val updated = current.copy(
            scaleX = newScale,
            scaleY = newScale
        )
        updateElement(updated)
    }

    fun setElementAnchor(elementId: String, anchor: LayoutAnchor) {
        val current = getElementConfig(elementId)
        val updated = current.copy(anchor = anchor)
        updateElement(updated)
    }

    fun getElementConfig(elementId: String): ElementLayoutConfig {
        return _activeElements.value[elementId] ?: defaultMap[elementId] ?: ElementLayoutConfig(
            id = elementId,
            nameRu = elementId,
            screenId = "common"
        )
    }
}

@Composable
fun Modifier.studioElement(elementId: String): Modifier {
    val activeElements by StudioLayoutManager.activeElements.collectAsState()
    val config = activeElements[elementId] ?: StudioLayoutManager.getElementConfig(elementId)

    if (!config.isVisible) {
        return this.then(
            Modifier
                .size(0.dp)
                .alpha(0f)
                .graphicsLayer {
                    alpha = 0f
                    scaleX = 0f
                    scaleY = 0f
                    translationX = -99999f
                }
        )
    }

    return this.then(
        Modifier.graphicsLayer {
            this.translationX = config.offsetX
            this.translationY = config.offsetY
            this.scaleX = config.scaleX
            this.scaleY = config.scaleY
            this.alpha = config.alpha
        }
    )
}
