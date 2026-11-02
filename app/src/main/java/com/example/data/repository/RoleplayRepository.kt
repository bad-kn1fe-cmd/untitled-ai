package com.example.data.repository

import com.example.ai.engine.AiProviderEngine
import com.example.ai.engine.MemoryEngine
import com.example.ai.engine.RoleplayPromptBuilder
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class RoleplayRepository(private val db: AppDatabase) {

    val allCharacters: Flow<List<CharacterEntity>> = db.characterDao().getAllCharacters()
    val allChats: Flow<List<ChatEntity>> = db.chatDao().getAllChats()
    val allPersonas: Flow<List<UserPersonaEntity>> = db.userPersonaDao().getAllPersonas()
    val activePersonaFlow: Flow<UserPersonaEntity?> = db.userPersonaDao().getActivePersonaFlow()
    val allStories: Flow<List<StoryEntity>> = db.storyDao().getAllStories()
    val allComics: Flow<List<ComicEntity>> = db.comicDao().getAllComics()
    val allRelationships: Flow<List<RelationshipTableEntity>> = db.relationshipDao().getAllRelationshipTablesFlow()

    suspend fun getActivePersona(): UserPersonaEntity? = db.userPersonaDao().getActivePersona()

    suspend fun savePersona(persona: UserPersonaEntity): Long = withContext(Dispatchers.IO) {
        if (persona.id == 0L) {
            val id = db.userPersonaDao().insertPersona(persona)
            if (persona.isSelected) {
                db.userPersonaDao().clearSelection()
                db.userPersonaDao().selectPersona(id)
            }
            id
        } else {
            db.userPersonaDao().updatePersona(persona)
            if (persona.isSelected) {
                db.userPersonaDao().clearSelection()
                db.userPersonaDao().selectPersona(persona.id)
            }
            persona.id
        }
    }

    suspend fun selectPersona(personaId: Long) = withContext(Dispatchers.IO) {
        db.userPersonaDao().clearSelection()
        db.userPersonaDao().selectPersona(personaId)
    }

    suspend fun deletePersona(persona: UserPersonaEntity) = withContext(Dispatchers.IO) {
        db.userPersonaDao().deletePersona(persona)
        val remaining = db.userPersonaDao().getAllPersonasList()
        if (remaining.isNotEmpty() && remaining.none { it.isSelected }) {
            db.userPersonaDao().selectPersona(remaining.first().id)
        }
    }

    fun getChatsForCharacter(characterId: Long): Flow<List<ChatEntity>> =
        db.chatDao().getChatsForCharacter(characterId)

    fun getCharacterFlow(id: Long): Flow<CharacterEntity?> = db.characterDao().getCharacterFlow(id)
    suspend fun getCharacterById(id: Long): CharacterEntity? = db.characterDao().getCharacterById(id)

    suspend fun saveCharacter(character: CharacterEntity): Long = withContext(Dispatchers.IO) {
        val sanitized = character.copy(isAdult = true)
        if (sanitized.id == 0L) {
            db.characterDao().insertCharacter(sanitized)
        } else {
            db.characterDao().updateCharacter(sanitized)
            // Sync greeting banner (and greeting text if chat only has greeting) in existing chats
            try {
                val existingChats = db.chatDao().getChatsForCharacter(sanitized.id).first()
                for (chat in existingChats) {
                    val msgs = db.messageDao().getMessagesListForChat(chat.id)
                    if (msgs.isEmpty() && (sanitized.greetingMessage.isNotBlank() || sanitized.greetingBannerUri.isNotBlank())) {
                        val activePersona = chat.personaId?.let { db.userPersonaDao().getPersonaById(it) }
                            ?: db.userPersonaDao().getActivePersona()
                        val userName = activePersona?.name?.ifBlank { "Пользователь" } ?: "Пользователь"
                        val formattedGreeting = RoleplayPromptBuilder.formatText(
                            sanitized.greetingMessage.ifBlank { "*Приветствует вас*" },
                            sanitized.name,
                            userName
                        )
                        db.messageDao().insertMessage(
                            MessageEntity(
                                chatId = chat.id,
                                sender = "character",
                                content = formattedGreeting,
                                imageUrl = sanitized.greetingBannerUri.ifBlank { null },
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        val firstCharMsg = msgs.firstOrNull { it.sender == "character" }
                        if (firstCharMsg != null) {
                            val newBanner = sanitized.greetingBannerUri.ifBlank { null }
                            val updatedMsg = if (msgs.size == 1 && sanitized.greetingMessage.isNotBlank()) {
                                val activePersona = chat.personaId?.let { db.userPersonaDao().getPersonaById(it) }
                                    ?: db.userPersonaDao().getActivePersona()
                                val userName = activePersona?.name?.ifBlank { "Пользователь" } ?: "Пользователь"
                                val formattedGreeting = RoleplayPromptBuilder.formatText(
                                    sanitized.greetingMessage,
                                    sanitized.name,
                                    userName
                                )
                                firstCharMsg.copy(content = formattedGreeting, imageUrl = newBanner)
                            } else if (firstCharMsg.imageUrl != newBanner && newBanner != null) {
                                firstCharMsg.copy(imageUrl = newBanner)
                            } else {
                                null
                            }
                            if (updatedMsg != null) {
                                db.messageDao().updateMessage(updatedMsg)
                            }
                        }
                    }
                    // Also sync relationship board with existing chat
                    val rels = db.relationshipDao().getRelationshipTablesForChat(chat.id).first()
                    val updatedBoardJson = if (sanitized.relationshipBoardJson.isNotBlank()) {
                        sanitized.relationshipBoardJson
                    } else {
                        sanitized.getParsedRelationshipBoard().toJson()
                    }
                    if (rels.isEmpty()) {
                        db.relationshipDao().insertRelationshipTable(
                            RelationshipTableEntity(
                                chatId = chat.id,
                                status = sanitized.getEffectiveInitialRelTitle(),
                                trustScore = sanitized.getEffectiveInitialRelPercent(),
                                mood = "Любопытство",
                                boardJson = updatedBoardJson
                            )
                        )
                    } else {
                        val latestRel = rels.first()
                        db.relationshipDao().insertRelationshipTable(
                            latestRel.copy(
                                id = 0L,
                                timestamp = System.currentTimeMillis(),
                                status = sanitized.getEffectiveInitialRelTitle(),
                                trustScore = sanitized.getEffectiveInitialRelPercent(),
                                boardJson = updatedBoardJson
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
            sanitized.id
        }
    }

    suspend fun deleteCharacter(character: CharacterEntity) = db.characterDao().deleteCharacter(character)

    suspend fun setCharacterPinned(id: Long, isPinned: Boolean) = db.characterDao().setCharacterPinned(id, isPinned)
    suspend fun setCharacterArchived(id: Long, isArchived: Boolean) = db.characterDao().setCharacterArchived(id, isArchived)
    suspend fun setCharacterHidden(id: Long, isHidden: Boolean) = db.characterDao().setCharacterHidden(id, isHidden)
    suspend fun updateCharacterLastInteractedAt(id: Long, timestamp: Long) = db.characterDao().updateLastInteractedAt(id, timestamp)

    // Stories (Character Groups / Worlds)
    fun getStoryFlow(id: Long): Flow<StoryEntity?> = db.storyDao().getStoryFlow(id)
    suspend fun getStoryById(id: Long): StoryEntity? = db.storyDao().getStoryById(id)
    suspend fun saveStory(story: StoryEntity): Long {
        return if (story.id == 0L) {
            db.storyDao().insertStory(story.copy(createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        } else {
            db.storyDao().updateStory(story.copy(updatedAt = System.currentTimeMillis()))
            story.id
        }
    }
    suspend fun deleteStory(story: StoryEntity) = db.storyDao().deleteStory(story)
    suspend fun setStoryPinned(id: Long, isPinned: Boolean) = db.storyDao().setStoryPinned(id, isPinned)
    suspend fun setStoryArchived(id: Long, isArchived: Boolean) = db.storyDao().setStoryArchived(id, isArchived)

    // Comics
    fun getComicsForChat(chatId: Long): Flow<List<ComicEntity>> = db.comicDao().getComicsForChat(chatId)
    suspend fun getComicById(id: Long): ComicEntity? = db.comicDao().getComicById(id)
    suspend fun saveComic(comic: ComicEntity): Long {
        return if (comic.id == 0L) {
            db.comicDao().insertComic(comic)
        } else {
            db.comicDao().updateComic(comic)
            comic.id
        }
    }
    suspend fun deleteComic(comic: ComicEntity) = db.comicDao().deleteComic(comic)

    fun getChatFlow(chatId: Long): Flow<ChatEntity?> = db.chatDao().getChatFlow(chatId)
    suspend fun getChatById(chatId: Long): ChatEntity? = db.chatDao().getChatById(chatId)

    suspend fun createChat(chat: ChatEntity): Long {
        val chatId = db.chatDao().insertChat(chat)
        val character = db.characterDao().getCharacterById(chat.characterId)
        if (character != null && (character.greetingMessage.isNotBlank() || character.greetingBannerUri.isNotBlank())) {
            val activePersona = db.userPersonaDao().getActivePersona()
            val userName = activePersona?.name?.ifBlank { "Пользователь" } ?: "Пользователь"
            val formattedGreeting = RoleplayPromptBuilder.formatText(
                character.greetingMessage.ifBlank { "*Приветствует вас*" },
                character.name,
                userName
            )
            val greeting = MessageEntity(
                chatId = chatId,
                sender = "character",
                content = formattedGreeting,
                imageUrl = character.greetingBannerUri.ifBlank { null },
                timestamp = System.currentTimeMillis()
            )
            db.messageDao().insertMessage(greeting)
        }
        val initialBoardJson = character?.getParsedRelationshipBoard()?.toJson() ?: ""
        val relTable = RelationshipTableEntity(
            chatId = chatId,
            status = character?.getEffectiveInitialRelTitle() ?: when (chat.relationshipType) {
                "romantic" -> "Любовь"
                "close_friends" -> "Друзья"
                "friends" -> "Друзья"
                "acquaintances" -> "Симпатия"
                else -> "Знакомые"
            },
            trustScore = character?.getEffectiveInitialRelPercent() ?: when (chat.relationshipType) {
                "romantic" -> 90
                "close_friends" -> 60
                "friends" -> 60
                "acquaintances" -> 40
                else -> 0
            },
            mood = "Любопытство",
            boardJson = initialBoardJson
        )
        db.relationshipDao().insertRelationshipTable(relTable)
        return chatId
    }

    suspend fun updateChat(chat: ChatEntity) = db.chatDao().updateChat(chat)
    suspend fun deleteChat(chatId: Long) = db.chatDao().deleteChatById(chatId)

    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>> = db.messageDao().getMessagesForChat(chatId)
    fun getSummariesForChat(chatId: Long): Flow<List<SummaryEntity>> = db.summaryDao().getSummariesForChat(chatId)
    fun getRelationshipTablesForChat(chatId: Long): Flow<List<RelationshipTableEntity>> =
        db.relationshipDao().getRelationshipTablesForChat(chatId)
    fun getLatestRelationshipFlow(chatId: Long): Flow<RelationshipTableEntity?> =
        db.relationshipDao().getLatestRelationshipTableFlow(chatId)

    suspend fun toggleTextingMode(chatId: Long, enabled: Boolean) {
        db.chatDao().setTextingMode(chatId, enabled)
    }

    suspend fun toggleNsfw(chatId: Long, enabled: Boolean) {
        val chat = db.chatDao().getChatById(chatId) ?: return
        db.chatDao().setNsfwEnabled(chatId, enabled)
    }

    suspend fun setChatPinned(chatId: Long, enabled: Boolean) {
        db.chatDao().setChatPinned(chatId, enabled)
    }

    suspend fun setChatArchived(chatId: Long, enabled: Boolean) {
        db.chatDao().setChatArchived(chatId, enabled)
    }

    suspend fun updateChatTitle(chatId: Long, title: String) {
        db.chatDao().updateChatTitle(chatId, title)
    }

    suspend fun branchChatAtMessage(chatId: Long, messageId: Long): Long? = withContext(Dispatchers.IO) {
        val sourceChat = db.chatDao().getChatById(chatId) ?: return@withContext null
        val allMsgs = db.messageDao().getMessagesListForChat(chatId)
        val targetIndex = allMsgs.indexOfFirst { it.id == messageId }
        if (targetIndex == -1) return@withContext null

        val msgsToCopy = allMsgs.take(targetIndex + 1)
        val lastMsg = msgsToCopy.last()
        val maxTimestamp = lastMsg.timestamp

        // Create new branched chat
        val branchedChat = sourceChat.copy(
            id = 0L,
            title = "${sourceChat.title} (Ветка)",
            createdAt = System.currentTimeMillis(),
            isPinned = false,
            isArchived = false
        )
        val newChatId = db.chatDao().insertChat(branchedChat)

        // Copy messages up to selected message
        val copiedMessages = msgsToCopy.map { msg ->
            msg.copy(
                id = 0L,
                chatId = newChatId
            )
        }
        db.messageDao().insertMessages(copiedMessages)

        // Copy summaries up to message timestamp
        val summariesToCopy = db.summaryDao().getSummariesUpToTimestamp(chatId, maxTimestamp)
        summariesToCopy.forEach { s ->
            db.summaryDao().insertSummary(s.copy(id = 0L, chatId = newChatId))
        }

        // Copy relationship records up to message timestamp
        val relsToCopy = db.relationshipDao().getRelationshipTablesUpToTimestamp(chatId, maxTimestamp)
        if (relsToCopy.isNotEmpty()) {
            relsToCopy.forEach { r ->
                db.relationshipDao().insertRelationshipTable(r.copy(id = 0L, chatId = newChatId))
            }
        } else {
            val character = db.characterDao().getCharacterById(sourceChat.characterId)
            val initialRel = RelationshipTableEntity(
                chatId = newChatId,
                status = character?.getEffectiveInitialRelTitle() ?: when (sourceChat.relationshipType) {
                    "romantic" -> "Любовь"
                    "close_friends" -> "Друзья"
                    "friends" -> "Друзья"
                    "acquaintances" -> "Симпатия"
                    else -> "Знакомые"
                },
                trustScore = character?.getEffectiveInitialRelPercent() ?: when (sourceChat.relationshipType) {
                    "romantic" -> 90
                    "close_friends" -> 60
                    "friends" -> 60
                    "acquaintances" -> 40
                    else -> 0
                },
                mood = "Любопытство",
                boardJson = character?.getParsedRelationshipBoard()?.toJson() ?: ""
            )
            db.relationshipDao().insertRelationshipTable(initialRel)
        }

        newChatId
    }

    suspend fun editMessage(messageId: Long, newContent: String) = withContext(Dispatchers.IO) {
        val msg = db.messageDao().getMessageById(messageId) ?: return@withContext
        db.messageDao().updateMessage(msg.copy(content = newContent))
    }

    suspend fun updateMessageReaction(messageId: Long, reaction: String?) = withContext(Dispatchers.IO) {
        db.messageDao().updateReaction(messageId, reaction)
    }

    suspend fun deleteMessage(messageId: Long) = withContext(Dispatchers.IO) {
        db.messageDao().deleteMessageById(messageId)
    }

    suspend fun clearMessagesForChat(chatId: Long) = withContext(Dispatchers.IO) {
        db.messageDao().deleteMessagesForChat(chatId)
        val chat = db.chatDao().getChatById(chatId)
        if (chat != null) {
            val character = db.characterDao().getCharacterById(chat.characterId)
            if (character != null && (character.greetingMessage.isNotBlank() || character.greetingBannerUri.isNotBlank())) {
                val activePersona = (chat.personaId?.let { db.userPersonaDao().getPersonaById(it) } ?: db.userPersonaDao().getActivePersona())
                val userName = activePersona?.name?.ifBlank { "Пользователь" } ?: "Пользователь"
                val formattedGreeting = RoleplayPromptBuilder.formatText(
                    character.greetingMessage.ifBlank { "*Приветствует вас*" },
                    character.name,
                    userName
                )
                db.messageDao().insertMessage(
                    MessageEntity(
                        chatId = chatId,
                        sender = "character",
                        content = formattedGreeting,
                        imageUrl = character.greetingBannerUri.ifBlank { null },
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun updateChatDraft(chatId: Long, draft: String) = withContext(Dispatchers.IO) {
        db.chatDao().updateChatDraft(chatId, draft)
    }

    suspend fun updateChatNotes(chatId: Long, notes: String) = withContext(Dispatchers.IO) {
        db.chatDao().updateChatNotes(chatId, notes)
    }

    suspend fun setChatPersona(chatId: Long, personaId: Long?) = withContext(Dispatchers.IO) {
        db.chatDao().setChatPersona(chatId, personaId)
    }

    fun getAllGeneratedImages(): Flow<List<MessageEntity>> = db.messageDao().getAllGeneratedImages()

    suspend fun switchMessageVariant(messageId: Long, targetIndex: Int) = withContext(Dispatchers.IO) {
        val msg = db.messageDao().getMessageById(messageId) ?: return@withContext
        if (msg.variantsJson.isNullOrBlank()) return@withContext
        try {
            val jsonArray = org.json.JSONArray(msg.variantsJson)
            if (targetIndex in 0 until jsonArray.length()) {
                val selectedText = jsonArray.getString(targetIndex)
                db.messageDao().updateMessage(
                    msg.copy(
                        content = selectedText,
                        currentVariantIndex = targetIndex
                    )
                )
            }
        } catch (_: Exception) {}
    }

    suspend fun regenerateVariant(
        chatId: Long,
        messageId: Long,
        contextWindowSize: Int = 12,
        settingsSummary: String = "",
        openRouterFallbackModels: String = "",
        customSystemPrompt: String = "",
        userSettings: com.example.data.datastore.UserSettings? = null,
        onTokenStream: ((String) -> Unit)? = null
    ): Result<MessageEntity> = withContext(Dispatchers.IO) {
        val chat = db.chatDao().getChatById(chatId) ?: return@withContext Result.failure(
            IllegalArgumentException("Чат не найден")
        )
        val character = db.characterDao().getCharacterById(chat.characterId) ?: return@withContext Result.failure(
            IllegalArgumentException("Персонаж не найден")
        )
        val targetMsg = db.messageDao().getMessageById(messageId) ?: return@withContext Result.failure(
            IllegalArgumentException("Сообщение не найдено")
        )

        val textProfile = db.aiProviderDao().getDefaultTextProfile()
            ?: return@withContext Result.failure(
                IllegalStateException("Не настроен текстовый AI-провайдер. Перейдите в настройки провайдеров и укажите API-ключ.")
            )

        val effectiveMaxTokens = if (userSettings != null && userSettings.defaultMaxTokens > 0) {
            userSettings.defaultMaxTokens
        } else {
            textProfile.maxTokens
        }
        val effectiveProfile = textProfile.copy(maxTokens = effectiveMaxTokens)

        val isAssistant = character.isAssistant
        val activePersona = (chat.personaId?.let { db.userPersonaDao().getPersonaById(it) } ?: db.userPersonaDao().getActivePersona())
        val latestSummary = db.summaryDao().getLatestSummary(chatId)
        val latestRelationship = db.relationshipDao().getLatestRelationshipTable(chatId)

        val systemPrompt = RoleplayPromptBuilder.buildSystemPrompt(
            character = character,
            chat = chat,
            latestSummary = latestSummary,
            latestRelationship = latestRelationship,
            userPersona = activePersona,
            settingsSummary = settingsSummary,
            customTemplate = customSystemPrompt.ifBlank { textProfile.systemPromptTemplate },
            userSettings = userSettings,
            maxTokens = effectiveMaxTokens
        )

        // Fetch recent messages before this message timestamp
        val recentMessages = db.messageDao().getRecentMessages(chatId, contextWindowSize)
            .filter { it.timestamp < targetMsg.timestamp || (it.id == targetMsg.id && it.sender == "user") }
            .reversed()

        val aiResult = AiProviderEngine.generateChatCompletion(
            profile = effectiveProfile,
            systemPrompt = systemPrompt,
            recentMessages = recentMessages,
            openRouterFallbackModels = openRouterFallbackModels,
            geminiSafetyThreshold = userSettings?.geminiSafetyThreshold ?: "BLOCK_NONE",
            onTokenStream = onTokenStream
        )

        if (aiResult.isFailure) {
            return@withContext Result.failure(aiResult.exceptionOrNull() ?: Exception("Неизвестная ошибка ИИ"))
        }

        val rawAiText = aiResult.getOrNull() ?: ""
        var newContent = com.example.ai.engine.ResponseCutoffSanitizer.sanitizeAndComplete(rawAiText)
        // Check autonomous mode tags
        val textingOnRegex = Regex("""<(?:texting:\s*on|texting_mode:\s*on|mode:\s*texting|переписка)>|\[(?:TEXTING_MODE_ON)\]""", RegexOption.IGNORE_CASE)
        val textingOffRegex = Regex("""<(?:texting:\s*off|texting_mode:\s*off|mode:\s*roleplay|/переписка|обычный)>|\[(?:TEXTING_MODE_OFF)\]""", RegexOption.IGNORE_CASE)
        if (textingOnRegex.containsMatchIn(newContent)) {
            db.chatDao().setTextingMode(chatId, true)
            newContent = newContent.replace(textingOnRegex, "").trim()
        } else if (textingOffRegex.containsMatchIn(newContent)) {
            db.chatDao().setTextingMode(chatId, false)
            newContent = newContent.replace(textingOffRegex, "").trim()
        }

        // Strip photo tags if any
        val photoTagRegex = Regex("""\[(?:PHOTO_REQUEST|SEND_PHOTO):\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)
        newContent = photoTagRegex.replace(newContent, "").trim()
        if (newContent.isBlank()) newContent = "..."

        // Build list of variants
        val variantsList = mutableListOf<String>()
        if (!targetMsg.variantsJson.isNullOrBlank()) {
            try {
                val existing = org.json.JSONArray(targetMsg.variantsJson)
                for (i in 0 until existing.length()) {
                    variantsList.add(existing.getString(i))
                }
            } catch (_: Exception) {}
        }
        if (variantsList.isEmpty() && targetMsg.content.isNotBlank()) {
            variantsList.add(targetMsg.content)
        }
        variantsList.add(newContent)

        val updatedVariantsJson = org.json.JSONArray(variantsList).toString()
        val newIndex = variantsList.size - 1

        val updatedMsg = targetMsg.copy(
            content = newContent,
            variantsJson = updatedVariantsJson,
            currentVariantIndex = newIndex
        )
        db.messageDao().updateMessage(updatedMsg)
        Result.success(updatedMsg)
    }

    suspend fun insertUserMessage(
        chatId: Long,
        userText: String,
        imageUrl: String? = null,
        audioFilePath: String? = null,
        audioDurationSec: Int = 0
    ): MessageEntity = withContext(Dispatchers.IO) {
        val chat = db.chatDao().getChatById(chatId)
        val defaultPlaceholder = if (imageUrl != null) {
            val items = com.example.util.ChatMediaHelper.deserialize(imageUrl)
            val photos = items.count { it is com.example.util.ChatMediaItem.Photo }
            val videos = items.count { it is com.example.util.ChatMediaItem.Video }
            when {
                items.isEmpty() -> "..."
                items.size == 1 && videos == 1 -> "*отправил видеозапись*"
                items.size == 1 && photos == 1 -> "*отправил изображение*"
                videos == 0 -> "*отправил $photos фото*"
                photos == 0 -> "*отправил $videos видео*"
                else -> "*отправил $photos фото и $videos видео*"
            }
        } else if (audioFilePath != null) {
            "*отправил голосовое сообщение*"
        } else {
            "..."
        }
        val msg = MessageEntity(
            chatId = chatId,
            sender = "user",
            content = userText.ifBlank { defaultPlaceholder },
            imageUrl = imageUrl,
            audioFilePath = audioFilePath,
            audioDurationSec = audioDurationSec,
            isTextingMode = false,
            timestamp = System.currentTimeMillis()
        )
        val id = db.messageDao().insertMessage(msg)
        msg.copy(id = id)
    }

    /**
     * Executes the AI call for the given chat and saves the assistant response.
     */
    suspend fun generateAiResponse(
        chatId: Long,
        contextWindowSize: Int = 12,
        settingsSummary: String = "",
        isContinuation: Boolean = false,
        openRouterFallbackModels: String = "",
        customSystemPrompt: String = "",
        userSettings: com.example.data.datastore.UserSettings? = null,
        onTokenStream: ((String) -> Unit)? = null
    ): Result<List<MessageEntity>> = withContext(Dispatchers.IO) {
        val chat = db.chatDao().getChatById(chatId) ?: return@withContext Result.failure(
            IllegalArgumentException("Чат не найден")
        )
        val character = db.characterDao().getCharacterById(chat.characterId) ?: return@withContext Result.failure(
            IllegalArgumentException("Персонаж не найден")
        )
        val isAssistant = character.isAssistant

        // Resolve AI provider profile and effective token budget
        val textProfile = if (chat.providerProfileId != null) {
            db.aiProviderDao().getProfileById(chat.providerProfileId)
        } else {
            db.aiProviderDao().getDefaultTextProfile()
        } ?: return@withContext Result.failure(
            IllegalStateException("Не выбран AI-провайдер. Перейдите в Настройки -> AI-провайдеры.")
        )

        val effectiveMaxTokens = if (userSettings != null && userSettings.defaultMaxTokens > 0) {
            userSettings.defaultMaxTokens
        } else {
            textProfile.maxTokens
        }
        val effectiveProfile = textProfile.copy(maxTokens = effectiveMaxTokens)

        // Build context
        val latestSummary = db.summaryDao().getLatestSummary(chatId)
        val latestRelationship = db.relationshipDao().getLatestRelationshipTable(chatId)
        val activePersona = (chat.personaId?.let { db.userPersonaDao().getPersonaById(it) } ?: db.userPersonaDao().getActivePersona())
        val systemPrompt = RoleplayPromptBuilder.buildSystemPrompt(
            character = character,
            chat = chat,
            latestSummary = latestSummary,
            latestRelationship = latestRelationship,
            userPersona = activePersona,
            settingsSummary = settingsSummary,
            customTemplate = customSystemPrompt.ifBlank { textProfile.systemPromptTemplate },
            userSettings = userSettings,
            maxTokens = effectiveMaxTokens
        )

        val isEn = userSettings?.appLanguage.equals("en", ignoreCase = true)
        val baseRecentMessages = db.messageDao().getRecentMessages(chatId, contextWindowSize).reversed()
        val recentMessages = if (isContinuation) {
            val continuationPrompt = if (isAssistant) {
                if (isEn) "[Continue explanation or answer from where you left off without repeating.]"
                else "[Продолжай объяснение или ответ с того места, где остановился, без повторения сказанного.]"
            } else {
                if (isEn) "[Continue narrative and dialogue from where you left off. Describe further actions, dialogue or events without repeating.]"
                else "[Продолжай повествование и диалог с того места, где остановился. Опиши дальнейшие события, реплики или действия персонажа без повторения сказанного.]"
            }
            val tempUserContinuationMsg = MessageEntity(
                chatId = chatId,
                sender = "user",
                content = continuationPrompt,
                timestamp = System.currentTimeMillis()
            )
            baseRecentMessages + tempUserContinuationMsg
        } else {
            baseRecentMessages
        }

        val aiResult = AiProviderEngine.generateChatCompletion(
            profile = effectiveProfile,
            systemPrompt = systemPrompt,
            recentMessages = recentMessages,
            openRouterFallbackModels = openRouterFallbackModels,
            geminiSafetyThreshold = userSettings?.geminiSafetyThreshold ?: "BLOCK_NONE",
            onTokenStream = onTokenStream
        )

        if (aiResult.isFailure) {
            return@withContext Result.failure(aiResult.exceptionOrNull() ?: Exception("Неизвестная ошибка"))
        }

        val rawAiResponse = aiResult.getOrNull() ?: ""

        val resultMessages = mutableListOf<MessageEntity>()
        // Sanitize and complete any truncated post to ensure clean sentence completion
        val completedAiResponse = com.example.ai.engine.ResponseCutoffSanitizer.sanitizeAndComplete(rawAiResponse)
        var responseText = completedAiResponse
        var photoUrl: String? = null
        var photoPrompt: String? = null

        // Clean any leftover mode tags
        val leftoverModeRegex = Regex("""<(?:texting:\s*on|texting:\s*off|texting_mode:\s*on|texting_mode:\s*off|переписка|обычный)>|\[(?:TEXTING_MODE_ON|TEXTING_MODE_OFF|PHOTO_REQUEST:[^\]]*|SEND_PHOTO:[^\]]*)\]""", RegexOption.IGNORE_CASE)
        responseText = responseText.replace(leftoverModeRegex, "").trim()

        // Save assistant response message
        val msg = MessageEntity(
            chatId = chatId,
            sender = "character",
            content = responseText.ifBlank { "..." },
            imageUrl = photoUrl,
            imagePrompt = photoPrompt,
            isChunked = false,
            isTextingMode = false,
            timestamp = System.currentTimeMillis()
        )
        val id = db.messageDao().insertMessage(msg)
        resultMessages.add(msg.copy(id = id))

        // Memory Engine background update
        MemoryEngine.checkAndGenerateMemory(
            chat = chat,
            character = character,
            providerProfile = textProfile,
            messageDao = db.messageDao(),
            summaryDao = db.summaryDao(),
            relationshipDao = db.relationshipDao()
        )

        Result.success(resultMessages)
    }

    suspend fun editUserMessageAndRegenerate(
        chatId: Long,
        messageId: Long,
        newText: String,
        contextWindowSize: Int = 12,
        settingsSummary: String = ""
    ): Result<List<MessageEntity>> = withContext(Dispatchers.IO) {
        val userMsg = db.messageDao().getMessageById(messageId) ?: return@withContext Result.failure(
            IllegalArgumentException("Сообщение не найдено")
        )
        // 1. Update user message
        db.messageDao().updateMessage(userMsg.copy(content = newText))

        // 2. Delete all subsequent messages (including AI response)
        db.messageDao().deleteMessagesAfter(chatId, userMsg.timestamp)

        // 3. Generate fresh response from AI provider
        generateAiResponse(chatId, contextWindowSize, settingsSummary)
    }

    suspend fun updateMessageText(messageId: Long, newText: String) = withContext(Dispatchers.IO) {
        val msg = db.messageDao().getMessageById(messageId) ?: return@withContext
        db.messageDao().updateMessage(msg.copy(content = newText))
    }

    suspend fun sendMessage(
        chatId: Long,
        userText: String,
        imageUrl: String? = null,
        contextWindowSize: Int = 12,
        settingsSummary: String = ""
    ): Result<List<MessageEntity>> = withContext(Dispatchers.IO) {
        insertUserMessage(chatId, userText, imageUrl)
        generateAiResponse(chatId, contextWindowSize, settingsSummary)
    }
}
