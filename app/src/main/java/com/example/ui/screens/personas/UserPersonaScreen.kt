package com.example.ui.screens.personas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.UserPersonaEntity
import com.example.ui.components.AvatarImage
import com.example.ui.studio.studioElement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPersonaScreen(
    personas: List<UserPersonaEntity>,
    activePersona: UserPersonaEntity?,
    onSavePersona: (UserPersonaEntity) -> Unit,
    onSelectPersona: (Long) -> Unit,
    onDeletePersona: (UserPersonaEntity) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingPersona by remember { mutableStateOf<UserPersonaEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.studioElement("personas_top_bar"),
                title = {
                    Column {
                        Text(if (isEn) "User Personas" else "Персоны пользователя", fontWeight = FontWeight.Bold)
                        Text(
                            if (isEn) "Profile and appearance of your roleplay identity" else "Профиль и внешность вашего персонажа в диалогах",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = if (isEn) "Back" else "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPersona = UserPersonaEntity(
                        name = "",
                        avatarUri = "drawable://avatar_app_dark",
                        age = 22,
                        gender = if (isEn) "Male" else "Мужской",
                        isSelected = personas.isEmpty()
                    )
                    isCreatingNew = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.studioElement("personas_add_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = if (isEn) "Create Persona" else "Создать персону")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                modifier = Modifier
                    .studioElement("personas_filter_chips")
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isEn) "A persona establishes your name, appearance, habits, and traits that AI characters perceive in roleplay." else "Персона задает имя, внешность, привычки и характер, которые ИИ-боты видят и учитывают в ролплее.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (personas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            if (isEn) "No Personas Created" else "Нет созданных персон",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isEn) "Create your persona so bots know how to address you and what you look like." else "Создайте свою персону, чтобы боты знали, как к вам обращаться и как вы выглядите.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                editingPersona = UserPersonaEntity(
                                    name = if (isEn) "Traveler" else "Странник",
                                    avatarUri = "drawable://avatar_app_dark",
                                    isSelected = true
                                )
                                isCreatingNew = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEn) "Create Persona" else "Создать персону")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(personas, key = { it.id }) { persona ->
                        val isActive = persona.isSelected || (activePersona?.id == persona.id)
                        UserPersonaCard(
                            persona = persona,
                            isActive = isActive,
                            onSelect = { onSelectPersona(persona.id) },
                            onEdit = {
                                editingPersona = persona
                                isCreatingNew = false
                            },
                            onDelete = { onDeletePersona(persona) },
                            canDelete = personas.size > 1
                        )
                    }
                }
            }
        }
    }

    editingPersona?.let { personaToEdit ->
        UserPersonaEditorDialog(
            persona = personaToEdit,
            isNew = isCreatingNew,
            onDismiss = { editingPersona = null },
            onSave = { updated ->
                onSavePersona(updated)
                editingPersona = null
            }
        )
    }
}

@Composable
fun UserPersonaCard(
    persona: UserPersonaEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
    modifier: Modifier = Modifier
) {
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() },
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(
            if (isActive) 2.dp else 1.dp,
            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarImage(
                    uri = persona.avatarUri,
                    contentDescription = persona.name,
                    size = 52.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = persona.name.ifBlank { if (isEn) "Unnamed" else "Без имени" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (isActive) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = if (isEn) "ACTIVE" else "АКТИВНАЯ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val details = buildList {
                        add(if (isEn) "${persona.age} y/o" else "${persona.age} лет")
                        if (persona.gender.isNotBlank()) add(persona.gender)
                    }.joinToString(" • ")

                    Text(
                        text = details,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (persona.shortDescription.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = persona.shortDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                RadioButton(
                    selected = isActive,
                    onClick = onSelect
                )
            }

            if (persona.appearance.isNotBlank() || persona.personality.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))

                if (persona.appearance.isNotBlank()) {
                    Text(
                        text = "${if (isEn) "Appearance" else "Внешность"}: ${persona.appearance}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (persona.personality.isNotBlank()) {
                    Text(
                        text = "${if (isEn) "Personality" else "Характер"}: ${persona.personality}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = if (isEn) "Edit" else "Редактировать",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isEn) "Delete" else "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserPersonaEditorDialog(
    persona: UserPersonaEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (UserPersonaEntity) -> Unit
) {
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    var name by remember { mutableStateOf(persona.name) }
    var avatarUri by remember { mutableStateOf(persona.avatarUri) }
    var age by remember { mutableIntStateOf(persona.age) }
    var gender by remember { mutableStateOf(persona.gender) }
    var shortDescription by remember { mutableStateOf(persona.shortDescription) }
    var appearance by remember { mutableStateOf(persona.appearance) }
    var personality by remember { mutableStateOf(persona.personality) }
    var habits by remember { mutableStateOf(persona.habits) }
    var likes by remember { mutableStateOf(persona.likes) }
    var dislikes by remember { mutableStateOf(persona.dislikes) }
    var isSelected by remember { mutableStateOf(persona.isSelected) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = com.example.util.ImageStorageHelper.uriToBase64(context, uri)
            avatarUri = base64 ?: uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
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
                    Text(
                        text = if (isNew) {
                            if (isEn) "Create Persona" else "Создание персоны"
                        } else {
                            if (isEn) "Edit Persona" else "Редактирование персоны"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Avatar Picker
                    Text(if (isEn) "Persona Avatar" else "Аватар персоны", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AvatarImage(uri = avatarUri, contentDescription = name, size = 64.dp)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Default Dark App Logo
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF14141E))
                                        .border(
                                            if (avatarUri == "drawable://avatar_app_dark") 2.dp else 1.dp,
                                            if (avatarUri == "drawable://avatar_app_dark") MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                        .clickable { avatarUri = "drawable://avatar_app_dark" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("AI", color = Color(0xFF8E8BFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // Default Light App Logo
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF3F3FA))
                                        .border(
                                            if (avatarUri == "drawable://avatar_app_light") 2.dp else 1.dp,
                                            if (avatarUri == "drawable://avatar_app_light") MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                        .clickable { avatarUri = "drawable://avatar_app_light" },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("AI", color = Color(0xFF5350C7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isEn) "Upload Photo" else "Загрузить фото", fontSize = 12.sp)
                            }
                        }
                    }

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isEn) "Persona Name / Your Name *" else "Имя персоны / Ваше имя *") },
                        placeholder = { Text(if (isEn) "e.g. Alex, Rain, Nathan" else "Например: Александр, Alex, Рейн") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Age & Gender
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = age.toString(),
                            onValueChange = { age = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 20 },
                            label = { Text(if (isEn) "Age" else "Возраст") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = gender,
                            onValueChange = { gender = it },
                            label = { Text(if (isEn) "Gender" else "Пол") },
                            placeholder = { Text(if (isEn) "Male/Female" else "Муж/Жен") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Short Description
                    OutlinedTextField(
                        value = shortDescription,
                        onValueChange = { shortDescription = it },
                        label = { Text(if (isEn) "Short Description / Role" else "Краткое описание / Профессия / Роль") },
                        placeholder = { Text(if (isEn) "e.g. Architecture student, wanderer, companion" else "Например: Студент архитектурного, странник, напарник") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Appearance
                    OutlinedTextField(
                        value = appearance,
                        onValueChange = { appearance = it },
                        label = { Text(if (isEn) "Appearance" else "Внешность") },
                        placeholder = { Text(if (isEn) "Height, build, hair & eye color, clothing style..." else "Рост, телосложение, цвет волос и глаз, стиль одежды...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Personality
                    OutlinedTextField(
                        value = personality,
                        onValueChange = { personality = it },
                        label = { Text(if (isEn) "Personality & Behavior" else "Характер и поведение") },
                        placeholder = { Text(if (isEn) "Calm, sarcastic, caring, determined..." else "Спокойный, саркастичный, заботливый, упрямый...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Habits
                    OutlinedTextField(
                        value = habits,
                        onValueChange = { habits = it },
                        label = { Text(if (isEn) "Habits & Quirks" else "Привычки и особенности") },
                        placeholder = { Text(if (isEn) "Drinks coffee in mornings, spins ring on finger..." else "Пьет кофе по утрам, крутит кольцо на пальце...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Likes & Dislikes
                    OutlinedTextField(
                        value = likes,
                        onValueChange = { likes = it },
                        label = { Text(if (isEn) "Likes / Preferences" else "Что любит / Предпочтения") },
                        placeholder = { Text(if (isEn) "Music, rain, walking, books..." else "Музыка, дождь, прогулки, книги...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = dislikes,
                        onValueChange = { dislikes = it },
                        label = { Text(if (isEn) "Dislikes / Pet Peeves" else "Что не любит / Раздражает") },
                        placeholder = { Text(if (isEn) "Hypocrisy, loud noise, cold..." else "Лицемерие, громкие звуки, холод...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSelected = !isSelected }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = { isSelected = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEn) "Make active default persona" else "Сделать активной персоной по умолчанию", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isEn) "Cancel" else "Отмена")
                    }

                    Button(
                        onClick = {
                            val defaultName = if (isEn) "Traveler" else "Странник"
                            val saved = persona.copy(
                                name = name.trim().ifBlank { defaultName },
                                avatarUri = avatarUri,
                                age = age.coerceIn(1, 999),
                                gender = gender.trim(),
                                shortDescription = shortDescription.trim(),
                                appearance = appearance.trim(),
                                personality = personality.trim(),
                                habits = habits.trim(),
                                likes = likes.trim(),
                                dislikes = dislikes.trim(),
                                isSelected = isSelected
                            )
                            onSave(saved)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isEn) "Save" else "Сохранить")
                    }
                }
            }
        }
    }
}
