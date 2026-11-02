package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.zIndex
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.components.VoiceMessagePlayer
import com.example.ui.components.VoiceRecordingBar
import com.example.ui.studio.studioElement
import com.example.util.VoicePlayerManager
import com.example.util.VoiceRecorderHelper
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.datastore.UserSettings
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.ui.components.*
import com.example.util.ImageStorageHelper
import com.example.util.VideoAnalysisHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    character: CharacterEntity,
    chat: ChatEntity,
    allChatsForCharacter: List<ChatEntity> = emptyList(),
    messages: List<MessageEntity>,
    summaries: List<SummaryEntity>,
    latestRelationship: RelationshipTableEntity?,
    userSettings: UserSettings,
    providerProfiles: List<AiProviderProfileEntity> = emptyList(),
    isGeneratingResponse: Boolean = false,
    streamingMessage: String? = null,
    onSendMessage: (String, String?) -> Unit,
    onSendVoiceMessage: (String, String, Int) -> Unit = { _, _, _ -> },
    onStopGeneration: () -> Unit = {},
    onRegenerateVariant: (Long) -> Unit = {},
    onSwitchVariant: (Long, Int) -> Unit = { _, _ -> },
    onEditUserMessageAndRegenerate: (Long, String) -> Unit,
    onEditMessage: (Long, String) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onClearChat: () -> Unit,
    onToggleTextingMode: (Boolean) -> Unit,
    onSwitchChat: (Long) -> Unit,
    onCreateNewChat: (String) -> Unit,
    onTogglePinChat: (Long, Boolean) -> Unit = { _, _ -> },
    onToggleArchiveChat: (Long, Boolean) -> Unit = { _, _ -> },
    onRenameChat: (Long, String) -> Unit = { _, _ -> },
    onDeleteChat: (Long) -> Unit = {},
    onGenerateComic: (
        chapterTitle: String,
        chapterSummaryText: String?,
        style: String,
        panelCount: Int,
        selectedProviderId: Long?,
        civitaiModelAir: String?,
        customPrompt: String,
        negativePrompt: String
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onOpenChatSettings: () -> Unit,
    onOpenRelationshipHistory: () -> Unit,
    onOpenComicsGallery: () -> Unit = {},
    onGeneratePhoto: (
        prompt: String,
        includeAppearance: Boolean,
        style: String,
        resolution: String,
        providerProfileId: Long?,
        engineMode: String
    ) -> Unit = { _, _, _, _, _, _ -> },
    allPersonas: List<UserPersonaEntity> = emptyList(),
    activePersona: UserPersonaEntity? = null,
    onSelectChatPersona: (Long?) -> Unit = {},
    initialDraftText: String = "",
    onChatInputTopYChanged: ((Float?) -> Unit)? = null,
    returnedFailedMessageText: String? = null,
    onConsumedReturnedMessage: () -> Unit = {},
    onDraftChange: (String) -> Unit = {},
    onUpdateChatNotes: (Long, String) -> Unit = { _, _ -> },
    onSetReaction: (Long, String?) -> Unit = { _, _ -> },
    onBranchChat: (Long) -> Unit = {},
    isHalloweenActive: Boolean = false,
    onBlackPumpkinTapped: () -> Unit = {},
    onAssistantCreateBot: ((String, () -> Unit, (String) -> Unit) -> Unit)? = null,
    onAssistantCreatePersona: ((String, () -> Unit, (String) -> Unit) -> Unit)? = null,
    isPreloadedShell: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !isPreloadedShell, onBack = onBack)

    val effectiveInitialDraft = initialDraftText.ifEmpty { chat.draftText }
    var inputText by remember(chat.id) {
        mutableStateOf(TextFieldValue(effectiveInitialDraft, TextRange(effectiveInitialDraft.length)))
    }

    // Load saved draft if chat.draftText or initialDraftText arrives asynchronously on app start
    LaunchedEffect(chat.id, effectiveInitialDraft) {
        if (inputText.text.isEmpty() && effectiveInitialDraft.isNotEmpty()) {
            inputText = TextFieldValue(effectiveInitialDraft, TextRange(effectiveInitialDraft.length))
        }
    }

    // Restore message to input field if sending failed due to no internet
    LaunchedEffect(returnedFailedMessageText) {
        if (!returnedFailedMessageText.isNullOrEmpty()) {
            inputText = TextFieldValue(returnedFailedMessageText, TextRange(returnedFailedMessageText.length))
            onDraftChange(returnedFailedMessageText)
            onConsumedReturnedMessage()
        }
    }
    var showNotesDialog by remember { mutableStateOf(false) }
    var notesInput by remember(chat.notes) { mutableStateOf(chat.notes) }
    var attachedMediaList by remember { mutableStateOf<List<com.example.util.ChatMediaItem>>(emptyList()) }
    var isMediaProcessing by remember { mutableStateOf(false) }
    var mediaProcessingStatus by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val voiceRecorder = remember { VoiceRecorderHelper(context) }
    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.release()
            VoicePlayerManager.stop()
        }
    }
    val isRecordingAudio by voiceRecorder.isRecording.collectAsState()
    val recordingDurationSec by voiceRecorder.recordingDurationSec.collectAsState()
    val recordingAmplitudes by voiceRecorder.currentAmplitudes.collectAsState()
    val recognizedSpeechText by voiceRecorder.recognizedText.collectAsState()

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = voiceRecorder.startRecording()
            if (!started) {
                Toast.makeText(context, "Не удалось начать запись звука", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Разрешение на микрофон необходимо для записи голосовых", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onChatInputTopYChanged?.invoke(null)
        }
    }

    var showPersonaSheet by remember { mutableStateOf(false) }
    var assistantCreatorMode by remember { mutableStateOf<String?>(null) } // "bot" or "persona"
    var assistantCreatorWishes by remember { mutableStateOf("") }
    var isAssistantCreatorLoading by remember { mutableStateOf(false) }
    var assistantCreatorError by remember { mutableStateOf<String?>(null) }

    // Resolve active chat persona (either explicit chat persona or global active persona)
    val effectivePersona = remember(chat.personaId, allPersonas, activePersona) {
        if (chat.personaId != null) {
            allPersonas.find { it.id == chat.personaId } ?: activePersona
        } else {
            activePersona
        }
    }

    // Auto-save draft on disposal and when app goes to background (ON_PAUSE / ON_STOP)
    // Using a non-state reference avoids reading inputText at the root of ChatScreen on every keystroke
    val latestDraftRef = remember(chat.id) { arrayOf(effectiveInitialDraft) }
    val currentOnDraftChange by rememberUpdatedState(onDraftChange)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, chat.id) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE ||
                event == androidx.lifecycle.Lifecycle.Event.ON_STOP
            ) {
                currentOnDraftChange(latestDraftRef[0])
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentOnDraftChange(latestDraftRef[0])
        }
    }

    var branchesTab by remember { mutableIntStateOf(0) } // 0 = Активные, 1 = Архив
    var chatSortMode by remember { mutableStateOf("updated_desc") }
    var renamingChatId by remember { mutableStateOf<Long?>(null) }
    var renamingChatTitle by remember { mutableStateOf("") }
    var deletingChatId by remember { mutableStateOf<Long?>(null) }

    val activeBranches = remember(allChatsForCharacter) { allChatsForCharacter.filter { !it.isArchived } }
    val archivedBranches = remember(allChatsForCharacter) { allChatsForCharacter.filter { it.isArchived } }
    val currentBranchesList = if (branchesTab == 0) activeBranches else archivedBranches

    val sortedChats = remember(currentBranchesList, chatSortMode) {
        currentBranchesList.sortedWith { c1, c2 ->
            if (c1.isPinned != c2.isPinned) {
                if (c1.isPinned) -1 else 1
            } else {
                when (chatSortMode) {
                    "updated_desc" -> c2.updatedAt.compareTo(c1.updatedAt)
                    "updated_asc" -> c1.updatedAt.compareTo(c2.updatedAt)
                    "title_asc" -> c1.title.compareTo(c2.title, ignoreCase = true)
                    "title_desc" -> c2.title.compareTo(c1.title, ignoreCase = true)
                    "created_desc" -> c2.createdAt.compareTo(c1.createdAt)
                    else -> c2.updatedAt.compareTo(c1.updatedAt)
                }
            }
        }
    }

    val insertAtCursor: (TextFieldValue, String, Int) -> TextFieldValue = { currentValue, insertion, cursorOffsetInside ->
        val text = currentValue.text
        val selection = currentValue.selection
        val start = minOf(selection.start, selection.end)
        val end = maxOf(selection.start, selection.end)

        val newText = text.substring(0, start) + insertion + text.substring(end)
        val newCursorPos = start + cursorOffsetInside
        val updated = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
        onDraftChange(updated.text)
        updated
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            val remainingSlots = (com.example.util.ChatMediaHelper.MAX_MEDIA_COUNT - attachedMediaList.size).coerceAtLeast(0)
            if (remainingSlots <= 0) {
                Toast.makeText(context, "Достигнут лимит вложений (максимум ${com.example.util.ChatMediaHelper.MAX_MEDIA_COUNT} медиа)", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val toProcess = uris.take(remainingSlots)
            if (uris.size > remainingSlots) {
                Toast.makeText(context, "Добавлено первых $remainingSlots фото (всего до ${com.example.util.ChatMediaHelper.MAX_MEDIA_COUNT})", Toast.LENGTH_SHORT).show()
            }
            val newPhotos = mutableListOf<com.example.util.ChatMediaItem.Photo>()
            for (uri in toProcess) {
                val base64 = ImageStorageHelper.uriToBase64(context, uri)
                if (base64 != null) {
                    newPhotos.add(com.example.util.ChatMediaItem.Photo(base64))
                }
            }
            if (newPhotos.isNotEmpty()) {
                attachedMediaList = (attachedMediaList + newPhotos).take(com.example.util.ChatMediaHelper.MAX_MEDIA_COUNT)
                HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            val remainingSlots = (com.example.util.ChatMediaHelper.MAX_MEDIA_COUNT - attachedMediaList.size).coerceAtLeast(0)
            if (remainingSlots <= 0) {
                Toast.makeText(context, "Достигнут лимит вложений (максимум ${com.example.util.ChatMediaHelper.MAX_MEDIA_COUNT} медиа)", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val toProcess = uris.take(remainingSlots)
            if (uris.size > remainingSlots) {
                Toast.makeText(context, "Выбрано $remainingSlots видео (всего до ${com.example.util.ChatMediaHelper.MAX_MEDIA_COUNT})", Toast.LENGTH_SHORT).show()
            }
            isMediaProcessing = true
            coroutineScope.launch {
                try {
                    val newVideos = mutableListOf<com.example.util.ChatMediaItem.Video>()
                    for ((index, videoUri) in toProcess.withIndex()) {
                        mediaProcessingStatus = "Анализ видео (${index + 1}/${toProcess.size})..."
                        val result = VideoAnalysisHelper.analyzeVideo(context, videoUri)
                        val analysis = result.getOrNull()
                        if (analysis != null) {
                            newVideos.add(
                                com.example.util.ChatMediaItem.Video(
                                    durationSec = analysis.durationSec.toInt(),
                                    thumbnailBase64 = analysis.thumbnailBase64,
                                    frameBase64List = analysis.frameBase64List
                                )
                            )
                        }
                    }
                    if (newVideos.isNotEmpty()) {
                        attachedMediaList = (attachedMediaList + newVideos).take(com.example.util.ChatMediaHelper.MAX_MEDIA_COUNT)
                        HapticHelper.performSuccessHaptic(context, userSettings.naturalHapticsEnabled)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка анализа видео: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                } finally {
                    isMediaProcessing = false
                    mediaProcessingStatus = ""
                }
            }
        }
    }

    var isLocalAITyping by remember { mutableStateOf(false) }
    val isAITyping = isGeneratingResponse || isLocalAITyping
    val appLang = com.example.util.LocalAppLanguage.current
    val isEn = appLang.equals("en", ignoreCase = true)
    val isFemboy = appLang.equals("femboy", ignoreCase = true)
    var hiddenMessageId by remember { mutableStateOf<Long?>(null) }
    var selectedPreviewImage by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }

    var showBranchesDialog by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var newChatTitleInput by remember { mutableStateOf("") }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showSystemPromptDialog by remember { mutableStateOf(false) }

    // Message action state (Inline Edit / Delete)
    var selectedMessageForAction by remember { mutableStateOf<MessageEntity?>(null) }
    var showMessageActionSheet by remember { mutableStateOf(false) }
    var inlineEditingMessageId by remember { mutableStateOf<Long?>(null) }
    var inlineEditingText by remember { mutableStateOf("") }
    var showRelationshipBanner by remember { mutableStateOf(false) }
    var lastKnownRelationshipId by remember { mutableStateOf<Long?>(null) }

    // Auto-show relationship banner with animation when a new relationship table entity is generated
    LaunchedEffect(latestRelationship?.id) {
        if (latestRelationship != null) {
            if (lastKnownRelationshipId != null && latestRelationship.id != lastKnownRelationshipId) {
                showRelationshipBanner = true
            }
            lastKnownRelationshipId = latestRelationship.id
        }
    }

    var visibleMessageLimit by remember(chat.id) { mutableIntStateOf(45) }

    val fullChatMessages = remember(messages, hiddenMessageId, chat.id) {
        if (chat.id <= 0L) emptyList()
        else messages.filter { it.chatId == chat.id && it.id != hiddenMessageId }
    }

    val hiddenOlderCount = (fullChatMessages.size - visibleMessageLimit).coerceAtLeast(0)

    val prewarmDummyMessages = remember {
        listOf(
            MessageEntity(
                id = -9001L,
                chatId = 0L,
                sender = "character",
                content = "**Система:** *предзагрузка элементов чата*",
                timestamp = 1700000000000L
            ),
            MessageEntity(
                id = -9002L,
                chatId = 0L,
                sender = "user",
                content = "Предзагрузка сообщения пользователя",
                timestamp = 1700000060000L
            )
        )
    }

    val allChatMessages = remember(fullChatMessages, visibleMessageLimit, isPreloadedShell) {
        if (isPreloadedShell || chat.id <= 0L) {
            prewarmDummyMessages
        } else if (fullChatMessages.size > visibleMessageLimit) {
            fullChatMessages.takeLast(visibleMessageLimit)
        } else {
            fullChatMessages
        }
    }

    var hasInitialScrolled by remember(chat.id) { mutableStateOf(false) }

    val firstCharacterMessageId = remember(fullChatMessages) {
        fullChatMessages.firstOrNull { it.sender == "character" }?.id
    }

    data class UserPromptAnchor(
        val listIndex: Int,
        val messageId: Long,
        val previewText: String
    )

    val userPromptAnchors = remember(allChatMessages, isPreloadedShell) {
        if (isPreloadedShell) emptyList()
        else allChatMessages.mapIndexedNotNull { index, msg ->
            if (msg.sender == "user") {
                UserPromptAnchor(
                    listIndex = index,
                    messageId = msg.id,
                    previewText = msg.content.take(50).replace("\n", " ")
                )
            } else null
        }
    }

    val hasStandaloneBanner = !isPreloadedShell && firstCharacterMessageId == null && character.greetingBannerUri.isNotBlank()
    val hasOlderHeader = !isPreloadedShell && hiddenOlderCount > 0
    val bannerItemOffset = (if (hasStandaloneBanner) 1 else 0) + (if (hasOlderHeader) 1 else 0)
    val bottomAnchorIndex = bannerItemOffset + allChatMessages.size + 1

    var isAutoScrollLockedToBottom by remember(chat.id) { mutableStateOf(true) }
    var lastMessageCount by remember(chat.id) { mutableIntStateOf(0) }
    var selectedDotPreviewText by remember { mutableStateOf<String?>(null) }

    val isNearBottom by remember(bottomAnchorIndex) {
        derivedStateOf {
            !listState.canScrollForward || listState.firstVisibleItemIndex >= (bottomAnchorIndex - 4).coerceAtLeast(0)
        }
    }

    // Update lock state when user manually scrolls without triggering per-pixel recomposition
    LaunchedEffect(isNearBottom) {
        isAutoScrollLockedToBottom = isNearBottom
    }

    // Auto-hide dot preview tooltip after 2.2 seconds
    LaunchedEffect(selectedDotPreviewText) {
        if (selectedDotPreviewText != null) {
            kotlinx.coroutines.delay(2200L)
            selectedDotPreviewText = null
        }
    }

    // Scroll to bottom guaranteeing full scroll to the very end of the last message
    val scrollToBottom: (Boolean) -> Unit = { animate ->
        isAutoScrollLockedToBottom = true
        coroutineScope.launch {
            if (allChatMessages.isNotEmpty()) {
                val anchorIdx = (bannerItemOffset + allChatMessages.size + 1).coerceAtLeast(0)
                if (animate && userSettings.chatScrollAnimation != "instant") {
                    listState.animateScrollToItem(anchorIdx)
                } else {
                    listState.scrollToItem(anchorIdx)
                }
            }
        }
    }

    // Reset initial scroll flag when chat changes or when shell is hidden
    LaunchedEffect(chat.id, isPreloadedShell) {
        if (isPreloadedShell) {
            hasInitialScrolled = false
            isAutoScrollLockedToBottom = true
        } else if (allChatMessages.isNotEmpty() && !hasInitialScrolled) {
            isAutoScrollLockedToBottom = true
            listState.scrollToItem(bottomAnchorIndex.coerceAtLeast(0))
            withFrameNanos { }
            listState.scrollToItem(bottomAnchorIndex.coerceAtLeast(0))
            hasInitialScrolled = true
            lastMessageCount = allChatMessages.size
        }
    }

    // Auto scroll to bottom upon entering chat and when new messages arrive
    LaunchedEffect(allChatMessages.size, isPreloadedShell) {
        if (!isPreloadedShell && allChatMessages.isNotEmpty()) {
            if (!hasInitialScrolled) {
                isAutoScrollLockedToBottom = true
                listState.scrollToItem(bottomAnchorIndex.coerceAtLeast(0))
                withFrameNanos { }
                listState.scrollToItem(bottomAnchorIndex.coerceAtLeast(0))
                hasInitialScrolled = true
                lastMessageCount = allChatMessages.size
            } else if (allChatMessages.size > lastMessageCount) {
                val lastSender = allChatMessages.lastOrNull()?.sender
                val shouldScroll = lastSender == "user" || isAutoScrollLockedToBottom
                lastMessageCount = allChatMessages.size
                if (shouldScroll) {
                    isAutoScrollLockedToBottom = true
                    listState.scrollToItem(bottomAnchorIndex.coerceAtLeast(0))
                    withFrameNanos { }
                    listState.scrollToItem(bottomAnchorIndex.coerceAtLeast(0))
                }
            } else {
                lastMessageCount = allChatMessages.size
            }
        }
    }

    // Keep fully scrolled to bottom during streaming if locked to bottom
    LaunchedEffect(isAITyping, streamingMessage?.length) {
        if (isAITyping && hasInitialScrolled && isAutoScrollLockedToBottom) {
            listState.scrollToItem(bottomAnchorIndex.coerceAtLeast(0))
        }
    }

    // Fix phantom typing bug: clear typing state immediately when generation is inactive and streaming is empty
    LaunchedEffect(isGeneratingResponse, streamingMessage) {
        if (!isGeneratingResponse && streamingMessage.isNullOrBlank()) {
            isLocalAITyping = false
            hiddenMessageId = null
        }
    }

    val isGlassEnabled = userSettings.glassmorphismEnabled
    val isLightGlass = userSettings.wallpaperTone == "light"
    val glassAlpha = userSettings.glassAlpha.coerceIn(0.15f, 0.95f)

    val glassBg = when {
        !isGlassEnabled -> MaterialTheme.colorScheme.surface
        isLightGlass -> Color(0xFFFFFFFF).copy(alpha = glassAlpha.coerceAtLeast(0.35f))
        else -> Color(0xFF141418).copy(alpha = glassAlpha)
    }
    val glassContentColor = when {
        !isGlassEnabled -> MaterialTheme.colorScheme.onSurface
        isLightGlass -> Color(0xFF181A20)
        else -> Color(0xFFF3F4F6)
    }
    val glassSubtitleColor = when {
        !isGlassEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
        isLightGlass -> Color(0xFF555965)
        else -> Color(0xFFA0A4B0)
    }
    val glassBorderColor = when {
        !isGlassEnabled -> Color.Transparent
        isLightGlass -> Color.White.copy(alpha = 0.6f)
        else -> Color.White.copy(alpha = 0.15f)
    }
    val glassInputBg = when {
        !isGlassEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isLightGlass -> Color(0x1F000000)
        else -> Color(0x33FFFFFF)
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ParallaxChatWallpaper(
            preset = userSettings.chatWallpaperPreset,
            customUri = userSettings.customWallpaperUri,
            alpha = userSettings.chatWallpaperAlpha,
            parallaxEnabled = userSettings.parallaxEnabled && !isPreloadedShell && !listState.isScrollInProgress,
            liveVideoWallpaperEnabled = userSettings.liveVideoWallpaperEnabled && !isPreloadedShell,
            liveVideoWallpaperUri = userSettings.liveVideoWallpaperUri.ifBlank { userSettings.customWallpaperUri }
        )

        // Halloween pumpkins falling BEHIND text and messages, never obscuring reading
        if (isHalloweenActive && !isPreloadedShell) {
            HalloweenPumpkinRainOverlay(
                isActive = true,
                dropLimit = userSettings.pumpkinRainDropLimit,
                pumpkinAlpha = userSettings.pumpkinRainAlpha * 0.70f,
                onBlackPumpkinTapped = onBlackPumpkinTapped
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    modifier = Modifier.studioElement("chat_top_bar"),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = glassBg,
                        titleContentColor = glassContentColor,
                        navigationIconContentColor = glassContentColor,
                        actionIconContentColor = glassContentColor
                    ),
                    title = {
                        if (!isPreloadedShell && character.id > 0L) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .studioElement("chat_branch_selector")
                                    .clickable { showBranchesDialog = true }
                            ) {
                                AvatarImage(
                                    uri = character.avatarUri,
                                    contentDescription = character.name,
                                    size = 40.dp,
                                    frameUri = character.avatarFrameUri
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = character.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = glassContentColor
                                        )
                                        if (chat.isNsfwEnabled) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "18+",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (isAITyping) (if (isFemboy) "печатает ответик... 🐾" else if (isEn) "is typing..." else "печатает...") else if (character.isAssistant && (chat.title.isBlank() || chat.title.startsWith("Диалог") || chat.title.startsWith("Dialogue") || chat.title == "Помощь и консультации")) (if (isFemboy) "Помощь и мурчалки 🐾" else if (isEn) "Help & Consultation" else "Помощь и консультации") else chat.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAITyping) MaterialTheme.colorScheme.primary else glassSubtitleColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = if (isEn) "Back" else "Назад", tint = glassContentColor)
                        }
                    },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.studioElement("chat_action_menu")
                    ) {
                        // Persona selector icon button
                        IconButton(onClick = { showPersonaSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = if (isEn) "Chat Persona" else "Персона чата",
                                tint = if (chat.personaId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Single Three-dots Overflow Menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = if (isEn) "Chat Menu" else "Меню чата")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                            // 0. Select Persona
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(if (isFemboy) "Масочка для чатика" else if (isEn) "Persona for chat" else "Персона для чата")
                                        Text(
                                            text = effectivePersona?.name ?: (if (isFemboy) "Глобальная по умолчанию" else if (isEn) "Global Default" else "Глобальная по умолчанию"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showPersonaSheet = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // 1. Chat Branches
                            DropdownMenuItem(
                                text = { Text(if (isFemboy) "Ветки диалога... 🐾" else if (isEn) "Dialogue branches..." else "Ветки диалога...") },
                                onClick = {
                                    showMenu = false
                                    showBranchesDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Forum, contentDescription = null)
                                }
                            )

                            // 3. New Chat Branch
                            DropdownMenuItem(
                                text = { Text(if (isFemboy) "Новый чатик с ботиком" else if (isEn) "New dialogue with bot" else "Новый диалог с ботом") },
                                onClick = {
                                    showMenu = false
                                    newChatTitleInput = (if (isEn) "Dialogue " else "Диалог ") + (allChatsForCharacter.size + 1)
                                    showNewChatDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AddComment, contentDescription = null)
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // 4. Relationship Table
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isFemboy) "История милоты и отношений" else if (isEn) "Relationship history" else "История отношений")
                                        if (latestRelationship != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "(${latestRelationship.trustScore}%)",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onOpenRelationshipHistory()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            // 6. Chat Notes
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(if (isFemboy) "Заметочки к чатику" else if (isEn) "Chat notes" else "Заметки к чату")
                                        Text(
                                            text = if (chat.notes.isNotBlank()) (if (isEn) "Filled" else "Заполнено") else (if (isFemboy) "Пустенько" else if (isEn) "Empty" else "Пусто"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    notesInput = chat.notes
                                    showNotesDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            // 6b. System Prompt Viewer
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(if (isEn) "AI System Prompt" else "Системный промпт ИИ")
                                        Text(
                                            text = if (isFemboy) "Просмотр и цап-царап промпта" else if (isEn) "View and copy prompt" else "Просмотр и копирование промпта",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    showSystemPromptDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )

                            // 7. Chat Settings
                            DropdownMenuItem(
                                text = { Text(if (isFemboy) "Настроечки чатика (18+, ИИ)" else if (isEn) "Chat settings (18+, AI)" else "Настройки чата (18+, ИИ)") },
                                onClick = {
                                    showMenu = false
                                    onOpenChatSettings()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Tune, contentDescription = null)
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // 7. Clear Chat
                            DropdownMenuItem(
                                text = { Text(if (isFemboy) "Стереть сообщения (ня)" else if (isEn) "Clear messages" else "Очистить сообщения", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showClearChatDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            }
        )
    }
) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Persona indicator banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPersonaSheet = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Face,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val personaTitle = if (isFemboy) "Масочка" else if (isEn) "Persona" else "Персона"
                            val globalDefault = if (isFemboy) "Глобальная по умолчанию" else if (isEn) "Global Default" else "Глобальная по умолчанию"
                            Text(
                                text = "$personaTitle: ${effectivePersona?.name ?: globalDefault}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (effectivePersona?.gender?.isNotBlank() == true) {
                                Text(
                                    text = " • ${effectivePersona.gender}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isFemboy) "Выбрать ▾" else if (isEn) "Select" else "Выбрать",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Archived banner if active chat is in archive
                if (chat.isArchived) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Диалог находится в архиве",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            FilledTonalButton(
                                onClick = { onToggleArchiveChat(chat.id, false) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Вернуть", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Animated Relationship Alert Banner
                androidx.compose.animation.AnimatedVisibility(
                    visible = showRelationshipBanner && latestRelationship != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (latestRelationship != null) {
                        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                            com.example.ui.components.RelationshipCard(
                                table = latestRelationship,
                                isInitialExpanded = false,
                                character = character,
                                relationshipViewMode = userSettings.relationshipViewMode,
                                customRelationshipTypesJson = userSettings.customRelationshipTypesJson,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Message List Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val lastVisibleId = allChatMessages.lastOrNull()?.id

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Ultra-lazy older messages loader so off-screen history stays unmounted
                        if (hasOlderHeader) {
                            item(key = "load_older_messages_header", contentType = "older_header") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FilledTonalButton(
                                        onClick = { visibleMessageLimit += 60 },
                                        shape = RoundedCornerShape(999.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.ExpandLess, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Показать ранние сообщения (скрыто: $hiddenOlderCount)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Fallback standalone greeting banner if chat has no character messages yet
                        if (!isPreloadedShell && firstCharacterMessageId == null && character.greetingBannerUri.isNotBlank()) {
                            item(key = "standalone_greeting_banner", contentType = "greeting_banner") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .clickable { selectedPreviewImage = character.greetingBannerUri }
                                ) {
                                    AppAsyncImage(
                                        model = character.greetingBannerUri,
                                        contentDescription = "Приветственный баннер",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        itemsIndexed(
                            items = allChatMessages,
                            key = { _, msg -> msg.id },
                            contentType = { _, msg -> msg.sender }
                        ) { index, msg ->
                            val isInlineEditing = inlineEditingMessageId == msg.id

                            val prevTimestamp = if (index > 0) allChatMessages[index - 1].timestamp else 0L
                            val showDateHeader = index == 0 || isNewDay(prevTimestamp, msg.timestamp)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (showDateHeader && msg.timestamp > 0L) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                        ) {
                                            Text(
                                                text = formatMessageDateHeader(msg.timestamp, isEn),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                MessageItemView(
                                    message = msg,
                                    character = character,
                                    userSettings = userSettings,
                                    isLastMessage = msg.id == lastVisibleId,
                                    isGreetingMessage = (msg.id == firstCharacterMessageId) || (index == 0 && msg.sender == "character"),
                                    isEditing = isInlineEditing,
                                    editingText = if (isInlineEditing) inlineEditingText else msg.content,
                                    onEditingTextChange = { inlineEditingText = it },
                                    onSaveEdit = {
                                        val text = inlineEditingText.trim()
                                        if (text.isNotBlank()) {
                                            onEditMessage(msg.id, text)
                                            inlineEditingMessageId = null
                                        }
                                    },
                                    onSaveAndRegenerate = {
                                        val text = inlineEditingText.trim()
                                        if (text.isNotBlank()) {
                                            isLocalAITyping = true
                                            onEditUserMessageAndRegenerate(msg.id, text)
                                            inlineEditingMessageId = null
                                        }
                                    },
                                    onCancelEdit = {
                                        inlineEditingMessageId = null
                                    },
                                    onImageClick = { url -> selectedPreviewImage = url },
                                    onRegenerateVariant = { msgId ->
                                        hiddenMessageId = msgId
                                        isLocalAITyping = true
                                        onRegenerateVariant(msgId)
                                    },
                                    onSwitchVariant = onSwitchVariant,
                                    onEditMessage = onEditMessage,
                                    onSetReaction = onSetReaction,
                                    onLongClick = {
                                        selectedMessageForAction = msg
                                        showMessageActionSheet = true
                                    }
                                )
                            }
                        }

                        // Typing / Streaming Indicator with smooth entrance & exit
                        item(key = "typing_streaming_indicator") {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isAITyping,
                                enter = fadeIn(animationSpec = tween(250)) + expandVertically(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    expandFrom = Alignment.Bottom
                                ),
                                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(
                                    animationSpec = tween(200),
                                    shrinkTowards = Alignment.Bottom
                                )
                            ) {
                                if (!streamingMessage.isNullOrBlank()) {
                                    com.example.ui.components.StreamingMessageBubble(
                                        characterName = character.name,
                                        streamedText = streamingMessage,
                                        avatarUri = if (userSettings.showAvatarInChat && userSettings.bubbleStyle != "novel") character.avatarUri else null,
                                        appearanceMode = userSettings.botTextAppearanceMode,
                                        typingSpeedMs = userSettings.botTypingSpeedMs,
                                        onStop = {
                                            isLocalAITyping = false
                                            HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                            onStopGeneration()
                                        }
                                    )
                                } else {
                                    TypingIndicator(
                                        characterName = character.name,
                                        onStop = {
                                            isLocalAITyping = false
                                            HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                            onStopGeneration()
                                        }
                                    )
                                }
                            }
                        }

                        // Bottom spacer anchor ensuring full view of the last message
                        item(key = "bottom_spacer_anchor") {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // ChatGPT-style User Prompt Dots Navigation Bar on the right side
                    if (userPromptAnchors.isNotEmpty()) {
                        val activeUserAnchorIdx by remember(userPromptAnchors, bannerItemOffset) {
                            derivedStateOf {
                                val approxVisibleMsgIdx = (listState.firstVisibleItemIndex + 2 - bannerItemOffset).coerceAtLeast(0)
                                val found = userPromptAnchors.indexOfLast { it.listIndex <= approxVisibleMsgIdx }
                                if (found >= 0) found else 0
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 6.dp)
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = selectedDotPreviewText != null,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.widthIn(max = 210.dp)
                                ) {
                                    Text(
                                        text = selectedDotPreviewText ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                                shadowElevation = 2.dp
                            ) {
                                LazyColumn(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(max = 220.dp)
                                ) {
                                    itemsIndexed(
                                        items = userPromptAnchors,
                                        key = { _, anchor -> anchor.messageId }
                                    ) { dotIdx, anchor ->
                                        val isCurrentDot = dotIdx == activeUserAnchorIdx
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(width = 16.dp, height = if (isCurrentDot) 16.dp else 12.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    selectedDotPreviewText = "💬 #${dotIdx + 1}: ${anchor.previewText}"
                                                    isAutoScrollLockedToBottom = false
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(
                                                            (anchor.listIndex + bannerItemOffset).coerceAtLeast(0)
                                                        )
                                                    }
                                                }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(
                                                        width = if (isCurrentDot) 7.dp else 5.dp,
                                                        height = if (isCurrentDot) 12.dp else 5.dp
                                                    )
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (isCurrentDot) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Scroll-To-Bottom button if scrolled up (memoized via derivedStateOf to prevent per-pixel recomposition)
                    val showScrollToBottomFab by remember(allChatMessages.isNotEmpty()) {
                        derivedStateOf {
                            allChatMessages.isNotEmpty() && listState.canScrollForward
                        }
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showScrollToBottomFab,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 12.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                scrollToBottom(true)
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.studioElement("chat_scroll_bottom_fab")
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Вниз к последнему сообщению", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Quick Formatting / Assistant Quick Action Chips
                val isAssistant = character.name.equals("Untitled Ассистент", ignoreCase = true)
                Surface(
                    color = if (isGlassEnabled) glassBg.copy(alpha = (glassAlpha * 0.85f).coerceIn(0.20f, 0.92f)) else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(0.5.dp, if (isGlassEnabled) glassBorderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .studioElement("chat_format_chips")
                        .fillMaxWidth()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAssistant) {
                            item {
                                AssistChip(
                                    onClick = {
                                        assistantCreatorError = null
                                        assistantCreatorWishes = ""
                                        assistantCreatorMode = "bot"
                                    },
                                    label = {
                                        Text(
                                            if (isFemboy) "🤖 Помоги создать ботика! 🐾" else if (isEn) "🤖 Help create a bot!" else "🤖 Помоги создать бота!",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("assistant_create_bot_btn")
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = {
                                        assistantCreatorError = null
                                        assistantCreatorWishes = ""
                                        assistantCreatorMode = "persona"
                                    },
                                    label = {
                                        Text(
                                            if (isFemboy) "🎭 Помоги создать масочку! 💖" else if (isEn) "🎭 Help create a persona!" else "🎭 Помоги создать персону!",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("assistant_create_persona_btn")
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = { inputText = insertAtCursor(inputText, "Как настроить OpenRouter и бесплатные модели?", "Как настроить OpenRouter и бесплатные модели?".length) },
                                    label = { Text("⚙️ Как настроить OpenRouter?", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = { inputText = insertAtCursor(inputText, "Проверь мои текущие настройки и подскажи, что можно улучшить.", "Проверь мои текущие настройки и подскажи, что можно улучшить.".length) },
                                    label = { Text("🔍 Проверь настройки", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = { inputText = insertAtCursor(inputText, "Что делать при ошибке 402 Insufficient Quota или 429 Rate Limit?", "Что делать при ошибке 402 Insufficient Quota или 429 Rate Limit?".length) },
                                    label = { Text("⚠️ Ошибки 402/429", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                AssistChip(
                                    onClick = { inputText = insertAtCursor(inputText, "Расскажи про Telegram-канал движка @untitledAI и поддержку.", "Расскажи про Telegram-канал движка @untitledAI и поддержку.".length) },
                                    label = { Text("📢 Канал @untitledAI", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        } else {
                            item {
                                LiquidGlassPillButton(
                                    text = "**${character.name}**",
                                    onClick = { inputText = insertAtCursor(inputText, "****", 2) },
                                    userSettings = userSettings,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            item {
                                LiquidGlassPillButton(
                                    text = "*действие*",
                                    onClick = { inputText = insertAtCursor(inputText, "**", 1) },
                                    userSettings = userSettings,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                            item {
                                LiquidGlassPillButton(
                                    text = "<мысль>",
                                    onClick = { inputText = insertAtCursor(inputText, "<>", 1) },
                                    userSettings = userSettings
                                )
                            }
                        }
                    }
                }

                // Input Bar with Liquid Glass
                Surface(
                    color = glassBg,
                    shape = if (isGlassEnabled) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) else RoundedCornerShape(0.dp),
                    border = if (isGlassEnabled) BorderStroke(1.dp, glassBorderColor) else null,
                    modifier = Modifier
                        .studioElement("chat_input_bar")
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // Attachment Preview Banner (Photo or Video, up to 10)
                        if (isMediaProcessing) {
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = mediaProcessingStatus.ifBlank { "🎬 Извлечение ключевых кадров видео..." },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Подготовка раскадровки для Vision-модели ИИ",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (attachedMediaList.isNotEmpty()) {
                            val photoCount = attachedMediaList.count { it is com.example.util.ChatMediaItem.Photo }
                            val videoCount = attachedMediaList.count { it is com.example.util.ChatMediaItem.Video }
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "📎 Вложения: ${attachedMediaList.size}/${com.example.util.ChatMediaHelper.MAX_MEDIA_COUNT} (фото: $photoCount, видео: $videoCount)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Очистить все",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.clickable {
                                                attachedMediaList = emptyList()
                                                HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        itemsIndexed(attachedMediaList) { index, item ->
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            ) {
                                                val previewModel = when (item) {
                                                    is com.example.util.ChatMediaItem.Photo -> item.base64Data
                                                    is com.example.util.ChatMediaItem.Video -> item.thumbnailBase64
                                                }
                                                AppAsyncImage(
                                                    model = previewModel,
                                                    contentDescription = "Вложение #$index",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clickable {
                                                            selectedPreviewImage = previewModel
                                                        }
                                                )
                                                if (item is com.example.util.ChatMediaItem.Video) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color.Black.copy(alpha = 0.65f),
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .align(Alignment.Center)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.padding(2.dp)
                                                        )
                                                    }
                                                    if (item.durationSec > 0) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color.Black.copy(alpha = 0.7f),
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .padding(2.dp)
                                                        ) {
                                                            Text(
                                                                text = "${item.durationSec}s",
                                                                color = Color.White,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                modifier = Modifier.padding(horizontal = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                // Remove button
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(2.dp)
                                                        .size(18.dp)
                                                        .clickable {
                                                            attachedMediaList = attachedMediaList.filterIndexed { i, _ -> i != index }
                                                            HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                                        }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Удалить",
                                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                                        modifier = Modifier.padding(2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isRecordingAudio) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                VoiceRecordingBar(
                                    durationSec = recordingDurationSec,
                                    amplitudes = recordingAmplitudes,
                                    recognizedText = recognizedSpeechText,
                                    accentColor = Color(userSettings.accentColorLong),
                                    onCancel = {
                                        voiceRecorder.cancelRecording()
                                        HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                    },
                                    onSend = {
                                        val result = voiceRecorder.stopRecording()
                                        if (result != null) {
                                            HapticHelper.performSuccessHaptic(context, userSettings.naturalHapticsEnabled)
                                            isLocalAITyping = true
                                            onSendVoiceMessage(
                                                result.recognizedText,
                                                result.audioFile.absolutePath,
                                                result.durationSec
                                            )
                                        } else {
                                            Toast.makeText(context, "Запись не сохранена (слишком короткая)", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Unified Media Attach Button (Paperclip) with Photo 1 Popup Menu
                                Box {
                                    IconButton(
                                        onClick = { showAttachMenu = !showAttachMenu },
                                        modifier = Modifier
                                            .studioElement("chat_attach_photo_btn")
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AttachFile,
                                            contentDescription = "Прикрепить фото или видео",
                                            tint = if (attachedMediaList.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showAttachMenu,
                                        onDismissRequest = { showAttachMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(if (isFemboy) "Прикрепить фоточку" else if (isEn) "Attach Photo" else "Прикрепить фото") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.AddPhotoAlternate,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            onClick = {
                                                showAttachMenu = false
                                                imagePickerLauncher.launch("image/*")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isFemboy) "Прикрепить видяшку" else if (isEn) "Attach Video" else "Прикрепить видео") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.VideoFile,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                            },
                                            onClick = {
                                                showAttachMenu = false
                                                videoPickerLauncher.launch("video/*")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isFemboy) "Голосовая реплика 🎙" else if (isEn) "Voice Message 🎙" else "Голосовое сообщение 🎙") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Mic,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            },
                                            onClick = {
                                                showAttachMenu = false
                                                val permissionCheck = androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context, android.Manifest.permission.RECORD_AUDIO
                                                )
                                                if (permissionCheck == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                    voiceRecorder.startRecording()
                                                    HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                                } else {
                                                    recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                val rememberedVisualTransformation = remember(userSettings.accentColorLong, glassContentColor) {
                                    RoleplayMarkdownVisualTransformation(
                                        accentColor = Color(userSettings.accentColorLong),
                                        textColor = glassContentColor
                                    )
                                }

                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = {
                                        val textChanged = it.text != inputText.text
                                        inputText = it
                                        latestDraftRef[0] = it.text
                                        if (textChanged) {
                                            onDraftChange(it.text)
                                        }
                                    },
                                    visualTransformation = rememberedVisualTransformation,
                                    placeholder = {
                                        Text(
                                            text = if (isFemboy) "сообщение... (uwu)" else if (isEn) "message..." else "сообщение...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = glassSubtitleColor.copy(alpha = 0.8f)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp, max = 120.dp)
                                        .onGloballyPositioned { coords ->
                                            if (coords.isAttached) {
                                                val topY = coords.positionInWindow().y
                                                onChatInputTopYChanged?.invoke(topY)
                                            }
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = if (isGlassEnabled) glassBorderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        focusedContainerColor = glassInputBg,
                                        unfocusedContainerColor = glassInputBg,
                                        focusedTextColor = glassContentColor,
                                        unfocusedTextColor = glassContentColor
                                    )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                if (isAITyping) {
                                    // Stop generation button
                                    IconButton(
                                        onClick = {
                                            isLocalAITyping = false
                                            HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                            onStopGeneration()
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = if (isEn) "Stop generation" else "Остановить генерацию",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    val hasContent = inputText.text.isNotBlank() || attachedMediaList.isNotEmpty()

                                    if (hasContent) {
                                        IconButton(
                                            onClick = {
                                                val textToSend = inputText.text
                                                val payloadToSend = com.example.util.ChatMediaHelper.serialize(attachedMediaList)
                                                inputText = TextFieldValue("")
                                                onDraftChange("")
                                                attachedMediaList = emptyList()
                                                isLocalAITyping = true
                                                HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                                onSendMessage(textToSend, payloadToSend)
                                            },
                                            modifier = Modifier
                                                .studioElement("chat_send_btn")
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = if (isEn) "Send" else "Отправить",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        // Photo 1: Token Limit Circle Button (50%) -> Click to skip turn / generate response!
                                        val contextLimit = userSettings.contextWindowSize * 150
                                        val currentTokens = remember(messages) { messages.takeLast(userSettings.contextWindowSize).sumOf { it.content.length / 3 } }
                                        val limitPercent = ((currentTokens.toFloat() / contextLimit.toFloat()) * 100).toInt().coerceIn(10, 100)

                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                            border = BorderStroke(1.8.dp, MaterialTheme.colorScheme.primary),
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    inputText = TextFieldValue("")
                                                    onDraftChange("")
                                                    attachedMediaList = emptyList()
                                                    isLocalAITyping = true
                                                    HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                                    onSendMessage("", null)
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${limitPercent}%",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary,
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

    // Fullscreen Image Dialog
    if (selectedPreviewImage != null) {
        Dialog(onDismissRequest = { selectedPreviewImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { selectedPreviewImage = null }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AppAsyncImage(
                    model = selectedPreviewImage,
                    contentDescription = "Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }

    // Persona Selection Sheet
    if (showPersonaSheet) {
        SelectChatPersonaSheet(
            allPersonas = allPersonas,
            globalActivePersona = activePersona,
            currentChatPersonaId = chat.personaId,
            onSelectPersona = { personaId ->
                onSelectChatPersona(personaId)
            },
            onDismiss = { showPersonaSheet = false }
        )
    }

    // Branches Selection Dialog
    if (showBranchesDialog) {
        AlertDialog(
            onDismissRequest = { showBranchesDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFemboy) "Ветки диалога 🐾" else if (isEn) "Dialogue Branches" else "Ветки диалога")
                    }
                    Text(
                        text = if (isEn) "${allChatsForCharacter.size} total" else "${allChatsForCharacter.size} всего",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tabs: Активные vs Архив
                    PrimaryTabRow(
                        selectedTabIndex = branchesTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = branchesTab == 0,
                            onClick = { branchesTab = 0 },
                            text = { Text(if (isEn) "Active (${activeBranches.size})" else "Активные (${activeBranches.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = branchesTab == 1,
                            onClick = { branchesTab = 1 },
                            text = { Text(if (isFemboy) "Архивчик (${archivedBranches.size})" else if (isEn) "Archive (${archivedBranches.size})" else "Архив (${archivedBranches.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    // Sorting Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = chatSortMode == "updated_desc",
                                onClick = { chatSortMode = "updated_desc" },
                                label = { Text(if (isEn) "🕒 Recent" else "🕒 Свежие", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = chatSortMode == "updated_asc",
                                onClick = { chatSortMode = "updated_asc" },
                                label = { Text(if (isEn) "⏳ Oldest" else "⏳ Старые", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = chatSortMode == "title_asc",
                                onClick = { chatSortMode = "title_asc" },
                                label = { Text(if (isEn) "🔤 A-Z" else "🔤 А-Я", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = chatSortMode == "created_desc",
                                onClick = { chatSortMode = "created_desc" },
                                label = { Text(if (isEn) "📅 Creation" else "📅 Создание", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (sortedChats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (branchesTab == 0) (if (isEn) "No active dialogues" else "Нет активных диалогов") else (if (isFemboy) "Архивчик пуст" else if (isEn) "Archive is empty" else "Архив пуст"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(sortedChats, key = { it.id }) { itemChat ->
                                val isCurrent = itemChat.id == chat.id
                                val branchDisplayTitle = if (character.isAssistant && (itemChat.title.startsWith("Помощь") || itemChat.title.startsWith("Диалог"))) {
                                    if (isFemboy) "Помощь и мурчалки 🐾" else if (isEn) "Help & Consultation" else itemChat.title
                                } else if (isEn && itemChat.title.startsWith("Диалог ")) {
                                    itemChat.title.replace("Диалог ", "Dialogue ")
                                } else itemChat.title

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else if (itemChat.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            showBranchesDialog = false
                                            if (!isCurrent) onSwitchChat(itemChat.id)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (itemChat.isPinned) {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                Text(
                                                    text = branchDisplayTitle,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Pin
                                            IconButton(
                                                onClick = { onTogglePinChat(itemChat.id, !itemChat.isPinned) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (itemChat.isPinned) Icons.Default.Star else Icons.Default.StarOutline,
                                                    contentDescription = if (isEn) "Pin" else "Закрепить",
                                                    tint = if (itemChat.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            // Archive
                                            IconButton(
                                                onClick = { onToggleArchiveChat(itemChat.id, !itemChat.isArchived) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (itemChat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                    contentDescription = if (isEn) "Archive" else "Архив",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            // Rename
                                            IconButton(
                                                onClick = {
                                                    renamingChatId = itemChat.id
                                                    renamingChatTitle = itemChat.title
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = if (isEn) "Rename" else "Переименовать",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            // Delete (only if more than 1 chat exists or it is not current)
                                            IconButton(
                                                onClick = { deletingChatId = itemChat.id },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = if (isEn) "Delete" else "Удалить",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBranchesDialog = false
                        newChatTitleInput = (if (isEn) "Dialogue " else "Диалог ") + (allChatsForCharacter.size + 1)
                        showNewChatDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFemboy) "+ Новый чатик" else if (isEn) "+ New Dialogue" else "+ Новый диалог")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBranchesDialog = false }) {
                    Text(if (isFemboy) "Закрыть лапкой 🐾" else if (isEn) "Close" else "Закрыть")
                }
            }
        )
    }

    // Rename Chat Dialog inside ChatScreen
    if (renamingChatId != null) {
        AlertDialog(
            onDismissRequest = { renamingChatId = null },
            title = { Text(if (isEn) "Rename branch" else "Переименовать ветку") },
            text = {
                OutlinedTextField(
                    value = renamingChatTitle,
                    onValueChange = { renamingChatTitle = it },
                    label = { Text(if (isEn) "Title" else "Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTitle = renamingChatTitle.trim()
                        if (newTitle.isNotBlank()) {
                            onRenameChat(renamingChatId!!, newTitle)
                            renamingChatId = null
                        }
                    },
                    enabled = renamingChatTitle.isNotBlank()
                ) {
                    Text(if (isEn) "Save" else "Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingChatId = null }) {
                    Text(if (isEn) "Cancel" else "Отмена")
                }
            }
        )
    }

    // Delete Chat Confirmation Dialog inside ChatScreen
    if (deletingChatId != null) {
        AlertDialog(
            onDismissRequest = { deletingChatId = null },
            title = { Text(if (isEn) "Delete dialogue branch?" else "Удалить ветку диалога?") },
            text = { Text(if (isEn) "Message history for this branch will be permanently deleted." else "История сообщений этой ветки будет удалена без возможности восстановления.") },
            confirmButton = {
                Button(
                    onClick = {
                        val target = deletingChatId!!
                        deletingChatId = null
                        onDeleteChat(target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isEn) "Delete" else "Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingChatId = null }) {
                    Text(if (isEn) "Cancel" else "Отмена")
                }
            }
        )
    }

    // New Chat Title Dialog
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text(if (isFemboy) "Начать новый чатик 🐾" else if (isEn) "Start New Dialogue" else "Начать новый диалог") },
            text = {
                Column {
                    Text(
                        if (isEn) "A new storyline branch with clean memory will be created:" else "Будет создана новая сюжетная линия с чистой памятью:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newChatTitleInput,
                        onValueChange = { newChatTitleInput = it },
                        label = { Text(if (isEn) "Branch Name" else "Название ветки") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNewChatDialog = false
                        onCreateNewChat(newChatTitleInput)
                    },
                    enabled = newChatTitleInput.isNotBlank()
                ) {
                    Text(if (isFemboy) "Создать (ня~)" else if (isEn) "Create" else "Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text(if (isEn) "Cancel" else "Отмена")
                }
            }
        )
    }

    // Clear Chat Confirmation Dialog
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text(if (isEn) "Clear messages?" else "Очистить сообщения?") },
            text = { Text(if (isEn) "All messages in the current conversation will be cleared, and character greeting will be reset to start." else "Все сообщения текущего диалога будут удалены, а приветствие персонажа будет сброшено в начало.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearChatDialog = false
                        onClearChat()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isFemboy) "Стереть" else if (isEn) "Clear" else "Очистить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text(if (isEn) "Cancel" else "Отмена")
                }
            }
        )
    }

    // Message Actions (Edit / Delete / Copy) Bottom Sheet or Dialog
    if (showMessageActionSheet && selectedMessageForAction != null) {
        val msg = selectedMessageForAction!!
        val isUserMsg = msg.sender == "user"
        val isLastMsg = messages.lastOrNull()?.id == msg.id

        AlertDialog(
            onDismissRequest = { showMessageActionSheet = false },
            title = {
                Text(if (isUserMsg) "Ваше сообщение" else "Реплика ${character.name}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Reactions and AI feedback signal
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isEn) "Reaction & AI Feedback" else "Реакция и фидбек для ИИ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isEn) "Guide the plot trajectory and bot response style" else "Направьте сюжет и стиль ответа персонажа",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(com.example.util.MessageReactionHelper.ALL_REACTIONS) { reactionItem ->
                                    val isSelected = msg.reaction == reactionItem.emoji
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val newReaction = if (isSelected) null else reactionItem.emoji
                                            onSetReaction(msg.id, newReaction)
                                            selectedMessageForAction = msg.copy(reaction = newReaction)
                                            HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                        },
                                        label = {
                                            Text(
                                                text = "${reactionItem.emoji} ${reactionItem.displayTitle(appLang)}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (reactionItem.isPositive)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.errorContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Regenerate Alternative Variant (Branching) for AI messages - only for latest message
                    if (!isUserMsg && isLastMsg) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showMessageActionSheet = false
                                    hiddenMessageId = msg.id
                                    isLocalAITyping = true
                                    HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                    onRegenerateVariant(msg.id)
                                }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Сгенерировать другой вариант ответа", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Создает альтернативную ветку ответа с возможностью переключения",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Copy
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Message Text", msg.content)
                                clipboard.setPrimaryClip(clip)
                                HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                Toast.makeText(context, "Текст скопирован", Toast.LENGTH_SHORT).show()
                                showMessageActionSheet = false
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Скопировать текст", fontWeight = FontWeight.Medium)
                        }
                    }

                    // Edit
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showMessageActionSheet = false
                                inlineEditingMessageId = msg.id
                                inlineEditingText = msg.content
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Редактировать", fontWeight = FontWeight.Medium)
                                Text(
                                    if (isUserMsg) "Прямое редактирование с возможностью перегенерации" else "Редактирование реплики прямо в чате",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Branching (Разветвление)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showMessageActionSheet = false
                                HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                onBranchChat(msg.id)
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Разветвление", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text(
                                    "Создать новый диалог от этого сообщения со всеми хрониками до этого момента",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Delete - allows deleting both user message and bot replica
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showMessageActionSheet = false
                                HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                onDeleteMessage(msg.id)
                                Toast.makeText(
                                    context,
                                    if (isUserMsg) "Сообщение удалено" else "Реплика бота удалена",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (isUserMsg) "Удалить сообщение" else "Удалить реплику бота",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMessageActionSheet = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (showNotesDialog) {
        var tempNotes by remember(showNotesDialog, chat.notes) { mutableStateOf(chat.notes) }
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFemboy) "Заметочки к чатику" else if (isEn) "Chat Notes" else "Примечания к чату")
                }
            },
            text = {
                Column {
                    Text(
                        if (isEn) "Notes are sent to AI along with system prompt, character description, and history. Describe key plot points, current location, rules, or context." else "Заметки отправляются к ИИ вместе с системным промптом, описанием персонажа и историей. Опишите важные детали сюжета, текущую локацию, правила или контекст.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempNotes,
                        onValueChange = { tempNotes = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 260.dp),
                        placeholder = { Text(if (isEn) "e.g. We are inside an old tavern. Rain is falling outside..." else "Например: Мы находимся в старой таверне. На улице идет дождь...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateChatNotes(chat.id, tempNotes)
                        notesInput = tempNotes
                        HapticHelper.performSuccessHaptic(context, userSettings.naturalHapticsEnabled)
                        Toast.makeText(context, if (isEn) "Notes saved" else "Примечания сохранены", Toast.LENGTH_SHORT).show()
                        showNotesDialog = false
                    }
                ) {
                    Text(if (isEn) "Save" else "Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialog = false }) {
                    Text(if (isEn) "Cancel" else "Отмена")
                }
            }
        )
    }
    }

    // System Prompt Viewer Dialog
    if (showSystemPromptDialog) {
        val lastSummary = remember(summaries) { summaries.lastOrNull() }
        val compiledPrompt = remember(chat, character, activePersona, lastSummary, latestRelationship, userSettings, appLang) {
            com.example.ai.engine.RoleplayPromptBuilder.buildSystemPrompt(
                character = character,
                chat = chat,
                latestSummary = lastSummary,
                latestRelationship = latestRelationship,
                userPersona = activePersona,
                settingsSummary = if (character.isAssistant) (if (isEn) "Assistant mode" else "Режим ассистента") else "",
                customTemplate = userSettings.customSystemPrompt,
                userSettings = userSettings,
                lang = appLang
            )
        }

        AlertDialog(
            onDismissRequest = { showSystemPromptDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEn) "System Prompt" else "Системный промпт", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = if (isFemboy) "Персик: ${character.getDisplayName(appLang)}" else if (isEn) "Character: ${character.getDisplayName(appLang)}" else "Персонаж: ${character.getDisplayName(appLang)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = if (isEn) "${compiledPrompt.length} chars (~${compiledPrompt.length / 4} tokens)" else "${compiledPrompt.length} симв. (~${compiledPrompt.length / 4} токенов)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp),
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
                                    text = compiledPrompt,
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
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("System Prompt", compiledPrompt)
                        clipboard.setPrimaryClip(clip)
                        HapticHelper.performSuccessHaptic(context, userSettings.naturalHapticsEnabled)
                        Toast.makeText(context, if (isEn) "System prompt copied to clipboard" else "Системный промпт скопирован в буфер", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFemboy) "Цап-царап (Копировать)" else if (isEn) "Copy" else "Копировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSystemPromptDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    if (assistantCreatorMode != null) {
        val isBotMode = assistantCreatorMode == "bot"
        Dialog(
            onDismissRequest = {
                if (!isAssistantCreatorLoading) {
                    assistantCreatorMode = null
                }
            }
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isBotMode) "🤖 Помоги создать бота!" else "🎭 Помоги создать персону!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!isAssistantCreatorLoading) {
                            IconButton(onClick = { assistantCreatorMode = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }
                    }

                    Text(
                        text = if (isBotMode) {
                            "Опиши свои пожелания к боту (имя, характер, внешность, вселенная, завязка). Ассистент обратится к API, сам напишет персонажа и автоматически вставит текст во все нужные поля редактора!"
                        } else {
                            "Опиши свои пожелания к персоне игрока (имя, возраст, характер, внешность, роль). Ассистент обратится к API, напишет анкету и автоматически заполнит и активирует твою Персону!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = assistantCreatorWishes,
                        onValueChange = {
                            assistantCreatorWishes = it
                            assistantCreatorError = null
                        },
                        enabled = !isAssistantCreatorLoading,
                        placeholder = {
                            Text(
                                if (isBotMode) {
                                    "Например: Хочу загадочного детектива-вампира в нуарном городе, который расследует дело вместе с игроком..."
                                } else {
                                    "Например: Молодой наёмник с кибернетической рукой, саркастичный, но верный друзьям..."
                                }
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("assistant_creator_wishes_input")
                    )

                    if (assistantCreatorError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = assistantCreatorError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (isAssistantCreatorLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            Text(
                                text = if (isBotMode) "Ассистент пишет бота и заполняет поля..." else "Ассистент пишет персону и заполняет поля...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(
                            onClick = { assistantCreatorMode = null },
                            enabled = !isAssistantCreatorLoading
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                val wishes = assistantCreatorWishes.trim()
                                if (wishes.isBlank()) return@Button
                                isAssistantCreatorLoading = true
                                assistantCreatorError = null
                                if (isBotMode) {
                                    val handler = onAssistantCreateBot
                                    if (handler == null) {
                                        isAssistantCreatorLoading = false
                                        assistantCreatorError = "Функция создания бота недоступна"
                                        return@Button
                                    }
                                    handler(
                                        wishes,
                                        {
                                            isAssistantCreatorLoading = false
                                            assistantCreatorMode = null
                                        },
                                        { err: String ->
                                            isAssistantCreatorLoading = false
                                            assistantCreatorError = err
                                        }
                                    )
                                } else {
                                    val handler = onAssistantCreatePersona
                                    if (handler == null) {
                                        isAssistantCreatorLoading = false
                                        assistantCreatorError = "Функция создания персоны недоступна"
                                        return@Button
                                    }
                                    handler(
                                        wishes,
                                        {
                                            isAssistantCreatorLoading = false
                                            assistantCreatorMode = null
                                        },
                                        { err: String ->
                                            isAssistantCreatorLoading = false
                                            assistantCreatorError = err
                                        }
                                    )
                                }
                            },
                            enabled = assistantCreatorWishes.isNotBlank() && !isAssistantCreatorLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("assistant_creator_submit_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Создать и заполнить")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItemView(
    message: MessageEntity,
    character: CharacterEntity,
    userSettings: UserSettings,
    isLastMessage: Boolean,
    isGreetingMessage: Boolean = false,
    isEditing: Boolean = false,
    editingText: String = message.content,
    onEditingTextChange: (String) -> Unit = {},
    onSaveEdit: () -> Unit = {},
    onSaveAndRegenerate: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onImageClick: (String) -> Unit,
    onRegenerateVariant: (Long) -> Unit = {},
    onSwitchVariant: (Long, Int) -> Unit = { _, _ -> },
    onEditMessage: (Long, String) -> Unit = { _, _ -> },
    onSetReaction: (Long, String?) -> Unit = { _, _ -> },
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (message.sender == "system") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
        return
    }

    val isUser = message.sender == "user"
    val isAssistant = character.name.equals("Untitled Ассистент", ignoreCase = true)
    val isNovel = userSettings.bubbleStyle == "novel"
    val showAvatar = userSettings.showAvatarInChat && !isNovel

    // Variant calculation cached across LazyColumn item recycling
    val variantsCount = remember(message.id, message.variantsJson) {
        val raw = message.variantsJson
        if (raw.isNullOrBlank()) {
            1
        } else {
            val cacheKey = message.id * 31L + raw.length.toLong()
            synchronized(variantsCountCache) {
                variantsCountCache.get(cacheKey)
            } ?: run {
                val parsedCount = try {
                    org.json.JSONArray(raw).length().coerceAtLeast(1)
                } catch (_: Exception) { 1 }
                synchronized(variantsCountCache) {
                    variantsCountCache.put(cacheKey, parsedCount)
                }
                parsedCount
            }
        }
    }
    val currentIdx = message.currentVariantIndex.coerceIn(0, (variantsCount - 1).coerceAtLeast(0))

    val canSwipeVariants = !isUser && !isEditing && (variantsCount > 1 || isLastMessage)

    // Two-way swipe state (only allocated and tracked when message can actually be swiped):
    // Swipe left (справа - влево): next variant, or regenerate if on newest variant
    // Swipe right (слева - вправо): previous variant
    var swipeDragOffset by remember { mutableFloatStateOf(0f) }
    val swipeOffsetX = if (canSwipeVariants) remember { Animatable(0f) } else null
    val density = LocalDensity.current
    val triggerThresholdPx = if (canSwipeVariants) with(density) { 65.dp.toPx() } else 0f
    val maxDragPx = if (canSwipeVariants) with(density) { 105.dp.toPx() } else 0f

    val swipeModifier = if (canSwipeVariants && swipeOffsetX != null) {
        Modifier.pointerInput(message.id, currentIdx, variantsCount, isLastMessage) {
            val canGoRight = currentIdx > 0
            val canGoLeft = (currentIdx < variantsCount - 1) || isLastMessage
            val minDragAllowed = if (canGoLeft) -maxDragPx else 0f
            val maxDragAllowed = if (canGoRight) maxDragPx else 0f

            detectHorizontalDragGestures(
                onDragEnd = {
                    val offset = swipeDragOffset
                    coroutineScope.launch {
                        if (offset <= -triggerThresholdPx) {
                            if (currentIdx < variantsCount - 1) {
                                HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                onSwitchVariant(message.id, currentIdx + 1)
                            } else if (isLastMessage) {
                                HapticHelper.performSuccessHaptic(context, userSettings.naturalHapticsEnabled)
                                onRegenerateVariant(message.id)
                            }
                        } else if (offset >= triggerThresholdPx) {
                            if (currentIdx > 0) {
                                HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                onSwitchVariant(message.id, currentIdx - 1)
                            }
                        }
                        swipeOffsetX.snapTo(swipeDragOffset)
                        swipeDragOffset = 0f
                        swipeOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onDragCancel = {
                    coroutineScope.launch {
                        swipeOffsetX.snapTo(swipeDragOffset)
                        swipeDragOffset = 0f
                        swipeOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                onHorizontalDrag = { _, dragAmount ->
                    swipeDragOffset = (swipeDragOffset + dragAmount * 0.75f).coerceIn(minDragAllowed, maxDragAllowed)
                }
            )
        }
    } else {
        Modifier
    }

    val contentModifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {},
            onLongClick = if (isEditing) ({}) else onLongClick
        )

    val innerContent: @Composable () -> Unit = {
        val novelAlignment = if (isNovel) {
            when (userSettings.novelTextAlignment) {
                "center" -> Alignment.CenterHorizontally
                "right" -> Alignment.End
                else -> Alignment.Start
            }
        } else {
            Alignment.Start
        }
        Column(
            modifier = Modifier.padding(if (isNovel) 4.dp else 12.dp),
            horizontalAlignment = novelAlignment
        ) {
            // Divider in Novel mode for both bot and user messages when enabled
            if (isNovel && userSettings.novelDividerEnabled) {
                val parsedDividerColor = remember(userSettings.novelDividerColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(userSettings.novelDividerColorHex))
                    } catch (_: Exception) { Color.White }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(userSettings.novelDividerWidthPercent / 100f)
                            .height(userSettings.novelDividerThicknessDp.dp)
                            .background(
                                color = parsedDividerColor.copy(alpha = userSettings.novelDividerAlpha),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            // Header: Avatar & Name
            // In novel mode, bot's name is removed per user request. User's label is kept subtle or shown in bubble mode.
            val showHeader = if (isNovel) isUser else true
            if (showHeader) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    if (!isUser && showAvatar) {
                        AvatarImage(
                            uri = character.avatarUri,
                            contentDescription = character.name,
                            size = 24.dp,
                            frameUri = character.avatarFrameUri,
                            modifier = Modifier.studioElement("chat_bot_avatar")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isUser) "ВЫ" else character.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) Color(userSettings.accentColorLong) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            val effectiveMediaUrl = if (isGreetingMessage && character.greetingBannerUri.isNotBlank()) {
                character.greetingBannerUri
            } else if (!message.imageUrl.isNullOrBlank()) {
                message.imageUrl
            } else null

            // Photo / Video if present (supports up to 10 media items) or Greeting Banner
            if (!effectiveMediaUrl.isNullOrBlank()) {
                val mediaItems = remember(effectiveMediaUrl) {
                    com.example.util.ChatMediaHelper.deserialize(effectiveMediaUrl)
                }

                if (mediaItems.size <= 1) {
                    val singleItem = mediaItems.firstOrNull()
                    val isVideoMsg = singleItem is com.example.util.ChatMediaItem.Video || effectiveMediaUrl.startsWith("video_frames:")
                    val modelToLoad = when (singleItem) {
                        is com.example.util.ChatMediaItem.Photo -> singleItem.base64Data
                        is com.example.util.ChatMediaItem.Video -> singleItem.thumbnailBase64
                        else -> effectiveMediaUrl
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isGreetingMessage && message.imageUrl.isNullOrBlank()) 200.dp else 220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .clickable { onImageClick(modelToLoad) }
                    ) {
                        AppAsyncImage(
                            model = modelToLoad,
                            contentDescription = message.imagePrompt ?: if (isVideoMsg) "Video" else if (isGreetingMessage) "Приветственный баннер" else "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isVideoMsg) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Видео",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(bottomStart = 8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = if (isVideoMsg) "🎬 Видео" else if (isUser) "📷 Ваше фото" else "📷 Иллюстрация",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Multi-media gallery (up to 10 items)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = "📎 Прикреплено ${mediaItems.size} медиа:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(mediaItems) { idx, item ->
                                val previewModel = when (item) {
                                    is com.example.util.ChatMediaItem.Photo -> item.base64Data
                                    is com.example.util.ChatMediaItem.Video -> item.thumbnailBase64
                                }
                                Box(
                                    modifier = Modifier
                                        .size(130.dp, 130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onImageClick(previewModel) }
                                ) {
                                    AppAsyncImage(
                                        model = previewModel,
                                        contentDescription = "Медиа #$idx",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (item is com.example.util.ChatMediaItem.Video) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .align(Alignment.Center)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Видео",
                                                tint = Color.White,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Black.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = if (item.durationSec > 0) "${item.durationSec}s" else "video",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 3.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(bottomStart = 6.dp),
                                        color = Color.Black.copy(alpha = 0.65f),
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Text(
                                            text = "${idx + 1}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isEditing) {
                // Inline Markdown Editor with single unified editing block
                var tfValue by remember(isEditing) {
                    mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(text = editingText, selection = androidx.compose.ui.text.TextRange(editingText.length)))
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUser) "✏️ Редактирование сообщения" else "✏️ Редактирование реплики бота",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick formatting toolbar: ****, *действие*, <мысль>
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LiquidGlassPillButton(
                                text = "****",
                                onClick = {
                                    val currentText = tfValue.text
                                    val start = tfValue.selection.start.coerceIn(0, currentText.length)
                                    val end = tfValue.selection.end.coerceIn(0, currentText.length)
                                    val newText = if (start != end) {
                                        currentText.substring(0, start) + "**" + currentText.substring(start, end) + "**" + currentText.substring(end)
                                    } else {
                                        currentText.substring(0, start) + "****" + currentText.substring(start)
                                    }
                                    val newCursor = if (start != end) end + 4 else start + 2
                                    val newTf = androidx.compose.ui.text.input.TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(newCursor))
                                    tfValue = newTf
                                    onEditingTextChange(newText)
                                },
                                userSettings = userSettings,
                                fontWeight = FontWeight.Bold
                            )
                            LiquidGlassPillButton(
                                text = "действие",
                                onClick = {
                                    val currentText = tfValue.text
                                    val start = tfValue.selection.start.coerceIn(0, currentText.length)
                                    val end = tfValue.selection.end.coerceIn(0, currentText.length)
                                    val newText = if (start != end) {
                                        currentText.substring(0, start) + "*" + currentText.substring(start, end) + "*" + currentText.substring(end)
                                    } else {
                                        currentText.substring(0, start) + "**" + currentText.substring(start)
                                    }
                                    val newCursor = if (start != end) end + 2 else start + 1
                                    val newTf = androidx.compose.ui.text.input.TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(newCursor))
                                    tfValue = newTf
                                    onEditingTextChange(newText)
                                },
                                userSettings = userSettings,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            LiquidGlassPillButton(
                                text = "<мысль>",
                                onClick = {
                                    val currentText = tfValue.text
                                    val start = tfValue.selection.start.coerceIn(0, currentText.length)
                                    val end = tfValue.selection.end.coerceIn(0, currentText.length)
                                    val newText = if (start != end) {
                                        currentText.substring(0, start) + "<" + currentText.substring(start, end) + ">" + currentText.substring(end)
                                    } else {
                                        currentText.substring(0, start) + "<>" + currentText.substring(start)
                                    }
                                    val newCursor = if (start != end) end + 2 else start + 1
                                    val newTf = androidx.compose.ui.text.input.TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(newCursor))
                                    tfValue = newTf
                                    onEditingTextChange(newText)
                                },
                                userSettings = userSettings
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = tfValue,
                            onValueChange = { newTf ->
                                tfValue = newTf
                                onEditingTextChange(newTf.text)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 240.dp),
                            shape = RoundedCornerShape(8.dp),
                            placeholder = { Text("Текст сообщения...") },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = userSettings.fontSizeSp.sp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onCancelEdit,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Отмена", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            if (isUser) {
                                OutlinedButton(
                                    onClick = onSaveEdit,
                                    enabled = editingText.isNotBlank(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Только сохранить", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Button(
                                    onClick = onSaveAndRegenerate,
                                    enabled = editingText.isNotBlank(),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Перегенерировать", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = onSaveEdit,
                                    enabled = editingText.isNotBlank(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Сохранить", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                if (!message.audioFilePath.isNullOrBlank()) {
                    VoiceMessagePlayer(
                        audioFilePath = message.audioFilePath,
                        durationSec = message.audioDurationSec,
                        recognizedText = message.content,
                        accentColor = Color(userSettings.accentColorLong),
                        onTranscriptUpdated = { newText ->
                            onEditMessage(message.id, newText)
                        }
                    )
                } else {
                    val botTextAlign = if (isNovel) {
                        when (userSettings.novelTextAlignment) {
                            "center" -> androidx.compose.ui.text.style.TextAlign.Center
                            "right" -> androidx.compose.ui.text.style.TextAlign.Right
                            else -> androidx.compose.ui.text.style.TextAlign.Left
                        }
                    } else androidx.compose.ui.text.style.TextAlign.Unspecified

                    RoleplayText(
                        text = message.content,
                        accentColor = Color(userSettings.accentColorLong),
                        textColor = MaterialTheme.colorScheme.onSurface,
                        baseFontSizeSp = userSettings.fontSizeSp,
                        isRoleplayFormatting = !isAssistant,
                        textAlign = botTextAlign
                    )
                }
            }

            if (!isEditing && userSettings.showInternalReasoningDebug && !message.internalReasoning.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "💡 Рассуждение: ${message.internalReasoning}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(6.dp),
                        fontSize = 10.sp
                    )
                }
            }

            // Reaction Badge
            val reactionInfo = remember(message.reaction) {
                com.example.util.MessageReactionHelper.getReactionInfo(message.reaction)
            }
            if (reactionInfo != null && !isEditing) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (reactionInfo.isPositive)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                    border = BorderStroke(
                        1.dp,
                        if (reactionInfo.isPositive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.clickable { onLongClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = reactionInfo.emoji, fontSize = 13.sp)
                        Text(
                            text = reactionInfo.titleRu,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (reactionInfo.isPositive)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Variant Navigator for AI responses: ONLY shown if this is the latest message AND variantsCount > 1
            if (!isEditing && !isUser && isLastMessage && variantsCount > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .studioElement("chat_message_variant_bar")
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (currentIdx > 0) {
                                    HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                    onSwitchVariant(message.id, currentIdx - 1)
                                }
                            },
                            enabled = currentIdx > 0,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий вариант", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "${currentIdx + 1} / $variantsCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = {
                                if (currentIdx < variantsCount - 1) {
                                    HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                                    onSwitchVariant(message.id, currentIdx + 1)
                                }
                            },
                            enabled = currentIdx < variantsCount - 1,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Следующий вариант", modifier = Modifier.size(16.dp))
                        }
                    }

                    IconButton(
                        onClick = {
                            HapticHelper.performLightHaptic(context, userSettings.naturalHapticsEnabled)
                            onRegenerateVariant(message.id)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Создать альтернативный вариант", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(swipeModifier)
    ) {
        val currentSwipeVal = if (swipeOffsetX != null && swipeOffsetX.isRunning) swipeOffsetX.value else swipeDragOffset

        // Background reveal indicators for swipe navigation
        // 1. Swiping right-to-left (drag towards left, currentSwipeVal < -8f) -> Next variant or new response
        if (canSwipeVariants && swipeOffsetX != null && currentSwipeVal < -8f) {
            val absOffset = -currentSwipeVal
            val progress = (absOffset / triggerThresholdPx).coerceIn(0f, 1f)
            val isThresholdReached = absOffset >= triggerThresholdPx

            if (currentIdx < variantsCount - 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isThresholdReached) "Отпустите для переключения" else "Вариант ${currentIdx + 2}/$variantsCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(progress)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (isThresholdReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(34.dp)
                            .scale(0.7f + progress * 0.3f),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Следующий ответ",
                                tint = if (isThresholdReached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else if (isLastMessage) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isThresholdReached) "Отпустите для генерации" else "Новый ответ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.alpha(progress)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (isThresholdReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(34.dp)
                            .scale(0.7f + progress * 0.3f),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Новый ответ",
                                tint = if (isThresholdReached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(progress * 180f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Swiping left-to-right (drag towards right, currentSwipeVal > 8f) -> Previous variant
        if (canSwipeVariants && swipeOffsetX != null && currentSwipeVal > 8f && currentIdx > 0) {
            val progress = (currentSwipeVal / triggerThresholdPx).coerceIn(0f, 1f)
            val isThresholdReached = currentSwipeVal >= triggerThresholdPx

            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isThresholdReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(34.dp)
                        .scale(0.7f + progress * 0.3f),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Предыдущий ответ",
                            tint = if (isThresholdReached) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isThresholdReached) "Отпустите для переключения" else "Вариант ${currentIdx}/$variantsCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alpha(progress)
                )
            }
        }

        // Foreground Message Container
        val bubbleOffsetModifier = if (canSwipeVariants && swipeOffsetX != null) {
            Modifier.offset { androidx.compose.ui.unit.IntOffset(currentSwipeVal.toInt(), 0) }
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .studioElement(if (isUser) "chat_user_bubble" else "chat_bot_bubble")
                .then(bubbleOffsetModifier)
                .fillMaxWidth()
        ) {
            if (isNovel) {
                Box(modifier = contentModifier) {
                    innerContent()
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isUser) {
                        Color(userSettings.accentColorLong).copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isUser) Color(userSettings.accentColorLong).copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    ),
                    modifier = contentModifier
                ) {
                    innerContent()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectChatPersonaSheet(
    allPersonas: List<UserPersonaEntity>,
    globalActivePersona: UserPersonaEntity?,
    currentChatPersonaId: Long?,
    onSelectPersona: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Персона для этого диалога",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Выберите, от чьего лица вы общаетесь с ботом в этой ветке",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: Global default persona
            val isGlobalSelected = currentChatPersonaId == null
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isGlobalSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    if (isGlobalSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectPersona(null)
                        onDismiss()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Глобальная персона по умолчанию",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (globalActivePersona != null) "Активна: ${globalActivePersona.name} (${globalActivePersona.gender.ifBlank { "Персона" }})" else "Персона не выбрана в профиле",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isGlobalSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Выбрано",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (allPersonas.isNotEmpty()) {
                Text(
                    "Сохраненные персоны (${allPersonas.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allPersonas, key = { it.id }) { persona ->
                        val isThisSelected = currentChatPersonaId == persona.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isThisSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isThisSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPersona(persona.id)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarImage(
                                    uri = persona.avatarUri,
                                    contentDescription = persona.name,
                                    size = 42.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = persona.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (persona.gender.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${persona.gender}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (persona.age > 0) {
                                            Text(
                                                text = ", ${persona.age} лет",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (persona.shortDescription.isNotBlank()) {
                                        Text(
                                            text = persona.shortDescription,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isThisSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Выбрано",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
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

private val variantsCountCache = androidx.collection.LruCache<Long, Int>(500)
private val dateHeaderCache = androidx.collection.LruCache<Long, String>(100)
private val ruDateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
private val cachedLocalTzOffsetMs = java.util.TimeZone.getDefault().rawOffset.toLong()

private fun getEpochDay(timestamp: Long): Long {
    return (timestamp + cachedLocalTzOffsetMs) / 86_400_000L
}

private fun formatMessageDateHeader(timestamp: Long, isEn: Boolean = false): String {
    if (timestamp <= 0L) return ""
    val day = getEpochDay(timestamp)

    val today = getEpochDay(System.currentTimeMillis())
    val formatted = when (today - day) {
        0L -> if (isEn) "Today" else "Сегодня"
        1L -> if (isEn) "Yesterday" else "Вчера"
        else -> {
            if (isEn) {
                java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.ENGLISH).format(java.util.Date(timestamp))
            } else {
                synchronized(ruDateFormat) { ruDateFormat.format(java.util.Date(timestamp)) }
            }
        }
    }
    return formatted
}

private fun isNewDay(prevTimestamp: Long, currentTimestamp: Long): Boolean {
    if (prevTimestamp <= 0L || currentTimestamp <= 0L) return true
    return getEpochDay(prevTimestamp) != getEpochDay(currentTimestamp)
}
