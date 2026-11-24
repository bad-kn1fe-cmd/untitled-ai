package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.data.datastore.UserSettings
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.JsePlatform
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class LuaPluginMetadata(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val author: String = "Unknown",
    val description: String = "",
    val permissions: Set<String> = emptySet()
)

data class LuaExecutionResult(
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val metadata: LuaPluginMetadata? = null
)

object ULuaEngine {
    private const val TAG = "ULuaEngine"

    private val _consoleLogs = MutableStateFlow<List<String>>(emptyList())
    val consoleLogs: StateFlow<List<String>> = _consoleLogs.asStateFlow()

    // Registered hooks per plugin: pluginId -> (eventName -> LuaFunction)
    private val pluginHooks = ConcurrentHashMap<String, ConcurrentHashMap<String, LuaFunction>>()

    // Active timers per plugin: pluginId -> (timerId -> Job)
    private val pluginTimers = ConcurrentHashMap<String, ConcurrentHashMap<String, Job>>()

    // Metadata extracted from registered plugins
    private val registeredMetadata = ConcurrentHashMap<String, LuaPluginMetadata>()

    // Tracks original setting values before they were altered by a plugin: pluginId -> (SettingKey -> OriginalValue)
    private val pluginOriginalSettings = ConcurrentHashMap<String, ConcurrentHashMap<String, Any?>>()
    private var lastGetSettings: (() -> UserSettings)? = null
    private var lastUpdateSettings: ((UserSettings) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun appendLog(msg: String) {
        Log.d(TAG, msg)
        val current = _consoleLogs.value
        val updated = (current + msg).takeLast(250)
        _consoleLogs.value = updated
    }

    fun clearLogs() {
        _consoleLogs.value = emptyList()
    }

    fun clearPluginHooks(pluginId: String) {
        // 1. Invoke onDisable hook if registered by the plugin
        val disableHook = pluginHooks[pluginId]?.get("onDisable")
        if (disableHook != null && disableHook.isfunction()) {
            try {
                disableHook.call()
                appendLog("🧹 Invoked onDisable hook for [$pluginId]")
            } catch (e: Exception) {
                appendLog("⚠️ Error in onDisable hook for [$pluginId]: ${e.message}")
            }
        }

        // 2. Remove hooks and metadata
        pluginHooks.remove(pluginId)
        registeredMetadata.remove(pluginId)

        // 3. Cancel all running timers for this plugin
        pluginTimers.remove(pluginId)?.values?.forEach { job ->
            try { job.cancel() } catch (_: Exception) {}
        }

        // 4. Remove all interactive widgets and dialogs created by this plugin
        mainHandler.post {
            PluginUiManager.clearForPlugin(pluginId)
            PluginOverlayManager.clearForPlugin(pluginId)
        }

        // 5. Stop all audio and speech started by this plugin
        PluginAudioManager.stopForPlugin(pluginId)

        // 6. Revert any UserSettings altered by this plugin back to their original values
        val originals = pluginOriginalSettings.remove(pluginId)
        val updater = lastUpdateSettings
        val getter = lastGetSettings
        if (originals != null && updater != null && getter != null) {
            try {
                var restored = getter()
                if (originals.containsKey("pluginMotionBlurEnabled")) {
                    restored = restored.copy(
                        pluginMotionBlurEnabled = (originals["pluginMotionBlurEnabled"] as? Boolean) ?: false,
                        pluginMotionBlurIntensity = (originals["pluginMotionBlurIntensity"] as? Float) ?: restored.pluginMotionBlurIntensity
                    )
                }
                if (originals.containsKey("buttonShapePreset")) {
                    restored = restored.copy(
                        buttonShapePreset = (originals["buttonShapePreset"] as? String) ?: "default",
                        buttonShapeWaves = (originals["buttonShapeWaves"] as? Int) ?: restored.buttonShapeWaves,
                        buttonShapeDepthPercent = (originals["buttonShapeDepthPercent"] as? Float) ?: restored.buttonShapeDepthPercent
                    )
                }
                if (originals.containsKey("tabSwitchAnimation")) {
                    restored = restored.copy(
                        tabSwitchAnimation = (originals["tabSwitchAnimation"] as? String) ?: "default",
                        tabSwitchDurationMs = (originals["tabSwitchDurationMs"] as? Int) ?: restored.tabSwitchDurationMs
                    )
                }
                if (originals.containsKey("boykisserPetEnabled")) {
                    restored = restored.copy(
                        boykisserPetEnabled = (originals["boykisserPetEnabled"] as? Boolean) ?: false,
                        boykisserPetSpeed = (originals["boykisserPetSpeed"] as? Float) ?: restored.boykisserPetSpeed,
                        boykisserPetVariant = (originals["boykisserPetVariant"] as? String) ?: restored.boykisserPetVariant
                    )
                }
                if (originals.containsKey("themeMode")) {
                    restored = restored.copy(themeMode = (originals["themeMode"] as? String) ?: restored.themeMode)
                }
                if (originals.containsKey("accentColorLong")) {
                    restored = restored.copy(accentColorLong = (originals["accentColorLong"] as? Long) ?: restored.accentColorLong)
                }
                if (originals.containsKey("appLanguage")) {
                    restored = restored.copy(appLanguage = (originals["appLanguage"] as? String) ?: restored.appLanguage)
                }
                updater(restored)
                appendLog("🔄 Reverted settings modified by [$pluginId]")
            } catch (e: Exception) {
                appendLog("⚠️ Error reverting settings for [$pluginId]: ${e.message}")
            }
        }

        appendLog("🔌 Plugin [$pluginId] fully cleaned up and deactivated.")
    }

    fun clearAllHooks() {
        pluginHooks.clear()
        registeredMetadata.clear()
        pluginTimers.values.forEach { map ->
            map.values.forEach { job ->
                try { job.cancel() } catch (_: Exception) {}
            }
        }
        pluginTimers.clear()
        pluginOriginalSettings.clear()
        mainHandler.post {
            PluginUiManager.clearAll()
            PluginOverlayManager.clear()
        }
        PluginAudioManager.stopAll()
    }

    /**
     * Creates a Globals instance equipped with full native capabilities:
     * app, overlay, hooks, chat, ui, http, db, timer, audio, plugin.
     */
    fun createGlobals(
        context: Context,
        pluginId: String,
        getSettings: () -> UserSettings,
        updateSettings: (UserSettings) -> Unit,
        insertMessageCallback: ((text: String, isUser: Boolean) -> Unit)? = null,
        getActiveBotNameCallback: (() -> String)? = null,
        getCharactersCallback: (() -> List<CharacterEntity>)? = null,
        getActiveCharacterCallback: (() -> CharacterEntity?)? = null,
        updateCharacterCallback: ((Long, Map<String, Any?>) -> Unit)? = null,
        getRecentMessagesCallback: ((limit: Int) -> List<MessageEntity>)? = null
    ): Globals {
        lastGetSettings = getSettings
        lastUpdateSettings = updateSettings

        val recordSettingOriginal: (String, Any?) -> Unit = { key, value ->
            pluginOriginalSettings.getOrPut(pluginId) { ConcurrentHashMap() }.putIfAbsent(key, value)
        }

        val globals = JsePlatform.standardGlobals()

        // 1. Sandbox restrictions: disable raw unsafe JVM reflections and direct os commands
        globals.set("luajava", LuaValue.NIL)
        try {
            val os = globals.get("os")
            if (os.istable()) {
                os.set("exit", LuaValue.NIL)
                os.set("execute", LuaValue.NIL)
                os.set("remove", LuaValue.NIL)
                os.set("rename", LuaValue.NIL)
            }
            val io = globals.get("io")
            if (io.istable()) {
                io.set("open", LuaValue.NIL)
                io.set("popen", LuaValue.NIL)
            }
        } catch (_: Exception) {}

        // 2. Custom print(...) function that directs output to the in-app console
        globals.set("print", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                val n = args.narg()
                for (i in 1..n) {
                    if (i > 1) sb.append("  ")
                    sb.append(args.arg(i).tojstring())
                }
                val line = "[${pluginId.take(14)}] ${sb}"
                appendLog(line)
                return LuaValue.NONE
            }
        })

        // 3. Register `plugin` table
        val pluginTable = LuaTable()
        pluginTable.set("register", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                if (arg.istable()) {
                    val id = arg.get("id").optjstring(pluginId)
                    val name = arg.get("name").optjstring("Lua Plugin")
                    val version = arg.get("version").optjstring("1.0.0")
                    val author = arg.get("author").optjstring("Developer")
                    val desc = arg.get("description").optjstring("")
                    val perms = mutableSetOf<String>()
                    val permTable = arg.get("permissions")
                    if (permTable.istable()) {
                        val len = permTable.length()
                        for (i in 1..len) {
                            perms.add(permTable.get(i).tojstring().uppercase())
                        }
                    }
                    val meta = LuaPluginMetadata(id, name, version, author, desc, perms)
                    registeredMetadata[pluginId] = meta
                    appendLog("ℹ️ Plugin registered: $name ($id) v$version by $author")
                }
                return LuaValue.NIL
            }
        })
        globals.set("plugin", pluginTable)

        // 4. Register `app` table
        val appTable = LuaTable()

        appTable.set("toast", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val text = arg.tojstring()
                mainHandler.post {
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
                return LuaValue.NIL
            }
        })
        appTable.set("showToast", appTable.get("toast"))

        appTable.set("setTheme", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val mode = arg.tojstring().lowercase()
                val targetMode = when (mode) {
                    "dark", "light" -> mode
                    else -> "system"
                }
                val current = getSettings()
                recordSettingOriginal("themeMode", current.themeMode)
                updateSettings(current.copy(themeMode = targetMode))
                appendLog("🎨 Theme changed to: $targetMode")
                return LuaValue.NIL
            }
        })

        appTable.set("setLanguage", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val lang = arg.tojstring().lowercase()
                val targetLang = when (lang) {
                    "ru", "en", "femboy" -> lang
                    else -> "ru"
                }
                val current = getSettings()
                recordSettingOriginal("appLanguage", current.appLanguage)
                updateSettings(current.copy(appLanguage = targetLang))
                appendLog("🌐 Language set to: $targetLang")
                return LuaValue.NIL
            }
        })

        appTable.set("enableMotionBlur", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val intensity = if (args.narg() >= 1) args.arg(1).tofloat() else 14.0f
                val current = getSettings()
                recordSettingOriginal("pluginMotionBlurEnabled", current.pluginMotionBlurEnabled)
                recordSettingOriginal("pluginMotionBlurIntensity", current.pluginMotionBlurIntensity)
                updateSettings(current.copy(pluginMotionBlurEnabled = true, pluginMotionBlurIntensity = intensity.coerceIn(2f, 30f)))
                appendLog("⚡ Motion Blur enabled with intensity: $intensity")
                return LuaValue.NONE
            }
        })

        appTable.set("disableMotionBlur", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val current = getSettings()
                recordSettingOriginal("pluginMotionBlurEnabled", current.pluginMotionBlurEnabled)
                updateSettings(current.copy(pluginMotionBlurEnabled = false))
                appendLog("⚡ Motion Blur disabled")
                return LuaValue.NIL
            }
        })

        appTable.set("setButtonShape", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val preset = args.arg(1).optjstring("cookie")
                val waves = if (args.narg() >= 2) args.arg(2).toint() else 12
                val depth = if (args.narg() >= 3) args.arg(3).tofloat() else 0.06f
                val current = getSettings()
                recordSettingOriginal("buttonShapePreset", current.buttonShapePreset)
                recordSettingOriginal("buttonShapeWaves", current.buttonShapeWaves)
                recordSettingOriginal("buttonShapeDepthPercent", current.buttonShapeDepthPercent)
                updateSettings(current.copy(
                    buttonShapePreset = preset,
                    buttonShapeWaves = waves.coerceIn(4, 24),
                    buttonShapeDepthPercent = depth.coerceIn(0.01f, 0.20f)
                ))
                appendLog("🔘 Button shape set to: $preset (waves=$waves, depth=$depth)")
                return LuaValue.NONE
            }
        })

        appTable.set("setTabAnimation", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val anim = args.arg(1).optjstring("spring_slide")
                val duration = if (args.narg() >= 2) args.arg(2).toint() else 260
                val current = getSettings()
                recordSettingOriginal("tabSwitchAnimation", current.tabSwitchAnimation)
                recordSettingOriginal("tabSwitchDurationMs", current.tabSwitchDurationMs)
                updateSettings(current.copy(
                    tabSwitchAnimation = anim,
                    tabSwitchDurationMs = duration.coerceIn(100, 1000)
                ))
                appendLog("✨ Tab animation set to: $anim ($duration ms)")
                return LuaValue.NONE
            }
        })

        appTable.set("setPet", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val enabled = if (args.narg() >= 1) args.arg(1).toboolean() else true
                val speed = if (args.narg() >= 2) args.arg(2).tofloat() else 1.0f
                val variant = if (args.narg() >= 3) args.arg(3).tojstring() else "boykisser"
                val current = getSettings()
                recordSettingOriginal("boykisserPetEnabled", current.boykisserPetEnabled)
                recordSettingOriginal("boykisserPetSpeed", current.boykisserPetSpeed)
                recordSettingOriginal("boykisserPetVariant", current.boykisserPetVariant)
                updateSettings(current.copy(
                    boykisserPetEnabled = enabled,
                    boykisserPetSpeed = speed.coerceIn(0.2f, 3.0f),
                    boykisserPetVariant = variant
                ))
                appendLog("🐾 Pet set: enabled=$enabled, speed=$speed, variant=$variant")
                return LuaValue.NONE
            }
        })

        appTable.set("setPetBehavior", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val target = arg1.optjstring("main").lowercase()
                val behavior = arg2.optjstring("walk").lowercase()
                val current = getSettings()
                if (target.contains("chat")) {
                    recordSettingOriginal("boykisserChatBehavior", current.boykisserChatBehavior)
                    updateSettings(current.copy(boykisserChatBehavior = behavior))
                } else {
                    recordSettingOriginal("boykisserMainBehavior", current.boykisserMainBehavior)
                    updateSettings(current.copy(boykisserMainBehavior = behavior))
                }
                appendLog("🐾 Pet behavior ($target) set to: $behavior")
                return LuaValue.NIL
            }
        })

        appTable.set("setBotTypingSpeed", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val speed = arg.toint().coerceIn(5, 100)
                val current = getSettings()
                recordSettingOriginal("botTypingSpeedMs", current.botTypingSpeedMs)
                updateSettings(current.copy(botTypingSpeedMs = speed))
                appendLog("⌨️ Typing speed set to: $speed ms")
                return LuaValue.NIL
            }
        })

        appTable.set("copy", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val text = arg.tojstring()
                mainHandler.post {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("Lua Copied", text)
                    clipboard?.setPrimaryClip(clip)
                }
                appendLog("📋 Text copied to clipboard: ${text.take(30)}...")
                return LuaValue.NIL
            }
        })

        appTable.set("haptic", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val type = arg.optjstring("light").lowercase()
                mainHandler.post {
                    when (type) {
                        "success" -> com.example.ui.components.HapticHelper.performSuccessHaptic(context, true)
                        else -> com.example.ui.components.HapticHelper.performLightHaptic(context, true)
                    }
                }
                return LuaValue.NIL
            }
        })

        appTable.set("getSettings", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val s = getSettings()
                val t = LuaTable()
                t.set("theme", LuaValue.valueOf(s.themeMode))
                t.set("language", LuaValue.valueOf(s.appLanguage))
                t.set("motionBlurEnabled", LuaValue.valueOf(s.pluginMotionBlurEnabled))
                t.set("motionBlurIntensity", LuaValue.valueOf(s.pluginMotionBlurIntensity.toDouble()))
                t.set("buttonShape", LuaValue.valueOf(s.buttonShapePreset))
                t.set("petEnabled", LuaValue.valueOf(s.boykisserPetEnabled))
                t.set("petSpeed", LuaValue.valueOf(s.boykisserPetSpeed.toDouble()))
                return t
            }
        })

        globals.set("app", appTable)

        // 5. Register `overlay` table
        val overlayTable = LuaTable()

        overlayTable.set("showImage", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val id = args.arg(1).optjstring("img_${System.currentTimeMillis()}")
                val url = args.arg(2).optjstring("sparkle")
                val x = if (args.narg() >= 3) args.arg(3).tofloat() else 0.5f
                val y = if (args.narg() >= 4) args.arg(4).tofloat() else 0.5f
                val w = if (args.narg() >= 5) args.arg(5).tofloat() else 52f
                val h = if (args.narg() >= 6) args.arg(6).tofloat() else 52f
                val alpha = if (args.narg() >= 7) args.arg(7).tofloat() else 1.0f

                mainHandler.post {
                    PluginOverlayManager.showImage(id, url, x, y, w, h, alpha, pluginId = pluginId)
                }
                appendLog("🖼️ Overlay image shown: $id ($url) at ($x, $y)")
                return LuaValue.NONE
            }
        })

        overlayTable.set("animateImage", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val id = args.arg(1).optjstring("")
                val targetX = if (args.narg() >= 2) args.arg(2).tofloat() else 0.5f
                val targetY = if (args.narg() >= 3) args.arg(3).tofloat() else 0.5f
                val durationMs = if (args.narg() >= 4) args.arg(4).toint() else 400
                val rotation = if (args.narg() >= 5) args.arg(5).tofloat() else 0f
                val scale = if (args.narg() >= 6) args.arg(6).tofloat() else 1.0f

                mainHandler.post {
                    PluginOverlayManager.animateImage(id, targetX, targetY, durationMs, rotation, scale)
                }
                appendLog("🎬 Overlay animated: $id to ($targetX, $targetY) in ${durationMs}ms")
                return LuaValue.NONE
            }
        })

        overlayTable.set("hideImage", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val id = arg.tojstring()
                mainHandler.post {
                    PluginOverlayManager.hideImage(id)
                }
                appendLog("🗑️ Overlay hidden: $id")
                return LuaValue.NIL
            }
        })

        overlayTable.set("clear", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                mainHandler.post {
                    PluginOverlayManager.clear()
                }
                appendLog("🧹 All overlays cleared")
                return LuaValue.NIL
            }
        })

        globals.set("overlay", overlayTable)

        // 6. Register `hooks` table
        val hooksTable = LuaTable()

        hooksTable.set("register", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val eventName = arg1.tojstring()
                if (arg2.isfunction()) {
                    val map = pluginHooks.getOrPut(pluginId) { ConcurrentHashMap() }
                    map[eventName] = arg2.checkfunction()
                    appendLog("🔗 Hook registered: [$pluginId] -> '$eventName'")
                }
                return LuaValue.NIL
            }
        })

        hooksTable.set("unregister", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val eventName = arg.tojstring()
                pluginHooks[pluginId]?.remove(eventName)
                appendLog("🔓 Hook unregistered: [$pluginId] -> '$eventName'")
                return LuaValue.NIL
            }
        })

        globals.set("hooks", hooksTable)

        // 7. Register `chat` table
        val chatTable = LuaTable()

        chatTable.set("insertMessage", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val text = arg1.tojstring()
                val isUser = arg2.optboolean(false)
                mainHandler.post {
                    insertMessageCallback?.invoke(text, isUser)
                }
                appendLog("💬 Chat message inserted (isUser=$isUser): ${text.take(30)}...")
                return LuaValue.NIL
            }
        })

        chatTable.set("getBotName", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val name = getActiveBotNameCallback?.invoke() ?: "Bot"
                return LuaValue.valueOf(name)
            }
        })

        globals.set("chat", chatTable)

        // 8. Register `ui` module (Interactive Widgets and Dialogs)
        val uiTable = LuaTable()

        uiTable.set("showBanner", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val text = args.arg(1).optjstring("Plugin Notification")
                mainHandler.post {
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                }
                return LuaValue.NONE
            }
        })

        uiTable.set("showWidget", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                if (!arg.istable()) return LuaValue.NIL
                val id = arg.get("id").optjstring("widget_${System.currentTimeMillis()}")
                val title = arg.get("title").optjstring("Widget")
                val content = arg.get("content").optjstring("")
                val x = arg.get("x").optjstring("0.5").toFloatOrNull() ?: 0.5f
                val y = arg.get("y").optjstring("0.4").toFloatOrNull() ?: 0.4f
                val w = arg.get("width").optjstring("240").toFloatOrNull() ?: 240f
                val closeable = if (arg.get("closeable").isnil()) true else arg.get("closeable").toboolean()

                val buttonsList = mutableListOf<PluginWidgetButton>()
                val btnTable = arg.get("buttons")
                if (btnTable.istable()) {
                    val len = btnTable.length()
                    for (i in 1..len) {
                        val b = btnTable.get(i)
                        if (b.istable()) {
                            val btnText = b.get("text").optjstring("Button")
                            val btnFunc = b.get("onClick")
                            buttonsList.add(
                                PluginWidgetButton(
                                    id = "btn_$i",
                                    text = btnText,
                                    onClick = {
                                        if (btnFunc.isfunction()) {
                                            engineScope.launch {
                                                try { btnFunc.call() } catch (e: Exception) {
                                                    appendLog("⚠️ Error in widget button callback: ${e.message}")
                                                }
                                            }
                                        }
                                    }
                                )
                            )
                        }
                    }
                }

                mainHandler.post {
                    PluginUiManager.showWidget(
                        PluginWidget(
                            id = id,
                            pluginId = pluginId,
                            title = title,
                            content = content,
                            xFraction = x,
                            yFraction = y,
                            widthDp = w,
                            buttons = buttonsList,
                            closeable = closeable
                        )
                    )
                }
                appendLog("🪟 UI Widget shown: $id ('$title')")
                return LuaValue.valueOf(id)
            }
        })

        uiTable.set("hideWidget", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val id = arg.tojstring()
                mainHandler.post {
                    PluginUiManager.hideWidget(id)
                }
                appendLog("🪟 UI Widget hidden: $id")
                return LuaValue.NIL
            }
        })

        uiTable.set("clearWidgets", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                mainHandler.post {
                    PluginUiManager.clearWidgets()
                }
                return LuaValue.NIL
            }
        })

        uiTable.set("showDialog", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                if (!arg.istable()) return LuaValue.NIL
                val id = arg.get("id").optjstring("dialog_${System.currentTimeMillis()}")
                val title = arg.get("title").optjstring("Dialog")
                val message = arg.get("message").optjstring("")
                val confirmText = arg.get("confirmText").optjstring("OK")
                val cancelText = if (arg.get("cancelText").isnil()) null else arg.get("cancelText").tojstring()
                val onConfirmFunc = arg.get("onConfirm")
                val onCancelFunc = arg.get("onCancel")

                mainHandler.post {
                    PluginUiManager.showDialog(
                        PluginDialogData(
                            id = id,
                            pluginId = pluginId,
                            title = title,
                            message = message,
                            confirmText = confirmText,
                            cancelText = cancelText,
                            onConfirm = {
                                if (onConfirmFunc.isfunction()) {
                                    engineScope.launch {
                                        try { onConfirmFunc.call() } catch (e: Exception) {
                                            appendLog("⚠️ Dialog onConfirm error: ${e.message}")
                                        }
                                    }
                                }
                            },
                            onCancel = {
                                if (onCancelFunc.isfunction()) {
                                    engineScope.launch {
                                        try { onCancelFunc.call() } catch (e: Exception) {
                                            appendLog("⚠️ Dialog onCancel error: ${e.message}")
                                        }
                                    }
                                }
                            }
                        )
                    )
                }
                appendLog("💬 Modal Dialog displayed: '$title'")
                return LuaValue.NIL
            }
        })

        globals.set("ui", uiTable)

        // 9. Register `http` module (Network requests)
        val httpTable = LuaTable()

        httpTable.set("get", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val url = arg1.tojstring()
                appendLog("🌐 HTTP GET: $url")
                return try {
                    val reqBuilder = Request.Builder().url(url)
                    if (arg2.istable()) {
                        var k = LuaValue.NIL
                        while (true) {
                            val next = arg2.next(k)
                            k = next.arg1()
                            if (k.isnil()) break
                            val v = next.arg(2)
                            reqBuilder.addHeader(k.tojstring(), v.tojstring())
                        }
                    }
                    val response = httpClient.newCall(reqBuilder.build()).execute()
                    val responseBody = response.body?.string() ?: ""
                    val code = response.code

                    val resTable = LuaTable()
                    resTable.set("status", LuaValue.valueOf(code))
                    resTable.set("ok", LuaValue.valueOf(response.isSuccessful))
                    resTable.set("body", LuaValue.valueOf(responseBody))
                    appendLog("🌐 HTTP Response: $code (len=${responseBody.length})")
                    resTable
                } catch (e: Exception) {
                    appendLog("❌ HTTP GET Error ($url): ${e.message}")
                    val errTable = LuaTable()
                    errTable.set("status", LuaValue.valueOf(0))
                    errTable.set("ok", LuaValue.valueOf(false))
                    errTable.set("error", LuaValue.valueOf(e.message ?: "Network error"))
                    errTable.set("body", LuaValue.valueOf(""))
                    errTable
                }
            }
        })

        httpTable.set("post", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val url = args.arg(1).tojstring()
                val bodyText = args.arg(2).optjstring("")
                val contentType = args.arg(3).optjstring("application/json")
                val headerTable = args.arg(4)

                appendLog("🌐 HTTP POST: $url")
                return try {
                    val mediaType = contentType.toMediaTypeOrNull()
                    val reqBody = bodyText.toRequestBody(mediaType)
                    val reqBuilder = Request.Builder().url(url).post(reqBody)

                    if (headerTable.istable()) {
                        var k = LuaValue.NIL
                        while (true) {
                            val next = headerTable.next(k)
                            k = next.arg1()
                            if (k.isnil()) break
                            val v = next.arg(2)
                            reqBuilder.addHeader(k.tojstring(), v.tojstring())
                        }
                    }

                    val response = httpClient.newCall(reqBuilder.build()).execute()
                    val responseBody = response.body?.string() ?: ""
                    val code = response.code

                    val resTable = LuaTable()
                    resTable.set("status", LuaValue.valueOf(code))
                    resTable.set("ok", LuaValue.valueOf(response.isSuccessful))
                    resTable.set("body", LuaValue.valueOf(responseBody))
                    appendLog("🌐 HTTP POST Response: $code")
                    resTable
                } catch (e: Exception) {
                    appendLog("❌ HTTP POST Error ($url): ${e.message}")
                    val errTable = LuaTable()
                    errTable.set("status", LuaValue.valueOf(0))
                    errTable.set("ok", LuaValue.valueOf(false))
                    errTable.set("error", LuaValue.valueOf(e.message ?: "Network error"))
                    errTable.set("body", LuaValue.valueOf(""))
                    errTable
                }
            }
        })

        globals.set("http", httpTable)

        // 10. Register `db` module (Characters, Lore & Chat history)
        val dbTable = LuaTable()

        dbTable.set("getCharacters", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val chars = getCharactersCallback?.invoke() ?: emptyList()
                val listTable = LuaTable()
                chars.forEachIndexed { idx, c ->
                    val ct = LuaTable()
                    ct.set("id", LuaValue.valueOf(c.id.toDouble()))
                    ct.set("name", LuaValue.valueOf(c.name))
                    ct.set("description", LuaValue.valueOf(c.shortDescription))
                    ct.set("avatarUrl", LuaValue.valueOf(c.avatarUri))
                    ct.set("personality", LuaValue.valueOf(c.personality))
                    ct.set("greeting", LuaValue.valueOf(c.greetingMessage))
                    listTable.set(idx + 1, ct)
                }
                return listTable
            }
        })

        dbTable.set("getActiveCharacter", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val c = getActiveCharacterCallback?.invoke() ?: return LuaValue.NIL
                val ct = LuaTable()
                ct.set("id", LuaValue.valueOf(c.id.toDouble()))
                ct.set("name", LuaValue.valueOf(c.name))
                ct.set("description", LuaValue.valueOf(c.shortDescription))
                ct.set("avatarUrl", LuaValue.valueOf(c.avatarUri))
                ct.set("personality", LuaValue.valueOf(c.personality))
                ct.set("greeting", LuaValue.valueOf(c.greetingMessage))
                return ct
            }
        })

        dbTable.set("updateCharacter", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val charId = arg1.tolong()
                if (!arg2.istable()) return LuaValue.FALSE

                val updates = mutableMapOf<String, Any?>()
                if (!arg2.get("name").isnil()) updates["name"] = arg2.get("name").tojstring()
                if (!arg2.get("description").isnil()) updates["description"] = arg2.get("description").tojstring()
                if (!arg2.get("personality").isnil()) updates["personality"] = arg2.get("personality").tojstring()
                if (!arg2.get("greeting").isnil()) updates["greetingMessage"] = arg2.get("greeting").tojstring()
                if (!arg2.get("avatarUrl").isnil()) updates["avatarUrl"] = arg2.get("avatarUrl").tojstring()

                updateCharacterCallback?.invoke(charId, updates)
                appendLog("💾 Character #$charId updated via Lua db API")
                return LuaValue.TRUE
            }
        })

        dbTable.set("getRecentMessages", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val limit = if (arg.isnumber()) arg.toint().coerceIn(1, 100) else 20
                val msgs = getRecentMessagesCallback?.invoke(limit) ?: emptyList()
                val listTable = LuaTable()
                msgs.forEachIndexed { idx, m ->
                    val mt = LuaTable()
                    mt.set("id", LuaValue.valueOf(m.id.toDouble()))
                    mt.set("text", LuaValue.valueOf(m.content))
                    mt.set("isUser", LuaValue.valueOf(m.sender == "user"))
                    mt.set("timestamp", LuaValue.valueOf(m.timestamp.toDouble()))
                    listTable.set(idx + 1, mt)
                }
                return listTable
            }
        })

        globals.set("db", dbTable)

        // 11. Register `timer` module (Async intervals and delays)
        val timerTable = LuaTable()

        timerTable.set("after", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val delayMs = arg1.tolong().coerceAtLeast(10L)
                if (!arg2.isfunction()) return LuaValue.NIL
                val timerId = "timer_${UUID.randomUUID().toString().take(6)}"

                val job = engineScope.launch {
                    delay(delayMs)
                    try {
                        arg2.call()
                    } catch (e: Exception) {
                        appendLog("⚠️ Timer [$timerId] error: ${e.message}")
                    } finally {
                        pluginTimers[pluginId]?.remove(timerId)
                    }
                }
                pluginTimers.getOrPut(pluginId) { ConcurrentHashMap() }[timerId] = job
                return LuaValue.valueOf(timerId)
            }
        })

        timerTable.set("every", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val intervalMs = arg1.tolong().coerceAtLeast(50L)
                if (!arg2.isfunction()) return LuaValue.NIL
                val timerId = "rep_timer_${UUID.randomUUID().toString().take(6)}"

                val job = engineScope.launch {
                    while (isActive) {
                        delay(intervalMs)
                        try {
                            arg2.call()
                        } catch (e: Exception) {
                            appendLog("⚠️ Repeating timer [$timerId] error: ${e.message}")
                        }
                    }
                }
                pluginTimers.getOrPut(pluginId) { ConcurrentHashMap() }[timerId] = job
                return LuaValue.valueOf(timerId)
            }
        })

        timerTable.set("cancel", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val timerId = arg.tojstring()
                pluginTimers[pluginId]?.remove(timerId)?.cancel()
                appendLog("⏹️ Timer [$timerId] cancelled")
                return LuaValue.NIL
            }
        })

        timerTable.set("clearAll", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                pluginTimers.remove(pluginId)?.values?.forEach { it.cancel() }
                return LuaValue.NIL
            }
        })

        globals.set("timer", timerTable)

        // 12. Register `audio` module (SFX, Web Audio and TTS)
        val audioTable = LuaTable()

        audioTable.set("play", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sound = args.arg(1).optjstring("pop")
                val volume = if (args.narg() >= 2) args.arg(2).tofloat() else 1.0f
                val soundId = args.arg(3).optjstring("sfx_${System.currentTimeMillis()}")
                PluginAudioManager.playSound(sound, volume, soundId, pluginId = pluginId)
                appendLog("🔊 Audio playing: $sound (vol=$volume)")
                return LuaValue.valueOf(soundId)
            }
        })

        audioTable.set("stop", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                PluginAudioManager.stopSound(arg.tojstring())
                return LuaValue.NIL
            }
        })

        audioTable.set("stopAll", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                PluginAudioManager.stopAll()
                return LuaValue.NIL
            }
        })

        audioTable.set("tts", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val text = arg1.tojstring()
                val lang = arg2.optjstring("ru")
                PluginAudioManager.speakText(text, lang)
                appendLog("🗣️ TTS: \"${text.take(30)}...\" ($lang)")
                return LuaValue.NIL
            }
        })

        globals.set("audio", audioTable)

        return globals
    }

    /**
     * Executes a Lua script in a fully featured environment.
     */
    fun execute(
        script: String,
        pluginId: String,
        context: Context,
        getSettings: () -> UserSettings,
        updateSettings: (UserSettings) -> Unit,
        insertMessageCallback: ((String, Boolean) -> Unit)? = null,
        getActiveBotNameCallback: (() -> String)? = null,
        getCharactersCallback: (() -> List<CharacterEntity>)? = null,
        getActiveCharacterCallback: (() -> CharacterEntity?)? = null,
        updateCharacterCallback: ((Long, Map<String, Any?>) -> Unit)? = null,
        getRecentMessagesCallback: ((limit: Int) -> List<MessageEntity>)? = null
    ): LuaExecutionResult {
        if (script.isBlank()) {
            return LuaExecutionResult(true, "Script is empty")
        }

        clearPluginHooks(pluginId)

        val tempGlobals = createGlobals(
            context = context,
            pluginId = pluginId,
            getSettings = getSettings,
            updateSettings = updateSettings,
            insertMessageCallback = insertMessageCallback,
            getActiveBotNameCallback = getActiveBotNameCallback,
            getCharactersCallback = getCharactersCallback,
            getActiveCharacterCallback = getActiveCharacterCallback,
            updateCharacterCallback = updateCharacterCallback,
            getRecentMessagesCallback = getRecentMessagesCallback
        )

        return try {
            val chunk = tempGlobals.load(script, pluginId)
            val result = chunk.call()
            val meta = registeredMetadata[pluginId]
            val out = if (!result.isnil()) result.tojstring() else "Executed successfully."
            appendLog("✅ Plugin [$pluginId] loaded & executing.")
            LuaExecutionResult(success = true, output = out, metadata = meta)
        } catch (e: LuaError) {
            val errMsg = "Lua Error in [$pluginId]: ${e.message}"
            appendLog("❌ $errMsg")
            LuaExecutionResult(success = false, output = "", error = errMsg)
        } catch (e: Exception) {
            val errMsg = "Exception in [$pluginId]: ${e.message}"
            appendLog("❌ $errMsg")
            LuaExecutionResult(success = false, output = "", error = errMsg)
        }
    }

    /**
     * Dispatches `onBeforeSendPrompt` hook across all registered plugins.
     */
    fun dispatchBeforeSendPrompt(prompt: String): String {
        var currentPrompt = prompt
        pluginHooks.forEach { (pluginId, hooks) ->
            val hookFunc = hooks["onBeforeSendPrompt"] ?: return@forEach
            try {
                val ret = hookFunc.call(LuaValue.valueOf(currentPrompt))
                if (ret.isstring()) {
                    currentPrompt = ret.tojstring()
                }
            } catch (e: Exception) {
                appendLog("⚠️ Error in [$pluginId] onBeforeSendPrompt hook: ${e.message}")
            }
        }
        return currentPrompt
    }

    /**
     * Dispatches `onMessageReceived` hook across all registered plugins.
     */
    fun dispatchMessageReceived(botName: String, text: String): String {
        var currentText = text
        pluginHooks.forEach { (pluginId, hooks) ->
            val hookFunc = hooks["onMessageReceived"] ?: return@forEach
            try {
                val ret = hookFunc.call(LuaValue.valueOf(botName), LuaValue.valueOf(currentText))
                if (ret.isstring()) {
                    currentText = ret.tojstring()
                }
            } catch (e: Exception) {
                appendLog("⚠️ Error in [$pluginId] onMessageReceived hook: ${e.message}")
            }
        }
        return currentText
    }

    /**
     * Dispatches `onSlashCommand` hook across all registered plugins.
     */
    fun dispatchSlashCommand(rawCommand: String): Boolean {
        if (!rawCommand.startsWith("/")) return false
        val parts = rawCommand.trim().removePrefix("/").split("\\s+".toRegex(), limit = 2)
        val cmd = parts.getOrNull(0)?.lowercase() ?: return false
        val args = parts.getOrNull(1) ?: ""

        var handled = false
        pluginHooks.forEach { (pluginId, hooks) ->
            val hookFunc = hooks["onSlashCommand"] ?: return@forEach
            try {
                val ret = hookFunc.call(LuaValue.valueOf(cmd), LuaValue.valueOf(args))
                if (ret.isboolean() && ret.toboolean()) {
                    handled = true
                }
            } catch (e: Exception) {
                appendLog("⚠️ Error in [$pluginId] onSlashCommand hook: ${e.message}")
            }
        }
        return handled
    }

    /**
     * Dispatches `onChatOpened` hook across all registered plugins.
     */
    fun dispatchChatOpened(chatId: Long, botName: String) {
        pluginHooks.forEach { (pluginId, hooks) ->
            val hookFunc = hooks["onChatOpened"] ?: return@forEach
            try {
                hookFunc.call(LuaValue.valueOf(chatId.toDouble()), LuaValue.valueOf(botName))
            } catch (e: Exception) {
                appendLog("⚠️ Error in [$pluginId] onChatOpened hook: ${e.message}")
            }
        }
    }
}
