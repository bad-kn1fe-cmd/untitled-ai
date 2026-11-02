package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ai.engine.RoleplayPresets
import com.example.ai.engine.RoleplayPromptBuilder
import com.example.ui.studio.studioElement
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R
import com.example.data.datastore.UserSettings
import com.example.ui.components.*
import com.example.ui.components.AppLogo
import com.example.ui.components.ParallaxChatWallpaper
import com.example.ui.components.WallpaperPresetsList
import com.example.ui.theme.AccentPresets
import com.example.ui.theme.AvailableBaseFonts
import com.example.ui.theme.FontHelper
import com.example.ui.studio.StudioLayoutManager
import com.example.util.AppStrings
import com.example.util.LanguagePackInfo
import kotlinx.coroutines.launch

data class SettingsTabItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null
)

private fun localizeAccentName(name: String, isEn: Boolean): String {
    if (!isEn) return name
    return when (name) {
        "Лаванда" -> "Lavender"
        "Нежная Сакура" -> "Soft Sakura"
        "Розовая пудра" -> "Powder Pink"
        "Сиреневый туман" -> "Lilac Mist"
        "Небесный пастель" -> "Sky Pastel"
        "Мятный мусс" -> "Mint Mousse"
        "Персиковый шелк" -> "Peach Silk"
        "Коралловый крем" -> "Coral Cream"
        "Мальва" -> "Mauve"
        "Неон Циан" -> "Neon Cyan"
        "Электро-Фиолет" -> "Electro-Violet"
        "Неоновый Жёлтый" -> "Neon Yellow"
        "Лазерная Маджента" -> "Laser Magenta"
        "Токсичный Неон" -> "Toxic Neon"
        "Неоновый Оранж" -> "Neon Orange"
        "Ультрамарин" -> "Ultramarine"
        "Плазма" -> "Plasma"
        "Индиго" -> "Indigo"
        "Сапфир" -> "Sapphire"
        "Морская волна" -> "Sea Wave"
        "Изумруд" -> "Emerald"
        "Мята" -> "Mint"
        "Бирюза" -> "Turquoise"
        "Лайм" -> "Lime"
        "Янтарь" -> "Amber"
        "Закат" -> "Sunset"
        "Рубин" -> "Ruby"
        "Сакура" -> "Sakura"
        "Маджента" -> "Magenta"
        "Аметист" -> "Amethyst"
        "Глубокий Сапфир" -> "Deep Sapphire"
        "Бордо" -> "Bordeaux"
        "Золотой Песок" -> "Gold Sand"
        "Шоколад" -> "Chocolate"
        "Сталь" -> "Steel"
        "Обсидиан" -> "Obsidian"
        else -> name
    }
}

private data class HubCategoryItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val isExternalNav: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    settings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenProviders: () -> Unit,
    providers: List<com.example.data.local.entity.AiProviderProfileEntity> = emptyList(),
    activePersona: com.example.data.local.entity.UserPersonaEntity? = null,
    onOpenPersonas: () -> Unit = {},
    onOpenStudioMode: () -> Unit = {},
    onOpenGeminiDebug: () -> Unit = {},
    onOpenVerityGame: (() -> Unit)? = null,
    onOpenSkibidiGame: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    val isFemboy = com.example.util.LocalAppLanguage.current.equals("femboy", ignoreCase = true)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    var showLocalVerityDialog by remember { mutableStateOf(false) }
    var showLocalSkibidiDialog by remember { mutableStateOf(false) }

    val launchVerityGame: () -> Unit = {
        if (onOpenVerityGame != null) {
            onOpenVerityGame()
        } else {
            showLocalVerityDialog = true
        }
    }

    val launchSkibidiGame: () -> Unit = {
        if (onOpenSkibidiGame != null) {
            onOpenSkibidiGame()
        } else {
            showLocalSkibidiDialog = true
        }
    }

    if (showLocalVerityDialog) {
        VerityCareGameDialog(
            settings = settings,
            onUpdateSettings = onUpdateSettings,
            onDismiss = { showLocalVerityDialog = false }
        )
    }

    if (showLocalSkibidiDialog) {
        com.example.ui.components.Skibidi3DForestDialog(
            settings = settings,
            onUpdateSettings = onUpdateSettings,
            onDismiss = { showLocalSkibidiDialog = false }
        )
    }

    val isGeminiMain = remember(providers, settings.defaultTextProviderId) {
        val defaultTextProfile = providers.find { it.isDefaultText }
        (defaultTextProfile != null && defaultTextProfile.providerType.equals("GEMINI", ignoreCase = true)) ||
                (providers.find { it.id == settings.defaultTextProviderId }?.providerType.equals("GEMINI", ignoreCase = true)) ||
                (providers.none { it.isDefaultText } && settings.defaultTextProviderId == null && providers.any { it.providerType.equals("GEMINI", ignoreCase = true) })
    }

    val customWallpapers = remember(settings.customWallpapersJson) {
        settings.getCustomWallpapersList()
    }

    val novelExtra = if (settings.bubbleStyle == "novel") 1 else 0
    val geminiExtra = if (isGeminiMain) 1 else 0
    val wallpapersStartIndex = 12 + novelExtra
    val aiModelsStartIndex = 20 + novelExtra
    val behaviorStartIndex = 27 + novelExtra + geminiExtra
    val systemStartIndex = 33 + novelExtra + geminiExtra

    val ultraLazyCategoryMode = settings.settingsCategoryIsolationMode
    var selectedCategoryTab by remember { mutableIntStateOf(0) }
    var clickedTabOverride by remember { mutableStateOf<Int?>(null) }

    val activeTabByScroll by remember(ultraLazyCategoryMode, selectedCategoryTab, wallpapersStartIndex, aiModelsStartIndex, behaviorStartIndex, systemStartIndex) {
        derivedStateOf {
            if (ultraLazyCategoryMode) {
                selectedCategoryTab
            } else {
                val firstVisible = listState.firstVisibleItemIndex
                val canScrollForward = listState.canScrollForward
                when {
                    !canScrollForward && firstVisible >= behaviorStartIndex -> 4
                    firstVisible >= systemStartIndex -> 4
                    firstVisible >= behaviorStartIndex -> 3
                    firstVisible >= aiModelsStartIndex -> 2
                    firstVisible >= wallpapersStartIndex -> 1
                    else -> 0
                }
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!ultraLazyCategoryMode && listState.isScrollInProgress && clickedTabOverride != null) {
            clickedTabOverride = null
        }
    }

    val settingsTabs = remember(isEn) {
        listOf(
            SettingsTabItem(if (isEn) "Appearance" else "Оформление", if (isEn) "Themes & color" else "Темы & цвет", Icons.Default.Palette),
            SettingsTabItem(if (isEn) "Wallpapers" else "Обои", if (isEn) "FX & Backgrounds" else "Фоны & эффект", Icons.Default.Wallpaper),
            SettingsTabItem(if (isEn) "AI & Models" else "ИИ & Модели", if (isEn) "Providers & API" else "Провайдеры & сеть", Icons.Default.Psychology),
            SettingsTabItem(if (isEn) "Behavior" else "Поведение", if (isEn) "Chat & tokens" else "Чат & токены", Icons.Default.Forum, badge = "AI"),
            SettingsTabItem(if (isEn) "System" else "Система", if (isEn) "Backup & system" else "Бэкап & инфо", Icons.Default.SettingsBackupRestore)
        )
    }
    val isVaultHub = settings.uiVariant == "vault_hub"
    var vaultHubSection by rememberSaveable { mutableStateOf<Int?>(null) }

    BackHandler(enabled = isVaultHub && vaultHubSection != null) {
        vaultHubSection = null
    }

    var showCustomHexDialog by remember { mutableStateOf(false) }
    var showDefaultPromptDialog by remember { mutableStateOf(false) }
    var showDownloadPromptConfirmDialog by remember { mutableStateOf(false) }
    var isDownloadingPrompt by remember { mutableStateOf(false) }
    var showFrameRepositoryDialog by remember { mutableStateOf(false) }

    val customWallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = com.example.util.ImageStorageHelper.saveWallpaperFile(context, uri)
            if (savedPath != null) {
                val updated = settings.withAddedCustomWallpaper(savedPath)
                onUpdateSettings(updated)
            }
        }
    }

    val fontFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val res = FontHelper.importCustomFont(context, uri)
            if (res != null) {
                onUpdateSettings(
                    settings.copy(
                        fontFamilyChoice = "custom",
                        customFontPath = res.first,
                        customFontName = res.second
                    )
                )
                Toast.makeText(context, "Шрифт «${res.second}» успешно загружен!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val liveVideoWallpaperPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = com.example.util.ImageStorageHelper.saveWallpaperFile(context, uri) ?: uri.toString()
            onUpdateSettings(
                settings.copy(
                    liveVideoWallpaperUri = savedPath,
                    liveVideoWallpaperEnabled = true
                )
            )
            Toast.makeText(context, "Живые видео-обои установлены!", Toast.LENGTH_SHORT).show()
        }
    }

    val customAppBackgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = com.example.util.ImageStorageHelper.saveWallpaperFile(context, uri) ?: uri.toString()
            onUpdateSettings(
                settings.copy(
                    appThemeBackgroundPreset = "custom",
                    appCustomBackgroundUri = savedPath
                )
            )
            Toast.makeText(context, "Фон приложения установлен!", Toast.LENGTH_SHORT).show()
        }
    }

    if (showCustomHexDialog) {
        var hexInput by remember {
            mutableStateOf(String.format("#%06X", 0xFFFFFF and settings.accentColorLong.toInt()))
        }
        var parseError by remember { mutableStateOf(false) }

        val parsedColor: Color? = remember(hexInput) {
            try {
                val clean = hexInput.trim().removePrefix("#")
                if (clean.length == 6) {
                    val colorInt = clean.toLong(16)
                    parseError = false
                    Color(0xFF000000 or colorInt)
                } else {
                    parseError = true
                    null
                }
            } catch (_: Exception) {
                parseError = true
                null
            }
        }

        AlertDialog(
            onDismissRequest = { showCustomHexDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Свой HEX цвет", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Введите произвольный шестнадцатеричный код цвета в формате #RRGGBB:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { hexInput = it },
                        label = { Text("HEX код") },
                        placeholder = { Text("#7C3AED") },
                        singleLine = true,
                        isError = parseError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Предпросмотр цвета:", style = MaterialTheme.typography.bodyMedium)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(parsedColor ?: Color.Gray)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }

                    if (parseError) {
                        Text("Пожалуйста, введите корректный 6-значный HEX (например, #9333EA)", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = hexInput.trim().removePrefix("#")
                        try {
                            val colorLong = 0xFF000000L or clean.toLong(16)
                            onUpdateSettings(settings.copy(accentColorLong = colorLong))
                            showCustomHexDialog = false
                        } catch (_: Exception) {}
                    },
                    enabled = !parseError && parsedColor != null
                ) {
                    Text("Применить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomHexDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDefaultPromptDialog) {
        val defaultTemplate = remember {
            com.example.ai.engine.RoleplayPromptBuilder.getDefaultSystemPromptTemplate("{{char}}")
        }

        AlertDialog(
            onDismissRequest = { showDefaultPromptDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Стандартный системный промпт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Этот шаблон используется по умолчанию, когда поле пользовательского промпта пустое. Переменные {{char}} и {{user}} заменяются на имена персонажа и персоны.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 380.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        SelectionContainer {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = defaultTemplate,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Default System Prompt", defaultTemplate)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Стандартный промпт скопирован", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Копировать")
                    }

                    Button(
                        onClick = {
                            onUpdateSettings(settings.copy(customSystemPrompt = defaultTemplate))
                            showDefaultPromptDialog = false
                            Toast.makeText(context, "Вставлено как пользовательский промпт", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Вставить в поле")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDefaultPromptDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (showDownloadPromptConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadPromptConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    "Скачать системный промпт?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Внимание! Текущий системный промпт будет удален и заменен на актуальный системный промпт из официального репозитория GitHub.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Все ваши персональные изменения системного промпта будут перезаписаны!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        "Продолжить загрузку и замену?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadPromptConfirmDialog = false
                        isDownloadingPrompt = true
                        coroutineScope.launch {
                            val result = com.example.util.SystemPromptManager.fetchRemoteSystemPrompt()
                            isDownloadingPrompt = false
                            if (result.isSuccess) {
                                val remote = result.getOrNull()
                                if (remote != null && remote.prompt.isNotBlank()) {
                                    onUpdateSettings(
                                        settings.copy(
                                            customSystemPrompt = remote.prompt,
                                            systemPromptVersion = remote.version,
                                            downloadedSystemPrompt = remote.prompt,
                                            isSystemPromptUserModified = false,
                                            isSystemPromptFirstLaunchDone = true
                                        )
                                    )
                                    Toast.makeText(
                                        context,
                                        "Системный промпт (версия ${remote.version}) успешно установлен!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(context, "Не удалось разобрать системный промпт", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "Ошибка скачивания: ${result.exceptionOrNull()?.localizedMessage ?: "Нет подключения к сети"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Скачать и заменить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadPromptConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showFrameRepositoryDialog) {
        UserFrameRepositoryDialog(
            userAvatarUri = "",
            currentSelectedFrameUri = settings.userActiveFrameUri,
            rawFramesJson = settings.userFramesJson,
            onDismiss = { showFrameRepositoryDialog = false },
            onSelectFrame = { selectedUri ->
                onUpdateSettings(settings.copy(userActiveFrameUri = selectedUri))
                showFrameRepositoryDialog = false
            },
            onSaveFramesJson = { updatedJson ->
                onUpdateSettings(settings.copy(userFramesJson = updatedJson))
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.studioElement("settings_top_bar"),
                title = {
                    Text(
                        text = if (isVaultHub && vaultHubSection != null) {
                            when (vaultHubSection) {
                                0 -> if (isFemboy) "Оформление и Стиль (uwu)" else if (isEn) "Appearance & Design" else "Оформление и Стиль"
                                1 -> if (isFemboy) "Язык интерфейса / Ня~" else if (isEn) "Language & Localization" else "Язык интерфейса"
                                2 -> if (isFemboy) "Обои и видеофончики" else if (isEn) "Wallpapers & Live Video" else "Обои и видеофон"
                                3 -> if (isFemboy) "ИИ-Няшки и Модельки" else if (isEn) "AI Models & Providers" else "ИИ-Провайдеры и Модели"
                                4 -> if (isFemboy) "Поведение и мармеладки" else if (isEn) "Behavior & Tokens" else "Поведение и генерация"
                                5 -> if (isFemboy) "Сохраняшки и Система" else if (isEn) "Backup & System" else "Бэкапы и Система"
                                else -> if (isFemboy) "Настроечки" else if (isEn) "Settings" else "Настройки"
                            }
                        } else {
                            if (isFemboy) "Настроечки приложения (uwu)" else if (isEn) "Application Settings" else "Настройки приложения"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isVaultHub && vaultHubSection != null) {
                            vaultHubSection = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isEn) "Back" else "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isVaultHub && vaultHubSection == null) {
            // NEW UNTITLED HUB SETTINGS LANDING MENU (MATCHING DRAFT SKETCH)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Persona Header Card (Matching top of draft sketch!)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onOpenPersonas() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val defaultPersonaTitle = if (isFemboy) "Твой Фембойчик" else if (isEn) "Your Persona" else "Ваша персона"
                                val fallbackPersonaTitle = if (isFemboy) "Главный Фембойчик" else if (isEn) "Main Persona" else "Основная персона"
                                Text(
                                    text = activePersona?.name?.ifBlank { fallbackPersonaTitle } ?: defaultPersonaTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = if (isFemboy) "🐾 ФЕМБОЙЧИК" else if (isEn) "PERSONA" else "ПЕРСОНА",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            val ageStr = if (activePersona != null && activePersona.age > 0) "${activePersona.age} ${if (isEn) "y/o" else "лет"}" else ""
                            val genderStr = activePersona?.gender?.ifBlank { null } ?: (if (isEn) "Female" else "Женский")
                            val metaStr = listOfNotNull(ageStr.ifBlank { null }, genderStr).joinToString("; ")
                            val descStr = activePersona?.shortDescription?.ifBlank { null } ?: metaStr

                            Text(
                                text = descStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box {
                            AvatarImage(
                                uri = activePersona?.avatarUri ?: "drawable://avatar_app_dark",
                                contentDescription = activePersona?.name ?: "Persona",
                                size = 64.dp,
                                frameUri = activePersona?.avatarFrameUri ?: settings.userActiveFrameUri
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Switch persona",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Category Menu Cards / Buttons (Matching draft sketch list!)
                val hubCategories = listOf(
                    HubCategoryItem(
                        id = 0,
                        title = if (isFemboy) "Оформление и Стиль (uwu)" else if (isEn) "Appearance & Design" else "Оформление и Стиль",
                        subtitle = if (isFemboy) "Палитра, темы, миленькие шрифты и чулочки" else if (isEn) "Themes, accent color, fonts & transparency" else "Палитра, темы, шрифты и прозрачность",
                        icon = Icons.Default.Palette,
                        accentColor = Color(0xFFC084FC)
                    ),
                    HubCategoryItem(
                        id = 1,
                        title = if (isFemboy) "Язык интерфейса / Ня~" else if (isEn) "Language & Localization" else "Язык интерфейса / Language",
                        subtitle = if (isFemboy) "Русский, English, Фембойский (🐾)" else if (isEn) "English / Русский language" else "Выбор языка: Русский / English",
                        icon = Icons.Default.Language,
                        accentColor = Color(0xFF38BDF8)
                    ),
                    HubCategoryItem(
                        id = 2,
                        title = if (isFemboy) "Обои и видеофончики" else if (isEn) "Wallpapers & Live Video" else "Обои и видеофон",
                        subtitle = if (isFemboy) "Фоны чатиков, живые обои и параллакс" else if (isEn) "Chat backgrounds, video wallpapers, parallax" else "Фоны диалогов, живые видео-обои и параллакс",
                        icon = Icons.Default.Wallpaper,
                        accentColor = Color(0xFFF472B6)
                    ),
                    HubCategoryItem(
                        id = 3,
                        title = if (isFemboy) "ИИ-Няшки и Модельки" else if (isEn) "AI Models & Providers" else "ИИ-Провайдеры и Модели",
                        subtitle = if (isFemboy) "Настройка OpenRouter, Gemini, Grok, ключики" else if (isEn) "OpenRouter, Gemini, Grok, BYOK keys" else "Настройка OpenRouter, Gemini, Grok, API ключи",
                        icon = Icons.Default.Psychology,
                        accentColor = Color(0xFF34D399),
                        isExternalNav = true
                    ),
                    HubCategoryItem(
                        id = 4,
                        title = if (isFemboy) "Поведение и мармеладки" else if (isEn) "Behavior & Chat Parameters" else "Поведение и генерация",
                        subtitle = if (isFemboy) "Лимиты токенов, скорость печати, разделители" else if (isEn) "Context limits, typing speed, text alignment" else "Лимиты токенов, скорость печати, разделители",
                        icon = Icons.Default.Forum,
                        accentColor = Color(0xFFF59E0B)
                    ),
                    HubCategoryItem(
                        id = 5,
                        title = if (isFemboy) "Сохраняшки и Система" else if (isEn) "Backup & System" else "Бэкапы и Система",
                        subtitle = if (isFemboy) "Выборочный экспорт ZIP, обновления, промпты" else if (isEn) "ZIP export/import, updates, system prompt" else "Выборочный экспорт ZIP, обновления, промпт",
                        icon = Icons.Default.SettingsBackupRestore,
                        accentColor = Color(0xFF6366F1)
                    )
                )

                hubCategories.forEach { cat ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                if (cat.isExternalNav) {
                                    onOpenProviders()
                                } else {
                                    vaultHubSection = cat.id
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = cat.accentColor.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, cat.accentColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = cat.icon,
                                            contentDescription = null,
                                            tint = cat.accentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = cat.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = cat.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (!isVaultHub) {
                    // Left Navigation Rail / Vertical Tabs for Classic Mode
                    Surface(
                        modifier = Modifier
                            .studioElement("settings_category_nav")
                            .width(82.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            settingsTabs.forEachIndexed { index, tabItem ->
                                val isSelected = if (ultraLazyCategoryMode) selectedCategoryTab == index else (clickedTabOverride ?: activeTabByScroll) == index
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedCategoryTab = index
                                            clickedTabOverride = index
                                            if (ultraLazyCategoryMode) {
                                                coroutineScope.launch {
                                                    listState.scrollToItem(0, 0)
                                                }
                                            } else {
                                                val targetIndex = when (index) {
                                                    0 -> 0
                                                    1 -> wallpapersStartIndex
                                                    2 -> aiModelsStartIndex
                                                    3 -> behaviorStartIndex
                                                    4 -> systemStartIndex
                                                    else -> 0
                                                }
                                                coroutineScope.launch {
                                                    listState.scrollToItem(targetIndex, 0)
                                                }
                                            }
                                        },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            Icon(
                                                imageVector = tabItem.icon,
                                                contentDescription = tabItem.title,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            if (tabItem.badge != null) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.offset(x = 8.dp, y = (-4).dp)
                                                ) {
                                                    Text(
                                                        text = tabItem.badge,
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tabItem.title,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 2,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Persona Header Card
                item(key = "persona_profile_header") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenPersonas() }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                AvatarImage(
                                    uri = activePersona?.avatarUri ?: "drawable://avatar_app_dark",
                                    contentDescription = activePersona?.name ?: "Персона",
                                    size = 56.dp,
                                    frameUri = activePersona?.avatarFrameUri ?: ""
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Сменить персону",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(2.5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val defaultPersonaTitle = if (isEn) "Your Persona" else "Ваша персона"
                                    val fallbackPersonaTitle = if (isEn) "Main Persona" else "Основная персона"
                                    Text(
                                        text = activePersona?.name?.ifBlank { fallbackPersonaTitle } ?: defaultPersonaTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isEn) "PERSONA" else "ПЕРСОНА",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                val defaultGender = if (isEn) "Persona" else "Персона"
                                val defaultAgeUnit = if (isEn) "y/o" else "лет"
                                val defaultHint = if (isEn) "Tap to configure persona profile..." else "Нажмите, чтобы настроить профиль персоны"
                                val personaDesc = activePersona?.shortDescription?.ifBlank { null }
                                    ?: if (activePersona != null && activePersona.age > 0) "${activePersona.gender.ifBlank { defaultGender }}, ${activePersona.age} $defaultAgeUnit"
                                    else defaultHint
                                Text(
                                    text = personaDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Настроить персону",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                val activeCategorySection: Int? = if (isVaultHub) vaultHubSection else (if (settings.settingsCategoryIsolationMode) selectedCategoryTab else null)

                // 0. Language & Interface Variant Section (Оформление & Язык)
                if (activeCategorySection == null || activeCategorySection == 1) {
                item(key = "section_language") {
                    Text(
                        text = "🌐 " + com.example.util.tr("settings_language_section", "Язык интерфейса / Localization"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🌐", fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = com.example.util.tr("settings_language_section", "Язык интерфейса / Localization"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = com.example.util.tr("settings_language_desc", "Выберите язык приложения, импортируйте или экспортируйте JSON перевода"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            val languages = remember(context) { com.example.util.AppStrings.getAvailableLanguages(context) }
                            val currentLang = settings.appLanguage

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                languages.forEach { langInfo ->
                                    val isSelected = currentLang.equals(langInfo.code, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        border = BorderStroke(
                                            if (isSelected) 1.5.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onUpdateSettings(settings.copy(appLanguage = langInfo.code))
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(langInfo.iconEmoji, fontSize = 22.sp)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = langInfo.name,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        if (isSelected) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = MaterialTheme.colorScheme.primary
                                                            ) {
                                                                Text(
                                                                    text = com.example.util.tr("providers_active_badge", "АКТИВЕН"),
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = langInfo.description,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { onUpdateSettings(settings.copy(appLanguage = langInfo.code)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                }

                if (activeCategorySection == null || activeCategorySection == 0) {
                item(key = "section_appearance") {
                    Text(
                        text = if (isEn) "🎨 Appearance & Themes" else "🎨 Оформление",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Dashboard, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isEn) "Interface Layout Variant" else "Вариант интерфейса",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isEn) "Select the main screen layout for the application" else "Выберите компоновку главного экрана приложения",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Variant cards
                            val variants = listOf(
                                Triple("classic", if (isEn) "Classic Messenger" else "Классический мессенджер", if (isEn) "Traditional list view with dialogues, tabs, and categories" else "Традиционный вид со списком диалогов, вкладками и категориями"),
                                Triple("vault_hub", if (isEn) "New Untitled Hub" else "Новый Untitled", if (isEn) "Main showcase screen with character vault from box.json, banners, quick actions, and dialogues" else "Главный экран — витрина персонажей из box.json, баннеры, быстрые действия и диалоги")
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                variants.forEach { (id, title, desc) ->
                                    val isSelected = settings.uiVariant == id || (id == "classic" && settings.uiVariant != "vault_hub")
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onUpdateSettings(settings.copy(uiVariant = id)) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { onUpdateSettings(settings.copy(uiVariant = id)) }
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(desc, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }

                            if (settings.uiVariant == "vault_hub") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    if (isEn) "Default Start Tab on Hub:" else "Основная вкладка Новый Untitled:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val startTabs = listOf(
                                        "general" to if (isEn) "General (Vault)" else "Общее (Хранилище)",
                                        "chats" to if (isEn) "Chats (Dialogues)" else "Чаты (Диалоги)"
                                    )
                                    startTabs.forEach { (tabId, label) ->
                                        val isSelected = settings.vaultHubStartTab == tabId
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onUpdateSettings(settings.copy(vaultHubStartTab = tabId)) },
                                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                        }
                    }
                }

                item(key = "section_appearance_fonts_and_glass") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Typography / Fonts Section
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isEn) "App Fonts:" else "Шрифты приложения:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(AvailableBaseFonts) { fontOpt ->
                                    val isSelected = settings.fontFamilyChoice == fontOpt.id
                                    val fontName = if (isEn) {
                                        when (fontOpt.id) {
                                            "system" -> "System (Default)"
                                            "montserrat" -> "Montserrat (Modern Sans)"
                                            "playfair" -> "Playfair Display (Serif)"
                                            "roboto_mono" -> "Roboto Mono (Monospace)"
                                            "serif" -> "Serif (Classic)"
                                            "sans_serif" -> "Sans-Serif"
                                            else -> fontOpt.nameRu
                                        }
                                    } else fontOpt.nameRu
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onUpdateSettings(settings.copy(fontFamilyChoice = fontOpt.id)) },
                                        label = { Text(fontName, fontSize = 11.sp, fontFamily = fontOpt.fontFamily) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val isCustomFont = settings.fontFamilyChoice == "custom"
                            FilledTonalButton(
                                onClick = { fontFilePicker.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (settings.customFontName.isNotBlank()) {
                                        if (isEn) "Imported: ${settings.customFontName} (Change .TTF)" else "Импортирован: ${settings.customFontName} (Сменить .TTF)"
                                    } else {
                                        if (isEn) "➕ Import Custom Font (.ttf / .otf)" else "➕ Импортировать свой шрифт (.ttf / .otf)"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (settings.customFontName.isNotBlank() && !isCustomFont) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = { onUpdateSettings(settings.copy(fontFamilyChoice = "custom")) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(if (isEn) "Activate ${settings.customFontName}" else "Активировать ${settings.customFontName}", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // 🔮 TRANSPARENCY & GLASSMORPHISM CONTROLS
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isEn) "Interface Transparency" else "Прозрачность интерфейса", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            if (isEn) "Elegant translucency and frosted glass effect for dialogues and panels" else "Элегантная полупрозрачность и матовый эффект для диалогов и панелей",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = settings.glassmorphismEnabled,
                                    onCheckedChange = { onUpdateSettings(settings.copy(glassmorphismEnabled = it)) }
                                )
                            }

                            if (settings.glassmorphismEnabled) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // ✨ REAL-TIME INTERACTIVE TRANSPARENCY LIVE PREVIEW CARD
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF6366F1),
                                                    Color(0xFFA855F7),
                                                    Color(0xFFEC4899),
                                                    Color(0xFF3B82F6)
                                                )
                                            )
                                        )
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LiquidGlassSurface(
                                        userSettings = settings,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                                Text(if (isEn) "✨ Transparency Preview" else "✨ Предпросмотр прозрачности", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (settings.wallpaperTone == "light") Color.Black else Color.White)
                                                Text(if (isEn) "Smooth frosted surface with soft borders" else "Плавная матовая подложка с мягкой гранью", fontSize = 10.5.sp, color = if (settings.wallpaperTone == "light") Color(0xFF334155) else Color(0xFFCBD5E1))
                                            }
                                            LiquidGlassPillButton(
                                                text = if (isEn) "Button Test" else "Тест кнопки",
                                                onClick = {},
                                                userSettings = settings,
                                                fontSize = 10.5.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Transparency Presets
                                Text(if (isEn) "Transparency Presets:" else "Пресеты прозрачности:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val glassPresets = listOf(
                                        (if (isEn) "Minimal" else "Минимальная") to 0.88f,
                                        (if (isEn) "Balanced" else "Сбалансированная") to 0.70f,
                                        (if (isEn) "High" else "Высокая") to 0.50f,
                                        (if (isEn) "Ultra Translucent" else "Ультрапрозрачность") to 0.30f
                                    )
                                    items(glassPresets.size) { idx ->
                                        val (name, value) = glassPresets[idx]
                                        OutlinedButton(
                                            onClick = {
                                                onUpdateSettings(settings.copy(glassAlpha = value))
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(name, fontSize = 10.5.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Slider: Alpha
                                Text(if (isEn) "Transparency level: ${(settings.glassAlpha * 100).toInt()}%" else "Степень прозрачности: ${(settings.glassAlpha * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = settings.glassAlpha,
                                    onValueChange = { onUpdateSettings(settings.copy(glassAlpha = it)) },
                                    valueRange = 0.15f..0.95f
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(if (isEn) "Glass Background Tone:" else "Тон подложки прозрачности:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    FilterChip(
                                        selected = settings.wallpaperTone != "light",
                                        onClick = { onUpdateSettings(settings.copy(wallpaperTone = "dark")) },
                                        label = { Text(if (isEn) "Dark Tint" else "Тёмная подложка", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = settings.wallpaperTone == "light",
                                        onClick = { onUpdateSettings(settings.copy(wallpaperTone = "light")) },
                                        label = { Text(if (isEn) "Light Tint" else "Светлая подложка", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. App Icons in Different Colors
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AppShortcut, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(if (isEn) "App Icon Variants" else "Варианты иконки приложения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(if (isEn) "Choose app icon color and style" else "Выберите цвет и стиль значка приложения", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val iconVariants = listOf(
                                Triple("purple", if (isEn) "Purple Neon" else "Фиолетовый Неон", Color(0xFFC084FC)),
                                Triple("cyan", if (isEn) "Cyber Cyan" else "Кибер Циан", Color(0xFF00F0FF)),
                                Triple("pink", if (isEn) "Sakura Pink" else "Сакура Розовый", Color(0xFFF472B6)),
                                Triple("emerald", if (isEn) "Emerald Jade" else "Изумрудный Нефрит", Color(0xFF34D399)),
                                Triple("crimson", if (isEn) "Royal Ruby" else "Королевский Рубин", Color(0xFFFB7185)),
                                Triple("gold", if (isEn) "Imperial Gold" else "Имперское Золото", Color(0xFFF59E0B)),
                                Triple("dark", if (isEn) "Deep Obsidian" else "Глубокий Обсидиан", Color(0xFF334155)),
                                Triple("arctic", if (isEn) "Arctic Ice" else "Арктический Лёд", Color(0xFF7DD3FC))
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                iconVariants.chunked(2).forEach { rowIcons ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowIcons.forEach { (id, label, col) ->
                                            val isSelected = settings.appIconVariant == id
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                modifier = Modifier.weight(1f).clickable { onUpdateSettings(settings.copy(appIconVariant = id)) }
                                            ) {
                                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(col),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(label, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, modifier = Modifier.weight(1f))
                                                    if (isSelected) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Theme and Background Styling with Video Live Wallpaper
                item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                modifier = Modifier
                                    .studioElement("settings_theme_card")
                                    .fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isEn) "App Appearance & Themes" else "Оформление и фоны приложения",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (isEn) "13 palette styles, gallery backgrounds and live video wallpapers" else "13 стилей палитры, фоны из галереи и живые видео-обои",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Theme cards grid
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        com.example.ui.theme.AvailableThemes.chunked(2).forEach { rowThemes ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                rowThemes.forEach { themeOption ->
                                                    val isSelected = settings.themeMode == themeOption.id
                                                    val themeName = if (isEn) {
                                                        when (themeOption.id) {
                                                            "dark" -> "Dark (Classic)"
                                                            "cyberpunk" -> "Cyberpunk Neon 2077"
                                                            "sakura" -> "Sakura Gradient"
                                                            "ocean" -> "Ocean Abyss"
                                                            "sunset" -> "Warm Sunset"
                                                            "amber" -> "Imperial Gold"
                                                            "purple" -> "Purple Neon"
                                                            "oled" -> "OLED Pure Black"
                                                            "gruvbox" -> "Gruvbox (Retro)"
                                                            "nord" -> "Nord (Arctic)"
                                                            "forest" -> "Emerald Forest"
                                                            "crimson" -> "Wine Velvet"
                                                            "light" -> "Light"
                                                            else -> themeOption.name
                                                        }
                                                    } else themeOption.name
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = themeOption.previewSurface,
                                                        border = BorderStroke(
                                                            width = if (isSelected) 2.5.dp else 1.dp,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                onUpdateSettings(settings.copy(themeMode = themeOption.id))
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(24.dp)
                                                                    .clip(CircleShape)
                                                                    .background(themeOption.previewBg)
                                                                    .border(2.dp, themeOption.previewAccent, CircleShape)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = themeName,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (themeOption.isDark) Color(0xFFEEEEEE) else Color(0xFF222222),
                                                                maxLines = 1,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            if (isSelected) {
                                                                Icon(
                                                                    imageVector = Icons.Default.CheckCircle,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                if (rowThemes.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .studioElement("settings_accent_card")
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isEn) "Accent Color Palette" else "Палитра акцентных цветов", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(if (isEn) "Select UI accent or set custom HEX code" else "Выберите акцент интерфейса или задайте свой HEX код", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    FilledTonalButton(
                                        onClick = { showCustomHexDialog = true },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Colorize, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("HEX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Beautiful grid of color circles with names
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AccentPresets.chunked(4).forEach { rowPresets ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            rowPresets.forEach { (name, colorValue) ->
                                                val isSelected = settings.accentColorLong == colorValue
                                                val localizedName = localizeAccentName(name, isEn)
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .width(76.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { onUpdateSettings(settings.copy(accentColorLong = colorValue)) }
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(colorValue))
                                                            .then(
                                                                if (isSelected) Modifier.border(
                                                                    BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface),
                                                                    CircleShape
                                                                ) else Modifier
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = if (Color(colorValue).red * 0.299 + Color(colorValue).green * 0.587 + Color(colorValue).blue * 0.114 > 0.6) Color.Black else Color.White,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = localizedName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        maxLines = 1,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                            if (rowPresets.size < 4) {
                                                repeat(4 - rowPresets.size) {
                                                    Spacer(modifier = Modifier.width(76.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        item {
                            Text(if (isEn) "Font Size" else "Размер шрифта", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEn) "Message font size: ${settings.fontSizeSp} sp" else "Шрифт сообщений: ${settings.fontSizeSp} sp",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalIconButton(
                                        onClick = { if (settings.fontSizeSp > 12) onUpdateSettings(settings.copy(fontSizeSp = settings.fontSizeSp - 1)) }
                                    ) {
                                        Text("A-", fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalIconButton(
                                        onClick = { if (settings.fontSizeSp < 24) onUpdateSettings(settings.copy(fontSizeSp = settings.fontSizeSp + 1)) }
                                    ) {
                                        Text("A+", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        item {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        item {
                            Column(
                                modifier = Modifier
                                    .studioElement("settings_chat_view_card")
                                    .fillMaxWidth()
                            ) {
                                Text(if (isEn) "Dialogue Style" else "Стиль диалогов", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "bubbles" to (if (isEn) "Message Bubbles" else "Пузыри сообщений"),
                                        "novel" to (if (isEn) "Novel Scenario" else "Литературный сценарий")
                                    ).forEach { (styleKey, label) ->
                                        FilterChip(
                                            modifier = Modifier.weight(1f),
                                            selected = settings.bubbleStyle == styleKey,
                                            onClick = { onUpdateSettings(settings.copy(bubbleStyle = styleKey)) },
                                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isEn) "Chat Avatars" else "Аватарки в чате", fontWeight = FontWeight.Bold)
                                        Text(if (isEn) "Display avatars next to character messages" else "Отображать аватарки возле сообщений персонажа", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = settings.showAvatarInChat,
                                        onCheckedChange = { onUpdateSettings(settings.copy(showAvatarInChat = it)) }
                                    )
                                }
                            }
                        }

                        // Literary (Novel) Mode Customization Section
                        if (settings.bubbleStyle == "novel") {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isEn) "📖 Novel Scenario Settings" else "📖 Литературный сценарий: Настройки",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = if (isEn) "Bot text alignment in chat:" else "Выравнивание текста бота в чате:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "left" to (if (isEn) "⬅️ Left" else "⬅️ Слева"),
                                                "center" to (if (isEn) "↔️ Center" else "↔️ Посередине"),
                                                "right" to (if (isEn) "➡️ Right" else "➡️ Справа")
                                            ).forEach { (alignKey, alignLabel) ->
                                                FilterChip(
                                                    modifier = Modifier.weight(1f),
                                                    selected = settings.novelTextAlignment == alignKey,
                                                    onClick = { onUpdateSettings(settings.copy(novelTextAlignment = alignKey)) },
                                                    label = { Text(alignLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = if (isEn) "Dialogue Separator Line" else "Полосочка-разделитель реплик",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isEn) "Customize dividing line between messages" else "Кастомизация разделительной линии между репликами",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(if (isEn) "Show divider line" else "Отображать полосочку", fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(end = 8.dp))
                                        Switch(
                                            checked = settings.novelDividerEnabled,
                                            onCheckedChange = { onUpdateSettings(settings.copy(novelDividerEnabled = it)) }
                                        )
                                    }

                                    if (settings.novelDividerEnabled) {
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Width %
                                        Text(
                                            text = if (isEn) "Line width: ${settings.novelDividerWidthPercent}%" else "Ширина полосы: ${settings.novelDividerWidthPercent}%",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Slider(
                                            value = settings.novelDividerWidthPercent.toFloat(),
                                            onValueChange = { onUpdateSettings(settings.copy(novelDividerWidthPercent = it.toInt())) },
                                            valueRange = 10f..100f,
                                            steps = 17
                                        )

                                        // Thickness dp
                                        Text(
                                            text = if (isEn) "Line thickness: ${settings.novelDividerThicknessDp} dp" else "Толщина полосы: ${settings.novelDividerThicknessDp} dp",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Slider(
                                            value = settings.novelDividerThicknessDp.toFloat(),
                                            onValueChange = { onUpdateSettings(settings.copy(novelDividerThicknessDp = it.toInt())) },
                                            valueRange = 1f..5f,
                                            steps = 3
                                        )

                                        // Alpha %
                                        Text(
                                            text = if (isEn) "Opacity: ${(settings.novelDividerAlpha * 100).toInt()}%" else "Прозрачность: ${(settings.novelDividerAlpha * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Slider(
                                            value = settings.novelDividerAlpha,
                                            onValueChange = { onUpdateSettings(settings.copy(novelDividerAlpha = it)) },
                                            valueRange = 0.1f..1.0f
                                        )

                                        // Color Presets
                                        Text(if (isEn) "Divider color" else "Цвет полосочки", style = MaterialTheme.typography.labelMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val colorPresets = if (isEn) listOf(
                                            "#FFFFFF" to "White",
                                            "#D0BCFF" to "Accent",
                                            "#FFD54F" to "Gold",
                                            "#B0BEC5" to "Gray",
                                            "#80CBC4" to "Turquoise",
                                            "#F48FB1" to "Pink"
                                        ) else listOf(
                                            "#FFFFFF" to "Белый",
                                            "#D0BCFF" to "Акцент",
                                            "#FFD54F" to "Золотой",
                                            "#B0BEC5" to "Серый",
                                            "#80CBC4" to "Бирюза",
                                            "#F48FB1" to "Розовый"
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(colorPresets.size) { idx ->
                                                val (hex, name) = colorPresets[idx]
                                                val isSelected = settings.novelDividerColorHex.equals(hex, ignoreCase = true)
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { onUpdateSettings(settings.copy(novelDividerColorHex = hex)) },
                                                    label = { Text(name, fontSize = 11.sp) },
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        // Live Mini-Preview of Divider
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val parsedColor = try {
                                                Color(android.graphics.Color.parseColor(settings.novelDividerColorHex))
                                            } catch (_: Exception) { Color.White }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(settings.novelDividerWidthPercent / 100f)
                                                    .height(settings.novelDividerThicknessDp.dp)
                                                    .background(
                                                        color = parsedColor.copy(alpha = settings.novelDividerAlpha),
                                                        shape = RoundedCornerShape(2.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }


                        item {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        item {
                            Text(if (isEn) "Chat Scroll Animation" else "Анимация прокрутки чата", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (isEn) "Auto-scroll effect and animation to new messages" else "Эффект авто-прокрутки и перемещения к новым сообщениям", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            val scrollAnimations = listOf(
                                "spring" to (if (isEn) "🌸 Smooth Spring (Recommended)" else "🌸 Плавная пружина (Spring / Рекомендуется)"),
                                "bounce" to (if (isEn) "⚡ Bouncy Spring" else "⚡ Упругий отскок (Bouncy)"),
                                "cinematic" to (if (isEn) "🎬 Cinematic Glide" else "🎬 Кинематографичная (Cinematic glide)"),
                                "instant" to (if (isEn) "🚀 Instant (No Animation)" else "🚀 Без анимации (Мгновенный переход)")
                            )

                            scrollAnimations.forEach { (key, label) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (settings.chatScrollAnimation == key) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (settings.chatScrollAnimation == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable { onUpdateSettings(settings.copy(chatScrollAnimation = key)) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = settings.chatScrollAnimation == key,
                                            onClick = { onUpdateSettings(settings.copy(chatScrollAnimation = key)) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                    // 5. Interface Animation Settings Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Animation,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEn) "Tab & Chat Transition Animations" else "Анимации переходов между вкладками и чатом",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isEn) "Transition styles between tabs, vault, and entering/exiting chat with preview" else "Стили переходов между вкладками, хранилищем и входом/выходом из чата с предпросмотром",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Interactive Live Preview for Tab Transitions
                                TabTransitionInteractivePreview(
                                    animationStyle = settings.tabSwitchAnimation,
                                    durationMs = settings.tabSwitchDurationMs,
                                    accentColor = Color(settings.accentColorLong)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Duration Slider & Presets
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = if (isEn) "Transition Animation Duration" else "Длительность анимации перехода",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = if (settings.tabSwitchAnimation == "instant") {
                                                        if (isEn) "Delay is disabled in Instant mode" else "В режиме «Мгновенно» задержка отключена"
                                                    } else {
                                                        if (isEn) "Trigger speed for selected effect (80 – 750 ms)" else "Скорость срабатывания выбранного эффекта (80 – 750 мс)"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = if (settings.tabSwitchAnimation == "instant") "0 ms" else "${settings.tabSwitchDurationMs} ms",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Slider(
                                            value = settings.tabSwitchDurationMs.toFloat().coerceIn(80f, 750f),
                                            onValueChange = { newVal ->
                                                val newAnim = if (settings.tabSwitchAnimation == "instant") "smooth_slide" else settings.tabSwitchAnimation
                                                onUpdateSettings(
                                                    settings.copy(
                                                        tabSwitchAnimation = newAnim,
                                                        tabSwitchDurationMs = newVal.toInt()
                                                    )
                                                )
                                            },
                                            valueRange = 80f..750f
                                        )

                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(
                                                listOf(
                                                    (if (isEn) "140 ms (Fast)" else "140 мс (Быстро)") to 140,
                                                    (if (isEn) "260 ms (Standard)" else "260 мс (Стандарт)") to 260,
                                                    (if (isEn) "420 ms (Smooth)" else "420 мс (Плавно)") to 420,
                                                    (if (isEn) "600 ms (Cinema)" else "600 мс (Кино)") to 600
                                                )
                                            ) { (label, ms) ->
                                                val selected = settings.tabSwitchAnimation != "instant" && kotlin.math.abs(settings.tabSwitchDurationMs - ms) <= 20
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = {
                                                        val newAnim = if (settings.tabSwitchAnimation == "instant") "smooth_slide" else settings.tabSwitchAnimation
                                                        onUpdateSettings(
                                                            settings.copy(
                                                                tabSwitchAnimation = newAnim,
                                                                tabSwitchDurationMs = ms
                                                            )
                                                        )
                                                    },
                                                    label = { Text(label, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (isEn) "Tab Transition Animation Variants:" else "Варианты анимаций переключения вкладок:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TabTransitionHelper.availableAnimations.forEach { option ->
                                        val isSelected = settings.tabSwitchAnimation == option.key
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onUpdateSettings(settings.copy(tabSwitchAnimation = option.key)) }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { onUpdateSettings(settings.copy(tabSwitchAnimation = option.key)) }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = option.displayTitle(isEn),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                        ) {
                                                            Text(
                                                                text = option.displayBadge(isEn),
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = option.displaySubtitle(isEn),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (isEn) "Hotbar Glow Animation:" else "Анимация подсветки хотбара:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                listOf(
                                    "spring" to (if (isEn) "🎨 Smooth Spring Effect" else "🎨 Плавный пружинный эффект"),
                                    "instant" to (if (isEn) "⚡ Instant Switch Without Animation" else "⚡ Мгновенное переключение без анимации")
                                ).forEach { (key, label) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onUpdateSettings(settings.copy(hotbarAnimation = key)) }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = settings.hotbarAnimation == key,
                                            onClick = { onUpdateSettings(settings.copy(hotbarAnimation = key)) }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(label, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text(
                                            text = if (isEn) "Show Navigation Bar (Hotbar)" else "Отображать панель навигации (хотбар)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (isEn) "Show or hide bottom floating navigation bar" else "Включить или скрыть нижнюю плавающую панель переключения разделов",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = settings.isHotbarVisible,
                                        onCheckedChange = { onUpdateSettings(settings.copy(isHotbarVisible = it)) }
                                    )
                                }

                                if (settings.verityUnlocked || settings.easterEggCode.trim().removePrefix("#").equals("VERITY", ignoreCase = true)) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(
                                                text = if (isEn) "Verity Emoji Button (😊) in Hotbar" else "Кнопка со смайликом Верити (😊) в хотбаре",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (isEn) "Show or hide quick button to #VERITY game in hotbar" else "Показывать или скрывать кнопку быстрого перехода в игру #VERITY в хотбаре",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = settings.showVerityInHotbar,
                                            onCheckedChange = { show ->
                                                onUpdateSettings(
                                                    settings.copy(
                                                        verityUnlocked = true,
                                                        showVerityInHotbar = show,
                                                        isHotbarVisible = if (show) true else settings.isHotbarVisible
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                        // Adaptive Accent Tinting for Avatars & Banners
                        item(key = "section_appearance_tinting") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                            Text(
                                                text = if (isFemboy) "🎨 Тонирование аватарок под цвет темы" else if (isEn) "Adaptive Accent Tinting" else "Тонирование аватарок под цвет темы",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isFemboy) "Аватарки ботиков, баннеры и рамочки слегка подкрашиваются в цвет выбранной темы (uwu)" else if (isEn) "Slightly tints bot avatars, banners, and frames in your active theme accent color" else "Аватарки ботов, баннеры и рамки слегка подкрашиваются в выбранный акцентный цвет темы",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${(settings.accentTintIntensity * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Slider(
                                        value = settings.accentTintIntensity,
                                        onValueChange = { onUpdateSettings(settings.copy(accentTintIntensity = it)) },
                                        valueRange = 0.0f..0.40f,
                                        steps = 7
                                    )

                                    // Live Interactive Preview of Tinting
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AvatarImage(
                                                uri = activePersona?.avatarUri ?: "drawable://avatar_app_dark",
                                                contentDescription = null,
                                                size = 48.dp,
                                                accentTintIntensity = settings.accentTintIntensity,
                                                accentColor = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = if (isFemboy) "Предпросмотр тонирования (uwu)" else if (isEn) "Tinting Live Preview" else "Предпросмотр тонирования",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (isEn) "Live theme accent overlay effect" else "Наглядный эффект подкраски под тему",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Boykisser Screen Companion Pet
                        item(key = "section_appearance_boykisser") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                                        ) {
                                            Text("🐱", fontSize = 22.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isFemboy) "Питомец Бойкиссер на экране 🐾" else if (isEn) "Boykisser Screen Companion" else "Питомец Бойкиссер на экране",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (isFemboy) "Милый бойкиссер бегает по экрану, не мешая чтению. Тапни, чтобы помурчать! (uwu)" else if (isEn) "A cute Boykisser pet walks along the screen edge without blocking text" else "Милый бойкиссер передвигается по краю экрана, не мешая чтению чатов и кнопкам",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = settings.boykisserPetEnabled,
                                            onCheckedChange = { onUpdateSettings(settings.copy(boykisserPetEnabled = it)) }
                                        )
                                    }

                                    if (settings.boykisserPetEnabled) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Boykisser Variant Switcher: Classic (2D) vs 3D
                                        Text(
                                            text = if (isEn) "Boykisser Style" else "Стиль Бойкиссера",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = settings.boykisserPetVariant.equals("classic", ignoreCase = true),
                                                onClick = { onUpdateSettings(settings.copy(boykisserPetVariant = "classic")) },
                                                label = { Text(if (isFemboy) "2D Классика (>w<)" else if (isEn) "2D Classic" else "2D Классика (>w<)") },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            FilterChip(
                                                selected = settings.boykisserPetVariant.equals("3d", ignoreCase = true),
                                                onClick = { onUpdateSettings(settings.copy(boykisserPetVariant = "3d")) },
                                                label = { Text(if (isFemboy) "3D Фигурка ✨" else if (isEn) "3D Figurine" else "3D Фигурка ✨") },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Boykisser Speed Slider
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isEn) "Walking Speed" else "Скорость передвижения",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${(settings.boykisserPetSpeed * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Slider(
                                            value = settings.boykisserPetSpeed,
                                            onValueChange = { onUpdateSettings(settings.copy(boykisserPetSpeed = it)) },
                                            valueRange = 0.5f..2.5f,
                                            steps = 19
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Behavior on Main Screens (Tabs)
                                        Text(
                                            text = if (isFemboy) "Поведение на главных экранах (Вкладки)" else if (isEn) "Main Screens Behavior" else "Поведение на главных экранах (Вкладки)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = settings.boykisserMainBehavior == "walk",
                                                onClick = { onUpdateSettings(settings.copy(boykisserMainBehavior = "walk")) },
                                                label = { Text(if (isFemboy) "🐾 Ходить" else if (isEn) "🐾 Walk" else "🐾 Ходить") },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            FilterChip(
                                                selected = settings.boykisserMainBehavior == "sit",
                                                onClick = { onUpdateSettings(settings.copy(boykisserMainBehavior = "sit")) },
                                                label = { Text(if (isFemboy) "🧘 Сидеть" else if (isEn) "🧘 Sit" else "🧘 Сидеть") },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Behavior inside Chat
                                        Text(
                                            text = if (isFemboy) "Поведение внутри чатов" else if (isEn) "In-Chat Behavior" else "Поведение внутри чатов",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = settings.boykisserChatBehavior == "walk",
                                                onClick = { onUpdateSettings(settings.copy(boykisserChatBehavior = "walk")) },
                                                label = { Text(if (isFemboy) "🐾 Ходить" else if (isEn) "🐾 Walk" else "🐾 Ходить") },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            FilterChip(
                                                selected = settings.boykisserChatBehavior == "sit",
                                                onClick = { onUpdateSettings(settings.copy(boykisserChatBehavior = "sit")) },
                                                label = { Text(if (isFemboy) "🧘 Сидеть" else if (isEn) "🧘 Sit" else "🧘 Сидеть") },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }

                // Dedicated Wallpapers Management Section (Обои)
                if (activeCategorySection == null || activeCategorySection == 2) {
                        // 1. Live Video Wallpaper (BETA) Switch
                        item(key = "section_wallpapers") {
                            Text(
                                text = if (isEn) "🖼️ Wallpapers & Chat Background" else "🖼️ Обои и фон чата",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (settings.liveVideoWallpaperEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (settings.liveVideoWallpaperEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(if (isEn) "Live Video Wallpaper" else "Живые обои (Видеофон)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
                                                    Text("BETA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                if (isEn) "Allows selecting video files (.mp4 / .webm) as a seamless animated chat background" else "При включении позволяет выбирать видеофайлы (.mp4 / .webm) в качестве бесшовного анимированного фона для диалогов",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = settings.liveVideoWallpaperEnabled,
                                            onCheckedChange = { onUpdateSettings(settings.copy(liveVideoWallpaperEnabled = it)) }
                                        )
                                    }
                                }
                            }
                        }

                        // Adaptive Accent Tinting for Avatars & Banners
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                            Text(
                                                text = if (isFemboy) "🎨 Тонирование аватарок под цвет темы" else if (isEn) "Adaptive Accent Tinting" else "Тонирование аватарок под цвет темы",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isFemboy) "Аватарки ботиков, баннеры и рамочки слегка подкрашиваются в цвет выбранной темы (uwu)" else if (isEn) "Slightly tints bot avatars, banners, and frames in your active theme accent color" else "Аватарки ботов, баннеры и рамки слегка подкрашиваются в выбранный акцентный цвет темы",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${(settings.accentTintIntensity * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Slider(
                                        value = settings.accentTintIntensity,
                                        onValueChange = { onUpdateSettings(settings.copy(accentTintIntensity = it)) },
                                        valueRange = 0.0f..0.40f,
                                        steps = 7
                                    )
                                }
                            }
                        }

                        // Boykisser Screen Companion Pet
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 10.dp)
                                        ) {
                                            Text("🐱", fontSize = 22.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isFemboy) "Питомец Бойкиссер на экране 🐾" else if (isEn) "Boykisser Screen Companion" else "Питомец Бойкиссер на экране",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (isFemboy) "Милый бойкиссер бегает по экрану, не мешая чтению. Тапни, чтобы помурчать! (uwu)" else if (isEn) "A cute Boykisser pet walks along the screen edge without blocking text" else "Милый бойкиссер передвигается по краю экрана, не мешая чтению чатов и кнопкам",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = settings.boykisserPetEnabled,
                                            onCheckedChange = { onUpdateSettings(settings.copy(boykisserPetEnabled = it)) }
                                        )
                                    }

                                    if (settings.boykisserPetEnabled) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Boykisser Variant Switcher: Classic (2D) vs 3D
                                        Text(
                                            text = if (isEn) "Boykisser Style" else "Стиль Бойкиссера",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = settings.boykisserPetVariant.equals("classic", ignoreCase = true),
                                                onClick = { onUpdateSettings(settings.copy(boykisserPetVariant = "classic")) },
                                                label = { Text(if (isFemboy) "2D Классика (>w<)" else if (isEn) "2D Classic" else "2D Классика (>w<)") },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            FilterChip(
                                                selected = settings.boykisserPetVariant.equals("3d", ignoreCase = true),
                                                onClick = { onUpdateSettings(settings.copy(boykisserPetVariant = "3d")) },
                                                label = { Text(if (isFemboy) "3D Фигурка ✨" else if (isEn) "3D Figurine" else "3D Фигурка ✨") },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Boykisser Speed Slider
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isEn) "Walking Speed" else "Скорость передвижения",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${(settings.boykisserPetSpeed * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Slider(
                                            value = settings.boykisserPetSpeed,
                                            onValueChange = { onUpdateSettings(settings.copy(boykisserPetSpeed = it)) },
                                            valueRange = 0.5f..2.5f,
                                            steps = 19
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                if (isEn) "Custom Backgrounds" else "Пользовательские фоны",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    "${customWallpapers.size}",
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (settings.liveVideoWallpaperEnabled) {
                                                if (isEn) "Upload images or videos (.mp4) for chat background" else "Загружайте изображения или видео (.mp4) для фона диалогов"
                                            } else {
                                                if (isEn) "Upload any images, artwork or photos to use as chat background" else "Загружайте любые картинки, арты или фотографии и применяйте их для диалогов"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            customWallpaperPicker.launch(if (settings.liveVideoWallpaperEnabled) "*/*" else "image/*")
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(if (settings.liveVideoWallpaperEnabled) Icons.Default.VideoLibrary else Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            if (settings.liveVideoWallpaperEnabled) {
                                                if (isEn) "+ Photo/Video" else "+ Фото/Видео"
                                            } else {
                                                if (isEn) "+ Upload" else "+ Загрузить"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (customWallpapers.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Wallpaper,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            if (isEn) "No uploaded backgrounds yet" else "У вас пока нет загруженных фонов",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (settings.liveVideoWallpaperEnabled) {
                                                if (isEn) "Tap '+ Photo/Video' above to choose an image or video file from device." else "Нажмите «+ Фото/Видео» выше, чтобы выбрать картинку или видеофайл из устройства."
                                            } else {
                                                if (isEn) "Tap '+ Upload' above to choose an image from device gallery. It will be saved locally." else "Нажмите «+ Загрузить» выше, чтобы выбрать картинку из галереи устройства. Она сохранится локально и не исчезнет."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                Text(if (isEn) "Your Collection of Uploaded Backgrounds:" else "Ваша коллекция загруженных фонов:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(customWallpapers) { wallpaperPath ->
                                        val isSelected = settings.chatWallpaperPreset == "custom" && settings.customWallpaperUri == wallpaperPath
                                        val isVideo = wallpaperPath.contains("video", ignoreCase = true) || wallpaperPath.endsWith(".mp4", ignoreCase = true) || wallpaperPath.endsWith(".webm", ignoreCase = true)

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            border = BorderStroke(2.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            modifier = Modifier
                                                .width(130.dp)
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    onUpdateSettings(
                                                        settings.copy(
                                                            chatWallpaperPreset = "custom",
                                                            customWallpaperUri = wallpaperPath,
                                                            liveVideoWallpaperUri = if (isVideo) wallpaperPath else settings.liveVideoWallpaperUri
                                                        )
                                                    )
                                                }
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                if (isVideo) {
                                                    LiveVideoWallpaperPlayer(
                                                        videoUri = wallpaperPath,
                                                        alpha = 0.9f,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(context)
                                                            .data(wallpaperPath)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = "Custom wallpaper",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                // Top-right selection badge or Video indicator
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    if (isVideo) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color.Black.copy(alpha = 0.7f)
                                                        ) {
                                                            Text(
                                                                if (isEn) "🎬 Video" else "🎬 Видео",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.width(1.dp))
                                                    }

                                                    if (isSelected) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = if (isEn) "Selected" else "Выбрано",
                                                                tint = Color.White,
                                                                modifier = Modifier.padding(4.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Bottom bar with Delete and Select indicator
                                                Surface(
                                                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                                                    color = Color.Black.copy(alpha = 0.75f),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (isSelected) (if (isEn) "Active" else "Активен") else (if (isEn) "Select" else "Выбрать"),
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                com.example.util.ImageStorageHelper.deleteWallpaperFile(wallpaperPath)
                                                                val updated = settings.withRemovedCustomWallpaper(wallpaperPath)
                                                                onUpdateSettings(updated)
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = if (isEn) "Delete background" else "Удалить фон",
                                                                tint = Color(0xFFFF6B6B),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        item {
                            Text(if (isEn) "Built-in Background Presets" else "Встроенные пресеты фонов", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (isEn) "Ready-made atmospheric illustrations and minimalist themes:" else "Готовые атмосферные иллюстрации и минималистичные темы:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(WallpaperPresetsList.filter { !it.isCustom }) { preset ->
                                    val isSelected = settings.chatWallpaperPreset == preset.id
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onUpdateSettings(settings.copy(chatWallpaperPreset = preset.id))
                                            }
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            when {
                                                preset.id == "none" -> {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(MaterialTheme.colorScheme.background),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                                else -> {
                                                    com.example.ui.components.ProceduralWallpaper(
                                                        preset = preset.id,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }

                                            // Label overlay
                                            Surface(
                                                shape = RoundedCornerShape(topStart = 8.dp),
                                                color = Color.Black.copy(alpha = 0.7f),
                                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = preset.displayName(isEn),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        color = Color.White,
                                                        maxLines = 1
                                                    )
                                                }
                                            }

                                            if (isSelected) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Parallax Switch & Alpha Slider Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(if (isEn) "Parallax Effect" else "Параллакс-эффект", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text(
                                                if (isEn) "Background tilts smoothly when device moves via accelerometer, creating depth" else "Фон плавно смещается при наклоне устройства благодаря датчику акселерометра, создавая эффект глубины",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = settings.parallaxEnabled,
                                            onCheckedChange = { onUpdateSettings(settings.copy(parallaxEnabled = it)) }
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.FilterFrames, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(if (isEn) "Frame Parallax Effect" else "Параллакс-эффект рамок", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text(
                                                if (isEn) "Avatar frames shift on device tilt" else "Рамки аватара смещаются при наклоне устройства",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = settings.frameParallaxEnabled,
                                            onCheckedChange = { onUpdateSettings(settings.copy(frameParallaxEnabled = it)) }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(if (isEn) "Frame Repository" else "Хранилище рамок", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Text(
                                                if (isEn) "Browse all frames including Halloween, and download new ones for bots" else "Просмотр всех рамок, включая Halloween, и загрузка новых для ботов",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = { showFrameRepositoryDialog = true },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(if (isEn) "Open" else "Открыть", fontSize = 12.sp)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(if (isEn) "Background Darkening (Opacity)" else "Затемнение фона (Прозрачность)", style = MaterialTheme.typography.bodyMedium)
                                            Text("${(settings.chatWallpaperAlpha * 100).toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Slider(
                                            value = settings.chatWallpaperAlpha,
                                            onValueChange = { onUpdateSettings(settings.copy(chatWallpaperAlpha = it)) },
                                            valueRange = 0.1f..0.85f
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Live Interactive Preview Box
                            Text(if (isEn) "Live Chat Preview" else "Живой предпросмотр чата", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (isEn) "Tilt your phone to appreciate parallax and message contrast:" else "Наклоните телефон, чтобы оценить параллакс и контрастность реплик:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Parallax / Video Wallpaper
                                    ParallaxChatWallpaper(
                                        preset = settings.chatWallpaperPreset,
                                        customUri = settings.customWallpaperUri,
                                        alpha = settings.chatWallpaperAlpha,
                                        parallaxEnabled = settings.parallaxEnabled && !isScrolling,
                                        liveVideoWallpaperEnabled = settings.liveVideoWallpaperEnabled && !isScrolling,
                                        liveVideoWallpaperUri = settings.liveVideoWallpaperUri.ifBlank { settings.customWallpaperUri }
                                    )

                                    // Sample chat messages
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Character bubble
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AppLogo(size = 28.dp, shapeRadius = 14.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 2.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                            ) {
                                                Text(
                                                    text = if (isEn) "*smiled softly* The atmosphere here is truly special..." else "*тихо улыбнулась* Атмосфера здесь действительно особенная...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }

                                        // User bubble
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 2.dp),
                                                color = Color(settings.accentColorLong).copy(alpha = 0.35f),
                                                border = BorderStroke(1.dp, Color(settings.accentColorLong).copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = if (isEn) "Yes, the new colors and background look great!" else "Да, новые цвета и фон отлично смотрятся!",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Halloween Pumpkin Rain Drop Height Setting with Interactive Preview
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Text("🎃", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    if (isEn) "Pumpkin Rain Settings (Halloween)" else "Настройки дождя тыковок (Halloween)",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    if (isEn) "Drop height and transparency of falling pumpkins on screen" else "Высота падения и прозрачность падающих тыковок на экране",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    "↕ ${(settings.pumpkinRainDropLimit * 100).toInt()}%",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    "👁 ${(settings.pumpkinRainAlpha * 100).toInt()}%",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    // Interactive Mini Phone Screen Preview
                                    Text(
                                        if (isEn) "Interactive falling & opacity preview:" else "Интерактивный предпросмотр падения и прозрачности:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    ) {
                                        // Miniature falling pumpkins preview (paused during scroll for smooth 60fps list flinging)
                                        HalloweenPumpkinRainOverlay(
                                            isActive = !isScrolling,
                                            onBlackPumpkinTapped = {},
                                            dropLimit = settings.pumpkinRainDropLimit,
                                            pumpkinAlpha = settings.pumpkinRainAlpha,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Cutoff boundary line indicator
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(settings.pumpkinRainDropLimit)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .height(2.dp)
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            listOf(
                                                                Color.Transparent,
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                MaterialTheme.colorScheme.primary,
                                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                Color.Transparent
                                                            )
                                                        )
                                                    )
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .offset(y = (-4).dp)
                                            ) {
                                                Text(
                                                    text = if (isEn) "Despawn limit ${(settings.pumpkinRainDropLimit * 100).toInt()}% • Visibility ${(settings.pumpkinRainAlpha * 100).toInt()}%" else "Лимит исчезновения ${(settings.pumpkinRainDropLimit * 100).toInt()}% • Видимость ${(settings.pumpkinRainAlpha * 100).toInt()}%",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Drop Height Slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(if (isEn) "Pumpkin Drop Height" else "Высота падения тыковок", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text(if (isEn) "${(settings.pumpkinRainDropLimit * 100).toInt()}% of screen" else "${(settings.pumpkinRainDropLimit * 100).toInt()}% экрана", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = settings.pumpkinRainDropLimit,
                                            onValueChange = { onUpdateSettings(settings.copy(pumpkinRainDropLimit = it)) },
                                            valueRange = 0.15f..1.0f
                                        )
                                    }

                                    // Drop Height Presets
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(
                                            listOf(
                                                (if (isEn) "35% (Top)" else "35% (Верх)") to 0.35f,
                                                (if (isEn) "55% (Middle)" else "55% (Середина)") to 0.55f,
                                                (if (isEn) "70% (Above Chat)" else "70% (Над чатом)") to 0.70f,
                                                (if (isEn) "100% (Screen)" else "100% (Экран)") to 1.0f
                                            )
                                        ) { (label, value) ->
                                            val isSelected = kotlin.math.abs(settings.pumpkinRainDropLimit - value) < 0.04f
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onUpdateSettings(settings.copy(pumpkinRainDropLimit = value)) },
                                                label = { Text(label, fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))

                                    // Pumpkin Opacity Slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(if (isEn) "Pumpkin Opacity (Visibility)" else "Прозрачность тыковок (Видимость)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                            Text("${(settings.pumpkinRainAlpha * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = settings.pumpkinRainAlpha,
                                            onValueChange = { onUpdateSettings(settings.copy(pumpkinRainAlpha = it)) },
                                            valueRange = 0.10f..1.0f
                                        )
                                    }

                                    // Pumpkin Opacity Presets
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(
                                            listOf(
                                                (if (isEn) "20% (Background)" else "20% (Фон)") to 0.20f,
                                                (if (isEn) "45% (Standard)" else "45% (Стандарт)") to 0.45f,
                                                (if (isEn) "75% (Bright)" else "75% (Ярко)") to 0.75f,
                                                (if (isEn) "100% (Max)" else "100% (Макс)") to 1.0f
                                            )
                                        ) { (label, value) ->
                                            val isSelected = kotlin.math.abs(settings.pumpkinRainAlpha - value) < 0.04f
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onUpdateSettings(settings.copy(pumpkinRainAlpha = value)) },
                                                label = { Text(label, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }

                // AI & Memory Section (ИИ & Модели)
                if (activeCategorySection == null || activeCategorySection == 3) {
                        item(key = "section_ai_models") {
                            Text(
                                text = if (isEn) "🤖 AI & Models" else "🤖 ИИ & Модели",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Button(
                                onClick = onOpenProviders,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Cloud, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isEn) "Configure AI Providers (OpenRouter / DeepSeek / Gemini)" else "Настроить AI-провайдеры (OpenRouter / DeepSeek / Gemini)")
                            }
                        }

                        // Gemini Model Safety Threshold Slider (Shown if main provider is Google Gemini)
                        if (isGeminiMain) {
                            item(key = "gemini_safety_threshold") {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isEn) "Gemini Safety Threshold" else "Порог безопасности Gemini",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "PROHIBITED_CONTENT FIX",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (isEn) "Controls safetySettings API filters (Harassment, Hate Speech, Sexually Explicit, Dangerous Content). Set to 'Disabled (BLOCK_NONE)' to prevent empty responses caused by PROHIBITED_CONTENT blocks." else "Управляет фильтрацией safetySettings API (Harassment, Hate Speech, Sexually Explicit, Dangerous Content). Установите 'Отключено (BLOCK_NONE)', чтобы исключить пустые ответы из-за блокировки PROHIBITED_CONTENT.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        val safetyLevels = listOf(
                                            "BLOCK_NONE" to ((if (isEn) "Disabled (BLOCK_NONE)" else "Отключено (BLOCK_NONE)") to (if (isEn) "Full roleplay freedom, censorship removed" else "Полная свобода ролплея, блокировки сняты")),
                                            "BLOCK_ONLY_HIGH" to ((if (isEn) "Minimal (BLOCK_ONLY_HIGH)" else "Минимальный (BLOCK_ONLY_HIGH)") to (if (isEn) "Block extreme risk only" else "Блокировать только экстремальный риск")),
                                            "BLOCK_MEDIUM_AND_ABOVE" to ((if (isEn) "Standard (BLOCK_MEDIUM_AND_ABOVE)" else "Стандартный (BLOCK_MEDIUM_AND_ABOVE)") to (if (isEn) "Block medium and high risk" else "Блокировать средний и высокий риск")),
                                            "BLOCK_LOW_AND_ABOVE" to ((if (isEn) "Strict (BLOCK_LOW_AND_ABOVE)" else "Строгий (BLOCK_LOW_AND_ABOVE)") to (if (isEn) "Block most controversial themes" else "Блокировать большинство спорных тем"))
                                        )
                                        val currentIdx = safetyLevels.indexOfFirst { it.first == settings.geminiSafetyThreshold }.coerceAtLeast(0)

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = safetyLevels[currentIdx].second.first,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = safetyLevels[currentIdx].second.second,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Slider(
                                            value = currentIdx.toFloat(),
                                            onValueChange = { newVal ->
                                                val newIdx = kotlin.math.round(newVal).toInt().coerceIn(0, 3)
                                                val newThreshold = safetyLevels[newIdx].first
                                                if (newThreshold != settings.geminiSafetyThreshold) {
                                                    onUpdateSettings(settings.copy(geminiSafetyThreshold = newThreshold))
                                                }
                                            },
                                            valueRange = 0f..3f,
                                            steps = 2,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(if (isEn) "Disabled" else "Отключено", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            Text(if (isEn) "Minimal" else "Минимум", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(if (isEn) "Standard" else "Стандарт", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(if (isEn) "Strict" else "Строгий", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        // Custom Global System Prompt
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .studioElement("settings_generation_card")
                                    .fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            if (isEn) "Custom Global System Prompt" else "Пользовательский системный промпт",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            if (isEn) "Additional global directives for all bots (or replace system prompt style)" else "Дополнительные общие директивы для всех ботов (или замена системного стиля)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Small download button proposing to download the system prompt with warning
                                        FilledTonalButton(
                                            onClick = { showDownloadPromptConfirmDialog = true },
                                            enabled = !isDownloadingPrompt,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            if (isDownloadingPrompt) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.CloudDownload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isEn) "Download" else "Скачать", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        if (settings.customSystemPrompt.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            TextButton(
                                                onClick = {
                                                    onUpdateSettings(
                                                        settings.copy(
                                                            customSystemPrompt = "",
                                                            systemPromptVersion = "",
                                                            downloadedSystemPrompt = "",
                                                            isSystemPromptUserModified = false
                                                        )
                                                    )
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(if (isEn) "Reset" else "Сбросить", fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Display version above system prompt ONLY if not modified by user
                                    if (settings.isSystemPromptUnmodified && settings.systemPromptVersion.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Версия системного промпта: ${settings.systemPromptVersion}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }

                                    var customPromptInput by remember(settings.customSystemPrompt) {
                                        mutableStateOf(settings.customSystemPrompt)
                                    }

                                    OutlinedTextField(
                                        value = customPromptInput,
                                        onValueChange = { newValue ->
                                            customPromptInput = newValue
                                            val isModified = settings.downloadedSystemPrompt.isNotBlank() &&
                                                newValue.trim() != settings.downloadedSystemPrompt.trim()
                                            onUpdateSettings(
                                                settings.copy(
                                                    customSystemPrompt = newValue,
                                                    isSystemPromptUserModified = isModified
                                                )
                                            )
                                        },
                                        placeholder = {
                                            Text(if (isEn) "Leave empty to use standard high-quality engine prompt..." else "Оставьте пустым для использования стандартного качественного промпта движка...")
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 100.dp, max = 220.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedButton(
                                        onClick = { showDefaultPromptDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isEn) "View Default System Prompt" else "Просмотреть стандартный системный промпт")
                                    }

                                    if (settings.customSystemPrompt.isBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            if (isEn) "Using standard engine system prompt with support for natural thoughts, dialogue, and emotional depth." else "Используется стандартный системный промпт движка с поддержкой естественных мыслей, диалогов и эмоционального погружения.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(if (isEn) "System Context Length (Messages)" else "Длина системного контекста (Сообщений)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(if (isEn) "Recent message history sent to model: ${settings.contextWindowSize}" else "Сколько последних реплик передаётся модели в промпте: ${settings.contextWindowSize}", style = MaterialTheme.typography.bodyMedium)
                            Slider(
                                value = settings.contextWindowSize.toFloat(),
                                onValueChange = { onUpdateSettings(settings.copy(contextWindowSize = it.toInt())) },
                                valueRange = 4f..30f,
                                steps = 25
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isEn) "Character Card Length Limit" else "Лимит карточки персонажа", fontWeight = FontWeight.Bold)
                                    Text(if (isEn) "Extended bot description mode (up to 50,000 chars)" else "Режим расширенного описания бота (до 50000 символов)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = settings.characterLimitMode == "extended",
                                    onCheckedChange = {
                                        onUpdateSettings(settings.copy(characterLimitMode = if (it) "extended" else "standard"))
                                    }
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isEn) "Internal Reasoning Debug (<thought>)" else "Отладка внутреннего рассуждения", fontWeight = FontWeight.Bold)
                                    Text(if (isEn) "Display hidden thought tags <thought> below responses" else "Отображать теги скрытых мыслей <thought> под репликами", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = settings.showInternalReasoningDebug,
                                    onCheckedChange = { onUpdateSettings(settings.copy(showInternalReasoningDebug = it)) }
                                )
                            }
                        }
                }

                // 4. Поведение и длина ответов (Behavior & Tokens)
                if (activeCategorySection == null || activeCategorySection == 4) {
                        item(key = "section_behavior") {
                            Text(
                                text = if (isEn) "⚡ Behavior" else "⚡ Поведение",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isEn) "Global Max Output Tokens Limit" else "Глобальный лимит токенов на ответ ИИ",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (isEn) "Universal setting for all chats, characters and models: ${settings.defaultMaxTokens} tokens (~${(settings.defaultMaxTokens * 0.75).toInt()} words)" else "Единая настройка для всех чатов, персонажей и моделей: ${settings.defaultMaxTokens} токенов (~${(settings.defaultMaxTokens * 0.75).toInt()} слов)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Slider(
                                        value = settings.defaultMaxTokens.toFloat().coerceIn(100f, 8192f),
                                        onValueChange = { onUpdateSettings(settings.copy(defaultMaxTokens = it.toInt())) },
                                        valueRange = 100f..8192f,
                                        steps = 80
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = if (isEn) "Quick preset selection:" else "Быстрый выбор пресетов:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(listOf(250, 500, 1000, 1500, 2048, 4096, 8192)) { preset ->
                                            FilterChip(
                                                selected = settings.defaultMaxTokens == preset,
                                                onClick = { onUpdateSettings(settings.copy(defaultMaxTokens = preset)) },
                                                label = { Text("$preset", fontSize = 11.sp) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isEn) "With a low token limit, the model generates concise, punchy messages while ensuring logical completion of all sentences without cutting off mid-text." else "При низком лимите токенов модель генерирует короткие, ёмкие посты с обязательным логическим завершением всех фраз и предложений, без обрыва текста на середине.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (isEn) "Bot Text Appearance Mode" else "Режим появления текста бота",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        if (isEn) "Choose between character typing effect and instant output" else "Выбор между эффектом печати символов и мгновенным выводом ответа",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = settings.botTextAppearanceMode == "typing",
                                            onClick = { onUpdateSettings(settings.copy(botTextAppearanceMode = "typing")) },
                                            label = { Text(if (isEn) "⌨️ Typing" else "⌨️ Печатание", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = settings.botTextAppearanceMode == "instant",
                                            onClick = { onUpdateSettings(settings.copy(botTextAppearanceMode = "instant")) },
                                            label = { Text(if (isEn) "⚡ Instant" else "⚡ Мгновенно", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (settings.botTextAppearanceMode == "typing") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (isEn) "Typing speed: ${settings.botTypingSpeedMs} ms/char" else "Скорость печатания: ${settings.botTypingSpeedMs} мс/символ",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Slider(
                                            value = settings.botTypingSpeedMs.toFloat().coerceIn(1f, 100f),
                                            onValueChange = { onUpdateSettings(settings.copy(botTypingSpeedMs = it.toInt())) },
                                            valueRange = 1f..100f,
                                            steps = 20
                                        )
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(listOf(
                                                5 to (if (isEn) "Fast (5ms)" else "Быстро (5мс)"),
                                                15 to (if (isEn) "Standard (15ms)" else "Стандарт (15мс)"),
                                                30 to (if (isEn) "Slow (30ms)" else "Медленно (30мс)"),
                                                60 to (if (isEn) "Smooth (60ms)" else "Плавно (60мс)")
                                            )) { (preset, label) ->
                                                FilterChip(
                                                    selected = settings.botTypingSpeedMs == preset,
                                                    onClick = { onUpdateSettings(settings.copy(botTypingSpeedMs = preset)) },
                                                    label = { Text(label, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FastForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isEn) "Continue Story on Empty 'Send'" else "Продолжение по кнопке «Отправить»", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(if (isEn) "If input field is empty, tapping ⏩ sends an invisible command to continue its thought or story." else "Если поле ввода пустое, нажатие на кнопку ⏩ отправляет невидимую команду боту продолжать свою мысль или историю.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isEn) "Background Generation & Service" else "Генерация в фоне и служба", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Text(if (isEn) "BACKGROUND" else "ФОН", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.tertiary)
                                        }
                                    }
                                    Text(if (isEn) "Continue generation when app is minimized or screen is off, sending a notification when ready" else "Продолжать генерацию при свёрнутом приложении или выключенном экране с присылкой уведомления о готовности", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = settings.backgroundNotificationsEnabled,
                                    onCheckedChange = { onUpdateSettings(settings.copy(backgroundNotificationsEnabled = it)) }
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isEn) "Global NSFW 18+" else "Глобальный NSFW 18+", fontWeight = FontWeight.Bold)
                                    Text(if (isEn) "Automatically enable 18+ for mature characters" else "Автоматически включать 18+ для взрослых персонажей", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = settings.globalDefaultNsfw,
                                    onCheckedChange = { onUpdateSettings(settings.copy(globalDefaultNsfw = it)) }
                                )
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .studioElement("settings_tts_card")
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(if (isEn) "Voice Messages" else "Голосовые сообщения", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (isEn) "Record voice messages with speech recognition. Bot receives transcript and voice player is active in chat" else "Запись голосовых реплик с распознаванием речи. Бот получает распознанный текст, а вам доступен плеер записи в чате",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = settings.voiceMessagesEnabled,
                                        onCheckedChange = { onUpdateSettings(settings.copy(voiceMessagesEnabled = it)) }
                                    )
                                }
                            }
                        }

                        // Режим отображения отношений: Доска отношений (БЕТА) или классическая Таблица отношений
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Hub,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isEn) "Relationship View Mode in History" else "Отображение отношений в истории",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "BETA",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isEn) "Choose between interactive Relationship Board and classic Relationship Table." else "Выберите между новой интерактивной «Доской отношений» (по умолчанию) и классической «Таблицей отношений».",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isBoardSelected = settings.relationshipViewMode != "table"
                                        FilterChip(
                                            selected = isBoardSelected,
                                            onClick = { onUpdateSettings(settings.copy(relationshipViewMode = "board")) },
                                            label = { Text(if (isEn) "🕸 Relationship Board" else "🕸 Доска отношений", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = !isBoardSelected,
                                            onClick = { onUpdateSettings(settings.copy(relationshipViewMode = "table")) },
                                            label = { Text(if (isEn) "📊 Relationship Table" else "📊 Таблица отношений", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Пасхалка / Секретный код
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .studioElement("settings_easter_egg_card")
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CardGiftcard,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isEn) "Easter Egg / Secret Code" else "Пасхалка / Секретный код",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        text = if (isEn) "Secret code or easter egg input field (saved in settings)" else "Поле ввода секретного кода или пасхалки (сохраняется в настройках)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    var easterEggInput by remember(settings.easterEggCode) {
                                        mutableStateOf(settings.easterEggCode)
                                    }

                                    OutlinedTextField(
                                        value = easterEggInput,
                                        onValueChange = { newCode ->
                                            easterEggInput = newCode
                                            val isVerityMatch = newCode.trim().removePrefix("#").equals("VERITY", ignoreCase = true)
                                            val wasVerityMatch = settings.easterEggCode.trim().removePrefix("#").equals("VERITY", ignoreCase = true)

                                            val isSkibidiMatch = newCode.trim().removePrefix("#").equals("SKIBIDILEAVE", ignoreCase = true)
                                            val wasSkibidiMatch = settings.easterEggCode.trim().removePrefix("#").equals("SKIBIDILEAVE", ignoreCase = true)

                                            if (isVerityMatch) {
                                                onUpdateSettings(
                                                    settings.copy(
                                                        easterEggCode = newCode,
                                                        verityUnlocked = true,
                                                        showVerityInHotbar = true,
                                                        isHotbarVisible = true
                                                    )
                                                )
                                                if (!wasVerityMatch) {
                                                    launchVerityGame()
                                                }
                                            } else if (isSkibidiMatch) {
                                                onUpdateSettings(
                                                    settings.copy(
                                                        easterEggCode = newCode,
                                                        skibidiUnlocked = true
                                                    )
                                                )
                                                if (!wasSkibidiMatch) {
                                                    launchSkibidiGame()
                                                }
                                            } else {
                                                onUpdateSettings(settings.copy(easterEggCode = newCode))
                                            }
                                        },
                                        label = { Text(if (isEn) "Easter Egg" else "Пасхалка") },
                                        placeholder = { Text(if (isEn) "Enter secret code..." else "Введите секретный код...") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Celebration,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingIcon = if (easterEggInput.isNotBlank()) {
                                            {
                                                IconButton(onClick = {
                                                    easterEggInput = ""
                                                    onUpdateSettings(settings.copy(easterEggCode = ""))
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Clear,
                                                        contentDescription = if (isEn) "Clear" else "Очистить"
                                                    )
                                                }
                                            }
                                        } else null,
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    val isVerityUnlockedNow = settings.verityUnlocked || settings.easterEggCode.trim().removePrefix("#").equals("VERITY", ignoreCase = true)
                                    if (isVerityUnlockedNow) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFEAB308).copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, Color(0xFFEAB308).copy(alpha = 0.45f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = if (isEn) "🎉 Easter Egg #VERITY Unlocked!" else "🎉 Пасхалка #VERITY открыта!",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFFFACC15)
                                                        )
                                                        Text(
                                                            text = if (isEn) "Current Form: ${VerityStage.fromScore(settings.verityFormScore).title} (${settings.verityFormScore})" else "Текущая форма: ${VerityStage.fromScore(settings.verityFormScore).title} (${settings.verityFormScore})",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { launchVerityGame() },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFFEAB308),
                                                            contentColor = Color.Black
                                                        ),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(if (isEn) "😊 Play" else "😊 Играть", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                                    }
                                                }

                                                HorizontalDivider(color = Color(0xFFEAB308).copy(alpha = 0.25f))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                                        Text(
                                                            text = if (isEn) "Show 😊 button in hotbar" else "Показывать кнопку 😊 в хотбаре",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = if (isEn) "If turned off, emoji button in hotbar will be hidden" else "Если выключить, кнопка со смайликом в хотбаре будет скрыта",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Switch(
                                                        checked = settings.showVerityInHotbar,
                                                        onCheckedChange = { show ->
                                                            onUpdateSettings(
                                                                settings.copy(
                                                                    verityUnlocked = true,
                                                                    showVerityInHotbar = show,
                                                                    isHotbarVisible = if (show) true else settings.isHotbarVisible
                                                                )
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val isSkibidiUnlockedNow = settings.skibidiUnlocked || settings.easterEggCode.trim().removePrefix("#").equals("SKIBIDILEAVE", ignoreCase = true)
                                    if (isSkibidiUnlockedNow) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFFEF4444).copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = if (isEn) "🚽 Easter Egg SKIBIDILEAVE Unlocked!" else "🚽 Пасхалка SKIBIDILEAVE открыта!",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFFEF4444)
                                                        )
                                                        Text(
                                                            text = if (isEn) "Defeat Skibidi Toilets in 3D Forest and save Agents 📹" else "Победи Скибиди Туалетов в 3D Лесу и спаси Агентов 📹",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Button(
                                                        onClick = { launchSkibidiGame() },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFFEF4444),
                                                            contentColor = Color.White
                                                        ),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                                    ) {
                                                        Text(if (isEn) "🚽 Play 3D" else "🚽 Играть 3D", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }

                // 5. О приложении & Резервные копии (System)
                if (activeCategorySection == null || activeCategorySection == 5) {
                        item(key = "section_system") {
                            Text(
                                text = if (isEn) "🛠️ System" else "🛠️ Система",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AppLogo(size = 64.dp, shapeRadius = 32.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Untitled", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Text(if (isEn) "Version 1.0-alpha • Free Roleplay Engine" else "Версия 1.0-alpha • Свободный ролевой движок", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isEn) "Powerful, independent roleplay engine for communicating with your favorite characters. Uncensored, no strict limits, supporting open AI providers and local data privacy." else "Мощный, независимый ролплей-движок для общения с любимыми персонажами. Без цензуры, без жестких лимитов, с поддержкой открытых ИИ-провайдеров и локальной конфиденциальностью данных.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Remote Update Status & Config Card
                        item {
                            var isCheckingUpdate by remember { mutableStateOf(false) }
                            var updateResultText by remember { mutableStateOf<String?>(if (isEn) "Version ${com.example.util.RemoteUpdateChecker.CURRENT_APP_VERSION} (Current)" else "Версия ${com.example.util.RemoteUpdateChecker.CURRENT_APP_VERSION} (Актуальная)") }
                            var isNewerAvailable by remember { mutableStateOf(false) }
                            val coroutineScope = rememberCoroutineScope()

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.CloudSync,
                                                contentDescription = null,
                                                tint = if (isNewerAvailable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (isEn) "Remote Updates" else "Дистанционные обновления",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = updateResultText ?: "Версия ${com.example.util.RemoteUpdateChecker.CURRENT_APP_VERSION}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isNewerAvailable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                isCheckingUpdate = true
                                                coroutineScope.launch {
                                                    val result = com.example.util.RemoteUpdateChecker.checkForUpdates()
                                                    isCheckingUpdate = false
                                                    if (result.isSuccess) {
                                                        val manifest = result.getOrNull()
                                                        if (manifest?.hasUpdate == true) {
                                                            isNewerAvailable = true
                                                            updateResultText = if (isEn) "🚀 New version available: v${manifest.latestVersion}!" else "🚀 Есть новая версия: v${manifest.latestVersion}!"
                                                        } else {
                                                            isNewerAvailable = false
                                                            updateResultText = if (isEn) "✅ Latest version v${com.example.util.RemoteUpdateChecker.CURRENT_APP_VERSION}" else "✅ Актуальная версия v${com.example.util.RemoteUpdateChecker.CURRENT_APP_VERSION}"
                                                        }
                                                    } else {
                                                        isNewerAvailable = false
                                                        updateResultText = if (isEn) "ℹ️ Latest version v${com.example.util.RemoteUpdateChecker.CURRENT_APP_VERSION} (Server did not respond)" else "ℹ️ Актуальная версия v${com.example.util.RemoteUpdateChecker.CURRENT_APP_VERSION} (Сервер не ответил)"
                                                    }
                                                }
                                            },
                                            enabled = !isCheckingUpdate,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            if (isCheckingUpdate) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                            } else {
                                                Text(if (isEn) "Check" else "Проверить", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = onOpenBackup,
                                modifier = Modifier
                                    .studioElement("settings_backup_card")
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isEn) "Backups & ZIP Export" else "Резервные копии и экспорт ZIP")
                            }
                        }

                        // Telegram Channel Card
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        try {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                Uri.parse("https://t.me/UntitledAI")
                                            )
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            // fallback
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF229ED9),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = "Telegram",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isEn) "Engine Telegram Channel" else "Telegram-канал движка",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isEn) "@UntitledAI • News, updates, and support" else "@UntitledAI • Новости, обновления и поддержка",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Icon(
                                         imageVector = Icons.Default.OpenInNew,
                                         contentDescription = if (isEn) "Open" else "Открыть",
                                         tint = MaterialTheme.colorScheme.primary,
                                         modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Features List
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(if (isEn) "Application Features:" else "Особенности приложения:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    
                                    val features = if (isEn) listOf(
                                        "⚡ Open access to OpenRouter, Polza.ai, Groq, and any compatible endpoints",
                                        "🔞 Full support for 18+ roleplay without topic restrictions",
                                        "🎭 User Personas: choose your identity for each dialogue",
                                        "🧠 Built-in autonomous Untitled Assistant for help and debugging",
                                        "🔀 Dialogue branching and alternative story paths",
                                        "🖼️ Custom wallpapers, parallax effect and color themes",
                                        "📦 Full backup and character export to JSON"
                                    ) else listOf(
                                        "⚡ Открытый доступ к OpenRouter, Polza.ai, Groq и любым совместимым эндпоинтам",
                                        "🔞 Полная поддержка 18+ ролплея и отсутствие ограничений тем",
                                        "🎭 Персоны пользователя: выбирайте от чьего лица ведете диалог",
                                        "🧠 Автономный встроенный Untitled Ассистент для помощи и отладки",
                                        "🔀 Ветвление диалогов и возможность альтернативных сюжетов",
                                        "🖼️ Кастомизация фонов, параллакс-эффект и цветовая палитра",
                                        "📦 Полное резервное копирование и экспорт персонажей в JSON"
                                    )

                                    features.forEach { feat ->
                                        Text(
                                            text = feat,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}
}
