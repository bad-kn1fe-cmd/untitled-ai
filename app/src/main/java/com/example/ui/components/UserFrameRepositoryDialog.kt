package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.HalloweenFrameHelper
import org.json.JSONArray
import org.json.JSONObject

data class UserFrameItem(
    val id: String,
    val name: String,
    val uri: String,
    val isCustom: Boolean = false,
    val isHalloween: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

object UserFrameStorageHelper {

    fun parseFramesJson(json: String, context: android.content.Context): List<UserFrameItem> {
        val list = mutableListOf<UserFrameItem>()

        // Default item: No frame
        list.add(UserFrameItem(id = "none", name = "Без рамки", uri = "", isCustom = false))

        try {
            val array = JSONArray(json.ifBlank { "[]" })
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", "frame_$i")
                val name = obj.optString("name", "Рамка")
                val uri = obj.optString("uri", "")
                val isCustom = obj.optBoolean("is_custom", false)
                val isHalloween = obj.optBoolean("is_halloween", false)
                val addedAt = obj.optLong("added_at", System.currentTimeMillis())

                if (uri.isNotBlank() && list.none { it.uri == uri || it.id == id }) {
                    list.add(
                        UserFrameItem(
                            id = id,
                            name = name,
                            uri = uri,
                            isCustom = isCustom,
                            isHalloween = isHalloween,
                            addedAt = addedAt
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return list
    }

    fun framesToJson(frames: List<UserFrameItem>): String {
        val array = JSONArray()
        for (item in frames) {
            if (item.id == "none" || item.uri.isBlank()) continue
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("uri", item.uri)
            obj.put("is_custom", item.isCustom)
            obj.put("is_halloween", item.isHalloween)
            obj.put("added_at", item.addedAt)
            array.put(obj)
        }
        return array.toString()
    }
}

@Composable
fun UserFrameRepositoryDialog(
    userAvatarUri: String,
    currentSelectedFrameUri: String,
    rawFramesJson: String,
    onDismiss: () -> Unit,
    onSelectFrame: (frameUri: String) -> Unit,
    onSaveFramesJson: (updatedJson: String) -> Unit
) {
    val context = LocalContext.current
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    val framesList = remember(rawFramesJson) {
        UserFrameStorageHelper.parseFramesJson(rawFramesJson, context).toMutableStateList()
    }

    var activeSelectedUri by remember { mutableStateOf(currentSelectedFrameUri) }

    // Adjuster dialog state for fitting newly chosen frame
    var pendingAdjustFrameSource by remember { mutableStateOf<String?>(null) }

    // Picker for adding a custom frame from gallery
    val pickFrameLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingAdjustFrameSource = uri.toString()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FilterFrames,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEn) "Frames Vault" else "Хранилище рамок",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEn) "Dedicated vault of frames for creating & editing bots" else "Отдельное хранилище рамок для создания и редактирования ботов",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview Showcase
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEn) "Live Avatar Preview" else "Предпросмотр аватара",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Big interactive avatar with selected frame (supports 3D parallax if enabled!)
                        AvatarImage(
                            uri = userAvatarUri,
                            contentDescription = "User Avatar Preview",
                            size = 100.dp,
                            frameUri = activeSelectedUri
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        val currentFrameName = framesList.find { it.uri == activeSelectedUri }?.name
                            ?: if (activeSelectedUri.isBlank()) (if (isEn) "No frame" else "Без рамки") else (if (isEn) "Custom frame" else "Своя рамка")
                        Text(
                            text = currentFrameName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (FrameParallaxController.isEnabled) {
                            Text(
                                text = if (isEn) "✨ 3D Parallax active: tilt phone to view" else "✨ 3D Параллакс активен: наклоните телефон",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar: Add Frame Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEn) "Available Frames (${framesList.size})" else "Доступные рамки (${framesList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            pickFrameLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEn) "Add Frame" else "Добавить рамку", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Frames Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(framesList, key = { it.id }) { item ->
                        val isSelected = activeSelectedUri == item.uri
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    activeSelectedUri = item.uri
                                    onSelectFrame(item.uri)
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.uri.isBlank()) {
                                        Surface(
                                            modifier = Modifier.size(52.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Block,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    } else {
                                        AvatarImage(
                                            uri = userAvatarUri,
                                            contentDescription = item.name,
                                            size = 64.dp,
                                            frameUri = item.uri
                                        )
                                    }

                                    if (isSelected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )

                                if (item.isHalloween) {
                                    Text(
                                        text = "🎃 Event",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFF9E00),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Delete option for custom user-added frames
                                if (item.isCustom) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    IconButton(
                                        onClick = {
                                            framesList.remove(item)
                                            if (activeSelectedUri == item.uri) {
                                                activeSelectedUri = ""
                                                onSelectFrame("")
                                            }
                                            onSaveFramesJson(UserFrameStorageHelper.framesToJson(framesList))
                                            Toast.makeText(context, if (isEn) "Frame deleted" else "Рамка удалена", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Удалить",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom apply button
                Button(
                    onClick = {
                        onSelectFrame(activeSelectedUri)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isEn) "Apply Frame" else "Применить рамку", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Secondary Dialog: Fitting & Adjusting newly picked custom PNG frame
    if (pendingAdjustFrameSource != null) {
        AvatarFrameAdjusterDialog(
            avatarUri = userAvatarUri,
            initialFrameSource = pendingAdjustFrameSource!!,
            onDismiss = { pendingAdjustFrameSource = null },
            onSaveFrame = { savedFileUri, savedBase64, _, _, _ ->
                val chosenUri = savedFileUri.ifBlank { savedBase64 }
                val newFrame = UserFrameItem(
                    id = "custom_${System.currentTimeMillis()}",
                    name = if (isEn) "Custom Frame #${framesList.count { it.isCustom } + 1}" else "Своя рамка #${framesList.count { it.isCustom } + 1}",
                    uri = chosenUri,
                    isCustom = true
                )
                framesList.add(newFrame)
                val updatedJson = UserFrameStorageHelper.framesToJson(framesList)
                onSaveFramesJson(updatedJson)
                activeSelectedUri = chosenUri
                onSelectFrame(chosenUri)
                pendingAdjustFrameSource = null
                Toast.makeText(context, if (isEn) "New frame added to vault!" else "Новая рамка добавлена в хранилище!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
