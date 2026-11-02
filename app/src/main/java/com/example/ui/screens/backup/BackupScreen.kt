package com.example.ui.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.BackupManifest
import com.example.data.repository.SelectiveExportOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onExportBackup: suspend (Uri, SelectiveExportOptions) -> Result<BackupManifest>,
    onInspectBackup: suspend (Uri) -> Result<BackupManifest>,
    onImportBackup: suspend (Uri, Boolean) -> Result<Unit>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isOperating by remember { mutableStateOf(false) }

    var exportOptions by remember { mutableStateOf(SelectiveExportOptions()) }
    var inspectResult by remember { mutableStateOf<Pair<Uri, BackupManifest>?>(null) }
    var importReplaceAll by remember { mutableStateOf(false) }
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    // Launcher for creating ZIP export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isOperating = true
                statusMessage = if (isEn) "Creating selective backup archive..." else "Создание выборочного архива резервной копии..."
                val res = onExportBackup(uri, exportOptions)
                statusMessage = if (res.isSuccess) {
                    val m = res.getOrNull()
                    if (isEn) "Export completed!\nCharacters: ${m?.characterCount}, Chats: ${m?.chatCount}, Messages: ${m?.messageCount}, Personas: ${m?.personaCount}, Stories: ${m?.storyCount}"
                    else "Экспорт успешно завершен!\nСохранено персонажей: ${m?.characterCount}, диалогов: ${m?.chatCount}, сообщений: ${m?.messageCount}, персон: ${m?.personaCount}, историй: ${m?.storyCount}"
                } else {
                    if (isEn) "Export error: ${res.exceptionOrNull()?.message}" else "Ошибка экспорта: ${res.exceptionOrNull()?.message}"
                }
                isOperating = false
            }
        }
    }

    // Launcher for selecting ZIP import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isOperating = true
                statusMessage = if (isEn) "Analyzing backup archive..." else "Анализ архива резервной копии..."
                val inspectRes = onInspectBackup(uri)
                if (inspectRes.isSuccess) {
                    inspectResult = uri to inspectRes.getOrNull()!!
                    statusMessage = null
                } else {
                    statusMessage = if (isEn) "Failed to read backup: ${inspectRes.exceptionOrNull()?.message}" else "Не удалось прочитать архив: ${inspectRes.exceptionOrNull()?.message}"
                }
                isOperating = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isEn) "Backup & Restoration (ZIP)" else "Резервные копии (ZIP)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isEn) "Back" else "Назад")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(if (isEn) "Selective Data Export" else "Выборочный экспорт данных", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isEn) "Select which sections to include in ZIP archive:" else "Выберите, какие разделы включить в ZIP-архив:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Checkboxes for each data category
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BackupOptionRow(
                                title = if (isEn) "Characters (Bots)" else "Персонажи (боты)",
                                subtitle = if (isEn) "Includes descriptions, avatars, scenarios and tags" else "Включает описания, аватары, сценарии и теги",
                                checked = exportOptions.includeCharacters,
                                onCheckedChange = { exportOptions = exportOptions.copy(includeCharacters = it) }
                            )
                            BackupOptionRow(
                                title = if (isEn) "Dialogues and Messages" else "Диалоги и сообщения",
                                subtitle = if (isEn) "Chat branches, chronicle memory and relationship history" else "Ветки чатов, память хроник и история отношений",
                                checked = exportOptions.includeChatsAndMessages,
                                onCheckedChange = { exportOptions = exportOptions.copy(includeChatsAndMessages = it) }
                            )
                            BackupOptionRow(
                                title = if (isEn) "User Personas" else "Персоны пользователя",
                                subtitle = if (isEn) "Profiles, avatars and descriptions of who you converse as" else "Профили, аватары и описания от чьего лица ведется диалог",
                                checked = exportOptions.includePersonas,
                                onCheckedChange = { exportOptions = exportOptions.copy(includePersonas = it) }
                            )
                            BackupOptionRow(
                                title = if (isEn) "Stories and Worlds (Bot Groups)" else "Истории и Миры (Группы ботов)",
                                subtitle = if (isEn) "Character collections, lore and world rules" else "Коллекции персонажей, лор и правила миров",
                                checked = exportOptions.includeStories,
                                onCheckedChange = { exportOptions = exportOptions.copy(includeStories = it) }
                            )
                            BackupOptionRow(
                                title = if (isEn) "AI Provider Profiles" else "Профили AI-провайдеров",
                                subtitle = if (isEn) "Endpoints, models, temperature and settings" else "Эндпоинты, модели, температура и настройки",
                                checked = exportOptions.includeProviders,
                                onCheckedChange = { exportOptions = exportOptions.copy(includeProviders = it) }
                            )
                            BackupOptionRow(
                                title = if (isEn) "Settings and Themes" else "Настройки и темы оформления",
                                subtitle = if (isEn) "Color scheme, themes, font sizes and chat parameters" else "Цветовая гамма, темы, размеры шрифта и параметры чата",
                                checked = exportOptions.includeSettings,
                                onCheckedChange = { exportOptions = exportOptions.copy(includeSettings = it) }
                            )
                            if (exportOptions.includeProviders) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp, top = 2.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isEn) "Include secret API keys" else "Включать секретные API-ключи", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text(if (isEn) "Uncheck for safe backup sharing with friends" else "Снимите галочку для безопасной передачи бэкапа друзьям", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Checkbox(
                                        checked = exportOptions.includeApiKeys,
                                        onCheckedChange = { exportOptions = exportOptions.copy(includeApiKeys = it) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                exportLauncher.launch("ai_roleplay_backup_${System.currentTimeMillis()}.zip")
                            },
                            enabled = !isOperating,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEn) "Export to ZIP" else "Экспортировать в ZIP")
                        }
                    }
                }
            }

            // Import Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(if (isEn) "Import from ZIP Archive" else "Импорт из ZIP-архива", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isEn) "Restore characters, stories and dialogues from backup archive. A detailed summary will be shown before importing." else "Восстановление персонажей, историй и диалогов из резервной копии. Перед импортом будет показана подробная сводка содержимого архива.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            },
                            enabled = !isOperating,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEn) "Select ZIP File to Import" else "Выбрать ZIP-файл для импорта")
                        }
                    }
                }
            }

            // Status message
            if (statusMessage != null || isOperating) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isOperating) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Text(
                                text = statusMessage ?: (if (isEn) "Processing..." else "Выполняется..."),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    // Inspect & Confirm Import Dialog
    if (inspectResult != null) {
        val (uri, manifest) = inspectResult!!
        AlertDialog(
            onDismissRequest = { inspectResult = null },
            icon = { Icon(Icons.Default.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp)) },
            title = { Text(if (isEn) "Backup Contents" else "Содержимое резервной копии", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (isEn) "Found in archive:" else "В архиве найдено:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (isEn) "• Characters: ${manifest.characterCount}" else "• Персонажей: ${manifest.characterCount}")
                    Text(if (isEn) "• Dialogues: ${manifest.chatCount}" else "• Диалогов: ${manifest.chatCount}")
                    Text(if (isEn) "• Messages: ${manifest.messageCount}" else "• Сообщений: ${manifest.messageCount}")
                    Text(if (isEn) "• Personas: ${manifest.personaCount}" else "• Персон: ${manifest.personaCount}")
                    Text(if (isEn) "• Stories / Worlds: ${manifest.storyCount}" else "• Историй / Миров: ${manifest.storyCount}")
                    Text(if (isEn) "• Comics: ${manifest.comicCount}" else "• Комиксов: ${manifest.comicCount}")
                    Text(if (isEn) "• AI Providers: ${manifest.providerCount}" else "• AI-провайдеров: ${manifest.providerCount}")
                    Text(if (isEn) "• Settings and themes: ${if (manifest.hasSettings) "Yes" else "No"}" else "• Настройки и темы оформления: ${if (manifest.hasSettings) "Да" else "Нет"}")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(if (isEn) "Import mode:" else "Режим импорта:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (importReplaceAll) (if (isEn) "Replace all current data" else "Заменить все текущие данные") else (if (isEn) "Merge (add to existing)" else "Объединить (добавить к текущим)"), style = MaterialTheme.typography.bodySmall)
                        Switch(
                            checked = importReplaceAll,
                            onCheckedChange = { importReplaceAll = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toImportUri = uri
                        val replace = importReplaceAll
                        inspectResult = null
                        coroutineScope.launch {
                            isOperating = true
                            statusMessage = if (isEn) "Importing data..." else "Импорт данных..."
                            val res = onImportBackup(toImportUri, replace)
                            statusMessage = if (res.isSuccess) {
                                if (isEn) "Import successful! All data restored." else "Импорт успешно завершен! Все данные восстановлены."
                            } else {
                                if (isEn) "Import error: ${res.exceptionOrNull()?.message}" else "Ошибка импорта: ${res.exceptionOrNull()?.message}"
                            }
                            isOperating = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(if (isEn) "Import" else "Импортировать")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { inspectResult = null }) {
                    Text(if (isEn) "Cancel" else "Отмена")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun BackupOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

