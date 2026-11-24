package com.example.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.UserPersonaEntity
import com.example.ui.components.AvatarImage
import com.example.ui.components.ChatCustomIcons

@Composable
fun DeepSeekInputBar(
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
    activeProviderName: String,
    reasoningLevelName: String,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onProviderClick: () -> Unit,
    onReasoningClick: () -> Unit,
    onVoiceClick: () -> Unit,
    isAITyping: Boolean,
    onStopGeneration: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF222326), // Dark slate-charcoal matching DeepSeek screenshot
        border = BorderStroke(0.5.dp, Color(0xFF33353A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        "Напишите или удерживайте, чтобы говорить",
                        fontSize = 14.5.sp,
                        color = Color(0xFF90949D)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 38.dp, max = 100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFE5E7EB)
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Chips: [Atom "Рассуждение"] and [Spark "Gemini"]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Reasoning Chip (dark pill with atom icon)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF2B2C30),
                        border = BorderStroke(0.8.dp, Color(0xFF3F4148)),
                        modifier = Modifier.clickable { onReasoningClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChatCustomIcons.DeepSeekAtom(
                                size = 16.dp,
                                color = Color(0xFFE5E7EB)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reasoningLevelName.ifBlank { "Рассуждение" },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFFE5E7EB)
                            )
                        }
                    }

                    // 2. Provider Chip (blueish pill with spark icon)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(0.8.dp, Color(0xFF334A6E)),
                        modifier = Modifier.clickable { onProviderClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChatCustomIcons.GeminiSpark(
                                size = 13.dp,
                                color = Color(0xFF60A5FA)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = activeProviderName.ifBlank { "Gemini" },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF60A5FA)
                            )
                        }
                    }
                }

                // Right action buttons: (+) attach and soundwave voice/send
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onAttachClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.AddCircleOutline,
                            contentDescription = "Прикрепить",
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (isAITyping) {
                        IconButton(
                            onClick = onStopGeneration,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Стоп",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (inputText.text.isNotBlank()) {
                        IconButton(
                            onClick = onSend,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = "Отправить",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onVoiceClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            ChatCustomIcons.DeepSeekVoice(
                                size = 24.dp,
                                color = Color(0xFFD1D5DB)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiInputBar(
    character: CharacterEntity,
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onVoiceClick: () -> Unit,
    isAITyping: Boolean,
    onStopGeneration: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(percent = 50),
        color = Color(0xFF1E1F22), // Gemini dark pill background
        border = BorderStroke(0.5.dp, Color(0xFF333538))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAttachClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color(0xFFE5E7EB),
                    modifier = Modifier.size(22.dp)
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        "Спросить ${character.name.ifBlank { "имя бота" }}",
                        fontSize = 15.sp,
                        color = Color(0xFF8E9199)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 100.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFE5E7EB)
                )
            )

            if (isAITyping) {
                IconButton(
                    onClick = onStopGeneration,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else if (inputText.text.isNotBlank()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = Color(0xFFA8C7FA)
                    )
                }
            } else {
                IconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.MicNone,
                        contentDescription = "Voice",
                        tint = Color(0xFFE5E7EB),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CharacterAiInputBar(
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
    effectivePersona: UserPersonaEntity?,
    onPersonaClick: () -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onVoiceClick: () -> Unit,
    isAITyping: Boolean,
    onStopGeneration: () -> Unit,
    useMaterial3: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Input Pill Capsule
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xFF262629), // Dark gray pill matching C.AI screenshot
                border = BorderStroke(0.5.dp, Color(0xFF38383C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Persona avatar or default user circle icon
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp, end = 6.dp)
                            .clickable { onPersonaClick() }
                    ) {
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
                                tint = Color(0xFFD1D5DB)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChange,
                        placeholder = {
                            Text(
                                "Сообщение...",
                                fontSize = 14.5.sp,
                                color = Color(0xFF8E8E93)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp, max = 90.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFE5E7EB)
                        )
                    )

                    // Image attachment icon
                    IconButton(
                        onClick = onAttachClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Photo",
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Microphone icon
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.MicNone,
                            contentDescription = "Mic",
                            tint = Color(0xFFD1D5DB),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Round Send Action Button (Circle with >>)
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        if (isAITyping) onStopGeneration() else onSend()
                    },
                shape = CircleShape,
                color = Color(0xFF262629),
                border = BorderStroke(0.5.dp, Color(0xFF38383C))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isAITyping) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        ChatCustomIcons.CaiDoubleChevron(
                            size = 20.dp,
                            color = Color(0xFFE5E7EB)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // C.AI Disclaimer bar matching screenshot
        var disclaimerExpanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { disclaimerExpanded = !disclaimerExpanded }
                .padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Это ИИ, а не реальный человек. Рассматривайте всё, что он гов...",
                fontSize = 10.sp,
                color = Color(0xFF8E8E93),
                maxLines = if (disclaimerExpanded) Int.MAX_VALUE else 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                if (disclaimerExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = Color(0xFF8E8E93)
            )
        }
    }
}

@Composable
fun JanitorAiInputBar(
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
    reasoningLevelName: String,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onReasoningClick: () -> Unit,
    isAITyping: Boolean,
    onStopGeneration: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left (+) button with round dark circle
        Surface(
            modifier = Modifier
                .size(42.dp)
                .clickable { onAttachClick() },
            shape = CircleShape,
            color = Color(0xFF262629)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color(0xFFE5E7EB),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Center Input capsule
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF222328),
            border = BorderStroke(0.5.dp, Color(0xFF33353E))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = {
                        Text(
                            "Нажмите кнопку, чтобы отпр...",
                            fontSize = 14.sp,
                            color = Color(0xFF888B96),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp, max = 90.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color(0xFFE5E7EB)
                    )
                )

                // Gauge / Speedometer reasoning icon inside capsule
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onReasoningClick() },
                    shape = CircleShape,
                    color = Color(0xFF333640)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ChatCustomIcons.JanitorGauge(
                            size = 18.dp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Send button (purple round button with up arrow)
        Surface(
            modifier = Modifier
                .size(42.dp)
                .clickable {
                    if (isAITyping) onStopGeneration() else onSend()
                },
            shape = CircleShape,
            color = if (isAITyping) MaterialTheme.colorScheme.error else Color(0xFF6B58A6) // Purple matching Janitor screenshot
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isAITyping) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
