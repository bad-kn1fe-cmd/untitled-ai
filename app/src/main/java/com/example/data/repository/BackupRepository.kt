package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupManifest(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val characterCount: Int = 0,
    val chatCount: Int = 0,
    val messageCount: Int = 0,
    val providerCount: Int = 0,
    val personaCount: Int = 0,
    val storyCount: Int = 0,
    val comicCount: Int = 0,
    val hasSettings: Boolean = false
)

data class SelectiveExportOptions(
    val includeCharacters: Boolean = true,
    val includeChatsAndMessages: Boolean = true,
    val includePersonas: Boolean = true,
    val includeProviders: Boolean = true,
    val includeApiKeys: Boolean = true,
    val includeStories: Boolean = true,
    val includeSettings: Boolean = true
)

class BackupRepository(private val context: Context, private val db: AppDatabase) {

    private fun copyUriToZip(
        zipOut: ZipOutputStream,
        uriString: String,
        subfolder: String,
        processedMap: MutableMap<String, String>
    ): String {
        val s = uriString.trim()
        if (s.isBlank()) return ""
        if (s.startsWith("drawable://") || s.startsWith("http://") || s.startsWith("https://")) return s
        if (processedMap.containsKey(s)) return processedMap[s]!!

        return try {
            val uri = Uri.parse(s)
            val extension = when {
                s.endsWith(".png", ignoreCase = true) -> "png"
                s.endsWith(".webp", ignoreCase = true) -> "webp"
                s.endsWith(".mp4", ignoreCase = true) -> "mp4"
                s.endsWith(".webm", ignoreCase = true) -> "webm"
                s.endsWith(".mkv", ignoreCase = true) -> "mkv"
                s.endsWith(".ttf", ignoreCase = true) -> "ttf"
                s.endsWith(".otf", ignoreCase = true) -> "otf"
                else -> {
                    val mime = context.contentResolver.getType(uri) ?: ""
                    when {
                        mime.contains("png", ignoreCase = true) -> "png"
                        mime.contains("video/", ignoreCase = true) -> "mp4"
                        mime.contains("webp", ignoreCase = true) -> "webp"
                        else -> "jpg"
                    }
                }
            }

            val filename = "${subfolder.trimEnd('s')}_${System.currentTimeMillis()}_${(1000..9999).random()}.$extension"
            val relativeZipPath = "files/$subfolder/$filename"

            val inputStream = if (s.startsWith("file://") || s.startsWith("/")) {
                val path = s.removePrefix("file://")
                val file = File(path)
                if (!file.exists()) return s
                FileInputStream(file)
            } else {
                context.contentResolver.openInputStream(uri) ?: return s
            }

            val entry = ZipEntry(relativeZipPath)
            zipOut.putNextEntry(entry)
            inputStream.copyTo(zipOut)
            inputStream.close()
            zipOut.closeEntry()

            processedMap[s] = relativeZipPath
            relativeZipPath
        } catch (e: Exception) {
            e.printStackTrace()
            s
        }
    }

    private fun copyUriArrayToZip(
        zipOut: ZipOutputStream,
        jsonArrayStr: String,
        subfolder: String,
        processedMap: MutableMap<String, String>
    ): String {
        if (jsonArrayStr.isBlank() || jsonArrayStr == "[]") return jsonArrayStr
        return try {
            val oldArray = JSONArray(jsonArrayStr)
            val newArray = JSONArray()
            for (i in 0 until oldArray.length()) {
                val item = oldArray.getString(i)
                val exportedPath = copyUriToZip(zipOut, item, subfolder, processedMap)
                newArray.put(exportedPath)
            }
            newArray.toString()
        } catch (_: Exception) {
            jsonArrayStr
        }
    }

    suspend fun exportBackup(
        destinationUri: Uri,
        options: SelectiveExportOptions = SelectiveExportOptions(),
        specificCharacterId: Long? = null
    ): Result<BackupManifest> = withContext(Dispatchers.IO) {
        try {
            val characters = if (options.includeCharacters) {
                if (specificCharacterId != null) {
                    listOfNotNull(db.characterDao().getCharacterById(specificCharacterId))
                } else {
                    db.characterDao().getAllCharacters().first()
                }
            } else emptyList()

            val chats = if (options.includeChatsAndMessages) {
                if (specificCharacterId != null) {
                    db.chatDao().getChatsForCharacter(specificCharacterId).first()
                } else {
                    db.chatDao().getAllChats().first()
                }
            } else emptyList()

            val chatIds = chats.map { it.id }.toSet()
            val allMessages = mutableListOf<MessageEntity>()
            val allSummaries = mutableListOf<SummaryEntity>()
            val allRelationships = mutableListOf<RelationshipTableEntity>()

            if (options.includeChatsAndMessages) {
                for (chatId in chatIds) {
                    allMessages.addAll(db.messageDao().getMessagesListForChat(chatId))
                    allSummaries.addAll(db.summaryDao().getSummariesForChat(chatId).first())
                    allRelationships.addAll(db.relationshipDao().getRelationshipTablesForChat(chatId).first())
                }
            }

            val providers = if (options.includeProviders) {
                db.aiProviderDao().getAllProfiles().first()
            } else emptyList()

            val personas = if (options.includePersonas) {
                db.userPersonaDao().getAllPersonasList()
            } else emptyList()

            val stories = if (options.includeStories) {
                db.storyDao().getAllStoriesList()
            } else emptyList()

            val comics = emptyList<com.example.data.local.entity.ComicEntity>()

            val manifest = BackupManifest(
                version = 2,
                exportedAt = System.currentTimeMillis(),
                characterCount = characters.size,
                chatCount = chats.size,
                messageCount = allMessages.size,
                providerCount = providers.size,
                personaCount = personas.size,
                storyCount = stories.size,
                comicCount = 0,
                hasSettings = options.includeSettings
            )

            val processedMap = mutableMapOf<String, String>()

            context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                ZipOutputStream(BufferedOutputStream(outStream)).use { zipOut ->

                    // Convert to JSON
                    val rootJson = JSONObject()
                    rootJson.put("manifest", JSONObject().apply {
                        put("version", manifest.version)
                        put("exportedAt", manifest.exportedAt)
                        put("characterCount", manifest.characterCount)
                        put("chatCount", manifest.chatCount)
                        put("messageCount", manifest.messageCount)
                        put("providerCount", manifest.providerCount)
                        put("personaCount", manifest.personaCount)
                        put("storyCount", manifest.storyCount)
                        put("comicCount", manifest.comicCount)
                        put("hasSettings", manifest.hasSettings)
                    })

                    // Characters Array
                    val charArray = JSONArray()
                    for (c in characters) {
                        charArray.put(JSONObject().apply {
                            put("id", c.id)
                            put("name", c.name)
                            put("avatarUri", copyUriToZip(zipOut, c.avatarUri, "avatars", processedMap))
                            put("shortDescription", c.shortDescription)
                            put("personality", c.personality)
                            put("appearance", c.appearance)
                            put("referenceImages", copyUriArrayToZip(zipOut, c.referenceImages, "references", processedMap))
                            put("habits", c.habits)
                            put("likes", c.likes)
                            put("dislikes", c.dislikes)
                            put("greetingMessage", c.greetingMessage)
                            put("age", c.age)
                            put("birthDate", c.birthDate)
                            put("knowledgeCards", c.knowledgeCards)
                            put("tags", c.tags)
                            put("voiceStyle", c.voiceStyle)
                            put("bannerUri", copyUriToZip(zipOut, c.bannerUri, "banners", processedMap))
                            put("greetingBannerUri", copyUriToZip(zipOut, c.greetingBannerUri, "banners", processedMap))
                            put("avatarFrameUri", copyUriToZip(zipOut, c.avatarFrameUri, "frames", processedMap))
                            put("startingRelationship", c.startingRelationship)
                            put("customRelationship", c.customRelationship)
                            put("initialRelationshipTitle", c.initialRelationshipTitle)
                            put("initialRelationshipPercent", c.initialRelationshipPercent)
                            put("relationshipBoardJson", c.relationshipBoardJson)
                            put("isAdult", c.isAdult)
                            put("isPinned", c.isPinned)
                            put("isArchived", c.isArchived)
                            put("isHidden", c.isHidden)
                            put("createdAt", c.createdAt)
                        })
                    }
                    rootJson.put("characters", charArray)

                    // Chats Array
                    val chatArray = JSONArray()
                    for (ch in chats) {
                        chatArray.put(JSONObject().apply {
                            put("id", ch.id)
                            put("characterId", ch.characterId)
                            put("title", ch.title)
                            put("relationshipType", ch.relationshipType)
                            put("customRelationship", ch.customRelationship)
                            put("formalityDistance", ch.formalityDistance)
                            put("isNsfwEnabled", ch.isNsfwEnabled)
                            put("isTextingMode", ch.isTextingMode)
                            put("isPinned", ch.isPinned)
                            put("isArchived", ch.isArchived)
                            put("summaryFrequency", ch.summaryFrequency)
                            put("roleplayMode", ch.roleplayMode)
                            put("responseLength", ch.responseLength)
                            put("atmosphere", ch.atmosphere)
                            put("createdAt", ch.createdAt)
                            put("updatedAt", ch.updatedAt)
                        })
                    }
                    rootJson.put("chats", chatArray)

                    // Messages Array
                    val msgArray = JSONArray()
                    for (m in allMessages) {
                        msgArray.put(JSONObject().apply {
                            put("id", m.id)
                            put("chatId", m.chatId)
                            put("sender", m.sender)
                            put("content", m.content)
                            put("imageUrl", m.imageUrl?.let { copyUriToZip(zipOut, it, "messages", processedMap) })
                            put("imagePrompt", m.imagePrompt)
                            put("internalReasoning", m.internalReasoning)
                            put("isChunked", m.isChunked)
                            put("isTextingMode", m.isTextingMode)
                            put("timestamp", m.timestamp)
                        })
                    }
                    rootJson.put("messages", msgArray)

                    // Summaries Array
                    val sumArray = JSONArray()
                    for (s in allSummaries) {
                        sumArray.put(JSONObject().apply {
                            put("id", s.id)
                            put("chatId", s.chatId)
                            put("messageRange", s.messageRange)
                            put("summaryText", s.summaryText)
                            put("timestamp", s.timestamp)
                        })
                    }
                    rootJson.put("summaries", sumArray)

                    // Relationships Array
                    val relArray = JSONArray()
                    for (r in allRelationships) {
                        relArray.put(JSONObject().apply {
                            put("id", r.id)
                            put("chatId", r.chatId)
                            put("status", r.status)
                            put("trustScore", r.trustScore)
                            put("mood", r.mood)
                            put("keyEvents", r.keyEvents)
                            put("openPlotThreads", r.openPlotThreads)
                            put("boardJson", r.boardJson)
                            put("timestamp", r.timestamp)
                        })
                    }
                    rootJson.put("relationships", relArray)

                    // Providers Array
                    val provArray = JSONArray()
                    for (p in providers) {
                        provArray.put(JSONObject().apply {
                            put("id", p.id)
                            put("name", p.name)
                            put("providerType", p.providerType)
                            put("baseUrl", p.baseUrl)
                            put("apiKey", if (options.includeApiKeys) p.apiKey else "")
                            put("model", p.model)
                            put("visionModel", p.visionModel)
                            put("temperature", p.temperature)
                            put("topP", p.topP)
                            put("maxTokens", p.maxTokens)
                            put("systemPromptTemplate", p.systemPromptTemplate)
                            put("isDefaultText", p.isDefaultText)
                            put("isDefaultImage", p.isDefaultImage)
                            put("isImageProvider", p.isImageProvider)
                            put("negativePrompt", p.negativePrompt)
                            put("imageResolution", p.imageResolution)
                            put("seedMode", p.seedMode)
                            put("fixedSeed", p.fixedSeed)
                            put("thinkingMode", p.thinkingMode)
                            put("thinkingBudget", p.thinkingBudget)
                            put("topK", p.topK)
                            put("frequencyPenalty", p.frequencyPenalty)
                            put("presencePenalty", p.presencePenalty)
                            put("repetitionPenalty", p.repetitionPenalty)
                            put("minP", p.minP)
                            if (p.seed != null) put("seed", p.seed)
                            put("stopSequences", p.stopSequences)
                            put("responseFormatMode", p.responseFormatMode)
                            put("extraHeadersJson", p.extraHeadersJson)
                            put("extraBodyJson", p.extraBodyJson)
                            put("rawJsonTemplate", p.rawJsonTemplate)
                            put("testPrompt", p.testPrompt)
                            put("testSystemPrompt", p.testSystemPrompt)
                            put("testMaxTokens", p.testMaxTokens)
                            put("testStream", p.testStream)
                            put("testCustomBodyJson", p.testCustomBodyJson)
                        })
                    }
                    rootJson.put("providers", provArray)

                    // Personas Array
                    val personaArray = JSONArray()
                    for (per in personas) {
                        personaArray.put(JSONObject().apply {
                            put("id", per.id)
                            put("name", per.name)
                            put("avatarUri", copyUriToZip(zipOut, per.avatarUri, "avatars", processedMap))
                            put("age", per.age)
                            put("gender", per.gender)
                            put("shortDescription", per.shortDescription)
                            put("appearance", per.appearance)
                            put("personality", per.personality)
                            put("habits", per.habits)
                            put("likes", per.likes)
                            put("dislikes", per.dislikes)
                            put("isSelected", per.isSelected)
                            put("createdAt", per.createdAt)
                        })
                    }
                    rootJson.put("personas", personaArray)

                    // Stories Array
                    val storyArray = JSONArray()
                    for (st in stories) {
                        storyArray.put(JSONObject().apply {
                            put("id", st.id)
                            put("title", st.title)
                            put("shortDescription", st.shortDescription)
                            put("lore", st.lore)
                            put("worldRules", st.worldRules)
                            put("characterIdsJson", st.characterIdsJson)
                            put("coverUri", copyUriToZip(zipOut, st.coverUri, "banners", processedMap))
                            put("tags", st.tags)
                            put("customSystemPrompt", st.customSystemPrompt)
                            put("isPinned", st.isPinned)
                            put("isArchived", st.isArchived)
                            put("createdAt", st.createdAt)
                            put("updatedAt", st.updatedAt)
                        })
                    }
                    rootJson.put("stories", storyArray)

                    // Comics Array
                    val comicArray = JSONArray()
                    for (cm in comics) {
                        comicArray.put(JSONObject().apply {
                            put("id", cm.id)
                            put("title", cm.title)
                            put("chatId", cm.chatId)
                            put("characterId", cm.characterId)
                            put("chapterName", cm.chapterName)
                            put("style", cm.style)
                            put("panelsJson", cm.panelsJson)
                            put("coverUri", copyUriToZip(zipOut, cm.coverUri, "banners", processedMap))
                            put("createdAt", cm.createdAt)
                        })
                    }
                    rootJson.put("comics", comicArray)

                    if (options.includeSettings) {
                        val userPrefs = com.example.data.datastore.UserPreferencesRepository(context)
                        val currentSettings = userPrefs.userSettingsFlow.first()
                        rootJson.put("settings", JSONObject().apply {
                            put("themeMode", currentSettings.themeMode)
                            put("accentColorLong", currentSettings.accentColorLong)
                            put("chatWallpaperPreset", currentSettings.chatWallpaperPreset)
                            put("customWallpaperUri", copyUriToZip(zipOut, currentSettings.customWallpaperUri, "wallpapers", processedMap))
                            put("customWallpapersJson", copyUriArrayToZip(zipOut, currentSettings.customWallpapersJson, "wallpapers", processedMap))
                            put("liveVideoWallpaperUri", copyUriToZip(zipOut, currentSettings.liveVideoWallpaperUri, "wallpapers", processedMap))
                            put("liveVideoWallpaperEnabled", currentSettings.liveVideoWallpaperEnabled)
                            put("appThemeBackgroundPreset", currentSettings.appThemeBackgroundPreset)
                            put("appCustomBackgroundUri", copyUriToZip(zipOut, currentSettings.appCustomBackgroundUri, "wallpapers", processedMap))
                            put("appCustomBackgroundAlpha", currentSettings.appCustomBackgroundAlpha.toDouble())
                            put("userActiveFrameUri", copyUriToZip(zipOut, currentSettings.userActiveFrameUri, "frames", processedMap))
                            put("userFramesJson", copyUriArrayToZip(zipOut, currentSettings.userFramesJson, "frames", processedMap))
                            put("customFontPath", copyUriToZip(zipOut, currentSettings.customFontPath, "fonts", processedMap))
                            put("customFontName", currentSettings.customFontName)
                            put("chatWallpaperAlpha", currentSettings.chatWallpaperAlpha.toDouble())
                            put("fontSizeSp", currentSettings.fontSizeSp)
                            put("bubbleStyle", currentSettings.bubbleStyle)
                            put("glassmorphismEnabled", currentSettings.glassmorphismEnabled)
                            put("wallpaperTone", currentSettings.wallpaperTone)
                            put("glassAlpha", currentSettings.glassAlpha.toDouble())
                            put("glassBlurRadius", currentSettings.glassBlurRadius.toDouble())
                            put("glassBorderGlow", currentSettings.glassBorderGlow)
                            put("glassSpecularIntensity", currentSettings.glassSpecularIntensity.toDouble())
                            put("frameParallaxEnabled", currentSettings.frameParallaxEnabled)
                            put("frameParallaxSensitivity", currentSettings.frameParallaxSensitivity.toDouble())
                            put("frameParallaxMaxOffset", currentSettings.frameParallaxMaxOffset.toDouble())
                            put("novelDividerEnabled", currentSettings.novelDividerEnabled)
                            put("novelDividerWidthPercent", currentSettings.novelDividerWidthPercent)
                            put("novelDividerThicknessDp", currentSettings.novelDividerThicknessDp)
                            put("novelDividerAlpha", currentSettings.novelDividerAlpha.toDouble())
                            put("novelDividerColorHex", currentSettings.novelDividerColorHex)
                            put("novelTextAlignment", currentSettings.novelTextAlignment)
                        })
                    }

                    // Write Manifest Entry
                    val entry = ZipEntry("manifest.json")
                    zipOut.putNextEntry(entry)
                    zipOut.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                    zipOut.closeEntry()
                }
            }

            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inspectBackup(sourceUri: Uri): Result<BackupManifest> = withContext(Dispatchers.IO) {
        try {
            var jsonString = ""
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                ZipInputStream(BufferedInputStream(inStream)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "manifest.json" || (entry.name.endsWith(".json") && !entry.name.startsWith("files/"))) {
                            val reader = BufferedReader(InputStreamReader(zipIn, Charsets.UTF_8))
                            jsonString = reader.readText()
                            break
                        }
                        entry = zipIn.nextEntry
                    }
                }
            }

            if (jsonString.isBlank()) return@withContext Result.failure(IOException("Не найден manifest.json в архиве"))

            val root = JSONObject(jsonString)
            val chars = root.optJSONArray("characters")?.length() ?: 0
            val chats = root.optJSONArray("chats")?.length() ?: 0
            val msgs = root.optJSONArray("messages")?.length() ?: 0
            val provs = root.optJSONArray("providers")?.length() ?: 0
            val hasSettings = root.optJSONObject("manifest")?.optBoolean("hasSettings", false) == true || root.has("settings")

            Result.success(
                BackupManifest(
                    characterCount = chars,
                    chatCount = chats,
                    messageCount = msgs,
                    providerCount = provs,
                    hasSettings = hasSettings
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(sourceUri: Uri, replaceAll: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var jsonString = ""
            val importedMediaMap = mutableMapOf<String, String>()

            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                ZipInputStream(BufferedInputStream(inStream)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "manifest.json" || (entry.name.endsWith(".json") && !entry.name.startsWith("files/"))) {
                            val reader = BufferedReader(InputStreamReader(zipIn, Charsets.UTF_8))
                            jsonString = reader.readText()
                        } else if (!entry.isDirectory && (entry.name.startsWith("files/") || entry.name.startsWith("media/"))) {
                            val relativePath = entry.name
                            val fileName = File(relativePath).name
                            val subfolder = File(relativePath).parentFile?.name ?: "wallpapers"

                            val targetFolder = File(context.filesDir, subfolder).apply {
                                if (!exists()) mkdirs()
                            }
                            val targetFile = File(targetFolder, fileName)

                            FileOutputStream(targetFile).use { fos ->
                                zipIn.copyTo(fos)
                                fos.flush()
                            }

                            val newLocalUri = Uri.fromFile(targetFile).toString()
                            importedMediaMap[relativePath] = newLocalUri
                            importedMediaMap[relativePath.removePrefix("files/")] = newLocalUri
                            importedMediaMap[relativePath.removePrefix("media/")] = newLocalUri
                            importedMediaMap[fileName] = newLocalUri
                        }
                        entry = zipIn.nextEntry
                    }
                }
            }

            if (jsonString.isBlank()) return@withContext Result.failure(IOException("Архив поврежден или пуст"))
            val root = JSONObject(jsonString)

            fun resolveImportedUri(storedUri: String): String {
                val s = storedUri.trim()
                if (s.isBlank()) return ""
                if (s.startsWith("drawable://") || s.startsWith("http://") || s.startsWith("https://")) return s
                if (importedMediaMap.containsKey(s)) return importedMediaMap[s]!!

                if (s.startsWith("files/") || s.startsWith("media/")) {
                    val key1 = s
                    val key2 = s.removePrefix("files/").removePrefix("media/")
                    if (importedMediaMap.containsKey(key1)) return importedMediaMap[key1]!!
                    if (importedMediaMap.containsKey(key2)) return importedMediaMap[key2]!!

                    val subfolderFile = File(context.filesDir, key2)
                    if (subfolderFile.exists()) return Uri.fromFile(subfolderFile).toString()
                }

                if (s.startsWith("file://") || s.startsWith("/")) {
                    val path = s.removePrefix("file://")
                    val localFile = File(path)
                    if (localFile.exists()) return s
                    val fileName = localFile.name
                    if (importedMediaMap.containsKey(fileName)) return importedMediaMap[fileName]!!

                    val subfolders = listOf("wallpapers", "banners", "frames", "avatars", "references", "fonts")
                    for (sub in subfolders) {
                        val candidate = File(File(context.filesDir, sub), fileName)
                        if (candidate.exists()) return Uri.fromFile(candidate).toString()
                    }
                }

                return s
            }

            fun resolveImportedUriArray(jsonArrayStr: String): String {
                if (jsonArrayStr.isBlank() || jsonArrayStr == "[]") return jsonArrayStr
                return try {
                    val oldArray = JSONArray(jsonArrayStr)
                    val newArray = JSONArray()
                    for (i in 0 until oldArray.length()) {
                        val item = oldArray.getString(i)
                        newArray.put(resolveImportedUri(item))
                    }
                    newArray.toString()
                } catch (_: Exception) {
                    jsonArrayStr
                }
            }

            if (replaceAll) {
                // Clear existing
                val existingChats = db.chatDao().getAllChats().first()
                for (ch in existingChats) db.chatDao().deleteChatById(ch.id)
                val existingChars = db.characterDao().getAllCharacters().first()
                for (c in existingChars) db.characterDao().deleteCharacterById(c.id)
            }

            // Map old IDs to new IDs
            val charIdMap = mutableMapOf<Long, Long>()
            val chatIdMap = mutableMapOf<Long, Long>()

            // 1. Import Characters
            val charArray = root.optJSONArray("characters") ?: JSONArray()
            for (i in 0 until charArray.length()) {
                val obj = charArray.getJSONObject(i)
                val oldId = obj.optLong("id", 0L)
                val entity = CharacterEntity(
                    name = obj.optString("name"),
                    avatarUri = resolveImportedUri(obj.optString("avatarUri")),
                    shortDescription = obj.optString("shortDescription"),
                    personality = obj.optString("personality"),
                    appearance = obj.optString("appearance"),
                    referenceImages = resolveImportedUriArray(obj.optString("referenceImages", "[]")),
                    habits = obj.optString("habits"),
                    likes = obj.optString("likes"),
                    dislikes = obj.optString("dislikes"),
                    greetingMessage = obj.optString("greetingMessage"),
                    age = obj.optInt("age", 18),
                    birthDate = obj.optString("birthDate"),
                    knowledgeCards = obj.optString("knowledgeCards", "[]"),
                    tags = obj.optString("tags", "[]"),
                    voiceStyle = obj.optString("voiceStyle", "natural"),
                    bannerUri = resolveImportedUri(obj.optString("bannerUri", "")),
                    greetingBannerUri = resolveImportedUri(obj.optString("greetingBannerUri", "")),
                    avatarFrameUri = resolveImportedUri(obj.optString("avatarFrameUri", "")),
                    isAdult = obj.optInt("age", 18) >= 18,
                    isPinned = obj.optBoolean("isPinned", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    isHidden = obj.optBoolean("isHidden", false),
                    startingRelationship = obj.optString("startingRelationship", "strangers"),
                    customRelationship = obj.optString("customRelationship", ""),
                    initialRelationshipTitle = obj.optString("initialRelationshipTitle", ""),
                    initialRelationshipPercent = obj.optInt("initialRelationshipPercent", -1),
                    relationshipBoardJson = obj.optString("relationshipBoardJson", "")
                )
                val newId = db.characterDao().insertCharacter(entity)
                if (oldId != 0L) charIdMap[oldId] = newId
            }

            // 2. Import Chats
            val chatArray = root.optJSONArray("chats") ?: JSONArray()
            for (i in 0 until chatArray.length()) {
                val obj = chatArray.getJSONObject(i)
                val oldChatId = obj.optLong("id", 0L)
                val oldCharId = obj.optLong("characterId", 0L)
                val mappedCharId = charIdMap[oldCharId] ?: continue

                val chatEntity = ChatEntity(
                    characterId = mappedCharId,
                    title = obj.optString("title", "Диалог"),
                    relationshipType = obj.optString("relationshipType", "strangers"),
                    customRelationship = obj.optString("customRelationship"),
                    formalityDistance = obj.optString("formalityDistance", "informal"),
                    isNsfwEnabled = obj.optBoolean("isNsfwEnabled", false),
                    isTextingMode = obj.optBoolean("isTextingMode", false),
                    isPinned = obj.optBoolean("isPinned", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    summaryFrequency = obj.optInt("summaryFrequency", 18),
                    roleplayMode = obj.optString("roleplayMode", "medium"),
                    responseLength = obj.optString("responseLength", "medium"),
                    atmosphere = obj.optString("atmosphere", "default")
                )
                val newChatId = db.chatDao().insertChat(chatEntity)
                if (oldChatId != 0L) chatIdMap[oldChatId] = newChatId
            }

            // 3. Import Messages
            val msgArray = root.optJSONArray("messages") ?: JSONArray()
            for (i in 0 until msgArray.length()) {
                val obj = msgArray.getJSONObject(i)
                val oldChatId = obj.optLong("chatId", 0L)
                val mappedChatId = chatIdMap[oldChatId] ?: continue

                val msg = MessageEntity(
                    chatId = mappedChatId,
                    sender = obj.optString("sender", "user"),
                    content = obj.optString("content", ""),
                    imageUrl = obj.optString("imageUrl").takeIf { it.isNotBlank() }?.let { resolveImportedUri(it) },
                    imagePrompt = obj.optString("imagePrompt").takeIf { it.isNotBlank() },
                    internalReasoning = obj.optString("internalReasoning").takeIf { it.isNotBlank() },
                    isChunked = obj.optBoolean("isChunked", false),
                    isTextingMode = obj.optBoolean("isTextingMode", false),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                db.messageDao().insertMessage(msg)
            }

            // 4. Import Summaries
            val sumArray = root.optJSONArray("summaries") ?: JSONArray()
            for (i in 0 until sumArray.length()) {
                val obj = sumArray.getJSONObject(i)
                val oldChatId = obj.optLong("chatId", 0L)
                val mappedChatId = chatIdMap[oldChatId] ?: continue

                val sum = SummaryEntity(
                    chatId = mappedChatId,
                    messageRange = obj.optString("messageRange"),
                    summaryText = obj.optString("summaryText"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                db.summaryDao().insertSummary(sum)
            }

            // 5. Import Relationships
            val relArray = root.optJSONArray("relationships") ?: JSONArray()
            for (i in 0 until relArray.length()) {
                val obj = relArray.getJSONObject(i)
                val oldChatId = obj.optLong("chatId", 0L)
                val mappedChatId = chatIdMap[oldChatId] ?: continue

                val rel = RelationshipTableEntity(
                    chatId = mappedChatId,
                    status = obj.optString("status", "Общение"),
                    trustScore = obj.optInt("trustScore", 10),
                    mood = obj.optString("mood", "Нейтральное"),
                    keyEvents = obj.optString("keyEvents", "[]"),
                    openPlotThreads = obj.optString("openPlotThreads", "[]"),
                    boardJson = obj.optString("boardJson", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                db.relationshipDao().insertRelationshipTable(rel)
            }

            // 6. Import Providers
            val provArray = root.optJSONArray("providers") ?: JSONArray()
            for (i in 0 until provArray.length()) {
                val obj = provArray.getJSONObject(i)
                val entity = AiProviderProfileEntity(
                    name = obj.optString("name", "Импортированный провайдер"),
                    providerType = obj.optString("providerType", "OPENROUTER"),
                    baseUrl = obj.optString("baseUrl"),
                    apiKey = obj.optString("apiKey"),
                    model = obj.optString("model"),
                    temperature = obj.optDouble("temperature", 0.85).toFloat(),
                    topP = obj.optDouble("topP", 0.95).toFloat(),
                    maxTokens = obj.optInt("maxTokens", 1500),
                    systemPromptTemplate = obj.optString("systemPromptTemplate"),
                    isDefaultText = obj.optBoolean("isDefaultText", false),
                    isDefaultImage = obj.optBoolean("isDefaultImage", false),
                    isImageProvider = obj.optBoolean("isImageProvider", false),
                    negativePrompt = obj.optString("negativePrompt"),
                    imageResolution = obj.optString("imageResolution", "1024x1024"),
                    seedMode = obj.optString("seedMode", "random"),
                    fixedSeed = obj.optLong("fixedSeed", 42L),
                    thinkingMode = obj.optString("thinkingMode", "auto"),
                    thinkingBudget = obj.optInt("thinkingBudget", 0),
                    topK = obj.optInt("topK", 0),
                    frequencyPenalty = obj.optDouble("frequencyPenalty", 0.0).toFloat(),
                    presencePenalty = obj.optDouble("presencePenalty", 0.0).toFloat(),
                    repetitionPenalty = obj.optDouble("repetitionPenalty", 1.0).toFloat(),
                    minP = obj.optDouble("minP", 0.0).toFloat(),
                    seed = if (obj.has("seed") && !obj.isNull("seed")) obj.optInt("seed") else null,
                    stopSequences = obj.optString("stopSequences", ""),
                    responseFormatMode = obj.optString("responseFormatMode", "default"),
                    extraHeadersJson = obj.optString("extraHeadersJson", "{}"),
                    extraBodyJson = obj.optString("extraBodyJson", "{}"),
                    rawJsonTemplate = obj.optString("rawJsonTemplate", ""),
                    testPrompt = obj.optString("testPrompt", "Привет! Ответь одним коротким предложением, что связь работает."),
                    testSystemPrompt = obj.optString("testSystemPrompt", "You are a helpful assistant."),
                    testMaxTokens = obj.optInt("testMaxTokens", 128),
                    testStream = obj.optBoolean("testStream", false),
                    testCustomBodyJson = obj.optString("testCustomBodyJson", "")
                )
                db.aiProviderDao().insertProfile(entity)
            }

            // 7. Import Personas
            val personaArray = root.optJSONArray("personas") ?: JSONArray()
            for (i in 0 until personaArray.length()) {
                val obj = personaArray.getJSONObject(i)
                val entity = UserPersonaEntity(
                    name = obj.optString("name", "Пользователь"),
                    avatarUri = resolveImportedUri(obj.optString("avatarUri", "drawable://avatar_app_dark")),
                    age = obj.optInt("age", 22),
                    gender = obj.optString("gender", ""),
                    shortDescription = obj.optString("shortDescription", ""),
                    appearance = obj.optString("appearance", ""),
                    personality = obj.optString("personality", ""),
                    habits = obj.optString("habits", ""),
                    likes = obj.optString("likes", ""),
                    dislikes = obj.optString("dislikes", ""),
                    isSelected = obj.optBoolean("isSelected", i == 0),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                db.userPersonaDao().insertPersona(entity)
            }

            // 8. Import Stories
            val storyArray = root.optJSONArray("stories") ?: JSONArray()
            for (i in 0 until storyArray.length()) {
                val obj = storyArray.getJSONObject(i)
                val entity = StoryEntity(
                    title = obj.optString("title", "История"),
                    shortDescription = obj.optString("shortDescription", ""),
                    lore = obj.optString("lore", ""),
                    worldRules = obj.optString("worldRules", ""),
                    characterIdsJson = obj.optString("characterIdsJson", "[]"),
                    coverUri = resolveImportedUri(obj.optString("coverUri", "")),
                    tags = obj.optString("tags", "[]"),
                    customSystemPrompt = obj.optString("customSystemPrompt", ""),
                    isPinned = obj.optBoolean("isPinned", false),
                    isArchived = obj.optBoolean("isArchived", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                db.storyDao().insertStory(entity)
            }

            // 9. Import Comics (Disabled)

            // 10. Import settings (wallpapers, live video, frames, theme & accent colors)
            if (root.has("settings")) {
                val settingsObj = root.getJSONObject("settings")
                val userPrefs = com.example.data.datastore.UserPreferencesRepository(context)
                val currentSettings = userPrefs.userSettingsFlow.first()

                val updatedSettings = currentSettings.copy(
                    themeMode = settingsObj.optString("themeMode", currentSettings.themeMode),
                    accentColorLong = settingsObj.optLong("accentColorLong", currentSettings.accentColorLong),
                    chatWallpaperPreset = settingsObj.optString("chatWallpaperPreset", currentSettings.chatWallpaperPreset),
                    customWallpaperUri = resolveImportedUri(settingsObj.optString("customWallpaperUri", currentSettings.customWallpaperUri)),
                    customWallpapersJson = resolveImportedUriArray(settingsObj.optString("customWallpapersJson", currentSettings.customWallpapersJson)),
                    liveVideoWallpaperUri = resolveImportedUri(settingsObj.optString("liveVideoWallpaperUri", currentSettings.liveVideoWallpaperUri)),
                    liveVideoWallpaperEnabled = settingsObj.optBoolean("liveVideoWallpaperEnabled", currentSettings.liveVideoWallpaperEnabled),
                    appThemeBackgroundPreset = settingsObj.optString("appThemeBackgroundPreset", currentSettings.appThemeBackgroundPreset),
                    appCustomBackgroundUri = resolveImportedUri(settingsObj.optString("appCustomBackgroundUri", currentSettings.appCustomBackgroundUri)),
                    appCustomBackgroundAlpha = settingsObj.optDouble("appCustomBackgroundAlpha", currentSettings.appCustomBackgroundAlpha.toDouble()).toFloat(),
                    userActiveFrameUri = resolveImportedUri(settingsObj.optString("userActiveFrameUri", currentSettings.userActiveFrameUri)),
                    userFramesJson = resolveImportedUriArray(settingsObj.optString("userFramesJson", currentSettings.userFramesJson)),
                    customFontPath = resolveImportedUri(settingsObj.optString("customFontPath", currentSettings.customFontPath)),
                    customFontName = settingsObj.optString("customFontName", currentSettings.customFontName),
                    glassmorphismEnabled = settingsObj.optBoolean("glassmorphismEnabled", currentSettings.glassmorphismEnabled),
                    wallpaperTone = settingsObj.optString("wallpaperTone", currentSettings.wallpaperTone),
                    glassAlpha = settingsObj.optDouble("glassAlpha", currentSettings.glassAlpha.toDouble()).toFloat(),
                    glassBlurRadius = settingsObj.optDouble("glassBlurRadius", currentSettings.glassBlurRadius.toDouble()).toFloat(),
                    glassBorderGlow = settingsObj.optBoolean("glassBorderGlow", currentSettings.glassBorderGlow),
                    glassSpecularIntensity = settingsObj.optDouble("glassSpecularIntensity", currentSettings.glassSpecularIntensity.toDouble()).toFloat(),
                    frameParallaxEnabled = settingsObj.optBoolean("frameParallaxEnabled", currentSettings.frameParallaxEnabled),
                    frameParallaxSensitivity = settingsObj.optDouble("frameParallaxSensitivity", currentSettings.frameParallaxSensitivity.toDouble()).toFloat(),
                    frameParallaxMaxOffset = settingsObj.optDouble("frameParallaxMaxOffset", currentSettings.frameParallaxMaxOffset.toDouble()).toFloat(),
                    fontSizeSp = settingsObj.optInt("fontSizeSp", currentSettings.fontSizeSp),
                    bubbleStyle = settingsObj.optString("bubbleStyle", currentSettings.bubbleStyle),
                    novelDividerEnabled = settingsObj.optBoolean("novelDividerEnabled", currentSettings.novelDividerEnabled),
                    novelDividerWidthPercent = settingsObj.optInt("novelDividerWidthPercent", currentSettings.novelDividerWidthPercent),
                    novelDividerThicknessDp = settingsObj.optInt("novelDividerThicknessDp", currentSettings.novelDividerThicknessDp),
                    novelDividerAlpha = settingsObj.optDouble("novelDividerAlpha", currentSettings.novelDividerAlpha.toDouble()).toFloat(),
                    novelDividerColorHex = settingsObj.optString("novelDividerColorHex", currentSettings.novelDividerColorHex),
                    novelTextAlignment = settingsObj.optString("novelTextAlignment", currentSettings.novelTextAlignment)
                )
                userPrefs.updateSettings(updatedSettings)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
