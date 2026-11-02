package com.example.ui.screens.store

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.CharacterEntity
import com.example.data.repository.StoreBotItem
import com.example.ui.components.AppAsyncImage
import com.example.ui.components.AvatarImage
import com.example.ui.viewmodels.MainViewModel
import com.example.ui.viewmodels.MainViewModel.StoreUiState
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxStoreScreen(
    viewModel: MainViewModel,
    onNavigateToChat: ((Long) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storeState by viewModel.storeState.collectAsState()
    val localCharacters by viewModel.characters.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val isEn = userSettings.appLanguage == "en"

    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var inspectBot by remember { mutableStateOf<StoreBotItem?>(null) }
    var downloadingBotName by remember { mutableStateOf<String?>(null) }

    var isExiting by remember { mutableStateOf(false) }
    val handleExit: () -> Unit = {
        if (!isExiting && onBack != null) {
            isExiting = true
            onBack()
        }
    }

    BackHandler(enabled = onBack != null) {
        handleExit()
    }

    // Load store catalog initially when entering screen
    LaunchedEffect(Unit) {
        viewModel.loadStoreCatalog(force = false)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = handleExit) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад"
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEn) "Bot Repository" else "Хранилище",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Text(
                            text = if (isEn) "Public GitHub repository: box-of-untitledAI" else "Каталог готовых ботов из GitHub репозитория",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadStoreCatalog(force = true) },
                        enabled = storeState !is StoreUiState.Loading
                    ) {
                        if (storeState is StoreUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isEn) "Refresh Catalog" else "Обновить каталог",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = storeState) {
                is StoreUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isEn) "Connecting to GitHub repository..." else "Подключение к репозиторию Хранилища...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isEn) "Loading bots list from box.json" else "Загрузка списка ботов из box.json",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is StoreUiState.Offline -> {
                    // Offline state: tell user and refuse to load until retried
                    OfflineStateView(
                        message = state.message,
                        isEn = isEn,
                        onRetry = { viewModel.loadStoreCatalog(force = true) }
                    )
                }

                is StoreUiState.Error -> {
                    // Error or connection failure view
                    ErrorStateView(
                        message = state.message,
                        isEn = isEn,
                        onRetry = { viewModel.loadStoreCatalog(force = true) }
                    )
                }

                is StoreUiState.Success -> {
                    val catalog = state.catalog
                    val allItems = catalog.characters

                    // Extract all unique tags for quick filter chips
                    val allUniqueTags = remember(allItems) {
                        val tagsSet = mutableSetOf<String>()
                        allItems.forEach { bot ->
                            tagsSet.addAll(bot.tags)
                        }
                        tagsSet.toList().sorted()
                    }

                    // Filter bots by search query (Name, Tags, Author) and active tag filter
                    val filteredBots = remember(allItems, searchQuery, selectedTagFilter) {
                        allItems.filter { bot ->
                            val query = searchQuery.trim()
                            val matchesQuery = if (query.isBlank()) {
                                true
                            } else {
                                // Search by: 1. name, 2. tags, 3. author
                                bot.name.contains(query, ignoreCase = true) ||
                                        bot.tags.any { it.contains(query, ignoreCase = true) } ||
                                        bot.author.contains(query, ignoreCase = true) ||
                                        bot.shortDescription.contains(query, ignoreCase = true)
                            }

                            val matchesTag = if (selectedTagFilter.isNullOrBlank()) {
                                true
                            } else {
                                bot.tags.any { it.equals(selectedTagFilter, ignoreCase = true) }
                            }

                            matchesQuery && matchesTag
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        // Repository Header Banner
                        item {
                            StoreHeaderCard(
                                version = catalog.info.ver,
                                totalCount = catalog.info.characterNumber.ifBlank { allItems.size.toString() },
                                isEn = isEn,
                                onRefresh = { viewModel.loadStoreCatalog(force = true) }
                            )
                        }

                        // Search Bar
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = if (isEn) "Search by name, tags, or author..." else "Поиск по названию, тегам или автору...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = if (isEn) "Clear" else "Очистить"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                        }

                        // Tag Filter Chips (if any tags present)
                        if (allUniqueTags.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedTagFilter == null,
                                        onClick = { selectedTagFilter = null },
                                        label = { Text(if (isEn) "All (${allItems.size})" else "Все (${allItems.size})") },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    allUniqueTags.forEach { tag ->
                                        val isSelected = selectedTagFilter.equals(tag, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedTagFilter = if (isSelected) null else tag
                                            },
                                            label = { Text("#$tag") },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Search Results Count
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEn) "Available bots: ${filteredBots.size}" else "Доступно ботов: ${filteredBots.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (searchQuery.isNotBlank() || selectedTagFilter != null) {
                                    TextButton(
                                        onClick = {
                                            searchQuery = ""
                                            selectedTagFilter = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(if (isEn) "Reset filters" else "Сбросить фильтры", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Empty Search State
                        if (filteredBots.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isEn) "No bots found" else "Ничего не найдено",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isEn) "Try changing your search query or tag filter" else "Попробуйте изменить поисковый запрос или выбрать другой тег",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Bots List
                            items(filteredBots, key = { it.name }) { bot ->
                                val isLocalInstalled = localCharacters.any {
                                    it.name.trim().equals(bot.name.trim(), ignoreCase = true)
                                }
                                val isDownloading = downloadingBotName == bot.name

                                StoreBotCard(
                                    bot = bot,
                                    isInstalled = isLocalInstalled,
                                    isDownloading = isDownloading,
                                    isEn = isEn,
                                    onInspect = { inspectBot = bot },
                                    onDownload = {
                                        downloadingBotName = bot.name
                                        viewModel.downloadStoreBot(
                                            bot = bot,
                                            onSuccess = { savedChar ->
                                                downloadingBotName = null
                                                Toast.makeText(
                                                    context,
                                                    if (isEn) "Bot «${savedChar.name}» added to your library!" else "Бот «${savedChar.name}» успешно скачан и добавлен в список!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onError = { err ->
                                                downloadingBotName = null
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                StoreUiState.Idle -> {
                    // Initial idle state, trigger load
                    LaunchedEffect(Unit) {
                        viewModel.loadStoreCatalog()
                    }
                }
            }

    // Inspect Bot Details Dialog
    inspectBot?.let { bot ->
        val isLocalInstalled = localCharacters.any {
            it.name.trim().equals(bot.name.trim(), ignoreCase = true)
        }
        val isDownloading = downloadingBotName == bot.name

        BotDetailDialog(
            bot = bot,
            isInstalled = isLocalInstalled,
            isDownloading = isDownloading,
            isEn = isEn,
            onDismiss = { inspectBot = null },
            onDownload = {
                downloadingBotName = bot.name
                viewModel.downloadStoreBot(
                    bot = bot,
                    onSuccess = { savedChar ->
                        downloadingBotName = null
                        inspectBot = null
                        Toast.makeText(
                            context,
                            if (isEn) "Bot «${savedChar.name}» successfully downloaded!" else "Бот «${savedChar.name}» успешно скачан!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onError = { err ->
                        downloadingBotName = null
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
        }
    }
}

@Composable
fun StoreHeaderCard(
    version: String,
    totalCount: String,
    isEn: Boolean,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "v$version",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = if (isEn) "$totalCount characters" else "$totalCount персонажей",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.clickable { onRefresh() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEn) "GitHub Sync" else "Синхронизация",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isEn) "Catalog of additional bots and characters available for download to your local application database." else "Каталог дополнительных ботов и персонажей. Доступные профили для загрузки в локальную базу данных приложения.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun StoreBotCard(
    bot: StoreBotItem,
    isInstalled: Boolean,
    isDownloading: Boolean,
    isEn: Boolean,
    onInspect: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onInspect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isInstalled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
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
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                    )
                                )
                            )
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Bot Avatar
                AvatarImage(
                    uri = bot.avatarUri.ifBlank { "drawable://app_logo_black" },
                    contentDescription = bot.name,
                    size = 64.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bot.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Author badge (by field)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEn) "by: ${bot.author}" else "Автор: ${bot.author}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Voice style
                    if (bot.voiceStyle.isNotBlank()) {
                        Text(
                            text = if (isEn) "Voice: ${bot.voiceStyle}" else "Стиль речи: ${bot.voiceStyle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Short Description
            if (bot.shortDescription.isNotBlank()) {
                Text(
                    text = bot.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Tags Chips
            if (bot.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bot.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onInspect,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isEn) "Details" else "Подробнее", style = MaterialTheme.typography.labelMedium)
                }

                if (isDownloading) {
                    FilledTonalButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEn) "Downloading..." else "Загрузка...", style = MaterialTheme.typography.labelMedium)
                    }
                } else if (isInstalled) {
                    FilledTonalButton(
                        onClick = onDownload,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEn) "Installed (Update)" else "Скачан (Обновить)", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEn) "Download" else "Скачать к себе", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
}

@Composable
fun BotDetailDialog(
    bot: StoreBotItem,
    isInstalled: Boolean,
    isDownloading: Boolean,
    isEn: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        AvatarImage(
                            uri = bot.avatarUri.ifBlank { "drawable://app_logo_black" },
                            contentDescription = bot.name,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = bot.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isEn) "by: ${bot.author}" else "Автор: ${bot.author}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Knowledge Cards / Lore
                val parsedKnowledgeCards = remember(bot.knowledgeCards) {
                    val list = mutableListOf<Pair<String, String>>()
                    try {
                        val arr = JSONArray(bot.knowledgeCards)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(obj.optString("title") to obj.optString("content"))
                        }
                    } catch (_: Exception) {}
                    list
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Short description
                    if (bot.shortDescription.isNotBlank()) {
                        item {
                            DetailSection(
                                title = if (isEn) "Description" else "Описание",
                                content = bot.shortDescription
                            )
                        }
                    }

                    // Greeting Message Preview
                    if (bot.greetingMessage.isNotBlank()) {
                        item {
                            DetailSection(
                                title = if (isEn) "First Greeting Message" else "Первое сообщение бота",
                                content = bot.greetingMessage,
                                isQuote = true
                            )
                        }
                    }

                    // Personality
                    if (bot.personality.isNotBlank()) {
                        item {
                            DetailSection(
                                title = if (isEn) "Personality" else "Личность и характер",
                                content = bot.personality
                            )
                        }
                    }

                    // Appearance
                    if (bot.appearance.isNotBlank()) {
                        item {
                            DetailSection(
                                title = if (isEn) "Appearance" else "Внешность",
                                content = bot.appearance
                            )
                        }
                    }

                    // Habits
                    if (bot.habits.isNotBlank()) {
                        item {
                            DetailSection(
                                title = if (isEn) "Habits & Quirks" else "Привычки и поведение",
                                content = bot.habits
                            )
                        }
                    }

                    // Likes / Dislikes
                    if (bot.likes.isNotBlank() || bot.dislikes.isNotBlank()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (bot.likes.isNotBlank()) {
                                    DetailSection(
                                        title = if (isEn) "Likes" else "Что любит",
                                        content = bot.likes
                                    )
                                }
                                if (bot.dislikes.isNotBlank()) {
                                    DetailSection(
                                        title = if (isEn) "Dislikes" else "Что не любит",
                                        content = bot.dislikes
                                    )
                                }
                            }
                        }
                    }

                    // Knowledge Cards / Lore
                    if (parsedKnowledgeCards.isNotEmpty()) {
                        item {
                            var collapsedIndices by remember(bot.name) { mutableStateOf(setOf<Int>()) }
                            val allCollapsed = collapsedIndices.size >= parsedKnowledgeCards.size
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEn) "Lore & Knowledge Cards (${parsedKnowledgeCards.size})" else "Карточки знаний и лор (${parsedKnowledgeCards.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        collapsedIndices = if (allCollapsed) emptySet() else parsedKnowledgeCards.indices.toSet()
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
                                        text = if (allCollapsed) (if (isEn) "Expand all" else "Развернуть все") else (if (isEn) "Collapse all" else "Свернуть все"),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                parsedKnowledgeCards.forEachIndexed { idx, (title, content) ->
                                    val isCardCollapsed = idx in collapsedIndices
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                collapsedIndices = if (isCardCollapsed) collapsedIndices - idx else collapsedIndices + idx
                                            }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = title.ifBlank { if (isEn) "Card #${idx + 1}" else "Карточка #${idx + 1}" },
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

                    // Tags
                    if (bot.tags.isNotEmpty()) {
                        item {
                            Text(
                                text = if (isEn) "Tags" else "Теги",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                bot.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Bottom Download Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(if (isEn) "Close" else "Закрыть")
                    }

                    if (isDownloading) {
                        FilledTonalButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Downloading..." else "Загрузка...")
                        }
                    } else if (isInstalled) {
                        Button(
                            onClick = onDownload,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Update Bot" else "Обновить бота")
                        }
                    } else {
                        Button(
                            onClick = onDownload,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Download to Library" else "Скачать к себе")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSection(
    title: String,
    content: String,
    isQuote: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isQuote) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(
                1.dp,
                if (isQuote) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun OfflineStateView(
    message: String,
    isEn: Boolean,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isEn) "No Internet Connection" else "Нет подключения к интернету",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isEn)
                "Internet access is required to load the bot repository from GitHub. Please check your connection and try again."
            else
                "Для загрузки каталога ботов из репозитория Хранилища требуется интернет. Проверьте соединение и попробуйте снова.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isEn) "Try Again" else "Попробовать снова")
        }
    }
}

@Composable
fun ErrorStateView(
    message: String,
    isEn: Boolean,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isEn) "Failed to Load Repository" else "Не удалось загрузить Хранилище",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isEn) "Retry" else "Попробовать снова")
        }
    }
}
