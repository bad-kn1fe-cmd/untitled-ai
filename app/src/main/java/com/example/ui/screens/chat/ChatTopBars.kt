package com.example.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.UserPersonaEntity
import com.example.ui.components.AvatarImage
import com.example.ui.components.ChatCustomIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeepSeekTopBar(
    glassBg: Color,
    glassContentColor: Color,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = glassBg,
            navigationIconContentColor = glassContentColor,
            actionIconContentColor = glassContentColor
        ),
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = onMenuClick,
                        onDoubleClick = onMenuClick
                    )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                // Two horizontal parallel lines matching screenshot
                ChatCustomIcons.TwoLines(color = glassContentColor)
            }
        },
        actions = {
            IconButton(onClick = onNewChatClick) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "New Chat", tint = glassContentColor)
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = glassContentColor)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GeminiTopBar(
    glassBg: Color,
    glassContentColor: Color,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onPenClick: () -> Unit = onMenuClick,
    onPersonaClick: () -> Unit,
    onOpenProviderPicker: () -> Unit,
    activeProviderName: String,
    effectivePersona: UserPersonaEntity?
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = glassBg,
            titleContentColor = glassContentColor,
            navigationIconContentColor = glassContentColor,
            actionIconContentColor = glassContentColor
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { onOpenProviderPicker() },
                        onLongClick = onMenuClick,
                        onDoubleClick = onMenuClick
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = activeProviderName.ifBlank { "Gemini" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = glassContentColor
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = glassContentColor.copy(alpha = 0.75f)
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                // Two horizontal parallel lines matching Gemini screenshot
                ChatCustomIcons.TwoLines(color = glassContentColor)
            }
        },
        actions = {
            IconButton(onClick = onPenClick) {
                // Sparkle stylus / pen icon matching Gemini screenshot
                ChatCustomIcons.GeminiPenSparkle(color = glassContentColor)
            }
            IconButton(onClick = onPersonaClick) {
                if (!effectivePersona?.avatarUri.isNullOrBlank()) {
                    AvatarImage(
                        uri = effectivePersona!!.avatarUri,
                        size = 32.dp,
                        modifier = Modifier.clip(CircleShape),
                        contentDescription = null,
                        frameUri = ""
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Persona",
                        modifier = Modifier.size(32.dp),
                        tint = glassContentColor
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CharacterAiTopBar(
    character: CharacterEntity,
    glassBg: Color,
    glassContentColor: Color,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onPersonaClick: () -> Unit,
    effectivePersona: UserPersonaEntity?
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = glassBg,
            titleContentColor = glassContentColor,
            navigationIconContentColor = glassContentColor,
            actionIconContentColor = glassContentColor
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .combinedClickable(
                        onClick = { onPersonaClick() },
                        onLongClick = onMenuClick,
                        onDoubleClick = onMenuClick
                    )
            ) {
                if (character.avatarUri.isNotBlank()) {
                    AvatarImage(
                        uri = character.avatarUri,
                        size = 36.dp,
                        modifier = Modifier.clip(CircleShape),
                        contentDescription = null,
                        frameUri = ""
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = glassContentColor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = character.name.ifBlank { "имя бота" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = glassContentColor
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = glassContentColor)
            }
        },
        actions = {
            IconButton(onClick = onHistoryClick) {
                // Tilted book with ribbon bookmark icon
                ChatCustomIcons.CaiBook(color = glassContentColor)
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = glassContentColor)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JanitorAiTopBar(
    character: CharacterEntity,
    glassBg: Color,
    glassContentColor: Color,
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    onOpenProviderPicker: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = glassBg,
            titleContentColor = glassContentColor,
            navigationIconContentColor = glassContentColor,
            actionIconContentColor = glassContentColor
        ),
        title = {
            Text(
                text = character.name.ifBlank { "имя бота" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = glassContentColor,
                modifier = Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = onMenuClick,
                    onDoubleClick = onMenuClick
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = glassContentColor, modifier = Modifier.size(28.dp))
            }
        },
        actions = {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF4C3B78), // Purple badge from Janitor AI screenshot
                border = BorderStroke(1.dp, Color(0xFF7C66B8)),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { onOpenProviderPicker() }
            ) {
                Text(
                    text = "using untitled",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFECE6FF),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = glassContentColor)
            }
        }
    )
}
