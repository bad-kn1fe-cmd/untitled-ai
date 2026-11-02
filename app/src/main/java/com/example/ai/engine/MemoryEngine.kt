package com.example.ai.engine

import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.RelationshipDao
import com.example.data.local.dao.SummaryDao
import com.example.data.local.entity.AiProviderProfileEntity
import com.example.data.local.entity.CharacterEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.RelationshipTableEntity
import com.example.data.local.entity.SummaryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object MemoryEngine {

    suspend fun checkAndGenerateMemory(
        chat: ChatEntity,
        character: CharacterEntity,
        providerProfile: AiProviderProfileEntity,
        messageDao: MessageDao,
        summaryDao: SummaryDao,
        relationshipDao: RelationshipDao
    ): Boolean = withContext(Dispatchers.IO) {
        val totalCount = messageDao.getMessageCountForChat(chat.id)
        if (totalCount < chat.summaryFrequency) return@withContext false

        // Check if generated strictly at boundary (e.g., at exactly message 10, 20, 30...)
        if (totalCount % chat.summaryFrequency != 0) {
            return@withContext false
        }

        try {
            val latestSummary = summaryDao.getLatestSummary(chat.id)
            val expectedRange = "Сообщения #${totalCount - chat.summaryFrequency} - #$totalCount"
            if (latestSummary != null && latestSummary.messageRange == expectedRange) {
                // Already generated for this exact batch, skip to prevent duplicates
                return@withContext false
            }

            val recentMessages = messageDao.getRecentMessages(chat.id, chat.summaryFrequency + 2).reversed()
            if (recentMessages.isEmpty()) return@withContext false

            val msgTextBuilder = StringBuilder()
            for (m in recentMessages) {
                val author = if (m.sender == "user") "Пользователь" else character.name
                msgTextBuilder.append("$author: ${m.content}\n")
            }

            val latestRel = relationshipDao.getLatestRelationshipTable(chat.id)
            val currentBoard = com.example.util.RelationshipBoardHelper.parseBoard(
                json = latestRel?.boardJson?.takeIf { it.isNotBlank() } ?: character.relationshipBoardJson,
                mainCharName = character.name,
                playerName = "Игрок",
                startingRelationship = chat.relationshipType.ifBlank { character.startingRelationship },
                customRelationship = chat.customRelationship.ifBlank { character.customRelationship },
                initialTitle = character.initialRelationshipTitle,
                initialPercent = if (character.initialRelationshipPercent != -1) character.initialRelationshipPercent else -999
            )

            val prompt = RoleplayPromptBuilder.buildSummaryPrompt(
                characterName = character.name,
                previousSummary = latestSummary?.summaryText,
                messagesText = msgTextBuilder.toString(),
                currentBoard = currentBoard
            )

            val dummyMsg = MessageEntity(
                chatId = chat.id,
                sender = "user",
                content = prompt,
                timestamp = System.currentTimeMillis()
            )

            val result = AiProviderEngine.generateChatCompletion(
                profile = providerProfile,
                systemPrompt = "Ты модуль аналитики и памяти ролплея. Отвечай СТРОГО в формате JSON.",
                recentMessages = listOf(dummyMsg)
            )

            val responseText = result.getOrNull() ?: return@withContext false
            val cleanJson = extractJson(responseText) ?: return@withContext false
            val jsonObject = JSONObject(cleanJson)

            val summaryStr = jsonObject.optString("summary", "")
            if (summaryStr.isNotBlank()) {
                val summaryEntity = SummaryEntity(
                    chatId = chat.id,
                    messageRange = "Сообщения #${totalCount - chat.summaryFrequency} - #$totalCount",
                    summaryText = summaryStr,
                    timestamp = System.currentTimeMillis()
                )
                summaryDao.insertSummary(summaryEntity)
            }

            val relObj = jsonObject.optJSONObject("relationship")
            if (relObj != null) {
                val rawStatus = relObj.optString("status", "Знакомые")
                val rawTrustScore = relObj.optInt("trustScore", 0).coerceIn(-100, 100)
                val mood = relObj.optString("mood", "Спокойное")
                val keyEventsArr = relObj.optJSONArray("keyEvents") ?: JSONArray()
                val openPlotsArr = relObj.optJSONArray("openPlotThreads") ?: JSONArray()
                val boardEdgesArr = relObj.optJSONArray("boardEdges")

                val firstEventNote = if (keyEventsArr.length() > 0) keyEventsArr.optString(0, "") else ""
                val updatedBoard = com.example.util.RelationshipBoardHelper.applyAiUpdatesToBoard(
                    currentBoard = currentBoard,
                    aiEdgesArray = boardEdgesArr,
                    fallbackMainToPlayerStatus = rawStatus,
                    fallbackMainToPlayerScore = rawTrustScore,
                    fallbackDescription = firstEventNote
                )

                val finalAvgScore = if (updatedBoard.edges.isNotEmpty()) {
                    updatedBoard.calculateAverageScore()
                } else {
                    rawTrustScore
                }
                val finalStatus = if (rawStatus.isNotBlank()) {
                    rawStatus
                } else {
                    updatedBoard.calculateOverallTitle("Знакомые")
                }

                val relEntity = RelationshipTableEntity(
                    chatId = chat.id,
                    status = finalStatus,
                    trustScore = finalAvgScore,
                    mood = mood,
                    keyEvents = keyEventsArr.toString(),
                    openPlotThreads = openPlotsArr.toString(),
                    boardJson = updatedBoard.toJson(),
                    timestamp = System.currentTimeMillis()
                )
                relationshipDao.insertRelationshipTable(relEntity)
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    private fun extractJson(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1)
        }
        return null
    }
}
