package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AiProviderProfileEntity
import com.example.data.local.entity.CharacterEntity
import org.json.JSONArray

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratePhotoDialog(
    character: CharacterEntity,
    initialPrompt: String = "",
    providerProfiles: List<AiProviderProfileEntity> = emptyList(),
    appLanguage: String = "ru",
    onDismiss: () -> Unit,
    onGenerate: (
        prompt: String,
        includeAppearance: Boolean,
        style: String,
        resolution: String,
        providerProfileId: Long?,
        engineMode: String
    ) -> Unit
) {
    val isEn = appLanguage.equals("en", ignoreCase = true)

    var scenePrompt by remember { mutableStateOf(initialPrompt) }
    var includeAppearance by remember { mutableStateOf(true) }
    var selectedStyle by remember { mutableStateOf("anime") }
    var selectedResolution by remember { mutableStateOf("832x1216") }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }

    val filteredProfiles = remember(providerProfiles) {
        providerProfiles.filter {
            it.providerType != "CIVITAI" && it.providerType != "GEMINI"
        }
    }

    // Engine / Provider selection state
    // "flux", "turbo", "openai", "profile"
    var selectedEngineMode by remember { mutableStateOf("flux") }
    var selectedProfileId by remember {
        mutableStateOf<Long?>(
            filteredProfiles.find { it.isDefaultImage }?.id
                ?: filteredProfiles.firstOrNull { it.apiKey.isNotBlank() }?.id
        )
    }

    // Parse character appearance photos (Avatar + referenceImages)
    val appearancePhotos = remember(character.avatarUri, character.referenceImages) {
        val list = mutableListOf<Pair<String, String>>()
        if (character.avatarUri.isNotBlank()) {
            list.add(Pair(character.avatarUri, if (isEn) "Main Avatar" else "Основной аватар"))
        }
        try {
            val arr = JSONArray(character.referenceImages)
            for (i in 0 until arr.length()) {
                val uri = arr.getString(i).trim()
                if (uri.isNotBlank() && list.none { it.first == uri }) {
                    list.add(Pair(uri, if (isEn) "Photo #${i + 1}" else "Фото #${i + 1}"))
                }
            }
        } catch (_: Exception) {}
        list
    }

    val quickSceneIdeas = if (isEn) listOf(
        "📸 Close-up portrait",
        "☕ Cozy coffee shop",
        "🌆 City lights at night",
        "👗 Full body portrait",
        "🏖️ Seaside beach",
        "⚔️ Action / Combat pose",
        "🛋️ Relaxing in cozy sweater",
        "🌸 Under blooming sakura",
        "✨ Elegant evening outfit"
    ) else listOf(
        "📸 Портрет крупным планом",
        "☕ В уютной кофейне",
        "🌆 В огнях вечернего города",
        "👗 Портрет в полный рост",
        "🏖️ На морском побережье",
        "⚔️ В боевой стойке",
        "🛋️ Отдых в домашней обстановке",
        "🌸 Среди цветущей сакуры",
        "✨ Элегантный вечерний образ"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isEn) "Scene Image Generation" else "Генерация изображения сцены",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (isEn) "Character" else "Персонаж"}: ${character.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Provider / Engine Selector
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEn) "Generation Provider & Engine" else "Провайдер и движок генерации",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedEngineMode == "flux",
                                    onClick = { selectedEngineMode = "flux" },
                                    leadingIcon = { Text("⚡", fontSize = 12.sp) },
                                    label = { Text("Flux Ultra (Free)", fontSize = 11.sp, fontWeight = if (selectedEngineMode == "flux") FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedEngineMode == "turbo",
                                    onClick = { selectedEngineMode = "turbo" },
                                    leadingIcon = { Text("🚀", fontSize = 12.sp) },
                                    label = { Text("SDXL Turbo", fontSize = 11.sp, fontWeight = if (selectedEngineMode == "turbo") FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedEngineMode == "openai",
                                    onClick = { selectedEngineMode = "openai" },
                                    leadingIcon = { Text("🤖", fontSize = 12.sp) },
                                    label = { Text("OpenAI DALL-E", fontSize = 11.sp, fontWeight = if (selectedEngineMode == "openai") FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            if (filteredProfiles.isNotEmpty()) {
                                item {
                                    FilterChip(
                                        selected = selectedEngineMode == "profile",
                                        onClick = { selectedEngineMode = "profile" },
                                        leadingIcon = { Text("⚙️", fontSize = 12.sp) },
                                        label = { Text(if (isEn) "My Profiles" else "Мои профили", fontSize = 11.sp, fontWeight = if (selectedEngineMode == "profile") FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // If user chose "profile", let them pick from configured profiles
                        if (selectedEngineMode == "profile" && filteredProfiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filteredProfiles) { prof ->
                                    FilterChip(
                                        selected = selectedProfileId == prof.id,
                                        onClick = { selectedProfileId = prof.id },
                                        label = { Text(prof.name, fontSize = 10.sp) },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (selectedEngineMode) {
                                "flux" -> if (isEn) "Instant high-detail generation powered by Flux (free, no API key required)" else "Высокая детализация и качество (бесплатно, без API ключа)"
                                "turbo" -> if (isEn) "Rapid generation mode for quick scene drafts" else "Быстрая генерация для мгновенных эскизов сцен"
                                "openai" -> if (isEn) "Generates via DALL-E 3 API (uses your OpenAI key or fallback)" else "Генерация через OpenAI DALL-E 3 API"
                                else -> if (isEn) "Custom provider profile selected" else "Используется выбранный профиль из раздела провайдеров"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Section 2: Character Appearance Photos & Data
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (includeAppearance) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = if (includeAppearance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEn) "Character Visual Identity" else "Внешность и фото персонажа",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (includeAppearance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = includeAppearance,
                                onCheckedChange = { includeAppearance = it },
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        if (includeAppearance) {
                            Spacer(modifier = Modifier.height(8.dp))

                            if (appearancePhotos.isNotEmpty()) {
                                Text(
                                    text = "${if (isEn) "Reference Visuals" else "Референс-фотографии"} (${appearancePhotos.size}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    itemsIndexed(appearancePhotos) { index, (uri, label) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { previewPhotoUri = uri }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(
                                                        1.5.dp,
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                        RoundedCornerShape(10.dp)
                                                    )
                                            ) {
                                                AvatarImage(
                                                    uri = uri,
                                                    contentDescription = label,
                                                    size = 56.dp,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            if (character.appearance.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = if (isEn) "Appearance Profile:" else "Описание внешности:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = character.appearance,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEn) "✨ Facial features, hairstyle, and wardrobe tags are synthesized into the generation prompt." else "✨ Черты лица, прическа и стиль одежды точно синтезируются в промпт генерации.",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = if (isEn) "Character appearance disabled. Generating only prompt scene." else "Внешность персонажа отключена. Будет сгенерирован только описанный сюжет.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Section 3: Scene Prompt Input
                Column {
                    Text(
                        text = if (isEn) "Scene, Action, or Environment:" else "Сцена, действие или окружение:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = scenePrompt,
                        onValueChange = { scenePrompt = it },
                        placeholder = {
                            Text(
                                text = if (isEn) "e.g. 'relaxing in a cozy cafe with warm lighting' or 'evening walk in the rain'" else "Например: 'отдых в уютной кофейне с чашкой чая' или 'вечерняя прогулка под дождем'",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Section 4: Quick Idea Chips
                Column {
                    Text(
                        text = if (isEn) "Quick Scene Concepts:" else "Быстрые концепции сцен:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickSceneIdeas.forEach { idea ->
                            val cleanIdea = idea.substringAfter(" ")
                            SuggestionChip(
                                onClick = {
                                    scenePrompt = if (scenePrompt.isBlank()) cleanIdea else "$scenePrompt, $cleanIdea"
                                },
                                label = { Text(idea, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Section 5: Art Style
                Column {
                    Text(
                        text = if (isEn) "Artistic Style:" else "Художественный стиль:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Pair("anime", if (isEn) "✨ Anime / 2D" else "✨ Аниме"),
                            Pair("photorealistic", if (isEn) "📷 Photorealism" else "📷 Фотореализм"),
                            Pair("cinematic", if (isEn) "🎬 Cinematic" else "🎬 Кинематограф"),
                            Pair("digital_art", if (isEn) "🎨 Digital Art" else "🎨 Цифровой арт")
                        ).forEach { (styleKey, styleLabel) ->
                            FilterChip(
                                selected = selectedStyle == styleKey,
                                onClick = { selectedStyle = styleKey },
                                label = { Text(styleLabel, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Section 6: Aspect Ratio / Resolution
                Column {
                    Text(
                        text = if (isEn) "Aspect Ratio & Resolution:" else "Формат и соотношение сторон:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            Pair("832x1216", if (isEn) "📱 Portrait (9:16)" else "📱 Портрет (9:16)"),
                            Pair("1024x1024", if (isEn) "⬛ Square (1:1)" else "⬛ Квадрат (1:1)"),
                            Pair("1216x832", if (isEn) "🖥️ Landscape (16:9)" else "🖥️ Пейзаж (16:9)")
                        ).forEach { (resKey, resLabel) ->
                            FilterChip(
                                selected = selectedResolution == resKey,
                                onClick = { selectedResolution = resKey },
                                label = { Text(resLabel, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalProfileId = if (selectedEngineMode == "profile") selectedProfileId else null
                    onGenerate(
                        scenePrompt.trim(),
                        includeAppearance,
                        selectedStyle,
                        selectedResolution,
                        finalProfileId,
                        selectedEngineMode
                    )
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isEn) "Generate Artwork" else "Сгенерировать арт")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEn) "Cancel" else "Отмена")
            }
        }
    )

    // Photo Enlarged Preview Dialog
    if (previewPhotoUri != null) {
        Dialog(onDismissRequest = { previewPhotoUri = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { previewPhotoUri = null }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                AvatarImage(
                    uri = previewPhotoUri!!,
                    contentDescription = "Референс-фото",
                    size = 280.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                )
            }
        }
    }
}
