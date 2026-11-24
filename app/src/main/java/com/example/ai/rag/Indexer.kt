package com.example.ai.rag

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

interface EmbeddingProvider {
    suspend fun getEmbedding(text: String): FloatArray?
}

interface TagExtractor {
    suspend fun extractTags(text: String): Set<String>
}

class Indexer(
    private val embeddingProvider: EmbeddingProvider? = null,
    private val tagExtractor: TagExtractor? = null
) {
    suspend fun buildIndexedChunks(rawText: String, idPrefix: String = "lore"): List<LoreChunk> = coroutineScope {
        val baseChunks = TextChunker.chunkText(rawText, idPrefix)
        if (baseChunks.isEmpty()) return@coroutineScope emptyList()

        baseChunks.map { chunk ->
            async(Dispatchers.IO) {
                val extractedTags = tagExtractor?.extractTags(chunk.text) ?: emptySet()
                val mergedKeywords = chunk.keywords + extractedTags
                val embedding = embeddingProvider?.getEmbedding(chunk.text)

                chunk.copy(
                    keywords = mergedKeywords,
                    embedding = embedding
                )
            }
        }.awaitAll()
    }
}

object RagManager {

    fun embeddingToJson(embedding: FloatArray?): String? {
        if (embedding == null || embedding.isEmpty()) return null
        return embedding.joinToString(prefix = "[", postfix = "]", separator = ",")
    }

    fun parseEmbeddingJson(json: String?): FloatArray? {
        if (json.isNullOrBlank()) return null
        val clean = json.trim().removeSurrounding("[", "]").trim()
        if (clean.isBlank()) return null
        return try {
            val tokens = clean.split(",")
            val result = FloatArray(tokens.size)
            for (i in tokens.indices) {
                result[i] = tokens[i].trim().toFloat()
            }
            result
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Формирует единый текст лора из не-важных карточек знаний (делящихся на чанки RAG)
     * и дополнительного объемного описания персонажа при необходимости.
     */
    fun extractStandardLoreText(character: com.example.data.local.entity.CharacterEntity): String {
        val sb = StringBuilder()
        if (character.knowledgeCards.isNotBlank() && character.knowledgeCards != "[]") {
            try {
                val arr = org.json.JSONArray(character.knowledgeCards)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val isImp = obj.optBoolean("isImportant", false) || obj.optBoolean("important", false)
                    if (!isImp) {
                        val t = obj.optString("title").trim()
                        val c = obj.optString("content").trim()
                        if (t.isNotBlank() || c.isNotBlank()) {
                            if (t.isNotBlank()) sb.append("### ").append(t).append("\n")
                            if (c.isNotBlank()) sb.append(c).append("\n\n")
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Если обычных карточек знаний мало, но у персонажа есть объемные поля описания, включаем их в нарезку чанков
        if (sb.length < 300) {
            val extra = buildString {
                if (character.personality.isNotBlank()) append(character.personality).append("\n\n")
                if (character.appearance.isNotBlank()) append(character.appearance).append("\n\n")
                if (character.habits.isNotBlank()) append(character.habits).append("\n\n")
            }.trim()
            if (extra.length > 300) {
                if (sb.isNotEmpty()) sb.append("\n")
                sb.append(extra)
            }
        }
        return sb.toString().trim()
    }

    suspend fun getIndexedChunks(
        charId: Long, 
        text: String, 
        provider: EmbeddingProvider, 
        dao: com.example.data.local.dao.LoreChunkDao
    ): List<LoreChunk> = withContext(Dispatchers.IO) {
        val existingEntities = dao.getChunksForCharacter(charId)
        if (existingEntities.isNotEmpty()) {
            var updatedAny = false
            // Параллельно довычисляем недостающие векторы, если они отсутствовали
            val chunks = coroutineScope {
                existingEntities.map { entity ->
                    async(Dispatchers.IO) {
                        val kws = try {
                            val arr = org.json.JSONArray(entity.keywordsJson)
                            List(arr.length()) { arr.getString(it) }.toSet()
                        } catch (_: Exception) { emptySet() }

                        var emb = parseEmbeddingJson(entity.embeddingJson)
                        if (emb == null) {
                            val computed = provider.getEmbedding(entity.text)
                            if (computed != null) {
                                emb = computed
                                updatedAny = true
                            }
                        }

                        LoreChunk(
                            id = entity.id,
                            text = entity.text,
                            keywords = kws,
                            embedding = emb
                        )
                    }
                }.awaitAll()
            }

            if (updatedAny) {
                val updatedEntities = chunks.map { c ->
                    val kwJson = org.json.JSONArray(c.keywords).toString()
                    val embJson = embeddingToJson(c.embedding)
                    com.example.data.local.entity.LoreChunkEntity(
                        id = c.id,
                        characterId = charId,
                        text = c.text,
                        keywordsJson = kwJson,
                        embeddingJson = embJson
                    )
                }
                dao.insertChunks(updatedEntities)
            }
            return@withContext chunks
        }
        
        val indexer = Indexer(embeddingProvider = provider)
        val chunks = indexer.buildIndexedChunks(text, "lore_$charId")
        
        if (chunks.isNotEmpty()) {
            val entities = chunks.map { c ->
                val kwJson = org.json.JSONArray(c.keywords).toString()
                val embJson = embeddingToJson(c.embedding)
                com.example.data.local.entity.LoreChunkEntity(
                    id = c.id,
                    characterId = charId,
                    text = c.text,
                    keywordsJson = kwJson,
                    embeddingJson = embJson
                )
            }
            dao.insertChunks(entities)
        }
        
        return@withContext chunks
    }

    suspend fun reindexCharacter(
        charId: Long, 
        text: String, 
        provider: EmbeddingProvider, 
        dao: com.example.data.local.dao.LoreChunkDao
    ): List<LoreChunk> {
        dao.deleteChunksForCharacter(charId)
        return getIndexedChunks(charId, text, provider, dao)
    }
}
