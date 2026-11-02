package com.example.util

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

data class RelationshipTypeItem(
    val id: String,
    val label: String,
    val colorHex: String,
    val defaultScore: Int,
    val isCustom: Boolean = false
) {
    fun toComposeColor(): Color = RelationshipBoardHelper.parseHexColor(colorHex)
}

data class BoardNode(
    val id: String,
    val name: String,
    val role: String = "npc" // "main", "player", "npc"
)

data class BoardEdge(
    val fromId: String,
    val toId: String,
    val typeId: String,
    val label: String,
    val colorHex: String,
    val score: Int, // -100 .. +100
    val description: String = ""
) {
    fun toComposeColor(): Color = RelationshipBoardHelper.parseHexColor(colorHex)
}

data class RelationshipBoardState(
    val nodes: List<BoardNode>,
    val edges: List<BoardEdge>
) {
    fun calculateAverageScore(): Int {
        if (edges.isEmpty()) return 0
        val avg = edges.map { it.score.coerceIn(-100, 100) }.average()
        return avg.roundToInt().coerceIn(-100, 100)
    }

    fun calculateOverallTitle(fallbackTitle: String = "Знакомые"): String {
        if (edges.isEmpty()) return fallbackTitle.ifBlank { "Знакомые" }
        val mainToPlayer = edges.find { it.fromId == "node_main" && it.toId == "node_player" }
            ?: edges.firstOrNull { it.fromId == "node_main" }
            ?: edges.firstOrNull()
        if (edges.size == 1 && mainToPlayer != null) {
            return mainToPlayer.label
        }
        val avg = calculateAverageScore()
        return when {
            avg >= 75 -> "Глубокая привязанность"
            avg >= 45 -> "Дружба и доверие"
            avg >= 15 -> "Симпатия"
            avg > -15 -> mainToPlayer?.label ?: "Знакомые"
            avg > -45 -> "Напряжение"
            avg > -75 -> "Неприязнь"
            else -> "Вражда"
        }
    }

    fun toJson(): String {
        return RelationshipBoardHelper.serializeBoard(this)
    }
}

object RelationshipBoardHelper {

    val DEFAULT_TYPES = listOf(
        RelationshipTypeItem(
            id = "love",
            label = "Любовь",
            colorHex = "#FF2D95",
            defaultScore = 90,
            isCustom = false
        ),
        RelationshipTypeItem(
            id = "friends",
            label = "Друзья",
            colorHex = "#34C759",
            defaultScore = 60,
            isCustom = false
        ),
        RelationshipTypeItem(
            id = "sympathy",
            label = "Симпатия",
            colorHex = "#00E5FF",
            defaultScore = 40,
            isCustom = false
        ),
        RelationshipTypeItem(
            id = "neutral",
            label = "Знакомые",
            colorHex = "#8E8E93",
            defaultScore = 0,
            isCustom = false
        ),
        RelationshipTypeItem(
            id = "dislike",
            label = "Недолюбие",
            colorHex = "#FF9500",
            defaultScore = -40,
            isCustom = false
        ),
        RelationshipTypeItem(
            id = "enemies",
            label = "Враги",
            colorHex = "#FF3B30",
            defaultScore = -80,
            isCustom = false
        )
    )

    fun parseHexColor(hex: String, fallback: Color = Color(0xFF8E8E93)): Color {
        return try {
            val clean = hex.trim().removePrefix("#")
            val colorLong = when (clean.length) {
                6 -> 0xFF000000L or clean.toLong(16)
                8 -> clean.toLong(16)
                else -> return fallback
            }
            Color(colorLong)
        } catch (_: Exception) {
            fallback
        }
    }

    fun parseCustomTypes(json: String): List<RelationshipTypeItem> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<RelationshipTypeItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", "custom_$i")
                val label = obj.optString("label", "").trim()
                val colorHex = obj.optString("colorHex", "#A855F7")
                val score = obj.optInt("defaultScore", 0).coerceIn(-100, 100)
                if (label.isNotBlank()) {
                    result.add(
                        RelationshipTypeItem(
                            id = id,
                            label = label,
                            colorHex = colorHex,
                            defaultScore = score,
                            isCustom = true
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeCustomTypes(list: List<RelationshipTypeItem>): String {
        val arr = JSONArray()
        for (item in list.filter { it.isCustom }) {
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("label", item.label)
                put("colorHex", item.colorHex)
                put("defaultScore", item.defaultScore.coerceIn(-100, 100))
            })
        }
        return arr.toString()
    }

    fun getAllTypes(customTypesJson: String): List<RelationshipTypeItem> {
        return DEFAULT_TYPES + parseCustomTypes(customTypesJson)
    }

    fun createDefaultBoard(
        mainCharName: String,
        playerName: String = "Игрок",
        startingRelationship: String = "strangers",
        customRelationship: String = "",
        initialTitle: String = "",
        initialPercent: Int = -999
    ): RelationshipBoardState {
        val cleanMain = mainCharName.trim().ifBlank { "Персонаж 1" }
        val cleanPlayer = playerName.trim().ifBlank { "Игрок" }

        val nodes = listOf(
            BoardNode(id = "node_main", name = cleanMain, role = "main"),
            BoardNode(id = "node_player", name = cleanPlayer, role = "player")
        )

        val mappedType = when (startingRelationship) {
            "romantic" -> DEFAULT_TYPES.first { it.id == "love" }
            "close_friends" -> DEFAULT_TYPES.first { it.id == "friends" }
            "friends" -> DEFAULT_TYPES.first { it.id == "friends" }
            "acquaintances" -> DEFAULT_TYPES.first { it.id == "sympathy" }
            "enemies" -> DEFAULT_TYPES.first { it.id == "enemies" }
            else -> DEFAULT_TYPES.first { it.id == "neutral" }
        }

        val effectiveLabel = initialTitle.trim().ifBlank {
            if (startingRelationship == "custom" && customRelationship.isNotBlank()) {
                customRelationship.trim().take(28)
            } else {
                mappedType.label
            }
        }

        val effectiveScore = if (initialPercent in -100..100) {
            initialPercent
        } else {
            mappedType.defaultScore
        }

        val edges = listOf(
            BoardEdge(
                fromId = "node_main",
                toId = "node_player",
                typeId = mappedType.id,
                label = effectiveLabel,
                colorHex = mappedType.colorHex,
                score = effectiveScore,
                description = customRelationship.trim()
            ),
            BoardEdge(
                fromId = "node_player",
                toId = "node_main",
                typeId = mappedType.id,
                label = effectiveLabel,
                colorHex = mappedType.colorHex,
                score = effectiveScore,
                description = ""
            )
        )

        return RelationshipBoardState(nodes = nodes, edges = edges)
    }

    fun parseBoard(
        json: String,
        mainCharName: String = "Персонаж 1",
        playerName: String = "Игрок",
        startingRelationship: String = "strangers",
        customRelationship: String = "",
        initialTitle: String = "",
        initialPercent: Int = -999
    ): RelationshipBoardState {
        val cleanMain = mainCharName.trim().ifBlank { "Персонаж 1" }
        val cleanPlayer = playerName.trim().ifBlank { "Игрок" }

        if (json.isBlank() || json == "{}") {
            return createDefaultBoard(
                mainCharName = cleanMain,
                playerName = cleanPlayer,
                startingRelationship = startingRelationship,
                customRelationship = customRelationship,
                initialTitle = initialTitle,
                initialPercent = initialPercent
            )
        }

        return try {
            val root = JSONObject(json)
            val nodesArr = root.optJSONArray("nodes") ?: JSONArray()
            val edgesArr = root.optJSONArray("edges") ?: JSONArray()
            val hasMainExplicit = root.optBoolean("hasMainNode", true)

            val parsedNodes = mutableListOf<BoardNode>()
            for (i in 0 until nodesArr.length()) {
                val nObj = nodesArr.optJSONObject(i) ?: continue
                val id = nObj.optString("id", "").trim()
                val role = nObj.optString("role", "npc").trim()
                var name = nObj.optString("name", "").trim()
                if (role == "main" && cleanMain != "Персонаж 1") {
                    name = cleanMain
                }
                if (id.isNotBlank() && name.isNotBlank()) {
                    parsedNodes.add(BoardNode(id = id, name = name, role = role))
                }
            }

            if (parsedNodes.isEmpty()) {
                if (hasMainExplicit) {
                    parsedNodes.add(BoardNode(id = "node_main", name = cleanMain, role = "main"))
                }
                parsedNodes.add(BoardNode(id = "node_player", name = cleanPlayer, role = "player"))
            } else {
                // Ensure the player node is present in the board
                if (parsedNodes.none { it.role == "player" || it.id == "node_player" }) {
                    parsedNodes.add(BoardNode(id = "node_player", name = cleanPlayer, role = "player"))
                }
            }

            val validNodeIds = parsedNodes.map { it.id }.toSet()
            val parsedEdges = mutableListOf<BoardEdge>()
            for (i in 0 until edgesArr.length()) {
                val eObj = edgesArr.optJSONObject(i) ?: continue
                val fromId = eObj.optString("fromId", "").trim()
                val toId = eObj.optString("toId", "").trim()
                if (fromId !in validNodeIds || toId !in validNodeIds || fromId == toId) continue

                val typeId = eObj.optString("typeId", "neutral")
                val fallbackType = DEFAULT_TYPES.find { it.id == typeId } ?: DEFAULT_TYPES.first { it.id == "neutral" }
                val label = eObj.optString("label", fallbackType.label).trim().ifBlank { fallbackType.label }
                val colorHex = eObj.optString("colorHex", fallbackType.colorHex).trim().ifBlank { fallbackType.colorHex }
                val score = eObj.optInt("score", fallbackType.defaultScore).coerceIn(-100, 100)
                val desc = eObj.optString("description", "").trim()

                parsedEdges.removeAll { it.fromId == fromId && it.toId == toId }
                parsedEdges.add(
                    BoardEdge(
                        fromId = fromId,
                        toId = toId,
                        typeId = typeId,
                        label = label,
                        colorHex = colorHex,
                        score = score,
                        description = desc
                    )
                )
            }

            RelationshipBoardState(nodes = parsedNodes, edges = parsedEdges)
        } catch (_: Exception) {
            createDefaultBoard(
                mainCharName = cleanMain,
                playerName = cleanPlayer,
                startingRelationship = startingRelationship,
                customRelationship = customRelationship,
                initialTitle = initialTitle,
                initialPercent = initialPercent
            )
        }
    }

    fun serializeBoard(state: RelationshipBoardState): String {
        val root = JSONObject()
        val nodesArr = JSONArray()
        for (n in state.nodes) {
            nodesArr.put(JSONObject().apply {
                put("id", n.id)
                put("name", n.name)
                put("role", n.role)
            })
        }
        val edgesArr = JSONArray()
        for (e in state.edges) {
            edgesArr.put(JSONObject().apply {
                put("fromId", e.fromId)
                put("toId", e.toId)
                put("typeId", e.typeId)
                put("label", e.label)
                put("colorHex", e.colorHex)
                put("score", e.score.coerceIn(-100, 100))
                put("description", e.description)
            })
        }
        root.put("nodes", nodesArr)
        root.put("edges", edgesArr)
        val hasMain = state.nodes.any { it.role == "main" || it.id == "node_main" }
        root.put("hasMainNode", hasMain)
        return root.toString()
    }

    /**
     * Converts the board into a clean, human-editable Code Editor text format:
     * Выбранное имя - Персонаж 1 : Любовь | +90% | Описание
     * Выбранное имя - Персонаж 2 : Враги | -80%
     * Выбранное имя - Персонаж 3 : Не задано
     */
    fun formatToCodeEditorText(
        board: RelationshipBoardState,
        selectedPerspectiveNodeId: String? = null,
        includeUnconnectedPairs: Boolean = true
    ): String {
        if (board.nodes.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("# Имена на доске (удалите строку @Имя, чтобы удалить персонажа):\n")
        for (node in board.nodes) {
            sb.append("@${node.name}\n")
        }
        sb.append("\n# Связи (Формат: Имя 1 - Имя 2 : Статус | Процент% | Описание)\n")

        val sourceNodes = if (selectedPerspectiveNodeId != null) {
            board.nodes.filter { it.id == selectedPerspectiveNodeId }
        } else {
            board.nodes
        }

        for (src in sourceNodes) {
            sb.append("\n# От лица: ${src.name}\n")
            for (target in board.nodes) {
                if (target.id == src.id) continue
                val edge = board.edges.find { it.fromId == src.id && it.toId == target.id }
                if (edge != null) {
                    val sign = if (edge.score > 0) "+" else ""
                    val descPart = if (edge.description.isNotBlank()) " | ${edge.description}" else ""
                    sb.append("${src.name} - ${target.name} : ${edge.label} | $sign${edge.score}%$descPart\n")
                } else if (includeUnconnectedPairs) {
                    sb.append("${src.name} - ${target.name} : -\n")
                }
            }
        }
        return sb.toString().trim()
    }

    /**
     * Parses the Code Editor text back into a RelationshipBoardState.
     * Supports:
     * - @Имя (declares/keeps a character; removing @Имя when @ lines are present removes the character)
     * - Выбранное имя - Персонаж 1 : Любовь | +90% | Описание
     * - Выбранное имя - Персонаж 2 : Друзья (+60%)
     * - Выбранное имя - Персонаж 3 : - (clears/ignores unconnected pair)
     */
    fun parseFromCodeEditorText(
        codeText: String,
        currentBoard: RelationshipBoardState,
        selectedPerspectiveNodeId: String? = null,
        customTypesJson: String = "[]"
    ): RelationshipBoardState {
        val allTypes = getAllTypes(customTypesJson)
        val lines = codeText.lines()

        val declaredAtNames = mutableListOf<String>()
        var hasAtDeclarations = false
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.startsWith("@")) {
                hasAtDeclarations = true
                val name = line.removePrefix("@").trim()
                if (name.isNotBlank() && declaredAtNames.none { it.equals(name, ignoreCase = true) }) {
                    declaredAtNames.add(name)
                }
            }
        }

        val updatedNodes = mutableListOf<BoardNode>()
        if (hasAtDeclarations) {
            for (name in declaredAtNames) {
                val existing = currentBoard.nodes.find { it.name.equals(name, ignoreCase = true) }
                if (existing != null) {
                    updatedNodes.add(existing.copy(name = name))
                } else {
                    updatedNodes.add(
                        BoardNode(
                            id = "node_${System.currentTimeMillis()}_${updatedNodes.size}",
                            name = name,
                            role = "npc"
                        )
                    )
                }
            }
        } else {
            updatedNodes.addAll(currentBoard.nodes)
        }

        fun getOrCreateNode(name: String): BoardNode {
            val clean = name.trim()
            val found = updatedNodes.find { it.name.equals(clean, ignoreCase = true) }
            if (found != null) return found
            val created = BoardNode(
                id = "node_${System.currentTimeMillis()}_${updatedNodes.size}",
                name = clean,
                role = "npc"
            )
            updatedNodes.add(created)
            return created
        }

        fun resolveType(label: String, score: Int?): RelationshipTypeItem {
            val clean = label.trim()
            allTypes.find { it.label.equals(clean, ignoreCase = true) || it.id.equals(clean, ignoreCase = true) }?.let { return it }
            val lower = clean.lowercase()
            val s = score ?: 0
            return when {
                lower.contains("любов") || lower.contains("роман") || s >= 75 -> DEFAULT_TYPES.first { it.id == "love" }
                lower.contains("друз") || lower.contains("друг") || s in 45..74 -> DEFAULT_TYPES.first { it.id == "friends" }
                lower.contains("симпат") || s in 15..44 -> DEFAULT_TYPES.first { it.id == "sympathy" }
                lower.contains("враг") || lower.contains("ненавист") || s <= -60 -> DEFAULT_TYPES.first { it.id == "enemies" }
                lower.contains("недолюб") || lower.contains("неприязн") || s in -59..-15 -> DEFAULT_TYPES.first { it.id == "dislike" }
                else -> DEFAULT_TYPES.first { it.id == "neutral" }
            }
        }

        val validIds = updatedNodes.map { it.id }.toSet()
        // If editing a single perspective, preserve edges from other perspectives
        val updatedEdges = if (selectedPerspectiveNodeId != null) {
            currentBoard.edges.filter {
                it.fromId != selectedPerspectiveNodeId && it.fromId in validIds && it.toId in validIds
            }.toMutableList()
        } else {
            mutableListOf()
        }

        val scoreRegex = Regex("""([+-]?\d+)\s*%?""")

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#") || line.startsWith("//") || line.startsWith("@")) continue

            val colonIdx = line.indexOf(':')
            val pairPart = if (colonIdx >= 0) line.substring(0, colonIdx).trim() else line
            val rightPart = if (colonIdx >= 0) line.substring(colonIdx + 1).trim() else ""

            val separator = when {
                pairPart.contains(" -> ") -> " -> "
                pairPart.contains(" — ") -> " — "
                pairPart.contains(" - ") -> " - "
                pairPart.contains("->") -> "->"
                pairPart.contains("-") -> "-"
                else -> continue
            }

            val fromName = pairPart.substringBefore(separator).trim()
            val toName = pairPart.substringAfter(separator).trim()
            if (fromName.isBlank() || toName.isBlank()) continue

            val fromNode = getOrCreateNode(fromName)
            val toNode = getOrCreateNode(toName)
            if (fromNode.id == toNode.id) continue

            if (rightPart == "-" || rightPart.equals("нет", ignoreCase = true) || rightPart.equals("пусто", ignoreCase = true)) {
                updatedEdges.removeAll { it.fromId == fromNode.id && it.toId == toNode.id }
                continue
            }

            val segments = rightPart.split("|").map { it.trim() }
            var rawStatus = segments.getOrNull(0).orEmpty()
            var parsedScore: Int? = null
            var description = segments.getOrNull(2).orEmpty()

            if (segments.size >= 2) {
                val m = scoreRegex.find(segments[1])
                if (m != null) {
                    parsedScore = m.groupValues[1].toIntOrNull()?.coerceIn(-100, 100)
                } else if (description.isBlank()) {
                    description = segments[1]
                }
            } else if (rawStatus.contains("(") && rawStatus.contains(")")) {
                val inside = rawStatus.substringAfter("(").substringBefore(")")
                val m = scoreRegex.find(inside)
                if (m != null) {
                    parsedScore = m.groupValues[1].toIntOrNull()?.coerceIn(-100, 100)
                    rawStatus = rawStatus.substringBefore("(").trim()
                }
            }

            if (rawStatus.isBlank()) {
                rawStatus = "Знакомые"
            }

            val matchedType = resolveType(rawStatus, parsedScore)
            val finalScore = parsedScore ?: matchedType.defaultScore

            updatedEdges.removeAll { it.fromId == fromNode.id && it.toId == toNode.id }
            updatedEdges.add(
                BoardEdge(
                    fromId = fromNode.id,
                    toId = toNode.id,
                    typeId = matchedType.id,
                    label = rawStatus,
                    colorHex = matchedType.colorHex,
                    score = finalScore,
                    description = description
                )
            )
        }

        val finalValidIds = updatedNodes.map { it.id }.toSet()
        val cleanEdges = updatedEdges.filter { it.fromId in finalValidIds && it.toId in finalValidIds && it.fromId != it.toId }
        return RelationshipBoardState(nodes = updatedNodes, edges = cleanEdges)
    }

    /**
     * Formats the relationship board for the AI System Prompt in the exact structure requested:
     * Персонаж 1:
     * Персонаж 2 - любовь
     * Персонаж 3 - враги
     */
    fun formatBoardForPrompt(
        board: RelationshipBoardState,
        mainCharacterName: String,
        userName: String,
        isEn: Boolean = false
    ): String {
        if (board.nodes.isEmpty() || board.edges.isEmpty()) return ""

        val resolvedNodes = board.nodes.map { node ->
            when (node.role) {
                "main" -> node.copy(name = mainCharacterName.ifBlank { node.name })
                "player" -> node.copy(name = if (node.name == "Игрок" || node.name.isBlank()) "$userName (Игрок)" else "${node.name} ($userName)")
                else -> node
            }
        }
        val nodeById = resolvedNodes.associateBy { it.id }

        val sb = StringBuilder()
        for (sourceNode in resolvedNodes) {
            val outgoing = board.edges.filter { it.fromId == sourceNode.id && nodeById.containsKey(it.toId) }
            if (outgoing.isEmpty()) continue

            sb.append("${sourceNode.name}:\n\n")
            for (edge in outgoing) {
                val targetNode = nodeById[edge.toId] ?: continue
                val sign = if (edge.score > 0) "+" else ""
                val descSuffix = if (edge.description.isNotBlank()) " (${edge.description})" else ""
                sb.append("${targetNode.name} - ${edge.label.lowercase()} ($sign${edge.score}%)$descSuffix\n")
            }
            sb.append("\n")
        }
        return sb.toString().trim()
    }

    /**
     * Applies AI-generated dynamic relationship updates to an existing RelationshipBoardState.
     * Supports matching characters by name or node id.
     */
    fun applyAiUpdatesToBoard(
        currentBoard: RelationshipBoardState,
        aiEdgesArray: JSONArray?,
        fallbackMainToPlayerStatus: String?,
        fallbackMainToPlayerScore: Int?,
        fallbackDescription: String?,
        customTypesJson: String = "[]"
    ): RelationshipBoardState {
        val allTypes = getAllTypes(customTypesJson)
        val nodes = currentBoard.nodes.toMutableList()
        val edges = currentBoard.edges.toMutableList()

        fun resolveTypeByLabelOrScore(label: String, score: Int): RelationshipTypeItem {
            val lower = label.lowercase()
            allTypes.find { it.label.equals(label, ignoreCase = true) || it.id.equals(label, ignoreCase = true) }?.let { return it }
            return when {
                lower.contains("любов") || lower.contains("страст") || lower.contains("роман") || score >= 75 -> DEFAULT_TYPES.first { it.id == "love" }
                lower.contains("друз") || lower.contains("друг") || lower.contains("довер") || score in 45..74 -> DEFAULT_TYPES.first { it.id == "friends" }
                lower.contains("симпат") || lower.contains("тепл") || lower.contains("приятел") || score in 15..44 -> DEFAULT_TYPES.first { it.id == "sympathy" }
                lower.contains("враг") || lower.contains("ненавист") || lower.contains("вражд") || score <= -60 -> DEFAULT_TYPES.first { it.id == "enemies" }
                lower.contains("недолюб") || lower.contains("неприязн") || lower.contains("злост") || lower.contains("обид") || score in -59..-15 -> DEFAULT_TYPES.first { it.id == "dislike" }
                else -> DEFAULT_TYPES.first { it.id == "neutral" }
            }
        }

        if (aiEdgesArray != null && aiEdgesArray.length() > 0) {
            for (i in 0 until aiEdgesArray.length()) {
                val obj = aiEdgesArray.optJSONObject(i) ?: continue
                val fromName = obj.optString("from", "").trim()
                val toName = obj.optString("to", "").trim()
                if (fromName.isBlank() || toName.isBlank()) continue

                val fromNode = nodes.find {
                    it.name.equals(fromName, ignoreCase = true) ||
                            it.id.equals(fromName, ignoreCase = true) ||
                            (it.role == "player" && (fromName.equals("Игрок", true) || fromName.equals("Пользователь", true) || fromName.equals("User", true))) ||
                            (it.role == "main" && i == 0 && nodes.none { n -> n.name.equals(fromName, true) })
                } ?: continue

                val toNode = nodes.find {
                    it.name.equals(toName, ignoreCase = true) ||
                            it.id.equals(toName, ignoreCase = true) ||
                            (it.role == "player" && (toName.equals("Игрок", true) || toName.equals("Пользователь", true) || toName.equals("User", true)))
                } ?: continue

                if (fromNode.id == toNode.id) continue

                val existingEdge = edges.find { it.fromId == fromNode.id && it.toId == toNode.id }
                val rawScore = obj.optInt("score", existingEdge?.score ?: 0).coerceIn(-100, 100)
                val rawStatus = obj.optString("status", existingEdge?.label ?: "Знакомые").trim().ifBlank { "Знакомые" }
                val rawDesc = obj.optString("description", existingEdge?.description ?: "").trim()

                val matchedType = resolveTypeByLabelOrScore(rawStatus, rawScore)
                val updatedEdge = BoardEdge(
                    fromId = fromNode.id,
                    toId = toNode.id,
                    typeId = matchedType.id,
                    label = rawStatus,
                    colorHex = matchedType.colorHex,
                    score = rawScore,
                    description = rawDesc.ifBlank { existingEdge?.description ?: "" }
                )
                edges.removeAll { it.fromId == fromNode.id && it.toId == toNode.id }
                edges.add(updatedEdge)
            }
        } else if (fallbackMainToPlayerStatus != null || fallbackMainToPlayerScore != null) {
            val mainNode = nodes.find { it.role == "main" } ?: nodes.firstOrNull()
            val playerNode = nodes.find { it.role == "player" } ?: nodes.lastOrNull()
            if (mainNode != null && playerNode != null && mainNode.id != playerNode.id) {
                val existing = edges.find { it.fromId == mainNode.id && it.toId == playerNode.id }
                val newScore = (fallbackMainToPlayerScore ?: existing?.score ?: 0).coerceIn(-100, 100)
                val newLabel = fallbackMainToPlayerStatus?.takeIf { it.isNotBlank() } ?: existing?.label ?: "Знакомые"
                val matchedType = resolveTypeByLabelOrScore(newLabel, newScore)
                val updatedEdge = BoardEdge(
                    fromId = mainNode.id,
                    toId = playerNode.id,
                    typeId = matchedType.id,
                    label = newLabel,
                    colorHex = matchedType.colorHex,
                    score = newScore,
                    description = fallbackDescription?.takeIf { it.isNotBlank() } ?: existing?.description ?: ""
                )
                edges.removeAll { it.fromId == mainNode.id && it.toId == playerNode.id }
                edges.add(updatedEdge)
            }
        }

        return RelationshipBoardState(nodes = nodes, edges = edges)
    }
}

