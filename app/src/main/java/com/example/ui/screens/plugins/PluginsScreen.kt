package com.example.ui.screens.plugins

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.datastore.UserSettings
import com.example.util.PluginItem
import com.example.util.PluginManager
import com.example.util.ULuaEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    userSettings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    val isFemboy = com.example.util.LocalAppLanguage.current.equals("femboy", ignoreCase = true)

    var currentTab by remember { mutableIntStateOf(0) } // 0 = Plugins, 1 = Lua IDE & Console, 2 = API Docs
    val installedPlugins by PluginManager.installedPlugins.collectAsStateWithLifecycle()
    val consoleLogs by ULuaEngine.consoleLogs.collectAsStateWithLifecycle()

    var editingPluginId by remember { mutableStateOf<String?>(null) }
    var ideScriptText by remember {
        mutableStateOf(
            """
            -- === НОВЫЙ LUA ПЛАГИН С ПОЛНЫМИ ПРАВАМИ ===
            plugin.register({
                id = "my_super_plugin",
                name = "My Super Plugin",
                version = "1.0.0",
                author = "bad_kn1fe",
                description = "Кастомный мод с сетью, UI и звуками",
                permissions = { "NET", "UI", "AUDIO", "HOOKS", "TIMER" }
            })

            -- 1. Звуковое приветствие и тост
            audio.play("levelup")
            app.toast("Плагин успешно инициализирован! ✨")

            -- 2. Плавающее перетаскиваемое окно управления
            ui.showWidget({
                id = "quick_actions",
                title = "⚡ Быстрые действия",
                content = "Плавающий пульт прямо на экране!",
                x = 0.5,
                y = 0.18,
                buttons = {
                    {
                        text = "🔊 Звук",
                        onClick = function() audio.play("bell") end
                    },
                    {
                        text = "🗣️ Голос",
                        onClick = function() audio.tts("Привет, bad_kn1fe!", "ru") end
                    }
                }
            })

            print("✅ Скрипт успешно выполнен!")
            """.trimIndent()
        )
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newPluginName by remember { mutableStateOf("") }
    var newPluginDesc by remember { mutableStateOf("") }

    // Security Disclaimer Dialog State
    var pendingImportScript by remember { mutableStateOf<String?>(null) }
    var pendingPluginToEnable by remember { mutableStateOf<PluginItem?>(null) }

    val pluginImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val rawText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (rawText.isNotBlank()) {
                    val detected = PluginManager.detectPermissions(rawText)
                    if (detected.contains("NET") || detected.contains("DB")) {
                        pendingImportScript = rawText
                    } else {
                        val imported = PluginManager.importPlugin(rawText)
                        Toast.makeText(context, if (isEn) "Plugin '${imported.name}' imported!" else "Плагин «${imported.name}» импортирован!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, if (isEn) "Empty file" else "Файл плагина пуст", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isFemboy) "🔌 Lua Моды и Плагинчики (uwu)" else if (isEn) "🔌 Lua Plugins & Engine" else "🔌 Lua Моды и Расширения",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isEn) "Back" else "Назад")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { pluginImportLauncher.launch("*/*") },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEn) "Import" else "Импорт", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = currentTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text(if (isEn) "Plugins (${installedPlugins.size})" else "Плагины (${installedPlugins.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text(if (isEn) "Lua IDE & Logs" else "Lua IDE и Логи", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    text = { Text(if (isEn) "Full API Manual" else "Справка API (Все модули)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            when (currentTab) {
                0 -> {
                    // TAB 0: PLUGINS LIST
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item(key = "create_plugin_header") {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                        Text(
                                            text = if (isEn) "🚀 Full Native Lua 5.2 Platform" else "🚀 Полноценная платформа Lua 5.2",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isEn) "Access HTTP network, SQLite characters/history, floating UI widgets, SFX audio & timers."
                                            else "Сетевой стек HTTP, доступ к базе персонажей и истории, виджеты UI, звуки и таймеры.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = { showCreateDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEn) "Create" else "Создать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        items(installedPlugins, key = { it.id }) { plugin ->
                            PluginItemCard(
                                plugin = plugin,
                                isEn = isEn,
                                onToggle = { enabled ->
                                    if (enabled && (plugin.permissions.contains("NET") || plugin.permissions.contains("DB")) && !plugin.isBuiltIn) {
                                        pendingPluginToEnable = plugin
                                    } else {
                                        PluginManager.togglePlugin(plugin.id, enabled)
                                    }
                                },
                                onEdit = {
                                    editingPluginId = plugin.id
                                    ideScriptText = plugin.script
                                    currentTab = 1
                                },
                                onDelete = {
                                    PluginManager.deletePlugin(plugin.id)
                                    Toast.makeText(context, if (isEn) "Plugin deleted" else "Плагин удален", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: LUA IDE & CONSOLE
                    val consoleListState = rememberLazyListState()
                    LaunchedEffect(consoleLogs.size) {
                        if (consoleLogs.isNotEmpty()) {
                            consoleListState.animateScrollToItem(consoleLogs.size - 1)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingPluginId != null) "✏️ Редактирование: ${editingPluginId}" else "🧪 Песочница скрипта",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (editingPluginId != null) {
                                    TextButton(onClick = { editingPluginId = null }) {
                                        Text(if (isEn) "New script" else "Новый", fontSize = 11.sp)
                                    }
                                }
                                TextButton(onClick = { ULuaEngine.clearLogs() }) {
                                    Text(if (isEn) "Clear log" else "Очистить логи", fontSize = 11.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = ideScriptText,
                            onValueChange = { ideScriptText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1.1f),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Extended Quick snippet pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                onClick = { ideScriptText += "\nlocal res = http.get('https://dummyjson.com/quotes/random')\nprint('Status:', res.status, res.body)" },
                                label = { Text("http.get", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = {
                                    ideScriptText += "\nui.showWidget({\n  id = 'w1',\n  title = 'Виджет',\n  content = 'Текст виджета',\n  buttons = {{\n    text = 'Нажми меня',\n    onClick = function() audio.play('pop') end\n  }}\n})"
                                },
                                label = { Text("ui.showWidget", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = { ideScriptText += "\naudio.play('levelup', 1.0)\naudio.tts('Привет из Луа!', 'ru')" },
                                label = { Text("audio.play & tts", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = { ideScriptText += "\nlocal chars = db.getCharacters()\nfor i, c in ipairs(chars) do\n  print(c.id, c.name)\nend" },
                                label = { Text("db.getCharacters", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = { ideScriptText += "\ntimer.after(2000, function()\n  app.toast('Прошло 2 секунды!')\nend)" },
                                label = { Text("timer.after", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = { ideScriptText += "\noverlay.showImage('pet', 'sparkle', 0.85, 0.25, 52, 52, 1.0)" },
                                label = { Text("overlay.show", fontSize = 11.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val res = PluginManager.executeSandbox(ideScriptText)
                                    if (res.success) {
                                        Toast.makeText(context, if (isEn) "Executed successfully!" else "Выполнено успешно!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Ошибка: ${res.error}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEn) "Run Test" else "Запустить тест", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val targetId = editingPluginId ?: ("custom_" + System.currentTimeMillis() % 10000)
                                    val existing = PluginManager.installedPlugins.value.firstOrNull { it.id == targetId }
                                    val itemToSave = existing?.copy(script = ideScriptText) ?: PluginItem(
                                        id = targetId,
                                        name = "Пользовательский мод",
                                        version = "1.0.0",
                                        author = "bad_kn1fe",
                                        description = "Создан в Lua IDE",
                                        script = ideScriptText,
                                        isEnabled = true,
                                        isBuiltIn = false
                                    )
                                    PluginManager.savePlugin(itemToSave)
                                    editingPluginId = itemToSave.id
                                    Toast.makeText(context, if (isEn) "Plugin saved!" else "Плагин сохранён!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEn) "Save Plugin" else "Сохранить", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.9f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📟 Консоль вывода (print / logs):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                if (consoleLogs.isEmpty()) {
                                    Text(
                                        text = "Логи пусты. Нажмите «Запустить тест» для исполнения.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                } else {
                                    LazyColumn(
                                        state = consoleListState,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(consoleLogs) { logLine ->
                                            Text(
                                                text = logLine,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                ),
                                                color = if (logLine.contains("❌") || logLine.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: FULL API MANUAL
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("🚀 Untitled Lua 5.2 Full API Reference", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Полная документация по всем 9 встроенным модулям расширений.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        item {
                            DocSection(
                                title = "1. Модуль http (Сетевой стек)",
                                items = listOf(
                                    "http.get(url, [headers])" to "Синхронный GET запрос. Возвращает таблицу { status = 200, ok = true, body = '...' }.",
                                    "http.post(url, body, [contentType], [headers])" to "Синхронный POST запрос с отправкой данных (JSON/текст). Возвращает таблицу ответа."
                                )
                            )
                        }

                        item {
                            DocSection(
                                title = "2. Модуль ui (Плавающие окна и диалоги)",
                                items = listOf(
                                    "ui.showWidget({ id, title, content, x, y, width, buttons })" to "Создает перетаскиваемый интерактивный пульт с кнопками поверх любого экрана.",
                                    "ui.hideWidget(id)" to "Скрывает плавающее окно по ID.",
                                    "ui.clearWidgets()" to "Закрывает все созданные плагинами виджеты.",
                                    "ui.showDialog({ title, message, confirmText, cancelText, onConfirm, onCancel })" to "Открывает системный модальный Compose AlertDialog."
                                )
                            )
                        }

                        item {
                            DocSection(
                                title = "3. Модуль db (Персонажи, лор и сообщения)",
                                items = listOf(
                                    "db.getCharacters()" to "Возвращает массив всех персонажей из базы Room SQLite: { id, name, description, avatarUrl, systemPrompt, greeting }.",
                                    "db.getActiveCharacter()" to "Возвращает таблицу текущего персонажа, с которым открыт чат.",
                                    "db.updateCharacter(id, { name, description, systemPrompt, greeting, avatarUrl })" to "Напрямую изменяет данные или системный промпт персонажа в базе данных!",
                                    "db.getRecentMessages([limit])" to "Возвращает список последних сообщений текущего диалога."
                                )
                            )
                        }

                        item {
                            DocSection(
                                title = "4. Модуль audio (Звуки и синтез речи TTS)",
                                items = listOf(
                                    "audio.play(presetOrUrl, [volume], [id])" to "Воспроизводит звук (пресеты: 'pop', 'click', 'bell', 'levelup', 'alert' или прямая ссылка https://...).",
                                    "audio.stop(id) / audio.stopAll()" to "Останавливает воспроизведение звука.",
                                    "audio.tts(text, [lang])" to "Озвучивает текст голосом через Android TTS (языки: 'ru', 'en', 'ja')."
                                )
                            )
                        }

                        item {
                            DocSection(
                                title = "5. Модуль timer (Таймеры и интервалы)",
                                items = listOf(
                                    "timer.after(delayMs, func)" to "Вызывает функцию единоразово с задержкой delayMs миллисекунд. Возвращает timerId.",
                                    "timer.every(intervalMs, func)" to "Запускает бесконечный периодический цикл каждые intervalMs миллисекунд. Возвращает timerId.",
                                    "timer.cancel(timerId)" to "Отменяет активный таймер.",
                                    "timer.clearAll()" to "Отменяет все таймеры данного плагина."
                                )
                            )
                        }

                        item {
                            DocSection(
                                title = "6. Модули app, overlay, hooks и chat",
                                items = listOf(
                                    "app.toast, setTheme, setLanguage, enableMotionBlur, setButtonShape" to "Управление системными настройками и оформлением приложения.",
                                    "overlay.showImage, overlay.animateImage, overlay.hideImage" to "Вывод и физическая плавная анимация спрайтов и картинок на экране.",
                                    "hooks.register('onSlashCommand', func(cmd, args))" to "Перехват слэш-команд в поле ввода чата (/roll, /quote, /say и т.д.).",
                                    "hooks.register('onBeforeSendPrompt', func(prompt))" to "Трансформация или аугментация промпта перед отправкой нейросети.",
                                    "hooks.register('onMessageReceived', func(botName, text))" to "Реакция на входящий ответ от ИИ-персонажа.",
                                    "chat.insertMessage(text, isUser)" to "Вставка сообщения в текущую переписку."
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Security Warning Dialog on Importing External Script with NET / DB
    if (pendingImportScript != null) {
        val script = pendingImportScript ?: ""
        val detected = remember(script) { PluginManager.detectPermissions(script) }

        AlertDialog(
            onDismissRequest = { pendingImportScript = null },
            icon = {
                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = if (isEn) "🛡️ Plugin Security Warning" else "🛡️ Предупреждение о безопасности плагина",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isEn) "This plugin requests powerful system permissions:"
                        else "Этот плагин запрашивает расширенные системные разрешения:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        detected.forEach { perm ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (perm == "NET" || perm == "DB") MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "[$perm]",
                                    color = if (perm == "NET" || perm == "DB") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isEn) "Plugins with NET can send requests to external servers. Plugins with DB can modify characters and reading messages. Install only from trusted authors!"
                        else "Плагины с правом NET могут отправлять сетевые запросы во внешние сервисы. Плагины с правом DB могут модифицировать системные промпты персонажей. Устанавливайте скрипты только от проверенных разработчиков!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val imported = PluginManager.importPlugin(script)
                        pendingImportScript = null
                        Toast.makeText(context, if (isEn) "Plugin '${imported.name}' trusted and installed!" else "Плагин «${imported.name}» установлен!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isEn) "I trust this plugin (Install)" else "Я доверяю этому плагину (Установить)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingImportScript = null }) {
                    Text(if (isEn) "Cancel" else "Отмена")
                }
            }
        )
    }

    // Security Warning Dialog on Enabling Plugin with NET / DB
    if (pendingPluginToEnable != null) {
        val plugin = pendingPluginToEnable!!
        AlertDialog(
            onDismissRequest = { pendingPluginToEnable = null },
            icon = {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(36.dp))
            },
            title = {
                Text(
                    text = if (isEn) "Activate Plugin with Permissions?" else "Активировать плагин с расширенными правами?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Плагин «${plugin.name}» использует разрешения: ${plugin.permissions.joinToString { "[$it]" }}.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Убедитесь, что вы проверили код плагина в редакторе перед активацией.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PluginManager.togglePlugin(plugin.id, true)
                        pendingPluginToEnable = null
                    }
                ) {
                    Text("Активировать")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingPluginToEnable = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Create New Plugin Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(if (isEn) "New Lua Plugin" else "Создание Lua плагина", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPluginName,
                        onValueChange = { newPluginName = it },
                        label = { Text(if (isEn) "Plugin Name" else "Название плагина") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPluginDesc,
                        onValueChange = { newPluginDesc = it },
                        label = { Text(if (isEn) "Description" else "Описание") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPluginName.ifBlank { "New Plugin" }
                        val id = "user_" + System.currentTimeMillis() % 100000
                        val initialScript = """
                            -- === $name ===
                            plugin.register({
                                id = "$id",
                                name = "$name",
                                version = "1.0.0",
                                author = "bad_kn1fe",
                                description = "$newPluginDesc",
                                permissions = { "APP", "AUDIO", "UI" }
                            })

                            audio.play("levelup")
                            app.toast("Плагин $name активирован! ✨")
                            print("Плагин $name готов к работе!")
                        """.trimIndent()

                        val newPlugin = PluginItem(
                            id = id,
                            name = name,
                            description = newPluginDesc,
                            script = initialScript,
                            isEnabled = true,
                            isBuiltIn = false,
                            permissions = setOf("APP", "AUDIO", "UI")
                        )
                        PluginManager.savePlugin(newPlugin)
                        showCreateDialog = false
                        newPluginName = ""
                        newPluginDesc = ""
                        editingPluginId = id
                        ideScriptText = initialScript
                        currentTab = 1
                        Toast.makeText(context, "Плагин создан и открыт в IDE!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(if (isEn) "Create & Edit" else "Создать и открыть")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(if (isEn) "Cancel" else "Отмена")
                }
            }
        )
    }
}

@Composable
private fun PluginItemCard(
    plugin: PluginItem,
    isEn: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (plugin.isEnabled) 0.6f else 0.3f),
        border = BorderStroke(1.dp, if (plugin.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (plugin.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (plugin.isBuiltIn) Icons.Default.Extension else Icons.Default.Code,
                                contentDescription = null,
                                tint = if (plugin.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = plugin.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (plugin.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (plugin.isBuiltIn) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Built-in",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "v${plugin.version} • ${plugin.author}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }

                Switch(
                    checked = plugin.isEnabled,
                    onCheckedChange = onToggle
                )
            }

            if (plugin.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (plugin.permissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    plugin.permissions.forEach { perm ->
                        val isSensitive = perm == "NET" || perm == "DB"
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSensitive) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "[$perm]",
                                color = if (isSensitive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEn) "View / Edit" else "Редактировать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (!plugin.isBuiltIn) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = if (isEn) "Delete" else "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))

            items.forEachIndexed { index, (func, desc) ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Column {
                    Text(
                        text = func,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}
