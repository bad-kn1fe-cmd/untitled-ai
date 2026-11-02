package com.example.ui.screens.chat_settings

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ai.engine.RoleplayPresets
import com.example.data.local.entity.AiProviderProfileEntity
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.ChatEntity
import com.example.ui.components.AppAsyncImage
import com.example.ui.components.AvatarImage
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    chat: ChatEntity,
    character: CharacterEntity,
    providers: List<AiProviderProfileEntity>,
    onSave: (ChatEntity) -> Unit,
    onDeleteChat: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appLang = com.example.util.LocalAppLanguage.current
    val isEn = appLang.equals("en", ignoreCase = true)
    val isFemboy = appLang.equals("femboy", ignoreCase = true)

    var isNsfwEnabled by remember(chat) { mutableStateOf(chat.isNsfwEnabled) }
    var selectedProviderId by remember(chat) { mutableStateOf(chat.providerProfileId) }
    var summaryFrequency by remember(chat) { mutableStateOf(chat.summaryFrequency.toFloat()) }
    var roleplayMode by remember(chat) { mutableStateOf(chat.roleplayMode) }
    var responseLength by remember(chat) { mutableStateOf(chat.responseLength) }
    var atmosphere by remember(chat) { mutableStateOf(chat.atmosphere) }

    var fullScreenPhotoUrl by remember { mutableStateOf<String?>(null) }

    val characterAppearancePhotos = remember(character) {
        val list = mutableListOf<String>()
        try {
            val arr = JSONArray(character.referenceImages)
            for (i in 0 until arr.length()) {
                val s = arr.getString(i)
                if (s.isNotBlank()) list.add(s)
            }
        } catch (_: Exception) {}
        list
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isFemboy) "Настроечки чатика" else if (isEn) "Dialogue Settings" else "Настройки диалога", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isEn) "Back" else "Назад")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val updated = chat.copy(
                                isNsfwEnabled = isNsfwEnabled,
                                providerProfileId = selectedProviderId,
                                summaryFrequency = summaryFrequency.toInt(),
                                roleplayMode = roleplayMode,
                                responseLength = responseLength,
                                atmosphere = atmosphere,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(updated)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFemboy) "✓ Применить (ня~)" else if (isEn) "✓ Apply" else "Применить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Character summary card with avatar, banner & author under avatar, short description first, and expandable details
            item {
                val context = LocalContext.current
                var isBioExpanded by remember { mutableStateOf(false) }
                val authorName = character.author.ifBlank { "UntitledAI" }
                val bannerModel = character.bannerUri.ifBlank { character.greetingBannerUri }
                val colorSourceModel = bannerModel.ifBlank { character.avatarUri }
                val defaultPlateColor = MaterialTheme.colorScheme.primaryContainer
                var bannerDominantColor by remember(colorSourceModel) { mutableStateOf<Color?>(null) }

                LaunchedEffect(colorSourceModel) {
                    if (colorSourceModel.isNotBlank()) {
                        val extracted = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val bmp = com.example.util.ImageStorageHelper.loadCachedBitmap(context, colorSourceModel, 64)
                                if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                                    var rSum = 0L
                                    var gSum = 0L
                                    var bSum = 0L
                                    var count = 0L
                                    val stepX = (bmp.width / 8).coerceAtLeast(1)
                                    val stepY = (bmp.height / 8).coerceAtLeast(1)
                                    for (y in 0 until bmp.height step stepY) {
                                        for (x in 0 until bmp.width step stepX) {
                                            val px = bmp.getPixel(x, y)
                                            val r = (px shr 16) and 0xFF
                                            val g = (px shr 8) and 0xFF
                                            val b = px and 0xFF
                                            rSum += r
                                            gSum += g
                                            bSum += b
                                            count++
                                        }
                                    }
                                    if (count > 0) {
                                        Color(
                                            red = ((rSum / count).toInt().coerceIn(25, 220)) / 255f,
                                            green = ((gSum / count).toInt().coerceIn(25, 220)) / 255f,
                                            blue = ((bSum / count).toInt().coerceIn(25, 220)) / 255f
                                        )
                                    } else null
                                } else null
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (extracted != null) {
                            bannerDominantColor = extracted
                        }
                    }
                }

                val effectiveBannerColor = bannerDominantColor ?: defaultPlateColor

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Hero header: Banner sits behind avatar, name, and badges, and smoothly fades into settings background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        ) {
                            if (bannerModel.isNotBlank()) {
                                AppAsyncImage(
                                    model = bannerModel,
                                    contentDescription = "Баннер ${character.name}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (character.avatarUri.isNotBlank()) {
                                AppAsyncImage(
                                    model = character.avatarUri,
                                    contentDescription = "Баннер ${character.name}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                                )
                                            )
                                        )
                                )
                            }

                            // Smooth gradient fade from top subtle scrim to complete surface background at the bottom
                            val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            0.0f to Color.Black.copy(alpha = 0.25f),
                                            0.40f to Color.Black.copy(alpha = 0.45f),
                                            0.72f to surfaceColor.copy(alpha = 0.85f),
                                            1.0f to surfaceColor
                                        )
                                    )
                            )

                            // Centered content over the banner: Avatar, Name, Author & Age badges
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .padding(top = 18.dp, start = 14.dp, end = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                                    shadowElevation = 6.dp
                                ) {
                                    AvatarImage(
                                        uri = character.avatarUri,
                                        contentDescription = character.name,
                                        size = 72.dp,
                                        frameUri = character.avatarFrameUri
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = effectiveBannerColor.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
                                    shadowElevation = 3.dp
                                ) {
                                    Text(
                                    text = character.getDisplayName(appLang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Author & age badges under avatar
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = if (isEn) "✍️ Author: $authorName" else "✍️ Автор: $authorName",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                ) {
                                    Text(
                                        text = if (isEn) "🎂 Age: ${character.age}" else "🎂 Возраст: ${character.age}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Bottom bar over the gradient fade (clickable to open full-screen banner)
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = bannerModel.isNotBlank() || character.avatarUri.isNotBlank()) {
                                    fullScreenPhotoUrl = bannerModel.ifBlank { character.avatarUri }
                                },
                            color = Color.Black.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFemboy) "Баннер персика • by $authorName" else if (isEn) "Character banner • by $authorName" else "Баннер персонажа • by $authorName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "📊 ${character.getEffectiveInitialRelTitle()} (${character.getEffectiveInitialRelPercent()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Seamless bio content directly under the hero banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            border = BorderStroke(
                                1.dp,
                                if (isBioExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { isBioExpanded = !isBioExpanded }
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isEn) "Short description:" else "Краткое описание:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isBioExpanded) (if (isEn) "Collapse" else "Свернуть") else (if (isEn) "Tap to expand all" else "Нажмите, чтобы раскрыть всё"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp
                                    )
                                    Icon(
                                        imageVector = if (isBioExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = character.getDisplayDescription(appLang).ifBlank { if (isEn) "Short description not specified." else "Краткое описание не указано." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                                androidx.compose.animation.AnimatedVisibility(visible = isBioExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                        if (character.appearance.isNotBlank()) {
                                            Column {
                                                Text(
                                                    text = "✨ Внешность:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = character.appearance,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (characterAppearancePhotos.isNotEmpty()) {
                                            Column {
                                                Text(
                                                    text = "📸 Фото внешности:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(characterAppearancePhotos) { photoUri ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(width = 80.dp, height = 110.dp)
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
                                                                .clickable { fullScreenPhotoUrl = photoUri }
                                                        ) {
                                                            AppAsyncImage(
                                                                model = photoUri,
                                                                contentDescription = "Внешность",
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                            Surface(
                                                                color = Color.Black.copy(alpha = 0.5f),
                                                                shape = RoundedCornerShape(topStart = 6.dp),
                                                                modifier = Modifier.align(Alignment.BottomEnd)
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.ZoomIn,
                                                                    contentDescription = "Увеличить",
                                                                    tint = Color.White,
                                                                    modifier = Modifier.padding(3.dp).size(12.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (character.personality.isNotBlank()) {
                                            Column {
                                                Text(
                                                    text = "🧠 Характер:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = character.personality,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (character.voiceStyle.isNotBlank()) {
                                            Column {
                                                Text(
                                                    text = "🗣️ Голос и манера общения:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = character.voiceStyle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (character.habits.isNotBlank()) {
                                            Column {
                                                Text(
                                                    text = "🎭 Привычки и особенности:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = character.habits,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (character.likes.isNotBlank()) {
                                            Column {
                                                Text(
                                                    text = "💚 Любит:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = character.likes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (character.dislikes.isNotBlank()) {
                                            Column {
                                                Text(
                                                    text = "💔 Не любит:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = character.dislikes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (character.greetingMessage.isNotBlank()) {
                                            Column {
                                                Text(
                                                    text = "👋 Приветственное сообщение:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = character.greetingMessage,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        val parsedLoreCards = remember(character.knowledgeCards) {
                                            val list = mutableListOf<Pair<String, String>>()
                                            try {
                                                val arr = org.json.JSONArray(character.knowledgeCards)
                                                for (i in 0 until arr.length()) {
                                                    val obj = arr.getJSONObject(i)
                                                    list.add(obj.optString("title") to obj.optString("content"))
                                                }
                                            } catch (_: Exception) {}
                                            list
                                        }
                                        if (parsedLoreCards.isNotEmpty()) {
                                            var collapsedBioLoreIndices by remember(character.id) { mutableStateOf(parsedLoreCards.indices.toSet()) }
                                            val allBioLoreCollapsed = collapsedBioLoreIndices.size >= parsedLoreCards.size
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "📚 Карточки знаний (${parsedLoreCards.size}):",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    TextButton(
                                                        onClick = {
                                                            collapsedBioLoreIndices = if (allBioLoreCollapsed) emptySet() else parsedLoreCards.indices.toSet()
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                                    ) {
                                                        Text(
                                                            text = if (allBioLoreCollapsed) "Развернуть все" else "Свернуть все",
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                                parsedLoreCards.forEachIndexed { idx, (title, content) ->
                                                    val isCardCollapsed = idx in collapsedBioLoreIndices
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable {
                                                                collapsedBioLoreIndices = if (isCardCollapsed) collapsedBioLoreIndices - idx else collapsedBioLoreIndices + idx
                                                            }
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = title.ifBlank { "Карточка #${idx + 1}" },
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                                Icon(
                                                                    imageVector = if (isCardCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                            androidx.compose.animation.AnimatedVisibility(visible = !isCardCollapsed) {
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
                                }
                            }
                        }
                    }
                }
            }
            }

            // 6. 18+ Content Toggle
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isEn) "18+ Content Mode" else "Режим 18+ контента",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "NSFW",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isEn) "Permits unfiltered adult narratives and intimate scenes." else "Позволяет откровенные сцены и диалоги без цензуры приложения.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = isNsfwEnabled,
                                onCheckedChange = { enable ->
                                    isNsfwEnabled = enable
                                }
                            )
                        }
                    }
                }
            }

            // 4. Memory / Summary frequency slider
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEn) "Auto-memory & chronicle frequency" else "Частота авто-памяти и хроники",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEn) "Every ${summaryFrequency.toInt()} msgs" else "Каждые ${summaryFrequency.toInt()} сообщ.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isEn) "Interval for auto-summary generation and relationship board update." else "Интервал генерации авто-саммари и обновления таблицы отношений.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = summaryFrequency,
                            onValueChange = { summaryFrequency = it },
                            valueRange = 10f..30f,
                            steps = 19
                        )
                    }
                }
            }

            // 5. Provider override
            item {
                Text(
                    text = if (isFemboy) "ИИ-Провайдер для этого чатика" else if (isEn) "AI Provider for this dialogue" else "AI-провайдер для этого диалога",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedProviderId == null,
                            onClick = { selectedProviderId = null },
                            label = { Text(if (isFemboy) "По умолчанию из настроечек" else if (isEn) "Default from Settings" else "По умолчанию из настроек") },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(providers.filter { !it.isImageProvider }) { p ->
                        FilterChip(
                            selected = selectedProviderId == p.id,
                            onClick = { selectedProviderId = p.id },
                            label = { Text(p.name) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // 6. Delete chat
            item {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDeleteChat,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFemboy) "🗑 Удалить этот чатик и историю" else if (isEn) "🗑 Delete this dialogue and history" else "Удалить этот диалог и историю")
                }
            }
        }
    }

    fullScreenPhotoUrl?.let { photoUri ->
        Dialog(onDismissRequest = { fullScreenPhotoUrl = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Внешность: ${character.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { fullScreenPhotoUrl = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp, max = 460.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri.startsWith("drawable://")) {
                            val resName = photoUri.removePrefix("drawable://")
                            val context = LocalContext.current
                            val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
                            if (resId != 0) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = "Внешность персонажа",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Внешность персонажа",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
