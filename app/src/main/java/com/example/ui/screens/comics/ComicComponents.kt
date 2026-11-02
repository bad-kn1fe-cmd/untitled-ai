package com.example.ui.screens.comics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ai.engine.ComicPanelData
import com.example.data.local.entity.AiProviderProfileEntity
import com.example.data.local.entity.ComicEntity
import com.example.data.local.entity.SummaryEntity
import org.json.JSONArray

@Composable
fun ComicViewerDialog(
    comic: ComicEntity,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val panels = remember(comic.panelsJson) {
        try {
            val arr = JSONArray(comic.panelsJson)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val pIdx = if (obj.has("panelIndex")) obj.optInt("panelIndex", i + 1) else obj.optInt("panelNumber", i + 1)
                val speaker = if (obj.has("speakerName")) obj.optString("speakerName", "") else obj.optString("speaker", "")
                val dialogue = if (obj.has("dialogueText")) obj.optString("dialogueText", "") else obj.optString("dialogue", "")
                val img = if (obj.has("imageUri")) obj.optString("imageUri", "") else obj.optString("imageUrl", "")
                val scene = if (obj.has("sceneDescription")) obj.optString("sceneDescription", "") else obj.optString("caption", "")

                ComicPanelData(
                    panelIndex = pIdx,
                    speakerName = speaker,
                    dialogueText = dialogue,
                    soundEffect = obj.optString("soundEffect", ""),
                    sceneDescription = scene,
                    imagePrompt = obj.optString("imagePrompt", ""),
                    imageUri = img
                )
            }
        } catch (_: Exception) {
            emptyList<ComicPanelData>()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = comic.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Стиль: ${comic.style} • Кадров: ${panels.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row {
                        if (onDelete != null) {
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить комикс", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Panels List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(panels) { panel ->
                        ComicPanelCard(panel = panel)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Закрыть комикс")
                }
            }
        }
    }
}

@Composable
fun ComicPanelCard(panel: ComicPanelData) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Panel Header / Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "КАДР ${panel.panelIndex}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (panel.soundEffect.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "💥 ${panel.soundEffect.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Panel Image if generated
            if (panel.imageUri.isNotBlank()) {
                AsyncImage(
                    model = panel.imageUri,
                    contentDescription = panel.imagePrompt,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Caption / Scene Description
            if (panel.sceneDescription.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = panel.sceneDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Speech Bubble
            if (panel.dialogueText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (panel.speakerName.isNotBlank()) {
                            Text(
                                text = panel.speakerName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = "«${panel.dialogueText}»",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicGenerationDialog(
    characterName: String,
    summaries: List<SummaryEntity> = emptyList(),
    providerProfiles: List<AiProviderProfileEntity> = emptyList(),
    onDismiss: () -> Unit,
    onGenerate: (
        chapterTitle: String,
        chapterSummaryText: String?,
        style: String,
        panelCount: Int,
        selectedProviderId: Long?,
        civitaiModelAir: String?,
        customPrompt: String,
        negativePrompt: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("Комикс: $characterName") }
    var selectedStyle by remember { mutableStateOf("anime_color") }
    var panelCount by remember { mutableIntStateOf(4) }
    var customPrompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("ugly, deformed, bad anatomy, bad hands, lowres, watermark, blurry") }

    // Chapter selection state (-1 means current active messages)
    var selectedSummaryIndex by remember { mutableIntStateOf(-1) }

    // Provider selection state
    val imageProviders = remember(providerProfiles) {
        val list = providerProfiles.filter { it.isImageProvider || it.providerType.equals("CIVITAI", ignoreCase = true) || it.providerType.equals("CUSTOM", ignoreCase = true) }
        if (list.isNotEmpty()) list else providerProfiles
    }

    var selectedProviderId by remember {
        mutableStateOf<Long?>(
            providerProfiles.find { it.isDefaultImage }?.id
                ?: imageProviders.firstOrNull()?.id
        )
    }

    val currentSelectedProfile = remember(selectedProviderId, providerProfiles) {
        providerProfiles.find { it.id == selectedProviderId }
    }

    val isCivitaiSelected = remember(currentSelectedProfile) {
        currentSelectedProfile?.providerType.equals("CIVITAI", ignoreCase = true)
    }

    // Civitai models preset
    val curatedCivitaiModels = remember {
        listOf(
            "urn:air:sdxl:checkpoint:civitai:257201@290640" to "Pony Diffusion V6 XL",
            "urn:air:sdxl:checkpoint:civitai:497558@556754" to "Illustrious-XL",
            "urn:air:sdxl:checkpoint:civitai:260267@293564" to "Animagine XL V3.1",
            "urn:air:sdxl:checkpoint:civitai:112552@126688" to "DreamShaper XL",
            "urn:air:sdxl:checkpoint:civitai:133005@456627" to "Juggernaut XL",
            "urn:air:sdxl:checkpoint:civitai:241415@272376" to "AutismMix SDXL"
        )
    }

    var selectedCivitaiModelAir by remember(isCivitaiSelected) {
        mutableStateOf(
            if (isCivitaiSelected && !currentSelectedProfile?.model.isNullOrBlank()) {
                currentSelectedProfile?.model ?: curatedCivitaiModels.first().first
            } else {
                curatedCivitaiModels.first().first
            }
        )
    }

    val styles = listOf(
        "anime_color" to "Аниме (Цвет)",
        "manga_bw" to "Манга (Ч/Б)",
        "marvel_dc" to "Комикс Marvel/DC",
        "cyberpunk" to "Футуристический Неон",
        "webtoon" to "Манхва / Вебтун",
        "watercolor" to "Акварель / Новелла",
        "pixel_art" to "Пиксель-арт 16-бит",
        "dark_fantasy" to "Темное фэнтези"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Создание комикса",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Title field
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Название выпуска комикса") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 1. Chapter / Chronicle Selection
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Выбор главы / хроники:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedSummaryIndex == -1,
                                    onClick = { selectedSummaryIndex = -1 },
                                    label = { Text("Текущий диалог (последние сообщения)", fontSize = 12.sp) },
                                    leadingIcon = {
                                        if (selectedSummaryIndex == -1) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            items(summaries.indices.toList()) { idx ->
                                val summary = summaries[idx]
                                val isSelected = selectedSummaryIndex == idx
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSummaryIndex = idx },
                                    label = {
                                        Text(
                                            text = if (summary.messageRange.isNotBlank()) "Хроника (${summary.messageRange})" else "Глава #${idx + 1}",
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // 2. Artistic Style Selection
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Стилистика арта:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        styles.chunked(2).forEach { rowStyles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowStyles.forEach { (key, name) ->
                                    val isSelected = selectedStyle == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedStyle = key },
                                        label = { Text(name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Provider Selection
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Провайдер генерации артов:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        if (providerProfiles.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "В Настройках не настроены AI-провайдеры. Комикс будет сгенерирован в виде раскадровки и реплик.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(providerProfiles) { profile ->
                                    val isSelected = selectedProviderId == profile.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedProviderId = profile.id },
                                        label = {
                                            Text(
                                                "${profile.name} (${profile.providerType})",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Civitai Model Selection (if Civitai provider selected)
                    if (isCivitaiSelected) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Модель Civitai SDXL Checkpoint:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                curatedCivitaiModels.forEach { (air, modelName) ->
                                    val isModelSelected = selectedCivitaiModelAir == air
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedCivitaiModelAir = air }
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isModelSelected,
                                            onClick = { selectedCivitaiModelAir = air }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(modelName, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isModelSelected) FontWeight.Bold else FontWeight.Normal)
                                            Text(air.takeLast(30), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = selectedCivitaiModelAir,
                                    onValueChange = { selectedCivitaiModelAir = it },
                                    label = { Text("Кастомный URN / AIR модели Civitai") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // 5. Prompt Additions (Optional)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Дополнительный промпт (необязательно):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = customPrompt,
                            onValueChange = { customPrompt = it },
                            placeholder = { Text("Детали окружения, ракурсы, освещение (напр. rainy night, cinematic lighting, neon)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }

                    // 6. Negative Prompt (Optional)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Негативный промпт (необязательно):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = negativePrompt,
                            onValueChange = { negativePrompt = it },
                            placeholder = { Text("Что исключить из генерации картинок") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }

                    // 7. Panel Count Slider
                    Column {
                        Text("Количество кадров (панелей): $panelCount", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Slider(
                            value = panelCount.toFloat(),
                            onValueChange = { panelCount = it.toInt() },
                            valueRange = 2f..8f,
                            steps = 5
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Отмена")
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val selectedSummary = if (selectedSummaryIndex in summaries.indices) {
                                    summaries[selectedSummaryIndex]
                                } else null

                                val chapterTitle = if (selectedSummary != null && selectedSummary.messageRange.isNotBlank()) {
                                    "Хроника (${selectedSummary.messageRange})"
                                } else if (selectedSummary != null) {
                                    "Глава #${selectedSummaryIndex + 1}"
                                } else {
                                    "Текущий диалог"
                                }
                                val chapterSummaryText = selectedSummary?.summaryText

                                val civitaiAir = if (isCivitaiSelected) selectedCivitaiModelAir else null

                                onGenerate(
                                    chapterTitle,
                                    chapterSummaryText,
                                    selectedStyle,
                                    panelCount,
                                    selectedProviderId,
                                    civitaiAir,
                                    customPrompt.trim(),
                                    negativePrompt.trim()
                                )
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Сгенерировать комикс")
                    }
                }
            }
        }
    }
}

@Composable
fun ComicsGalleryDialog(
    comics: List<ComicEntity>,
    onDismiss: () -> Unit,
    onSelectComic: (ComicEntity) -> Unit,
    onDeleteComic: (ComicEntity) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Галерея комиксов (${comics.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (comics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "У вас пока нет созданных комиксов",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Создайте первый комикс через меню чата",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(comics, key = { it.id }) { comic ->
                            ComicItemCard(
                                comic = comic,
                                onClick = { onSelectComic(comic) },
                                onDelete = { onDeleteComic(comic) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Закрыть галерею")
                }
            }
        }
    }
}

@Composable
fun ComicItemCard(
    comic: ComicEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(comic.createdAt) {
        val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(comic.createdAt))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (comic.coverUri.isNotBlank()) {
                AsyncImage(
                    model = comic.coverUri,
                    contentDescription = comic.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comic.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Стиль: ${comic.style} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (comic.chapterName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = comic.chapterName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ComicProgressDialog(
    statusText: String,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Генерация комикса",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusText.ifBlank { "Подготовка сцен и иллюстраций..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onCancel) {
                    Text("Скрыть в фон")
                }
            }
        }
    }
}

@Composable
fun ComicErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = {
            Text("Ошибка генерации комикса")
        },
        text = {
            Text(errorMessage)
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Понятно")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
