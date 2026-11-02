package com.example.ui.studio

import org.json.JSONArray
import org.json.JSONObject

enum class LayoutAnchor(val titleRu: String, val titleEn: String, val icon: String) {
    TOP_START("Сверху-Слева", "Top-Left", "↖️"),
    TOP_CENTER("Сверху-Центр", "Top-Center", "⬆️"),
    TOP_END("Сверху-Справа", "Top-Right", "↗️"),
    CENTER_START("По центру слева", "Center-Left", "⬅️"),
    CENTER("По центру", "Center", "🎯"),
    CENTER_END("По центру справа", "Center-Right", "➡️"),
    BOTTOM_START("Снизу-Слева", "Bottom-Left", "↙️"),
    BOTTOM_CENTER("Снизу-Центр", "Bottom-Center", "⬇️"),
    BOTTOM_END("Снизу-Справа", "Bottom-Right", "↘️");

    companion object {
        fun fromNormalized(normX: Float, normY: Float): LayoutAnchor {
            val clampedX = normX.coerceIn(0f, 1f)
            val clampedY = normY.coerceIn(0f, 1f)

            return when {
                clampedY < 0.33f -> when {
                    clampedX < 0.33f -> TOP_START
                    clampedX > 0.66f -> TOP_END
                    else -> TOP_CENTER
                }
                clampedY > 0.66f -> when {
                    clampedX < 0.33f -> BOTTOM_START
                    clampedX > 0.66f -> BOTTOM_END
                    else -> BOTTOM_CENTER
                }
                else -> when {
                    clampedX < 0.33f -> CENTER_START
                    clampedX > 0.66f -> CENTER_END
                    else -> CENTER
                }
            }
        }
    }
}

data class ElementLayoutConfig(
    val id: String,
    val nameRu: String,
    val screenId: String,
    val categoryRu: String = "Элемент",
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val alpha: Float = 1.0f,
    val isVisible: Boolean = true,
    val isDeletable: Boolean = true,
    val anchor: LayoutAnchor = LayoutAnchor.CENTER
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("nameRu", nameRu)
            put("screenId", screenId)
            put("categoryRu", categoryRu)
            put("offsetX", offsetX.toDouble())
            put("offsetY", offsetY.toDouble())
            put("scaleX", scaleX.toDouble())
            put("scaleY", scaleY.toDouble())
            put("alpha", alpha.toDouble())
            put("isVisible", isVisible)
            put("isDeletable", isDeletable)
            put("anchor", anchor.name)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ElementLayoutConfig {
            val id = json.optString("id", "")
            val nameRu = json.optString("nameRu", id)
            val screenId = json.optString("screenId", "common")
            val categoryRu = json.optString("categoryRu", "Элемент")
            val offsetX = json.optDouble("offsetX", 0.0).toFloat()
            val offsetY = json.optDouble("offsetY", 0.0).toFloat()
            val scaleX = json.optDouble("scaleX", 1.0).toFloat()
            val scaleY = json.optDouble("scaleY", 1.0).toFloat()
            val alpha = json.optDouble("alpha", 1.0).toFloat()
            val isVisible = json.optBoolean("isVisible", true)
            val isDeletable = json.optBoolean("isDeletable", true)
            val anchorName = json.optString("anchor", LayoutAnchor.CENTER.name)
            val anchor = try {
                LayoutAnchor.valueOf(anchorName)
            } catch (_: Exception) {
                LayoutAnchor.CENTER
            }

            return ElementLayoutConfig(
                id = id,
                nameRu = nameRu,
                screenId = screenId,
                categoryRu = categoryRu,
                offsetX = offsetX,
                offsetY = offsetY,
                scaleX = scaleX,
                scaleY = scaleY,
                alpha = alpha,
                isVisible = isVisible,
                isDeletable = isDeletable,
                anchor = anchor
            )
        }
    }
}

data class LayoutPreset(
    val id: String,
    val nameRu: String,
    val descriptionRu: String,
    val isBuiltIn: Boolean = false,
    val elements: Map<String, ElementLayoutConfig> = emptyMap()
) {
    fun toJson(): JSONObject {
        val root = JSONObject()
        root.put("id", id)
        root.put("nameRu", nameRu)
        root.put("descriptionRu", descriptionRu)
        root.put("isBuiltIn", isBuiltIn)
        val elemsArray = JSONArray()
        elements.values.forEach { elemsArray.put(it.toJson()) }
        root.put("elements", elemsArray)
        return root
    }

    companion object {
        fun fromJson(json: JSONObject): LayoutPreset {
            val id = json.optString("id", "custom")
            val nameRu = json.optString("nameRu", "Пользовательский")
            val descRu = json.optString("descriptionRu", "Настроен в Студии")
            val isBuiltIn = json.optBoolean("isBuiltIn", false)
            val elemsMap = mutableMapOf<String, ElementLayoutConfig>()

            val elemsArray = json.optJSONArray("elements")
            if (elemsArray != null) {
                for (i in 0 until elemsArray.length()) {
                    val obj = elemsArray.optJSONObject(i)
                    if (obj != null) {
                        val elem = ElementLayoutConfig.fromJson(obj)
                        elemsMap[elem.id] = elem
                    }
                }
            }

            return LayoutPreset(
                id = id,
                nameRu = nameRu,
                descriptionRu = descRu,
                isBuiltIn = isBuiltIn,
                elements = elemsMap
            )
        }
    }
}

enum class StudioScreenType(val id: String, val titleRu: String, val icon: String) {
    CHATS("chats", "Чаты", "💬"),
    CHAT_ROOM("chat", "Диалог", "💭"),
    PROVIDERS("providers", "Провайдеры", "⚡"),
    PERSONAS("personas", "Персоны", "🎭"),
    SETTINGS("settings", "Настройки", "⚙️")
}
