package com.example.ui.screens.relationship

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.RelationshipTableEntity
import com.example.data.local.entity.SummaryEntity
import com.example.ui.components.RelationshipBoardSection
import com.example.ui.components.RelationshipCard
import com.example.ui.components.SummaryCard
import com.example.util.RelationshipBoardHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipHistoryScreen(
    characterName: String,
    relationships: List<RelationshipTableEntity>,
    summaries: List<SummaryEntity>,
    onBack: () -> Unit,
    character: CharacterEntity? = null,
    relationshipViewMode: String = "board",
    customRelationshipTypesJson: String = "[]",
    modifier: Modifier = Modifier
) {
    val appLang = com.example.util.LocalAppLanguage.current
    val isEn = appLang.equals("en", ignoreCase = true)
    val isFemboy = appLang.equals("femboy", ignoreCase = true)

    var selectedTab by remember { mutableIntStateOf(0) }
    val isBoardMode = relationshipViewMode != "table"
    val latestRelationship = relationships.firstOrNull()

    val displayCharName = character?.getDisplayName(appLang) ?: characterName

    val currentBoardState = remember(
        latestRelationship?.boardJson,
        latestRelationship?.status,
        latestRelationship?.trustScore,
        character?.relationshipBoardJson,
        displayCharName
    ) {
        val rawJson = latestRelationship?.boardJson?.ifBlank { character?.relationshipBoardJson ?: "" }
            ?: (character?.relationshipBoardJson ?: "")
        RelationshipBoardHelper.parseBoard(
            json = rawJson,
            mainCharName = displayCharName,
            playerName = if (isFemboy) "Фембойчик" else if (isEn) "Player" else "Игрок",
            startingRelationship = character?.startingRelationship ?: "strangers",
            customRelationship = character?.customRelationship ?: "",
            initialTitle = latestRelationship?.status ?: (character?.initialRelationshipTitle ?: ""),
            initialPercent = latestRelationship?.trustScore ?: (character?.initialRelationshipPercent ?: -999)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isFemboy) (if (isBoardMode) "Доска милоты и Память" else "Таблица отношений и Память") else if (isEn) (if (isBoardMode) "Relationship Board & Memory" else "Relationship Table & Memory") else (if (isBoardMode) "Доска отношений и Память" else "Таблица отношений и Память"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (isFemboy) "Персик: $displayCharName" else if (isEn) "Character: $displayCharName" else "Персонаж: $displayCharName",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isBoardMode) Icons.Default.Hub else Icons.Default.Timeline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isFemboy) (if (isBoardMode) "Доска милоты" else "Таблица отношений (${relationships.size})") else if (isEn) (if (isBoardMode) "Relationship Board" else "Relationship Table (${relationships.size})") else (if (isBoardMode) "Доска отношений" else "Таблица отношений (${relationships.size})"))
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Chronicles (${summaries.size})" else "Хроники (${summaries.size})")
                        }
                    }
                )
            }

            when (selectedTab) {
                0 -> {
                    if (isBoardMode) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item(key = "active_relationship_board") {
                                RelationshipBoardSection(
                                    boardState = currentBoardState,
                                    onBoardChange = {},
                                    customTypesJson = customRelationshipTypesJson,
                                    isReadOnly = true,
                                    canvasHeightDp = 380
                                )
                            }

                            if (relationships.isNotEmpty()) {
                                item(key = "history_section_header") {
                                    Text(
                                        text = if (isFemboy) "История милоты и события (${relationships.size})" else if (isEn) "Relationship dynamics history (${relationships.size})" else "История динамики и ключевые события (${relationships.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                itemsIndexed(
                                    items = relationships,
                                    key = { _, rel -> rel.id }
                                ) { index, rel ->
                                    RelationshipCard(
                                        table = rel,
                                        character = character,
                                        relationshipViewMode = "board",
                                        customRelationshipTypesJson = customRelationshipTypesJson,
                                        showBoardCanvasWhenExpanded = false,
                                        isInitialExpanded = index == 0
                                    )
                                }
                            }
                        }
                    } else {
                        if (relationships.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isEn) "Relationship table is being formed as dialogue progresses." else "Таблица отношений пока формируется в процессе диалога.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                               contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(
                                    items = relationships,
                                    key = { it.id }
                                ) { rel ->
                                    RelationshipCard(
                                        table = rel,
                                        character = character,
                                        relationshipViewMode = "table",
                                        customRelationshipTypesJson = customRelationshipTypesJson,
                                        isInitialExpanded = true
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (summaries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFemboy) "Хроники чатика генерируются автоматически каждые 18 сообщений." else if (isEn) "Plot chronicles are generated automatically every 18 messages." else "Хроники сюжета генерируются автоматически каждые 18 сообщений.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = summaries,
                                key = { it.id }
                            ) { sum ->
                                SummaryCard(summary = sum, isInitialExpanded = true)
                            }
                        }
                    }
                }
            }
        }
    }
}
