package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AvailableBaseFonts
import com.example.ui.theme.FontHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontPickerDialog(
    currentChoice: String,
    customFontName: String,
    glassmorphismEnabled: Boolean,
    wallpaperTone: String,
    onSelectChoice: (String) -> Unit,
    onCustomFontImported: (path: String, name: String) -> Unit,
    onToggleGlassmorphism: (Boolean) -> Unit,
    onSelectWallpaperTone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    val fontFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val res = FontHelper.importCustomFont(context, uri)
            if (res != null) {
                onCustomFontImported(res.first, res.second)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(if (isEn) "Fonts & Typography" else "Шрифты и Стилизация", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(if (isEn) "Choose application typeface" else "Выбор гарнитуры приложения", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = if (isEn) "Application font typeface:" else "Гарнитура текста приложения:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(AvailableBaseFonts) { fontOption ->
                    val isSelected = currentChoice == fontOption.id
                    val name = if (isEn) {
                        when (fontOption.id) {
                            "system" -> "System (Default)"
                            "montserrat" -> "Montserrat (Modern Sans)"
                            "playfair" -> "Playfair Display (Serif)"
                            "roboto_mono" -> "Roboto Mono (Monospace)"
                            "serif" -> "Serif (Classic)"
                            "sans_serif" -> "Sans-Serif"
                            else -> fontOption.nameRu
                        }
                    } else fontOption.nameRu

                    val desc = if (isEn) {
                        when (fontOption.id) {
                            "system" -> "Standard Android system font"
                            "montserrat" -> "Clean geometric sans-serif with excellent readability"
                            "playfair" -> "Elegant literary font for deep roleplay immersion"
                            "roboto_mono" -> "Crisp monospace font in cyberpunk and console style"
                            "serif" -> "Classic serif typeface for reading"
                            "sans_serif" -> "Simple clean sans-serif font"
                            else -> fontOption.descriptionRu
                        }
                    } else fontOption.descriptionRu

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectChoice(fontOption.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectChoice(fontOption.id) }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontFamily = fontOption.fontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Custom font option & import button
                item {
                    val isCustomSelected = currentChoice == "custom"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCustomSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (customFontName.isNotBlank()) {
                                    onSelectChoice("custom")
                                } else {
                                    fontFilePicker.launch("*/*")
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = isCustomSelected,
                                    onClick = {
                                        if (customFontName.isNotBlank()) onSelectChoice("custom")
                                        else fontFilePicker.launch("*/*")
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (customFontName.isNotBlank()) (if (isEn) "Custom font: $customFontName" else "Свой шрифт: $customFontName") else (if (isEn) "Import Custom Font" else "Импортировать свой шрифт"),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.5.sp,
                                        color = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isEn) "Load any .ttf or .otf file from device" else "Загрузить любой файл .ttf или .otf с устройства",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = { fontFilePicker.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (customFontName.isNotBlank()) (if (isEn) "Change .TTF File" else "Заменить .TTF файл") else (if (isEn) "Select .TTF / .OTF File" else "Выбрать .TTF / .OTF файл"), fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Section 2: Transparency (Прозрачность)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEn) "Dialogue Transparency Effect:" else "Эффект прозрачности в диалоге:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(if (isEn) "Adaptive Transparency" else "Адаптивная прозрачность", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        if (isEn) "Header and input panel are semi-transparent and blend with light and dark chat wallpapers" else "Шапка и панель ввода полупрозрачны и гармонируют со светлыми и тёмными обоями чата",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp
                                    )
                                }
                                Switch(
                                    checked = glassmorphismEnabled,
                                    onCheckedChange = { onToggleGlassmorphism(it) }
                                )
                            }

                            if (glassmorphismEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(if (isEn) "Transparency tone for your wallpaper:" else "Тон прозрачности под ваши обои:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    FilterChip(
                                        selected = wallpaperTone == "dark",
                                        onClick = { onSelectWallpaperTone("dark") },
                                        label = { Text(if (isEn) "Dark Tone" else "Тёмный тон", fontSize = 11.sp) },
                                        leadingIcon = if (wallpaperTone == "dark") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = wallpaperTone == "light",
                                        onClick = { onSelectWallpaperTone("light") },
                                        label = { Text(if (isEn) "Light Tone" else "Светлый тон", fontSize = 11.sp) },
                                        leadingIcon = if (wallpaperTone == "light") {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isEn) "Done" else "Готово")
            }
        }
    )
}
