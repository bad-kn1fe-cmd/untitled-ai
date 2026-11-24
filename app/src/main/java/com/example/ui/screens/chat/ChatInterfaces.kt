package com.example.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CharacterEntity
import com.example.ui.components.AvatarImage
import com.example.ui.components.ChatCustomIcons

@Composable
fun DeepSeekEmptyState(
    character: CharacterEntity,
    greetingText: String = character.greetingMessage
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 130.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big round avatar in center
        if (character.avatarUri.isNotBlank()) {
            AvatarImage(
                uri = character.avatarUri,
                size = 100.dp,
                modifier = Modifier.clip(CircleShape),
                contentDescription = null,
                frameUri = character.avatarFrameUri
            )
        } else {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bot name
        Text(
            text = character.name.ifBlank { "Bot name" },
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Age and gender indicator (e.g. 18 ♂)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val genderIcon = if (character.tags.contains("female")) "♀" else "♂"
            val genderColor = if (character.tags.contains("female")) Color(0xFFF472B6) else Color(0xFF38BDF8)
            val displayAge = if (character.age > 0) character.age.toString() else "18"

            Text(
                text = displayAge,
                fontSize = 15.sp,
                color = Color(0xFF9CA3AF)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = genderIcon,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = genderColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Welcome message
        Text(
            text = greetingText.ifBlank { character.greetingMessage.ifBlank { "Welcome message" } },
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GeminiEmptyState(
    character: CharacterEntity,
    greetingText: String = character.greetingMessage
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Small avatar in center
        if (character.avatarUri.isNotBlank()) {
            AvatarImage(
                uri = character.avatarUri,
                size = 46.dp,
                modifier = Modifier.clip(CircleShape),
                contentDescription = null,
                frameUri = character.avatarFrameUri
            )
        } else {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // "Всегда готов"
        Text(
            text = "Всегда готов",
            fontSize = 25.sp,
            fontWeight = FontWeight.Normal,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Greeting message
        Text(
            text = greetingText.ifBlank { character.greetingMessage.ifBlank { "Приветственное сообщение" } },
            fontSize = 15.sp,
            color = Color(0xFFE5E7EB),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CharacterAiEmptyState(
    character: CharacterEntity,
    onRegenerateClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        // Centered author subtitle
        Text(
            text = "Автором  ${character.name.ifBlank { "имя бота" }}  является автор бота",
            fontSize = 13.sp,
            color = Color(0xFFE5E7EB),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        // Bot greeting bubble message with avatar on left
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            if (character.avatarUri.isNotBlank()) {
                AvatarImage(
                    uri = character.avatarUri,
                    size = 38.dp,
                    modifier = Modifier.clip(CircleShape),
                    contentDescription = null,
                    frameUri = ""
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF262629)
                ) {
                    Text(
                        text = character.greetingMessage.ifBlank { "описание бота" },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Blue circle with cycle refresh arrow under bubble
                Surface(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(36.dp)
                        .clickable { onRegenerateClick() },
                    shape = CircleShape,
                    color = Color(0xFF1B243B) // Dark navy circle
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ChatCustomIcons.CaiCycleArrow(
                            size = 18.dp,
                            color = Color(0xFF3B82F6) // Bright blue matching screenshot
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JanitorAiEmptyState(character: CharacterEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 220.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Top disclaimer matching screenshot
        Text(
            text = "Все ответы — художественный вымысел,\nсгенерированный ИИ.",
            fontSize = 13.5.sp,
            color = Color(0xFFE5E7EB),
            textAlign = TextAlign.Start,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Bot Greeting entry: Avatar + Name + Greeting message
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            if (character.avatarUri.isNotBlank()) {
                AvatarImage(
                    uri = character.avatarUri,
                    size = 44.dp,
                    modifier = Modifier.clip(CircleShape),
                    contentDescription = null,
                    frameUri = character.avatarFrameUri
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = character.name.ifBlank { "имя бота" },
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = character.greetingMessage.ifBlank { "приветственное сообщение бота" },
                    fontSize = 14.sp,
                    color = Color(0xFFE5E7EB)
                )
            }
        }
    }
}
