package com.example.ui.screens.characters

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.data.datastore.UserSettings
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AiProviderProfileEntity
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.RelationshipTableEntity
import com.example.data.local.entity.StoryEntity
import com.example.data.repository.CharacterSerializer
import com.example.ui.components.AppAsyncImage
import com.example.ui.components.AppLogo
import com.example.ui.components.AvatarImage
import com.example.ui.studio.studioElement
import com.example.ui.screens.stories.StoryCard
import com.example.ui.screens.stories.StoryEditDialog
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    characters: List<CharacterEntity>,
    providers: List<AiProviderProfileEntity>,
    stories: List<StoryEntity> = emptyList(),
    chats: List<ChatEntity> = emptyList(),
    latestRelationshipsByChat: Map<Long, RelationshipTableEntity> = emptyMap(),
    onCharacterClick: (Long) -> Unit,
    onSelectChat: (Long) -> Unit = {},
    onCreateNewChatForCharacter: (Long, String, (Long) -> Unit) -> Unit = { _, _, _ -> },
    onTogglePinChat: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleArchiveChat: (Long, Boolean) -> Unit = { _, _ -> },
    onRenameChat: (Long, String) -> Unit = { _, _ -> },
    onDeleteChat: (Long) -> Unit = {},
    onTogglePinCharacter: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleArchiveCharacter: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleHideCharacter: (Long, Boolean) -> Unit = { _, _ -> },
    onEditCharacter: (Long) -> Unit,
    onCreateCharacter: () -> Unit,
    onImportCharacterJson: (String) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit,
    onSaveStory: (StoryEntity) -> Unit = {},
    onDeleteStory: (StoryEntity) -> Unit = {},
    onTogglePinStory: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleArchiveStory: (Long, Boolean) -> Unit = { _, _ -> },
    onQuickSaveKey: (String) -> Unit,
    onOpenProviders: () -> Unit,
    userSettings: UserSettings = UserSettings(),
    onUpdateSettings: (UserSettings) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenPersonas: () -> Unit = {},
    onOpenStore: () -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var currentTab by remember { mutableIntStateOf(0) } // 0 = Все, 1 = Закрепленные 📌, 2 = Архив 📦, 3 = Скрытые 👁️, 4 = Истории 📖
    var sortMode by remember { mutableStateOf("interaction_desc") } // interaction_desc, name_asc, name_desc, created_desc, created_asc

    var showImportDialog by remember { mutableStateOf(false) }
    var showKeyDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var openRouterKeyInput by remember { mutableStateOf("") }

    var selectedCharacterForChats by remember { mutableStateOf<CharacterEntity?>(null) }
    var editingStory by remember { mutableStateOf<StoryEntity?>(null) }
    var showCreateStoryDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    val isFemboy = com.example.util.LocalAppLanguage.current.equals("femboy", ignoreCase = true)

    var pendingExportCharacter by remember { mutableStateOf<CharacterEntity?>(null) }
    var pendingExportJson by remember { mutableStateOf("") }

    val characterFileSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val character = pendingExportCharacter
        val json = pendingExportJson
        if (uri != null && json.isNotBlank()) {
            val ok = com.example.util.BotExportHelper.writeToUri(context, uri, json)
            coroutineScope.launch {
                if (ok) {
                    snackbarHostState.showSnackbar("Бот '${character?.name ?: ""}' успешно сохранен в файл!")
                } else {
                    snackbarHostState.showSnackbar("Не удалось записать файл")
                }
            }
        }
        pendingExportCharacter = null
        pendingExportJson = ""
    }

    val characterFileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val jsonText = com.example.util.BotExportHelper.readJsonFromUri(context, uri)
            if (!jsonText.isNullOrBlank()) {
                val result = CharacterSerializer.importFromJson(jsonText, context)
                val imported = result.getOrNull()
                if (imported != null) {
                    onImportCharacterJson(jsonText)
                    showImportDialog = false
                    importJsonText = ""
                    importError = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Бот '${imported.name}' успешно импортирован из файла!")
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Не удалось распознать формат JSON"
                    importError = err
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Ошибка импорта: $err")
                    }
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Не удалось прочитать выбранный файл")
                }
            }
        }
    }

    // Check if at least one AI provider has an API key configured (not only OpenRouter)
    val hasConfiguredProvider = remember(providers) {
        providers.any { it.apiKey.isNotBlank() }
    }

    val allTags = remember(characters) {
        val tagsSet = mutableSetOf<String>()
        characters.forEach { char ->
            try {
                val arr = JSONArray(char.tags)
                for (i in 0 until arr.length()) tagsSet.add(arr.getString(i))
            } catch (_: Exception) {}
        }
        tagsSet.toList()
    }

    // Filter by tab
    val tabCharacters = remember(characters, currentTab) {
        when (currentTab) {
            0 -> characters.filter { !it.isArchived && !it.isHidden }
            1 -> characters.filter { it.isPinned && !it.isArchived && !it.isHidden }
            2 -> characters.filter { it.isArchived }
            3 -> characters.filter { it.isHidden }
            else -> characters.filter { !it.isArchived && !it.isHidden }
        }
    }

    // Filter by search & tags
    val searchedCharacters = remember(tabCharacters, searchQuery, selectedTag) {
        tabCharacters.filter { char ->
            val matchesQuery = searchQuery.isBlank() ||
                    char.name.contains(searchQuery, ignoreCase = true) ||
                    char.shortDescription.contains(searchQuery, ignoreCase = true) ||
                    char.personality.contains(searchQuery, ignoreCase = true)

            val matchesTag = selectedTag == null || try {
                val arr = JSONArray(char.tags)
                (0 until arr.length()).any { arr.getString(it).equals(selectedTag, ignoreCase = true) }
            } catch (_: Exception) { false }

            matchesQuery && matchesTag
        }
    }

    // Sort characters
    val sortedCharacters = remember(searchedCharacters, sortMode, chats) {
        searchedCharacters.sortedWith { c1, c2 ->
            // In the "All" tab, pinned items always come first
            if (currentTab == 0 && c1.isPinned != c2.isPinned) {
                if (c1.isPinned) -1 else 1
            } else {
                when (sortMode) {
                    "interaction_desc" -> {
                        val c1Last = chats.filter { it.characterId == c1.id }.maxOfOrNull { it.updatedAt }
                            ?: (if (c1.lastInteractedAt > 0L) c1.lastInteractedAt else c1.createdAt)
                        val c2Last = chats.filter { it.characterId == c2.id }.maxOfOrNull { it.updatedAt }
                            ?: (if (c2.lastInteractedAt > 0L) c2.lastInteractedAt else c2.createdAt)
                        c2Last.compareTo(c1Last)
                    }
                    "name_asc" -> c1.name.compareTo(c2.name, ignoreCase = true)
                    "name_desc" -> c2.name.compareTo(c1.name, ignoreCase = true)
                    "created_desc" -> c2.createdAt.compareTo(c1.createdAt)
                    "created_asc" -> c1.createdAt.compareTo(c2.createdAt)
                    else -> c2.createdAt.compareTo(c1.createdAt)
                }
            }
        }
    }

    // Filtered Stories
    val filteredStories = remember(stories, searchQuery) {
        stories.filter { story ->
            searchQuery.isBlank() ||
                    story.title.contains(searchQuery, ignoreCase = true) ||
                    story.shortDescription.contains(searchQuery, ignoreCase = true) ||
                    story.lore.contains(searchQuery, ignoreCase = true)
        }.sortedWith { s1, s2 ->
            if (s1.isPinned != s2.isPinned) {
                if (s1.isPinned) -1 else 1
            } else {
                s2.updatedAt.compareTo(s1.updatedAt)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.studioElement("chats_top_header"),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenSettings() }
                    ) {
                        AppLogo(size = 38.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "UntitledAI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEn) "Roleplay Engine" else "Движок ролплея",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.studioElement("chats_quick_menu")
                    ) {
                        // Direct quick access to the Bot Store
                        IconButton(onClick = onOpenStore) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = if (isEn) "Bot Repository" else "Хранилище ботов",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Three dots overflow menu
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = if (isEn) "More actions" else "Дополнительные действия",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "Bot Repository (GitHub)" else "Хранилище ботов (GitHub)") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenStore()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "Import from .json file" else "Импорт из файла .json") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.FileOpen,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        characterFileImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "Import Bot (JSON)" else "Импорт бота (JSON)") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.FileUpload,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        importJsonText = ""
                                        importError = null
                                        showImportDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "User Personas" else "Персоны пользователя") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AccountCircle,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenPersonas()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "AI Providers" else "Провайдеры ИИ") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Hub,
                                            contentDescription = null,
                                            tint = if (!hasConfiguredProvider) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenProviders()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "Backups" else "Резервные копии") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.SaveAlt,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenBackup()
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "Settings" else "Настройки") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenSettings()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isEn) "Check for Updates" else "Проверить обновления") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        onCheckUpdates()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (currentTab == 4) {
                FloatingActionButton(
                    onClick = { showCreateStoryDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.studioElement("chats_fab_create")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Создать групповой чат", modifier = Modifier.size(26.dp))
                }
            } else {
                FloatingActionButton(
                    onClick = onCreateCharacter,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.studioElement("chats_fab_create")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать бота", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Missing Provider Key Banner
            if (!hasConfiguredProvider) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.KeyOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isEn) "AI Provider Not Configured" else "ИИ-провайдер не настроен",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (isEn) "Add API key for OpenRouter, Gemini, DeepSeek or Groq" else "Добавьте ключ для OpenRouter, Gemini, DeepSeek или Groq",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = onOpenProviders,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (isEn) "Providers" else "Провайдеры", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Tabs Row
            PrimaryTabRow(
                selectedTabIndex = currentTab,
                containerColor = Color.Transparent,
                divider = {},
                modifier = Modifier.studioElement("chats_category_chips")
            ) {
                Tab(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    text = { Text("${if (isEn) "All" else "Все"} (${characters.count { !it.isArchived && !it.isHidden }})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    text = { Text("📌 (${characters.count { it.isPinned && !it.isArchived && !it.isHidden }})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    text = { Text("📦 (${characters.count { it.isArchived }})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    text = { Text("👁️ (${characters.count { it.isHidden }})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    text = { Text("${if (isEn) "👥 Groups" else "👥 Группы"} (${stories.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Search Bar
            Column(
                modifier = Modifier
                    .studioElement("chats_search_bar")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (currentTab == 4) {
                                if (isEn) "Search group chats..." else "Поиск групповых чатов..."
                            } else {
                                if (isEn) "Search characters, lore, tags..." else "Поиск персонажей, лора, тегов..."
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = if (isEn) "Clear" else "Очистить")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                // Sorting and Tag Chips (for Characters)
                if (currentTab != 4) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Sort Chips
                        item {
                            FilterChip(
                                selected = sortMode == "interaction_desc",
                                onClick = { sortMode = "interaction_desc" },
                                label = { Text(if (isEn) "🕒 Activity" else "🕒 Активность", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortMode == "name_asc",
                                onClick = { sortMode = "name_asc" },
                                label = { Text(if (isEn) "🔤 A-Z" else "🔤 А-Я", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortMode == "name_desc",
                                onClick = { sortMode = "name_desc" },
                                label = { Text(if (isEn) "🔤 Z-A" else "🔤 Я-А", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortMode == "created_desc",
                                onClick = { sortMode = "created_desc" },
                                label = { Text(if (isEn) "📅 Newest" else "📅 Новые", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortMode == "created_asc",
                                onClick = { sortMode = "created_asc" },
                                label = { Text(if (isEn) "⏳ Oldest" else "⏳ Старые", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Tags
                        if (allTags.isNotEmpty()) {
                            item {
                                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                            }
                            item {
                                FilterChip(
                                    selected = selectedTag == null,
                                    onClick = { selectedTag = null },
                                    label = { Text(if (isEn) "All Tags" else "Все теги", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            items(allTags) { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { selectedTag = if (selectedTag == tag) null else tag },
                                    label = { Text("#$tag", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // CONTENT: Characters Tab OR Group Chats Tab
            if (currentTab == 4) {
                // GROUP CHATS TAB
                if (filteredStories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (stories.isEmpty()) "Нет созданных групповых чатов" else "Ничего не найдено",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Групповые чаты объединяют нескольких персонажей и вас в одну беседу с общим контекстом и правилами.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showCreateStoryDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Создать групповой чат")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredStories, key = { it.id }) { story ->
                            StoryCard(
                                story = story,
                                allCharacters = characters,
                                onClick = { editingStory = story },
                                onTogglePin = { onTogglePinStory(story.id, !story.isPinned) },
                                onToggleArchive = { onToggleArchiveStory(story.id, !story.isArchived) },
                                onEdit = { editingStory = story },
                                onDelete = { onDeleteStory(story) }
                            )
                        }
                    }
                }
            } else {
                // CHARACTERS TAB
                if (sortedCharacters.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = when (currentTab) {
                                    1 -> "Нет закрепленных персонажей"
                                    2 -> "Архив пуст"
                                    3 -> "Нет скрытых персонажей"
                                    else -> if (characters.isEmpty()) "Список персонажей пуст" else "Ничего не найдено"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (characters.isEmpty()) "Создайте нового бота или импортируйте JSON-карточку." else "Попробуйте переключить вкладку или изменить фильтр.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (characters.isEmpty()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            characterFileImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.FileOpen, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Файл .json")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            importJsonText = ""
                                            importError = null
                                            showImportDialog = true
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Вставить текст")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val chatsByCharacterId = remember(chats) {
                        chats.groupBy { it.characterId }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = sortedCharacters,
                            key = { it.id },
                            contentType = { "character_card" }
                        ) { character ->
                            val charChats = chatsByCharacterId[character.id].orEmpty()
                            val latestRel = remember(charChats, latestRelationshipsByChat) {
                                charChats.mapNotNull { latestRelationshipsByChat[it.id] }
                                    .maxByOrNull { it.timestamp }
                            }
                            CharacterCard(
                                character = character,
                                latestRelationship = latestRel,
                                chatsCount = charChats.size,
                                activeChatsCount = charChats.count { !it.isArchived },
                                archivedChatsCount = charChats.count { it.isArchived },
                                onClick = {
                                    if (charChats.size > 1) {
                                        selectedCharacterForChats = character
                                    } else {
                                        onCharacterClick(character.id)
                                    }
                                },
                                onOpenChatChooser = {
                                    selectedCharacterForChats = character
                                },
                                onTogglePin = { onTogglePinCharacter(character.id, !character.isPinned) },
                                onToggleArchive = { onToggleArchiveCharacter(character.id, !character.isArchived) },
                                onToggleHide = { onToggleHideCharacter(character.id, !character.isHidden) },
                                onEdit = { onEditCharacter(character.id) },
                                onExportJson = {
                                    val json = CharacterSerializer.exportToJson(character)
                                    pendingExportCharacter = character
                                    pendingExportJson = json
                                    val safeName = com.example.util.BotExportHelper.sanitizeFileName(character.name)
                                    try {
                                        characterFileSaveLauncher.launch("$safeName.json")
                                    } catch (_: Exception) {
                                        val uri = com.example.util.BotExportHelper.saveJsonToDownloads(
                                            context,
                                            safeName,
                                            json
                                        )
                                        coroutineScope.launch {
                                            if (uri != null) {
                                                snackbarHostState.showSnackbar("Бот '${character.name}' сохранен в Загрузки/UntitledAI")
                                            } else {
                                                clipboardManager.setText(AnnotatedString(json))
                                                snackbarHostState.showSnackbar("Не удалось открыть проводник, JSON скопирован в буфер")
                                            }
                                        }
                                    }
                                },
                                onDelete = { onDeleteCharacter(character) }
                            )
                        }

                        // Pet Vault Showcase Card in Classic Vault
                        item(key = "classic_pet_vault_card") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("🐾", fontSize = 20.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isFemboy) "🐾 Хранилище питомцев (uwu)" else if (isEn) "🐾 Pet Vault" else "🐾 Хранилище питомцев",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = if (isEn) "Select your active screen companion pet" else "Выбирайте активного питомца для экрана и общения",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Pet Cards (Static Sprite Previews)
                                    val petsList = listOf(
                                        Triple("classic", if (isFemboy) "🐱 Бойкиссер (>w<)" else if (isEn) "🐱 Boykisser" else "🐱 Бойкиссер", com.example.R.drawable.boykisser_classic),
                                        Triple("pochita", if (isFemboy) "🐶 Почита ✨" else if (isEn) "🐶 Pochita" else "🐶 Почита ✨", com.example.R.drawable.pochita_classic)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        petsList.forEach { (petId, petTitle, spriteRes) ->
                                            val isSelected = if (petId == "pochita") userSettings.boykisserPetVariant.equals("pochita", ignoreCase = true)
                                            else !userSettings.boykisserPetVariant.equals("pochita", ignoreCase = true)

                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                border = BorderStroke(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .clickable {
                                                        onUpdateSettings(userSettings.copy(boykisserPetVariant = petId, boykisserPetEnabled = true))
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    // Static Sprite Preview
                                                    Box(
                                                        modifier = Modifier
                                                            .size(64.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color.Black.copy(alpha = 0.25f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Image(
                                                            painter = painterResource(id = spriteRes),
                                                            contentDescription = petTitle,
                                                            modifier = Modifier.size(52.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        RadioButton(
                                                            selected = isSelected,
                                                            onClick = {
                                                                onUpdateSettings(userSettings.copy(boykisserPetVariant = petId, boykisserPetEnabled = true))
                                                            },
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = petTitle,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 12.sp
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
            }
        }
    }

    // Create Story Dialog
    if (showCreateStoryDialog) {
        StoryEditDialog(
            initialStory = null,
            allCharacters = characters,
            onDismiss = { showCreateStoryDialog = false },
            onSave = { newStory ->
                onSaveStory(newStory)
            }
        )
    }

    // Edit Story Dialog
    if (editingStory != null) {
        StoryEditDialog(
            initialStory = editingStory,
            allCharacters = characters,
            onDismiss = { editingStory = null },
            onSave = { updatedStory ->
                onSaveStory(updatedStory)
            }
        )
    }

    // Chat Selection & Management Bottom Dialog
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

    // Quick Key Setup Dialog
    if (showKeyDialog) {
        Dialog(onDismissRequest = { showKeyDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Быстрая настройка ключа", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showKeyDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Вставьте ваш API-ключ OpenRouter или перейдите в раздел Провайдеров для Gemini / DeepSeek.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = openRouterKeyInput,
                        onValueChange = { openRouterKeyInput = it },
                        label = { Text("OpenRouter API Key") },
                        placeholder = { Text("sk-or-v1-...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                try { uriHandler.openUri("https://openrouter.ai/keys") } catch (_: Exception) {}
                            }
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Получить ключ OpenRouter", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { showKeyDialog = false }) {
                            Text("Отмена")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (openRouterKeyInput.isNotBlank()) {
                                    onQuickSaveKey(openRouterKeyInput.trim())
                                    showKeyDialog = false
                                }
                            },
                            enabled = openRouterKeyInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Сохранить ключ")
                        }
                    }
                }
            }
        }
    }

    // Import Character Dialog
    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isEn) "Import Bot (JSON)" else "Импорт бота (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showImportDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            characterFileImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEn) "Choose .json file from device" else "Выбрать .json файл с устройства")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = if (isEn) " or paste text below " else " или вставьте текст ниже ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importError = null
                        },
                        placeholder = { Text("{\n  \"name\": \"Elinor\",\n  \"personality\": \"...\"\n}") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    if (importError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = importError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { showImportDialog = false }) {
                            Text(if (isEn) "Cancel" else "Отмена")
                        }

                        Button(
                            onClick = {
                                val result = CharacterSerializer.importFromJson(importJsonText)
                                val imported = result.getOrNull()
                                if (imported != null) {
                                    onImportCharacterJson(importJsonText)
                                    showImportDialog = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(if (isEn) "Bot '${imported.name}' successfully imported!" else "Бот '${imported.name}' успешно импортирован!")
                                    }
                                } else {
                                    importError = result.exceptionOrNull()?.message ?: (if (isEn) "Failed to parse JSON format." else "Не удалось распознать формат JSON.")
                                }
                            },
                            enabled = importJsonText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (isEn) "Import" else "Импортировать")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterCard(
    character: CharacterEntity,
    latestRelationship: RelationshipTableEntity? = null,
    chatsCount: Int = 0,
    activeChatsCount: Int = 0,
    archivedChatsCount: Int = 0,
    onClick: () -> Unit,
    onOpenChatChooser: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleArchive: () -> Unit = {},
    onToggleHide: () -> Unit = {},
    onEdit: () -> Unit,
    onExportJson: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val isAssistant = character.name.equals("Untitled Ассистент", ignoreCase = true) || character.tags.contains("Ассистент")

    val tagsList = remember(character.tags, isAssistant) {
        try {
            val arr = JSONArray(character.tags)
            val list = (0 until arr.length()).map { arr.getString(it) }
            if (isAssistant) {
                list.filter { !it.equals("Ассистент", ignoreCase = true) && !it.equals("Помощь", ignoreCase = true) && !it.equals("Untitled", ignoreCase = true) }
            } else list
        } catch (_: Exception) {
            emptyList()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = if (character.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        else if (isAssistant) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            if (character.isPinned) 1.5.dp else if (isAssistant) 1.2.dp else 1.dp,
            if (character.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else if (isAssistant) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                if (character.bannerUri.isNotBlank()) {
                    AppAsyncImage(
                        model = character.bannerUri,
                        contentDescription = "Баннер ${character.name}",
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
                } else if (character.avatarUri.isNotBlank()) {
                    AppAsyncImage(
                        model = character.avatarUri,
                        contentDescription = "Фон ${character.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.75f)
                                    )
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

                // Relationship status & average percentage from Relationship Board / Table
                val relBadge = remember(character, latestRelationship) {
                    val board = character.getParsedRelationshipBoard()
                    val title = latestRelationship?.status?.takeIf { it.isNotBlank() }
                        ?: character.getEffectiveInitialRelTitle()
                    val pct = latestRelationship?.trustScore
                        ?: character.getEffectiveInitialRelPercent()
                    val sign = if (pct > 0) "+" else ""
                    val icon = if (board != null && board.edges.isNotEmpty()) "🕸" else "📊"
                    "$icon $title • $sign$pct%"
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.60f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = relBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarImage(
                    uri = character.avatarUri,
                    contentDescription = character.name,
                    size = 56.dp,
                    frameUri = character.avatarFrameUri
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (character.author.isNotBlank()) {
                            Text(
                                text = "by ${character.author}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (character.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Закреплен",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (character.isArchived) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Архив",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (character.isHidden) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Скрыт",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (isAssistant) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "АССИСТЕНТ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            // Age badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${character.age} лет",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (character.shortDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = character.shortDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Опции",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (character.isPinned) "Открепить" else "Закрепить 📌") },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (character.isArchived) "Разархивировать" else "В архив 📦") },
                            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onToggleArchive()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (character.isHidden) "Показать (убрать из скрытых)" else "Скрыть бота 👁️") },
                            leadingIcon = { Icon(if (character.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onToggleHide()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Скачать бота в файл (.json)") },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onExportJson()
                            }
                        )
                        if (chatsCount > 0) {
                            DropdownMenuItem(
                                text = { Text("Все ветки диалогов ($chatsCount)") },
                                leadingIcon = { Icon(Icons.Default.Forum, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenChatChooser()
                                }
                            )
                        }
                        if (!isAssistant) {
                            DropdownMenuItem(
                                text = { Text("Редактировать") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            if (tagsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tagsList.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Pin Toggle Button
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (character.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = if (character.isPinned) "Открепить" else "Закрепить",
                            modifier = Modifier.size(18.dp),
                            tint = if (character.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = onExportJson,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Скачать бота в файл",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (chatsCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenChatChooser() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$activeChatsCount акт." + if (archivedChatsCount > 0) " • 📁 $archivedChatsCount" else "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                FilledTonalButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = if (chatsCount > 1) Icons.Default.Forum else Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (chatsCount > 1) "Диалоги ($chatsCount)" else "Диалог",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSelectionDialog(
    character: CharacterEntity,
    chats: List<ChatEntity>,
    onDismiss: () -> Unit,
    onSelectChat: (Long) -> Unit,
    onCreateNewChat: (String) -> Unit,
    onTogglePinChat: (Long, Boolean) -> Unit,
    onToggleArchiveChat: (Long, Boolean) -> Unit,
    onRenameChat: (Long, String) -> Unit,
    onDeleteChat: (Long) -> Unit,
    onEditCharacter: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Активные, 1 = Архив
    var sortMode by remember { mutableStateOf("updated_desc") }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var newChatTitleInput by remember { mutableStateOf("") }

    var renamingChat by remember { mutableStateOf<ChatEntity?>(null) }
    var renameChatTitleInput by remember { mutableStateOf("") }

    var deletingChat by remember { mutableStateOf<ChatEntity?>(null) }

    val activeChats = remember(chats) { chats.filter { !it.isArchived } }
    val archivedChats = remember(chats) { chats.filter { it.isArchived } }

    val currentList = if (selectedTab == 0) activeChats else archivedChats

    val sortedList = remember(currentList, sortMode) {
        currentList.sortedWith { c1, c2 ->
            if (c1.isPinned != c2.isPinned) {
                if (c1.isPinned) -1 else 1
            } else {
                when (sortMode) {
                    "updated_desc" -> c2.updatedAt.compareTo(c1.updatedAt)
                    "updated_asc" -> c1.updatedAt.compareTo(c2.updatedAt)
                    "title_asc" -> c1.title.compareTo(c2.title, ignoreCase = true)
                    "created_desc" -> c2.createdAt.compareTo(c1.createdAt)
                    else -> c2.updatedAt.compareTo(c1.updatedAt)
                }
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarImage(uri = character.avatarUri, contentDescription = character.name, size = 42.dp, frameUri = character.avatarFrameUri)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Управление ветками диалогов",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onEditCharacter != null) {
                            IconButton(onClick = {
                                onDismiss()
                                onEditCharacter()
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Редактировать персонажа",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Активные vs Архив
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Активные (${activeChats.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Архив (${archivedChats.size})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sorting Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = sortMode == "updated_desc",
                            onClick = { sortMode = "updated_desc" },
                            label = { Text("🕒 Свежие", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = sortMode == "updated_asc",
                            onClick = { sortMode = "updated_asc" },
                            label = { Text("⏳ Старые", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = sortMode == "title_asc",
                            onClick = { sortMode = "title_asc" },
                            label = { Text("🔤 А-Я", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    item {
                        FilterChip(
                            selected = sortMode == "created_desc",
                            onClick = { sortMode = "created_desc" },
                            label = { Text("📅 Создание", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chat items list
                if (sortedList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Default.ChatBubbleOutline else Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Text(
                                text = if (selectedTab == 0) "Нет активных диалогов" else "В архиве пока пусто",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedList, key = { it.id }) { itemChat ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (itemChat.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    if (itemChat.isPinned) 1.dp else 0.5.dp,
                                    if (itemChat.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectChat(itemChat.id) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (itemChat.isPinned) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Закреплен",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }

                                            Text(
                                                text = itemChat.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Action buttons
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onTogglePinChat(itemChat.id, !itemChat.isPinned) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (itemChat.isPinned) Icons.Default.Star else Icons.Default.StarOutline,
                                                    contentDescription = if (itemChat.isPinned) "Открепить" else "Закрепить",
                                                    tint = if (itemChat.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onToggleArchiveChat(itemChat.id, !itemChat.isArchived) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (itemChat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                    contentDescription = if (itemChat.isArchived) "Восстановить из архива" else "В архив",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    renamingChat = itemChat
                                                    renameChatTitleInput = itemChat.title
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Переименовать",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { deletingChat = itemChat },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Удалить диалог",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = "💬 Чат",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }

                                        Text(
                                            text = "Обновлен: ${dateFormatter.format(Date(itemChat.updatedAt))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Bar: "➕ Новый диалог"
                Button(
                    onClick = {
                        newChatTitleInput = "Диалог ${(chats.size + 1)}"
                        showNewChatDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создать новый диалог", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // New Chat Title Dialog
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("Новая ветка диалога") },
            text = {
                Column {
                    Text(
                        "Будет создана новая независимая сюжетная линия с чистой памятью диалога:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newChatTitleInput,
                        onValueChange = { newChatTitleInput = it },
                        label = { Text("Название диалога") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = newChatTitleInput.trim()
                        if (title.isNotBlank()) {
                            showNewChatDialog = false
                            onCreateNewChat(title)
                        }
                    },
                    enabled = newChatTitleInput.isNotBlank()
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Rename Chat Dialog
    if (renamingChat != null) {
        val target = renamingChat!!
        AlertDialog(
            onDismissRequest = { renamingChat = null },
            title = { Text("Переименовать диалог") },
            text = {
                OutlinedTextField(
                    value = renameChatTitleInput,
                    onValueChange = { renameChatTitleInput = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTitle = renameChatTitleInput.trim()
                        if (newTitle.isNotBlank()) {
                            onRenameChat(target.id, newTitle)
                            renamingChat = null
                        }
                    },
                    enabled = renameChatTitleInput.isNotBlank()
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingChat = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Delete Chat Confirmation
    if (deletingChat != null) {
        val target = deletingChat!!
        AlertDialog(
            onDismissRequest = { deletingChat = null },
            title = { Text("Удалить диалог '${target.title}'?") },
            text = { Text("Все сообщения и история этой сюжетной линии будут безвозвратно удалены.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChat(target.id)
                        deletingChat = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingChat = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}
