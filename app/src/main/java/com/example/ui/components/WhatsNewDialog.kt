package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WhatsNewFeatureItem(
    val emoji: String,
    val titleRu: String,
    val titleEn: String,
    val titleFemboy: String,
    val descRu: String,
    val descEn: String,
    val descFemboy: String,
    val badge: String = "1.0.0"
)

object WhatsNewReleaseNotes {
    val FEATURES_100 = listOf(
        WhatsNewFeatureItem(
            emoji = "🐱",
            titleRu = "Питомец Бойкиссер на экране",
            titleEn = "Boykisser Screen Companion Pet",
            titleFemboy = "Питомец Бойкиссер на экране (🐾)",
            descRu = "Милый бегающий питомец с выбором стиля (2D / 3D), скоростью и реагированием на тапы! Сидит над полем ввода и не перекрывает текст.",
            descEn = "A cute Boykisser pet walking on screen with 2D / 3D styles, speed controls and tap reactions! Sits over the input bar without blocking text.",
            descFemboy = "Миленький бойкиссер передвигается по экрану и мурчит по тапу! Сидит над полем ввода и не закрывает буковки (uwu)."
        ),
        WhatsNewFeatureItem(
            emoji = "🎨",
            titleRu = "Адаптивное тонирование элементов",
            titleEn = "Adaptive Theme Accent Tinting",
            titleFemboy = "Тонирование аватарок под цвет темы",
            descRu = "Аватарки ботов, баннеры и рамки автоматически слегка подкрашиваются в выбранный акцентный цвет вашей темы с живым предпросмотром.",
            descEn = "Bot avatars, banners, and frames automatically get a subtle accent tint matching your active theme with live preview.",
            descFemboy = "Аватарки ботиков, баннеры и рамочки подкрашиваются в миленький цвет выбранной темы с живым предпросмотром (ня~)."
        ),
        WhatsNewFeatureItem(
            emoji = "📎",
            titleRu = "Новая панель прикреплений в чате",
            titleEn = "New Attachments Popup Bar in Chat",
            titleFemboy = "Новая панелька скрепки в чатике",
            descRu = "Быстрая запись голосовых сообщений 🎙, скачивание и экспорт переписки 📥, а также отправка любых файлов и документов 📄 прямо из скрепки.",
            descEn = "Quick voice message recording 🎙, chat export/import 📥, and any file/document upload 📄 directly inside the paperclip menu.",
            descFemboy = "Быстрая запись голосовых реплик 🎙, сохраняшки чатика 📥 и загрузка любых файликов 📄 прямо из скрепки (owo)."
        ),
        WhatsNewFeatureItem(
            emoji = "⚡",
            titleRu = "Кнопка % лимита и скип очереди",
            titleEn = "Token Limit % Button & Turn Skip",
            titleFemboy = "Кнопочка % лимита и скип очереди",
            descRu = "Индикатор использования токенов в поле ввода. Нажатие на % позволяет моментально скипнуть ход и сгенерировать ответ ИИ.",
            descEn = "Circular token limit indicator in input field. Tap % to instantly skip turn and generate bot response.",
            descFemboy = "Круглая кнопочка % лимита. Нажми %, чтобы моментально сгенерировать ответик ботика (uwu)."
        ),
        WhatsNewFeatureItem(
            emoji = "🌐",
            titleRu = "Полная тройная локализация",
            titleEn = "Complete Triple Localization",
            titleFemboy = "Полный тройной перевод (Ня~)",
            descRu = "100% перевод каждого экрана, диалога и меню на Русский, English и Фембойский (Ня~) языки.",
            descEn = "100% full translation of every screen, dialog, and menu into Russian, English, and Femboy languages.",
            descFemboy = "100% переводик всех экранов, диалогов и менюшек на Русский, English и Фембойский (🐾) язычки."
        ),
        WhatsNewFeatureItem(
            emoji = "🚀",
            titleRu = "Экстремальная оптимизация 60+ FPS",
            titleEn = "Extreme Low-End Optimization (60+ FPS)",
            titleFemboy = "Экстремальная оптимизация скорости (60+ FPS)",
            descRu = "Мгновенное рециклирование вкладок, O(1) кэширование растров и супер-плавный скролл даже на слабых устройствах.",
            descEn = "Instant tab node recycling, O(1) in-memory bitmap cache, and ultra-smooth scrolling on low-end devices.",
            descFemboy = "Мгновенное переключение вкладок, кэширование картинкочек и супер-плавный скролл на любых телефончиках."
        ),
        WhatsNewFeatureItem(
            emoji = "🛠️",
            titleRu = "Изолированные категории настроек",
            titleEn = "Isolated Settings Category Views",
            titleFemboy = "Изолированные разделы настроечек",
            descRu = "Удобный и четкий доступ к разделам оформления, языков, обоев, параметров поведения и системы без каши.",
            descEn = "Clean, isolated navigation across appearance, language, wallpapers, behavior, and system settings.",
            descFemboy = "Удобный переходик по разделам оформления, языков, обойчиков, поведения и системы без лишней каши (uwu)."
        )
    )

    suspend fun fetchRemoteMarkdownChanges(): String? = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://raw.githubusercontent.com/bad-kn1fe-cmd/untitled-ai/main/new-changes.md",
            "https://github.com/bad-kn1fe-cmd/untitled-ai/raw/main/new-changes.md",
            "https://raw.githubusercontent.com/yoayammmDEV/box-of-untitledAI/main/new-changes.md"
        )
        for (url in urls) {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 4500
                connection.readTimeout = 4500
                connection.requestMethod = "GET"
                if (connection.responseCode == 200) {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    if (text.isNotBlank()) return@withContext text
                }
            } catch (_: Exception) {}
        }
        null
    }
}

@Composable
fun WhatsNewDialog(
    appLanguage: String,
    onConfirm: () -> Unit
) {
    val isEn = appLanguage.equals("en", ignoreCase = true)
    val isFemboy = appLanguage.equals("femboy", ignoreCase = true)

    var remoteMarkdown by remember { mutableStateOf<String?>(null) }
    var isLoadingRemote by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingRemote = true
        val text = WhatsNewReleaseNotes.fetchRemoteMarkdownChanges()
        remoteMarkdown = text
        isLoadingRemote = false
    }

    val titleText = if (isFemboy) "🎉 Что нового в 1.0.0 (uwu)" else if (isEn) "🎉 What's New in 1.0.0" else "🎉 Что добавлено в 1.0.0"
    val subtitleText = if (isFemboy) "Официальный релиз версии 1.0.0 — Свободный ролевой движок «Untitled» 🐾"
    else if (isEn) "Official release 1.0.0 — Open Roleplay Engine 'Untitled'"
    else "Официальный релиз версии 1.0.0 — Свободный ролевой движок «Untitled»"

    val btnText = if (isFemboy) "Отлично, помурчали! 🐾" else if (isEn) "Awesome, let's go! 🚀" else "Отлично, погнали! 🚀"

    Dialog(onDismissRequest = onConfirm) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))

                // Content View: Loading vs Remote Markdown vs Built-in Fallback
                if (isLoadingRemote) {
                    // Empty Window / Skeleton Loading State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isFemboy) "Загружаем new-changes.md из GitHub (uwu)..."
                                else if (isEn) "Fetching new-changes.md from GitHub..."
                                else "Загрузка new-changes.md с GitHub...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else if (!remoteMarkdown.isNullOrBlank()) {
                    // Render Remote Markdown Content from new-changes.md
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val lines = remoteMarkdown!!.lines()
                            items(lines) { line ->
                                val trimmed = line.trim()
                                when {
                                    trimmed.startsWith("# ") -> {
                                        Text(
                                            text = trimmed.removePrefix("# ").trim(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                        )
                                    }
                                    trimmed.startsWith("## ") -> {
                                        Text(
                                            text = trimmed.removePrefix("## ").trim(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                        )
                                    }
                                    trimmed.startsWith("### ") -> {
                                        Text(
                                            text = trimmed.removePrefix("### ").trim(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                        ) {
                                            Text(
                                                text = "• ",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = trimmed.removePrefix("- ").removePrefix("* ").trim()
                                                    .replace("**", ""),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.5.sp,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                    trimmed.isNotBlank() -> {
                                        Text(
                                            text = trimmed.replace("**", ""),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.5.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Built-in Release Notes Fallback
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(WhatsNewReleaseNotes.FEATURES_100, key = { it.titleRu }) { item ->
                            val itemTitle = if (isFemboy) item.titleFemboy else if (isEn) item.titleEn else item.titleRu
                            val itemDesc = if (isFemboy) item.descFemboy else if (isEn) item.descEn else item.descRu

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = item.emoji,
                                        fontSize = 24.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = itemTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = item.badge,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = itemDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Confirm Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = btnText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
