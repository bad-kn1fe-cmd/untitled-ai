package com.example.util

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class PluginWidgetButton(
    val id: String,
    val text: String,
    val onClick: () -> Unit
)

data class PluginWidget(
    val id: String,
    val pluginId: String = "",
    val title: String,
    val content: String,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.4f,
    val widthDp: Float = 240f,
    val buttons: List<PluginWidgetButton> = emptyList(),
    val closeable: Boolean = true
)

data class PluginDialogData(
    val id: String,
    val pluginId: String = "",
    val title: String,
    val message: String,
    val confirmText: String = "OK",
    val cancelText: String? = null,
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {}
)

object PluginUiManager {
    private val _widgets = MutableStateFlow<Map<String, PluginWidget>>(emptyMap())
    val widgets: StateFlow<Map<String, PluginWidget>> = _widgets.asStateFlow()

    private val _activeDialog = MutableStateFlow<PluginDialogData?>(null)
    val activeDialog: StateFlow<PluginDialogData?> = _activeDialog.asStateFlow()

    fun showWidget(widget: PluginWidget) {
        _widgets.value = _widgets.value + (widget.id to widget)
    }

    fun hideWidget(id: String) {
        _widgets.value = _widgets.value - id
    }

    fun clearWidgets() {
        _widgets.value = emptyMap()
    }

    fun showDialog(dialog: PluginDialogData) {
        _activeDialog.value = dialog
    }

    fun dismissDialog() {
        _activeDialog.value = null
    }

    fun clearForPlugin(pluginId: String) {
        if (pluginId.isBlank()) return
        _widgets.value = _widgets.value.filterValues { it.pluginId != pluginId }
        if (_activeDialog.value?.pluginId == pluginId) {
            _activeDialog.value = null
        }
    }

    fun clearAll() {
        _widgets.value = emptyMap()
        _activeDialog.value = null
    }
}

@Composable
fun PluginUiHost(modifier: Modifier = Modifier) {
    val widgets by PluginUiManager.widgets.collectAsState()
    val dialog by PluginUiManager.activeDialog.collectAsState()
    val density = LocalDensity.current

    // 1. Floating Interactive Widgets
    if (widgets.isNotEmpty()) {
        BoxWithConstraints(modifier = modifier.fillMaxSize().zIndex(280f)) {
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }

            widgets.values.forEach { widget ->
                key(widget.id) {
                    var dragOffsetX by remember(widget.id) { mutableFloatStateOf(0f) }
                    var dragOffsetY by remember(widget.id) { mutableFloatStateOf(0f) }

                    val baseXPx = screenWidthPx * widget.xFraction - with(density) { (widget.widthDp / 2).dp.toPx() }
                    val baseYPx = screenHeightPx * widget.yFraction

                    val currentXPx = baseXPx + dragOffsetX
                    val currentYPx = baseYPx + dragOffsetY

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        shadowElevation = 8.dp,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .offset { IntOffset(currentXPx.roundToInt(), currentYPx.roundToInt()) }
                            .width(widget.widthDp.dp)
                            .pointerInput(widget.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    dragOffsetY += dragAmount.y
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = widget.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )

                                if (widget.closeable) {
                                    IconButton(
                                        onClick = { PluginUiManager.hideWidget(widget.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (widget.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = widget.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (widget.buttons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    widget.buttons.forEach { btn ->
                                        FilledTonalButton(
                                            onClick = btn.onClick,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Text(btn.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    // 2. Modal Alert Dialog
    dialog?.let { d ->
        AlertDialog(
            onDismissRequest = {
                d.onCancel()
                PluginUiManager.dismissDialog()
            },
            title = {
                Text(d.title, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(d.message, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        d.onConfirm()
                        PluginUiManager.dismissDialog()
                    }
                ) {
                    Text(d.confirmText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = d.cancelText?.let { cancelLabel ->
                {
                    OutlinedButton(
                        onClick = {
                            d.onCancel()
                            PluginUiManager.dismissDialog()
                        }
                    ) {
                        Text(cancelLabel)
                    }
                }
            }
        )
    }
}
