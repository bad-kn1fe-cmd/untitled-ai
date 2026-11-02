package com.example.ui.studio

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.datastore.UserSettings
import com.example.data.local.entity.*
import com.example.ui.components.AvatarImage
import com.example.ui.components.ParallaxChatWallpaper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioModeScreen(
    accentColor: Color,
    userSettings: UserSettings = UserSettings(),
    characters: List<CharacterEntity> = emptyList(),
    providerProfiles: List<AiProviderProfileEntity> = emptyList(),
    personas: List<UserPersonaEntity> = emptyList(),
    activePersona: UserPersonaEntity? = null,
    allStories: List<StoryEntity> = emptyList(),
    allChats: List<ChatEntity> = emptyList(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val presets by StudioLayoutManager.presets.collectAsState()
    val activePreset by StudioLayoutManager.activePreset.collectAsState()
    val activeElements by StudioLayoutManager.activeElements.collectAsState()

    var selectedElementId by remember { mutableStateOf<String?>("chat_send_btn") }
    var showCreatePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var showPresetMenu by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var isInspectorMinimized by remember { mutableStateOf(false) }

    val screens = remember {
        listOf(
            StudioScreenType.CHAT_ROOM,
            StudioScreenType.CHATS,
            StudioScreenType.PROVIDERS,
            StudioScreenType.PERSONAS,
            StudioScreenType.SETTINGS
        )
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { screens.size })

    // Screen elements for current active page
    val currentScreen = screens[pagerState.currentPage]
    val screenElements = remember(currentScreen, activeElements) {
        StudioLayoutManager.defaultElements.filter { it.screenId == currentScreen.id }
    }

    LaunchedEffect(pagerState.currentPage) {
        val currentScreenId = screens[pagerState.currentPage].id
        val firstElem = StudioLayoutManager.defaultElements.firstOrNull { it.screenId == currentScreenId }
        if (firstElem != null && (selectedElementId == null || !selectedElementId!!.startsWith(currentScreenId))) {
            selectedElementId = firstElem.id
        }
    }

    val currentSelectedConfig = selectedElementId?.let {
        activeElements[it] ?: StudioLayoutManager.getElementConfig(it)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎨 Режим Студии", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Layout Studio",
                                    color = accentColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Лайаут: ${activePreset.nameRu}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Preset Selector Button
                    Box {
                        FilledTonalButton(
                            onClick = { showPresetMenu = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Пресеты", fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showPresetMenu,
                            onDismissRequest = { showPresetMenu = false }
                        ) {
                            Text(
                                "Готовые и свои лайауты",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider()

                            presets.forEach { preset ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(preset.nameRu, fontWeight = if (preset.id == activePreset.id) FontWeight.Bold else FontWeight.Normal)
                                                if (preset.id == activePreset.id) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Text(preset.descriptionRu, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        showPresetMenu = false
                                        StudioLayoutManager.selectPreset(preset.id)
                                        Toast.makeText(context, "Выбран лайаут: ${preset.nameRu}", Toast.LENGTH_SHORT).show()
                                    },
                                    trailingIcon = {
                                        if (!preset.isBuiltIn) {
                                            IconButton(
                                                onClick = {
                                                    StudioLayoutManager.deletePreset(preset.id)
                                                    Toast.makeText(context, "Лайаут удален", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                )
                            }

                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Создать новый лайаут...", color = accentColor, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    showPresetMenu = false
                                    newPresetName = "Мой лайаут ${presets.count { !it.isBuiltIn } + 1}"
                                    showCreatePresetDialog = true
                                }
                            )
                        }
                    }

                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Справка", modifier = Modifier.size(20.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen tabs selector
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 12.dp,
                    divider = {},
                    containerColor = Color.Transparent
                ) {
                    screens.forEachIndexed { index, screen ->
                        val isSelected = pagerState.currentPage == index
                        Tab(
                            selected = isSelected,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(screen.icon, fontSize = 14.sp)
                                    Text(
                                        text = screen.titleRu,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Interactive Design Canvas & Previews
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Horizontal Screen Pager rendering safe studio previews
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (screens[page]) {
                        StudioScreenType.CHAT_ROOM -> {
                            StudioChatScreenPreview(
                                accentColor = accentColor,
                                userSettings = userSettings,
                                characters = characters,
                                activePersona = activePersona,
                                selectedElementId = selectedElementId,
                                onSelectElement = { selectedElementId = it }
                            )
                        }
                        StudioScreenType.CHATS -> {
                            StudioChatsScreenPreview(
                                accentColor = accentColor,
                                characters = characters,
                                selectedElementId = selectedElementId,
                                onSelectElement = { selectedElementId = it }
                            )
                        }
                        StudioScreenType.PROVIDERS -> {
                            StudioProvidersScreenPreview(
                                accentColor = accentColor,
                                providerProfiles = providerProfiles,
                                selectedElementId = selectedElementId,
                                onSelectElement = { selectedElementId = it }
                            )
                        }
                        StudioScreenType.PERSONAS -> {
                            StudioPersonasScreenPreview(
                                accentColor = accentColor,
                                personas = personas,
                                activePersona = activePersona,
                                selectedElementId = selectedElementId,
                                onSelectElement = { selectedElementId = it }
                            )
                        }
                        StudioScreenType.SETTINGS -> {
                            StudioSettingsScreenPreview(
                                accentColor = accentColor,
                                userSettings = userSettings,
                                selectedElementId = selectedElementId,
                                onSelectElement = { selectedElementId = it }
                            )
                        }
                    }
                }

                // Floating Quick D-Pad & Control bar above the inspector
                if (currentSelectedConfig != null) {
                    FloatingElementQuickBar(
                        elementConfig = currentSelectedConfig,
                        accentColor = accentColor,
                        onNudge = { dx, dy -> StudioLayoutManager.nudgeElement(currentSelectedConfig.id, dx, dy) },
                        onScale = { delta -> StudioLayoutManager.scaleElement(currentSelectedConfig.id, delta) },
                        onCenter = {
                            StudioLayoutManager.updateElement(currentSelectedConfig.copy(offsetX = 0f, offsetY = 0f))
                        },
                        onReset = { StudioLayoutManager.resetElement(currentSelectedConfig.id) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    )
                }

                // Studio Inspector Floating Sheet at Bottom
                RealStudioInspectorOverlay(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    screenElements = screenElements,
                    selectedElementId = selectedElementId,
                    activeElements = activeElements,
                    accentColor = accentColor,
                    isMinimized = isInspectorMinimized,
                    onToggleMinimize = { isInspectorMinimized = !isInspectorMinimized },
                    onSelectElement = { selectedElementId = it },
                    onUpdateElement = { StudioLayoutManager.updateElement(it) },
                    onResetScreen = {
                        StudioLayoutManager.resetScreenElements(currentScreen.id)
                        Toast.makeText(context, "Экран «${currentScreen.titleRu}» сброшен", Toast.LENGTH_SHORT).show()
                    },
                    onResetAll = {
                        StudioLayoutManager.resetActiveLayout()
                        Toast.makeText(context, "Все элементы сброшены", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Dialog: Create New Preset
    if (showCreatePresetDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePresetDialog = false },
            title = { Text("Сохранить как новый лайаут") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Текущее расположение, масштабы и привязки кнопок сохранятся под новым именем.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Название лайаута") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            StudioLayoutManager.createCustomPreset(newPresetName.trim())
                            showCreatePresetDialog = false
                            Toast.makeText(context, "Лайаут «$newPresetName» создан!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePresetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Dialog: Help
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DesignServices, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Режим Студии Лайаутов")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "В Режиме Студии вы настраиваете **реальные экраны приложения** в реальном времени:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "1. 👆 **Перетаскивайте пальцем прямо на экране**: коснитесь любого элемента (кнопка отправки, микрофон, шапка, поле ввода, FAB) и двигайте его в любую точку экрана!",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "2. 🎛️ **Быстрый пульт сверху экрана**: кнопки [ ⬅️ ➡️ ⬆️ ⬇️ ] смещают элемент на 10px, а [ ➖ / ➕ ] изменяют масштаб.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "3. 📑 **Переключайте вкладки** (Диалог, Чаты, Провайдеры, Персоны, Настройки), чтобы настроить каждый экран.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "4. 📌 **Привязка (Anchor)**: привязывайте элементы к углам и краям (Снизу-Справа, По Центру), чтобы они сохраняли идеальные пропорции.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "5. 💾 **Сохраняйте свои лайауты** через кнопку «Пресеты» в шапке или переключайтесь на готовые (Эргономичный под одну руку, Компактный).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Понятно!")
                }
            }
        )
    }
}

/**
 * Interactive Canvas Wrapper for any Studio Element
 * Provides touch-to-select, touch-to-drag, and glowing bounding box!
 */
@Composable
fun StudioInteractiveElement(
    elementId: String,
    selectedElementId: String?,
    onSelect: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val activeElements by StudioLayoutManager.activeElements.collectAsState()
    val config = activeElements[elementId] ?: StudioLayoutManager.getElementConfig(elementId)
    val isSelected = selectedElementId == elementId

    if (!config.isVisible) {
        return
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = config.offsetX
                translationY = config.offsetY
                scaleX = config.scaleX
                scaleY = config.scaleY
                alpha = config.alpha
            }
            .pointerInput(elementId) {
                detectDragGestures(
                    onDragStart = { onSelect(elementId) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        StudioLayoutManager.nudgeElement(elementId, dragAmount.x, dragAmount.y)
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onSelect(elementId)
            }
    ) {
        content()

        // Highlight frame when element is selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 2.dp,
                        color = accentColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(accentColor.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
            )

            // Live Coordinate & Drag Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = accentColor,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-18).dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.OpenWith, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${config.nameRu.take(14)} (${config.offsetX.toInt()}, ${config.offsetY.toInt()})",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Floating Quick Nudge & Control HUD on top of the preview canvas
 */
@Composable
fun FloatingElementQuickBar(
    elementConfig: ElementLayoutConfig,
    accentColor: Color,
    onNudge: (Float, Float) -> Unit,
    onScale: (Float) -> Unit,
    onCenter: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Selected element indicator
            Text(
                text = "🎯 ${elementConfig.nameRu.take(12)}",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = accentColor,
                modifier = Modifier.padding(end = 4.dp)
            )

            // D-Pad Left
            IconButton(
                onClick = { onNudge(-10f, 0f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Влево", modifier = Modifier.size(18.dp))
            }

            // D-Pad Up
            IconButton(
                onClick = { onNudge(0f, -10f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Вверх", modifier = Modifier.size(18.dp))
            }

            // D-Pad Down
            IconButton(
                onClick = { onNudge(0f, 10f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Вниз", modifier = Modifier.size(18.dp))
            }

            // D-Pad Right
            IconButton(
                onClick = { onNudge(10f, 0f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Вправо", modifier = Modifier.size(18.dp))
            }

            VerticalDivider(modifier = Modifier.height(18.dp))

            // Scale -
            IconButton(
                onClick = { onScale(-0.05f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Уменьшить", modifier = Modifier.size(16.dp))
            }

            Text(
                text = "${(elementConfig.scaleX * 100).toInt()}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            // Scale +
            IconButton(
                onClick = { onScale(0.05f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Увеличить", modifier = Modifier.size(16.dp))
            }

            VerticalDivider(modifier = Modifier.height(18.dp))

            // Center
            IconButton(
                onClick = onCenter,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.FilterCenterFocus, contentDescription = "Центр", tint = accentColor, modifier = Modifier.size(16.dp))
            }

            // Reset
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.RotateLeft, contentDescription = "Сброс", modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Safe Full-Fidelity Chat Studio Preview
 */
@Composable
fun StudioChatScreenPreview(
    accentColor: Color,
    userSettings: UserSettings,
    characters: List<CharacterEntity>,
    activePersona: UserPersonaEntity?,
    selectedElementId: String?,
    onSelectElement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val char = characters.firstOrNull() ?: CharacterEntity(
        id = 1L,
        name = "Элиза (ИИ-Ассистент)",
        avatarUri = "",
        shortDescription = "Умный и дружелюбный помощник",
        personality = "Дружелюбная, внимательная",
        greetingMessage = "Привет! Я готова помочь с любыми задачами, творчеством или просто пообщаться. О чем поговорим?"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Wallpaper
        ParallaxChatWallpaper(
            preset = userSettings.chatWallpaperPreset,
            customUri = userSettings.customWallpaperUri,
            alpha = userSettings.chatWallpaperAlpha,
            parallaxEnabled = false,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Top Bar Element
            StudioInteractiveElement(
                elementId = "chat_top_bar",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }

                        AvatarImage(
                            uri = char.avatarUri,
                            contentDescription = char.name,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = char.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "в сети • ${char.shortDescription.ifBlank { "онлайн" }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Branch Selector
                        StudioInteractiveElement(
                            elementId = "chat_branch_selector",
                            selectedElementId = selectedElementId,
                            onSelect = onSelectElement,
                            accentColor = accentColor
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ForkRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("1/1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Action Menu
                        StudioInteractiveElement(
                            elementId = "chat_action_menu",
                            selectedElementId = selectedElementId,
                            onSelect = onSelectElement,
                            accentColor = accentColor
                        ) {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                            }
                        }
                    }
                }
            }

            // Chat Messages Body
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Character Greeting
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AvatarImage(
                            uri = char.avatarUri,
                            contentDescription = char.name,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = char.greetingMessage.ifBlank { "Привет! О чем поговорим сегодня?" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "OpenRouter",
                                        fontSize = 10.sp,
                                        color = accentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "14:20",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // User Response
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.widthIn(max = 270.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Привет! Как ты думаешь, что лучше всего сделать сегодня вечером?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "14:21",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        AvatarImage(
                            uri = activePersona?.avatarUri ?: "",
                            contentDescription = activePersona?.name ?: "Вы",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                // AI Reply
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AvatarImage(
                            uri = char.avatarUri,
                            contentDescription = char.name,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "*мягко улыбается, листая блокнот с идеями*\n\nОтличный вопрос! Можно устроить уютный вечер кино, почитать хорошую фантастику или придумать новый захватывающий сюжет для ролевой истории вместе!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = "14:22",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }

            // Bottom Chat Input Bar Element
            StudioInteractiveElement(
                elementId = "chat_input_bar",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Unified Media attach (Paperclip)
                        StudioInteractiveElement(
                            elementId = "chat_attach_photo_btn",
                            selectedElementId = selectedElementId,
                            onSelect = onSelectElement,
                            accentColor = accentColor
                        ) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Прикрепить фото или видео", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }

                        // Text input field
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "Напишите сообщение...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Voice Button
                        StudioInteractiveElement(
                            elementId = "chat_voice_btn",
                            selectedElementId = selectedElementId,
                            onSelect = onSelectElement,
                            accentColor = accentColor
                        ) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Голосовое", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Send Button
                        StudioInteractiveElement(
                            elementId = "chat_send_btn",
                            selectedElementId = selectedElementId,
                            onSelect = onSelectElement,
                            accentColor = accentColor
                        ) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Scroll down fab
        StudioInteractiveElement(
            elementId = "chat_scroll_bottom_fab",
            selectedElementId = selectedElementId,
            onSelect = onSelectElement,
            accentColor = accentColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
        ) {
            FloatingActionButton(
                onClick = {},
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Вниз", modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Safe Full-Fidelity Chats Screen Studio Preview
 */
@Composable
fun StudioChatsScreenPreview(
    accentColor: Color,
    characters: List<CharacterEntity>,
    selectedElementId: String?,
    onSelectElement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header & Logo
            StudioInteractiveElement(
                elementId = "chats_top_header",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("AI Roleplay", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Персонажи & Истории", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Quick menu
                    StudioInteractiveElement(
                        elementId = "chats_quick_menu",
                        selectedElementId = selectedElementId,
                        onSelect = onSelectElement,
                        accentColor = accentColor
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }
                    }
                }
            }

            // Search Bar
            StudioInteractiveElement(
                elementId = "chats_search_bar",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Поиск персонажей и диалогов...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Category Chips
            StudioInteractiveElement(
                elementId = "chats_category_chips",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categories = listOf("Все", "✨ Избранные", "🎭 Ролевые", "💡 Ассистенты", "⚔️ Фэнтези", "🚀 Футуризм")
                    items(categories) { cat ->
                        FilterChip(
                            selected = cat == "Все",
                            onClick = {},
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Character Cards List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val displayChars = if (characters.isNotEmpty()) characters else listOf(
                    CharacterEntity(1L, "Элиза", "", "Умный помощник", "Дружелюбная", "Привет!"),
                    CharacterEntity(2L, "Алиса Фокс", "", "Детектив", "Холодная, проницательная", "Есть зацепки?"),
                    CharacterEntity(3L, "Эльвира", "", "Маг темных искусств", "Таинственная", "Тьма шепчет...")
                )

                items(displayChars) { ch ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(
                                uri = ch.avatarUri,
                                contentDescription = ch.name,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ch.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(ch.shortDescription, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.12f)) {
                                        Text("AI Roleplay", color = accentColor, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Чат", tint = accentColor)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // FAB Create Character
        StudioInteractiveElement(
            elementId = "chats_fab_create",
            selectedElementId = selectedElementId,
            onSelect = onSelectElement,
            accentColor = accentColor,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 80.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {},
                containerColor = accentColor,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Создать", fontWeight = FontWeight.Bold) }
            )
        }
    }
}

/**
 * Safe Full-Fidelity Providers Screen Studio Preview
 */
@Composable
fun StudioProvidersScreenPreview(
    accentColor: Color,
    providerProfiles: List<AiProviderProfileEntity>,
    selectedElementId: String?,
    onSelectElement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Providers Top Bar
            StudioInteractiveElement(
                elementId = "providers_top_bar",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("⚡ ИИ Провайдеры", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Настройка моделей и API ключей", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Test All Button
                            StudioInteractiveElement(
                                elementId = "providers_test_all_btn",
                                selectedElementId = selectedElementId,
                                onSelect = onSelectElement,
                                accentColor = accentColor
                            ) {
                                FilledTonalButton(
                                    onClick = {},
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Тест", fontSize = 11.sp)
                                }
                            }

                            // Add Provider Button
                            StudioInteractiveElement(
                                elementId = "providers_add_btn",
                                selectedElementId = selectedElementId,
                                onSelect = onSelectElement,
                                accentColor = accentColor
                            ) {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accentColor)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Search / Filter
            StudioInteractiveElement(
                elementId = "providers_search_filter",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Все провайдеры (OpenRouter, Gemini, DeepSeek)", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Provider Cards List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val demoProviders = if (providerProfiles.isNotEmpty()) providerProfiles else listOf(
                    AiProviderProfileEntity(id = 1L, name = "OpenRouter (Основной)", providerType = "OPENROUTER", model = "anthropic/claude-3.5-sonnet", isDefaultText = true, isDefaultImage = false),
                    AiProviderProfileEntity(id = 2L, name = "Google Gemini AI", providerType = "GEMINI", model = "gemini-1.5-pro", isDefaultText = false, isDefaultImage = true),
                    AiProviderProfileEntity(id = 3L, name = "DeepSeek AI", providerType = "CUSTOM", model = "deepseek/deepseek-chat", isDefaultText = false, isDefaultImage = false)
                )

                items(demoProviders) { p ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accentColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                    if (p.isDefaultText) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = accentColor) {
                                            Text("ТЕКСТ", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                Text("Модель: ${p.model.ifBlank { "auto" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Settings, contentDescription = "Настройки", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

/**
 * Safe Full-Fidelity Personas Screen Studio Preview
 */
@Composable
fun StudioPersonasScreenPreview(
    accentColor: Color,
    personas: List<UserPersonaEntity>,
    activePersona: UserPersonaEntity?,
    selectedElementId: String?,
    onSelectElement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Personas Top Bar
            StudioInteractiveElement(
                elementId = "personas_top_bar",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("🎭 Персоны пользователя", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Настройте ваш образ и характер в диалогах", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Add Persona Button
                        StudioInteractiveElement(
                            elementId = "personas_add_btn",
                            selectedElementId = selectedElementId,
                            onSelect = onSelectElement,
                            accentColor = accentColor
                        ) {
                            IconButton(
                                onClick = {},
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Создать", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Personas Filter Chips
            StudioInteractiveElement(
                elementId = "personas_filter_chips",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Все персоны", "Активная", "Ролевые", "Деловые")) { chip ->
                        FilterChip(
                            selected = chip == "Все персоны",
                            onClick = {},
                            label = { Text(chip, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Persona Cards List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val demoPersonas = if (personas.isNotEmpty()) personas else listOf(
                    UserPersonaEntity(id = 1L, name = "Основная персона", shortDescription = "Дружелюбный искатель приключений", isSelected = true),
                    UserPersonaEntity(id = 2L, name = "Детектив Холмс", shortDescription = "Внимательный аналитик", isSelected = false),
                    UserPersonaEntity(id = 3L, name = "Космический странник", shortDescription = "Пилот звездного корабля", isSelected = false)
                )

                items(demoPersonas) { pr ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarImage(
                                uri = pr.avatarUri,
                                contentDescription = pr.name,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(pr.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                    if (pr.isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = accentColor) {
                                            Text("АКТИВНА", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                }
                                Text(pr.shortDescription.ifBlank { "Без описания" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

/**
 * Safe Full-Fidelity Settings Screen Studio Preview
 */
@Composable
fun StudioSettingsScreenPreview(
    accentColor: Color,
    userSettings: UserSettings,
    selectedElementId: String?,
    onSelectElement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Settings Top Bar
            StudioInteractiveElement(
                elementId = "settings_top_bar",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚙️ Настройки приложения", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
            }

            // Search Bar
            StudioInteractiveElement(
                elementId = "settings_search_bar",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Поиск опций, тем и конфигураций...", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Category Navigation
            StudioInteractiveElement(
                elementId = "settings_category_nav",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("🎨 Оформление", "🤖 Генерация", "🔊 Звук & TTS", "💾 Бэкап")) { item ->
                        FilterChip(
                            selected = item.startsWith("🎨"),
                            onClick = {},
                            label = { Text(item, fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Studio Mode Banner
            StudioInteractiveElement(
                elementId = "settings_studio_banner",
                selectedElementId = selectedElementId,
                onSelect = onSelectElement,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = accentColor, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DesignServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Режим Студии (Studio Mode)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                            Text("Полная кастомизация кнопок, отступов и интерфейса", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Settings Options List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    ListItem(
                        headlineContent = { Text("Акцентный цвет темы", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Текущий выбранный оттенок интерфейса") },
                        leadingContent = { Icon(Icons.Default.Palette, contentDescription = null, tint = accentColor) },
                        trailingContent = {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(accentColor))
                        }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Темная тема", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Автоматически или всегда включена") },
                        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        trailingContent = {
                            Switch(checked = true, onCheckedChange = {})
                        }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Обои диалогов", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Кастомный фон или встроенные градиенты") },
                        leadingContent = { Icon(Icons.Default.Wallpaper, contentDescription = null) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

/**
 * Real Studio Inspector Overlay - Controls and Tweaks for Selected Real UI Elements
 */
@Composable
fun RealStudioInspectorOverlay(
    screenElements: List<ElementLayoutConfig>,
    selectedElementId: String?,
    activeElements: Map<String, ElementLayoutConfig>,
    accentColor: Color,
    isMinimized: Boolean,
    onToggleMinimize: () -> Unit,
    onSelectElement: (String) -> Unit,
    onUpdateElement: (ElementLayoutConfig) -> Unit,
    onResetScreen: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentConfig = selectedElementId?.let { activeElements[it] ?: StudioLayoutManager.getElementConfig(it) }

    Surface(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Header bar: Element Switcher & Minimize Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Инспектор элементов",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onResetScreen,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сброс экрана", modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onToggleMinimize,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (isMinimized) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isMinimized) "Развернуть" else "Свернуть",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Element selection chips (scrollable row)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(screenElements) { elem ->
                    val isSelected = elem.id == selectedElementId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectElement(elem.id) },
                        label = { Text(elem.nameRu, fontSize = 11.5.sp, maxLines = 1) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                        } else null,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            if (!isMinimized && currentConfig != null) {
                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Controls for Selected Element
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Element Name & Visibility & Anchor Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentConfig.nameRu,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = accentColor
                            )
                            Text(
                                text = "Привязка: ${currentConfig.anchor.titleRu} • X: ${currentConfig.offsetX.toInt()}px, Y: ${currentConfig.offsetY.toInt()}px",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Видимость:", fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = currentConfig.isVisible,
                                onCheckedChange = { onUpdateElement(currentConfig.copy(isVisible = it)) },
                                modifier = Modifier.scale(0.75f)
                            )
                        }
                    }

                    // 1. Offset X & Offset Y Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Offset X
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Смещение X", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("${currentConfig.offsetX.toInt()} px", fontSize = 11.sp, color = accentColor)
                            }
                            Slider(
                                value = currentConfig.offsetX,
                                onValueChange = { onUpdateElement(currentConfig.copy(offsetX = it)) },
                                valueRange = -250f..250f,
                                steps = 50
                            )
                        }

                        // Offset Y
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Смещение Y", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("${currentConfig.offsetY.toInt()} px", fontSize = 11.sp, color = accentColor)
                            }
                            Slider(
                                value = currentConfig.offsetY,
                                onValueChange = { onUpdateElement(currentConfig.copy(offsetY = it)) },
                                valueRange = -350f..350f,
                                steps = 70
                            )
                        }
                    }

                    // 2. Scale & Alpha Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Scale (Proportional)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Масштаб", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("${(currentConfig.scaleX * 100).toInt()}%", fontSize = 11.sp, color = accentColor)
                            }
                            Slider(
                                value = currentConfig.scaleX,
                                onValueChange = { scaleValue ->
                                    onUpdateElement(currentConfig.copy(scaleX = scaleValue, scaleY = scaleValue))
                                },
                                valueRange = 0.5f..1.8f,
                                steps = 13
                            )
                        }

                        // Alpha / Opacity
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Прозрачность", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("${(currentConfig.alpha * 100).toInt()}%", fontSize = 11.sp, color = accentColor)
                            }
                            Slider(
                                value = currentConfig.alpha,
                                onValueChange = { onUpdateElement(currentConfig.copy(alpha = it)) },
                                valueRange = 0.1f..1.0f,
                                steps = 9
                            )
                        }
                    }

                    // 3. Anchor Grid Selector (9 positions)
                    Text("📌 Привязка (Anchor):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            LayoutAnchor.TOP_START to "↖",
                            LayoutAnchor.TOP_CENTER to "⬆",
                            LayoutAnchor.TOP_END to "↗",
                            LayoutAnchor.CENTER to "⏺",
                            LayoutAnchor.BOTTOM_START to "↙",
                            LayoutAnchor.BOTTOM_CENTER to "⬇",
                            LayoutAnchor.BOTTOM_END to "↘"
                        ).forEach { (anchor, symbol) ->
                            val isAnchorSelected = currentConfig.anchor == anchor
                            OutlinedButton(
                                onClick = { onUpdateElement(currentConfig.copy(anchor = anchor)) },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                colors = if (isAnchorSelected) ButtonDefaults.outlinedButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.2f),
                                    contentColor = accentColor
                                ) else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(symbol, fontSize = 13.sp, fontWeight = if (isAnchorSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    // Reset Element Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val def = StudioLayoutManager.defaultElements.find { it.id == currentConfig.id }
                                if (def != null) onUpdateElement(def)
                            }
                        ) {
                            Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сбросить элемент", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
