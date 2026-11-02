package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.RelationshipTableEntity
import com.example.util.RelationshipBoardHelper
import org.json.JSONArray

@Composable
fun RelationshipCard(
    table: RelationshipTableEntity,
    modifier: Modifier = Modifier,
    isInitialExpanded: Boolean = false,
    character: CharacterEntity? = null,
    relationshipViewMode: String = "board",
    customRelationshipTypesJson: String = "[]",
    showBoardCanvasWhenExpanded: Boolean = true
) {
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    var expanded by remember { mutableStateOf(isInitialExpanded) }
    val isBoardMode = relationshipViewMode != "table"

    val keyEventsList = remember(table.keyEvents) {
        try {
            val arr = JSONArray(table.keyEvents)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val openPlotsList = remember(table.openPlotThreads) {
        try {
            val arr = JSONArray(table.openPlotThreads)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    val boardState = remember(table.boardJson, character?.relationshipBoardJson, character?.name, table.status, table.trustScore) {
        val rawJson = table.boardJson.ifBlank { character?.relationshipBoardJson ?: "" }
        RelationshipBoardHelper.parseBoard(
            json = rawJson,
            mainCharName = character?.name?.ifBlank { "Персонаж 1" } ?: "Персонаж 1",
            playerName = "Игрок",
            startingRelationship = character?.startingRelationship ?: "strangers",
            customRelationship = character?.customRelationship ?: "",
            initialTitle = table.status,
            initialPercent = table.trustScore
        )
    }

    val displayScore = remember(isBoardMode, table.trustScore, table.boardJson, boardState) {
        if (isBoardMode && (table.boardJson.isNotBlank() || character?.relationshipBoardJson?.isNotBlank() == true)) {
            boardState.calculateAverageScore()
        } else {
            table.trustScore.coerceIn(-100, 100)
        }
    }

    val displayStatus = remember(isBoardMode, table.status, table.boardJson, boardState) {
        if (isBoardMode && (table.boardJson.isNotBlank() || character?.relationshipBoardJson?.isNotBlank() == true)) {
            boardState.calculateOverallTitle(table.status)
        } else {
            table.status
        }
    }

    val scoreColor = when {
        displayScore >= 40 -> Color(0xFF34C759)
        displayScore <= -20 -> Color(0xFFFF3B30)
        else -> MaterialTheme.colorScheme.primary
    }

    val progressFraction = remember(displayScore, isBoardMode) {
        if (isBoardMode || displayScore < 0) {
            ((displayScore.coerceIn(-100, 100) + 100f) / 200f).coerceIn(0f, 1f)
        } else {
            (displayScore.coerceIn(0, 100) / 100f).coerceIn(0f, 1f)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isBoardMode) Icons.Default.Hub else Icons.Default.Timeline,
                        contentDescription = if (isBoardMode) "Доска отношений" else "Таблица отношений",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBoardMode) {
                            if (isEn) "Relationship Board" else "Доска отношений"
                        } else {
                            if (isEn) "Relationship Table" else "Таблица отношений"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = scoreColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = displayStatus,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = scoreColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sign = if (displayScore > 0) "+" else ""
                    Text(
                        text = "$sign${displayScore}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) (if (isEn) "Collapse" else "Свернуть") else (if (isEn) "Expand" else "Развернуть"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp).padding(start = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Compact Trust Bar (-100%..+100% normalized in Board Mode, 0..100% in Classic Mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = scoreColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = scoreColor,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = table.mood,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isBoardMode && showBoardCanvasWhenExpanded) {
                        RelationshipBoardSection(
                            boardState = boardState,
                            onBoardChange = {},
                            customTypesJson = customRelationshipTypesJson,
                            isReadOnly = true,
                            canvasHeightDp = 300
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (keyEventsList.isNotEmpty()) {
                        Text(
                            text = if (isEn) "Key Milestones & Events:" else "Ключевые события:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        keyEventsList.forEach { ev ->
                            Text(
                                text = "• $ev",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (openPlotsList.isNotEmpty()) {
                        Text(
                            text = if (isEn) "Plot Lines & Threads:" else "Сюжетные линии:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        openPlotsList.forEach { plot ->
                            Text(
                                text = "• $plot",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
