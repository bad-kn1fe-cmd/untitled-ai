package com.example.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.engine.AiProviderEngine
import com.example.data.datastore.UserPreferencesRepository
import com.example.data.datastore.UserSettings
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.AiProviderRepository
import com.example.data.repository.BackupManifest
import com.example.data.repository.BackupRepository
import com.example.data.repository.RoleplayRepository
import com.example.util.BackgroundGenerationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val roleplayRepository = RoleplayRepository(db)
    val providerRepository = AiProviderRepository(db)
    val preferencesRepository = UserPreferencesRepository(application)
    val backupRepository = BackupRepository(application, db)
    val storeRepository = com.example.data.repository.StoreRepository(application)

    sealed interface StoreUiState {
        object Idle : StoreUiState
        object Loading : StoreUiState
        data class Success(val catalog: com.example.data.repository.StoreCatalog) : StoreUiState
        data class Offline(val message: String) : StoreUiState
        data class Error(val message: String) : StoreUiState
    }

    private val _storeState = MutableStateFlow<StoreUiState>(StoreUiState.Idle)
    val storeState: StateFlow<StoreUiState> = _storeState.asStateFlow()

    private val activeGenerationJobs = ConcurrentHashMap<Long, Job>()

    val allChats: StateFlow<List<ChatEntity>> = roleplayRepository.allChats
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val characters: StateFlow<List<CharacterEntity>> = roleplayRepository.allCharacters
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val personas: StateFlow<List<UserPersonaEntity>> = roleplayRepository.allPersonas
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activePersona: StateFlow<UserPersonaEntity?> = roleplayRepository.activePersonaFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val _chatInputTopYPx = MutableStateFlow<Float?>(null)
    val chatInputTopYPx: StateFlow<Float?> = _chatInputTopYPx.asStateFlow()

    fun updateChatInputTopYPx(yPx: Float?) {
        _chatInputTopYPx.value = yPx
    }

    val providerProfiles: StateFlow<List<AiProviderProfileEntity>> = providerRepository.allProfiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val latestRelationshipsByChat: StateFlow<Map<Long, RelationshipTableEntity>> = roleplayRepository.allRelationships
        .map { list ->
            val map = mutableMapOf<Long, RelationshipTableEntity>()
            for (rel in list) {
                if (!map.containsKey(rel.chatId)) {
                    map[rel.chatId] = rel
                }
            }
            map
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // In-memory preloaded message cache per chatId for 0ms instant chat entry
    private val recentMessagesByChat = ConcurrentHashMap<Long, List<MessageEntity>>()

    private val draftPrefs = application.getSharedPreferences("chat_drafts_prefs", android.content.Context.MODE_PRIVATE)

    fun saveDraftSync(chatId: Long, draft: String) {
        if (chatId <= 0L) return
        draftPrefs.edit().putString("draft_$chatId", draft).commit()
    }

    fun getDraftSync(chatId: Long, fallback: String = ""): String {
        if (chatId <= 0L) return fallback
        val saved = draftPrefs.getString("draft_$chatId", null)
        return when {
            !saved.isNullOrEmpty() -> saved
            saved == "" && fallback.isEmpty() -> ""
            fallback.isNotEmpty() -> fallback
            else -> ""
        }
    }

    private val _connectivityWarning = MutableStateFlow<String?>(null)
    val connectivityWarning: StateFlow<String?> = _connectivityWarning.asStateFlow()

    fun dismissConnectivityWarning() {
        _connectivityWarning.value = null
    }

    fun checkInternetAndRepoConnection() {
        viewModelScope.launch {
            if (!storeRepository.isOnline()) {
                _connectivityWarning.value = "Нет подключения к интернету. Некоторые функции и репозиторий недоступны."
                return@launch
            }
            val repoReachable = storeRepository.checkRepositoryConnection()
            if (!repoReachable) {
                if (!storeRepository.isOnline()) {
                    _connectivityWarning.value = "Нет подключения к интернету. Некоторые функции и репозиторий недоступны."
                } else {
                    _connectivityWarning.value = "Не удалось подключиться к репозиторию (github.com/bad-kn1fe-cmd/box-of-untitledAI)."
                }
            } else {
                _connectivityWarning.value = null
            }
        }
    }

    private val _returnedFailedMessageText = MutableStateFlow<Pair<Long, String>?>(null)
    val returnedFailedMessageText: StateFlow<Pair<Long, String>?> = _returnedFailedMessageText.asStateFlow()

    fun clearReturnedFailedMessageText() {
        _returnedFailedMessageText.value = null
    }

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _startupProgress = MutableStateFlow(0.05f)
    val startupProgress: StateFlow<Float> = _startupProgress.asStateFlow()

    private val _startupStatusText = MutableStateFlow("Инициализация ядра Untitled...")
    val startupStatusText: StateFlow<String> = _startupStatusText.asStateFlow()

    private val _warmedTabCount = MutableStateFlow(1)
    val warmedTabCount: StateFlow<Int> = _warmedTabCount.asStateFlow()

    private val _preloadChatShells = MutableStateFlow(false)
    val preloadChatShells: StateFlow<Boolean> = _preloadChatShells.asStateFlow()

    // App remote update state (parsed from info.json)
    private val _appUpdateState = MutableStateFlow<com.example.util.AppUpdateManifest?>(null)
    val appUpdateState: StateFlow<com.example.util.AppUpdateManifest?> = _appUpdateState.asStateFlow()

    fun dismissAppUpdateDialog() {
        _appUpdateState.value = null
    }

    // Remote seasonal event state (from info.json)
    private val _remoteEventInfo = MutableStateFlow<com.example.util.RemoteEventInfo?>(null)
    val remoteEventInfo: StateFlow<com.example.util.RemoteEventInfo?> = _remoteEventInfo.asStateFlow()
    val isHalloweenEventActive: StateFlow<Boolean> = _remoteEventInfo.map { it?.isHalloween == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun checkRemoteEvent(manual: Boolean = false, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = com.example.util.RemoteEventManager.checkEvent()
            if (result.isSuccess) {
                val info = result.getOrNull()
                _remoteEventInfo.value = info
                if (info != null && info.isHalloween) {
                    onResult?.invoke(true, "🎃 Активно событие: Halloween!")
                } else {
                    if (manual) {
                        onResult?.invoke(false, "События в info.json не обнаружено (${info?.eventName ?: "нет"}).")
                    }
                }
            } else {
                if (manual) {
                    onResult?.invoke(false, "Не удалось проверить событие: ${result.exceptionOrNull()?.localizedMessage ?: "Сервер недоступен"}")
                }
            }
        }
    }

    /**
     * Unlocks the special Halloween frame, adds it to the user's frame repository,
     * and automatically equips it for the user.
     */
    fun unlockHalloweenFrame(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val halloweenUri = com.example.util.HalloweenFrameHelper.ensureHalloweenFrame(context)
            val settings = preferencesRepository.userSettingsFlow.first()
            val frames = com.example.ui.components.UserFrameStorageHelper.parseFramesJson(settings.userFramesJson, context).toMutableList()
            if (frames.none { it.uri == halloweenUri || it.isHalloween }) {
                frames.add(
                    com.example.ui.components.UserFrameItem(
                        id = "halloween_frame",
                        name = "🎃 Halloween",
                        uri = halloweenUri,
                        isHalloween = true
                    )
                )
                preferencesRepository.updateUserFramesJson(com.example.ui.components.UserFrameStorageHelper.framesToJson(frames))
            }
            onSuccess?.invoke()
        }
    }

    fun selectUserFrame(frameUri: String) {
        viewModelScope.launch {
            preferencesRepository.updateUserActiveFrame(frameUri)
        }
    }

    fun saveUserFramesJson(json: String) {
        viewModelScope.launch {
            preferencesRepository.updateUserFramesJson(json)
        }
    }

    fun checkForAppUpdates(manual: Boolean = false, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = com.example.util.RemoteUpdateChecker.checkForUpdates()
            if (result.isSuccess) {
                val manifest = result.getOrNull()
                if (manifest != null && manifest.hasUpdate) {
                    _appUpdateState.value = manifest
                    onResult?.invoke(true, "Доступна новая версия: ${manifest.latestVersion}!")
                } else {
                    if (manual) {
                        onResult?.invoke(false, "У вас установлена актуальная версия UntitledAI.")
                    }
                }
            } else {
                if (manual) {
                    onResult?.invoke(false, "Не удалось проверить обновления: ${result.exceptionOrNull()?.localizedMessage ?: "Сервер недоступен"}")
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            try {
                // Stage 1: Database & Settings initialization
                _startupStatusText.value = "Загрузка базы данных, персонажей и настроек..."
                _startupProgress.value = 0.14f
                val startTime = System.currentTimeMillis()
                while (characters.value.isEmpty() && System.currentTimeMillis() - startTime < 900) {
                    delay(45)
                }

                // Stage 2: Preload avatars into RAM & cache recent chat histories
                _startupStatusText.value = "Декодирование аватаров и кэширование диалогов в ОЗУ..."
                _startupProgress.value = 0.30f
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val appContext = getApplication<Application>()
                        characters.value.forEach { ch ->
                            com.example.ui.components.prewarmAvatarInMemory(appContext, ch.avatarUri, 160)
                            com.example.ui.components.prewarmAvatarInMemory(appContext, ch.avatarUri, 320)
                            if (ch.avatarFrameUri.isNotBlank()) {
                                com.example.ui.components.prewarmAvatarInMemory(appContext, ch.avatarFrameUri, 160)
                            }
                        }
                        personas.value.forEach { p ->
                            com.example.ui.components.prewarmAvatarInMemory(appContext, p.avatarUri, 160)
                        }
                        allChats.value.take(15).forEach { chat ->
                            val msgs = roleplayRepository.getMessagesForChat(chat.id).first()
                            recentMessagesByChat[chat.id] = msgs
                        }
                    } catch (_: Exception) {}
                }

                // Stage 3: Pre-connect to GitHub Repository, sync system prompt, events, and Store catalog
                _startupStatusText.value = "Подключение к репозиторию GitHub и синхронизация..."
                _startupProgress.value = 0.48f
                kotlinx.coroutines.withTimeoutOrNull(1800L) {
                    val repoJob = async { checkInternetAndRepoConnection() }
                    val promptJob = async { syncSystemPromptOnLaunch() }
                    val eventJob = async { checkRemoteEvent(manual = false) }
                    val updateJob = async { checkForAppUpdates(manual = false) }
                    val storeJob = async {
                        try {
                            if (_storeState.value is StoreUiState.Idle && storeRepository.isOnline()) {
                                val res = storeRepository.fetchCatalog()
                                if (res.isSuccess) {
                                    _storeState.value = StoreUiState.Success(res.getOrThrow())
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    repoJob.await()
                    promptJob.await()
                    eventJob.await()
                    updateJob.await()
                    storeJob.await()
                }

                // Stage 4: Pre-compose Tab 0 (Characters) & Tab 1 (Chats)
                _startupStatusText.value = "Предзагрузка экранов: Персонажи и Диалоги..."
                _startupProgress.value = 0.66f
                _warmedTabCount.value = 2
                delay(75)

                // Stage 5: Pre-compose Tab 2 (Personas) & Tab 3 (AI Providers)
                _startupStatusText.value = "Предзагрузка экранов: Персоны и AI-Провайдеры..."
                _startupProgress.value = 0.80f
                _warmedTabCount.value = 4
                delay(75)

                // Stage 6: Pre-compose Tab 4 (Settings), Chat shells, and GPU transition layers
                _startupStatusText.value = "Предзагрузка экрана Настроек, чата и анимаций..."
                _startupProgress.value = 0.94f
                _warmedTabCount.value = 5
                _preloadChatShells.value = true
                delay(110)

                // Stage 7: Complete
                _startupStatusText.value = "Готово!"
                _startupProgress.value = 1.0f
                delay(60)
            } catch (_: Exception) {
                _warmedTabCount.value = 5
                _preloadChatShells.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun syncSystemPromptOnLaunch() {
        try {
            val currentSettings = preferencesRepository.userSettingsFlow.first()
            // NEVER overwrite if prompt was modified by the user!
            if (currentSettings.isSystemPromptUserModified) return
            if (currentSettings.customSystemPrompt.isNotBlank() &&
                currentSettings.downloadedSystemPrompt.isNotBlank() &&
                currentSettings.customSystemPrompt.trim() != currentSettings.downloadedSystemPrompt.trim()
            ) {
                return
            }

            // Download on first launch, or if prompt is unmodified and a newer version exists
            val shouldFetch = !currentSettings.isSystemPromptFirstLaunchDone ||
                    currentSettings.customSystemPrompt.isBlank() ||
                    (!currentSettings.isSystemPromptUserModified && currentSettings.isSystemPromptUnmodified)

            if (shouldFetch) {
                val result = com.example.util.SystemPromptManager.fetchRemoteSystemPrompt()
                if (result.isSuccess) {
                    val remote = result.getOrNull()
                    if (remote != null && remote.prompt.isNotBlank()) {
                        val isFirstLaunch = !currentSettings.isSystemPromptFirstLaunchDone || currentSettings.customSystemPrompt.isBlank()
                        val isNewerVersion = remote.version.isNotBlank() && remote.version != currentSettings.systemPromptVersion

                        if (isFirstLaunch || isNewerVersion) {
                            preferencesRepository.updateSettings(
                                currentSettings.copy(
                                    customSystemPrompt = remote.prompt,
                                    systemPromptVersion = remote.version,
                                    downloadedSystemPrompt = remote.prompt,
                                    isSystemPromptUserModified = false,
                                    isSystemPromptFirstLaunchDone = true
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun downloadAndApplyRemoteSystemPrompt(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = com.example.util.SystemPromptManager.fetchRemoteSystemPrompt()
            if (result.isSuccess) {
                val remote = result.getOrNull()
                if (remote != null && remote.prompt.isNotBlank()) {
                    val currentSettings = preferencesRepository.userSettingsFlow.first()
                    preferencesRepository.updateSettings(
                        currentSettings.copy(
                            customSystemPrompt = remote.prompt,
                            systemPromptVersion = remote.version,
                            downloadedSystemPrompt = remote.prompt,
                            isSystemPromptUserModified = false,
                            isSystemPromptFirstLaunchDone = true
                        )
                    )
                    onResult(true, "Системный промпт v${remote.version} успешно установлен!")
                } else {
                    onResult(false, "Не удалось разобрать системный промпт")
                }
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Ошибка сети при загрузке")
            }
        }
    }

    // Tracks chats where AI is actively generating/thinking a response across navigation/tabs
    private val _generatingChatIds = MutableStateFlow<Set<Long>>(emptySet())
    val generatingChatIds: StateFlow<Set<Long>> = _generatingChatIds.asStateFlow()

    // Tracks specific message IDs currently being regenerated so they stay hidden across navigation/tabs
    private val _regeneratingMessageIds = MutableStateFlow<Set<Long>>(emptySet())
    val regeneratingMessageIds: StateFlow<Set<Long>> = _regeneratingMessageIds.asStateFlow()

    // Real-time token streaming state for chats (chatId -> accumulated streamed text)
    private val _streamingMessageByChat = MutableStateFlow<Map<Long, String>>(emptyMap())
    val streamingMessageByChat: StateFlow<Map<Long, String>> = _streamingMessageByChat.asStateFlow()

    val allStories: StateFlow<List<StoryEntity>> = roleplayRepository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allComics: StateFlow<List<ComicEntity>> = roleplayRepository.allComics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chat State — resolved synchronously from in-memory StateFlows for 0ms chat entry!
    private val _activeChatId = MutableStateFlow<Long?>(null)
    val activeChatId: StateFlow<Long?> = _activeChatId.asStateFlow()

    val activeChat: StateFlow<ChatEntity?> = combine(_activeChatId, allChats) { id, chats ->
        if (id == null) null else chats.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeCharacter: StateFlow<CharacterEntity?> = combine(activeChat, characters) { chat, chars ->
        if (chat == null) null else chars.find { it.id == chat.characterId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeMessages: StateFlow<List<MessageEntity>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) {
                combine(
                    roleplayRepository.getMessagesForChat(id),
                    _regeneratingMessageIds
                ) { messages, regeneratingIds ->
                    recentMessagesByChat[id] = messages
                    if (regeneratingIds.isEmpty()) messages
                    else messages.filter { !regeneratingIds.contains(it.id) }
                }.onStart {
                    emit(recentMessagesByChat[id] ?: emptyList())
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeSummaries: StateFlow<List<SummaryEntity>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) roleplayRepository.getSummariesForChat(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGeneratedImages: StateFlow<List<MessageEntity>> = roleplayRepository.getAllGeneratedImages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRelationships: StateFlow<List<RelationshipTableEntity>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) roleplayRepository.getRelationshipTablesForChat(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestRelationship: StateFlow<RelationshipTableEntity?> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) roleplayRepository.getLatestRelationshipFlow(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Per-chat draft texts preserved across tabs, back navigation, and app lifecycle
    private val _chatDrafts = MutableStateFlow<Map<Long, String>>(emptyMap())
    val chatDrafts: StateFlow<Map<Long, String>> = _chatDrafts.asStateFlow()
    private val draftRoomSyncJobs = java.util.concurrent.ConcurrentHashMap<Long, Job>()

    fun setDraft(chatId: Long, draft: String) {
        if (_chatDrafts.value[chatId] == draft) return
        _chatDrafts.update { it + (chatId to draft) }
        saveDraftSync(chatId, draft)
        draftRoomSyncJobs[chatId]?.cancel()
        draftRoomSyncJobs[chatId] = viewModelScope.launch {
            delay(1500L)
            roleplayRepository.updateChatDraft(chatId, draft)
        }
    }

    fun clearDraft(chatId: Long) {
        draftRoomSyncJobs[chatId]?.cancel()
        _chatDrafts.update { it - chatId }
        saveDraftSync(chatId, "")
        viewModelScope.launch {
            roleplayRepository.updateChatDraft(chatId, "")
        }
    }

    fun setChatPersona(chatId: Long, personaId: Long?) {
        viewModelScope.launch {
            roleplayRepository.setChatPersona(chatId, personaId)
        }
    }

    // Character CRUD & Management
    fun saveCharacter(character: CharacterEntity, onComplete: (Long) -> Unit) {
        viewModelScope.launch {
            val id = roleplayRepository.saveCharacter(character)
            onComplete(id)
        }
    }

    fun deleteCharacter(character: CharacterEntity, onComplete: () -> Unit) {
        viewModelScope.launch {
            roleplayRepository.deleteCharacter(character)
            onComplete()
        }
    }

    fun togglePinCharacter(character: CharacterEntity) {
        viewModelScope.launch {
            roleplayRepository.setCharacterPinned(character.id, !character.isPinned)
        }
    }

    fun setCharacterPinned(characterId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            roleplayRepository.setCharacterPinned(characterId, isPinned)
        }
    }

    fun toggleArchiveCharacter(character: CharacterEntity) {
        viewModelScope.launch {
            roleplayRepository.setCharacterArchived(character.id, !character.isArchived)
        }
    }

    fun setCharacterArchived(characterId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            roleplayRepository.setCharacterArchived(characterId, isArchived)
        }
    }

    fun toggleHideCharacter(character: CharacterEntity) {
        viewModelScope.launch {
            roleplayRepository.setCharacterHidden(character.id, !character.isHidden)
        }
    }

    fun setCharacterHidden(characterId: Long, isHidden: Boolean) {
        viewModelScope.launch {
            roleplayRepository.setCharacterHidden(characterId, isHidden)
        }
    }

    // Story (Character Groups / Lore Worlds)
    fun saveStory(story: StoryEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = roleplayRepository.saveStory(story)
            onComplete(id)
        }
    }

    fun deleteStory(story: StoryEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            roleplayRepository.deleteStory(story)
            onComplete()
        }
    }

    fun togglePinStory(story: StoryEntity) {
        viewModelScope.launch {
            roleplayRepository.setStoryPinned(story.id, !story.isPinned)
        }
    }

    fun setStoryPinned(storyId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            roleplayRepository.setStoryPinned(storyId, isPinned)
        }
    }

    fun toggleArchiveStory(story: StoryEntity) {
        viewModelScope.launch {
            roleplayRepository.setStoryArchived(story.id, !story.isArchived)
        }
    }

    fun setStoryArchived(storyId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            roleplayRepository.setStoryArchived(storyId, isArchived)
        }
    }

    // Comics
    fun generateComic(
        chatId: Long,
        characterId: Long,
        chapterTitle: String,
        style: String,
        chapterSummaryText: String? = null,
        panelCount: Int = 4,
        selectedImageProfileId: Long? = null,
        civitaiModelAir: String? = null,
        customPrompt: String = "",
        negativePrompt: String = "",
        onProgress: (String) -> Unit,
        onComplete: (ComicEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val character = roleplayRepository.getCharacterById(characterId)
                    ?: return@launch onError("Персонаж не найден")
                val textProfile = providerProfiles.value.find { it.isDefaultText }
                    ?: providerProfiles.value.firstOrNull()
                    ?: return@launch onError("Не настроен текстовый AI-провайдер")

                val imageProfile = if (selectedImageProfileId != null) {
                    providerProfiles.value.find { it.id == selectedImageProfileId }
                } else {
                    providerProfiles.value.find { it.isDefaultImage }
                        ?: providerProfiles.value.find { it.isImageProvider }
                }

                val messages = roleplayRepository.getMessagesForChat(chatId).first()
                if (messages.isEmpty() && chapterSummaryText.isNullOrBlank()) {
                    return@launch onError("В этой главе еще нет сообщений для комикса")
                }

                onProgress("Генерация сценария и раскадровки...")

                val result = AiProviderEngine.generateComicPanels(
                    textProfile = textProfile,
                    imageProfile = imageProfile,
                    characterName = character.name,
                    characterAppearance = character.appearance,
                    messages = messages,
                    style = style,
                    chapterTitle = chapterTitle,
                    civitaiModelAir = civitaiModelAir,
                    customPromptAddition = customPrompt,
                    customNegativePrompt = negativePrompt,
                    chapterSummaryText = chapterSummaryText,
                    panelCount = panelCount
                ) { current, total, statusText ->
                    onProgress(statusText)
                }

                if (result.isSuccess) {
                    val panels = result.getOrThrow()
                    val panelsJsonArray = org.json.JSONArray()
                    var firstImg = ""
                    for (p in panels) {
                        if (firstImg.isBlank() && p.imageUri.isNotBlank()) {
                            firstImg = p.imageUri
                        }
                        val obj = org.json.JSONObject()
                        obj.put("panelIndex", p.panelIndex)
                        obj.put("speakerName", p.speakerName)
                        obj.put("dialogueText", p.dialogueText)
                        obj.put("soundEffect", p.soundEffect)
                        obj.put("sceneDescription", p.sceneDescription)
                        obj.put("imagePrompt", p.imagePrompt)
                        obj.put("imageUri", p.imageUri)
                        panelsJsonArray.put(obj)
                    }

                    val comicEntity = ComicEntity(
                        title = "Комикс: ${character.name} — $chapterTitle",
                        chatId = chatId,
                        characterId = characterId,
                        chapterName = chapterTitle,
                        style = style,
                        panelsJson = panelsJsonArray.toString(),
                        coverUri = firstImg.ifBlank { character.avatarUri },
                        createdAt = System.currentTimeMillis()
                    )
                    val comicId = roleplayRepository.saveComic(comicEntity)
                    onComplete(comicEntity.copy(id = comicId))
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Не удалось сгенерировать комикс")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка генерации комикса")
            }
        }
    }

    fun deleteComic(comic: ComicEntity) {
        viewModelScope.launch {
            roleplayRepository.deleteComic(comic)
        }
    }

    // Settings
    fun updateUserSettings(settings: UserSettings) {
        viewModelScope.launch {
            preferencesRepository.updateSettings(settings)
        }
    }

    fun resetSystemPrompt() {
        viewModelScope.launch {
            val current = userSettings.value
            preferencesRepository.updateSettings(current.copy(customSystemPrompt = ""))
        }
    }

    // Chat Operations — fast-path uses in-memory allChats for 0ms instant navigation
    fun openChatForCharacter(characterId: Long, onChatReady: (Long) -> Unit) {
        val cached = allChats.value.firstOrNull { it.characterId == characterId }
        if (cached != null) {
            _activeChatId.value = cached.id
            onChatReady(cached.id)
            return
        }
        viewModelScope.launch {
            val existingChats = roleplayRepository.getChatsForCharacter(characterId).first()
            val chatId = if (existingChats.isNotEmpty()) {
                existingChats.first().id
            } else {
                val character = characters.value.find { it.id == characterId }
                    ?: roleplayRepository.getCharacterById(characterId)
                val newChat = ChatEntity(
                    characterId = characterId,
                    title = "Диалог 1",
                    isTextingMode = false,
                    relationshipType = character?.startingRelationship ?: "strangers",
                    customRelationship = character?.customRelationship ?: ""
                )
                roleplayRepository.createChat(newChat)
            }
            _activeChatId.value = chatId
            onChatReady(chatId)
        }
    }

    fun createAdditionalChatForCharacter(characterId: Long, title: String, onReady: (Long) -> Unit) {
        viewModelScope.launch {
            val count = roleplayRepository.getChatsForCharacter(characterId).first().size
            val defaultTitle = if (title.isNotBlank()) title else "Диалог ${count + 1}"
            val character = roleplayRepository.getCharacterById(characterId)
            val newChat = ChatEntity(
                characterId = characterId,
                title = defaultTitle,
                isTextingMode = false,
                relationshipType = character?.startingRelationship ?: "strangers",
                customRelationship = character?.customRelationship ?: ""
            )
            val newId = roleplayRepository.createChat(newChat)
            _activeChatId.value = newId
            onReady(newId)
        }
    }

    fun getChatsForCharacter(characterId: Long): Flow<List<ChatEntity>> {
        return roleplayRepository.getChatsForCharacter(characterId)
    }

    fun setActiveChat(chatId: Long) {
        _activeChatId.value = chatId
    }

    fun clearActiveChat() {
        _activeChatId.value = null
    }

    /**
     * Sends a message with instant UI feedback:
     * 1. User message is inserted and shown immediately (optionally with user image attachment).
     *    If text is blank and no image, treats as an invisible continuation command to continue the AI's story/dialogue.
     * 2. Network request is launched IMMEDIATELY in parallel with the artificial delay timer (if texting mode delay is active).
     * 3. Response is rendered seamlessly without double-waiting.
     */
    fun sendMessageOptimized(
        chatId: Long,
        text: String,
        isTextingMode: Boolean,
        imageUrl: String? = null,
        audioFilePath: String? = null,
        audioDurationSec: Int = 0,
        onAiStateChange: (Boolean) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (!storeRepository.isOnline()) {
            _connectivityWarning.value = "Нет подключения к интернету. Сообщение возвращено в поле ввода."
            if (text.isNotBlank()) {
                _returnedFailedMessageText.value = chatId to text
                setDraft(chatId, text)
            }
            onError("Нет подключения к интернету")
            return
        }

        val isContinuation = text.isBlank() && imageUrl == null && audioFilePath == null
        val job = viewModelScope.launch {
            _generatingChatIds.update { it + chatId }
            _streamingMessageByChat.update { it - chatId }
            onAiStateChange(true)
            val charName = activeCharacter.value?.name ?: "Собеседник"
            val settings = userSettings.value
            BackgroundGenerationManager.startGeneration(chatId, charName)
            var generatedText: String? = null
            var hasError = false
            var insertedUserMsg: MessageEntity? = null

            try {
                var effectiveText = text
                if (audioFilePath != null && effectiveText.isBlank()) {
                    val audioFile = java.io.File(audioFilePath)
                    if (audioFile.exists() && audioFile.length() > 0L) {
                        val activeChat = allChats.value.firstOrNull { it.id == chatId }
                        val transcript = com.example.ai.engine.AudioTranscriptionEngine.transcribeAudioFile(
                            context = getApplication(),
                            audioFile = audioFile,
                            preferredProfileId = activeChat?.providerProfileId
                        )
                        if (!transcript.isNullOrBlank()) {
                            effectiveText = transcript
                        }
                    }
                }

                // 1. Insert user message into DB if not an invisible continuation command
                if (!isContinuation) {
                    insertedUserMsg = roleplayRepository.insertUserMessage(chatId, effectiveText, imageUrl, audioFilePath, audioDurationSec)
                }
                clearDraft(chatId)

                val delayDurationMs = if (!isContinuation && isTextingMode && !settings.streamingEnabled && settings.textingModeDelayPreset != "instant") {
                    when (settings.textingModeDelayPreset) {
                        "fast" -> 1800L
                        "normal" -> 3500L
                        "slow" -> 6000L
                        "custom" -> settings.textingModeCustomSeconds * 1000L
                        else -> 3000L
                    }
                } else 0L

                // 2. Launch network request immediately in parallel
                val isAssistant = activeCharacter.value?.name.equals("Untitled Ассистент", ignoreCase = true)
                val settingsSummary = if (isAssistant) buildSystemConfigSummary() else ""
                val streamBuffer = StringBuilder()
                var lastStreamEmitMs = 0L
                val streamCallback: ((String) -> Unit)? = if (settings.streamingEnabled) {
                    { token ->
                        synchronized(streamBuffer) {
                            streamBuffer.append(token)
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastStreamEmitMs >= 38L) {
                                lastStreamEmitMs = now
                                val snapshot = streamBuffer.toString()
                                _streamingMessageByChat.update { map ->
                                    map + (chatId to snapshot)
                                }
                            }
                        }
                    }
                } else null

                val aiDeferred = async {
                    roleplayRepository.generateAiResponse(
                        chatId = chatId,
                        contextWindowSize = settings.contextWindowSize,
                        settingsSummary = settingsSummary,
                        isContinuation = isContinuation,
                        openRouterFallbackModels = settings.openRouterFallbackModels,
                        customSystemPrompt = settings.customSystemPrompt,
                        userSettings = settings,
                        onTokenStream = streamCallback
                    )
                }

                // 3. Wait for the artificial typing delay if needed
                if (delayDurationMs > 0L) {
                    delay(delayDurationMs)
                }

                // 4. Await network result
                val result = aiDeferred.await()

                if (result.isFailure) {
                    hasError = true
                    val err = result.exceptionOrNull()
                    val errMsg = err?.message ?: "Ошибка получения ответа"
                    val isOfflineError = !storeRepository.isOnline() ||
                            err is java.net.UnknownHostException ||
                            err is java.net.ConnectException ||
                            errMsg.contains("Unable to resolve host", ignoreCase = true) ||
                            errMsg.contains("Failed to connect", ignoreCase = true) ||
                            errMsg.contains("Network is unreachable", ignoreCase = true)
                    if (isOfflineError && insertedUserMsg != null) {
                        roleplayRepository.deleteMessage(insertedUserMsg.id)
                        if (text.isNotBlank()) {
                            _returnedFailedMessageText.value = chatId to text
                            setDraft(chatId, text)
                        }
                        _connectivityWarning.value = "Нет подключения к интернету. Сообщение возвращено в поле ввода."
                    }
                    onError(errMsg)
                } else {
                    generatedText = result.getOrNull()?.lastOrNull()?.content
                }
            } catch (e: CancellationException) {
                // Intentionally stopped by user
                hasError = false
            } catch (e: Exception) {
                hasError = true
                val errMsg = e.message ?: "Не удалось отправить сообщение"
                val isOfflineError = !storeRepository.isOnline() ||
                        e is java.net.UnknownHostException ||
                        e is java.net.ConnectException ||
                        errMsg.contains("Unable to resolve host", ignoreCase = true) ||
                        errMsg.contains("Failed to connect", ignoreCase = true) ||
                        errMsg.contains("Network is unreachable", ignoreCase = true)
                if (isOfflineError && insertedUserMsg != null) {
                    roleplayRepository.deleteMessage(insertedUserMsg.id)
                    if (text.isNotBlank()) {
                        _returnedFailedMessageText.value = chatId to text
                        setDraft(chatId, text)
                    }
                    _connectivityWarning.value = "Нет подключения к интернету. Сообщение возвращено в поле ввода."
                }
                onError(errMsg)
            } finally {
                activeGenerationJobs.remove(chatId)
                _streamingMessageByChat.update { it - chatId }
                BackgroundGenerationManager.endGeneration(
                    chatId = chatId,
                    characterName = charName,
                    responseText = generatedText,
                    isError = hasError,
                    notificationsEnabled = settings.backgroundNotificationsEnabled
                )
                _generatingChatIds.update { it - chatId }
                onAiStateChange(false)
            }
        }
        activeGenerationJobs[chatId] = job
    }

    fun stopGeneration(chatId: Long) {
        val job = activeGenerationJobs.remove(chatId)
        job?.cancel()
        _generatingChatIds.update { it - chatId }
        _regeneratingMessageIds.update { emptySet() }
        _streamingMessageByChat.update { it - chatId }
        val charName = activeCharacter.value?.name ?: "Собеседник"
        BackgroundGenerationManager.endGeneration(
            chatId = chatId,
            characterName = charName,
            responseText = null,
            isError = false,
            notificationsEnabled = false
        )
    }

    fun regenerateAiResponseVariant(
        chatId: Long,
        messageId: Long,
        onAiStateChange: (Boolean) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val job = viewModelScope.launch {
            _generatingChatIds.update { it + chatId }
            _regeneratingMessageIds.update { it + messageId }
            _streamingMessageByChat.update { it - chatId }
            onAiStateChange(true)
            val charName = activeCharacter.value?.name ?: "Собеседник"
            val settings = userSettings.value
            BackgroundGenerationManager.startGeneration(chatId, charName)
            var generatedText: String? = null
            var hasError = false

            try {
                val isAssistant = activeCharacter.value?.name.equals("Untitled Ассистент", ignoreCase = true)
                val settingsSummary = if (isAssistant) buildSystemConfigSummary() else ""
                val streamBuffer = StringBuilder()
                var lastStreamEmitMs = 0L
                val streamCallback: ((String) -> Unit)? = if (settings.streamingEnabled) {
                    { token ->
                        synchronized(streamBuffer) {
                            streamBuffer.append(token)
                            val now = android.os.SystemClock.elapsedRealtime()
                            if (now - lastStreamEmitMs >= 38L) {
                                lastStreamEmitMs = now
                                val snapshot = streamBuffer.toString()
                                _streamingMessageByChat.update { map ->
                                    map + (chatId to snapshot)
                                }
                            }
                        }
                    }
                } else null

                val res = roleplayRepository.regenerateVariant(
                    chatId = chatId,
                    messageId = messageId,
                    contextWindowSize = userSettings.value.contextWindowSize,
                    settingsSummary = settingsSummary,
                    openRouterFallbackModels = userSettings.value.openRouterFallbackModels,
                    customSystemPrompt = userSettings.value.customSystemPrompt,
                    userSettings = userSettings.value,
                    onTokenStream = streamCallback
                )
                if (res.isFailure) {
                    hasError = true
                    onError(res.exceptionOrNull()?.message ?: "Ошибка генерации нового варианта")
                } else {
                    generatedText = res.getOrNull()?.content
                }
            } catch (e: CancellationException) {
                hasError = false
            } catch (e: Exception) {
                hasError = true
                onError(e.message ?: "Ошибка при регенерации ответа")
            } finally {
                activeGenerationJobs.remove(chatId)
                _streamingMessageByChat.update { it - chatId }
                BackgroundGenerationManager.endGeneration(
                    chatId = chatId,
                    characterName = charName,
                    responseText = generatedText,
                    isError = hasError,
                    notificationsEnabled = settings.backgroundNotificationsEnabled
                )
                _generatingChatIds.update { it - chatId }
                _regeneratingMessageIds.update { it - messageId }
                onAiStateChange(false)
            }
        }
        activeGenerationJobs[chatId] = job
    }

    fun switchMessageVariant(messageId: Long, targetIndex: Int) {
        viewModelScope.launch {
            roleplayRepository.switchMessageVariant(messageId, targetIndex)
        }
    }

    fun updateCivitaiBuzz(blue: Int, orange: Int, green: Int) {
        viewModelScope.launch {
            preferencesRepository.updateSettings(
                userSettings.value.copy(
                    civitaiBlueBuzz = blue,
                    civitaiOrangeBuzz = orange,
                    civitaiGreenBuzz = green
                )
            )
        }
    }

    fun editMessage(messageId: Long, newText: String) {
        viewModelScope.launch {
            roleplayRepository.editMessage(messageId, newText)
        }
    }

    fun editUserMessageAndRegenerate(
        chatId: Long,
        messageId: Long,
        newText: String,
        onAiStateChange: (Boolean) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val job = viewModelScope.launch {
            _generatingChatIds.update { it + chatId }
            onAiStateChange(true)
            val charName = activeCharacter.value?.name ?: "Собеседник"
            val settings = userSettings.value
            BackgroundGenerationManager.startGeneration(chatId, charName)
            var generatedText: String? = null
            var hasError = false

            try {
                val isAssistant = activeCharacter.value?.name.equals("Untitled Ассистент", ignoreCase = true)
                val settingsSummary = if (isAssistant) buildSystemConfigSummary() else ""
                val res = roleplayRepository.editUserMessageAndRegenerate(
                    chatId = chatId,
                    messageId = messageId,
                    newText = newText,
                    contextWindowSize = userSettings.value.contextWindowSize,
                    settingsSummary = settingsSummary
                )
                if (res.isFailure) {
                    hasError = true
                    onError(res.exceptionOrNull()?.message ?: "Ошибка генерации")
                } else {
                    generatedText = res.getOrNull()?.lastOrNull()?.content
                }
            } catch (e: CancellationException) {
                hasError = false
            } catch (e: Exception) {
                hasError = true
                onError(e.message ?: "Ошибка при редактировании")
            } finally {
                activeGenerationJobs.remove(chatId)
                BackgroundGenerationManager.endGeneration(
                    chatId = chatId,
                    characterName = charName,
                    responseText = generatedText,
                    isError = hasError,
                    notificationsEnabled = settings.backgroundNotificationsEnabled
                )
                _generatingChatIds.update { it - chatId }
                onAiStateChange(false)
            }
        }
        activeGenerationJobs[chatId] = job
    }

    // Persona CRUD
    fun savePersona(persona: UserPersonaEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = roleplayRepository.savePersona(persona)
            onComplete(id)
        }
    }

    fun selectPersona(personaId: Long) {
        viewModelScope.launch {
            roleplayRepository.selectPersona(personaId)
        }
    }

    fun deletePersona(persona: UserPersonaEntity) {
        viewModelScope.launch {
            roleplayRepository.deletePersona(persona)
        }
    }

    fun buildSystemConfigSummary(): String {
        val s = userSettings.value
        val providers = providerProfiles.value
        val textDefault = providers.find { it.isDefaultText }?.let { "${it.name} (${it.model})" } ?: "Не выбран"
        val imgDefault = providers.find { it.isDefaultImage }?.let { "${it.name} (${it.model})" } ?: "Не выбран"
        val totalChars = characters.value.size
        val totalPersonas = personas.value.size
        return """
            - Тема оформления: ${s.themeMode}
            - Акцентный цвет: 0x${s.accentColorLong.toString(16).uppercase()}
            - Фон чата: ${s.chatWallpaperPreset} (Параллакс: ${s.parallaxEnabled})
            - Размер контекстного окна: ${s.contextWindowSize} сообщений
            - Имитация переписки (задержка): ${s.textingModeDelayPreset}
            - Показывать индикатор набора: ${s.showTypingIndicator}
            - Глобальный NSFW 18+: ${s.globalDefaultNsfw}
            - Всего персонажей в базе: $totalChars
            - Всего персон пользователя: $totalPersonas
            - Настроенные провайдеры (${providers.size}):
              * Основной для текста: $textDefault (Ключ указан: ${providers.find { it.isDefaultText }?.apiKey?.isNotBlank() == true})
              * Основной для изображений: $imgDefault (Ключ указан: ${providers.find { it.isDefaultImage }?.apiKey?.isNotBlank() == true})
        """.trimIndent()
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            roleplayRepository.deleteMessage(messageId)
        }
    }

    fun setMessageReaction(messageId: Long, reaction: String?) {
        viewModelScope.launch {
            roleplayRepository.updateMessageReaction(messageId, reaction)
        }
    }

    fun branchChat(chatId: Long, messageId: Long, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newChatId = roleplayRepository.branchChatAtMessage(chatId, messageId)
            if (newChatId != null) {
                onCreated(newChatId)
            }
        }
    }

    fun clearChat(chatId: Long) {
        viewModelScope.launch {
            roleplayRepository.clearMessagesForChat(chatId)
        }
    }

    fun toggleTextingMode(chatId: Long, enabled: Boolean) {
        viewModelScope.launch {
            roleplayRepository.toggleTextingMode(chatId, enabled)
        }
    }

    fun updateChat(chat: ChatEntity) {
        viewModelScope.launch {
            roleplayRepository.updateChat(chat)
        }
    }

    fun setChatPinned(chatId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            roleplayRepository.setChatPinned(chatId, isPinned)
        }
    }

    fun setChatArchived(chatId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            roleplayRepository.setChatArchived(chatId, isArchived)
        }
    }

    fun updateChatTitle(chatId: Long, title: String) {
        viewModelScope.launch {
            roleplayRepository.updateChatTitle(chatId, title)
        }
    }

    fun updateChatNotes(chatId: Long, notes: String) {
        viewModelScope.launch {
            roleplayRepository.updateChatNotes(chatId, notes)
        }
    }

    fun deleteChat(chatId: Long, onDeleted: () -> Unit) {
        viewModelScope.launch {
            roleplayRepository.deleteChat(chatId)
            _activeChatId.value = null
            onDeleted()
        }
    }

    // Provider CRUD
    fun saveProvider(profile: AiProviderProfileEntity) {
        viewModelScope.launch {
            providerRepository.saveProfile(profile)
        }
    }

    fun deleteProvider(profile: AiProviderProfileEntity) {
        viewModelScope.launch {
            providerRepository.deleteProfile(profile)
        }
    }

    fun setDefaultTextProvider(id: Long) {
        viewModelScope.launch {
            providerRepository.setDefaultText(id)
        }
    }

    fun setDefaultImageProvider(id: Long) {
        viewModelScope.launch {
            providerRepository.setDefaultImage(id)
        }
    }

    suspend fun testProviderConnection(profile: AiProviderProfileEntity): Result<String> {
        return providerRepository.testProfileConnection(profile)
    }

    suspend fun fetchRemoteModels(providerType: String, baseUrl: String = "", apiKey: String = ""): Result<List<String>> {
        return providerRepository.fetchRemoteModels(providerType, baseUrl, apiKey)
    }

    suspend fun fetchOpenRouterModels(apiKey: String): Result<List<String>> {
        return providerRepository.fetchOpenRouterModels(apiKey)
    }

    suspend fun searchCivitaiModels(query: String, apiKey: String = ""): Result<List<com.example.data.model.CivitaiModelItem>> {
        return providerRepository.searchCivitaiModels(query, apiKey)
    }

    fun exportCharacterJson(character: CharacterEntity): String {
        return com.example.data.repository.CharacterSerializer.exportToJson(character)
    }

    fun importCharacterFromJson(
        jsonStr: String,
        onComplete: (CharacterEntity) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val result = com.example.data.repository.CharacterSerializer.importFromJson(jsonStr, getApplication())
        if (result.isSuccess) {
            val char = result.getOrThrow()
            viewModelScope.launch {
                val id = roleplayRepository.saveCharacter(char)
                onComplete(char.copy(id = id))
            }
        } else {
            onError(result.exceptionOrNull()?.message ?: "Ошибка парсинга JSON")
        }
    }

    fun loadStoreCatalog(force: Boolean = false) {
        if (!force && _storeState.value is StoreUiState.Success) return

        _storeState.value = StoreUiState.Loading
        viewModelScope.launch {
            val result = storeRepository.fetchCatalog()
            if (result.isSuccess) {
                _storeState.value = StoreUiState.Success(result.getOrThrow())
            } else {
                val err = result.exceptionOrNull()
                val isConnError = err is java.net.UnknownHostException || 
                                  err is java.net.SocketTimeoutException || 
                                  err is java.net.ConnectException ||
                                  err?.message?.contains("OFFLINE", ignoreCase = true) == true
                if (isConnError) {
                    _storeState.value = StoreUiState.Offline("Нет подключения к интернету. Проверьте соединение и попробуйте снова.")
                } else {
                    _storeState.value = StoreUiState.Error(err?.localizedMessage ?: "Не удалось загрузить данные Хранилища")
                }
            }
        }
    }

    fun downloadStoreBot(
        bot: com.example.data.repository.StoreBotItem,
        onSuccess: (CharacterEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val entityToSave = bot.toCharacterEntity(getApplication())
                val existing = characters.value.find { it.name.trim().equals(entityToSave.name.trim(), ignoreCase = true) }
                val finalEntity = if (existing != null) {
                    val finalAvatar = if (entityToSave.avatarUri.isNotBlank() && entityToSave.avatarUri != "drawable://app_logo_black") {
                        entityToSave.avatarUri
                    } else {
                        existing.avatarUri
                    }
                    val updated = entityToSave.copy(
                        id = existing.id,
                        avatarUri = finalAvatar,
                        isPinned = existing.isPinned,
                        isArchived = existing.isArchived,
                        isHidden = existing.isHidden,
                        lastInteractedAt = existing.lastInteractedAt,
                        createdAt = existing.createdAt
                    )
                    roleplayRepository.saveCharacter(updated)
                    updated
                } else {
                    val id = roleplayRepository.saveCharacter(entityToSave)
                    entityToSave.copy(id = id)
                }
                onSuccess(finalEntity)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Ошибка сохранения бота")
            }
        }
    }

    fun markGeminiTutorialShown() {
        viewModelScope.launch {
            preferencesRepository.markGeminiTutorialShown()
        }
    }

    fun quickSaveGeminiKey(
        key: String,
        model: String = "gemini-3.5-flash-lite",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleanKey = key.trim()
            val cleanModel = model.trim().ifBlank { "gemini-3.5-flash-lite" }
            val profiles = providerProfiles.value.ifEmpty {
                db.aiProviderDao().getAllProfilesList()
            }
            val existingGemini = profiles.find { it.providerType.equals("GEMINI", ignoreCase = true) }
            val savedId = if (existingGemini != null) {
                val updated = existingGemini.copy(
                    apiKey = cleanKey,
                    model = cleanModel,
                    visionModel = cleanModel,
                    isDefaultText = true,
                    isImageProvider = false
                )
                providerRepository.saveProfile(updated)
                existingGemini.id
            } else {
                val newProfile = AiProviderProfileEntity(
                    name = "Google Gemini (Text & Vision)",
                    providerType = "GEMINI",
                    baseUrl = "https://generativelanguage.googleapis.com",
                    apiKey = cleanKey,
                    model = cleanModel,
                    visionModel = cleanModel,
                    temperature = 0.85f,
                    topP = 0.95f,
                    maxTokens = 2048,
                    isDefaultText = true,
                    isDefaultImage = false,
                    isImageProvider = false
                )
                providerRepository.saveProfile(newProfile)
            }
            if (savedId > 0L) {
                providerRepository.setDefaultText(savedId)
            }
            preferencesRepository.markGeminiTutorialShown()
            onComplete()
        }
    }

    fun quickSaveOpenRouterKey(key: String) {
        viewModelScope.launch {
            val profiles = providerProfiles.value
            val openRouter = profiles.find { it.providerType == "OPENROUTER" }
            if (openRouter != null) {
                providerRepository.saveProfile(openRouter.copy(apiKey = key.trim()))
            } else {
                val newProfile = AiProviderProfileEntity(
                    name = "OpenRouter",
                    providerType = "OPENROUTER",
                    baseUrl = "https://openrouter.ai/api/v1",
                    apiKey = key.trim(),
                    model = "meta-llama/llama-3.3-70b-instruct:free",
                    isDefaultText = true
                )
                providerRepository.saveProfile(newProfile)
            }
        }
    }

    fun generateImageForChat(
        chatId: Long,
        prompt: String,
        includeAppearance: Boolean = true,
        style: String = "anime",
        resolution: String = "832x1216",
        providerProfileId: Long? = null,
        engineMode: String = "auto",
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val chat = db.chatDao().getChatById(chatId)
                val character = if (chat != null) db.characterDao().getCharacterById(chat.characterId) else null

                val finalPrompt = if (character != null) {
                    com.example.ai.engine.RoleplayPromptBuilder.buildCharacterImagePrompt(
                        character = character,
                        scenePrompt = prompt,
                        includeAppearance = includeAppearance,
                        style = style,
                        isNsfw = chat?.isNsfwEnabled ?: false
                    )
                } else {
                    prompt
                }

                val selectedProfile = if (providerProfileId != null) {
                    providerProfiles.value.find { it.id == providerProfileId }
                } else {
                    providerProfiles.value.find { it.isDefaultImage }
                        ?: providerProfiles.value.find { it.providerType == "CIVITAI" && it.apiKey.isNotBlank() }
                        ?: providerProfiles.value.firstOrNull { it.apiKey.isNotBlank() }
                }

                val res = providerRepository.generateImage(
                    profile = selectedProfile,
                    prompt = finalPrompt,
                    negativePrompt = selectedProfile?.negativePrompt ?: "",
                    resolution = resolution,
                    engineMode = engineMode
                )

                if (res.isSuccess) {
                    val imgUrl = res.getOrThrow()
                    val displayContent = if (prompt.isNotBlank()) "🎨 *[Фото по запросу: \"$prompt\"]*" else "🎨 *[Фото персонажа]*"
                    val msg = MessageEntity(
                        chatId = chatId,
                        sender = "character",
                        content = displayContent,
                        imageUrl = imgUrl,
                        imagePrompt = finalPrompt,
                        timestamp = System.currentTimeMillis()
                    )
                    db.messageDao().insertMessage(msg)
                    if (chat != null) {
                        db.chatDao().updateChat(chat.copy(updatedAt = System.currentTimeMillis()))
                    }
                    onComplete()
                } else {
                    onError(res.exceptionOrNull()?.message ?: "Не удалось сгенерировать изображение")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Ошибка при создании изображения")
            }
        }
    }

    // AI Bot & Persona Creator Assistant
    private fun resolveActiveTextProfile(): AiProviderProfileEntity? {
        return providerProfiles.value.find { it.isDefaultText }
            ?: providerProfiles.value.firstOrNull { !it.isImageProvider && it.apiKey.isNotBlank() }
            ?: providerProfiles.value.firstOrNull { !it.isImageProvider }
            ?: providerProfiles.value.firstOrNull()
    }

    fun generateBotFromPrompt(
        userWishes: String,
        chatIdForLog: Long? = null,
        saveToDatabase: Boolean = false,
        onSuccess: (com.example.ai.engine.GeneratedBotDraft, CharacterEntity?) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val profile = resolveActiveTextProfile()
                    ?: return@launch onError("Не настроен текстовый ИИ-провайдер. Добавьте ключ в разделе Провайдеры.")

                if (chatIdForLog != null && chatIdForLog > 0L) {
                    roleplayRepository.insertUserMessage(
                        chatIdForLog,
                        "🤖 Помоги создать бота: $userWishes"
                    )
                }

                val settings = userSettings.value
                val systemPrompt = com.example.ai.engine.AiCreatorAssistantHelper.buildCreateBotSystemPrompt()
                val reqMsg = MessageEntity(
                    chatId = chatIdForLog ?: 0L,
                    sender = "user",
                    content = "Пожелания к боту:\n$userWishes"
                )

                val aiResult = AiProviderEngine.generateChatCompletion(
                    profile = profile,
                    systemPrompt = systemPrompt,
                    recentMessages = listOf(reqMsg),
                    openRouterFallbackModels = settings.openRouterFallbackModels,
                    geminiSafetyThreshold = settings.geminiSafetyThreshold
                )

                if (aiResult.isFailure) {
                    return@launch onError(aiResult.exceptionOrNull()?.message ?: "Ошибка обращения к API")
                }

                val rawJson = aiResult.getOrThrow()
                val parsedResult = com.example.ai.engine.AiCreatorAssistantHelper.parseBotDraft(rawJson)
                if (parsedResult.isFailure) {
                    return@launch onError(parsedResult.exceptionOrNull()?.message ?: "Не удалось распознать структуру бота")
                }

                val draft = parsedResult.getOrThrow()
                val savedEntity = if (saveToDatabase) {
                    val playerName = activePersona.value?.name?.ifBlank { "Игрок" } ?: "Игрок"
                    val entity = draft.toCharacterEntity(base = null, playerName = playerName)
                    val newId = roleplayRepository.saveCharacter(entity)
                    entity.copy(id = newId)
                } else null

                if (chatIdForLog != null && chatIdForLog > 0L) {
                    val botName = savedEntity?.name ?: draft.name ?: "Новый персонаж"
                    val replyText = buildString {
                        appendLine("✨ **${draft.assistantMessage}**")
                        appendLine()
                        appendLine("Я создал бота **«$botName»** и автоматически заполнил все поля:")
                        if (!draft.shortDescription.isNullOrBlank()) appendLine("• **Описание:** ${draft.shortDescription}")
                        if (!draft.personality.isNullOrBlank()) appendLine("• **Характер:** ${draft.personality.take(160)}${if (draft.personality.length > 160) "..." else ""}")
                        if (!draft.appearance.isNullOrBlank()) appendLine("• **Внешность:** ${draft.appearance.take(140)}${if (draft.appearance.length > 140) "..." else ""}")
                        appendLine()
                        append("✅ Открываю редактор персонажа с уже заполненными полями!")
                    }
                    db.messageDao().insertMessage(
                        MessageEntity(
                            chatId = chatIdForLog,
                            sender = "character",
                            content = replyText,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                onSuccess(draft, savedEntity)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Ошибка генерации бота")
            }
        }
    }

    fun assistBotInEditor(
        promptPayload: String,
        onSuccess: (com.example.ai.engine.GeneratedBotDraft) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val profile = resolveActiveTextProfile()
                    ?: return@launch onError("Не настроен текстовый ИИ-провайдер. Проверьте раздел Провайдеры.")

                val settings = userSettings.value
                val systemPrompt = com.example.ai.engine.AiCreatorAssistantHelper.buildAssistBotInEditorSystemPrompt()
                val reqMsg = MessageEntity(
                    chatId = 0L,
                    sender = "user",
                    content = promptPayload
                )

                val aiResult = AiProviderEngine.generateChatCompletion(
                    profile = profile,
                    systemPrompt = systemPrompt,
                    recentMessages = listOf(reqMsg),
                    openRouterFallbackModels = settings.openRouterFallbackModels,
                    geminiSafetyThreshold = settings.geminiSafetyThreshold
                )

                if (aiResult.isFailure) {
                    return@launch onError(aiResult.exceptionOrNull()?.message ?: "Ошибка обращения к API")
                }

                val rawJson = aiResult.getOrThrow()
                val parsed = com.example.ai.engine.AiCreatorAssistantHelper.parseBotDraft(rawJson)
                if (parsed.isFailure) {
                    return@launch onError(parsed.exceptionOrNull()?.message ?: "Ошибка разбора ответа ИИ")
                }

                onSuccess(parsed.getOrThrow())
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Ошибка работы ИИ-помощника")
            }
        }
    }

    fun generatePersonaFromPrompt(
        userWishes: String,
        chatIdForLog: Long? = null,
        onSuccess: (com.example.ai.engine.GeneratedPersonaDraft, UserPersonaEntity) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val profile = resolveActiveTextProfile()
                    ?: return@launch onError("Не настроен текстовый ИИ-провайдер. Добавьте ключ в разделе Провайдеры.")

                if (chatIdForLog != null && chatIdForLog > 0L) {
                    roleplayRepository.insertUserMessage(
                        chatIdForLog,
                        "🎭 Помоги создать персону: $userWishes"
                    )
                }

                val settings = userSettings.value
                val systemPrompt = com.example.ai.engine.AiCreatorAssistantHelper.buildCreatePersonaSystemPrompt()
                val reqMsg = MessageEntity(
                    chatId = chatIdForLog ?: 0L,
                    sender = "user",
                    content = "Пожелания к моей персоне (персонажу игрока):\n$userWishes"
                )

                val aiResult = AiProviderEngine.generateChatCompletion(
                    profile = profile,
                    systemPrompt = systemPrompt,
                    recentMessages = listOf(reqMsg),
                    openRouterFallbackModels = settings.openRouterFallbackModels,
                    geminiSafetyThreshold = settings.geminiSafetyThreshold
                )

                if (aiResult.isFailure) {
                    return@launch onError(aiResult.exceptionOrNull()?.message ?: "Ошибка обращения к API")
                }

                val rawJson = aiResult.getOrThrow()
                val parsed = com.example.ai.engine.AiCreatorAssistantHelper.parsePersonaDraft(rawJson)
                if (parsed.isFailure) {
                    return@launch onError(parsed.exceptionOrNull()?.message ?: "Не удалось распознать структуру персоны")
                }

                val draft = parsed.getOrThrow()
                val entity = draft.toPersonaEntity()
                val newId = roleplayRepository.savePersona(entity)
                roleplayRepository.selectPersona(newId)
                val savedPersona = entity.copy(id = newId, isSelected = true)

                if (chatIdForLog != null && chatIdForLog > 0L) {
                    val replyText = buildString {
                        appendLine("🎭 **${draft.assistantMessage}**")
                        appendLine()
                        appendLine("Персона **«${savedPersona.name}»** создана, заполнена и сделана активной:")
                        if (savedPersona.shortDescription.isNotBlank()) appendLine("• **Описание:** ${savedPersona.shortDescription}")
                        if (savedPersona.personality.isNotBlank()) appendLine("• **Характер:** ${savedPersona.personality.take(150)}${if (savedPersona.personality.length > 150) "..." else ""}")
                        if (savedPersona.appearance.isNotBlank()) appendLine("• **Внешность:** ${savedPersona.appearance.take(140)}${if (savedPersona.appearance.length > 140) "..." else ""}")
                        appendLine()
                        append("✅ Все поля автоматически вставлены в твою Персону!")
                    }
                    db.messageDao().insertMessage(
                        MessageEntity(
                            chatId = chatIdForLog,
                            sender = "character",
                            content = replyText,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                onSuccess(draft, savedPersona)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Ошибка генерации персоны")
            }
        }
    }

    // Backup
    suspend fun exportBackup(
        uri: Uri,
        options: com.example.data.repository.SelectiveExportOptions = com.example.data.repository.SelectiveExportOptions()
    ): Result<BackupManifest> {
        return backupRepository.exportBackup(uri, options)
    }

    suspend fun inspectBackup(uri: Uri): Result<BackupManifest> {
        return backupRepository.inspectBackup(uri)
    }

    suspend fun importBackup(uri: Uri, replaceAll: Boolean): Result<Unit> {
        return backupRepository.importBackup(uri, replaceAll)
    }
}
