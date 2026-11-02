package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import com.example.ui.components.PillNavigationHotbar
import com.example.ui.components.HalloweenPumpkinRainOverlay
import com.example.ui.components.FrameParallaxController
import com.example.ui.components.ParallaxChatWallpaper
import com.example.ui.components.PreloadedAnimatedTabs
import com.example.ui.components.TabPagerController
import com.example.ui.components.TabTransitionHelper
import com.example.ui.components.rememberTabPagerController
import com.example.ui.screens.backup.BackupScreen
import com.example.ui.screens.character_editor.CharacterEditorScreen
import com.example.ui.screens.characters.CharacterListScreen
import com.example.ui.screens.store.BoxStoreScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.chat_settings.ChatSettingsScreen
import com.example.ui.screens.personas.UserPersonaScreen
import com.example.ui.screens.providers.ProvidersScreen
import com.example.ui.screens.relationship.RelationshipHistoryScreen
import com.example.ui.screens.settings.AppSettingsScreen
import com.example.ui.screens.debug.GeminiDebugScreen
import com.example.ui.studio.StudioLayoutManager
import com.example.ui.studio.StudioModeScreen
import com.example.ui.studio.studioElement
import com.example.ui.screens.comics.*
import com.example.ui.screens.vault.VaultHubScreen
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ComicEntity
import com.example.ui.theme.AiRoleplayTheme
import com.example.ui.viewmodels.MainViewModel
import com.example.util.BackgroundGenerationManager
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val pendingTargetChatId = MutableStateFlow<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        BackgroundGenerationManager.init(applicationContext)
        StudioLayoutManager.init(applicationContext)

        handleTargetChatIntent(intent)

        setContent {
            val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
            val isDarkTheme = when (userSettings.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            val accentColor = Color(userSettings.accentColorLong)

            // Auto-request Notification permission on Android 13+ if enabled
            val context = LocalContext.current
            val requestNotifLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* granted or denied handled gracefully */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) {
                        requestNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(userSettings.frameParallaxEnabled, userSettings.frameParallaxSensitivity, userSettings.frameParallaxMaxOffset) {
                FrameParallaxController.updateConfig(
                    context = context,
                    enabled = userSettings.frameParallaxEnabled,
                    sens = userSettings.frameParallaxSensitivity,
                    maxOffset = userSettings.frameParallaxMaxOffset
                )
            }

            val currentFontFamily = remember(userSettings.fontFamilyChoice, userSettings.customFontPath) {
                com.example.ui.theme.FontHelper.resolveFontFamily(
                    userSettings.fontFamilyChoice,
                    userSettings.customFontPath
                )
            }

            AiRoleplayTheme(
                themeMode = userSettings.themeMode,
                darkTheme = isDarkTheme,
                accentColor = accentColor,
                fontFamily = currentFontFamily
            ) {
                CompositionLocalProvider(
                    com.example.util.LocalAppLanguage provides userSettings.appLanguage
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        var errorDialogText by remember { mutableStateOf<String?>(null) }
                        val clipboardManager = LocalClipboardManager.current
                        val isEn = userSettings.appLanguage.equals("en", ignoreCase = true)

                        if (errorDialogText != null) {
                            val errText = errorDialogText!!
                            AlertDialog(
                                onDismissRequest = { errorDialogText = null },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Text(if (isEn) "Request / Generation Error" else "Ошибка запроса / генерации")
                                    }
                                },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = if (isEn) "An error occurred. You can copy the full error text below:" else "Произошла ошибка. Вы можете скопировать полный текст ошибки ниже:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 240.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                                        ) {
                                            SelectionContainer {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp)
                                                        .verticalScroll(rememberScrollState())
                                                ) {
                                                    Text(
                                                        text = errText,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(errText))
                                            Toast.makeText(this@MainActivity, if (isEn) "Error copied to clipboard" else "Ошибка полностью скопирована", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEn) "Copy Error" else "Копировать ошибку")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { errorDialogText = null }) {
                                        Text(if (isEn) "Close" else "Закрыть")
                                    }
                                }
                            )
                        }

                        val chatInputTopYPx by viewModel.chatInputTopYPx.collectAsStateWithLifecycle()
                        val appUpdateManifest by viewModel.appUpdateState.collectAsStateWithLifecycle()
                        if (appUpdateManifest != null) {
                            val manifest = appUpdateManifest!!
                            AlertDialog(
                                onDismissRequest = {
                                    if (!manifest.forceUpdate) {
                                        viewModel.dismissAppUpdateDialog()
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                title = {
                                    Text(
                                        text = manifest.title,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Доступна новая версия: ${manifest.latestVersion} (текущая: ${com.example.util.RemoteUpdateChecker.CURRENT_APP_VERSION})",
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = manifest.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Скачивание и свежие новости в Telegram",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(manifest.telegramUrl))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "Не удалось открыть Telegram: ${manifest.telegramUrl}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Скачать в Telegram")
                                    }
                                },
                                dismissButton = {
                                    if (!manifest.forceUpdate) {
                                        TextButton(onClick = { viewModel.dismissAppUpdateDialog() }) {
                                            Text("Позже")
                                        }
                                    }
                                }
                            )
                        }

                     val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                     val startupProgress by viewModel.startupProgress.collectAsStateWithLifecycle()
                     val startupStatusText by viewModel.startupStatusText.collectAsStateWithLifecycle()
                     val warmedTabCount by viewModel.warmedTabCount.collectAsStateWithLifecycle()
                     val preloadChatShells by viewModel.preloadChatShells.collectAsStateWithLifecycle()

                     if (!isLoading && !userSettings.isGeminiTutorialShown) {
                         com.example.ui.components.GeminiTutorialDialog(
                             initialStep = 0,
                             onDismiss = {
                                 viewModel.markGeminiTutorialShown()
                             },
                             onCompleteWithApiKey = { apiKey, selectedModel ->
                                 val finalModel = selectedModel.ifBlank { "gemini-3.5-flash-lite" }
                                 viewModel.quickSaveGeminiKey(
                                     key = apiKey,
                                     model = finalModel,
                                     onComplete = {
                                         Toast.makeText(
                                             this@MainActivity,
                                             "✨ Google Gemini ($finalModel) автоматически подключён и выбран по умолчанию!",
                                             Toast.LENGTH_LONG
                                         ).show()
                                     }
                                 )
                             }
                         )
                     }

                     if (true) {
                         val navController = rememberNavController()

                        val targetChatIdState by pendingTargetChatId.collectAsStateWithLifecycle()
                        LaunchedEffect(targetChatIdState) {
                            val targetId = targetChatIdState
                            if (targetId != null && targetId > 0) {
                                viewModel.setActiveChat(targetId)
                                pendingTargetChatId.value = null
                            }
                        }

                        val characters by viewModel.characters.collectAsStateWithLifecycle()
                        val allChats by viewModel.allChats.collectAsStateWithLifecycle()
                        val personas by viewModel.personas.collectAsStateWithLifecycle()
                        val activePersona by viewModel.activePersona.collectAsStateWithLifecycle()
                        val providerProfiles by viewModel.providerProfiles.collectAsStateWithLifecycle()
                        val allStories by viewModel.allStories.collectAsStateWithLifecycle()
                        val storeState by viewModel.storeState.collectAsStateWithLifecycle()
                        val generatingChatIds by viewModel.generatingChatIds.collectAsStateWithLifecycle()

                        val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
                        val activeChat by viewModel.activeChat.collectAsStateWithLifecycle()
                        val activeCharacter by viewModel.activeCharacter.collectAsStateWithLifecycle()
                        val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
                        val activeSummaries by viewModel.activeSummaries.collectAsStateWithLifecycle()
                        val activeRelationships by viewModel.activeRelationships.collectAsStateWithLifecycle()
                        val latestRelationship by viewModel.latestRelationship.collectAsStateWithLifecycle()
                        val latestRelationshipsByChat by viewModel.latestRelationshipsByChat.collectAsStateWithLifecycle()
                        val connectivityWarning by viewModel.connectivityWarning.collectAsStateWithLifecycle()
                        val returnedFailedMessageText by viewModel.returnedFailedMessageText.collectAsStateWithLifecycle()
                        val allComics by viewModel.allComics.collectAsStateWithLifecycle()

                        val isVaultHub = userSettings.uiVariant == "vault_hub"
                        val defaultStartPage = if (isVaultHub && userSettings.vaultHubStartTab == "chats") 1 else 0
                        val tabController = rememberTabPagerController(defaultStartPage)
                        LaunchedEffect(userSettings.uiVariant) {
                            val startPage = if (userSettings.uiVariant == "vault_hub" && userSettings.vaultHubStartTab == "chats") 1 else 0
                            tabController.switchTo(startPage, "instant", 0)
                        }
                        var isStoreOverlayOpen by rememberSaveable { mutableStateOf(false) }

                        val switchToPagerTab: (Int) -> Unit = { targetPage ->
                            tabController.switchTo(
                                targetPage = targetPage,
                                style = userSettings.tabSwitchAnimation,
                                durationMs = userSettings.tabSwitchDurationMs
                            )
                        }

                        val navigateBackPagerTab: () -> Unit = {
                            val homePage = if (isVaultHub && userSettings.vaultHubStartTab == "chats") 1 else 0
                            tabController.navigateBack(
                                homePage = homePage,
                                style = userSettings.tabSwitchAnimation,
                                durationMs = userSettings.tabSwitchDurationMs
                            )
                        }

                        val safePopBackStack: () -> Unit = {
                            val currentRouteNow = navController.currentBackStackEntry?.destination?.route
                            if (currentRouteNow != null && currentRouteNow != "characters" && navController.previousBackStackEntry != null) {
                                navController.popBackStack(currentRouteNow, inclusive = true)
                            }
                        }

                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route ?: "characters"
                        val isChatOverlayOpen = activeChatId != null && currentRoute == "characters"

                        var isCharacterEditorOpen by rememberSaveable { mutableStateOf(false) }
                        var editingCharacterId by rememberSaveable { mutableStateOf(0L) }

                        // Seamless sub-screens inside active Chat ("settings" or "relationship_history")
                        var activeChatSubScreen by rememberSaveable { mutableStateOf<String?>(null) }
                        var lastChatSubScreen by rememberSaveable { mutableStateOf<String?>(null) }
                        if (activeChatSubScreen != null) {
                            lastChatSubScreen = activeChatSubScreen
                        }
                        LaunchedEffect(activeChatId) {
                            if (activeChatId == null) {
                                activeChatSubScreen = null
                            }
                        }

                        var isChatSubMounted by remember { mutableStateOf(activeChatSubScreen != null) }
                        val chatSubTransitionProgress = remember { Animatable(if (activeChatSubScreen != null) 1f else 0f) }
                        LaunchedEffect(activeChatSubScreen, userSettings.tabSwitchAnimation, userSettings.tabSwitchDurationMs) {
                            val targetVal = if (activeChatSubScreen != null) 1f else 0f
                            if (activeChatSubScreen != null) {
                                isChatSubMounted = true
                            }
                            if (userSettings.tabSwitchAnimation == "instant" || userSettings.tabSwitchDurationMs <= 0) {
                                chatSubTransitionProgress.snapTo(targetVal)
                            } else {
                                if (chatSubTransitionProgress.targetValue != targetVal) {
                                    chatSubTransitionProgress.animateTo(
                                        targetValue = targetVal,
                                        animationSpec = tween(
                                            durationMillis = userSettings.tabSwitchDurationMs.coerceIn(60, 1200),
                                            easing = TabTransitionHelper.easingForStyle(userSettings.tabSwitchAnimation)
                                        )
                                    )
                                }
                            }
                            if (activeChatSubScreen == null) {
                                isChatSubMounted = false
                            }
                        }
                        val isChatSubVisibleOrAnimating = (isChatSubMounted || activeChatSubScreen != null) && isChatOverlayOpen

                        var isChatMounted by remember { mutableStateOf(isChatOverlayOpen) }

                        // Smooth hardware-accelerated animated transition between tabs/screens and Chat
                        val chatTransitionProgress = remember { Animatable(if (isChatOverlayOpen) 1f else 0f) }
                        LaunchedEffect(isChatOverlayOpen, userSettings.tabSwitchAnimation, userSettings.tabSwitchDurationMs) {
                            val targetVal = if (isChatOverlayOpen) 1f else 0f
                            if (isChatOverlayOpen) {
                                isChatMounted = true
                            }
                            if (userSettings.tabSwitchAnimation == "instant" || userSettings.tabSwitchDurationMs <= 0) {
                                chatTransitionProgress.snapTo(targetVal)
                            } else {
                                if (chatTransitionProgress.targetValue != targetVal) {
                                    chatTransitionProgress.animateTo(
                                        targetValue = targetVal,
                                        animationSpec = tween(
                                            durationMillis = userSettings.tabSwitchDurationMs.coerceIn(60, 1200),
                                            easing = TabTransitionHelper.easingForStyle(userSettings.tabSwitchAnimation)
                                        )
                                    )
                                }
                            }
                            if (!isChatOverlayOpen) {
                                isChatMounted = false
                            }
                        }
                        val isHalloweenActive by viewModel.isHalloweenEventActive.collectAsStateWithLifecycle()
                        val isChatVisibleOrAnimating = isChatMounted || isChatOverlayOpen
                        var isVerityGameOpen by rememberSaveable { mutableStateOf(false) }
                        var isSkibidiGameOpen by rememberSaveable { mutableStateOf(false) }
                        val isVerityHotbarEnabled = (userSettings.verityUnlocked || userSettings.easterEggCode.trim().removePrefix("#").equals("VERITY", ignoreCase = true)) && userSettings.showVerityInHotbar
                        val showHotbar = currentRoute == "characters" && !isStoreOverlayOpen && !isCharacterEditorOpen && !isChatOverlayOpen && userSettings.isHotbarVisible

                        if (isVerityGameOpen) {
                            com.example.ui.components.VerityCareGameDialog(
                                settings = userSettings,
                                onUpdateSettings = { newSettings -> viewModel.updateUserSettings(newSettings) },
                                onDismiss = { isVerityGameOpen = false }
                            )
                        }

                        if (isSkibidiGameOpen) {
                            com.example.ui.components.Skibidi3DForestDialog(
                                settings = userSettings,
                                onUpdateSettings = { newSettings -> viewModel.updateUserSettings(newSettings) },
                                onDismiss = { isSkibidiGameOpen = false }
                            )
                        }

                        val perfMode = userSettings.animationPerformanceMode

                        Box(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = "characters",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val p = chatTransitionProgress.value
                                        if (p > 0f && currentRoute == "characters") {
                                            with(TabTransitionHelper) {
                                                applyTabLayerTransform(
                                                    style = userSettings.tabSwitchAnimation,
                                                    progress = p,
                                                    isIncoming = false,
                                                    direction = 1,
                                                    width = size.width,
                                                    height = size.height
                                                )
                                            }
                                        }
                                    },
                                enterTransition = {
                                    TabTransitionHelper.buildEnterTransition(
                                        style = userSettings.tabSwitchAnimation,
                                        durationMs = userSettings.tabSwitchDurationMs,
                                        forward = true
                                    )
                                },
                                exitTransition = {
                                    TabTransitionHelper.buildExitTransition(
                                        style = userSettings.tabSwitchAnimation,
                                        durationMs = userSettings.tabSwitchDurationMs,
                                        forward = true
                                    )
                                },
                                popEnterTransition = {
                                    TabTransitionHelper.buildEnterTransition(
                                        style = userSettings.tabSwitchAnimation,
                                        durationMs = userSettings.tabSwitchDurationMs,
                                        forward = false
                                    )
                                },
                                popExitTransition = {
                                    TabTransitionHelper.buildExitTransition(
                                        style = userSettings.tabSwitchAnimation,
                                        durationMs = userSettings.tabSwitchDurationMs,
                                        forward = false
                                    )
                                }
                            ) {
                        // 1. Main Screen hosting all tabs preloaded in memory
                        composable("characters") {
                            val homePage = if (isVaultHub && userSettings.vaultHubStartTab == "chats") 1 else 0
                            IsolatedTabBackHandler(
                                controller = tabController,
                                homePage = homePage,
                                enabled = !isChatOverlayOpen && !isStoreOverlayOpen && !isCharacterEditorOpen,
                                onBack = navigateBackPagerTab
                            )
                            BackHandler(enabled = isCharacterEditorOpen && !isChatOverlayOpen) {
                                isCharacterEditorOpen = false
                            }

                            @Composable
                            fun RenderVaultHubPage(tabIndex: Int) {
                                if (tabIndex == 0) {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadStoreCatalog(force = false)
                                    }
                                }
                                VaultHubScreen(
                                    characters = characters,
                                    providers = providerProfiles,
                                    stories = allStories,
                                    chats = allChats,
                                    latestRelationshipsByChat = latestRelationshipsByChat,
                                    activePersona = activePersona,
                                    userSettings = userSettings,
                                    storeState = storeState,
                                    fixedTab = tabIndex,
                                    onTabChange = { targetTab ->
                                        switchToPagerTab(targetTab)
                                    },
                                    onRefreshStore = { viewModel.loadStoreCatalog(force = true) },
                                    onDownloadStoreBot = { bot, onSuccess, onError ->
                                        viewModel.downloadStoreBot(bot, onSuccess, onError)
                                    },
                                    onUpdateSettings = { newSettings -> viewModel.updateUserSettings(newSettings) },
                                    onCharacterClick = { charId ->
                                        viewModel.openChatForCharacter(charId) { chatId ->
                                            viewModel.setActiveChat(chatId)
                                        }
                                    },
                                    onSelectChat = { chatId ->
                                        viewModel.setActiveChat(chatId)
                                    },
                                    onCreateNewChatForCharacter = { charId, title, onCreated ->
                                        viewModel.createAdditionalChatForCharacter(charId, title, onCreated)
                                    },
                                    onTogglePinChat = { chatId, isPinned ->
                                        viewModel.setChatPinned(chatId, isPinned)
                                    },
                                    onToggleArchiveChat = { chatId, isArchived ->
                                        viewModel.setChatArchived(chatId, isArchived)
                                    },
                                    onRenameChat = { chatId, title ->
                                        viewModel.updateChatTitle(chatId, title)
                                    },
                                    onDeleteChat = { chatId ->
                                        viewModel.deleteChat(chatId) {}
                                    },
                                    onEditCharacter = { charId ->
                                        editingCharacterId = charId
                                        isCharacterEditorOpen = true
                                    },
                                    onCreateCharacter = {
                                        editingCharacterId = 0L
                                        isCharacterEditorOpen = true
                                    },
                                    onImportCharacterJson = { json ->
                                        viewModel.importCharacterFromJson(json)
                                    },
                                    onTogglePinCharacter = { charId, isPinned ->
                                        viewModel.setCharacterPinned(charId, isPinned)
                                    },
                                    onToggleArchiveCharacter = { charId, isArchived ->
                                        viewModel.setCharacterArchived(charId, isArchived)
                                    },
                                    onDeleteCharacter = { char ->
                                        viewModel.deleteCharacter(char) {}
                                    },
                                    onOpenProviders = {
                                        switchToPagerTab(3)
                                    },
                                    onOpenPersonas = {
                                        switchToPagerTab(2)
                                    },
                                    onOpenStore = {
                                        isStoreOverlayOpen = true
                                    },
                                    onOpenSettings = {
                                        switchToPagerTab(4)
                                    },
                                    onOpenBackup = {
                                        navController.navigate("backup") { launchSingleTop = true }
                                    },
                                    onSwitchToClassic = {
                                        viewModel.updateUserSettings(userSettings.copy(uiVariant = "classic"))
                                    }
                                )
                            }

                            PreloadedAnimatedTabs(
                                pageCount = if (isVaultHub) 5 else 4,
                                controller = tabController,
                                animationStyle = userSettings.tabSwitchAnimation,
                                durationMs = userSettings.tabSwitchDurationMs,
                                warmedUpCount = warmedTabCount,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                if (isVaultHub) {
                                    when (page) {
                                        0 -> RenderVaultHubPage(0)
                                        1 -> RenderVaultHubPage(1)
                                        2 -> {
                                            UserPersonaScreen(
                                                personas = personas,
                                                activePersona = activePersona,
                                                onSavePersona = { persona -> viewModel.savePersona(persona) },
                                                onSelectPersona = { personaId -> viewModel.selectPersona(personaId) },
                                                onDeletePersona = { persona -> viewModel.deletePersona(persona) },
                                                onBack = { navigateBackPagerTab() }
                                            )
                                        }
                                        3 -> {
                                            ProvidersScreen(
                                                profiles = providerProfiles,
                                                onSaveProfile = { profile -> viewModel.saveProvider(profile) },
                                                onDeleteProfile = { profile -> viewModel.deleteProvider(profile) },
                                                onSetDefaultText = { id -> viewModel.setDefaultTextProvider(id) },
                                                onSetDefaultImage = { id -> viewModel.setDefaultImageProvider(id) },
                                                onTestConnection = { profile -> viewModel.testProviderConnection(profile) },
                                                onFetchOpenRouterModels = { apiKey -> viewModel.fetchOpenRouterModels(apiKey) },
                                                onSearchCivitaiModels = { query, apiKey -> viewModel.searchCivitaiModels(query, apiKey) },
                                                onFetchRemoteModels = { type, base, key -> viewModel.fetchRemoteModels(type, base, key) },
                                                onBack = { navigateBackPagerTab() }
                                            )
                                        }
                                        4 -> {
                                            AppSettingsScreen(
                                                settings = userSettings,
                                                providers = providerProfiles,
                                                activePersona = activePersona,
                                                onOpenPersonas = { switchToPagerTab(2) },
                                                onUpdateSettings = { newSettings -> viewModel.updateUserSettings(newSettings) },
                                                onOpenBackup = { navController.navigate("backup") { launchSingleTop = true } },
                                                onOpenProviders = { switchToPagerTab(3) },
                                                onOpenStudioMode = { navController.navigate("studio_mode") { launchSingleTop = true } },
                                                onOpenGeminiDebug = { navController.navigate("gemini_debug") { launchSingleTop = true } },
                                                onOpenVerityGame = { isVerityGameOpen = true },
                                                onOpenSkibidiGame = { isSkibidiGameOpen = true },
                                                onBack = { navigateBackPagerTab() }
                                            )
                                        }
                                    }
                                } else {
                                    when (page) {
                                        0 -> {
                                            CharacterListScreen(
                                                characters = characters,
                                                providers = providerProfiles,
                                                stories = allStories,
                                                chats = allChats,
                                                latestRelationshipsByChat = latestRelationshipsByChat,
                                                onCharacterClick = { charId ->
                                                    viewModel.openChatForCharacter(charId) { chatId ->
                                                        viewModel.setActiveChat(chatId)
                                                    }
                                                },
                                                onSelectChat = { chatId ->
                                                    viewModel.setActiveChat(chatId)
                                                },
                                                onCreateNewChatForCharacter = { charId, title, onCreated ->
                                                    viewModel.createAdditionalChatForCharacter(charId, title, onCreated)
                                                },
                                                onTogglePinChat = { chatId, isPinned ->
                                                    viewModel.setChatPinned(chatId, isPinned)
                                                },
                                                onToggleArchiveChat = { chatId, isArchived ->
                                                    viewModel.setChatArchived(chatId, isArchived)
                                                },
                                                onRenameChat = { chatId, title ->
                                                    viewModel.updateChatTitle(chatId, title)
                                                },
                                                onDeleteChat = { chatId ->
                                                    viewModel.deleteChat(chatId) {}
                                                },
                                                onTogglePinCharacter = { charId, isPinned ->
                                                    viewModel.setCharacterPinned(charId, isPinned)
                                                },
                                                onToggleArchiveCharacter = { charId, isArchived ->
                                                    viewModel.setCharacterArchived(charId, isArchived)
                                                },
                                                onToggleHideCharacter = { charId, isHidden ->
                                                    viewModel.setCharacterHidden(charId, isHidden)
                                                },
                                                onEditCharacter = { charId ->
                                                    editingCharacterId = charId
                                                    isCharacterEditorOpen = true
                                                },
                                                onCreateCharacter = {
                                                    editingCharacterId = 0L
                                                    isCharacterEditorOpen = true
                                                },
                                                onImportCharacterJson = { json ->
                                                    viewModel.importCharacterFromJson(json)
                                                },
                                                onDeleteCharacter = { char ->
                                                    viewModel.deleteCharacter(char) {}
                                                },
                                                onSaveStory = { story ->
                                                    viewModel.saveStory(story)
                                                },
                                                onDeleteStory = { story ->
                                                    viewModel.deleteStory(story)
                                                },
                                                onTogglePinStory = { storyId, isPinned ->
                                                    viewModel.setStoryPinned(storyId, isPinned)
                                                },
                                                onToggleArchiveStory = { storyId, isArchived ->
                                                    viewModel.setStoryArchived(storyId, isArchived)
                                                },
                                                onQuickSaveKey = { key ->
                                                    viewModel.quickSaveOpenRouterKey(key)
                                                },
                                                onOpenProviders = {
                                                    switchToPagerTab(2)
                                                },
                                                onOpenSettings = {
                                                    switchToPagerTab(3)
                                                },
                                                onOpenBackup = {
                                                    navController.navigate("backup") { launchSingleTop = true }
                                                },
                                                onOpenPersonas = {
                                                    switchToPagerTab(1)
                                                },
                                                onOpenStore = {
                                                    isStoreOverlayOpen = true
                                                },
                                                onCheckUpdates = {
                                                    viewModel.checkForAppUpdates(manual = true)
                                                }
                                            )
                                        }
                                        1 -> {
                                            UserPersonaScreen(
                                                personas = personas,
                                                activePersona = activePersona,
                                                onSavePersona = { persona -> viewModel.savePersona(persona) },
                                                onSelectPersona = { personaId -> viewModel.selectPersona(personaId) },
                                                onDeletePersona = { persona -> viewModel.deletePersona(persona) },
                                                onBack = { navigateBackPagerTab() }
                                            )
                                        }
                                        2 -> {
                                            ProvidersScreen(
                                                profiles = providerProfiles,
                                                onSaveProfile = { profile -> viewModel.saveProvider(profile) },
                                                onDeleteProfile = { profile -> viewModel.deleteProvider(profile) },
                                                onSetDefaultText = { id -> viewModel.setDefaultTextProvider(id) },
                                                onSetDefaultImage = { id -> viewModel.setDefaultImageProvider(id) },
                                                onTestConnection = { profile -> viewModel.testProviderConnection(profile) },
                                                onFetchOpenRouterModels = { apiKey -> viewModel.fetchOpenRouterModels(apiKey) },
                                                onSearchCivitaiModels = { query, apiKey -> viewModel.searchCivitaiModels(query, apiKey) },
                                                onFetchRemoteModels = { type, base, key -> viewModel.fetchRemoteModels(type, base, key) },
                                                onBack = { navigateBackPagerTab() }
                                            )
                                        }
                                        3 -> {
                                            AppSettingsScreen(
                                                settings = userSettings,
                                                providers = providerProfiles,
                                                activePersona = activePersona,
                                                onOpenPersonas = { switchToPagerTab(1) },
                                                onUpdateSettings = { newSettings -> viewModel.updateUserSettings(newSettings) },
                                                onOpenBackup = { navController.navigate("backup") { launchSingleTop = true } },
                                                onOpenProviders = { switchToPagerTab(2) },
                                                onOpenStudioMode = { navController.navigate("studio_mode") { launchSingleTop = true } },
                                                onOpenGeminiDebug = { navController.navigate("gemini_debug") { launchSingleTop = true } },
                                                onOpenVerityGame = { isVerityGameOpen = true },
                                                onOpenSkibidiGame = { isSkibidiGameOpen = true },
                                                onBack = { navigateBackPagerTab() }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 1.1 Store Screen
                        composable(
                            route = "box",
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) {
                            LaunchedEffect(Unit) {
                                isStoreOverlayOpen = true
                                safePopBackStack()
                            }
                        }

                        // 2. Character Editor
                        composable(
                            route = "character_editor/{characterId}",
                            arguments = listOf(navArgument("characterId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val charId = backStackEntry.arguments?.getLong("characterId") ?: 0L
                            val characterToEdit = remember(charId, characters) {
                                if (charId != 0L) characters.find { it.id == charId } else null
                            }
                            val maxLimit = 50000 // Лимит до 50 000 символов в боте

                            CharacterEditorScreen(
                                character = characterToEdit,
                                maxCharacterLimit = maxLimit,
                                activePersona = activePersona,
                                userFramesJson = userSettings.userFramesJson,
                                onSaveFramesJson = { viewModel.saveUserFramesJson(it) },
                                customRelationshipTypesJson = userSettings.customRelationshipTypesJson,
                                onSaveCustomRelationshipTypesJson = { newCustomJson ->
                                    viewModel.updateUserSettings(userSettings.copy(customRelationshipTypesJson = newCustomJson))
                                },
                                onGenerateBotFromPrompt = { wishes, onDone, onError ->
                                    viewModel.generateBotFromPrompt(
                                        userWishes = wishes,
                                        chatIdForLog = null,
                                        saveToDatabase = false,
                                        onSuccess = { draft, _ -> onDone(draft) },
                                        onError = onError
                                    )
                                },
                                onAssistBotInEditor = { payload, onDone, onError ->
                                    viewModel.assistBotInEditor(
                                        promptPayload = payload,
                                        onSuccess = onDone,
                                        onError = onError
                                    )
                                },
                                onSave = { saved ->
                                    viewModel.saveCharacter(saved) {
                                        safePopBackStack()
                                    }
                                },
                                onDelete = if (characterToEdit != null) {
                                    { toDelete ->
                                        viewModel.deleteCharacter(toDelete) {
                                            safePopBackStack()
                                        }
                                    }
                                } else null,
                                onBack = { safePopBackStack() }
                            )
                        }

                        // 3. Legacy Chat Route redirector -> redirects immediately to the single persistent preloaded chat screen
                        composable(
                            route = "chat/{chatId}",
                            arguments = listOf(navArgument("chatId") { type = NavType.LongType }),
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
                            LaunchedEffect(chatId) {
                                if (chatId > 0L) {
                                    viewModel.setActiveChat(chatId)
                                }
                                navController.popBackStack("characters", inclusive = false)
                            }
                        }

                        // 4. Chat Settings redirector -> opens seamless overlay
                        composable(
                            route = "chat_settings/{chatId}",
                            arguments = listOf(navArgument("chatId") { type = NavType.LongType }),
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
                            LaunchedEffect(chatId) {
                                if (chatId > 0L) {
                                    viewModel.setActiveChat(chatId)
                                }
                                activeChatSubScreen = "settings"
                                navController.popBackStack("characters", inclusive = false)
                            }
                        }

                        // 5. Relationship History redirector -> opens seamless overlay
                        composable(
                            route = "relationship_history/{chatId}",
                            arguments = listOf(navArgument("chatId") { type = NavType.LongType }),
                            enterTransition = { EnterTransition.None },
                            exitTransition = { ExitTransition.None },
                            popEnterTransition = { EnterTransition.None },
                            popExitTransition = { ExitTransition.None }
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getLong("chatId") ?: 0L
                            LaunchedEffect(chatId) {
                                if (chatId > 0L) {
                                    viewModel.setActiveChat(chatId)
                                }
                                activeChatSubScreen = "relationship_history"
                                navController.popBackStack("characters", inclusive = false)
                            }
                        }

                        // 6. Providers Screen
                        composable("providers") {
                            ProvidersScreen(
                                profiles = providerProfiles,
                                onSaveProfile = { profile -> viewModel.saveProvider(profile) },
                                onDeleteProfile = { profile -> viewModel.deleteProvider(profile) },
                                onSetDefaultText = { id -> viewModel.setDefaultTextProvider(id) },
                                onSetDefaultImage = { id -> viewModel.setDefaultImageProvider(id) },
                                onTestConnection = { profile -> viewModel.testProviderConnection(profile) },
                                onFetchOpenRouterModels = { apiKey -> viewModel.fetchOpenRouterModels(apiKey) },
                                onSearchCivitaiModels = { query, apiKey -> viewModel.searchCivitaiModels(query, apiKey) },
                                onFetchRemoteModels = { type, base, key -> viewModel.fetchRemoteModels(type, base, key) },
                                onBack = { safePopBackStack() }
                            )
                        }

                        // 7. App Settings Screen
                        composable("settings") {
                            AppSettingsScreen(
                                settings = userSettings,
                                providers = providerProfiles,
                                activePersona = activePersona,
                                onOpenPersonas = { navController.navigate("user_persona") { launchSingleTop = true } },
                                onUpdateSettings = { newSettings -> viewModel.updateUserSettings(newSettings) },
                                onOpenBackup = { navController.navigate("backup") { launchSingleTop = true } },
                                onOpenProviders = { navController.navigate("providers") { launchSingleTop = true } },
                                onOpenStudioMode = { navController.navigate("studio_mode") { launchSingleTop = true } },
                                onOpenGeminiDebug = { navController.navigate("gemini_debug") { launchSingleTop = true } },
                                onOpenVerityGame = { isVerityGameOpen = true },
                                onOpenSkibidiGame = { isSkibidiGameOpen = true },
                                onBack = { safePopBackStack() }
                            )
                        }

                        // 7.1 Gemini Debug Screen
                        composable("gemini_debug") {
                            GeminiDebugScreen(
                                geminiProfiles = providerProfiles,
                                onBack = { safePopBackStack() }
                            )
                        }

                        // 8. Studio Mode Screen (Layout Customization)
                        composable("studio_mode") {
                            StudioModeScreen(
                                accentColor = accentColor,
                                userSettings = userSettings,
                                characters = characters,
                                providerProfiles = providerProfiles,
                                personas = personas,
                                activePersona = activePersona,
                                allStories = allStories,
                                allChats = allChats,
                                onBack = { safePopBackStack() }
                            )
                        }

                        // 9. Backup Screen
                        composable("backup") {
                            BackupScreen(
                                onExportBackup = { uri, options ->
                                    viewModel.exportBackup(uri, options)
                                },
                                onInspectBackup = { uri ->
                                    viewModel.inspectBackup(uri)
                                },
                                onImportBackup = { uri, replaceAll ->
                                    viewModel.importBackup(uri, replaceAll)
                                },
                                onBack = { safePopBackStack() }
                            )
                        }

                        // 10. User Personas Screen
                        composable("personas") {
                            UserPersonaScreen(
                                personas = personas,
                                activePersona = activePersona,
                                onSavePersona = { persona ->
                                    viewModel.savePersona(persona)
                                },
                                onSelectPersona = { personaId ->
                                    viewModel.selectPersona(personaId)
                                },
                                onDeletePersona = { persona ->
                                    viewModel.deletePersona(persona)
                                },
                                 onBack = { safePopBackStack() }
                            )
                        }
                      }

                      AnimatedVisibility(
                          visible = isStoreOverlayOpen && currentRoute == "characters" && !isChatOverlayOpen,
                          enter = TabTransitionHelper.buildEnterTransition(
                              style = userSettings.tabSwitchAnimation,
                              durationMs = userSettings.tabSwitchDurationMs,
                              forward = true
                          ),
                          exit = TabTransitionHelper.buildExitTransition(
                              style = userSettings.tabSwitchAnimation,
                              durationMs = userSettings.tabSwitchDurationMs,
                              forward = false
                          ),
                          modifier = Modifier
                              .fillMaxSize()
                              .zIndex(1.5f)
                      ) {
                          Box(
                              modifier = Modifier
                                  .fillMaxSize()
                                  .background(MaterialTheme.colorScheme.background)
                                  .clickable(
                                      interactionSource = remember { MutableInteractionSource() },
                                      indication = null,
                                      onClick = {}
                                  )
                          ) {
                              BoxStoreScreen(
                                  viewModel = viewModel,
                                  onNavigateToChat = { chatId ->
                                      isStoreOverlayOpen = false
                                      viewModel.setActiveChat(chatId)
                                  },
                                  onBack = if (isStoreOverlayOpen) {
                                      { isStoreOverlayOpen = false }
                                  } else null
                              )
                          }
                      }

                      AnimatedVisibility(
                          visible = isCharacterEditorOpen && currentRoute == "characters" && !isChatOverlayOpen,
                          enter = TabTransitionHelper.buildEnterTransition(
                              style = userSettings.tabSwitchAnimation,
                              durationMs = userSettings.tabSwitchDurationMs,
                              forward = true
                          ),
                          exit = TabTransitionHelper.buildExitTransition(
                              style = userSettings.tabSwitchAnimation,
                              durationMs = userSettings.tabSwitchDurationMs,
                              forward = false
                          ),
                          modifier = Modifier
                              .fillMaxSize()
                              .zIndex(1.6f)
                      ) {
                          val charId = editingCharacterId
                          val characterToEdit = remember(charId, characters) {
                              if (charId != 0L) characters.find { it.id == charId } else null
                          }
                          Box(
                              modifier = Modifier
                                  .fillMaxSize()
                                  .background(MaterialTheme.colorScheme.background)
                                  .clickable(
                                      interactionSource = remember { MutableInteractionSource() },
                                      indication = null,
                                      onClick = {}
                                  )
                          ) {
                              CharacterEditorScreen(
                                  character = characterToEdit,
                                  maxCharacterLimit = 50000,
                                  activePersona = activePersona,
                                  userFramesJson = userSettings.userFramesJson,
                                  onSaveFramesJson = { viewModel.saveUserFramesJson(it) },
                                  customRelationshipTypesJson = userSettings.customRelationshipTypesJson,
                                  onSaveCustomRelationshipTypesJson = { newCustomJson ->
                                      viewModel.updateUserSettings(userSettings.copy(customRelationshipTypesJson = newCustomJson))
                                  },
                                  onGenerateBotFromPrompt = { wishes, onDone, onError ->
                                      viewModel.generateBotFromPrompt(
                                          userWishes = wishes,
                                          chatIdForLog = null,
                                          saveToDatabase = false,
                                          onSuccess = { draft, _ -> onDone(draft) },
                                          onError = onError
                                      )
                                  },
                                  onAssistBotInEditor = { payload, onDone, onError ->
                                      viewModel.assistBotInEditor(
                                          promptPayload = payload,
                                          onSuccess = onDone,
                                          onError = onError
                                      )
                                  },
                                  onSave = { saved ->
                                      viewModel.saveCharacter(saved) {
                                          isCharacterEditorOpen = false
                                      }
                                  },
                                  onDelete = if (characterToEdit != null) {
                                      { toDelete ->
                                          viewModel.deleteCharacter(toDelete) {
                                              isCharacterEditorOpen = false
                                          }
                                      }
                                  } else null,
                                  onBack = { isCharacterEditorOpen = false }
                              )
                          }
                      }

                      // SINGLE PRE-MOUNTED CHAT SCREEN (kept alive on the same screen, never recreated on entry/exit)
                      val hostChatId = activeChatId ?: 0L
                      val resolvedChat = remember(hostChatId, activeChat, allChats) {
                          if (hostChatId > 0L) {
                              (if (activeChat?.id == hostChatId) activeChat else null)
                                  ?: allChats.find { it.id == hostChatId }
                          } else null
                      }
                      val resolvedChar = remember(resolvedChat?.characterId, activeCharacter, characters) {
                          if (resolvedChat != null) {
                              (if (activeCharacter?.id == resolvedChat.characterId) activeCharacter else null)
                                  ?: characters.find { it.id == resolvedChat.characterId }
                          } else null
                      }

                      var lastRenderedChat by remember { mutableStateOf<ChatEntity?>(null) }
                      var lastRenderedChar by remember { mutableStateOf<CharacterEntity?>(null) }
                      if (resolvedChat != null) lastRenderedChat = resolvedChat
                      if (resolvedChar != null) lastRenderedChar = resolvedChar

                      val displayChat = resolvedChat ?: lastRenderedChat
                      val displayChar = resolvedChar ?: lastRenderedChar
                      val emptyShellChat = remember { ChatEntity(id = 0L, characterId = 0L, title = "") }
                      val emptyShellChar = remember { CharacterEntity(id = 0L, name = "", avatarUri = "") }
                      val isReadyActiveChat = isChatVisibleOrAnimating && displayChat != null && displayChar != null

                      val allChatsForChar by remember(displayChar?.id) {
                          if (displayChar != null && displayChar.id > 0L) viewModel.getChatsForCharacter(displayChar.id)
                          else flowOf(emptyList())
                      }.collectAsStateWithLifecycle(initialValue = emptyList())

                      var isGeneratingComic by remember { mutableStateOf(false) }
                      var comicProgressStatus by remember { mutableStateOf("") }
                      var comicErrorMessage by remember { mutableStateOf<String?>(null) }
                      var activeComicToView by remember { mutableStateOf<ComicEntity?>(null) }
                      var showComicsGallery by remember { mutableStateOf(false) }

                      if (isReadyActiveChat && isGeneratingComic) {
                          ComicProgressDialog(
                              statusText = comicProgressStatus,
                              onCancel = { isGeneratingComic = false }
                          )
                      }

                      if (isReadyActiveChat && comicErrorMessage != null) {
                          ComicErrorDialog(
                              errorMessage = comicErrorMessage ?: "",
                              onDismiss = { comicErrorMessage = null }
                          )
                      }

                      if (isReadyActiveChat && activeComicToView != null) {
                          ComicViewerDialog(
                              comic = activeComicToView!!,
                              onDismiss = { activeComicToView = null },
                              onDelete = {
                                  viewModel.deleteComic(activeComicToView!!)
                                  activeComicToView = null
                              }
                          )
                      }

                      if (isReadyActiveChat && showComicsGallery) {
                          val chatComics = remember(allComics, hostChatId) {
                              allComics.filter { it.chatId == hostChatId }
                          }
                          ComicsGalleryDialog(
                              comics = chatComics.ifEmpty { allComics },
                              onDismiss = { showComicsGallery = false },
                              onSelectComic = { comic ->
                                  showComicsGallery = false
                                  activeComicToView = comic
                              },
                              onDeleteComic = { comic ->
                                  viewModel.deleteComic(comic)
                              }
                          )
                      }

                      Box(
                          modifier = Modifier
                              .fillMaxSize()
                              .zIndex(if (isChatVisibleOrAnimating) 2f else -1f)
                              .graphicsLayer {
                                  val p = chatTransitionProgress.value
                                  val subP = chatSubTransitionProgress.value
                                  if (p <= 0f) {
                                      alpha = 0f
                                      translationX = size.width * 10f
                                  } else if (subP > 0f) {
                                      with(TabTransitionHelper) {
                                          applyTabLayerTransform(
                                              style = userSettings.tabSwitchAnimation,
                                              progress = subP,
                                              isIncoming = false,
                                              direction = 1,
                                              width = size.width,
                                              height = size.height
                                          )
                                      }
                                  } else {
                                      with(TabTransitionHelper) {
                                          applyTabLayerTransform(
                                              style = userSettings.tabSwitchAnimation,
                                              progress = p,
                                              isIncoming = true,
                                              direction = 1,
                                              width = size.width,
                                              height = size.height
                                          )
                                      }
                                  }
                              }
                              .then(
                                  if (isChatVisibleOrAnimating) {
                                      Modifier
                                          .background(MaterialTheme.colorScheme.background)
                                          .clickable(
                                              interactionSource = remember { MutableInteractionSource() },
                                              indication = null,
                                              onClick = {}
                                          )
                                  } else {
                                      Modifier
                                  }
                              )
                      ) {
                          val streamingMessageByChat by viewModel.streamingMessageByChat.collectAsStateWithLifecycle()
                          val resolvedInitialDraft = remember(hostChatId) {
                              viewModel.getDraftSync(hostChatId, displayChat?.draftText ?: "")
                          }
                          ChatScreen(
                              character = displayChar ?: emptyShellChar,
                              chat = displayChat ?: emptyShellChat,
                              allPersonas = personas,
                              activePersona = activePersona,
                              isHalloweenActive = isHalloweenActive,
                              initialDraftText = resolvedInitialDraft,
                              onChatInputTopYChanged = { yPx -> viewModel.updateChatInputTopYPx(yPx) },
                              returnedFailedMessageText = if (returnedFailedMessageText?.first == hostChatId) returnedFailedMessageText?.second else null,
                              onConsumedReturnedMessage = {
                                  viewModel.clearReturnedFailedMessageText()
                              },
                              allChatsForCharacter = allChatsForChar,
                              messages = activeMessages,
                              summaries = activeSummaries,
                              latestRelationship = latestRelationship,
                              userSettings = userSettings,
                              providerProfiles = providerProfiles,
                              isGeneratingResponse = generatingChatIds.contains(hostChatId),
                              streamingMessage = streamingMessageByChat[hostChatId],
                              isPreloadedShell = !isChatOverlayOpen,
                              onStopGeneration = {
                                  if (hostChatId > 0L) viewModel.stopGeneration(hostChatId)
                              },
                              onSelectChatPersona = { personaId ->
                                  if (hostChatId > 0L) viewModel.setChatPersona(hostChatId, personaId)
                              },
                              onDraftChange = { draft ->
                                  if (hostChatId > 0L) viewModel.setDraft(hostChatId, draft)
                              },
                              onUpdateChatNotes = { targetChatId, notes ->
                                  viewModel.updateChatNotes(targetChatId, notes)
                              },
                              onSendMessage = { text, imgUri ->
                                  val c = resolvedChat ?: return@ChatScreen
                                  viewModel.sendMessageOptimized(
                                      chatId = c.id,
                                      text = text,
                                      isTextingMode = c.isTextingMode,
                                      imageUrl = imgUri,
                                      onAiStateChange = {},
                                      onError = { err ->
                                          Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                                      }
                                  )
                              },
                              onSendVoiceMessage = { recognizedText, audioPath, durationSec ->
                                  val c = resolvedChat ?: return@ChatScreen
                                  viewModel.sendMessageOptimized(
                                      chatId = c.id,
                                      text = recognizedText,
                                      isTextingMode = c.isTextingMode,
                                      audioFilePath = audioPath,
                                      audioDurationSec = durationSec,
                                      onAiStateChange = {},
                                      onError = { err ->
                                          Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                                      }
                                  )
                              },
                              onRegenerateVariant = { msgId ->
                                  if (hostChatId > 0L) {
                                      viewModel.regenerateAiResponseVariant(
                                          chatId = hostChatId,
                                          messageId = msgId,
                                          onAiStateChange = {},
                                          onError = { err ->
                                              Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                                          }
                                      )
                                  }
                              },
                              onSwitchVariant = { msgId, idx ->
                                  viewModel.switchMessageVariant(msgId, idx)
                              },
                              onEditUserMessageAndRegenerate = { msgId, newText ->
                                  if (hostChatId > 0L) {
                                      viewModel.editUserMessageAndRegenerate(
                                          chatId = hostChatId,
                                          messageId = msgId,
                                          newText = newText,
                                          onAiStateChange = {},
                                          onError = { err ->
                                              Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                                          }
                                      )
                                  }
                              },
                              onEditMessage = { msgId, newText ->
                                  viewModel.editMessage(msgId, newText)
                              },
                              onDeleteMessage = { msgId ->
                                  viewModel.deleteMessage(msgId)
                              },
                              onSetReaction = { msgId, reaction ->
                                  viewModel.setMessageReaction(msgId, reaction)
                              },
                              onBranchChat = { msgId ->
                                  if (hostChatId > 0L) {
                                      viewModel.branchChat(hostChatId, msgId) { newChatId ->
                                          viewModel.setActiveChat(newChatId)
                                          Toast.makeText(this@MainActivity, "Ветка диалога создана", Toast.LENGTH_SHORT).show()
                                      }
                                  }
                              },
                              onClearChat = {
                                  if (hostChatId > 0L) viewModel.clearChat(hostChatId)
                              },
                              onToggleTextingMode = { enabled ->
                                  if (hostChatId > 0L) viewModel.toggleTextingMode(hostChatId, enabled)
                              },
                              onSwitchChat = { newChatId ->
                                  viewModel.setActiveChat(newChatId)
                              },
                              onCreateNewChat = { title ->
                                  val charId = resolvedChar?.id ?: return@ChatScreen
                                  viewModel.createAdditionalChatForCharacter(
                                      characterId = charId,
                                      title = title
                                  ) { createdId ->
                                      viewModel.setActiveChat(createdId)
                                  }
                              },
                              onTogglePinChat = { targetChatId, isPinned ->
                                  viewModel.setChatPinned(targetChatId, isPinned)
                              },
                              onToggleArchiveChat = { targetChatId, isArchived ->
                                  viewModel.setChatArchived(targetChatId, isArchived)
                              },
                              onRenameChat = { targetChatId, title ->
                                  viewModel.updateChatTitle(targetChatId, title)
                              },
                              onDeleteChat = { targetChatId ->
                                  viewModel.deleteChat(targetChatId) {}
                              },
                              onGenerateComic = { chapterTitle, chapterSummaryText, style, panelCount, selectedProviderId, civitaiModelAir, customPrompt, negativePrompt ->
                                  val charId = resolvedChar?.id ?: return@ChatScreen
                                  isGeneratingComic = true
                                  comicProgressStatus = "Инициализация генерации комикса..."
                                  comicErrorMessage = null
                                  viewModel.generateComic(
                                      chatId = hostChatId,
                                      characterId = charId,
                                      chapterTitle = chapterTitle,
                                      style = style,
                                      chapterSummaryText = chapterSummaryText,
                                      panelCount = panelCount,
                                      selectedImageProfileId = selectedProviderId,
                                      civitaiModelAir = civitaiModelAir,
                                      customPrompt = customPrompt,
                                      negativePrompt = negativePrompt,
                                      onProgress = { status ->
                                          comicProgressStatus = status
                                      },
                                      onComplete = { comic ->
                                          isGeneratingComic = false
                                          activeComicToView = comic
                                          Toast.makeText(this@MainActivity, "Комикс «${comic.title}» готов!", Toast.LENGTH_SHORT).show()
                                      },
                                      onError = { err ->
                                          isGeneratingComic = false
                                          comicErrorMessage = err
                                      }
                                  )
                              },
                              onOpenComicsGallery = {
                                  showComicsGallery = true
                              },
                              onGeneratePhoto = { prompt, includeAppearance, style, resolution, profileId, engineMode ->
                                  if (hostChatId > 0L) {
                                      viewModel.generateImageForChat(
                                          chatId = hostChatId,
                                          prompt = prompt,
                                          includeAppearance = includeAppearance,
                                          style = style,
                                          resolution = resolution,
                                          providerProfileId = profileId,
                                          engineMode = engineMode,
                                          onComplete = {
                                              val successMsg = if (userSettings.appLanguage.equals("en", true)) "Artwork generated successfully!" else "Фото успешно создано!"
                                              Toast.makeText(this@MainActivity, successMsg, Toast.LENGTH_SHORT).show()
                                          },
                                          onError = { err ->
                                              Toast.makeText(this@MainActivity, err, Toast.LENGTH_LONG).show()
                                          }
                                      )
                                  }
                              },
                              onOpenChatSettings = {
                                  if (hostChatId > 0L) activeChatSubScreen = "settings"
                              },
                              onOpenRelationshipHistory = {
                                  if (hostChatId > 0L) activeChatSubScreen = "relationship_history"
                              },
                              onAssistantCreateBot = { wishes, onDone, onError ->
                                  viewModel.generateBotFromPrompt(
                                      userWishes = wishes,
                                      chatIdForLog = hostChatId,
                                      saveToDatabase = true,
                                      onSuccess = { _, savedBot ->
                                          onDone()
                                          if (savedBot != null) {
                                              Toast.makeText(
                                                  this@MainActivity,
                                                  "Бот «${savedBot.name}» создан и поля заполнены!",
                                                  Toast.LENGTH_SHORT
                                              ).show()
                                              viewModel.clearActiveChat()
                                              editingCharacterId = savedBot.id
                                              isCharacterEditorOpen = true
                                          }
                                      },
                                      onError = onError
                                  )
                              },
                              onAssistantCreatePersona = { wishes, onDone, onError ->
                                  viewModel.generatePersonaFromPrompt(
                                      userWishes = wishes,
                                      chatIdForLog = hostChatId,
                                      onSuccess = { _, savedPersona ->
                                          onDone()
                                          Toast.makeText(
                                              this@MainActivity,
                                              "Персона «${savedPersona.name}» создана и поля заполнены!",
                                              Toast.LENGTH_SHORT
                                          ).show()
                                          viewModel.clearActiveChat()
                                          switchToPagerTab(1)
                                      },
                                      onError = onError
                                  )
                              },
                              onBack = {
                                  if (activeChatSubScreen != null) {
                                      activeChatSubScreen = null
                                  } else {
                                      viewModel.clearActiveChat()
                                  }
                              }
                          )
                      }

                      // SEAMLESS OVERLAY FOR CHAT SETTINGS AND RELATIONSHIP HISTORY
                      BackHandler(enabled = activeChatSubScreen != null && isChatOverlayOpen) {
                          activeChatSubScreen = null
                      }
                      Box(
                          modifier = Modifier
                              .fillMaxSize()
                              .zIndex(if (isChatSubVisibleOrAnimating) 2.5f else -2f)
                              .graphicsLayer {
                                  val subP = chatSubTransitionProgress.value
                                  if (subP <= 0f || !isChatOverlayOpen) {
                                      alpha = 0f
                                      translationX = size.width * 10f
                                  } else {
                                      with(TabTransitionHelper) {
                                          applyTabLayerTransform(
                                              style = userSettings.tabSwitchAnimation,
                                              progress = subP,
                                              isIncoming = true,
                                              direction = 1,
                                              width = size.width,
                                              height = size.height
                                          )
                                      }
                                  }
                              }
                              .then(
                                  if (isChatSubVisibleOrAnimating) {
                                      Modifier
                                          .background(MaterialTheme.colorScheme.background)
                                          .clickable(
                                              interactionSource = remember { MutableInteractionSource() },
                                              indication = null,
                                              onClick = {}
                                          )
                                  } else {
                                      Modifier
                                  }
                              )
                      ) {
                          val subMode = activeChatSubScreen ?: lastChatSubScreen ?: "settings"
                          val resolvedSubChat = displayChat ?: emptyShellChat
                          val resolvedSubChar = displayChar ?: emptyShellChar
                          if ((displayChat != null && displayChar != null && isChatSubVisibleOrAnimating) || preloadChatShells) {
                              when (subMode) {
                                  "settings" -> {
                                      ChatSettingsScreen(
                                          chat = resolvedSubChat,
                                          character = resolvedSubChar,
                                          providers = providerProfiles,
                                          onSave = { updatedChat ->
                                              if (resolvedSubChat.id > 0L) viewModel.updateChat(updatedChat)
                                          },
                                          onDeleteChat = {
                                              if (resolvedSubChat.id > 0L) {
                                                  viewModel.deleteChat(resolvedSubChat.id) {
                                                      activeChatSubScreen = null
                                                      viewModel.clearActiveChat()
                                                  }
                                              }
                                          },
                                          onBack = { activeChatSubScreen = null }
                                      )
                                  }
                                  "relationship_history" -> {
                                      RelationshipHistoryScreen(
                                          characterName = resolvedSubChar.name,
                                          relationships = activeRelationships,
                                          summaries = activeSummaries,
                                          onBack = { activeChatSubScreen = null },
                                          character = resolvedSubChar,
                                          relationshipViewMode = userSettings.relationshipViewMode,
                                          customRelationshipTypesJson = userSettings.customRelationshipTypesJson
                                      )
                                  }
                              }
                          }
                      }

                      // TOP SWIPEABLE CONNECTIVITY / REPOSITORY WARNING BANNER
                      AnimatedVisibility(
                          visible = connectivityWarning != null,
                          enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                          exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                          modifier = Modifier
                              .align(Alignment.TopCenter)
                              .statusBarsPadding()
                              .padding(horizontal = 12.dp, vertical = 8.dp)
                              .zIndex(10f)
                      ) {
                          val warningMsg = connectivityWarning ?: ""
                          var dragOffsetX by remember(warningMsg) { mutableStateOf(0f) }
                          var dragOffsetY by remember(warningMsg) { mutableStateOf(0f) }
                          Surface(
                              modifier = Modifier
                                  .fillMaxWidth()
                                  .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.coerceAtMost(0f).roundToInt()) }
                                  .pointerInput(warningMsg) {
                                      detectDragGestures(
                                          onDragEnd = {
                                              if (abs(dragOffsetX) > 120f || dragOffsetY < -60f) {
                                                  viewModel.dismissConnectivityWarning()
                                              } else {
                                                  dragOffsetX = 0f
                                                  dragOffsetY = 0f
                                              }
                                          },
                                          onDragCancel = {
                                              dragOffsetX = 0f
                                              dragOffsetY = 0f
                                          },
                                          onDrag = { change, dragAmount ->
                                              change.consume()
                                              dragOffsetX += dragAmount.x
                                              dragOffsetY += dragAmount.y
                                          }
                                      )
                                  },
                              shape = RoundedCornerShape(16.dp),
                              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f),
                              tonalElevation = 8.dp,
                              shadowElevation = 8.dp,
                              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                          ) {
                              Row(
                                  modifier = Modifier
                                      .fillMaxWidth()
                                      .padding(horizontal = 14.dp, vertical = 10.dp),
                                  verticalAlignment = Alignment.CenterVertically
                              ) {
                                  Icon(
                                      imageVector = Icons.Default.WifiOff,
                                      contentDescription = null,
                                      tint = MaterialTheme.colorScheme.onErrorContainer,
                                      modifier = Modifier.size(22.dp)
                                  )
                                  Spacer(modifier = Modifier.width(10.dp))
                                  Column(modifier = Modifier.weight(1f)) {
                                      Text(
                                          text = warningMsg,
                                          style = MaterialTheme.typography.bodySmall,
                                          fontWeight = FontWeight.SemiBold,
                                          color = MaterialTheme.colorScheme.onErrorContainer
                                      )
                                      Text(
                                          text = "Смахните уведомление, чтобы скрыть",
                                          style = MaterialTheme.typography.labelSmall,
                                          color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f)
                                      )
                                  }
                                  IconButton(
                                      onClick = { viewModel.checkInternetAndRepoConnection() },
                                      modifier = Modifier.size(32.dp)
                                  ) {
                                      Icon(
                                          imageVector = Icons.Default.Refresh,
                                          contentDescription = "Повторить проверку",
                                          tint = MaterialTheme.colorScheme.onErrorContainer,
                                          modifier = Modifier.size(18.dp)
                                      )
                                  }
                                  IconButton(
                                      onClick = { viewModel.dismissConnectivityWarning() },
                                      modifier = Modifier.size(32.dp)
                                  ) {
                                      Icon(
                                          imageVector = Icons.Default.Close,
                                          contentDescription = "Скрыть",
                                          tint = MaterialTheme.colorScheme.onErrorContainer,
                                          modifier = Modifier.size(18.dp)
                                      )
                                  }
                              }
                          }
                      }

                      if (showHotbar && chatTransitionProgress.value < 1f) {
                          IsolatedHotbarHost(
                              controller = tabController,
                              uiVariant = userSettings.uiVariant,
                              vaultHubStartTab = userSettings.vaultHubStartTab,
                              hapticsEnabled = userSettings.naturalHapticsEnabled,
                              animationMode = userSettings.hotbarAnimation,
                              showVerityButton = isVerityHotbarEnabled,
                              onOpenVerityGame = { isVerityGameOpen = true },
                              onSwitchToTab = switchToPagerTab,
                              modifier = Modifier
                                  .studioElement("global_hotbar")
                                  .align(Alignment.BottomCenter)
                                  .graphicsLayer {
                                      val p = chatTransitionProgress.value
                                      alpha = (1f - p * 1.5f).coerceIn(0f, 1f)
                                      translationY = p * 40f
                                  }
                          )
                      } else if (!showHotbar && isVerityHotbarEnabled && currentRoute == "characters" && !isStoreOverlayOpen && !isCharacterEditorOpen && !isChatOverlayOpen && chatTransitionProgress.value < 1f) {
                          Surface(
                              shape = CircleShape,
                              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                              tonalElevation = 10.dp,
                              shadowElevation = 8.dp,
                              border = BorderStroke(1.2.dp, Color(0xFFEAB308).copy(alpha = 0.5f)),
                              modifier = Modifier
                                  .align(Alignment.BottomCenter)
                                  .padding(bottom = 14.dp)
                                  .size(50.dp)
                                  .clip(CircleShape)
                                  .clickable {
                                      com.example.ui.components.HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                      isVerityGameOpen = true
                                  }
                          ) {
                              Box(contentAlignment = Alignment.Center) {
                                  androidx.compose.foundation.Image(
                                      painter = androidx.compose.ui.res.painterResource(id = R.drawable.verity_normal),
                                      contentDescription = "Игра Верити",
                                      modifier = Modifier
                                          .size(32.dp)
                                          .clip(CircleShape)
                                  )
                              }
                          }
                      }

                      // Boykisser Screen Companion Pet
                      com.example.ui.components.BoykisserOverlay(
                          enabled = userSettings.boykisserPetEnabled,
                          variant = userSettings.boykisserPetVariant,
                          speedFactor = userSettings.boykisserPetSpeed,
                          isInChat = isChatVisibleOrAnimating,
                          customTopYPx = chatInputTopYPx,
                          behavior = if (isChatVisibleOrAnimating) userSettings.boykisserChatBehavior else userSettings.boykisserMainBehavior,
                          modifier = Modifier.fillMaxSize().zIndex(200.0f)
                      )

                      // Seasonal Event: Falling pumpkins overlay (only active if event in info.json is "halloween")
                      HalloweenPumpkinRainOverlay(
                          isActive = isHalloweenActive && !isChatVisibleOrAnimating,
                          dropLimit = userSettings.pumpkinRainDropLimit,
                          pumpkinAlpha = userSettings.pumpkinRainAlpha,
                          onBlackPumpkinTapped = {
                              com.example.ui.components.HapticHelper.performSuccessHaptic(context, true)
                              viewModel.unlockHalloweenFrame {
                                  Toast.makeText(
                                      context,
                                      if (userSettings.appLanguage == "en") "🎃 You caught the rare Black Pumpkin! Halloween Frame added to your frames vault for your bots!"
                                      else "🎃 Вы поймали редкую Черную Тыкву! Рамка «Halloween» добавлена в хранилище рамок и доступна для ботов!",
                                      Toast.LENGTH_LONG
                                  ).show()
                              }
                          }
                      )

                      // "What's New in 1.0.0" Dialog on First Launch after Update
                      if (!isLoading && userSettings.lastSeenWhatsNewVersion != "1.0.0") {
                          com.example.ui.components.WhatsNewDialog(
                              appLanguage = userSettings.appLanguage,
                              onConfirm = {
                                  viewModel.updateUserSettings(userSettings.copy(lastSeenWhatsNewVersion = "1.0.0"))
                              }
                          )
                      }

                      // STARTUP PRELOADER OVERLAY: Keeps all tabs and chat shells hidden underneath while warming them up
                      AnimatedVisibility(
                          visible = isLoading,
                          enter = EnterTransition.None,
                          exit = fadeOut(tween(220)),
                          modifier = Modifier
                              .fillMaxSize()
                              .zIndex(100f)
                      ) {
                          SplashScreen(
                              progress = startupProgress,
                              statusText = startupStatusText
                          )
                      }
                    }
                    }
                }
            }
        }
    }
}

    override fun onResume() {
        super.onResume()
        BackgroundGenerationManager.setAppForegroundState(true)
    }

    override fun onStop() {
        super.onStop()
        BackgroundGenerationManager.setAppForegroundState(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTargetChatIntent(intent)
    }

    private fun handleTargetChatIntent(intent: Intent?) {
        val targetChatId = intent?.getLongExtra("target_chat_id", -1L) ?: -1L
        if (targetChatId > 0) {
            pendingTargetChatId.value = targetChatId
        }
    }
}

@Composable
fun SplashScreen(
    progress: Float = 0.1f,
    statusText: String = "Загрузка локальной базы данных Untitled..."
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0.02f, 1f),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "splash_progress"
    )
    val percentInt = (animatedProgress * 100f).roundToInt().coerceIn(1, 100)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(96.dp),
                tonalElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Загрузка Untitled",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                minLines = 2
            )
            Spacer(modifier = Modifier.height(22.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Предзагрузка экранов и кэша",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                Text(
                    text = "$percentInt%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun IsolatedTabBackHandler(
    controller: TabPagerController,
    homePage: Int,
    enabled: Boolean,
    onBack: () -> Unit
) {
    BackHandler(enabled = enabled && controller.canNavigateBack(homePage), onBack = onBack)
}

@Composable
private fun IsolatedHotbarHost(
    controller: TabPagerController,
    uiVariant: String,
    vaultHubStartTab: String,
    hapticsEnabled: Boolean,
    animationMode: String,
    showVerityButton: Boolean = false,
    onOpenVerityGame: () -> Unit = {},
    onSwitchToTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentHotbarRoute = if (uiVariant == "vault_hub") {
        when (controller.activeTarget) {
            0 -> "vault_general"
            1 -> "vault_chats"
            2 -> "personas"
            3 -> "providers"
            else -> "settings"
        }
    } else {
        when (controller.activeTarget) {
            0 -> "characters"
            1 -> "personas"
            2 -> "providers"
            else -> "settings"
        }
    }

    PillNavigationHotbar(
        currentRoute = currentHotbarRoute,
        uiVariant = uiVariant,
        onNavigate = { targetRoute ->
            when (targetRoute) {
                "vault_general" -> onSwitchToTab(0)
                "vault_chats" -> onSwitchToTab(1)
                "characters" -> onSwitchToTab(0)
                "personas" -> onSwitchToTab(if (uiVariant == "vault_hub") 2 else 1)
                "providers" -> onSwitchToTab(if (uiVariant == "vault_hub") 3 else 2)
                "settings" -> onSwitchToTab(if (uiVariant == "vault_hub") 4 else 3)
                else -> onSwitchToTab(if (uiVariant == "vault_hub" && vaultHubStartTab == "chats") 1 else 0)
            }
        },
        hapticsEnabled = hapticsEnabled,
        animationMode = animationMode,
        showVerityButton = showVerityButton,
        onOpenVerityGame = onOpenVerityGame,
        modifier = modifier
    )
}

