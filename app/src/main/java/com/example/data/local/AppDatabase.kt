package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CharacterEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        SummaryEntity::class,
        RelationshipTableEntity::class,
        AiProviderProfileEntity::class,
        UserPersonaEntity::class,
        StoryEntity::class,
        ComicEntity::class
    ],
    version = 20,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun summaryDao(): SummaryDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun aiProviderDao(): AiProviderDao
    abstract fun userPersonaDao(): UserPersonaDao
    abstract fun storyDao(): StoryDao
    abstract fun comicDao(): ComicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN audioFilePath TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE messages ADD COLUMN audioDurationSec INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE messages ADD COLUMN reaction TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE characters ADD COLUMN author TEXT NOT NULL DEFAULT 'UntitledAI'")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE characters ADD COLUMN startingRelationship TEXT NOT NULL DEFAULT 'strangers'")
                database.execSQL("ALTER TABLE characters ADD COLUMN customRelationship TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE characters ADD COLUMN bannerUri TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE characters ADD COLUMN communicationFormality TEXT NOT NULL DEFAULT 'adaptive'")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE characters ADD COLUMN avatarFrameUri TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_personas ADD COLUMN avatarFrameUri TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE characters ADD COLUMN greetingBannerUri TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE characters ADD COLUMN initialRelationshipTitle TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE characters ADD COLUMN initialRelationshipPercent INTEGER NOT NULL DEFAULT -1")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE characters ADD COLUMN relationshipBoardJson TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE relationship_tables ADD COLUMN boardJson TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                fun safeAddCol(sql: String) {
                    try { database.execSQL(sql) } catch (_: Exception) {}
                }
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN imageModel TEXT NOT NULL DEFAULT ''")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN systemPromptPrefix TEXT NOT NULL DEFAULT ''")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN extraHeadersJson TEXT NOT NULL DEFAULT '{}'")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN thinkingMode TEXT NOT NULL DEFAULT 'auto'")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN thinkingBudget INTEGER NOT NULL DEFAULT 0")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN topK INTEGER NOT NULL DEFAULT 0")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN frequencyPenalty REAL NOT NULL DEFAULT 0")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN presencePenalty REAL NOT NULL DEFAULT 0")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN repetitionPenalty REAL NOT NULL DEFAULT 1")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN minP REAL NOT NULL DEFAULT 0")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN seed INTEGER DEFAULT NULL")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN stopSequences TEXT NOT NULL DEFAULT ''")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN responseFormatMode TEXT NOT NULL DEFAULT 'default'")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN extraBodyJson TEXT NOT NULL DEFAULT '{}'")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN rawJsonTemplate TEXT NOT NULL DEFAULT ''")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN testPrompt TEXT NOT NULL DEFAULT 'Привет! Ответь одним коротким предложением, что связь работает.'")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN testSystemPrompt TEXT NOT NULL DEFAULT 'You are a helpful assistant.'")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN testMaxTokens INTEGER NOT NULL DEFAULT 128")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN testStream INTEGER NOT NULL DEFAULT 0")
                safeAddCol("ALTER TABLE ai_provider_profiles ADD COLUMN testCustomBodyJson TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase = getInstance(context)

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "ai_roleplay.db"
                    )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    INSTANCE?.seedInitialData()
                                } catch (_: Exception) {}
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val database = INSTANCE ?: return@launch
                                    database.characterDao().deleteAliceAndMark()

                                    // Remove Eleanor as requested by user
                                    val allChars = database.characterDao().getAllCharactersList()
                                    for (c in allChars) {
                                        if (c.name.contains("Элинор", ignoreCase = true) || c.name.equals("Elena", ignoreCase = true)) {
                                            database.characterDao().deleteCharacter(c)
                                        }
                                    }

                                    // Deduplicate characters with identical names
                                    val refreshedChars = database.characterDao().getAllCharactersList()
                                    val groupedByName = refreshedChars.groupBy { it.name }
                                    for ((_, group) in groupedByName) {
                                        if (group.size > 1) {
                                            // Keep the one with lowest id (original) and remove duplicates
                                            group.drop(1).forEach { duplicate ->
                                                database.characterDao().deleteCharacter(duplicate)
                                            }
                                        }
                                    }
                                    if (database.characterDao().getCharacterCount() == 0) {
                                        database.seedInitialData()
                                    }
                                    // Ensure Untitled Assistant character exists with app_logo_black avatar and texting mode
                                    val personaAssistant = database.characterDao().getAllCharactersList().find { it.name == "PersonaAI Ассистент" }
                                    if (personaAssistant != null) {
                                        database.characterDao().updateCharacter(
                                            personaAssistant.copy(
                                                name = "Untitled Ассистент",
                                                shortDescription = "Официальный ИИ-помощник движка Untitled. Подскажет настройки, исправит ошибки, поможет с промптами.",
                                                personality = "Компетентный, дружелюбный, практичный технический консультант и помощник по движку Untitled.",
                                                appearance = "Фирменный стиль Untitled на черном фоне.",
                                                tags = "[\"Ассистент\", \"Помощь\", \"Untitled\"]"
                                            )
                                        )
                                    }
                                    val assistant = database.characterDao().getAllCharactersList().find { it.name == "Untitled Ассистент" }
                                    if (assistant == null && personaAssistant == null) {
                                        database.characterDao().insertCharacter(
                                            CharacterEntity(
                                                name = "Untitled Ассистент",
                                                avatarUri = "drawable://app_logo_black",
                                                shortDescription = "Официальный ИИ-помощник движка Untitled. Подскажет настройки, исправит ошибки, поможет с промптами.",
                                                personality = "Компетентный, дружелюбный, практичный технический консультант и помощник по движку Untitled.",
                                                appearance = "Фирменный стиль Untitled на черном фоне.",
                                                referenceImages = "[\"drawable://app_logo_black\"]",
                                                habits = "Дает четкие понятные советы, помогает составить промпты и решить ошибки API",
                                                likes = "Чистый код, гибкие настройки OpenRouter, свобода творчества",
                                                dislikes = "Искусственная цензура, скрытые лимиты, сбои серверов",
                                                greetingMessage = "Привет! Я твой ассистент по движку Untitled.\n\nЯ помогу настроить любые ИИ-провайдеры (OpenRouter, Groq, Gemini), подобрать оптимальные параметры моделей, составить карточку персонажа или персоны пользователя, а также подскажу решение любых ошибок.\n\nЗадавай любые вопросы по приложению или настройкам!",
                                                age = 99,
                                                birthDate = "",
                                                knowledgeCards = "[{\"title\":\"Концепция Untitled\",\"content\":\"Локальный ролплей-движок без ограничений по NSFW (для 18+ персонажей), без искусственной цензуры, работающий на прямых API-ключах с полной приватностью данных на устройстве.\"}]",
                                                tags = "[\"Ассистент\", \"Помощь\", \"Untitled\"]",
                                                voiceStyle = "natural",
                                                isAdult = true
                                            )
                                        )
                                    } else if (assistant != null && assistant.avatarUri != "drawable://app_logo_black") {
                                        database.characterDao().updateCharacter(assistant.copy(avatarUri = "drawable://app_logo_black"))
                                    }

                                    // Update default models for OpenRouter if needed
                                    val openRouterProfile = database.aiProviderDao().getAllProfilesList().find { it.providerType == "OPENROUTER" }
                                    if (openRouterProfile != null) {
                                        var needsUpdate = false
                                        var updated = openRouterProfile
                                        if (updated.model.isBlank() || updated.model == "meta-llama/llama-3.3-70b-instruct:free" || updated.model == "deepseek/deepseek-chat") {
                                            updated = updated.copy(model = "google/gemma-4-26b-a4b-it:free")
                                            needsUpdate = true
                                        }
                                        if (updated.visionModel.isBlank() || updated.visionModel == "google/gemini-2.0-flash-001") {
                                            updated = updated.copy(visionModel = "google/gemma-4-26b-a4b-it:free")
                                            needsUpdate = true
                                        }
                                        if (needsUpdate) {
                                            database.aiProviderDao().updateProfile(updated)
                                        }
                                    }

                                    // Update default model for Google Gemini to gemini-3.5-flash-lite
                                    val geminiProfiles = database.aiProviderDao().getAllProfilesList().filter { it.providerType == "GEMINI" }
                                    for (gp in geminiProfiles) {
                                        var updatedGp = gp
                                        var gpNeedsUpdate = false
                                        if (updatedGp.model.isBlank() || updatedGp.model == "gemini-2.5-flash") {
                                            updatedGp = updatedGp.copy(model = "gemini-3.5-flash-lite")
                                            gpNeedsUpdate = true
                                        }
                                        if (updatedGp.visionModel.isBlank() || updatedGp.visionModel == "gemini-2.5-flash") {
                                            updatedGp = updatedGp.copy(visionModel = "gemini-3.5-flash-lite")
                                            gpNeedsUpdate = true
                                        }
                                        if (gpNeedsUpdate) {
                                            database.aiProviderDao().updateProfile(updatedGp)
                                        }
                                    }

                                    // Ensure Civitai profile exists if no image profile
                                    val civitaiExists = database.aiProviderDao().getAllProfilesList().any { it.providerType == "CIVITAI" }
                                    if (!civitaiExists) {
                                        database.aiProviderDao().insertProfile(
                                            AiProviderProfileEntity(
                                                name = "Civitai (Pony Diffusion V6)",
                                                providerType = "CIVITAI",
                                                baseUrl = "https://orchestration.civitai.com/v2/consumer",
                                                apiKey = "",
                                                model = "urn:air:sdxl:checkpoint:civitai:257201@290640",
                                                isDefaultText = false,
                                                isDefaultImage = true,
                                                isImageProvider = true,
                                                negativePrompt = "score_4, score_5, score_6, ugly, deformed, blurry, bad anatomy",
                                                imageResolution = "832x1216"
                                            )
                                        )
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    })
                    .build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }

    suspend fun seedInitialData() {
        if (aiProviderDao().getProfileCount() == 0) {
            val openRouter = AiProviderProfileEntity(
                name = "OpenRouter (Default)",
                providerType = "OPENROUTER",
                baseUrl = "https://openrouter.ai/api/v1",
                apiKey = "",
                model = "google/gemma-4-26b-a4b-it:free",
                visionModel = "google/gemma-4-26b-a4b-it:free",
                temperature = 0.85f,
                topP = 0.95f,
                maxTokens = 1800,
                isDefaultText = true,
                isDefaultImage = false,
                isImageProvider = false
            )
            val gemini = AiProviderProfileEntity(
                name = "Google Gemini (Text & Vision)",
                providerType = "GEMINI",
                baseUrl = "https://generativelanguage.googleapis.com",
                apiKey = "",
                model = "gemini-3.5-flash-lite",
                visionModel = "gemini-3.5-flash-lite",
                temperature = 0.85f,
                topP = 0.95f,
                maxTokens = 2048,
                isDefaultText = false,
                isDefaultImage = false,
                isImageProvider = false
            )
            val civitai = AiProviderProfileEntity(
                name = "Civitai (Pony Diffusion V6)",
                providerType = "CIVITAI",
                baseUrl = "https://orchestration.civitai.com/v2/consumer",
                apiKey = "",
                model = "urn:air:sdxl:checkpoint:civitai:257201@290640",
                isDefaultText = false,
                isDefaultImage = true,
                isImageProvider = true,
                negativePrompt = "score_4, score_5, score_6, ugly, deformed, blurry, bad anatomy",
                imageResolution = "832x1216"
            )
            aiProviderDao().insertProfiles(listOf(openRouter, gemini, civitai))
        }

        if (characterDao().getCharacterCount() == 0) {
            // Seed Assistant
            val assistant = CharacterEntity(
                name = "Untitled Ассистент",
                avatarUri = "drawable://app_logo_black",
                shortDescription = "Официальный ИИ-помощник движка Untitled. Подскажет настройки, исправит ошибки, поможет с промптами.",
                personality = "Компетентный, дружелюбный, практичный технический консультант и помощник по движку Untitled.",
                appearance = "Фирменный стиль Untitled на глубоком черном фоне.",
                referenceImages = "[\"drawable://app_logo_black\"]",
                habits = "Дает четкие понятные советы, помогает составить промпты и решить ошибки API",
                likes = "Чистый код, гибкие настройки моделей, свобода творчества",
                dislikes = "Искусственная цензура, скрытые лимиты, сбои серверов",
                greetingMessage = "Привет! Я твой ассистент по движку Untitled.\n\nЯ помогу настроить любые ИИ-провайдеры (OpenRouter, Groq, Gemini), подобрать оптимальные параметры моделей, составить карточку персонажа или персоны пользователя, а также подскажу решение любых ошибок.\n\nЗадавай любые вопросы по приложению или настройкам!",
                age = 99,
                birthDate = "",
                knowledgeCards = "[{\"title\":\"Концепция Untitled\",\"content\":\"Локальный ролплей-движок без ограничений по NSFW (для 18+ персонажей), без искусственной цензуры, работающий на прямых API-ключах с полной приватностью данных на устройстве.\"}]",
                tags = "[\"Ассистент\", \"Помощь\", \"Untitled\"]",
                voiceStyle = "natural",
                isAdult = true
            )
            val assistantId = characterDao().insertCharacter(assistant)
            val chatAssistant = ChatEntity(
                characterId = assistantId,
                title = "Помощь и консультации",
                relationshipType = "friends",
                formalityDistance = "informal",
                isNsfwEnabled = true,
                isTextingMode = true,
                summaryFrequency = 20
            )
            val assistantChatId = chatDao().insertChat(chatAssistant)
            messageDao().insertMessage(
                MessageEntity(
                    chatId = assistantChatId,
                    sender = "character",
                    content = assistant.greetingMessage,
                    isTextingMode = true,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}
