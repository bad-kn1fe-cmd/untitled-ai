package com.example.ui.screens.character_editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.UserPersonaEntity
import com.example.ui.components.AppAsyncImage
import com.example.ui.components.AvatarImage
import com.example.ui.components.CharacterLimitIndicator
import com.example.ui.components.PhotoCropEditorDialog
import com.example.ui.components.CropMode
import com.example.ui.components.AvatarFrameAdjusterDialog
import com.example.ui.components.UserFrameRepositoryDialog
import com.example.ui.components.RelationshipBoardSection
import com.example.util.RelationshipBoardHelper
import com.example.util.RelationshipBoardState
import org.json.JSONArray
import org.json.JSONObject

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

private data class EditorAssistantChatTurn(
    val sender: String, // "user" or "assistant"
    val text: String,
    val updatedFields: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditorScreen(
    character: CharacterEntity?,
    maxCharacterLimit: Int = 50000, // Up to 50,000 characters
    activePersona: UserPersonaEntity? = null,
    userFramesJson: String = "[]",
    onSaveFramesJson: ((String) -> Unit)? = null,
    customRelationshipTypesJson: String = "[]",
    onSaveCustomRelationshipTypesJson: ((String) -> Unit)? = null,
    onGenerateBotFromPrompt: ((String, (com.example.ai.engine.GeneratedBotDraft) -> Unit, (String) -> Unit) -> Unit)? = null,
    onAssistBotInEditor: ((String, (com.example.ai.engine.GeneratedBotDraft) -> Unit, (String) -> Unit) -> Unit)? = null,
    onSave: (CharacterEntity) -> Unit,
    onDelete: ((CharacterEntity) -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val charKey = character?.id ?: 0L
    var initializedCharId by rememberSaveable { mutableLongStateOf(if (character != null) character.id else 0L) }

    var name by rememberSaveable(charKey) { mutableStateOf(character?.name ?: "") }
    var avatarUri by rememberSaveable(charKey) { mutableStateOf(character?.avatarUri ?: "drawable://app_logo_black") }
    var shortDescription by rememberSaveable(charKey) { mutableStateOf(character?.shortDescription ?: "") }
    var personality by rememberSaveable(charKey) { mutableStateOf(character?.personality ?: "") }
    var appearance by rememberSaveable(charKey) { mutableStateOf(character?.appearance ?: "") }
    var habits by rememberSaveable(charKey) { mutableStateOf(character?.habits ?: "") }
    var likes by rememberSaveable(charKey) { mutableStateOf(character?.likes ?: "") }
    var dislikes by rememberSaveable(charKey) { mutableStateOf(character?.dislikes ?: "") }
    var greetingMessage by rememberSaveable(charKey) { mutableStateOf(character?.greetingMessage ?: "") }
    var ageText by rememberSaveable(charKey) { mutableStateOf((character?.age ?: 18).toString()) }
    var birthDate by rememberSaveable(charKey) { mutableStateOf(character?.birthDate ?: "") }
    var voiceStyle by rememberSaveable(charKey) { mutableStateOf(character?.voiceStyle ?: "cheeky") }
    var startingRelationship by rememberSaveable(charKey) { mutableStateOf(character?.startingRelationship ?: "strangers") }
    var customRelationship by rememberSaveable(charKey) { mutableStateOf(character?.customRelationship ?: "") }
    var initialRelationshipTitle by rememberSaveable(charKey) { mutableStateOf(character?.initialRelationshipTitle ?: "") }
    var initialRelationshipPercentText by rememberSaveable(charKey) {
        val pct = character?.initialRelationshipPercent ?: -999
        mutableStateOf(if (pct in -100..100) pct.toString() else "")
    }
    val defaultPlayerName = activePersona?.name?.trim()?.ifBlank { "Игрок" } ?: "Игрок"
    var relationshipBoardJson by rememberSaveable(charKey) {
        mutableStateOf(character?.relationshipBoardJson ?: "")
    }
    var boardState by remember(charKey) {
        mutableStateOf(
            RelationshipBoardHelper.parseBoard(
                json = relationshipBoardJson,
                mainCharName = character?.name?.ifBlank { "Персонаж 1" } ?: "Персонаж 1",
                playerName = defaultPlayerName,
                startingRelationship = character?.startingRelationship ?: "strangers",
                customRelationship = character?.customRelationship ?: "",
                initialTitle = character?.initialRelationshipTitle ?: "",
                initialPercent = character?.initialRelationshipPercent ?: -999
            )
        )
    }
    LaunchedEffect(character?.relationshipBoardJson) {
        val incoming = character?.relationshipBoardJson.orEmpty()
        if (incoming.isNotBlank() && incoming != relationshipBoardJson) {
            relationshipBoardJson = incoming
            boardState = RelationshipBoardHelper.parseBoard(
                json = incoming,
                mainCharName = character?.name?.ifBlank { "Персонаж 1" } ?: "Персонаж 1",
                playerName = defaultPlayerName,
                startingRelationship = character?.startingRelationship ?: "strangers",
                customRelationship = character?.customRelationship ?: "",
                initialTitle = character?.initialRelationshipTitle ?: "",
                initialPercent = character?.initialRelationshipPercent ?: -999
            )
        }
    }
    LaunchedEffect(name) {
        val cleanName = name.trim().ifBlank { "Персонаж 1" }
        val updatedNodes = boardState.nodes.map { node ->
            if (node.id == "node_main" || node.role == "main") node.copy(name = cleanName) else node
        }
        if (updatedNodes != boardState.nodes) {
            boardState = boardState.copy(nodes = updatedNodes)
            relationshipBoardJson = boardState.toJson()
        }
    }
    var bannerUri by rememberSaveable(charKey) { mutableStateOf(character?.bannerUri ?: "") }
    var greetingBannerUri by rememberSaveable(charKey) { mutableStateOf(character?.greetingBannerUri ?: "") }
    var isEditingGreetingBanner by rememberSaveable { mutableStateOf(false) }
    var communicationFormality by rememberSaveable(charKey) { mutableStateOf(character?.communicationFormality ?: "adaptive") }
    var avatarFrameUri by rememberSaveable(charKey) { mutableStateOf(character?.avatarFrameUri ?: "") }
    var author by rememberSaveable(charKey) { mutableStateOf(character?.author ?: "UntitledAI") }

    // Dialog state for photo cropping editor (avatar & banner)
    var showCropEditorDialog by rememberSaveable { mutableStateOf(false) }
    var cropMode by remember { mutableStateOf(CropMode.AVATAR_CIRCLE) }
    var cropImageSource by rememberSaveable { mutableStateOf("") }

    // Dialog state for avatar frame fitting and persistent transformations
    var showFrameAdjusterDialog by rememberSaveable { mutableStateOf(false) }
    var pendingFrameSource by rememberSaveable { mutableStateOf("") }
    var rawFrameSource by rememberSaveable { mutableStateOf("") }
    var frameScaleState by rememberSaveable { mutableFloatStateOf(1.0f) }
    var frameOffsetXState by rememberSaveable { mutableFloatStateOf(0f) }
    var frameOffsetYState by rememberSaveable { mutableFloatStateOf(0f) }
    var showFrameRepositoryDialog by rememberSaveable { mutableStateOf(false) }

    var showImportJsonDialog by rememberSaveable { mutableStateOf(false) }
    var showExportJsonDialog by rememberSaveable { mutableStateOf(false) }
    var jsonImportText by rememberSaveable { mutableStateOf("") }
    var importErrorText by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cropMode = CropMode.AVATAR_CIRCLE
            cropImageSource = uri.toString()
            showCropEditorDialog = true
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isEditingGreetingBanner = false
            cropMode = CropMode.BANNER_RECT
            cropImageSource = uri.toString()
            showCropEditorDialog = true
        }
    }

    val framePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val uriStr = uri.toString()
            rawFrameSource = uriStr
            pendingFrameSource = uriStr
            frameScaleState = 1.0f
            frameOffsetXState = 0f
            frameOffsetYState = 0f
            showFrameAdjusterDialog = true
        }
    }

    val greetingBannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cropImageSource = uri.toString()
            cropMode = CropMode.BANNER_RECT
            isEditingGreetingBanner = true
            showCropEditorDialog = true
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && jsonImportText.isNotBlank()) {
            val success = com.example.util.BotExportHelper.writeToUri(context, uri, jsonImportText)
            if (success) {
                android.widget.Toast.makeText(
                    context,
                    if (isEn) "Character saved to file!" else "Бот сохранен в файл!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                showExportJsonDialog = false
            } else {
                android.widget.Toast.makeText(
                    context,
                    if (isEn) "Error saving file" else "Ошибка сохранения файла",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var tagsText by rememberSaveable(charKey) {
        val str = try {
            val arr = JSONArray(character?.tags ?: "[]")
            (0 until arr.length()).map { arr.getString(it) }.joinToString(", ")
        } catch (_: Exception) { "" }
        mutableStateOf(str)
    }

    var savedKnowledgeCardsJson by rememberSaveable(charKey) {
        mutableStateOf(character?.knowledgeCards ?: "[]")
    }

    // Knowledge cards
    val knowledgeCards = remember(charKey) {
        val list = mutableStateListOf<Pair<String, String>>()
        try {
            val arr = JSONArray(savedKnowledgeCardsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(obj.optString("title") to obj.optString("content"))
            }
        } catch (_: Exception) {}
        list
    }

    val syncKnowledgeCardsToSaved: () -> Unit = {
        val arr = JSONArray()
        knowledgeCards.forEach { (t, c) ->
            val obj = JSONObject()
            obj.put("title", t)
            obj.put("content", c)
            arr.put(obj)
        }
        savedKnowledgeCardsJson = arr.toString()
    }

    var referenceImagesJson by rememberSaveable(charKey) {
        mutableStateOf(character?.referenceImages ?: "[]")
    }

    LaunchedEffect(character?.id) {
        if (character != null && character.id != 0L && initializedCharId != character.id) {
            name = character.name
            avatarUri = character.avatarUri
            shortDescription = character.shortDescription
            personality = character.personality
            appearance = character.appearance
            habits = character.habits
            likes = character.likes
            dislikes = character.dislikes
            greetingMessage = character.greetingMessage
            ageText = character.age.toString()
            birthDate = character.birthDate
            voiceStyle = character.voiceStyle
            startingRelationship = character.startingRelationship
            customRelationship = character.customRelationship
            initialRelationshipTitle = character.initialRelationshipTitle
            initialRelationshipPercentText = if (character.initialRelationshipPercent in -100..100) character.initialRelationshipPercent.toString() else ""
            relationshipBoardJson = character.relationshipBoardJson
            boardState = RelationshipBoardHelper.parseBoard(
                json = character.relationshipBoardJson,
                mainCharName = character.name.ifBlank { "Персонаж 1" },
                playerName = defaultPlayerName,
                startingRelationship = character.startingRelationship,
                customRelationship = character.customRelationship,
                initialTitle = character.initialRelationshipTitle,
                initialPercent = character.initialRelationshipPercent
            )
            bannerUri = character.bannerUri
            greetingBannerUri = character.greetingBannerUri
            communicationFormality = character.communicationFormality
            avatarFrameUri = character.avatarFrameUri
            author = character.author
            tagsText = try {
                val arr = JSONArray(character.tags)
                (0 until arr.length()).map { arr.getString(it) }.joinToString(", ")
            } catch (_: Exception) { "" }
            savedKnowledgeCardsJson = character.knowledgeCards
            knowledgeCards.clear()
            try {
                val arr = JSONArray(character.knowledgeCards)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    knowledgeCards.add(obj.optString("title") to obj.optString("content"))
                }
            } catch (_: Exception) {}
            referenceImagesJson = character.referenceImages
            initializedCharId = character.id
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileContent = com.example.util.BotExportHelper.readJsonFromUri(context, uri)
            if (!fileContent.isNullOrBlank()) {
                val result = com.example.data.repository.CharacterSerializer.importFromJson(fileContent, context)
                val imported = result.getOrNull()
                if (imported != null) {
                    name = imported.name
                    avatarUri = imported.avatarUri
                    shortDescription = imported.shortDescription
                    personality = imported.personality
                    appearance = imported.appearance
                    habits = imported.habits
                    likes = imported.likes
                    dislikes = imported.dislikes
                    greetingMessage = imported.greetingMessage
                    ageText = imported.age.toString()
                    birthDate = imported.birthDate
                    voiceStyle = imported.voiceStyle

                    val tagsStr = try {
                        val arr = JSONArray(imported.tags)
                        (0 until arr.length()).map { arr.getString(it) }.joinToString(", ")
                    } catch (_: Exception) { "" }
                    tagsText = tagsStr

                    knowledgeCards.clear()
                    try {
                        val arr = JSONArray(imported.knowledgeCards)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            knowledgeCards.add(obj.optString("title") to obj.optString("content"))
                        }
                    } catch (_: Exception) {}

                    syncKnowledgeCardsToSaved()
                    referenceImagesJson = imported.referenceImages

                    if (imported.avatarFrameUri.isNotBlank()) avatarFrameUri = imported.avatarFrameUri
                    if (imported.bannerUri.isNotBlank()) bannerUri = imported.bannerUri
                    if (imported.greetingBannerUri.isNotBlank()) greetingBannerUri = imported.greetingBannerUri
                    author = imported.author.ifBlank { "UntitledAI" }
                    if (imported.communicationFormality.isNotBlank()) communicationFormality = imported.communicationFormality
                    if (imported.startingRelationship.isNotBlank()) startingRelationship = imported.startingRelationship
                    if (imported.customRelationship.isNotBlank()) customRelationship = imported.customRelationship
                    if (imported.initialRelationshipTitle.isNotBlank()) initialRelationshipTitle = imported.initialRelationshipTitle
                    if (imported.initialRelationshipPercent in -100..100) initialRelationshipPercentText = imported.initialRelationshipPercent.toString()
                    relationshipBoardJson = imported.relationshipBoardJson
                    boardState = RelationshipBoardHelper.parseBoard(
                        json = imported.relationshipBoardJson,
                        mainCharName = imported.name.ifBlank { "Персонаж 1" },
                        playerName = defaultPlayerName,
                        startingRelationship = imported.startingRelationship,
                        customRelationship = imported.customRelationship,
                        initialTitle = imported.initialRelationshipTitle,
                        initialPercent = imported.initialRelationshipPercent
                    )

                    jsonImportText = fileContent
                    importErrorText = null
                    showImportJsonDialog = false

                    android.widget.Toast.makeText(
                        context,
                        if (isEn) "Loaded bot '${imported.name}' from file!" else "Бот '${imported.name}' загружен из файла!",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val err = result.exceptionOrNull()?.message ?: (if (isEn) "Failed to parse JSON" else "Ошибка распознавания JSON карточки")
                    importErrorText = err
                    android.widget.Toast.makeText(context, "Ошибка: $err", android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                android.widget.Toast.makeText(
                    context,
                    if (isEn) "Failed to read file" else "Не удалось прочитать файл",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabListStates = remember { List(5) { androidx.compose.foundation.lazy.LazyListState() } }
    var collapsedLoreCardIndices by remember { mutableStateOf(setOf<Int>()) }
    val tabs = if (isEn) listOf("General", "Personality", "Lore & Cards", "Greeting", "Relations (BETA)")
    else listOf("Основное", "Личность", "Лор и Карточки", "Приветствие", "Отношения (БЕТА)")

    // Live calculation of character limit memoized with derivedStateOf for zero lag
    val totalCharsLength by remember(name, shortDescription, personality, appearance, habits, likes, dislikes, greetingMessage, knowledgeCards.size) {
        derivedStateOf {
            name.length + shortDescription.length + personality.length +
                    appearance.length + habits.length + likes.length + dislikes.length +
                    greetingMessage.length + knowledgeCards.sumOf { it.first.length + it.second.length }
        }
    }
    val isOverLimit = totalCharsLength > maxCharacterLimit

    val hasExistingText by remember(
        name, shortDescription, personality, appearance,
        habits, likes, dislikes, greetingMessage, knowledgeCards.size
    ) {
        derivedStateOf {
            name.isNotBlank() || shortDescription.isNotBlank() || personality.isNotBlank() ||
                    appearance.isNotBlank() || habits.isNotBlank() || likes.isNotBlank() ||
                    dislikes.isNotBlank() || greetingMessage.isNotBlank() || knowledgeCards.isNotEmpty()
        }
    }

    var showCreateByPromptDialog by rememberSaveable { mutableStateOf(false) }
    var createPromptInput by rememberSaveable { mutableStateOf("") }
    var isGeneratingBotByPrompt by remember { mutableStateOf(false) }
    var createByPromptError by remember { mutableStateOf<String?>(null) }

    var showFloatingAssistantWindow by rememberSaveable { mutableStateOf(false) }
    var isFloatingAssistantMinimized by rememberSaveable { mutableStateOf(false) }
    var floatingAssistantInput by rememberSaveable { mutableStateOf("") }
    var isFloatingAssistantLoading by remember { mutableStateOf(false) }
    var floatingOffsetX by remember { mutableFloatStateOf(0f) }
    var floatingOffsetY by remember { mutableFloatStateOf(0f) }
    val assistantHistory = remember {
        mutableStateListOf(
            EditorAssistantChatTurn(
                sender = "assistant",
                text = "Привет! Я помогу дописать или улучшить бота. Напиши, что добавить или изменить — я сам вставлю текст в нужные поля!"
            )
        )
    }

    val applyDraftToEditorFields: (com.example.ai.engine.GeneratedBotDraft) -> List<String> = { draft ->
        val updated = mutableListOf<String>()
        if (!draft.name.isNullOrBlank()) {
            name = draft.name
            updated.add("Имя")
        }
        if (draft.age != null && draft.age > 0) {
            ageText = draft.age.toString()
            updated.add("Возраст")
        }
        if (!draft.shortDescription.isNullOrBlank()) {
            shortDescription = draft.shortDescription
            updated.add("Краткое описание")
        }
        if (!draft.personality.isNullOrBlank()) {
            personality = draft.personality
            updated.add("Характер")
        }
        if (!draft.appearance.isNullOrBlank()) {
            appearance = draft.appearance
            updated.add("Внешность")
        }
        if (!draft.habits.isNullOrBlank()) {
            habits = draft.habits
            updated.add("Привычки")
        }
        if (!draft.likes.isNullOrBlank()) {
            likes = draft.likes
            updated.add("Любит")
        }
        if (!draft.dislikes.isNullOrBlank()) {
            dislikes = draft.dislikes
            updated.add("Не любит")
        }
        if (!draft.greetingMessage.isNullOrBlank()) {
            greetingMessage = draft.greetingMessage
            updated.add("Приветствие")
        }
        if (!draft.tags.isNullOrEmpty()) {
            tagsText = draft.tags.joinToString(", ")
            updated.add("Теги")
        }
        val validVoices = setOf("natural", "cheeky", "formal", "shy", "gentle", "cold", "playful")
        if (!draft.voiceStyle.isNullOrBlank() && draft.voiceStyle.lowercase() in validVoices) {
            voiceStyle = draft.voiceStyle.lowercase()
            updated.add("Стиль речи")
        }
        val validFormalities = setOf("adaptive", "informal", "formal")
        if (!draft.communicationFormality.isNullOrBlank() && draft.communicationFormality.lowercase() in validFormalities) {
            communicationFormality = draft.communicationFormality.lowercase()
        }
        val validRels = setOf("strangers", "friends", "lovers", "enemies", "rivals", "colleagues", "custom")
        if (!draft.startingRelationship.isNullOrBlank() && draft.startingRelationship.lowercase() in validRels) {
            startingRelationship = draft.startingRelationship.lowercase()
            updated.add("Отношения")
        }
        if (!draft.customRelationship.isNullOrBlank()) {
            customRelationship = draft.customRelationship
        }
        if (!draft.initialRelationshipTitle.isNullOrBlank()) {
            initialRelationshipTitle = draft.initialRelationshipTitle
        }
        if (!draft.knowledgeCards.isNullOrEmpty()) {
            draft.knowledgeCards.forEach { (title, content) ->
                val existingIdx = knowledgeCards.indexOfFirst { it.first.equals(title, ignoreCase = true) }
                if (existingIdx >= 0) {
                    knowledgeCards[existingIdx] = title to content
                } else {
                    knowledgeCards.add(title to content)
                }
            }
            syncKnowledgeCardsToSaved()
            updated.add("Карточки лора (${draft.knowledgeCards.size})")
        }
        val updatedBoard = RelationshipBoardHelper.parseBoard(
            json = relationshipBoardJson,
            mainCharName = name.ifBlank { "Персонаж 1" },
            playerName = defaultPlayerName,
            startingRelationship = startingRelationship,
            customRelationship = customRelationship,
            initialTitle = initialRelationshipTitle,
            initialPercent = initialRelationshipPercentText.toIntOrNull() ?: -999
        )
        boardState = updatedBoard
        relationshipBoardJson = updatedBoard.toJson()
        updated
    }

    val age = ageText.toIntOrNull() ?: 18
    val isAdult = true

    val presetAvatars = listOf(
        "drawable://app_logo_black" to (if (isEn) "Untitled AI (Dark)" else "Untitled AI (Тёмная)"),
        "drawable://app_logo_white" to (if (isEn) "Untitled AI (Light)" else "Untitled AI (Светлая)")
    )

    val voicePresets = if (isEn) listOf(
        "natural" to "Natural / Expressive",
        "cheeky" to "Sassy / Ironic",
        "formal" to "Formal / Strict",
        "shy" to "Shy / Soft",
        "gentle" to "Poetic / Warm",
        "cold" to "Cold / Calculated",
        "playful" to "Playful / Cheerful"
    ) else listOf(
        "natural" to "Естественный / Живой",
        "cheeky" to "Дерзкий / Ироничный",
        "formal" to "Формальный / Строгий",
        "shy" to "Застенчивый / Мягкий",
        "gentle" to "Поэтичный / Тёплый",
        "cold" to "Холодный / Расчётливый",
        "playful" to "Игривый / Весёлый"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (character == null || character.id == 0L) {
                            if (isEn) "New Character" else "Новый персонаж"
                        } else {
                            if (isEn) "Edit Character" else "Редактирование"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isEn) "Back" else "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val serializedBoard = boardState.toJson()
                        val computedAvg = boardState.calculateAverageScore()
                        val computedTitle = boardState.calculateOverallTitle(initialRelationshipTitle.trim())
                        val entity = (character ?: CharacterEntity(name = name, avatarUri = avatarUri)).copy(
                            name = name.trim(),
                            avatarUri = avatarUri,
                            shortDescription = shortDescription.trim(),
                            personality = personality.trim(),
                            appearance = appearance.trim(),
                            habits = habits.trim(),
                            likes = likes.trim(),
                            dislikes = dislikes.trim(),
                            greetingMessage = greetingMessage.trim(),
                            age = age,
                            birthDate = birthDate.trim(),
                            voiceStyle = voiceStyle,
                            isAdult = isAdult,
                            avatarFrameUri = avatarFrameUri.trim(),
                            bannerUri = bannerUri.trim(),
                            greetingBannerUri = greetingBannerUri.trim(),
                            startingRelationship = startingRelationship,
                            customRelationship = customRelationship.trim(),
                            initialRelationshipTitle = computedTitle,
                            initialRelationshipPercent = computedAvg,
                            relationshipBoardJson = serializedBoard,
                            author = author.trim().ifBlank { "UntitledAI" }
                        )
                        jsonImportText = com.example.data.repository.CharacterSerializer.exportToJson(entity)
                        showExportJsonDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = if (isEn) "Export JSON" else "Экспорт JSON",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = if (isEn) "Import from .json file" else "Загрузить из файла .json",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = {
                        jsonImportText = ""
                        importErrorText = null
                        showImportJsonDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = if (isEn) "Import JSON" else "Импорт JSON",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (character != null && character.id != 0L && onDelete != null) {
                        IconButton(onClick = { onDelete(character) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (isEn) "Delete" else "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            val tagsArray = JSONArray()
                            tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tagsArray.put(it) }

                            val cardsArray = JSONArray()
                            knowledgeCards.forEach { card ->
                                if (card.first.isNotBlank()) {
                                    val obj = JSONObject()
                                    obj.put("title", card.first)
                                    obj.put("content", card.second)
                                    cardsArray.put(obj)
                                }
                            }

                            val serializedBoard = boardState.toJson()
                            val computedAvg = boardState.calculateAverageScore()
                            val computedTitle = boardState.calculateOverallTitle(initialRelationshipTitle.trim())
                            val entity = (character ?: CharacterEntity(name = name, avatarUri = avatarUri)).copy(
                                name = name.trim(),
                                avatarUri = avatarUri,
                                author = author.trim().ifBlank { "UntitledAI" },
                                shortDescription = shortDescription.trim(),
                                personality = personality.trim(),
                                appearance = appearance.trim(),
                                habits = habits.trim(),
                                likes = likes.trim(),
                                dislikes = dislikes.trim(),
                                greetingMessage = greetingMessage.trim(),
                                age = age,
                                birthDate = birthDate.trim(),
                                knowledgeCards = cardsArray.toString(),
                                referenceImages = referenceImagesJson,
                                tags = tagsArray.toString(),
                                voiceStyle = voiceStyle,
                                startingRelationship = startingRelationship,
                                customRelationship = customRelationship.trim(),
                                initialRelationshipTitle = computedTitle,
                                initialRelationshipPercent = computedAvg,
                                relationshipBoardJson = serializedBoard,
                                bannerUri = bannerUri.trim(),
                                greetingBannerUri = greetingBannerUri.trim(),
                                communicationFormality = communicationFormality,
                                avatarFrameUri = avatarFrameUri.trim(),
                                isAdult = isAdult
                            )
                            onSave(entity)
                        },
                        enabled = name.isNotBlank() && !isOverLimit,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEn) "Save" else "Сохранить")
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
            // Live Limit Bar
            CharacterLimitIndicator(
                currentLength = totalCharsLength,
                maxLimit = maxCharacterLimit,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // AI Creation / Co-writer Banner
            if (!hasExistingText) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            createByPromptError = null
                            showCreateByPromptDialog = true
                        }
                        .testTag("create_bot_by_prompt_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = if (isEn) "✨ Create Bot from Prompt" else "✨ Создать бота по промпту",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEn) "Describe your idea and AI will fill all fields automatically" else "Опиши идею — ИИ сам напишет и автоматически заполнит все поля",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Button(
                            onClick = {
                                createByPromptError = null
                                showCreateByPromptDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (isEn) "Create" else "Создать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEn) "Need help expanding or refining fields?" else "Нужна помощь с доработкой персонажа?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            showFloatingAssistantWindow = true
                            isFloatingAssistantMinimized = false
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("assist_bot_floating_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "🪄 Help!" else "🪄 Помоги!",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tabs (ScrollableTabRow for 5 tabs including Отношения БЕТА)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                fontSize = 12.5.sp
                            )
                        }
                    )
                }
            }

            LazyColumn(
                state = tabListStates[selectedTab],
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Basic Tab
                        item {
                            Text(
                                text = if (isEn) "Character Avatar" else "Аватар персонажа",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clickable {
                                            cropMode = CropMode.AVATAR_CIRCLE
                                            cropImageSource = avatarUri
                                            showCropEditorDialog = true
                                        }
                                ) {
                                    AvatarImage(
                                        uri = avatarUri,
                                        contentDescription = if (isEn) "Avatar" else "Аватар",
                                        size = 76.dp,
                                        frameUri = avatarFrameUri
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(22.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Crop,
                                            contentDescription = "Редактировать фото",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                avatarPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isEn) "Upload photo" else "Загрузить фото", fontSize = 13.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                cropMode = CropMode.AVATAR_CIRCLE
                                                cropImageSource = avatarUri
                                                showCropEditorDialog = true
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isEn) "Crop / Fit" else "Подогнать", fontSize = 12.sp)
                                        }
                                    }

                                    Text(
                                        text = if (isEn) "Click photo to move, scale & crop to circle" else "Нажмите на фото, чтобы двигать и подогнать под круг",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(presetAvatars) { (uri, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (avatarUri == uri) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, if (avatarUri == uri) MaterialTheme.colorScheme.primary else Color.Transparent),
                                        modifier = Modifier.clickable { avatarUri = uri }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AvatarImage(uri = uri, contentDescription = null, size = 28.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(label, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }

                        // Avatar Frame Section (Рамка для бота)
                        item {
                            Text(
                                text = if (isEn) "Bot Avatar Frame (Transparent PNG)" else "Рамка для бота (PNG с прозрачным фоном)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(76.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AvatarImage(
                                            uri = avatarUri,
                                            contentDescription = "Аватар с рамкой",
                                            size = 76.dp,
                                            frameUri = avatarFrameUri
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (avatarFrameUri.isBlank()) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { showFrameRepositoryDialog = true },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(if (isEn) "Frames Vault" else "Хранилище рамок", fontSize = 12.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        framePickerLauncher.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                 ) {
                                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isEn) "PNG File" else "Файл PNG", fontSize = 12.sp)
                                                }
                                            }
                                            Text(
                                                text = if (isEn) "Choose a frame from your Frame Vault (including Halloween) or upload a custom PNG frame."
                                                else "Выберите рамку из хранилища рамок (включая Halloween) или загрузите свой PNG файл.",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                             Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { showFrameRepositoryDialog = true },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isEn) "Vault" else "Хранилище", fontSize = 12.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        pendingFrameSource = if (rawFrameSource.isNotBlank()) rawFrameSource else avatarFrameUri
                                                        showFrameAdjusterDialog = true
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isEn) "Adjust" else "Подстроить", fontSize = 12.sp)
                                                }

                                                FilledTonalIconButton(
                                                    onClick = {
                                                        avatarFrameUri = ""
                                                        rawFrameSource = ""
                                                        frameScaleState = 1.0f
                                                        frameOffsetXState = 0f
                                                        frameOffsetYState = 0f
                                                    },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Удалить рамку", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                            Text(
                                                text = if (isEn) "Frame applied & saved to JSON via Base64" else "Рамка сохраняется в JSON персонажа по Base64",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Banner section
                        item {
                            Text(
                                text = if (isEn) "Character Banner Image" else "Баннер персонажа",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        isEditingGreetingBanner = false
                                        if (bannerUri.isNotBlank()) {
                                            cropMode = CropMode.BANNER_RECT
                                            cropImageSource = bannerUri
                                            showCropEditorDialog = true
                                        } else {
                                            bannerPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (bannerUri.isNotBlank()) {
                                        com.example.ui.components.AppAsyncImage(
                                            model = bannerUri,
                                            contentDescription = "Character Banner",
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
                                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(if (isEn) "Click to add banner" else "Нажмите, чтобы выбрать баннер", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (bannerUri.isNotBlank()) {
                                            FilledTonalIconButton(
                                                onClick = { bannerUri = "" },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Удалить баннер", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                            FilledTonalIconButton(
                                                onClick = {
                                                    isEditingGreetingBanner = false
                                                    cropMode = CropMode.BANNER_RECT
                                                    cropImageSource = bannerUri
                                                    showCropEditorDialog = true
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Crop, contentDescription = "Подрезать баннер", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Button(
                                            onClick = {
                                                bannerPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (bannerUri.isBlank()) (if (isEn) "Add Banner" else "Выбрать баннер") else (if (isEn) "Change" else "Сменить"), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(if (isEn) "Character Name *" else "Имя персонажа *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = author,
                                onValueChange = { author = it },
                                label = { Text(if (isEn) "Author" else "Автор") },
                                placeholder = { Text(if (isEn) "Author (saved as 'by' in JSON)" else "Автор (сохраняется в поле 'by' в JSON)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = ageText,
                                    onValueChange = { ageText = it },
                                    label = { Text(if (isEn) "Age" else "Возраст") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = birthDate,
                                    onValueChange = { birthDate = it },
                                    label = { Text(if (isEn) "Birth Date" else "Дата рождения") },
                                    placeholder = { Text(if (isEn) "YYYY-MM-DD" else "ГГГГ-ММ-ДД") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = shortDescription,
                                onValueChange = { shortDescription = it },
                                label = { Text(if (isEn) "Short Card Description *" else "Краткое описание для карточки *") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 6,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = voiceStyle,
                                onValueChange = { voiceStyle = it },
                                label = { Text(if (isEn) "Voice & Tone Style *" else "Голос и манера общения *") },
                                placeholder = { Text(if (isEn) "E.g.: Sassy, ironic, loves sarcasm, friendly" else "Например: Дерзкий, ироничный, любит сарказм, обращается на «ты»") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEn) "Quick Presets:" else "Быстрые шаблоны:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val presetChips = if (isEn) listOf(
                                "Natural & lively",
                                "Sassy & ironic",
                                "Formal & strict",
                                "Shy & soft",
                                "Poetic & warm",
                                "Cold & calculating"
                            ) else listOf(
                                "Естественный и живой",
                                "Дерзкий и ироничный",
                                "Формальный и строгий",
                                "Застенчивый и мягкий",
                                "Поэтичный и тёплый",
                                "Холодный и расчётливый"
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(presetChips) { preset ->
                                    AssistChip(
                                        onClick = { voiceStyle = preset },
                                        label = { Text(preset, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = tagsText,
                                onValueChange = { tagsText = it },
                                label = { Text(if (isEn) "Tags (comma-separated)" else "Теги (через запятую)") },
                                placeholder = { Text(if (isEn) "Futurism, Detective, Mystery, Friendship" else "Футуризм, Детектив, Ирония, Дружба") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    1 -> {
                        // Personality & Appearance Tab
                        item {
                            OutlinedTextField(
                                value = personality,
                                onValueChange = { personality = it },
                                label = { Text(if (isEn) "Personality, Psychology, Tone *" else "Характер, психология, манера речи *") },
                                placeholder = { Text(if (isEn) "Describe character traits, sense of humor, response to compliments, weaknesses..." else "Опишите характер, манеру шутить, реакцию на комплименты, слабости...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = appearance,
                                onValueChange = { appearance = it },
                                label = { Text(if (isEn) "Appearance Description *" else "Описание внешности *") },
                                placeholder = { Text(if (isEn) "Hair color, eyes, build, clothing, distinctive features..." else "Цвет волос, глаз, телосложение, одежда, отличительные черты...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = habits,
                                onValueChange = { habits = it },
                                label = { Text(if (isEn) "Habits" else "Привычки") },
                                placeholder = { Text(if (isEn) "Snaps fingers, twirls hair, drinks tea constantly..." else "Щёлкает пальцами, крутит прядь волос, часто пьёт чай...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = likes,
                                onValueChange = { likes = it },
                                label = { Text(if (isEn) "Likes / Interests" else "Любимые вещи / интересы") },
                                placeholder = { Text(if (isEn) "Jazz, night walks, books, rain..." else "Джаз, ночные прогулки, книги, дождь...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = dislikes,
                                onValueChange = { dislikes = it },
                                label = { Text(if (isEn) "Dislikes / Turn-offs" else "Нелюбимые вещи / антипатии") },
                                placeholder = { Text(if (isEn) "Lies, loud noises, cold coffee..." else "Ложь, громкие звуки, холодный кофе...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isEn) "Starting Relationship with User" else "Стартовые отношения с пользователем",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isEn) "Sets the initial trust level, distance, and context for all new chats with this character."
                                        else "Определяет исходный уровень доверия, дистанцию и контекст общения во всех новых диалогах с этим персонажем.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    val relPresets = if (isEn) listOf(
                                        "strangers" to "Strangers (Caution, emotional distance)",
                                        "acquaintances" to "Acquaintances (Casual, surface chats)",
                                        "friends" to "Friends (Warmth, mutual humor & trust)",
                                        "close_friends" to "Close Friends (Deep trust & secrets)",
                                        "romantic" to "Romantic (Attraction, crush, tender chemistry)",
                                        "custom" to "Custom context..."
                                    ) else listOf(
                                        "strangers" to "Незнакомцы (настороженность, дистанция)",
                                        "acquaintances" to "Знакомые (поверхностное общение)",
                                        "friends" to "Друзья (поддержка, юмор, теплота)",
                                        "close_friends" to "Близкие друзья (высокое доверие)",
                                        "romantic" to "Романтические отношения (взаимное влечение)",
                                        "custom" to "Свой контекст отношений..."
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        relPresets.forEach { (key, label) ->
                                            val isSelected = startingRelationship == key
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { startingRelationship = key }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = { startingRelationship = key }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }

                                    if (startingRelationship == "custom") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = customRelationship,
                                            onValueChange = { customRelationship = it },
                                            label = { Text(if (isEn) "Describe Starting Context" else "Опишите исходный контекст отношений") },
                                            placeholder = { Text(if (isEn) "e.g., Former partners who haven't spoken in 5 years..." else "Например: Бывшие напарники, потерявшие связь 5 лет назад...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Shortcut to the new BETA Relationship Board tab
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedTab = 4 }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isEn) "Relationship Board (BETA)" else "Доска отношений (БЕТА)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val avgScore = boardState.calculateAverageScore()
                                                val sign = if (avgScore > 0) "+" else ""
                                                Text(
                                                    text = if (isEn) "Nodes: ${boardState.nodes.size} • Avg: $sign$avgScore% (${boardState.calculateOverallTitle()})"
                                                    else "Персонажей: ${boardState.nodes.size} • Среднее: $sign$avgScore% (${boardState.calculateOverallTitle()})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Hub,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Communication Formality / Distance
                                    Text(
                                        text = if (isEn) "Tone & Communication Distance" else "Дистанция и тон в общении",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isEn) "Controls address form (ты/вы) and conversational closeness" else "Определяет обращение (на «ты» или на «вы») и психологическую дистанцию бота",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    val formalityOptions = if (isEn) listOf(
                                        "adaptive" to "Adaptive (Natural dynamic distance)",
                                        "informal_ty" to "Informal (Close, casual)",
                                        "formal_vy" to "Formal / Respectful (Polite)",
                                        "distant" to "Cold & Distant (Strict boundary)",
                                        "flirty" to "Flirtatious & Playful",
                                        "submissive" to "Gentle & Submissive",
                                        "dominant" to "Dominant & Commanding"
                                    ) else listOf(
                                        "adaptive" to "Адаптивный (Естественная гибкая дистанция)",
                                        "informal_ty" to "Неформальный (Строго на «ты», дружеский тон)",
                                        "formal_vy" to "Уважительный (Строго на «вы», вежливость)",
                                        "distant" to "Холодная дистанция (Строгий, отстранённый)",
                                        "flirty" to "Игривый / Флиртующий (Кокетливый)",
                                        "submissive" to "Покорный / Мягкий (Уступчивый)",
                                        "dominant" to "Доминантный / Властный (Уверенный контроль)"
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        formalityOptions.forEach { (key, label) ->
                                            val isSelected = communicationFormality == key
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { communicationFormality = key }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = { communicationFormality = key }
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Lore & Knowledge Cards
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEn) "Knowledge Cards & Lore (${knowledgeCards.size})" else "Карточки знаний и лор (${knowledgeCards.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (knowledgeCards.isNotEmpty()) {
                                        TextButton(onClick = {
                                            collapsedLoreCardIndices = if (collapsedLoreCardIndices.size == knowledgeCards.size) {
                                                emptySet()
                                            } else {
                                                knowledgeCards.indices.toSet()
                                            }
                                        }) {
                                            Text(
                                                text = if (collapsedLoreCardIndices.size == knowledgeCards.size) {
                                                    if (isEn) "Expand all" else "Развернуть все"
                                                } else {
                                                    if (isEn) "Collapse all" else "Свернуть все"
                                                },
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                    TextButton(onClick = {
                                        knowledgeCards.add("" to "")
                                        syncKnowledgeCardsToSaved()
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEn) "Add Card" else "Добавить карточку")
                                    }
                                }
                            }
                        }

                        if (knowledgeCards.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isEn) "Add specialized knowledge cards: backstory facts, hidden secrets, biography, or important universe rules." else "Добавьте специальные карточки знаний: факты из прошлого, скрытые секреты, биографию или важные правила вселенной.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }

                        items(knowledgeCards.indices.toList()) { index ->
                            val card = knowledgeCards[index]
                            val isCollapsed = collapsedLoreCardIndices.contains(index)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                collapsedLoreCardIndices = if (isCollapsed) {
                                                    collapsedLoreCardIndices - index
                                                } else {
                                                    collapsedLoreCardIndices + index
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                contentDescription = if (isCollapsed) "Развернуть карточку" else "Свернуть карточку",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        OutlinedTextField(
                                            value = card.first,
                                            onValueChange = { newTitle ->
                                                knowledgeCards[index] = newTitle to card.second
                                                syncKnowledgeCardsToSaved()
                                            },
                                            label = { Text(if (isEn) "Card Title" else "Заголовок карточки") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        IconButton(onClick = {
                                            knowledgeCards.removeAt(index)
                                            collapsedLoreCardIndices = collapsedLoreCardIndices.filter { it != index }.map { if (it > index) it - 1 else it }.toSet()
                                            syncKnowledgeCardsToSaved()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = if (isEn) "Delete" else "Удалить",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = !isCollapsed,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = card.second,
                                                onValueChange = { newContent ->
                                                    knowledgeCards[index] = card.first to newContent
                                                    syncKnowledgeCardsToSaved()
                                                },
                                                label = { Text(if (isEn) "Content / Lore" else "Содержание / Лор") },
                                                minLines = 2,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }

                                    if (isCollapsed && card.second.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = card.second,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Greeting Message Tab
                        item {
                            val defaultUserName = if (isEn) "User" else "Пользователь"
                            val currentUserName = activePersona?.name?.ifBlank { defaultUserName } ?: defaultUserName
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isEn) "User Placeholder" else "Упоминание пользователя",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isEn) "To mention the user name, use {{user}} (active persona: $currentUserName)" else "Чтобы упомянуть имя пользователя, используйте {{user}} (текущая персона: $currentUserName)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEn) "Greeting Message Banner" else "Баннер приветственного сообщения",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (greetingBannerUri.isNotBlank()) {
                                    TextButton(onClick = { greetingBannerUri = "" }) {
                                        Text(if (isEn) "Remove Banner" else "Удалить баннер", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                    }
                                }
                            }
                            Text(
                                text = "Welcome Banner",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        greetingBannerPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (greetingBannerUri.isNotBlank()) {
                                        com.example.ui.components.AppAsyncImage(
                                            model = greetingBannerUri,
                                            contentDescription = "Greeting Banner",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = if (isEn) "Add banner image for greeting post" else "Добавить баннер для приветственного сообщения",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isEn) "Greeting Message *" else "Приветственное сообщение *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEn) "Use roleplay syntax: **Name** for name, *action* for narrative actions, <thoughts> for internal monologue." else "Используйте синтаксис ролплея: **Имя** для имени, *действие* для действий и описаний, <мысли> для внутренних мыслей.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = greetingMessage,
                                onValueChange = { greetingMessage = it },
                                placeholder = { Text(if (isEn) "**Name** *looks in your direction and takes a step closer.* \n\n— Hello, {{user}}! Good to see you." else "**Имя** *делает действие и смотрит на тебя.* \n\n— Здравствуй, {{user}}! Рад встрече.") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 6,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    4 -> {
                        // Relationship Board (BETA) Tab
                        item {
                            RelationshipBoardSection(
                                boardState = boardState,
                                onBoardChange = { updatedBoard ->
                                    boardState = updatedBoard
                                    relationshipBoardJson = updatedBoard.toJson()
                                    initialRelationshipTitle = updatedBoard.calculateOverallTitle(initialRelationshipTitle)
                                    initialRelationshipPercentText = updatedBoard.calculateAverageScore().toString()
                                },
                                customTypesJson = customRelationshipTypesJson,
                                onCustomTypesChange = { newCustomJson ->
                                    onSaveCustomRelationshipTypesJson?.invoke(newCustomJson)
                                },
                                isReadOnly = false,
                                canvasHeightDp = 420
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Bot by Prompt Dialog (when fields are empty or triggered)
    if (showCreateByPromptDialog) {
        Dialog(onDismissRequest = { if (!isGeneratingBotByPrompt) showCreateByPromptDialog = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isEn) "Create Bot from Prompt" else "Создать бота по промпту",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!isGeneratingBotByPrompt) {
                            IconButton(onClick = { showCreateByPromptDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Закрыть")
                            }
                        }
                    }

                    Text(
                        text = if (isEn) {
                            "Describe who you want to create (character, genre, personality, starting scene). AI will generate and fill all fields automatically."
                        } else {
                            "Опиши свои пожелания (кто этот персонаж, его характер, внешность, вселенная или начальная сцена). ИИ сгенерирует бота и автоматически заполнит все поля."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = createPromptInput,
                        onValueChange = {
                            createPromptInput = it
                            createByPromptError = null
                        },
                        enabled = !isGeneratingBotByPrompt,
                        placeholder = {
                            Text(
                                if (isEn) "E.g., A sarcastic cyberpunk hacker girl who owes the player a favor..."
                                else "Например: Дерзкая девушка-хакер из киберпанк-мегаполиса, которая случайно взломала имплант игрока в баре..."
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_bot_prompt_input")
                    )

                    if (createByPromptError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = createByPromptError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (isGeneratingBotByPrompt) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                            Text(
                                text = if (isEn) "Generating character & filling fields..." else "ИИ пишет персонажа и заполняет поля...",
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
                            onClick = { showCreateByPromptDialog = false },
                            enabled = !isGeneratingBotByPrompt
                        ) {
                            Text(if (isEn) "Cancel" else "Отмена")
                        }
                        Button(
                            onClick = {
                                val wishes = createPromptInput.trim()
                                if (wishes.isBlank()) return@Button
                                val generator = onGenerateBotFromPrompt
                                if (generator == null) {
                                    createByPromptError = "Генератор ИИ недоступен"
                                    return@Button
                                }
                                isGeneratingBotByPrompt = true
                                createByPromptError = null
                                generator(
                                    wishes,
                                    { draft ->
                                        isGeneratingBotByPrompt = false
                                        val updatedFields = applyDraftToEditorFields(draft)
                                        showCreateByPromptDialog = false
                                        assistantHistory.add(
                                            EditorAssistantChatTurn(
                                                sender = "assistant",
                                                text = draft.assistantMessage,
                                                updatedFields = updatedFields
                                            )
                                        )
                                        android.widget.Toast.makeText(
                                            context,
                                            "Поля бота автоматически заполнены!",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    { err ->
                                        isGeneratingBotByPrompt = false
                                        createByPromptError = err
                                    }
                                )
                            },
                            enabled = createPromptInput.isNotBlank() && !isGeneratingBotByPrompt,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("submit_create_bot_prompt_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Generate & Fill" else "Создать и заполнить")
                        }
                    }
                }
            }
        }
    }

    // Floating Assistant Window ("🪄 Помоги!")
    if (showFloatingAssistantWindow) {
        val sendAssistantRequest: (String) -> Unit = { requestText ->
            val cleanReq = requestText.trim()
            if (cleanReq.isNotBlank() && !isFloatingAssistantLoading) {
                val assistFn = onAssistBotInEditor
                assistantHistory.add(EditorAssistantChatTurn(sender = "user", text = cleanReq))
                floatingAssistantInput = ""
                if (assistFn == null) {
                    assistantHistory.add(
                        EditorAssistantChatTurn(
                            sender = "assistant",
                            text = "ИИ-помощник сейчас недоступен. Проверьте настройки провайдера."
                        )
                    )
                } else {
                    isFloatingAssistantLoading = true
                    val payload = com.example.ai.engine.AiCreatorAssistantHelper.buildAssistBotInEditorUserPrompt(
                        name = name,
                        ageText = ageText,
                        shortDescription = shortDescription,
                        personality = personality,
                        appearance = appearance,
                        habits = habits,
                        likes = likes,
                        dislikes = dislikes,
                        greetingMessage = greetingMessage,
                        tagsText = tagsText,
                        knowledgeCards = knowledgeCards.toList(),
                        userRequest = cleanReq
                    )
                    assistFn(
                        payload,
                        { draft ->
                            isFloatingAssistantLoading = false
                            val updatedFields = applyDraftToEditorFields(draft)
                            assistantHistory.add(
                                EditorAssistantChatTurn(
                                    sender = "assistant",
                                    text = draft.assistantMessage,
                                    updatedFields = updatedFields
                                )
                            )
                        },
                        { err ->
                            isFloatingAssistantLoading = false
                            assistantHistory.add(
                                EditorAssistantChatTurn(
                                    sender = "assistant",
                                    text = "⚠️ Ошибка: $err"
                                )
                            )
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 10.dp,
                shadowElevation = 14.dp,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .offset { IntOffset(floatingOffsetX.roundToInt(), floatingOffsetY.roundToInt()) }
                    .widthIn(max = 390.dp)
                    .fillMaxWidth(0.94f)
                    .then(
                        if (isFloatingAssistantMinimized) Modifier.wrapContentHeight()
                        else Modifier.heightIn(min = 280.dp, max = 470.dp)
                    )
                    .testTag("floating_assistant_window")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Draggable Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                                    )
                                )
                            )
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    floatingOffsetX = (floatingOffsetX + dragAmount.x).coerceIn(-260f, 260f)
                                    floatingOffsetY = (floatingOffsetY + dragAmount.y).coerceIn(-520f, 220f)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "🪄 Помоги! — Ассистент",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Перетаскивай окно • Автозаполнение полей",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isFloatingAssistantMinimized = !isFloatingAssistantMinimized },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFloatingAssistantMinimized) Icons.Default.OpenInFull else Icons.Default.Minimize,
                                    contentDescription = if (isFloatingAssistantMinimized) "Развернуть" else "Свернуть",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showFloatingAssistantWindow = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (!isFloatingAssistantMinimized) {
                        // Quick suggestion chips
                        val quickPrompts = listOf(
                            "✨ Заполни пустые поля" to "Допиши и заполни все пустые или короткие поля этого бота, чтобы он стал полноценным и живым.",
                            "🎭 Расширь характер" to "Сделай характер, привычки и личность бота глубже, детальнее и интереснее.",
                            "💬 Напиши приветствие" to "Придумай атмосферное, интригующее первое сообщение (приветствие) с действиями и прямой речью.",
                            "📚 Добавь карточки лора" to "Придумай и добавь 2-3 интересные карточки лора (предыстория, мир, секреты) для этого персонажа."
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(quickPrompts) { (label, promptText) ->
                                AssistChip(
                                    onClick = { sendAssistantRequest(promptText) },
                                    enabled = !isFloatingAssistantLoading,
                                    label = { Text(label, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Chat History
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(assistantHistory) { turn ->
                                val isUser = turn.sender == "user"
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isUser) 14.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 14.dp
                                        ),
                                        color = if (isUser) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(
                                                text = turn.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (turn.updatedFields.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "✅ Обновлено: ${turn.updatedFields.joinToString(", ")}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (isFloatingAssistantLoading) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text(
                                            text = "Ассистент дописывает поля...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Input Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = floatingAssistantInput,
                                onValueChange = { floatingAssistantInput = it },
                                enabled = !isFloatingAssistantLoading,
                                placeholder = {
                                    Text(
                                        "Что дописать или изменить в боте?",
                                        fontSize = 12.sp
                                    )
                                },
                                maxLines = 3,
                                shape = RoundedCornerShape(14.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("floating_assistant_input")
                            )
                            IconButton(
                                onClick = { sendAssistantRequest(floatingAssistantInput) },
                                enabled = floatingAssistantInput.isNotBlank() && !isFloatingAssistantLoading,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (floatingAssistantInput.isNotBlank() && !isFloatingAssistantLoading)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .testTag("floating_assistant_send_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Отправить",
                                    tint = if (floatingAssistantInput.isNotBlank() && !isFloatingAssistantLoading)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Export JSON Dialog
    if (showExportJsonDialog) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        var isCopied by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showExportJsonDialog = false }) {
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
                        Text(
                            text = if (isEn) "Export Character (JSON)" else "Экспорт персонажа (JSON)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showExportJsonDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = jsonImportText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(jsonImportText))
                                isCopied = true
                            }
                        ) {
                            Icon(if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isCopied) (if (isEn) "Copied!" else "Скопировано!") else (if (isEn) "Copy" else "Копировать"), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val fileName = com.example.util.BotExportHelper.sanitizeFileName(name)
                                try {
                                    saveFileLauncher.launch("$fileName.json")
                                } catch (_: Exception) {
                                    val uri = com.example.util.BotExportHelper.saveJsonToDownloads(context, fileName, jsonImportText)
                                    if (uri != null) {
                                        android.widget.Toast.makeText(context, if (isEn) "Saved to Downloads/UntitledAI" else "Сохранено в Загрузки/UntitledAI", android.widget.Toast.LENGTH_LONG).show()
                                        showExportJsonDialog = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Save to File (.json)" else "Скачать в файл (.json)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Import JSON Dialog
    if (showImportJsonDialog) {
        Dialog(onDismissRequest = { showImportJsonDialog = false }) {
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
                        Text(
                            text = if (isEn) "Import Character (JSON)" else "Импорт персонажа (JSON)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showImportJsonDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
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
                            text = if (isEn) " or paste JSON text below " else " или вставьте текст ниже ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = jsonImportText,
                        onValueChange = {
                            jsonImportText = it
                            importErrorText = null
                        },
                        placeholder = { Text("{\n  \"name\": \"...\",\n  \"personality\": \"...\"\n}") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    if (importErrorText != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = importErrorText!!,
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
                        OutlinedButton(onClick = { showImportJsonDialog = false }) {
                            Text(if (isEn) "Cancel" else "Отмена")
                        }

                        Button(
                            onClick = {
                                val result = com.example.data.repository.CharacterSerializer.importFromJson(jsonImportText, context)
                                val imported = result.getOrNull()
                                if (imported != null) {
                                    name = imported.name
                                    avatarUri = imported.avatarUri
                                    shortDescription = imported.shortDescription
                                    personality = imported.personality
                                    appearance = imported.appearance
                                    habits = imported.habits
                                    likes = imported.likes
                                    dislikes = imported.dislikes
                                    greetingMessage = imported.greetingMessage
                                    ageText = imported.age.toString()
                                    birthDate = imported.birthDate
                                    voiceStyle = imported.voiceStyle

                                    val tagsStr = try {
                                        val arr = JSONArray(imported.tags)
                                        (0 until arr.length()).map { arr.getString(it) }.joinToString(", ")
                                    } catch (_: Exception) { "" }
                                    tagsText = tagsStr

                                    knowledgeCards.clear()
                                    try {
                                        val arr = JSONArray(imported.knowledgeCards)
                                        for (i in 0 until arr.length()) {
                                            val obj = arr.getJSONObject(i)
                                            knowledgeCards.add(obj.optString("title") to obj.optString("content"))
                                        }
                                    } catch (_: Exception) {}

                                    syncKnowledgeCardsToSaved()
                                    referenceImagesJson = imported.referenceImages

                                    if (imported.avatarFrameUri.isNotBlank()) avatarFrameUri = imported.avatarFrameUri
                                    if (imported.bannerUri.isNotBlank()) bannerUri = imported.bannerUri
                                    if (imported.greetingBannerUri.isNotBlank()) greetingBannerUri = imported.greetingBannerUri

                                    author = imported.author.ifBlank { "UntitledAI" }
                                    if (imported.communicationFormality.isNotBlank()) communicationFormality = imported.communicationFormality
                                    if (imported.startingRelationship.isNotBlank()) startingRelationship = imported.startingRelationship
                                    if (imported.customRelationship.isNotBlank()) customRelationship = imported.customRelationship
                                    relationshipBoardJson = imported.relationshipBoardJson
                                    boardState = RelationshipBoardHelper.parseBoard(
                                        json = imported.relationshipBoardJson,
                                        mainCharName = imported.name.ifBlank { "Персонаж 1" },
                                        playerName = defaultPlayerName,
                                        startingRelationship = imported.startingRelationship,
                                        customRelationship = imported.customRelationship,
                                        initialTitle = imported.initialRelationshipTitle,
                                        initialPercent = imported.initialRelationshipPercent
                                    )

                                    showImportJsonDialog = false
                                } else {
                                    importErrorText = result.exceptionOrNull()?.message ?: (if (isEn) "Failed to recognize JSON format. Verify data structure." else "Не удалось распознать формат JSON. Убедитесь в корректности данных.")
                                }
                            },
                            enabled = jsonImportText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isEn) "Apply to Form" else "Применить в форму")
                        }
                    }
                }
            }
        }
    }

    // Photo Crop / Editor Dialog
    if (showCropEditorDialog && cropImageSource.isNotBlank()) {
        PhotoCropEditorDialog(
            initialImageSource = cropImageSource,
            cropMode = cropMode,
            onDismiss = {
                isEditingGreetingBanner = false
                showCropEditorDialog = false
            },
            onConfirmCrop = { savedUri, base64 ->
                val chosenUri = savedUri.ifBlank { base64 }
                if (cropMode == CropMode.AVATAR_CIRCLE) {
                    avatarUri = chosenUri
                } else if (isEditingGreetingBanner) {
                    greetingBannerUri = chosenUri
                    isEditingGreetingBanner = false
                } else {
                    bannerUri = chosenUri
                }
                showCropEditorDialog = false
            }
        )
    }

    // Avatar Frame Adjuster Dialog
    if (showFrameAdjusterDialog && pendingFrameSource.isNotBlank()) {
        AvatarFrameAdjusterDialog(
            avatarUri = avatarUri,
            initialFrameSource = pendingFrameSource,
            initialScale = frameScaleState,
            initialOffsetX = frameOffsetXState,
            initialOffsetY = frameOffsetYState,
            onDismiss = { showFrameAdjusterDialog = false },
            onSaveFrame = { savedUri, _, scale, offX, offY ->
                avatarFrameUri = savedUri
                frameScaleState = scale
                frameOffsetXState = offX
                frameOffsetYState = offY
                showFrameAdjusterDialog = false
            }
        )
    }

    if (showFrameRepositoryDialog) {
        UserFrameRepositoryDialog(
            userAvatarUri = avatarUri,
            currentSelectedFrameUri = avatarFrameUri,
            rawFramesJson = userFramesJson,
            onDismiss = { showFrameRepositoryDialog = false },
            onSelectFrame = { selectedUri ->
                avatarFrameUri = selectedUri
                rawFrameSource = selectedUri
                showFrameRepositoryDialog = false
            },
            onSaveFramesJson = { updatedJson ->
                onSaveFramesJson?.invoke(updatedJson)
            }
        )
    }
}
