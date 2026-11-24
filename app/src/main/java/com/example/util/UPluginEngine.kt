package com.example.util

import com.example.data.datastore.UserSettings

data class UPluginItem(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val rawScript: String,
    val isEnabled: Boolean = true
)

object UPluginEngine {

    val BUILTIN_PLUGINS = listOf(
        UPluginItem(
            id = "motion_blur",
            name = "Motion Blur",
            version = "1.0.0",
            description = "Динамическое аппаратное размытие элементов интерфейса при скроллинге любого экрана.",
            author = "Untitled Engine",
            rawScript = """
                // Motion Blur Engine Plugin
                app.enableMotionBlur(12.0);
            """.trimIndent(),
            isEnabled = false
        )
    )

    /**
     * Compiles and executes pseudo-code script statements directly on UserSettings.
     */
    fun compileAndExecute(scriptText: String, currentSettings: UserSettings): UserSettings {
        if (scriptText.isBlank()) return currentSettings

        var newSettings = currentSettings
        val lines = scriptText.lines()

        for (line in lines) {
            val clean = line.trim()
            if (clean.isBlank() || clean.startsWith("//") || clean.startsWith("#")) continue

            try {
                when {
                    clean.contains("app.enableMotionBlur") -> {
                        val num = extractNumber(clean, 12f)
                        newSettings = newSettings.copy(pluginMotionBlurEnabled = true, pluginMotionBlurIntensity = num)
                    }
                    clean.contains("app.disableMotionBlur") -> {
                        newSettings = newSettings.copy(pluginMotionBlurEnabled = false)
                    }
                    clean.contains("app.setButtonShape") -> {
                        val shape = extractStringArg(clean, "cookie")
                        val waves = extractNumber(clean, 12f).toInt()
                        newSettings = newSettings.copy(buttonShapePreset = shape, buttonShapeWaves = waves)
                    }
                    clean.contains("app.setTheme") -> {
                        val theme = extractStringArg(clean, "dark")
                        newSettings = newSettings.copy(themeMode = theme)
                    }
                    clean.contains("app.setLanguage") -> {
                        val lang = extractStringArg(clean, "ru")
                        newSettings = newSettings.copy(appLanguage = lang)
                    }
                    clean.contains("app.setTabAnimation") -> {
                        val anim = extractStringArg(clean, "smooth_slide")
                        newSettings = newSettings.copy(tabSwitchAnimation = anim)
                    }
                    clean.contains("app.setBotTypingSpeed") -> {
                        val ms = extractNumber(clean, 15f).toInt()
                        newSettings = newSettings.copy(botTypingSpeedMs = ms)
                    }
                }
            } catch (_: Exception) {}
        }

        return newSettings
    }

    private fun extractNumber(line: String, defaultVal: Float): Float {
        val match = Regex("(?::|\\(|=)\\s*([0-9.]+)\\s*(?:\\)|;|$)").find(line)
        return match?.groupValues?.get(1)?.toFloatOrNull() ?: defaultVal
    }

    private fun extractStringArg(line: String, defaultVal: String): String {
        val match = Regex("[\"']([^\"']+)[\"']").find(line)
        return match?.groupValues?.get(1) ?: defaultVal
    }
}
