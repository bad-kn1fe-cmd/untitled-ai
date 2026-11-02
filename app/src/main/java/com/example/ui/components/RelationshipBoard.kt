package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BoardEdge
import com.example.util.BoardNode
import com.example.util.RelationshipBoardHelper
import com.example.util.RelationshipBoardState
import com.example.util.RelationshipTypeItem
import kotlin.math.*

/**
 * Capsule layout bounding box for a character or player node.
 * Dynamically expands based on text width so names never overflow.
 */
private data class NodeCapsule(
    val node: BoardNode,
    val center: Offset,
    val width: Float,
    val height: Float,
    val isPlayer: Boolean = false
) {
    fun contains(point: Offset): Boolean {
        val extra = if (isPlayer) 12f else 8f
        return point.x >= center.x - width / 2f - extra &&
               point.x <= center.x + width / 2f + extra &&
               point.y >= center.y - height / 2f - extra &&
               point.y <= center.y + height / 2f + extra
    }
}

/**
 * Pair of connecting nodes for edge editing dialog.
 */
private data class EdgePairInfo(
    val nodeA: BoardNode,
    val nodeB: BoardNode,
    val edgeAB: BoardEdge?, // attitude of A -> B
    val edgeBA: BoardEdge?  // attitude of B -> A
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationshipBoardSection(
    boardState: RelationshipBoardState,
    onBoardChange: (RelationshipBoardState) -> Unit,
    customTypesJson: String = "[]",
    onCustomTypesChange: ((String) -> Unit)? = null,
    isReadOnly: Boolean = false,
    canvasHeightDp: Int = 420,
    modifier: Modifier = Modifier
) {
    val allTypes = remember(customTypesJson) {
        RelationshipBoardHelper.getAllTypes(customTypesJson)
    }
    var selectedType by remember(allTypes) {
        mutableStateOf(allTypes.firstOrNull { it.id == "neutral" } ?: allTypes.first())
    }

    // Zoom & pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Connection creation state (drag or tap-tap)
    var activeSourceNodeId by remember { mutableStateOf<String?>(null) }
    var dragSourceNodeId by remember { mutableStateOf<String?>(null) }
    var dragCurrentWorldPos by remember { mutableStateOf<Offset?>(null) }

    // Dialogs
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<BoardNode?>(null) }
    var editingEdgePair by remember { mutableStateOf<EdgePairInfo?>(null) }
    var showAddCustomTypeDialog by remember { mutableStateOf(false) }

    val currentBoardState by rememberUpdatedState(boardState)
    val currentSelectedType by rememberUpdatedState(selectedType)
    val currentReadOnly by rememberUpdatedState(isReadOnly)

    val textMeasurer = rememberTextMeasurer()

    // Identify the Player node: role == "player" or id == "node_player" or name == "Игрок"
    val playerNode = remember(boardState.nodes) {
        boardState.nodes.find { it.role == "player" || it.id == "node_player" || it.name.equals("Игрок", ignoreCase = true) }
            ?: boardState.nodes.firstOrNull()
            ?: BoardNode(id = "node_player", name = "Игрок", role = "player")
    }

    val avgScore = remember(boardState) {
        boardState.calculateAverageScore()
    }
    val avgLabel = remember(boardState, avgScore) {
        boardState.calculateOverallTitle()
    }

    // Cleanup activeSourceNodeId when nodes are removed
    LaunchedEffect(boardState.nodes) {
        if (activeSourceNodeId != null && boardState.nodes.none { it.id == activeSourceNodeId }) {
            activeSourceNodeId = null
        }
    }

    fun deleteNode(nodeId: String) {
        val updatedNodes = currentBoardState.nodes.filterNot { it.id == nodeId }
        val updatedEdges = currentBoardState.edges.filterNot { it.fromId == nodeId || it.toId == nodeId }
        if (activeSourceNodeId == nodeId) activeSourceNodeId = null
        onBoardChange(currentBoardState.copy(nodes = updatedNodes, edges = updatedEdges))
    }

    fun applyConnection(fromId: String, toId: String, type: RelationshipTypeItem) {
        if (fromId == toId) return
        val updatedEdges = currentBoardState.edges.toMutableList()
        val existingIdx = updatedEdges.indexOfFirst { it.fromId == fromId && it.toId == toId }
        if (existingIdx >= 0) {
            val existing = updatedEdges[existingIdx]
            updatedEdges[existingIdx] = existing.copy(
                typeId = type.id,
                label = type.label,
                colorHex = type.colorHex,
                score = type.defaultScore
            )
        } else {
            updatedEdges.add(
                BoardEdge(
                    fromId = fromId,
                    toId = toId,
                    typeId = type.id,
                    label = type.label,
                    colorHex = type.colorHex,
                    score = type.defaultScore,
                    description = ""
                )
            )
        }
        onBoardChange(currentBoardState.copy(edges = updatedEdges))
    }

    val appLang = com.example.util.LocalAppLanguage.current
    val isEn = appLang.equals("en", ignoreCase = true)
    val isFemboy = appLang.equals("femboy", ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF13141A))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        // 1. Header: Title, Player status, node count, and "+ Персонаж" action
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isFemboy) "Доска милоты" else if (isEn) "Relationship Board" else "Доска отношений",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFFFFD54F).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isFemboy) "ФЕМБОЙЧИК В ЦЕНТРЕ" else if (isEn) "PLAYER IN CENTER" else "ИГРОК В ЦЕНТРЕ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isEn) "(${boardState.nodes.size} names)" else "(${boardState.nodes.size} имён)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                }

                val scoreColor = when {
                    avgScore >= 25 -> Color(0xFF66BB6A)
                    avgScore <= -25 -> Color(0xFFEF5350)
                    else -> Color(0xFFBDBDBD)
                }
                Text(
                    text = if (isFemboy) "Средний фон милоты: ${if (avgScore > 0) "+$avgScore" else "$avgScore"}% ($avgLabel)" else if (isEn) "Average relationship score: ${if (avgScore > 0) "+$avgScore" else "$avgScore"}% ($avgLabel)" else "Средний фон отношений: ${if (avgScore > 0) "+$avgScore" else "$avgScore"}% ($avgLabel)",
                    style = MaterialTheme.typography.bodySmall,
                    color = scoreColor
                )
            }

            if (!isReadOnly) {
                FilledTonalButton(
                    onClick = { showAddNodeDialog = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isEn) "Character" else "Персонаж", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 2. Palette: Type selection (Любовь, Друзья, Симпатия, Знакомые, Неприязнь, Враги, + Свой)
        if (!isReadOnly) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                allTypes.forEach { typeItem ->
                    val isSelected = selectedType.id == typeItem.id
                    val chipColor = typeItem.toComposeColor()
                    Surface(
                        color = if (isSelected) chipColor.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            width = if (isSelected) 1.6.dp else 1.dp,
                            color = if (isSelected) chipColor else Color.White.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.clickable { selectedType = typeItem }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(chipColor)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = typeItem.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.78f),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = chipColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                if (onCustomTypesChange != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.clickable { showAddCustomTypeDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Свой", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
        }

        // 3. Interactive Canvas Box with Player in center and auto-expanding circle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeightDp.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF090A0E))
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
        ) {
            var canvasSize by remember { mutableStateOf(Size.Zero) }

            // Compute capsule sizes and positions
            val capsules = remember(boardState.nodes, canvasSize, textMeasurer) {
                computeCapsuleLayout(
                    nodes = boardState.nodes,
                    playerNode = playerNode,
                    canvasSize = canvasSize,
                    textMeasurer = textMeasurer
                )
            }

            // Auto-fit scale on size/count change if not modified
            LaunchedEffect(canvasSize, capsules.size) {
                if (canvasSize.width > 0f && canvasSize.height > 0f && capsules.isNotEmpty()) {
                    val maxDist = capsules.maxOfOrNull { (it.center - Offset(canvasSize.width / 2f, canvasSize.height / 2f)).getDistance() + it.width / 2f } ?: 180f
                    val avail = min(canvasSize.width, canvasSize.height) * 0.44f
                    val ideal = (avail / maxDist).coerceIn(0.25f, 1.0f)
                    if (scale == 1f) {
                        scale = ideal
                    }
                }
            }

            fun screenToWorld(screenPt: Offset): Offset {
                if (canvasSize.width <= 0f || canvasSize.height <= 0f) return screenPt
                val cx = canvasSize.width / 2f
                val cy = canvasSize.height / 2f
                return Offset(
                    x = (screenPt.x - cx - panOffset.x) / scale + cx,
                    y = (screenPt.y - cy - panOffset.y) / scale + cy
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isReadOnly) {
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            firstDown.consume()
                            val downScreenPos = firstDown.position
                            val downWorldPos = screenToWorld(downScreenPos)
                            val hitCapsule = capsules.find { it.contains(downWorldPos) }

                            var isMultiTouch = false
                            var totalMoveDistance = 0f
                            var isDraggingArrow = false

                            do {
                                val event = awaitPointerEvent()
                                val activeChanges = event.changes.filter { it.pressed }

                                if (activeChanges.size >= 2) {
                                    isMultiTouch = true
                                    isDraggingArrow = false
                                    dragSourceNodeId = null
                                    dragCurrentWorldPos = null

                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = true)

                                    if (zoomChange != 1f || panChange != Offset.Zero) {
                                        val oldScale = scale
                                        val newScale = (oldScale * zoomChange).coerceIn(0.18f, 3.5f)
                                        val cx = canvasSize.width / 2f
                                        val cy = canvasSize.height / 2f
                                        val focusOffset = centroid - Offset(cx, cy)
                                        val scaleFactor = newScale / oldScale
                                        panOffset = Offset(
                                            x = focusOffset.x * (1f - scaleFactor) + panOffset.x * scaleFactor + panChange.x,
                                            y = focusOffset.y * (1f - scaleFactor) + panOffset.y * scaleFactor + panChange.y
                                        )
                                        scale = newScale
                                    }
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                } else if (activeChanges.size == 1 && !isMultiTouch) {
                                    val change = activeChanges.first()
                                    val delta = change.position - change.previousPosition
                                    totalMoveDistance += delta.getDistance()

                                    if (totalMoveDistance > 10f) {
                                        if (!currentReadOnly && hitCapsule != null) {
                                            isDraggingArrow = true
                                            dragSourceNodeId = hitCapsule.node.id
                                            dragCurrentWorldPos = screenToWorld(change.position)
                                        } else {
                                            panOffset += delta
                                        }
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (isDraggingArrow) {
                                val srcId = dragSourceNodeId
                                val endWorld = dragCurrentWorldPos
                                if (srcId != null && endWorld != null) {
                                    val targetCapsule = capsules.find { it.contains(endWorld) }
                                    if (targetCapsule != null && targetCapsule.node.id != srcId) {
                                        applyConnection(srcId, targetCapsule.node.id, currentSelectedType)
                                        activeSourceNodeId = null
                                    }
                                }
                                dragSourceNodeId = null
                                dragCurrentWorldPos = null
                            } else if (!isMultiTouch && totalMoveDistance <= 12f) {
                                if (hitCapsule != null) {
                                    val tappedId = hitCapsule.node.id
                                    if (currentReadOnly) {
                                        // Read-only tap: open character edit if tapped
                                    } else {
                                        val currentActive = activeSourceNodeId
                                        if (currentActive != null && currentActive != tappedId) {
                                            // Second tap: create connection from active to tapped!
                                            applyConnection(currentActive, tappedId, currentSelectedType)
                                            activeSourceNodeId = null
                                        } else if (currentActive == tappedId) {
                                            // Tap same capsule twice: open character rename / delete dialog
                                            activeSourceNodeId = null
                                            editingNode = hitCapsule.node
                                        } else {
                                            // First tap: select as source for arrow
                                            activeSourceNodeId = tappedId
                                        }
                                    }
                                } else {
                                    // Tapped empty space or an arrow
                                    val tappedPair = findTappedEdgePair(
                                        worldPt = downWorldPos,
                                        capsules = capsules,
                                        edges = currentBoardState.edges
                                    )
                                    if (tappedPair != null && !currentReadOnly) {
                                        editingEdgePair = tappedPair
                                        activeSourceNodeId = null
                                    } else {
                                        activeSourceNodeId = null
                                    }
                                }
                            }
                        }
                    }
            ) {
                canvasSize = size
                val cx = size.width / 2f
                val cy = size.height / 2f

                // 1. Subtle background grid
                val gridStep = (36f * scale).coerceAtLeast(14f)
                val startX = ((panOffset.x + cx) % gridStep + gridStep) % gridStep
                val startY = ((panOffset.y + cy) % gridStep + gridStep) % gridStep
                val gridColor = Color.White.copy(alpha = 0.03f)
                var gx = startX
                while (gx < size.width) {
                    drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                    gx += gridStep
                }
                var gy = startY
                while (gy < size.height) {
                    drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                    gy += gridStep
                }

                drawContext.canvas.save()
                drawContext.canvas.translate(cx + panOffset.x, cy + panOffset.y)
                drawContext.canvas.scale(scale, scale)
                drawContext.canvas.translate(-cx, -cy)

                val capsuleById = capsules.associateBy { it.node.id }
                val centerOffset = Offset(cx, cy)

                // 2. Collect unique pairs of nodes that have at least one connection between them
                val pairEdges = mutableMapOf<String, Pair<BoardEdge?, BoardEdge?>>()
                boardState.edges.forEach { edge ->
                    if (edge.fromId == edge.toId) return@forEach
                    val a = minOf(edge.fromId, edge.toId)
                    val b = maxOf(edge.fromId, edge.toId)
                    val key = "$a::$b"
                    val current = pairEdges[key] ?: (null to null)
                    pairEdges[key] = if (edge.fromId == a) {
                        edge to current.second
                    } else {
                        current.first to edge
                    }
                }

                // 3. Draw All Arrows (Bidirectional Gradient & Directed Single)
                pairEdges.forEach { (key, pair) ->
                    val (edgeAB, edgeBA) = pair
                    val parts = key.split("::")
                    val idA = parts[0]
                    val idB = parts[1]
                    val boxA = capsuleById[idA] ?: return@forEach
                    val boxB = capsuleById[idB] ?: return@forEach

                    drawConnectionArrow(
                        boxA = boxA,
                        boxB = boxB,
                        edgeAB = edgeAB, // A -> B
                        edgeBA = edgeBA, // B -> A
                        centerOffset = centerOffset
                    )
                }

                // 4. Draw active dragging arrow
                val curDragSrc = dragSourceNodeId
                val curDragPos = dragCurrentWorldPos
                if (curDragSrc != null && curDragPos != null) {
                    val srcBox = capsuleById[curDragSrc]
                    if (srcBox != null) {
                        val startPt = borderIntersection(srcBox, curDragPos)
                        val dragColor = currentSelectedType.toComposeColor()
                        drawLine(
                            color = dragColor,
                            start = startPt,
                            end = curDragPos,
                            strokeWidth = 3.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawArrowHead(tip = curDragPos, from = startPt, color = dragColor, sizePx = 14.dp.toPx())
                    }
                }

                // 5. Draw Node Capsules (Opaque pill capsules that completely cover arrows)
                capsules.forEach { capsule ->
                    val isSource = activeSourceNodeId == capsule.node.id || dragSourceNodeId == capsule.node.id
                    val displayNodeName = if (capsule.isPlayer || capsule.node.name.equals("Игрок", ignoreCase = true)) {
                        if (isFemboy) "Фембойчик" else if (isEn) "Player" else "Игрок"
                    } else if (capsule.node.name.equals("Untitled Ассистент", ignoreCase = true)) {
                        if (isFemboy) "Untitled Ассистент (ня~)" else if (isEn) "Untitled Assistant" else "Untitled Ассистент"
                    } else capsule.node.name

                    val textLayout = textMeasurer.measure(
                        text = AnnotatedString(displayNodeName),
                        style = TextStyle(
                            color = Color.White,
                            fontSize = if (capsule.isPlayer) 13.5.sp else 12.5.sp,
                            fontWeight = if (capsule.isPlayer) FontWeight.Bold else FontWeight.SemiBold
                        )
                    )

                    drawCapsule(
                        capsule = capsule,
                        isSource = isSource,
                        highlightColor = currentSelectedType.toComposeColor(),
                        textLayout = textLayout
                    )
                }

                drawContext.canvas.restore()
            }

            // Top-right floating controls: Zoom in, Zoom out, Reset Center
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.size(32.dp).clickable { scale = (scale * 1.25f).coerceAtMost(3.5f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Приблизить", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.size(32.dp).clickable { scale = (scale / 1.25f).coerceAtLeast(0.2f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Отдалить", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.size(32.dp).clickable {
                        scale = 1f
                        panOffset = Offset.Zero
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "К игроку", tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Bottom helper hint
            if (!isReadOnly) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (activeSourceNodeId != null) "Нажмите на второго персонажа для связи" else "Тяните от персонажа к персонажу или нажмите на стрелку",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // --- Dialog: Add New Character ---
    if (showAddNodeDialog) {
        var newCharName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNodeDialog = false },
            title = { Text("Новый персонаж") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Персонаж появится в круге вокруг игрока.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newCharName,
                        onValueChange = { newCharName = it },
                        label = { Text("Имя персонажа") },
                        placeholder = { Text("Персонаж ${boardState.nodes.size}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = newCharName.trim().ifEmpty { "Персонаж ${boardState.nodes.size}" }
                        val newId = "node_${System.currentTimeMillis()}"
                        val updatedNodes = boardState.nodes + BoardNode(id = newId, name = clean, role = "npc")
                        // Automatically create neutral bidirectional relation with player
                        val updatedEdges = boardState.edges + listOf(
                            BoardEdge(fromId = playerNode.id, toId = newId, typeId = selectedType.id, label = selectedType.label, colorHex = selectedType.colorHex, score = selectedType.defaultScore),
                            BoardEdge(fromId = newId, toId = playerNode.id, typeId = selectedType.id, label = selectedType.label, colorHex = selectedType.colorHex, score = selectedType.defaultScore)
                        )
                        onBoardChange(boardState.copy(nodes = updatedNodes, edges = updatedEdges))
                        showAddNodeDialog = false
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNodeDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // --- Dialog: Edit Character Name / Delete Character ---
    editingNode?.let { node ->
        var updatedName by remember(node) { mutableStateOf(node.name) }
        val isPlayer = node.id == playerNode.id

        AlertDialog(
            onDismissRequest = { editingNode = null },
            title = { Text(if (isPlayer) "Персона игрока (Центр)" else "Персонаж «${node.name}»") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = updatedName,
                        onValueChange = { updatedName = it },
                        label = { Text("Имя на плашке") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!isPlayer) {
                        Text(
                            text = "Если это имя сценария или игры (например, «Кто предатель?»), удалите его, чтобы ИИ не считал его действующим лицом истории.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                deleteNode(node.id)
                                editingNode = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Удалить имя из отношений")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = updatedName.trim()
                        if (clean.isNotEmpty()) {
                            val updatedNodes = boardState.nodes.map {
                                if (it.id == node.id) it.copy(name = clean) else it
                            }
                            onBoardChange(boardState.copy(nodes = updatedNodes))
                        }
                        editingNode = null
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNode = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    // --- Dialog: Bidirectional Relationship Editor (A -> B and B -> A) ---
    editingEdgePair?.let { pair ->
        var edgeAB by remember(pair) { mutableStateOf(pair.edgeAB) }
        var edgeBA by remember(pair) { mutableStateOf(pair.edgeBA) }

        var labelAB by remember(pair) { mutableStateOf(pair.edgeAB?.label ?: "Нейтрально") }
        var scoreAB by remember(pair) { mutableFloatStateOf(pair.edgeAB?.score?.toFloat() ?: 0f) }
        var colorHexAB by remember(pair) { mutableStateOf(pair.edgeAB?.colorHex ?: "#8E8E93") }

        var labelBA by remember(pair) { mutableStateOf(pair.edgeBA?.label ?: "Нейтрально") }
        var scoreBA by remember(pair) { mutableFloatStateOf(pair.edgeBA?.score?.toFloat() ?: 0f) }
        var colorHexBA by remember(pair) { mutableStateOf(pair.edgeBA?.colorHex ?: "#8E8E93") }

        AlertDialog(
            onDismissRequest = { editingEdgePair = null },
            title = {
                Text(
                    text = "${pair.nodeA.name} ⇄ ${pair.nodeB.name}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Отношения можно настраивать в обе стороны раздельно (как на схеме).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Direction A -> B
                    Surface(
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RelationshipBoardHelper.parseHexColor(colorHexAB).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(RelationshipBoardHelper.parseHexColor(colorHexAB))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "От «${pair.nodeA.name}» к «${pair.nodeB.name}»",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Quick type selector
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                allTypes.forEach { t ->
                                    val isCur = colorHexAB.equals(t.colorHex, ignoreCase = true)
                                    val c = t.toComposeColor()
                                    Surface(
                                        color = if (isCur) c.copy(alpha = 0.3f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isCur) c else Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.clickable {
                                            colorHexAB = t.colorHex
                                            labelAB = t.label
                                            scoreAB = t.defaultScore.toFloat()
                                        }
                                    ) {
                                        Text(
                                            text = t.label,
                                            fontSize = 11.sp,
                                            color = if (isCur) Color.White else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Уровень отношения: ${scoreAB.roundToInt()}% ($labelAB)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = scoreAB,
                                onValueChange = { scoreAB = it },
                                valueRange = -100f..100f
                            )
                        }
                    }

                    // 2. Direction B -> A
                    Surface(
                        color = Color.White.copy(alpha = 0.04f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RelationshipBoardHelper.parseHexColor(colorHexBA).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(RelationshipBoardHelper.parseHexColor(colorHexBA))
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "От «${pair.nodeB.name}» к «${pair.nodeA.name}»",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Quick type selector
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                allTypes.forEach { t ->
                                    val isCur = colorHexBA.equals(t.colorHex, ignoreCase = true)
                                    val c = t.toComposeColor()
                                    Surface(
                                        color = if (isCur) c.copy(alpha = 0.3f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, if (isCur) c else Color.White.copy(alpha = 0.1f)),
                                        modifier = Modifier.clickable {
                                            colorHexBA = t.colorHex
                                            labelBA = t.label
                                            scoreBA = t.defaultScore.toFloat()
                                        }
                                    ) {
                                        Text(
                                            text = t.label,
                                            fontSize = 11.sp,
                                            color = if (isCur) Color.White else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Уровень отношения: ${scoreBA.roundToInt()}% ($labelBA)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = scoreBA,
                                onValueChange = { scoreBA = it },
                                valueRange = -100f..100f
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedEdges = currentBoardState.edges.toMutableList()
                        // Update or add A -> B
                        updatedEdges.removeAll { it.fromId == pair.nodeA.id && it.toId == pair.nodeB.id }
                        updatedEdges.add(
                            BoardEdge(
                                fromId = pair.nodeA.id,
                                toId = pair.nodeB.id,
                                typeId = "custom_${colorHexAB.removePrefix("#")}",
                                label = labelAB,
                                colorHex = colorHexAB,
                                score = scoreAB.roundToInt().coerceIn(-100, 100),
                                description = ""
                            )
                        )

                        // Update or add B -> A
                        updatedEdges.removeAll { it.fromId == pair.nodeB.id && it.toId == pair.nodeA.id }
                        updatedEdges.add(
                            BoardEdge(
                                fromId = pair.nodeB.id,
                                toId = pair.nodeA.id,
                                typeId = "custom_${colorHexBA.removePrefix("#")}",
                                label = labelBA,
                                colorHex = colorHexBA,
                                score = scoreBA.roundToInt().coerceIn(-100, 100),
                                description = ""
                            )
                        )

                        onBoardChange(currentBoardState.copy(edges = updatedEdges))
                        editingEdgePair = null
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val updatedEdges = currentBoardState.edges.filterNot {
                                (it.fromId == pair.nodeA.id && it.toId == pair.nodeB.id) ||
                                (it.fromId == pair.nodeB.id && it.toId == pair.nodeA.id)
                            }
                            onBoardChange(currentBoardState.copy(edges = updatedEdges))
                            editingEdgePair = null
                        }
                    ) {
                        Text("Удалить связь", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { editingEdgePair = null }) {
                        Text("Отмена")
                    }
                }
            }
        )
    }

    // --- Dialog: Add Custom Relationship Type ---
    if (showAddCustomTypeDialog && onCustomTypesChange != null) {
        val presetColors = listOf(
            "#FF4081" to "Ярко-розовый",
            "#9C27B0" to "Фиолетовый",
            "#00BCD4" to "Бирюзовый",
            "#FFD54F" to "Золотой",
            "#8BC34A" to "Лаймовый",
            "#FF5722" to "Алый",
            "#7986CB" to "Индиго"
        )
        var customLabel by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf(presetColors.first().first) }
        var defaultScore by remember { mutableFloatStateOf(20f) }

        AlertDialog(
            onDismissRequest = { showAddCustomTypeDialog = false },
            title = { Text("Свой статус отношений") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Название (напр. Соперники, Симпатия)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Цвет стрелки:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetColors.forEach { (hex, _) ->
                            val isChosen = selectedColorHex == hex
                            val c = RelationshipBoardHelper.parseHexColor(hex)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isChosen) 3.dp else 1.dp,
                                        color = if (isChosen) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                    Text("Базовый процент: ${defaultScore.roundToInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = defaultScore,
                        onValueChange = { defaultScore = it },
                        valueRange = -100f..100f
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = customLabel.trim()
                        if (clean.isNotEmpty()) {
                            val currentCustom = RelationshipBoardHelper.parseCustomTypes(customTypesJson).toMutableList()
                            currentCustom.add(
                                RelationshipTypeItem(
                                    id = "custom_${System.currentTimeMillis()}",
                                    label = clean,
                                    colorHex = selectedColorHex,
                                    defaultScore = defaultScore.roundToInt(),
                                    isCustom = true
                                )
                            )
                            onCustomTypesChange(RelationshipBoardHelper.serializeCustomTypes(currentCustom))
                        }
                        showAddCustomTypeDialog = false
                    }
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomTypeDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Computes capsule positions placing Player strictly at the center (cx, cy)
 * and all outer characters in an expanding circle with dynamic perimeter calculation.
 */
private fun computeCapsuleLayout(
    nodes: List<BoardNode>,
    playerNode: BoardNode,
    canvasSize: Size,
    textMeasurer: TextMeasurer
): List<NodeCapsule> {
    if (nodes.isEmpty() || canvasSize.width <= 0f || canvasSize.height <= 0f) return emptyList()

    val cx = canvasSize.width / 2f
    val cy = canvasSize.height / 2f
    val boxH = 40f

    // 1. Player Capsule at the exact center
    val playerNameLayout = textMeasurer.measure(
        text = AnnotatedString(playerNode.name),
        style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
    )
    val playerW = max(90f, playerNameLayout.size.width.toFloat() + 38f)
    val playerCapsule = NodeCapsule(
        node = playerNode,
        center = Offset(cx, cy),
        width = playerW,
        height = boxH + 4f,
        isPlayer = true
    )

    val outerNodes = nodes.filter { it.id != playerNode.id }
    if (outerNodes.isEmpty()) {
        return listOf(playerCapsule)
    }

    // 2. Measure all outer capsule widths
    val measuredOuter = outerNodes.map { node ->
        val layout = textMeasurer.measure(
            text = AnnotatedString(node.name),
            style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        )
        val w = max(86f, layout.size.width.toFloat() + 34f)
        node to w
    }

    // 3. Dynamic radius expansion:
    // Ensure sufficient clearance around the circumference so plaques never overlap even with 31+ characters!
    val totalPerimeterNeeded = measuredOuter.sumOf { it.second.toDouble() + 42.0 }.toFloat()
    val minRadiusForPlaques = totalPerimeterNeeded / (2f * PI.toFloat())
    val clearanceFromPlayer = playerW / 2f + (measuredOuter.maxOfOrNull { it.second } ?: 100f) / 2f + 65f
    val baseRadius = min(canvasSize.width, canvasSize.height) * 0.38f
    val radius = max(baseRadius, max(clearanceFromPlayer, minRadiusForPlaques))

    val result = mutableListOf<NodeCapsule>()
    result.add(playerCapsule)

    val count = measuredOuter.size
    measuredOuter.forEachIndexed { idx, (node, width) ->
        val angle = -PI / 2.0 + (2.0 * PI * idx.toDouble() / count.toDouble())
        val x = cx + (radius * cos(angle)).toFloat()
        val y = cy + (radius * sin(angle)).toFloat()
        result.add(
            NodeCapsule(
                node = node,
                center = Offset(x, y),
                width = width,
                height = boxH,
                isPlayer = false
            )
        )
    }

    return result
}

/**
 * Draws a clean pill capsule with name text and glowing borders.
 * NO sub-labels ("Персонаж", "Игрок") beneath names.
 */
private fun DrawScope.drawCapsule(
    capsule: NodeCapsule,
    isSource: Boolean,
    highlightColor: Color,
    textLayout: TextLayoutResult
) {
    val topLeft = Offset(capsule.center.x - capsule.width / 2f, capsule.center.y - capsule.height / 2f)
    val corner = CornerRadius(capsule.height / 2f, capsule.height / 2f)

    val bgColor = if (capsule.isPlayer) Color(0xFF262117) else Color(0xFF161822)

    val borderColor = when {
        isSource -> highlightColor
        capsule.isPlayer -> Color(0xFFFFD54F) // Distinct Golden Glow for the Player in center
        else -> Color.White.copy(alpha = 0.28f)
    }

    // Golden halo glow for player capsule
    if (capsule.isPlayer) {
        drawRoundRect(
            color = Color(0xFFFFD54F).copy(alpha = 0.16f),
            topLeft = Offset(topLeft.x - 4f, topLeft.y - 4f),
            size = Size(capsule.width + 8f, capsule.height + 8f),
            cornerRadius = CornerRadius((capsule.height + 8f) / 2f, (capsule.height + 8f) / 2f)
        )
    }

    // Opaque pill background
    drawRoundRect(
        color = bgColor,
        topLeft = topLeft,
        size = Size(capsule.width, capsule.height),
        cornerRadius = corner
    )

    // Border
    drawRoundRect(
        color = borderColor,
        topLeft = topLeft,
        size = Size(capsule.width, capsule.height),
        cornerRadius = corner,
        style = Stroke(width = if (capsule.isPlayer || isSource) 3.dp.toPx() else 1.5.dp.toPx())
    )

    // Name text: exactly centered vertically and horizontally inside the pill
    drawText(
        textLayoutResult = textLayout,
        topLeft = Offset(
            x = capsule.center.x - textLayout.size.width / 2f,
            y = capsule.center.y - textLayout.size.height / 2f
        )
    )
}

/**
 * Draws connection arrow according to user specifications (image_ca4b845c.png):
 * - If both A -> B and B -> A exist:
 *   - The line transitions smoothly with a linear gradient.
 *   - Arrowhead at B points at B with color of A -> B.
 *   - Arrowhead at A points at A with color of B -> A.
 *   - In the middle, the colors merge seamlessly.
 * - If only one direction exists:
 *   - Single colored line with arrowhead at destination pointing towards destination.
 * - If connecting two outer nodes:
 *   - Curved quadratic path deflecting away from center so it never obscures the player capsule.
 */
private fun DrawScope.drawConnectionArrow(
    boxA: NodeCapsule,
    boxB: NodeCapsule,
    edgeAB: BoardEdge?,
    edgeBA: BoardEdge?,
    centerOffset: Offset
) {
    if (edgeAB == null && edgeBA == null) return

    val startA = borderIntersection(boxA, boxB.center)
    val endB = borderIntersection(boxB, boxA.center)
    val connectsCenter = boxA.isPlayer || boxB.isPlayer

    if (edgeAB != null && edgeBA != null) {
        // --- Bidirectional Gradient Arrow ---
        val colorAB = edgeAB.toComposeColor() // A -> B (points at B)
        val colorBA = edgeBA.toComposeColor() // B -> A (points at A)

        // Line gradient: from colorBA at A to colorAB at B
        val brush = Brush.linearGradient(
            colors = listOf(colorBA, colorAB),
            start = startA,
            end = endB
        )

        if (connectsCenter) {
            drawLine(
                brush = brush,
                start = startA,
                end = endB,
                strokeWidth = 3.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Arrowhead at B pointing AT B with color of A -> B
            drawArrowHead(tip = endB, from = startA, color = colorAB, sizePx = 13.dp.toPx())
            // Arrowhead at A pointing AT A with color of B -> A
            drawArrowHead(tip = startA, from = endB, color = colorBA, sizePx = 13.dp.toPx())
        } else {
            // Arc around center for two outer characters
            val mid = Offset((startA.x + endB.x) / 2f, (startA.y + endB.y) / 2f)
            val distToCenter = (mid - centerOffset).getDistance()
            val ctrl = calculateArcControl(startA, endB, mid, centerOffset, distToCenter)

            val path = Path().apply {
                moveTo(startA.x, startA.y)
                quadraticTo(ctrl.x, ctrl.y, endB.x, endB.y)
            }
            drawPath(
                path = path,
                brush = brush,
                style = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArrowHead(tip = endB, from = ctrl, color = colorAB, sizePx = 13.dp.toPx())
            drawArrowHead(tip = startA, from = ctrl, color = colorBA, sizePx = 13.dp.toPx())
        }
    } else if (edgeAB != null) {
        // --- Single Direction A -> B ---
        val colorAB = edgeAB.toComposeColor()
        if (connectsCenter) {
            drawLine(
                color = colorAB,
                start = startA,
                end = endB,
                strokeWidth = 3.4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawArrowHead(tip = endB, from = startA, color = colorAB, sizePx = 13.dp.toPx())
        } else {
            val mid = Offset((startA.x + endB.x) / 2f, (startA.y + endB.y) / 2f)
            val distToCenter = (mid - centerOffset).getDistance()
            val ctrl = calculateArcControl(startA, endB, mid, centerOffset, distToCenter)
            val path = Path().apply {
                moveTo(startA.x, startA.y)
                quadraticTo(ctrl.x, ctrl.y, endB.x, endB.y)
            }
            drawPath(
                path = path,
                color = colorAB,
                style = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArrowHead(tip = endB, from = ctrl, color = colorAB, sizePx = 13.dp.toPx())
        }
    } else if (edgeBA != null) {
        // --- Single Direction B -> A ---
        val colorBA = edgeBA.toComposeColor()
        if (connectsCenter) {
            drawLine(
                color = colorBA,
                start = endB,
                end = startA,
                strokeWidth = 3.4.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawArrowHead(tip = startA, from = endB, color = colorBA, sizePx = 13.dp.toPx())
        } else {
            val mid = Offset((startA.x + endB.x) / 2f, (startA.y + endB.y) / 2f)
            val distToCenter = (mid - centerOffset).getDistance()
            val ctrl = calculateArcControl(startA, endB, mid, centerOffset, distToCenter)
            val path = Path().apply {
                moveTo(endB.x, endB.y)
                quadraticTo(ctrl.x, ctrl.y, startA.x, startA.y)
            }
            drawPath(
                path = path,
                color = colorBA,
                style = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArrowHead(tip = startA, from = ctrl, color = colorBA, sizePx = 13.dp.toPx())
        }
    }
}

/**
 * Calculates curved control point pushing arc outward away from the center Player capsule.
 */
private fun calculateArcControl(
    start: Offset,
    end: Offset,
    mid: Offset,
    center: Offset,
    distToCenter: Float
): Offset {
    val dirFromCenter = mid - center
    val len = dirFromCenter.getDistance()
    return if (len > 1f) {
        val unit = dirFromCenter / len
        mid + unit * (if (distToCenter < 110f) 60f else 32f)
    } else {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val perp = Offset(-dy, dx)
        val pLen = perp.getDistance()
        if (pLen > 1f) mid + (perp / pLen) * 55f else mid
    }
}

/**
 * Draws sharp, clear triangle arrowhead.
 */
private fun DrawScope.drawArrowHead(
    tip: Offset,
    from: Offset,
    color: Color,
    sizePx: Float
) {
    val dx = tip.x - from.x
    val dy = tip.y - from.y
    val len = hypot(dx, dy)
    if (len < 1f) return
    val ux = dx / len
    val uy = dy / len
    val px = -uy
    val py = ux

    val baseCenter = Offset(tip.x - ux * sizePx, tip.y - uy * sizePx)
    val wing = sizePx * 0.50f
    val p1 = Offset(baseCenter.x + px * wing, baseCenter.y + py * wing)
    val p2 = Offset(baseCenter.x - px * wing, baseCenter.y - py * wing)

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path, color = color)
}

/**
 * Computes exact intersection point of line connecting center to target with capsule boundary.
 */
private fun borderIntersection(box: NodeCapsule, target: Offset): Offset {
    val dx = target.x - box.center.x
    val dy = target.y - box.center.y
    if (abs(dx) < 0.001f && abs(dy) < 0.001f) return box.center

    val hw = box.width / 2f + 2f
    val hh = box.height / 2f + 2f
    val scaleX = if (abs(dx) > 0.001f) hw / abs(dx) else Float.MAX_VALUE
    val scaleY = if (abs(dy) > 0.001f) hh / abs(dy) else Float.MAX_VALUE
    val t = min(scaleX, scaleY)
    return Offset(box.center.x + dx * t, box.center.y + dy * t)
}

/**
 * Finds edge pair when user taps on an arrow line.
 */
private fun findTappedEdgePair(
    worldPt: Offset,
    capsules: List<NodeCapsule>,
    edges: List<BoardEdge>
): EdgePairInfo? {
    val boxById = capsules.associateBy { it.node.id }
    val checkedPairs = mutableSetOf<String>()

    for (edge in edges) {
        val a = minOf(edge.fromId, edge.toId)
        val b = maxOf(edge.fromId, edge.toId)
        val key = "$a::$b"
        if (!checkedPairs.add(key)) continue

        val boxA = boxById[a] ?: continue
        val boxB = boxById[b] ?: continue

        val p1 = boxA.center
        val p2 = boxB.center
        val mid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
        val q1 = Offset(p1.x * 0.68f + p2.x * 0.32f, p1.y * 0.68f + p2.y * 0.32f)
        val q2 = Offset(p1.x * 0.32f + p2.x * 0.68f, p1.y * 0.32f + p2.y * 0.68f)

        if ((worldPt - mid).getDistance() < 36f ||
            (worldPt - q1).getDistance() < 36f ||
            (worldPt - q2).getDistance() < 36f
        ) {
            val edgeAB = edges.find { it.fromId == boxA.node.id && it.toId == boxB.node.id }
            val edgeBA = edges.find { it.fromId == boxB.node.id && it.toId == boxA.node.id }
            return EdgePairInfo(
                nodeA = boxA.node,
                nodeB = boxB.node,
                edgeAB = edgeAB,
                edgeBA = edgeBA
            )
        }
    }
    return null
}
