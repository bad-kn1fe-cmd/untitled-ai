package com.example.util

import android.content.Context
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.datastore.UserSettings
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.repository.RoleplayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class PluginItem(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val author: String = "Untitled",
    val description: String = "",
    val script: String = "",
    val isEnabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val permissions: Set<String> = emptySet()
)

object PluginManager {
    private const val TAG = "PluginManager"
    private const val MANIFEST_FILENAME = "plugins_manifest.json"

    private var appContext: Context? = null
    private var prefsRepository: UserPreferencesRepository? = null
    private var roleplayRepository: RoleplayRepository? = null
    private var appDatabase: AppDatabase? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _installedPlugins = MutableStateFlow<List<PluginItem>>(emptyList())
    val installedPlugins: StateFlow<List<PluginItem>> = _installedPlugins.asStateFlow()

    // Callbacks for chat interaction from Lua
    private var onInsertChatMessage: ((String, Boolean) -> Unit)? = null
    private var onGetActiveBotName: (() -> String)? = null
    private var onGetActiveCharacter: (() -> CharacterEntity?)? = null
    private var activeChatId: Long = 0L

    val BUILTIN_PLUGINS = listOf(
        PluginItem(
            id = "motion_blur_pro",
            name = "Motion Blur & Shape Styler",
            version = "1.2.0",
            author = "Untitled Core",
            description = "Включает динамический Motion Blur при скроллинге и Scalloped форму кнопок.",
            script = """
                -- === MOTION BLUR & BUTTON SHAPES ===
                plugin.register({
                    id = "motion_blur_pro",
                    name = "Motion Blur & Shape Styler",
                    version = "1.2.1",
                    author = "Untitled Core",
                    description = "Включает динамический Motion Blur и Scalloped форму кнопок"
                })

                app.enableMotionBlur(14.0)
                app.setButtonShape("cookie", 12, 0.06)

                hooks.register("onDisable", function()
                    app.disableMotionBlur()
                    app.setButtonShape("default")
                    print("🧹 Motion Blur & Button Shapes возвращены в исходное состояние")
                end)

                print("✨ Motion Blur & Button Shape Styler активирован!")
            """.trimIndent(),
            isEnabled = false,
            isBuiltIn = true,
            permissions = setOf("APP")
        ),
        PluginItem(
            id = "sparkle_companion",
            name = "Floating Pet Companion",
            version = "1.1.1",
            author = "Untitled Core",
            description = "Анимированный компаньон на экране со звуками и реакцией на события.",
            script = """
                -- === FLOATING COMPANION & SFX ===
                plugin.register({
                    id = "sparkle_companion",
                    name = "Floating Pet Companion",
                    version = "1.1.1",
                    author = "Untitled Core",
                    description = "Анимированный оверлей компаньона со звуками",
                    permissions = { "OVERLAY", "AUDIO", "HOOKS" }
                })

                overlay.showImage("sparkle_companion", "sparkle", 0.88, 0.22, 54, 54, 0.95)

                hooks.register("onMessageReceived", function(botName, text)
                    print("Сообщение от " .. botName .. "! Анимируем компаньона...")
                    audio.play("bell", 0.8)
                    overlay.animateImage("sparkle_companion", 0.84, 0.26, 450, 20, 1.25)
                    return text
                end)

                hooks.register("onChatOpened", function(chatId, botName)
                    print("Открыт чат #" .. chatId .. " с " .. botName)
                    audio.play("pop", 0.5)
                    overlay.animateImage("sparkle_companion", 0.88, 0.22, 500, 0, 1.0)
                end)

                hooks.register("onDisable", function()
                    overlay.hideImage("sparkle_companion")
                    print("🧹 Floating Pet Companion скрыт с экрана")
                end)

                print("🐾 Floating Pet Companion готов к работе!")
            """.trimIndent(),
            isEnabled = false,
            isBuiltIn = true,
            permissions = setOf("OVERLAY", "AUDIO", "HOOKS")
        ),
        PluginItem(
            id = "chat_slash_commands",
            name = "Slash Commands Pack",
            version = "1.4.0",
            author = "Untitled Core",
            description = "Слэш-команды для чата: /roll, /coin, /shrug, /time, /say [текст], /clearoverlay.",
            script = """
                -- === CHAT SLASH COMMANDS PACK ===
                plugin.register({
                    id = "chat_slash_commands",
                    name = "Slash Commands Pack",
                    version = "1.4.0",
                    author = "Untitled Core",
                    description = "Добавляет /roll, /coin, /shrug, /time, /say, /clearoverlay",
                    permissions = { "HOOKS", "CHAT", "AUDIO" }
                })

                hooks.register("onSlashCommand", function(cmd, args)
                    if cmd == "roll" then
                        local maxVal = tonumber(args) or 100
                        local res = math.random(1, maxVal)
                        local text = "🎲 Бросок кубика (1-" .. maxVal .. "): " .. res
                        audio.play("pop")
                        app.toast(text)
                        chat.insertMessage(text, true)
                        return true
                    elseif cmd == "coin" then
                        local res = (math.random(1, 2) == 1) and "Орёл 🦅" or "Решка 🪙"
                        local text = "🪙 Монетка: " .. res
                        audio.play("bell")
                        app.toast(text)
                        chat.insertMessage(text, true)
                        return true
                    elseif cmd == "say" then
                        if args ~= "" then
                            audio.tts(args, "ru")
                            app.toast("🗣️ Озвучивание: " .. args)
                            return true
                        end
                    elseif cmd == "shrug" then
                        chat.insertMessage("¯\\_(ツ)_/¯", true)
                        return true
                    elseif cmd == "time" then
                        local t = os.date("%H:%M:%S")
                        app.toast("⏰ Текущее время: " .. t)
                        return true
                    elseif cmd == "clearoverlay" then
                        overlay.clear()
                        ui.clearWidgets()
                        app.toast("Оверлеи и виджеты очищены")
                        return true
                    end
                    return false
                end)

                print("⚡ Slash Commands Pack зарегистрирован!")
            """.trimIndent(),
            isEnabled = true,
            isBuiltIn = true,
            permissions = setOf("HOOKS", "CHAT", "AUDIO")
        ),
        PluginItem(
            id = "interactive_control_widget",
            name = "Interactive Floating Widget",
            version = "1.0.0",
            author = "Untitled Core",
            description = "Демонстрация модуля UI: плавающий виджет на экране с кнопками и звуками.",
            script = """
                -- === INTERACTIVE FLOATING WIDGET ===
                plugin.register({
                    id = "interactive_control_widget",
                    name = "Interactive Floating Widget",
                    version = "1.0.0",
                    author = "Untitled Core",
                    description = "Плавающий перетаскиваемый пульт с кнопками",
                    permissions = { "UI", "AUDIO", "APP" }
                })

                ui.showWidget({
                    id = "control_pad",
                    title = "🎮 Пульт Управления",
                    content = "Перетаскивайте виджет пальцем!",
                    x = 0.5,
                    y = 0.15,
                    width = 250,
                    buttons = {
                        {
                            text = "🔊 Дзынь",
                            onClick = function()
                                audio.play("levelup")
                                app.toast("Уровень повышен! ✨")
                            end
                        },
                        {
                            text = "🌗 Тема",
                            onClick = function()
                                local s = app.getSettings()
                                local nextTheme = (s.theme == "dark") and "light" or "dark"
                                app.setTheme(nextTheme)
                                audio.play("pop")
                            end
                        }
                    }
                })

                hooks.register("onDisable", function()
                    ui.hideWidget("control_pad")
                    print("🧹 Control Pad закрыт")
                end)

                print("🎮 Interactive Control Widget запущен!")
            """.trimIndent(),
            isEnabled = false,
            isBuiltIn = true,
            permissions = setOf("UI", "AUDIO", "APP")
        ),
        PluginItem(
            id = "web_quote_helper",
            name = "Web Quote of the Day",
            version = "1.0.0",
            author = "Untitled Core",
            description = "Демонстрация модуля HTTP: команда /quote загружает цитату из сети.",
            script = """
                -- === WEB QUOTE OF THE DAY ===
                plugin.register({
                    id = "web_quote_helper",
                    name = "Web Quote of the Day",
                    version = "1.0.0",
                    author = "Untitled Core",
                    description = "Загружает случайную мудрую цитату из интернета через /quote",
                    permissions = { "HTTP", "HOOKS", "CHAT" }
                })

                hooks.register("onSlashCommand", function(cmd, args)
                    if cmd == "quote" then
                        app.toast("🌐 Загрузка цитаты из интернета...")
                        timer.after(50, function()
                            local res = http.get("https://dummyjson.com/quotes/random")
                            if res.ok and res.body ~= "" then
                                -- Простой поиск цитаты и автора по полям
                                local quote = string.match(res.body, '"quote"%s*:%s*"([^"]+)"')
                                local author = string.match(res.body, '"author"%s*:%s*"([^"]+)"')
                                if quote and author then
                                    local msg = "💬 «" .. quote .. "» — " .. author
                                    chat.insertMessage(msg, true)
                                    return
                                end
                            end
                            chat.insertMessage("«Жизнь — это то, что происходит, пока ты строишь другие планы.» — Джон Леннон", true)
                        end)
                        return true
                    end
                    return false
                end)

                print("🌐 Web Quote Helper зарегистрирован!")
            """.trimIndent(),
            isEnabled = false,
            isBuiltIn = true,
            permissions = setOf("HTTP", "HOOKS", "CHAT")
        ),
        PluginItem(
            id = "smart_time_context",
            name = "Реальное время суток в промпте",
            version = "1.0.0",
            author = "Untitled Core",
            description = "Автоматически подмешивает скрытый контекст текущего часа в запрос (ночь, утро, вечер), заставляя бота учитывать время.",
            script = """
                -- === РЕАЛЬНОЕ ВРЕМЯ СУТОК В ПРОМПТЕ ===
                plugin.register({
                    id = "smart_time_context",
                    name = "Реальное время суток в промпте",
                    version = "1.0.0",
                    author = "Untitled Core",
                    description = "Подмешивает контекст времени суток в промпт",
                    permissions = { "HOOKS" }
                })

                hooks.register("onBeforeSendPrompt", function(prompt)
                    local hour = tonumber(os.date("%H")) or 12
                    local timeDesc = "день"
                    if hour >= 23 or hour < 5 then
                        timeDesc = "глухая ночь (пользователь пишет тебе ночью, отвечай сонно или поинтересуйся, почему он еще не спит)"
                    elseif hour >= 5 and hour < 11 then
                        timeDesc = "раннее утро (пожелай бодрого утра или попей кофе)"
                    elseif hour >= 18 and hour < 23 then
                        timeDesc = "вечер (время отдыха после долгого дня)"
                    end

                    local contextTag = "\n[Скрытый контекст окружения: сейчас в реальном мире " .. timeDesc .. "]"
                    print("⏰ Добавлен контекст времени: " .. timeDesc)
                    return prompt .. contextTag
                end)

                print("⏰ Плагин 'Реальное время суток' активирован!")
            """.trimIndent(),
            isEnabled = false,
            isBuiltIn = true,
            permissions = setOf("HOOKS")
        ),
        PluginItem(
            id = "dynamic_mood_tracker",
            name = "Динамический трекер настроения бота",
            version = "1.0.0",
            author = "Untitled Core",
            description = "Анализирует эмоциональный накал ответов бота, адаптирует характер в базе SQLite и запускает звуковые алерты.",
            script = """
                -- === ДИНАМИЧЕСКИЙ ТРЕКЕР НАСТРОЕНИЯ БОТА ===
                plugin.register({
                    id = "dynamic_mood_tracker",
                    name = "Динамический трекер настроения бота",
                    version = "1.0.0",
                    author = "Untitled Core",
                    description = "Адаптирует характер бота по ходу сюжета диалога",
                    permissions = { "HOOKS", "DB", "AUDIO", "APP" }
                })

                hooks.register("onMessageReceived", function(botName, text)
                    local bot = db.getActiveCharacter()
                    if not bot then return text end

                    local lower = string.lower(text)
                    -- Реакция на злость или конфликт
                    if string.find(lower, "ненавижу") or string.find(lower, "уходи") or string.find(lower, "предатель") or string.find(lower, "злится") then
                        audio.play("alert")
                        app.toast("💢 Настроение «" .. botName .. "» ухудшилось!")
                        if not string.find(bot.personality, "раздражен") then
                            db.updateCharacter(bot.id, {
                                personality = bot.personality .. "\n[Текущее настроение: ты очень раздражен и зол на собеседника]"
                            })
                            print("💢 Характер бота обновлен: добавлено раздражение.")
                        end
                    -- Реакция на ласку, похвалу или любовь
                    elseif string.find(lower, "люблю") or string.find(lower, "милый") or string.find(lower, "спасибо") or string.find(lower, "улыбается") then
                        audio.play("levelup")
                        app.toast("💖 «" .. botName .. "» в восторге от общения!")
                        if not string.find(bot.personality, "нежность") then
                            db.updateCharacter(bot.id, {
                                personality = bot.personality .. "\n[Текущее настроение: ты испытываешь нежность, симпатию и теплоту]"
                            })
                            print("💖 Характер бота обновлен: добавлена нежность.")
                        end
                    end

                    return text
                end)

                print("💖 Динамический трекер настроения бота запущен!")
            """.trimIndent(),
            isEnabled = false,
            isBuiltIn = true,
            permissions = setOf("HOOKS", "DB", "AUDIO", "APP")
        )
    )

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        prefsRepository = UserPreferencesRepository(context.applicationContext)
        val db = AppDatabase.getDatabase(context.applicationContext)
        appDatabase = db
        roleplayRepository = RoleplayRepository(db)

        PluginAudioManager.init(context.applicationContext)

        scope.launch {
            loadPluginsFromDisk()
            runAllActivePlugins()
        }
    }

    fun setChatCallbacks(
        chatId: Long,
        insertMessage: (String, Boolean) -> Unit,
        getActiveBotName: () -> String,
        getActiveCharacter: () -> CharacterEntity?
    ) {
        this.activeChatId = chatId
        this.onInsertChatMessage = insertMessage
        this.onGetActiveBotName = getActiveBotName
        this.onGetActiveCharacter = getActiveCharacter
    }

    private fun getPluginsDir(): File {
        val dir = File(appContext?.filesDir, "plugins")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun loadPluginsFromDisk() {
        val dir = getPluginsDir()
        val manifestFile = File(dir, MANIFEST_FILENAME)

        val diskPlugins = mutableListOf<PluginItem>()

        if (manifestFile.exists()) {
            try {
                val jsonStr = manifestFile.readText()
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val scriptFile = File(dir, "$id.lua")
                    val script = if (scriptFile.exists()) scriptFile.readText() else obj.optString("script", "")

                    val perms = mutableSetOf<String>()
                    val permArr = obj.optJSONArray("permissions")
                    if (permArr != null) {
                        for (p in 0 until permArr.length()) {
                            perms.add(permArr.getString(p))
                        }
                    }

                    diskPlugins.add(
                        PluginItem(
                            id = id,
                            name = obj.optString("name", id),
                            version = obj.optString("version", "1.0.0"),
                            author = obj.optString("author", "Developer"),
                            description = obj.optString("description", ""),
                            script = script,
                            isEnabled = obj.optBoolean("isEnabled", true),
                            isBuiltIn = obj.optBoolean("isBuiltIn", false),
                            permissions = perms
                        )
                    )
                }
            } catch (e: Exception) {
                ULuaEngine.appendLog("⚠️ Failed to parse plugins manifest: ${e.message}")
            }
        }

        // Merge with built-in plugins (ensure all built-in plugins exist)
        val finalMap = LinkedHashMap<String, PluginItem>()
        BUILTIN_PLUGINS.forEach { builtin ->
            finalMap[builtin.id] = builtin
        }
        // Override with disk state if saved
        diskPlugins.forEach { disk ->
            val existing = finalMap[disk.id]
            if (existing != null && existing.isBuiltIn) {
                // Keep the updated script and metadata from builtin, but preserve user's isEnabled preference
                finalMap[disk.id] = existing.copy(isEnabled = disk.isEnabled)
            } else {
                finalMap[disk.id] = disk
            }
        }

        _installedPlugins.value = finalMap.values.toList()
        savePluginsToDisk()
    }

    private fun savePluginsToDisk() {
        val dir = getPluginsDir()
        val manifestFile = File(dir, MANIFEST_FILENAME)

        try {
            val array = JSONArray()
            _installedPlugins.value.forEach { item ->
                val scriptFile = File(dir, "${item.id}.lua")
                scriptFile.writeText(item.script)

                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("version", item.version)
                    put("author", item.author)
                    put("description", item.description)
                    put("isEnabled", item.isEnabled)
                    put("isBuiltIn", item.isBuiltIn)
                    put("permissions", JSONArray(item.permissions))
                }
                array.put(obj)
            }
            manifestFile.writeText(array.toString(2))
        } catch (e: Exception) {
            ULuaEngine.appendLog("⚠️ Error saving plugins manifest: ${e.message}")
        }
    }

    fun togglePlugin(id: String, enabled: Boolean) {
        val updated = _installedPlugins.value.map {
            if (it.id == id) it.copy(isEnabled = enabled) else it
        }
        _installedPlugins.value = updated
        savePluginsToDisk()

        if (enabled) {
            val target = updated.firstOrNull { it.id == id }
            if (target != null) {
                executePlugin(target)
            }
        } else {
            ULuaEngine.clearPluginHooks(id)
            ULuaEngine.appendLog("🔌 Plugin [$id] deactivated.")
        }
    }

    fun savePlugin(plugin: PluginItem) {
        val detectedPerms = detectPermissions(plugin.script)
        val enriched = plugin.copy(permissions = detectedPerms)

        val current = _installedPlugins.value.toMutableList()
        val index = current.indexOfFirst { it.id == enriched.id }
        if (index >= 0) {
            current[index] = enriched
        } else {
            current.add(enriched)
        }
        _installedPlugins.value = current
        savePluginsToDisk()

        if (enriched.isEnabled) {
            executePlugin(enriched)
        }
    }

    fun deletePlugin(id: String) {
        val target = _installedPlugins.value.firstOrNull { it.id == id } ?: return
        if (target.isBuiltIn) {
            togglePlugin(id, false)
            return
        }

        ULuaEngine.clearPluginHooks(id)
        val updated = _installedPlugins.value.filter { it.id != id }
        _installedPlugins.value = updated

        val scriptFile = File(getPluginsDir(), "$id.lua")
        if (scriptFile.exists()) scriptFile.delete()

        savePluginsToDisk()
        ULuaEngine.appendLog("🗑️ Plugin [$id] deleted.")
    }

    fun importPlugin(rawScript: String): PluginItem {
        val id = "imported_" + UUID.randomUUID().toString().take(8)
        var name = "Imported Plugin"
        var author = "Unknown"
        var version = "1.0.0"
        var desc = ""

        val idMatch = Regex("""id\s*=\s*["']([^"']+)["']""").find(rawScript)
        val finalId = idMatch?.groupValues?.get(1)?.trim()?.ifBlank { null } ?: id

        val nameMatch = Regex("""name\s*=\s*["']([^"']+)["']""").find(rawScript)
        if (nameMatch != null) name = nameMatch.groupValues[1]

        val authorMatch = Regex("""author\s*=\s*["']([^"']+)["']""").find(rawScript)
        if (authorMatch != null) author = authorMatch.groupValues[1]

        val verMatch = Regex("""version\s*=\s*["']([^"']+)["']""").find(rawScript)
        if (verMatch != null) version = verMatch.groupValues[1]

        val descMatch = Regex("""description\s*=\s*["']([^"']+)["']""").find(rawScript)
        if (descMatch != null) desc = descMatch.groupValues[1]

        val perms = detectPermissions(rawScript)

        val item = PluginItem(
            id = finalId,
            name = name,
            version = version,
            author = author,
            description = desc,
            script = rawScript,
            isEnabled = true,
            isBuiltIn = false,
            permissions = perms
        )

        savePlugin(item)
        return item
    }

    fun detectPermissions(script: String): Set<String> {
        val perms = mutableSetOf<String>()
        if (script.contains("http.")) perms.add("NET")
        if (script.contains("db.")) perms.add("DB")
        if (script.contains("ui.")) perms.add("UI")
        if (script.contains("audio.")) perms.add("AUDIO")
        if (script.contains("overlay.")) perms.add("OVERLAY")
        if (script.contains("hooks.")) perms.add("HOOKS")
        if (script.contains("timer.")) perms.add("TIMER")
        if (script.contains("app.")) perms.add("APP")
        return perms
    }

    fun runAllActivePlugins() {
        ULuaEngine.clearAllHooks()
        _installedPlugins.value.filter { it.isEnabled }.forEach { plugin ->
            executePlugin(plugin)
        }
    }

    private fun executePlugin(plugin: PluginItem): LuaExecutionResult {
        val context = appContext ?: return LuaExecutionResult(false, "Context not initialized")
        val repo = prefsRepository
        val db = appDatabase
        val roleRepo = roleplayRepository

        return ULuaEngine.execute(
            script = plugin.script,
            pluginId = plugin.id,
            context = context,
            getSettings = {
                runBlocking {
                    repo?.userSettingsFlow?.first() ?: UserSettings()
                }
            },
            updateSettings = { newSettings ->
                scope.launch {
                    repo?.updateSettings(newSettings)
                }
            },
            insertMessageCallback = onInsertChatMessage,
            getActiveBotNameCallback = onGetActiveBotName,
            getCharactersCallback = {
                runBlocking {
                    db?.characterDao()?.getAllCharactersList() ?: emptyList()
                }
            },
            getActiveCharacterCallback = onGetActiveCharacter,
            updateCharacterCallback = { charId, updates ->
                scope.launch {
                    val existing = roleRepo?.getCharacterById(charId) ?: return@launch
                    var modified: CharacterEntity = existing
                    updates["name"]?.let { modified = modified.copy(name = it as String) }
                    updates["description"]?.let { modified = modified.copy(shortDescription = it as String) }
                    updates["personality"]?.let { modified = modified.copy(personality = it as String) }
                    updates["greetingMessage"]?.let { modified = modified.copy(greetingMessage = it as String) }
                    updates["avatarUrl"]?.let { modified = modified.copy(avatarUri = it as String) }
                    roleRepo.saveCharacter(modified)
                }
            },
            getRecentMessagesCallback = { limit ->
                runBlocking {
                    if (activeChatId > 0L) {
                        db?.messageDao()?.getRecentMessages(activeChatId, limit)?.reversed() ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
            }
        )
    }

    fun executeSandbox(script: String): LuaExecutionResult {
        val context = appContext ?: return LuaExecutionResult(false, "Context not initialized")
        val repo = prefsRepository
        val db = appDatabase
        val roleRepo = roleplayRepository

        return ULuaEngine.execute(
            script = script,
            pluginId = "sandbox_tester",
            context = context,
            getSettings = {
                runBlocking {
                    repo?.userSettingsFlow?.first() ?: UserSettings()
                }
            },
            updateSettings = { newSettings ->
                scope.launch {
                    repo?.updateSettings(newSettings)
                }
            },
            insertMessageCallback = onInsertChatMessage,
            getActiveBotNameCallback = onGetActiveBotName,
            getCharactersCallback = {
                runBlocking {
                    db?.characterDao()?.getAllCharactersList() ?: emptyList()
                }
            },
            getActiveCharacterCallback = onGetActiveCharacter,
            updateCharacterCallback = { charId, updates ->
                scope.launch {
                    val existing = roleRepo?.getCharacterById(charId) ?: return@launch
                    var modified: CharacterEntity = existing
                    updates["name"]?.let { modified = modified.copy(name = it as String) }
                    updates["description"]?.let { modified = modified.copy(shortDescription = it as String) }
                    updates["personality"]?.let { modified = modified.copy(personality = it as String) }
                    updates["greetingMessage"]?.let { modified = modified.copy(greetingMessage = it as String) }
                    updates["avatarUrl"]?.let { modified = modified.copy(avatarUri = it as String) }
                    roleRepo.saveCharacter(modified)
                }
            },
            getRecentMessagesCallback = { limit ->
                runBlocking {
                    if (activeChatId > 0L) {
                        db?.messageDao()?.getRecentMessages(activeChatId, limit)?.reversed() ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
            }
        )
    }

    // Hook Dispatchers
    fun dispatchBeforeSendPrompt(prompt: String): String = ULuaEngine.dispatchBeforeSendPrompt(prompt)
    fun dispatchMessageReceived(botName: String, text: String): String = ULuaEngine.dispatchMessageReceived(botName, text)
    fun dispatchSlashCommand(rawCommand: String): Boolean = ULuaEngine.dispatchSlashCommand(rawCommand)
    fun dispatchChatOpened(chatId: Long, botName: String) {
        this.activeChatId = chatId
        ULuaEngine.dispatchChatOpened(chatId, botName)
    }
}
