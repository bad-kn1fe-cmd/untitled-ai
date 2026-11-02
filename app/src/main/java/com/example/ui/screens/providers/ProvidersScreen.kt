package com.example.ui.screens.providers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import coil.compose.AsyncImage
import com.example.data.local.entity.AiProviderProfileEntity
import com.example.data.model.CivitaiModelItem
import com.example.ui.studio.studioElement
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreen(
    profiles: List<AiProviderProfileEntity>,
    onSaveProfile: (AiProviderProfileEntity) -> Unit,
    onDeleteProfile: (AiProviderProfileEntity) -> Unit,
    onSetDefaultText: (Long) -> Unit,
    onSetDefaultImage: (Long) -> Unit,
    onTestConnection: suspend (AiProviderProfileEntity) -> Result<String>,
    onFetchOpenRouterModels: suspend (String) -> Result<List<String>>,
    onSearchCivitaiModels: suspend (String, String) -> Result<List<CivitaiModelItem>>,
    onFetchRemoteModels: (suspend (String, String, String) -> Result<List<String>>)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingProfile by remember { mutableStateOf<AiProviderProfileEntity?>(null) }
    var isNewProfileDialog by remember { mutableStateOf(false) }
    var showGeminiTutorial by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    val openRouterProfile = profiles.find { it.providerType == "OPENROUTER" }
    val civitaiProfile = profiles.find { it.providerType == "CIVITAI" }
    val isTextKeyMissing = profiles.none { !it.isImageProvider && (it.apiKey.isNotBlank() || it.providerType == "CUSTOM") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.studioElement("providers_top_bar"),
                title = {
                    Column {
                        Text(if (isEn) "AI Providers (BYOK)" else "AI-провайдеры (BYOK)", fontWeight = FontWeight.Bold)
                        Text("Gemini, OpenRouter, Groq, SambaNova, DeepSeek, Civitai...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isEn) "Back" else "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingProfile = AiProviderProfileEntity(
                        name = "Groq (Ultra-Fast)",
                        providerType = "GROQ",
                        baseUrl = "https://api.groq.com/openai/v1",
                        model = "llama-3.3-70b-versatile"
                    )
                    isNewProfileDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (isEn) "Add Provider" else "Добавить провайдер") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.studioElement("providers_add_btn")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick preset bar to add popular providers with 1 tap
            item {
                Column(
                    modifier = Modifier
                        .studioElement("providers_search_filter")
                        .fillMaxWidth()
                ) {
                    Text(
                        text = if (isEn) "Quick add provider:" else "Быстрое добавление провайдера:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    editingProfile = AiProviderProfileEntity(
                                        name = "Google Gemini (Text & Vision)",
                                        providerType = "GEMINI",
                                        baseUrl = "https://generativelanguage.googleapis.com",
                                        model = "gemini-3.5-flash-lite",
                                        visionModel = "gemini-3.5-flash-lite",
                                        isDefaultText = true
                                    )
                                    isNewProfileDialog = true
                                },
                                label = { Text("✨ Google Gemini", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { showGeminiTutorial = true },
                                label = { Text(if (isEn) "🎓 Tutorial: How to connect Gemini" else "🎓 Обучение: как подключить Gemini", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    labelColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    editingProfile = AiProviderProfileEntity(
                                        name = "OpenRouter",
                                        providerType = "OPENROUTER",
                                        baseUrl = "https://openrouter.ai/api/v1",
                                        model = "google/gemma-4-26b-a4b-it:free",
                                        visionModel = "google/gemma-4-26b-a4b-it:free",
                                        isDefaultText = true
                                    )
                                    isNewProfileDialog = true
                                },
                                label = { Text("🌐 OpenRouter", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    editingProfile = AiProviderProfileEntity(
                                        name = "xAI Grok",
                                        providerType = "GROK",
                                        baseUrl = "https://api.x.ai/v1",
                                        model = "grok-2-latest",
                                        visionModel = "grok-2-vision-1212"
                                    )
                                    isNewProfileDialog = true
                                },
                                label = { Text("🧠 xAI Grok", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    editingProfile = AiProviderProfileEntity(
                                        name = "Cerebras",
                                        providerType = "CEREBRAS",
                                        baseUrl = "https://api.cerebras.ai/v1",
                                        model = "gpt-oss-120b",
                                        visionModel = ""
                                    )
                                    isNewProfileDialog = true
                                },
                                label = { Text("⚡ Cerebras (120B)", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    editingProfile = AiProviderProfileEntity(
                                        name = "Groq",
                                        providerType = "GROQ",
                                        baseUrl = "https://api.groq.com/openai/v1",
                                        model = "llama-3.3-70b-versatile",
                                        visionModel = "llama-3.2-11b-vision-preview"
                                    )
                                    isNewProfileDialog = true
                                },
                                label = { Text("⚡ Groq", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    editingProfile = AiProviderProfileEntity(
                                        name = "SambaNova",
                                        providerType = "SAMBANOVA",
                                        baseUrl = "https://api.sambanova.ai/v1",
                                        model = "Meta-Llama-3.3-70B-Instruct",
                                        visionModel = "Llama-3.2-11B-Vision-Instruct"
                                    )
                                    isNewProfileDialog = true
                                },
                                label = { Text("⚡ SambaNova", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    editingProfile = AiProviderProfileEntity(
                                        name = "DeepSeek",
                                        providerType = "DEEPSEEK",
                                        baseUrl = "https://api.deepseek.com/v1",
                                        model = "deepseek-chat",
                                        visionModel = "deepseek-chat"
                                    )
                                    isNewProfileDialog = true
                                },
                                label = { Text("🧠 DeepSeek", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    editingProfile = AiProviderProfileEntity(
                                        name = "Civitai (Pony V6)",
                                        providerType = "CIVITAI",
                                        baseUrl = "https://orchestration.civitai.com/v2/consumer",
                                        model = "urn:air:sdxl:checkpoint:civitai:257201@290640",
                                        isImageProvider = true,
                                        negativePrompt = "score_4, score_5, score_6, ugly, deformed, blurry, bad anatomy",
                                        imageResolution = "832x1216"
                                    )
                                    isNewProfileDialog = true
                                },
                                label = { Text("🎨 Civitai", fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
            // Missing Text Key Alert Banner
            if (isTextKeyMissing) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEn) "Text AI Provider Not Configured" else "Не настроен текстовый AI-провайдер",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEn) "To chat with characters, enter an API key for any provider: Google Gemini, OpenRouter, Groq, SambaNova, DeepSeek, or a custom OpenAI server." else "Для общения с персонажами укажите API-ключ любого провайдера: Google Gemini, OpenRouter, Groq, SambaNova, DeepSeek или кастомного OpenAI-сервера.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val firstText = profiles.find { !it.isImageProvider }
                                        if (firstText != null) {
                                            editingProfile = firstText
                                            isNewProfileDialog = false
                                        } else {
                                            editingProfile = AiProviderProfileEntity(
                                                name = "Google Gemini (Text & Vision)",
                                                providerType = "GEMINI",
                                                baseUrl = "https://generativelanguage.googleapis.com",
                                                model = "gemini-3.5-flash-lite",
                                                visionModel = "gemini-3.5-flash-lite",
                                                isDefaultText = true
                                            )
                                            isNewProfileDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEn) "Enter API Key" else "Ввести ключ", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showGeminiTutorial = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isEn) "Gemini Tutorial" else "Туториал Gemini", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Info Card
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isEn) "BYOK (Bring Your Own Key): Your API keys are stored only locally on your device in a secure Room database. Requests are sent directly to the provider." else "BYOK (Bring Your Own Key): ваши API-ключи хранятся только локально на устройстве в защищенной базе данных Room. Запросы отправляются напрямую провайдеру.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            items(profiles, key = { it.id }) { profile ->
                ProviderProfileCard(
                    profile = profile,
                    onEdit = {
                        editingProfile = profile
                        isNewProfileDialog = false
                    },
                    onSetDefaultText = { onSetDefaultText(profile.id) },
                    onSetDefaultImage = { onSetDefaultImage(profile.id) }
                )
            }
        }
    }

    if (showGeminiTutorial) {
        com.example.ui.components.GeminiTutorialDialog(
            initialStep = 1,
            onDismiss = { showGeminiTutorial = false },
            onCompleteWithApiKey = { apiKey, selectedModel ->
                val cleanModel = selectedModel.ifBlank { "gemini-3.5-flash-lite" }
                val existingGemini = profiles.find { it.providerType.equals("GEMINI", ignoreCase = true) }
                if (existingGemini != null) {
                    val updated = existingGemini.copy(
                        apiKey = apiKey.trim(),
                        model = cleanModel,
                        visionModel = cleanModel,
                        isDefaultText = true,
                        isImageProvider = false
                    )
                    onSaveProfile(updated)
                    onSetDefaultText(existingGemini.id)
                } else {
                    onSaveProfile(
                        AiProviderProfileEntity(
                            name = "Google Gemini (Text & Vision)",
                            providerType = "GEMINI",
                            baseUrl = "https://generativelanguage.googleapis.com",
                            apiKey = apiKey.trim(),
                            model = cleanModel,
                            visionModel = cleanModel,
                            isDefaultText = true,
                            isImageProvider = false
                        )
                    )
                }
                showGeminiTutorial = false
                Toast.makeText(
                    context,
                    "✨ Провайдер Google Gemini ($cleanModel) автоматически добавлен и активирован!",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    if (editingProfile != null) {
        ProviderEditorDialog(
            profile = editingProfile!!,
            isNew = isNewProfileDialog,
            onDismiss = { editingProfile = null },
            onSave = { saved ->
                onSaveProfile(saved)
                editingProfile = null
            },
            onDelete = if (!isNewProfileDialog) {
                {
                    onDeleteProfile(editingProfile!!)
                    editingProfile = null
                }
            } else null,
            onTestConnection = onTestConnection,
            onFetchOpenRouterModels = onFetchOpenRouterModels,
            onSearchCivitaiModels = onSearchCivitaiModels,
            onFetchRemoteModels = onFetchRemoteModels
        )
    }
}

@Composable
fun ProviderProfileCard(
    profile: AiProviderProfileEntity,
    onEdit: () -> Unit,
    onSetDefaultText: () -> Unit,
    onSetDefaultImage: () -> Unit
) {
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (profile.isDefaultText || profile.isDefaultImage) 2.dp else 1.dp,
            color = if (profile.isDefaultText || profile.isDefaultImage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (profile.providerType.uppercase()) {
                            "CEREBRAS" -> Color(0xFFFF6A00).copy(alpha = 0.2f)
                            "GROK", "XAI" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            "GROQ" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            "SAMBANOVA" -> Color(0xFF8B5CF6).copy(alpha = 0.2f)
                            "DEEPSEEK" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                            "TOGETHER" -> Color(0xFF06B6D4).copy(alpha = 0.2f)
                            "MISTRAL" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                            "FIREWORKS" -> Color(0xFFDC2626).copy(alpha = 0.2f)
                            "PERPLEXITY" -> Color(0xFF14B8A6).copy(alpha = 0.2f)
                            "OPENROUTER" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            "CIVITAI" -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                            "GEMINI" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            "ANTHROPIC" -> Color(0xFFD97706).copy(alpha = 0.2f)
                            "OPENAI" -> Color(0xFF10B981).copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = profile.providerType.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (profile.providerType.uppercase()) {
                                "CEREBRAS" -> Color(0xFFEA580C)
                                "GROK", "XAI" -> MaterialTheme.colorScheme.tertiary
                                "GROQ" -> Color(0xFFD97706)
                                "SAMBANOVA" -> Color(0xFF7C3AED)
                                "DEEPSEEK" -> Color(0xFF2563EB)
                                "TOGETHER" -> Color(0xFF0891B2)
                                "MISTRAL" -> Color(0xFFDC2626)
                                "FIREWORKS" -> Color(0xFFB91C1C)
                                "PERPLEXITY" -> Color(0xFF0D9488)
                                "OPENROUTER" -> MaterialTheme.colorScheme.primary
                                "CIVITAI" -> MaterialTheme.colorScheme.tertiary
                                "GEMINI" -> MaterialTheme.colorScheme.secondary
                                "ANTHROPIC" -> Color(0xFFB45309)
                                "OPENAI" -> Color(0xFF059669)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Model info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (profile.model.isNotBlank()) profile.model else (if (isEn) "Default model" else "Модель по умолчанию"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Status key
            Row(verticalAlignment = Alignment.CenterVertically) {
                val hasKey = profile.apiKey.isNotBlank() || profile.providerType == "CUSTOM"
                Icon(
                    imageVector = if (hasKey) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (hasKey) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasKey) (if (isEn) "Key configured" else "Ключ настроен") else (if (isEn) "⚠️ Key not entered (tap to enter)" else "⚠️ Ключ не введен (нажмите для ввода)"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasKey) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    fontWeight = if (hasKey) FontWeight.Normal else FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Defaults toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!profile.isImageProvider) {
                    FilterChip(
                        selected = profile.isDefaultText,
                        onClick = onSetDefaultText,
                        label = { Text(if (isEn) "Default Chat" else "Осн. для чата", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = if (profile.isDefaultText) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }

                if (profile.isImageProvider || profile.providerType == "CIVITAI" || profile.providerType == "CUSTOM") {
                    FilterChip(
                        selected = profile.isDefaultImage,
                        onClick = onSetDefaultImage,
                        label = { Text(if (isEn) "Default Photo" else "Осн. для фото", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = if (profile.isDefaultImage) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderEditorDialog(
    profile: AiProviderProfileEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (AiProviderProfileEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onTestConnection: suspend (AiProviderProfileEntity) -> Result<String>,
    onFetchOpenRouterModels: suspend (String) -> Result<List<String>>,
    onSearchCivitaiModels: suspend (String, String) -> Result<List<CivitaiModelItem>>,
    onFetchRemoteModels: (suspend (String, String, String) -> Result<List<String>>)? = null
) {
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    var name by remember { mutableStateOf(profile.name) }
    var providerType by remember { mutableStateOf(profile.providerType) }
    var baseUrl by remember { mutableStateOf(profile.baseUrl) }
    var apiKey by remember { mutableStateOf(profile.apiKey) }
    var model by remember { mutableStateOf(profile.model) }
    var temperature by remember { mutableStateOf(profile.temperature) }
    var topP by remember { mutableStateOf(profile.topP) }
    var maxTokens by remember { mutableStateOf(profile.maxTokens.toString()) }
    var isImageProvider by remember { mutableStateOf(profile.isImageProvider || profile.providerType == "CIVITAI") }
    var visionModel by remember { mutableStateOf(profile.visionModel) }
    var negativePrompt by remember { mutableStateOf(profile.negativePrompt) }
    var imageResolution by remember { mutableStateOf(profile.imageResolution) }
    var seedMode by remember { mutableStateOf(profile.seedMode) }

    // Deep model behavior & JSON customization states
    var thinkingMode by remember { mutableStateOf(profile.thinkingMode) }
    var thinkingBudget by remember { mutableStateOf(if (profile.thinkingBudget > 0) profile.thinkingBudget.toString() else "") }
    var topK by remember { mutableStateOf(if (profile.topK > 0) profile.topK.toString() else "") }
    var frequencyPenalty by remember { mutableFloatStateOf(profile.frequencyPenalty) }
    var presencePenalty by remember { mutableFloatStateOf(profile.presencePenalty) }
    var repetitionPenalty by remember { mutableFloatStateOf(profile.repetitionPenalty) }
    var minP by remember { mutableFloatStateOf(profile.minP) }
    var seedText by remember { mutableStateOf(profile.seed?.toString() ?: "") }
    var stopSequences by remember { mutableStateOf(profile.stopSequences) }
    var responseFormatMode by remember { mutableStateOf(profile.responseFormatMode) }
    var extraHeadersJson by remember { mutableStateOf(profile.extraHeadersJson) }
    var extraBodyJson by remember { mutableStateOf(profile.extraBodyJson) }
    var rawJsonTemplate by remember { mutableStateOf(profile.rawJsonTemplate) }

    // Customizable test behavior states
    var testPrompt by remember { mutableStateOf(profile.testPrompt) }
    var testSystemPrompt by remember { mutableStateOf(profile.testSystemPrompt) }
    var testMaxTokens by remember { mutableStateOf(profile.testMaxTokens.toString()) }
    var testStream by remember { mutableStateOf(profile.testStream) }
    var testCustomBodyJson by remember { mutableStateOf(profile.testCustomBodyJson) }

    var showDeepJsonSection by remember { mutableStateOf(false) }
    var showTestConfigSection by remember { mutableStateOf(false) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingModels by remember { mutableStateOf(false) }

    var showCivitaiPicker by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    fun buildCurrentCandidate(): AiProviderProfileEntity {
        return profile.copy(
            name = name.trim(),
            providerType = providerType,
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            model = model.trim(),
            visionModel = visionModel.trim(),
            temperature = temperature,
            topP = topP,
            maxTokens = (maxTokens.toIntOrNull() ?: 1500).coerceIn(16, 128000),
            isImageProvider = isImageProvider || providerType == "CIVITAI",
            negativePrompt = negativePrompt.trim(),
            imageResolution = imageResolution,
            seedMode = seedMode,
            thinkingMode = thinkingMode,
            thinkingBudget = thinkingBudget.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            topK = topK.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            frequencyPenalty = frequencyPenalty,
            presencePenalty = presencePenalty,
            repetitionPenalty = repetitionPenalty,
            minP = minP,
            seed = seedText.toIntOrNull(),
            stopSequences = stopSequences.trim(),
            responseFormatMode = responseFormatMode,
            extraHeadersJson = extraHeadersJson.trim().ifBlank { "{}" },
            extraBodyJson = extraBodyJson.trim().ifBlank { "{}" },
            rawJsonTemplate = rawJsonTemplate.trim(),
            testPrompt = testPrompt.trim().ifBlank { "Привет! Ответь одним коротким предложением, что связь работает." },
            testSystemPrompt = testSystemPrompt.trim(),
            testMaxTokens = (testMaxTokens.toIntOrNull() ?: 128).coerceIn(1, 32768),
            testStream = testStream,
            testCustomBodyJson = testCustomBodyJson.trim()
        )
    }

    val providerTypes = listOf(
        "CEREBRAS" to "Cerebras (⚡ 120B / Ultra-Fast)",
        "GROK" to "xAI Grok (🧠 Grok-2 / Grok-3)",
        "GROQ" to "Groq (⚡ Ultra-Fast)",
        "SAMBANOVA" to "SambaNova (⚡ 405B/70B)",
        "DEEPSEEK" to "DeepSeek (🧠 V3 / R1)",
        "TOGETHER" to "Together AI (🚀 Open Source)",
        "MISTRAL" to "Mistral AI (🇪🇺 Le Plateforme)",
        "FIREWORKS" to "Fireworks AI (⚡ Inference)",
        "PERPLEXITY" to "Perplexity AI (🔍 Search)",
        "OPENROUTER" to "OpenRouter (🌐 Мульти-шлюз)",
        "CIVITAI" to "Civitai (🎨 Генерация фото)",
        "OPENAI" to "OpenAI (ChatGPT)",
        "ANTHROPIC" to "Anthropic Claude",
        "GEMINI" to "Google Gemini",
        "CUSTOM" to "Локальный SD WebUI"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNew) (if (isEn) "New AI Provider" else "Новый AI-провайдер") else (if (isEn) "Provider Settings" else "Настройка провайдера"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(if (isEn) "Provider Type" else "Тип провайдера", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                            items(providerTypes) { (type, label) ->
                                FilterChip(
                                    selected = providerType == type,
                                    onClick = {
                                        providerType = type
                                        when (type) {
                                            "CEREBRAS" -> {
                                                if (name.isBlank() || name == "Новый провайдер" || name == "New Provider") name = "Cerebras"
                                                baseUrl = "https://api.cerebras.ai/v1"
                                                model = "gpt-oss-120b"
                                                visionModel = ""
                                                isImageProvider = false
                                            }
                                            "GROK" -> {
                                                if (name.isBlank() || name == "Новый провайдер" || name == "New Provider") name = "xAI Grok"
                                                baseUrl = "https://api.x.ai/v1"
                                                model = "grok-2-latest"
                                                visionModel = "grok-2-vision-1212"
                                                isImageProvider = false
                                            }
                                            "OPENROUTER" -> {
                                                if (name.isBlank() || name == "Новый провайдер" || name == "New Provider") name = "OpenRouter"
                                                baseUrl = "https://openrouter.ai/api/v1"
                                                model = "google/gemma-4-26b-a4b-it:free"
                                                visionModel = "google/gemma-4-26b-a4b-it:free"
                                                isImageProvider = false
                                            }
                                            "CIVITAI" -> {
                                                if (name.isBlank() || name == "Новый провайдер" || name == "New Provider") name = "Civitai (Pony V6)"
                                                baseUrl = "https://orchestration.civitai.com/v2/consumer"
                                                model = "urn:air:sdxl:checkpoint:civitai:257201@290640"
                                                isImageProvider = true
                                                negativePrompt = "score_4, score_5, score_6, ugly, deformed, blurry, bad anatomy"
                                                imageResolution = "832x1216"
                                            }
                                            "OPENAI" -> {
                                                if (name.isBlank() || name == "Новый провайдер" || name == "New Provider") name = "OpenAI"
                                                baseUrl = "https://api.openai.com/v1"
                                                model = "gpt-4o-mini"
                                            }
                                            "ANTHROPIC" -> {
                                                if (name.isBlank() || name == "Новый провайдер" || name == "New Provider") name = "Claude"
                                                baseUrl = "https://api.anthropic.com/v1"
                                                model = "claude-3-5-haiku-20241022"
                                            }
                                            "GEMINI" -> {
                                                if (name.isBlank() || name == "Новый провайдер" || name == "New Provider") name = "Google Gemini"
                                                baseUrl = "https://generativelanguage.googleapis.com"
                                                model = "gemini-3.5-flash-lite"
                                                visionModel = "gemini-3.5-flash-lite"
                                                isImageProvider = false
                                            }
                                            "CUSTOM" -> {
                                                if (name.isBlank() || name == "Новый провайдер" || name == "New Provider") name = "SD WebUI"
                                                baseUrl = "http://192.168.1.100:7860/sdapi/v1"
                                                isImageProvider = true
                                            }
                                        }
                                    },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(if (isEn) "Profile Name" else "Название профиля") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text(if (isEn) "Base Endpoint URL" else "Базовый URL эндпоинта") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    item {
                        Column {
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text(if (providerType == "CIVITAI") (if (isEn) "Civitai API Token *" else "Civitai API Токен *") else (if (isEn) "API Key *" else "API Ключ *")) },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Quick Link to get API Key
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        val url = when (providerType) {
                                            "CEREBRAS" -> "https://cloud.cerebras.ai/"
                                            "GROK" -> "https://console.x.ai/"
                                            "OPENROUTER" -> "https://openrouter.ai/keys"
                                            "CIVITAI" -> "https://civitai.com/user/account"
                                            "GEMINI" -> "https://aistudio.google.com/app/apikey"
                                            "ANTHROPIC" -> "https://console.anthropic.com/settings/keys"
                                            else -> "https://platform.openai.com/api-keys"
                                        }
                                        try { uriHandler.openUri(url) } catch (_: Exception) {}
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (providerType) {
                                            "CEREBRAS" -> if (isEn) "Get key on cloud.cerebras.ai" else "Получить ключ на cloud.cerebras.ai"
                                            "GROK" -> if (isEn) "Get key on console.x.ai" else "Получить ключ на console.x.ai"
                                            "OPENROUTER" -> if (isEn) "Get key on OpenRouter" else "Получить ключ на OpenRouter"
                                            "CIVITAI" -> if (isEn) "Get token on Civitai" else "Получить токен на Civitai"
                                            else -> if (isEn) "Get key on website" else "Получить ключ на сайте"
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            if (providerType == "GROK") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "💡 Grok (xAI API): что означает 403 Forbidden",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "У Grok есть бесплатный лимит, поэтому 403 не обязательно означает отсутствие кредитов. Возможные причины: региональные ограничения (требуется IP поддерживаемой страны), исчерпание квоты бесплатного тарифа, доступ к конкретной модели для ключа или верификация аккаунта на console.x.ai. Подписка X Premium в приложении Twitter не включает доступ к API xAI.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = model,
                                    onValueChange = { model = it },
                                    label = { Text(if (providerType == "CIVITAI") (if (isEn) "Civitai Model (AIR / ID)" else "Модель Civitai (AIR / ID)") else (if (isEn) "Model Identifier" else "Имя модели")) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                if (providerType == "OPENROUTER" || providerType == "GROK" || providerType == "GROQ" || providerType == "CEREBRAS") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isLoadingModels = true
                                                val res = onFetchRemoteModels?.invoke(providerType, baseUrl, apiKey)
                                                    ?: if (providerType == "OPENROUTER") onFetchOpenRouterModels(apiKey) else Result.success(emptyList())
                                                availableModels = res.getOrDefault(emptyList())
                                                isLoadingModels = false
                                            }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        if (isLoadingModels) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                        } else {
                                            Icon(Icons.Default.CloudDownload, contentDescription = "Загрузить модели")
                                        }
                                    }
                                }

                                if (providerType == "CIVITAI") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { showCivitaiPicker = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = "Выбрать модель на Civitai", tint = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }

                            if (providerType == "CEREBRAS") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Рекомендуемые модели Cerebras:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    item {
                                        AssistChip(
                                            onClick = { model = "gpt-oss-120b" },
                                            label = { Text("gpt-oss-120b (Стандартная)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "llama-3.3-70b" },
                                            label = { Text("llama-3.3-70b", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "llama3.1-8b" },
                                            label = { Text("llama3.1-8b (Быстрая)", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "qwen-2.5-coder-32b" },
                                            label = { Text("qwen-2.5-coder-32b", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }

                            if (providerType == "GROK") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Рекомендуемые модели Grok:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    item {
                                        AssistChip(
                                            onClick = { model = "grok-2-latest" },
                                            label = { Text("grok-2-latest (Реком.)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "grok-3" },
                                            label = { Text("grok-3", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "grok-3-mini" },
                                            label = { Text("grok-3-mini (Быстрая)", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "grok-2-vision-1212" },
                                            label = { Text("grok-2-vision", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "grok-beta" },
                                            label = { Text("grok-beta", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }

                            if (providerType == "OPENROUTER") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (isEn) "Recommended models (auto-switch on 429):" else "Рекомендуемые модели (с авто-переключением при 429):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    item {
                                        AssistChip(
                                            onClick = { model = "google/gemma-4-26b-a4b-it:free" },
                                            label = { Text(if (isEn) "gemma-4-26b:free (Main)" else "gemma-4-26b:free (Осн)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "google/gemma-4-31b-it:free" },
                                            label = { Text("gemma-4-31b:free", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "z-ai/glm-5.2:free" },
                                            label = { Text("glm-5.2:free", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "openai/gpt-oss-2" },
                                            label = { Text("gpt-oss-2", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }

                            if (providerType == "GEMINI") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (isEn) "Recommended Gemini models:" else "Рекомендуемые модели Gemini:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    item {
                                        AssistChip(
                                            onClick = {
                                                model = "gemini-3.5-flash-lite"
                                                visionModel = "gemini-3.5-flash-lite"
                                            },
                                            label = { Text(if (isEn) "gemini-3.5-flash-lite (Default)" else "gemini-3.5-flash-lite (По умолчанию)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "gemini-2.5-flash" },
                                            label = { Text("gemini-2.5-flash", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "gemini-2.5-pro" },
                                            label = { Text(if (isEn) "gemini-2.5-pro (Smart)" else "gemini-2.5-pro (Умная)", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { model = "gemini-2.0-flash" },
                                            label = { Text("gemini-2.0-flash", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }

                            if (providerType == "CIVITAI") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AssistChip(
                                        onClick = {
                                            model = "urn:air:sdxl:checkpoint:civitai:257201@290640"
                                        },
                                        label = { Text(if (isEn) "Default: Pony Diffusion V6" else "По умолчанию: Pony Diffusion V6", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    TextButton(onClick = { showCivitaiPicker = true }) {
                                        Text(if (isEn) "Civitai Models Catalog" else "Каталог моделей Civitai", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (availableModels.isNotEmpty() && (providerType == "OPENROUTER" || providerType == "GROK" || providerType == "GROQ" || providerType == "CEREBRAS")) {
                        item {
                            Text(if (isEn) "Available models (${availableModels.size}):" else "Доступные модели (${availableModels.size}):", style = MaterialTheme.typography.labelSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                items(availableModels.take(25)) { m ->
                                    AssistChip(
                                        onClick = { model = m },
                                        label = { Text(m.substringAfter("/"), fontSize = 10.sp) },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Vision Model (Multimodal photo recognition)
                    if (providerType == "OPENROUTER" || providerType == "OPENAI" || providerType == "GEMINI" || providerType == "GROK") {
                        item {
                            Column {
                                Text(if (isEn) "📷 Second Model: Vision (user photo analysis)" else "📷 Вторая модель: Vision (анализ фото пользователя)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = visionModel,
                                    onValueChange = { visionModel = it },
                                    label = { Text(if (isEn) "Vision Model (e.g. gemini-2.0-flash)" else "Vision Модель (например gemini-2.0-flash)") },
                                    placeholder = { Text("google/gemini-2.0-flash-001") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(if (isEn) "Recommended Vision models:" else "Рекомендуемые Vision модели:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                    item {
                                        AssistChip(
                                            onClick = { visionModel = "google/gemini-2.0-flash-001" },
                                            label = { Text(if (isEn) "Gemini 2.0 Flash (Fast)" else "Gemini 2.0 Flash (Быстрая)", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { visionModel = "meta-llama/llama-3.2-11b-vision-instruct:free" },
                                            label = { Text("Llama 3.2 Vision (Free)", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = { visionModel = "qwen/qwen-2-vl-72b-instruct" },
                                            label = { Text("Qwen2-VL 72B", fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Image generation specific settings
                    if (providerType == "CIVITAI" || isImageProvider) {
                        item {
                            Text(if (isEn) "Image Generation Settings" else "Настройки генерации изображений", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = negativePrompt,
                                onValueChange = { negativePrompt = it },
                                label = { Text(if (isEn) "Negative Prompt" else "Негативный промпт (Negative Prompt)") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        item {
                            Text(if (isEn) "Resolution:" else "Разрешение (Resolution):", style = MaterialTheme.typography.bodySmall)
                            val resolutions = listOf("832x1216", "1024x1024", "768x1024", "1216x832", "512x512")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                                items(resolutions) { res ->
                                    FilterChip(
                                        selected = imageResolution == res,
                                        onClick = { imageResolution = res },
                                        label = { Text(res, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Text generation parameters
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (isEn) "Temperature (creativity):" else "Температура (креативность):", style = MaterialTheme.typography.bodySmall)
                                    Text(String.format("%.2f", temperature), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = temperature,
                                    onValueChange = { temperature = it },
                                    valueRange = 0.1f..1.5f,
                                    steps = 27
                                )
                            }
                        }

                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (isEn) "Top-P sampling:" else "Тор-P выборка:", style = MaterialTheme.typography.bodySmall)
                                    Text(String.format("%.2f", topP), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                Slider(
                                    value = topP,
                                    onValueChange = { topP = it },
                                    valueRange = 0.1f..1.0f,
                                    steps = 17
                                )
                            }
                        }

                        // Max Tokens directly editable per provider!
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (isEn) "Max response tokens limit (max_tokens):" else "Лимит токенов ответа (max_tokens):", style = MaterialTheme.typography.bodySmall)
                                    OutlinedTextField(
                                        value = maxTokens,
                                        onValueChange = { maxTokens = it.filter { ch -> ch.isDigit() } },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(110.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Slider(
                                    value = (maxTokens.toFloatOrNull() ?: 1500f).coerceIn(64f, 16384f),
                                    onValueChange = { maxTokens = it.toInt().toString() },
                                    valueRange = 64f..16384f
                                )
                            }
                        }

                        // THINKING / REASONING MODE CONTROL (Can be DISABLED!)
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isEn) "🧠 Reasoning Mode (Thinking / Reasoning)" else "🧠 Режим размышлений (Thinking / Reasoning)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (thinkingMode == "disabled") {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isEn) "OFF" else "ВЫКЛ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = if (isEn) "Allows forcibly TURNING OFF reasoning mode (Gemini 2.5 thinkingBudget=0, OpenRouter reasoning.exclude=true, Qwen/GLM enable_thinking=false, Claude thinking=disabled + cleaning <think> tags)." else "Позволяет принудительно ВЫКЛЮЧИТЬ режим размышлений (Gemini 2.5 thinkingBudget=0, OpenRouter reasoning.exclude=true, Qwen/GLM enable_thinking=false, Claude thinking=disabled + очистка <think> тегов).",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    val thinkingModes = listOf(
                                        "auto" to (if (isEn) "Auto (Default)" else "Авто (По умолч.)"),
                                        "disabled" to (if (isEn) "🚫 DISABLE Thinking" else "🚫 ВЫКЛЮЧИТЬ Thinking"),
                                        "low" to (if (isEn) "Low" else "Низкий (Low)"),
                                        "medium" to (if (isEn) "Medium" else "Средний (Medium)"),
                                        "high" to (if (isEn) "High" else "Глубокий (High)")
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        thinkingModes.forEach { (modeKey, modeLabel) ->
                                            FilterChip(
                                                selected = thinkingMode == modeKey,
                                                onClick = { thinkingMode = modeKey },
                                                label = {
                                                    Text(
                                                        modeLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (thinkingMode == modeKey) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    if (thinkingMode != "disabled") {
                                        OutlinedTextField(
                                            value = thinkingBudget,
                                            onValueChange = { thinkingBudget = it.filter { ch -> ch.isDigit() } },
                                            label = { Text(if (isEn) "Thinking token budget (optional, 0 = auto)" else "Бюджет токенов размышлений (опционально, 0 = авто)") },
                                            placeholder = { Text("1024 / 4096") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // DEEP JSON & SAMPLING CUSTOMIZATION ("Почти каждая строчка в JSON")
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showDeepJsonSection = !showDeepJsonSection },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isEn) "⚙️ Deep model tuning and raw JSON modification" else "⚙️ Глубокая настройка модели и каждой строчки JSON",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (isEn) "Penalties, Top-K, Min-P, Stop words, headers, and raw JSON editing" else "Штрафы, Top-K, Min-P, Stop-слова, заголовки и прямое редактирование JSON",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = if (showDeepJsonSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null
                                        )
                                    }

                                    AnimatedVisibility(visible = showDeepJsonSection) {
                                        Column(
                                            modifier = Modifier.padding(top = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = topK,
                                                    onValueChange = { topK = it.filter { ch -> ch.isDigit() } },
                                                    label = { Text(if (isEn) "Top-K (0=auto)" else "Top-K (0=авто)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                OutlinedTextField(
                                                    value = seedText,
                                                    onValueChange = { seedText = it.filter { ch -> ch.isDigit() || ch == '-' } },
                                                    label = { Text(if (isEn) "Seed (empty=random)" else "Seed (пусто=случ.)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }

                                            Column {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Frequency Penalty:", style = MaterialTheme.typography.labelSmall)
                                                    Text(String.format("%.2f", frequencyPenalty), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                                Slider(
                                                    value = frequencyPenalty,
                                                    onValueChange = { frequencyPenalty = it },
                                                    valueRange = -2.0f..2.0f
                                                )
                                            }

                                            Column {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Presence Penalty:", style = MaterialTheme.typography.labelSmall)
                                                    Text(String.format("%.2f", presencePenalty), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                                Slider(
                                                    value = presencePenalty,
                                                    onValueChange = { presencePenalty = it },
                                                    valueRange = -2.0f..2.0f
                                                )
                                            }

                                            Column {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(if (isEn) "Repetition Penalty (1.00 = off):" else "Repetition Penalty (1.00 = выкл):", style = MaterialTheme.typography.labelSmall)
                                                    Text(String.format("%.2f", repetitionPenalty), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                                Slider(
                                                    value = repetitionPenalty,
                                                    onValueChange = { repetitionPenalty = it },
                                                    valueRange = 0.8f..1.5f
                                                )
                                            }

                                            Column {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text(if (isEn) "Min-P (0.00 = off):" else "Min-P (0.00 = выкл):", style = MaterialTheme.typography.labelSmall)
                                                    Text(String.format("%.2f", minP), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                                Slider(
                                                    value = minP,
                                                    onValueChange = { minP = it },
                                                    valueRange = 0.0f..0.5f
                                                )
                                            }

                                            OutlinedTextField(
                                                value = stopSequences,
                                                onValueChange = { stopSequences = it },
                                                label = { Text(if (isEn) "Stop Sequences (comma separated)" else "Stop-последовательности (через запятую)") },
                                                placeholder = { Text("User:, Player:, <|eot_id|>") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )

                                            Text(if (isEn) "Response format (response_format):" else "Формат ответа (response_format):", style = MaterialTheme.typography.labelSmall)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf(
                                                    "default" to (if (isEn) "Standard" else "Стандартный"),
                                                    "text" to "text",
                                                    "json_object" to "json_object"
                                                ).forEach { (fmtKey, fmtLabel) ->
                                                    FilterChip(
                                                        selected = responseFormatMode == fmtKey,
                                                        onClick = { responseFormatMode = fmtKey },
                                                        label = { Text(fmtLabel, fontSize = 11.sp) }
                                                    )
                                                }
                                            }

                                            OutlinedTextField(
                                                value = extraHeadersJson,
                                                onValueChange = { extraHeadersJson = it },
                                                label = { Text(if (isEn) "Extra HTTP Headers (JSON)" else "Дополнительные HTTP-заголовки (JSON)") },
                                                placeholder = { Text("{\"X-Custom-Header\": \"value\"}") },
                                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                minLines = 2,
                                                maxLines = 5,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )

                                            OutlinedTextField(
                                                value = extraBodyJson,
                                                onValueChange = { extraBodyJson = it },
                                                label = { Text(if (isEn) "Override / Add JSON fields (extraBodyJson)" else "Переопределение/добавление полей JSON (extraBodyJson)") },
                                                placeholder = { Text("{\"provider\": {\"allow_fallbacks\": true}, \"top_a\": 0.2}") },
                                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                minLines = 3,
                                                maxLines = 8,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Text(
                                                text = if (isEn) "💡 Tip: Any field in extraBodyJson is merged into request. To remove a field from JSON (e.g. \"top_p\"), set {\"top_p\": null}." else "💡 Совет: любое поле в extraBodyJson сливается с запросом. Чтобы удалить стандартное поле из JSON (например \"top_p\"), укажите {\"top_p\": null}.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            OutlinedTextField(
                                                value = rawJsonTemplate,
                                                onValueChange = { rawJsonTemplate = it },
                                                label = { Text(if (isEn) "Full raw JSON template (line-by-line edit)" else "Полный JSON-шаблон запроса (построчное изменение всего JSON)") },
                                                placeholder = { Text(if (isEn) "Leave blank for auto-build or tap 'Load full JSON into template' below" else "Оставьте пустым для авто-сборки или нажмите «Загрузить текущий JSON в шаблон» ниже") },
                                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                                minLines = 4,
                                                maxLines = 12,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        val candidate = buildCurrentCandidate().copy(rawJsonTemplate = "")
                                                        rawJsonTemplate = com.example.ai.engine.AiProviderEngine.buildLiveRequestPreview(candidate, forTest = false)
                                                    }
                                                ) {
                                                    Text(if (isEn) "Load full JSON into template" else "Загрузить весь JSON в шаблон", fontSize = 11.sp)
                                                }
                                                if (rawJsonTemplate.isNotBlank()) {
                                                    TextButton(onClick = { rawJsonTemplate = "" }) {
                                                        Text(if (isEn) "Reset template" else "Сбросить шаблон", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }

                                            // Live preview of the exact JSON that will be sent
                                            val livePreview = remember(
                                                providerType, model, temperature, topP, maxTokens,
                                                thinkingMode, thinkingBudget, topK, frequencyPenalty,
                                                presencePenalty, repetitionPenalty, minP, seedText,
                                                stopSequences, responseFormatMode, extraBodyJson, rawJsonTemplate
                                            ) {
                                                com.example.ai.engine.AiProviderEngine.buildLiveRequestPreview(
                                                    buildCurrentCandidate(),
                                                    forTest = false
                                                )
                                            }

                                            Text(
                                                text = if (isEn) "Live preview of final request JSON payload:" else "Предпросмотр итогового JSON запроса к провайдеру:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Surface(
                                                color = Color(0xFF0A0B0E),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = livePreview,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF80CBC4),
                                                    modifier = Modifier.padding(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Customizable Provider Connection Test Section + Button
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showTestConfigSection = !showTestConfigSection },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isEn) "🧪 Provider test behavior setup (token limit, prompt, JSON)" else "🧪 Настройка поведения теста провайдера (лимит токенов, промпт, JSON)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (showTestConfigSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null
                                        )
                                    }

                                    AnimatedVisibility(visible = showTestConfigSection) {
                                        Column(
                                            modifier = Modifier.padding(top = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = testPrompt,
                                                onValueChange = { testPrompt = it },
                                                label = { Text(if (isEn) "Test User Prompt" else "Тестовый запрос (User Prompt)") },
                                                minLines = 1,
                                                maxLines = 4,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            OutlinedTextField(
                                                value = testSystemPrompt,
                                                onValueChange = { testSystemPrompt = it },
                                                label = { Text(if (isEn) "Test System Prompt" else "Системный промпт теста (System Prompt)") },
                                                minLines = 1,
                                                maxLines = 3,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = testMaxTokens,
                                                    onValueChange = { testMaxTokens = it.filter { ch -> ch.isDigit() } },
                                                    label = { Text(if (isEn) "Test max tokens limit" else "Лимит токенов теста") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(
                                                        checked = testStream,
                                                        onCheckedChange = { testStream = it }
                                                    )
                                                    Text("Stream (SSE)", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            OutlinedTextField(
                                                value = testCustomBodyJson,
                                                onValueChange = { testCustomBodyJson = it },
                                                label = { Text(if (isEn) "Custom test JSON payload (optional)" else "Кастомный JSON специально для теста (опционально)") },
                                                placeholder = { Text("{\"temperature\": 0.1, \"max_tokens\": 32}") },
                                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                                minLines = 2,
                                                maxLines = 6,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isTesting = true
                                        testResultText = null
                                        val candidate = buildCurrentCandidate()
                                        val result = onTestConnection(candidate)
                                        testResultText = if (result.isSuccess) {
                                            result.getOrNull()
                                        } else {
                                            result.exceptionOrNull()?.message ?: (if (isEn) "Connection error" else "Ошибка соединения")
                                        }
                                        isTesting = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEn) "Testing connection..." else "Проверка соединения...", color = MaterialTheme.colorScheme.onSurface)
                                } else {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEn) "Test Connection" else "Тест подключения", color = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            if (testResultText != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                TestConnectionResultView(
                                    rawText = testResultText!!,
                                    onDismiss = { testResultText = null }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Bottom actions anchored at the bottom with solid Surface background
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onDelete != null) {
                            TextButton(onClick = onDelete) {
                                Text(if (isEn) "Delete" else "Удалить", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onDismiss) {
                                Text(if (isEn) "Cancel" else "Отмена")
                            }
                            Button(
                                onClick = {
                                    onSave(buildCurrentCandidate())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(if (isEn) "Save" else "Сохранить", maxLines = 1, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showCivitaiPicker) {
                    CivitaiModelPickerDialog(
                        apiKey = apiKey,
                        onDismiss = { showCivitaiPicker = false },
                        onSelectModel = { selectedAir ->
                            model = selectedAir
                            showCivitaiPicker = false
                        },
                        onSearch = onSearchCivitaiModels
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivitaiModelPickerDialog(
    apiKey: String,
    onDismiss: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSearch: suspend (String, String) -> Result<List<CivitaiModelItem>>
) {
    var searchQuery by remember { mutableStateOf("") }
    var modelsList by remember { mutableStateOf<List<CivitaiModelItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isSearching = true
        val res = onSearch("Pony", apiKey)
        modelsList = res.getOrDefault(emptyList())
        isSearching = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(if (isEn) "Civitai Models" else "Модели Civitai", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (isEn) "SDXL Checkpoints" else "SDXL Чекпоинты", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isEn) "Search: Pony, Anime, Realistic..." else "Поиск: Pony, Anime, Realistic...") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                isSearching = true
                                val res = onSearch(searchQuery, apiKey)
                                modelsList = res.getOrDefault(emptyList())
                                isSearching = false
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = if (isEn) "Search" else "Поиск")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isSearching) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(modelsList) { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (item.description.isNotBlank()) {
                                        Text(
                                            text = item.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Versions chips
                                    item.versions.forEach { version ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clickable { onSelectModel(version.air) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = version.name,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        text = "${version.baseModel} • ${version.air}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Button(
                                                    onClick = { onSelectModel(version.air) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(if (isEn) "Select" else "Выбрать", fontSize = 11.sp)
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

@Composable
fun TestConnectionResultView(
    rawText: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    val isSuccess = rawText.contains("--- RESULT_STATUS: SUCCESS ---")

    fun extractBetween(startTag: String, endTag: String): String? {
        val sIdx = rawText.indexOf(startTag)
        if (sIdx == -1) return null
        val afterStart = sIdx + startTag.length
        val eIdx = rawText.indexOf(endTag, afterStart)
        if (eIdx == -1) return null
        return rawText.substring(afterStart, eIdx).trim()
    }

    fun extractLine(tag: String): String? {
        val sIdx = rawText.indexOf(tag)
        if (sIdx == -1) return null
        val afterStart = sIdx + tag.length
        val lineEnd = rawText.indexOf("\n", afterStart)
        val value = if (lineEnd == -1) rawText.substring(afterStart) else rawText.substring(afterStart, lineEnd)
        return value.trimEnd('-').trim()
    }

    val status = extractLine("--- STATUS: ") ?: if (isSuccess) (if (isEn) "Connection successful (HTTP 200)" else "Подключение успешно (HTTP 200)") else (if (isEn) "Connection error" else "Ошибка соединения")
    val durationMs = extractLine("--- DURATION_MS: ")
    val endpoint = extractLine("--- ENDPOINT: ")
    val model = extractLine("--- MODEL: ")
    val summary = extractLine("--- SUMMARY: ") ?: ""
    val advice = extractBetween("--- ADVICE_START ---", "--- ADVICE_END ---")
    val requestJson = extractBetween("--- REQUEST_JSON_START ---", "--- REQUEST_JSON_END ---")
    val responseJson = extractBetween("--- RESPONSE_JSON_START ---", "--- RESPONSE_JSON_END ---")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, if (isSuccess) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSuccess) (if (isEn) "Connection successful!" else "Подключение успешно!") else (if (isEn) "Connection error" else "Ошибка подключения"),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildString {
                    append(status)
                    if (!durationMs.isNullOrBlank()) append(" • ${durationMs} ms")
                    if (!model.isNullOrBlank()) append(" • $model")
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )

            if (summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!endpoint.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "URL: $endpoint",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!advice.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 $advice",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Quick Copy All Button
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(rawText))
                        Toast.makeText(context, if (isEn) "Full test log copied to clipboard" else "Весь лог теста скопирован в буфер", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEn) "Copy All" else "Скопировать всё", fontSize = 11.sp)
                }
            }

            // REQUEST JSON SECTION
            if (!requestJson.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEn) "Sent JSON (Request Body):" else "Отправленный JSON (Request Body):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(requestJson))
                            Toast.makeText(context, if (isEn) "Sent JSON copied" else "Отправленный JSON скопирован", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(if (isEn) "Copy" else "Копировать", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181825), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = requestJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = Color(0xFFA6E3A1)
                    )
                }
            }

            // RESPONSE JSON SECTION
            if (!responseJson.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isSuccess) Color(0xFF89B4FA) else MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEn) "Received JSON (Response Body):" else "Полученный JSON (Response Body):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSuccess) Color(0xFF89B4FA) else MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(responseJson))
                            Toast.makeText(context, if (isEn) "Received JSON copied" else "Полученный JSON скопирован", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text(if (isEn) "Copy" else "Копировать", fontSize = 10.sp)
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181825), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = responseJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = if (isSuccess) Color(0xFF89DCEB) else Color(0xFFF38BA8)
                    )
                }
            }

            // Fallback if raw text couldn't be parsed with tokens
            if (requestJson.isNullOrBlank() && responseJson.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181825), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = rawText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

