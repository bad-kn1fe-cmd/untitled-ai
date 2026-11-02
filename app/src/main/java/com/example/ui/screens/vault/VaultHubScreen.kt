package com.example.ui.screens.vault

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.data.datastore.CustomChatFolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.datastore.UserSettings
import com.example.data.local.entity.AiProviderProfileEntity
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.RelationshipTableEntity
import com.example.data.local.entity.StoryEntity
import com.example.data.local.entity.UserPersonaEntity
import com.example.data.repository.StoreBotItem
import com.example.ui.components.AppAsyncImage
import com.example.ui.components.AvatarImage
import com.example.ui.components.FontPickerDialog
import com.example.ui.screens.characters.ChatSelectionDialog
import com.example.ui.viewmodels.MainViewModel
import com.example.ui.viewmodels.MainViewModel.StoreUiState
import kotlinx.coroutines.launch
import org.json.JSONArray

data class VaultBannerItem(
    val title: String,
    val subtitle: String,
    val buttonText: String,
    val icon: ImageVector,
    val badge: String,
    val gradientColors: List<Color>,
    val onClick: () -> Unit
)

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val containerColor: Color,
    val iconTint: Color,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHubScreen(
    characters: List<CharacterEntity>,
    providers: List<AiProviderProfileEntity>,
    stories: List<StoryEntity>,
    chats: List<ChatEntity>,
    latestRelationshipsByChat: Map<Long, RelationshipTableEntity> = emptyMap(),
    activePersona: UserPersonaEntity?,
    userSettings: UserSettings,
    storeState: StoreUiState,
    onRefreshStore: () -> Unit,
    onDownloadStoreBot: (StoreBotItem, (CharacterEntity) -> Unit, (String) -> Unit) -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onCharacterClick: (Long) -> Unit,
    onSelectChat: (Long) -> Unit,
    onCreateNewChatForCharacter: (Long, String, (Long) -> Unit) -> Unit = { _, _, _ -> },
    onTogglePinChat: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleArchiveChat: (Long, Boolean) -> Unit = { _, _ -> },
    onRenameChat: (Long, String) -> Unit = { _, _ -> },
    onDeleteChat: (Long) -> Unit = {},
    onEditCharacter: (Long) -> Unit,
    onCreateCharacter: () -> Unit,
    onImportCharacterJson: (String) -> Unit,
    onTogglePinCharacter: (Long, Boolean) -> Unit,
    onToggleArchiveCharacter: (Long, Boolean) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit,
    onOpenProviders: () -> Unit,
    onOpenPersonas: () -> Unit,
    onOpenStore: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onSwitchToClassic: () -> Unit,
    selectedTab: Int? = null,
    fixedTab: Int? = null,
    onTabChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val initialTab = remember(userSettings.vaultHubStartTab) {
        if (userSettings.vaultHubStartTab == "chats") 1 else 0
    }
    var internalVaultTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val currentVaultTab = fixedTab ?: selectedTab ?: internalVaultTab
    val onVaultTabSelect: (Int) -> Unit = { newTab ->
        internalVaultTab = newTab
        onTabChange?.invoke(newTab)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var inspectBot by remember { mutableStateOf<StoreBotItem?>(null) }
    var downloadingBotName by remember { mutableStateOf<String?>(null) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showFramesVaultDialog by remember { mutableStateOf(false) }
    var selectedCharacterForChats by remember { mutableStateOf<CharacterEntity?>(null) }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val json = stream.bufferedReader().use { it.readText() }
                    onImportCharacterJson(json)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showFontDialog) {
        FontPickerDialog(
            currentChoice = userSettings.fontFamilyChoice,
            customFontName = userSettings.customFontName,
            glassmorphismEnabled = userSettings.glassmorphismEnabled,
            wallpaperTone = userSettings.wallpaperTone,
            onSelectChoice = { choice ->
                onUpdateSettings(userSettings.copy(fontFamilyChoice = choice))
            },
            onCustomFontImported = { path, name ->
                onUpdateSettings(
                    userSettings.copy(
                        fontFamilyChoice = "custom",
                        customFontPath = path,
                        customFontName = name
                    )
                )
                Toast.makeText(context, "Шрифт «$name» успешно применён!", Toast.LENGTH_SHORT).show()
            },
            onToggleGlassmorphism = { enabled ->
                onUpdateSettings(userSettings.copy(glassmorphismEnabled = enabled))
            },
            onSelectWallpaperTone = { tone ->
                onUpdateSettings(userSettings.copy(wallpaperTone = tone))
            },
            onDismiss = { showFontDialog = false }
        )
    }

    if (showFramesVaultDialog) {
        com.example.ui.components.UserFrameRepositoryDialog(
            userAvatarUri = characters.firstOrNull()?.avatarUri ?: "drawable://avatar_alice",
            currentSelectedFrameUri = userSettings.userActiveFrameUri,
            rawFramesJson = userSettings.userFramesJson,
            onDismiss = { showFramesVaultDialog = false },
            onSelectFrame = { selectedUri ->
                onUpdateSettings(userSettings.copy(userActiveFrameUri = selectedUri))
            },
            onSaveFramesJson = { updatedJson ->
                onUpdateSettings(userSettings.copy(userFramesJson = updatedJson))
            }
        )
    }

    val appLang = com.example.util.LocalAppLanguage.current
    val isEn = appLang.equals("en", ignoreCase = true)
    val isFemboy = appLang.equals("femboy", ignoreCase = true)

    // Hero banners
    val banners = remember(isEn, isFemboy) {
        listOf(
            VaultBannerItem(
                title = if (isFemboy) "Конструктор милашек" else if (isEn) "Character Studio" else "Конструктор Персонажей",
                subtitle = if (isFemboy) "Создайте уникального фембойчика с характером и чулочками" else if (isEn) "Create a unique AI character with personality, backstory, and banner" else "Создайте уникального бота с характером, прописанным лором, тоном и баннером",
                buttonText = if (isFemboy) "Создать ботика" else if (isEn) "Create Bot" else "Создать бота",
                icon = Icons.Default.Add,
                badge = if (isEn) "STUDIO" else "СТУДИЯ",
                gradientColors = listOf(Color(0xFF6B21A8), Color(0xFF3B0764)),
                onClick = onCreateCharacter
            ),
            VaultBannerItem(
                title = if (isEn) "Cerebras & Grok API" else "Cerebras & Grok API",
                subtitle = if (isEn) "Models like gpt-oss-120b, Llama 3.3 70B, Grok and OpenRouter with diagnostic tools" else "Модели gpt-oss-120b, Llama 3.3 70B, Grok и OpenRouter с диагностикой запросов",
                buttonText = if (isEn) "Configure API" else "Настроить API",
                icon = Icons.Default.Bolt,
                badge = if (isEn) "AI ENGINES" else "НЕЙРОСЕТИ",
                gradientColors = listOf(Color(0xFFB45309), Color(0xFF78350F)),
                onClick = onOpenProviders
            ),
            VaultBannerItem(
                title = if (isFemboy) "Масочки пользователя" else if (isEn) "User Personas" else "Пользовательские Персоны",
                subtitle = if (isFemboy) "Настройте ваш собственный профиль и масочку для диалогов" else if (isEn) "Customize your own profile, name, and appearance for realistic roleplay" else "Настройте ваш собственный профиль и маску для реалистичного отыгрыша в диалогах",
                buttonText = if (isFemboy) "Мои масочки" else if (isEn) "My Personas" else "Мои персоны",
                icon = Icons.Default.Person,
                badge = if (isEn) "ROLEPLAY" else "ОТЫГРЫШ",
                gradientColors = listOf(Color(0xFF0F766E), Color(0xFF134E4A)),
                onClick = onOpenPersonas
            )
        )
    }

    // Quick action buttons
    val quickActions = remember(isEn, isFemboy) {
        listOf(
            QuickActionItem(if (isFemboy) "Создать (ня)" else if (isEn) "Create" else "Создать", Icons.Default.AddCircle, Color(0xFF8B5CF6).copy(alpha = 0.2f), Color(0xFFC4B5FD), onCreateCharacter),
            QuickActionItem(if (isFemboy) "Рамочки" else if (isEn) "Frames" else "Рамки", Icons.Default.FilterFrames, Color(0xFFF97316).copy(alpha = 0.2f), Color(0xFFFED7AA)) { showFramesVaultDialog = true },
            QuickActionItem(if (isFemboy) "Загрузка" else if (isEn) "Import" else "Импорт", Icons.Default.FileUpload, Color(0xFF3B82F6).copy(alpha = 0.2f), Color(0xFF93C5FD)) { importFileLauncher.launch("*/*") },
            QuickActionItem(if (isFemboy) "Провайдеры" else if (isEn) "Providers" else "Провайдеры", Icons.Default.Bolt, Color(0xFFF59E0B).copy(alpha = 0.2f), Color(0xFFFDE68A), onOpenProviders),
            QuickActionItem(if (isFemboy) "Масочки" else if (isEn) "Personas" else "Персоны", Icons.Default.Face, Color(0xFF10B981).copy(alpha = 0.2f), Color(0xFFA7F3D0), onOpenPersonas),
            QuickActionItem(if (isFemboy) "Шрифтики" else if (isEn) "Fonts" else "Шрифты", Icons.Default.TextFields, Color(0xFF6366F1).copy(alpha = 0.2f), Color(0xFFA5B4FC)) { showFontDialog = true },
            QuickActionItem(if (isFemboy) "Стиль (uwu)" else if (isEn) "Style" else "Стиль", Icons.Default.Palette, Color(0xFFEC4899).copy(alpha = 0.2f), Color(0xFFFBCFE8), onOpenSettings),
            QuickActionItem(if (isFemboy) "Сохраняшки" else if (isEn) "Backup" else "Бэкап", Icons.Default.Save, Color(0xFF14B8A6).copy(alpha = 0.2f), Color(0xFF99F6E4), onOpenBackup)
        )
    }

    // O(1) lookup maps to eliminate O(N*M) list scans during tab switches on low-end devices
    val charactersByLowerName = remember(characters) {
        characters.associateBy { it.name.trim().lowercase() }
    }
    val chatsByCharacterId = remember(chats) {
        chats.groupBy { it.characterId }
    }

    // Top-level remember state for online catalog (only computed when on Tab 0)
    val allBots = remember(storeState, currentVaultTab) {
        if (currentVaultTab == 0 && storeState is StoreUiState.Success) storeState.catalog.characters else emptyList()
    }

    val uniqueTags = remember(allBots) {
        if (allBots.isEmpty()) emptyList()
        else {
            val tagsSet = LinkedHashSet<String>()
            allBots.forEach { tagsSet.addAll(it.tags) }
            tagsSet.sorted()
        }
    }

    val filteredBots = remember(allBots, searchQuery, selectedTagFilter) {
        if (allBots.isEmpty()) emptyList()
        else {
            val q = searchQuery.trim()
            allBots.filter { bot ->
                val matchesQuery = q.isBlank() ||
                        bot.name.contains(q, ignoreCase = true) ||
                        bot.shortDescription.contains(q, ignoreCase = true) ||
                        bot.author.contains(q, ignoreCase = true) ||
                        bot.tags.any { it.contains(q, ignoreCase = true) }

                val matchesTag = selectedTagFilter == null || bot.tags.contains(selectedTagFilter)
                matchesQuery && matchesTag
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isEn) "Untitled Hub" else "Новый Untitled",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isEn) "Intelligent character & dialogue hub" else "Интеллектуальный хаб персонажей и диалогов",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshStore) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить каталог", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showFontDialog = true }) {
                        Icon(Icons.Default.TextFields, contentDescription = "Шрифты и стиль", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Vault Mode Navigation Tabs: 0: "Общее" (box.json), 1: "Чаты" (Диалоги)
            PrimaryTabRow(
                selectedTabIndex = currentVaultTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = currentVaultTab == 0,
                    onClick = { onVaultTabSelect(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Hub (Vault)" else "Общее (Хранилище)", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = currentVaultTab == 1,
                    onClick = { onVaultTabSelect(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${if (isEn) "Chats" else "Чаты"} (${chats.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            when (currentVaultTab) {
                0 -> {
                    // MAIN VAULT TAB: Banners, Quick Actions, and GitHub Box.json Online Showcase
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Hero Banners Pager
                        item {
                            val pagerState = rememberPagerState(pageCount = { banners.size })
                            Column {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                ) { page ->
                                    val item = banners[page]
                                    VaultHeroBannerCard(item = item)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    repeat(banners.size) { idx ->
                                        val isSelected = pagerState.currentPage == idx
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 3.dp)
                                                .size(if (isSelected) 8.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Quick Actions Grid
                        item {
                            Text(
                                text = if (isEn) "Quick Actions" else "Быстрые действия",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                quickActions.take(4).forEach { action ->
                                    QuickActionButtonItem(action = action)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                quickActions.drop(4).take(4).forEach { action ->
                                    QuickActionButtonItem(action = action)
                                }
                            }
                        }

                        // 3. GitHub Box.json Online Showcase Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEn) "Characters in Vault" else "Персонажи в Хранилище",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isEn) "Catalog from GitHub repository (box.json)" else "Каталог из GitHub репозитория (box-of-untitledAI/box.json)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = onRefreshStore) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Обновить",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(if (isEn) "Search Vault by name, description or tag..." else "Поиск в Хранилище по имени, описанию или тегу...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Очистить")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 4. Online Catalog Content
                        when (storeState) {
                            is StoreUiState.Idle, is StoreUiState.Loading -> {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Загрузка персонажей из box.json...", fontWeight = FontWeight.Bold)
                                            Text("Подключение к GitHub репозиторию bad-kn1fe-cmd/box-of-untitledAI", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            is StoreUiState.Error, is StoreUiState.Offline -> {
                                val errMessage = if (storeState is StoreUiState.Offline) storeState.message else (storeState as StoreUiState.Error).message
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Не удалось загрузить Хранилище", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(errMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(onClick = onRefreshStore, shape = RoundedCornerShape(8.dp)) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Повторить попытку")
                                            }
                                        }
                                    }
                                }
                            }

                            is StoreUiState.Success -> {
                                item {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item {
                                            FilterChip(
                                                selected = selectedTagFilter == null,
                                                onClick = { selectedTagFilter = null },
                                                label = { Text("Все (${allBots.size})", fontSize = 11.5.sp) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                        items(uniqueTags, key = { it }, contentType = { "tag_chip" }) { tag ->
                                            FilterChip(
                                                selected = selectedTagFilter == tag,
                                                onClick = { selectedTagFilter = if (selectedTagFilter == tag) null else tag },
                                                label = { Text(tag, fontSize = 11.5.sp) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }

                                if (filteredBots.isEmpty()) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(40.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Боты не найдены", fontWeight = FontWeight.Bold)
                                                Text("Попробуйте изменить запрос поиска", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                } else {
                                    items(
                                        items = filteredBots,
                                        key = { "${it.name}_${it.author}" },
                                        contentType = { "store_bot_card" }
                                    ) { bot ->
                                        val isImporting = downloadingBotName == bot.name
                                        val existingChar = charactersByLowerName[bot.name.trim().lowercase()]
                                        val botChats = if (existingChar != null) {
                                            chatsByCharacterId[existingChar.id].orEmpty()
                                        } else {
                                            emptyList()
                                        }
                                        val botRelBadge = remember(existingChar, botChats, latestRelationshipsByChat) {
                                            if (existingChar != null) {
                                                val latestRel = botChats.mapNotNull { latestRelationshipsByChat[it.id] }
                                                    .maxByOrNull { it.timestamp }
                                                val board = existingChar.getParsedRelationshipBoard()
                                                val title = latestRel?.status?.takeIf { it.isNotBlank() }
                                                    ?: existingChar.getEffectiveInitialRelTitle()
                                                val pct = latestRel?.trustScore
                                                    ?: existingChar.getEffectiveInitialRelPercent()
                                                val sign = if (pct > 0) "+" else ""
                                                val icon = if (board != null && board.edges.isNotEmpty()) "🕸" else "📊"
                                                "$icon $title • $sign$pct%"
                                            } else null
                                        }

                                        StoreBotCardItem(
                                            bot = bot,
                                            isAlreadyImported = existingChar != null,
                                            chatsCount = botChats.size,
                                            relationshipBadge = botRelBadge,
                                            isDownloading = isImporting,
                                            onInspect = { inspectBot = bot },
                                            onEdit = if (existingChar != null) { { onEditCharacter(existingChar.id) } } else null,
                                            onStartOrDownload = {
                                                if (existingChar != null) {
                                                    if (botChats.size > 1) {
                                                        selectedCharacterForChats = existingChar
                                                    } else {
                                                        onCharacterClick(existingChar.id)
                                                    }
                                                } else {
                                                    downloadingBotName = bot.name
                                                    onDownloadStoreBot(bot, { newChar ->
                                                        downloadingBotName = null
                                                        Toast.makeText(context, "Бот «${newChar.name}» успешно импортирован!", Toast.LENGTH_SHORT).show()
                                                        onCharacterClick(newChar.id)
                                                    }, { err ->
                                                        downloadingBotName = null
                                                        Toast.makeText(context, "Ошибка импорта: $err", Toast.LENGTH_LONG).show()
                                                    })
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // DEDICATED DIALOGS TAB (Grouped by Character)
                    VaultChatsTab(
                        chats = chats,
                        characters = characters,
                        latestRelationshipsByChat = latestRelationshipsByChat,
                        searchQuery = searchQuery,
                        userSettings = userSettings,
                        onUpdateSettings = onUpdateSettings,
                        onTogglePinCharacter = onTogglePinCharacter,
                        onSearchChange = { searchQuery = it },
                        onSelectChat = onSelectChat,
                        onOpenChatChooser = { char -> selectedCharacterForChats = char },
                        onCreateCharacter = onCreateCharacter,
                        onEditCharacter = onEditCharacter,
                        onCharacterClick = onCharacterClick
                    )
                }
            }
        }
    }

    // Bot Detailed Profile Modal
    if (inspectBot != null) {
        val bot = inspectBot!!
        val existingChar = characters.find { it.name.trim().equals(bot.name.trim(), ignoreCase = true) }
        val botChats = remember(chats, existingChar?.id) {
            if (existingChar != null) chats.filter { it.characterId == existingChar.id } else emptyList()
        }

        VaultBotInspectDialog(
            bot = bot,
            isAlreadyImported = existingChar != null,
            onDismiss = { inspectBot = null },
            onEditCharacter = { existingChar?.let { onEditCharacter(it.id) } },
            onImportAndChat = {
                inspectBot = null
                if (existingChar != null) {
                    if (botChats.size > 1) {
                        selectedCharacterForChats = existingChar
                    } else {
                        onCharacterClick(existingChar.id)
                    }
                } else {
                    downloadingBotName = bot.name
                    onDownloadStoreBot(bot, { newChar ->
                        downloadingBotName = null
                        Toast.makeText(context, "Бот «${newChar.name}» импортирован!", Toast.LENGTH_SHORT).show()
                        onCharacterClick(newChar.id)
                    }, { err ->
                        downloadingBotName = null
                        Toast.makeText(context, "Ошибка: $err", Toast.LENGTH_LONG).show()
                    })
                }
            }
        )
    }

    // Multi-Chat Choice Dialog for a Character
    if (selectedCharacterForChats != null) {
        val targetCharacter = selectedCharacterForChats!!
        val characterChats = remember(chats, targetCharacter.id) {
            chats.filter { it.characterId == targetCharacter.id }
        }

        ChatSelectionDialog(
            character = targetCharacter,
            chats = characterChats,
            onDismiss = { selectedCharacterForChats = null },
            onSelectChat = { chatId ->
                selectedCharacterForChats = null
                onSelectChat(chatId)
            },
            onCreateNewChat = { title ->
                onCreateNewChatForCharacter(targetCharacter.id, title) { newId ->
                    selectedCharacterForChats = null
                    onSelectChat(newId)
                }
            },
            onTogglePinChat = onTogglePinChat,
            onToggleArchiveChat = onToggleArchiveChat,
            onRenameChat = onRenameChat,
            onDeleteChat = onDeleteChat,
            onEditCharacter = {
                onEditCharacter(targetCharacter.id)
            }
        )
    }
}

@Composable
fun StoreBotCardItem(
    bot: StoreBotItem,
    isAlreadyImported: Boolean,
    chatsCount: Int = 0,
    relationshipBadge: String? = null,
    isDownloading: Boolean,
    onInspect: () -> Unit,
    onStartOrDownload: () -> Unit,
    onEdit: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onInspect() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                if (bot.bannerUri.isNotBlank()) {
                    AppAsyncImage(
                        model = bot.bannerUri,
                        contentDescription = "Баннер ${bot.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                )
                            )
                    )
                } else if (bot.avatarUri.isNotBlank()) {
                    AppAsyncImage(
                        model = bot.avatarUri,
                        contentDescription = "Фон ${bot.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.75f))
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                }

                // Top badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color.Black.copy(alpha = 0.6f)) {
                            Text(
                                text = "by ${bot.author.ifBlank { "UntitledAI" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (!relationshipBadge.isNullOrBlank()) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.Black.copy(alpha = 0.6f)) {
                                Text(
                                    text = relationshipBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (chatsCount > 1) {
                            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {
                                Text(
                                    text = "💬 $chatsCount ветки",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Body
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarImage(
                    uri = bot.avatarUri,
                    contentDescription = bot.name,
                    size = 56.dp,
                    frameUri = bot.avatarFrameUri
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bot.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (bot.shortDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = bot.shortDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isAlreadyImported && onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Button(
                    onClick = onStartOrDownload,
                    enabled = !isDownloading,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(
                            imageVector = if (isAlreadyImported) Icons.AutoMirrored.Filled.Chat else Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAlreadyImported) (if (chatsCount > 1) "Выбор" else "Чат") else "Забрать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VaultHeroBannerCard(item: VaultBannerItem) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(155.dp)
            .padding(horizontal = 4.dp),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(item.gradientColors))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.72f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = item.badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                }

                Button(
                    onClick = item.onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1E1B4B)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(item.buttonText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .align(Alignment.CenterEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionButtonItem(action: QuickActionItem) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable { action.onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = action.containerColor,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = action.iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = action.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VaultChatsTab(
    chats: List<ChatEntity>,
    characters: List<CharacterEntity>,
    latestRelationshipsByChat: Map<Long, RelationshipTableEntity> = emptyMap(),
    searchQuery: String,
    userSettings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit = {},
    onTogglePinCharacter: (Long, Boolean) -> Unit = { _, _ -> },
    onSearchChange: (String) -> Unit,
    onSelectChat: (Long) -> Unit,
    onOpenChatChooser: (CharacterEntity) -> Unit,
    onCreateCharacter: () -> Unit,
    onEditCharacter: (Long) -> Unit = {},
    onCharacterClick: (Long) -> Unit = {}
) {
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    var selectedCategory by remember { mutableStateOf("all") } // "all", "pinned", "folder:{id}", "assistants", "archived"
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var editingFolderId by remember { mutableStateOf<String?>(null) }
    var folderNameInput by remember { mutableStateOf("") }
    var folderSelectedBotIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var botForFolderAssign by remember { mutableStateOf<CharacterEntity?>(null) }

    val customFolders = remember(userSettings.customChatFoldersJson) {
        userSettings.getCustomChatFolders()
    }

    val chatsByCharId = remember(chats) {
        chats.groupBy { it.characterId }.mapValues { (_, list) -> list.sortedByDescending { it.updatedAt } }
    }

    // Group chats by CharacterEntity using O(1) map lookup with REAL pinned items on top
    val groupedCharacterChats = remember(chatsByCharId, characters, searchQuery, selectedCategory, customFolders) {
        val q = searchQuery.trim()
        characters.mapNotNull { char ->
            val botChats = chatsByCharId[char.id].orEmpty()
            val matchesQuery = q.isBlank() ||
                    char.name.contains(q, ignoreCase = true) ||
                    botChats.any { it.title.contains(q, ignoreCase = true) }

            val matchesCategory = when {
                selectedCategory == "pinned" -> char.isPinned || botChats.any { it.isPinned }
                selectedCategory == "assistants" -> char.isAssistant
                selectedCategory == "archived" -> char.isArchived || (botChats.isNotEmpty() && botChats.all { it.isArchived })
                selectedCategory.startsWith("folder:") -> {
                    val fId = selectedCategory.removePrefix("folder:")
                    customFolders.find { it.id == fId }?.characterIds?.contains(char.id) == true
                }
                else -> !char.isArchived && !char.isHidden
            }
            if (matchesQuery && matchesCategory) Pair(char, botChats) else null
        }.sortedWith(
            compareByDescending<Pair<CharacterEntity, List<ChatEntity>>> { (char, botChats) ->
                char.isPinned || botChats.any { it.isPinned }
            }.thenByDescending { (char, botChats) ->
                botChats.firstOrNull()?.updatedAt ?: char.createdAt
            }
        )
    }

    val categories = remember(chatsByCharId, characters, customFolders, isEn) {
        val list = mutableListOf(
            "all" to (if (isEn) "All (${characters.size})" else "Все (${characters.size})"),
            "pinned" to "📌 " + (if (isEn) "Pinned (${characters.count { char -> val cChats = chatsByCharId[char.id].orEmpty(); char.isPinned || cChats.any { it.isPinned } }})" else "Закрепленные (${characters.count { char -> val cChats = chatsByCharId[char.id].orEmpty(); char.isPinned || cChats.any { it.isPinned } }})")
        )
        customFolders.forEach { folder ->
            val count = characters.count { folder.characterIds.contains(it.id) }
            list.add("folder:${folder.id}" to "📁 ${folder.name} ($count)")
        }
        list.add("assistants" to "⚡ " + (if (isEn) "Assistants (${characters.count { it.isAssistant }})" else "Ассистенты (${characters.count { it.isAssistant }})"))
        list.add("archived" to "📦 " + (if (isEn) "Archive (${characters.count { char -> val cChats = chatsByCharId[char.id].orEmpty(); char.isArchived || (cChats.isNotEmpty() && cChats.all { it.isArchived }) }})" else "Архив (${characters.count { char -> val cChats = chatsByCharId[char.id].orEmpty(); char.isArchived || (cChats.isNotEmpty() && cChats.all { it.isArchived }) }})"))
        list.toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(if (isEn) "Search dialogue or character..." else "Поиск диалога или персонажа...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = if (isEn) "Clear" else "Очистить")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips in Dialogs Tab
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(categories, key = { (id, _) -> id }, contentType = { "chat_category_chip" }) { (id, label) ->
                FilterChip(
                    selected = selectedCategory == id,
                    onClick = { selectedCategory = id },
                    label = { Text(label, fontSize = 11.5.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
            item {
                AssistChip(
                    onClick = {
                        editingFolderId = null
                        folderNameInput = ""
                        folderSelectedBotIds = emptySet()
                        showCreateFolderDialog = true
                    },
                    label = { Text(if (isEn) "+ New Folder" else "+ Новая папка", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Active Custom Folder Info Bar
        val currentFolder = customFolders.find { "folder:${it.id}" == selectedCategory }
        if (currentFolder != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📁 Папка «${currentFolder.name}» (${currentFolder.characterIds.size} ботов)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            editingFolderId = currentFolder.id
                            folderNameInput = currentFolder.name
                            folderSelectedBotIds = currentFolder.characterIds.toSet()
                            showCreateFolderDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Управление папкой", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (groupedCharacterChats.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Нет персонажей в данной категории", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Создайте нового персонажа или добавьте в эту папку", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(groupedCharacterChats, key = { (char, _) -> char.id }, contentType = { "character_chat_card" }) { (char, botChats) ->
                    val latestChat = botChats.firstOrNull()
                    val chatCount = botChats.size
                    val isPinned = char.isPinned || latestChat?.isPinned == true

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (isPinned) 1.5.dp else 1.dp,
                            if (isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        shadowElevation = if (isPinned) 4.dp else 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (chatCount > 1) {
                                    onOpenChatChooser(char)
                                } else if (latestChat != null) {
                                    onSelectChat(latestChat.id)
                                } else {
                                    onCharacterClick(char.id)
                                }
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Character Banner on top of Chat Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                            ) {
                                if (char.bannerUri.isNotBlank()) {
                                    AppAsyncImage(
                                        model = char.bannerUri,
                                        contentDescription = "Баннер ${char.name}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                                )
                                            )
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                                    )
                                                )
                                            )
                                    )
                                }

                                // Top badges overlay
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Black.copy(alpha = 0.55f)
                                    ) {
                                        val latestRel = remember(botChats, latestRelationshipsByChat) {
                                            botChats.mapNotNull { latestRelationshipsByChat[it.id] }
                                                .maxByOrNull { it.timestamp }
                                        }
                                        val relText = remember(char, latestRel) {
                                            val relTitle = latestRel?.status?.takeIf { it.isNotBlank() }
                                                ?: char.getEffectiveInitialRelTitle()
                                            val relPercent = latestRel?.trustScore
                                                ?: char.getEffectiveInitialRelPercent()
                                            val sign = if (relPercent > 0) "+" else ""
                                            val board = char.getParsedRelationshipBoard()
                                            val icon = if (board != null && board.edges.isNotEmpty()) "🕸" else "📊"
                                            "$icon $relTitle • $sign$relPercent%"
                                        }
                                        Text(
                                            text = relText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (chatCount > 1) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "💬 $chatCount ветки",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (isPinned) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.PushPin,
                                                        contentDescription = "Закреплен",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Закреплен",
                                                        fontSize = 9.5.sp,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Card Content below Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarImage(
                                    uri = char.avatarUri,
                                    contentDescription = char.name,
                                    size = 48.dp,
                                    frameUri = char.avatarFrameUri,
                                    accentTintIntensity = userSettings.accentTintIntensity
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = char.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        if (char.author.isNotBlank()) {
                                            Text(
                                                text = "by ${char.author}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (chatCount > 1 && latestChat != null) "Последний: ${latestChat.title}" else if (latestChat != null) latestChat.title else char.greetingMessage.ifBlank { "Нажмите, чтобы начать диалог" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    val tagsList = remember(char.tags) {
                                        try {
                                            val arr = JSONArray(char.tags)
                                            val list = mutableListOf<String>()
                                            for (i in 0 until arr.length()) {
                                                val t = arr.optString(i, "").trim()
                                                if (t.isNotBlank()) list.add(t)
                                            }
                                            list
                                        } catch (_: Exception) {
                                            emptyList<String>()
                                        }
                                    }

                                    if (tagsList.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            tagsList.forEach { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                                ) {
                                                    Text(
                                                        text = "#$tag",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Pin Toggle Button
                                IconButton(
                                    onClick = { onTogglePinCharacter(char.id, !char.isPinned) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = if (char.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = if (char.isPinned) "Открепить" else "Закрепить",
                                        tint = if (char.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Folder Assign Button
                                IconButton(
                                    onClick = { botForFolderAssign = char },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    val inFolder = customFolders.any { it.characterIds.contains(char.id) }
                                    Icon(
                                        imageVector = if (inFolder) Icons.Default.Folder else Icons.Outlined.Folder,
                                        contentDescription = "Папки",
                                        tint = if (inFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onEditCharacter(char.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Редактировать персонажа",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                if (chatCount > 1) {
                                    OutlinedButton(
                                        onClick = { onOpenChatChooser(char) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Выбрать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (chatCount == 1) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Открыть",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    OutlinedButton(
                                        onClick = { onCharacterClick(char.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Начать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Create or Edit Custom Folder
    if (showCreateFolderDialog) {
        val isEditing = editingFolderId != null
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = {
                Text(
                    text = if (isEditing) (if (isEn) "Folder Settings" else "Настройка папки") else (if (isEn) "New Folder" else "Новая вкладка / папка"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = folderNameInput,
                        onValueChange = { folderNameInput = it },
                        label = { Text(if (isEn) "Folder Name" else "Название папки") },
                        placeholder = { Text(if (isEn) "e.g. Favorites, RPG, Work" else "например: Избранное, RPG, Работа") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Выберите ботов для этой папки (${folderSelectedBotIds.size}/${characters.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            items(characters, key = { it.id }, contentType = { "vault_bot_picker_row" }) { bot ->
                                val isSelected = folderSelectedBotIds.contains(bot.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            folderSelectedBotIds = if (isSelected) {
                                                folderSelectedBotIds - bot.id
                                            } else {
                                                folderSelectedBotIds + bot.id
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            folderSelectedBotIds = if (checked) {
                                                folderSelectedBotIds + bot.id
                                            } else {
                                                folderSelectedBotIds - bot.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    AvatarImage(uri = bot.avatarUri, contentDescription = bot.name, size = 28.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = bot.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (isEditing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                onUpdateSettings(userSettings.withRemovedCustomChatFolder(editingFolderId!!))
                                if (selectedCategory == "folder:$editingFolderId") {
                                    selectedCategory = "all"
                                }
                                showCreateFolderDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить эту папку")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = folderNameInput.trim().ifBlank { "Папка" }
                        val id = editingFolderId ?: System.currentTimeMillis().toString()
                        val folder = CustomChatFolder(
                            id = id,
                            name = name,
                            icon = "folder",
                            characterIds = folderSelectedBotIds.toList()
                        )
                        val updatedSettings = if (isEditing) {
                            userSettings.withUpdatedCustomChatFolder(folder)
                        } else {
                            userSettings.withAddedCustomChatFolder(folder)
                        }
                        onUpdateSettings(updatedSettings)
                        selectedCategory = "folder:$id"
                        showCreateFolderDialog = false
                    },
                    enabled = folderNameInput.isNotBlank()
                ) {
                    Text(if (isEditing) "Сохранить" else "Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Assign Bot to Folders
    if (botForFolderAssign != null) {
        val bot = botForFolderAssign!!
        AlertDialog(
            onDismissRequest = { botForFolderAssign = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarImage(uri = bot.avatarUri, contentDescription = bot.name, size = 32.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Папки: ${bot.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (customFolders.isEmpty()) {
                        Text(
                            text = "У вас пока нет созданных папок. Создайте первую вкладку/папку, чтобы распределять ботов!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Отметьте папки, в которых должен отображаться этот персонаж:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            customFolders.forEach { folder ->
                                val isInFolder = folder.characterIds.contains(bot.id)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isInFolder) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onUpdateSettings(userSettings.withBotInFolder(folder.id, bot.id, !isInFolder))
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = isInFolder,
                                            onCheckedChange = { checked ->
                                                onUpdateSettings(userSettings.withBotInFolder(folder.id, bot.id, checked))
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "📁 ${folder.name}",
                                            fontWeight = if (isInFolder) FontWeight.Bold else FontWeight.Normal,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { botForFolderAssign = null }) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        botForFolderAssign = null
                        editingFolderId = null
                        folderNameInput = ""
                        folderSelectedBotIds = setOf(bot.id)
                        showCreateFolderDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Новая папка")
                }
            }
        )
    }
}

@Composable
fun VaultBotInspectDialog(
    bot: StoreBotItem,
    isAlreadyImported: Boolean,
    onDismiss: () -> Unit,
    onImportAndChat: () -> Unit,
    onEditCharacter: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Banner
                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    if (bot.bannerUri.isNotBlank()) {
                        AppAsyncImage(
                            model = bot.bannerUri,
                            contentDescription = bot.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                )
                            )
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
                        )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = Color.White)
                    }

                    Row(
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarImage(uri = bot.avatarUri, contentDescription = bot.name, size = 56.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(bot.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Автор: ${bot.author.ifBlank { "UntitledAI" }}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                val parsedKnowledgeCards = remember(bot.knowledgeCards) {
                    val list = mutableListOf<Pair<String, String>>()
                    try {
                        val arr = org.json.JSONArray(bot.knowledgeCards)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(obj.optString("title") to obj.optString("content"))
                        }
                    } catch (_: Exception) {}
                    list
                }
                var collapsedKnowledgeIndices by remember(bot.name) { mutableStateOf(setOf<Int>()) }

                // Scrollable details
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (bot.shortDescription.isNotBlank()) {
                        item {
                            Text("Описание", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(bot.shortDescription, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (bot.personality.isNotBlank()) {
                        item {
                            Text("Характер и лор", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(bot.personality, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (bot.greetingMessage.isNotBlank()) {
                        item {
                            Text("Приветствие", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                Text(bot.greetingMessage, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (parsedKnowledgeCards.isNotEmpty()) {
                        item {
                            val allCollapsed = collapsedKnowledgeIndices.size >= parsedKnowledgeCards.size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Карточки знаний (${parsedKnowledgeCards.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        collapsedKnowledgeIndices = if (allCollapsed) emptySet() else parsedKnowledgeCards.indices.toSet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = if (allCollapsed) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (allCollapsed) "Развернуть все" else "Свернуть все",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                parsedKnowledgeCards.forEachIndexed { idx, (title, content) ->
                                    val isCardCollapsed = idx in collapsedKnowledgeIndices
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                collapsedKnowledgeIndices = if (isCardCollapsed) collapsedKnowledgeIndices - idx else collapsedKnowledgeIndices + idx
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = title.ifBlank { "Карточка #${idx + 1}" },
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Icon(
                                                    imageVector = if (isCardCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            AnimatedVisibility(visible = !isCardCollapsed) {
                                                Column {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = content,
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

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Закрыть")
                    }
                    if (isAlreadyImported && onEditCharacter != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onEditCharacter()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Изменить")
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onImportAndChat, shape = RoundedCornerShape(10.dp)) {
                        Icon(if (isAlreadyImported) Icons.AutoMirrored.Filled.Chat else Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isAlreadyImported) "Перейти в чат" else "Забрать и начать чат")
                    }
                }
            }
        }
    }
}
