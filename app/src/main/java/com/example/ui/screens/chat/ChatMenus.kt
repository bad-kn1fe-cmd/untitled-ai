package com.example.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.RelationshipTableEntity
import com.example.data.local.entity.UserPersonaEntity
import com.example.ui.components.AvatarImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isEn: Boolean,
    isFemboy: Boolean,
    effectivePersona: UserPersonaEntity?,
    chat: ChatEntity,
    character: CharacterEntity? = null,
    latestRelationship: RelationshipTableEntity?,
    onPersonaSheetOpen: () -> Unit,
    onBranchesDialogOpen: () -> Unit,
    onNewChatDialogOpen: () -> Unit,
    onOpenRelationshipHistory: () -> Unit,
    onNotesDialogOpen: () -> Unit,
    onSystemPromptDialogOpen: () -> Unit,
    onChatSettingsOpen: () -> Unit,
    onRagDebugDialogOpen: () -> Unit,
    onClearChatDialogOpen: () -> Unit,
    onToggleArchiveChat: () -> Unit = {},
    onFreeMemoryNewChat: () -> Unit = {}
) {
    if (!expanded) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Шапка: Название диалога / Бот
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (character != null && character.avatarUri.isNotBlank()) {
                    AvatarImage(
                        uri = character.avatarUri,
                        contentDescription = character.name,
                        size = 46.dp,
                        modifier = Modifier.clip(CircleShape),
                        frameUri = character.avatarFrameUri
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = chat.title.ifBlank { character?.name ?: "Диалог" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (chat.isArchived) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = if (isEn) "Archive" else "Архив",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (character != null) {
                            if (isFemboy) "Настроечки и опции чатика с ${character.name} 🐾" else if (isEn) "Chat options & settings for ${character.name}" else "Опции и управление диалогом с ${character.name}"
                        } else {
                            if (isEn) "Chat Options & Settings" else "Опции и управление диалогом"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Персона диалога (Акцентная плашка)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onDismissRequest()
                        onPersonaSheetOpen()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!effectivePersona?.avatarUri.isNullOrBlank()) {
                        AvatarImage(
                            uri = effectivePersona.avatarUri,
                            size = 40.dp,
                            modifier = Modifier.clip(CircleShape),
                            contentDescription = null,
                            frameUri = ""
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                com.example.ui.components.AdaptiveUiIcon(
                                    emoji = "🎭",
                                    overrideVector = Icons.Default.Face,
                                    tint = MaterialTheme.colorScheme.primary,
                                    size = 22.dp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFemboy) "Масочка (персона) для чатика" else if (isEn) "Chat Persona" else "Персона для этого диалога",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = effectivePersona?.name ?: (if (isFemboy) "Глобальная по умолчанию" else if (isEn) "Global Default" else "Глобальная персона по умолчанию"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 3. Блок: Ветки и управление диалогами
            Text(
                text = if (isEn) "DIALOGUE & BRANCHES" else "ВЕТКИ И ДИАЛОГИ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SheetActionRow(
                        emoji = "💬",
                        icon = Icons.Default.Forum,
                        title = if (isFemboy) "Ветки диалога... 🐾" else if (isEn) "Dialogue branches..." else "Ветки диалога",
                        subtitle = if (isEn) "Switch between parallel storylines" else "Переключение между параллельными сюжетами",
                        onClick = {
                            onDismissRequest()
                            onBranchesDialogOpen()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    SheetActionRow(
                        emoji = "➕",
                        icon = Icons.Default.AddComment,
                        title = if (isFemboy) "Новый чатик с ботиком" else if (isEn) "New dialogue" else "Новый диалог с ботом",
                        subtitle = if (isEn) "Start a fresh story from the beginning" else "Начать диалог с чистого листа",
                        onClick = {
                            onDismissRequest()
                            onNewChatDialogOpen()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    SheetActionRow(
                        emoji = "🧠",
                        icon = Icons.Default.Psychology,
                        title = if (isFemboy) "Новая главка (разгрузить память ИИ) ✨" else if (isEn) "New Chapter (Free AI Memory)" else "Новая глава (освободить память ИИ)",
                        subtitle = if (isEn) "Archive current chat & compress memories into fresh context" else "Архивировать текущий чат и продолжить со сжатой памятью",
                        onClick = {
                            onDismissRequest()
                            onFreeMemoryNewChat()
                        },
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    SheetActionRow(
                        emoji = "📦",
                        icon = if (chat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        title = if (chat.isArchived) {
                            if (isFemboy) "Разархивировать чатик" else if (isEn) "Unarchive chat" else "Разархивировать чат"
                        } else {
                            if (isFemboy) "В архивчик 📦" else if (isEn) "Archive chat" else "В архив 📦"
                        },
                        subtitle = if (chat.isArchived) {
                            if (isEn) "Restore chat to active list" else "Вернуть диалог в список активных"
                        } else {
                            if (isEn) "Move to read-only archive" else "Перенести в архив только для чтения"
                        },
                        onClick = {
                            onDismissRequest()
                            onToggleArchiveChat()
                        }
                    )
                }
            }

            // 4. Блок: ИИ, Настройки и Контекст
            Text(
                text = if (isEn) "AI & CONTEXT" else "ИНТЕЛЛЕКТ И КОНТЕКСТ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SheetActionRow(
                        emoji = "🛠️",
                        icon = Icons.Default.Tune,
                        title = if (isFemboy) "Настроечки чатика ⚙️" else if (isEn) "Chat Settings" else "Настройки чата",
                        subtitle = if (isEn) "AI model, temperature, prompt parameters" else "Модель ИИ, температура, параметры генерации",
                        onClick = {
                            onDismissRequest()
                            onChatSettingsOpen()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    SheetActionRow(
                        emoji = "💚",
                        icon = Icons.Default.Favorite,
                        title = if (isFemboy) "История милоты и отношений" else if (isEn) "Relationship History" else "История отношений",
                        subtitle = if (latestRelationship != null) {
                            if (isEn) "Trust score: ${latestRelationship.trustScore}%" else "Уровень доверия: ${latestRelationship.trustScore}%"
                        } else {
                            if (isEn) "Affection & trust tracking" else "Динамика доверия и симпатии"
                        },
                        onClick = {
                            onDismissRequest()
                            onOpenRelationshipHistory()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    SheetActionRow(
                        emoji = "📝",
                        icon = Icons.Default.EditNote,
                        title = if (isFemboy) "Заметочки к чатику" else if (isEn) "Chat Notes" else "Заметки к чату",
                        subtitle = if (chat.notes.isNotBlank()) (if (isEn) "Notes filled" else "Заметки заполнены") else (if (isFemboy) "Пустенько" else if (isEn) "Empty" else "Пусто"),
                        onClick = {
                            onDismissRequest()
                            onNotesDialogOpen()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    SheetActionRow(
                        emoji = "⚙️",
                        icon = Icons.Default.Terminal,
                        title = if (isEn) "AI System Prompt" else "Системный промпт ИИ",
                        subtitle = if (isFemboy) "Просмотр и цап-царап промпта" else if (isEn) "View and copy active prompt" else "Просмотр и копирование промпта",
                        onClick = {
                            onDismissRequest()
                            onSystemPromptDialogOpen()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    SheetActionRow(
                        emoji = "🧪",
                        icon = Icons.Default.Analytics,
                        title = if (isEn) "RAG Knowledge Base & Vectors" else "База знаний RAG и Векторы",
                        subtitle = if (isEn) "Vector search and lore chunks" else "Семантический поиск по лору и фактам",
                        onClick = {
                            onDismissRequest()
                            onRagDebugDialogOpen()
                        }
                    )
                }
            }

            // 5. Очистка сообщений (Опасная зона)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onDismissRequest()
                        onClearChatDialogOpen()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.components.AdaptiveUiIcon(
                        emoji = "🗑️",
                        overrideVector = Icons.Default.DeleteSweep,
                        tint = MaterialTheme.colorScheme.error,
                        size = 22.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFemboy) "Стереть сообщения (ня)" else if (isEn) "Clear Messages" else "Очистить сообщения",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (isEn) "Delete dialogue history in this branch" else "Удалить все сообщения в текущей ветке",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    emoji: String = "",
    icon: ImageVector? = null,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji.isNotBlank()) {
            com.example.ui.components.AdaptiveUiIcon(
                emoji = emoji,
                overrideVector = icon,
                tint = iconTint,
                size = 22.dp
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiNewChatSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    isEn: Boolean,
    isFemboy: Boolean,
    characterName: String,
    onNewChatClick: () -> Unit,
    onFreeMemoryNewChatClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onOpenAllSettingsClick: () -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        com.example.ui.components.ChatCustomIcons.GeminiPenSparkle(
                            color = MaterialTheme.colorScheme.primary,
                            size = 24.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (isFemboy) "Новый чатик или история 🐾" else if (isEn) "Gemini Conversations" else "Диалоги Gemini",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEn) "Start a new conversation or explore history with $characterName" else "Создание нового диалога или просмотр истории с $characterName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Options Surface
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // 1. Fresh New Chat
                    SheetActionRow(
                        emoji = "💬",
                        icon = Icons.Default.AddComment,
                        title = if (isFemboy) "Новый чатик с ботиком" else if (isEn) "New chat" else "Создать новый чат",
                        subtitle = if (isEn) "Start fresh conversation from the beginning" else "Начать общение с чистого листа",
                        onClick = {
                            onDismissRequest()
                            onNewChatClick()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // 2. Free AI memory / new chapter
                    SheetActionRow(
                        emoji = "🧠",
                        icon = Icons.Default.Psychology,
                        title = if (isFemboy) "Новая главка (разгрузить память ИИ) ✨" else if (isEn) "New Chapter (Free AI Memory)" else "Новая глава (освободить память ИИ)",
                        subtitle = if (isEn) "Archive current chat & compress memories" else "Архивировать текущий чат и продолжить со сжатой памятью",
                        onClick = {
                            onDismissRequest()
                            onFreeMemoryNewChatClick()
                        },
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // 3. Dialogue History / branches
                    SheetActionRow(
                        emoji = "💬",
                        icon = Icons.Default.Forum,
                        title = if (isFemboy) "История диалогов и ветки... 🐾" else if (isEn) "Dialogue history & branches" else "История диалогов и ветки",
                        subtitle = if (isEn) "View all active and archived chats" else "Просмотреть все активные и архивные ветки",
                        onClick = {
                            onDismissRequest()
                            onHistoryClick()
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // 4. All Chat Settings
                    SheetActionRow(
                        emoji = "⚙️",
                        icon = Icons.Default.MoreVert,
                        title = if (isEn) "All Chat Options & Settings..." else "Все опции и настройки чата...",
                        subtitle = if (isEn) "Persona, prompt, notes, relationships & parameters" else "Персона, системный промпт, заметки и параметры",
                        onClick = {
                            onDismissRequest()
                            onOpenAllSettingsClick()
                        }
                    )
                }
            }
        }
    }
}

